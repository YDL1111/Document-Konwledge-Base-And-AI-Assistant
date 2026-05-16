<script setup lang="ts">
defineOptions({
  name: "KnowledgeWorkspace"
});

const cards = [
  {
    title: "文档资产管理",
    description:
      "统一承接企业制度、流程、规范、项目资料和知识沉淀，作为后续知识库建设的核心入口。"
  },
  {
    title: "知识入库任务",
    description:
      "后续接入文档解析、切块、向量化与任务状态跟踪，支撑企业知识库的数据准备流程。"
  },
  {
    title: "智能问答入口",
    description:
      "由 Java 后台统一鉴权并转发到 Python RAG 服务，形成企业内部知识问答能力。"
  }
];
</script>

<template>
  <div class="workspace-page">
    <section class="hero">
      <p class="eyebrow">Enterprise Knowledge Base</p>
      <h1>企业文档知识库工作台</h1>
      <p class="summary">
        当前阶段已完成项目定位收敛，后续将在这里承接文档管理、知识入库和智能问答能力。
      </p>
    </section>

    <section class="card-grid">
      <article v-for="card in cards" :key="card.title" class="card">
        <h2>{{ card.title }}</h2>
        <p>{{ card.description }}</p>
      </article>
    </section>

    <section class="roadmap">
      <h2>当前阶段保留的系统底座</h2>
      <p>用户、角色、部门、菜单、参数配置、公告、登录日志、操作日志。</p>
      <h2>当前阶段收起的无关能力</h2>
      <p>岗位管理、监控页、权限演示页、通用欢迎页展示。</p>
    </section>
  </div>
</template>

<style scoped>
.workspace-page {
  min-height: 100%;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(20, 184, 166, 0.16), transparent 28%),
    radial-gradient(circle at right center, rgba(14, 116, 144, 0.12), transparent 24%),
    linear-gradient(180deg, #f4fbfb 0%, #f7f8fc 100%);
}

.hero,
.roadmap,
.card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.hero,
.roadmap {
  padding: 28px;
}

.eyebrow {
  margin: 0 0 12px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero h1,
.roadmap h2,
.card h2 {
  color: #0f172a;
}

.hero h1 {
  margin: 0;
  font-size: 32px;
  line-height: 1.2;
}

.summary,
.roadmap p,
.card p {
  color: #334155;
  line-height: 1.7;
}

.summary {
  max-width: 760px;
  margin: 16px 0 0;
  font-size: 15px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
  margin: 20px 0;
}

.card {
  padding: 22px;
}

.card h2,
.roadmap h2 {
  margin: 0 0 10px;
  font-size: 18px;
}

.roadmap {
  display: grid;
  gap: 8px;
}

.roadmap p {
  margin: 0;
}
</style>
