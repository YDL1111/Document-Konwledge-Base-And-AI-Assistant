<script setup lang="ts">
import {
  onBeforeMount,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  toRaw,
  watch
} from "vue";
import Motion from "./utils/motion";
import { useRouter } from "vue-router";
import { message } from "@/utils/message";
import TypeIt from "@/components/ReTypeit";
import { useNav } from "@/layout/hooks/useNav";
import type { FormInstance, FormRules } from "element-plus";
import { useLayout } from "@/layout/hooks/useLayout";
import { rsaEncrypt } from "@/utils/crypt";
import { getTopMenu, initRouter } from "@/router/utils";
import { avatar, bg, illustration } from "./utils/static";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { useDataThemeChange } from "@/layout/hooks/useDataThemeChange";
import {
  getIsRememberMe,
  getPassword,
  removePassword,
  saveIsRememberMe,
  savePassword,
  setTokenFromBackend
} from "@/utils/auth";
import { operates } from "./utils/enums";
import { isAllEmpty, isEmail, isPhone } from "@pureadmin/utils";

import dayIcon from "@/assets/svg/day.svg?component";
import darkIcon from "@/assets/svg/dark.svg?component";
import Lock from "@iconify-icons/ri/lock-fill";
import User from "@iconify-icons/ri/user-3-fill";
import Mail from "@iconify-icons/ri/mail-line";
import Phone from "@iconify-icons/ri/smartphone-line";
import * as CommonAPI from "@/api/common/login";
import { useUserStoreHook } from "@/store/modules/user";

defineOptions({
  name: "Login"
});

const REGEXP_PWD =
  /^(?![0-9]+$)(?![a-z]+$)(?![A-Z]+$)(?!([^(0-9a-zA-Z)]|[()])+$)(?!^.*[\u4E00-\u9FA5].*$)([^(0-9a-zA-Z)]|[()]|[a-z]|[A-Z]|[0-9]){8,18}$/;

type ViewMode = "login" | "register";

const captchaCodeBase64 = ref("");
const isCaptchaOn = ref(false);
const isRegisterEnabled = ref(false);
const currentMode = ref<ViewMode>("login");
const router = useRouter();
const loading = ref(false);
const registerLoading = ref(false);
const isRememberMe = ref(false);
const loginFormRef = ref<FormInstance>();
const registerFormRef = ref<FormInstance>();

const { initStorage } = useLayout();
initStorage();

const { dataTheme, dataThemeChange } = useDataThemeChange();
dataThemeChange();

const { title } = useNav();

const loginForm = reactive({
  username: "admin",
  password: getPassword(),
  captchaCode: "",
  captchaCodeKey: ""
});

const registerForm = reactive({
  username: "",
  nickname: "",
  email: "",
  phoneNumber: "",
  password: "",
  confirmPassword: ""
});

