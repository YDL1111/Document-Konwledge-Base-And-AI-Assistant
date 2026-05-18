package com.docbase.domain.knowledge.document.dto;

import com.docbase.domain.knowledge.document.db.KnowledgeDocumentVersionEntity;
import java.util.Date;
import lombok.Data;

@Data
public class KnowledgeDocumentVersionDTO {

    private Long versionId;
    private Long documentId;
    private String versionNo;
    private String fileName;
    private String fileExt;
    private Long fileSize;
    private String storageType;
    private String storagePath;
    private String storageUrl;
    private String versionRemark;
    private Integer parseStatus;
    private Boolean isCurrent;
    private Date createTime;

    public KnowledgeDocumentVersionDTO(KnowledgeDocumentVersionEntity entity) {
        if (entity != null) {
            this.versionId = entity.getVersionId();
            this.documentId = entity.getDocumentId();
            this.versionNo = entity.getVersionNo();
            this.fileName = entity.getFileName();
            this.fileExt = entity.getFileExt();
            this.fileSize = entity.getFileSize();
            this.storageType = entity.getStorageType();
            this.storagePath = entity.getStoragePath();
            this.storageUrl = entity.getStorageUrl();
            this.versionRemark = entity.getVersionRemark();
            this.parseStatus = entity.getParseStatus();
            this.isCurrent = entity.getIsCurrent();
            this.createTime = entity.getCreateTime();
        }
    }
}
