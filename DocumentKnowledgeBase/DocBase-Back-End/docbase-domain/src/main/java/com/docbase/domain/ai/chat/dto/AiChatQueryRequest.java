package com.docbase.domain.ai.chat.dto;

import lombok.Data;

@Data
public class AiChatQueryRequest {

    private Long sessionId;
    private String question;
    private Integer kbId;
    private Long categoryId;
    private Long documentId;
}
