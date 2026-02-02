-- Pilipili视频网站数据库表结构

-- 用户表
CREATE TABLE IF NOT EXISTS `t_user` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_name` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `nike_name` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '电话',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别',
  `role` VARCHAR(20) DEFAULT 'user' COMMENT '角色：user/manage/admin',
  `authorization` VARCHAR(50) DEFAULT NULL COMMENT '授权信息',
  `create_id` BIGINT(20) DEFAULT NULL COMMENT '创建者ID',
  `create_name` VARCHAR(50) DEFAULT NULL COMMENT '创建者名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_id` BIGINT(20) DEFAULT NULL COMMENT '更新者ID',
  `update_name` VARCHAR(50) DEFAULT NULL COMMENT '更新者名',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `logic_del` INT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name` (`user_name`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 视频表
CREATE TABLE IF NOT EXISTS `t_video` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(200) NOT NULL COMMENT '视频标题',
  `description` TEXT COMMENT '视频描述',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '视频封面URL',
  `video_url` VARCHAR(500) NOT NULL COMMENT '视频文件URL',
  `duration` INT(11) DEFAULT NULL COMMENT '视频时长（秒）',
  `file_size` BIGINT(20) DEFAULT NULL COMMENT '视频大小（字节）',
  `format` VARCHAR(20) DEFAULT NULL COMMENT '视频格式',
  `category_id` BIGINT(20) DEFAULT NULL COMMENT '视频分类ID',
  `category_name` VARCHAR(50) DEFAULT NULL COMMENT '视频分类名称',
  `tags` VARCHAR(500) DEFAULT NULL COMMENT '视频标签（逗号分隔）',
  `user_id` BIGINT(20) NOT NULL COMMENT '上传用户ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '上传用户名',
  `status` INT(1) DEFAULT 0 COMMENT '视频状态：0-待审核，1-已上线，2-已下架，3-审核不通过',
  `play_count` BIGINT(20) DEFAULT 0 COMMENT '播放量',
  `like_count` BIGINT(20) DEFAULT 0 COMMENT '点赞数',
  `collect_count` BIGINT(20) DEFAULT 0 COMMENT '收藏数',
  `comment_count` BIGINT(20) DEFAULT 0 COMMENT '评论数',
  `quality` INT(1) DEFAULT 1 COMMENT '视频清晰度：1-标清，2-高清，3-超清',
  `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  `create_id` BIGINT(20) DEFAULT NULL COMMENT '创建者ID',
  `create_name` VARCHAR(50) DEFAULT NULL COMMENT '创建者名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_id` BIGINT(20) DEFAULT NULL COMMENT '更新者ID',
  `update_name` VARCHAR(50) DEFAULT NULL COMMENT '更新者名',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `logic_del` INT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_play_count` (`play_count`),
  FULLTEXT KEY `ft_title_desc` (`title`, `description`, `tags`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频表';

-- 视频分类表
CREATE TABLE IF NOT EXISTS `t_video_category` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '分类描述',
  `parent_id` BIGINT(20) DEFAULT 0 COMMENT '父分类ID（0表示顶级分类）',
  `sort_order` INT(11) DEFAULT 0 COMMENT '排序序号',
  `enabled` INT(1) DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `create_id` BIGINT(20) DEFAULT NULL COMMENT '创建者ID',
  `create_name` VARCHAR(50) DEFAULT NULL COMMENT '创建者名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_id` BIGINT(20) DEFAULT NULL COMMENT '更新者ID',
  `update_name` VARCHAR(50) DEFAULT NULL COMMENT '更新者名',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `logic_del` INT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频分类表';

-- 视频点赞表
CREATE TABLE IF NOT EXISTS `t_video_like` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `video_id` BIGINT(20) NOT NULL COMMENT '视频ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `is_like` INT(1) DEFAULT 1 COMMENT '是否点赞：0-取消点赞，1-点赞',
  `create_id` BIGINT(20) DEFAULT NULL COMMENT '创建者ID',
  `create_name` VARCHAR(50) DEFAULT NULL COMMENT '创建者名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_id` BIGINT(20) DEFAULT NULL COMMENT '更新者ID',
  `update_name` VARCHAR(50) DEFAULT NULL COMMENT '更新者名',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `logic_del` INT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_user` (`video_id`, `user_id`),
  KEY `idx_video_id` (`video_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频点赞表';

