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
  pythonKbId?: number;
  pythonDocId?: number;
}

export const getKnowledgeIngestTaskListApi = (params?: KnowledgeIngestTaskQuery) => {
  return http.request<ResponseData<PageDTO<KnowledgeIngestTaskDTO>>>(
    "get",
    "/knowledge/ingest/tasks",
    { params }
  );
};

export const submitImportTaskApi = (documentId: number) => {
  return http.request<ResponseData<KnowledgeIngestTaskDTO>>(
    "post",
    `/knowledge/ingest/tasks/submit/${documentId}`
  );
};

export const retryIngestTaskApi = (taskId: number) => {
  return http.request<ResponseData<KnowledgeIngestTaskDTO>>(
    "post",
    `/knowledge/ingest/tasks/${taskId}/retry`
  );
};

export const processIngestTaskApi = (taskId: number) => {
  return http.request<ResponseData<KnowledgeIngestTaskDTO>>(
    "post",
    `/knowledge/ingest/tasks/${taskId}/process`
  );
};

export const pollIngestTaskApi = (taskId: number) => {
  return http.request<ResponseData<KnowledgeIngestTaskDTO>>(
    "post",
    `/knowledge/ingest/tasks/${taskId}/poll`
  );
};

export const processPendingTasksApi = () => {
  return http.request<ResponseData<{ processed: number }>>(
    "post",
    "/knowledge/ingest/tasks/process-pending"
  );
};
