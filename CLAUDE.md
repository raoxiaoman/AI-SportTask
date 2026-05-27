# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

AI SportTask 是一款个人运动任务管理与打卡应用，基于 Kotlin Multiplatform 构建。支持 Android 和 iOS 平台，帮助用户以动作为单位组织训练、制定计划、记录打卡、统计分析，并支持云端同步。

### 核心概念
- **动作分组（ActionGroup）**：用于组织同类训练动作的集合
- **动作（Action）**：具体训练条目，包含步骤说明、默认时长与休息时长
- **训练序列**：按照动作的 `order_index` 生成的执行队列
- **打卡（Checkin）**：记录某日训练的完成状态与耗时的数据项
- **成就系统**：基于训练数据自动触发成就解锁

## 构建命令

```bash
# 构建并安装到 Android 设备
./gradlew androidApp:installDebug

# 仅构建 APK
./gradlew androidApp:assembleDebug

# 清理构建
./gradlew clean androidApp:assembleDebug

# 启动后端（自托管）
cd /path/to/sporttask-backend
./sporttask-backend.sh start
```

## 服务器配置

**后端地址不提交到 git。** 通过以下方式配置：

1. `local.properties`（项目根目录，已 `.gitignore`）：
   ```
   api.baseUrl=http://你的服务器IP:3456
   ```
   编译时 Gradle 自动生成 `cloud/ServerConfig.kt`。

2. Android 首次启动从 `assets/server_url.txt` 读取（已 `.gitignore`）。

3. 登录页面底部可手动配置服务器地址，保存后自动持久化。

## 架构说明

### 多平台项目结构
```
├── shared/
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/
│   │   │   │   ├── App.kt                # 应用主入口（登录拦截 + 导航）
│   │   │   │   ├── data/                 # 本地数据层
│   │   │   │   │   ├── Models.kt         # 数据类定义
│   │   │   │   │   ├── Repository.kt     # SQLDelight 封装
│   │   │   │   │   ├── AchievementManager.kt  # 成就系统
│   │   │   │   │   └── remote/           # 云网络层
│   │   │   │   │       ├── ApiClient.kt      # Ktor HTTP 客户端
│   │   │   │   │       ├── AuthService.kt    # 登录/注册/Token 持久化
│   │   │   │   │       ├── ServerConfig.kt   # 服务器地址（本地持久化）
│   │   │   │   │       └── PlatformStorage.kt # expect 跨平台存储接口
│   │   │   │   ├── cloud/                # 云端数据层（并行实现）
│   │   │   │   │   ├── ApiClient.kt      # 云 API 客户端
│   │   │   │   │   ├── AuthService.kt    # 云认证服务（StateFlow）
│   │   │   │   │   ├── SyncManager.kt    # 同步编排器
│   │   │   │   │   └── SupabaseConfig.kt # Supabase 配置
│   │   │   │   ├── pages/                # 页面组件
│   │   │   │   │   ├── LoginScreen.kt    # 登录/注册页面
│   │   │   │   │   ├── TrainingPage.kt   # 训练执行
│   │   │   │   │   ├── GroupPage.kt      # 分组管理
│   │   │   │   │   ├── CheckinPage.kt    # 打卡记录
│   │   │   │   │   ├── StatisticsPage.kt # 数据统计
│   │   │   │   │   └── SettingsPage.kt   # 设置+云同步
│   │   │   │   ├── ui/                   # 通用 UI 组件
│   │   │   │   └── db/Database.kt        # 数据库提供方
│   │   │   └── sqldelight/               # SQLDelight 查询定义
│   │   └── build.gradle.kts
├── androidApp/                           # Android 应用入口
├── iosApp/                               # iOS 应用项目
├── backend/ (or external)                # 自托管后端（独立仓库）
└── docs/
```

### 代码组织
- `App.kt`: 应用主入口，先检查登录态，未登录显示 `LoginScreen`，已登录显示 `MainAppScreen`
- `MainAppScreen`: 包含底部导航（训练、分组、打卡、统计、设置 5 个 Tab）
- `data/remote/AuthService`: 使用 `expect/actual PlatformStorage` 持久化 Token（跨平台）
- `cloud.AuthService`: 使用 `StateFlow` 模式的备选实现（from upstream）
- `pages/SettingsPage`: 已集成云同步板块（账号信息、手动同步、退出登录）

### 入口点
- Android: `MainActivity.kt` → `MainView()` → `App()`（先初始化 PlatformStorage + 读取服务器地址）
- iOS: `main.ios.kt` → `MainViewController()` → `App()`

