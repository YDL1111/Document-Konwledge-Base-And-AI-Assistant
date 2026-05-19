<script setup lang="ts">
import VDialog from "@/components/VDialog/VDialog.vue";
import { computed, nextTick, reactive, ref } from "vue";
import { useUserStoreHook } from "@/store/modules/user";
import { ElMessage, type ElTree, type FormInstance, type FormRules } from "element-plus";
import {
  addRoleApi,
  updateRoleApi,
  type AddRoleCommand,
  type RoleDTO,
  type UpdateRoleCommand
} from "@/api/system/role";
import type { MenuDTO } from "@/api/system/menu";

interface Props {
  type: "add" | "update";
  modelValue: boolean;
  row?: RoleDTO;
  menuOptions: MenuDTO[];
  buttonOptions: MenuDTO[];
}

interface PermissionGroup {
  key: string;
  title: string;
  description: string;
  parentMenuName: string;
  items: MenuDTO[];
}

const props = defineProps<Props>();
const emits = defineEmits<{
  (e: "update:modelValue", v: boolean): void;
  (e: "success"): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set(v) {
    emits("update:modelValue", v);
  }
});

const formData = reactive<AddRoleCommand | UpdateRoleCommand>({
  roleId: 0,
  dataScope: "",
  menuIds: [],
  remark: "",
  roleKey: "",
  roleName: "",
  roleSort: 1,
  status: "1"
});

const statusList = useUserStoreHook().dictionaryMap["common.status"] ?? {};

const rules: FormRules = {
  roleName: [{ required: true, message: "角色名称不能为空" }],
  roleKey: [{ required: true, message: "权限字符不能为空" }],
  roleSort: [{ required: true, message: "角色排序不能为空" }]
};

const formRef = ref<FormInstance>();
const treeRef = ref<InstanceType<typeof ElTree>>();

const groupedButtonPermissions = computed<PermissionGroup[]>(() => {
  const source = props.buttonOptions ?? [];
  const menuMap = new Map<number, MenuDTO>();

  const collectMenuMap = (menus: MenuDTO[]) => {
    menus.forEach(menu => {
      if (menu.id !== undefined) {
        menuMap.set(menu.id, menu);
      }
      if (menu.children?.length) {
        collectMenuMap(menu.children);
      }
    });
  };

  collectMenuMap(props.menuOptions ?? []);

  const groupMap = new Map<string, PermissionGroup>();

  source.forEach(item => {
    const parentMenu = item.parentId ? menuMap.get(item.parentId) : undefined;
    const topMenu = parentMenu?.parentId ? menuMap.get(parentMenu.parentId) : undefined;
    const parentMenuName = parentMenu?.menuName || "其他功能";
    const topMenuName = topMenu?.menuName || parentMenuName;
    const groupKey = `${topMenuName}-${parentMenuName}`;

    if (!groupMap.has(groupKey)) {
      groupMap.set(groupKey, {
        key: groupKey,
        title: parentMenuName,
        description: buildGroupDescription(parentMenuName),
        parentMenuName: topMenuName,
        items: []
      });
    }

    groupMap.get(groupKey)?.items.push(item);
  });

  return Array.from(groupMap.values())
    .map(group => ({
      ...group,
      items: group.items.sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
    }))
    .sort((a, b) => {
      if (a.parentMenuName === b.parentMenuName) {
        return a.title.localeCompare(b.title, "zh-CN");
      }
      return a.parentMenuName.localeCompare(b.parentMenuName, "zh-CN");
    });
});

function buildGroupDescription(groupName: string) {
  switch (groupName) {
    case "文档管理":
      return "控制文档列表页中的新增、查看、预览、下载、审核等具体操作。";
    case "分类管理":
      return "控制知识分类的查看与维护类操作。";
    case "导入任务":
      return "控制导入任务查看及后续扩展操作。";
    case "AI问答":
      return "控制 AI 问答入口及后续扩展操作。";
    case "用户管理":
      return "控制用户增删改查、导入导出、重置密码等操作。";
    case "角色管理":
      return "控制角色增删改查及授权相关操作。";
    default:
      return "控制该页面下的按钮级操作权限。";
  }
}

function uniqueNumberList(values: Array<number | string>) {
  return Array.from(new Set(values.map(item => Number(item)).filter(item => !Number.isNaN(item))));
}

function handleOpened() {
  const checkedIds = props.row
    ? uniqueNumberList(props.row.selectedMenuList ?? [])
    : [];

  if (props.row) {
    Object.assign(formData, props.row);
    formData.menuIds = checkedIds;
  } else {
    Object.assign(formData, {
      roleId: 0,
      dataScope: "",
      menuIds: [],
      remark: "",
      roleKey: "",
      roleName: "",
      roleSort: 1,
      status: "1"
    });
    formRef.value?.resetFields();
  }

  nextTick(() => {
    treeRef.value?.setCheckedKeys(checkedIds, false);
  });
}

function handleCheckChange() {
  const checkedTreeKeys = uniqueNumberList((treeRef.value?.getCheckedKeys(false) ?? []) as number[]);
  const retainedButtonKeys = (formData.menuIds ?? []).filter(menuId =>
    props.buttonOptions.some(item => item.id === menuId)
  );
  formData.menuIds = uniqueNumberList([...checkedTreeKeys, ...retainedButtonKeys]);
}

function isPermissionChecked(menuId?: number) {
  if (menuId === undefined) return false;
  return (formData.menuIds ?? []).includes(menuId);
}

