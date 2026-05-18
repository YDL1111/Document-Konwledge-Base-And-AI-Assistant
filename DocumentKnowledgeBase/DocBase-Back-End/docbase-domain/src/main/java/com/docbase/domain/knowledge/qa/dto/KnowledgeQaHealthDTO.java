package com.docbase.domain.knowledge.qa.dto;

import lombok.Data;

@Data
public class KnowledgeQaHealthDTO {

    private String service;
    private String status;
    private String provider;
    private String message;
}
