# 场景 · 主框架 / 多 Tab

承载所有页面的骨架。**写法在官方 example 与 5 个真实项目里高度一致**：`Scaffold` + `HorizontalPager` + `NavigationBar`，
再叠一层「分页状态同步 + 返回键回首个 Tab」。

---

## 1. 结构

```kotlin
@Composable
fun MainShell() {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val mainPagerState = rememberMainPagerState(pagerState)
    val scope = rememberCoroutineScope()

    val navigationItems = remember {
        listOf(
            NavigationItem("首页", MiuixIcons.Home),
            NavigationItem("功能", MiuixIcons.Tune),
            NavigationItem("设置", MiuixIcons.Settings),
        )
    }

    BackToFirstTabHandler(mainPagerState)

    Scaffold(
        bottomBar = {
            NavigationBar {
                navigationItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = mainPagerState.selectedPage == index,
                        onClick = { mainPagerState.animateToPage(index) },
                        icon = item.icon,
                        label = item.label,
                    )
                }
            }
        },
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            pageContent = { page ->
                when (page) {
                    0 -> HomePage(paddingValues)
                    1 -> FeaturesPage(paddingValues)
                    else -> SettingsPage(paddingValues)
                }
            },
        )
    }
}
```

四个要点，缺一个就会出问题：

1. **底部导航栏只在主框架建一次** —— 这条是硬的。但「Tab 页面要不要自带 `Scaffold`」**两种都行，取决于你需要什么**：
   - **不自带**：主框架的 `Scaffold` 提供 `paddingValues` 与 popup 宿主，Tab 页最省事，适合大多数情况。
   - **自带**：Tab 页需要自己的 `snackbarHost`、独立的 `TopAppBar`（大标题折叠）、或自己的弹层作用域时。
     **官方 example 的 Tab 页 `MainPage.kt:257` 就是这么做的**，InstallerX 的首页同样自带。
   - 唯一的禁忌是**在 Tab 页里再放一个 `NavigationBar`**：那才会叠两层底栏、insets 双算。
   - 自带 `Scaffold` 时注意 insets 归属：外层已消费的部分不要再 padding 一次。
     （本规则早前写成「Tab 页一律不许建 `Scaffold`」，**是错的，已更正**。）
2. **`NavigationBarItem` 必须是 `NavigationBar` lambda 的 `RowScope` 子节点**（它是 `RowScope` 扩展）。
   0.9 之前的 `NavigationBar(items = , selected = , onClick = )` 是旧 API，见 `pitfalls.md`。
3. **Tab 状态与 pager 状态要双向同步**（见 §2）。
4. **返回键要先回首个 Tab**（见 §3）。

---

## 2. `MainPagerState`：分页状态同步层

这是被反复重写的同一个东西（官方 example、InstallerX-Revived 各有一份），它解决三个问题：

- 点击 Tab 时用**弹簧/自定义时长**切页，而不是 `animateScrollToPage` 的默认速度（跨多页时更稳）；
- 切页动画期间用户再点，要**取消上一次**（防抖）；
- `selectedPage`（UI 选中态）与 `pagerState.currentPage`（真实滚动位置）**不能各说各话**。

```kotlin
@Stable
class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return
        navJob?.cancel()
        selectedPage = targetIndex
        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                pagerState.springAnimateToPage(targetIndex)   // miuix 提供的弹簧切页
            } finally {
                if (navJob == myJob && pagerState.currentPage != targetIndex) {
                    selectedPage = pagerState.currentPage      // 被打断则回滚选中态
                }
            }
        }
    }

    /** 手指滑动切页后同步选中态 */
    fun syncPage() {
        if (selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): MainPagerState = remember(pagerState, coroutineScope) { MainPagerState(pagerState, coroutineScope) }
```

配套在顶层监听手指滑动：

```kotlin
LaunchedEffect(mainPagerState.pagerState.currentPage) {
    mainPagerState.syncPage()
}
```

