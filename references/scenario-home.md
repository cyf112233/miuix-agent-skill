# 场景 · 首页 / 仪表盘

首页要在一屏内回答两件事：**「现在是什么状态」** 和 **「我能去哪」**。
8 个项目里的首页结构高度收敛为四段：

```
① 状态卡（Hero）      —— 一屏里最醒目的那张卡，讲清当前状态
② 快捷入口行          —— 2–4 个等宽小卡，直达最常用的两个地方
③ 信息卡              —— 只读的版本/设备/环境信息
④ 功能入口列表        —— SmallTitle + Card + ArrowPreference，通往其余功能
```

首页通常**没有返回按钮**（`canBack = false`），顶栏 `actions` 放设置 / 搜索 / 更多。

---

## 1. 完整骨架

```kotlin
@Composable
fun HomePage(
    onOpenSettings: () -> Unit,
    onOpenFeatureA: () -> Unit,
    onOpenFeatureB: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = "应用名",
                actions = {
                    IconButton(onClick = onOpenSettings) {
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
            item(key = "status") { StatusCard(active = true, versionName = "1.0.0") }
            item(key = "shortcuts") { QuickActionRow(countA = "12", countB = "5", onOpenFeatureA, onOpenFeatureB) }
            item(key = "system_info") { SystemInfoCard(items = deviceInfoItems()) }
            item(key = "history") { HistoryEntryCard(onClick = onOpenHistory) }

            item { SmallTitle("更多") }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    ArrowPreference(title = "功能 A", summary = "说明", onClick = onOpenFeatureA)
                    HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    ArrowPreference(title = "功能 B", summary = "说明", onClick = onOpenFeatureB)
                }
            }

            item { Spacer(Modifier.size(12.dp)) }
        }
    }
}
```

> 横向外边距：本文件示例统一用 **12.dp**（与设置页一致，最安全）。
> lyricon 的首页用 16.dp，因为它整个项目「自由内容卡」都取 16.dp。**跟随项目既有值，不要混用。**

---

## 2. ① 状态卡（Hero）

首页最重要的组件。要点：**语义底色 + 大装饰图标 + 两行文字 + 轻微点击反馈**。

```kotlin
@Composable
fun StatusCard(
    active: Boolean,
    versionName: String,
    onClick: () -> Unit = {},
) {
    // 状态色必须给足对比度：底色 + 前景色成对出现，且明暗两套
    // isInDarkTheme() 是项目封装的辅助函数（定义见 theme.md「判断当前是否深色」）；
    // 不想封装就直接用 isSystemInDarkTheme()，或用 colorScheme.background.luminance() < 0.5f 反推
    val isDark = isInDarkTheme()
    val containerColor = when {
        active -> if (isDark) Color(0xFF102819) else Color(0xFFDFFAE4)
        else -> if (isDark) Color(0xFF402626) else Color(0xFFFDECEE)
    }
    val accentColor = if (active) Color(0xFF36D167) else Color(0xFFE25B5B)
    val titleColor = if (active) {
        if (isDark) Color.White else Color(0xFF0F1B13)
    } else {
        if (isDark) Color.White else Color(0xFF3A1416)
    }
    val summaryColor = titleColor.copy(alpha = 0.68f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .height(140.dp),
        colors = CardDefaults.defaultColors(color = containerColor),
        pressFeedbackType = PressFeedbackType.Tilt,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize()) {
            // 右下角超尺寸装饰图标，被卡片裁掉一部分 —— 这是原生观感的关键
            Icon(
                imageVector = if (active) MiuixIcons.Ok else MiuixIcons.Info,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 28.dp, y = 34.dp)
                    .size(128.dp),
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
            ) {
                Text(
                    text = if (active) "已激活" else "未激活",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "版本 $versionName",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = summaryColor,
                )
            }
        }
    }
}
```

规范表：

