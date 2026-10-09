# 主题、颜色与文本样式

## 1. 两种等价的接线方式

### 方式 A：`colors` 直传（最简，旧项目/固定配色）

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MiuixTheme(
        colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}
```

> 这是 8 个真实项目里出现最多的写法（StatusBarLyric、Payload-Dumper-Compose、OShin、Updater-KMP 均如此）。手写模式偏好时用它。

### 方式 B：`ThemeController`（推荐，支持动态取色/跟随系统/强制明暗）

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val controller = remember { ThemeController(ColorSchemeMode.System) }
    MiuixTheme(controller = controller, content = content)
}
```

完整的「跟随设置项」写法（照抄官方 example 的结构）：

```kotlin
@Composable
fun AppTheme(
    colorMode: Int = 0,          // 0 System / 1 Light / 2 Dark / 3 MonetSystem / 4 MonetLight / 5 MonetDark
    keyColor: Color? = null,     // 动态取色种子色
    content: @Composable () -> Unit,
) {
    val controller = remember(colorMode, keyColor) {
        when (colorMode) {
            1 -> ThemeController(ColorSchemeMode.Light)
            2 -> ThemeController(ColorSchemeMode.Dark)
            3 -> ThemeController(ColorSchemeMode.MonetSystem, keyColor = keyColor)
            4 -> ThemeController(ColorSchemeMode.MonetLight, keyColor = keyColor)
            5 -> ThemeController(ColorSchemeMode.MonetDark, keyColor = keyColor)
            else -> ThemeController(ColorSchemeMode.System)
        }
    }
    MiuixTheme(controller = controller, content = content)
}
```

## 2. `ThemeController` 参数

| 参数 | 类型 | 默认 | 说明 |
| :-- | :-- | :-- | :-- |
| `colorSchemeMode` | `ColorSchemeMode` | `System` | 配色模式 |
| `lightColors` | `Colors` | `lightColorScheme()` | 浅色方案 |
| `darkColors` | `Colors` | `darkColorScheme()` | 深色方案 |
| `keyColor` | `Color?` | `null` | Monet 种子色；`null` = 用系统壁纸色 |
| `colorSpec` | `ThemeColorSpec` | `Spec2021` | 颜色规范版本 |
| `paletteStyle` | `ThemePaletteStyle` | `TonalSpot` | 调色板风格 |
| `isDark` | `Boolean?` | `null` | 覆盖系统明暗；`null` = 跟随系统 |

### `ColorSchemeMode`

| 值 | 说明 |
| :-- | :-- |
| `System` | 跟随系统明暗 |
| `Light` / `Dark` | 强制浅色 / 深色 |
| `MonetSystem` | 动态取色 + 跟随系统 |
| `MonetLight` / `MonetDark` | 动态取色 + 强制明暗 |

### `ThemePaletteStyle`

| 值 | 支持 `Spec2025` | 说明 |
| :-- | :-- | :-- |
| `TonalSpot` | 是 | 默认 Material You，平衡 |
| `Neutral` | 是 | 低彩度柔和 |
| `Vibrant` | 是 | 高彩度鲜艳 |
| `Expressive` | 是 | 大胆创意 |
| `Rainbow` / `FruitSalad` / `Monochrome` / `Fidelity` / `Content` | 否 | 其它风格，自动降级 `Spec2021` |

### `ThemeColorSpec`

`Spec2021`（原始 Material 3）/ `Spec2025`（仅 `TonalSpot`、`Neutral`、`Vibrant`、`Expressive` 支持）。

## 3. 读取主题值

```kotlin
val colors = MiuixTheme.colorScheme
val textStyles = MiuixTheme.textStyles
val mode = MiuixTheme.colorSchemeMode   // ColorSchemeMode?
val isDynamic = MiuixTheme.isDynamicColor
```

## 4. 颜色语义表（非 Monet 模式的默认值）

**只用下表的语义名，不要记十六进制值。** 深色值由主题自动切换。

### 主色 / 强调