## 数据库 schema (SQLDelight)

位于 `shared/src/commonMain/sqldelight/sporttask.sq`：

| 表名 | 字段 | 说明 |
|------|------|------|
| `action_group` | `id`, `name`, `created_at` | 动作分组，主键自增 |
| `action` | `id`, `group_id`, `name`, `steps_text`, `default_time`, `rest_time`, `order_index`, `created_at` | 动作明细，`group_id` 为外键 |
| `checkin` | `id`, `date`, `group_id`, `action_id`, `duration`, `is_completed` | 打卡记录，`date` 格式 `YYYY-MM-DD`，`is_completed` 使用 0/1 |

### 单位约定
- `default_time`、`rest_time`、`duration`：单位为秒
- `is_completed`：0/1 表示布尔状态

### 查询命名规范
- 获取：`getAllActionGroups`、`getActionsByGroup`、`getDailySummary`
- 插入：`insertActionGroup`、`insertAction`、`insertCheckin`
- 更新：`updateActionGroupName`、`updateActionOrder`
- 删除：`deleteActionGroupById`、`deleteActionById`

## 功能状态

### 已实现 ✅
- 分组管理：创建、编辑、删除
- 动作管理：添加、编辑、排序、删除（含长按拖拽排序）
- 训练执行：进度环动画、大字体倒计时、动作切换动效、开始/暂停/跳过/重复
- 训练音效与震动反馈
- 打卡记录：按日查看、训练完成后自动打卡
- 数据统计：日/周/月趋势、连续打卡天数
- 导入/导出：JSON 格式数据
- 深色模式
- 训练提醒通知（Android）
- 成就系统：自动解锁条件成就
- 底部导航栏（训练、分组、打卡、统计、设置 5 个 Tab）
- 设置页面：深色模式、通知、训练偏好、数据管理
- **云同步后端**：自托管 Node.js + Express + SQLite + JWT 认证 ✅
  - 用户注册/登录 (JWT)
  - 数据增量拉取 (Pull) / 推送 (Push) / 全量同步 (Full)
  - 自启脚本 + crontab 开机自启
- **App 端登录**：LoginScreen 登录/注册双模式 ✅
- **网络层**：Ktor HTTP 客户端（ApiClient）✅
- **Token 持久化**：expect/actual 跨平台存储 ✅
- **服务器配置**：从本地文件读取，不提交 git ✅

### 待实现 📋
1. **本地操作 ↔ 云端同步串联**：CRUD 操作后自动调用 `markDirty` 触发同步
2. **iOS 编译验证**：需 macOS 环境
3. **离线队列**：无网络时变更暂存 pending_operations 表
4. **同步冲突处理**：服务端时间戳比对
5. **批量离线同步**：恢复网络后自动补传

## 交互流程

1. **启动 → 登录**：App 启动 → 检查本地 Token → 未登录显示登录页 → 登录/注册 → 进入主界面
2. **创建分组 → 添加动作**（设置时长/休息/排序）
3. **选择分组开始训练** → 逐个动作计时与休息 → 可跳过/重复
4. **训练完成** → 自动打卡 → 检查成就
5. **数据同步**：设置页手动同步 / 后续自动同步
6. **查看统计** → 按区间查看次数与时长 → 查看连续打卡

## 后端 API

后端运行在 `http://<server-ip>:3456`：

| 端点 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/health` | GET | 否 | 健康检查 |
| `/api/auth/signup` | POST | 否 | 注册 |
| `/api/auth/signin` | POST | 否 | 登录 |
| `/api/user` | GET | 是 | 用户信息 |
| `/api/sync/push` | POST | 是 | 推送本地数据 |
| `/api/sync/pull` | POST | 是 | 拉取增量数据 |
| `/api/sync/full` | GET | 是 | 全量同步 |

## 关键约定

- 数据库时间戳存储为 ISO 日期字符串（`LocalDate.now().toString()`）
- Repository 方法使用 `Dispatchers.Default` + `withContext`
- Compose UI 使用 Material 主题和 Scaffold + BottomNavigation
- CRUD 操作使用对话框实现（添加/编辑/删除分组）
- 日期格式约定为 `YYYY-MM-DD`
- 数据本地优先存储，云同步为异步后台操作
- **IP 地址不提交到 git**，通过 local.properties + Gradle 生成配置
- 后端地址在 `SettingsPage` 和 `LoginScreen` 底部均可查看/修改
- 每完成一个功能，提交并推送
