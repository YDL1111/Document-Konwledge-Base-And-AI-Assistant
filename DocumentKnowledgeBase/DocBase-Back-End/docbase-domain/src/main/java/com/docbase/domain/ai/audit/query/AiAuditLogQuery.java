package com.docbase.domain.ai.audit.query;

import cn.hutool.core.util.StrUtil;
import com.docbase.common.core.page.AbstractPageQuery;
import com.docbase.domain.ai.audit.db.AiAuditLogEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiAuditLogQuery extends AbstractPageQuery<AiAuditLogEntity> {

    private String questionText;
    private Integer resultStatus;

    @Override
    public QueryWrapper<AiAuditLogEntity> addQueryCondition() {
        return new QueryWrapper<AiAuditLogEntity>()
            .like(StrUtil.isNotBlank(questionText), "question_text", questionText)
            .eq(resultStatus != null, "result_status", resultStatus)
            .orderByDesc("create_time");
    }
}
