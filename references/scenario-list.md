# 场景 · 通用列表

所有列表都是 `LazyColumn`，区别只在**每行的粒度**。先选粒度，再抄对应结构。

| 粒度 | 每行长什么样 | 用在哪 | 组件 |
| :-- | :-- | :-- | :-- |
| **A. 行列表** | 一行一个设置项 | 设置页、开关列表 | `SwitchPreference` / `ArrowPreference` … 见 `preferences.md` |
| **B. 卡片列表** | 每条一张独立 Card：图标 + 标题 + 副标题 + 操作 | 应用列表、插件列表、记录列表 | `Card` + `BasicComponent` 或手写 `Row` |
| **C. 内容卡** | 每条一张 Card，含大段文本/多行结构 | 历史、日志、歌词、章节 | `Card` + `Column` + `HorizontalDivider` |
| **D. 网格** | 多列等宽卡片 | 图标墙、颜色、缩略图 | `LazyVerticalGrid` |

---

## 1. 通用外壳

```kotlin
val scrollBehavior = MiuixScrollBehavior()

Scaffold(
    topBar = {
        SmallTopAppBar(
            title = "列表",
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(MiuixIcons.Back, contentDescription = "返回") }
            },
            actions = {
                IconButton(onClick = { expanded = true }) {
                    Icon(MiuixIcons.Search, contentDescription = "搜索")
                }
            },
            scrollBehavior = scrollBehavior,
        )
    },
) { paddingValues ->
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .scrollEndHaptic()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
        overscrollEffect = null,
    ) {
        item { Spacer(Modifier.size(12.dp)) }
        items(list, key = { it.id }) { item -> ListItemCard(item) }
        item { Spacer(Modifier.size(12.dp)) }
    }
}
```

**`key` 是硬要求**：没有稳定 key，插入/删除时滚动位置和 item 状态都会错乱。

---

## 2. A. 行列表（设置项）

见 `preferences.md`。规则回顾：

```kotlin
item { SmallTitle("分组标题") }
item {
    Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        SwitchPreference(title = "开关项", checked = on, onCheckedChange = { })
        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        ArrowPreference(title = "跳转项", summary = "说明", onClick = { })
    }
}
```

组内**最后一项不加分割线**。多项时用 `forEachIndexed` + `if (index < lastIndex)`。

---

## 3. B. 卡片列表（最通用的形态）

每条一张 Card，内部结构：**左图标 → 中标题/副标题 → 右操作**；可选下方描述与标签。

```kotlin
@Composable
fun ListItemCard(
    item: Item,
    onOpen: () -> Unit,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        onClick = onOpen,
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 左：图标 40.dp
                Image(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.width(12.dp))

                // 中：标题 + 副标题（weight(1f) 吃掉剩余宽度）
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.subtitle,
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }

                // 右：操作槽（对齐到顶部，避免标题两行时被垂直居中）
                IconButton(
                    onClick = onAction,
                    modifier = Modifier.align(Alignment.Top).padding(start = 8.dp),
                ) {
                    Icon(MiuixIcons.More, contentDescription = "更多")
                }
            }

            // 可选：下方描述块，用分割线分隔
            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Text(
                    text = item.description,
                    modifier = Modifier.padding(top = 10.dp),
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}
```

| 项 | 值 | 说明 |
| :-- | :-- | :-- |
| 卡片横向外边距 | `12.dp` | 与分组列表一致 |
| 卡片间距 | `bottom = 12.dp` | 卡片列表**不加组内分割线**，卡片间距就是分隔 |
| 卡片内边距 | `16.dp`（自己加） | `Card` 默认 `insideMargin = 0` |
| 左侧图标 | `40.dp` | 应用图标惯例 40.dp；小图标 24.dp |
| 图标与文字间距 | `12.dp` | |
| 标题 | `16.sp` `Medium` `onSurface` | |
| 副标题 | `14.sp` `onSurfaceVariantSummary`，间距 `4.dp` | |
| 操作槽 | `Modifier.align(Alignment.Top).padding(start = 8.dp)` | 标题两行时不错位 |
| 描述块 | `10.dp` 间距 + `HorizontalDivider()` + `10.dp` | **这里的分割线是全宽的**（在卡片内，无需 16.dp 缩进） |
| 点击反馈 | `PressFeedbackType.Sink` | 大卡片用 `Tilt`，列表项用 `Sink` |

