package com.docbase.domain.knowledge.ingest.dto;

import com.docbase.domain.knowledge.ingest.db.KnowledgeIngestTaskEntity;
import java.util.Date;
import lombok.Data;

@Data
public class KnowledgeIngestTaskDTO {

    private Long taskId;
    private String taskNo;
    private Long documentId;
    private Long versionId;
    private Integer taskType;
    private Integer status;
    private Integer retryCount;
    private Integer chunkCount;
    private String errorMessage;
    private String traceId;
    private Date startedTime;
    private Date finishedTime;
    private Integer pythonKbId;
    private Integer pythonDocId;

    public KnowledgeIngestTaskDTO(KnowledgeIngestTaskEntity entity) {
        if (entity != null) {
            this.taskId = entity.getTaskId();
            this.taskNo = entity.getTaskNo();
            this.documentId = entity.getDocumentId();
            this.versionId = entity.getVersionId();
            this.taskType = entity.getTaskType();
            this.status = entity.getStatus();
            this.retryCount = entity.getRetryCount();
            this.chunkCount = entity.getChunkCount();
            this.errorMessage = entity.getErrorMessage();
            this.traceId = entity.getTraceId();
            this.startedTime = entity.getStartedTime();
            this.finishedTime = entity.getFinishedTime();
            this.pythonKbId = entity.getPythonKbId();
            this.pythonDocId = entity.getPythonDocId();
        }
    }
}
