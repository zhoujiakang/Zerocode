USE zerocode;

CREATE TABLE IF NOT EXISTS app (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appName VARCHAR(128) NOT NULL,
    initPrompt TEXT NOT NULL,
    codeGenType VARCHAR(32) NOT NULL,
    userId BIGINT NOT NULL,
    status VARCHAR(32) DEFAULT 'CREATED' NOT NULL,
    lastError VARCHAR(512),
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_app_user (userId, id)
) COMMENT 'AI 生成应用' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appId BIGINT NOT NULL,
    userId BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL COMMENT 'user/assistant/error',
    content LONGTEXT NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_history_app_user (appId, userId, id)
) COMMENT '应用完整用户对话历史' COLLATE = utf8mb4_unicode_ci;
