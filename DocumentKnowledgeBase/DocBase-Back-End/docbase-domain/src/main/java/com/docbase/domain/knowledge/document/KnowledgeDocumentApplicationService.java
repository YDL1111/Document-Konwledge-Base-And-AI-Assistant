package com.docbase.domain.knowledge.document;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.docbase.common.constant.Constants.UploadSubDir;
import com.docbase.common.core.page.PageDTO;
import com.docbase.common.exception.ApiException;
import com.docbase.common.exception.error.ErrorCode;
import com.docbase.common.utils.ServletHolderUtil;
import com.docbase.common.utils.file.FileUploadUtils;
import com.docbase.domain.knowledge.category.db.KnowledgeCategoryEntity;
import com.docbase.domain.knowledge.category.db.KnowledgeCategoryService;
import com.docbase.domain.knowledge.document.command.KnowledgeDocumentAddCommand;
import com.docbase.domain.knowledge.document.command.KnowledgeDocumentAuditCommand;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentAuditLogEntity;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentAuditLogService;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentEntity;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentService;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentVersionEntity;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentVersionService;
import com.docbase.domain.knowledge.document.dto.KnowledgeDocumentAuditLogDTO;
import com.docbase.domain.knowledge.document.dto.KnowledgeDocumentDTO;
import com.docbase.domain.knowledge.document.dto.KnowledgeDocumentDetailDTO;
import com.docbase.domain.knowledge.document.dto.KnowledgeDocumentVersionDTO;
import com.docbase.domain.knowledge.document.query.KnowledgeDocumentQuery;
import com.docbase.domain.knowledge.ingest.KnowledgeIngestTaskApplicationService;
import com.docbase.domain.knowledge.ingest.db.KnowledgeIngestTaskEntity;
import com.docbase.domain.knowledge.ingest.db.KnowledgeIngestTaskService;
import com.docbase.domain.system.dept.db.SysDeptEntity;
import com.docbase.domain.system.dept.db.SysDeptService;
import com.docbase.domain.system.notice.NoticeApplicationService;
import com.docbase.domain.system.notice.command.NoticeAddCommand;
import com.docbase.domain.system.user.db.SysUserEntity;
import com.docbase.domain.system.user.db.SysUserService;
import com.docbase.infrastructure.user.AuthenticationUtils;
import com.docbase.infrastructure.user.web.DataScopeEnum;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeDocumentApplicationService {

    private final KnowledgeDocumentService knowledgeDocumentService;
    private final KnowledgeDocumentVersionService knowledgeDocumentVersionService;
    private final KnowledgeDocumentAuditLogService knowledgeDocumentAuditLogService;
    private final KnowledgeCategoryService knowledgeCategoryService;
    private final SysUserService sysUserService;
    private final SysDeptService sysDeptService;
    private final NoticeApplicationService noticeApplicationService;
    @Lazy
    private final KnowledgeIngestTaskApplicationService knowledgeIngestTaskApplicationService;
    private final KnowledgeIngestTaskService knowledgeIngestTaskService;

    public PageDTO<KnowledgeDocumentDTO> getDocumentList(KnowledgeDocumentQuery query) {
        Page<KnowledgeDocumentEntity> page =
            knowledgeDocumentService.page(query.toPage(), query.toQueryWrapper());
        List<KnowledgeDocumentDTO> records = page.getRecords().stream()
            .map(KnowledgeDocumentDTO::new)
            .collect(Collectors.toList());

        // 检查每个文档的当前版本是否已成功导入AI知识库
        if (!records.isEmpty()) {
            List<Long> documentIds = records.stream()
                    .map(KnowledgeDocumentDTO::getDocumentId).collect(Collectors.toList());
            java.util.Map<Long, Long> docVersionMap = knowledgeDocumentService.listByIds(documentIds).stream()
                    .collect(Collectors.toMap(
                            KnowledgeDocumentEntity::getDocumentId,
                            doc -> doc.getCurrentVersionId() != null ? doc.getCurrentVersionId() : 0L));
            java.util.Set<Long> importedPairs = knowledgeIngestTaskService.lambdaQuery()
                    .eq(KnowledgeIngestTaskEntity::getStatus, KnowledgeIngestTaskApplicationService.STATUS_SUCCESS)
                    .in(KnowledgeIngestTaskEntity::getDocumentId, documentIds)
                    .list().stream()
                    .filter(t -> t.getVersionId() != null
                            && t.getVersionId().equals(docVersionMap.get(t.getDocumentId())))
                    .map(KnowledgeIngestTaskEntity::getDocumentId)
                    .collect(Collectors.toSet());
            records.forEach(dto -> dto.setHasAiImport(importedPairs.contains(dto.getDocumentId())));
        }

        return new PageDTO<>(records, page.getTotal());
    }

    public KnowledgeDocumentDetailDTO getDocumentDetail(Long documentId,
                                                        Long currentUserId,
                                                        Long currentDeptId,
                                                        boolean isAdmin) {
        KnowledgeDocumentEntity entity = getAccessibleDocument(documentId, currentUserId, currentDeptId, isAdmin);

        KnowledgeDocumentDetailDTO detailDTO = new KnowledgeDocumentDetailDTO();
        KnowledgeDocumentDTO baseDTO = new KnowledgeDocumentDTO(entity);
        detailDTO.setDocumentId(baseDTO.getDocumentId());
        detailDTO.setCategoryId(baseDTO.getCategoryId());
        detailDTO.setDeptId(baseDTO.getDeptId());
        detailDTO.setTitle(baseDTO.getTitle());
        detailDTO.setDocCode(baseDTO.getDocCode());
        detailDTO.setSummary(baseDTO.getSummary());
        detailDTO.setTags(baseDTO.getTags());
        detailDTO.setVisibility(baseDTO.getVisibility());
        detailDTO.setStatus(baseDTO.getStatus());
        detailDTO.setCurrentVersionNo(baseDTO.getCurrentVersionNo());
        detailDTO.setAuditRemark(baseDTO.getAuditRemark());
        detailDTO.setUpdateTime(baseDTO.getUpdateTime());

        KnowledgeCategoryEntity category = entity.getCategoryId() == null
            ? null
            : knowledgeCategoryService.getById(entity.getCategoryId());
        SysUserEntity creator = entity.getCreatorId() == null
            ? null
            : sysUserService.getById(entity.getCreatorId());
        SysDeptEntity dept = entity.getDeptId() == null
            ? null
            : sysDeptService.getById(entity.getDeptId());

        detailDTO.setCategoryName(category != null ? category.getCategoryName() : null);
        detailDTO.setCreatorName(creator != null ? creator.getNickname() : null);
        detailDTO.setDeptName(dept != null ? dept.getDeptName() : null);

        List<KnowledgeDocumentVersionDTO> versionList;
        try {
            versionList = knowledgeDocumentVersionService.list(
                    new LambdaQueryWrapper<KnowledgeDocumentVersionEntity>()
                        .eq(KnowledgeDocumentVersionEntity::getDocumentId, documentId)
                        .orderByDesc(KnowledgeDocumentVersionEntity::getCreateTime)
                ).stream()
                .map(KnowledgeDocumentVersionDTO::new)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("加载文档版本列表失败，降级为空列表。documentId={}", documentId, e);
            versionList = java.util.Collections.emptyList();
        }
        detailDTO.setVersionList(versionList);

        KnowledgeDocumentVersionDTO currentVersion = versionList.stream()
            .filter(item -> Boolean.TRUE.equals(item.getIsCurrent()))
            .findFirst()
            .orElse(null);
        detailDTO.setCurrentVersion(currentVersion);
        if (currentVersion != null) {
            detailDTO.setCurrentVersionStorageUrl(currentVersion.getStorageUrl());
            detailDTO.setCurrentVersionStoragePath(currentVersion.getStoragePath());
        }

        List<KnowledgeDocumentAuditLogDTO> auditHistoryList;
        try {
            auditHistoryList = knowledgeDocumentAuditLogService.list(
                    new LambdaQueryWrapper<KnowledgeDocumentAuditLogEntity>()
                        .eq(KnowledgeDocumentAuditLogEntity::getDocumentId, documentId)
                        .orderByDesc(KnowledgeDocumentAuditLogEntity::getCreateTime)
                ).stream()
                .map(KnowledgeDocumentAuditLogDTO::new)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("加载文档审核历史失败，降级为空列表。documentId={}", documentId, e);
            auditHistoryList = java.util.Collections.emptyList();
        }
        detailDTO.setAuditHistoryList(auditHistoryList);

        return detailDTO;
    }

    public String getPreviewUrl(Long documentId, Long currentUserId, Long currentDeptId, boolean isAdmin) {
        KnowledgeDocumentVersionEntity currentVersion =
            getCurrentVersionEntity(documentId, currentUserId, currentDeptId, isAdmin);
        return currentVersion.getStorageUrl();
    }

    public ResponseEntity<byte[]> downloadCurrentDocument(Long documentId,
                                                          Long currentUserId,
                                                          Long currentDeptId,
                                                          boolean isAdmin) {
        KnowledgeDocumentVersionEntity currentVersion =
            getCurrentVersionEntity(documentId, currentUserId, currentDeptId, isAdmin);

        String storedFileName = FileNameUtil.getName(currentVersion.getStoragePath());
        if (!FileUploadUtils.isAllowDownload(storedFileName)) {
            throw new ApiException(ErrorCode.Business.COMMON_FILE_NOT_ALLOWED_TO_DOWNLOAD, storedFileName);
        }

        String filePath = FileUploadUtils.getFileAbsolutePath(UploadSubDir.DOCUMENT_PATH, storedFileName);
        if (!FileUtil.exist(filePath)) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, storedFileName, "文档文件");
        }

        HttpHeaders headers = FileUploadUtils.getDownloadHeader(
            Objects.requireNonNullElse(currentVersion.getFileName(), storedFileName)
        );
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(FileUtil.readBytes(new File(filePath)), headers, HttpStatus.OK);
    }

    private KnowledgeDocumentEntity getAccessibleDocument(Long documentId,
                                                          Long currentUserId,
                                                          Long currentDeptId,
                                                          boolean isAdmin) {
        KnowledgeDocumentEntity entity = knowledgeDocumentService.getById(documentId);
        if (entity == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, documentId, "文档");
        }
        if (!isAdmin && !canCurrentUserView(entity, currentUserId, currentDeptId)) {
            throw new ApiException(ErrorCode.Business.PERMISSION_NOT_ALLOWED_TO_OPERATE);
        }
        return entity;
    }

    private KnowledgeDocumentVersionEntity getCurrentVersionEntity(Long documentId,
                                                                   Long currentUserId,
                                                                   Long currentDeptId,
                                                                   boolean isAdmin) {
        getAccessibleDocument(documentId, currentUserId, currentDeptId, isAdmin);
        KnowledgeDocumentVersionEntity currentVersion = knowledgeDocumentVersionService.getOne(
            new LambdaQueryWrapper<KnowledgeDocumentVersionEntity>()
                .eq(KnowledgeDocumentVersionEntity::getDocumentId, documentId)
                .eq(KnowledgeDocumentVersionEntity::getIsCurrent, Boolean.TRUE)
                .last("limit 1")
        );
        if (currentVersion == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, documentId, "文档当前版本");
        }
        return currentVersion;
    }

    private boolean canCurrentUserView(KnowledgeDocumentEntity entity, Long currentUserId, Long currentDeptId) {
        if (Objects.equals(entity.getCreatorId(), currentUserId)) {
            return true;
        }
        if (!Objects.equals(entity.getStatus(), KnowledgeDocumentConstant.Status.PUBLISHED)) {
            log.debug("文档不可见：状态非已发布 docId={} status={} visibility={} currentUserId={}",
                entity.getDocumentId(), entity.getStatus(), entity.getVisibility(), currentUserId);
            return false;
        }
        if (Objects.equals(entity.getVisibility(), KnowledgeDocumentConstant.Visibility.PUBLIC)) {
            return true;
        }
        if (Objects.equals(entity.getVisibility(), KnowledgeDocumentConstant.Visibility.DEPT)) {
            if (entity.getDeptId() == null) {
                log.debug("文档不可见：部门可见但deptId为空 docId={}", entity.getDocumentId());
                return false;
            }
            if (Objects.equals(entity.getDeptId(), currentDeptId)) {
                return true;
            }
            // DEPT_TREE 数据范围：允许查看子部门的文档
            DataScopeEnum scope = AuthenticationUtils.getSystemLoginUser().getRoleInfo().getDataScope();
            if (scope == DataScopeEnum.DEPT_TREE) {
                return sysDeptService.isChildOfTheDept(currentDeptId, entity.getDeptId());
            }
            log.debug("文档不可见：部门不匹配 docId={} entityDept={} userDept={} scope={}",
                entity.getDocumentId(), entity.getDeptId(), currentDeptId, scope);
            return false;
        }
        log.debug("文档不可见：visibility不匹配 docId={} visibility={} creatorId={}",
            entity.getDocumentId(), entity.getVisibility(), entity.getCreatorId());
        return false;
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditDocument(Long documentId, KnowledgeDocumentAuditCommand command) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        KnowledgeDocumentEntity entity = getAccessibleDocument(
            documentId,
            loginUser.getUserId(),
            loginUser.getDeptId(),
            loginUser.isAdmin()
        );
        if (!loginUser.isAdmin() && !canAccessTargetDept(entity.getDeptId())) {
            throw new ApiException(ErrorCode.Business.PERMISSION_NOT_ALLOWED_TO_OPERATE);
        }
        if (!Objects.equals(entity.getStatus(), KnowledgeDocumentConstant.Status.PENDING_AUDIT)) {
            throw new ApiException(ErrorCode.Internal.INVALID_PARAMETER, "当前文档状态不允许审核");
        }

        int beforeStatus = entity.getStatus();
        boolean approved = Objects.equals(command.getApproved(), 1);
        entity.setStatus(approved
            ? KnowledgeDocumentConstant.Status.PUBLISHED
            : KnowledgeDocumentConstant.Status.REJECTED);
        entity.setAuditRemark(command.getAuditRemark());
        knowledgeDocumentService.updateById(entity);

        KnowledgeDocumentAuditLogEntity auditLogEntity = new KnowledgeDocumentAuditLogEntity();
        auditLogEntity.setDocumentId(documentId);
        auditLogEntity.setAuditResult(command.getApproved());
        auditLogEntity.setAuditRemark(command.getAuditRemark());
        auditLogEntity.setAuditorId(loginUser.getUserId());
        auditLogEntity.setAuditorName(loginUser.getUsername());
        auditLogEntity.setBeforeStatus(beforeStatus);
        auditLogEntity.setAfterStatus(entity.getStatus());
        knowledgeDocumentAuditLogService.save(auditLogEntity);

        // 审核完成后通知文档创建人
        publishAuditNotice(entity, approved);

        // 审核通过后自动创建导入任务，同步到Python RAG
        if (approved) {
            try {
                knowledgeIngestTaskApplicationService.submitImportTask(documentId);
                log.info("Auto-created import task for approved document: documentId={}", documentId);
            } catch (Exception e) {
                log.warn("审核通过后创建导入任务失败，主流程不受影响。documentId={}", documentId, e);
            }
        }
    }

    private void publishAuditNotice(KnowledgeDocumentEntity entity, boolean approved) {
        try {
            NoticeAddCommand noticeCommand = new NoticeAddCommand();
            noticeCommand.setNoticeTitle("文档审核结果通知");
            noticeCommand.setNoticeType("1");
            String resultText = approved ? "审批通过" : "审批驳回";
            String remarkText = entity.getAuditRemark() != null && !entity.getAuditRemark().isEmpty()
                ? "。审核备注：" + entity.getAuditRemark()
                : "";
            noticeCommand.setNoticeContent(
                "您的文档「" + entity.getTitle() + "」已被" + resultText + "。" + remarkText);
            noticeCommand.setStatus("1");
            noticeApplicationService.addNotice(noticeCommand);
        } catch (Exception e) {
            log.warn("审核通知创建失败，已降级跳过。documentId={}", entity.getDocumentId(), e);
        }
    }

    /**
     * 基于当前用户的数据范围（DataScopeEnum）判断是否有权操作目标部门。
     */
    private boolean canAccessTargetDept(Long targetDeptId) {
        if (targetDeptId == null) {
            return false;
        }
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        DataScopeEnum scope = loginUser.getRoleInfo().getDataScope();
        switch (scope) {
            case ALL:
                return true;
            case CUSTOM_DEFINE:
                java.util.Set<Long> deptIdSet = loginUser.getRoleInfo().getDeptIdSet();
                return deptIdSet != null && deptIdSet.contains(targetDeptId);
            case SINGLE_DEPT:
                return Objects.equals(targetDeptId, loginUser.getDeptId());
            case DEPT_TREE:
                return Objects.equals(targetDeptId, loginUser.getDeptId())
                    || sysDeptService.isChildOfTheDept(loginUser.getDeptId(), targetDeptId);
            case ONLY_SELF:
            default:
                return false;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void addDocument(KnowledgeDocumentAddCommand command, MultipartFile file) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        String versionNo = "v1.0.0";
        String storedPath = FileUploadUtils.upload(UploadSubDir.DOCUMENT_PATH, file);

        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setCategoryId(command.getCategoryId());
        entity.setDeptId(loginUser.getDeptId());
        entity.setTitle(command.getTitle());
        entity.setDocCode(command.getDocCode());
        entity.setSummary(command.getSummary());
        entity.setTags(command.getTags());
        entity.setVisibility(command.getVisibility());
        entity.setStatus(KnowledgeDocumentConstant.Status.PENDING_AUDIT);
        entity.setCurrentVersionNo(versionNo);
        knowledgeDocumentService.save(entity);

        KnowledgeDocumentVersionEntity versionEntity = new KnowledgeDocumentVersionEntity();
        versionEntity.setDocumentId(entity.getDocumentId());
        versionEntity.setVersionNo(versionNo);
        versionEntity.setFileName(file.getOriginalFilename());
        versionEntity.setFileExt(FileNameUtil.extName(file.getOriginalFilename()));
        versionEntity.setFileSize(file.getSize());
        versionEntity.setStorageType("local");
        versionEntity.setStoragePath(storedPath);
        versionEntity.setStorageUrl(ServletHolderUtil.getContextUrl() + storedPath);
        versionEntity.setVersionRemark("首次上传");
        versionEntity.setParseStatus(1);
        versionEntity.setIsCurrent(Boolean.TRUE);
        knowledgeDocumentVersionService.save(versionEntity);

        entity.setCurrentVersionId(versionEntity.getVersionId());
        knowledgeDocumentService.updateById(entity);
    }
}