> 卡片内的分割线**不需要** `padding(horizontal = 16.dp)`——它已经在 16.dp 内边距里面了。
> 只有「分组 Card 内的 Preference 行之间」才需要 16.dp，因为那些行自带内边距。

### 用 `BasicComponent` 代替手写 Row（更省事）

结构简单的行直接用库组件，缩进/按压/禁用态都自动正确：

```kotlin
Card(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp),
) {
    BasicComponent(
        title = item.title,
        summary = item.subtitle,
        startAction = {
            Image(painterResource(item.iconRes), contentDescription = null, modifier = Modifier.size(40.dp))
        },
        endActions = {
            IconButton(onClick = onAction) { Icon(MiuixIcons.More, contentDescription = "更多") }
        },
        onClick = onOpen,
    )
}
```

**优先用 `BasicComponent`**；只有当需要「标题+副标题+独立描述块+标签流」这种复合布局时才手写 `Column`。

---

## 4. C. 内容卡（大段文本 / 多行）

```kotlin
@Composable
fun HistoryItemCard(item: HistoryItem, onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        onClick = onOpen,
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = item.time,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.body,
                style = MiuixTheme.textStyles.paragraph,     // 17sp / 行高 1.2em，长文本专用
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
```

长段落一律用 `MiuixTheme.textStyles.paragraph`（1.2em 行高），不要用 `main`（无行高，密排难读）。

---

## 5. 分组与排序

### 5.1 按类型分组（`SmallTitle` + 一组卡片）

```kotlin
grouped.forEach { (sectionTitle, items) ->
    item(key = "header_$sectionTitle") { SmallTitle(sectionTitle) }
    items(items, key = { it.id }) { item -> ListItemCard(item) }
}
```

`SmallTitle` 在需要加大分组间距时显式给 `insideMargin`：

```kotlin
SmallTitle(
    text = sectionTitle,
    insideMargin = PaddingValues(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 8.dp),
)
```

### 5.2 字母索引 / 吸顶

- 字母分组：`SmallTitle('A')` + 该字母下的条目；`stickyHeader` 不是 Miuix 组件，需要吸顶时用 A–Z 侧边索引条自己实现。
- 排序/筛选入口放顶栏 `actions`，用 `OverlayIconDropdownMenu`（`recipes.md` 配方 10）。

---

## 6. 搜索过滤

```kotlin
var query by rememberSaveable { mutableStateOf("") }
var expanded by rememberSaveable { mutableStateOf(false) }

val filtered = remember(query, allItems) {
    if (query.isBlank()) allItems
    else allItems.filter { it.title.contains(query, ignoreCase = true) }
}
```

- 过滤用 `remember(query, allItems)`，不要每帧重算。
- 搜索框展开时用 `SearchBar` + `InputField`（`components.md` §4），收起时是顶栏的一个图标按钮。
- **「搜索无结果」的空态文案要和「本来就没数据」区分开**（见 `scenario-state.md` §3）。

---

## 7. 多选列表

```kotlin
var selected by remember { mutableStateOf(setOf<String>()) }

Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
    items.forEachIndexed { index, item ->
        if (index > 0) {
            HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }
        CheckboxPreference(
            title = item.title,
            summary = item.subtitle,
            checked = item.id in selected,
            onCheckedChange = { checked ->
                selected = if (checked) selected + item.id else selected - item.id
            },
        )
    }
}
```

- 多选且需要「全选」时，把全选放组外单独一张卡，或顶栏 `actions` 里放全选/取消。
- 选中数量 > 0 时，顶栏切换为「已选 N 项」+ 操作按钮（`TopAppBar(title = "已选 $count 项", navigationIcon = 关闭)`）——这是原生惯例。
- 批量操作确认用 `OverlayDialog`（`recipes.md` 配方 8）。

