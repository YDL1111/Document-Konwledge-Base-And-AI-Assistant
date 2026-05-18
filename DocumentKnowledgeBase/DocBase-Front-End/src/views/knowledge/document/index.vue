<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import dayjs from "dayjs";
import type {
  FormInstance,
  FormRules,
  UploadFile,
  UploadFiles,
  UploadUserFile
} from "element-plus";
import { hasAuth } from "@/router/utils";
import { getToken } from "@/utils/auth";
import { formatToken } from "@/utils/auth";
import { handleTree } from "@/utils/tree";
import { message } from "@/utils/message";
import {
  addKnowledgeDocumentApi,
  auditKnowledgeDocumentApi,
  getKnowledgeDocumentDetailApi,
  getKnowledgeDocumentListApi,
  getKnowledgeDocumentPreviewApi,
  type KnowledgeDocumentAddRequest,
  type KnowledgeDocumentAuditRequest,
  type KnowledgeDocumentDTO,
  type KnowledgeDocumentDetailDTO,
  type KnowledgeDocumentQuery
} from "@/api/knowledge/document";
import {
  getKnowledgeCategoryListApi,
  type KnowledgeCategoryDTO
} from "@/api/knowledge/category";

defineOptions({
  name: "KnowledgeDocument"
});

const route = useRoute();

const loading = ref(false);
const submitting = ref(false);
const detailLoading = ref(false);
const auditSubmitting = ref(false);
const dialogVisible = ref(false);
const detailVisible = ref(false);
const auditVisible = ref(false);
const dataList = ref<KnowledgeDocumentDTO[]>([]);
const total = ref(0);
const categoryOptions = ref<KnowledgeCategoryDTO[]>([]);
const formRef = ref<FormInstance>();
const auditFormRef = ref<FormInstance>();
const fileList = ref<UploadUserFile[]>([]);
const selectedFile = ref<File>();
const detailData = ref<KnowledgeDocumentDetailDTO>();
const auditTarget = ref<KnowledgeDocumentDTO>();

const canAudit = computed(() => hasAuth("knowledge:document:audit"));
const isPendingAuditFilter = computed(() => searchForm.status === 2);

const categoryTreeOptions = computed(() => {
  const treeSource = categoryOptions.value.map(item => ({ ...item }));
  return handleTree(treeSource, "categoryId", "parentId", "children");
});

const searchForm = reactive<KnowledgeDocumentQuery>({
  title: "",
  status: undefined,
  visibility: undefined,
  pageNum: 1,
  pageSize: 10
});

const formModel = reactive<KnowledgeDocumentAddRequest>({
  categoryId: undefined as unknown as number,
  title: "",
  docCode: "",
  summary: "",
  tags: "",
  visibility: 2
});

const auditForm = reactive<KnowledgeDocumentAuditRequest>({
  approved: 1,
  auditRemark: ""
});

const rules: FormRules = {
  categoryId: [{ required: true, message: "请选择分类", trigger: "change" }],
  title: [{ required: true, message: "请输入文档标题", trigger: "blur" }],
  visibility: [{ required: true, message: "请选择可见范围", trigger: "change" }]
};

const auditRules: FormRules = {
  approved: [{ required: true, message: "请选择审批结果", trigger: "change" }]
};

const statusMap: Record<
  number,
  { label: string; type: "" | "success" | "info" | "warning" | "danger" }
> = {
  1: { label: "草稿", type: "info" },
  2: { label: "审核中", type: "warning" },
  3: { label: "已发布", type: "success" },
  4: { label: "已驳回", type: "danger" },
  5: { label: "已归档", type: "" }
};

const visibilityMap: Record<number, string> = {
  1: "全员可见",
  2: "本部门可见",
  3: "仅本人可见"
};

const parseStatusMap: Record<number, string> = {
  1: "待解析",
  2: "解析中",
  3: "解析成功",
  4: "解析失败"
};

const auditResultMap: Record<number, string> = {
  1: "通过",
  0: "驳回"
};

