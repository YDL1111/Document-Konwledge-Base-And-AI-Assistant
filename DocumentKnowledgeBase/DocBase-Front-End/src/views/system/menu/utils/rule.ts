import { reactive } from "vue";
import type { FormRules } from "element-plus";

export const formRules = reactive(<FormRules>{
  menuName: [{ required: true, message: "菜单名称为必填项", trigger: "blur" }]
});
