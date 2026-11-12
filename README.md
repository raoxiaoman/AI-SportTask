# AI SportTask 🏋️

[![Android 自动打包](https://github.com/raoxiaoman/AI-SportTask/actions/workflows/android-build.yml/badge.svg)](https://github.com/raoxiaoman/AI-SportTask/actions/workflows/android-build.yml)
[![Release v1.0.2](https://img.shields.io/badge/Release-v1.0.2-blue)](https://github.com/raoxiaoman/AI-SportTask/releases/tag/v1.0.2)

个人运动任务管理与打卡应用，基于 Kotlin Multiplatform + Compose Multiplatform，支持 Android 与 iOS，可选自托管云同步。

> **最新版本 [v1.0.2](https://github.com/raoxiaoman/AI-SportTask/releases/tag/v1.0.2)**（2026-11-12）
> · [下载 APK](https://github.com/raoxiaoman/AI-SportTask/releases/download/v1.0.2/AI-SportTask-v1.0.2-release.apk)
> · [全部 Release](https://github.com/raoxiaoman/AI-SportTask/releases)
>
> 云同步服务端尚未部署，首次启动可在登录页选择「先不登录 · 离线使用」，本地功能不受影响。

## 功能

- **训练管理**：分组与动作的增删改、拖拽排序、默认时长与休息时长
- **训练执行**：大字体倒计时与进度环，开始 / 暂停 / 跳过 / 重来，自动进入休息
- **计时反馈**：木质秒针声、最后 5 秒中文倒数，开始 / 休息 / 切换 / 完成语音播报；声音、语音、秒针、震动可分别开关
- **动作图解**：内置康复动作的分步指导、要点与注意信号，配自绘示意图与视频示范检索
- **打卡与统计**：每日打卡、按日 / 周 / 月统计、连续天数
- **成就系统**：首次训练、连续打卡、累计时长等条件成就
- **其他**：深色模式、训练提醒、数据导入导出（JSON）

## 下载安装

到 [Releases](https://github.com/raoxiaoman/AI-SportTask/releases) 下载最新的 `AI-SportTask-vX.Y.Z-release.apk`：

```bash
adb install -r AI-SportTask-v1.0.2-release.apk
```

> 若之前装的是自行编译的 Debug 包，需先卸载再装（签名不同）；正式版本之间可直接覆盖升级。

## 本地运行

```bash
git clone https://github.com/raoxiaoman/AI-SportTask.git
cd AI-SportTask

# Android
./gradlew :androidApp:installDebug

# iOS：用 Xcode 打开 iosApp/（尚未完成编译验证）
```

云同步为可选能力。服务端随仓库提供（`backend/`，Node.js + Express + SQLite），启动后可在 App 登录页「服务器设置」填写地址；未启动时用离线模式即可。

## 构建与发布

- 推送到 `main`：自动跑单测并产出 Debug APK
- 推送 `v*` 标签：自动产出**签名 Release APK** 并创建 GitHub Release

```bash
git tag -a v1.0.3 -m "AI SportTask v1.0.3"
git push origin v1.0.3
```

版本号由标签决定，安装包文件名、Release 标题与 App 内「关于」保持一致。Release 签名密钥保存在仓库 Secrets 中，不进入代码库。

## 文档

- [界面与设计规范](docs/UI设计规范.md)
- [训练音频素材与授权](docs/训练音频素材.md)
- [iOS 部署清单](docs/iOS-Deployment-Checklist.md)

## 版本记录

- **v1.0.2** — 计时音频重制（内置离线音频，不再依赖系统 TTS）、补齐休息与切换播报、新增过半提醒与最后 3 秒强调、修复准备页底部按钮显示不全
- **v1.0.1** — 修复正式包在服务端未部署时无法进入主界面的问题，新增离线使用入口
- **v1.0.0** — 首个正式版本：动作图解、UI 设计规范落地、自动打包发版

后续计划：自动同步（离线队列补发）、iOS 编译验证、动作模板库。

## 许可证

[MIT License](LICENSE.txt)
