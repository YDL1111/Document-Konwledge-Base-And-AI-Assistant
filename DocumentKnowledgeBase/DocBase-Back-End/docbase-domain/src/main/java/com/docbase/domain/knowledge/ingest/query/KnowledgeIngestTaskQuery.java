package com.docbase.domain.knowledge.ingest.query;

import cn.hutool.core.util.StrUtil;
import com.docbase.common.core.page.AbstractPageQuery;
import com.docbase.domain.knowledge.ingest.db.KnowledgeIngestTaskEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeIngestTaskQuery extends AbstractPageQuery<KnowledgeIngestTaskEntity> {

    private String taskNo;
    private Integer status;
    private Long documentId;
    private Long creatorId;

    @Override
    public QueryWrapper<KnowledgeIngestTaskEntity> addQueryCondition() {
        return new QueryWrapper<KnowledgeIngestTaskEntity>()
            .like(StrUtil.isNotBlank(taskNo), "task_no", taskNo)
            .eq(status != null, "status", status)
            .eq(documentId != null, "document_id", documentId)
            .eq(creatorId != null, "creator_id", creatorId)
            .orderByDesc("create_time");
    }
}
