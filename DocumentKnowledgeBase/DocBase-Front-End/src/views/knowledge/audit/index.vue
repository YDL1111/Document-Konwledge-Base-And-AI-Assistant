<script setup lang="ts">
import { onMounted } from "vue";
import { useRouter } from "vue-router";

defineOptions({
  name: "KnowledgeAudit"
});

const router = useRouter();

onMounted(() => {
  router.replace("/knowledge/document/index?status=2");
});
</script>

<template>
  <div class="audit-redirect">
    <el-empty description="正在进入待审核文档列表..." />
  </div>
</template>

<style scoped>
.audit-redirect {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
