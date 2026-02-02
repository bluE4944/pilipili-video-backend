# Pilipili视频网站后端服务

基于Spring Boot开发的视频网站后端服务，提供完整的视频管理、播放、互动等功能。

## 技术栈

- **框架**: Spring Boot 2.7.6
- **语言**: Java 8
- **ORM**: MyBatis Plus 3.5.3.1
- **安全**: Spring Security
- **数据库**: MySQL
- **缓存**: Redis
- **文档**: Swagger 2.9.2
- **WebSocket**: Spring WebSocket（用于弹幕实时推送）

## 功能模块

### 1. 用户模块
- 用户注册/登录/注销
- 用户信息修改
- 密码修改
- 角色权限管理（普通用户/管理员）

### 2. 视频资源模块
- 视频上传（支持分片上传/断点续传）
- 视频元信息管理（标题/封面/分类/标签）
- 视频状态管理（审核/上线/下架）
- 视频CRUD操作

### 3. 视频播放模块
- 播放地址生成（含防盗链/时效控制）
- 播放进度记录
- 清晰度/倍速适配

### 4. 互动模块
- 视频点赞/取消点赞
- 视频收藏/取消收藏
- 视频评论（支持回复）
- 弹幕功能（WebSocket实时推送）

### 5. 搜索与推荐模块
- 关键词搜索
- 分类/标签搜索
- 相关视频推荐
- 热门视频推荐

### 6. 数据统计模块
- 视频播放量统计
- 点赞量/收藏量统计
- 用户行为分析
- 热门视频排行

### 7. 系统配置模块
- 系统参数配置管理
- 配置项CRUD操作

## 项目结构

```
pilipili-video-backend/
├── pilipili-video-service/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/pilipili/
│   │   │   │   ├── config/          # 配置类
│   │   │   │   ├── controller/      # 控制器
│   │   │   │   ├── entity/          # 实体类
│   │   │   │   │   ├── in/          # 输入实体
│   │   │   │   │   └── out/         # 输出实体
│   │   │   │   ├── exception/       # 异常处理
│   │   │   │   ├── mapper/          # MyBatis Mapper
│   │   │   │   ├── repository/      # Repository层
│   │   │   │   ├── service/         # Service层
│   │   │   │   │   └── impl/        # Service实现
│   │   │   │   └── utils/           # 工具类
│   │   │   └── resources/
│   │   │       ├── application.yml  # 配置文件
│   │   │       └── db/
│   │   │           └── schema.sql  # 数据库表结构
│   │   └── test/                    # 测试代码
│   └── pom.xml
└── README.md
```

## 快速开始

### 1. 环境要求

- JDK 8+
- Maven 3.6+
- MySQL 5.7+
- Redis（可选）

### 2. 数据库配置

执行 `src/main/resources/db/schema.sql` 创建数据库表结构。

修改 `application.yml` 中的数据库连接配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pilipili_video?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

### 3. 运行项目

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run
```

项目启动后，访问：
- API文档: http://localhost:8316/swagger-ui.html
- 服务端口: 8316

### 4. 配置说明

#### 文件上传配置

在 `application.yml` 中添加：

```yaml
file:
  upload:
    path: ./uploads  # 文件上传路径（本地存储）
```

#### Redis配置（可选）

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
```

## API接口说明

### 用户接口

- `POST /api/user/register` - 用户注册
- `GET /api/user/info` - 获取当前用户信息
- `PUT /api/user/info` - 更新用户信息
- `PUT /api/user/password` - 修改密码
- `POST /api/user/logout` - 用户注销

### 视频接口

- `POST /api/video/upload` - 上传视频
- `POST /api/video/upload/chunk` - 分片上传
- `POST /api/video/upload/complete` - 完成分片上传
- `GET /api/video/page` - 分页查询视频
- `GET /api/video/{videoId}` - 获取视频详情
- `PUT /api/video/{videoId}` - 更新视频信息
- `DELETE /api/video/{videoId}` - 删除视频
- `PUT /api/video/{videoId}/status` - 更新视频状态

