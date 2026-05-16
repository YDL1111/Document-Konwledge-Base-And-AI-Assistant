<script setup lang="ts">
defineOptions({
  name: "KnowledgeIngestTask"
});
</script>

<template>
  <div class="task-page">
    <section class="panel">
      <p class="eyebrow">Knowledge Ingest</p>
      <h1>入库任务</h1>
      <p>
        这里将展示文档解析、切块、向量化和重试任务状态。当前页面先作为任务中心占位，保证菜单与动态路由可访问。
      </p>
    </section>

    <section class="panel soft">
      <h2>预留能力</h2>
      <p>任务列表、失败原因、traceId、重试按钮、任务详情抽屉。</p>
    </section>
  </div>
</template>

<style scoped>
.task-page {
  min-height: 100%;
  padding: 24px;
  background:
    radial-gradient(circle at top center, rgba(245, 158, 11, 0.15), transparent 25%),
    linear-gradient(180deg, #fffaf2 0%, #f8fafc 100%);
}

.panel {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 20px;
  padding: 26px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.06);
}

.panel + .panel {
  margin-top: 18px;
}

.eyebrow {
  margin: 0 0 12px;
  color: #d97706;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.panel h1,
.panel h2 {
  margin: 0 0 12px;
  color: #0f172a;
}

.panel p {
  margin: 0;
  color: #334155;
  line-height: 1.7;
}

.soft {
  background: linear-gradient(135deg, rgba(254, 240, 199, 0.75), rgba(255, 255, 255, 0.94));
}
</style>
