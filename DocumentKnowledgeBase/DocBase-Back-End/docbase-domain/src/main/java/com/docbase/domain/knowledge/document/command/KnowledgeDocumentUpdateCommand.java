package com.docbase.domain.knowledge.document.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeDocumentUpdateCommand {

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "文档标题不能为空")
    @Size(max = 256, message = "文档标题长度不能超过256个字符")
    private String title;

    @Size(max = 64, message = "文档编号长度不能超过64个字符")
    private String docCode;

    @Size(max = 1000, message = "文档摘要长度不能超过1000个字符")
    private String summary;

    @Size(max = 1000, message = "标签长度不能超过1000个字符")
    private String tags;

    @NotNull(message = "可见范围不能为空")
    private Integer visibility;

    @Size(max = 500, message = "版本说明长度不能超过500个字符")
    private String versionRemark;
}
