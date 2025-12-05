# AI SportTask 项目状态文档

## 项目概述
AI SportTask 是一款个人运动任务管理与打卡应用，帮助用户以动作为单位组织训练，制定训练计划，按日打卡并进行统计分析。

## 技术栈
- **平台**: Android、iOS (Kotlin Multiplatform)
- **UI**: Compose Multiplatform
- **数据**: SQLDelight (本地持久化)

## 项目结构
```
├── shared/
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/
│   │   │   │   ├── App.kt             # 应用主入口与导航
│   │   │   │   ├── data/             # 数据层
│   │   │   │   ├── db/                # 数据库层
│   │   │   │   └── pages/             # 页面组件
│   │   │   └── sqldelight/            # SQLDelight 数据库定义
│   └── build.gradle.kts
├── androidApp/
├── iosApp/
├── docs/                             # 需求文档与UI原型
└── build.gradle.kts
```

## 已实现功能

### 1. 基础架构
- ✅ 项目配置与依赖管理
- ✅ SQLDelight 数据库集成
- ✅ Repository 数据访问层

### 2. 界面与导航
- ✅ 底部导航栏（训练、分组、打卡、统计4个Tab）
- ✅ 分组列表页面
- ✅ 分组详情页面
- ✅ 模拟数据展示

## 待实现功能

### 1. 分组管理
- 🔄 **实现分组的创建、编辑、删除功能**
- ⏸️ 实现分组内动作的排序

### 2. 动作管理
- ⏸️ 实现动作列表页面和动作编辑页面
- ⏸️ 实现动作的添加、编辑、排序、删除功能

### 3. 训练功能
- ⏸️ 实现训练执行页面（计时、休息提示）
- ⏸️ 实现训练的开始/暂停、跳过、重复、完成功能

### 4. 打卡与统计
- ⏸️ 实现打卡记录列表页面
- ⏸️ 实现训练完成后的打卡记录功能
- ⏸️ 实现统计页面布局
- ⏸️ 实现日/周/月统计、连续打卡天数等数据展示

## 开发计划

### 当前任务
1. **实现分组的创建功能** - 包含对话框和数据库操作
2. **实现分组的编辑功能** - 支持修改分组名称
3. **实现分组的删除功能** - 包含确认对话框

### 后续任务
- 实现动作管理功能
- 实现训练执行功能
- 实现打卡记录和统计功能

## 构建状态
✅ **项目可成功编译**
- Android: 可生成 APK 文件
- 构建命令: `./gradlew androidApp:assembleDebug`

## 运行方式
```bash
# 构建并安装到 Android 设备
./gradlew androidApp:installDebug

# 仅构建 APK 文件
./gradlew androidApp:assembleDebug
```

## 注意事项
- 项目采用 Kotlin Multiplatform 架构，支持 Android 和 iOS
- 使用 Compose Multiplatform 构建跨平台 UI
- 数据默认本地存储，后续将支持云同步

---

**更新时间**: 2025-12-05
**版本**: v0.1 MVP