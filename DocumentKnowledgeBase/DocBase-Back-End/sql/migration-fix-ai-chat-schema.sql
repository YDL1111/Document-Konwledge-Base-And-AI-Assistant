-- ===================================================================
-- Align ai_chat_session / ai_chat_message with current Java code.
-- Run this on an existing database before using AI chat.
-- ===================================================================

ALTER TABLE `ai_chat_session`
    ADD COLUMN IF NOT EXISTS `python_conv_id` INT DEFAULT NULL COMMENT 'Python conversation ID'
    AFTER `last_message_time`;

ALTER TABLE `ai_chat_message`
    ADD COLUMN IF NOT EXISTS `sources_json` LONGTEXT DEFAULT NULL COMMENT 'Sources JSON'
    AFTER `message_content`,
    ADD COLUMN IF NOT EXISTS `python_conv_id` INT DEFAULT NULL COMMENT 'Python conversation ID'
    AFTER `sources_json`,
    ADD COLUMN IF NOT EXISTS `kb_id` INT DEFAULT NULL COMMENT 'Knowledge base ID'
    AFTER `python_conv_id`,
    ADD COLUMN IF NOT EXISTS `error_flag` TINYINT NOT NULL DEFAULT 0 COMMENT 'Error flag: 0=normal, 1=error'
    AFTER `model_name`,
    ADD COLUMN IF NOT EXISTS `error_message` VARCHAR(500) DEFAULT NULL COMMENT 'Error message'
    AFTER `error_flag`,
    ADD COLUMN IF NOT EXISTS `creator` VARCHAR(64) DEFAULT NULL COMMENT 'Creator username'
    AFTER `create_time`,
    ADD COLUMN IF NOT EXISTS `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT 'Delete flag'
    AFTER `creator`;

ALTER TABLE `ai_chat_message`
    MODIFY COLUMN `model_name` VARCHAR(64) DEFAULT NULL COMMENT 'Model name';
