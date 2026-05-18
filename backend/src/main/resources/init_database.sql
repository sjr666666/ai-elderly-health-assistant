-- =====================================================
-- 老年人用药管理系统 - 数据库初始化脚本
-- =====================================================
-- 版本: 2.0
-- 创建时间: 2026-05-18
-- 描述: 完整的数据库初始化脚本，包含建表和测试数据
-- 兼容: MySQL 5.7+ / MySQL 8.0+
-- =====================================================

-- ----------------------------
-- 脚本配置参数
-- ----------------------------
SET @DB_NAME = 'elderly_medication';
SET @CHARSET = 'utf8mb4';
SET @COLLATE = 'utf8mb4_unicode_ci';

-- ----------------------------
-- 第一部分：安全创建/使用数据库
-- ----------------------------
-- 尝试创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `elderly_medication` 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE elderly_medication;

-- 关闭外键检查，确保表操作顺利进行
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 第二部分：创建数据表
-- ----------------------------

-- ==================== 用户表 ====================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
  `user_id` bigint NOT NULL UNIQUE COMMENT '用户ID（雪花算法生成）',
  `username` varchar(50) NOT NULL UNIQUE COMMENT '登录名',
  `password` varchar(255) NOT NULL COMMENT '加密密码',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名/称呼',
  `age` tinyint NULL COMMENT '年龄',
  `allergy_history` text NULL COMMENT '过敏史描述',
  `chronic_diseases` text NULL COMMENT '慢性病史描述',
  `role` varchar(20) NOT NULL DEFAULT 'elder' COMMENT '角色：elder/family',
  `bind_elder_id` bigint NULL COMMENT '家属绑定的老人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ==================== 药品基础库表 ====================
DROP TABLE IF EXISTS `drug_base`;
CREATE TABLE IF NOT EXISTS `drug_base` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '药品ID',
  `approval_number` varchar(100) UNIQUE COMMENT '国药准字',
  `generic_name` varchar(200) NOT NULL COMMENT '通用名',
  `trade_name` varchar(200) NULL COMMENT '商品名',
  `common_name` varchar(200) NULL COMMENT '俗名/别名',
  `specification` varchar(100) NULL COMMENT '规格',
  `manufacturer` varchar(200) NULL COMMENT '生产厂家',
  `category` varchar(100) NULL COMMENT '药品分类',
  `description` text NULL COMMENT '药品说明原文',
  `image_url` varchar(500) NULL COMMENT '药品标准图片',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_approval_number` (`approval_number`),
  INDEX `idx_generic_name` (`generic_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品基础库表';

-- ==================== 家庭药箱表 ====================
DROP TABLE IF EXISTS `user_medicine_box`;
CREATE TABLE IF NOT EXISTS `user_medicine_box` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '所属老人',
  `drug_id` bigint NOT NULL COMMENT '药品',
  `dosage` varchar(50) NOT NULL COMMENT '每次用量',
  `frequency` varchar(50) NOT NULL COMMENT '频率',
  `start_date` date NULL COMMENT '开始服用日期',
  `end_date` date NULL COMMENT '预计结束日期',
  `expiry_date` date NULL COMMENT '药品有效期',
  `total_quantity` int NULL COMMENT '总数量',
  `remaining_quantity` int NULL COMMENT '剩余数量',
  `note` varchar(500) NULL COMMENT '用户备注',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/stopped',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_status` (`user_id`, `status`),
  INDEX `idx_drug_id` (`drug_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭药箱表';

-- ==================== OCR识别记录表 ====================
DROP TABLE IF EXISTS `ocr_record`;
CREATE TABLE IF NOT EXISTS `ocr_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '上传用户',
  `image_url` varchar(500) NOT NULL COMMENT '图片存储路径',
  `raw_text` text NULL COMMENT 'OCR原始识别文本',
  `matched_drug_id` bigint NULL COMMENT '匹配到的药品ID',
  `match_score` decimal(5,4) NULL COMMENT '匹配置信度',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/matched/unmatched/failed',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  INDEX `idx_ocr_user_time` (`user_id`, `created_at`),
  INDEX `idx_ocr_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OCR识别记录表';

-- ==================== 药品识别日志表 ====================
DROP TABLE IF EXISTS `drug_recognition_log`;
CREATE TABLE IF NOT EXISTS `drug_recognition_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `ocr_record_id` bigint NULL COMMENT '关联OCR记录',
  `user_id` bigint NULL COMMENT '用户ID',
  `raw_text` text NULL COMMENT '原始识别文本',
  `normalized_name` varchar(255) NULL COMMENT '标准化后的名称',
  `matched_drug_id` bigint NULL COMMENT '匹配的药品ID',
  `match_score` decimal(5,4) NULL COMMENT '匹配分数',
  `status` varchar(50) NULL COMMENT '识别状态',
  `is_new_drug` tinyint(1) DEFAULT 0 COMMENT '是否新药品入库',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_rec_log_ocr` (`ocr_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品识别日志表';

-- ----------------------------
-- 第三部分：添加外键约束
-- ----------------------------

-- 为家庭药箱表添加外键
ALTER TABLE `user_medicine_box`
ADD CONSTRAINT `fk_box_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
ADD CONSTRAINT `fk_box_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE;

-- 为OCR识别记录表添加外键
ALTER TABLE `ocr_record`
ADD CONSTRAINT `fk_ocr_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
ADD CONSTRAINT `fk_ocr_drug` FOREIGN KEY (`matched_drug_id`) REFERENCES `drug_base`(`id`) ON DELETE SET NULL;

-- 为药品识别日志表添加外键
ALTER TABLE `drug_recognition_log`
ADD CONSTRAINT `fk_rec_log_ocr` FOREIGN KEY (`ocr_record_id`) REFERENCES `ocr_record`(`id`) ON DELETE CASCADE;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- 第四部分：插入测试数据
-- ----------------------------

-- 插入测试用户
INSERT INTO `sys_user` (`user_id`, `username`, `password`, `real_name`, `age`, `allergy_history`, `chronic_diseases`, `role`) VALUES
(10001, 'laowang', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '王阿姨', 68, '无药物过敏史', '高血压、糖尿病', 'elder'),
(10002, 'zhangsan', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '张三', 35, NULL, NULL, 'family')
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`);

