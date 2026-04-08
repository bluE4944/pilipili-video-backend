# 远程下载中心部署说明

## 1. 目标

本方案用于把已经实现的“管理员下载中心”接到家庭下载机上的 `qBittorrent WebUI`，并通过阿里云服务器作为中转，让你在外网也能通过自己的网站后台发起下载、查看状态、暂停、继续、删除任务，并在下载完成后自动触发视频扫描入库。

如果需要让另一个 AI 代理直接按步骤执行安装和部署，优先阅读：

- [docs/ai-agent-deployment-runbook.md](/d:/workspace/pilipili-video-backend/docs/ai-agent-deployment-runbook.md)

核心原则：

- 浏览器永远只访问你自己的前后端服务。
- 家庭下载机不直接暴露 `qBittorrent WebUI` 到公网。
- 阿里云服务器只开放 `frps` 监听端口，不开放 `qB WebUI`。
- 后端只访问阿里云本机回环地址 `127.0.0.1:18080`。

## 2. 链路结构

```text
浏览器
  -> Pilipili 前端
  -> Pilipili 后端
  -> 阿里云本机 127.0.0.1:18080
  -> frpc(visitor)
  -> frps
  -> 家庭下载机 frpc
  -> qBittorrent WebUI
  -> 家庭下载目录
```

其中：

- 家庭下载机主动连出到阿里云 `frps`，不需要公网入站。
- 阿里云上的 `frpc(visitor)` 也主动连到 `frps`。
- `visitor` 和家庭下载机的 `stcp` 服务通过同一个 `secretKey` 建立安全隧道。
- Spring Boot 服务通过 `download.qb.base-url=http://127.0.0.1:18080` 访问下载器。

## 3. 当前后端已实现内容

已实现接口：

- `POST /api/admin/download/tasks/magnet`
- `POST /api/admin/download/tasks/torrent`
- `GET /api/admin/download/tasks`
- `GET /api/admin/download/tasks/{taskId}`
- `POST /api/admin/download/tasks/{taskId}/pause`
- `POST /api/admin/download/tasks/{taskId}/resume`
- `DELETE /api/admin/download/tasks/{taskId}?deleteFiles=false`
- `POST /api/admin/download/tasks/{taskId}/retry-import`
- `GET /api/admin/download/status`

已实现行为：

- 创建磁力下载任务
- 上传 `.torrent` 文件创建下载任务
- 任务列表和详情查询
- 暂停、继续、删除
- 每 5 秒轮询 qB 状态
- 下载完成后自动调用现有扫描逻辑入库
- 入库失败后支持手动重试

## 4. 目录与角色约束

要让自动入库可靠，必须满足这一条：

- 后端任务里使用的 `folderConfigId` 对应的 `t_local_folder_config.folder_path`
- 必须与家庭下载机里 qB 实际写入的下载目录一致

如果路径不一致，下载虽然能完成，但扫描入库会找不到文件。

示例：

- 家庭下载机 qB 保存目录：`/data/media/anime`
- 后端 `t_local_folder_config.folder_path`：`/data/media/anime`

不要让前端自由输入保存路径，当前实现已经固定从 `folderConfigId` 关联的目录读取。

## 5. 家庭下载机部署步骤

### 5.1 安装 qBittorrent

Manjaro 建议直接使用无头版 `qBittorrent-nox`，更适合做长期后台下载节点。

安装示例：

```bash
sudo pacman -S qbittorrent-nox
```

建议：

- 单独创建下载账号
- 下载目录使用稳定挂载点，不要使用临时移动盘或临时挂载目录
- 开启开机自启

### 5.2 开启 WebUI

在 qB 设置中启用 WebUI，建议：

- 监听地址：`127.0.0.1` 或家庭局域网 IP
- 监听端口：例如 `8080`
- 设置独立用户名密码
- 关闭“对所有地址暴露且无鉴权”的危险配置

推荐最小化暴露：

- 如果 `frpc(home)` 和 qB 在同一台机器，优先让 WebUI 只监听 `127.0.0.1:8080`

### 5.3 配置下载目录

例如：

- 默认保存目录：`/data/media/anime`
- 分类：`pilipili`

并在你的后台“本地文件夹配置”中创建对应目录配置：

- 配置名：`家庭下载目录`
- 目录：`/data/media/anime`
- 启用：`1`

### 5.4 部署家庭下载机 frpc

把示例文件 [deploy/frp/frpc-home.toml.example](/d:/workspace/pilipili-video-backend/deploy/frp/frpc-home.toml.example) 复制为实际配置，例如 `frpc.toml`，按你的实际地址修改：

