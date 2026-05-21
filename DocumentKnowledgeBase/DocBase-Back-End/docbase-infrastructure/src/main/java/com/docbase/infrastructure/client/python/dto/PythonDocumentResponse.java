package com.docbase.infrastructure.client.python.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PythonDocumentResponse {

    private Integer code;
    private String message;
    private PythonDocumentData data;

    @Data
    public static class PythonDocumentData {
        private Integer id;
        @JsonProperty("kb_id")
        private Integer kbId;
        private String filename;
        @JsonProperty("file_path")
        private String filePath;
        @JsonProperty("file_type")
        private String fileType;
        @JsonProperty("file_size")
        private Long fileSize;
        private String status;
        @JsonProperty("error_msg")
        private String errorMsg;
        @JsonProperty("chunk_count")
        private Integer chunkCount;
        @JsonProperty("char_count")
        private Integer charCount;
        @JsonProperty("source_type")
        private String sourceType;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
    }
}
