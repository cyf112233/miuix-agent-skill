---
name: miuix-ui
description: "用 Miuix（小米 HyperOS 设计规范的 Compose Multiplatform UI 库，top.yukonga.miuix.kmp）构建界面。按 8 个通用场景组织（主框架/多 Tab、首页/仪表盘、通用列表、信息展示、表单/编辑、状态页、自适应、引导与权限），设置页单独成篇。结构约定来自官方 example 与 8 个真实开源项目的交叉比对；SKILL.md 内按「库默认值 / 多数派惯例 / 单项目偏好 / 已被证伪」四级标注来源强度，不把单项目偏好当规范。覆盖依赖版本、MiuixTheme/ThemeController 配色与文本样式、Scaffold + TopAppBar + LazyColumn 页面骨架、SmallTitle + Card + XxxPreference 分组列表、对话框/底部抽屉/下拉菜单、miuix-nav 导航、icons、squircle/blur/overscroll、旧版 API 迁移，一个配套的零依赖 lint（E2/E3 + W1–W9），以及 11 个对 0.9.4 编译通过的样张源码（`references/showcase/`）。状态卡的红/黄/绿有专门一章（Miuix 没有 success/warning token，这里给出 InstallerX / lyricon / OShin / Updater 四个项目的取值与出处）。适用于任何要求 miuix 风格、HyperOS 风格、小米原生观感的 Compose/KMP/Android 界面任务：写首页、写列表页、写关于/信息页、写表单、做设置页、搭 Tab 框架、处理空/加载/错误/权限态、适配大屏与折叠屏、调主题配色，或把 Material 界面迁移到 Miuix。Triggers: miuix, MiuixTheme, HyperOS UI, 小米风格, 澎湃 UI, mui 界面, 写个设置页, 写个首页, 写个列表页, 用 miuix 做个页面, miuix 主题, miuix 组件, top.yukonga.miuix."
---

# 用 Miuix 构建界面

Miuix 是小米 HyperOS 设计规范的 Compose Multiplatform UI 库（Android / iOS / Desktop / macOS / Wasm / JS）。

**本 skill 的约定来自「官方文档 + 官方 example + 7 个真实开源项目」中反复出现的统一写法**，而不是个人偏好。（XiaomiHelper 未声明 miuix 依赖，只能作反面参考，见 §2「已被证伪」。）

