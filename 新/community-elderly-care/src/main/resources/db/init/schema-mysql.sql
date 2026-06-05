-- 社区养老服务系统数据库初始化脚本（幂等）

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    user_type TINYINT NOT NULL DEFAULT 1 COMMENT '用户类型(1-老人 2-家属 3-管理员 4-服务人员)',
    avatar VARCHAR(255) COMMENT '头像URL',
    address VARCHAR(255) COMMENT '地址',
    emergency_contact VARCHAR(50) COMMENT '紧急联系人',
    emergency_phone VARCHAR(20) COMMENT '紧急联系电话',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0-禁用 1-正常)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_user_type (user_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS elderly_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '关联用户ID',
    age INT COMMENT '年龄',
    gender TINYINT COMMENT '性别(0-女 1-男)',
    health_status VARCHAR(50) COMMENT '健康状况',
    medical_history TEXT COMMENT '病史',
    longitude DECIMAL(10,7) COMMENT '经度',
    latitude DECIMAL(10,7) COMMENT '纬度',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人信息表';

CREATE TABLE IF NOT EXISTS family_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    elderly_id BIGINT NOT NULL COMMENT '老人ID',
    family_id BIGINT NOT NULL COMMENT '家属ID',
    relation VARCHAR(20) COMMENT '关系(子女/配偶/其他)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态(0-待确认 1-已确认)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elderly_id (elderly_id),
    INDEX idx_family_id (family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家属绑定表';

CREATE TABLE IF NOT EXISTS service_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    description VARCHAR(255) COMMENT '分类描述',
    icon VARCHAR(255) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0-禁用 1-正常)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_service_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务分类表';

CREATE TABLE IF NOT EXISTS service_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    category_id BIGINT NOT NULL COMMENT '所属分类ID',
    name VARCHAR(100) NOT NULL COMMENT '服务项目名称',
    description TEXT COMMENT '服务描述',
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格',
    duration INT DEFAULT 60 COMMENT '服务时长(分钟)',
    image_url VARCHAR(255) COMMENT '图片',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0-下架 1-上架)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (category_id) REFERENCES service_category(id),
    UNIQUE KEY uk_service_item_category_name (category_id, name),
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务项目表';