---

## 8. 加载更多 / 分页

官方 example 的做法（`snapshotFlow` + 临界索引 + 防抖）：

```kotlin
var loadingMore by remember { mutableStateOf(false) }
val listState = rememberLazyListState()

LaunchedEffect(listState, items) {
    snapshotFlow {
        val info = listState.layoutInfo
        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
        lastVisible >= 0 && lastVisible >= info.totalItemsCount - 3   // 距底部 3 项
    }
        .distinctUntilChanged()
        .collect { nearEnd ->
            if (nearEnd && !loadingMore && hasMore) {
                loadingMore = true
                viewModel.loadMore()          // 由状态层在完成时把 loadingMore 置回 false
            }
        }
}
```

列表末尾的加载指示器：

```kotlin
if (loadingMore) {
    item(key = "loading_more") {
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            InfiniteProgressIndicator()
        }
    }
}
if (!hasMore && items.isNotEmpty()) {
    item(key = "no_more") {
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("没有更多了", style = MiuixTheme.textStyles.footnote1,
                 color = MiuixTheme.colorScheme.onBackgroundVariant)
        }
    }
}
```

要点：`distinctUntilChanged()` 必须有（否则会连续触发）；`loadingMore` 的置回由状态层负责，不要放在 Composable 里。

---

## 9. 下拉刷新 + 列表

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

---

## 10. 列表项常用操作槽

| 需求 | 写法 |
| :-- | :-- |
| 显示一个值 | `endActions = { Text(value, color = colorScheme.onSurfaceVariantSummary) }` |
| 单个图标操作 | `endActions = { IconButton(onClick) { Icon(MiuixIcons.Delete, "删除") } }` |
| 多个操作 | 用 `OverlayIconDropdownMenu` 收进「更多」菜单 |
| 开关 | 直接用 `SwitchPreference`，不要自己塞 `Switch` 进 `endActions` |
| 危险操作 | 图标 `tint = colorScheme.error`，或 `titleColor = BasicComponentDefaults.titleColor(color = colorScheme.error)` |
| 状态标签 | 小号 `Card(colors = CardDefaults.defaultColors(color = colorScheme.tertiaryContainer))` + `Row(padding(horizontal = 10.dp, vertical = 7.dp))` |
| 复制到剪贴板 | `LocalClipboardManager.current.setText(AnnotatedString(text))`，用 `IconButton` 触发 + `Snackbar` 反馈 |

---

## 11. 性能清单

- [ ] `items(list, key = { it.id })`
- [ ] 需要动画加 `Modifier.animateItem()`
- [ ] `Overlay*` / 对话框放在 `LazyColumn` **之外**
- [ ] 过滤/映射结果用 `remember(dep) { }` 缓存
- [ ] 列表项里不做网络/IO
- [ ] 图标 `ImageVector` 用 `remember` 或放常量，不要在 item 里重新构造
- [ ] 集合用 `ImmutableList` / `PersistentList`（对 Compose 是稳定类型）
- [ ] 不需要滚动的短列表（<10 项）可以用 `Column`，但**设置页一律用 LazyColumn**（保持一致）

---

## 12. 常见错误

| 错误 | 后果 | 正确 |
| :-- | :-- | :-- |
| `items(list)` 无 `key` | 增删时状态错乱、动画异常 | `key = { it.id }` |
| `LazyColumn(Modifier.padding(horizontal = 12.dp))` | 分割线与标题错位 | 间距放 `Card` 上 |
| 卡片列表还给每张卡加组内分割线 | 双重分隔，视觉噪音 | 卡片列表靠 `bottom = 12.dp` 分隔 |
| 卡片内分割线加 `padding(horizontal = 16.dp)` | 与内容不齐（已经在 16.dp 内边距里） | 卡片内分割线用全宽 `HorizontalDivider()` |
| `verticalArrangement = Arrangement.spacedBy(12.dp)` | 组内条目也被撑开 | 组间距用 Card 的 `bottom` padding |
| 加载更多没有 `loadingMore` 守卫 | 同一次触底连续请求多次 | 加守卫 + `distinctUntilChanged()` |
| 搜索过滤直接 `list.filter{}` 写在 Composable 参数位 | 每帧重算 | `remember(query) { }` |
| 用 `Column` 渲染 100+ 项 | 首屏极慢 | `LazyColumn` |

