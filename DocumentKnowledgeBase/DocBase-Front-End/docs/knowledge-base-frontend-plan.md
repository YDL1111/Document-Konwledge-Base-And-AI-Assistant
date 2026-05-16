# 企业文档知识库前端改造骨架

## 当前定位

前端不再延续通用后台演示站思路，而是收敛为企业文档知识库管理端。

## 当前保留页面

- 登录
- 用户管理
- 角色管理
- 部门管理
- 菜单管理
- 参数配置
- 公告管理
- 登录日志
- 操作日志
- 知识库工作台占位页

## 已收起页面

- 权限演示页
- 岗位管理
- 监控相关页面
- 通用欢迎页

## 后续建议新增的前端业务目录

建议后续围绕以下结构逐步落地：

- `src\views\knowledge\category`
- `src\views\knowledge\document`
- `src\views\knowledge\ingest`
- `src\views\knowledge\preview`
- `src\views\ai\chat`
- `src\api\knowledge`
- `src\api\ai`

## 首页调整说明

当前首页已替换为“企业文档知识库工作台”占位页，用来承接之后的业务入口聚合：

- 文档资产管理
- 知识入库任务
- 智能问答入口
