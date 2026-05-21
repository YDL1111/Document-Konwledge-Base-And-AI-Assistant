package com.docbase.infrastructure.client.python.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PythonKbResponse {

    private Integer code;
    private String message;
    private PythonKbData data;

    @Data
    public static class PythonKbData {
        private Integer id;
        private String name;
        private String description;
        private String icon;
        private String status;
        @JsonProperty("embedding_model")
        private String embeddingModel;
        @JsonProperty("doc_count")
        private Integer docCount;
        @JsonProperty("vector_count")
        private Integer vectorCount;
    }
}