### 视频播放接口

- `GET /api/video/play/url/{videoId}` - 获取播放地址
- `POST /api/video/play/progress` - 记录播放进度
- `GET /api/video/play/progress/{videoId}` - 获取播放进度

### 视频互动接口

- `POST /api/video/interaction/like/{videoId}` - 点赞/取消点赞
- `GET /api/video/interaction/like/{videoId}` - 检查是否已点赞
- `POST /api/video/interaction/collect/{videoId}` - 收藏视频
- `DELETE /api/video/interaction/collect/{videoId}` - 取消收藏
- `POST /api/video/interaction/comment` - 添加评论
- `DELETE /api/video/interaction/comment/{commentId}` - 删除评论
- `GET /api/video/interaction/comment/{videoId}` - 查询评论列表

### 弹幕接口

- `POST /api/video/danmaku` - 添加弹幕
- `GET /api/video/danmaku/{videoId}` - 获取弹幕列表
- `DELETE /api/video/danmaku/{danmakuId}` - 删除弹幕

### 搜索接口

- `GET /api/video/search/keyword` - 关键词搜索
- `GET /api/video/search/category/{categoryId}` - 分类搜索
- `GET /api/video/search/tag` - 标签搜索
- `GET /api/video/search/related/{videoId}` - 相关视频推荐
- `GET /api/video/search/hot` - 热门视频推荐

### 统计接口

- `GET /api/video/statistics/video/{videoId}` - 视频播放量统计
- `GET /api/video/statistics/user/behavior` - 用户行为分析
- `GET /api/video/statistics/hot/ranking` - 热门视频排行

### 系统配置接口

- `GET /api/system/config/{configKey}` - 获取配置值
- `POST /api/system/config` - 设置配置值
- `GET /api/system/config/all` - 获取所有配置
- `DELETE /api/system/config/{configKey}` - 删除配置

## 对象存储集成

项目提供了 `StorageService` 接口，支持对象存储的适配。当前实现了本地存储（`LocalStorageServiceImpl`），可以扩展实现OSS/S3等云存储：

```java
@Service
public class OssStorageServiceImpl implements StorageService {
    // 实现OSS上传逻辑
}
```

## WebSocket弹幕推送

弹幕实时推送使用WebSocket实现：

- WebSocket端点: `/ws/danmaku`
- 订阅主题: `/topic/danmaku/{videoId}`
- 发送消息: `/app/danmaku/send`

前端可以使用SockJS和STOMP客户端连接：

```javascript
var socket = new SockJS('/ws/danmaku');
var stompClient = Stomp.over(socket);
stompClient.connect({}, function(frame) {
    stompClient.subscribe('/topic/danmaku/' + videoId, function(danmaku) {
        // 处理弹幕消息
    });
});
```

## 安全说明

- 使用Spring Security进行认证授权
- 密码使用BCrypt加密存储
- 支持RSA加密传输（可选）
- 视频播放地址支持时效控制和防盗链

## 开发规范

- 统一使用 `Result<T>` 作为接口返回格式
- 统一使用 `Status` 枚举定义错误码
- 统一异常处理通过 `GlobalExceptionHandler`
- 数据库操作使用MyBatis Plus
- 分页查询使用MyBatis Plus分页插件

## 注意事项

1. 文件上传默认使用本地存储，生产环境建议使用OSS/S3等对象存储
2. 视频播放地址的防盗链和时效控制需要根据实际存储服务实现
3. WebSocket配置需要根据实际部署环境调整跨域设置
4. 数据库表结构已创建，但需要根据实际需求调整索引和字段

## 后续优化建议

1. 集成Elasticsearch实现全文搜索
2. 实现视频转码和多种清晰度支持
3. 添加视频审核工作流
4. 实现更完善的推荐算法
5. 添加接口限流和防刷机制
6. 完善单元测试和集成测试

## 许可证

本项目采用MIT许可证。
