# 布局：页面骨架、栅格与结构

## 1. `Scaffold`

```kotlin
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    floatingToolbar: @Composable () -> Unit = {},
    floatingToolbarPosition: ToolbarPosition = ToolbarPosition.BottomCenter,
    snackbarHost: @Composable () -> Unit = {},
    popupHost: @Composable () -> Unit = { MiuixPopupHost() },
    containerColor: Color = MiuixTheme.colorScheme.surface,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
    content: @Composable (PaddingValues) -> Unit,
)
```

**`Scaffold` 同时是弹出层容器。** `OverlayDialog`、`OverlayBottomSheet`、`Overlay*ListPopup`、`Overlay*DropdownMenu`、`Overlay*Preference` 全部依赖它；调用点必须在某个 `Scaffold` 的 composition 树内。

- 每个 `Scaffold` 自动管理**独立**的弹层状态，多个并列/嵌套 `Scaffold` 无需配置 `popupHost`。
- `FabPosition`：`Start` / `Center` / `End`（都在底栏之上）/ `EndOverlay`（覆盖在底栏上）。
- `ToolbarPosition`：`TopStart` / `CenterStart` / `BottomStart` / `TopEnd` / `CenterEnd` / `BottomEnd` / `TopCenter` / `BottomCenter`。

## 2. 三种页面骨架

### 2.1 设置页 / 列表页（最常用）

```kotlin
val scrollBehavior = MiuixScrollBehavior()

Scaffold(
    topBar = {
        TopAppBar(
            title = "标题",
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(MiuixIcons.Back, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = { /* ... */ }) {
                    Icon(MiuixIcons.Settings, contentDescription = "设置")
                }
            },
            scrollBehavior = scrollBehavior,
        )
    },
) { paddingValues ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .scrollEndHaptic()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
        overscrollEffect = null,
    ) {
        item { Spacer(Modifier.size(12.dp)) }
        item { SmallTitle("分组一") }
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                SwitchPreference(title = "开关", checked = on, onCheckedChange = { })
            }
        }
        // 更多分组...
        item { Spacer(Modifier.size(12.dp)) } // 底部呼吸位
    }
}
```

关键点：

- `contentPadding` **只取 `top`**（底部由最后那个 `Spacer` + 系统 insets 处理；需要精确时再加 `bottom = paddingValues.calculateBottomPadding()`）。
- **不要**给 `LazyColumn` 加 `horizontal` padding —— 横向间距由 `Card` 的 `padding(horizontal = 12.dp)` 提供，这样分割线/大标题缩进才对齐。
- `Modifier` 顺序固定：`fillMaxSize()` → `overScrollVertical()` → `scrollEndHaptic()` → `nestedScroll(...)`。`nestedScroll` 必须在越界回弹之后（官方文档明确要求）。

### 2.2 详情页 / 表单页

同上，把 `Card` 内的 Preference 换成 `TextField` + `Button`：

```kotlin
item {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = "名称",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
item {
    Button(
        onClick = onSave,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Text("保存")
    }
}
```

### 2.3 无顶栏页（首页 / 单屏）

```kotlin
Scaffold { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { /* ... */ }
}
```

## 3. 顶部栏

### `TopAppBar`（可折叠大标题）

```kotlin
fun TopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    largeTitle: String = title,
    largeTitleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitle: String = "",
    subtitleColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: ScrollBehavior? = null,
    defaultWindowInsetsPadding: Boolean = true,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,        // 26.dp
    navigationIconPadding: Dp = TopAppBarDefaults.NavigationIconPadding, // 16.dp
    actionIconPadding: Dp = TopAppBarDefaults.ActionIconPadding,         // 16.dp
    bottomContent: @Composable () -> Unit = {},
)
```

- `title` = 折叠后的小标题，`largeTitle` = 展开时的大标题（默认与 `title` 相同）。
- **必须**传 `scrollBehavior`，否则不折叠、不吸附。
- `TopAppBarDefaults`：`TitlePadding = 26.dp`、`NavigationIconPadding = 16.dp`、`ActionIconPadding = 16.dp`、`CollapsedHeight = 52.dp`、`SmallTopAppBarCenterHeight = 50.dp`、`LargeTitleBottomPadding = 4.dp`、`SubtitleBottomPadding = 8.dp`。

### `SmallTopAppBar`（固定单行标题）

```kotlin
fun SmallTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitle: String = "",
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: ScrollBehavior? = null,
    defaultWindowInsetsPadding: Boolean = true,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    navigationIconPadding: Dp = TopAppBarDefaults.NavigationIconPadding,
    actionIconPadding: Dp = TopAppBarDefaults.ActionIconPadding,
    bottomContent: @Composable () -> Unit = {},
)
```

