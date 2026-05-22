<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import dayjs from "dayjs";
import {
  getKnowledgeIngestTaskListApi,
  retryIngestTaskApi,
  processIngestTaskApi,
  pollIngestTaskApi,
  processPendingTasksApi,
  type KnowledgeIngestTaskDTO,
  type KnowledgeIngestTaskQuery
} from "@/api/knowledge/ingest";
import { message } from "@/utils/message";

defineOptions({
  name: "KnowledgeIngestTask"
});

const loading = ref(false);
const processing = ref(false);
const dataList = ref<KnowledgeIngestTaskDTO[]>([]);
const total = ref(0);

const searchForm = reactive<KnowledgeIngestTaskQuery>({
  taskNo: "",
  status: undefined,
  pageNum: 1,
  pageSize: 10
});

const taskTypeMap: Record<number, string> = {
  1: "首次导入",
  2: "重新导入",
  3: "重试",
  4: "删除向量"
};

const statusMap: Record<number, { label: string; type: string }> = {
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

async function handleRetry(row: KnowledgeIngestTaskDTO) {
  try {
    await retryIngestTaskApi(row.taskId);
    message("任务已重置为待处理", { type: "success" });
    loadTasks();
  } catch (error) {
    console.error(error);
    message("重试失败", { type: "error" });
  }
}

async function handleProcess(row: KnowledgeIngestTaskDTO) {
  try {
    await processIngestTaskApi(row.taskId);
    message("任务执行完成", { type: "success" });
    loadTasks();
  } catch (error) {
    console.error(error);
    message("任务执行失败", { type: "error" });
  }
}

async function handlePoll(row: KnowledgeIngestTaskDTO) {
  try {
    const { data } = await pollIngestTaskApi(row.taskId);
    const statusLabel = statusMap[data.status]?.label ?? "未知";
    message(`Python侧处理状态：${statusLabel}`, { type: "success" });
    loadTasks();
  } catch (error) {
    console.error(error);
    message("状态查询失败", { type: "error" });
  }
}

async function handleProcessPending() {
  processing.value = true;
  try {
    const { data } = await processPendingTasksApi();
    message(`已处理 ${data.processed} 个待执行任务`, { type: "success" });
    loadTasks();
  } catch (error) {
    console.error(error);
    message("批量处理失败", { type: "error" });
  } finally {
    processing.value = false;
  }
}

function handleCurrentChange(pageNum: number) {
  searchForm.pageNum = pageNum;
  loadTasks();
}

function handleSizeChange(pageSize: number) {
  searchForm.pageSize = pageSize;
  searchForm.pageNum = 1;
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
            <span>导入任务</span>
            <p class="subtext">
              管理文档同步到 AI 知识库的导入任务，支持创建、执行、重试和状态追踪。
            </p>
          </div>
          <div>
            <el-button type="primary" :loading="processing" @click="handleProcessPending">
              批量处理待执行任务
            </el-button>
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

      <el-alert
        title='提示：文档"审核通过"后会自动创建导入任务。失败的任务可以重试，处理中的任务可以查询Python侧状态。'
        type="info"
        :closable="false"
        class="tips"
      />

      <el-table :data="dataList" v-loading="loading" border>
        <el-table-column prop="taskId" label="任务ID" width="80" />
        <el-table-column prop="taskNo" label="任务编号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="documentId" label="文档ID" width="80" />
        <el-table-column prop="versionId" label="版本ID" width="80" />
        <el-table-column prop="taskType" label="任务类型" width="100">
          <template #default="{ row }">
            {{ taskTypeMap[row.taskType] || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'">
              {{ statusMap[row.status]?.label || "-" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试次数" width="90" />
        <el-table-column prop="chunkCount" label="切块数" width="80" />
        <el-table-column prop="pythonKbId" label="Python KB" width="90" />
        <el-table-column prop="pythonDocId" label="Python Doc" width="100" />
        <el-table-column prop="errorMessage" label="失败原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="startedTime" label="开始时间" min-width="160">
          <template #default="{ row }">
            {{ formatTime(row.startedTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="finishedTime" label="完成时间" min-width="160">
          <template #default="{ row }">
            {{ formatTime(row.finishedTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              link
              type="primary"
              @click="handleProcess(row)"
            >
              执行
            </el-button>
            <el-button
              v-if="row.status === 4"
              link
              type="warning"
              @click="handleRetry(row)"
            >
              重试
            </el-button>
            <el-button
              v-if="row.status === 2"
              link
              type="info"
              @click="handlePoll(row)"
            >
              查询状态
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无导入任务" />
        </template>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page="searchForm.pageNum"
          :page-size="searchForm.pageSize"
          :page-sizes="[10, 20, 50]"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
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

.tips {
  margin-bottom: 12px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
