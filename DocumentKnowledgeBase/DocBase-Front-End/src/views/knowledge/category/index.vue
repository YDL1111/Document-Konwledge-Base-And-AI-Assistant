<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import type { FormInstance, FormRules } from "element-plus";
import {
  addKnowledgeCategoryApi,
  deleteKnowledgeCategoryApi,
  getKnowledgeCategoryDetailApi,
  getKnowledgeCategoryListApi,
  updateKnowledgeCategoryApi,
  type KnowledgeCategoryDTO,
  type KnowledgeCategoryQuery,
  type KnowledgeCategoryRequest
} from "@/api/knowledge/category";
import { message } from "@/utils/message";
import { handleTree } from "@/utils/tree";

defineOptions({
  name: "KnowledgeCategory"
});

interface CategoryTreeNode extends KnowledgeCategoryDTO {
  children?: CategoryTreeNode[];
  level?: number;
  pathLabel?: string;
}

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const dialogMode = ref<"add" | "edit">("add");
const categoryList = ref<KnowledgeCategoryDTO[]>([]);
const formRef = ref<FormInstance>();
const editingId = ref<number>();

const searchForm = reactive<KnowledgeCategoryQuery>({
  categoryName: "",
  status: undefined,
  pageNum: 1,
  pageSize: 500
});

const formModel = reactive<KnowledgeCategoryRequest>({
  parentId: 0,
  categoryName: "",
  sortNum: 1,
  status: 1,
  remark: ""
});

const rules: FormRules = {
  categoryName: [{ required: true, message: "请输入分类名称", trigger: "blur" }],
  sortNum: [{ required: true, message: "请输入排序", trigger: "blur" }],
  status: [{ required: true, message: "请选择状态", trigger: "change" }]
};

const categoryNameMap = computed(() => {
  const map = new Map<number, string>();
  categoryList.value.forEach(item => {
    map.set(item.categoryId, item.categoryName);
  });
  return map;
});

function sortTree(nodes: CategoryTreeNode[] = []) {
  return nodes
    .sort((a, b) => {
      const sortDiff = (a.sortNum ?? 0) - (b.sortNum ?? 0);
      if (sortDiff !== 0) return sortDiff;
      return (a.categoryId ?? 0) - (b.categoryId ?? 0);
    })
    .map(node => ({
      ...node,
      children: sortTree(node.children || [])
    }));
}

function decorateTree(
  nodes: CategoryTreeNode[] = [],
  level = 1,
  parentPath = ""
): CategoryTreeNode[] {
  return nodes.map(node => {
    const pathLabel = parentPath
      ? `${parentPath} / ${node.categoryName}`
      : node.categoryName;
    return {
      ...node,
      level,
      pathLabel,
      children: decorateTree(node.children || [], level + 1, pathLabel)
    };
  });
}

const categoryTreeOptions = computed(() => {
  const treeSource = categoryList.value.map(item => ({ ...item }));
  const tree = handleTree(treeSource, "categoryId", "parentId", "children") as CategoryTreeNode[];
  return decorateTree(sortTree(tree));
});

const parentTreeOptions = computed(() => [
  {
    categoryId: 0,
    categoryName: "顶级分类",
    children: categoryTreeOptions.value
  }
]);

const filteredTreeData = computed(() => {
  const keyword = searchForm.categoryName?.trim();
  const status = searchForm.status;

  const filterNodes = (nodes: CategoryTreeNode[] = []): CategoryTreeNode[] => {
    return nodes
      .map(node => {
        const children = filterNodes(node.children || []);
        const matchKeyword = !keyword || node.categoryName?.includes(keyword);
        const matchStatus = status === undefined || node.status === status;
        if ((matchKeyword && matchStatus) || children.length > 0) {
          return {
            ...node,
            children
          };
        }
        return null;
      })
      .filter(Boolean) as CategoryTreeNode[];
  };

  return filterNodes(categoryTreeOptions.value);
});