- `serverAddr`
- `serverPort`
- `auth.token`
- `secretKey`
- `localIP`
- `localPort`

Manjaro 手动启动示例：

```bash
./frpc -c ./frpc-home.toml
```

启动后应能看到已连接到阿里云 `frps`。

### 5.5 Manjaro 下的 systemd 托管

建议把 `qBittorrent-nox` 和 `frpc(home)` 都做成 `systemd` 服务，避免机器重启后需要手工拉起。

`qbittorrent-nox` 在 Arch/Manjaro 的官方包里已经自带 `qbittorrent-nox.service` 和 `qbittorrent-nox@.service`。如果你直接使用系统自带服务，就不一定需要下面这份自定义样例；自定义样例的作用主要是方便你统一路径、用户和启动参数。

已提供样例：

- [deploy/systemd/qbittorrent-nox.service.example](/d:/workspace/pilipili-video-backend/deploy/systemd/qbittorrent-nox.service.example)
- [deploy/systemd/frpc-home.service.example](/d:/workspace/pilipili-video-backend/deploy/systemd/frpc-home.service.example)

典型流程：

```bash
sudo cp deploy/systemd/qbittorrent-nox.service.example /etc/systemd/system/qbittorrent-nox.service
sudo cp deploy/systemd/frpc-home.service.example /etc/systemd/system/frpc-home.service
sudo systemctl daemon-reload
sudo systemctl enable --now qbittorrent-nox.service
sudo systemctl enable --now frpc-home.service
```

查看状态：

```bash
systemctl status qbittorrent-nox.service
systemctl status frpc-home.service
```

## 6. 阿里云服务器部署步骤

### 6.1 安装 frps

使用示例文件 [deploy/frp/frps.toml.example](/d:/workspace/pilipili-video-backend/deploy/frp/frps.toml.example)。

建议：

- `bindPort` 使用独立端口，例如 `7000`
- 安全组只放行这个 `frps` 端口
- 不要开放 `18080`

Linux 启动示例：

```bash
./frps -c ./frps.toml
```

### 6.2 安装 frpc(visitor)

使用示例文件 [deploy/frp/frpc-visitor.toml.example](/d:/workspace/pilipili-video-backend/deploy/frp/frpc-visitor.toml.example)。

关键点：

- `visitor` 监听在阿里云本机 `127.0.0.1:18080`
- Spring Boot 服务只连接这个地址
- `secretKey` 必须和家庭下载机 `frpc(home)` 中的 `secretKey` 一致

Linux 启动示例：

```bash
./frpc -c ./frpc-visitor.toml
```

### 6.3 部署 Spring Boot 服务

后端配置示例：

```yaml
download:
  qb:
    base-url: http://127.0.0.1:18080
    username: your-qb-username
    password: your-qb-password
    poll-interval-seconds: 5
    default-category: pilipili
```

推荐通过环境变量或私有配置覆盖，不要把真实密码提交进仓库。

### 6.4 安全组与防火墙

阿里云需要：

- 放行 `frps.bindPort`，例如 `7000/tcp`
- 放行你的 Spring Boot 对外端口，例如 `8316/tcp`
- 不要放行 `127.0.0.1:18080`
- 不要放行家庭下载机 qB WebUI 端口

## 7. 示例配置文件说明

已提供：

- [docs/remote-download-deployment.md](/d:/workspace/pilipili-video-backend/docs/remote-download-deployment.md)
- [deploy/frp/frps.toml.example](/d:/workspace/pilipili-video-backend/deploy/frp/frps.toml.example)
- [deploy/frp/frpc-visitor.toml.example](/d:/workspace/pilipili-video-backend/deploy/frp/frpc-visitor.toml.example)
- [deploy/frp/frpc-home.toml.example](/d:/workspace/pilipili-video-backend/deploy/frp/frpc-home.toml.example)
- [deploy/qbittorrent/README.md](/d:/workspace/pilipili-video-backend/deploy/qbittorrent/README.md)
- [deploy/systemd/qbittorrent-nox.service.example](/d:/workspace/pilipili-video-backend/deploy/systemd/qbittorrent-nox.service.example)
- [deploy/systemd/frpc-home.service.example](/d:/workspace/pilipili-video-backend/deploy/systemd/frpc-home.service.example)

## 8. 推荐部署顺序

