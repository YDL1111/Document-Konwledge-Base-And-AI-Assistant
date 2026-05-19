import { reactive } from "vue";
import type { FormRules } from "element-plus";

export const formRules = reactive(<FormRules>{
  noticeTitle: [{ required: true, message: "公告标题为必填项", trigger: "blur" }],
  noticeType: [{ required: true, message: "公告类型为必填项", trigger: "change" }],
  noticeContent: [{ required: true, message: "公告内容为必填项", trigger: "blur" }]
});
