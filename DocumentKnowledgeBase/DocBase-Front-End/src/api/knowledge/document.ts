import { http } from "@/utils/http";

export interface KnowledgeDocumentQuery extends BasePageQuery {
  title?: string;
  categoryId?: number;
  status?: number;
  visibility?: number;
}

export interface KnowledgeDocumentVersionDTO {
  versionId: number;
  documentId: number;
  versionNo: string;
  fileName?: string;
  fileExt?: string;
  fileSize?: number;
  storageType?: string;
  storagePath?: string;
  storageUrl?: string;
  versionRemark?: string;
  parseStatus?: number;
  isCurrent?: boolean;
  createTime?: string;
}

export interface KnowledgeDocumentDTO {
  documentId: number;
  categoryId?: number;
  deptId?: number;
  title: string;
  docCode?: string;
  summary?: string;
  tags?: string;
  visibility?: number;
  status?: number;
  currentVersionNo?: string;
  auditRemark?: string;
  creatorId?: number;
  updateTime?: string;
}

export interface KnowledgeDocumentDetailDTO extends KnowledgeDocumentDTO {
  categoryName?: string;
  creatorName?: string;
  deptName?: string;
  currentVersionStorageUrl?: string;
  currentVersionStoragePath?: string;
  currentVersion?: KnowledgeDocumentVersionDTO;
  versionList?: KnowledgeDocumentVersionDTO[];
  auditHistoryList?: KnowledgeDocumentAuditLogDTO[];
}

export interface KnowledgeDocumentAuditLogDTO {
  auditLogId: number;
  documentId: number;
  auditResult: number;
  auditRemark?: string;
  auditorId?: number;
  auditorName?: string;
  beforeStatus?: number;
  afterStatus?: number;
  createTime?: string;
}

export interface KnowledgeDocumentAddRequest {
  categoryId: number;
  title: string;
  docCode?: string;
  summary?: string;
  tags?: string;
  visibility: number;
}

export interface KnowledgeDocumentAuditRequest {
  approved: number;
  auditRemark?: string;
}

export const getKnowledgeDocumentListApi = (params?: KnowledgeDocumentQuery) => {
  return http.request<ResponseData<PageDTO<KnowledgeDocumentDTO>>>(
    "get",
    "/knowledge/documents",
    {
      params
    }
  );
};

export const getKnowledgeDocumentDetailApi = (documentId: number) => {
  return http.request<ResponseData<KnowledgeDocumentDetailDTO>>(
    "get",
    `/knowledge/documents/${documentId}`
  );
};

export const getKnowledgeDocumentPreviewApi = (documentId: number) => {
  return http.request<ResponseData<string>>(
    "get",
    `/knowledge/documents/${documentId}/preview`
  );
};

export const addKnowledgeDocumentApi = (
  data: KnowledgeDocumentAddRequest,
  file: File
) => {
  const formData = new FormData();
  formData.append("categoryId", String(data.categoryId));
  formData.append("title", data.title);
  if (data.docCode) formData.append("docCode", data.docCode);
  if (data.summary) formData.append("summary", data.summary);
  if (data.tags) formData.append("tags", data.tags);
  formData.append("visibility", String(data.visibility));
  formData.append("file", file);

  return http.request<ResponseData<void>>(
    "post",
    "/knowledge/documents",
    {
      data: formData
    },
    {
      headers: {
        "Content-Type": "multipart/form-data"
      }
    }
  );
};

export const auditKnowledgeDocumentApi = (
  documentId: number,
  data: KnowledgeDocumentAuditRequest
) => {
  return http.request<ResponseData<void>>(
    "put",
    `/knowledge/documents/${documentId}/audit`,
    { data }
  );
};

export const getKnowledgeDocumentDownloadUrl = (documentId: number) =>
  `/knowledge/documents/${documentId}/download`;
