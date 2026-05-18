package com.docbase.domain.knowledge.qa.command;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class KnowledgeQaAskCommand {

    @NotBlank(message = "问题不能为空")
    private String question;

    private Long categoryId;

    private List<Long> documentIds;
}