### `MiuixScrollBehavior`

```kotlin
@Composable
fun MiuixScrollBehavior(
    state: TopAppBarState = rememberTopAppBarState(),
    canScroll: () -> Boolean = { true },
    snapAnimationSpec: AnimationSpec<Float>? = spring(stiffness = 2500f),
    flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
): ScrollBehavior
```

`ScrollBehavior` 暴露 `state.collapsedFraction`（0f–1f），可用它做模糊/透明度渐变（真实项目做法，见 `effects.md`）。

### 返回按钮

标准写法（8 个项目一致）：

```kotlin
navigationIcon = {
    IconButton(onClick = { navigator.pop() }) {
        Icon(MiuixIcons.Back, contentDescription = "返回")
    }
}
```

RTL 时需要翻转方向图标（官方约定）：

```kotlin
val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
Icon(
    imageVector = MiuixIcons.Back,
    contentDescription = "返回",
    modifier = Modifier.graphicsLayer { scaleX = if (isRtl) -1f else 1f },
)
```

## 4. 底部导航

```kotlin
val items = listOf(
    NavigationItem("首页", MiuixIcons.Home),
    NavigationItem("设置", MiuixIcons.Settings),
)
var selected by rememberSaveable { mutableIntStateOf(0) }

Scaffold(
    bottomBar = {
        NavigationBar {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = selected == index,
                    onClick = { selected = index },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
    },
) { /* ... */ }
```

- `NavigationItem(label: String, icon: ImageVector)` 是数据类。
- `NavigationBarItem` 是 `RowScope` 扩展，**必须在 `NavigationBar` 的 lambda 内调用**。
- `NavigationBarDisplayMode`：`IconAndText`（默认）/ `IconOnly` / `IconWithSelectedLabel`，通过 `NavigationBar(mode = ...)` 传。
- 角标：`NavigationBarItem(badge = { Badge { Text("8") } })`。
- 大屏用 `NavigationRail`：

```kotlin
val railState = rememberNavigationRailState(NavigationRailValue.Expanded)
NavigationRail(state = railState) {
    items.forEachIndexed { index, item ->
        NavigationRailItem(
            selected = selected == index,
            onClick = { selected = index },
            icon = item.icon,
            label = item.label,
        )
    }
}
```

> ⚠️ **0.9 之前的旧 API 是 `NavigationBar(items = ..., selected = ..., onClick = ...)`（无 `NavigationBarItem`）。** 旧项目沿用旧写法；新项目不要用。

## 5. 窗口 insets

| 场景 | 做法 |
| :-- | :-- |
| 有 `TopAppBar` | 顶栏自己处理顶部 inset，内容只 `paddingValues.calculateTopPadding()`；**不要**再加 `statusBarsPadding()` |
| 无 `TopAppBar` | 用 `contentPadding = paddingValues` 或 `Modifier.padding(paddingValues)` |
| 需要裁剪 `displayCutout` | `Scaffold(contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout))` |
| 大屏双栏，右侧窗格贴着 rail | 用 `WindowInsets.displayCutout.exclude(WindowInsets.displayCutout.only(WindowInsetsSides.Start))` 去掉 rail 已消费的 start 侧缺口 |
| 键盘弹起 | `Modifier.imePadding()`。**放哪取决于项目**：InstallerX 放 `Scaffold`，Updater-KMP 0.9.4 与官方 example 放 `LazyColumn`/pager 的 modifier。唯一硬要求是**不要同时放两处**（会双倍上移）。详见 `scenario-form.md` §6 |
| 横向安全区（横屏刘海） | `contentPadding` 里用 `horizontalSafeInsets.calculateStartPadding(layoutDirection)` / `calculateEndPadding(layoutDirection)` |

## 6. 列表与分组

### 标准分组（必须照抄）

```kotlin
item { SmallTitle("通知") }
item {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        SwitchPreference(
            title = "允许通知",
            summary = "关闭后不再弹出提醒",
            checked = allow,
            onCheckedChange = { allow = it },
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        ArrowPreference(title = "通知渠道", summary = "3 个", onClick = { /* ... */ })
    }
}
```

- **组内最后一个元素后面不要加分隔线。**
- 组内条目多时，用 `forEachIndexed` + `if (index < lastIndex) HorizontalDivider(...)`。
- **不要**在 `Card` 上再加 `cornerRadius`（默认 `CardDefaults.CornerRadius = 16.dp` 即设计值）。

### `SmallTitle`

```kotlin
fun SmallTitle(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    insideMargin: PaddingValues = SmallTitleDefaults.InsideMargin, // PaddingValues(28.dp, 8.dp)
)
```

