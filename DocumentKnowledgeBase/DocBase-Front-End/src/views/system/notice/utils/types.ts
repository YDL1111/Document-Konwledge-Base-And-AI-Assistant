interface AddNoticeRequest {
  noticeId?: number;
  noticeTitle: string;
  noticeType: number;
  status: number;
  noticeContent: string;
}

interface FormProps {
  formInline: AddNoticeRequest;
}

export type { AddNoticeRequest, FormProps };
