# AI SportTask 🏋️

一款基于 Kotlin Multiplatform 的个人运动任务管理与打卡应用，支持 **Android** 和 **iOS**，并配有自托管云端同步后端。

## 核心功能

### 🎯 训练管理
- **分组管理**：创建、编辑、删除训练分组；分组内长按拖拽排序
- **动作管理**：设置名称、步骤说明、默认时长、休息时长、排序
- **训练执行**：进度环动画 + 大字体倒计时 + 动作切换动效
- **计时模式**：开始/暂停/跳过/重复，自动休息计时
- **音效与震动**：计时开始/结束时反馈

### ✅ 打卡与统计
- **每日打卡**：训练完成后自动记录
- **数据统计**：按日/周/月查看训练次数与时长
- **连续打卡**：自动计算连续天数

### 🏆 成就系统
自动解锁条件成就（如首次训练、连续打卡、累计时长等），训练完成时弹窗展示。

### ☁️ 云同步（v0.3）
- **自托管后端**：Node.js + Express + SQLite + JWT 认证
- **用户系统**：注册、登录、Token 自动持久化
- **数据同步**：增量 Pull / Push / 全量 Full
- **手动同步**：设置页一键同步
- **多端共享**：同一账号在不同设备间同步数据
- **隐私安全**：服务器地址从本地文件读取，绝不提交到 git

### 🎨 其他
- 深色模式
- 训练提醒通知（Android）
- 数据导入/导出（JSON）
- 服务器地址随时可配置

## 截图预览

| 页面 | 截图 |
|------|------|
| 登录/注册 |  |
| 训练 |  |
| 分组管理 |  |
| 打卡 |  |
| 统计 |  |
| 设置+云同步 |  |

## 技术栈

| 层 | 技术 |
|---|------|
| **前端** | Kotlin Multiplatform + Compose Multiplatform |
| **本地存储** | SQLDelight (local) |
| **网络** | Ktor Client (HTTP) |
| **序列化** | kotlinx-serialization |
| **认证** | JWT (bcrypt + jsonwebtoken) |
| **后端** | Node.js + Express + SQLite |
| **跨平台** | expect/actual 模式 |

## 快速开始

### 1. 克隆项目
```bash
git clone https://github.com/raoxiaoman/AI-SportTask.git
cd AI-SportTask
```

### 2. 配置服务器地址（可选，不配置则默认 localhost）

**Android：**
创建文件 `androidApp/src/main/assets/server_url.txt`（已 gitignored）：
```
http://你的服务器IP:3456
```

**或者通过 Gradle 配置：**
在项目根目录 `local.properties` 中添加：
```
api.baseUrl=http://你的服务器IP:3456
```
编译时自动生成配置，IP 永远不入 git。

**也可以运行时配置：**
登录页底部点击"服务器设置" → 输入地址 → 保存（自动持久化）。

### 3. 启动后端（自托管）
```bash
# 后端独立项目
cd sporttask-backend
./sporttask-backend.sh start
```

### 4. 构建运行
```bash
# Android
./gradlew androidApp:installDebug

# iOS（需 macOS）
# 在 Xcode 中打开 iosApp/ 项目
```

## 项目结构

```
├── shared/                          # KMP 共享代码
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── App.kt               # 应用入口（登录拦截 + 导航）
│       │   ├── data/                # 本地数据 + 远程网络层
│       │   │   ├── Models.kt
│       │   │   ├── Repository.kt
│       │   │   ├── AchievementManager.kt
│       │   │   └── remote/          # 云网络层
│       │   │       ├── ApiClient.kt
│       │   │       ├── AuthService.kt
│       │   │       ├── ServerConfig.kt
│       │   │       └── PlatformStorage.kt (expect)
│       │   ├── cloud/               # 云端实现（并行）
│       │   ├── pages/               # 页面组件
│       │   │   ├── LoginScreen.kt
│       │   │   ├── TrainingPage.kt
│       │   │   ├── GroupPage.kt
│       │   │   ├── CheckinPage.kt
│       │   │   ├── StatisticsPage.kt
│       │   │   └── SettingsPage.kt
│       │   └── ui/                  # 通用组件
│       └── androidMain/ & iosMain/ # 平台实际实现
├── androidApp/                      # Android 入口
├── iosApp/                          # iOS 入口
└── backend/ (external)              # 自托管后端
```

## 后端 API

| 端点 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/health` | GET | 否 | 健康检查 |
| `/api/auth/signup` | POST | 否 | 注册 |
| `/api/auth/signin` | POST | 否 | 登录 |
| `/api/user` | GET | 是 | 用户信息 |
| `/api/sync/push` | POST | 是 | 推送本地数据 |
| `/api/sync/pull` | POST | 是 | 拉取增量数据 |
| `/api/sync/full` | GET | 是 | 全量同步 |

## 迭代计划

### ✅ v0.1 — 基础骨架
- 分组管理（CRUD + 排序）
- 动作管理（CRUD + 排序）
- 底部导航

### ✅ v0.2 — 训练与打卡
- 训练执行（计时器、休息、跳过/重复）
- 打卡记录
- 数据统计（日/周/月、连续天数）
- 音效/震动、深色模式、通知

### ✅ v0.3 — 云同步
- 自托管后端（注册/登录/JWT/同步 API）
- App 端登录/注册页面
- Ktor HTTP 网络层
- Token 持久化
- 设置页云同步板块

### 🔜 v0.3.1 — 自动同步
- 本地 CRUD 操作串联 markDirty
- 离线变更暂存队列
- 恢复网络时自动补发

### 🔜 v0.4+ — 增强
- iOS 编译验证
- 动作模板库
- 训练计划分享
- 个性化统计报表

## 贡献

1. Fork 本仓库
2. 创建分支 (`git checkout -b feature/xxx`)
3. 提交更改 (`git commit -m 'feat: xxx'`)
4. 推送到分支 (`git push origin feature/xxx`)
5. 创建 Pull Request

## 许可证

[MIT License](LICENSE.txt)
