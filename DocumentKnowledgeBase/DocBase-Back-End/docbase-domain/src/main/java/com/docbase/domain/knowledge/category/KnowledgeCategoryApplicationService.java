package com.docbase.domain.knowledge.category;

import com.docbase.common.core.page.PageDTO;
import com.docbase.common.exception.ApiException;
import com.docbase.common.exception.error.ErrorCode;
import com.docbase.domain.knowledge.category.command.KnowledgeCategoryAddCommand;
import com.docbase.domain.knowledge.category.command.KnowledgeCategoryUpdateCommand;
import com.docbase.domain.knowledge.category.db.KnowledgeCategoryEntity;
import com.docbase.domain.knowledge.category.db.KnowledgeCategoryService;
import com.docbase.domain.knowledge.category.dto.KnowledgeCategoryDTO;
import com.docbase.domain.knowledge.category.query.KnowledgeCategoryQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeCategoryApplicationService {

    private final KnowledgeCategoryService knowledgeCategoryService;

    public PageDTO<KnowledgeCategoryDTO> getCategoryList(KnowledgeCategoryQuery query) {
        Page<KnowledgeCategoryEntity> page = knowledgeCategoryService.page(query.toPage(), query.toQueryWrapper());
        List<KnowledgeCategoryDTO> records = page.getRecords().stream()
            .map(KnowledgeCategoryDTO::new)
            .collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public KnowledgeCategoryDTO getCategoryInfo(Long categoryId) {
        KnowledgeCategoryEntity entity = knowledgeCategoryService.getById(categoryId);
        if (entity == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, categoryId, "知识库分类");
        }
        return new KnowledgeCategoryDTO(entity);
    }

    public void addCategory(KnowledgeCategoryAddCommand command) {
        KnowledgeCategoryEntity entity = new KnowledgeCategoryEntity();
        entity.setParentId(command.getParentId() == null ? 0L : command.getParentId());
        entity.setAncestors(buildAncestors(entity.getParentId()));
        entity.setCategoryName(command.getCategoryName());
        entity.setDeptId(command.getDeptId());
        entity.setSortNum(command.getSortNum());
        entity.setStatus(command.getStatus());
        entity.setRemark(command.getRemark());
        knowledgeCategoryService.save(entity);
    }

    public void updateCategory(KnowledgeCategoryUpdateCommand command) {
        KnowledgeCategoryEntity entity = knowledgeCategoryService.getById(command.getCategoryId());
        if (entity == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, command.getCategoryId(), "知识库分类");
        }
        entity.setParentId(command.getParentId() == null ? 0L : command.getParentId());
        entity.setAncestors(buildAncestors(entity.getParentId()));
        entity.setCategoryName(command.getCategoryName());
        entity.setDeptId(command.getDeptId());
        entity.setSortNum(command.getSortNum());
        entity.setStatus(command.getStatus());
        entity.setRemark(command.getRemark());
        knowledgeCategoryService.updateById(entity);
    }

    public void deleteCategory(Long categoryId) {
        long childCount = knowledgeCategoryService.lambdaQuery().eq(KnowledgeCategoryEntity::getParentId, categoryId).count();
        if (childCount > 0) {
            throw new ApiException(ErrorCode.Business.COMMON_UNSUPPORTED_OPERATION);
        }
        knowledgeCategoryService.removeById(categoryId);
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
