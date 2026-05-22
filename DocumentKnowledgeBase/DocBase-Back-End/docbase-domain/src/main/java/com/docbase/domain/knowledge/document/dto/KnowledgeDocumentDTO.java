package com.docbase.domain.knowledge.document.dto;

import com.docbase.domain.knowledge.document.db.KnowledgeDocumentEntity;
import java.util.Date;
import lombok.Data;

@Data
public class KnowledgeDocumentDTO {

    private Long documentId;
    private Long categoryId;
    private Long deptId;
    private String title;
    private String docCode;
    private String summary;
    private String tags;
    private Integer visibility;
    private Integer status;
    private String currentVersionNo;
    private String auditRemark;
    private Long creatorId;
    private Date updateTime;
    private Boolean hasAiImport;

    public KnowledgeDocumentDTO(KnowledgeDocumentEntity entity) {
        if (entity != null) {
            this.documentId = entity.getDocumentId();
            this.categoryId = entity.getCategoryId();
            this.deptId = entity.getDeptId();
            this.title = entity.getTitle();
            this.docCode = entity.getDocCode();
            this.summary = entity.getSummary();
            this.tags = entity.getTags();
            this.visibility = entity.getVisibility();
            this.status = entity.getStatus();
            this.currentVersionNo = entity.getCurrentVersionNo();
            this.auditRemark = entity.getAuditRemark();
            this.creatorId = entity.getCreatorId();
            this.updateTime = entity.getUpdateTime();
        }
    }
}
