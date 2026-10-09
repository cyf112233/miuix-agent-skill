# 设置页体系（miuix-preference）

导入根：`top.yukonga.miuix.kmp.preference.*`（个别在 `top.yukonga.miuix.kmp.menu.*`）。

**设置页的「正确长相」只有一种：`SmallTitle` 分组标题 + `Card` 分组容器 + `XxxPreference` 行 + 组内 16.dp 缩进分割线。** 8 个真实项目全部如此。

---

## 1. 完整标准设置页

```kotlin
@Composable
fun SettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "设置",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
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

            item { SmallTitle("外观") }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    SwitchPreference(
                        title = "深色模式",
                        summary = "手动指定明暗外观",
                        checked = state.darkMode,
                        onCheckedChange = viewModel::setDarkMode,
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                    OverlaySpinnerPreference(
                        title = "主题色",
                        summary = "动态取色或指定种子色",
                        items = themeItems,
                        selectedIndex = state.themeIndex,
                        onSelectedIndexChange = viewModel::setThemeIndex,
                    )
                }
            }

            item { SmallTitle("行为") }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    SwitchPreference(
                        title = "开机自启动",
                        checked = state.autoStart,
                        onCheckedChange = viewModel::setAutoStart,
                        enabled = state.hasPermission,
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                    SliderPreference(
                        title = "动画时长",
                        summary = "单位：毫秒",
                        value = state.duration,
                        valueText = "${state.duration.toInt()} ms",
                        valueRange = 0f..1000f,
                        onValueChange = viewModel::setDuration,
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                    ArrowPreference(
                        title = "高级设置",
                        summary = "实验性功能",
                        onClick = { navigate(Route.Advanced) },
                    )
                }
            }

            item { SmallTitle("其他") }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    ArrowPreference(title = "关于", summary = "版本 1.0.0", onClick = onOpenAbout)
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                    ArrowPreference(
                        title = "恢复默认设置",
                        titleColor = BasicComponentDefaults.titleColor(
                            color = MiuixTheme.colorScheme.error,
                        ),
                        onClick = { showResetDialog = true },
                    )
                }
            }

            item { Spacer(Modifier.size(12.dp)) }
        }

        OverlayDialog(
            show = showResetDialog,
            title = "恢复默认设置",
            summary = "所有自定义设置将被清除，且不可撤销。",
            onDismissRequest = { showResetDialog = false },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton("取消", onClick = { showResetDialog = false }, modifier = Modifier.weight(1f))
                TextButton(
                    text = "恢复",
                    onClick = { viewModel.reset(); showResetDialog = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
```

---

## 2. Preference 组件表

| 组件 | 用途 | 右侧控件 |
| :-- | :-- | :-- |
| `SwitchPreference` | 开关设置项 | `Switch` |
| `ArrowPreference` | 跳转/展开指示 | 右箭头 |
| `CheckboxPreference` | 多选 | `Checkbox` |
| `RadioButtonPreference` | 单选 | `RadioButton` |
| `SliderPreference` | 连续数值 | `Slider` |
| `RangeSliderPreference` | 区间数值 | `RangeSlider` |
| `OverlaySpinnerPreference` | 下拉选择（新窗口风格、支持二级） | 值 + 箭头 |
| `OverlayDropdownPreference` | 下拉选择（简单平铺） | 值 + 箭头 |
| `OverlayDropdownMenu` | 动作菜单（不显示当前值） | 箭头 |
| `OverlayIconDropdownMenu` | 图标按钮触发的动作菜单 | 图标按钮 |

所有 `XxxPreference` 共享的公共参数（来自 `BasicComponent`）：

| 参数 | 类型 | 默认 | 说明 |
| :-- | :-- | :-- | :-- |
| `title` | `String` | — | 标题（必填） |
| `summary` | `String?` | `null` | 摘要/说明文字 |
| `titleColor` | `BasicComponentColors` | `onBackground` | 标题色（含禁用色） |
| `summaryColor` | `BasicComponentColors` | `onSurfaceVariantSummary` | 摘要色 |
| `startAction` | `@Composable (() -> Unit)?` | `null` | 左侧插槽（图标等） |
| `endActions` | `@Composable RowScope.() -> Unit` | `{}` / `null` | 右侧插槽（在控件之前） |
| `bottomAction` | `(@Composable () -> Unit)?` | `null` | 行下方内容（展开区） |
| `insideMargin` | `PaddingValues` | `PaddingValues(16.dp)` | 行内边距 |
| `enabled` | `Boolean` | `true` | 是否可用 |
| `holdDownState` | `Boolean` | `false` | 由外部驱动的按下态 |

> 用 `titleColor = BasicComponentDefaults.titleColor(color = MiuixTheme.colorScheme.error)` 做危险项，**不要**写 `Color.Red`。

### `SwitchPreference`

```kotlin
@Composable @NonRestartableComposable
fun SwitchPreference(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable RowScope.() -> Unit = {},
    bottomAction: (@Composable () -> Unit)? = null,
    switchColors: SwitchColors = SwitchDefaults.switchColors(),
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
)
```

