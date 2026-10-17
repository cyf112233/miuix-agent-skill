# 可运行样张（showcase）

`references/showcase/` 下的 11 个 `.kt` 文件不是「代码片段」，而是**一个能编译、能渲染的真实页面集**：
它们针对 **miuix 0.9.4** 编写，整体编译通过，并用 Compose Desktop + `ImageComposeScene`
离线渲染成了 `assets/screens/*.png`。文档里的结构、间距、颜色，都以这份源码为准。

> 为什么要有它：片段能保证「符号存在」，但保证不了「能编译」和「好看」。
> 这份样张把两件事都钉住了——**编译器**证明 API 用对，**截图**证明观感对。

---

## 1. 文件

| 文件 | 内容 | 对应文档 |
| :-- | :-- | :-- |
| `Frame.kt` | `ShowcaseTheme`（强制 Light/Dark）、`PhoneScreen`、仿真状态栏、`ScreenTopBar`、`IconBadge`、`AppIconTile`、`ProgressRing` | `layout.md` §3-4 |
| `StatusPalette.kt` | 三态 `StatusTone` + `StatusCard` + `StatCard` + `isMiuixDark()` | `status-colors.md`、`scenario-home.md` §2 |
| `StatusIcons.kt` | 圆环 + 勾 / 感叹号的 `ImageVector`（移植自 lyricon 的矢量） | `status-colors.md` §2 |
| `ScreenHome.kt` | 首页：状态卡 → 两列数据卡 → 快捷入口 → 最近使用 → 常用设置 → 底部导航 | `scenario-home.md`、`scenario-shell.md` |
| `ScreenSettings.kt` | 设置页：4 个分组、Switch/Arrow/Dropdown/Slider、可选 `OverlayDialog` | `preferences.md`、`scenario-shell.md` |
| `ScreenList.kt` | 应用列表：搜索框 → `TabRow` 分段 → 分组卡片 → 自启动开关 | `scenario-list.md` |
| `ScreenInfo.kt` | 关于页：Hero → 只读信息卡 → 支持卡 → 页脚 | `scenario-info.md` |
| `ScreenForm.kt` | 表单：`Close`/`Ok` 顶栏 → `TextField` → 可见错误 → 单选/复选 → 主按钮 | `scenario-form.md` |
| `ScreenState.kt` | 空态：居中插画 + 文案 + 主操作 | `scenario-state.md` |
| `ScreenOnboarding.kt` | 引导页：插画 → 能力列表 → 页点 → 主/次按钮 | `scenario-onboarding.md` |
| `ScreenStatusSpec.kt` | 状态卡三态规格页（文档样张） | `status-colors.md` |

## 2. 截图

| 页面 | 浅色 | 深色 |
| :-- | :-- | :-- |
| 首页 | `assets/screens/home.png` | `assets/screens/home-dark.png` |
| 设置 | `assets/screens/settings.png` | `assets/screens/settings-dark.png` |
| 设置 + 弹窗 | `assets/screens/settings-dialog.png` | — |
| 状态卡三态 | `assets/screens/status-cards.png` | — |
| 列表 | `assets/screens/list.png` | — |
| 关于 | `assets/screens/info.png` | — |
| 表单 | `assets/screens/form.png` | — |
| 空态 | `assets/screens/state.png` | — |
| 引导 | `assets/screens/onboarding.png` | — |

## 3. 怎么重新生成

样张本身是纯 UI 代码，只需要一个 JVM + Compose Desktop target：

```kotlin
// build.gradle.kts（Kotlin JVM + Compose Multiplatform）
dependencies {
    implementation(compose.desktop.currentOs)
    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-squircle:0.9.4")
}
```

渲染入口（无窗口、无模拟器，输出 PNG）：

```kotlin
val scene = ImageComposeScene(
    width = (412 * 2.5f).toInt(),      // 412dp × 2.5
    height = (915 * 2.5f).toInt(),
    density = Density(2.5f),
) { HomeScreen() }
scene.render(0)
scene.render(16_000_000L)
scene.render(120_000_000L)
val image = scene.render(600_000_000L)   // 多帧推进，让弹层/动画落到终态
File("home.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
```

要点：
- `PhoneScreen` 只负责「主题 + 仿真状态栏」，页面自己仍然是 `Scaffold`——**不要**把状态栏包在 `Scaffold` 外面，
  否则 `OverlayDialog` 的遮罩盖不到它，弹窗一出顶部就露一条亮边。
- 桌面端没有系统状态栏，所以 `StatusBar()` 是自绘的（时间 + 信号/Wi-Fi/电池），只用于截图，不进业务代码。
- 深色图用 `MiuixTheme(controller = ThemeController(ColorSchemeMode.Dark))` 强制，**不要**依赖系统主题。
- 字体用 MiSans（`fc-match sans-serif` 应命中 MiSans）；没有就退到 Noto Sans CJK，观感差别主要在字重。

## 4. 这份样张保证了什么 / 没保证什么

**保证了**
- 11 个文件全部对 **0.9.4** 编译通过（Kotlin 2.4.21 / Compose 1.10+ / Compose Multiplatform 1.12.1）
- `scripts/miuix-lint.mjs references/showcase` → **0 error / 0 warning**
- 每张截图都是这份源码的真实渲染，不是手绘稿

**没保证**
- **真机行为**：insets、手势、动画、字体缩放、RTL 只能在真机验证
- **多平台**：截图来自 Desktop target；Android / iOS 的 `MiuixTheme` 默认色一致，但系统栏与输入法相关部分不同
- **业务正确性**：数据是写死的示例（`remember { mutableStateOf(...) }`），没有 ViewModel / 持久化
- 业务占位符（`onClick = {}`）没有实现

## 5. 复制时改什么

1. 把 `PhoneScreen(...)` 换成你自己的 `MiuixTheme { ... }`（保留页面里的 `Scaffold`）。
2. 删掉 `ScreenTopBar` 里的 `StatusBar()`，改用系统的 `paddingValues.calculateTopPadding()`。
3. 数据源换成 ViewModel / `StateFlow`，`remember` 只在样张里用于演示交互态。
4. 字符串抽到资源文件（Android）或 `compose.resources`（KMP）。
