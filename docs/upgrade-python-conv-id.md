# python_conv_id 持久化升级说明

## 适用版本

第二阶段：替换 ConcurrentHashMap，持久化 Python 会话关联

## 需要执行的 SQL 脚本

```sql
ALTER TABLE `ai_chat_session`
    ADD COLUMN `python_conv_id` INT DEFAULT NULL COMMENT 'Python AI 服务会话ID'
    AFTER `last_message_time`;
```

脚本文件位置：`sql/migration-add-python-conv-id.sql`

执行方式：
```bash
mysql -u root -p docbase_knowledge < sql/migration-add-python-conv-id.sql
```

## 影响的表和字段

| 表 | 操作 | 字段 | 类型 | 说明 |
|----|------|------|------|------|
| `ai_chat_session` | 新增列 | `python_conv_id` | `INT DEFAULT NULL` | 存储 Python 侧 `conversations.id`，用于多轮对话上下文关联 |

不涉及其他表。无数据迁移。存量 session 的 `python_conv_id` 为 null，首次提问时自动补齐。

## 不执行 SQL 的后果

Java 服务启动正常，但 `AiChatApplicationService.query()` 第 42 行 `session.getPythonConvId()` 时将触发 MyBatis-Plus 映射异常：

```
Unknown column 'python_conv_id' in 'field list'
```

所有 AI 问答请求失败。**必须在启动 Java 服务前执行此 SQL。**

## MyBatis-Plus 行为验证

- `getById()` → `SELECT * FROM ai_chat_session WHERE session_id=?` → 自动读取所有列，包括 `python_conv_id`
- `updateById(entity)` → 生成全字段 UPDATE，`python_conv_id` 随其他字段一起持久化
- 版本 MyBatis-Plus 3.5.7，`FieldStrategy.DEFAULT`，无特殊配置覆盖

## 验证记录

| 步骤 | 内容 | 结果 |
|------|------|------|
| 1 | Python `conversations.id` 类型确认 | `INT`，示例值 `1, 2, 3` |
| 2 | SQL 迁移执行 | `python_conv_id int YES NULL` 添加成功 |
| 3 | 模拟首次 query：`pythonConvId=null` → Python 返回 `conv_id=3` → `UPDATE SET python_conv_id=3` → DB 确认值 `3` | 通过 |
| 4 | 模拟二次 query：从 DB 读取 `python_conv_id=3` → 传给 Python `conv_id=3` → Python 返回 `conv_id=3`（复用会话） | 通过 |
| 5 | MyBatis-Plus `updateById` 全字段更新确认 | 通过 |

## 回滚

如需回滚到 ConcurrentHashMap 方案：
```sql
ALTER TABLE `ai_chat_session` DROP COLUMN `python_conv_id`;
```
