# 场景 · 自适应（大屏 / 横屏 / 折叠屏 / RTL）

Miuix 本身**不提供尺寸分级 API**——自适应在页面外层自己判断。判定依据直接照抄官方 example 的
`utils/AdaptiveUtils.kt`（这是唯一一处官方给出了具体阈值的地方）：

```kotlin
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity

/** 双窗格（列表 + 详情） */
@Composable
fun shouldShowSplitPane(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    return with(density) {
        val widthDp = windowInfo.containerSize.width.toDp()
        val heightDp = windowInfo.containerSize.height.toDp()
        val ratio = heightDp / widthDp
        widthDp >= 840.dp || (widthDp >= 600.dp && ratio < 1.2f)   // 宽屏，或「够宽且不太瘦长」
    }
}

/** 三窗格 / 展开侧栏 */
@Composable
fun shouldShowThreePanes(): Boolean = with(LocalDensity.current) {
    LocalWindowInfo.current.containerSize.width.toDp() >= 1200.dp
}

/** 侧栏是否默认展开 */
@Composable
fun shouldExpandNavigationRail(): Boolean = shouldShowThreePanes()
```

阈值语义：**< 600dp 单窗格（手机）**、**≥ 600dp 且不瘦长 / ≥ 840dp 双窗格**、**≥ 1200dp 可上三窗格或常驻展开侧栏**。

---

## 1. 单窗格 → 双窗格（list-detail）

这是大屏最常见、也最容易写错的结构。

```kotlin
@Composable
fun AdaptiveListDetail(backStack: NavBackStack, navigator: Navigator) {
    val isWide = shouldShowSplitPane()

    if (isWide) {
        Row(Modifier.fillMaxSize()) {
            // 左侧：常驻列表
            Box(Modifier.width(360.dp).fillMaxHeight()) {
                ItemListPane(
                    selectedId = (backStack.lastOrNull() as? Route.Detail)?.id,
                    onSelect = { id -> backStack.add(Route.Detail(id)) },
                )
            }
            // 右侧：详情
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clipToBounds(),          // ① 必须裁剪
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { navigator.pop() },
                    effects = NavDisplayEffects(
                        cornerClipRadius = 0.dp,       // ② 窗格内屏幕圆角归零
                        backdropColor = MiuixTheme.colorScheme.background,
                    ),
                ) {
                    entry<Route.Empty> { DetailPlaceholder() }   // ③ 占位根路由
                    entry<Route.Detail> { route -> DetailPane(route.id) }
                }
            }
        }
    } else {
        // 单窗格：整页导航
        NavDisplay(backStack = backStack, onBack = { navigator.pop() }) {
            entry<Route.List> { ItemListPaneFull(navigator) }
            entry<Route.Detail> { route -> DetailPane(route.id) }
        }
    }
}
```

### 三条硬性规则（来自 `navigation.md` §9，违反会出错）

| # | 规则 | 后果 |
| :-- | :-- | :-- |
| ① | 窗格必须 `.clipToBounds()` | 转场图层会画到隔壁窗格上 |
| ② | 窗格内 `cornerClipRadius = 0.dp` | 窗格边界在屏幕中间，设备圆角只属于物理屏幕边缘 |
| ③ | 详情窗格必须有一个**占位根路由** | 空返回栈会被 `miuix-nav` 拒绝 |

另外两条：

- **去掉侧栏已消费的 insets**：紧贴 `NavigationRail` 右侧的窗格不应再叠 `displayCutout` 的 start padding。
  用 `WindowInsets.displayCutout.exclude(WindowInsets.displayCutout.only(WindowInsetsSides.Start))`。
- **形态切换要保住状态**：不要迁移路由 key（会丢 `rememberSaveable`/ViewModel），
  而是用 app 级 `movableContentOf` 移动**整个** `NavDisplay`（官方 example 的做法），
  并且**让 `NavDisplay` 保持在同一个 composition 位置**，只让周边 chrome 变。

