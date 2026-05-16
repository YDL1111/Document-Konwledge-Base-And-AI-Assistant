package com.docbase.domain.knowledge.category.query;

import cn.hutool.core.util.StrUtil;
import com.docbase.common.core.page.AbstractPageQuery;
import com.docbase.domain.knowledge.category.db.KnowledgeCategoryEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeCategoryQuery extends AbstractPageQuery<KnowledgeCategoryEntity> {

    private String categoryName;
    private Integer status;
    private Long parentId;

    @Override
    public QueryWrapper<KnowledgeCategoryEntity> addQueryCondition() {
        return new QueryWrapper<KnowledgeCategoryEntity>()
            .like(StrUtil.isNotBlank(categoryName), "category_name", categoryName)
            .eq(status != null, "status", status)
            .eq(parentId != null, "parent_id", parentId)
            .orderByAsc("sort_num")
            .orderByDesc("create_time");
    }
}
