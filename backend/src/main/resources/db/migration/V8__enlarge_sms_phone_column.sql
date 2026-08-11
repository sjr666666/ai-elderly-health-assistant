-- V8: sms_notification_log.phone 扩容
-- 原因：手机号 AES 加密后 Base64 密文约 44 字符，varchar(20) 放不下，
-- 导致 sendNotification 插入报 Data too long（存量 bug：漏服提醒/紧急通知路径同样受影响）
ALTER TABLE sms_notification_log
    MODIFY COLUMN phone VARCHAR(128) NULL COMMENT '接收手机号（AES加密存储）';