> 只有 2–3 个 Tab、且不需要自定义切页速度时，可以直接用 `pagerState` + `scope.launch { pagerState.springAnimateToPage(i) }`，
> 省略这一层。

---

## 3. 返回键：非首个 Tab 时先回 Tab 0

官方 example 与 InstallerX-Revived 的实现完全一致：

```kotlin
@Composable
fun BackToFirstTabHandler(mainPagerState: MainPagerState, navigator: Navigator) {
    val enabled by remember {
        derivedStateOf {
            navigator.current() is Route.Home &&
                navigator.backStackSize() == 1 &&
                mainPagerState.selectedPage != 0
        }
    }
    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = enabled,
        onBackCompleted = { mainPagerState.animateToPage(0) },
    )
}
```

三个条件缺一不可：

| 条件 | 为什么 |
| :-- | :-- |
| `current() is Route.Home` | 只在主框架路由上生效，二级页的返回交给 `miuix-nav` |
| `backStackSize() == 1` | 栈里有二级页时，返回应该 pop 二级页 |
| `selectedPage != 0` | 已在首个 Tab 时不拦截，让系统正常退出应用 |

注意 `isBackEnabled` 是**动态**的：条件变化时返回拦截会自动开关，不需要重建 handler。

---

## 4. 底部栏的显示/隐藏

### 4.1 滚动时收起、停止时展开（OShin 的做法）

用顶栏的 `collapsedFraction` 驱动，并在切页时重置：

```kotlin
val scrollBehavior = MiuixScrollBehavior()
var isBottomBarVisible by rememberSaveable { mutableStateOf(true) }

LaunchedEffect(scrollBehavior.state.collapsedFraction) {
    isBottomBarVisible = scrollBehavior.state.collapsedFraction < 0.5f
}
LaunchedEffect(pagerState.currentPage) {
    isBottomBarVisible = true      // 切页后一定要恢复，否则会「消失不回来」
}
```

```kotlin
AnimatedVisibility(
    visible = isBottomBarVisible,
    enter = slideInVertically(initialOffsetY = { it }),
    exit = slideOutVertically(targetOffsetY = { it }),
) {
    NavigationBar { /* NavigationBarItem */ }
}
```

### 4.2 悬浮胶囊底栏（InstallerX-Revived / 官方 example 的做法）

底栏不贴边，居中悬浮、带模糊与圆角：

```kotlin
Box(modifier = Modifier.fillMaxWidth()) {
    FloatingNavigationBar(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        navigationItems.forEachIndexed { index, item ->
            FloatingNavigationBarItem(
                selected = page == index,
                onClick = { mainPagerState.animateToPage(index) },
                icon = item.icon,
                label = item.label,
            )
        }
    }
}
```

或者自己拼 `FloatingToolbar(color = colorScheme.surfaceContainer, cornerRadius = 50.dp)` + `IconButton`。

### 4.3 模糊底栏

```kotlin
val backdrop = rememberLayerBackdrop { drawRect(MiuixTheme.colorScheme.surface); drawContent() }
val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
    // 页面内容
    NavigationBar(
        modifier = Modifier
            .textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = 25f,
                colors = BlurDefaults.blurColors(
                    blendColors = listOf(BlendColorEntry(MiuixTheme.colorScheme.surface.copy(alpha = 0.8f))),
                ),
            ),
        color = barColor,
    ) { /* ... */ }
}
```

规则：**模糊层的 `color` 必须是 `Color.Transparent`**，否则把模糊盖住（详见 `effects.md` §5.4）。

---

## 5. 宽屏：`NavigationRail` + 单窗格

官方 example 的模式（`layout.md` §7 也提过）：

```kotlin
if (isWideScreen) {
    Row(Modifier.fillMaxSize()) {
        NavigationRail(state = railState) {
            navigationItems.forEachIndexed { index, item ->
                NavigationRailItem(
                    selected = selectedPage == index,
                    onClick = { onSelectTab(index) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
            // 内容（若用 miuix-nav，这里放 NavDisplay）
        }
    }
} else {
    // 手机布局：Scaffold + bottomBar NavigationBar
}
```

