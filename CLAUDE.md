# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

AI SportTask 是一款个人运动任务管理与打卡应用，基于 Kotlin Multiplatform 构建。支持 Android 和 iOS 平台，帮助用户以动作为单位组织训练、制定计划、记录打卡和统计分析。

## 构建命令

```bash
# 构建并安装到 Android 设备
./gradlew androidApp:installDebug

# 仅构建 APK
./gradlew androidApp:assembleDebug

# 清理构建
./gradlew clean androidApp:assembleDebug
```

## 架构说明

### 多平台项目结构
- **shared/**: 跨平台代码（Android & iOS 共享）
  - `commonMain/kotlin/`: 业务逻辑和 UI 代码
  - `androidMain/kotlin/`: Android 平台入口
  - `iosMain/kotlin/`: iOS 平台入口
- **androidApp/**: Android 应用入口
- **iosApp/**: iOS 应用项目

### 代码组织
- `App.kt`: 应用主入口，包含底部导航（训练、分组、打卡、统计 4 个 Tab）
- `pages/`: 页面组件 - `TrainingPage.kt`、`GroupPage.kt`、`CheckinPage.kt`、`StatisticsPage.kt`
- `data/Repository.kt`: 数据访问层，封装 SQLDelight 查询
- `db/Database.kt`: 数据库提供方，使用 `expect/actual` 模式实现平台特定的 SqlDriver

### 数据库 schema (SQLDelight)
位于 `sporttask.sq`：
- `action_group`: 动作分组
- `action`: 动作明细，关联分组（含 order_index 用于排序）
- `checkin`: 打卡记录，包含日期、耗时、完成状态

查询命名规范：`getAllActionGroups`、`insertActionGroup`、`deleteActionGroupById` 等。

### 入口点
- Android: `MainActivity.kt` → `MainView()` → `App()`
- iOS: `main.ios.kt` → `MainViewController()` → `App()`

## 关键约定

- 数据库时间戳存储为 ISO 日期字符串（`LocalDate.now().toString()`）
- Repository 方法使用 `Dispatchers.Default` + `withContext`
- Compose UI 使用 Material 主题和 Scaffold + BottomNavigation
- CRUD 操作使用对话框实现（添加/编辑/删除分组）
