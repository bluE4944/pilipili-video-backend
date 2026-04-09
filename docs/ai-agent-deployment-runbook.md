# AI 代理可执行部署手册（UTF-8）

## 1. 文档目的

这份手册给 AI 代理直接执行，不是讲概念。

目标：

- 在本地 Manjaro 下载机部署 `qBittorrent-nox + frpc(home)`。
- 在阿里云服务器部署 `frps + frpc(visitor) + Pilipili 后端`。
- 让后端通过 `http://127.0.0.1:18080` 访问家庭下载机 qB WebUI。
- 让你在外网可直接使用后台下载管理功能。

## 2. 仓库地址

- 后端仓库：`https://github.com/bluE4944/pilipili-video-backend/tree/dev`
- 前端仓库：`https://github.com/bluE4944/pilipili-video/tree/dev`

关键文档与模板：

- 部署总文档：`https://github.com/bluE4944/pilipili-video-backend/blob/dev/docs/remote-download-deployment.md`
- frps 模板：`https://github.com/bluE4944/pilipili-video-backend/blob/dev/deploy/frp/frps.toml.example`
- frpc(visitor) 模板：`https://github.com/bluE4944/pilipili-video-backend/blob/dev/deploy/frp/frpc-visitor.toml.example`
- frpc(home) 模板：`https://github.com/bluE4944/pilipili-video-backend/blob/dev/deploy/frp/frpc-home.toml.example`
- qB systemd 模板：`https://github.com/bluE4944/pilipili-video-backend/blob/dev/deploy/systemd/qbittorrent-nox.service.example`
- frpc(home) systemd 模板：`https://github.com/bluE4944/pilipili-video-backend/blob/dev/deploy/systemd/frpc-home.service.example`
- 后端配置文件：`https://github.com/bluE4944/pilipili-video-backend/blob/dev/pilipili-video-service/src/main/resources/application.yml`

## 3. AI 执行硬规则

1. 全流程必须幂等，重复执行不能破坏已有部署。
2. 覆盖已有配置前必须先备份。
3. 不允许把 qB WebUI 暴露公网。
4. 不允许把阿里云 `127.0.0.1:18080` 绑定到公网地址。
5. 每个阶段结束必须执行验证命令。
6. 遇到错误立即停止，并输出失败步骤、命令、原始错误和修复建议。

## 4. 输入变量（必须先确认）

```text
ECS_PUBLIC_HOST=阿里云公网IP或域名
FRP_TOKEN=强随机字符串
FRP_SECRET_KEY=另一条强随机字符串
FRPS_BIND_PORT=7000
BACKEND_PORT=8316
QBT_WEBUI_PORT=8080
QBT_VISITOR_PORT=18080
QBT_CATEGORY=pilipili
DOWNLOAD_DIR=/data/media/anime
HOME_NODE_USER=media
HOME_NODE_GROUP=media
QBT_PROFILE_DIR=/var/lib/qbittorrent
FRP_INSTALL_DIR=/opt/frp
FRP_CONFIG_DIR=/etc/frp
```

约束：

- `FRP_TOKEN` 与 `FRP_SECRET_KEY` 不能相同。
- `DOWNLOAD_DIR` 必须与 `t_local_folder_config.folder_path` 一致。

## 5. 部署拓扑（固定）

```text
浏览器
  -> 前端
  -> 后端
  -> 阿里云本机 127.0.0.1:18080
  -> frpc(visitor)
  -> frps
  -> 家庭下载机 frpc(home)
  -> qBittorrent WebUI
```

## 6. 本地 Manjaro 下载机执行步骤

### 6.1 安装基础包

```bash
sudo pacman -Sy --needed qbittorrent-nox curl tar
```

### 6.2 创建用户和目录

```bash
id -u "${HOME_NODE_USER}" >/dev/null 2>&1 || sudo useradd -r -m -d /var/lib/qbittorrent -s /usr/bin/nologin "${HOME_NODE_USER}"
sudo mkdir -p "${DOWNLOAD_DIR}" "${QBT_PROFILE_DIR}" "${FRP_INSTALL_DIR}" "${FRP_CONFIG_DIR}"
sudo chown -R "${HOME_NODE_USER}:${HOME_NODE_GROUP}" "${DOWNLOAD_DIR}" "${QBT_PROFILE_DIR}"
```

### 6.3 安装 frpc

执行策略：

1. 先检查 `frpc` 是否已存在。
2. 不存在则下载 frp 官方发布包。
3. 放到 `${FRP_INSTALL_DIR}` 并赋予可执行权限。

验证：

```bash
${FRP_INSTALL_DIR}/frpc -v || frpc -v
```

### 6.4 生成 `${FRP_CONFIG_DIR}/frpc-home.toml`

```toml
serverAddr = "ECS_PUBLIC_HOST"
serverPort = 7000

auth.method = "token"
auth.token = "FRP_TOKEN"

log.to = "/var/log/frpc-home.log"
log.level = "info"
log.maxDays = 7

[[proxies]]
name = "pilipili-qb"
type = "stcp"
secretKey = "FRP_SECRET_KEY"
localIP = "127.0.0.1"
localPort = 8080
```

