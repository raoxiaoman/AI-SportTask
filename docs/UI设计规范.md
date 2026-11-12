# AI SportTask UI 设计规范

> 版本：v1.0 · 2026-11-12
> 适用范围：`shared/src/commonMain/kotlin` 下所有页面、组件、弹窗；Android / iOS 共用同一套语义 token。
> 本文是**强制约束**。新增页面或组件前先读「禁止清单」和「交付检查表」。

---

## 1. 设计原则

1. **中性底 + 少量品牌蓝**：页面大面积使用 `background / surface / surfaceAlt`，品牌蓝只用于选中态、进度、主操作，蓝色覆盖面积控制在 10% 以内。
2. **语义色优先**：颜色只按「用途」取（`inkSub`、`danger`、`warningSoft`），不按「看起来像什么」取（`Color.Gray`）。
3. **4pt 栅格**：所有 padding / Spacer / spacedBy 只能取 `s1…s16` 或具名语义尺寸，不允许出现 `13.dp` 这类散值。
4. **唯一来源**：颜色、字号、间距、圆角分别只在一个文件定义，页面层零硬编码。
5. **深浅主题等价**：任何新增颜色必须同时在 `LightAppColors` / `DarkAppColors` 中给出可用值，并满足正文 4.5:1 对比度。

---

## 2. 文件与职责（唯一来源）

| 关注点 | 文件 | 页面如何取用 |
| --- | --- | --- |
| 语义色板 | `ui/theme/AppColors.kt` | `AppTheme.colors.xxx` |
| 字阶 / 字重 | `ui/theme/AppTypography.kt` | `MaterialTheme.typography.xxx`、`AppTypeScale.*`、`AppTypeWeight.*` |
| 间距 / 尺寸 | `ui/theme/AppDimens.kt` | `AppDimens.sN` 或具名尺寸 |
| 圆角 | `ui/theme/AppShapes.kt` | `AppShapes.card / button / pill / chip / dialog / sheet` |
| 主题装配 | `ui/theme/AppTheme.kt` | `SportTaskTheme { … }` |
| 图标 | `ui/AppIcons.kt` | `AppIcons.Xxx` + `AppIcon(...)` |
| 通用组件 | `ui/CommonComponents.kt` | `PageHeader`、`SectionHeader`、`LiteCard`、`IconBadge`、`EmptyState` |
| 弹窗 / 输入框 | `ui/AppDialog.kt` | `AppDialog`、`AppConfirmDialog`、`AppTextField`、`AppDialogTextField` |
| 系统栏 | `ui/SystemBars.kt`、`ui/DialogSystemBars.kt` | `SystemBarAppearanceEffect`、`DialogSystemBarAppearanceEffect` |

---

## 3. 色彩

### 3.1 语义色表（浅色主题取值）

| Token | 值 | 用途 |
| --- | --- | --- |
| `brand` | `#4F6FE8` | 图标、次级强调、箭头 |
| `brandStrong` | `#3B57CC` | 主按钮 / 选中态（白字 6.1:1） |
| `brandDeep` | `#2B3F86` | 渐变 hero 卡、深色主卡（白字 9.8:1） |
| `brandSoft` | `#EEF2FF` | 选中态 chip 背景、成就行背景 |
| `onBrand` | `#FFFFFF` | 品牌色块上的文字/图标 |
| `success` / `successSoft` | `#1D9E75` / `#E1F5EE` | 完成、达标、正向提示 |
| `warning` / `warningSoft` | `#BA7517` / `#FAEEDA` | 注意信号卡片 |
| `danger` / `dangerSoft` | `#C43C3C` / `#FCEBEB` | 删除、不可逆操作、错误态 |
| `background` | `#F5F7FB` | 页面底色 |
| `surface` | `#FFFFFF` | 卡片、弹窗、底栏 |
| `surfaceAlt` | `#EEF2F7` | 输入框填充、胶囊、次要块 |
| `outline` | `#E6EBF3` | 分隔线、输入框边框 |
| `ink` / `inkSub` / `inkMuted` | `#16203A` / `#5B667A` / `#8A94A6` | 主文字 / 副文字 / 提示文字 |

深色主题同名 token 由 `DarkAppColors` 覆盖，页面代码不需要分支判断。

### 3.2 使用规则

