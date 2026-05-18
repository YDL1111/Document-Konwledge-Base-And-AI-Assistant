import { http } from "@/utils/http";

export interface KnowledgeIngestTaskQuery extends BasePageQuery {
  taskNo?: string;
  status?: number;
  documentId?: number;
}

export interface KnowledgeIngestTaskDTO {
  taskId: number;
  taskNo?: string;
  documentId?: number;
  versionId?: number;
  taskType?: number;
  status?: number;
  retryCount?: number;
  chunkCount?: number;
  errorMessage?: string;
  traceId?: string;
  startedTime?: string;
  finishedTime?: string;
}

export const getKnowledgeIngestTaskListApi = (params?: KnowledgeIngestTaskQuery) => {
  return http.request<ResponseData<PageDTO<KnowledgeIngestTaskDTO>>>(
    "get",
    "/knowledge/ingest/tasks",
    { params }
  );
};
