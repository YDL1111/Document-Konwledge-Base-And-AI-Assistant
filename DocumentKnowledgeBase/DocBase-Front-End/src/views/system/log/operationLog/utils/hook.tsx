import dayjs from "dayjs";
import { h, onMounted, reactive, ref, toRaw } from "vue";
import { ElMessageBox, type Sort } from "element-plus";
import { addDialog, closeDialog } from "@/components/ReDialog";
import { PaginationProps } from "@pureadmin/table";
import { CommonUtils } from "@/utils/common";
import { message } from "@/utils/message";
import { useUserStoreHook } from "@/store/modules/user";
import descriptionForm from "../description.vue";
import {
  cleanOperationLogApi,
  deleteOperationLogApi,
  exportOperationLogExcelApi,
  getOperationLogListApi,
  type OperationLogDTO,
  type OperationLogsQuery
} from "@/api/system/log";

const operationLogStatusMap =
  useUserStoreHook().dictionaryMap["sysOperationLog.status"] ?? {};
const businessTypeMap =
  useUserStoreHook().dictionaryMap["sysOperationLog.businessType"] ?? {};

export function useOperationLogHook() {
  const defaultSort: Sort = {
    prop: "operationTime",
    order: "descending"
  };

  const pagination: PaginationProps = {
    total: 0,
    pageSize: 10,
    currentPage: 1,
    background: true
  };

  const timeRange = ref<[string, string] | []>([]);
  const searchFormParams = reactive<OperationLogsQuery>({
    beginTime: undefined,
    endTime: undefined,
    businessType: undefined,
    requestModule: undefined,
    status: undefined,
    username: undefined,
    timeRangeColumn: defaultSort.prop
  });

  const dataList = ref<OperationLogDTO[]>([]);
  const pageLoading = ref(true);
  const multipleSelection = ref<number[]>([]);

  const columns: TableColumnList = [
    { type: "selection", align: "left" },
    { label: "日志ID", prop: "operationId", minWidth: 100 },
    { label: "业务模块", prop: "requestModule", minWidth: 120 },
    {
      label: "业务类型",
      prop: "businessType",
      minWidth: 120,
      cellRenderer: ({ row, props }) => {
        const current = businessTypeMap[row.businessType] ?? {
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
    { label: "请求方式", prop: "requestMethod", minWidth: 120 },
    { label: "操作人", prop: "username", minWidth: 120 },
    { label: "操作IP", prop: "operatorIp", minWidth: 120 },
    {
      label: "状态",
      prop: "status",
      minWidth: 120,
      cellRenderer: ({ row, props }) => {
        const current = operationLogStatusMap[row.status] ?? {
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
      label: "操作时间",
      minWidth: 160,
      prop: "operationTime",
      sortable: "custom",
      formatter: ({ operationTime }) =>
        dayjs(operationTime).format("YYYY-MM-DD HH:mm:ss")
    },
    { label: "操作", fixed: "right", width: 140, slot: "operation" }
  ];

  async function onSearch() {
    pagination.currentPage = 1;
    await getOperationLogList();
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

  async function getOperationLogList(sort: Sort = defaultSort) {
    pageLoading.value = true;
    if (sort) {
      CommonUtils.fillSortParams(searchFormParams, sort);
    }
    CommonUtils.fillPaginationParams(searchFormParams, pagination);
    CommonUtils.fillTimeRangeParams(searchFormParams, timeRange.value);

    try {
      const { data } = await getOperationLogListApi(toRaw(searchFormParams));
      dataList.value = data.rows ?? [];
      pagination.total = data.total ?? 0;
    } finally {
      pageLoading.value = false;
    }
  }

  function handleSelectionChange(rows: OperationLogDTO[]) {
    multipleSelection.value = rows.map(item => item.operationId);
  }

  function handlePageSizeChange(pageSize: number) {
    pagination.pageSize = pageSize;
    pagination.currentPage = 1;
    getOperationLogList();
  }

  function handlePageCurrentChange(currentPage: number) {
    pagination.currentPage = currentPage;
    getOperationLogList();
  }

  function handleSortChange(sort: Sort) {
    getOperationLogList(sort);
  }

  async function exportAllExcel(sort: Sort = defaultSort) {
    if (sort) {
      CommonUtils.fillSortParams(searchFormParams, sort);
    }
    CommonUtils.fillPaginationParams(searchFormParams, pagination);
    CommonUtils.fillTimeRangeParams(searchFormParams, timeRange.value);
    exportOperationLogExcelApi(toRaw(searchFormParams), "操作日志.xls");
  }

  async function handleDelete(row: OperationLogDTO) {
    await deleteOperationLogApi([row.operationId]);
    message(`删除操作日志 ${row.operationId} 成功`, { type: "success" });
    getOperationLogList();
  }

  async function handleBulkDelete(tableRef) {
    if (multipleSelection.value.length === 0) {
      message("请先选择要删除的日志", { type: "warning" });
      return;
    }

    ElMessageBox.confirm(
      `确认删除选中的操作日志：${multipleSelection.value.join(", ")} 吗？`,
      "系统提示",
      {
        confirmButtonText: "确认",
        cancelButtonText: "取消",
        type: "warning",
        draggable: true
      }
    )
      .then(async () => {
        await deleteOperationLogApi(multipleSelection.value);
        message("批量删除成功", { type: "success" });
        getOperationLogList();
      })
      .catch(() => {
        message("已取消删除", { type: "info" });
        tableRef?.getTableRef?.().clearSelection();
      });
  }

  function openDialog(row: OperationLogDTO) {
    addDialog({
      title: "日志详情",
      width: "60%",
      draggable: true,
      fullscreenIcon: false,
      closeOnClickModal: true,
      contentRenderer: () => h(descriptionForm, row),
      footerButtons: [
        {
          label: "关闭",
          text: true,
          size: "large",
          bg: true,
          btnClick: ({ dialog: { options, index } }) => {
            closeDialog(options, index);
          }
        }
      ]
    });
  }

  async function handleClean() {
    try {
      await ElMessageBox.confirm(
        "确认清空全部操作日志吗？此操作不可恢复。",
        "系统提示",
        { confirmButtonText: "确认清空", cancelButtonText: "取消", type: "warning" }
      );
      await cleanOperationLogApi();
      message("已清空全部操作日志", { type: "success" });
      getOperationLogList();
    } catch (e: any) {
      if (e !== "cancel") {
        message("清空失败", { type: "error" });
      }
    }
  }

  onMounted(() => {
    getOperationLogList();
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
    openDialog,
    getOperationLogList,
    handleDelete,
    handleClean,
    handleBulkDelete,
    handleSelectionChange,
    handlePageSizeChange,
    handlePageCurrentChange,
    handleSortChange
  };
}
