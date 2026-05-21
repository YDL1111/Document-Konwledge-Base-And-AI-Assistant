package com.docbase.domain.ai.chat.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiChatAnswerDTO {

    private Long sessionId;
    private String answer;
    private List<SourceInfo> sources;

    @Data
    @Builder
    public static class SourceInfo {
        private String filename;
        private Integer page;
        private Double score;
        private String content;
    }
}
