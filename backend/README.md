# AI SportTask 后端

轻量级 Node.js + SQLite 后端服务，提供 Auth 认证和数据同步 REST API。

## 部署

本项目已部署在 VPS `23.94.233.92:3456`，开机自启。

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