```kotlin
SwitchPreference(
    title = "自动更新",
    summary = "仅在 Wi-Fi 下检查更新",
    checked = autoUpdate,
    onCheckedChange = { autoUpdate = it },
)
```

### `ArrowPreference`

```kotlin
@Composable @NonRestartableComposable
fun ArrowPreference(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable RowScope.() -> Unit = {},
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    onClick: (() -> Unit)? = null,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
)
```

```kotlin
ArrowPreference(
    title = "关于",
    summary = "版本 1.0.0",
    startAction = { Icon(MiuixIcons.Info, contentDescription = null) },
    onClick = onOpenAbout,
)
```

在箭头左侧插值：

```kotlin
ArrowPreference(
    title = "当前版本",
    endActions = {
        Text("0.9.4", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    },
    onClick = { },
)
```

### `CheckboxPreference` / `RadioButtonPreference`

```kotlin
@Composable @NonRestartableComposable
fun CheckboxPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    checkboxColors: CheckboxColors = CheckboxDefaults.checkboxColors(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    checkboxLocation: CheckboxLocation = CheckboxLocation.Start,   // Start / End
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
)

@Composable @NonRestartableComposable
fun RadioButtonPreference(
    title: String,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    summary: String? = null,
    colors: RadioButtonPreferenceColors = RadioButtonPreferenceDefaults.radioButtonPreferenceColors(),
    radioButtonColors: RadioButtonColors = RadioButtonDefaults.radioButtonColors(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    radioButtonLocation: RadioButtonLocation = RadioButtonLocation.Start, // Start / End
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
)
```

单选组标准写法：

```kotlin
Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
    options.forEachIndexed { index, option ->
        if (index > 0) {
            HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }
        RadioButtonPreference(
            title = option.title,
            summary = option.summary,
            selected = selectedIndex == index,
            onClick = { selectedIndex = index },
        )
    }
}
```

### `SliderPreference` / `RangeSliderPreference`

```kotlin
@Composable @NonRestartableComposable
fun SliderPreference(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    valueText: String? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    reverseDirection: Boolean = false,
    sliderHeight: Dp = SliderDefaults.MinHeight,
    sliderColors: SliderColors = SliderDefaults.sliderColors(),
    hapticEffect: SliderDefaults.SliderHapticEffect = SliderDefaults.DefaultHapticEffect,
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
)

@Composable @NonRestartableComposable
fun RangeSliderPreference(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    /* ... 同上，无 reverseDirection ... */
)
```

```kotlin
SliderPreference(
    title = "字号缩放",
    summary = "影响所有界面文字",
    value = scale,
    valueText = "%.1f×".format(scale),
    valueRange = 0.5f..2f,
    steps = 14,
    onValueChange = { scale = it },
)
```

### 下拉选择

```kotlin
// 平铺（List<String>）
@Composable
fun OverlayDropdownPreference(
    items: List<String>,
    selectedIndex: Int,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    dropdownColors: DropdownColors = DropdownDefaults.dropdownColors(),
    startAction: @Composable (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    maxHeight: Dp? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    renderInRootScaffold: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onSelectedIndexChange: ((Int) -> Unit)? = null,
)

// 富文本 / 选中态（List<DropdownItem>），支持二级级联（DropdownEntry / List<DropdownEntry>）
@Composable
fun OverlaySpinnerPreference(
    items: List<DropdownItem>,
    selectedIndex: Int,
    title: String,
    modifier: Modifier = Modifier,
    /* ... 同上 + spinnerColors, showValue, collapseOnSelection ... */
    onSelectedIndexChange: ((Int) -> Unit)? = null,
)
```

**怎么选**：

| 场景 | 用法 |
| :-- | :-- |
| 简单字符串列表 | `OverlayDropdownPreference(items = listOf("A","B"), selectedIndex, title, onSelectedIndexChange)` |
| 需要自定义每项文案/禁用/图标 | `OverlaySpinnerPreference(items = listOf(DropdownItem(...)))` |
| 需要二级子菜单 | `OverlaySpinnerPreference(entries = listOf(DropdownEntry(...)))`，其中某项用 `DropdownItem(children = listOf(...))` |
| 选项特别多、想用弹窗而非浮层 | 传 `dialogButtonString = "完成"` 的重载（弹窗模式） |

```kotlin
val items = remember { listOf("跟随系统", "浅色", "深色") }
OverlayDropdownPreference(
    title = "外观模式",
    summary = "选择应用的明暗外观",
    items = items,
    selectedIndex = modeIndex,
    onSelectedIndexChange = { modeIndex = it },
)
```

### 动作菜单

