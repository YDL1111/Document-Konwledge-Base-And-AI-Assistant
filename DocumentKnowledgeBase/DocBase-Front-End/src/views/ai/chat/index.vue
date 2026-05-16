<script setup lang="ts">
defineOptions({
  name: "AiChat"
});

const highlights = [
  "企业知识问答入口",
  "引用文档片段展示",
  "会话历史与权限隔离"
];
</script>

<template>
  <div class="ai-page">
    <section class="hero-card">
      <p class="eyebrow">AI Chat</p>
      <h1>AI问答</h1>
      <p class="summary">
        这里将承接基于企业文档知识库的问答能力，包括会话管理、引用来源展示和权限隔离后的问答访问。
      </p>
    </section>

    <section class="chips">
      <span v-for="item in highlights" :key="item" class="chip">{{ item }}</span>
    </section>
  </div>
</template>

<style scoped>
.ai-page {
  min-height: 100%;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(236, 72, 153, 0.12), transparent 22%),
    radial-gradient(circle at right bottom, rgba(59, 130, 246, 0.12), transparent 24%),
    linear-gradient(180deg, #fff8fc 0%, #f8fafc 100%);
}

.hero-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 20px;
  padding: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.06);
}

.eyebrow {
  margin: 0 0 12px;
  color: #db2777;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-card h1 {
  margin: 0;
  color: #0f172a;
  font-size: 30px;
}

.summary {
  margin: 14px 0 0;
  max-width: 760px;
  color: #334155;
  line-height: 1.7;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.chip {
  padding: 10px 16px;
  border: 1px solid rgba(219, 39, 119, 0.16);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #831843;
  font-size: 14px;
  font-weight: 600;
}
</style>
