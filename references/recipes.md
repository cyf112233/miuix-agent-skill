# 任务配方

每个配方都是「可直接抄的结构 + 真实项目出处」。

> **关于文中的 `/root/miuix/repos/...` 路径**：那是产出本 skill 的机器上的克隆（`miuix-lib` 是库本体与官方 example，其余 8 个是真实项目），**仅用于追溯结论出处**。
> 换机器就失效，**不要把它当作任务步骤或必读清单**——它们不在你的工作区。

---

> **原来在这里的 8 个配方已经合并进场景文件，不在此重复**（避免同一份代码两处漂移）：
>
> | 原配方 | 现在去哪看 |
> | :-- | :-- |
> | 1 设置页 | `preferences.md` §1（完整可拷贝页面） |
> | 2 关于页 | `scenario-info.md` §1（Hero 视差完整实现） |
> | 4 表单页 + 校验 | `scenario-form.md`（§1 骨架、§2 字段、§3 校验、§4 提交） |
> | 5 带搜索的列表页 | `scenario-list.md` §6（搜索过滤）+ §1（外壳） |
> | 6 多标签页 | `scenario-shell.md`（主框架 + Pager） |
> | 7 加载/空/错误 | `scenario-state.md`（§2 加载、§3 空、§4 错误、§6 优先级） |
> | 11 下拉选择设置项 | `preferences.md` §2「下拉选择」 |
> | 13 下拉刷新 | `scenario-list.md` §9 |
>
> 本文件只保留**跨场景的横向配方**：开关联动、确认/输入弹窗、底部抽屉、顶栏动作菜单、列表项删除确认、项目级可复用容器、Material 3 迁移映射。
>

## 配方 1：开关联动（A 打开才允许操作 B）

**状态推导放在 ViewModel/状态层，UI 只用 `enabled`**：

```kotlin
item {
    Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        SwitchPreference(
            title = "启用自定义规则",
            summary = "关闭后以下设置不生效",
            checked = state.rulesEnabled,
            onCheckedChange = viewModel::setRulesEnabled,
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        ArrowPreference(
            title = "规则列表",
            summary = "${state.rules.size} 条",
            enabled = state.rulesEnabled,           // 联动禁用
            onClick = { navigate(Route.Rules) },
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        SliderPreference(
            title = "阈值",
            value = state.threshold,
            valueText = "${state.threshold.toInt()}",
            enabled = state.rulesEnabled,
            onValueChange = viewModel::setThreshold,
        )
    }
}
```

要点：

- 禁用的行**不要**隐藏，保留让用户知道「打开总开关才会有这些选项」。
- 需要整组变灰时，把 `enabled` 逐行传下去，或包一层 `CompositionLocalProvider(LocalContentColor provides ...)`（Miuix 组件的 `Colors` 都基于 `enabled` 自动降级，不需要手动改色）。
- 需要**批量**控制时用 `AnimatedVisibility` 展开/收起联动项，注意配合 `expandVertically()/shrinkVertically()`。

---

## 配方 2：确认弹窗 / 输入弹窗

### 确认

```kotlin
var show by remember { mutableStateOf(false) }

OverlayDialog(
    show = show,
    title = "删除该条目？",
    summary = "删除后不可恢复。",
    onDismissRequest = { show = false },
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton("取消", onClick = { show = false }, modifier = Modifier.weight(1f))
        TextButton(
            text = "删除",
            onClick = { onDelete(); show = false },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
}
```

### 只读提示（content 仍必填）

```kotlin
OverlayDialog(
    show = show,
    title = "提示",
    summary = "配置已保存。",
    onDismissRequest = { show = false },
) {
    // content 没有默认值，必须给；只读提示放一个「知道了」
    TextButton(
        text = "知道了",
        onClick = { show = false },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
}
```

### 输入

```kotlin
var text by rememberSaveable { mutableStateOf("") }
val state = rememberTextFieldState()

OverlayDialog(
    show = show,
    title = "输入名称",
    onDismissRequest = { show = false },
) {
    Column {
        TextField(
            state = state,
            label = "名称",
            modifier = Modifier.fillMaxWidth(),
            lineLimits = TextFieldLineLimits.SingleLine,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton("取消", onClick = { show = false }, modifier = Modifier.weight(1f))
            TextButton(
                text = "确定",
                onClick = { onConfirm(state.text.toString()); show = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                enabled = state.text.isNotBlank(),
            )
        }
    }
}
```