### 6.5 启动 qB 与 frpc

优先使用系统自带 `qbittorrent-nox.service`，若不满足再用自定义模板。

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now qbittorrent-nox.service
sudo systemctl enable --now frpc-home.service
```

若 `frpc-home.service` 不存在，按模板创建：

- `https://github.com/bluE4944/pilipili-video-backend/blob/dev/deploy/systemd/frpc-home.service.example`

### 6.6 本机验证

```bash
systemctl status qbittorrent-nox.service --no-pager
systemctl status frpc-home.service --no-pager
curl -I http://127.0.0.1:8080
```

成功条件：

- 两个服务都是 `active (running)`。
- `127.0.0.1:8080` 可访问。

## 7. 阿里云服务器执行步骤

### 7.1 安装 frps/frpc

执行策略：

1. 优先复用已有 `frps`、`frpc`。
2. 不存在则下载官方发布包到 `${FRP_INSTALL_DIR}`。

验证：

```bash
${FRP_INSTALL_DIR}/frps -v || frps -v
${FRP_INSTALL_DIR}/frpc -v || frpc -v
```

### 7.2 生成 `${FRP_CONFIG_DIR}/frps.toml`

```toml
bindPort = 7000

auth.method = "token"
auth.token = "FRP_TOKEN"

webServer.addr = "127.0.0.1"
webServer.port = 7500
webServer.user = "admin"
webServer.password = "change-this-password"

log.to = "/var/log/frps.log"
log.level = "info"
log.maxDays = 7
```

### 7.3 生成 `${FRP_CONFIG_DIR}/frpc-visitor.toml`

```toml
serverAddr = "ECS_PUBLIC_HOST"
serverPort = 7000

auth.method = "token"
auth.token = "FRP_TOKEN"

log.to = "/var/log/frpc-visitor.log"
log.level = "info"
log.maxDays = 7

[[visitors]]
name = "pilipili-qb-visitor"
type = "stcp"
serverName = "pilipili-qb"
secretKey = "FRP_SECRET_KEY"
bindAddr = "127.0.0.1"
bindPort = 18080
```

### 7.4 启动 frps 与 frpc(visitor)

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now frps.service
sudo systemctl enable --now frpc-visitor.service
```

若服务不存在，创建两个 systemd 文件：

- `frps.service` 执行 `${FRP_INSTALL_DIR}/frps -c ${FRP_CONFIG_DIR}/frps.toml`
- `frpc-visitor.service` 执行 `${FRP_INSTALL_DIR}/frpc -c ${FRP_CONFIG_DIR}/frpc-visitor.toml`

### 7.5 安全组/防火墙检查

必须满足：

- 放行 `${FRPS_BIND_PORT}/tcp`（默认 7000）。
- 放行 `${BACKEND_PORT}/tcp`（默认 8316）。
- 不放行 `${QBT_VISITOR_PORT}`（18080）。

### 7.6 服务器验证

```bash
systemctl status frps.service --no-pager
systemctl status frpc-visitor.service --no-pager
curl http://127.0.0.1:18080/api/v2/app/version
```

成功条件：

- 两个服务都是 `active (running)`。
- 能取到 qB 版本号。

## 8. 后端配置与数据库

### 8.1 后端下载配置

确保后端最终读取到：

```yaml
download:
  qb:
    base-url: http://127.0.0.1:18080
    username: 你的qB用户名
    password: 你的qB密码
    poll-interval-seconds: 5
    default-category: pilipili
```

### 8.2 目录配置一致性

若还没有对应目录配置，插入 `t_local_folder_config`，并确保：

- qB 下载目录 = `/data/media/anime`
- `folder_path` = `/data/media/anime`

示例 SQL：

```sql
INSERT INTO t_local_folder_config
(
  config_name, machine_ip, folder_path, enabled, scan_interval, scan_status,
  remark, create_time, update_time, logic_del
)
VALUES
(
  '家庭下载目录', 'manjaro-home', '/data/media/anime', 1, 60, 0,
  'qB 下载目录', NOW(), NOW(), 0
);
```

## 9. 联调验收

### 9.1 下载器状态

`GET /api/admin/download/status` 期望：

- `connected=true`
- `version` 非空

### 9.2 创建磁力任务

`POST /api/admin/download/tasks/magnet` 示例：

```json
{
  "magnetUrl": "magnet:?xt=urn:btih:...",
  "folderConfigId": 1,
  "addPaused": false
}
```

期望：

- 返回任务 ID。
- 状态进入 `queued/downloading`。
- 数秒后补齐 `torrentHash`。

### 9.3 自动入库

期望：

- 下载完成后任务状态 `completed`。
- `autoImportStatus=success`。

## 10. AI 执行结果输出格式

```text
执行主机:
执行模式:
已完成步骤:
变更文件:
新增服务:
开放端口:
验证结果:
失败项:
下一步建议:
```

## 11. 禁止事项

- 禁止把 qB WebUI 监听到 `0.0.0.0` 并放通公网。
- 禁止把 visitor 端口 `18080` 暴露公网。
- 禁止清空已有下载目录。
- 禁止删除现有业务表数据。
- 禁止无备份覆盖生产配置。
