# 场景 · 信息展示

「只读地告诉用户这是什么」。三个子型：

| 子型 | 典型页面 | 结构 |
| :-- | :-- | :-- |
| **A. 关于页** | 关于 / 致谢 / 版本 | Hero（Logo + 名称 + 版本）→ 分组链接卡 |
| **B. 只读信息卡** | 设备信息 / 环境信息 / 构建信息 | `SmallTitle` + 一张 key-value 卡 |
| **C. 详情页** | 某条记录的详情 / 某模块的状态 | Hero 预览 → 信息卡 → 底部操作 |

共同点：**没有输入控件，全部只读**；Hero 是视觉重心；信息用语义色分层（主文本 / 次级文本）。

---

## 1. A. 关于页完整骨架

```kotlin
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current

    // Hero 的视差进度：首项未滚完时 0→1
    val density = LocalDensity.current
    val heroHeightPx = with(density) { HERO_HEIGHT.toPx() }
    val scrollProgress by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) 1f
            else (lazyListState.firstVisibleItemScrollOffset / heroHeightPx).coerceIn(0f, 1f)
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "关于",
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(MiuixIcons.Back, contentDescription = "返回") }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MiuixTheme.colorScheme.background,
    ) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            // ① Hero：固定在上层，随滚动上移 + 淡出
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = (1f - scrollProgress * 1.35f).coerceIn(0f, 1f)
                        translationY = -96.dp.toPx() * scrollProgress   // 上移约 96.dp
                    }
                    .padding(top = paddingValues.calculateTopPadding() + HERO_TOP_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier.size(88.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "应用名",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "v$versionName ($versionCode)",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                )
            }

            // ② 内容：用 LazyColumn，首项是 Hero 的占位，随后才是真正的分组
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                overscrollEffect = null,
            ) {
                item(key = "hero_spacer") {
                    // 高度 = Hero 高度 + 上下留白；Hero 是独立图层，这里只负责占位
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(HERO_HEIGHT + HERO_TOP_PADDING + HERO_BOTTOM_PADDING),
                    )
                }

                item { SmallTitle("关于项目") }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    ) {
                        BasicComponent(
                            title = "检查更新",
                            summary = "获取最新版本",
                            startAction = { Icon(MiuixIcons.Update, contentDescription = null) },
                            onClick = { openUpdate() },
                        )
                        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                        BasicComponent(
                            title = "在 GitHub 上查看",
                            summary = GITHUB_URL,
                            startAction = { Icon(MiuixIcons.Link, contentDescription = null) },
                            onClick = { uriHandler.openUri(GITHUB_URL) },
                        )
                        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                        BasicComponent(
                            title = "开源许可",
                            startAction = { Icon(MiuixIcons.Notes, contentDescription = null) },
                            onClick = { openLicenses() },
                        )
                    }
                }

                item { SmallTitle("致谢") }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    ) {
                        BasicComponent(title = "compose-miuix-ui", summary = "UI 组件库", onClick = { })
                        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                        BasicComponent(title = "开源社区", summary = "问题反馈与贡献", onClick = { })
                    }
                }

                item { Spacer(Modifier.size(12.dp)) }
            }
        }
    }
}

private val HERO_HEIGHT = 300.dp
private val HERO_TOP_PADDING = 96.dp
private val HERO_BOTTOM_PADDING = 32.dp
```

### 为什么 Hero 要「独立图层 + LazyColumn 占位」

这是 3 个项目（lyricon `AboutScreen`、InstallerX `MiuixAboutPage`、官方 example `AboutPage`）的共同实现：

- Hero 放在 `LazyColumn` **外面**（`Box` 的上层），用 `graphicsLayer` 直接控制 `alpha` / `translationY`，
  这样滚动时它能**匀速上移并淡出**，而不是被列表整体推走。
- `LazyColumn` 的第一项是一个**等高的透明 `Spacer`**，负责把后续内容推到 Hero 下方。
- Hero 高度通过 `onSizeChanged` 实测（内容可能换行），再用它算占位高度与视差分母。

简化版（不需要视差）可以直接把 Hero 当普通 `item { }`：

