<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { getUserListApi, type UserDTO, type UserQuery } from "@/api/system/user";
import { message } from "@/utils/message";

defineOptions({
  name: "SystemUser"
});

const loading = ref(false);
const dataList = ref<UserDTO[]>([]);

const searchForm = reactive<UserQuery>({
  userId: undefined,
  username: "",
  phoneNumber: "",
  status: undefined,
  pageNum: 1,
  pageSize: 10
});

async function loadUsers() {
  loading.value = true;
  try {
    const { data } = await getUserListApi(searchForm);
    dataList.value = data.rows ?? [];
  } catch (error) {
    console.error(error);
    message("用户管理加载失败，请检查后端接口日志", { type: "error" });
  } finally {
    loading.value = false;
  }
}

function resetSearch() {
  searchForm.userId = undefined;
  searchForm.username = "";
  searchForm.phoneNumber = "";
  searchForm.status = undefined;
  loadUsers();
}

onMounted(() => {
  loadUsers();
});
</script>

<template>
  <div class="main">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <span class="subtext">当前先恢复最小可用查询列表，后续再继续补编辑弹窗与导入导出体验。</span>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户编号">
          <el-input v-model="searchForm.userId" clearable placeholder="请输入用户编号" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" clearable placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phoneNumber" clearable placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" clearable placeholder="请选择状态" style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadUsers">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="dataList" v-loading="loading" border>
        <el-table-column prop="userId" label="用户编号" width="100" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="显示名称" min-width="140" />
        <el-table-column prop="deptName" label="部门" min-width="140" />
        <el-table-column prop="roleName" label="角色" min-width="140" />
        <el-table-column prop="phoneNumber" label="手机号" min-width="140" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.subtext {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.search-form {
  margin-bottom: 16px;
}
</style>
