<script setup lang="ts">
import { computed, ref } from "vue";
import { PureTableBar } from "@/components/RePureTableBar";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { useUserStoreHook } from "@/store/modules/user";
import { CommonUtils } from "@/utils/common";
import Delete from "@iconify-icons/ep/delete";
import Refresh from "@iconify-icons/ep/refresh";
import Search from "@iconify-icons/ep/search";
import View from "@iconify-icons/ep/view";
import { useOperationLogHook } from "./utils/hook";

defineOptions({
  name: "SystemOperationLog"
});

const businessTypeList =
  useUserStoreHook().dictionaryList["sysOperationLog.businessType"] ?? [];
const operationStatusList =
  useUserStoreHook().dictionaryList["sysOperationLog.status"] ?? [];

const tableRef = ref();
const searchFormRef = ref();

const {
  searchFormParams,
  pageLoading,
  columns,
  dataList,
  pagination,
  timeRange,
  defaultSort,
  onSearch,
  resetForm,
  exportAllExcel,
  openDialog,
  handleClean,
  handleDelete,
  handleBulkDelete,
  handleSelectionChange,
  handlePageSizeChange,
  handlePageCurrentChange,
  handleSortChange
} = useOperationLogHook();

const timeRangeModel = computed({
  get: () => timeRange.value as any,
  set: value => {
    timeRange.value = (value ?? []) as any;
  }
});
</script>

<template>
  <div class="main">
    <el-form
      ref="searchFormRef"
      :inline="true"
      :model="searchFormParams"
      class="search-form bg-bg_color w-[99/100] pl-8 pt-[12px]"
    >
      <el-form-item label="业务模块" prop="requestModule">
        <el-input
          v-model="searchFormParams.requestModule"
          placeholder="请输入业务模块"
          clearable
          class="!w-[200px]"
        />
      </el-form-item>
      <el-form-item label="业务类型" prop="businessType">
        <el-select
          v-model="searchFormParams.businessType"
          placeholder="请选择业务类型"
          clearable
          class="!w-[180px]"
        >
          <el-option
            v-for="dict in businessTypeList"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="操作人" prop="username">
        <el-input
          v-model="searchFormParams.username"
          placeholder="请输入操作人"
          clearable
          class="!w-[180px]"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="searchFormParams.status"
          placeholder="请选择状态"
          clearable
          class="!w-[180px]"
        >
          <el-option
            v-for="dict in operationStatusList"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <label class="el-form-item__label font-bold">操作时间</label>
        <el-date-picker
          v-model="timeRangeModel"
          class="!w-[240px]"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          :icon="useRenderIcon(Search)"
          :loading="pageLoading"
          @click="onSearch"
        >
          查询
        </el-button>
        <el-button
          :icon="useRenderIcon(Refresh)"
          @click="resetForm(searchFormRef, tableRef)"
        >
          重置
        </el-button>
      </el-form-item>
    </el-form>

    <PureTableBar title="操作日志" :columns="columns" @refresh="onSearch">
      <template #buttons>
        <el-button
          type="danger"
          :icon="useRenderIcon(Delete)"
          @click="handleBulkDelete(tableRef)"
        >
          批量删除
        </el-button>
        <el-button
          type="warning"
          @click="handleClean"
        >
          清空日志
        </el-button>
        <el-button
          type="primary"
          @click="CommonUtils.exportExcel(columns, dataList, '操作日志')"
        >
          导出当前页
        </el-button>
        <el-button type="primary" @click="() => exportAllExcel()">
          导出全部
        </el-button>
      </template>

      <template #default="{ size, dynamicColumns }">
        <pure-table
          ref="tableRef"
          border
          align-whole="center"
          showOverflowTooltip
          table-layout="auto"
          :loading="pageLoading"
          :size="size"
          adaptive
          :data="dataList"
          :columns="dynamicColumns"
          :default-sort="defaultSort"
          :pagination="pagination"
          :pagination-small="size === 'small'"
          :header-cell-style="{
            background: 'var(--el-table-row-hover-bg-color)',
            color: 'var(--el-text-color-primary)'
          }"
          @page-size-change="handlePageSizeChange"
          @page-current-change="handlePageCurrentChange"
          @sort-change="sort => handleSortChange(sort as any)"
          @selection-change="handleSelectionChange"
        >
          <template #operation="{ row }">
            <el-button
              class="reset-margin"
              link
              type="primary"
              :size="size"
              :icon="useRenderIcon(View)"
              @click="openDialog(row)"
            >
              详情
            </el-button>
            <el-popconfirm
              :title="`确认删除操作日志 #${row.operationId} 吗？`"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button
                  class="reset-margin"
                  link
                  type="danger"
                  :size="size"
                  :icon="useRenderIcon(Delete)"
                >
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </pure-table>
      </template>
    </PureTableBar>
  </div>
</template>

<style scoped lang="scss">
:deep(.el-dropdown-menu__item i) {
  margin: 0;
}

.search-form {
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}
</style>
