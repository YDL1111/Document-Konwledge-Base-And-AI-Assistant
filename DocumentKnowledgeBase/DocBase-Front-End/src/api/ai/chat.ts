import { http } from "@/utils/http";
import type { AiChatQueryRequest, AiChatAnswerDTO, AiChatMessageDTO, AiChatSessionDTO } from "./types";

export const getAiChatSessionsApi = (params?: Record<string, unknown>) => {
  return http.request<ResponseData<PageDTO<AiChatSessionDTO>>>(
    "get",
    "/ai/chat/sessions",
    { params }
  );
};

export const getAiChatMessagesApi = (sessionId: number) => {
  return http.request<ResponseData<AiChatMessageDTO[]>>(
    "get",
    `/ai/chat/sessions/${sessionId}/messages`
  );
};

export const deleteAiChatSessionApi = (sessionId: number) => {
  return http.request<ResponseData<string>>(
    "delete",
    `/ai/chat/sessions/${sessionId}`
  );
};

export const queryAiChatApi = (data: AiChatQueryRequest) => {
  return http.request<ResponseData<AiChatAnswerDTO>>(
    "post",
    "/ai/chat/query",
    { data }
  );
};

export const getAiChatStreamUrl = () => "/ai/chat/stream";
