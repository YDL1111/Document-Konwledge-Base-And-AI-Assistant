<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import {
  getPostListApi,
  getPostInfoApi,
  addPostApi,
  updatePostApi,
  deletePostApi,
  type PostPageResponse,
  type PostQuery,
  type PostRequest
} from "@/api/system/post";
import { message } from "@/utils/message";
import { ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";

defineOptions({
  name: "SystemPost"
});

const loading = ref(false);
const submitting = ref(false);
const postList = ref<PostPageResponse[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const dialogTitle = ref("新增");
const formRef = ref<FormInstance>();

const queryForm = reactive<PostQuery>({
  postName: "",
  postCode: "",
  status: undefined,
  pageNum: 1,
  pageSize: 10
});

const formModel = reactive<PostRequest>({
  postCode: "",
  postName: "",
  postSort: 0,
  status: 1,
  remark: ""
});

const formRules: FormRules = {
  postName: [{ required: true, message: "请输入岗位名称", trigger: "blur" }],
  postCode: [{ required: true, message: "请输入岗位编码", trigger: "blur" }],
  postSort: [{ required: true, message: "请输入显示顺序", trigger: "blur" }]
};

async function loadPosts() {
  loading.value = true;
  try {
    const { data } = await getPostListApi(queryForm);
    postList.value = data.rows ?? [];
    total.value = data.total ?? 0;
  } catch (error) {
    console.error(error);
    message("岗位列表加载失败", { type: "error" });
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  queryForm.pageNum = 1;
  loadPosts();
}

function resetQuery() {
  queryForm.postName = "";
  queryForm.postCode = "";
  queryForm.status = undefined;
  queryForm.pageNum = 1;
  loadPosts();
}

function handlePageChange(pageNum: number) {
  queryForm.pageNum = pageNum;
  loadPosts();
}

function handleSizeChange(pageSize: number) {
  queryForm.pageSize = pageSize;
  queryForm.pageNum = 1;
  loadPosts();
}

function openAddDialog() {
  dialogTitle.value = "新增";
  formModel.postId = undefined;
  formModel.postCode = "";
  formModel.postName = "";
  formModel.postSort = 0;
  formModel.status = 1;
  formModel.remark = "";
  dialogVisible.value = true;
  formRef.value?.clearValidate();
}

async function openEditDialog(row: PostPageResponse) {
  dialogTitle.value = "编辑";
  try {
    const { data } = await getPostInfoApi(row.postId);
    if (data) {
      formModel.postId = data.postId;
      formModel.postCode = data.postCode;
      formModel.postName = data.postName;
      formModel.postSort = data.postSort ?? 0;
      formModel.status = data.status ?? 1;
      formModel.remark = data.remark ?? "";
    }
  } catch (error) {
    console.error(error);
    message("获取岗位详情失败", { type: "error" });
    return;
  }
  dialogVisible.value = true;
  formRef.value?.clearValidate();
}

async function submitForm() {
  if (!formRef.value) return;
  await formRef.value.validate(async valid => {
    if (!valid) return;
    submitting.value = true;
    try {
      if (dialogTitle.value === "新增") {
        await addPostApi({ ...formModel });
        message("新增岗位成功", { type: "success" });
      } else {
        await updatePostApi({ ...formModel });
        message("编辑岗位成功", { type: "success" });
      }
      dialogVisible.value = false;
      loadPosts();
    } catch (error) {
      console.error(error);
      message("操作失败", { type: "error" });
    } finally {
      submitting.value = false;
    }
  });
}

async function handleDelete(row: PostPageResponse) {
  try {
    await ElMessageBox.confirm(
      `确认删除岗位「${row.postName}」吗？`,
      "系统提示",
      { confirmButtonText: "确定", cancelButtonText: "取消", type: "warning" }
    );
    await deletePostApi([row.postId]);
    message("删除成功", { type: "success" });
    loadPosts();
  } catch (error: any) {
    if (error !== "cancel") {
      console.error(error);
      message("删除失败", { type: "error" });
    }
  }
}

onMounted(() => {
  loadPosts();
});
</script>

<template>
  <div class="main">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>岗位管理</span>
          <el-button type="primary" @click="openAddDialog">新增岗位</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="岗位名称">
          <el-input v-model="queryForm.postName" clearable placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="岗位编码">
          <el-input v-model="queryForm.postCode" clearable placeholder="请输入岗位编码" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" clearable placeholder="请选择状态" style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="postList" v-loading="loading" border>
        <el-table-column prop="postId" label="岗位编号" width="100" />
        <el-table-column prop="postName" label="岗位名称" min-width="160" />
        <el-table-column prop="postCode" label="岗位编码" min-width="140" />
        <el-table-column prop="postSort" label="显示顺序" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page="queryForm.pageNum"
          :page-size="queryForm.pageSize"
          :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="`${dialogTitle}岗位`"
      width="520px"
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="formModel" :rules="formRules" label-width="82px">
        <el-form-item label="岗位名称" prop="postName">
          <el-input v-model="formModel.postName" maxlength="64" placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="岗位编码" prop="postCode">
          <el-input v-model="formModel.postCode" maxlength="64" placeholder="请输入岗位编码" />
        </el-form-item>
        <el-form-item label="显示顺序" prop="postSort">
          <el-input-number v-model="formModel.postSort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formModel.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formModel.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-form {
  margin-bottom: 16px;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
