package com.docbase.infrastructure.client.python.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class PythonDocumentUploadResponse {

    private Integer code;
    private String message;
    private UploadData data;

    @Data
    public static class UploadData {
        @JsonProperty("doc_ids")
        private List<Integer> docIds;
    }
}
