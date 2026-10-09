# 通用场景总纲

Miuix 应用的界面可以归结为 **8 个通用场景**（另加一篇设置页）。先在这里看共享约定，再去 `SKILL.md` §3 的路由表选场景文件。

> ⚠️ **provenance 诚实声明**：本 skill 的结构约定**不是「8 个项目的一致共识」**——经第三方复核，真实成分是
> **一份现代 InstallerX / lyricon 风格指南**，加少量真正的库约束。来源等级见 `SKILL.md` §2 的三张表
>（库默认值 / 多数派惯例 / 单项目偏好 / 已被证伪）。凡标为「单项目偏好」的写法，跟随你项目的既有风格即可。

**路由表只有一份，在 `SKILL.md` §3「按通用场景选结构」** —— 本文件不重复它，避免两处措辞漂移。

设置页（`XxxPreference` 分组）见 `preferences.md`，它是「通用列表」的一个特化，但规则更硬，所以单独成篇。

---

## 1. 选择决策树

```
这个界面主要是干什么的？
├─ 承载其它页面 / 底部有 Tab 切换 ──────────────→ 场景：主框架
├─ 一屏说清「现在怎么样」+ 给几个入口 ───────────→ 场景：首页
├─ 展示一批同类数据 ────────────────────────────→ 场景：通用列表
│   ├─ 每行是一个设置项（开关/箭头/下拉）→ preferences.md（更硬的规则）
│   ├─ 每行是「图标 + 标题 + 副标题 + 操作」→ 通用列表 §3 卡片列表
│   └─ 每行是大段内容 ──────────────────→ 通用列表 §4 内容卡
├─ 只读地告诉用户「这是什么」────────────────→ 场景：信息展示
├─ 用户要输入并提交 ───────────────────────────→ 场景：表单
└─ 没有数据 / 加载中 / 出错 / 没权限 / 首次使用 → 场景：状态页
```

**场景是叠加的**：任何一个「有内容的页面」= 主框架（若有 Tab）+ 场景内容 + 状态页分支。
状态判断永远优先于内容渲染（见 `scenario-state.md` §6 的优先级顺序）。

---

## 2. 所有场景共享的通用约定

### 2.1 页面骨架（所有场景一致）

```kotlin
val scrollBehavior = MiuixScrollBehavior()

Scaffold(
    topBar = { /* TopAppBar / SmallTopAppBar */ },
    bottomBar = { /* 仅主框架场景有 */ },
    snackbarHost = { SnackbarHost(snackbarHostState) },
) { paddingValues ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            // 下面三个都可选：overScrollVertical 只在「内容未超出容器」时才需要
            // （MiuixTheme 已通过 LocalOverscrollFactory 全局装了回弹）；
            // scrollEndHaptic 可选且位置自由；只有「overScrollVertical 在 nestedScroll 之前」有文档依据。
            .overScrollVertical()
            .scrollEndHaptic()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
        overscrollEffect = null,   // 避免与 Miuix 回弹叠加
    ) { /* 场景内容 */ }
}
```

- **完整可拷贝的整页代码**：设置页见 `preferences.md` §1；其余场景见各自的 `scenario-*.md`。
- 首页/信息页的 Hero 区也放在 `LazyColumn` 里（`item { Box(Modifier.height(...)) }` 占位），**不要**在 `LazyColumn` 外面套 `Column`。
- 无 `TopAppBar` 的页面用 `contentPadding = paddingValues`。

### 2.2 间距栅格（唯一一套数值）

| 用途 | 数值 | 依据 |
| :-- | :-- | :-- |
| 分组/卡片横向外边距 | `12.dp` | 8 个项目里 125 处，压倒性共识 |
| 卡片纵向分组间距 | `12.dp`（`padding(bottom = 12.dp)`） | 同上 |
| 卡片内边距 | `16.dp`（`BasicComponentDefaults.InsideMargin`，Preference 自带） | 库默认值 |
| 分组标题缩进 | `28.dp` = 12 + 16，**与卡片文字严格对齐** | `SmallTitleDefaults.InsideMargin` |
| 列表首尾呼吸位 | `Spacer(Modifier.size(12.dp))` | 各项目一致 |
| 组内分割线横向缩进 | `16.dp` | 与卡片内文字对齐 |
| 内容卡（非分组）内边距 | `16.dp` | 各项目一致 |
| 顶栏标题缩进 | `26.dp` | `TopAppBarDefaults.TitlePadding` |

**例外**：部分项目的「首页大卡 / 信息卡」用 `16.dp` 横向外边距（lyricon 的 `HomeTab`、`AboutScreen`）。
允许，但**同一项目内必须统一**：要么全 12.dp，要么「分组 Preference 列表用 12.dp，自由内容卡用 16.dp」。
不确定时用 **12.dp**。

### 2.3 状态与数据流

