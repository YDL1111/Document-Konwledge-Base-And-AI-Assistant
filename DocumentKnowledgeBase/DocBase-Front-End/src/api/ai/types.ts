export interface AiChatQueryRequest {
  sessionId?: number;
  question: string;
  kbId?: number;
  categoryId?: number;
  documentId?: number;
}

export interface SourceInfo {
  filename: string;
  page?: number;
  score: number;
  content: string;
}

export interface AiChatAnswerDTO {
  sessionId: number;
  answer: string;
  sources: SourceInfo[];
}

export interface AiChatSessionDTO {
  sessionId: number;
  sessionTitle: string;
  userId: number;
  deptId: number;
  lastMessageTime: number | string;
  status: number;
}

export interface AiChatMessageDTO {
  messageId: number;
  sessionId: number;
  messageRole: number;
  messageContent: string;
  sources: SourceInfo[] | null;
  createTime: number | string;
}
