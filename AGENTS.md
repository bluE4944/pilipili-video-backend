# Repository Guidelines

## 项目结构与模块组织
- 根目录是 Maven 聚合工程，核心代码在 `pilipili-video-service/`。
- 业务代码位于 `pilipili-video-service/src/main/java/com/pilipili/`，按 `config/`、`controller/`、`entity/`（含 `in/`、`out/`）、`exception/`、`mapper/`、`repository/`、`service/`、`service/impl/`、`utils/` 分层。
- 资源与配置在 `pilipili-video-service/src/main/resources/`，数据库结构脚本在 `db/schema.sql`。
- 测试代码在 `pilipili-video-service/src/test/java/com/pilipili/`。

## 构建、测试与本地运行
```bash
# 编译全部模块
mvn clean compile

# 运行服务（从根目录）
mvn -pl pilipili-video-service -am spring-boot:run

# 执行测试
mvn -pl pilipili-video-service test
```
如需仅在模块目录运行，可进入 `pilipili-video-service/` 后执行相同命令。

## 编码风格与命名约定
- 语言为 Java 8，缩进 4 个空格，保持 Spring Boot 与 MyBatis Plus 的常规写法。
- 类名使用 PascalCase，方法与变量使用 lowerCamelCase，常量使用 UPPER_SNAKE_CASE。
- Lombok 已集成，优先使用 `@Slf4j`、`@RequiredArgsConstructor` 等简化样板代码。
- 保持分层边界清晰：Controller 只处理协议与参数，Service 处理业务，Repository/Mapper 处理数据访问。

## 测试指南
- 使用 JUnit 4 + SpringRunner，测试类示例见 `PilipiliVideoApplicationTests`。
- 测试命名建议以 `*Tests.java` 结尾，放在与生产代码同包路径下。
- 当前无硬性覆盖率门槛，新功能或修复请补充单测或集成测试。

## 提交与 Pull Request 规范
- Git 历史以简短祈使动词开头（如 `add userService`），建议保持类似风格：`add ...`、`fix ...`、`refactor ...`。
- PR 请包含变更说明、测试结果（命令与输出摘要）、必要的配置/数据库变更说明；涉及接口变更时补充示例或 Swagger 说明。

## 配置与安全提示
- 主要配置在 `application.yml`；本地开发建议使用 `application-local.yml` 并通过 `spring.profiles.active=local` 覆盖。
- 不要提交真实密码或密钥；敏感配置优先通过环境变量或私有配置文件注入。
- 文件上传目录由 `file.upload.path` 控制，生产环境请指向持久化存储。
