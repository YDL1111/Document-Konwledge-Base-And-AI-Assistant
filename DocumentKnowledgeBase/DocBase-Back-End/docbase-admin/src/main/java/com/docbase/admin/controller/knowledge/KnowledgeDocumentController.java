package com.docbase.admin.controller.knowledge;

import com.docbase.common.core.base.BaseController;
import com.docbase.common.core.dto.ResponseDTO;
import com.docbase.common.core.page.PageDTO;
import com.docbase.domain.knowledge.document.KnowledgeDocumentApplicationService;
import com.docbase.domain.knowledge.document.dto.KnowledgeDocumentDTO;
import com.docbase.domain.knowledge.document.query.KnowledgeDocumentQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识库文档API", description = "知识库文档相关接口")
@RestController
@RequestMapping("/knowledge/documents")
@RequiredArgsConstructor
public class KnowledgeDocumentController extends BaseController {

    private final KnowledgeDocumentApplicationService knowledgeDocumentApplicationService;

    @Operation(summary = "文档列表")
    @PreAuthorize("@permission.has('knowledge:document:list')")
    @GetMapping
    public ResponseDTO<PageDTO<KnowledgeDocumentDTO>> list(KnowledgeDocumentQuery query) {
        return ResponseDTO.ok(knowledgeDocumentApplicationService.getDocumentList(query));
    }
}
