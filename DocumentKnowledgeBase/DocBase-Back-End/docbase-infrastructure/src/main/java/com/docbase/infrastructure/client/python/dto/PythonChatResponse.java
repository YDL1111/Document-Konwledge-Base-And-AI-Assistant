package com.docbase.infrastructure.client.python.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class PythonChatResponse {

    private Integer code;
    private String message;
    private PythonChatData data;

    @Data
    public static class PythonChatData {
        @JsonProperty("conv_id")
        private Integer convId;
        private PythonMessage message;
    }

    @Data
    public static class PythonMessage {
        private Integer id;
        @JsonProperty("conv_id")
        private Integer convId;
        private String role;
        private String content;
        private List<PythonSource> sources;
        @JsonProperty("created_at")
        private String createdAt;
    }

    @Data
    public static class PythonSource {
        private Integer index;
        private String filename;
        private Integer page;
        private Double score;
        @JsonProperty("doc_id")
        private Integer docId;
        private String content;
    }
}
