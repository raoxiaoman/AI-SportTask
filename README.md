# AI SportTask 🏋️

[![Android 自动打包](https://github.com/raoxiaoman/AI-SportTask/actions/workflows/android-build.yml/badge.svg)](https://github.com/raoxiaoman/AI-SportTask/actions/workflows/android-build.yml)
[![Release v1.1.1](https://img.shields.io/badge/Release-v1.1.1-blue)](https://github.com/raoxiaoman/AI-SportTask/releases/tag/v1.1.1)

个人运动任务管理与打卡应用，基于 Kotlin Multiplatform + Compose Multiplatform，支持 Android 与 iOS，可选自托管云同步。

> **最新版本 [v1.1.1](https://github.com/raoxiaoman/AI-SportTask/releases/tag/v1.1.1)**（2026-11-26）
> · [下载 APK](https://github.com/raoxiaoman/AI-SportTask/releases/download/v1.1.1/AI-SportTask-v1.1.1-release.apk)
> · [全部 Release](https://github.com/raoxiaoman/AI-SportTask/releases)
>
> 云同步服务端可自托管（`backend/`，已提供 systemd + Caddy 部署手册）；未配置服务器时，登录页可选「先不登录 · 离线使用」，本地功能不受影响。

## 功能

- **训练管理**：分组与动作的增删改、拖拽排序、默认时长与休息时长
- **训练执行**：大字体倒计时与进度环，开始 / 暂停 / 跳过 / 重来，自动进入休息
- **计时反馈**：木质秒针声、最后 5 秒中文倒数，开始 / 休息 / 切换 / 完成语音播报；声音、语音、秒针、震动可分别开关
- **动作图解**：全部动作的分步指导、要点与注意信号，配自绘示意图与视频示范检索；图解由服务端分发、内置兜底，修正图解无需发版
- **打卡与统计**：每日打卡、按日 / 周 / 月统计、连续天数
- **成就系统**：首次训练、连续打卡、累计时长等条件成就
- **方案模板库**：内置 / 服务端三套方案（8 周康复、8 周办公室、久坐三关节恢复），一键导入为方案；导入后自动切换为当前方案并弹出成功提示，在「方案」页以单选方式点选切换当前方案，首页今日卡片与分组均跟随当前方案
- **其他**：深色模式、训练提醒、数据导入导出（JSON）

## 下载安装

到 [Releases](https://github.com/raoxiaoman/AI-SportTask/releases) 下载最新的 `AI-SportTask-vX.Y.Z-release.apk`：

```bash
adb install -r AI-SportTask-v1.1.1-release.apk
```

> 若之前装的是自行编译的 Debug 包，需先卸载再装（签名不同）；正式版本之间可直接覆盖升级。

### 桌面版（macOS / Windows / Linux）

除手机端外，还提供基于 Compose Multiplatform 的桌面版，UI 与功能与手机端一致（共用 `commonMain`）。
到 [Releases](https://github.com/raoxiaoman/AI-SportTask/releases) 下载对应平台的安装包：

- macOS：`SportTask-Desktop-X.Y.Z.dmg`
- Windows：`SportTask-Desktop-X.Y.Z.msi`
- Linux：`SportTask-Desktop-X.Y.Z.deb`

桌面版说明：

- **音效与语音**：倒计时秒针与提示音正常播放；macOS 通过系统 `say`（Tingting 中文语音）实时合成中文播报，Windows / Linux 无系统 TTS 时自动降级为只播音效。设置页的「语音播报」开关在 macOS 上同样生效。
- **音频调度**：秒针走独立通道，不与语音共用清场；含长钟声的阶段事件把语音延后到钟声播完，播报期间秒针自动让路，避免「时间过半」等中间提醒与钟声叠响、或被下一秒秒针拦腰截断。
- **本地数据**：登录会话、服务器地址、主题等存在 `~/.sporttask/`，训练数据在同目录的 SQLite 文件中；ESC 键等效于手机端返回键。
- **各平台安装包需在对应系统构建**：macOS 出 `.dmg`、Windows 出 `.msi`、Linux 出 `.deb`，CI 可按平台分别产出。

## 本地运行

```bash
git clone https://github.com/raoxiaoman/AI-SportTask.git
cd AI-SportTask

# Android
./gradlew :androidApp:installDebug

# iOS：用 Xcode 打开 iosApp/iosApp.xcodeproj 后运行，
#      命令行构建参数见 docs/iOS-Deployment-Checklist.md

# 桌面版（当前系统）
./gradlew :desktopApp:run

# 桌面版安装包（各平台需在对应系统构建）
./gradlew :desktopApp:packageDmg   # macOS
./gradlew :desktopApp:packageMsi   # Windows
./gradlew :desktopApp:packageDeb   # Linux
```

云同步为可选能力。服务端随仓库提供（`backend/`，Node.js + Express + SQLite），部署步骤见 [backend/DEPLOY.md](backend/DEPLOY.md)；App 登录页「服务器设置」填写对外地址即可，未部署时用离线模式。方案模板库通过服务端 `GET /api/templates` 分发，客户端自带同名模板作为离线兜底。

## 构建与发布

- 推送到 `main`：自动跑单测并产出 Debug APK
- 推送 `v*` 标签：自动产出**签名 Release APK** 并创建 GitHub Release

```bash
git tag -a v1.1.1 -m "AI SportTask v1.1.1"
git push origin v1.1.1
```

版本号由标签决定，安装包文件名、Release 标题与 App 内「关于」保持一致。Release 签名密钥保存在仓库 Secrets 中，不进入代码库。

## 文档

- [界面与设计规范](docs/UI设计规范.md)
- [训练音频素材与授权](docs/训练音频素材.md)
- [iOS 部署清单](docs/iOS-Deployment-Checklist.md)
- [桌面版构建说明](docs/桌面版构建说明.md)

## 版本记录

- **v1.1.1** — 修复桌面端语音播报与倒计时钟声/秒针叠响、播报被拦腰截断：秒针改用独立调度通道不再掐断中文语音，阶段事件语音延后到 1s 长钟声播完，播报期间秒针自动让路；新增桌面音频调度策略与 9 项回归测试；修正桌面版语音说明
- **v1.1.0** — 训练完成页重新设计（徽章/统计卡片/按钮对齐）；训练中查看图解改为页内嵌面板，顶部常驻倒计时与播放控制；修复成就提示每次训练都重复弹出；校正三套方案 20 处动作分配时间；离线兜底图解同步「坐姿髋外展」「踝外翻自我抗阻」力学修正
- **v1.0.9** — 图解详情的暂停/继续按钮缩小为轻量胶囊；修正「坐姿髋外展」步骤文案（服务端图解已同步生效）
- **v1.0.8** — 修复训练完成页「保存打卡」按钮图标颜色；动作图解详情页可直接暂停/继续计时；休息页卡片与入口改为下一动作；桌面端恢复中文语音播报（macOS 系统 TTS）
- **v1.0.7** — 云同步改为双向并支持登录自动同步：登录后自动拉取云端数据（以云端为准落地分组/动作/打卡与方案），手动同步改为先推后拉保证本地新增不丢；新增桌面版（macOS/Windows/Linux）
- **v1.0.6** — 动作图解改为服务端分发（`/api/guides`），内置库兜底且启动热更新：修正示意图与要点文案无需再发版；为办公室/关节方案补齐专属图解，修正示意图与动作不符
- **v1.0.5** — 「方案」页升级为两级结构并改用单选切换；今日卡片、本周节奏与通知均跟随当前方案；新增第三套「久坐三关节恢复」模板；重复导入自动替换不留重复分组；全页面 UI/交互审查修复；消除切换方案时空态闪现
- **v1.0.4** — 新增方案模板库：康复与办公室两套方案支持服务端分发与离线内置，一键导入为分组（重名自动加后缀）
- **v1.0.3** — 补齐 iOS 训练音频、触感反馈与启动崩溃修复；修复云同步“只拉不推”和重启后服务器地址丢失；服务端支持 systemd + Caddy 自托管部署
- **v1.0.2** — 计时音频重制（内置离线音频，不再依赖系统 TTS）、补齐休息与切换播报、新增过半提醒与最后 3 秒强调、修复准备页底部按钮显示不全
- **v1.0.1** — 修复正式包在服务端未部署时无法进入主界面的问题，新增离线使用入口
- **v1.0.0** — 首个正式版本：动作图解、UI 设计规范落地、自动打包发版

后续计划：自动同步（离线队列补发）、iOS 真机回归、云端模板热更新。

## 许可证

[MIT License](LICENSE.txt)
