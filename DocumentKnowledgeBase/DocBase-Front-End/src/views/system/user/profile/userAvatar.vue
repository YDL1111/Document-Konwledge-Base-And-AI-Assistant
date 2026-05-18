<script setup lang="ts">
import ReCropper from "@/components/ReCropper";
import { formatBytes } from "@pureadmin/utils";
import { ref } from "vue";
import { uploadUserAvatarApi } from "@/api/system/user";
import { useUserStoreHook } from "@/store/modules/user";
import { message } from "@/utils/message";
import type { CurrentUserInfoDTO } from "@/api/common/login";

defineProps<{
  user?: CurrentUserInfoDTO;
}>();

const currentUser = (useUserStoreHook().currentUserInfo ??
  {}) as CurrentUserInfoDTO;

const infos = ref();
const imgBlob = ref();
const refCropper = ref();
const showPopover = ref(false);
const cropperImg = ref<string>(
  currentUser.avatar ? import.meta.env.VITE_APP_BASE_API + currentUser.avatar : ""
);

function onCropper({ base64, blob, info }) {
  infos.value = info;
  imgBlob.value = blob;
  cropperImg.value = base64;
}

const open = ref(false);
const visible = ref(false);

function uploadImg() {
  const formData = new FormData();
  formData.append("avatarfile", imgBlob.value);
  uploadUserAvatarApi(formData).then(() => {
    open.value = false;
    message("上传头像成功", {
      type: "success"
    });
    visible.value = false;
  });
}
</script>

<template>
  <div class="user-info-head" @click="open = true">
    <el-avatar :size="120" :src="cropperImg" />
  </div>
  <el-dialog
    v-model="open"
    title="修改头像"
    width="900px"
    append-to-body
    @opened="visible = true"
    @close="visible = false"
  >
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="font-medium">右键头像可打开裁剪操作菜单</span>
        </div>
      </template>
      <el-popover
        :visible="showPopover"
        placement="right"
        width="300px"
        :teleported="false"
      >
        <template #reference>
          <ReCropper
            ref="refCropper"
            class="w-[500px]"
            :src="cropperImg"
            circled
            @cropper="onCropper"
            @readied="showPopover = true"
          />
        </template>
        <div class="flex flex-wrap items-center justify-center text-center">
          <el-image
            v-if="cropperImg"
            :src="cropperImg"
            :preview-src-list="Array.of(cropperImg)"
            fit="cover"
          />
          <div v-if="infos" class="mt-1">
            <p>
              图像尺寸：{{ parseInt(infos.width) }} x
              {{ parseInt(infos.height) }} 像素
            </p>
            <p>
              文件大小：{{ formatBytes(infos.size) }}（{{ infos.size }} 字节）
            </p>
          </div>
        </div>
      </el-popover>
    </el-card>
    <template #footer>
      <div>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="uploadImg">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.user-info-head {
  position: relative;
  display: inline-block;
  height: 120px;
}

.user-info-head:hover::after {
  position: absolute;
  inset: 0;
  font-size: 24px;
  font-style: normal;
  line-height: 110px;
  color: #eee;
  cursor: pointer;
  content: "+";
  background: rgb(0 0 0 / 50%);
  border-radius: 50%;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
</style>
