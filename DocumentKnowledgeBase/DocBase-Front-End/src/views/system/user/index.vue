<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import dayjs from "dayjs";
import {
  getUserListApi,
  addUserApi,
  updateUserApi,
  deleteUserApi,
  updateUserStatusApi,
  updateUserPasswordApi,
  exportUserExcelApi,
  type UserDTO,
  type UserQuery,
  type UserRequest,
  type PasswordRequest
} from "@/api/system/user";
import { getDeptListApi } from "@/api/system/dept";
import { getRoleListApi, type RoleDTO } from "@/api/system/role";
import { getPostListApi, type PostPageResponse } from "@/api/system/post";
import { handleTree } from "@/utils/tree";
import { message } from "@/utils/message";
import { ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";

defineOptions({
  name: "SystemUser"
});

const loading = ref(false);
const submitting = ref(false);
const dataList = ref<UserDTO[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const pwdDialogVisible = ref(false);
const dialogTitle = ref("新增");
const formRef = ref<FormInstance>();
const pwdFormRef = ref<FormInstance>();
const deptOptions = ref<any[]>([]);
const roleOptions = ref<RoleDTO[]>([]);
const postOptions = ref<PostPageResponse[]>([]);
const statusSwitching = ref<Record<number, boolean>>({});

const searchForm = reactive<UserQuery>({
  username: "",
  phoneNumber: "",
  status: undefined,
  pageNum: 1,
  pageSize: 10
});

const formModel = reactive<UserRequest>({
  userId: 0,
  username: "",
  nickname: "",
  deptId: undefined,
  phoneNumber: "",
  email: "",
  password: "",
  sex: undefined,
  status: 1,
  roleId: undefined,
  postId: undefined,
  remark: ""
});

const pwdForm = reactive<PasswordRequest>({
  userId: 0,
  password: ""
});

const formRules: FormRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }]
};

const pwdRules: FormRules = {
  password: [{ required: true, message: "请输入新密码", trigger: "blur" }]
};

async function loadUsers() {
  loading.value = true;
  try {
    const { data } = await getUserListApi(searchForm);
    dataList.value = data.rows ?? [];
    total.value = data.total ?? 0;
  } catch (error) {
    console.error(error);
    message("用户列表加载失败", { type: "error" });
  } finally {
    loading.value = false;
  }
}

async function loadOptions() {
  try {
    const [deptRes, roleRes, postRes] = await Promise.all([
      getDeptListApi(),
      getRoleListApi({ pageNum: 1, pageSize: 100 }),
      getPostListApi({ pageNum: 1, pageSize: 100 })
    ]);
    deptOptions.value = handleTree(deptRes.data ?? []);
    roleOptions.value = (roleRes.data as any)?.rows ?? [];
    postOptions.value = (postRes.data as any)?.rows ?? [];
  } catch (error) {
    console.error(error);
  }
}

function handleSearch() {
  searchForm.pageNum = 1;
  loadUsers();
}

function resetSearch() {
  searchForm.username = "";
  searchForm.phoneNumber = "";
  searchForm.status = undefined;
  searchForm.pageNum = 1;
  loadUsers();
}

function handlePageChange(pageNum: number) {
  searchForm.pageNum = pageNum;
  loadUsers();
}

function handleSizeChange(pageSize: number) {
  searchForm.pageSize = pageSize;
  searchForm.pageNum = 1;
  loadUsers();
}

function openAddDialog() {
  dialogTitle.value = "新增";
  formModel.userId = 0;
  formModel.username = "";
  formModel.nickname = "";
  formModel.deptId = undefined;
  formModel.phoneNumber = "";
  formModel.email = "";
  formModel.password = "";
  formModel.sex = undefined;
  formModel.status = 1;
  formModel.roleId = undefined;
  formModel.postId = undefined;
  formModel.remark = "";
  dialogVisible.value = true;
  formRef.value?.clearValidate();
}

function openEditDialog(row: UserDTO) {
  dialogTitle.value = "编辑";
  formModel.userId = row.userId ?? 0;
  formModel.username = row.username ?? "";
  formModel.nickname = row.nickname ?? "";
  formModel.deptId = row.deptId;
  formModel.phoneNumber = row.phoneNumber ?? "";
  formModel.email = row.email ?? "";
  formModel.password = undefined as any;
  formModel.sex = row.sex;
  formModel.status = row.status;
  formModel.roleId = row.roleId;
  formModel.postId = row.postId;
  formModel.remark = row.remark ?? "";
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
        await addUserApi({ ...formModel });
        message("新增用户成功", { type: "success" });
      } else {
        await updateUserApi(formModel.userId, { ...formModel });
        message("编辑用户成功", { type: "success" });
      }
      dialogVisible.value = false;
      loadUsers();
    } catch (error) {
      console.error(error);
      message("操作失败", { type: "error" });
    } finally {
      submitting.value = false;
    }
  });
}

