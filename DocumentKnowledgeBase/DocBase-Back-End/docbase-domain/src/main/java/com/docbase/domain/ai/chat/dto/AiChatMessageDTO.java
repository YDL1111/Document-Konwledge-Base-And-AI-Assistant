package com.docbase.domain.ai.chat.dto;

import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class AiChatMessageDTO {

    private Long messageId;
    private Long sessionId;
    private Integer messageRole;
    private String messageContent;
    private List<AiChatAnswerDTO.SourceInfo> sources;
    private Date createTime;
}
