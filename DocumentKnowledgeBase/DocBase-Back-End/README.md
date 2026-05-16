# DocBase Back End

企业文档知识库系统后端工程，基于 Spring Boot 3、Spring Security 6、MyBatis-Plus、Redis、MySQL 构建。

## 项目定位

本项目用于将通用后台框架改造为企业内部文档知识库系统，当前重点建设方向包括：

- 知识库分类管理
- 文档管理与版本管理
- 文档入库任务
- AI 问答与审计
- 权限、菜单、角色控制

## 技术栈

- Java 17
- Spring Boot 3.2.6
- Spring Security 6
- MyBatis-Plus
- Redis
- MySQL
- Maven 多模块

## 模块说明

- `docbase-admin`：管理后台启动模块与控制器入口
- `docbase-common`：通用配置、工具类、基础常量
- `docbase-domain`：领域服务、业务模型、应用服务
- `docbase-infrastructure`：缓存、持久化、框架集成、基础设施配置

## 当前业务能力

- 系统登录、用户、角色、菜单、部门等基础后台能力
- 企业文档知识库菜单与权限初始化
- 知识库、文档、入库任务、AI 模块的最小后端骨架
- 前端知识库相关占位页面与动态路由接入

## 本地启动

### 1. 准备环境

- JDK 17
- Maven 3.9+
- MySQL 8.x
- Redis

### 2. 初始化数据库

先导入原系统基础表，再执行知识库增量脚本：

- `knowledge-base-init.sql`
- `knowledge-base-menu-init.sql`

### 3. 修改开发环境配置

配置文件位置：

- `docbase-admin/src/main/resources/application-dev.yml`

重点确认：

- MySQL 连接
- Redis 连接
- 日志目录
- 文件存储目录

### 4. 启动后端

直接运行：

- `com.docbase.admin.DocBaseAdminApplication`

或在命令行执行：

```bash
mvn -pl docbase-admin spring-boot:run "-Dspring-boot.run.profiles=basic,dev"
```

## 接口文档

系统启动后可访问：

- `/swagger-ui/index.html`

## 改造说明

该项目来源于开源后台工程，当前已经完成第一轮企业文档知识库方向改造：

- Java 17 / Spring Boot 3 升级
- `javax` 到 `jakarta` 迁移
- 包名与模块命名统一为 `docbase`
- 新增知识库核心表设计与初始化 SQL
- 新增知识库菜单、权限与前端占位页面
- 新增知识库与 AI 模块最小后端骨架

后续将继续完善：

- 文档上传、解析、版本流转
- ACL 权限控制
- 入库任务状态流转
- AI 问答联调
- 企业级文档检索体验
