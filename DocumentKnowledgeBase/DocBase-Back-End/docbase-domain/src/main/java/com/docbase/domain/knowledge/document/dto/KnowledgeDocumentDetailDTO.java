package com.docbase.domain.knowledge.document.dto;

import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class KnowledgeDocumentDetailDTO {

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
    private Date updateTime;

    private String categoryName;
    private String creatorName;
    private String deptName;
    private String currentVersionStorageUrl;
    private String currentVersionStoragePath;
    private KnowledgeDocumentVersionDTO currentVersion;
    private List<KnowledgeDocumentVersionDTO> versionList;
    private List<KnowledgeDocumentAuditLogDTO> auditHistoryList;
}
