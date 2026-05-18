<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { getKnowledgeCategoryListApi } from "@/api/knowledge/category";
import { getKnowledgeDocumentListApi } from "@/api/knowledge/document";
import { getKnowledgeIngestTaskListApi } from "@/api/knowledge/ingest";

defineOptions({
  name: "KnowledgeWorkspace"
});

const categoryCount = ref(0);
const documentCount = ref(0);
const pendingAuditCount = ref(0);
const ingestCount = ref(0);
const loading = ref(false);

const cards = computed(() => [
  { label: "分类总数", value: categoryCount.value, tone: "blue" },
  { label: "文档总数", value: documentCount.value, tone: "green" },
  { label: "待审核文档", value: pendingAuditCount.value, tone: "amber" },
  { label: "入库任务数", value: ingestCount.value, tone: "slate" }
]);

async function loadDashboard() {
  loading.value = true;
  try {
    const [categoryRes, documentRes, pendingRes, ingestRes] = await Promise.all([
      getKnowledgeCategoryListApi({ pageNum: 1, pageSize: 1 }),
      getKnowledgeDocumentListApi({ pageNum: 1, pageSize: 1 }),
      getKnowledgeDocumentListApi({ pageNum: 1, pageSize: 1, status: 2 }),
      getKnowledgeIngestTaskListApi({ pageNum: 1, pageSize: 1 })
    ]);
    categoryCount.value = categoryRes.data.total ?? 0;
    documentCount.value = documentRes.data.total ?? 0;
    pendingAuditCount.value = pendingRes.data.total ?? 0;
    ingestCount.value = ingestRes.data.total ?? 0;
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadDashboard();
});
</script>

<template>
  <div class="workspace-page" v-loading="loading">
    <section class="hero">
      <div>
        <p class="eyebrow">DocBase Workspace</p>
        <h1>知识库工作台</h1>
        <p class="summary">
          这里聚合知识库的分类、文档、审核与入库任务数据，方便管理员先看到全局状态，再进入具体模块处理。
        </p>
      </div>
    </section>

    <section class="card-grid">
      <article v-for="card in cards" :key="card.label" class="metric-card" :data-tone="card.tone">
        <p class="metric-label">{{ card.label }}</p>
        <p class="metric-value">{{ card.value }}</p>
      </article>
    </section>

    <section class="insight-grid">
      <article class="panel">
        <h2>当前建议处理顺序</h2>
        <ul>
          <li>先维护分类树，保证文档上传时能正常归类。</li>
          <li>优先处理“待审核文档”，形成上传到发布的闭环。</li>
          <li>关注失败的入库任务，为后续 AI 检索与问答做准备。</li>
        </ul>
      </article>
      <article class="panel accent">
        <h2>本阶段已打通</h2>
        <ul>
          <li>文档上传与版本记录</li>
          <li>文档详情与审核流</li>
          <li>分类管理与入库任务查看</li>
        </ul>
      </article>
    </section>
  </div>
</template>

<style scoped>
.workspace-page {
  min-height: 100%;
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.12), transparent 26%),
    linear-gradient(180deg, #f8fbff 0%, #f8fafc 100%);
}

.hero,
.metric-card,
.panel {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.06);
}

.hero {
  padding: 28px;
}

.eyebrow {
  margin: 0 0 12px;
  color: #0284c7;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero h1,
.panel h2 {
  margin: 0;
  color: #0f172a;
}

.summary,
.panel li {
  color: #334155;
  line-height: 1.7;
}

.summary {
  margin: 14px 0 0;
  max-width: 760px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
  margin-top: 20px;
}

.metric-card {
  padding: 22px;
}

.metric-label {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.metric-value {
  margin: 12px 0 0;
  color: #0f172a;
  font-size: 34px;
  font-weight: 700;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
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
  background: linear-gradient(135deg, rgba(224, 242, 254, 0.8), rgba(255, 255, 255, 0.94));
}
</style>