---

## 2. 大屏用 `NavigationRail` 替代 `NavigationBar`

```kotlin
if (isWide) {
    Row(Modifier.fillMaxSize()) {
        val railState = rememberNavigationRailState(
            initialValue = if (shouldExpandNavigationRail()) NavigationRailValue.Expanded
                   else NavigationRailValue.Collapsed,
        )
        NavigationRail(state = railState) {
            navigationItems.forEachIndexed { index, item ->
                NavigationRailItem(
                    selected = selectedTab == index,
                    onClick = { onSelectTab(index) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) { /* 内容 */ }
    }
} else {
    Scaffold(bottomBar = { NavigationBar { /* NavigationBarItem */ } }) { /* 内容 */ }
}
```

| 组件 | 适用宽度 |
| :-- | :-- |
| `NavigationBar`（底部） | < 600dp |
| `NavigationRail`（`Collapsed`） | 600–1199dp |
| `NavigationRail`（`Expanded`） | ≥ 1200dp |

`NavigationRail` 默认 `minWidth = 80.dp`，`Expanded` 时 `expandedWidth = 240.dp`。

---

## 3. 宽屏内容限宽（否则一行文字 200 个字）

大屏上内容**不要拉满**，要限宽居中。真实项目普遍这么做：

```kotlin
LazyColumn(
    modifier = Modifier
        .fillMaxSize()
        .overScrollVertical()
        .nestedScroll(scrollBehavior.nestedScrollConnection),
    contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
    horizontalAlignment = Alignment.CenterHorizontally,   // 让 item 在宽屏居中
    overscrollEffect = null,
) {
    item {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)      // 限宽：设置页/表单页常用 600–640dp
                .fillMaxWidth(),
        ) {
            // 分组内容
        }
    }
}
```

或者对整块内容套一层：

```kotlin
Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) { /* 内容 */ }
}
```

参考值：Dialog 的 `MaxWidth = 420.dp`、BottomSheet 的 `maxWidth = 640.dp`——表单/设置内容限宽取 **600–640dp** 与它们一致。

---

## 4. 折叠屏

Miuix 没有折叠屏 API，用 androidx 的：

```kotlin
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo

@Composable
fun rememberFoldingFeature(): FoldingFeature? {
    val context = LocalContext.current
    var feature by remember { mutableStateOf<FoldingFeature?>(null) }
    LaunchedEffect(context) {
        WindowInfoTracker.getOrCreate(context)
            .windowLayoutInfo(context as Activity)
            .collect { info: WindowLayoutInfo ->
                feature = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
            }
    }
    return feature
}
```

用法：

- `FoldingFeature.isSeparating == true` → 沿铰链**分屏**：左右两个窗格，中间留出 `occlusion` 宽度。
- `state == FoldingFeature.State.HALF_OPENED` → 上屏内容、下屏控制区（更适合媒体/输入场景）。
- 分屏宽度用 `FoldingFeature.bounds` 与 `orientation` 计算，不要写死。

> 依赖：`androidx.window:window`（Android）。桌面/Web/iOS 上拿不到折叠信息，退回 §1 的宽度判定即可。

---

## 5. RTL（阿拉伯语/希伯来语）

Compose 的 `LocalLayoutDirection` 会自动为 `Row`、`padding(start/end)`、`Modifier.align` 翻转——**前提是你用了
`start`/`end` 而不是 `left`/`right`**。

| 项 | 正确做法 | 错误做法 |
| :-- | :-- | :-- |
| 内边距 | `padding(start = 16.dp, end = 12.dp)` | `padding(left = …, right = …)` |
| 方向性图标（返回/箭头） | `graphicsLayer { scaleX = if (isRtl) -1f else 1f }` | 直接画 |
| 物理方向判断 | `LocalLayoutDirection.current == LayoutDirection.Rtl` | 用语言/国家判断 |
| 导航滑动返回方向 | `NavSwipeDirection` 是**物理方向、不随 RTL 镜像**：RTL 下用 `RightToLeft` | 一律用 `LeftToRight` |
| 数字/时间 | 用 `Text` 默认行为即可 | 手动反转字符串 |

