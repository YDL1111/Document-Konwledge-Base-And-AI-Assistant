package com.docbase.domain.ai.audit.dto;

import com.docbase.domain.ai.audit.db.AiAuditLogEntity;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class AiAuditLogDTO {

    private Long auditId;
    private Long sessionId;
    private Long userId;
    private Long deptId;
    private String questionText;
    private Integer retrievalCount;
    private BigDecimal topScore;
    private String modelName;
    private Integer totalTokens;
    private Integer latencyMs;
    private Integer resultStatus;
    private String errorMessage;
    private Date createTime;

    public AiAuditLogDTO(AiAuditLogEntity entity) {
        if (entity != null) {
            this.auditId = entity.getAuditId();
            this.sessionId = entity.getSessionId();
            this.userId = entity.getUserId();
            this.deptId = entity.getDeptId();
            this.questionText = entity.getQuestionText();
            this.retrievalCount = entity.getRetrievalCount();
            this.topScore = entity.getTopScore();
            this.modelName = entity.getModelName();
            this.totalTokens = entity.getTotalTokens();
            this.latencyMs = entity.getLatencyMs();
            this.resultStatus = entity.getResultStatus();
            this.errorMessage = entity.getErrorMessage();
            this.createTime = entity.getCreateTime();
        }
    }
}
