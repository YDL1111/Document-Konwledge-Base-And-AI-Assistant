package com.docbase.domain.knowledge.qa.dto;

import lombok.Data;

@Data
public class KnowledgeQaCitationDTO {

    private Long documentId;
    private String documentTitle;
    private String snippet;
}