| 项 | 值 |
| :-- | :-- |
| 高度 | `140.dp`（固定；状态卡不怕矮，怕高度跳变） |
| 圆角 | 默认 `CardDefaults.CornerRadius` = 16.dp，不要改 |
| 底色 | 成功：浅 `#DFFAE4` / 深 `#102819`；异常：浅 `#FDECEE` / 深 `#402626` |
| 强调色（图标） | 成功 `#36D167`；异常 `#E25B5B` |
| 标题 | `20.sp` `SemiBold` |
| 摘要 | `14.sp` `Medium`，`titleColor.copy(alpha = 0.68f)` |
| 装饰图标 | `128.dp`，`offset(x = 28.dp, y = 34.dp)`，`Alignment.BottomEnd` |
| 内边距 | `16.dp` |
| 点击反馈 | `pressFeedbackType = PressFeedbackType.Tilt`（大卡用倾斜，小卡用 `Sink`） |

**为什么这里允许硬编码颜色**：状态卡的「成功绿/异常红」需要比主题语义色更高的饱和度与更明确的语义，
用 `tertiaryContainer`/`errorContainer` 会偏灰、状态不够醒目。这是 3 个项目的共同选择。
代价是必须自己写 `isDark` 分支 —— 所以**一定要连前景色一起给**，否则深色模式下文字会糊在底色上。
写完记得加注释说明（`miuix-lint.mjs` 的 W1 也要求这样）。

**如果不想承担这个代价**，安全的替代方案是用纯语义色：

```kotlin
Card(
    colors = CardDefaults.defaultColors(
        color = if (active) MiuixTheme.colorScheme.tertiaryContainer
        else MiuixTheme.colorScheme.errorContainer,
    ),
) {
    Text(if (active) "已激活" else "未激活", color = MiuixTheme.colorScheme.onTertiaryContainer)
    // 注意：errorContainer 对应的前景是 onErrorContainer
}
```

---

## 3. ② 快捷入口行

2–4 个等宽卡片，`weight(1f)` 平分宽度，展示一个**数字/短状态**。

```kotlin
@Composable
fun QuickActionRow(
    countA: String,
    countB: String,
    onOpenFeatureA: () -> Unit,
    onOpenFeatureB: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuickActionCard("提供商", countA, Modifier.weight(1f), onOpenFeatureA)
        QuickActionCard("样式", countB, Modifier.weight(1f), onOpenFeatureB)
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(modifier = modifier, onClick = onClick, pressFeedbackType = PressFeedbackType.Sink) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}
```

| 项 | 值 |
| :-- | :-- |
| 卡片间距 | `Arrangement.spacedBy(12.dp)` |
| 最小高度 | `heightIn(min = 96.dp)`（数字可能为空，高度不能塌） |
| 标签 | `16.sp` `SemiBold` `onSurfaceVariantSummary` |
| 数值 | `28.sp` `Bold` `onSurface` |
| 列数 | 2 列最常见；4 列时把字号降到 22.sp，标签降到 13.sp |

**3–4 列网格**用 `Row` 嵌套，或直接 `LazyVerticalGrid`（`GridCells.Fixed(4)`）。

---

## 4. ③ 信息卡（只读 key-value）

```kotlin
data class InfoItem(val title: String, val content: String)

@Composable
fun SystemInfoCard(items: List<InfoItem>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            items.forEachIndexed { index, item ->
                InfoText(
                    title = item.title,
                    content = item.content,
                    bottomPadding = if (index == items.lastIndex) 0.dp else 24.dp,
                )
            }
        }
    }
}

@Composable
private fun InfoText(title: String, content: String, bottomPadding: Dp) {
    Text(
        text = title,
        fontSize = MiuixTheme.textStyles.headline1.fontSize,  // 17sp
        fontWeight = FontWeight.Medium,
        color = MiuixTheme.colorScheme.onSurface,
    )
    Text(
        text = content,
        fontSize = MiuixTheme.textStyles.body2.fontSize,      // 14sp
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding),
    )
}
```

