package com.docbase.domain.knowledge.document.query;

import cn.hutool.core.util.StrUtil;
import com.docbase.common.core.page.AbstractPageQuery;
import com.docbase.domain.knowledge.document.db.KnowledgeDocumentEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeDocumentQuery extends AbstractPageQuery<KnowledgeDocumentEntity> {

    private String title;
    private Long categoryId;
    private Integer status;
    private Integer visibility;

    @Override
    public QueryWrapper<KnowledgeDocumentEntity> addQueryCondition() {
        return new QueryWrapper<KnowledgeDocumentEntity>()
            .like(StrUtil.isNotBlank(title), "title", title)
            .eq(categoryId != null, "category_id", categoryId)
            .eq(status != null, "status", status)
            .eq(visibility != null, "visibility", visibility)
            .orderByDesc("update_time");
    }
}
