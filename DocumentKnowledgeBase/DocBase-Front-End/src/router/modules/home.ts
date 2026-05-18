const { VITE_HIDE_HOME } = import.meta.env;
const Layout = () => import("@/layout/index.vue");

export default {
  path: "/",
  name: "Home",
  component: Layout,
  redirect: "/workspace",
  meta: {
    icon: "dashboard",
    title: "知识库工作台",
    rank: 0
  },
  children: [
    {
      path: "/workspace",
      name: "HomeWorkspace",
      component: () => import("@/views/knowledge/workspace/index.vue"),
      meta: {
        title: "知识库工作台",
        showLink: VITE_HIDE_HOME === "true" ? false : true
      }
    }
  ]
} as RouteConfigsTable;
