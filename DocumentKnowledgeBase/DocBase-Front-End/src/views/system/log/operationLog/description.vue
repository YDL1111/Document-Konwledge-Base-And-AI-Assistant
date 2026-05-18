<script setup lang="ts">
import { computed } from "vue";
import { useUserStoreHook } from "@/store/modules/user";
import type { OperationLogDTO } from "@/api/system/log";

const props = defineProps<OperationLogDTO>();

const operationLogStatusMap =
  useUserStoreHook().dictionaryMap["sysOperationLog.status"] ?? {};

const currentStatus = computed(() => {
  return operationLogStatusMap[props.status] ?? {
    cssTag: "info",
    label: "未知"
  };
});

const currentStatusType = computed<
  "" | "success" | "warning" | "info" | "danger"
>(() => {
  const tag = currentStatus.value.cssTag;
  if (
    tag === "success" ||
    tag === "warning" ||
    tag === "info" ||
    tag === "danger"
  ) {
    return tag;
  }
  return "info";
});
</script>

<template>
  <el-descriptions
    direction="horizontal"
    :column="2"
    :label-style="'white-space:nowrap;'"
    :content-style="'word-break:break-all;'"
    size="large"
  >
    <el-descriptions-item label="日志ID:" :width="'25%'">
      {{ props.operationId }}
    </el-descriptions-item>
    <el-descriptions-item label="业务模块:" :width="'25%'">
      {{ props.requestModule }}
    </el-descriptions-item>
    <el-descriptions-item :span="2" label="业务类型:">
      {{ props.businessTypeStr }}
    </el-descriptions-item>
    <el-descriptions-item label="操作人:">
      {{ props.username }}
    </el-descriptions-item>
    <el-descriptions-item label="操作人ID:">
      {{ props.userId }}
    </el-descriptions-item>
    <el-descriptions-item label="操作人类型:">
      {{ props.operatorTypeStr }}
    </el-descriptions-item>
    <el-descriptions-item label="所属部门:">
      {{ props.deptName }}
    </el-descriptions-item>
    <el-descriptions-item label="操作IP:">
      {{ props.operatorIp }}
    </el-descriptions-item>
    <el-descriptions-item :span="2" label="操作地点:">
      {{ props.operatorLocation }}
    </el-descriptions-item>
    <el-descriptions-item label="请求地址:">
      {{ props.requestUrl }}
    </el-descriptions-item>
    <el-descriptions-item label="请求方式:">
      {{ props.requestMethod }}
    </el-descriptions-item>
    <el-descriptions-item :span="2" label="请求参数:">
      <el-text>{{ props.operationParam }}</el-text>
    </el-descriptions-item>
    <el-descriptions-item :span="2" label="调用方法:">
      <el-text>{{ props.calledMethod }}</el-text>
    </el-descriptions-item>
    <el-descriptions-item :span="2" label="返回结果:">
      <el-text>{{ props.operationResult }}</el-text>
    </el-descriptions-item>
    <el-descriptions-item :span="2" label="异常信息:">
      <el-text>{{ props.errorStack }}</el-text>
    </el-descriptions-item>
    <el-descriptions-item label="状态:">
      <el-tag :type="currentStatusType" effect="plain">
        {{ currentStatus.label }}
      </el-tag>
    </el-descriptions-item>
    <el-descriptions-item label="操作时间:">
      {{ props.operationTime }}
    </el-descriptions-item>
  </el-descriptions>
</template>

<style>
.el-descriptions {
  margin-top: 20px;
}
</style>
