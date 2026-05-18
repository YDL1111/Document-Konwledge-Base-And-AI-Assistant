<script setup lang="ts">
import { reactive, ref } from "vue";
import { updateUserProfileApi, UserProfileRequest } from "@/api/system/user";
import { message } from "@/utils/message";
import { FormInstance, FormRules } from "element-plus";
import type { CurrentUserInfoDTO } from "@/api/common/login";

defineOptions({
  name: "SystemUserProfile"
});

const userRef = ref<FormInstance>();

const props = defineProps<{
  user: CurrentUserInfoDTO;
}>();

const userModel = reactive<UserProfileRequest>({
  nickName: props.user.nickName ?? "",
  phoneNumber: props.user.phoneNumber ?? "",
  email: props.user.email ?? "",
  sex: props.user.sex
});

const rules: FormRules = {
  nickName: [{ required: true, message: "用户昵称不能为空", trigger: "blur" }],
  email: [
    { required: true, message: "邮箱地址不能为空", trigger: "blur" },
    {
      type: "email",
      message: "请输入正确的邮箱地址",
      trigger: ["blur", "change"]
    }
  ],
  phoneNumber: [
    { required: true, message: "手机号不能为空", trigger: "blur" },
    {
      pattern: /^1[3-9][0-9]\d{8}$/,
      message: "请输入正确的手机号",
      trigger: "blur"
    }
  ]
};

function submit() {
  userRef.value?.validate(valid => {
    if (valid) {
      updateUserProfileApi(userModel).then(() => {
        message("修改成功", {
          type: "success"
        });
      });
    }
  });
}
</script>

<template>
  <el-form ref="userRef" :model="userModel" :rules="rules" label-width="80px">
    <el-form-item label="用户昵称" prop="nickName">
      <el-input v-model="userModel.nickName" maxlength="30" />
    </el-form-item>
    <el-form-item label="手机号" prop="phoneNumber">
      <el-input v-model="userModel.phoneNumber" maxlength="11" />
    </el-form-item>
    <el-form-item label="邮箱" prop="email">
      <el-input v-model="userModel.email" maxlength="50" />
    </el-form-item>
    <el-form-item label="性别" prop="sex">
      <el-radio-group v-model="userModel.sex">
        <el-radio :label="0">男</el-radio>
        <el-radio :label="1">女</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="submit">保存</el-button>
    </el-form-item>
  </el-form>
</template>
