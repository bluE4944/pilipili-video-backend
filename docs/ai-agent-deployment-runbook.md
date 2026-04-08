# AI 代理可执行部署手册

## 1. 文档用途

这份文档不是给人读原理的，而是给另一个 AI 代理直接执行部署用的。

目标：

- 让本地 Manjaro 下载机安装并运行 `qBittorrent-nox + frpc(home)`
- 让阿里云服务器安装并运行 `frps + frpc(visitor) + Pilipili 后端`
- 让后端通过 `http://127.0.0.1:18080` 访问家庭下载机的 qB WebUI
- 让管理员下载页可以直接创建、管理、同步下载任务

适用执行方：

- 本地 AI：运行在你的 Manjaro 下载机上
- 服务器 AI：运行在你的阿里云服务器上
- 同一个 AI 分别在两台机器执行，也可以

## 2. AI 执行规则

执行此文档的 AI 必须遵守：

1. 所有步骤必须幂等，重复执行不应破坏现有部署。
2. 如果目标文件已存在，先备份，再覆盖。
3. 不允许把 qB WebUI 暴露到公网。
4. 不允许把阿里云本机 `127.0.0.1:18080` 绑定到公网地址。
5. 每完成一个阶段，必须执行对应验证命令。
6. 如果某一步失败，停止后输出：
   - 失败步骤名
   - 执行命令
   - 原始错误输出
   - 当前建议修复动作

## 3. 当前项目事实

后端仓库路径：

- `d:\workspace\pilipili-video-backend`

前端仓库路径：

- `d:\workspace\pilipili-video`

后端已实现下载中心接口：

- `POST /api/admin/download/tasks/magnet`
- `POST /api/admin/download/tasks/torrent`
- `GET /api/admin/download/tasks`
- `GET /api/admin/download/tasks/{taskId}`
- `POST /api/admin/download/tasks/{taskId}/pause`
- `POST /api/admin/download/tasks/{taskId}/resume`
- `DELETE /api/admin/download/tasks/{taskId}?deleteFiles=false`
- `POST /api/admin/download/tasks/{taskId}/retry-import`
- `GET /api/admin/download/status`

后端已实现配置项：

文件：[application.yml](/d:/workspace/pilipili-video-backend/pilipili-video-service/src/main/resources/application.yml)

```yaml
download:
  qb:
    base-url: http://127.0.0.1:18080
    username:
    password:
    poll-interval-seconds: 5
    default-category: pilipili
```

## 4. 部署拓扑

固定拓扑如下：

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

说明：

- 家庭下载机不需要公网入站
- 家庭下载机主动连接阿里云 `frps`
- 阿里云 `frpc(visitor)` 也主动连接 `frps`
- 后端始终只连 `127.0.0.1:18080`

## 5. 输入变量

执行前，AI 必须先确认或设置下面这些变量。

### 5.1 通用变量

```text
FRP_TOKEN=替换为强随机字符串
FRP_SECRET_KEY=替换为另一条强随机字符串
FRPS_BIND_PORT=7000
QBT_WEBUI_PORT=8080
QBT_VISITOR_PORT=18080
QBT_CATEGORY=pilipili
DOWNLOAD_DIR=/data/media/anime
```

要求：

- `FRP_TOKEN` 和 `FRP_SECRET_KEY` 不能相同
- `DOWNLOAD_DIR` 必须与后端 `t_local_folder_config.folder_path` 一致

### 5.2 阿里云变量

```text
ECS_PUBLIC_HOST=你的阿里云公网 IP 或域名
BACKEND_PORT=8316
FRP_INSTALL_DIR=/opt/frp
FRP_CONFIG_DIR=/etc/frp
```

### 5.3 Manjaro 下载机变量

```text
HOME_NODE_USER=media
HOME_NODE_GROUP=media
QBT_PROFILE_DIR=/var/lib/qbittorrent
FRP_INSTALL_DIR=/opt/frp
FRP_CONFIG_DIR=/etc/frp
```