-- 插入测试药品数据
INSERT INTO `drug_base` (`approval_number`, `generic_name`, `trade_name`, `specification`, `manufacturer`, `category`, `description`) VALUES
('国药准字Z44021856', '感冒灵颗粒', '999', '每袋装10克', '华润三九医药股份有限公司', '中成药', '【成分】三叉苦、金盏银盘、野菊花、岗梅、咖啡因、对乙酰氨基酚、马来酸氯苯那敏。【适应症】用于感冒引起的头痛，发热，鼻塞，流涕，咽痛等。【用法用量】开水冲服，一次1袋，一日3次。【注意事项】1.忌烟、酒及辛辣、生冷、油腻食物。2.不宜在服药期间同时服用滋补性中成药。3.本品含对乙酰氨基酚、马来酸氯苯那敏、咖啡因。服用本品期间不得饮酒或含酒精的饮料；不得同时服用与本品成份相似的其他抗感冒药。4.肝、肾功能不全者慎用。5.膀胱颈梗阻、甲状腺功能亢进、青光眼、高血压和前列腺肥大者慎用。6.孕妇及哺乳期妇女慎用。7.服药3天后症状无改善，或症状加重，或出现新的严重症状如胸闷、心悸等应立即停药，并去医院就诊。8.小儿、年老体弱者应在医师指导下服用。9.对本品过敏者禁用，过敏体质者慎用。10.本品性状发生改变时禁止使用。11.儿童必须在成人监护下使用。请将本品放在儿童不能接触的地方。12.如正在使用其他药品，使用本品前请咨询医师或药师。【不良反应】偶见皮疹、荨麻疹、药热及粒细胞减少；长期大量用药会导致肝肾功能异常。'),
('国药准字H10910052', '硝苯地平缓释片', '伲福达', '10mg×30片', '青岛黄海制药有限责任公司', '化学药品', '【成分】硝苯地平。【适应症】用于治疗高血压、心绞痛。【用法用量】口服：一次10-20mg，一日2次。极量，一次40mg，一日0.12g。【注意事项】1.低血压患者慎用。2.肝、肾功能不全患者慎用。3.孕妇及哺乳期妇女慎用。4.长期给药不宜骤停，以避免发生停药综合症而出现反跳现象。【不良反应】1.肝脏：偶尔出现黄疸及谷氨酸草酰乙酸氨基转移酶、谷(氨酸)丙(酮酸)氨基转移酶升高。2.循环系统：偶尔出现胸部疼痛、头痛、脸红、眼花、心悸、血压下降、下肢浮肿等。3.过敏症：偶尔出现麻疹、瘙痒等过敏症状。4.消化系统：偶尔出现腹痛、恶心、食欲不振、便秘等症。5.口腔：可能出现牙龈肥厚。6.代谢异常：偶尔出现高血糖症状。'),
('国药准字H11021309', '阿司匹林肠溶片', '拜阿司匹林', '100mg×30片', '拜耳医药保健有限公司', '化学药品', '【成分】阿司匹林。【适应症】用于抑制血小板聚集，减少动脉粥样硬化患者的心肌梗塞、暂时性脑缺血或中风发生。【用法用量】口服，肠溶片应饭前用适量水送服。1.降低急性心肌梗死疑似患者的发病风险：建议剂量300mg，嚼碎后服用以快速吸收。以后每天100-200mg。2.预防心肌梗死复发：每天100-300mg。3.中风的二级预防：每天100-300mg。4.降低短暂性脑缺血发作(TIA)及其继发脑卒中的风险：每天100-300mg。【注意事项】1.孕妇及哺乳期妇女慎用。2.哮喘、鼻息肉综合征、对阿司匹林和其他解热镇痛药过敏者禁用。3.血友病或血小板减少症、溃疡病活动期患者禁用。【不良反应】1.较常见的有恶心、呕吐、上腹部不适或疼痛等胃肠道反应。2.较少见或罕见的有：(1)胃肠道出血或溃疡，表现为血性或柏油样便，胃部剧痛或呕吐血性或咖啡渣样物，多见于大剂量服药患者。(2)过敏反应，表现为哮喘、荨麻疹、血管神经性水肿或休克。(3)肝、肾功能损害，与剂量大小有关，尤其是剂量过大使血药浓度达250μg/ml时易发生。损害均是可逆性的，停药后可恢复。'),
('国药准字H44021524', '阿莫西林胶囊', '阿莫仙', '0.5g×24粒', '珠海联邦制药股份有限公司', '化学药品', '【成分】阿莫西林。【适应症】用于敏感菌(不产β内酰胺酶菌株)所致的下列感染：1.溶血链球菌、肺炎链球菌、葡萄球菌或流感嗜血杆菌所致中耳炎、鼻窦炎、咽炎、扁桃体炎等上呼吸道感染。2.大肠埃希菌、奇异变形杆菌或粪肠球菌所致的泌尿生殖道感染。3.溶血链球菌、葡萄球菌或大肠埃希菌所致的皮肤软组织感染。4.溶血链球菌、肺炎链球菌、葡萄球菌或流感嗜血杆菌所致急性支气管炎、肺炎等下呼吸道感染。5.急性单纯性淋病。6.本品尚可用于治疗伤寒、伤寒带菌者及钩端螺旋体病；阿莫西林亦可与克拉霉素、兰索拉唑三联用药根除胃、十二指肠幽门螺杆菌，降低消化道溃疡复发率。【用法用量】口服。成人一次0.5g，每6～8小时1次，一日剂量不超过4g。小儿一日剂量按体重每千克20-40mg，每8小时1次；3个月以下婴儿一日剂量按体重每千克30mg，每12小时1次；或遵医嘱。【注意事项】1.青霉素过敏者禁用。2.传染性单核细胞增多症患者应用本品易发生皮疹，应避免使用。3.疗程较长患者应检查肝、肾功能和血常规。【不良反应】1.恶心、呕吐、腹泻及假膜性肠炎等胃肠道反应。2.皮疹、药物热和哮喘等过敏反应。3.贫血、血小板减少、嗜酸性粒细胞增多等。4.血清氨基转移酶可轻度增高。5.由念珠菌或耐药菌引起的二重感染。6.偶见头痛、失眠、兴奋、焦虑等。'),
('国药准字Z44023485', '板蓝根颗粒', '白云山', '每袋装10克', '广州白云山和记黄埔中药有限公司', '中成药', '【成分】板蓝根。辅料为蔗糖、糊精。【适应症】清热解毒，凉血，利咽。用于肺胃热盛，咽喉肿痛，口咽干燥；急性扁桃体炎见上述证候者。【用法用量】开水冲服。一次5-10克，一日3-4次。【注意事项】1.忌烟酒、辛辣、鱼腥食物。2.不宜在服药期间同时服用滋补性中药。3.糖尿病患者及有高血压、心脏病、肝病、肾病等慢性病严重者应在医师指导下服用。4.儿童、孕妇、哺乳期妇女、年老体弱、脾虚便溏者应在医师指导下服用。5.扁桃体有化脓或发热体温超过38.5℃的患者应去医院就诊。6.服药3天症状无缓解，应去医院就诊。7.对本品过敏者禁用，过敏体质者慎用。8.本品性状发生改变时禁止使用。9.儿童必须在成人监护下使用。请将本品放在儿童不能接触的地方。10.如正在使用其他药品，使用本品前请咨询医师或药师。【不良反应】尚不明确。'),
('国药准字H10970418', '氯雷他定片', '开瑞坦', '10mg×6片', '上海先灵葆雅制药有限公司', '化学药品', '【成分】氯雷他定。【适应症】用于缓解过敏性鼻炎有关的症状，如喷嚏、流涕、鼻痒、鼻塞以及眼部痒及烧灼感。口服药物后，鼻和眼部症状及体征得以迅速缓解。亦适用于缓解慢性荨麻疹、瘙痒性皮肤病及其他过敏性皮肤病的症状及体征。【用法用量】口服。成人及12岁以上儿童：一日1次，一次1片(10毫克)。2-12岁儿童：体重>30公斤：一日1次，一次1片(10毫克)。体重≤30公斤：一日1次，一次半片(5毫克)。【注意事项】1.严重肝或肾功能不全患者应在医师指导下使用。2.孕妇及哺乳期妇女慎用。3.在作皮试前的约48小时左右应中止使用本品，因抗组胺药能阻止或降低皮试的阳性反应发生。【不良反应】在每天10mg的推荐剂量下，本品未见明显的镇静作用。常见不良反应有乏力、疲倦、口干和头痛。'),
('国药准字Z11020377', '藿香正气水', '同仁堂', '每支装10毫升', '北京同仁堂科技发展股份有限公司制药厂', '中成药', '【成分】苍术、陈皮、厚朴(姜制)、白芷、茯苓、大腹皮、生半夏、甘草浸膏、广藿香油、紫苏叶油。辅料为：乙醇。【适应症】解表化湿，理气和中。用于外感风寒、内伤湿滞或夏伤暑湿所致的感冒，头痛昏重、胸膈痞闷、脘腹胀痛、呕吐泄泻；胃肠型感冒见上述证候者。【用法用量】口服，一次5-10毫升，一日2次，用时摇匀。【注意事项】1.忌烟、酒及辛辣、生冷、油腻食物，饮食宜清淡。2.不宜在服药期间同时服用滋补性中药。3.有高血压、心脏病、肝病、糖尿病、肾病等慢性病严重者应在医师指导下服用。4.儿童、孕妇、哺乳期妇女、年老体弱者应在医师指导下服用。5.吐泻严重者应及时去医院就诊。6.本品含乙醇(酒精)40%-50%，服药后不得驾驶机、车、船、从事高空作业、机械作业及操作精密仪器。7.严格按用法用量服用，本品含乙醇，服药后不得驾驶机、车、船、从事高空作业、机械作业及操作精密仪器。8.对本品及所含成份过敏者禁用，过敏体质者慎用。9.本品性状发生改变时禁止使用。10.儿童必须在成人监护下使用。请将本品放在儿童不能接触的地方。11.如正在使用其他药品，使用本品前请咨询医师或药师。【不良反应】尚不明确。'),
('国药准字H11021600', '葡萄糖酸钙片', '双鹤', '0.5g×100片', '北京双鹤药业股份有限公司', '化学药品', '【成分】葡萄糖酸钙。【适应症】用于预防和治疗钙缺乏症，如骨质疏松、手足抽搐症、骨发育不全、佝偻病以及儿童、妊娠和哺乳期妇女、绝经期妇女、老年人钙的补充。【用法用量】口服，含化或咀嚼后服用。一次1-4片，一日3次。【注意事项】1.心肾功能不全者慎用。2.对本品过敏者禁用，过敏体质者慎用。3.本品性状发生改变时禁止使用。4.请将本品放在儿童不能接触的地方。5.儿童必须在成人监护下使用。6.如正在使用其他药品，使用本品前请咨询医师或药师。【不良反应】偶见便秘。')
ON DUPLICATE KEY UPDATE `generic_name` = VALUES(`generic_name`);

-- ----------------------------
-- 第五部分：操作完成报告
-- ----------------------------
SELECT '========== 数据库初始化完成 ==========' AS '操作日志';
SELECT CONCAT('数据库: ', DATABASE()) AS '数据库名称';
SELECT '表结构创建完成' AS '状态';
SELECT COUNT(*) AS '药品记录数' FROM `drug_base`;
SELECT COUNT(*) AS '用户记录数' FROM `sys_user`;

-- =====================================================
-- 脚本执行完成
-- =====================================================
-- 使用说明:
-- 1. 确保MySQL服务已启动
-- 2. 使用root用户执行此脚本:
--    mysql -u root -p -e "source /path/to/init_database.sql"
-- 或者在MySQL客户端中执行:
--    source init_database.sql
-- 3. 如果提示密码，输入数据库root密码
-- 4. 脚本不会删除现有数据，只会创建或更新
-- =====================================================