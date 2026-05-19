package com.docbase.admin.controller.knowledge;

import com.docbase.admin.customize.aop.accessLog.AccessLog;
import com.docbase.admin.customize.service.permission.KnowledgePermissionHelper;
import com.docbase.common.core.base.BaseController;
import com.docbase.common.core.dto.ResponseDTO;
import com.docbase.common.core.page.PageDTO;
import com.docbase.common.enums.common.BusinessTypeEnum;
import com.docbase.domain.knowledge.document.KnowledgeDocumentApplicationService;
import com.docbase.domain.knowledge.document.command.KnowledgeDocumentAddCommand;
import com.docbase.domain.knowledge.document.dto.KnowledgeDocumentDTO;
import com.docbase.domain.knowledge.document.dto.KnowledgeDocumentDetailDTO;
import com.docbase.domain.knowledge.document.query.KnowledgeDocumentQuery;
import com.docbase.infrastructure.user.AuthenticationUtils;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "知识库文档 API", description = "知识库文档管理接口")
@RestController
@RequestMapping("/knowledge/documents")
@RequiredArgsConstructor
public class KnowledgeDocumentController extends BaseController {

    private final KnowledgeDocumentApplicationService knowledgeDocumentApplicationService;
    private final KnowledgePermissionHelper knowledgePermissionHelper;

    @Operation(summary = "文档列表")
    @PreAuthorize("@permission.has('knowledge:document:list')")
    @GetMapping
    public ResponseDTO<PageDTO<KnowledgeDocumentDTO>> list(KnowledgeDocumentQuery query) {
        restrictToCurrentUserIfNeeded(query);
        return ResponseDTO.ok(knowledgeDocumentApplicationService.getDocumentList(query));
    }

    @Operation(summary = "文档详情")
    @PreAuthorize("@permission.has('knowledge:document:list')")
    @GetMapping("/{documentId}")
    public ResponseDTO<KnowledgeDocumentDetailDTO> detail(@PathVariable Long documentId) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        return ResponseDTO.ok(
            knowledgeDocumentApplicationService.getDocumentDetail(
                documentId,
                loginUser.getUserId(),
                loginUser.getDeptId(),
                loginUser.isAdmin()
            )
        );
    }

    @Operation(summary = "文档预览地址")
    @PreAuthorize("@permission.has('knowledge:document:preview')")
    @GetMapping("/{documentId}/preview")
    public ResponseDTO<String> preview(@PathVariable Long documentId) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        return ResponseDTO.ok(
            knowledgeDocumentApplicationService.getPreviewUrl(
                documentId,
                loginUser.getUserId(),
                loginUser.getDeptId(),
                loginUser.isAdmin()
            )
        );
    }

    @Operation(summary = "下载当前文档")
    @PreAuthorize("@permission.has('knowledge:document:download')")
    @AccessLog(title = "知识库文档", businessType = BusinessTypeEnum.EXPORT)
    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long documentId) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        return knowledgeDocumentApplicationService.downloadCurrentDocument(
            documentId,
            loginUser.getUserId(),
            loginUser.getDeptId(),
            loginUser.isAdmin()
        );
    }

    @Operation(summary = "新增文档")
    @PreAuthorize("@permission.has('knowledge:document:upload')")
    @AccessLog(title = "知识库文档", businessType = BusinessTypeEnum.ADD)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseDTO<Void> add(@Valid KnowledgeDocumentAddCommand addCommand,
                                 @RequestParam("file") MultipartFile file) {
        knowledgeDocumentApplicationService.addDocument(addCommand, file);
        return ResponseDTO.ok();
    }

    private void restrictToCurrentUserIfNeeded(KnowledgeDocumentQuery query) {
        knowledgePermissionHelper.applyDocumentQueryScope(query);
    }
}