- 文字：`ink`（标题）、`inkSub`（正文/说明）、`inkMuted`（占位/辅助）。**禁止** `Color.Gray`、`MaterialTheme.colors.onSurface.copy(alpha = …)`。
- 危险操作：用 `AppTheme.colors.danger` 作**按钮底色**（配 `onBrand` 文字），不要退化成「红色纯文字按钮」。
- 承载白色文字的色块只用 `brandStrong / brandDeep / danger`，不要用较亮的 `brand`。
- 禁止 `Color(0xFF…)` 出现在 `ui/theme/` 以外的任何文件。

---

## 4. 字体与字阶

字阶共 5 级，另有 3 个中间档，统一由 `AppTypeScale` 定义；页面只用 `MaterialTheme.typography.*`。

| 语义 | Material 槽位 | 字号 / 字重 | 用途 |
| --- | --- | --- | --- |
| 大标题 | `h1` / `h2` | 30 / 24 · Medium | 页面主标题、时间数字 |
| 区块标题 | `h3` / `h4` | 20 / 18 · Medium | 卡片标题 |
| 弹窗标题 | `h6` | 15 · Medium（配 `AppTypeWeight.emphasis`） | 弹窗标题 |
| 强调正文 | `subtitle1` | 15 · Medium | 主按钮文字、行标题 |
| 次要标签 | `subtitle2` | 13 · Medium | 弹窗按钮、分组标签 |
| 正文 | `body1` | 15 · Regular | 说明段落 |
| 辅助正文 | `body2` | 13 · Regular | 卡片副标题、输入框内容 |
| 提示 | `caption` | 11 · Regular | 时间戳、脚注 |

字重只用 `AppTypeWeight.regular / medium / emphasis`（400 / 500 / 700）。
**禁止** `fontSize = 16.sp`、`fontWeight = FontWeight.W600`、`fontFamily = …` 等分散写法。

---

## 5. 间距与尺寸

### 5.1 4pt 栅格

| Token | 值 | 典型用途 |
| --- | --- | --- |
| `s1` | 4 | 图标与文字微间距 |
| `s2` | 8 | 行内元素、chip 间距 |
| `s3` | 12 | 卡片内紧凑 padding |
| `s4` | 16 | 屏幕边距、卡片内边距、区块间距 |
| `s6` | 24 | 分区间距、弹窗内边距、屏幕留白 |
| `s8` / `s10` / `s12` / `s16` | 32 / 40 / 48 / 64 | 大留白、列表收尾 |

`screenPadding = s4`，`cardPadding = s4`，`sectionGap = s6`。所有卡片必须带 `padding(horizontal = AppDimens.screenPadding)`，禁止贴边。

### 5.2 具名语义尺寸

`buttonHeight 52`、`dialogButtonHeight 40`、`dialogMaxWidth 360`、`dialogTextAreaHeight 180`、`formTextAreaHeight 120`、`iconSmall 18`、`icon 24`、`iconLarge 48`、`iconHero 64`、`touchTarget 48`、`progressRing 200`、`chartHeight 120`、`diagramSize 224`、`navHeight 64`。

> **禁止语义错用**：`progressRing` 只能用于圆环，`chartHeight` 只能用于图表。历史代码曾用它们当文本框高度，已修正为 `dialogTextAreaHeight` / `formTextAreaHeight`。

---

## 6. 圆角

| Token | 值 | 用途 |
| --- | --- | --- |
| `card` | 16 | 常规卡片 |
| `cardLarge` | 18 | hero 卡、大卡 |
| `button` | 12 | 按钮、输入框 |
| `chip` | 8 | 标签、搜索框 |
| `pill` | 50% | 胶囊 chip |
| `dialog` | 20 | 弹窗 |
| `sheet` | 顶部 16 | 底部面板 |

---

## 7. 图标

- 全部使用 `ui/AppIcons.kt` 中的自绘线性图标（`materialIcon` / `strokeIcon`），24dp 网格、2dp 描边、圆角端点。
- 渲染统一通过 `AppIcon(imageVector, contentDescription, tint, size)`；可点击图标必须给 `content-desc`（用于 UI 自动走查）。
- **禁止**直接依赖 `material-icons-extended` 的默认图标：语义不一致且会造成整套图标风格混杂。
- 新增图标必须补进 `AppIcons`，不允许在页面里内联 `Path` 数据。

---

## 8. 组件规范

### 8.1 页面结构

- 每个主 tab 用 `PageHeader`（标题坐标实测统一为 `left=42 top=95`）+ `LazyColumn`，**不使用** `TopAppBar` / `FloatingActionButton`。
- 区块标题用 `SectionHeader`（中性色、非蓝色强调）。
- 卡片用 `LiteCard`，卡片之间用 `AppDimens.hairline` 分隔线；列表末尾留 `s8`。
- 底部导航在训练执行页隐藏（沉浸式），物理返回由 `PlatformBackHandler` 拦截，不能退出 App。

