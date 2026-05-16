# DocBase Front End

企业文档知识库系统前端工程，基于 Vue 3、TypeScript、Vite、Element Plus 构建。

## 项目定位

本项目是企业文档知识库系统的管理端前端，当前主要承载：

- 知识库工作台
- 分类管理
- 文档管理
- 入库任务
- AI 问答
- AI 审计

## 技术栈

- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia
- Vue Router
- pnpm

## 本地启动

### 1. 安装依赖

推荐使用 `pnpm`：

```bash
pnpm install
```

如果本地不是在 Git 根目录执行安装，`husky` 可能报错，可以临时跳过：

```powershell
$env:HUSKY=0
pnpm install
```

### 2. 启动开发环境

```bash
pnpm dev
```

默认访问地址：

- [http://localhost:80](http://localhost:80)

## 当前改造进度

已完成：

- 前端工程名称统一为 `DocBase`
- 知识库与 AI 模块路由占位页面接入
- 动态菜单可显示并可点击进入对应页面

后续计划：

- 对接真实知识库后端接口
- 完善列表查询、详情、上传、预览交互
- 优化企业知识库首页与导航体验