| 名称 | 用途 |
| :-- | :-- |
| `primary` | 主色：Switch 打开、Button 主按钮、Slider 已填充、FAB |
| `onPrimary` | 主色上的内容（主按钮文字、FAB 图标） |
| `primaryVariant` | 主色变体 |
| `onPrimaryVariant` | 主色变体上的内容 |
| `primaryContainer` / `onPrimaryContainer` | 主色容器及其内容 |
| `disabledPrimary` / `disabledOnPrimary` | 禁用态（开关/按钮/滑块） |
| `disabledPrimaryButton` / `disabledOnPrimaryButton` | 按钮禁用态专用 |
| `disabledPrimarySlider` | 滑块禁用态专用 |

### 次要 / 中性容器

| 名称 | 用途 |
| :-- | :-- |
| `secondary` / `onSecondary` | 次要色 |
| `secondaryVariant` / `onSecondaryVariant` | 次要变体（`ButtonDefaults.buttonColors()` 默认底色） |
| `disabledSecondary*` | 禁用态 |
| `secondaryContainer` / `onSecondaryContainer` | 次要容器 |
| `secondaryContainerVariant` / `onSecondaryContainerVariant` | 次要容器变体 |
| `tertiaryContainer` / `onTertiaryContainer` | 三级容器（常用于「信息/提示」底色） |
| `tertiaryContainerVariant` | 三级容器变体 |

### 背景 / 表面

| 名称 | 用途 |
| :-- | :-- |
| `background` | 页面底色（`Scaffold` 默认用 `surface`） |
| `onBackground` | 背景上的主文本 |
| `onBackgroundVariant` | 背景上的次要文本（`SmallTitle` 默认色） |
| `surface` | 表面（`Scaffold` / `TopAppBar` / `NavigationBar` 默认） |
| `onSurface` | 表面上的主文本 |
| `surfaceVariant` | 表面变体 |
| `onSurfaceSecondary` | 表面上的次要文本（对话框 summary 默认） |
| `onSurfaceVariantSummary` | 摘要文本（`TopAppBar` subtitle 默认） |
| `onSurfaceVariantActions` | 操作文本（`ArrowPreference` 箭头默认） |
| `disabledOnSurface` | 禁用文本 |
| `surfaceContainer` | 容器表面（`FloatingToolbar`、`FloatingNavigationBar` 默认） |
| `onSurfaceContainer` / `onSurfaceContainerVariant` | 容器表面上的文本 |
| `surfaceContainerHigh` | 高对比容器（`SearchBar` 输入框默认） |
| `onSurfaceContainerHigh` | 高对比容器上的文本 |
| `surfaceContainerHighest` / `onSurfaceContainerHighest` | 最高对比容器 |

### 语义

| 名称 | 用途 |
| :-- | :-- |
| `error` / `onError` | 错误色及其内容（`Badge` 默认底色） |
| `errorContainer` / `onErrorContainer` | 错误容器及其内容 |
| `outline` | 轮廓/边框 |
| `dividerLine` | 分隔线（`HorizontalDivider` 默认色） |
| `windowDimming` | 对话框/下拉遮罩 |
| `sliderKeyPoint` / `sliderKeyPointForeground` / `sliderBackground` | 滑块关键点与轨道 |

## 5. 文本样式表

默认颜色由主题按 `onBackground` 在运行时注入，**不要自己传 `color`**。

| 样式 | 字号 | 字重 | 行高 | 典型用途 |
| :-- | :-- | :-- | :-- | :-- |
| `main` | 17sp | Normal | — | **`Text` 的默认样式**、设置项标题、正文 |
| `paragraph` | 17sp | Normal | 1.2em | 多段说明文字 |
| `body1` | 16sp | Normal | — | 正文 |
| `body2` | 14sp | Normal | — | 小正文 |
| `button` | 17sp | Normal | — | 按钮文字 |
| `footnote1` | 13sp | Normal | — | 脚注 |
| `footnote2` | 11sp | Normal | — | 极小脚注 |
| `headline1` | 17sp | Normal | — | 标题 |
| `headline2` | 16sp | Normal | — | 标题 |
| `subtitle` | 14sp | **Bold** | — | 强调小标题 |
| `title1` | 32sp | Normal | — | 页面大标题 |
| `title2` | 24sp | Normal | — | 区块大标题 |
| `title3` | 20sp | Normal | — | 区块标题 |
| `title4` | 18sp | Normal | — | 次级标题 |