## 6. 可复用仓库文件

AI 可以直接使用这些模板文件：

- 部署总说明：[remote-download-deployment.md](/d:/workspace/pilipili-video-backend/docs/remote-download-deployment.md)
- `frps` 模板：[frps.toml.example](/d:/workspace/pilipili-video-backend/deploy/frp/frps.toml.example)
- `frpc(visitor)` 模板：[frpc-visitor.toml.example](/d:/workspace/pilipili-video-backend/deploy/frp/frpc-visitor.toml.example)
- `frpc(home)` 模板：[frpc-home.toml.example](/d:/workspace/pilipili-video-backend/deploy/frp/frpc-home.toml.example)
- `qbittorrent-nox` 服务样例：[qbittorrent-nox.service.example](/d:/workspace/pilipili-video-backend/deploy/systemd/qbittorrent-nox.service.example)
- `frpc(home)` 服务样例：[frpc-home.service.example](/d:/workspace/pilipili-video-backend/deploy/systemd/frpc-home.service.example)

## 7. 执行模式

执行此文档时，AI 应按三种模式之一运行：

### 7.1 只部署家庭下载机

适用：

- AI 当前只在 Manjaro 机器上

必须完成：

- 安装 `qbittorrent-nox`
- 安装 `frpc`
- 创建下载目录
- 启动本机 WebUI
- 启动 `frpc(home)`

### 7.2 只部署阿里云服务器

适用：

- AI 当前只在阿里云服务器上

必须完成：

- 安装 `frps`
- 安装 `frpc(visitor)`
- 配置后端 `download.qb.*`
- 验证 `127.0.0.1:18080`

### 7.3 完整部署

适用：

- AI 可以分别控制两台机器

必须完成：

- 先部署家庭下载机
- 再部署阿里云服务器
- 最后完成联调验证

## 8. 家庭下载机执行步骤

本节默认执行环境为 Manjaro。

### 8.1 安装基础包

```bash
sudo pacman -Sy --needed qbittorrent-nox curl tar
```

### 8.2 创建运行用户和目录

如果用户不存在，创建之：

```bash
id -u "${HOME_NODE_USER}" >/dev/null 2>&1 || sudo useradd -r -m -d /var/lib/qbittorrent -s /usr/bin/nologin "${HOME_NODE_USER}"
sudo mkdir -p "${DOWNLOAD_DIR}" "${QBT_PROFILE_DIR}" "${FRP_INSTALL_DIR}" "${FRP_CONFIG_DIR}"
sudo chown -R "${HOME_NODE_USER}:${HOME_NODE_GROUP}" "${DOWNLOAD_DIR}" "${QBT_PROFILE_DIR}"
```

### 8.3 安装 frp

优先策略：

- 若系统已有 `frpc`，直接复用
- 若没有，则下载官方发布包并安装到 `${FRP_INSTALL_DIR}`

执行逻辑：

1. 检测 `command -v frpc`
2. 若不存在，检测系统架构
3. 下载匹配架构的 `frp` 发布包
4. 解压出 `frpc` 到 `${FRP_INSTALL_DIR}`
5. 赋予可执行权限

成功判定：

```bash
${FRP_INSTALL_DIR}/frpc -v || frpc -v
```

### 8.4 写入 `frpc(home)` 配置

目标文件：

- `${FRP_CONFIG_DIR}/frpc-home.toml`

内容模板：

```toml
serverAddr = "${ECS_PUBLIC_HOST}"
serverPort = ${FRPS_BIND_PORT}

auth.method = "token"
auth.token = "${FRP_TOKEN}"

log.to = "/var/log/frpc-home.log"
log.level = "info"
log.maxDays = 7

[[proxies]]
name = "pilipili-qb"
type = "stcp"
secretKey = "${FRP_SECRET_KEY}"
localIP = "127.0.0.1"
localPort = ${QBT_WEBUI_PORT}
```

### 8.5 启动 qBittorrent-nox