### 8.2 按钮

| 类型 | 组件 | 规范 |
| --- | --- | --- |
| 主操作 | `Button` | `height(buttonHeight)`、`AppShapes.button`、`brandStrong` 底 + `onBrand` 字 |
| 次操作 | `OutlinedButton` | 同上尺寸，`BorderStroke(divider, brand)`，`brand` 字 |
| 文字操作 | `TextButton` | `inkSub` 或 `inkMuted`，`AppShapes.button` |
| 危险主操作 | `Button` | `danger` 底 + `onBrand` 字 |

按钮文字用 `subtitle1`（页面）或 `subtitle2`（弹窗）。

### 8.3 输入框

统一使用 `AppTextField`（页面表单）或 `AppDialogTextField`（弹窗表单语义别名）：
`surfaceAlt` 填充 + `outline` 细边 + `AppShapes.button` 圆角，聚焦边框 `brand`，错误态边框/标签 `danger`，文字 `ink`。

**禁止**直接调用 `OutlinedTextField` / `TextField` 原始样式（唯一例外：`AppDialog.kt` 内部实现）。

### 8.4 弹窗（本次重点整改）

统一容器 `AppDialog`，便捷封装 `AppConfirmDialog`。

| 项目 | 规范 |
| --- | --- |
| 容器 | `surface` 底、`AppShapes.dialog`(20dp)、左右屏幕留白 `s6`、最大宽 360dp、内边距 `s6` |
| 标题 | `h6` + `AppTypeWeight.emphasis` + `ink`；可选 `subtitle`（`caption` / `inkMuted`） |
| 正文 | `body1` / `inkSub`；标题与正文间距 `s4` |
| 按钮行 | 右对齐；辅助操作在最左，其次取消（文字按钮）、主操作（实心 40dp 高） |
| 危险操作 | `confirmDanger = true` → 主按钮 `danger` 底；**不允许**红色纯文字 |
| 加载态 | `busy = true` → 主按钮内显示进度圈并禁用所有按钮 |
| 表单控件 | 只用 `AppDialogTextField` |
| 重复结构 | 弹窗标题/按钮只由 `AppDialog` 提供一次，内容组件不得再自带标题或「确定」按钮 |

**全站禁止直接使用 `AlertDialog`**。历史问题：小圆角、默认字号、纯文字按钮、直角输入框、标题与时间箭头重叠、筛选弹窗出现两个标题两组按钮。

### 8.5 全屏详情页

`ActionGuideDialog`（动作图解）使用 `Dialog(usePlatformDefaultWidth = false)` + `fillMaxSize()`，
内容区为：参数 chip → 示意图卡 → 动作步骤 → 质量要点 → 注意信号 → 视频入口。
所有区块间距取 `s4`，示意图边长 `diagramSize`，底部视频入口为 `OutlinedButton`。

---

## 9. 系统栏与「沉浸式」策略

### 9.1 为什么不是全量 edge-to-edge

应用内部存在嵌套 `Scaffold`，若全局启用 `WindowCompat.setDecorFitsSystemWindows(window, false)`，
各层会重复叠加状态栏 insets，导致顶部间距不一致（实测 5 个 tab 页头错位）。
因此采用**同色系统栏**方案，视觉上与 edge-to-edge 等价、布局风险为零：

```
SystemBarAppearanceEffect(
    isDark = isDarkMode,
    statusBarColor = AppTheme.colors.background,
    navigationBarColor = AppTheme.colors.surface
)
```

状态栏与页面底色同色、导航栏与卡片/底栏同色，图标深浅随 `isDark` 切换（`WindowInsetsControllerCompat`）。
深色主题走 `DarkAppColors`，同一套代码自动生效。

### 9.2 Dialog 独立 Window

Compose `Dialog` 在 Android 上会创建**独立于 Activity 的 Window**，`SystemBarAppearanceEffect` 作用不到它，
因此全屏详情页曾出现「深灰状态栏 + 深灰导航栏」的割裂感。

凡是**全屏 Dialog 页面**，必须在 Dialog 内容内调用：

```
DialogSystemBarAppearanceEffect(
    isDark = !AppTheme.colors.isLight,
    statusBarColor = AppTheme.colors.background,
    navigationBarColor = AppTheme.colors.surface
)
```

Android 实现通过 `DialogWindowProvider` 向上找到 Dialog Window，完成三件事：

