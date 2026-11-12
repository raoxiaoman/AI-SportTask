# AI SportTask 🏋️

[![Android 自动打包](https://github.com/raoxiaoman/AI-SportTask/actions/workflows/android-build.yml/badge.svg)](https://github.com/raoxiaoman/AI-SportTask/actions/workflows/android-build.yml)
[![Release v1.0.0](https://img.shields.io/badge/Release-v1.0.0-blue)](https://github.com/raoxiaoman/AI-SportTask/releases/tag/v1.0.0)

一款基于 Kotlin Multiplatform 的个人运动任务管理与打卡应用，支持 **Android** 和 **iOS**，并配有自托管云端同步后端。

> **最新版本 [v1.0.0](https://github.com/raoxiaoman/AI-SportTask/releases/tag/v1.0.0)**（2026-11-12）
> · [下载签名 APK](https://github.com/raoxiaoman/AI-SportTask/releases/download/v1.0.0/AI-SportTask-v1.0.0-release.apk)
> · [全部 Release](https://github.com/raoxiaoman/AI-SportTask/releases)

## 核心功能

### 🎯 训练管理
- **分组管理**：创建、编辑、删除训练分组；分组内长按拖拽排序
- **动作管理**：设置名称、步骤说明、默认时长、休息时长、排序
- **训练执行**：进度环动画 + 大字体倒计时 + 动作切换动效
- **计时模式**：开始/暂停/跳过/重复，自动休息计时
- **计时反馈**：全程每秒秒针声、最后 5 秒中文语音倒数、物理返回拦截
- **本周节奏**：首页展示一周训练安排与完成状态

### 📖 动作图解与示范（v1.0.0）
- **详细指导**：内置 23 个康复动作的分步骤、要点、禁忌信号
- **统一示意图**：Compose Canvas 自绘线性小人，风格一致、零外部图片依赖
- **视频示范**：每个动作一键检索示范视频（外部浏览器打开），也支持配置直链优先直达
- 分组动作列表与训练执行页均可打开详情弹窗

### ✅ 打卡与统计
- **每日打卡**：训练完成后自动记录
- **数据统计**：按日/周/月查看训练次数与时长
- **连续打卡**：自动计算连续天数

### 🏆 成就系统
自动解锁条件成就（如首次训练、连续打卡、累计时长等），训练完成时弹窗展示。

### ☁️ 云同步（v0.3，预留能力）
> ⚠️ **服务端尚未部署**：以下为预留能力，当前 App 可在登录页点「先不登录 · 离线使用」，
> 分组 / 训练 / 打卡 / 统计 / 动作图解等本地功能不依赖服务端。

- **自托管后端**：Node.js + Express + SQLite + JWT 认证
- **用户系统**：注册、登录、Token 自动持久化
- **数据同步**：增量 Pull / Push / 全量 Full
- **手动同步**：设置页一键同步
- **多端共享**：同一账号在不同设备间同步数据
- **隐私安全**：服务器地址运行时配置并持久化在本地，不硬编码进源码、不提交到 git

### 🎨 其他
- 深色模式
- 训练提醒通知（Android）
- 数据导入/导出（JSON）
- 服务器地址随时可配置
- 统一设计系统与全站统一弹窗组件（见「界面与设计规范」）

## 界面与设计规范

设计规范已落文档：[docs/UI设计规范.md](docs/UI设计规范.md)。包含语义色、字阶、4pt 间距栅格、圆角与图标 token、页面与卡片结构、弹窗规范、系统栏与沉浸式策略、禁止硬编码清单、新增页面交付检查表。

页面层不得硬编码字号/颜色/间距；所有颜色走 `AppTheme.colors`、尺寸走 `AppDimens`、圆角走 `AppShapes`、图标走 `AppIcons`，弹窗统一使用 `AppDialog` / `AppConfirmDialog` / `AppTextField`。全屏 `Dialog` 需调用 `DialogSystemBars` 同步系统栏，避免独立 Window 出现深灰状态栏。

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
| **构建** | Gradle 8.5 · JDK 17 · AGP 8.1.2 · compileSdk 34 |
| **CI/CD** | GitHub Actions（单测 + Debug/Release APK + GitHub Release） |

## 构建与发布（GitHub Actions）

| 触发方式 | 产出 |
|---|---|
| push / PR 到 `main` | 303 项单测 + Debug APK（artifact 保留 30 天） |
| 手动运行 workflow（可填 `version`、勾 `build_release`） | 追加**签名 Release APK**，用于验证发版链路 |
| 推送 `v*` 标签，如 `v1.0.0` | 签名 Release APK + **GitHub Release**（只附正式签名包） |

- 正式版本到 [Releases](https://github.com/raoxiaoman/AI-SportTask/releases) 下载 `AI-SportTask-vX.Y.Z-release.apk`。
- 日常构建在 [Actions 页面](https://github.com/raoxiaoman/AI-SportTask/actions/workflows/android-build.yml) 的 **Artifacts** 下载 `AI-SportTask-apk-<run>`（含 debug 包，手动构建时另含 release 包）与测试报告。

**版本号由 tag 驱动**：`v1.0.0` → `versionName=1.0.0`、`versionCode=GITHUB_RUN_NUMBER`（自动递增）；APK 文件名、Release 标题、设置页「关于」三处同源（Android 读 `BuildConfig`、iOS 读 `Info.plist`）。本地未指定时默认 `1.0.0-dev` / `1`。

**签名**：release 密钥保存在仓库 Secrets 中，不进入代码库。正式版本之间可直接覆盖安装；Debug 包与正式包签名互不兼容。

发版命令（用 annotated tag，便于记录发版说明）：

```bash
git tag -a v1.0.1 -m "AI SportTask v1.0.1"
git push origin v1.0.1
# 约 5 分钟后自动生成 GitHub Release；generate_release_notes 会汇总本次变更
```

本地等价构建：

```bash
# Debug（默认，无需密钥）
./gradlew :shared:testDebugUnitTest :androidApp:assembleDebug \
  -Papp.versionName=1.0.0 -Papp.versionCode=42
# 产物：androidApp/build/outputs/apk/debug/androidApp-debug.apk

# 签名 Release：额外提供 4 个属性（推荐用 ORG_GRADLE_PROJECT_* 环境变量，避免密码进入命令行）
# keystorePath / keystoreStorePassword / keystoreKeyAlias / keystoreKeyPassword
./gradlew :androidApp:assembleRelease -Papp.versionName=1.0.0 -Papp.versionCode=42
```

> CI 不包含任何本地配置，包内默认指向 `http://localhost:3456`；云同步请在登录页「服务器设置」填写实际地址。
> **安装**：若此前装过 Debug 包，需先卸载再装正式签名包（会清空本地数据，请先在「设置 → 导出数据」备份 JSON）；之后的版本可直接覆盖。
> **Release 签名密钥丢失将无法发布应用更新，请离线备份 keystore 与密码说明文件。**

## 快速开始

### 1. 克隆项目
```bash
git clone https://github.com/raoxiaoman/AI-SportTask.git
cd AI-SportTask
```

### 2. 配置服务器地址（可选，不配置则默认 localhost）

服务器地址由 `ServerConfig` 在运行时管理，默认 `http://localhost:3456`：
登录页底部点击「服务器设置」→ 输入地址 → 保存，即写入本地持久化存储（`sporttask_server_url`），设置页会显示当前地址。地址不硬编码在源码里，也不随安装包分发。

> `local.properties` 中的 `api.baseUrl` 只是本机记录 VPS 地址的备注，**不参与构建、也不会被 App 读取**；
> 该文件已 gitignore，服务器 IP 不会进入仓库。因此 CI 打出的包一律默认 `localhost:3456`。
> **服务端尚未部署时，请在登录页点「先不登录 · 离线使用」直接进入主界面**（该选择会持久化，下次启动免点）；
> 服务端上线后，可在设置页「未登录 · 离线使用」行返回登录页，并在登录页「服务器设置」填写实际地址。

### 3. 启动后端（自托管，随仓库提供）
```bash
cd backend
./sporttask-backend.sh start
```

### 4. 构建运行
```bash
# Android
./gradlew :androidApp:installDebug

# iOS（需 macOS + Xcode，目前尚未完成编译验证）
# 在 Xcode 中打开 iosApp/ 项目
```

也可以直接安装 [v1.0.0 签名 APK](https://github.com/raoxiaoman/AI-SportTask/releases/download/v1.0.0/AI-SportTask-v1.0.0-release.apk)：
```bash
adb install -r AI-SportTask-v1.0.0-release.apk
```

## 项目结构

```
├── .github/workflows/android-build.yml   # CI：单测 / 打包 / 签名 / 发版
├── docs/UI设计规范.md                   # UI 设计规范
├── shared/                              # KMP 共享代码
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── App.kt                   # 应用入口（登录拦截 + 导航）
│       │   ├── data/                    # 本地数据 + 远程网络层
│       │   │   ├── Models.kt
│       │   │   ├── Repository.kt
│       │   │   ├── AchievementManager.kt
│       │   │   └── remote/              # 云网络层
│       │   │       ├── ApiClient.kt
│       │   │       ├── AuthService.kt
│       │   │       ├── ServerConfig.kt
│       │   │       └── PlatformStorage.kt (expect)
│       │   ├── cloud/                   # 云端实现（并行）
│       │   ├── pages/                   # 页面组件
│       │   │   ├── LoginScreen.kt
│       │   │   ├── TrainingPage.kt
│       │   │   ├── GroupPage.kt
│       │   │   ├── CheckinPage.kt
│       │   │   ├── StatisticsPage.kt
│       │   │   ├── SettingsPage.kt
│       │   │   └── ExternalLink.kt (expect)
│       │   └── ui/                      # 设计系统与通用组件
│       │       ├── theme/               # 色彩 / 字阶 / 间距 / 圆角 token
│       │       ├── AppIcons.kt          # 自绘线性图标
│       │       ├── AppDialog.kt         # 统一弹窗与输入框
│       │       ├── ActionGuideDialog.kt # 动作图解详情
│       │       ├── ExerciseGuides.kt    # 23 个动作指导库
│       │       ├── ExerciseDiagram.kt   # Canvas 自绘示意图
│       │       ├── DialogSystemBars.kt  # 全屏 Dialog 系统栏 (expect)
│       │       └── AppVersion.kt        # 版本号 (expect)
│       └── androidMain/ & iosMain/      # 平台实际实现
├── androidApp/                          # Android 入口（含 release 签名配置）
├── iosApp/                              # iOS 入口
└── backend/                             # 自托管后端（Node.js + Express + SQLite）
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

### ✅ v1.0.0 — 首个正式版本（2026-11-12）
- 动作详细图解：分步骤 / 要点 / 禁忌信号 + 自绘示意图 + 视频示范检索
- UI 设计规范落地（`docs/UI设计规范.md`）+ 全站统一弹窗与输入框组件
- 修复全屏 Dialog 的系统栏沉浸式表现，登录页与表单语义色统一
- GitHub Actions 自动打包：303 项单测、Debug / 签名 Release APK、密钥 Secrets 化
- 版本号由 tag 驱动，APK 文件名 / Release 标题 / App 关于页同源

### 🔜 v1.0.1 — 自动同步
- 本地 CRUD 操作串联 markDirty
- 离线变更暂存队列
- 恢复网络时自动补发

### 🔜 v1.1+ — 增强
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