CREATE TABLE IF NOT EXISTS service_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '订单编号',
    elderly_id BIGINT NOT NULL COMMENT '老人ID',
    family_id BIGINT COMMENT '家属ID(下单人)',
    service_item_id BIGINT NOT NULL COMMENT '服务项目ID',
    service_user_id BIGINT COMMENT '服务人员ID',
    appointment_time DATETIME NOT NULL COMMENT '预约时间',
    service_address VARCHAR(255) NOT NULL COMMENT '服务地址',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1-待接单 2-已接单 3-服务中 4-已完成 5-已取消)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_elderly_id (elderly_id),
    INDEX idx_service_user_id (service_user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务订单表';

CREATE TABLE IF NOT EXISTS health_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    elderly_id BIGINT NOT NULL COMMENT '老人ID',
    record_type TINYINT NOT NULL COMMENT '记录类型(1-血压 2-血糖 3-心率 4-其他)',
    systolic_pressure INT COMMENT '收缩压',
    diastolic_pressure INT COMMENT '舒张压',
    blood_sugar DECIMAL(5,2) COMMENT '血糖值',
    heart_rate INT COMMENT '心率',
    record_time DATETIME NOT NULL COMMENT '记录时间',
    warning_level TINYINT DEFAULT 0 COMMENT '预警级别(0-正常 1-低 2-高)',
    advice VARCHAR(500) COMMENT '建议',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elderly_id (elderly_id),
    INDEX idx_record_time (record_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康数据表';

CREATE TABLE IF NOT EXISTS emergency_help (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    elderly_id BIGINT NOT NULL COMMENT '老人ID',
    longitude DECIMAL(10,7) COMMENT '经度',
    latitude DECIMAL(10,7) COMMENT '纬度',
    location_address VARCHAR(255) COMMENT '位置地址',
    help_type TINYINT DEFAULT 3 COMMENT '求救类型(1-医疗 2-摔倒 3-其他)',
    description TEXT COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1-待响应 2-已响应 3-已解决)',
    response_user_id BIGINT COMMENT '响应人员ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
    resolve_time DATETIME COMMENT '解决时间',
    INDEX idx_elderly_id (elderly_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='紧急求助表';

CREATE TABLE IF NOT EXISTS information (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    info_type TINYINT NOT NULL DEFAULT 1 COMMENT '类型(1-政策 2-活动 3-通知 4-动态)',
    publisher_id BIGINT NOT NULL COMMENT '发布人ID',
    cover_image VARCHAR(255) COMMENT '封面图',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态(0-草稿 1-已发布)',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    publish_time DATETIME COMMENT '发布时间',
    INDEX idx_publisher_id (publisher_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信息发布表';

CREATE TABLE IF NOT EXISTS message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    content TEXT NOT NULL COMMENT '内容',
    message_type TINYINT NOT NULL DEFAULT 1 COMMENT '类型(1-留言 2-反馈 3-咨询 4-系统提醒)',
    parent_id BIGINT COMMENT '父消息ID(回复)',
    status TINYINT DEFAULT 0 COMMENT '状态(0-未读 1-已读)',
    reply_content TEXT COMMENT '回复内容',
    reply_time DATETIME COMMENT '回复时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互动交流表';

INSERT INTO service_category (name, description, icon, sort_order, status)
VALUES ('生活照料', '提供日常生活照料服务', 'life', 1, 1)
ON DUPLICATE KEY UPDATE description = VALUES(description), icon = VALUES(icon), sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO service_category (name, description, icon, sort_order, status)
VALUES ('健康护理', '提供健康护理和康复服务', 'health', 2, 1)
ON DUPLICATE KEY UPDATE description = VALUES(description), icon = VALUES(icon), sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO service_category (name, description, icon, sort_order, status)
VALUES ('精神慰藉', '提供精神陪伴和心理疏导服务', 'mind', 3, 1)
ON DUPLICATE KEY UPDATE description = VALUES(description), icon = VALUES(icon), sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO service_category (name, description, icon, sort_order, status)
VALUES ('法律咨询', '提供法律咨询和援助服务', 'law', 4, 1)
ON DUPLICATE KEY UPDATE description = VALUES(description), icon = VALUES(icon), sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO service_item (category_id, name, description, price, duration, status)
SELECT c.id, '助餐服务', '为老人提供送餐、做饭服务', 30.00, 60, 1
FROM service_category c WHERE c.name = '生活照料'
ON DUPLICATE KEY UPDATE description = VALUES(description), price = VALUES(price), duration = VALUES(duration), status = VALUES(status);

INSERT INTO service_item (category_id, name, description, price, duration, status)
SELECT c.id, '助浴服务', '为老人提供洗浴辅助服务', 50.00, 45, 1
FROM service_category c WHERE c.name = '生活照料'
ON DUPLICATE KEY UPDATE description = VALUES(description), price = VALUES(price), duration = VALUES(duration), status = VALUES(status);

INSERT INTO service_item (category_id, name, description, price, duration, status)
SELECT c.id, '助洁服务', '为老人提供居家清洁服务', 40.00, 90, 1
FROM service_category c WHERE c.name = '生活照料'
ON DUPLICATE KEY UPDATE description = VALUES(description), price = VALUES(price), duration = VALUES(duration), status = VALUES(status);

INSERT INTO service_item (category_id, name, description, price, duration, status)
SELECT c.id, '陪诊服务', '陪同老人就医取药', 60.00, 120, 1
FROM service_category c WHERE c.name = '生活照料'
ON DUPLICATE KEY UPDATE description = VALUES(description), price = VALUES(price), duration = VALUES(duration), status = VALUES(status);

INSERT INTO service_item (category_id, name, description, price, duration, status)
SELECT c.id, '血压监测', '定期为老人测量血压', 20.00, 20, 1
FROM service_category c WHERE c.name = '健康护理'
ON DUPLICATE KEY UPDATE description = VALUES(description), price = VALUES(price), duration = VALUES(duration), status = VALUES(status);

INSERT INTO service_item (category_id, name, description, price, duration, status)
SELECT c.id, '血糖监测', '定期为老人测量血糖', 20.00, 20, 1
FROM service_category c WHERE c.name = '健康护理'
ON DUPLICATE KEY UPDATE description = VALUES(description), price = VALUES(price), duration = VALUES(duration), status = VALUES(status);

INSERT INTO service_item (category_id, name, description, price, duration, status)
SELECT c.id, '康复护理', '专业康复护理服务', 80.00, 60, 1
FROM service_category c WHERE c.name = '健康护理'
ON DUPLICATE KEY UPDATE description = VALUES(description), price = VALUES(price), duration = VALUES(duration), status = VALUES(status);

INSERT INTO service_item (category_id, name, description, price, duration, status)
SELECT c.id, '用药指导', '专业用药指导服务', 30.00, 30, 1
FROM service_category c WHERE c.name = '健康护理'
ON DUPLICATE KEY UPDATE description = VALUES(description), price = VALUES(price), duration = VALUES(duration), status = VALUES(status);

INSERT INTO service_item (category_id, name, description, price, duration, status)
SELECT c.id, '陪伴聊天', '与老人进行情感交流', 40.00, 60, 1
FROM service_category c WHERE c.name = '精神慰藉'
ON DUPLICATE KEY UPDATE description = VALUES(description), price = VALUES(price), duration = VALUES(duration), status = VALUES(status);

INSERT INTO service_item (category_id, name, description, price, duration, status)
SELECT c.id, '心理疏导', '专业心理咨询服务', 100.00, 60, 1
FROM service_category c WHERE c.name = '精神慰藉'
ON DUPLICATE KEY UPDATE description = VALUES(description), price = VALUES(price), duration = VALUES(duration), status = VALUES(status);

INSERT INTO service_item (category_id, name, description, price, duration, status)
SELECT c.id, '文化娱乐', '组织文化娱乐活动', 0.00, 120, 1
FROM service_category c WHERE c.name = '精神慰藉'
ON DUPLICATE KEY UPDATE description = VALUES(description), price = VALUES(price), duration = VALUES(duration), status = VALUES(status);

INSERT INTO service_item (category_id, name, description, price, duration, status)
SELECT c.id, '法律咨询', '提供法律咨询服务', 50.00, 60, 1
FROM service_category c WHERE c.name = '法律咨询'
ON DUPLICATE KEY UPDATE description = VALUES(description), price = VALUES(price), duration = VALUES(duration), status = VALUES(status);

INSERT INTO service_item (category_id, name, description, price, duration, status)
SELECT c.id, '纠纷调解', '帮助调解家庭纠纷', 80.00, 90, 1
FROM service_category c WHERE c.name = '法律咨询'
ON DUPLICATE KEY UPDATE description = VALUES(description), price = VALUES(price), duration = VALUES(duration), status = VALUES(status);

INSERT INTO sys_user (username, password, real_name, phone, user_type, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt7IIue', '系统管理员', '13800000000', 3, 1)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), phone = VALUES(phone), user_type = VALUES(user_type), status = VALUES(status);

-- 联调用服务人员账号（密码: Service123!）
INSERT INTO sys_user (username, password, real_name, phone, user_type, status)
VALUES ('service_dev', '$2a$10$yzlr21yv9uDwa1bPtVXHNuE4pxXRZerE8U/S5lMU/jm33VmAk9.rG', '联调服务人员', '13920009999', 4, 1)
ON DUPLICATE KEY UPDATE password = VALUES(password), real_name = VALUES(real_name), phone = VALUES(phone), user_type = VALUES(user_type), status = VALUES(status);