function handlePermissionChange(menuId: number, checked: boolean) {
  const nextIds = new Set(formData.menuIds ?? []);
  if (checked) {
    nextIds.add(menuId);
  } else {
    nextIds.delete(menuId);
  }
  formData.menuIds = uniqueNumberList(Array.from(nextIds));
}

function isGroupChecked(group: PermissionGroup) {
  return group.items.every(item => item.id !== undefined && isPermissionChecked(item.id));
}

function isGroupIndeterminate(group: PermissionGroup) {
  const checkedCount = group.items.filter(item => item.id !== undefined && isPermissionChecked(item.id)).length;
  return checkedCount > 0 && checkedCount < group.items.length;
}

function toggleGroup(group: PermissionGroup, checked: boolean) {
  const nextIds = new Set(formData.menuIds ?? []);
  group.items.forEach(item => {
    if (item.id === undefined) return;
    if (checked) {
      nextIds.add(item.id);
    } else {
      nextIds.delete(item.id);
    }
  });
  formData.menuIds = uniqueNumberList(Array.from(nextIds));
}

const loading = ref(false);

async function handleConfirm() {
  try {
    loading.value = true;
    if (props.type === "add") {
      await addRoleApi(formData as AddRoleCommand);
    } else {
      await updateRoleApi(formData as UpdateRoleCommand);
    }
    ElMessage.success("提交成功");
    visible.value = false;
    emits("success");
  } catch (e) {
    console.error(e);
    ElMessage.error((e as Error)?.message || "提交失败");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <v-dialog
    show-full-screen
    fixed-body-height
    use-body-scrolling
    :title="type === 'add' ? '新增角色' : '修改角色'"
    v-model="visible"
    :loading="loading"
    @confirm="handleConfirm"
    @cancel="visible = false"
    @opened="handleOpened"
  >
    <el-form ref="formRef" :model="formData" label-width="120px" :rules="rules">
      <el-form-item prop="roleName" label="角色名称" required inline-message>
        <el-input v-model="formData.roleName" />
      </el-form-item>
      <el-form-item prop="roleKey" label="权限字符" required>
        <el-input v-model="formData.roleKey" />
      </el-form-item>
      <el-form-item prop="roleSort" label="角色排序" required>
        <el-input-number :min="1" v-model="formData.roleSort" />
      </el-form-item>
      <el-form-item prop="status" label="角色状态">
        <el-radio-group v-model="formData.status">
          <el-radio
            v-for="item in Object.keys(statusList)"
            :key="item"
            :label="String(statusList[item].value)"
          >
            {{ statusList[item].label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="菜单权限" prop="menuIds">
        <div class="permission-layout">
          <div class="permission-pane">
            <div class="permission-pane__title">菜单权限</div>
            <div class="permission-pane__desc">控制左侧导航和页面入口是否可见。</div>
            <el-tree
              ref="treeRef"
              :props="{ label: 'menuName', children: 'children' }"
              :data="props.menuOptions"
              node-key="id"
              check-strictly
              show-checkbox
              default-expand-all
              check-on-click-node
              :expand-on-click-node="false"
              :default-checked-keys="formData.menuIds"
              @check-change="handleCheckChange"
              style="width: 100%"
            />
          </div>
          <div class="permission-pane permission-pane--buttons">
            <div class="permission-pane__title">操作权限</div>
            <div class="permission-pane__desc">
              控制新增、预览、下载、审核等按钮级功能；可以按组一起控制，也可以单独控制。
            </div>
            <div v-if="groupedButtonPermissions.length" class="permission-groups">
              <div
                v-for="group in groupedButtonPermissions"
                :key="group.key"
                class="permission-group"
              >
                <div class="permission-group__header">
                  <div>
                    <div class="permission-group__title">
                      {{ group.parentMenuName }} / {{ group.title }}
                    </div>
                    <div class="permission-group__desc">{{ group.description }}</div>
                  </div>
                  <el-checkbox
                    :model-value="isGroupChecked(group)"
                    :indeterminate="isGroupIndeterminate(group)"
                    @change="value => toggleGroup(group, Boolean(value))"
                  >
                    整组控制
                  </el-checkbox>
                </div>
                <div class="permission-group__items">
                  <el-checkbox
                    v-for="item in group.items"
                    :key="item.id"
                    :model-value="isPermissionChecked(item.id)"
                    @change="value => handlePermissionChange(item.id!, Boolean(value))"
                  >
                    {{ item.menuName }}
                  </el-checkbox>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无可配置的操作权限" />
          </div>
        </div>
      </el-form-item>
      <el-form-item prop="remark" label="备注" style="margin-bottom: 0">
        <el-input type="textarea" v-model="formData.remark" />
      </el-form-item>
    </el-form>
  </v-dialog>
</template>

<style scoped>
.permission-layout {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(360px, 1fr);
  gap: 16px;
  align-items: start;
}

.permission-pane {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 14px 16px;
  background: var(--el-bg-color);
  min-height: 420px;
}

.permission-pane--buttons {
  background: var(--el-fill-color-blank);
}

.permission-pane__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.permission-pane__desc {
  margin: 6px 0 14px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}

.permission-groups {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 520px;
  overflow-y: auto;
  padding-right: 4px;
}

.permission-group {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  padding: 12px;
}

.permission-group__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
}

.permission-group__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.permission-group__desc {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}

.permission-group__items {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
}

@media (max-width: 1200px) {
  .permission-layout {
    grid-template-columns: 1fr;
  }
}
</style>