-- 视频收藏表
CREATE TABLE IF NOT EXISTS `t_video_collect` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `video_id` BIGINT(20) NOT NULL COMMENT '视频ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `folder_name` VARCHAR(50) DEFAULT '默认收藏夹' COMMENT '收藏夹名称',
  `create_id` BIGINT(20) DEFAULT NULL COMMENT '创建者ID',
  `create_name` VARCHAR(50) DEFAULT NULL COMMENT '创建者名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_id` BIGINT(20) DEFAULT NULL COMMENT '更新者ID',
  `update_name` VARCHAR(50) DEFAULT NULL COMMENT '更新者名',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `logic_del` INT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_video_id` (`video_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频收藏表';

-- 视频评论表
CREATE TABLE IF NOT EXISTS `t_video_comment` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `video_id` BIGINT(20) NOT NULL COMMENT '视频ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
  `user_avatar` VARCHAR(500) DEFAULT NULL COMMENT '用户头像',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `parent_id` BIGINT(20) DEFAULT 0 COMMENT '父评论ID（0表示顶级评论）',
  `like_count` BIGINT(20) DEFAULT 0 COMMENT '点赞数',
  `reply_count` BIGINT(20) DEFAULT 0 COMMENT '回复数',
  `create_id` BIGINT(20) DEFAULT NULL COMMENT '创建者ID',
  `create_name` VARCHAR(50) DEFAULT NULL COMMENT '创建者名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_id` BIGINT(20) DEFAULT NULL COMMENT '更新者ID',
  `update_name` VARCHAR(50) DEFAULT NULL COMMENT '更新者名',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `logic_del` INT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_video_id` (`video_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频评论表';

-- 视频弹幕表
CREATE TABLE IF NOT EXISTS `t_video_danmaku` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `video_id` BIGINT(20) NOT NULL COMMENT '视频ID',
  `user_id` BIGINT(20) DEFAULT NULL COMMENT '用户ID',
  `content` VARCHAR(500) NOT NULL COMMENT '弹幕内容',
  `time` INT(11) NOT NULL COMMENT '弹幕出现时间（秒）',
  `type` INT(1) DEFAULT 1 COMMENT '弹幕类型：1-滚动，2-顶部，3-底部',
  `color` VARCHAR(20) DEFAULT '#FFFFFF' COMMENT '弹幕颜色',
  `font_size` INT(11) DEFAULT 25 COMMENT '字体大小',
  `create_id` BIGINT(20) DEFAULT NULL COMMENT '创建者ID',
  `create_name` VARCHAR(50) DEFAULT NULL COMMENT '创建者名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_id` BIGINT(20) DEFAULT NULL COMMENT '更新者ID',
  `update_name` VARCHAR(50) DEFAULT NULL COMMENT '更新者名',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `logic_del` INT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_video_id` (`video_id`),
  KEY `idx_time` (`time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频弹幕表';

-- 视频播放历史表
CREATE TABLE IF NOT EXISTS `t_video_play_history` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `video_id` BIGINT(20) NOT NULL COMMENT '视频ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `progress` INT(11) DEFAULT 0 COMMENT '播放进度（秒）',
  `play_duration` INT(11) DEFAULT 0 COMMENT '播放时长（秒）',
  `quality` INT(1) DEFAULT 1 COMMENT '播放清晰度：1-标清，2-高清，3-超清',
  `playback_rate` DECIMAL(3,2) DEFAULT 1.00 COMMENT '播放倍速',
  `create_id` BIGINT(20) DEFAULT NULL COMMENT '创建者ID',
  `create_name` VARCHAR(50) DEFAULT NULL COMMENT '创建者名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_id` BIGINT(20) DEFAULT NULL COMMENT '更新者ID',
  `update_name` VARCHAR(50) DEFAULT NULL COMMENT '更新者名',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `logic_del` INT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_user` (`video_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频播放历史表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `t_system_config` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` TEXT COMMENT '配置值',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '配置描述',
  `config_type` INT(1) DEFAULT 1 COMMENT '配置类型：1-字符串，2-数字，3-布尔值，4-JSON',
  `create_id` BIGINT(20) DEFAULT NULL COMMENT '创建者ID',
  `create_name` VARCHAR(50) DEFAULT NULL COMMENT '创建者名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_id` BIGINT(20) DEFAULT NULL COMMENT '更新者ID',
  `update_name` VARCHAR(50) DEFAULT NULL COMMENT '更新者名',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `logic_del` INT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 本地文件夹配置表