const loginRules = reactive<FormRules>({
  username: [
    {
      required: true,
      message: "请输入用户名",
      trigger: "blur"
    }
  ],
  password: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback(new Error("请输入密码"));
        } else if (!REGEXP_PWD.test(value)) {
          callback(new Error("密码需为 8-18 位，并至少包含两种字符类型"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ],
  captchaCode: [
    {
      validator: (rule, value, callback) => {
        if (isCaptchaOn.value && !value) {
          callback(new Error("请输入验证码"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ]
});

const registerRules = reactive<FormRules>({
  username: [
    {
      required: true,
      message: "请输入注册用户名",
      trigger: "blur"
    }
  ],
  nickname: [
    {
      required: true,
      message: "请输入显示名称",
      trigger: "blur"
    }
  ],
  email: [
    {
      validator: (rule, value, callback) => {
        if (value && !isEmail(value)) {
          callback(new Error("请输入正确的邮箱地址"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ],
  phoneNumber: [
    {
      validator: (rule, value, callback) => {
        if (value && !isPhone(value)) {
          callback(new Error("请输入正确的手机号"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ],
  password: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback(new Error("请输入登录密码"));
        } else if (!REGEXP_PWD.test(value)) {
          callback(new Error("密码需为 8-18 位，并至少包含两种字符类型"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ],
  confirmPassword: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback(new Error("请再次输入密码"));
        } else if (value !== registerForm.password) {
          callback(new Error("两次输入的密码不一致"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ]
});

const activeOperateTitle = (mode: ViewMode) => {
  return operates.find(item => item.value === mode)?.title ?? "";
};

const switchMode = (mode: ViewMode) => {
  currentMode.value = mode;
};

const onLogin = async (formEl: FormInstance | undefined) => {
  if (!formEl) return;
  loading.value = true;
  await formEl.validate((valid, fields) => {
    if (valid) {
      CommonAPI.loginByPassword({
        username: loginForm.username,
        password: rsaEncrypt(loginForm.password),
        captchaCode: loginForm.captchaCode,
        captchaCodeKey: loginForm.captchaCodeKey
      })
        .then(({ data }) => {
          setTokenFromBackend(data);
          initRouter().then(() => {
            router.push(getTopMenu(true).path);
            message("登录成功", { type: "success" });
          });
          if (isRememberMe.value) {
            savePassword(loginForm.password);
          }
        })
        .catch(() => {
          loading.value = false;
          getCaptchaCode();
        });
    } else {
      loading.value = false;
      return fields;
    }
  });
};

const onRegister = async (formEl: FormInstance | undefined) => {
  if (!formEl) return;
  registerLoading.value = true;
  await formEl.validate((valid, fields) => {
    if (valid) {
      CommonAPI.register({
        username: registerForm.username,
        nickname: registerForm.nickname,
        email: registerForm.email || undefined,
        phoneNumber: registerForm.phoneNumber || undefined,
        password: rsaEncrypt(registerForm.password)
      })
        .then(() => {
          message("注册成功，请使用新账号登录", { type: "success" });
          loginForm.username = registerForm.username;
          loginForm.password = "";
          registerFormRef.value?.resetFields();
          switchMode("login");
        })
        .catch(() => {
          registerLoading.value = false;
        })
        .finally(() => {
          registerLoading.value = false;
        });
    } else {
      registerLoading.value = false;
      return fields;
    }
  });
};

function onkeypress({ code }: KeyboardEvent) {
  if (code === "Enter") {
    if (currentMode.value === "login") {
      onLogin(loginFormRef.value);
    } else {
      onRegister(registerFormRef.value);
    }
  }
}

async function getCaptchaCode() {
  if (isCaptchaOn.value) {
    await CommonAPI.getCaptchaCode().then(res => {
      captchaCodeBase64.value = `data:image/gif;base64,${res.data.captchaCodeImg}`;
      loginForm.captchaCodeKey = res.data.captchaCodeKey;
    });
  }
}

function onForgotPassword() {
  message("忘记密码功能暂未开放，请联系系统管理员处理。", {
    type: "warning"
  });
}

watch(isRememberMe, newVal => {
  saveIsRememberMe(newVal);
  if (newVal === false) {
    removePassword();
  }
});

watch(
  () => registerForm.password,
  () => {
    if (!isAllEmpty(registerForm.confirmPassword)) {
      registerFormRef.value?.validateField("confirmPassword");
    }
  }
);

onBeforeMount(async () => {
  await CommonAPI.getConfig().then(res => {
    isCaptchaOn.value = res.data.isCaptchaOn;
    isRegisterEnabled.value = res.data.isRegisterEnabled;
    useUserStoreHook().SET_DICTIONARY(res.data.dictionary);
  });

  await getCaptchaCode();

  isRememberMe.value = getIsRememberMe();
  if (isRememberMe.value) {
    loginForm.password = getPassword();
  }
});

onMounted(() => {
  window.document.addEventListener("keypress", onkeypress);
});

onBeforeUnmount(() => {
  window.document.removeEventListener("keypress", onkeypress);
});
</script>

<template>
  <div class="select-none">
    <img :src="bg" class="wave" />
    <div class="absolute flex-c right-5 top-3">
      <el-switch
        v-model="dataTheme"
        :active-icon="dayIcon"
        :inactive-icon="darkIcon"
        inline-prompt
        @change="dataThemeChange"
      />
    </div>
    <div class="login-container">
      <div class="img">
        <component :is="toRaw(illustration)" />
      </div>
      <div class="login-box">
        <div class="login-form">
          <avatar class="avatar" />
          <Motion>
            <h2 class="outline-none">
              <TypeIt :cursor="false" :speed="150" :values="[title]" />
            </h2>
          </Motion>
          <Motion :delay="50">
            <p class="form-subtitle">
              {{ currentMode === "login" ? "企业文档知识库后台登录" : "申请企业知识库账号" }}
            </p>
          </Motion>

          <Motion v-if="isRegisterEnabled" :delay="80">
            <div class="operate-tabs">
              <button
                v-for="item in operates"
                :key="item.value"
                :class="['operate-tab', { active: currentMode === item.value }]"
                type="button"
                @click="switchMode(item.value)"
              >
                {{ item.title }}
              </button>
            </div>
          </Motion>

          <el-form
            v-if="currentMode === 'login'"
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            size="large"
          >
            <Motion :delay="100">
              <el-form-item prop="username">
                <el-input
                  v-model="loginForm.username"
                  :prefix-icon="useRenderIcon(User)"
                  clearable
                  placeholder="用户名"
                />
              </el-form-item>
            </Motion>

            <Motion :delay="150">
              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  :prefix-icon="useRenderIcon(Lock)"
                  clearable
                  placeholder="密码"
                  show-password
                />
              </el-form-item>
            </Motion>

            <Motion :delay="200">
              <el-form-item v-if="isCaptchaOn" prop="captchaCode">
                <el-input
                  v-model="loginForm.captchaCode"
                  :prefix-icon="useRenderIcon('ri:shield-keyhole-line')"
                  clearable
                  placeholder="验证码"
                >
                  <template #append>
                    <el-image
                      :src="captchaCodeBase64"
                      style="justify-content: center; width: 120px; height: 40px"
                      @click="getCaptchaCode"
                    >
                      <template #error>
                        <span>Loading</span>
                      </template>
                    </el-image>
                  </template>
                </el-input>
              </el-form-item>
            </Motion>

            <Motion :delay="250">
              <el-form-item>
                <div class="w-full h-[20px] flex justify-between items-center">
                  <el-checkbox v-model="isRememberMe">记住密码</el-checkbox>
                  <el-button link disabled type="primary" @click="onForgotPassword">
                    忘记密码
                  </el-button>
                </div>
                <el-button
                  :loading="loading"
                  class="w-full mt-4"
                  size="default"
                  type="primary"
                  @click="onLogin(loginFormRef)"
                >
                  {{ activeOperateTitle("login") }}
                </el-button>
              </el-form-item>
            </Motion>
          </el-form>

          <el-form
            v-else
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            size="large"
          >
            <Motion :delay="100">
              <el-form-item prop="username">
                <el-input
                  v-model="registerForm.username"
                  :prefix-icon="useRenderIcon(User)"
                  clearable
                  placeholder="注册用户名"
                />
              </el-form-item>
            </Motion>

            <Motion :delay="130">
              <el-form-item prop="nickname">
                <el-input
                  v-model="registerForm.nickname"
                  :prefix-icon="useRenderIcon(User)"
                  clearable
                  placeholder="显示名称"
                />
              </el-form-item>
            </Motion>

            <Motion :delay="160">
              <el-form-item prop="email">
                <el-input
                  v-model="registerForm.email"
                  :prefix-icon="useRenderIcon(Mail)"
                  clearable
                  placeholder="邮箱（选填）"
                />
              </el-form-item>
            </Motion>

            <Motion :delay="190">
              <el-form-item prop="phoneNumber">
                <el-input
                  v-model="registerForm.phoneNumber"
                  :prefix-icon="useRenderIcon(Phone)"
                  clearable
                  placeholder="手机号（选填）"
                />
              </el-form-item>
            </Motion>

            <Motion :delay="220">
              <el-form-item prop="password">
                <el-input
                  v-model="registerForm.password"
                  :prefix-icon="useRenderIcon(Lock)"
                  clearable
                  placeholder="登录密码"
                  show-password
                />
              </el-form-item>
            </Motion>

            <Motion :delay="250">
              <el-form-item prop="confirmPassword">
                <el-input
                  v-model="registerForm.confirmPassword"
                  :prefix-icon="useRenderIcon(Lock)"
                  clearable
                  placeholder="确认密码"
                  show-password
                />
              </el-form-item>
            </Motion>

            <Motion :delay="280">
              <el-form-item>
                <el-button
                  :loading="registerLoading"
                  class="w-full"
                  type="primary"
                  @click="onRegister(registerFormRef)"
                >
                  {{ activeOperateTitle("register") }}
                </el-button>
                <el-button class="w-full mt-3" plain @click="switchMode('login')">
                  返回登录
                </el-button>
              </el-form-item>
            </Motion>
          </el-form>
        </div>
      </div>
    </div>
    <div class="flex items-center justify-center h-full">
      <div class="flex flex-col items-center justify-center mb-3">
        <span>Copyright © 2024-2026 DocBase All Rights Reserved.</span>
        <el-link
          href="https://beian.miit.gov.cn"
          rel="external nofollow"
          target="_blank"
          type="primary"
        >
          粤ICP备2022018106号-2
        </el-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url("@/style/login.css");
</style>

<style lang="scss" scoped>
:deep(.el-input-group__append, .el-input-group__prepend) {
  padding: 0;
}

.form-subtitle {
  margin: 12px 0 20px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  text-align: center;
}

.operate-tabs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 20px;
  padding: 6px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 999px;
}

.operate-tab {
  height: 38px;
  color: #475569;
  background: transparent;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.operate-tab.active {
  color: #0f172a;
  font-weight: 600;
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  box-shadow: 0 8px 18px rgba(59, 130, 246, 0.18);
}
</style>