| 项 | 约定 |
| :-- | :-- |
| 可跨配置变更的状态 | `rememberSaveable` |
| 业务状态 | ViewModel + `collectAsStateWithLifecycle()` |
| 页面级一次性动作 | `LaunchedEffect(key)` |
| 列表项的 key | `items(list, key = { it.id })`，**必须** |
| 派生数据 | `remember(dep) { derive(dep) }` 或 `derivedStateOf` |
| 弹层状态 | `var show by remember { mutableStateOf(false) }`，`Overlay*` 由它驱动 |

### 2.4 跳转与返回

| 项 | 约定 |
| :-- | :-- |
| 进入下一屏 | `navigator.push(Route.X)`（幂等，见 `navigation.md` §2） |
| 返回 | 顶栏 `IconButton { Icon(MiuixIcons.Back, "返回") }` → `navigator.pop()` |
| 二级页顶栏 | `SmallTopAppBar`（固定标题）或 `TopAppBar`（要大标题折叠） |
| 主框架的 Tab 页 | **没有返回按钮**，顶栏左侧留空或放 Logo |
| 系统返回 | 交给 `miuix-nav`；仅主框架场景需要额外处理「非首个 Tab 先回 Tab 0」 |

### 2.5 内容语义

- 主文本 `MiuixTheme.textStyles.main`（17sp，`Text` 默认）；次级文本 `onSurfaceVariantSummary`；弱化文本 `onBackgroundVariant`。
- 分区标题用 `SmallTitle`，不要用 `Text(style = title3)` 代替（缩进会错）。
- 强调用 `fontWeight = FontWeight.SemiBold/Bold` + 语义色，不要用背景渐变。
- 危险操作用 `BasicComponentDefaults.titleColor(color = colorScheme.error)` 或 `colorScheme.error` 图标色。

### 2.6 无障碍

- 纯图标按钮必须有 `contentDescription`；行内装饰图标用 `null`。
- 触控目标 ≥ 40.dp（`IconButtonDefaults.MinWidth/MinHeight`）。
- 不要用 `onBackgroundVariant` 承载关键信息（它是弱化色）。
- 文案走 `stringResource`。

### 2.7 深色模式

- 只用语义色，就不需要任何 `isSystemInDarkTheme()` 分支。
- 确实需要两套色（品牌色 / 状态卡底色）时，写成 `if (isDark) A else B` 并**同时**给出前景色，参考 `scenario-home.md` §3 的状态卡。
- 需要判断当前是否深色：`MiuixTheme.colorSchemeMode`，或用项目封装的 `isInDarkTheme()`（定义见 `theme.md`「判断当前是否深色」，也可用 `MiuixTheme.colorScheme.background.luminance() < 0.5f` 反推）。

### 2.8 性能

- 长列表必须有稳定 `key`；需要动画加 `Modifier.animateItem()`。
- `Overlay*` 弹层放在 `LazyColumn` 之外（不要放 `item {}`）。
- 模糊（`textureBlur`）只给固定数量的元素（顶栏/底栏/悬浮条），不要给列表项。
- 集合用 `kotlinx.collections.immutable` 的 `ImmutableList`/`PersistentList` 更利于 Compose 跳过重组。

---

## 3. 场景组合示例

一个典型的「Xposed 模块」类应用：

```
主框架（4 个 Tab）
├─ Tab 0 首页       → 首页场景（状态卡 + 快捷入口 + 信息卡 + 入口列表）
├─ Tab 1 功能       → 通用列表场景（分组卡片列表 + 开关）
├─ Tab 2 外观/自定义 → 表单场景（滑块 + 颜色选择 + 预览）
└─ Tab 3 设置/关于  → 设置场景（preferences.md）+ 信息展示场景（关于分组）
        └─ 二级页：关于 / 更新 / 历史 → 信息展示 或 通用列表
        └─ 每屏按需叠加状态页场景（未激活 / 未授权 / 加载中 / 空数据）
```

一个典型的「工具」类应用：

```
主框架（2 个 Tab 或单页）
├─ 首页 → 首页场景（状态卡改为「任务状态」，快捷入口改为「选择文件/开始」）
└─ 历史 → 通用列表场景（卡片列表 + 空态 + 下拉刷新）
```

---

## 4. 落地检查（所有场景通用）

- [ ] 用了 `Scaffold`，而不是裸 `Column`
- [ ] `TopAppBar` 传了 `scrollBehavior`
- [ ] `LazyColumn` 有四件套：`overScrollVertical()` + `scrollEndHaptic()` + `nestedScroll(...)` + `overscrollEffect = null`
- [ ] 横向间距在 `Card` 上（12.dp），不在 `LazyColumn` 上
- [ ] 分组标题是 `SmallTitle`；分割线有 `padding(horizontal = 16.dp)`
- [ ] 列表 `items` 有 `key`
- [ ] `Overlay*` 在 `Scaffold` 内、`LazyColumn` 外
- [ ] 颜色/文本样式来自 `MiuixTheme`
- [ ] 没有 `androidx.compose.material3` 导入
- [ ] 图标逐个 import
- [ ] 深色模式下过了一遍
- [ ] 空 / 加载 / 错误 / 权限 四态都有对应 UI（哪怕只是一行文案）