---

## 配方 3：底部抽屉选择器 / 操作面板

```kotlin
var showSheet by remember { mutableStateOf(false) }

OverlayBottomSheet(
    show = showSheet,
    title = "选择来源",
    onDismissRequest = { showSheet = false },
) {
    Column(Modifier.fillMaxWidth()) {
        options.forEach { option ->
            ArrowPreference(
                title = option.label,
                summary = option.summary,
                onClick = { onSelect(option); showSheet = false },
            )
        }
        TextButton(
            text = "取消",
            onClick = { showSheet = false },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
    }
}
```

需要拖拽抓手/自定义边距时：

```kotlin
OverlayBottomSheet(
    show = showSheet,
    title = "自定义样式",
    backgroundColor = MiuixTheme.colorScheme.surfaceVariant,
    dragHandleColor = MiuixTheme.colorScheme.primary,
    outsideMargin = DpSize(16.dp, 0.dp),
    insideMargin = DpSize(32.dp, 16.dp),
    onDismissRequest = { showSheet = false },
) { /* ... */ }
```

---

## 配方 4：顶栏动作菜单

```kotlin
actions = {
    OverlayIconDropdownMenu(
        entries = listOf(
            DropdownEntry(
                items = listOf(
                    // icon 是 @Composable (Modifier) -> Unit，要传 lambda，不能直接传 ImageVector
                    DropdownItem(
                        text = "刷新",
                        icon = { modifier -> Icon(MiuixIcons.Refresh, contentDescription = null, modifier = modifier) },
                        onClick = { refresh() },
                    ),
                    DropdownItem(text = "分享", onClick = { share() }),
                    DropdownItem(text = "删除", onClick = { delete() }),
                ),
            ),
        ),
    ) {
        Icon(MiuixIcons.More, contentDescription = "更多")
    }
}
```

二级级联：给某个 `DropdownItem` 加 `children = listOf(DropdownItem(...))`，然后用 `OverlayIconCascadingDropdownMenu` / `OverlayCascadingListPopup` 渲染：

```kotlin
OverlayIconCascadingDropdownMenu(
    entries = listOf(
        DropdownEntry(
            items = listOf(
                DropdownItem(text = "排序", children = listOf(
                    DropdownItem(text = "按名称", onClick = { sortByName() }),
                    DropdownItem(text = "按时间", onClick = { sortByTime() }),
                )),
                DropdownItem(text = "刷新", onClick = { refresh() }),
            ),
        ),
    ),
) {
    Icon(MiuixIcons.More, contentDescription = "更多")
}
```

---

## 配方 5：列表项 + 二次确认删除

```kotlin
var pendingDelete by remember { mutableStateOf<Item?>(null) }

items(list, key = { it.id }) { item ->
    Card(
        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
        onLongPress = { pendingDelete = item },
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = { open(item) },
    ) {
        BasicComponent(
            title = item.title,
            summary = item.summary,
            startAction = { Icon(item.icon, contentDescription = null) },
        )
    }
}

pendingDelete?.let { target ->
    OverlayDialog(
        show = true,
        title = "删除「${target.title}」？",
        summary = "删除后不可恢复。",
        onDismissRequest = { pendingDelete = null },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton("取消", onClick = { pendingDelete = null }, modifier = Modifier.weight(1f))
            TextButton(
                text = "删除",
                onClick = { onDelete(target); pendingDelete = null },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}
```

> 弹窗由 `pendingDelete != null` 驱动，且 `OverlayDialog` 位于 `LazyColumn` **之外**，避免 item 回收导致弹窗消失。

---

## 配方 6：项目级可复用页面容器（强烈推荐）

当项目有 5 个以上结构相同的页面时，抽一个 `AppToolBarListContainer`（lyricon 的做法），把 `Scaffold` + `TopAppBar` + `MiuixScrollBehavior` + 滚动修饰符 + 性能最佳实践一次性收敛：

