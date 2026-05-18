package com.docbase.domain.knowledge.qa.dto;

import java.util.List;
import lombok.Data;

@Data
public class KnowledgeQaAnswerDTO {

    private String question;
    private String answer;
    private String provider;
    private String status;
    private List<KnowledgeQaCitationDTO> citations;
}
