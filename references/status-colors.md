# 状态色：红 / 黄 / 绿怎么来

状态卡（「运行正常 / 空间不足 / 服务已停止」）是 miuix 界面里**唯一允许整卡换色**的组件。
但 Miuix **没有** success / warning token，这一组颜色必须由项目自己补。
本文给出四个真实项目的取值、推荐标准，以及「其余界面为什么只该用黑白 + 强调色」。

---

## 1. 先看清库的边界

`Colors.kt` 的默认浅色调色板（`lightColorScheme()`，`Colors.kt:341-390`）：

| token | 默认值 | 真实语义 | 能不能当「成功绿」用 |
| :-- | :-- | :-- | :-- |
| `primary` | `#3482FF` 蓝 | 可操作 / 选中 / 链接 | 否（是蓝） |
| `primaryContainer` | `#5D9BFF` 亮蓝 | 主色底 | 否 |
| `error` | `#E94634` 红 | 错误 / 危险 | 可以当「异常」 |
| `errorContainer` | `#FDF6F4` 极浅红 | 错误底 | 可以当「异常」底 |
| `secondary` | `#E6E6E6` **灰** | 中性按钮底 | **不要**：它不是绿 |
| `secondaryContainer` | `#F0F0F0` **灰** | 中性容器 | **不要**：它不是绿 |
| `tertiaryContainer` | `#EAF2FF` 浅蓝 | 主色的浅底 | 否 |
| `surfaceContainer` | `#FFFFFF` 白 | 卡片底 | — |

结论：

1. **静态主题下没有任何绿/黄 token。** 想用 `secondaryContainer` 表示成功，实际会得到一块灰底。
2. InstallerX 里 `secondaryContainer` 表示成功，**只在 `isDynamicColor`（Monet 取色）分支里**成立——
   取色后 secondary 的色相来自壁纸，不是固定的绿。复制代码时别把条件分支漏掉。
3. 所以三态色必须**成对定义**（容器 + 强调色），并同时给浅色与深色两套。

---

## 2. 四个项目各自怎么做

| 项目 | 做法 | 容器（浅 / 深） | 强调色 | 文字 |
| :-- | :-- | :-- | :-- | :-- |
| **lyricon** `HomeTab.kt:126-190` | 浅底 + 右下角 128dp 出血图标 | 绿 `#DFFAE4` / `#102819`；红 `#FDECEE` / `#402626` | `#36D167` / `#D84242` | 自定义：`#0F1B13` / 白；摘要再 0.68 alpha |
| **InstallerX** `MiuixHomePage.kt:234-360`、`theme/Color.kt:14-15` | 同上，图标放大到 170dp | 绿 `#DFFAE4` / `#1A3825`；红 `#FAEEEE` / `#381A1A` | `#36D167` / `#D13636` | `onSurface`（静态）/ `onSecondaryContainer`、`onErrorContainer`（动态） |
| **OShin** `MainHome.kt:566-648` | 渐变卡 + 白色状态点 | 渐变 `#6366F1→#8B5CF6` 等 | 点色 `#22C55E` / `#F59E0B` / `#EF4444` | 白 + 白 0.9 |
| **Updater-KMP** `LoginCardView.kt:62-65` | 浅底，无出血图标 | 绿 `#DFFAE4` / `#1A3825`；红 `#F8E2E2` / `#310808` | 无 | 默认 |

四分之三的项目用「浅底 + 实色图标」，只有 OShin 用渐变。
**本 skill 采用浅底方案**，理由：OShin 的渐变卡是它自己的视觉语言（YouTube 风格），不是 HyperOS 的；
而浅底 + 出血图标在 lyricon 与 InstallerX 两个独立项目里逐项一致——包括 140dp 高、16dp 内边距、
`offset(28, 34)`、20sp SemiBold 标题、14sp Medium 摘要、`PressFeedbackType.Tilt`。

### 出血图标的形状有讲究

lyricon 的图标是**圆环 + 勾**（`ic_home_status_check.xml`），InstallerX 用应用自己的 logo。
共同点是**轮廓接近圆形**：被卡片右下角裁掉一部分仍然成立。
裸勾 `MiuixIcons.Ok` 裁掉右侧就只剩一道斜线——这也是把矢量移植进代码的原因
（`references/showcase/StatusIcons.kt` 里就是这两个矢量的 Kotlin 版本）。

---

## 3. 推荐取值（本 skill 的标准）

| 语义 | 容器（浅） | 容器（深） | 强调色 | 什么时候用 |
| :-- | :-- | :-- | :-- | :-- |
| 正常 | `#DFFAE4` | `#102819` | `#36D167` | 全部检查通过、服务在跑、权限已给 |
| 注意 | `#FFF4E0` | `#2E2210` | `#F59E0B` | 有风险但可恢复（空间不足、未授权但可跳设置） |
| 异常 | `#FDECEE` | `#402626` | `#D13636` | 功能已经不可用（服务停止、配置损坏） |

- 绿 / 红的容器来自 lyricon，强调色 `#36D167` / `#D13636` 来自 lyricon + InstallerX。
- 琥珀的强调色 `#F59E0B` 来自 OShin 的 `Status.WARNING`；容器 `#FFF4E0` 是同色相的浅底，
  **这一格是本 skill 推导的，没有项目原样用过**（诚实标注）。
- 深色容器 `#102819` / `#402626` 来自 lyricon；`#2E2210` 同样是推导值。