```kotlin
Text("标题", style = MiuixTheme.textStyles.title2)
Text("正文", style = MiuixTheme.textStyles.body1)
Text("默认") // 就是 textStyles.main
```

## 6. 自定义主题

### 覆盖文本样式

```kotlin
val customTextStyles = defaultTextStyles(
    title1 = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold),
)
MiuixTheme(controller = controller, textStyles = customTextStyles) { /* ... */ }
```

### 覆盖部分颜色

用 `copy` 改单值，其余保持主题一致（真实项目常用，例如让下拉弹窗不透明）：

```kotlin
@Composable
fun OpaquePopupTheme(content: @Composable () -> Unit) {
    val colors = MiuixTheme.colorScheme
    MiuixTheme(
        colors = colors.copy(
            surfaceContainer = colors.surfaceContainer.copy(alpha = 1f),
            surfaceContainerHigh = colors.surfaceContainerHigh.copy(alpha = 1f),
            surfaceVariant = colors.surfaceVariant.copy(alpha = 1f),
        ),
        content = content,
    )
}
```

### 项目自定义配色

```kotlin
val MyLightColors = lightColorScheme(primary = Color(0xFF3482FF), /* ... */)
val MyDarkColors = darkColorScheme(primary = Color(0xFF277AF7), /* ... */)
val controller = remember { ThemeController(ColorSchemeMode.System, MyLightColors, MyDarkColors) }
```

### 判断当前是否深色

**库没有提供 `isInDarkTheme()`——这是项目自己封装的辅助函数**（skill 其它文件引用它时指的就是下面这段，照抄即可）：

```kotlin
// 把配色模式用 CompositionLocal 下发（官方 example 的做法），再据此推导
private val LocalAppColorMode = compositionLocalOf { AppColorMode.System }

@Composable
fun isInDarkTheme(): Boolean = when (LocalAppColorMode.current) {
    AppColorMode.Light, AppColorMode.MonetLight -> false
    AppColorMode.Dark, AppColorMode.MonetDark -> true
    else -> isSystemInDarkTheme()          // 跟随系统
}
```

不想引入 `CompositionLocal` 时，两个更省事的替代：

```kotlin
// 1) 只用系统值（方式 A：colors 直传时最合适）
val isDark = isSystemInDarkTheme()

// 2) 用主题当前背景色的亮度反推（lyricon 的做法，不依赖任何额外状态）
val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
```

> ⚠️ 用方式 A（直接传 `colors`）时，`ThemeController` 那套模式信息并不存在，所以**不要**依赖「强制浅色/深色」的推导——此时只有 `isSystemInDarkTheme()` 是准的。需要精确控制就用方式 B + `CompositionLocal`。

## 7. 常见错误

| 错误 | 后果 | 正确做法 |
| :-- | :-- | :-- |
| `Color(0xFF1A3825)` 写死状态色 | 深色模式刺眼/不可读 | 用 `tertiaryContainer` / `errorContainer` 等语义色；确需品牌色时同时提供明暗两套并注释 |
| 给 `Text` 传 `color = Color.Black` | 深色模式不可见 | 删掉 `color`，或传 `MiuixTheme.colorScheme.onBackground` |
| 在 `Theme.kt` 之外再套一层 `MiuixTheme` | 嵌套覆盖导致局部颜色不一致 | 只在需要局部改色时用（如上面的 OpaquePopupTheme），并说明原因 |
| 忘记 `MiuixTheme` 包裹 | `MiuixTheme.colorScheme` 抛异常/取到错误默认值 | 在 `MainActivity.setContent`/`App()` 最外层包一次 `AppTheme` |
| 同时用 `colors` 与 `controller` 两套写法 | 状态来源冲突 | 全项目统一一种；迁移时一次性替换 |
