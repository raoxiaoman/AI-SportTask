# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

AI SportTask 是一款个人运动任务管理与打卡应用，基于 Kotlin Multiplatform 构建。支持 Android 和 iOS 平台，帮助用户以动作为单位组织训练、制定计划、记录打卡和统计分析。

### 核心概念
- **动作分组（ActionGroup）**：用于组织同类训练动作的集合
- **动作（Action）**：具体训练条目，包含步骤说明、默认时长与休息时长
- **训练序列**：按照动作的 `order_index` 生成的执行队列
- **打卡（Checkin）**：记录某日训练的完成状态与耗时的数据项

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
```
├── shared/
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/
│   │   │   │   ├── App.kt             # 应用主入口与导航
│   │   │   │   ├── data/              # 数据层（Repository）
│   │   │   │   └── pages/             # 页面组件
│   │   │   └── sqldelight/            # SQLDelight 数据库定义
│   │   └── build.gradle.kts
├── androidApp/                         # Android 应用入口
├── iosApp/                             # iOS 应用项目
└── docs/                               # 需求文档与 UI 原型
```

### 代码组织
- `App.kt`: 应用主入口，包含底部导航（训练、分组、打卡、统计 4 个 Tab）
- `pages/`: 页面组件 - `TrainingPage.kt`、`GroupPage.kt`、`CheckinPage.kt`、`StatisticsPage.kt`
- `data/Repository.kt`: 数据访问层，封装 SQLDelight 查询
- `db/Database.kt`: 数据库提供方，使用 `expect/actual` 模式实现平台特定的 SqlDriver

### 入口点
- Android: `MainActivity.kt` → `MainView()` → `App()`
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

## 功能需求

### 已实现
- 分组管理：创建、编辑、删除功能
- 底部导航栏（训练、分组、打卡、统计）
- Repository 数据访问层

### 待实现
1. **动作管理**：动作列表、编辑页面、添加/编辑/排序/删除
2. **训练功能**：训练执行页面（计时、休息提示）、开始/暂停/跳过/重复
3. **打卡记录**：打卡列表页面、训练完成后打卡
4. **数据统计**：日/周/月统计、连续打卡天数、趋势展示

## 交互流程

1. 创建分组 → 添加动作（设置时长/休息/排序）
2. 选择分组开始训练 → 逐个动作计时与休息 → 可跳过/重复
3. 训练完成 → 创建打卡记录
4. 查看统计 → 按区间查看次数与时长 → 查看连续打卡

## 关键约定

- 数据库时间戳存储为 ISO 日期字符串（`LocalDate.now().toString()`）
- Repository 方法使用 `Dispatchers.Default` + `withContext`
- Compose UI 使用 Material 主题和 Scaffold + BottomNavigation
- CRUD 操作使用对话框实现（添加/编辑/删除分组）
- 日期格式约定为 `YYYY-MM-DD`
- 数据本地优先存储，云同步为后续迭代方向
