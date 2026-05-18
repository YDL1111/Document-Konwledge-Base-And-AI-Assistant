import { reactive } from "vue";
import { isPhone } from "@pureadmin/utils";
import type { FormRules } from "element-plus";

export const REGEXP_SIX = /^\d{6}$/;

export const REGEXP_PWD =
  /^(?![0-9]+$)(?![a-z]+$)(?![A-Z]+$)(?!([^(0-9a-zA-Z)]|[()])+$)(?!^.*[\u4E00-\u9FA5].*$)([^(0-9a-zA-Z)]|[()]|[a-z]|[A-Z]|[0-9]){8,18}$/;

const passwordValidator = (value: string, callback: (error?: Error) => void) => {
  if (value === "") {
    callback(new Error("请输入密码"));
  } else if (!REGEXP_PWD.test(value)) {
    callback(
      new Error("密码需为 8-18 位，并同时包含数字、大小写字母或符号中的至少两类")
    );
  } else {
    callback();
  }
};

const codeValidator = (value: string, callback: (error?: Error) => void) => {
  if (value === "") {
    callback(new Error("请输入验证码"));
  } else if (!REGEXP_SIX.test(value)) {
    callback(new Error("请输入 6 位数字验证码"));
  } else {
    callback();
  }
};

const phoneValidator = (value: string, callback: (error?: Error) => void) => {
  if (value === "") {
    callback(new Error("请输入手机号"));
  } else if (!isPhone(value)) {
    callback(new Error("请输入正确的手机号"));
  } else {
    callback();
  }
};

const loginRules = reactive<FormRules>({
  password: [
    {
      validator: (rule, value, callback) => passwordValidator(value, callback),
      trigger: "blur"
    }
  ],
  verifyCode: [
    {
      validator: (rule, value, callback) => codeValidator(value, callback),
      trigger: "blur"
    }
  ]
});

const phoneRules = reactive<FormRules>({
  phone: [
    {
      validator: (rule, value, callback) => phoneValidator(value, callback),
      trigger: "blur"
    }
  ],
  verifyCode: [
    {
      validator: (rule, value, callback) => codeValidator(value, callback),
      trigger: "blur"
    }
  ]
});

const updateRules = reactive<FormRules>({
  phone: [
    {
      validator: (rule, value, callback) => phoneValidator(value, callback),
      trigger: "blur"
    }
  ],
  verifyCode: [
    {
      validator: (rule, value, callback) => codeValidator(value, callback),
      trigger: "blur"
    }
  ],
  password: [
    {
      validator: (rule, value, callback) => passwordValidator(value, callback),
      trigger: "blur"
    }
  ]
});

export { loginRules, phoneRules, updateRules };