async function loadCategories() {
  loading.value = true;
  try {
    const { data } = await getKnowledgeCategoryListApi({
      pageNum: 1,
      pageSize: 500
    });
    categoryList.value = data.rows ?? [];
  } catch (error) {
    console.error(error);
    message("分类管理加载失败，请检查后端接口日志", { type: "error" });
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  // 本页已改成前端树形筛选，保持本地过滤即可
}

function resetSearch() {
  searchForm.categoryName = "";
  searchForm.status = undefined;
}

function resetForm() {
  editingId.value = undefined;
  formModel.parentId = 0;
  formModel.categoryName = "";
  formModel.sortNum = 1;
  formModel.status = 1;
  formModel.remark = "";
  formRef.value?.clearValidate();
}

function getNextSortNum(parentId = 0) {
  const siblingSortNums = categoryList.value
    .filter(item => (item.parentId ?? 0) === parentId)
    .map(item => item.sortNum ?? 0);
  const maxSortNum = siblingSortNums.length ? Math.max(...siblingSortNums) : 0;
  return maxSortNum + 1;
}

function getParentCategoryName(parentId?: number) {
  if (!parentId || parentId === 0) return "顶级分类";
  return categoryNameMap.value.get(parentId) ?? `ID:${parentId}`;
}

function openAddDialog(row?: KnowledgeCategoryDTO) {
  dialogMode.value = "add";
  resetForm();
  if (row?.categoryId) {
    formModel.parentId = row.categoryId;
  }
  formModel.sortNum = getNextSortNum(formModel.parentId ?? 0);
  dialogVisible.value = true;
}

async function openEditDialog(row: KnowledgeCategoryDTO) {
  dialogMode.value = "edit";
  resetForm();
  try {
    const { data } = await getKnowledgeCategoryDetailApi(row.categoryId);
    editingId.value = data.categoryId;
    formModel.parentId = data.parentId ?? 0;
    formModel.categoryName = data.categoryName;
    formModel.sortNum = data.sortNum ?? 1;
    formModel.status = data.status ?? 1;
    formModel.remark = data.remark ?? "";
    dialogVisible.value = true;
  } catch (error) {
    console.error(error);
    message("分类详情加载失败", { type: "error" });
  }
}

async function submitForm() {
  if (!formRef.value) return;
  await formRef.value.validate(async valid => {
    if (!valid) return;
    submitting.value = true;
    try {
      if (dialogMode.value === "add") {
        await addKnowledgeCategoryApi({ ...formModel });
        message("新增分类成功", { type: "success" });
      } else if (editingId.value) {
        await updateKnowledgeCategoryApi(editingId.value, { ...formModel });
        message("编辑分类成功", { type: "success" });
      }
      dialogVisible.value = false;
      resetForm();
      loadCategories();
    } catch (error) {
      console.error(error);
      message("保存分类失败，请检查后端接口日志", { type: "error" });
    } finally {
      submitting.value = false;
    }
  });
}

async function handleDelete(row: KnowledgeCategoryDTO) {
  try {
    await deleteKnowledgeCategoryApi(row.categoryId);
    message("删除分类成功", { type: "success" });
    loadCategories();
  } catch (error) {
    console.error(error);
    message("删除分类失败，请先清理子分类", { type: "error" });
  }
}

onMounted(() => {
  loadCategories();
});
</script>

<template>
  <div class="main">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <span>分类管理</span>
            <p class="subtext">
              已改成树形管理视图。每个父级下的子分类按“同级排序”展示，分类多了也能更清晰地看层级。
            </p>
          </div>
          <el-button type="primary" @click="openAddDialog()">新增顶级分类</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="分类名称">
          <el-input
            v-model="searchForm.categoryName"
            clearable
            placeholder="请输入分类名称"
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
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        title="说明：分类ID出现 1、2、3、6 这类断号是正常的数据库主键行为；排序现在按每个父级下的同级节点分别生效。"
        type="info"
        :closable="false"
        class="tips"
      />

      <el-table
        :data="filteredTreeData"
        v-loading="loading"
        border
        row-key="categoryId"
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="categoryName" label="分类名称" min-width="220">
          <template #default="{ row }">
            <div class="category-cell">
              <span>{{ row.categoryName }}</span>
              <el-tag size="small" effect="plain">L{{ row.level }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类路径" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.pathLabel }}
          </template>
        </el-table-column>
        <el-table-column label="父级分类" min-width="160">
          <template #default="{ row }">
            {{ getParentCategoryName(row.parentId) }}
          </template>
        </el-table-column>
        <el-table-column prop="sortNum" label="同级排序" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
        <el-table-column prop="categoryId" label="分类ID" width="100" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAddDialog(row)">加子分类</el-button>
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除该分类吗？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无分类数据" />
        </template>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增分类' : '编辑分类'"
      width="560px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="formModel" :rules="rules" label-width="90px">
        <el-form-item label="父级分类" prop="parentId">
          <el-tree-select
            v-model="formModel.parentId"
            class="w-full"
            :data="parentTreeOptions"
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
            placeholder="请选择父级分类"
          />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="formModel.categoryName" maxlength="128" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="同级排序" prop="sortNum">
          <el-input-number v-model="formModel.sortNum" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formModel.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formModel.remark"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="可填写分类用途说明"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
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
  margin-bottom: 16px;
}

.category-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
