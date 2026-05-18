package com.docbase.domain.knowledge.document.dto;

import com.docbase.domain.knowledge.document.db.KnowledgeDocumentAuditLogEntity;
import java.util.Date;
import lombok.Data;

@Data
public class KnowledgeDocumentAuditLogDTO {

    private Long auditLogId;
    private Long documentId;
    private Integer auditResult;
    private String auditRemark;
    private Long auditorId;
    private String auditorName;
    private Integer beforeStatus;
    private Integer afterStatus;
    private Date createTime;

    public KnowledgeDocumentAuditLogDTO(KnowledgeDocumentAuditLogEntity entity) {
        if (entity != null) {
            this.auditLogId = entity.getAuditLogId();
            this.documentId = entity.getDocumentId();
            this.auditResult = entity.getAuditResult();
            this.auditRemark = entity.getAuditRemark();
            this.auditorId = entity.getAuditorId();
            this.auditorName = entity.getAuditorName();
            this.beforeStatus = entity.getBeforeStatus();
            this.afterStatus = entity.getAfterStatus();
            this.createTime = entity.getCreateTime();
        }
    }
}
