package com.docbase.domain.knowledge.document.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.docbase.common.core.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("knowledge_document_audit_log")
@ApiModel(value = "KnowledgeDocumentAuditLogEntity", description = "知识库文档审核记录表")
public class KnowledgeDocumentAuditLogEntity extends BaseEntity<KnowledgeDocumentAuditLogEntity> {

    @TableId(value = "audit_log_id", type = IdType.AUTO)
    private Long auditLogId;

    @TableField("document_id")
    private Long documentId;

    @TableField("audit_result")
    private Integer auditResult;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("auditor_id")
    private Long auditorId;

    @TableField("auditor_name")
    private String auditorName;

    @TableField("before_status")
    private Integer beforeStatus;

    @TableField("after_status")
    private Integer afterStatus;

    @Override
    public Serializable pkVal() {
        return this.auditLogId;
    }
}