| 项目 | miuix 版本 | 用途 |
| :-- | :-- | :-- |
| [InstallerX-Revived](https://github.com/wxxsfxyzm/InstallerX-Revived) | 0.9.4 | 最完整的 miuix 设置体系（首选参考） |
| [lyricon](https://github.com/proify/lyricon) | 0.9.3 | 现代 0.9.x API + 自定义容器 |
| [Updater-KMP](https://github.com/YuKongA/Updater-KMP) | 0.9.4 | KMP + 对话框 + 表单 |
| [HyperStar](https://github.com/YunZiA/HyperStar) | 0.9.0 | 复杂布局 + 模糊背景 |
| [XiaomiHelper](https://github.com/HowieHChen/XiaomiHelper) | 0.9.x | ⚠️ 不可作结构证据：未声明 miuix 依赖、0 个 `Scaffold` |
| [OShin](https://github.com/suqix8/OShin) | 0.5.2 | 旧 API（含迁移参考） |
| [StatusBarLyric](https://github.com/Block-Network/StatusBarLyric) | 0.4.3 | 旧 API（含迁移参考） |
| [Payload-Dumper-Compose](https://github.com/rcmiku/Payload-Dumper-Compose) | 0.3.4 | 旧 API 最小示例 |

**照抄本 skill 的写法 = 直接得到「原生」观感。不要自创布局数值。**

---

## 1. 十六条铁律

违反任意一条就算返工。

1. **颜色只来自 `MiuixTheme.colorScheme.*`，文本样式只来自 `MiuixTheme.textStyles.*`。** 禁止 `Color(0xFF...)` 字面量（品牌色/图标底色等确有必要的除外，且必须注释原因）。
2. **不要用 Material 组件**（`androidx.compose.material3.*` 的 `Scaffold`/`Text`/`Card`/`TopAppBar`/`Icon`/`TextButton`…）。Miuix 有同名组件，导入 `top.yukonga.miuix.kmp.basic.*`。**首选 Miuix，不要混排**——真实项目里确有例外（lyricon 从 material3 借了一个 `Icon`，InstallerX 有 84 个文件带 Material），但那是迁移期妥协，不是目标状态。
3. **每个页面 = `Scaffold` + `TopAppBar` + `LazyColumn` 三段式**，不要用 `Column` + `verticalScroll` 代替设置/列表页。
4. **分组列表固定为 `SmallTitle` + `Card(padding(horizontal = 12.dp).padding(bottom = 12.dp))` + `XxxPreference`**。分组之间不要另加间距。
5. **`OverlayDialog` / `OverlayBottomSheet` / `Overlay*Preference` 必须位于某个 `Scaffold` 的 composition 内**（作为内容 lambda 里 `LazyColumn` 的兄弟节点），否则弹不出来。不要放在会被 `LazyColumn` 回收的 `item {}` 里。
6. **控件用 Preference 组件，不要手搓 Row + Switch**：设置项用 `SwitchPreference`/`ArrowPreference`/`CheckboxPreference`/`RadioButtonPreference`/`SliderPreference`/`Overlay*Preference`。
7. **`LazyColumn` 的滚动修饰符**：`overScrollVertical()` **非必需**——`MiuixTheme` 已通过 `LocalOverscrollFactory provides MiuixOverscrollFactory` 全局安装越界回弹，只有「内容未超出容器」时全局方案不触发，需手动加。`scrollEndHaptic()` 可选。**唯一有文档依据的顺序约束是 `overScrollVertical()` 在 `nestedScroll(...)` 之前**；`scrollEndHaptic()` 放哪都行（InstallerX/lyricon 实际把它放最前）。`overscrollEffect = null` 用于避免与 Miuix 回弹叠加。
8. **`MiuixScrollBehavior()` 必须由页面创建并传给 `TopAppBar`**，否则折叠大标题/滚动吸附失效。大标题页用 `TopAppBar`（带 `largeTitle`），普通页用 `SmallTopAppBar`。
9. **图标必须逐个 import**：`MiuixIcons.XXX` 是扩展属性，来自 `top.yukonga.miuix.kmp.icon.extended.XXX`（基础图标为 `top.yukonga.miuix.kmp.icon.basic.XXX`）。漏 import 会编译失败，不要以为存在 `MiuixIcons` 通配。
10. **不要手写 `dp` 常量表**：优先用 `XxxDefaults.*`（如 `ButtonDefaults.CornerRadius`、`BasicComponentDefaults.InsideMargin`）。只有当设计明确要求偏离时才显式传参。
11. **页面内不要自己算 insets**：只用 `Scaffold` 给回的 `paddingValues.calculateTopPadding()` 等；不要叠加 `statusBarsPadding()`（除非页面无 `TopAppBar`）。
12. **状态用 `rememberSaveable`/ViewModel**，不要用全局可变单例；`LazyColumn` 的 `items` 要给稳定 `key`。
13. **`NavigationBar` 只在主框架建一次**。Tab 页**可以**有自己到 `Scaffold`（官方 example 的 `MainPage.kt` 就是这样），但**不要再建一个底部导航栏**，否则叠两层、insets 双算。Tab 内建 `Scaffold` 时注意：会上移 insets 作用域并自带 popup host，别和外层宿主打架。详见 `scenario-shell.md`。
14. **破坏性操作必须二次确认**（`OverlayDialog`）；校验失败要给出可见的错误文案，不能只静默禁用按钮。
15. **你设了底色，就得自己设内容色**：`Icon` 的 `tint` 默认是 `LocalContentColor.current`，而只有 `Badge`/`Button`(含 `TextButton`)/`Card`/`Snackbar`/`Surface`/`Tooltip` 会替你传内容色。**`FloatingActionButton`、`IconButton`、`OverlayDialog`/`OverlayBottomSheet` 的 `content`、以及任何 `squircleSurface(...)` 容器都不传** —— 里面的 `Icon`/`Text` 会继承页面环境色（浅色下变黑、深色下变白）。详见 `components.md` §0。
16. **一屏只用「黑白灰 + 一个强调色」，唯一的例外是状态卡。** 除状态卡外，不要给快捷入口 / 列表图标底 / 卡片配第二种颜色：图标底座要么 `tertiaryContainer` + `primary`，要么 `surfaceContainerHigh` + `onSurface`。状态卡的红/黄/绿是**整卡换底**、容器与强调色成对定义、深浅各一套，规则与出处见 `references/status-colors.md`。另外 `Button` 默认是**灰底**（`secondaryVariant`），主 CTA 必须显式 `colors = ButtonDefaults.buttonColors(color = colorScheme.primary, contentColor = colorScheme.onPrimary)`。

---

## 2. 页面骨架的形状（不是可拷贝代码）
```text
页面（任何场景）
└─ Scaffold(
     topBar   = TopAppBar(title, navigationIcon = 返回, actions = 动作, scrollBehavior = scrollBehavior)
     bottomBar= NavigationBar { NavigationBarItem(...) }        // 只有主框架场景
     snackbarHost = SnackbarHost(state)
   ) { paddingValues ->
     LazyColumn(
        modifier = Modifier.fillMaxSize()
            .overScrollVertical()          // 可选：MiuixTheme 已全局装回弹
            .scrollEndHaptic()             // 可选
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
        overscrollEffect = null,
     ) {
        item { Spacer(Modifier.size(12.dp)) }        // 首行呼吸位
        item { SmallTitle("分组标题") }
        item {
            Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                SwitchPreference(...)                                 // 一行一个设置项
                HorizontalDivider(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                ArrowPreference(...)
            }
        }
     }

     OverlayDialog(...) { /* 弹层：在 Scaffold 内、LazyColumn 外 */ }
   }
```

> ⚠️ **SKILL.md 故意不含可编译的骨架代码**（否则「先读场景文件」就成了可跳过的步骤）。
> 上面只是形状示意——**真正的可拷贝代码在 `references/scenarios.md`（通用外壳）和各个 `scenario-*.md`（按场景）里**。
> 直接照这里的伪代码写会缺 import、缺状态声明、缺配对的分支。

### 这些数值的来源等级（请按等级信任，不要一律当铁律）

本 skill 的规则有四种来源，**强度不同**，我逐条标注。经第三方复核，「8 个项目共识」这个说法是被我夸大的：
真实成分是**一份现代 InstallerX / lyricon 风格指南**，加少量真·库约束。

| 层级 | 数值 | 来源等级 | 实际证据 |
| :-- | :-- | :-- | :-- |
| 卡片外间距 | `horizontal = 12.dp` / `bottom = 12.dp` | **多数派惯例**（非压倒性） | 原始计数 125× vs 37×，但**数的不是同一件事**：125 里只有 **87** 处在 `Card(` 附近，37 里只有 **14** 处是卡片外边距。精确惯用法 `Card(padding(h=12).padding(b=12))` 共 **42 处，其中 36 处在 InstallerX**。lyricon 对分组 `SwitchPreference` 卡用 16.dp |
| 卡片内间距 | `PaddingValues(16.dp)` | **库默认值**（最硬） | `BasicComponentDefaults.InsideMargin`，由 Preference 自带 |
| 列表顶部留白 | `Spacer(12.dp)` | 多数派惯例 | InstallerX / lyricon 一致 |
| 分组标题缩进 | `28.dp` | **库默认值 + 数学关系** | `SmallTitleDefaults.InsideMargin` = 12 + 16，与卡片文字**严格对齐**。这条是真洞察 |
| 组内分隔线（Preference 行之间） | `padding(horizontal = 16.dp)` | **库默认值推导** | 与卡片内 16.dp 文字对齐。注意：这是**分组内 Preference 行之间**的规则；内容卡内部的分割线用全宽（见 `scenario-list.md`） |
| 顶栏标题缩进 | `26.dp` | **库默认值** | `TopAppBarDefaults.TitlePadding` |

> `Card` 的默认 `insideMargin` 是 `PaddingValues(0.dp)`，所以组内边距由每个 Preference 的 `insideMargin`（默认 16.dp）提供 —— 这正是「卡片 + 分割线」能对齐的原因。

### 单项目偏好（我此前错误地写成了「共识」，现已标注）

以下写法的证据只有**一个**项目，**跟随你项目的既有风格即可，不要当成规范**：

| 写法 | 唯一来源 | 其它项目怎么做 |
| :-- | :-- | :-- |
| 首页状态卡 `140.dp` / 装饰图标 `128.dp` / `offset(28, 34)` / `Tilt` | lyricon `HomeTab.kt` | InstallerX 用 `size(170.dp)`、`offset(50, 38)` |
| 首页「状态卡→快捷入口→信息卡→入口列表」四段式 | lyricon | HyperStar 是设置列表；Updater 是表单；OShin 是媒体仪表盘；StatusBarLyric 是 Logo+开关；官方 example 是搜索+列表 |
| `imePadding()` 加在 `Scaffold` 上 | InstallerX `MiuixEditPage:189` | Updater 0.9.4 加在 `LazyColumn`（`App.kt:416`）；官方 example 与 OShin 加在 pager 的 modifier 上 |
| 编辑页 `Close` 取消 + `Ok` 保存 | InstallerX `MiuixEditPage:198,207` | Updater `LoginDialog` 用 `TextButton("登录")` + `TextButton("取消")` |
| 切页用 `springAnimateToPage` | 官方 example | InstallerX 用距离缩放的 tween（`PagerState.kt:60-75`）。（「返回键回首个 Tab」的处理倒是双方逐行一致） |
| `items(list, key = …)` 一律给 | — | 实测 6 带 / 21 总计。**仍是强烈建议**（不给 key 会导致增删错位），但别称它是共识 |

### 已被证伪、不要再传播的说法

- ❌ 「Tab 页面不要建 `Scaffold`」——官方 example `MainPage.kt:257` 的 Tab 页自带 `Scaffold`。
- ❌ 「滚动三件套顺序固定」——InstallerX/lyricon 的实际顺序与旧版说法相反，且 `overScrollVertical()` 非必需。
- ❌ 「`Overlay*` 需要 `Scaffold`，所以弹窗都必须在 `Scaffold` 内」——`window.Window*` 族（`WindowDialog`/`WindowBottomSheet`/`WindowListPopup`）的 `requiresScaffoldHost: false`，**不需要** `Scaffold`。InstallerX 用 `WindowDialog` **23 次**、`overlay.OverlayDialog` **0 次**。
- ❌ 「XiaomiHelper 是参考项目」——它**未声明 miuix 依赖**、0 个 `Scaffold`、0 个 `LazyColumn`，还在用 0.9.4 已移除的 `theme.miuixShape`。它的页面结构不能作为证据。

---

## 3. 按「通用场景」选结构（做页面的第一步）

先把需求归到一个场景，再读对应文件抄结构。**这是本 skill 的主线**，不要跳过直接写代码。

| 通用场景 | 一句话 | 结构主线 | 场景文件 |
| :-- | :-- | :-- | :-- |
| **主框架 / 多 Tab** | 应用骨架，底部 Tab 切换 | `Scaffold` + `HorizontalPager` + `NavigationBar` + 返回键回首个 Tab | `references/scenario-shell.md` |
| **首页 / 仪表盘** | 一屏说清状态 + 入口 | 状态卡(140dp) → 快捷入口行 → 信息卡 → 入口列表 | `references/scenario-home.md` |
| **通用列表** | 一批同类数据 | 行列表 / 卡片列表 / 内容卡 / 网格 | `references/scenario-list.md` |
| **信息展示** | 关于页、设备信息、详情 | Hero(视差) → 只读信息卡 → 操作 | `references/scenario-info.md` |
| **表单 / 编辑** | 输入并提交 | 顶栏 `Close` 取消 + `Ok` 保存 → `Card` 包 `TextField` → 错误提示 | `references/scenario-form.md` |
| **状态页** | 空 / 加载 / 错误 / 权限 / 引导 | 整页居中插画 + 文案（+ 重试/主操作） | `references/scenario-state.md` |
| **自适应** | 大屏 / 横屏 / 折叠屏 / RTL | 宽度分级判定 → 单/双窗格、`NavigationRail`、内容限宽 | `references/scenario-adaptive.md` |
| **引导与权限** | 多步向导、权限申请、首次使用 | Pager 向导 + 权限四态 + 可关闭提示卡 | `references/scenario-onboarding.md` |
| **设置页** | `XxxPreference` 分组 | `SmallTitle` + `Card(12.dp)` + Preference | `references/preferences.md` |

场景是**叠加**的：任何页面 = 主框架（若有 Tab）+ 场景内容 + 状态页分支。
所有场景共享的页面骨架、间距栅格、状态与跳转约定、**import 基线与状态层契约**见 `references/scenarios.md`（**先读它**；§5 是片段约定）。

---

## 4. 组件选型速查

| 需求 | 用这个 | 不要用 |
| :-- | :-- | :-- |
| 页面容器 | `Scaffold` | `Column` + `verticalScroll` |
| 顶栏（大标题折叠） | `TopAppBar` | — |
| 顶栏（固定小标题） | `SmallTopAppBar` | — |
| 底栏导航 | `NavigationBar` + `NavigationBarItem` | Material `NavigationBar` |
| 大屏侧边导航 | `NavigationRail` + `NavigationRailItem` | — |
| 设置项（开关/箭头/复选/单选/滑块） | `SwitchPreference` / `ArrowPreference` / `CheckboxPreference` / `RadioButtonPreference` / `SliderPreference` | 手搓 `Row { Text(); Switch() }` |
| 下拉选择（设置项内） | `OverlaySpinnerPreference`（高级）/ `OverlayDropdownPreference`（简单） | Material `ExposedDropdownMenu` |
| 自定义设置行 | `BasicComponent` | 手搓 `Row` |
| 内容卡片 / 分组容器 | `Card` | Material `Card` |
| 状态卡（正常/注意/异常） | `StatusCard` 模式：整卡换底 + 出血圆环图标（`references/status-colors.md`） | 给每个入口配一种颜色 |
| 快捷入口 / 列表图标底 | `tertiaryContainer` + `primary`，或 `surfaceContainerHigh` + `onSurface` | 绿/黄/红/紫各来一个 |
| 分组标题 | `SmallTitle` | `Text(style = title3)` |
| 分隔线 | `HorizontalDivider` / `VerticalDivider` | Material `Divider` |
| 按钮 | `Button` / `TextButton` | Material `Button` |
| 图标按钮 | `IconButton` + `Icon` | — |
| 图标 | `MiuixIcons.*` | Material Icons |
| 文本 | `Text`（默认样式即 `textStyles.main`） | Material `Text` |
| 输入框 | `TextField` | Material `OutlinedTextField` |
| 搜索 | `SearchBar` + `InputField` | — |
| 标签页 | `TabRow` | Material `TabRow` |
| 对话框 | `OverlayDialog` | Material `AlertDialog` |
| 底部抽屉 | `OverlayBottomSheet` | Material `ModalBottomSheet` |
| 列表弹窗 / 动作菜单 | `OverlayListPopup` / `OverlayDropdownMenu` / `OverlayIconDropdownMenu` | Material `DropdownMenu` |
| 提示条 | `SnackbarHost` + `SnackbarHostState` | Material `Snackbar` |
| 加载中 | `InfiniteProgressIndicator` / `CircularProgressIndicator` / `LinearProgressIndicator` | Material 同名 |
| 刷新 | `PullToRefresh` | Material `PullToRefresh` |
| 空状态 / 加载占位 | 居中 `Column` + `MiuixIcons.*` + `Text(onBackgroundVariant)` | — |
| 悬浮按钮 / 工具条 | `FloatingActionButton` / `FloatingToolbar` | Material `FAB` |
| 角标 | `Badge` / `BadgedBox` | Material `Badge` |
| 模糊背景 | `miuix-blur`：`layerBackdrop` / `textureBlur` / `rememberLayerBackdrop` | — |
| 平滑圆角 | `squircleSurface` / `squircleBorder` / `squircleClip` | `clip(RoundedCornerShape())` |
| 导航 | `miuix-nav`：`rememberNavBackStack` + `NavDisplay` + `entry<T>` | — |

完整签名与参数见 `references/components.md`；真实调用示例见 `references/recipes.md`。

---

## 5. 工作流

做任何 miuix 界面任务时按顺序执行：

1. **确认版本**：读项目的 `gradle/libs.versions.toml` / `build.gradle.kts`，确认 miuix 版本与已引入的模块。
   - `>= 0.9.0`：用本 skill 的现代 API。
   - `< 0.9.0`（旧项目，如 `extra.SuperSwitch`、`miuix.kmp.extra.*`）：**先读 `references/pitfalls.md` 的迁移表**，在旧项目里沿用旧 API 不要混用；新写的代码不要用旧 API。
   - 没有 miuix：按 `references/setup.md` 引入（当前最新 `0.9.4`）。
2. **定场景**：读 `references/scenarios.md`，把需求归到第 3 节表格里的某个通用场景，然后打开对应场景文件。
3. **读参考**：按需读 `references/` 下对应文件，**不要凭记忆写 API**。
4. **抄骨架**：从场景文件里挑最接近的骨架（首页 / 列表 / 信息 / 表单 / 状态页 / 主框架 / 设置页），复制后只改业务部分。
5. **写代码**：布局数值、`Modifier` 顺序、`Defaults` 引用全部照抄。
6. **自检**：跑第 6 节清单；能跑的话加跑 `scripts/miuix-lint.mjs`。

### 参考文件索引

**按场景（先读）**

| 文件 | 内容 |
| :-- | :-- |
| `references/scenarios.md` | 场景总纲：6 个场景索引 + 选择决策树 + 六个场景共享的骨架/间距/状态/跳转/无障碍/深色/性能约定 |
| `references/scenario-shell.md` | 主框架 / 多 Tab：`MainPagerState` 同步层、返回键回首个 Tab、底栏显隐、悬浮底栏、宽屏 rail |
| `references/scenario-home.md` | 首页 / 仪表盘：状态卡(140dp 语义底色+装饰图标)、快捷入口行、信息卡、入口列表、环境引导弹窗 |
| `references/scenario-list.md` | 通用列表：行/卡片/内容卡/网格四种粒度、分组、搜索、多选、加载更多、下拉刷新、操作槽 |
| `references/scenario-info.md` | 信息展示：关于页 Hero 视差、只读信息卡（竖排/同行）、详情页、调试组、警告卡、复制到剪贴板 |
| `references/scenario-form.md` | 表单 / 编辑：`Close`+`Ok` 顶栏、字段与校验、提交反馈、草稿保护、键盘处理 |
| `references/scenario-state.md` | 状态页：加载/空（三种空）/错误/环境权限/引导、状态优先级 |
| `references/scenario-adaptive.md` | 自适应：宽度分级阈值（官方 `AdaptiveUtils.kt`）、双窗格 list-detail、rail、限宽、折叠屏、RTL |
| `references/scenario-onboarding.md` | 引导与权限：多步向导、权限四态与跳设置页、可关闭提示卡、完成标记持久化、与状态页的协作顺序 |
| `references/status-colors.md` | **状态卡的红/黄/绿**：Miuix 为什么没有 success/warning token、四个项目的取值、成对定义规则、其余界面「黑白 + 强调色」清单 |
| `references/showcase.md` | **可运行样张**：11 个对 0.9.4 编译通过的页面源码，依赖、覆盖范围、复制时改什么 |

**按主题**

| 文件 | 内容 |
| :-- | :-- |
| `references/setup.md` | 依赖坐标、模块选择、Gradle 配置、平台注意事项、项目结构建议 |
| `references/theme.md` | `MiuixTheme` 两种用法、`ThemeController`、动态取色、完整颜色表、文本样式表、自定义 |
| `references/layout.md` | 页面骨架、栅格与间距、顶栏/底栏/大屏、insets、列表、状态页 |
| `references/components.md` | **基础组件**（`basic.*`）的签名、关键参数、Defaults 值、最小示例，含全组件默认值一览表 |
| `references/components-overlay.md` | **浮层**：`overlay.*` / `window.*` 两族、下拉数据结构、`OverlayDropdownPopup`/`Dialog`、两族选型对照 |
| `references/preferences.md` | 设置页体系：`BasicComponent` / 各 `XxxPreference` / 分组与分割线 / 自定义 Preference |
| `references/navigation.md` | `miuix-nav` 完整用法 + 与 androidx navigation / 自研导航的取舍 |
| `references/icons.md` | 图标体系、粗细、全部可用图标名、自定义图标 |
| `references/effects.md` | squircle、blur/backdrop、overscroll、haptic、pressable、色彩组件 |
| `references/recipes.md` | 横切配方：开关联动、确认/输入弹窗、底部抽屉、下拉选择、顶栏动作菜单、下拉刷新、项目级可复用容器、Material 迁移映射 |
| `references/pitfalls.md` | 反模式清单、常见编译/运行错误、旧版本 API 迁移表、版本差异 |

> 参考文件里出现的 `/root/miuix/repos/...` 绝对路径是**产出机器的 provenance**，换机器即失效；它们是「结论出处」不是「必读清单」。

---

## 6. 交付前自检清单

- [ ] 没有任何 `androidx.compose.material3` / `material.icons` 导入
- [ ] 没有任何硬编码颜色（除有注释的理由）
- [ ] 所有文本颜色/样式来自 `MiuixTheme`
- [ ] 页面是 `Scaffold` + `TopAppBar`（有 `scrollBehavior`）+ `LazyColumn`（有 `overScrollVertical` + `scrollEndHaptic` + `nestedScroll` + `overscrollEffect = null`）
- [ ] 分组是 `SmallTitle` + `Card(padding(horizontal = 12.dp).padding(bottom = 12.dp))`
- [ ] 组内分割线是 `padding(horizontal = 16.dp)`（不是全宽通铺）
- [ ] 设置项用的是 `XxxPreference`，不是手搓 `Row`
- [ ] 所有 `Overlay*` 都在 `Scaffold` 内，且不在 `LazyColumn` 的 `item {}` 里
- [ ] 图标逐个 import 了 `top.yukonga.miuix.kmp.icon.extended.<Name>`
- [ ] 列表 `items` 有稳定 `key`；可变状态用 `rememberSaveable` 或 ViewModel
- [ ] 深色模式下没有写死的浅色/深色假设（没有 `isSystemInDarkTheme()` 分支里的硬编码色）
- [ ] 除状态卡外，一屏只有黑白灰 + 一个强调色；主按钮显式传了 `primary`（默认灰底）
- [ ] 没有任何 `Preference` 被塞进另一个 `Preference` 的 `endActions`/`startAction`（运行时会崩）
- [ ] 字符串走资源文件 / `stringResource`（Android 项目）
- [ ] 新增的可组合函数参数顺序：必填回调 → `modifier` → Boolean → 视觉参数（带 `Defaults`） → `content` 放最后

---

## 7. 工具

```bash
node scripts/miuix-lint.mjs <项目或源码目录> [更多目录…] [--quiet]
```

零依赖 Node 脚本，输出 `文件:行` + 修法。**只有 `E2`/`E3` 是 error**（会编译失败或 API 已移除），其余是风格建议：

| 码 | 检查 |
| :-- | :-- |
| `E2` | 已移除的旧版 miuix API（附迁移建议） |
| `E3` | `MiuixIcons.X` 缺少 import（编译错误） |
| `M1` | `androidx.compose.material3` / material icons 残留 |
| `W1` | 硬编码颜色（`Color(0xFF…)`、`Color.Black` 等） |
| `W2` | `TopAppBar` 未传 `scrollBehavior` |
| `W3` | `LazyColumn` 等缺滚动修饰符 / 未设 `overscrollEffect = null` |
| `W4` | `Overlay*` 缺 `Scaffold` 宿主（**跨文件判定**，见下） |
| `W5` | `Card` 横向 padding 不在 {12, 16} 内 |
| `W6` | 分组 `Card` 内 Preference 行之间的分割线没有横向内边距 |
| `W7` | 已有 `TopAppBar` 却又用 `statusBarsPadding()` |
| `W8` | 有色容器里 `Icon` 缺 `tint`（浅色变黑 / 深色变白，见 `components.md` §0） |
| `W9` | `Preference` 的 `endActions`/`startAction` 里又嵌套了 `Preference`（`maxIntrinsicHeight` 会抛 `IllegalArgumentException`） |

### 豁免

任意检查都可以压掉：行内 `// miuix-lint:allow`，或按检查项 `// miuix-lint:allow-<码>`（如 `// miuix-lint:allow-scaffold`，作用于整个文件）。

### `W4` 是跨文件判定的

`Overlay*` 的 `requiresScaffoldHost = true`，但宿主可以在**别的文件**里（很多项目写一个共享页面容器）。所以它会先预扫描整个被扫描目录，识别「共享 Scaffold 宿主」——名字含 `Scaffold` 的可组合函数，以及任何声明于含 `Scaffold(` 文件中的函数——**确认项目里不存在任何 Scaffold 容器**时才报。被跳过的数量会打印出来，不静默：

```
W4 跳过 10 处 Overlay*：10 处因检出共享 Scaffold 宿主（MiuixPageScaffold）。
```

容器在**未被扫描的模块**里时（例如只传了 `app/` 而容器在 `:core:ui`），把两个目录都传给脚本，或在文件里加 `// miuix-lint:allow-scaffold`。`window.Window*` 族 `requiresScaffoldHost = false`，本来就不检查。
