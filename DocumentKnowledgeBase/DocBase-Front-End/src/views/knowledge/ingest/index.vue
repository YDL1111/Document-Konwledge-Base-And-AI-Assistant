<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import dayjs from "dayjs";
import {
  getKnowledgeIngestTaskListApi,
  type KnowledgeIngestTaskDTO,
  type KnowledgeIngestTaskQuery
} from "@/api/knowledge/ingest";
import { message } from "@/utils/message";

defineOptions({
  name: "KnowledgeIngestTask"
});

const loading = ref(false);
const dataList = ref<KnowledgeIngestTaskDTO[]>([]);
const total = ref(0);

const searchForm = reactive<KnowledgeIngestTaskQuery>({
  taskNo: "",
  status: undefined,
  pageNum: 1,
  pageSize: 10
});

const taskTypeMap = {
  1: "首次入库",
  2: "增量更新",
  3: "重试",
  4: "删除向量"
};

const statusMap = {
  1: { label: "待处理", type: "info" },
  2: { label: "处理中", type: "warning" },
  3: { label: "成功", type: "success" },
  4: { label: "失败", type: "danger" }
};

async function loadTasks() {
  loading.value = true;
  try {
    const { data } = await getKnowledgeIngestTaskListApi(searchForm);
    dataList.value = data.rows ?? [];
    total.value = data.total ?? 0;
  } catch (error) {
    console.error(error);
    message("入库任务加载失败，请检查后端接口日志", { type: "error" });
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  searchForm.pageNum = 1;
  loadTasks();
}

function resetSearch() {
  searchForm.taskNo = "";
  searchForm.status = undefined;
  searchForm.pageNum = 1;
  searchForm.pageSize = 10;
  loadTasks();
}

function formatTime(value?: string) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm:ss") : "-";
}

onMounted(() => {
  loadTasks();
});
</script>

<template>
  <div class="main">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <span>入库任务</span>
            <p class="subtext">这里展示文档解析、切块、入库处理的任务状态，先打通任务中心的查看能力。</p>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="任务编号">
          <el-input
            v-model="searchForm.taskNo"
            clearable
            placeholder="请输入任务编号"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            clearable
            placeholder="请选择状态"
            style="width: 140px"
          >
            <el-option label="待处理" :value="1" />
            <el-option label="处理中" :value="2" />
            <el-option label="成功" :value="3" />
            <el-option label="失败" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="dataList" v-loading="loading" border>
        <el-table-column prop="taskId" label="任务ID" width="96" />
        <el-table-column prop="taskNo" label="任务编号" min-width="180" />
        <el-table-column prop="documentId" label="文档ID" width="96" />
        <el-table-column prop="versionId" label="版本ID" width="96" />
        <el-table-column prop="taskType" label="任务类型" width="110">
          <template #default="{ row }">
            {{ taskTypeMap[row.taskType] || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'">
              {{ statusMap[row.status]?.label || "-" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试次数" width="100" />
        <el-table-column prop="chunkCount" label="切块数量" width="100" />
        <el-table-column prop="traceId" label="TraceId" min-width="180" show-overflow-tooltip />
        <el-table-column prop="errorMessage" label="失败原因" min-width="220" show-overflow-tooltip />
        <el-table-column prop="startedTime" label="开始时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.startedTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="finishedTime" label="完成时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.finishedTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  font-size: 16px;
  font-weight: 600;
}

.subtext {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.search-form {
  margin-bottom: 12px;
}
</style>