1. 先在家庭下载机安装并验证 qB WebUI 本地可访问。
2. 再部署阿里云 `frps`。
3. 部署家庭下载机 `frpc(home)`。
4. 部署阿里云 `frpc(visitor)`，验证阿里云本机可访问 `http://127.0.0.1:18080`。
5. 配置 Spring Boot 的 `download.qb.*`。
6. 在后台创建或检查 `t_local_folder_config` 是否与下载目录一致。
7. 打开下载管理页，先调 `GET /api/admin/download/status` 验证连通。
8. 创建一个磁力任务联调。
9. 等待下载完成，确认自动扫描入库。

## 9. 联调检查清单

### 9.1 下载器状态

调用：

```http
GET /api/admin/download/status
```

期望：

- `connected=true`
- `version` 有值
- `defaultSavePath` 有值
- `message=连接正常`

### 9.2 创建磁力任务

调用：

```http
POST /api/admin/download/tasks/magnet
Content-Type: application/json

{
  "magnetUrl": "magnet:?xt=urn:btih:...",
  "folderConfigId": 1,
  "addPaused": false
}
```

期望：

- 返回任务 ID
- 任务状态先是 `queued` 或 `downloading`
- 数秒内补齐 `torrentHash`

### 9.3 上传种子文件

调用 `multipart/form-data`：

- `torrentFile`
- `folderConfigId`
- `addPaused`

期望：

- 返回任务 ID
- qB 中出现对应任务

### 9.4 自动入库

期望：

- 下载完成后任务进入 `completed`
- `autoImportStatus` 变为 `success`
- 对应目录里的视频被扫描入库

## 10. 常见故障排查

### 10.1 `/api/admin/download/status` 报连接失败

优先检查：

- 阿里云 `frpc(visitor)` 是否在运行
- 家庭下载机 `frpc(home)` 是否在运行
- 两边 `secretKey` 是否一致
- 两边 `auth.token` 是否和 `frps` 一致
- 后端配置的 `download.qb.base-url` 是否是 `http://127.0.0.1:18080`
- 家庭下载机 qB WebUI 是否真的监听在 `frpc(home)` 指向的地址端口

阿里云本机先验证：

```bash
curl http://127.0.0.1:18080/api/v2/app/version
```

如果这里不通，问题不在 Spring Boot，而在 `frp` 或 qB 本身。

### 10.2 qB 登录失败

检查：

- `download.qb.username`
- `download.qb.password`
- qB WebUI 是否启用了 CSRF、Host Header 等限制导致当前代理链路被拦截
- qB 是否修改了监听端口但 `frpc(home)` 没同步更新

### 10.3 任务创建成功但一直拿不到 hash

检查：

- qB 是否真的收到了带 `tags=pilipili_task_xxx` 的任务
- 同步任务是否在运行
- `download.qb.poll-interval-seconds` 是否被错误改得过大

当前实现会先按 `tag` 反查，再回填 `torrent_hash`。

### 10.4 下载完成但未入库

优先检查：

- `folderConfigId` 绑定目录是否就是 qB 实际下载目录
- 下载内容是否在扫描规则支持的目录层级中
- `autoImportStatus` 是否为 `failed`
- `autoImportMessage` 的具体报错内容

如果任务已完成但入库失败，可调用：

```http
POST /api/admin/download/tasks/{taskId}/retry-import
```

### 10.5 删除任务时文件没删掉

确认调用时参数是：

```http
DELETE /api/admin/download/tasks/{taskId}?deleteFiles=true
```

后端会把 `deleteFiles` 原样透传给 qB 删除接口。

## 11. 生产环境建议

- 给 qB 使用独立账号，不要复用系统管理员密码。
- qB WebUI 仅监听本机或局域网地址，不暴露公网。
- `frp` 的 `secretKey` 使用高强度随机字符串。
- `frp` 的 `auth.token` 与 `secretKey` 不要相同。
- 阿里云只开放 `frps` 监听端口，不开放 visitor 本地端口。
- 下载目录使用固定磁盘和固定路径，不要频繁变更。
- 为 Spring Boot、frps、frpc 做系统服务化，避免重启后未自动恢复。
- 如果后续要扩展多下载节点，再单独设计 `t_download_node`，不要在 v1 上硬塞。

## 12. 最小上线验收

满足以下 6 条再算上线完成：

1. 阿里云本机可以访问 `http://127.0.0.1:18080/api/v2/app/version`
2. 后台状态页显示下载器已连接
3. 能创建磁力下载任务
4. 能上传 `.torrent` 文件任务
5. 能暂停、继续、删除任务
6. 下载完成后能自动扫描入库，失败后可手动重试
