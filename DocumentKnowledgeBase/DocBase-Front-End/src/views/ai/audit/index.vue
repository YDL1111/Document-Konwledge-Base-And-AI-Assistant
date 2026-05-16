<script setup lang="ts">
defineOptions({
  name: "AiAudit"
});
</script>

<template>
  <div class="audit-page">
    <section class="panel">
      <p class="eyebrow">AI Audit</p>
      <h1>AI审计</h1>
      <p>
        这里将用于查看问答审计日志、检索命中情况、模型消耗和异常记录，满足企业场景下的可追溯要求。
      </p>
    </section>

    <section class="panel secondary">
      <h2>计划展示字段</h2>
      <p>用户、部门、问题摘要、命中数、最高得分、tokens、耗时、结果状态。</p>
    </section>
  </div>
</template>

<style scoped>
.audit-page {
  min-height: 100%;
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(99, 102, 241, 0.16), transparent 24%),
    linear-gradient(180deg, #f7f8ff 0%, #f8fafc 100%);
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
  color: #4f46e5;
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

.secondary {
  background: linear-gradient(135deg, rgba(224, 231, 255, 0.78), rgba(255, 255, 255, 0.94));
}
</style>
