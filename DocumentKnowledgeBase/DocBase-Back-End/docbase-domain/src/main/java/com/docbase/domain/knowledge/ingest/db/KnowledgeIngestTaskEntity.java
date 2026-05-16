package com.docbase.domain.knowledge.ingest.db;

import com.docbase.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("knowledge_ingest_task")
@ApiModel(value = "KnowledgeIngestTaskEntity", description = "知识库入库任务表")
public class KnowledgeIngestTaskEntity extends BaseEntity<KnowledgeIngestTaskEntity> {

    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;

    @TableField("task_no")
    private String taskNo;

    @TableField("document_id")
    private Long documentId;

    @TableField("version_id")
    private Long versionId;

    @TableField("task_type")
    private Integer taskType;

    @TableField("`status`")
    private Integer status;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("error_message")
    private String errorMessage;

    @TableField("trace_id")
    private String traceId;

    @TableField("started_time")
    private Date startedTime;

    @TableField("finished_time")
    private Date finishedTime;

    @Override
    public Serializable pkVal() {
        return this.taskId;
    }
}
