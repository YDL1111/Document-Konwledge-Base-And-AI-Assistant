import dayjs from "dayjs";
import { reactive, ref, onMounted, toRaw } from "vue";
import { ElMessageBox, type Sort } from "element-plus";
import { PaginationProps } from "@pureadmin/table";
import { CommonUtils } from "@/utils/common";
import { message } from "@/utils/message";
import { useUserStoreHook } from "@/store/modules/user";
import {
  deleteLoginLogApi,
  exportLoginLogExcelApi,
  getLoginLogListApi,
  type LoginLogsDTO,
  type LoginLogQuery
} from "@/api/system/log";

const loginLogStatusMap =
  useUserStoreHook().dictionaryMap["sysLoginLog.status"] ?? {};

export function useLoginLogHook() {
  const defaultSort: Sort = {
    prop: "loginTime",
    order: "descending"
  };

  const pagination: PaginationProps = {
    total: 0,
    pageSize: 10,
    currentPage: 1,
    background: true
  };

  const timeRange = ref<[string, string] | []>([]);
  const searchFormParams = reactive<LoginLogQuery>({
    ipAddress: undefined,
    username: undefined,
    status: undefined,
    beginTime: undefined,
    endTime: undefined,
    timeRangeColumn: defaultSort.prop
  });

  const dataList = ref<LoginLogsDTO[]>([]);
  const pageLoading = ref(true);
  const multipleSelection = ref<number[]>([]);

  const columns: TableColumnList = [
    { type: "selection", align: "left" },
    { label: "日志ID", prop: "logId", minWidth: 100 },
    { label: "用户名", prop: "username", minWidth: 120, sortable: "custom" },
    { label: "登录IP", prop: "ipAddress", minWidth: 120 },
    { label: "登录地点", prop: "loginLocation", minWidth: 120 },
    { label: "操作系统", prop: "operationSystem", minWidth: 120 },
    { label: "浏览器", prop: "browser", minWidth: 120 },
    {
      label: "状态",
      prop: "status",
      minWidth: 120,
      cellRenderer: ({ row, props }) => {
        const current = loginLogStatusMap[row.status] ?? {
          cssTag: "info",
          label: "未知"
        };
        return (
          <el-tag size={props.size} type={current.cssTag} effect="plain">
            {current.label}
          </el-tag>
        );
      }
    },
    { label: "状态文本", prop: "statusStr", minWidth: 120, hide: true },
    {
      label: "登录时间",
      minWidth: 160,
      prop: "loginTime",
      sortable: "custom",
      formatter: ({ loginTime }) => dayjs(loginTime).format("YYYY-MM-DD HH:mm:ss")
    },
    { label: "操作", fixed: "right", width: 140, slot: "operation" }
  ];

  async function onSearch() {
    pagination.currentPage = 1;
    await getLoginLogList();
  }

  function resetForm(formEl, tableRef) {
    if (!formEl) return;
    formEl.resetFields();
    searchFormParams.orderColumn = undefined;
    searchFormParams.orderDirection = undefined;
    timeRange.value = [];
    searchFormParams.beginTime = undefined;
    searchFormParams.endTime = undefined;
    tableRef?.getTableRef?.().clearSort();
    onSearch();
  }

  async function getLoginLogList(sort: Sort = defaultSort) {
    pageLoading.value = true;
    if (sort) {
      CommonUtils.fillSortParams(searchFormParams, sort);
    }
    CommonUtils.fillPaginationParams(searchFormParams, pagination);
    CommonUtils.fillTimeRangeParams(searchFormParams, timeRange.value);

    try {
      const { data } = await getLoginLogListApi(toRaw(searchFormParams));
      dataList.value = data.rows ?? [];
      pagination.total = data.total ?? 0;
    } finally {
      pageLoading.value = false;
    }
  }

  function handleSelectionChange(rows: LoginLogsDTO[]) {
    multipleSelection.value = rows.map(item => item.logId);
  }

  function handlePageSizeChange(pageSize: number) {
    pagination.pageSize = pageSize;
    pagination.currentPage = 1;
    getLoginLogList();
  }

  function handlePageCurrentChange(currentPage: number) {
    pagination.currentPage = currentPage;
    getLoginLogList();
  }

  function handleSortChange(sort: Sort) {
    getLoginLogList(sort);
  }

  async function exportAllExcel(sort: Sort = defaultSort) {
    if (sort) {
      CommonUtils.fillSortParams(searchFormParams, sort);
    }
    CommonUtils.fillPaginationParams(searchFormParams, pagination);
    CommonUtils.fillTimeRangeParams(searchFormParams, timeRange.value);
    exportLoginLogExcelApi(toRaw(searchFormParams), "登录日志.xls");
  }

  async function handleDelete(row: LoginLogsDTO) {
    await deleteLoginLogApi([row.logId]);
    message(`删除登录日志 ${row.logId} 成功`, { type: "success" });
    getLoginLogList();
  }

  async function handleBulkDelete(tableRef) {
    if (multipleSelection.value.length === 0) {
      message("请先选择要删除的日志", { type: "warning" });
      return;
    }

    ElMessageBox.confirm(
      `确认删除选中的登录日志：${multipleSelection.value.join(", ")} 吗？`,
      "系统提示",
      {
        confirmButtonText: "确认",
        cancelButtonText: "取消",
        type: "warning",
        draggable: true
      }
    )
      .then(async () => {
        await deleteLoginLogApi(multipleSelection.value);
        message("批量删除成功", { type: "success" });
        getLoginLogList();
      })
      .catch(() => {
        message("已取消删除", { type: "info" });
        tableRef?.getTableRef?.().clearSelection();
      });
  }

  onMounted(() => {
    getLoginLogList();
  });

  return {
    searchFormParams,
    pageLoading,
    columns,
    dataList,
    pagination,
    timeRange,
    defaultSort,
    multipleSelection,
    onSearch,
    resetForm,
    exportAllExcel,
    getLoginLogList,
    handleDelete,
    handleBulkDelete,
    handleSelectionChange,
    handlePageSizeChange,
    handlePageCurrentChange,
    handleSortChange
  };
}
