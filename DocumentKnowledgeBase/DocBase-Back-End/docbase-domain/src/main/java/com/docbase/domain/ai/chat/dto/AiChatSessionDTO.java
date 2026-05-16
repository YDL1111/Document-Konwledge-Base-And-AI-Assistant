package com.docbase.domain.ai.chat.dto;

import com.docbase.domain.ai.chat.db.AiChatSessionEntity;
import java.util.Date;
import lombok.Data;

@Data
public class AiChatSessionDTO {

    private Long sessionId;
    private String sessionTitle;
    private Long userId;
    private Long deptId;
    private Date lastMessageTime;
    private Integer status;

    public AiChatSessionDTO(AiChatSessionEntity entity) {
        if (entity != null) {
            this.sessionId = entity.getSessionId();
            this.sessionTitle = entity.getSessionTitle();
            this.userId = entity.getUserId();
            this.deptId = entity.getDeptId();
            this.lastMessageTime = entity.getLastMessageTime();
            this.status = entity.getStatus();
        }
    }
}
