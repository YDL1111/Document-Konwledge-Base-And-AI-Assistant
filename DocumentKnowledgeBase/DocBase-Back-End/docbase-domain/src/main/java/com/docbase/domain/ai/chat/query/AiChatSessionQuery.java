package com.docbase.domain.ai.chat.query;

import cn.hutool.core.util.StrUtil;
import com.docbase.common.core.page.AbstractPageQuery;
import com.docbase.domain.ai.chat.db.AiChatSessionEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiChatSessionQuery extends AbstractPageQuery<AiChatSessionEntity> {

    private String sessionTitle;
    private Integer status;

    @Override
    public QueryWrapper<AiChatSessionEntity> addQueryCondition() {
        return new QueryWrapper<AiChatSessionEntity>()
            .like(StrUtil.isNotBlank(sessionTitle), "session_title", sessionTitle)
            .eq(status != null, "status", status)
            .orderByDesc("last_message_time");
    }
}