| 项 | 值 |
| :-- | :-- |
| 条间距 | `24.dp`（最后一条 `0.dp`） |
| 标签 | 17sp `Medium` `onSurface` |
| 值 | 14sp `onSurfaceVariantSummary`，`top = 2.dp` |
| 卡片内边距 | `16.dp`（`Card` 默认 `insideMargin = 0`，必须自己加） |
| 分隔线 | **不要**（信息量小，分隔线反而碎） |

如果信息条数多（>6）且是对齐的表格，用 `BasicComponent(title, endActions = { Text(value) })` 更紧凑：

```kotlin
BasicComponent(
    title = "版本",
    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    endActions = {
        Text(item.content, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    },
)
```

---

## 5. ④ 功能入口列表

与设置页同一套规则（`preferences.md`）：`SmallTitle` + `Card(12.dp)` + `ArrowPreference`。
首页通常只放 2–4 条，其余在「设置」页。

需要「图标 + 标题 + 描述」时，用 `ArrowPreference(startAction = ...)`：

```kotlin
ArrowPreference(
    title = "使用历史",
    summary = "查看最近 100 条记录",
    startAction = { Icon(MiuixIcons.Recent, contentDescription = null) },
    onClick = onOpenHistory,
)
```

需要**整块可点、内容较多**时，用「内容卡」而不是 `ArrowPreference`：

```kotlin
@Composable
fun HistoryEntryCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("使用历史", fontWeight = FontWeight.SemiBold)
            Text(
                text = "查看最近播放记录",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
```

---

## 6. 首页的顶栏

| 形态 | 用什么 | 说明 |
| :-- | :-- | :-- |
| 应用名 + 设置入口 | `TopAppBar(title = "应用名", actions = { IconButton { Icon(MiuixIcons.Settings, "设置") } })` | 最常见 |
| 大标题随滚动折叠 | 同上，靠 `scrollBehavior` + `largeTitle` | 首页内容多时更原生 |
| 顶部搜索 | `SearchBar` + `InputField` 放在 `TopAppBar` 下方或 `actions` 里 | 条目多、有明确搜索场景时 |

首页顶栏**不要**放 `navigationIcon`（没有上一级）。

---

## 7. 首次使用 / 状态未就绪的引导

首页是「引导」的最佳落点，两种做法：

### 7.1 顶部 Banner 卡（轻量）

```kotlin
@Composable
fun TipCard(text: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.tertiaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(MiuixIcons.Info, contentDescription = null, tint = MiuixTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                color = MiuixTheme.colorScheme.onTertiaryContainer,
                style = MiuixTheme.textStyles.body2,
            )
            IconButton(onClick = onDismiss) {
                Icon(MiuixIcons.Close, contentDescription = "关闭提示")
            }
        }
    }
}
```

放在状态卡之后、快捷入口之前。

### 7.2 进入即弹 `OverlayDialog`（强制）—— XiaomiHelper 的做法

环境不满足时（模块未激活 / 被系统禁用 / 无 root / 系统版本不满足）有 4 个独立 dialog，
用 `LaunchedEffect(envState)` 按优先级依次判定，只显示最高优先级的那一个：

```kotlin
LaunchedEffect(envState) {
    when {
        envState.canWork -> hideAllDialogs()
        !envState.isModuleActivated -> showInactiveDialog.value = true
        !envState.isModuleEnabled -> showDisabledDialog.value = true
        !envState.isRootRequirementSatisfied -> showRootDialog.value = true
        !envState.isSystemVersionRequirementSatisfied -> showSystemVersionDialog.value = true
    }
}

OverlayDialog(
    show = showInactiveDialog.value,
    title = "模块未激活",
    summary = "请在 LSPosed 中启用本模块并重启作用域应用。",
    onDismissRequest = { showInactiveDialog.value = false },
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton("忽略", onClick = { showInactiveDialog.value = false }, modifier = Modifier.weight(1f))
        TextButton(
            text = "去激活",
            onClick = { openLSPosed(); showInactiveDialog.value = false },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
}
```

