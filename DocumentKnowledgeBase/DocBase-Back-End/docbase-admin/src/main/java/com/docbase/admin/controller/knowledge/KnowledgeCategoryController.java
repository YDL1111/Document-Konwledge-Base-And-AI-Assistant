package com.docbase.admin.controller.knowledge;

import com.docbase.admin.customize.aop.accessLog.AccessLog;
import com.docbase.common.core.base.BaseController;
import com.docbase.common.core.dto.ResponseDTO;
import com.docbase.common.core.page.PageDTO;
import com.docbase.common.enums.common.BusinessTypeEnum;
import com.docbase.domain.knowledge.category.KnowledgeCategoryApplicationService;
import com.docbase.domain.knowledge.category.command.KnowledgeCategoryAddCommand;
import com.docbase.domain.knowledge.category.command.KnowledgeCategoryUpdateCommand;
import com.docbase.domain.knowledge.category.dto.KnowledgeCategoryDTO;
import com.docbase.domain.knowledge.category.query.KnowledgeCategoryQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识库分类 API", description = "知识库分类相关接口")
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

    @Operation(summary = "分类详情")
    @PreAuthorize("@permission.has('knowledge:category:list')")
    @GetMapping("/{categoryId}")
    public ResponseDTO<KnowledgeCategoryDTO> detail(@PathVariable Long categoryId) {
        return ResponseDTO.ok(knowledgeCategoryApplicationService.getCategoryInfo(categoryId));
    }

    @Operation(summary = "新增分类")
    @PreAuthorize("@permission.has('knowledge:category:add')")
    @AccessLog(title = "知识库分类", businessType = BusinessTypeEnum.ADD)
    @PostMapping
    public ResponseDTO<Void> add(@Valid @RequestBody KnowledgeCategoryAddCommand command) {
        knowledgeCategoryApplicationService.addCategory(command);
        return ResponseDTO.ok();
    }

    @Operation(summary = "编辑分类")
    @PreAuthorize("@permission.has('knowledge:category:edit')")
    @AccessLog(title = "知识库分类", businessType = BusinessTypeEnum.MODIFY)
    @PutMapping("/{categoryId}")
    public ResponseDTO<Void> edit(@PathVariable Long categoryId,
                                  @Valid @RequestBody KnowledgeCategoryUpdateCommand command) {
        command.setCategoryId(categoryId);
        knowledgeCategoryApplicationService.updateCategory(command);
        return ResponseDTO.ok();
    }

    @Operation(summary = "删除分类")
    @PreAuthorize("@permission.has('knowledge:category:remove')")
    @AccessLog(title = "知识库分类", businessType = BusinessTypeEnum.DELETE)
    @DeleteMapping("/{categoryId}")
    public ResponseDTO<Void> remove(@PathVariable Long categoryId) {
        knowledgeCategoryApplicationService.deleteCategory(categoryId);
        return ResponseDTO.ok();
    }
}
