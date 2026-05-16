<script setup lang="ts">
defineOptions({
  name: "KnowledgeDocument"
});

const modules = [
  "文档列表与筛选",
  "上传、预览、下载",
  "版本流转与入库触发"
];
</script>

<template>
  <div class="placeholder-page">
    <div class="hero-card">
      <p class="eyebrow">Knowledge Document</p>
      <h1>文档管理</h1>
      <p class="summary">
        这里会成为企业知识库的核心页面，用于管理文档元数据、版本、分类归档和可见权限。当前先完成前端入口占位。
      </p>
    </div>

    <div class="module-grid">
      <article v-for="item in modules" :key="item" class="module-card">
        <h2>{{ item }}</h2>
        <p>已预留对应权限码与菜单入口，待后端最小骨架接入后即可联调。</p>
      </article>
    </div>
  </div>
</template>

<style scoped>
.placeholder-page {
  min-height: 100%;
  padding: 24px;
  background:
    radial-gradient(circle at right top, rgba(16, 185, 129, 0.14), transparent 26%),
    linear-gradient(180deg, #f6fffb 0%, #f8fafc 100%);
}

.hero-card,
.module-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.06);
}

.hero-card {
  padding: 28px;
}

.eyebrow {
  margin: 0 0 12px;
  color: #059669;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-card h1,
.module-card h2 {
  margin: 0;
  color: #0f172a;
}

.hero-card h1 {
  font-size: 30px;
}

.summary,
.module-card p {
  color: #334155;
  line-height: 1.7;
}

.summary {
  margin-top: 14px;
  max-width: 760px;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
  margin-top: 20px;
}

.module-card {
  padding: 22px;
}

.module-card h2 {
  font-size: 18px;
  margin-bottom: 10px;
}
</style>