CREATE TABLE IF NOT EXISTS `t_local_folder_config` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_name` VARCHAR(100) NOT NULL COMMENT '配置名称',
  `machine_ip` VARCHAR(50) DEFAULT NULL COMMENT '机器IP或主机名',
  `folder_path` VARCHAR(500) NOT NULL COMMENT '文件夹路径',
  `enabled` INT(1) DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `scan_interval` INT(11) DEFAULT 60 COMMENT '扫描间隔（分钟）',
  `last_scan_time` DATETIME DEFAULT NULL COMMENT '最后扫描时间',
  `scan_status` INT(1) DEFAULT 0 COMMENT '扫描状态：0-未扫描，1-扫描中，2-扫描完成，3-扫描失败',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_id` BIGINT(20) DEFAULT NULL COMMENT '创建者ID',
  `create_name` VARCHAR(50) DEFAULT NULL COMMENT '创建者名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_id` BIGINT(20) DEFAULT NULL COMMENT '更新者ID',
  `update_name` VARCHAR(50) DEFAULT NULL COMMENT '更新者名',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `logic_del` INT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_scan_status` (`scan_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地文件夹配置表';

-- 视频合集表
CREATE TABLE IF NOT EXISTS `t_video_collection` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(200) NOT NULL COMMENT '合集标题',
  `description` TEXT COMMENT '合集描述',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '合集封面URL',
  `source_folder_path` VARCHAR(500) DEFAULT NULL COMMENT '源文件夹路径',
  `video_count` INT(11) DEFAULT 0 COMMENT '视频数量',
  `collection_type` INT(1) DEFAULT 1 COMMENT '合集类型：1-自动整合，2-手动创建',
  `enabled` INT(1) DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `create_id` BIGINT(20) DEFAULT NULL COMMENT '创建者ID',
  `create_name` VARCHAR(50) DEFAULT NULL COMMENT '创建者名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_id` BIGINT(20) DEFAULT NULL COMMENT '更新者ID',
  `update_name` VARCHAR(50) DEFAULT NULL COMMENT '更新者名',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `logic_del` INT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_collection_type` (`collection_type`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频合集表';

-- 视频分集表
CREATE TABLE IF NOT EXISTS `t_video_episode` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `collection_id` BIGINT(20) NOT NULL COMMENT '合集ID',
  `video_id` BIGINT(20) DEFAULT NULL COMMENT '视频ID（如果已上传到系统）',
  `episode_number` VARCHAR(20) DEFAULT NULL COMMENT '集数标识（如 "01", "02"）',
  `episode_name` VARCHAR(200) DEFAULT NULL COMMENT '分集名称',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
  `file_size` BIGINT(20) DEFAULT NULL COMMENT '文件大小（字节）',
  `file_modify_time` DATETIME DEFAULT NULL COMMENT '文件修改时间',
  `file_format` VARCHAR(20) DEFAULT NULL COMMENT '文件格式',
  `sort_order` INT(11) DEFAULT 0 COMMENT '排序序号',
  `create_id` BIGINT(20) DEFAULT NULL COMMENT '创建者ID',
  `create_name` VARCHAR(50) DEFAULT NULL COMMENT '创建者名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_id` BIGINT(20) DEFAULT NULL COMMENT '更新者ID',
  `update_name` VARCHAR(50) DEFAULT NULL COMMENT '更新者名',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `logic_del` INT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_collection_id` (`collection_id`),
  KEY `idx_video_id` (`video_id`),
  KEY `idx_episode_number` (`episode_number`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频分集表';
