<script setup lang="ts">
defineOptions({
  name: "KnowledgeCategory"
});

const quickActions = [
  "维护知识分类树",
  "配置分类排序与状态",
  "后续接入部门归属与可见范围"
];
</script>

<template>
  <div class="placeholder-page">
    <div class="hero-card">
      <p class="eyebrow">Knowledge Category</p>
      <h1>分类管理</h1>
      <p class="summary">
        这里将承接企业知识分类树、分类状态管理和部门归属配置。当前先提供页面占位，保证菜单和动态路由可正常访问。
      </p>
    </div>

    <div class="info-grid">
      <section class="panel">
        <h2>当前规划</h2>
        <ul>
          <li v-for="item in quickActions" :key="item">{{ item }}</li>
        </ul>
      </section>

      <section class="panel accent">
        <h2>后续接口</h2>
        <p>`knowledge:category:list`、`add`、`edit`、`remove`</p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.placeholder-page {
  min-height: 100%;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.12), transparent 24%),
    linear-gradient(180deg, #f8fbff 0%, #f6f8fc 100%);
}

.hero-card,
.panel {
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
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-card h1,
.panel h2 {
  margin: 0;
  color: #0f172a;
}

.hero-card h1 {
  font-size: 30px;
}

.summary,
.panel p,
.panel li {
  color: #334155;
  line-height: 1.7;
}

.summary {
  margin: 14px 0 0;
  max-width: 760px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
  margin-top: 20px;
}

.panel {
  padding: 22px;
}

.panel h2 {
  margin-bottom: 12px;
  font-size: 18px;
}

.panel ul {
  margin: 0;
  padding-left: 18px;
}

.accent {
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.82), rgba(255, 255, 255, 0.94));
}
</style>
