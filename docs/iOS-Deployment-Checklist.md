# iOS 部署检查清单

## 前置条件

需要在 **macOS 机器** 上操作，以下软件必须安装：

- [ ] Xcode 15+
- [ ] CocoaPods 或 SPM（KMP 项目可二选一）
- [ ] Apple Developer Program 账号（$99/年，TestFlight 和上架需要）

## 第一步：Xcode 项目配置

### 1.1 Team & Signing
在 Xcode 中打开 `iosApp/iosApp.xcodeproj`：

1. **Signing & Capabilities** → Team 选择你的 Apple Developer Team
2. 这会自动填充 `Config.xcconfig` 中的 `TEAM_ID`
   > 如果项目不自动读取配置，手动把 Team ID 填入 `iosApp/Configuration/Config.xcconfig` 的 `TEAM_ID` 字段
3. Bundle Identifier 已设置为 `com.raohui.sporttask`

### 1.2 项目设置确认

| 项目 | 值 | 检查 |
|------|------|:----:|
| Bundle Identifier | `com.raohui.sporttask` | ✅ |
| App Name | `AI SportTask` | ✅ |
| Deployment Target | iOS 16.0（Xcode 27 最低支持 15.0） | ✅ |
| Team ID | 你的 Apple Developer Team ID | ⬜ |
| Supported Orientations | Portrait + Landscape | ✅ |

## 第二步：KMP 构建

### 2.1 构建共享 Framework

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --no-daemon
```

> 加 `--no-daemon`：常驻 Gradle daemon 会继承启动它的 shell 的文件系统限制，表现为写 `.klib` 报
> `Operation not permitted`（代码本身没错）。

产物：`shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework`

### 2.2 命令行构建 App（不开 Xcode 也能编）

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /tmp/iosApp-dd \
  ARCHS=arm64 ONLY_ACTIVE_ARCH=YES \
  ENABLE_USER_SCRIPT_SANDBOXING=NO \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build
```

| 参数 | 作用 |
|------|------|
| `ENABLE_USER_SCRIPT_SANDBOXING=NO` | 否则 Xcode 沙箱化 Run Script，Gradle 无法读写 build 目录 |
| `ARCHS=arm64` | Apple Silicon 上避免同时编 x86_64 模拟器切片 |
| `CODE_SIGNING_ALLOWED=NO` | 模拟器不需要签名，避免空 `TEAM_ID` 报错 |

产物在 `-derivedDataPath` 下的 `Build/Products/Debug-iphonesimulator/AI SportTask.app`。

### 2.3 已知的 iOS 平台问题

| 问题 | 状态 |
|------|:----:|
| `Repository` 中的 `java.time.LocalDate` | ✅ 已改为跨平台 `todayDateString()`（commonMain 不能引 JVM API） |
| SQLDelight iOS 驱动 | ✅ 已加 `native-driver` 依赖 + Xcode 侧 `-lsqlite3` 链接 |
| `daysAgoDateString()` 的 `NSCalendarUnitDay` | ✅ 已补导入，`setValue` 传 `Long` |
| Deployment Target | ✅ 已设 iOS 16.0（Xcode 27 最低支持 15.0） |
| iOS 训练音频（`TrainingAudio.kt`） | ⬜ 存根，返回空；资源名已与 Android `res/raw/*.mp3` 对齐，接入时按 `TrainingAudioEvent` 用 `NSBundle` + `AVAudioPlayer` 展开 |
| iOS 震动反馈 | ⬜ 存根，返回空（不影响运行） |
| 深色模式适配 | ⚠️ Compose Multiplatform 自动适配，需验证 |

## 第三步：模拟器测试

1. Xcode 中选择 iOS Simulator（推荐 iPhone 15 Pro）
2. `Cmd + R` 构建并运行
3. 走一遍完整流程：**分组 → 动作 → 训练 → 打卡 → 统计**
4. 特别注意：
   - 训练计时器是否正常
   - 数据持久化是否正常（退出重进）
   - 深色模式切换

## 第四步：TestFlight 发布

### 4.1 Archive

```bash
# 先构建 Release Framework
./gradlew :shared:linkReleaseFrameworkIosArm64

# 然后在 Xcode 中：
# Product → Archive
```

### 4.2 上传 TestFlight

1. Xcode Organizer → Distribute App → TestFlight
2. 等待处理完成（通常 5-30 分钟）
3. 在 App Store Connect 添加测试员

### 4.3 已知限制

- 本地通知在 iOS 上需要额外的 entitlement 配置
- 如果通知不工作，不影响核心功能

## 第五步：App Store 上架

1. App Store Connect 完善应用信息
2. 截图准备（iPhone 6.7" / 6.5" 各一组）
3. 隐私政策（可选但推荐）
4. 提交审核

---

## 常见问题

### Q: KMP framework 编译失败
```bash
# 清理后重试
./gradlew clean :shared:linkDebugFrameworkIosSimulatorArm64
```

### Q: Xcode 找不到 framework
- 确保先执行了 `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
- 检查 Xcode 的 Framework Search Paths 设置

### Q: `daysAgoDateString` 编译错误
已修复（iOS 之前缺了这个 `actual` 实现），拉取最新代码即可。

### Q: 音效/震动不工作
iOS 音效和震动接口目前是空实现（stub），不影响应用运行和上架。后续迭代可接入 `AVFoundation` / `AudioToolbox`。
