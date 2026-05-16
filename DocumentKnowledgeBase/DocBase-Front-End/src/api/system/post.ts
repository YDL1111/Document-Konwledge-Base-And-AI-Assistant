import { http } from "@/utils/http";

export interface PostQuery extends BasePageQuery {
  postCode?: string;
  postName?: string;
  status?: number;
}

export interface PostPageResponse {
  postId: number;
  postCode: string;
  postName: string;
  postSort?: number;
  status?: number;
  remark?: string;
  createTime?: Date;
}

export interface PostRequest {
  postId?: number;
  postCode: string;
  postName: string;
  postSort: number;
  status: number;
  remark?: string;
}

export const getPostListApi = (params?: Partial<PostQuery>) => {
  return http.request<ResponseData<PageDTO<PostPageResponse>>>(
    "get",
    "/system/post/list",
    {
      params
    }
  );
};

export const getPostInfoApi = (postId: number) => {
  return http.request<ResponseData<PostPageResponse>>(
    "get",
    `/system/post/${postId}`
  );
};

export const addPostApi = (data: PostRequest) => {
  return http.request<ResponseData<void>>("post", "/system/post", {
    data
  });
};

export const updatePostApi = (data: PostRequest) => {
  return http.request<ResponseData<void>>("put", "/system/post", {
    data
  });
};

export const deletePostApi = (ids: number[]) => {
  return http.request<ResponseData<void>>("delete", "/system/post", {
    params: {
      ids: ids.toString()
    }
  });
};
