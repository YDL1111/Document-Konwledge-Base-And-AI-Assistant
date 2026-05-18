package com.docbase.domain.knowledge.document.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeDocumentAuditCommand {

    @NotNull(message = "审批结果不能为空")
    private Integer approved;

    @Size(max = 500, message = "审批备注长度不能超过500个字符")
    private String auditRemark;
}