优先顺序：

1. 如果系统自带 `qbittorrent-nox.service` 能满足需求，直接启用
2. 否则使用自定义 `systemd` 服务

系统自带服务检查：

```bash
systemctl list-unit-files | grep qbittorrent-nox
```

若使用自带服务，AI 需要确认：

- 服务运行用户正确
- WebUI 端口为 `${QBT_WEBUI_PORT}`
- 配置目录可写

若使用自定义服务，写入：

- `/etc/systemd/system/qbittorrent-nox.service`

参考模板：

- [qbittorrent-nox.service.example](/d:/workspace/pilipili-video-backend/deploy/systemd/qbittorrent-nox.service.example)

### 8.6 启动 `frpc(home)`

写入：

- `/etc/systemd/system/frpc-home.service`

参考模板：

- [frpc-home.service.example](/d:/workspace/pilipili-video-backend/deploy/systemd/frpc-home.service.example)

启动命令：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now qbittorrent-nox.service
sudo systemctl enable --now frpc-home.service
```

### 8.7 家庭下载机验证

验证 1：

```bash
systemctl status qbittorrent-nox.service --no-pager
```

验证 2：

```bash
systemctl status frpc-home.service --no-pager
```

验证 3：

```bash
curl -I http://127.0.0.1:${QBT_WEBUI_PORT}
```

成功条件：

- `qbittorrent-nox.service` 为 `active (running)`
- `frpc-home.service` 为 `active (running)`
- 本机 `127.0.0.1:${QBT_WEBUI_PORT}` 可访问

## 9. 阿里云服务器执行步骤

本节默认执行环境为 Linux ECS。

### 9.1 安装基础包

AI 应根据系统发行版选择包管理器，至少安装：

- `curl`
- `tar`
- `systemd` 运行能力

### 9.2 安装 frp

逻辑与家庭下载机相同：

1. 优先复用现有 `frps`、`frpc`
2. 若不存在，则下载官方发布包
3. 安装到 `${FRP_INSTALL_DIR}`

成功判定：

```bash
${FRP_INSTALL_DIR}/frps -v || frps -v
${FRP_INSTALL_DIR}/frpc -v || frpc -v
```

### 9.3 写入 `frps` 配置

目标文件：

- `${FRP_CONFIG_DIR}/frps.toml`

内容模板：

```toml
bindPort = ${FRPS_BIND_PORT}

auth.method = "token"
auth.token = "${FRP_TOKEN}"

webServer.addr = "127.0.0.1"
webServer.port = 7500
webServer.user = "admin"
webServer.password = "change-this-password"

log.to = "/var/log/frps.log"
log.level = "info"
log.maxDays = 7
```

### 9.4 写入 `frpc(visitor)` 配置

目标文件：

- `${FRP_CONFIG_DIR}/frpc-visitor.toml`

内容模板：

```toml
serverAddr = "${ECS_PUBLIC_HOST}"
serverPort = ${FRPS_BIND_PORT}

auth.method = "token"
auth.token = "${FRP_TOKEN}"

log.to = "/var/log/frpc-visitor.log"
log.level = "info"
log.maxDays = 7

[[visitors]]
name = "pilipili-qb-visitor"
type = "stcp"
serverName = "pilipili-qb"
secretKey = "${FRP_SECRET_KEY}"
bindAddr = "127.0.0.1"
bindPort = ${QBT_VISITOR_PORT}
```

### 9.5 写入 systemd 服务

如果系统不存在现成服务，则创建：

- `/etc/systemd/system/frps.service`
- `/etc/systemd/system/frpc-visitor.service`

推荐内容：

```ini
[Unit]
Description=frps Service
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
ExecStart=${FRP_INSTALL_DIR}/frps -c ${FRP_CONFIG_DIR}/frps.toml
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```ini
[Unit]
Description=frpc Visitor Service
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
ExecStart=${FRP_INSTALL_DIR}/frpc -c ${FRP_CONFIG_DIR}/frpc-visitor.toml
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

启动命令：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now frps.service
sudo systemctl enable --now frpc-visitor.service
```