```kotlin
@Composable
fun OverlayDropdownMenu(
    entries: List<DropdownEntry>,
    title: String,
    modifier: Modifier = Modifier,
    /* ... 同 BasicComponent 公共参数 ... */
    maxHeight: Dp? = null,
    enabled: Boolean = true,
    renderInRootScaffold: Boolean = true,
    collapseOnSelection: Boolean = entries.size <= 1,
    onExpandedChange: ((Boolean) -> Unit)? = null,
)

@Composable
fun OverlayIconDropdownMenu(
    entries: List<DropdownEntry>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxHeight: Dp? = null,
    dropdownColors: DropdownColors = DropdownDefaults.dropdownColors(),
    renderInRootScaffold: Boolean = true,
    collapseOnSelection: Boolean = entries.size <= 1,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    backgroundColor: Color = Color.Unspecified,
    cornerRadius: Dp = IconButtonDefaults.CornerRadius,
    minHeight: Dp = IconButtonDefaults.MinHeight,
    minWidth: Dp = IconButtonDefaults.MinWidth,
    content: @Composable () -> Unit,   // 触发按钮内容（通常是 Icon）
)
```

```kotlin
// 顶栏「更多」菜单
actions = {
    OverlayIconDropdownMenu(
        entries = listOf(
            DropdownEntry(
                items = listOf(
                    DropdownItem(text = "刷新", onClick = { refresh() }),
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

---

## 3. 自定义 Preference

需要项目特有的一行设置项时，**基于 `BasicComponent` 封装**，不要手搓 `Row`。参考 `SwitchPreference` 的做法（控件放 `endActions`，整行 `onClick`，正确的 `Role`）：

```kotlin
@Composable
fun SegmentPreference(
    title: String,
    summary: String? = null,
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BasicComponent(
        modifier = modifier,
        title = title,
        summary = summary,
        enabled = enabled,
        endActions = {
            TabRow(
                tabs = options,
                selectedTabIndex = selectedIndex,
                onTabSelected = onSelectedIndexChange,
                minWidth = 62.dp,
                maxWidth = 84.dp,
                height = 45.dp,
                cornerRadius = 8.dp,
                itemSpacing = 5.dp,
            )
        },
    )
}
```

`BasicComponent` 的 `titleColor` / `summaryColor` 是 `BasicComponentColors(color, disabledColor)`，用工厂构造：

```kotlin
BasicComponentDefaults.titleColor(color = MiuixTheme.colorScheme.primary)
BasicComponentColors(color = MiuixTheme.colorScheme.primary, disabledColor = MiuixTheme.colorScheme.disabledOnSecondaryVariant)
```

> ⚠️ **绝不要**把 `BasicComponent` 的内部实现改成 `Row + weight(1f)`。它使用自定义 `Layout`（2:5:3 权重 + intrinsic 测量）来保证 title/summary 在 start/end 内容溢出时的排版；换成 `Row` 会导致文字逐字竖排。

---

## 4. 分组规则速查

| 项 | 值 | 说明 |
| :-- | :-- | :-- |
| 分组标题 | `SmallTitle("…")` | 默认 `insideMargin = PaddingValues(28.dp, 8.dp)` |
| 分组容器 | `Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp))` | 12.dp 是压倒性共识 |
| 列表首行留白 | `item { Spacer(Modifier.size(12.dp)) }` | 第一个 `SmallTitle` 之前 |
| 列表末行留白 | `item { Spacer(Modifier.size(12.dp)) }` | 最后一个 Card 之后（可省，因 Card 自带 bottom 12.dp） |
| 组内分割线 | `HorizontalDivider(Modifier.fillMaxWidth().padding(horizontal = 16.dp))` | 与文字左对齐 |
| 组内最后一项 | **不加**分割线 | — |
| 危险操作 | `titleColor = BasicComponentDefaults.titleColor(color = colorScheme.error)` | 不要写死红色 |
| 图标 | `startAction = { Icon(MiuixIcons.X, contentDescription = null) }` | `contentDescription = null`（装饰性） |

---

## 5. 常见错误

| 错误 | 后果 | 正确 |
| :-- | :-- | :-- |
| `Row { Text("开关"); Switch(...) }` | 无整行点击、无按下态、内边距与其它行不一致 | `SwitchPreference(title = ..., checked = ..., onCheckedChange = ...)` |
| 每个 Preference 外面各自包一个 `Card` | 分组碎裂、圆角成堆 | 一个 `Card` 装一组 |
| `Card` 上再传 `cornerRadius = 12.dp` | 偏离设计规范 | 用默认 16.dp |
| 分割线用 `HorizontalDivider()` 全宽 | 与文字错位，视觉割裂 | 加 `padding(horizontal = 16.dp)`。**注意作用域**：这条只管「分组 `Card` 内、Preference 行之间」；内容卡内部的分割线应当全宽（见 `scenario-list.md`） |
| 用 `LazyColumn` 的 `verticalArrangement = Arrangement.spacedBy(12.dp)` 代替 Card `bottom` padding | 组内条目也被撑开 | 组间距由 Card 的 `bottom = 12.dp` 负责 |
| 把 `OverlayDialog` 写在 `Scaffold` 外面 | 弹不出来（日志可能无报错） | 放进 `Scaffold` 内容 lambda 内 |
| 把 `OverlayDialog` 放在 `LazyColumn` 的 `item {}` 里 | 滚动回收时弹窗消失 | 作为 `LazyColumn` 的兄弟节点 |
| `SwitchPreference(onCheckedChange = null)` | 该项不可交互（有些组件允许 `null` 表示只读，但语义要明确） | 需要只读时同时 `enabled = false` |