```kotlin
item {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(Res.drawable.ic_launcher), null, Modifier.size(88.dp))
        Spacer(Modifier.height(16.dp))
        Text("应用名", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text("v$versionName", fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}
```

**先做简化版**；只有明确要求「滚动视差」时才上完整版。

### Hero 规范

| 项 | 值 |
| :-- | :-- |
| 应用名 | `32–35.sp` `Bold`，`onBackground`，居中 |
| 版本号 | `14.sp`，`onSurfaceVariantSummary`，居中，距名称 `5.dp` |
| Logo | `72–88.dp`；有自定义插画时用 `Card` 包一张 `Image(fillMaxWidth())` |
| Hero 下留白 | `32.dp` 起，视内容而定 |
| 顶栏 | `SmallTopAppBar`（不要大标题，Hero 已经承担了大标题的职责） |
| 视差位移 | 约 `96.dp`；透明度随进度线性降到 0 |

---

## 2. B. 只读信息卡

### 2.1 竖排 key-value（信息量小、需要呼吸感）

```kotlin
@Composable
fun InfoCard(items: List<Pair<String, String>>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            items.forEachIndexed { index, (title, value) ->
                Text(
                    text = title,
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,   // 17sp
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = value,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,       // 14sp
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(
                        top = 2.dp,
                        bottom = if (index == items.lastIndex) 0.dp else 24.dp,
                    ),
                )
            }
        }
    }
}
```

### 2.2 同一行左右对齐（信息量大、像表格）

```kotlin
Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
    items.forEachIndexed { index, (title, value) ->
        if (index > 0) {
            HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }
        BasicComponent(
            title = title,
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            endActions = {
                Text(
                    text = value,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
    }
}
```

> 只读行用 `BasicComponent`（**不传 `onClick`**）：它自动是静态的，不会出现按压反馈。

### 2.3 三种信息行的选择

| 信息特征 | 用 |
| :-- | :-- |
| 值较长（路径、版本串、包名） | 竖排（2.1）—— 行内放不下 |
| 值很短（是/否、数量、日期） | 同行左右（2.2） |
| 值需要复制 | 同行 + 尾部 `IconButton(Copy)` |
| 是一组相关字段（如「构建信息」4 条） | 竖排（2.1） |

---

## 3. C. 详情页

```kotlin
@Composable
fun DetailScreen(item: Item, onBack: () -> Unit, onDelete: () -> Unit) {
    val scrollBehavior = MiuixScrollBehavior()
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = item.title,
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(MiuixIcons.Back, contentDescription = "返回") }
                },
                actions = {
                    OverlayIconDropdownMenu(
                        entries = listOf(
                            DropdownEntry(
                                items = listOf(
                                    DropdownItem(text = "分享", onClick = { share(item) }),
                                    DropdownItem(text = "删除", onClick = { showDelete = true }),
                                ),
                            ),
                        ),
                    ) {
                        Icon(MiuixIcons.More, contentDescription = "更多")
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
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding(),
            ),
            overscrollEffect = null,
        ) {
            // 预览区（图标/封面/主状态）
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painter = painterResource(item.iconRes), contentDescription = null,
                          modifier = Modifier.size(72.dp))
                }
            }

            item { SmallTitle("基本信息") }
            item {
                Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    BasicComponent(title = "名称", endActions = { Text(item.title) })
                    HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    BasicComponent(title = "版本", endActions = { Text(item.version) })
                }
            }

            item { SmallTitle("详细描述") }
            item {
                Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    Text(
                        text = item.description,
                        style = MiuixTheme.textStyles.paragraph,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }

            item {
                TextButton(
                    text = "删除",
                    onClick = { showDelete = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )
            }
        }

        OverlayDialog(
            show = showDelete,
            title = "删除「${item.title}」？",
            summary = "删除后不可恢复。",
            onDismissRequest = { showDelete = false },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton("取消", onClick = { showDelete = false }, modifier = Modifier.weight(1f))
                TextButton(
                    text = "删除",
                    onClick = { showDelete = false; onDelete() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
```

详情页规则：

