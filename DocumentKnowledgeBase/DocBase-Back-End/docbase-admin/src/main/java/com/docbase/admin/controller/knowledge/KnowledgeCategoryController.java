package com.docbase.admin.controller.knowledge;

import com.docbase.common.core.base.BaseController;
import com.docbase.common.core.dto.ResponseDTO;
import com.docbase.common.core.page.PageDTO;
import com.docbase.domain.knowledge.category.KnowledgeCategoryApplicationService;
import com.docbase.domain.knowledge.category.dto.KnowledgeCategoryDTO;
import com.docbase.domain.knowledge.category.query.KnowledgeCategoryQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识库分类API", description = "知识库分类相关接口")
@RestController
@RequestMapping("/knowledge/categories")
@RequiredArgsConstructor
public class KnowledgeCategoryController extends BaseController {

    private final KnowledgeCategoryApplicationService knowledgeCategoryApplicationService;

    @Operation(summary = "分类列表")
    @PreAuthorize("@permission.has('knowledge:category:list')")
    @GetMapping
    public ResponseDTO<PageDTO<KnowledgeCategoryDTO>> list(KnowledgeCategoryQuery query) {
        return ResponseDTO.ok(knowledgeCategoryApplicationService.getCategoryList(query));
    }
}
