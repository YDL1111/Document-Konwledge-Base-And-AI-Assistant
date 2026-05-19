import dayjs from "dayjs";
import { onMounted, reactive, ref, toRaw } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { PaginationProps } from "@pureadmin/table";
import { deleteRoleApi, getRoleListApi, type RoleDTO, type RoleQuery } from "@/api/system/role";
import { getMenuListApi, type MenuDTO } from "@/api/system/menu";
import { usePublicHooks } from "../../hooks";
import { message } from "@/utils/message";
import { toTree } from "@/utils/tree";

export function useRole() {
  const form = reactive<RoleQuery>({
    roleKey: "",
    roleName: "",
    status: undefined
  });

  const dataList = ref<RoleDTO[]>([]);
  const loading = ref(true);
  const switchLoadMap = ref<Record<number, { loading: boolean }>>({});
  const { switchStyle } = usePublicHooks();
  const pagination = reactive<PaginationProps>({
    total: 0,
    pageSize: 10,
    currentPage: 1,
    background: true
  });
  const multipleSelection = ref<RoleDTO[]>([]);

  const columns: TableColumnList = [
    { label: "角色ID", prop: "roleId", minWidth: 100 },
    { label: "角色名称", prop: "roleName", minWidth: 120 },
    { label: "角色权限字符", prop: "roleKey", minWidth: 150 },
    {
      label: "状态",
      minWidth: 130,
      cellRenderer: scope => (
        <el-switch
          size={scope.props.size === "small" ? "small" : "default"}
          loading={switchLoadMap.value[scope.index]?.loading}
          v-model={scope.row.status}
          active-value={1}
          inactive-value={0}
          active-text="启用"
          inactive-text="停用"
          inline-prompt
          style={switchStyle.value}
          onChange={() => onChange(scope as any)}
        />
      )
    },
    { label: "备注", prop: "remark", minWidth: 150 },
    {
      label: "创建时间",
      minWidth: 180,
      prop: "createTime",
      formatter: ({ createTime }) =>
        dayjs(createTime).format("YYYY-MM-DD HH:mm:ss")
    },
    { label: "操作", fixed: "right", width: 240, slot: "operation" }
  ];

  function onChange({ row, index }) {
    ElMessageBox.confirm(
      `确认要${row.status === 0 ? "停用" : "启用"}角色“${row.roleName}”吗？`,
      "系统提示",
      {
        confirmButtonText: "确认",
        cancelButtonText: "取消",
        type: "warning",
        draggable: true
      }
    )
      .then(() => {
        switchLoadMap.value[index] = { loading: true };
        setTimeout(() => {
          switchLoadMap.value[index] = { loading: false };
          message(`${row.status === 0 ? "停用" : "启用"} ${row.roleName} 成功`, {
            type: "success"
          });
        }, 300);
      })
      .catch(() => {
        row.status === 0 ? (row.status = 1) : (row.status = 0);
      });
  }

  async function handleDelete(row: RoleDTO) {
    try {
      loading.value = true;
      await deleteRoleApi(row.roleId);
      message(`删除角色 ${row.roleName} 成功`, { type: "info" });
      onSearch();
    } catch (e) {
      console.error(e);
      message((e as Error)?.message || "删除失败", { type: "error" });
    } finally {
      loading.value = false;
    }
  }

  async function onSearch() {
    try {
      loading.value = true;
      const { data } = await getRoleListApi(toRaw(form));
      dataList.value = data.rows ?? [];
      pagination.total = data.total ?? 0;
    } catch (e) {
      console.error(e);
      ElMessage.error((e as Error)?.message || "角色列表加载失败");
    } finally {
      loading.value = false;
    }
  }

  const resetForm = formEl => {
    if (!formEl) return;
    formEl.resetFields();
    onSearch();
  };

  function handleSelectionChange(rows: RoleDTO[]) {
    multipleSelection.value = rows;
  }

  function handleSizeChange(pageSize: number) {
    pagination.pageSize = pageSize;
    pagination.currentPage = 1;
    onSearch();
  }

  function handleCurrentChange(currentPage: number) {
    pagination.currentPage = currentPage;
    onSearch();
  }

  const menuTree = ref<MenuDTO[]>([]);
  const menuButtonList = ref<MenuDTO[]>([]);

  async function getMenuTree() {
    if (menuTree.value?.length || menuButtonList.value?.length) {
      return {
        menuTree: menuTree.value,
        menuButtonList: menuButtonList.value
      };
    }
    const [menuResponse, buttonResponse] = await Promise.all([
      getMenuListApi({ isButton: false }),
      getMenuListApi({ isButton: true })
    ]);
    menuTree.value = toTree(menuResponse.data, "id", "parentId");
    menuButtonList.value = buttonResponse.data ?? [];
    return {
      menuTree: menuTree.value,
      menuButtonList: menuButtonList.value
    };
  }

  onMounted(onSearch);

  return {
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
  };
}