---

## 13. 真实参考

> 📍 **provenance 说明**：下表路径是产出本 skill 的机器上的克隆（`/root/miuix/repos/`），**仅用于追溯结论出处**。
> 换一台机器这些路径就不存在 —— **不要把「去读这些文件」当作任务步骤**，它们不在你的工作区。
> 需要抄实现时，以本 skill 的片段 + 你自己项目的既有代码为准。


| 内容 | 文件 |
| :-- | :-- |
| 卡片列表（图标+标题+副标题+操作槽+描述+标签流） | `/root/miuix/repos/lyricon/app/src/main/kotlin/io/github/proify/lyricon/app/ui/tabs/ProviderPage.kt` |
| 分组卡片列表 + 开关行（最标准的设置型列表） | `/root/miuix/repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/miuix/settings/preferred/installer/MiuixInstallerGlobalSettingsPage.kt` |
| 可复用列表项封装（项目级 `MiuixNavigationItemWidget` / `MiuixSwitchWidget`） | `/root/miuix/repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/miuix/widgets/MiuixNavigationItemWidget.kt` 与 `MiuixBaseWidget.kt` |
| 加载更多（`snapshotFlow` + 临界索引） | `/root/miuix/repos/miuix-lib/example/shared/src/commonMain/kotlin/OverscrollLoadMorePage.kt` |
| 搜索 + 分组 + 顶栏动作菜单的长列表 | `/root/miuix/repos/miuix-lib/example/shared/src/commonMain/kotlin/MainPage.kt` |
| 项目级列表容器（统一外壳，页面只写 `LazyListScope`） | `/root/miuix/repos/lyricon/app/src/main/kotlin/io/github/proify/lyricon/app/compose/AppComposable.kt` |

---

## 14. 拖拽排序（`LazyColumn` 内）

Compose 没有内置的「列表项拖拽排序」，需要用 `detectDragGesturesAfterLongPress` + 手动 offset 实现。
Miuix 不提供该能力，所以**这类需求要自己写**，下面是可用的最小结构：

```kotlin
@Composable
fun ReorderableList(
    items: List<Item>,
    onMove: (from: Int, to: Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val itemHeightPx = with(LocalDensity.current) { ITEM_HEIGHT.toPx() }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical(isEnabled = { draggingIndex == null })   // 拖拽时禁用回弹
            .scrollEndHaptic()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        overscrollEffect = null,
    ) {
        itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
            val isDragging = draggingIndex == index
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        if (isDragging) {
                            translationY = dragOffset
                            scaleX = 1.02f
                            scaleY = 1.02f
                        }
                    }
                    .pointerInput(items) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                dragOffset = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                // 跨过一整行高度就交换位置
                                val from = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                if (dragOffset > itemHeightPx / 2 && from < items.lastIndex) {
                                    onMove(from, from + 1)
                                    draggingIndex = from + 1
                                    dragOffset -= itemHeightPx
                                } else if (dragOffset < -itemHeightPx / 2 && from > 0) {
                                    onMove(from, from - 1)
                                    draggingIndex = from - 1
                                    dragOffset += itemHeightPx
                                }
                            },
                            onDragEnd = {
                                draggingIndex = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggingIndex = null
                                dragOffset = 0f
                            },
                        )
                    },
            ) {
                BasicComponent(
                    title = item.title,
                    summary = item.subtitle,
                    // 视觉上给一个「抓手」，但整行都可长按拖
                    endActions = { Icon(MiuixIcons.ListView, contentDescription = "长按拖动排序") },
                )
            }
        }
    }
}
```

要点与坑：