async function loadDocuments() {
  loading.value = true;
  try {
    const { data } = await getKnowledgeDocumentListApi(searchForm);
    dataList.value = data.rows ?? [];
    total.value = data.total ?? 0;
  } catch (error) {
    console.error(error);
    message("文档列表加载失败，请检查后端接口日志", { type: "error" });
  } finally {
    loading.value = false;
  }
}

async function loadCategories() {
  try {
    const { data } = await getKnowledgeCategoryListApi({
      pageNum: 1,
      pageSize: 500,
      status: 1
    });
    categoryOptions.value = data.rows ?? [];
  } catch (error) {
    console.error(error);
    message("分类数据加载失败，请检查后端接口日志", { type: "error" });
  }
}

function handleSearch() {
  searchForm.pageNum = 1;
  loadDocuments();
}

function resetSearch() {
  searchForm.title = "";
  searchForm.status = undefined;
  searchForm.visibility = undefined;
  searchForm.pageNum = 1;
  searchForm.pageSize = 10;
  loadDocuments();
}

function filterPendingAudit() {
  searchForm.status = 2;
  searchForm.pageNum = 1;
  loadDocuments();
}

function clearPendingAuditFilter() {
  searchForm.status = undefined;
  searchForm.pageNum = 1;
  loadDocuments();
}

function handlePageChange(pageNum: number) {
  searchForm.pageNum = pageNum;
  loadDocuments();
}

function handleSizeChange(pageSize: number) {
  searchForm.pageSize = pageSize;
  searchForm.pageNum = 1;
  loadDocuments();
}

function openAddDialog() {
  dialogVisible.value = true;
}

function resetForm() {
  formModel.categoryId = undefined as unknown as number;
  formModel.title = "";
  formModel.docCode = "";
  formModel.summary = "";
  formModel.tags = "";
  formModel.visibility = 2;
  selectedFile.value = undefined;
  fileList.value = [];
  formRef.value?.clearValidate();
}

function handleDialogClosed() {
  resetForm();
}

function handleFileChange(uploadFile: UploadFile, uploadFiles: UploadFiles) {
  selectedFile.value = uploadFile.raw;
  fileList.value = uploadFiles.slice(-1).map(item => ({
    name: item.name,
    status: item.status,
    percentage: item.percentage,
    size: item.size,
    uid: item.uid
  }));
}

function handleFileRemove() {
  selectedFile.value = undefined;
  fileList.value = [];
}

async function submitForm() {
  if (!formRef.value) return;
  if (!selectedFile.value) {
    message("请先选择要上传的文档文件", { type: "warning" });
    return;
  }
  await formRef.value.validate(async valid => {
    if (!valid) return;
    submitting.value = true;
    try {
      await addKnowledgeDocumentApi({ ...formModel }, selectedFile.value as File);
      message("文档已提交，当前状态为审核中", { type: "success" });
      dialogVisible.value = false;
      resetForm();
      loadDocuments();
    } catch (error) {
      console.error(error);
      message("新增文档失败，请检查后端接口日志", { type: "error" });
    } finally {
      submitting.value = false;
    }
  });
}

