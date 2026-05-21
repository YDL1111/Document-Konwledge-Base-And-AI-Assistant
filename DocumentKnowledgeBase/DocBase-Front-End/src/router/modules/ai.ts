const Layout = () => import("@/layout/index.vue");

const aiChatRoute: RouteConfigsTable = {
  path: "/ai/chat",
  name: "AiChatRoot",
  component: Layout,
  redirect: "/ai/chat/index",
  meta: {
    title: "AI问答",
    showLink: false
  },
  children: [
    {
      path: "/ai/chat/index",
      name: "AiChat",
      component: () => import("@/views/ai/chat/index.vue"),
      meta: {
        title: "AI问答",
        showLink: false
      }
    }
  ]
};

const aiAuditRoute: RouteConfigsTable = {
  path: "/ai/audit",
  name: "AiAuditRoot",
  component: Layout,
  redirect: "/ai/audit/index",
  meta: {
    title: "AI审计",
    showLink: false
  },
  children: [
    {
      path: "/ai/audit/index",
      name: "AiAudit",
      component: () => import("@/views/ai/audit/index.vue"),
      meta: {
        title: "AI审计",
        showLink: false
      }
    }
  ]
};

export default [aiChatRoute, aiAuditRoute];