三条硬性规则：

1. 窗格必须 `.clipToBounds()`，否则转场图层会画到侧栏上。
2. 窗格内屏幕圆角归零（`cornerClipRadius = 0.dp`）。
3. 去掉侧栏已消费的边缘 insets（`displayCutout.exclude(only(Start))`）。

---

## 6. `NavigationBar` 的可用变形

| 形态 | 组件 | 何时用 |
| :-- | :-- | :-- |
| 标准底部栏 | `NavigationBar { NavigationBarItem(...) }` | 默认，2–5 个 Tab |
| 仅图标 | `NavigationBar(mode = NavigationBarDisplayMode.IconOnly)` | 窄屏 / 5 个以上 |
| 图标常显、文字仅选中 | `NavigationBarDisplayMode.IconWithSelectedLabel` | 4–5 个 Tab |
| 悬浮胶囊 | `FloatingNavigationBar { FloatingNavigationBarItem(...) }` | 想要轻盈感 / 有模糊背景 |
| 侧边栏 | `NavigationRail { NavigationRailItem(...) }` | 平板 / 折叠屏 |
| 带角标 | `NavigationBarItem(badge = { Badge { Text("8") } })` | 未读数 |

`NavigationItem(label: String, icon: ImageVector)` 是数据类，通常 `remember { listOf(...) }` 一次。

---

## 7. 常见错误

| 错误 | 后果 | 正确 |
| :-- | :-- | :-- |
| 每个 Tab 页面各建一个 `Scaffold` + `NavigationBar` | 底部栏叠两层、insets 双算、切页时底栏闪烁 | 只在主框架建一次，传 `paddingValues` 下去 |
| Tab 内容用 `when (page)` 直接在 `Scaffold` 里渲染 | 失去横滑手势与页面缓存 | 用 `HorizontalPager` |
| `selected` 直接用 `pagerState.currentPage` | 动画中选中态跳变（currentPage 在过半时才翻转） | `MainPagerState.selectedPage` |
| 没做返回键处理 | 在 Tab 3 按返回直接退出应用 | `BackToFirstTabHandler` |
| `AnimatedVisibility` 隐藏底栏后没有恢复 | 底栏消失不回来 | `LaunchedEffect(currentPage) { visible = true }` |
| `Overlay*` 弹层放在 Tab 页面里但该处没有 `Scaffold` | 弹不出来 | 主框架的 `Scaffold` 已提供宿主；确认调用点在它的 composition 内 |
| 用 `NavigationBar(items = , selected = , onClick = )` | 旧 API，0.9+ 编译失败 | `NavigationBar { NavigationBarItem(...) }` |

---

## 8. 真实参考

> 📍 **provenance 说明**：下表路径是产出本 skill 的机器上的克隆（`/root/miuix/repos/`），**仅用于追溯结论出处**。
> 换一台机器这些路径就不存在 —— **不要把「去读这些文件」当作任务步骤**，它们不在你的工作区。
> 需要抄实现时，以本 skill 的片段 + 你自己项目的既有代码为准。


| 内容 | 文件 |
| :-- | :-- |
| 完整主框架（分页 + rail + 弹层 + 返回） | `/root/miuix/repos/miuix-lib/example/shared/src/commonMain/kotlin/AppContent.kt` |
| `MainPagerState` 官方实现 | 同上（文件末尾） |
| `MainPagerState` 另一实现 + 悬浮底栏 | `/root/miuix/repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/navigation/PagerState.kt` |
| 返回键回首个 Tab | `/root/miuix/repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/navigation/MainScreenBackHandler.kt` |
| 底栏滚动显隐 | `/root/miuix/repos/OShin/app/src/main/java/com/suqi8/oshin/ui/mainscreen/MainScreen.kt` |
| 模糊顶栏 + Tab 容器 | `/root/miuix/repos/lyricon/app/src/main/kotlin/io/github/proify/lyricon/app/compose/AppComposable.kt` |
