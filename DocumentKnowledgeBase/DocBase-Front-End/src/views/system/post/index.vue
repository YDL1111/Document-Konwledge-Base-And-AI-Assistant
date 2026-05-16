<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { getPostListApi, type PostPageResponse } from "@/api/system/post";
import { message } from "@/utils/message";

defineOptions({
  name: "SystemPost"
});

const loading = ref(false);
const postList = ref<PostPageResponse[]>([]);

const queryForm = reactive({
  postName: "",
  postCode: "",
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 20
});

async function loadPosts() {
  loading.value = true;
  try {
    const { data } = await getPostListApi(queryForm);
    postList.value = data.rows ?? [];
  } catch (error) {
    console.error(error);
    message("岗位管理加载失败，请检查后端接口日志", { type: "error" });
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  queryForm.postName = "";
  queryForm.postCode = "";
  queryForm.status = undefined;
  loadPosts();
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
          <span class="subtext">当前先恢复岗位列表查询能力，后续再补增删改弹窗。</span>
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
          <el-button type="primary" @click="loadPosts">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="postList" v-loading="loading" border>
        <el-table-column prop="postId" label="岗位编号" width="100" />
        <el-table-column prop="postName" label="岗位名称" min-width="160" />
        <el-table-column prop="postCode" label="岗位编码" min-width="140" />
        <el-table-column prop="postSort" label="显示顺序" width="100" />
        <el-table-column prop="remark" label="备注" min-width="180" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? "启用" : "停用" }}
            </el-tag>
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