要点：

- `LaunchedEffect` 里加 `delay(500)`（XiaomiHelper 有）可以避免启动瞬间弹窗闪烁；
- 弹窗必须有「忽略/稍后」，不要做成无法关闭的死路；
- 「去激活」类跳转用 `context.startActivity(Intent(...))`，注意 `ActivityNotFoundException` 兜底。

---

## 8. 首页变体

| 变体 | 差异 |
| :-- | :-- |
| **工具型**（下载器 / 转换器） | 状态卡改为「任务状态 / 空闲」，快捷入口改为「选择文件 / 开始 / 设置」 |
| **内容型**（歌词 / 媒体） | 状态卡改为「当前播放/当前歌词」，第 ④ 段换成内容卡列表 |
| **带 Logo 大图** | StatusBarLyric 在 ① 之前放一张 `Card { Image(fillMaxWidth()) }` 作为 Logo，然后才是 master switch 卡 |
| **带搜索** | 顶栏 `actions` 放搜索图标 → 展开 `SearchBar`；搜索结果用第 ④ 段的结构渲染 |
| **单页应用**（无 Tab） | 首页本身就是唯一页面，把「设置」也做成第 ④ 段的一个入口 |

---

## 9. 常见错误

| 错误 | 后果 | 正确 |
| :-- | :-- | :-- |
| 首页用 `Column(verticalScroll)` | 无越界回弹、无滚动吸附、长列表卡顿 | `LazyColumn` |
| 每张卡单独 `horizontal = 16.dp`，第 ④ 段却用 12.dp | 页面左右不齐 | 同一页统一一个值，默认 12.dp |
| 状态卡只有底色没有前景色对 | 深色模式文字糊 | 底色/前景色成对给，并跑 `isDark` 分支 |
| 快捷入口卡不给 `heightIn(min)` | 数字为空时高度塌陷、行高跳动 | `heightIn(min = 96.dp)` |
| 状态卡不设固定高度 | 状态切换时页面跳动 | `height(140.dp)` |
| 引导弹窗无「忽略」 | 用户被卡死 | 至少给一个可关闭/稍后的出口 |
| 首页顶栏放了返回按钮 | 语义错误（首页无上一级） | 左侧留空或放 Logo |
| 首页把所有功能都堆进来 | 首屏过载 | 首页只放 2–4 个入口，其余进设置页 |

---

## 10. 真实参考

> 📍 **provenance 说明**：下表路径是产出本 skill 的机器上的克隆（`/root/miuix/repos/`），**仅用于追溯结论出处**。
> 换一台机器这些路径就不存在 —— **不要把「去读这些文件」当作任务步骤**，它们不在你的工作区。
> 需要抄实现时，以本 skill 的片段 + 你自己项目的既有代码为准。


| 内容 | 文件 |
| :-- | :-- |
| 状态卡 + 快捷入口 + 信息卡 + 内容卡（最完整的首页） | `/root/miuix/repos/lyricon/app/src/main/kotlin/io/github/proify/lyricon/app/ui/tabs/HomeTab.kt` |
| 首页 = Logo 卡 + 主开关卡 + 条件展开的子卡 | `/root/miuix/repos/StatusBarLyric/app/src/main/kotlin/statusbar/lyric/ui/page/HomePage.kt` |
| 环境引导弹窗（4 种状态） | `/root/miuix/repos/XiaomiHelper/app/src/main/kotlin/dev/lackluster/mihelper/app/screen/MainPage.kt` |
| 首页顶栏 + 搜索 + 分组 | `/root/miuix/repos/HyperStar/app/src/main/java/com/yunzia/hyperstar/ui/screen/pagers/main/Home.kt` |
| 官方 example 首页（搜索 + 下拉菜单 + 长列表） | `/root/miuix/repos/miuix-lib/example/shared/src/commonMain/kotlin/MainPage.kt` |
