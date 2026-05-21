-- ===================================================================
-- Initialize ai_chat_message for first-time deployment.
-- Safe to run repeatedly because it uses CREATE TABLE IF NOT EXISTS.
-- ===================================================================

CREATE TABLE IF NOT EXISTS `ai_chat_message`
(
    `message_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Message ID',
    `session_id`      BIGINT       NOT NULL                COMMENT 'Session ID',
    `message_role`    TINYINT      NOT NULL                COMMENT 'Message role: 1=user, 2=assistant',
    `message_content` LONGTEXT     NOT NULL                COMMENT 'Message content',
    `sources_json`    LONGTEXT     DEFAULT NULL            COMMENT 'Sources JSON',
    `python_conv_id`  INT          DEFAULT NULL            COMMENT 'Python conversation ID',
    `kb_id`           INT          DEFAULT NULL            COMMENT 'Knowledge base ID',
    `model_name`      VARCHAR(64)  DEFAULT NULL            COMMENT 'Model name',
    `error_flag`      TINYINT      NOT NULL DEFAULT 0      COMMENT 'Error flag: 0=normal, 1=error',
    `error_message`   VARCHAR(500) DEFAULT NULL            COMMENT 'Error message',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `creator`         VARCHAR(64)  DEFAULT NULL            COMMENT 'Creator username',
    `del_flag`        TINYINT      NOT NULL DEFAULT 0      COMMENT 'Delete flag',
    PRIMARY KEY (`message_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI chat messages';