标准的方向图标翻转写法（Miuix 官方约定）：

```kotlin
val layoutDirection = LocalLayoutDirection.current
Icon(
    imageVector = MiuixIcons.Back,
    contentDescription = "返回",
    modifier = Modifier.graphicsLayer {
        scaleX = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
    },
)
```

配套的滑动返回：

```kotlin
val swipeBack = when {
    !enableSwipeBack -> NavSwipeDirection.None
    LocalLayoutDirection.current == LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
    else -> NavSwipeDirection.LeftToRight
}
entry<Route.Detail>(swipeDismiss = swipeBack) { DetailScreen() }
```

---

## 6. 网格列数随宽度变

```kotlin
LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 160.dp),   // 按可用宽度自动决定列数
    contentPadding = PaddingValues(12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    items(list, key = { it.id }) { item ->
        Card(modifier = Modifier.fillMaxWidth()) { /* … */ }
    }
}
```

`GridCells.Adaptive` 比 `GridCells.Fixed(n)` 更适合自适应场景（无需自己判断宽度）。

---

## 7. 常见错误

| 错误 | 后果 | 正确 |
| :-- | :-- | :-- |
| 用 `LocalConfiguration.screenWidthDp` 判断宽度 | 多窗口/分屏时不准 | `LocalWindowInfo.current.containerSize`（官方 example 的做法） |
| 双窗格详情窗格没有占位根路由 | `miuix-nav` 拒绝空返回栈 | 加 `Route.Empty` 之类的占位根 |
| 窗格没 `clipToBounds()` | 转场画到隔壁窗格 | 加 |
| 窗格内保留 `cornerClipRadius = rememberNavSystemCornerRadius()` | 屏幕中间的窗格出现设备圆角 | 传 `0.dp` |
| 形态切换时迁移路由 key | 丢 `rememberSaveable` / ViewModel | app 级 `movableContentOf` 移动整个 `NavDisplay` |
| 宽屏内容拉满 | 一行文字过长、阅读困难 | `widthIn(max = 640.dp)` 居中 |
| 用 `left`/`right` 写内边距 | RTL 下布局错乱 | `start`/`end` |
| RTL 下仍用 `LeftToRight` 滑动返回 | 手势方向反了 | 按 `LocalLayoutDirection` 选 |
| 给 `NavigationRail` 传底部栏的 `NavigationItem` 列表后忘了 `weight(1f)` 窗格 | 内容被 rail 挤掉 | `Box(Modifier.weight(1f))` |

---

## 8. 真实参考

> 📍 **provenance 说明**：下表路径是产出本 skill 的机器上的克隆（`/root/miuix/repos/`），**仅用于追溯结论出处**。
> 换一台机器这些路径就不存在 —— **不要把「去读这些文件」当作任务步骤**，它们不在你的工作区。
> 需要抄实现时，以本 skill 的片段 + 你自己项目的既有代码为准。

| 内容 | 文件 |
| :-- | :-- |
| 自适应阈值（唯一官方给出具体 dp 的地方） | `/root/miuix/repos/miuix-lib/example/shared/src/commonMain/kotlin/utils/AdaptiveUtils.kt` |
| 双窗格 + rail + `movableContentOf` 保状态 | `/root/miuix/repos/miuix-lib/example/shared/src/commonMain/kotlin/AppContent.kt` |
| 多窗格 + 窗格内 NavDisplay | `/root/miuix/repos/miuix-lib/docs/zh_CN/guide/miuix-nav.md`（「多窗格布局」一节） |
| 官方 example 的三窗格测试页 | `/root/miuix/repos/miuix-lib/example/shared/src/commonMain/kotlin/MultiScaffoldTestPage.kt`（若存在） |
