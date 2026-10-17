# 可运行样张（showcase）

`references/showcase/` 下的 11 个 `.kt` 文件不是「代码片段」，而是**一个能编译的真实页面集**：
它们针对 **miuix 0.9.4** 编写，整体编译通过，并且 `miuix-lint` 跑出 0 error / 0 warning。

> 为什么要有它：片段能保证「符号存在」，但保证不了「能编译」。样张把「能编译」也钉住了——
> 结构、间距、颜色、Modifier 顺序都以这份源码为准；文档只解释为什么，不重复写代码。

---

## 1. 文件

| 文件 | 内容 | 对应文档 |
| :-- | :-- | :-- |
| `Frame.kt` | `ShowcaseTheme`（强制 Light/Dark）、`PhoneScreen`、仿真状态栏、`ScreenTopBar`、`IconBadge`、`AppIconTile` | `layout.md` §3-4 |
| `StatusPalette.kt` | 三态 `StatusTone` + `StatusCard` + `StatCard` + `isMiuixDark()` | `status-colors.md`、`scenario-home.md` §2 |
| `StatusIcons.kt` | 圆环 + 勾 / 感叹号的 `ImageVector`（移植自 lyricon 的矢量） | `status-colors.md` §2 |
| `ScreenHome.kt` | 首页：状态卡 → 两列数据卡 → 快捷入口 → 最近使用 → 常用设置 → 底部导航 | `scenario-home.md`、`scenario-shell.md` |
| `ScreenSettings.kt` | 设置页：4 个分组、Switch/Arrow/Dropdown/Slider、可选 `OverlayDialog` | `preferences.md`、`scenario-shell.md` |
| `ScreenList.kt` | 应用列表：搜索框 → `TabRow` 分段 → 分组卡片 → 自启动开关 | `scenario-list.md` |
| `ScreenInfo.kt` | 关于页：Hero → 只读信息卡 → 支持卡 → 页脚 | `scenario-info.md` |
| `ScreenForm.kt` | 表单：`Close`/`Ok` 顶栏 → `TextField` → 可见错误 → 单选/复选 → 主按钮 | `scenario-form.md` |
| `ScreenState.kt` | 空态：居中插画 + 文案 + 主操作 | `scenario-state.md` |
| `ScreenOnboarding.kt` | 引导页：插画 → 能力列表 → 页点 → 主/次按钮 | `scenario-onboarding.md` |
| `ScreenStatusSpec.kt` | 状态卡三态规格页（同一张卡换 tone 的对照） | `status-colors.md` |

## 2. 依赖（编译它需要什么）

```kotlin
// Kotlin JVM / Compose Multiplatform
implementation(compose.desktop.currentOs)
implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.4")
implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.4")
implementation("top.yukonga.miuix.kmp:miuix-squircle:0.9.4")
```

`MiuixIcons.*` 依赖 `miuix-icons`，`squircleSurface` 依赖 `miuix-squircle`，漏了就是 `Unresolved reference`。

## 3. 这份样张保证了什么 / 没保证什么

**保证了**
- 11 个文件全部对 **0.9.4** 编译通过（Kotlin 2.4.21 / Compose Multiplatform 1.12.1 / Desktop target）
- `node scripts/miuix-lint.mjs references/showcase` → **0 error / 0 warning**
- 每个页面都是「`Scaffold` + `TopAppBar` + `LazyColumn`」骨架，没有用 `Column + verticalScroll` 凑

**没保证**
- **真机行为**：insets、手势、动画、字体缩放、RTL 只能在真机验证
- **多平台**：样张在 Desktop target 编译；Android / iOS 的 `MiuixTheme` 默认色一致，但系统栏与输入法相关部分不同
- **业务正确性**：数据是写死的示例（`remember { mutableStateOf(...) }`），没有 ViewModel / 持久化
- 业务占位符（`onClick = {}`）没有实现

## 4. 复制时改什么

1. 把 `PhoneScreen(...)` 换成你自己的 `MiuixTheme { ... }`（保留页面里的 `Scaffold`）。
2. 删掉 `ScreenTopBar` 里的 `StatusBar()`，改用系统的 `paddingValues.calculateTopPadding()`。
   状态栏放进 `topBar` 槽而不是包在 `Scaffold` 外面，是为了让 `OverlayDialog` 的遮罩能盖住它。
3. 数据源换成 ViewModel / `StateFlow`，`remember` 只在样张里用于演示交互态。
4. 字符串抽到资源文件（Android）或 `compose.resources`（KMP）。
