package com.docbase.domain.knowledge.document.query;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.docbase.common.core.page.AbstractPageQuery;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentEntity;
import java.util.List;
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
    private List<Long> deptIdList;

    @Override
    public QueryWrapper<KnowledgeDocumentEntity> addQueryCondition() {
        QueryWrapper<KnowledgeDocumentEntity> queryWrapper = new QueryWrapper<KnowledgeDocumentEntity>()
            .like(StrUtil.isNotBlank(title), "title", title)
            .eq(categoryId != null, "category_id", categoryId)
            .eq(status != null, "status", status)
            .eq(visibility != null, "visibility", visibility)
            .orderByDesc("update_time");

        if (Boolean.TRUE.equals(onlyVisibleToCurrentUser)) {
            queryWrapper.and(wrapper -> {
                if (creatorId != null) {
                    wrapper.eq("creator_id", creatorId);
                }
                if (Boolean.TRUE.equals(includePublishedShared)) {
                    if (creatorId != null) {
                        wrapper.or();
                    }
                    wrapper.eq("status", 3).and(visWrapper -> {
                        visWrapper.eq("visibility", 1);
                        if (CollUtil.isNotEmpty(deptIdList)) {
                            visWrapper.or()
                                .eq("visibility", 2)
                                .in("dept_id", deptIdList);
                        } else if (deptId != null) {
                            visWrapper.or()
                                .eq("visibility", 2)
                                .eq("dept_id", deptId);
                        }
                    });
                }
            });
        } else if (creatorId != null) {
            queryWrapper.eq("creator_id", creatorId);
        }

        return queryWrapper;
    }
}
