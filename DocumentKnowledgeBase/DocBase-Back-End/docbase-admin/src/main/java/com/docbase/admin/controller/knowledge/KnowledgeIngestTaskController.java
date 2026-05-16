package com.docbase.admin.controller.knowledge;

import com.docbase.common.core.base.BaseController;
import com.docbase.common.core.dto.ResponseDTO;
import com.docbase.common.core.page.PageDTO;
import com.docbase.domain.knowledge.ingest.KnowledgeIngestTaskApplicationService;
import com.docbase.domain.knowledge.ingest.dto.KnowledgeIngestTaskDTO;
import com.docbase.domain.knowledge.ingest.query.KnowledgeIngestTaskQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识库入库任务API", description = "知识库入库任务相关接口")
@RestController
@RequestMapping("/knowledge/ingest/tasks")
@RequiredArgsConstructor
public class KnowledgeIngestTaskController extends BaseController {

    private final KnowledgeIngestTaskApplicationService knowledgeIngestTaskApplicationService;

    @Operation(summary = "入库任务列表")
    @PreAuthorize("@permission.has('knowledge:ingest:list')")
    @GetMapping
    public ResponseDTO<PageDTO<KnowledgeIngestTaskDTO>> list(KnowledgeIngestTaskQuery query) {
        return ResponseDTO.ok(knowledgeIngestTaskApplicationService.getTaskList(query));
    }
}
