package com.docbase.domain.knowledge.category.db;

import com.docbase.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("knowledge_category")
@ApiModel(value = "KnowledgeCategoryEntity", description = "知识库分类表")
public class KnowledgeCategoryEntity extends BaseEntity<KnowledgeCategoryEntity> {

    @TableId(value = "category_id", type = IdType.AUTO)
    @ApiModelProperty("分类ID")
    private Long categoryId;

    @TableField("parent_id")
    private Long parentId;

    @TableField("ancestors")
    private String ancestors;

    @TableField("category_name")
    private String categoryName;

    @TableField("dept_id")
    private Long deptId;

    @TableField("sort_num")
    private Integer sortNum;

    @TableField("`status`")
    private Integer status;

    @TableField("remark")
    private String remark;

    @Override
    public Serializable pkVal() {
        return this.categoryId;
    }
}