1. `clearFlags(FLAG_DIM_BEHIND)` + `setDimAmount(0f)`：全屏页面不需要背后遮罩，
   而 `FLAG_DIM_BEHIND` 会把**系统栏一起压暗**，这才是「深灰状态栏/导航栏」的真正根因，
   仅设置 `statusBarColor` 无效。
2. 同步 `statusBarColor = background`、`navigationBarColor = surface`。
3. 用 `WindowInsetsControllerCompat` 按 `isDark` 设置状态栏/导航栏图标深浅。

组合初期 DecorView 可能尚未 attach，实现里带最多 5 次 `view.post` 重试。
iOS 的 Dialog 不创建独立 Window，为空实现。

普通居中小弹窗**保留**系统标准 scrim 变暗行为（模态遮罩需要压暗背景），不额外干预；
`AppDialog` 因此不调用本 effect，只有全屏 Dialog 页面调用。

---

## 10. 页面合规状态（2026-11-12 走查）

| 页面 / 组件 | 状态 | 说明 |
| --- | --- | --- |
| 训练首页、统计、设置 | 已合规 | 按设计稿重构，页头坐标统一 |
| 分组列表 / 详情 / 动作编辑 | 已合规 | 动作编辑表单已换 `AppTextField` |
| 打卡列表 / 搜索 / 筛选 | 已合规 | 筛选弹窗去掉重复标题与按钮 |
| 训练执行页 | 已合规 | 沉浸式、要点卡、休息提示、按钮间距 |
| 动作图解详情 | 已合规 | 本次修复 Dialog 系统栏 |
| 登录 / 注册 | 已合规 | 输入框、按钮、错误文字改用语义色 |
| 全部弹窗（设置 7、分组 3、打卡 4、成就 1） | 已合规 | 全部改用 `AppDialog` / `AppConfirmDialog` |

`pages/LoginPage.kt` 为历史遗留、当前未被引用，不在本次整改范围，后续若启用需按本文重写。

---

## 11. 禁止清单（Code Review 直接照此卡）

- ❌ 页面层出现 `Color(0xFF…)`、`Color.Gray`、`.copy(alpha = …)` 造色
- ❌ `fontSize = …sp`、`fontWeight = FontWeight.…`、`fontFamily = …`
- ❌ `padding(13.dp)`、`Spacer(Modifier.height(20.dp))` 等非栅格散值
- ❌ 语义错用尺寸 token（如用 `progressRing` 当输入框高度）
- ❌ 直接使用 `AlertDialog`、`TextField`、`OutlinedTextField`
- ❌ 混用 `MaterialTheme.colors.*` 与 `AppTheme.colors.*`（前者仅用于向 Material 组件传参的兼容场景）
- ❌ 弹窗里自带第二层标题 / 第二组确认按钮
- ❌ 危险操作用红色纯文字按钮
- ❌ 卡片缺少 `screenPadding` 导致贴边
- ❌ 图标内联 `Path` 数据或缺少 `content-desc`
- ❌ 新增全屏 Dialog 不调用 `DialogSystemBarAppearanceEffect`

---

## 12. 新增页面 / 组件交付检查表

1. 颜色、字号、间距、圆角是否全部来自 token？
2. 卡片是否有屏幕边距、页头是否用 `PageHeader`？
3. 深浅两套主题下文字对比度是否 ≥ 4.5:1？
4. 弹窗是否用 `AppDialog`，危险操作是否 `confirmDanger`？
5. 表单是否用 `AppTextField`，错误态是否用 `danger`？
6. 图标是否来自 `AppIcons` 且可点击项有 `content-desc`？
7. 全屏 Dialog 是否处理了独立 Window 的系统栏？
8. 物理返回是否被正确拦截？
9. `./gradlew :shared:testDebugUnitTest :androidApp:assembleDebug` 通过？
10. 虚拟机截图走查：顶部间距、卡片边距、系统栏颜色、文字截断、按钮间距是否正常？

---

## 13. 验证手段

- 单测：`:shared:testDebugUnitTest`（当前 303 项，全通过）。
- 布局坐标：`adb -s emulator-5554 exec-out uiautomator dump /sdcard/ui.xml`，解析 `bounds` 对比页头/卡片坐标。
- 视觉：`adb -s emulator-5554 exec-out screencap -p` 截图逐屏走查；真机 `dcb4f7ed` 最终安装确认。
- 构建产物：`/tmp/sporttask-fresh-build2/androidApp/outputs/apk/debug/androidApp-debug.apk`。