---

## 5. 片段约定（读任何场景文件前先看这一节）

场景文件里的代码片段是**结构示意**，省略了与本 skill 无关的项目代码。三条约定：

### 5.1 import 基线

片段默认你已经导入以下内容（片段本身不再重复 import）：

```kotlin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
```

**图标必须各自单独 import**（它们是扩展属性），例如 `import top.yukonga.miuix.kmp.icon.extended.Back`。片段里不逐条列出图标 import。

Preferences 相关（用到 `XxxPreference` 时）：`import top.yukonga.miuix.kmp.preference.SwitchPreference` 等，按需添加。

### 5.2 状态层契约

片段里的 `state` / `viewModel` / `onXxx` 回调都假设来自下面这套结构。照着搭即可：

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Ready<T>(val data: T) : UiState<T>
}

class SettingsViewModel : ViewModel() {
    private val _state = MutableStateFlow<UiState<SettingsData>>(UiState.Loading)
    val state: StateFlow<UiState<SettingsData>> = _state.asStateFlow()

    // 一次性提示（Snackbar）走独立通道，不要混进 UiState
    private val _toast = Channel<String>(Channel.BUFFERED)
    val toast: Flow<String> = _toast.receiveAsFlow()

    fun setDarkMode(value: Boolean) { /* … */ }
    fun reload() { /* … */ }
}

// Composable 侧
@Composable
fun SettingsRoute(viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toast.collect { snackbarHostState.showSnackbar(it) }
    }

    when (val s = state) {
        UiState.Loading -> LoadingState()
        UiState.Empty -> EmptyState(kind = EmptyKind.NoData, onAction = viewModel::reload)
        is UiState.Error -> ErrorState(s.message, onRetry = viewModel::reload)
        is UiState.Ready -> SettingsContent(s.data, viewModel::setDarkMode)
    }
}
```

依赖：`lifecycle-viewmodel-compose`（KMP 用 `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`）、
`lifecycle-runtime-compose`（提供 `collectAsStateWithLifecycle`）、`kotlinx-coroutines-core`（`Channel` / `Flow`）。

### 5.3 占位符约定（这些名字是占位符，不是 miuix API）

片段里出现的下列名字**没有在本 skill 中定义实现**，它们代表「你项目自己的导航/工具函数」，按项目实际情况实现：

| 占位符 | 含义 | 最小实现示例 |
| :-- | :-- | :-- |
| `navigator`、`onBack`、`onOpenXxx()` | 导航，见 `navigation.md` | `navigator.push(Route.X)` / `navigator.pop()` |
| `openUpdate()`、`openLicenses()`、`onOpenAbout` | 跳转到对应页面 | `navigator.push(Route.Update)` |
| `openLSPosed()` | 跳系统或第三方应用 | `context.startActivity(Intent("…"))`，需 `try/catch` 兜底 `ActivityNotFoundException` |
| `copyToClipboard(text)` | 复制到剪贴板 | 见下方 `rememberCopyToClipboard()` |
| `uriHandler.openUri(url)` | 打开链接 | Compose 自带的 `LocalUriHandler.current` |
| `themeItems`、`themeOptions` | 下拉选项数据 | `remember { listOf("跟随系统", "浅色", "深色") }` |
| `deviceInfoItems()` | 只读信息列表 | 返回 `List<Pair<String, String>>`，内容取自 `Build.*` |
| `share(item)`、`sortByName()`、`onDelete(item)` | 业务动作 | 项目自己的 ViewModel 方法 |
| `Item`（在 `items`、`state.items` 里） | 你的数据模型 | `data class Item(val id: String, …)`；**`id` 是列表 `key` 所必需的** |

```kotlin
// 复制到剪贴板的最小实现（片段里的 copyToClipboard 指的就是它）
@Composable
fun rememberCopyToClipboard(): (String) -> Unit {
    val clipboard = LocalClipboardManager.current
    return remember(clipboard) {
        { text -> clipboard.setText(AnnotatedString(text)) }
    }
}
```

需要 `import androidx.compose.ui.platform.LocalClipboardManager` 与 `androidx.compose.ui.text.AnnotatedString`。
（新版本 Compose 已把 `LocalClipboardManager` 标为过时，改用 `LocalClipboard` 即可，语义相同。）

### 5.4 场景文件不自带 import 的原因

14 个场景文件如果各自贴一遍 import，既冗长又会漂移。所以约定：**import 基线在上面的 5.1，图标逐个 import，其余按需**。
如果你要「整页拷贝」，请从 `preferences.md` §1（设置页）或对应 `scenario-*.md` 的完整示例开始，再对照 5.1 补 import。