```kotlin
@Composable
fun AppToolBarListContainer(
    title: String,
    canBack: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                navigationIcon = {
                    if (canBack) {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(MiuixIcons.Back, contentDescription = "返回")
                        }
                    }
                },
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = bottomBar,
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
            content = content,
        )
    }
}

// 页面里只写内容
AppToolBarListContainer(title = "提供商", canBack = false) {
    item { SmallTitle("已安装") }
    item { Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) { /* ... */ } }
}
```

原文件：`/root/miuix/repos/lyricon/app/src/main/kotlin/io/github/proify/lyricon/app/compose/AppComposable.kt`

---

## 配方 7：从 Material 3 迁移到 Miuix

### 导入映射

| Material 3 | Miuix |
| :-- | :-- |
| `androidx.compose.material3.Scaffold` | `top.yukonga.miuix.kmp.basic.Scaffold` |
| `…material3.Text` | `…basic.Text` |
| `…material3.Card` / `ElevatedCard` | `…basic.Card` |
| `…material3.TopAppBar` / `CenterAlignedTopAppBar` | `…basic.TopAppBar` / `SmallTopAppBar` |
| `…material3.Button` / `OutlinedButton` / `TextButton` | `…basic.Button` / `TextButton` |
| `…material3.Icon` / `IconButton` | `…basic.Icon` / `…basic.IconButton` |
| `…material3.Switch` / `Checkbox` / `RadioButton` | `…basic.Switch` / `Checkbox` / `RadioButton`（设置页用 `…preference.XxxPreference`） |
| `…material3.Slider` / `RangeSlider` | `…basic.Slider` / `RangeSlider` |
| `…material3.OutlinedTextField` / `TextField` | `…basic.TextField` |
| `…material3.HorizontalDivider` / `Divider` | `…basic.HorizontalDivider` / `VerticalDivider` |
| `…material3.NavigationBar` / `NavigationBarItem` | `…basic.NavigationBar` / `NavigationBarItem`（0.9+ 的 `RowScope` 版本） |
| `…material3.AlertDialog` | `…overlay.OverlayDialog` |
| `…material3.ModalBottomSheet` | `…overlay.OverlayBottomSheet` |
| `…material3.DropdownMenu` / `ExposedDropdownMenuBox` | `…menu.OverlayIconDropdownMenu` / `…preference.OverlayDropdownPreference` |
| `…material3.Snackbar` / `SnackbarHost` | `…basic.Snackbar` / `SnackbarHost` |
| `…material3.CircularProgressIndicator` / `LinearProgressIndicator` | `…basic.CircularProgressIndicator` / `LinearProgressIndicator` / `InfiniteProgressIndicator` |
| `…material3.pulltorefresh.PullToRefreshBox` | `…basic.PullToRefresh` |
| `MaterialTheme.colorScheme.*` | `MiuixTheme.colorScheme.*` |
| `MaterialTheme.typography.*` | `MiuixTheme.textStyles.*` |
| `Icons.Default.*` | `MiuixIcons.*`（需 import 具体名字） |
| `Color(0xFF...)` | 语义色（见 `theme.md` §4） |

### 迁移顺序（建议）

1. 顶层 `MaterialTheme { }` → `MiuixTheme(colors = …)`（或 `ThemeController`）。
2. 顶层 `Scaffold` → Miuix `Scaffold`。**必须先做这一步**，否则 `OverlayDialog` 无处可弹。
3. 列表页 → `SmallTitle` + `Card`(12.dp) + `XxxPreference` 重构；这一步收益最大。
4. 弹窗/底部抽屉 → `OverlayDialog` / `OverlayBottomSheet`。
5. 图标 → `MiuixIcons.*`（逐个 import）。
6. 滚动修饰符补上 `overScrollVertical()` + `scrollEndHaptic()` + `overscrollEffect = null`。
7. 用 `scripts/miuix-lint.mjs` 扫残留。

### 过渡期注意

- 迁到一半时**不要**在同一个 `Row`/`Column` 里混用两套 `Scaffold` 逻辑。
- 可以从「一个 Tab 一个页面」的粒度逐页迁移；Miuix 页面放进 Material 容器里外观是自洽的（Miuix 组件只依赖 `MiuixTheme`，不依赖 Material）。
- 但 Material 组件**不能**放进 Miuix 页面里做「半吊子替换」：会同时拉到两套 `LocalContentColor`/`LocalIndication`，出现按压/文字色异常。
