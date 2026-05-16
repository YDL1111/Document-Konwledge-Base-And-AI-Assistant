package com.docbase.domain.ai.audit.db;

import com.docbase.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ai_audit_log")
public class AiAuditLogEntity extends BaseEntity<AiAuditLogEntity> {

    @TableId(value = "audit_id", type = IdType.AUTO)
    private Long auditId;

    @TableField("session_id")
    private Long sessionId;

    @TableField("message_id")
    private Long messageId;

    @TableField("user_id")
    private Long userId;

    @TableField("dept_id")
    private Long deptId;

    @TableField("question_text")
    private String questionText;

    @TableField("retrieval_count")
    private Integer retrievalCount;

    @TableField("top_score")
    private BigDecimal topScore;

    @TableField("model_name")
    private String modelName;

    @TableField("total_tokens")
    private Integer totalTokens;

    @TableField("latency_ms")
    private Integer latencyMs;

    @TableField("result_status")
    private Integer resultStatus;

    @TableField("error_message")
    private String errorMessage;

    @Override
    public Serializable pkVal() {
        return this.auditId;
    }
}
