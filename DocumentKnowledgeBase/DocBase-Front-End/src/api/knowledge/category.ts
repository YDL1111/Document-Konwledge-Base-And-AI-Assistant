import { http } from "@/utils/http";

export interface KnowledgeCategoryQuery extends BasePageQuery {
  categoryName?: string;
  status?: number;
  parentId?: number;
}

export interface KnowledgeCategoryDTO {
  categoryId: number;
  parentId?: number;
  categoryName: string;
  deptId?: number;
  sortNum?: number;
  status?: number;
  remark?: string;
  createTime?: string;
}

export interface KnowledgeCategoryRequest {
  parentId?: number;
  categoryName: string;
  deptId?: number;
  sortNum: number;
  status: number;
  remark?: string;
}

export const getKnowledgeCategoryListApi = (params?: KnowledgeCategoryQuery) => {
  return http.request<ResponseData<PageDTO<KnowledgeCategoryDTO>>>(
    "get",
    "/knowledge/categories",
    {
      params
    }
  );
};

export const getKnowledgeCategoryDetailApi = (categoryId: number) => {
  return http.request<ResponseData<KnowledgeCategoryDTO>>(
    "get",
    `/knowledge/categories/${categoryId}`
  );
};

export const addKnowledgeCategoryApi = (data: KnowledgeCategoryRequest) => {
  return http.request<ResponseData<void>>("post", "/knowledge/categories", { data });
};

export const updateKnowledgeCategoryApi = (
  categoryId: number,
  data: KnowledgeCategoryRequest
) => {
  return http.request<ResponseData<void>>(
    "put",
    `/knowledge/categories/${categoryId}`,
    { data }
  );
};

export const deleteKnowledgeCategoryApi = (categoryId: number) => {
  return http.request<ResponseData<void>>(
    "delete",
    `/knowledge/categories/${categoryId}`
  );
};
