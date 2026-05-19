package com.docbase.domain.knowledge.category;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.docbase.common.core.page.PageDTO;
import com.docbase.common.exception.ApiException;
import com.docbase.common.exception.error.ErrorCode;
import com.docbase.domain.knowledge.category.command.KnowledgeCategoryAddCommand;
import com.docbase.domain.knowledge.category.command.KnowledgeCategoryUpdateCommand;
import com.docbase.domain.knowledge.category.db.KnowledgeCategoryEntity;
import com.docbase.domain.knowledge.category.db.KnowledgeCategoryService;
import com.docbase.domain.knowledge.category.dto.KnowledgeCategoryDTO;
import com.docbase.domain.knowledge.category.query.KnowledgeCategoryQuery;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentEntity;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentService;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeCategoryApplicationService {

    private final KnowledgeCategoryService knowledgeCategoryService;
    private final KnowledgeDocumentService knowledgeDocumentService;

    public PageDTO<KnowledgeCategoryDTO> getCategoryList(KnowledgeCategoryQuery query) {
        Page<KnowledgeCategoryEntity> page =
            knowledgeCategoryService.page(query.toPage(), query.toQueryWrapper());
        List<KnowledgeCategoryDTO> records = page.getRecords().stream()
            .map(KnowledgeCategoryDTO::new)
            .collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public KnowledgeCategoryDTO getCategoryInfo(Long categoryId, Long currentDeptId, boolean isAdmin) {
        KnowledgeCategoryEntity entity = knowledgeCategoryService.getById(categoryId);
        if (entity == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, categoryId, "分类");
        }
        ensureCategoryAccessible(entity, currentDeptId, isAdmin);
        return new KnowledgeCategoryDTO(entity);
    }

    public void addCategory(KnowledgeCategoryAddCommand command, Long currentDeptId, boolean isAdmin) {
        ensureParentCategoryAccessible(command.getParentId(), currentDeptId, isAdmin);

        KnowledgeCategoryEntity entity = new KnowledgeCategoryEntity();
        entity.setParentId(command.getParentId() == null ? 0L : command.getParentId());
        entity.setAncestors(buildAncestors(entity.getParentId()));
        entity.setCategoryName(command.getCategoryName());
        entity.setDeptId(resolveDeptId(command.getDeptId(), currentDeptId, isAdmin));
        entity.setSortNum(command.getSortNum());
        entity.setStatus(command.getStatus());
        entity.setRemark(command.getRemark());
        knowledgeCategoryService.save(entity);
    }

    public void updateCategory(KnowledgeCategoryUpdateCommand command, Long currentDeptId, boolean isAdmin) {
        KnowledgeCategoryEntity entity = knowledgeCategoryService.getById(command.getCategoryId());
        if (entity == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, command.getCategoryId(), "分类");
        }
        ensureCategoryAccessible(entity, currentDeptId, isAdmin);
        ensureParentCategoryAccessible(command.getParentId(), currentDeptId, isAdmin);

        entity.setParentId(command.getParentId() == null ? 0L : command.getParentId());
        entity.setAncestors(buildAncestors(entity.getParentId()));
        entity.setCategoryName(command.getCategoryName());
        entity.setDeptId(resolveDeptId(command.getDeptId(), currentDeptId, isAdmin));
        entity.setSortNum(command.getSortNum());
        entity.setStatus(command.getStatus());
        entity.setRemark(command.getRemark());
        knowledgeCategoryService.updateById(entity);
    }

    public void deleteCategory(Long categoryId, Long currentDeptId, boolean isAdmin) {
        KnowledgeCategoryEntity entity = knowledgeCategoryService.getById(categoryId);
        if (entity == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, categoryId, "分类");
        }
        ensureCategoryAccessible(entity, currentDeptId, isAdmin);

        long childCount = knowledgeCategoryService.lambdaQuery()
            .eq(KnowledgeCategoryEntity::getParentId, categoryId)
            .count();
        if (childCount > 0) {
            throw new ApiException(ErrorCode.Business.COMMON_UNSUPPORTED_OPERATION, "当前分类下存在子分类，不能删除");
        }

        long documentCount = knowledgeDocumentService.lambdaQuery()
            .eq(KnowledgeDocumentEntity::getCategoryId, categoryId)
            .count();
        if (documentCount > 0) {
            throw new ApiException(ErrorCode.Business.COMMON_UNSUPPORTED_OPERATION, "当前分类下存在文档，不能删除");
        }

        knowledgeCategoryService.removeById(categoryId);
    }

    private Long resolveDeptId(Long commandDeptId, Long currentDeptId, boolean isAdmin) {
        return isAdmin ? commandDeptId : currentDeptId;
    }

    private void ensureCategoryAccessible(KnowledgeCategoryEntity entity, Long currentDeptId, boolean isAdmin) {
        if (!isAdmin && !Objects.equals(entity.getDeptId(), currentDeptId)) {
            throw new ApiException(ErrorCode.Business.PERMISSION_NOT_ALLOWED_TO_OPERATE);
        }
    }

    private void ensureParentCategoryAccessible(Long parentId, Long currentDeptId, boolean isAdmin) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        KnowledgeCategoryEntity parent = knowledgeCategoryService.getById(parentId);
        if (parent == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, parentId, "父分类");
        }
        ensureCategoryAccessible(parent, currentDeptId, isAdmin);
    }

    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return "0";
        }
        KnowledgeCategoryEntity parent = knowledgeCategoryService.getById(parentId);
        if (parent == null) {
            return "0";
        }
        return parent.getAncestors() + "," + parentId;
    }
}
