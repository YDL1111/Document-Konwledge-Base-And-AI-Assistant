# DocBase Front End

企业文档知识库系统前端工程，基于 Vue 3、TypeScript、Vite、Element Plus 构建。

## 项目定位

本项目是企业文档知识库系统的管理端前端，当前主要承载：

- 知识库工作台
- 分类管理
- 文档管理
- 导入任务
- AI 问答
- 轻量 Agent 模式与执行轨迹展示
- 系统管理、菜单权限与按钮权限控制

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

### 3. 常用检查命令

```bash
pnpm.cmd typecheck
pnpm build
```

如果在 PowerShell 中遇到脚本执行策略限制，优先使用 `pnpm.cmd`。

## 当前业务能力

已完成：

- 前端工程名称统一为 `DocBase`
- 动态菜单、角色菜单权限与按钮权限指令接入
- 知识库分类列表、新增、编辑、删除
- 文档列表、上传、新增、编辑、删除、审核状态展示
- 文档编辑/删除确认提示与接口刷新列表
- 导入任务列表、执行、批量处理、重试、查询状态
- AI 问答会话列表、历史消息展示、SSE 回答接收
- Agent 模式开关和工具调用执行轨迹展示

## 后端联调说明

前端默认通过 `VITE_APP_BASE_API` 访问 Java 后端，例如：

- `/dev-api`：本地开发代理
- `/ai/chat/stream`：普通 AI 问答
- `/ai/chat/agent/stream`：轻量 Agent 问答
- `/knowledge/document/*`：文档管理
- `/knowledge/ingest/tasks/*`：导入任务管理

知识库相关按钮会通过权限标识控制展示，例如：

- `knowledge:document:add`
- `knowledge:document:edit`
- `knowledge:document:remove`
- `knowledge:document:ingest`
- `knowledge:category:add`
- `knowledge:category:edit`
- `knowledge:category:remove`