- **长按触发**（`detectDragGesturesAfterLongPress`），不要用普通拖拽——会和列表滚动抢手势。
- 拖拽期间用 `overScrollVertical(isEnabled = { draggingIndex == null })` **关掉越界回弹**，否则会互相干扰。
- 被拖项用 `Modifier.zIndex(1f)` 抬到其它项之上，否则会被下面的项盖住。
- 交换用**回调**（`onMove`）交给 ViewModel 改数据，不要在 Composable 里直接改 list。
- 数据源必须是**稳定 key** 的列表（`itemsIndexed(..., key = ...)`），否则交换后 item 状态会错位。
- 自动滚动到边缘：需要自己用 `listState.scrollBy()` 配合 `LaunchedEffect`，这不是开箱能力。
- 简单场景（只是给个顺序，不需要拖拽手感）优先考虑**上下箭头按钮**或 `NumberPicker`——比拖拽可靠得多。

参考：`/root/miuix/repos/miuix-lib/example/shared/src/commonMain/kotlin/` 下有若干列表交互示例（`OverscrollLoadMorePage.kt` 等），
但没有拖拽排序实现——**这确实是 Miuix 未覆盖的领域**。

---

## 15. 媒体 / 图片查看器

Miuix 没有图片浏览组件，用 Compose 基础能力 + Miuix 的 chrome 拼：

```kotlin
@Composable
fun MediaViewer(
    urls: List<String>,
    initialIndex: Int,
    onBack: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { urls.size })
    var chromeVisible by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)          // miuix-lint:allow 媒体查看器必须是纯黑底
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { chromeVisible = !chromeVisible },   // 点击切换 chrome 显隐
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            // 图片加载用 Coil / Kamel（你自己的依赖），这里只示意
            AsyncImage(
                model = urls[page],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }

        // 顶栏：透明浮在图片上，随 chrome 显隐
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            SmallTopAppBar(
                title = "${pagerState.currentPage + 1} / ${urls.size}",
                color = Color.Transparent,     // miuix-lint:allow 浮在图片上
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Close, contentDescription = "关闭")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 保存到相册 */ }) {
                        Icon(MiuixIcons.Download, contentDescription = "保存")
                    }
                },
            )
        }
    }
}
```

要点：

- **必须用纯黑底**（`Color.Black`），这是查看器的行业惯例，也是唯一合理的硬编码颜色；记得加 `// miuix-lint:allow`。
- 顶栏 `color = Color.Transparent` 浮在图片上；点击图片切换 chrome 显隐（`AnimatedVisibility` + fade）。
- 页码用 `SmallTopAppBar` 的 `title`（"3 / 12"），不要另做底部指示器。
- 关闭用 `MiuixIcons.Close`（不是 `Back`）——这是模态查看器。
- 图片加载交给 Coil / Kamel，**Miuix 不含图片加载库**。
- 需要缩放手势时用 `Modifier.pointerInput { detectTransformGestures { ... } }` 自己实现，或引入
  `androidx.compose.foundation` 之上的第三方缩放库。

---

## 16. Miuix 未覆盖的领域（不要在这里找方案）

以下能力 **Miuix 不提供、也不是它的职责**，需要你自己实现或引入其它库：

| 能力 | 方案 |
| :-- | :-- |
| 图片/视频加载与缓存 | Coil（Android/KMP）、Kamel（KMP） |
| 拖拽排序、拖拽把手 | 自己写（本文件 §14） |
| 图表 / 数据可视化 | Vico、自绘 Canvas |
| 富文本 / Markdown 渲染 | multiplatform-markdown-renderer 等 |
| 相机、扫码 | 平台 API / CameraX |
| 折叠屏铰链信息 | `androidx.window`（见 `scenario-adaptive.md` §4） |
| 引导轮播 / Coach mark | 自己拼（见 `scenario-onboarding.md`） |
| 权限申请 | 平台 API（`rememberLauncherForActivityResult`） |
| 分享、通知、后台任务 | 平台 API |

> 遇到这些需求时，**用 Miuix 组件做外壳（页面结构、顶栏、卡片、按钮），把具体能力接第三方/平台 API**——
> 这正是真实项目的做法。不要试图在 Miuix 里找一个并不存在的组件。