需要加大分组间距时（真实项目做法）：

```kotlin
SmallTitle(
    text = category.name,
    insideMargin = PaddingValues(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 8.dp),
)
```

> `28.dp` = 卡片 12.dp 外边距 + 卡片内 16.dp，**与卡片内文字严格对齐**。改这个值会让标题和内容错位。

### 动态列表

```kotlin
items(
    items = list,
    key = { it.id },
) { item ->
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        BasicComponent(
            title = item.title,
            summary = item.summary,
            startAction = { Icon(item.icon, contentDescription = null) },
            onClick = { onOpen(item) },
        )
    }
}
```

- **必须给 `key`**，否则删除/插入时状态错乱。
- 需要动画时加 `Modifier.animateItem()`。

### 分隔线

```kotlin
HorizontalDivider()                                            // 默认 0.75.dp，色 = colorScheme.dividerLine
VerticalDivider()
HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
```

`DividerDefaults.Thickness = 0.75.dp`。

## 7. 大屏自适应（`NavigationRail` 双栏）

`miuix-nav` 刻意不做多窗格；自适应在 `NavDisplay` **外围**组合：

```kotlin
if (isWideScreen) {
    Row(Modifier.fillMaxSize()) {
        NavigationRail(state = railState) { /* NavigationRailItem */ }
        Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
            NavDisplay(backStack = backStack, onBack = { backStack.removeLastOrNull() }) { /* entry */ }
        }
    }
} else {
    NavDisplay(backStack = backStack, onBack = { backStack.removeLastOrNull() }) { /* entry */ }
}
```

三条硬性规则（官方文档明确）：

1. **窗格必须裁剪**（`clipToBounds()`），否则转场图层会画到侧栏上。
2. **窗格内屏幕圆角归零**：`cornerClipRadius = 0.dp`。
3. **去掉侧栏已消费的边缘 insets**。

## 8. 状态页（加载 / 空 / 错误）

```kotlin
// 加载
item {
    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
        InfiniteProgressIndicator()
    }
}

// 空状态
item {
    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = MiuixIcons.Search,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "暂无内容",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        }
    }
}
```

要点：

- 用 `fillParentMaxSize()`（`LazyItemScope` 扩展）让状态页占满视口并居中。
- 用 `MiuixIcons.*` 而不是 Material Icons。
- 空态文案用 `onBackgroundVariant`，不要 `Color.Gray`。
- 页面顶部有 `TopAppBar` 时不需要再单独做加载骨架屏；直接换列表内容即可。

## 9. 下拉刷新

```kotlin
PullToRefresh(
    isRefreshing = isRefreshing,
    onRefresh = { viewModel.refresh() },
    modifier = Modifier.fillMaxSize(),
    topAppBarScrollBehavior = scrollBehavior,
    contentPadding = paddingValues,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .scrollEndHaptic()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        overscrollEffect = null,
    ) { /* ... */ }
}
```

`PullToRefresh` 的关键参数：

| 参数 | 说明 |
| :-- | :-- |
| `isRefreshing` | 是否正在刷新（由 ViewModel 状态驱动） |
| `onRefresh` | 触发刷新的回调 |
| `contentPadding` | 直接传 `Scaffold` 的 `paddingValues` |
| `topAppBarScrollBehavior` | 传页面的 `scrollBehavior`，让顶栏与刷新联动 |
| `refreshTexts` | 四个阶段的提示文案列表，可本地化 |
| `circleSize` | 指示器尺寸，默认 20.dp |
| `onPullProgress` | 下拉进度回调，可做自定义联动 |

## 10. 悬浮元素

```kotlin
Scaffold(
    floatingActionButton = {
        FloatingActionButton(onClick = { /* ... */ }) {
            Icon(MiuixIcons.Add, contentDescription = "添加", tint = MiuixTheme.colorScheme.onPrimary)
        }
    },
    floatingActionButtonPosition = FabPosition.End,
    floatingToolbar = {
        FloatingToolbar(color = MiuixTheme.colorScheme.primary, cornerRadius = 20.dp) {
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { }) { Icon(MiuixIcons.Edit, "编辑") }
                IconButton(onClick = { }) { Icon(MiuixIcons.Delete, "删除") }
            }
        }
    },
    floatingToolbarPosition = ToolbarPosition.BottomCenter,
) { /* ... */ }
```

`FloatingActionButtonDefaults`：`MinWidth = 60.dp`、`MinHeight = 60.dp`、`ShadowElevation = 4.dp`。
`FloatingToolbarDefaults.CornerRadius = 50.dp`（胶囊）；显式传 `20.dp` 等可做方形工具条。
