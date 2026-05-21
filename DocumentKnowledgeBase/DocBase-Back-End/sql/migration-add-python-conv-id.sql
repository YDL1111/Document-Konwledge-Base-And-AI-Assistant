-- ===================================================================
-- 第二阶段：python_conv_id 持久化
-- 在 ai_chat_session 表增加 python_conv_id 字段
-- 用于持久化保存 Python AI 服务返回的会话 ID
-- 替换第一阶段的 ConcurrentHashMap 内存映射方案
-- ===================================================================

ALTER TABLE `ai_chat_session`
    ADD COLUMN `python_conv_id` INT DEFAULT NULL COMMENT 'Python AI 服务会话ID'
    AFTER `last_message_time`;
