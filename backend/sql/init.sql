CREATE DATABASE IF NOT EXISTS zerocode
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE zerocode;

CREATE TABLE IF NOT EXISTS `user`
(
    id           BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    userAccount  VARCHAR(256) NOT NULL COMMENT '账号',
    userPassword VARCHAR(512) NOT NULL COMMENT '密码（BCrypt 哈希）',
    userName     VARCHAR(256) NULL COMMENT '用户昵称',
    userAvatar   VARCHAR(1024) NULL COMMENT '用户头像',
    userProfile  VARCHAR(512) NULL COMMENT '用户简介',
    userRole     VARCHAR(256) DEFAULT 'user' NOT NULL COMMENT '用户角色：user/admin',
    editTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '编辑时间',
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) COMMENT '用户' COLLATE = utf8mb4_unicode_ci;

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
