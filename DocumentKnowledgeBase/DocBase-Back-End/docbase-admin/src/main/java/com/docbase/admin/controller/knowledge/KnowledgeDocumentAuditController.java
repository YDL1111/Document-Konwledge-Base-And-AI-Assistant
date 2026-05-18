package com.docbase.admin.controller.knowledge;

import com.docbase.admin.customize.aop.accessLog.AccessLog;
import com.docbase.common.core.base.BaseController;
import com.docbase.common.core.dto.ResponseDTO;
import com.docbase.common.enums.common.BusinessTypeEnum;
import com.docbase.domain.knowledge.document.KnowledgeDocumentApplicationService;
import com.docbase.domain.knowledge.document.command.KnowledgeDocumentAuditCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识库文档审核 API", description = "知识库文档审核相关接口")
@RestController
@RequestMapping("/knowledge/documents")
@RequiredArgsConstructor
public class KnowledgeDocumentAuditController extends BaseController {

    private final KnowledgeDocumentApplicationService knowledgeDocumentApplicationService;

    @Operation(summary = "审核文档")
    @PreAuthorize("@permission.has('knowledge:document:audit')")
    @AccessLog(title = "知识库文档审核", businessType = BusinessTypeEnum.MODIFY)
    @PutMapping("/{documentId}/audit")
    public ResponseDTO<Void> audit(@PathVariable Long documentId,
                                   @Valid @RequestBody KnowledgeDocumentAuditCommand command) {
        knowledgeDocumentApplicationService.auditDocument(documentId, command);
        return ResponseDTO.ok();
    }
}