async function handleDelete(row: UserDTO) {
  try {
    await ElMessageBox.confirm(
      `确认删除用户「${row.username}」吗？此操作不可恢复。`,
      "系统提示",
      { confirmButtonText: "确定", cancelButtonText: "取消", type: "warning" }
    );
    await deleteUserApi(row.userId!);
    message("删除成功", { type: "success" });
    loadUsers();
  } catch (error: any) {
    if (error !== "cancel") {
      message("删除失败", { type: "error" });
    }
  }
}

async function handleStatusChange(row: UserDTO) {
  const oldStatus = row.status;
  const newStatus = oldStatus === 1 ? 0 : 1;
  const actionText = newStatus === 0 ? "停用" : "启用";
  try {
    await ElMessageBox.confirm(
      `确认要${actionText}用户「${row.username}」吗？`,
      "系统提示",
      { confirmButtonText: "确定", cancelButtonText: "取消", type: "warning" }
    );
    statusSwitching.value[row.userId!] = true;
    await updateUserStatusApi(row.userId!, newStatus);
    row.status = newStatus;
    message(`${actionText}成功`, { type: "success" });
  } catch (error: any) {
    row.status = oldStatus;
  } finally {
    statusSwitching.value[row.userId!] = false;
  }
}

function openPwdDialog(row: UserDTO) {
  pwdForm.userId = row.userId ?? 0;
  pwdForm.password = "";
  pwdDialogVisible.value = true;
  pwdFormRef.value?.clearValidate();
}

async function submitPwd() {
  if (!pwdFormRef.value) return;
  await pwdFormRef.value.validate(async valid => {
    if (!valid) return;
    try {
      await updateUserPasswordApi({ ...pwdForm });
      message("密码重置成功", { type: "success" });
      pwdDialogVisible.value = false;
    } catch (error) {
      console.error(error);
      message("密码重置失败", { type: "error" });
    }
  });
}

async function handleExport() {
  try {
    await exportUserExcelApi(searchForm, "用户列表.xls");
    message("导出成功", { type: "success" });
  } catch (error) {
    console.error(error);
    message("导出失败", { type: "error" });
  }
}

function formatTime(value?: string | Date) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm:ss") : "-";
}

onMounted(() => {
  loadOptions();
  loadUsers();
});
</script>

<template>
  <div class="main">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div class="header-actions">
            <el-button type="primary" @click="openAddDialog">新增用户</el-button>
            <el-button plain @click="handleExport">导出Excel</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" clearable placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phoneNumber" clearable placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" clearable placeholder="请选择" style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="dataList" v-loading="loading" border>
        <el-table-column prop="userId" label="用户编号" width="100" fixed="left" />
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="nickname" label="昵称" min-width="130" />
        <el-table-column prop="deptName" label="部门" min-width="130" />
        <el-table-column prop="roleName" label="角色" min-width="130" />
        <el-table-column prop="phoneNumber" label="手机号" min-width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="statusSwitching[row.userId]"
              active-text="启用"
              inactive-text="停用"
              inline-prompt
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="260">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="success" @click="openPwdDialog(row)">重置密码</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="`${dialogTitle}用户`"
      width="640px"
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="formModel" :rules="formRules" label-width="82px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="formModel.username" maxlength="30" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="formModel.nickname" maxlength="30" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门" prop="deptId">
              <el-tree-select
                v-model="formModel.deptId"
                class="w-full"
                :data="deptOptions"
                :show-all-levels="false"
                value-key="id"
                :props="{ value: 'id', label: 'deptName', emitPath: false, checkStrictly: true }"
                clearable
                placeholder="请选择部门"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="roleId">
              <el-select v-model="formModel.roleId" class="w-full" placeholder="请选择角色" clearable>
                <el-option
                  v-for="item in roleOptions"
                  :key="item.roleId"
                  :label="item.roleName"
                  :value="item.roleId"
                  :disabled="item.status === 0"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位" prop="postId">
              <el-select v-model="formModel.postId" class="w-full" placeholder="请选择岗位" clearable>
                <el-option
                  v-for="item in postOptions"
                  :key="item.postId"
                  :label="item.postName"
                  :value="item.postId"
                  :disabled="item.status === 0"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phoneNumber">
              <el-input v-model="formModel.phoneNumber" maxlength="11" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="formModel.email" maxlength="50" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="sex">
              <el-select v-model="formModel.sex" class="w-full" placeholder="请选择" clearable>
                <el-option label="男" :value="0" />
                <el-option label="女" :value="1" />
                <el-option label="未知" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="dialogTitle === '新增'" :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="formModel.password" maxlength="20" placeholder="请输入初始密码" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="formModel.remark" type="textarea" :rows="3" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="420px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="82px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" maxlength="20" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPwd">确定</el-button>
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
.header-actions {
  display: flex;
  gap: 12px;
}
.search-form {
  margin-bottom: 16px;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.w-full {
  width: 100%;
}
</style>
