# AI SportTask 后端

轻量级 Node.js + SQLite 后端服务，提供 Auth 认证和数据同步 REST API。

## 部署状态

> **服务端尚未部署 / 联调**：本目录目前是一版可运行的骨架代码，未部署到 VPS，也未与 App 联调。
> App 默认 `http://localhost:3456`，云同步暂不可用；地址请在 App 登录页「服务器设置」中配置。
> `local.properties` 的 `api.baseUrl` 是历史遗留的本机备注，Gradle 与 App 均不读取。

本地开发联调可用下方命令手动启动。

### 手动管理

```bash
cd backend
chmod +x sporttask-backend.sh
./sporttask-backend.sh start     # 启动
./sporttask-backend.sh stop      # 停止
./sporttask-backend.sh restart   # 重启
./sporttask-backend.sh status    # 查看状态
./sporttask-backend.sh logs      # 查看日志
```

### 从零部署

```bash
npm install
node server.js &
```

## API 接口

### 公开路由

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 健康检查 |
| POST | `/api/auth/signup` | 注册 {email, password} |
| POST | `/api/auth/signin` | 登录 {email, password} |

### 需要认证 (Header: `Authorization: Bearer <token>`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user` | 当前用户信息 |
| POST | `/api/sync/push` | 推送本地变更 |
| POST | `/api/sync/pull` | 拉取云端增量 |
| GET | `/api/sync/full` | 首次全量同步 |
