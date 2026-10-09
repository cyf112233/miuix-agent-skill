# 依赖、模块与项目配置

## 1. 坐标与版本

Miuix 发布在 **Maven Central**，groupId `top.yukonga.miuix.kmp`。

| 模块 | 说明 | 何时需要 |
| :-- | :-- | :-- |
| `miuix-ui` | 核心 UI（自动包含 `miuix-core`、`miuix-squircle`） | **必装** |
| `miuix-preference` | Preference 组件（`SwitchPreference` 等）、`menu`、`popup`（依赖 `miuix-ui`） | 有设置页/下拉菜单 |
| `miuix-icons` | 扩展图标库（自动包含 `miuix-core`） | 需要 `MiuixIcons.*` 扩展图标 |
| `miuix-nav` | 自研导航运行时（零依赖，不依赖 navigation3） | 需要页面导航 |
| `miuix-squircle` | 平滑圆角（已被 `miuix-ui` 传递包含） | 单独用才需要显式声明 |
| `miuix-blur` | 模糊 / backdrop（Android 需 **minSdk 33**） | 需要高斯模糊背景 |
| `miuix-shader` | 运行时着色器抽象（被 `miuix-blur`/`miuix-squircle` 传递包含） | 一般不需要 |

**当前最新版本：`0.9.4`**（写作时的 Maven Central `<release>`；升级前请再确认一次）。
各模块版本号保持一致。

> Miuix 明确标注为实验性库，API 可能在小版本间变化。**改动前先读项目里已引入的版本**，再决定用哪套 API（见 `pitfalls.md` 的迁移表）。

## 2. Compose Multiplatform 项目

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
            implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.4") // 设置页
            implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.4")      // 图标
            implementation("top.yukonga.miuix.kmp:miuix-nav:0.9.4")        // 导航
            implementation("top.yukonga.miuix.kmp:miuix-blur:0.9.4")       // 模糊（可选）
        }
    }
}
```

`settings.gradle.kts` 里确保有：

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

## 3. 纯 Android（Jetpack Compose）项目

用带 `-android` 后缀的坐标：

```kotlin
dependencies {
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-nav-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.4") // minSdk 33
}
```

参考项目里的真实写法（InstallerX-Revived / lyricon 都用 `libs.versions.toml` 管理，建议照做）：

```toml
# gradle/libs.versions.toml
[versions]
miuix = "0.9.4"

[libraries]
miuix-ui = { module = "top.yukonga.miuix.kmp:miuix-ui", version.ref = "miuix" }
miuix-preference = { module = "top.yukonga.miuix.kmp:miuix-preference", version.ref = "miuix" }
miuix-icons = { module = "top.yukonga.miuix.kmp:miuix-icons", version.ref = "miuix" }
miuix-nav = { module = "top.yukonga.miuix.kmp:miuix-nav", version.ref = "miuix" }
miuix-blur = { module = "top.yukonga.miuix.kmp:miuix-blur", version.ref = "miuix" }
```

## 4. 其他平台（单平台直接依赖）

```kotlin
implementation("top.yukonga.miuix.kmp:miuix-ui-iosarm64:0.9.4")
implementation("top.yukonga.miuix.kmp:miuix-ui-iossimulatorarm64:0.9.4")
implementation("top.yukonga.miuix.kmp:miuix-ui-macosarm64:0.9.4")
implementation("top.yukonga.miuix.kmp:miuix-ui-desktop:0.9.4")
implementation("top.yukonga.miuix.kmp:miuix-ui-wasmjs:0.9.4")
implementation("top.yukonga.miuix.kmp:miuix-ui-js:0.9.4")
```

## 5. 项目结构建议（源自真实项目）

```
app/src/main/kotlin/<pkg>/
├── MainActivity.kt              // 或 KMP 的 App.kt
├── ui/
│   ├── theme/
│   │   ├── Theme.kt             // AppTheme()：MiuixTheme 统一入口
│   │   └── Color.kt             // 只有自定义 Colors 覆盖时才存在
│   ├── page/                    // 一个页面一个文件
│   │   ├── SettingsPage.kt
│   │   └── AboutPage.kt
│   ├── widget/                  // 项目自封装的复用组件
│   │   └── MiuixCards.kt
│   └── navigation/
│       ├── Route.kt             // @Serializable sealed interface Route : NavKey
│       └── Navigator.kt
```

关键实践：

- **`Theme.kt` 只暴露一个 `AppTheme { }`**，其它地方不再直接调 `MiuixTheme`（例外：局部改色，见 `theme.md`）。
- **业务页面不自己 new 主题、不自己算 insets**。
- 需要复用自己的设置行时，在 `widget/` 里基于 `BasicComponent` 封装，而不是复制粘贴。

## 6. 平台注意事项

| 事项 | 说明 |
| :-- | :-- |
| Android `minSdk` | `miuix-blur` 要求 **33+**；其余模块无此限制。Android 项目一般 `minSdk 26+` 即可 |
| Android 状态栏 | 需要浅色状态栏图标时手动设置：`WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme`（真实项目普遍这么做） |
| 依赖冲突 | 不要同时引入 Material3 的 `NavigationBar`/`Scaffold` 等同类组件；如需 Material 仅做过渡，明确 import 别名避免歧义 |
| Desktop/Web | 无 `WindowInsets.systemBars` 语义，`Scaffold` 会自动退化为 0；屏幕圆角为 0 |
| iOS/macOS | 与 Desktop 同源（Skiko），`miuix-blur` 走 Skia 实现 |
| 编译 | 库要求较新的 Kotlin/Compose；项目 Kotlin ≥ 2.1、Compose Multiplatform ≥ 1.8 时通常可用。若报 `NoSuchMethodError`/元数据版本错误，先升 Kotlin 编译器与 Compose 版本 |
