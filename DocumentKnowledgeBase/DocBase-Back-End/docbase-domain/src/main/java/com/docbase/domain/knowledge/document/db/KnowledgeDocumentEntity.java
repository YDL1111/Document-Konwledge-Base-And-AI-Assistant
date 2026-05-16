package com.docbase.domain.knowledge.document.db;

import com.docbase.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("knowledge_document")
@ApiModel(value = "KnowledgeDocumentEntity", description = "知识库文档表")
public class KnowledgeDocumentEntity extends BaseEntity<KnowledgeDocumentEntity> {

    @TableId(value = "document_id", type = IdType.AUTO)
    private Long documentId;

    @TableField("category_id")
    private Long categoryId;

    @TableField("dept_id")
    private Long deptId;

    @TableField("title")
    private String title;

    @TableField("doc_code")
    private String docCode;

    @TableField("summary")
    private String summary;

    @TableField("tags")
    private String tags;

    @TableField("visibility")
    private Integer visibility;

    @TableField("`status`")
    private Integer status;

    @TableField("current_version_id")
    private Long currentVersionId;

    @TableField("current_version_no")
    private String currentVersionNo;

    @Override
    public Serializable pkVal() {
        return this.documentId;
    }
}