async function openDetail(row: KnowledgeDocumentDTO) {
  detailVisible.value = true;
  detailLoading.value = true;
  detailData.value = undefined;
  try {
    const { data } = await getKnowledgeDocumentDetailApi(row.documentId);
    detailData.value = data;
  } catch (error) {
    console.error(error);
    message("文档详情加载失败，请检查后端接口日志", { type: "error" });
    detailVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
}

function openAuditDialog(row: KnowledgeDocumentDTO) {
  auditTarget.value = row;
  auditForm.approved = 1;
  auditForm.auditRemark = row.auditRemark || "";
  auditVisible.value = true;
  auditFormRef.value?.clearValidate();
}

function handleAuditClosed() {
  auditTarget.value = undefined;
  auditForm.approved = 1;
  auditForm.auditRemark = "";
}

async function submitAudit() {
  if (!auditTarget.value || !auditFormRef.value) return;
  await auditFormRef.value.validate(async valid => {
    if (!valid) return;
    auditSubmitting.value = true;
    try {
      await auditKnowledgeDocumentApi(auditTarget.value.documentId, {
        approved: auditForm.approved,
        auditRemark: auditForm.auditRemark
      });
      message(auditForm.approved === 1 ? "审批通过成功" : "审批驳回成功", {
        type: "success"
      });
      const currentDocumentId = auditTarget.value.documentId;
      auditVisible.value = false;
      await loadDocuments();
      if (
        detailVisible.value &&
        detailData.value?.documentId === currentDocumentId
      ) {
        const currentTarget =
          dataList.value.find(item => item.documentId === currentDocumentId) ??
          ({ documentId: currentDocumentId } as KnowledgeDocumentDTO);
        await openDetail(currentTarget);
      }
    } catch (error) {
      console.error(error);
      message("文档审批失败，请检查后端接口日志", { type: "error" });
    } finally {
      auditSubmitting.value = false;
    }
  });
}

async function previewCurrentDocument() {
  if (!detailData.value?.documentId) {
    message("当前文档信息不完整", { type: "warning" });
    return;
  }
  try {
    const { data } = await getKnowledgeDocumentPreviewApi(detailData.value.documentId);
    if (!data) {
      message("当前文档暂无可预览地址", { type: "warning" });
      return;
    }
    window.open(data, "_blank");
  } catch (error) {
    console.error(error);
    message("文档预览失败，请检查后端接口日志", { type: "error" });
  }
}

async function downloadCurrentDocument() {
  if (!detailData.value?.documentId) {
    message("当前文档信息不完整", { type: "warning" });
    return;
  }
  const downloadUrl =
    detailData.value.currentVersionStorageUrl ||
    detailData.value.currentVersion?.storageUrl;
  if (!downloadUrl) {
    message("当前文档暂无可下载地址", { type: "warning" });
    return;
  }
  try {
    const tokenInfo = getToken();
    const response = await fetch(downloadUrl, {
      method: "GET",
      headers: tokenInfo?.token
        ? {
            Authorization: formatToken(tokenInfo.token)
          }
        : undefined
    });
    if (!response.ok) {
      throw new Error(`download failed: ${response.status}`);
    }
    const blob = await response.blob();
    const objectUrl = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = objectUrl;
    link.download =
      detailData.value.currentVersion?.fileName || `${detailData.value.title}.bin`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(objectUrl);
  } catch (error) {
    console.error(error);
    message("文档下载失败，请检查文件地址或后端静态资源配置", {
      type: "error"
    });
  }
}

function formatTime(value?: string) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm:ss") : "-";
}