颜色常量集中在 `references/showcase/StatusPalette.kt`，每行都有 `// miuix-lint:allow` 与理由。

---

## 4. 四条硬规则

1. **整卡换底，不只给图标染色。** 状态是这一屏最重要的信息，换底才有一眼可辨的优先级。
2. **容器和强调色成对给，浅色 / 深色各一套。** 不要 `accent.copy(alpha = 0.1f)` 现算容器——
   在深色主题下会得到一块发灰的脏色。也不要把强调色直接当容器（`#36D167` 上放黑字对比度不足）。
3. **文字只从 `onSurface` / `onSurfaceVariantSummary` 取。**
   lyricon 自己写了一对 `#0F1B13` / 白，也是成对的；只要成对就可以，但**不要只写浅色那一半**。
   判断深浅不要用 `isSystemInDarkTheme()`（主题可能被 `ThemeController` 强制或换成 Monet），
   用 `MiuixTheme.colorScheme.background.luminance() < 0.5f`——`StatusPalette.isMiuixDark()` 就是这个。
4. **状态色只出现在状态卡上（可以再加一个 8dp 状态点）。**
   不要外溢到按钮、快捷入口、列表图标底色、导航栏。红/黄/绿各配一个圆形图标底，
   是最常见的「一看就不像原生」的做法——见下一节。

---

## 5. 其余界面：黑白 + 强调色就够

这是 HyperOS 的实际观感，也是四个项目的共同点：

| 元素 | 用什么 | 不要用 |
| :-- | :-- | :-- |
| 页面底色 | `Scaffold` 默认 `surface`（浅 `#F7F7F7`） | 纯白（卡片就浮不起来了） |
| 卡片 / 分组 | `surfaceContainer`（浅 `#FFFFFF`） | 彩色卡片 |
| 正文 / 标题 | `onBackground`、`onSurface` | 彩色文字 |
| 次级说明 | `onSurfaceVariantSummary`、`onBackgroundVariant` | 彩色文字 |
| 可操作 / 选中 | `primary` | 每个入口一种颜色 |
| 图标底座 | `tertiaryContainer` + `primary` 图标，或 `surfaceContainerHigh` + `onSurface` | 绿/黄/红/紫各来一个 |
| 次要按钮 | `TextButton`（默认 `secondaryVariant` 灰底） | 彩色底 |
| 主按钮 | `ButtonDefaults.buttonColors(color = primary, contentColor = onPrimary)` | **注意 `Button` 默认是灰底**（`secondaryVariant`），主 CTA 必须显式传 primary |
| 破坏性确认 | 可以给确认按钮 `error` 底；**不要**整卡变红 | 红色正文 |

一句话：**颜色是信息，不是装饰。** 一屏里除了状态卡，其余像素只应该出现黑、白、灰、一个强调色。

---

## 6. 提示卡（TipCard）是另一套，别和状态卡混用

InstallerX `MiuixCards.kt:52-90` 的 `TipCard`：

```kotlin
colors = CardDefaults.defaultColors(
    color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
    contentColor = MiuixTheme.colorScheme.onSurface,
)
// 文字：primary，SemiBold；右上角可放一个 22dp 的关闭 IconButton（primary）
```

它用于「范围提示 / 设置建议」，**不表示状态**，所以用主色而不是绿：
- 状态卡 = 绿/黄/红，回答「现在怎么样」
- 提示卡 = 主色淡底，回答「这里要注意」
- 危险提示卡 = `errorContainer` 底 + `error` 文字（`WarningCard`，同一文件 `:92-120`）

---

## 7. 自检

- [ ] 状态卡的容器与强调色成对出现，且有深色版本
- [ ] 文字色来自 `onSurface*`，或像 lyricon 那样成对自定义
- [ ] 状态色没有出现在状态卡与状态点之外
- [ ] 主按钮显式传了 `primary`（默认灰底是坑）
- [ ] 出血图标是圆形轮廓（圆环 / logo），不是裸勾
- [ ] `// miuix-lint:allow` 旁写了理由

## 8. 出处

| 文件 | 内容 |
| :-- | :-- |
| `repos/lyricon/app/src/main/kotlin/io/github/proify/lyricon/app/ui/tabs/HomeTab.kt:126-190` | 140dp 状态卡、容器/强调色/文字色、出血图标 |
| `repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/miuix/settings/home/MiuixHomePage.kt:234-410` | 容器分支（动态/静态）、170dp 图标、`MiuixStatCard` |
| `repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/theme/Color.kt:14-15` | `#36D167` / `#D13636` |
| `repos/OShin/app/src/main/java/com/suqi8/oshin/ui/mainscreen/home/MainHome.kt:566-648` | 三态语义（`#22C55E` / `#F59E0B` / `#EF4444`）、渐变卡 |
| `repos/Updater-KMP/app/shared/src/commonMain/kotlin/ui/LoginCardView.kt:62-65` | 浅底容器（另一组绿/红） |
| `repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/miuix/widgets/MiuixCards.kt:52-120` | `TipCard` / `WarningCard` |
| `miuix-lib/miuix-ui/src/commonMain/kotlin/top/yukonga/miuix/kmp/theme/Colors.kt:341-390` | 默认调色板（`secondary` 是灰的证据） |

> 路径是产出机器的 provenance，换机器即失效；行号对应当前克隆的版本（lyricon 0.9.3 / InstallerX 0.9.4 / OShin 0.5.2 / Updater 0.9.4）。