### 9.6 配置防火墙或安全组

AI 必须确保：

- 放行 `${FRPS_BIND_PORT}/tcp`
- 放行 `${BACKEND_PORT}/tcp`
- 不放行 `${QBT_VISITOR_PORT}`
- 不暴露家庭下载机 qB WebUI 端口

### 9.7 配置后端下载器连接

后端必须最终读到：

```yaml
download:
  qb:
    base-url: http://127.0.0.1:18080
    username: 你的 qB 用户名
    password: 你的 qB 密码
    poll-interval-seconds: 5
    default-category: pilipili
```

AI 的优先策略：

1. 优先写入独立生产配置文件，不直接污染仓库默认配置
2. 若现有部署就是直接使用仓库内 `application.yml`，则先备份后修改

### 9.8 阿里云验证

验证 1：

```bash
systemctl status frps.service --no-pager
```

验证 2：

```bash
systemctl status frpc-visitor.service --no-pager
```

验证 3：

```bash
curl http://127.0.0.1:${QBT_VISITOR_PORT}/api/v2/app/version
```

成功条件：

- `frps.service` 为 `active (running)`
- `frpc-visitor.service` 为 `active (running)`
- 阿里云本机能够取到 qB 版本号

## 10. 后端与数据库联动

### 10.1 本地文件夹配置必须存在

如果系统里还没有对应下载目录配置，AI 必须创建一条 `t_local_folder_config` 记录。

SQL 模板：

```sql
INSERT INTO t_local_folder_config
(
  config_name,
  machine_ip,
  folder_path,
  enabled,
  scan_interval,
  scan_status,
  remark,
  create_time,
  update_time,
  logic_del
)
VALUES
(
  '家庭下载目录',
  'manjaro-home',
  '/data/media/anime',
  1,
  60,
  0,
  'qB 下载目录',
  NOW(),
  NOW(),
  0
);
```

如果已存在相同路径的启用配置，则不要重复插入。

### 10.2 自动入库路径约束

AI 必须再次确认：

- qB 下载目录 = `/data/media/anime`
- `t_local_folder_config.folder_path` = `/data/media/anime`

否则不要继续联调。

## 11. 最终联调步骤

### 11.1 后台状态接口

请求：

```http
GET /api/admin/download/status
```

成功条件：

- `connected=true`
- `version` 非空

### 11.2 创建磁力任务

请求：

```http
POST /api/admin/download/tasks/magnet
Content-Type: application/json

{
  "magnetUrl": "magnet:?xt=urn:btih:...",
  "folderConfigId": 目标目录配置 ID,
  "addPaused": false
}
```

成功条件：

- 返回任务 ID
- 任务状态进入 `queued` 或 `downloading`
- 随后能够补齐 `torrentHash`

### 11.3 完成后自动入库

成功条件：

- 任务最终状态为 `completed`
- `autoImportStatus=success`
- 视频目录被扫描入库

## 12. AI 输出格式要求

执行完后，AI 应输出固定格式：

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

若执行失败，也必须使用同样格式，但在 `失败项` 中写明失败命令和错误输出。

## 13. 禁止事项

AI 禁止执行以下危险动作：

- 将 qB WebUI 监听到 `0.0.0.0` 且直接放通公网
- 将阿里云 visitor 端口绑定到公网地址
- 清空现有下载目录
- 删除现有数据库表数据
- 无备份覆盖已有生产配置

## 14. 人工只需准备的最小信息

如果 AI 需要人工输入，最多只应索取以下信息：

1. `ECS_PUBLIC_HOST`
2. `FRP_TOKEN`
3. `FRP_SECRET_KEY`
4. qB WebUI 用户名
5. qB WebUI 密码
6. 最终下载目录，默认 `/data/media/anime`

除这些变量外，AI 应自行完成检测、安装、写配置、启动和验证。
