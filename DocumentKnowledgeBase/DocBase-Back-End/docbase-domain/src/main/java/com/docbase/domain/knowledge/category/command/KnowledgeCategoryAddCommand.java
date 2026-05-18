package com.docbase.domain.knowledge.category.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeCategoryAddCommand {

    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 128, message = "分类名称不能超过128个字符")
    private String categoryName;

    private Long deptId;

    @NotNull(message = "排序不能为空")
    private Integer sortNum;

    @NotNull(message = "状态不能为空")
    private Integer status;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}
