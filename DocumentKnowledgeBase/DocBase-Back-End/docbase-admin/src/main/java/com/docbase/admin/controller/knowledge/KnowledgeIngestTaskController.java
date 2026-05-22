package com.docbase.admin.controller.knowledge;

import com.docbase.admin.customize.service.permission.KnowledgePermissionHelper;
import com.docbase.common.core.base.BaseController;
import com.docbase.common.core.dto.ResponseDTO;
import com.docbase.common.core.page.PageDTO;
import com.docbase.domain.knowledge.ingest.KnowledgeIngestTaskApplicationService;
import com.docbase.domain.knowledge.ingest.dto.KnowledgeIngestTaskDTO;
import com.docbase.domain.knowledge.ingest.query.KnowledgeIngestTaskQuery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识库入库任务API", description = "知识库入库任务相关接口")
@RestController
@RequestMapping("/knowledge/ingest/tasks")
@RequiredArgsConstructor
public class KnowledgeIngestTaskController extends BaseController {

    private final KnowledgeIngestTaskApplicationService knowledgeIngestTaskApplicationService;
    private final KnowledgePermissionHelper knowledgePermissionHelper;

    @Operation(summary = "入库任务列表")
    @PreAuthorize("@permission.has('knowledge:ingest:list')")
    @GetMapping
    public ResponseDTO<PageDTO<KnowledgeIngestTaskDTO>> list(KnowledgeIngestTaskQuery query) {
        restrictToCurrentUserIfNeeded(query);
        return ResponseDTO.ok(knowledgeIngestTaskApplicationService.getTaskList(query));
    }

    @Operation(summary = "为文档创建导入任务")
    @PreAuthorize("@permission.has('knowledge:ingest:list')")
    @PostMapping("/submit/{documentId}")
    public ResponseDTO<KnowledgeIngestTaskDTO> submit(@PathVariable Long documentId) {
        KnowledgeIngestTaskDTO task = knowledgeIngestTaskApplicationService.submitImportTask(documentId);
        return ResponseDTO.ok(task);
    }

    @Operation(summary = "重试失败的导入任务")
    @PreAuthorize("@permission.has('knowledge:ingest:list')")
    @PostMapping("/{taskId}/retry")
    public ResponseDTO<KnowledgeIngestTaskDTO> retry(@PathVariable Long taskId) {
        KnowledgeIngestTaskDTO task = knowledgeIngestTaskApplicationService.retryTask(taskId);
        return ResponseDTO.ok(task);
    }

    @Operation(summary = "执行指定导入任务")
    @PreAuthorize("@permission.has('knowledge:ingest:list')")
    @PostMapping("/{taskId}/process")
    public ResponseDTO<KnowledgeIngestTaskDTO> process(@PathVariable Long taskId) {
        KnowledgeIngestTaskDTO task = knowledgeIngestTaskApplicationService.processTask(taskId);
        return ResponseDTO.ok(task);
    }

    @Operation(summary = "查询任务在Python侧的处理状态")
    @PreAuthorize("@permission.has('knowledge:ingest:list')")
    @PostMapping("/{taskId}/poll")
    public ResponseDTO<KnowledgeIngestTaskDTO> pollStatus(@PathVariable Long taskId) {
        KnowledgeIngestTaskDTO task = knowledgeIngestTaskApplicationService.pollTaskStatus(taskId);
        return ResponseDTO.ok(task);
    }

    @Operation(summary = "批量处理待执行任务")
    @PreAuthorize("@permission.has('knowledge:ingest:list')")
    @PostMapping("/process-pending")
    public ResponseDTO<Map<String, Integer>> processPending() {
        int count = knowledgeIngestTaskApplicationService.processPendingTasks();
        return ResponseDTO.ok(Map.of("processed", count));
    }

    private void restrictToCurrentUserIfNeeded(KnowledgeIngestTaskQuery query) {
        knowledgePermissionHelper.applyIngestTaskQueryScope(query);
    }
}