- 危险操作是**一个单独的 `TextButton`**（红色文字），放在内容末尾，不要塞进信息卡里。
- 破坏性操作**必须**二次确认（`OverlayDialog`）。
- `contentPadding` 要给 `bottom`（详情页通常没有底部栏，但需要底部安全区）。

---

## 4. 调试 / 诊断信息组

只在调试构建显示，用 `AppConfig.isLogEnabled && context.packageName == BuildConfig.APPLICATION_ID` 之类的条件包住：

```kotlin
if (BuildConfig.DEBUG) {
    item { SmallTitle("调试") }
    item {
        Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            BasicComponent(
                title = "导出日志",
                summary = "复制到剪贴板",
                onClick = { copyToClipboard(collectLogs()) },
            )
        }
    }
}
```

---

## 5. 警告 / 错误提示卡

信息页里需要强调某段文字时，用「容器色 + 图标」而不是加粗：

```kotlin
@Composable
fun WarningCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.errorContainer),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = MiuixIcons.Info,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
```

`TipCard` 同理，换成 `tertiaryContainer` / `onTertiaryContainer`。

---

## 6. 复制到剪贴板

信息页高频需求（版本号、设备串、日志）：

```kotlin
val clipboard = LocalClipboardManager.current
val scope = rememberCoroutineScope()
val snackbarHostState = remember { SnackbarHostState() }

BasicComponent(
    title = "设备串",
    endActions = {
        IconButton(onClick = {
            clipboard.setText(AnnotatedString(deviceId))
            scope.launch { snackbarHostState.showSnackbar("已复制") }
        }) {
            Icon(MiuixIcons.Copy, contentDescription = "复制")
        }
    },
)
```

记得在 `Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) })` 挂上宿主。

---

## 7. 常见错误

| 错误 | 后果 | 正确 |
| :-- | :-- | :-- |
| Hero 放在 `LazyColumn` 外面但没做占位 | 首屏内容被 Hero 盖住 | 首项放等高的透明 `Spacer` |
| Hero 用 `Modifier.offset` 改位置 | 位移不影响布局，滚动时穿帮 | 用 `graphicsLayer { translationY = ... }` |
| 视差进度用 `firstVisibleItemScrollOffset` 但不 clamp | 快速甩动时 alpha 变负 / 溢出 | `coerceIn(0f, 1f)` |
| 信息行用了 `ArrowPreference` 但没有 `onClick` | 出现无意义的箭头和按压反馈 | 只读行用 `BasicComponent` |
| 竖排信息卡加了分割线 | 每条两行文字，分割线切割感强 | 竖排不加分割线，只留 `24.dp` 间距 |
| 长描述用 `textStyles.main` | 无行高，密排难读 | `textStyles.paragraph` |
| 危险操作只给一个按钮 | 误触不可恢复 | `OverlayDialog` 二次确认 |
| 调试信息始终显示 | 正式包泄漏内部信息 | `BuildConfig.DEBUG` 包住 |

---

## 8. 真实参考

> 📍 **provenance 说明**：下表路径是产出本 skill 的机器上的克隆（`/root/miuix/repos/`），**仅用于追溯结论出处**。
> 换一台机器这些路径就不存在 —— **不要把「去读这些文件」当作任务步骤**，它们不在你的工作区。
> 需要抄实现时，以本 skill 的片段 + 你自己项目的既有代码为准。


| 内容 | 文件 |
| :-- | :-- |
| Hero 视差 + 模糊卡 + 分组（最完整的关于页） | `/root/miuix/repos/lyricon/app/src/main/kotlin/io/github/proify/lyricon/app/ui/about/AboutScreen.kt` |
| Hero 占位 + 分组链接 + 调试组（`logoSpacer` 手法） | `/root/miuix/repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/miuix/settings/preferred/about/MiuixAboutPage.kt` |
| 关于对话框（轻量版） | `/root/miuix/repos/Updater-KMP/app/shared/src/commonMain/kotlin/ui/AboutDialog.kt` |
| 官方 example 的关于/许可页 | `/root/miuix/repos/miuix-lib/example/shared/src/commonMain/kotlin/AboutPage.kt`、`LicensePage.kt` |
| 提示卡 / 错误文本块封装 | `/root/miuix/repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/miuix/widgets/MiuixCards.kt` |