function formatFileSize(size?: number) {
  if (size === undefined || size === null) return "-";
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(2)} MB`;
}

function getStatusMeta(status?: number) {
  return (status && statusMap[status]) || {
    label: "未知",
    type: "info" as const
  };
}

function getCategoryName(categoryId?: number) {
  const category = categoryOptions.value.find(item => item.categoryId === categoryId);
  return category?.categoryName ?? "-";
}

onMounted(() => {
  if (route.query.status === "2") {
    searchForm.status = 2;
  }
  loadCategories();
  loadDocuments();
});
</script>

<template>
  <div class="main">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <span>文档管理</span>
            <p class="subtext">
              支持企业文档上传、查看详情、版本跟踪、审核流转，以及按可见范围控制普通用户访问。
            </p>
          </div>
          <div class="header-actions">
            <el-button v-if="canAudit" plain @click="filterPendingAudit">
              待审核列表
            </el-button>
            <el-button type="primary" @click="openAddDialog">新增文档</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="文档标题">
          <el-input
            v-model="searchForm.title"
            clearable
            placeholder="请输入文档标题"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="文档状态">
          <el-select
            v-model="searchForm.status"
            clearable
            placeholder="请选择文档状态"
            style="width: 140px"
          >
            <el-option label="草稿" :value="1" />
            <el-option label="审核中" :value="2" />
            <el-option label="已发布" :value="3" />
            <el-option label="已驳回" :value="4" />
            <el-option label="已归档" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见范围">
          <el-select
            v-model="searchForm.visibility"
            clearable
            placeholder="请选择可见范围"
            style="width: 140px"
          >
            <el-option label="全员可见" :value="1" />
            <el-option label="本部门可见" :value="2" />
            <el-option label="仅本人可见" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        title="管理员可在这里审批待审核文档；普通用户可查看自己提交的文档，以及符合权限范围且已发布的共享文档。"
        type="info"
        :closable="false"
        class="tips"
      />

      <div v-if="isPendingAuditFilter" class="filter-indicator">
        <el-tag type="warning" closable @close="clearPendingAuditFilter">
          当前正在查看：待审核文档
        </el-tag>
      </div>

      <el-table :data="dataList" v-loading="loading" border>
        <el-table-column prop="documentId" label="文档ID" width="96" />
        <el-table-column
          prop="title"
          label="文档标题"
          min-width="220"
          show-overflow-tooltip
        />
        <el-table-column label="分类" min-width="160">
          <template #default="{ row }">
            {{ getCategoryName(row.categoryId) }}
          </template>
        </el-table-column>
        <el-table-column prop="docCode" label="文档编号" min-width="140" />
        <el-table-column prop="currentVersionNo" label="当前版本" width="110" />
        <el-table-column prop="visibility" label="可见范围" width="120">
          <template #default="{ row }">
            {{ visibilityMap[row.visibility] || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusMeta(row.status).type">
              {{ getStatusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="220" show-overflow-tooltip />
        <el-table-column prop="auditRemark" label="审核备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="260">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openDetail(row)">
              预览/下载
            </el-button>
            <el-button
              v-if="canAudit && row.status === 2"
              link
              type="success"
              @click="openAuditDialog(row)"
            >
              审核
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无文档数据" />
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
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      title="新增文档"
      width="680px"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="formModel" :rules="rules" label-width="90px">
        <el-form-item label="所属分类" prop="categoryId">
          <el-tree-select
            v-model="formModel.categoryId"
            class="w-full"
            :data="categoryTreeOptions"
            node-key="categoryId"
            :render-after-expand="false"
            :expand-on-click-node="false"
            check-strictly
            filterable
            clearable
            :default-expand-all="false"
            :props="{
              value: 'categoryId',
              label: 'categoryName',
              children: 'children'
            }"
            placeholder="请选择分类"
          />
        </el-form-item>
        <el-form-item label="文档标题" prop="title">
          <el-input v-model="formModel.title" maxlength="256" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="文档编号" prop="docCode">
          <el-input
            v-model="formModel.docCode"
            maxlength="64"
            placeholder="可选，便于企业内部检索"
          />
        </el-form-item>
        <el-form-item label="上传文件" required>
          <el-upload
            :auto-upload="false"
            :limit="1"
            :file-list="fileList"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar"
          >
            <el-button type="primary" plain>选择本地文件</el-button>
            <template #tip>
              <div class="upload-tip">
                支持 PDF、Office、TXT、ZIP、RAR 等格式，首次上传后会自动生成版本号
                `v1.0.0`。
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="文档摘要" prop="summary">
          <el-input
            v-model="formModel.summary"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="请输入文档摘要"
          />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-input
            v-model="formModel.tags"
            maxlength="1000"
            placeholder="多个标签可用逗号分隔"
          />
        </el-form-item>
        <el-form-item label="可见范围" prop="visibility">
          <el-select v-model="formModel.visibility" style="width: 100%">
            <el-option label="全员可见" :value="1" />
            <el-option label="本部门可见" :value="2" />
            <el-option label="仅本人可见" :value="3" />
          </el-select>
        </el-form-item>
        <el-alert
          title="新增文档后状态默认为“审核中”，通过管理员审批后会自动变更为“已发布”。"
          type="info"
          :closable="false"
        />
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          提交
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="文档详情" size="760px">
      <div v-loading="detailLoading" class="detail-wrapper">
        <template v-if="detailData">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="文档标题">{{ detailData.title }}</el-descriptions-item>
            <el-descriptions-item label="文档编号">{{ detailData.docCode || "-" }}</el-descriptions-item>
            <el-descriptions-item label="所属分类">{{ detailData.categoryName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="所属部门">{{ detailData.deptName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="创建人">{{ detailData.creatorName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="可见范围">
              {{ visibilityMap[detailData.visibility || 0] || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="当前状态">
              <el-tag :type="getStatusMeta(detailData.status).type">
                {{ getStatusMeta(detailData.status).label }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="当前版本">
              {{ detailData.currentVersionNo || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              {{ formatTime(detailData.updateTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="审核备注">
              {{ detailData.auditRemark || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="文档摘要" :span="2">
              {{ detailData.summary || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="标签" :span="2">
              {{ detailData.tags || "-" }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="section-title section-with-action">
            <span>当前文档文件</span>
            <div class="section-actions">
              <el-button
                type="primary"
                plain
                size="small"
                :disabled="!detailData.currentVersion"
                @click="previewCurrentDocument"
              >
                预览
              </el-button>
              <el-button
                type="primary"
                size="small"
                :disabled="!detailData.currentVersion"
                @click="downloadCurrentDocument"
              >
                下载
              </el-button>
            </div>
          </div>

          <el-card shadow="never" class="version-card">
            <template v-if="detailData.currentVersion">
              <div class="version-line">
                <span>{{ detailData.currentVersion.fileName || "-" }}</span>
                <el-tag type="success">当前版本</el-tag>
              </div>
              <div class="version-meta">
                版本号：{{ detailData.currentVersion.versionNo }} /
                文件大小：{{ formatFileSize(detailData.currentVersion.fileSize) }} /
                解析状态：{{ parseStatusMap[detailData.currentVersion.parseStatus || 0] || "-" }}
              </div>
            </template>
            <el-empty v-else description="暂无版本文件信息" />
          </el-card>

          <div class="section-title">版本记录</div>
          <el-table :data="detailData.versionList || []" border>
            <el-table-column prop="versionNo" label="版本号" width="120" />
            <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
            <el-table-column prop="fileSize" label="文件大小" width="120">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="parseStatus" label="解析状态" width="120">
              <template #default="{ row }">
                {{ parseStatusMap[row.parseStatus] || "-" }}
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" min-width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
          </el-table>

          <div class="section-title">审核历史</div>
          <el-table :data="detailData.auditHistoryList || []" border>
            <el-table-column prop="auditorName" label="审核人" min-width="120" />
            <el-table-column prop="auditResult" label="审核结果" width="100">
              <template #default="{ row }">
                <el-tag :type="row.auditResult === 1 ? 'success' : 'danger'">
                  {{ auditResultMap[row.auditResult] || "-" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="beforeStatus" label="审核前状态" width="120">
              <template #default="{ row }">
                {{ getStatusMeta(row.beforeStatus).label }}
              </template>
            </el-table-column>
            <el-table-column prop="afterStatus" label="审核后状态" width="120">
              <template #default="{ row }">
                {{ getStatusMeta(row.afterStatus).label }}
              </template>
            </el-table-column>
            <el-table-column prop="auditRemark" label="审核备注" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createTime" label="审核时间" min-width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-drawer>

    <el-dialog
      v-model="auditVisible"
      title="文档审核"
      width="520px"
      @closed="handleAuditClosed"
    >
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
        <el-form-item label="文档标题">
          <span>{{ auditTarget?.title || "-" }}</span>
        </el-form-item>
        <el-form-item label="审批结果" prop="approved">
          <el-radio-group v-model="auditForm.approved">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="0">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批备注" prop="auditRemark">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入审批意见，驳回时建议填写原因"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditSubmitting" @click="submitAudit">
          提交审批
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.card-header span {
  font-size: 16px;
  font-weight: 600;
}

.subtext {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.search-form {
  margin-bottom: 12px;
}

.tips {
  margin-bottom: 12px;
}

.filter-indicator {
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.upload-tip {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.detail-wrapper {
  min-height: 240px;
}

.section-title {
  margin: 20px 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.section-with-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-actions {
  display: flex;
  gap: 8px;
}

.version-card {
  margin-bottom: 8px;
}

.version-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.version-meta {
  margin-top: 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
