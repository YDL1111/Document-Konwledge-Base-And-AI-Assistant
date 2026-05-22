package com.docbase.domain.knowledge.ingest;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.docbase.common.constant.Constants.UploadSubDir;
import com.docbase.common.core.page.PageDTO;
import com.docbase.common.exception.ApiException;
import com.docbase.common.exception.error.ErrorCode;
import com.docbase.common.utils.file.FileUploadUtils;
import com.docbase.domain.knowledge.document.KnowledgeDocumentConstant;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentEntity;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentService;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentVersionEntity;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentVersionService;
import com.docbase.domain.knowledge.ingest.db.KnowledgeIngestTaskEntity;
import com.docbase.domain.knowledge.ingest.db.KnowledgeIngestTaskService;
import com.docbase.domain.knowledge.ingest.dto.KnowledgeIngestTaskDTO;
import com.docbase.domain.knowledge.ingest.query.KnowledgeIngestTaskQuery;
import com.docbase.infrastructure.client.python.KbMappingProperties;
import com.docbase.infrastructure.client.python.PythonAiClient;
import com.docbase.infrastructure.client.python.dto.PythonDocumentResponse;
import com.docbase.infrastructure.client.python.dto.PythonDocumentUploadResponse;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIngestTaskApplicationService {

    public static final int TASK_TYPE_IMPORT = 1;
    public static final int TASK_TYPE_REIMPORT = 2;
    public static final int TASK_TYPE_RETRY = 3;
    public static final int TASK_TYPE_DELETE = 4;

    public static final int STATUS_PENDING = 1;
    public static final int STATUS_PROCESSING = 2;
    public static final int STATUS_SUCCESS = 3;
    public static final int STATUS_FAILED = 4;

    private final KnowledgeIngestTaskService knowledgeIngestTaskService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final KnowledgeDocumentVersionService knowledgeDocumentVersionService;
    private final PythonAiClient pythonAiClient;
    private final KbMappingProperties kbMappingProperties;

    public PageDTO<KnowledgeIngestTaskDTO> getTaskList(KnowledgeIngestTaskQuery query) {
        Page<KnowledgeIngestTaskEntity> page = knowledgeIngestTaskService.page(query.toPage(), query.toQueryWrapper());
        List<KnowledgeIngestTaskDTO> records = page.getRecords().stream()
            .map(KnowledgeIngestTaskDTO::new)
            .collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeIngestTaskDTO submitImportTask(Long documentId) {
        KnowledgeDocumentEntity document = knowledgeDocumentService.getById(documentId);
        if (document == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, documentId, "文档");
        }
        if (!Objects.equals(document.getStatus(), KnowledgeDocumentConstant.Status.PUBLISHED)) {
            throw new ApiException(ErrorCode.Internal.INVALID_PARAMETER,
                    "仅已发布的文档可以创建导入任务，当前状态：" + document.getStatus());
        }

        Integer kbId = resolveKbId(document);
        pythonAiClient.checkKnowledgeBase(kbId);

        KnowledgeDocumentVersionEntity currentVersion = knowledgeDocumentVersionService.getOne(
                new LambdaQueryWrapper<KnowledgeDocumentVersionEntity>()
                        .eq(KnowledgeDocumentVersionEntity::getDocumentId, documentId)
                        .eq(KnowledgeDocumentVersionEntity::getIsCurrent, Boolean.TRUE)
                        .last("limit 1"));
        if (currentVersion == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, documentId, "文档当前版本");
        }

        boolean hasPendingTask = knowledgeIngestTaskService.count(
                new LambdaQueryWrapper<KnowledgeIngestTaskEntity>()
                        .eq(KnowledgeIngestTaskEntity::getDocumentId, documentId)
                        .eq(KnowledgeIngestTaskEntity::getVersionId, currentVersion.getVersionId())
                        .in(KnowledgeIngestTaskEntity::getStatus, STATUS_PENDING, STATUS_PROCESSING)) > 0;
        if (hasPendingTask) {
            throw new ApiException(ErrorCode.Internal.INVALID_PARAMETER,
                    "该文档版本已有进行中的导入任务，请等待完成或重试");
        }

        KnowledgeIngestTaskEntity task = new KnowledgeIngestTaskEntity();
        task.setTaskNo(generateTaskNo());
        task.setDocumentId(documentId);
        task.setVersionId(currentVersion.getVersionId());
        task.setTaskType(TASK_TYPE_IMPORT);
        task.setStatus(STATUS_PENDING);
        task.setRetryCount(0);
        task.setPythonKbId(kbId);
        knowledgeIngestTaskService.save(task);

        log.info("Import task created: taskId={}, taskNo={}, documentId={}, kbId={}",
                task.getTaskId(), task.getTaskNo(), documentId, kbId);
        return new KnowledgeIngestTaskDTO(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeIngestTaskDTO retryTask(Long taskId) {
        KnowledgeIngestTaskEntity task = knowledgeIngestTaskService.getById(taskId);
        if (task == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, taskId, "导入任务");
        }
        if (task.getStatus() != STATUS_FAILED) {
            throw new ApiException(ErrorCode.Internal.INVALID_PARAMETER, "仅失败的任务可以重试");
        }

        task.setStatus(STATUS_PENDING);
        task.setRetryCount((task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1);
        task.setErrorMessage(null);
        task.setTaskType(TASK_TYPE_RETRY);
        knowledgeIngestTaskService.updateById(task);

        log.info("Import task reset for retry: taskId={}, retryCount={}", taskId, task.getRetryCount());
        return new KnowledgeIngestTaskDTO(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeIngestTaskDTO processTask(Long taskId) {
        KnowledgeIngestTaskEntity task = knowledgeIngestTaskService.getById(taskId);
        if (task == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, taskId, "导入任务");
        }
        if (task.getStatus() != STATUS_PENDING) {
            throw new ApiException(ErrorCode.Internal.INVALID_PARAMETER,
                    "仅待处理的任务可以执行，当前状态：" + task.getStatus());
        }
        return doProcessTask(task);
    }

    public int processPendingTasks() {
        List<KnowledgeIngestTaskEntity> pendingTasks = knowledgeIngestTaskService.list(
                new LambdaQueryWrapper<KnowledgeIngestTaskEntity>()
                        .eq(KnowledgeIngestTaskEntity::getStatus, STATUS_PENDING)
                        .orderByAsc(KnowledgeIngestTaskEntity::getCreateTime)
                        .last("limit 20"));
        int processed = 0;
        for (KnowledgeIngestTaskEntity task : pendingTasks) {
            try {
                doProcessTask(task);
                processed++;
            } catch (Exception e) {
                log.error("Failed to process pending task: taskId={}", task.getTaskId(), e);
            }
        }
        return processed;
    }

    private KnowledgeIngestTaskDTO doProcessTask(KnowledgeIngestTaskEntity task) {
        task.setStatus(STATUS_PROCESSING);
        task.setStartedTime(new Date());
        knowledgeIngestTaskService.updateById(task);

        try {
            KnowledgeDocumentVersionEntity version = knowledgeDocumentVersionService.getById(task.getVersionId());
            if (version == null) {
                throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND,
                        task.getVersionId(), "文档版本");
            }

            String storedFileName = FileNameUtil.getName(version.getStoragePath());
            String filePath = FileUploadUtils.getFileAbsolutePath(UploadSubDir.DOCUMENT_PATH, storedFileName);
            if (!FileUtil.exist(filePath)) {
                throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND,
                        storedFileName, "文档文件");
            }

            byte[] fileBytes = FileUtil.readBytes(filePath);
            Integer kbId = task.getPythonKbId();
            if (kbId == null) {
                KnowledgeDocumentEntity document = knowledgeDocumentService.getById(task.getDocumentId());
                kbId = resolveKbId(document);
                task.setPythonKbId(kbId);
            }

            String filename = version.getFileName() != null ? version.getFileName() : storedFileName;
            log.info("Uploading document to Python RAG: taskId={}, kbId={}, filename={}",
                    task.getTaskId(), kbId, filename);

            String sourceRef = "java_doc:" + task.getDocumentId();
            PythonDocumentUploadResponse response = pythonAiClient.uploadDocument(
                    kbId, fileBytes, filename, sourceRef);

            if (response.getData() != null && response.getData().getDocIds() != null
                    && !response.getData().getDocIds().isEmpty()) {
                Integer pythonDocId = response.getData().getDocIds().get(0);
                task.setPythonDocId(pythonDocId);
                // Python only creates the document record here; chunking/vectorization
                // continues asynchronously and must be confirmed via pollTaskStatus.
                task.setStatus(STATUS_PROCESSING);
                task.setFinishedTime(null);
                task.setChunkCount(null);
                knowledgeIngestTaskService.updateById(task);
                log.info("Import task submitted to Python for async processing: taskId={}, pythonDocId={}",
                        task.getTaskId(), pythonDocId);
            } else {
                task.setStatus(STATUS_PROCESSING);
                task.setFinishedTime(null);
                knowledgeIngestTaskService.updateById(task);
                log.info("Import task submitted to Python, awaiting async processing: taskId={}",
                        task.getTaskId());
            }
        } catch (Exception e) {
            task.setStatus(STATUS_FAILED);
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (errMsg.length() > 500) {
                errMsg = errMsg.substring(0, 500);
            }
            task.setErrorMessage(errMsg);
            task.setFinishedTime(new Date());
            knowledgeIngestTaskService.updateById(task);
            log.error("Import task failed: taskId={}", task.getTaskId(), e);
        }

        return new KnowledgeIngestTaskDTO(task);
    }

    public KnowledgeIngestTaskDTO pollTaskStatus(Long taskId) {
        KnowledgeIngestTaskEntity task = knowledgeIngestTaskService.getById(taskId);
        if (task == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, taskId, "导入任务");
        }
        if (task.getStatus() != STATUS_PROCESSING || task.getPythonDocId() == null) {
            return new KnowledgeIngestTaskDTO(task);
        }

        try {
            PythonDocumentResponse response = pythonAiClient.getDocumentStatus(task.getPythonDocId());
            if (response.getData() != null) {
                String pyStatus = response.getData().getStatus();
                if ("completed".equalsIgnoreCase(pyStatus)) {
                    task.setStatus(STATUS_SUCCESS);
                    task.setChunkCount(response.getData().getChunkCount());
                    task.setFinishedTime(new Date());
                    knowledgeIngestTaskService.updateById(task);
                } else if ("failed".equalsIgnoreCase(pyStatus)) {
                    task.setStatus(STATUS_FAILED);
                    task.setErrorMessage(response.getData().getErrorMsg());
                    task.setFinishedTime(new Date());
                    knowledgeIngestTaskService.updateById(task);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to poll Python document status: taskId={}, pythonDocId={}",
                    taskId, task.getPythonDocId(), e);
        }
        return new KnowledgeIngestTaskDTO(task);
    }

    private Integer resolveKbId(KnowledgeDocumentEntity document) {
        if (document != null && document.getCategoryId() != null) {
            Integer mapped = kbMappingProperties.getCategoryMappings().get(document.getCategoryId());
            if (mapped != null) {
                return mapped;
            }
        }
        return kbMappingProperties.getDefaultKbId();
    }

    private String generateTaskNo() {
        return "INGEST-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
