package com.docbase.domain.knowledge.document.query;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.docbase.common.core.page.AbstractPageQuery;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeDocumentQuery extends AbstractPageQuery<KnowledgeDocumentEntity> {

    private String title;
    private Long categoryId;
    private Integer status;
    private Integer visibility;
    private Long creatorId;
    private Long deptId;
    private Boolean onlyVisibleToCurrentUser;
    private Boolean includePublishedShared;

    @Override
    public QueryWrapper<KnowledgeDocumentEntity> addQueryCondition() {
        QueryWrapper<KnowledgeDocumentEntity> queryWrapper = new QueryWrapper<KnowledgeDocumentEntity>()
            .like(StrUtil.isNotBlank(title), "title", title)
            .eq(categoryId != null, "category_id", categoryId)
            .eq(status != null, "status", status)
            .eq(visibility != null, "visibility", visibility)
            .orderByDesc("update_time");

        if (Boolean.TRUE.equals(onlyVisibleToCurrentUser) && creatorId != null) {
            queryWrapper.and(wrapper -> wrapper
                .eq("creator_id", creatorId)
                .or(Boolean.TRUE.equals(includePublishedShared), shared -> {
                    shared.eq("status", 3);
                    if (deptId != null) {
                        shared.and(visibilityWrapper -> visibilityWrapper
                            .eq("visibility", 1)
                            .or()
                            .eq("visibility", 2)
                            .eq("dept_id", deptId)
                        );
                    } else {
                        shared.eq("visibility", 1);
                    }
                })
            );
        } else if (creatorId != null) {
            queryWrapper.eq("creator_id", creatorId);
        }

        return queryWrapper;
    }
}
