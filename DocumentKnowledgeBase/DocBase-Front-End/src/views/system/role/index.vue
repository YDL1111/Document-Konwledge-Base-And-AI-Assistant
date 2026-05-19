<script setup lang="ts">
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { PureTableBar } from "@/components/RePureTableBar";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { getRoleInfoApi, type RoleDTO } from "@/api/system/role";
import RoleFormModal from "./role-form-modal.vue";
import { useRole } from "./utils/hook";
import AddFill from "@iconify-icons/ri/add-circle-line";
import Delete from "@iconify-icons/ep/delete";
import EditPen from "@iconify-icons/ep/edit-pen";
import Refresh from "@iconify-icons/ep/refresh";
import Search from "@iconify-icons/ep/search";

defineOptions({
  name: "SystemRole"
});

const formRef = ref();
const modalVisible = ref(false);
const opType = ref<"add" | "update">("add");
const opRow = ref<RoleDTO>();

const {
  form,
  loading,
  columns,
  dataList,
  pagination,
  onSearch,
  resetForm,
  menuTree,
  menuButtonList,
  getMenuTree,
  handleDelete,
  handleSelectionChange,
  handleSizeChange,
  handleCurrentChange
} = useRole();

async function openDialog(type: "add" | "update", row?: RoleDTO) {
  try {
    await getMenuTree();
    if (row) {
      const { data } = await getRoleInfoApi(row.roleId);
      row.selectedMenuList = data.selectedMenuList;
      row.selectedDeptList = data.selectedDeptList;
    }
    opType.value = type;
    opRow.value = row;
    modalVisible.value = true;
  } catch (error) {
    console.error(error);
    ElMessage.error((error as Error)?.message || "角色信息加载失败");
  }
}
</script>

<template>
  <div class="main">
    <el-form
      ref="formRef"
      :inline="true"
      :model="form"
      class="search-form bg-bg_color w-[99/100] pl-8 pt-[12px]"
    >
      <el-form-item label="角色名称" prop="roleName">
        <el-input
          v-model="form.roleName"
          placeholder="请输入角色名称"
          clearable
          class="!w-[200px]"
        />
      </el-form-item>
      <el-form-item label="角色标识" prop="roleKey">
        <el-input
          v-model="form.roleKey"
          placeholder="请输入角色标识"
          clearable
          class="!w-[180px]"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="form.status"
          placeholder="请选择状态"
          clearable
          class="!w-[180px]"
        >
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          :icon="useRenderIcon(Search)"
          :loading="loading"
          @click="onSearch"
        >
          查询
        </el-button>
        <el-button :icon="useRenderIcon(Refresh)" @click="resetForm(formRef)">
          重置
        </el-button>
      </el-form-item>
    </el-form>

    <PureTableBar title="角色列表" :columns="columns" @refresh="onSearch">
      <template #buttons>
        <el-button
          type="primary"
          :icon="useRenderIcon(AddFill)"
          @click="openDialog('add')"
        >
          新增角色
        </el-button>
      </template>

      <template #default="{ size, dynamicColumns }">
        <pure-table
          border
          align-whole="center"
          showOverflowTooltip
          table-layout="auto"
          :loading="loading"
          :size="size"
          adaptive
          :data="dataList"
          :columns="dynamicColumns"
          :pagination="pagination"
          :pagination-small="size === 'small'"
          :header-cell-style="{
            background: 'var(--el-table-row-hover-bg-color)',
            color: 'var(--el-text-color-primary)'
          }"
          @selection-change="handleSelectionChange"
          @page-size-change="handleSizeChange"
          @page-current-change="handleCurrentChange"
        >
          <template #operation="{ row }">
            <el-button
              class="reset-margin"
              link
              type="primary"
              :size="size"
              :icon="useRenderIcon(EditPen)"
              @click="openDialog('update', row)"
            >
              编辑
            </el-button>
            <el-popconfirm
              :title="`确认删除角色「${row.roleName}」吗？`"
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

    <role-form-modal
      v-model="modalVisible"
      :type="opType"
      :row="opRow"
      :menu-options="menuTree"
      :button-options="menuButtonList"
    />
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
