# qBittorrent 配置建议

## 1. WebUI

Manjaro 推荐直接运行 `qbittorrent-nox`：

```bash
sudo pacman -S qbittorrent-nox
```

建议配置：

- WebUI 地址：`127.0.0.1`
- WebUI 端口：`8080`
- 用户名：单独创建，不要与网站管理员账号复用
- 密码：高强度随机密码

如果 `frpc(home)` 和 qB 在同一台机器，优先只监听 `127.0.0.1`。

## 2. 下载目录

示例：

- 默认保存目录：`/data/media/anime`
- 默认分类：`pilipili`

这个目录必须与你后台“本地文件夹配置”中的 `folder_path` 一致。

## 3. 安全建议

- 不要把 qB WebUI 暴露到公网。
- 不要关闭鉴权。
- 不要使用默认账号密码。
- 如果机器上还有其他服务，限制 qB 的监听地址，避免被局域网横向访问。

## 4. 联调自检

在家庭下载机本机访问：

```text
http://127.0.0.1:8080
```

确认：

- 能正常登录
- 能手动添加一个测试任务
- 下载目录写入正常
- 下载完成后文件确实落在预期目录

然后再继续配置 `frp`。

## 5. systemd 建议

如果这台机器长期作为下载节点，建议直接使用 systemd 托管：

Arch/Manjaro 的 `qbittorrent-nox` 官方包自带 `qbittorrent-nox.service` 和 `qbittorrent-nox@.service`。如果系统自带服务已经满足你的用户、端口和目录要求，可以直接启用，不必再复制自定义服务文件。

- [deploy/systemd/qbittorrent-nox.service.example](/d:/workspace/pilipili-video-backend/deploy/systemd/qbittorrent-nox.service.example)
- [deploy/systemd/frpc-home.service.example](/d:/workspace/pilipili-video-backend/deploy/systemd/frpc-home.service.example)

按需修改：

- `User`
- `Group`
- `ExecStart`
- 下载目录权限
