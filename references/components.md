# 组件手册（签名 · 默认值 · 最小示例）

所有签名摘自 miuix `0.9.4` 源码。`Scaffold`、`TopAppBar`、`SmallTitle`、`Card`、`HorizontalDivider`、`PullToRefresh`、`NavigationBar`、`NavigationRail` 已在 `layout.md` 详述，此处不重复。

导入根：
- `top.yukonga.miuix.kmp.basic.*` — 基础组件
- `top.yukonga.miuix.kmp.overlay.*` — `Overlay*` 浮层（需宿主在 `Scaffold` 内）
- `top.yukonga.miuix.kmp.window.*` — `Window*` 浮层（**不需要** `Scaffold`）
- `top.yukonga.miuix.kmp.layout.*` — `DialogDefaults`、`BottomSheetDefaults`（易漏！不在 basic 里）
- `top.yukonga.miuix.kmp.utils.*` — `PressFeedbackType`、`SinkFeedback`、`TiltFeedback`、overScroll/scrollEndHaptic
- `top.yukonga.miuix.kmp.theme.*` — `MiuixTheme`、`ThemeController`、`lightColorScheme`/`darkColorScheme`
- `top.yukonga.miuix.kmp.preference.*` — `XxxPreference`
- `top.yukonga.miuix.kmp.menu.*` / `popup.*` — 下拉菜单

**没有任何 `top.yukonga.miuix.kmp.*` 通配导入的写法，逐个 import。**

---

## 1. 容器

### `Card`

```kotlin
// 非交互
@Composable @NonRestartableComposable
fun Card(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CardDefaults.CornerRadius,      // 16.dp
    insideMargin: PaddingValues = CardDefaults.InsideMargin, // PaddingValues(0.dp)
    colors: CardColors = CardDefaults.defaultColors(),
    content: @Composable ColumnScope.() -> Unit,
)

// 可交互（整卡点击/长按）
@Composable
fun Card(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CardDefaults.CornerRadius,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    colors: CardColors = CardDefaults.defaultColors(),
    pressFeedbackType: PressFeedbackType = PressFeedbackType.None,
    showIndication: Boolean = false,
    holdDownState: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
)
```

```kotlin
Card(
    modifier = Modifier
        .padding(horizontal = 12.dp)
        .padding(bottom = 12.dp),
) {
    SwitchPreference(title = "开关", checked = on, onCheckedChange = { })
}

// 自定义底色（用语义色，不要写死）
Card(colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.tertiaryContainer)) { /* ... */ }

// 整卡点击 + 下沉反馈
Card(
    onClick = { open() },
    pressFeedbackType = PressFeedbackType.Sink,
) { /* ... */ }
```

> `insideMargin` 默认是 **0**：卡片的内边距由内部每个 Preference（默认 16.dp）提供。自己往 `Card` 里放 `Text` 时要显式 `Modifier.padding(16.dp)` 或传 `insideMargin = PaddingValues(16.dp)`。

### `Surface`

```kotlin
@Composable @NonRestartableComposable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = SurfaceDefaults.Shape,              // RectangleShape
    color: Color = MiuixTheme.colorScheme.surface,
    contentColor: Color = MiuixTheme.colorScheme.onSurface,
    border: BorderStroke? = null,
    shadowElevation: Dp = SurfaceDefaults.ShadowElevation, // 0.dp
    content: @Composable () -> Unit,
)
// 另有 onClick 重载（可交互 Surface）
```

### `BasicComponent`（自定义设置行的基石）

```kotlin
@Composable
fun BasicComponent(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin, // PaddingValues(16.dp)
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    role: Role? = null,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
)
// 另有 content: @Composable ColumnScope.() -> Unit 重载（无 title/summary，全自定义内容）
```

```kotlin
BasicComponent(
    title = "自定义项",
    summary = "说明文字",
    startAction = {
        Icon(MiuixIcons.Info, contentDescription = null, tint = MiuixTheme.colorScheme.primary)
    },
    endActions = {
        Text("值", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    },
    onClick = { /* ... */ },
)
```

> ⚠️ `BasicComponent` 内部使用**自定义 `Layout`**（2:5:3 权重 + intrinsic 测量）来保证 title/summary 溢出时的行为。**永远不要**把它换成 `Row + weight(1f)` 手写实现。详细见 `preferences.md`。

---

## 2. 按钮

### `Button`

```kotlin
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp = ButtonDefaults.CornerRadius,          // 16.dp
    minWidth: Dp = ButtonDefaults.MinWidth,                  // 58.dp
    minHeight: Dp = ButtonDefaults.MinHeight,                // 40.dp
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    insideMargin: PaddingValues = ButtonDefaults.InsideMargin, // PaddingValues(horizontal = 16.dp, vertical = 13.dp)
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = LocalIndication.current,
    content: @Composable RowScope.() -> Unit,
)
```

```kotlin
Button(onClick = onSave) { Text("保存") }                      // 默认 = 浅灰底
Button(onClick = onSave, colors = ButtonDefaults.buttonColorsPrimary()) { Text("保存") }
```

### `TextButton`

```kotlin
@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp = ButtonDefaults.CornerRadius,
    minWidth: Dp = ButtonDefaults.MinWidth,
    minHeight: Dp = ButtonDefaults.MinHeight,
    colors: TextButtonColors = ButtonDefaults.textButtonColors(),
    insideMargin: PaddingValues = ButtonDefaults.InsideMargin,
    textStyle: TextStyle? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = LocalIndication.current,
)
```

```kotlin
TextButton("取消", onClick = { dismiss() }, modifier = Modifier.weight(1f))
TextButton(
    text = "确定",
    onClick = { confirm() },
    modifier = Modifier.weight(1f),
    colors = ButtonDefaults.textButtonColorsPrimary(),
)
```

**对话框/底部抽屉的按钮区标准写法**：`Row(horizontalArrangement = Arrangement.spacedBy(12.dp))` + 每个按钮 `Modifier.weight(1f)`，主操作放右侧且用 `*Primary()`。

### `IconButton`

```kotlin
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    holdDownState: Boolean = false,
    backgroundColor: Color = Color.Unspecified,
    cornerRadius: Dp = IconButtonDefaults.CornerRadius,   // 40.dp
    minHeight: Dp = IconButtonDefaults.MinHeight,         // 40.dp
    minWidth: Dp = IconButtonDefaults.MinWidth,           // 40.dp
    content: @Composable () -> Unit,
)
```

### `FloatingActionButton` / `FloatingToolbar`

```kotlin
FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    containerColor: Color = MiuixTheme.colorScheme.primary,
    shadowElevation: Dp = FloatingActionButtonDefaults.ShadowElevation, // 4.dp
    minWidth: Dp = FloatingActionButtonDefaults.MinWidth,   // 60.dp
    minHeight: Dp = FloatingActionButtonDefaults.MinHeight, // 60.dp
    content: @Composable () -> Unit,
)

FloatingToolbar(
    modifier: Modifier = Modifier,
    color: Color = FloatingToolbarDefaults.defaultColor(), // colorScheme.surfaceContainer
    cornerRadius: Dp = FloatingToolbarDefaults.CornerRadius, // 50.dp
    outSidePadding: PaddingValues = FloatingToolbarDefaults.OutSidePadding, // PaddingValues(12.dp, 8.dp)
    shadowElevation: Dp = 4.dp,
    showDivider: Boolean = false,
    content: @Composable () -> Unit,
)
```

---

## 3. 文本与标题

### `Text`

```kotlin
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,                 // 默认跟随主题
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyles.current.main,   // 17sp Normal
)
// 另有 AnnotatedString 重载与 color: ColorProducer 重载
```

```kotlin
Text("默认样式（17sp）")
Text("区块标题", style = MiuixTheme.textStyles.title3)
Text("次级说明", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onBackgroundVariant)
```

### `SmallTitle`

见 `layout.md` §6。

---

## 4. 输入

### `TextField`

```kotlin
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    insideMargin: DpSize = TextFieldDefaults.InsideMargin,     // DpSize(16.dp, 16.dp)
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(),
    cornerRadius: Dp = TextFieldDefaults.CornerRadius,         // 16.dp
    label: String = "",
    useLabelAsPlaceholder: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = MiuixTheme.textStyles.main,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(colors.borderColor),
)
```

另有两个重载：`TextFieldState`（新式状态对象，支持 `lineLimits`/`inputTransformation`/`outputTransformation`/`onKeyboardAction`）与 `TextFieldValue`。

```kotlin
// 简单表单
var name by rememberSaveable { mutableStateOf("") }
TextField(
    value = name,
    onValueChange = { name = it },
    label = "名称",
    singleLine = true,
    modifier = Modifier.fillMaxWidth(),
)

// 新式状态对象（推荐用于复杂表单/校验）
val state = rememberTextFieldState()
TextField(
    state = state,
    label = "账号",
    lineLimits = TextFieldLineLimits.SingleLine,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    onKeyboardAction = KeyboardActionHandler { focusManager.moveFocus(FocusDirection.Down) },
)

// 密码框
TextField(
    value = pwd,
    onValueChange = { pwd = it },
    label = "密码",
    visualTransformation = PasswordVisualTransformation(),
    trailingIcon = {
        IconButton(onClick = { visible = !visible }) {
            Icon(if (visible) MiuixIcons.Hide else MiuixIcons.Show, contentDescription = null)
        }
    },
)
```

> 密码显隐、`ImeAction.Next` 焦点链、`VisualTransformation` 掩码在 Updater-KMP 的 `LoginDialog.kt` 里有完整实现，可作范本。

### `SearchBar` + `InputField`

```kotlin
@Composable
fun SearchBar(
    inputField: @Composable () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    insideMargin: DpSize = SearchBarDefaults.InsideMargin,   // DpSize(12.dp, 0.dp)
    expanded: Boolean = false,
    outsideEndAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
)

@Composable
fun InputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    color: Color = MiuixTheme.colorScheme.surfaceContainerHigh,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
)
```

```kotlin
var query by remember { mutableStateOf("") }
var expanded by remember { mutableStateOf(false) }

SearchBar(
    inputField = {
        InputField(
            query = query,
            onQueryChange = { query = it },
            onSearch = { /* 提交搜索 */ },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            label = "搜索",
        )
    },
    onExpandedChange = { expanded = it },
    expanded = expanded,
) {
    // 展开时显示的候选/结果列表
}
```

---

## 5. 选择控件

### `Switch`

```kotlin
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    colors: SwitchColors = SwitchDefaults.switchColors(),
    enabled: Boolean = true,
)
```

> 设置页**优先用 `SwitchPreference`**，不要手搓 `Row { Text(); Switch() }`。

### `Checkbox`

```kotlin
@Composable
fun Checkbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    colors: CheckboxColors = CheckboxDefaults.checkboxColors(),
    enabled: Boolean = true,
)
```

`state` 用 `androidx.compose.ui.state.ToggleableState`（`On`/`Off`/`Indeterminate`）。

```kotlin
Checkbox(state = ToggleableState(checked), onClick = { checked = !checked })
```

### `RadioButton`

```kotlin
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    colors: RadioButtonColors = RadioButtonDefaults.radioButtonColors(),
    enabled: Boolean = true,
)
```

### `Slider` / `RangeSlider` / `VerticalSlider`

```kotlin
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    reverseDirection: Boolean = false,
    height: Dp = SliderDefaults.MinHeight,                 // 28.dp
    colors: SliderColors = SliderDefaults.sliderColors(),
    hapticEffect: SliderDefaults.SliderHapticEffect = SliderDefaults.DefaultHapticEffect, // Edge
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
)

@Composable
fun RangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    height: Dp = SliderDefaults.MinHeight,
    colors: SliderColors = SliderDefaults.sliderColors(),
    hapticEffect: SliderDefaults.SliderHapticEffect = SliderDefaults.DefaultHapticEffect,
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
)

@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    /* ... 同上，width 代替 height，另有 effect: Boolean = false ... */
)
```

`SliderDefaults`：`MinHeight = 28.dp`、`KeyPointRadius = 3.855.dp`、`DefaultHapticEffect = SliderHapticEffect.Edge`。

设置页用 `SliderPreference` / `RangeSliderPreference`（见 `preferences.md`）。

### `NumberPicker`

```kotlin
@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    range: IntRange = 0..10,
    label: (Int) -> String = { it.toString() },
    visibleItemCount: Int = 5,
    wrapAround: Boolean = false,
    colors: NumberPickerColors = NumberPickerDefaults.colors(),
    textStyle: TextStyle = MiuixTheme.textStyles.title1,
    itemHeight: Dp = NumberPickerDefaults.ItemHeight, // 45.dp
)
```

### `TabRow` / `TabRowWithContour`

```kotlin
@Composable
fun TabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: TabRowColors = TabRowDefaults.tabRowColors(),
    minWidth: Dp = TabRowDefaults.TabRowMinWidth,   // 76.dp
    maxWidth: Dp = TabRowDefaults.TabRowMaxWidth,   // 98.dp
    height: Dp = TabRowDefaults.TabRowHeight,       // 42.dp
    cornerRadius: Dp = TabRowDefaults.TabRowCornerRadius, // 12.dp
    itemSpacing: Dp = 9.dp,
    contentAlignment: Alignment = Alignment.Center,
    listState: LazyListState? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
)
// TabRowWithContour：更紧凑的描边风格（height 45.dp, cornerRadius 8.dp, itemSpacing 5.dp）
```

```kotlin
val tabs = listOf("全部", "已启用", "已禁用")
var selected by rememberSaveable { mutableIntStateOf(0) }
Column {
    TabRow(tabs = tabs, selectedTabIndex = selected, onTabSelected = { selected = it })
    when (selected) { /* 内容 */ }
}
```

### `BreadcrumbBar`

```kotlin
@Composable
fun BreadcrumbBar(
    items: List<BreadcrumbItem>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    highlightIndex: Int = items.lastIndex,
    enabled: Boolean = true,
    colors: BreadcrumbBarColors = BreadcrumbBarDefaults.breadcrumbBarColors(),
    insideMargin: PaddingValues = BreadcrumbBarDefaults.InsideMargin, // PaddingValues(h 12.dp, v 8.dp)
    itemMaxWidth: Dp = BreadcrumbBarDefaults.ItemMaxWidth,            // 160.dp
    listState: LazyListState? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = LocalIndication.current,
)
```

---

## 6. 反馈与状态

### `Snackbar` / `SnackbarHost`

```kotlin
@Composable
fun SnackbarHost(
    state: SnackbarHostState,
    modifier: Modifier = Modifier,
    canSwipeToDismiss: Boolean = true,
    content: @Composable (SnackbarData) -> Unit = { Snackbar(it) },
)

@Composable
fun Snackbar(
    data: SnackbarData,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = SnackbarDefaults.CornerRadius,      // 16.dp
    colors: SnackbarColors = SnackbarDefaults.snackbarColors(),
    insideMargin: PaddingValues = SnackbarDefaults.InsideMargin, // PaddingValues(all = 12.dp)
)
```

```kotlin
val snackbarHostState = remember { SnackbarHostState() }

Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
    // ...
}

// 在事件回调里
val scope = rememberCoroutineScope()
scope.launch { snackbarHostState.showSnackbar("已保存") }

// 带操作按钮
val result = snackbarHostState.showSnackbar(
    message = "已删除",
    actionLabel = "撤销",
    withDismissAction = true,
)
if (result == SnackbarResult.ActionPerformed) { undo() }
```

### `ProgressIndicator`

```kotlin
@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,   // null = 不确定进度
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.progressIndicatorColors(),
    height: Dp = ProgressIndicatorDefaults.DefaultLinearProgressIndicatorHeight, // 6.dp
)

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.progressIndicatorColors(),
    strokeWidth: Dp = ProgressIndicatorDefaults.DefaultCircularProgressIndicatorStrokeWidth, // 4.dp
    size: Dp = ProgressIndicatorDefaults.DefaultCircularProgressIndicatorSize,               // 30.dp
)

@Composable
fun InfiniteProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray,
    size: Dp = ProgressIndicatorDefaults.DefaultInfiniteProgressIndicatorSize,   // 20.dp
    strokeWidth: Dp = ProgressIndicatorDefaults.DefaultInfiniteProgressIndicatorStrokeWidth, // 2.dp
    orbitingDotSize: Dp = ProgressIndicatorDefaults.DefaultInfiniteProgressIndicatorOrbitingDotSize, // 2.dp
)
```

`InfiniteProgressIndicator` 是接数据时最常用的「转圈」；给 `color = MiuixTheme.colorScheme.primary` 以符合主题。

### `PullToRefresh`

见 `layout.md` §9。

### `Badge` / `BadgedBox`

```kotlin
@Composable
fun BadgedBox(
    badge: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
)

@Composable
fun Badge(
    modifier: Modifier = Modifier,
    containerColor: Color = BadgeDefaults.containerColor, // colorScheme.error
    contentColor: Color = BadgeDefaults.contentColor,     // colorScheme.onError
    content: @Composable (RowScope.() -> Unit)? = null,   // null = 小圆点
)
```

```kotlin
Badge()                    // 6.dp 圆点
Badge { Text("8") }        // 数字角标
BadgedBox(badge = { Badge { Text("3") } }) {
    Icon(MiuixIcons.Messages, contentDescription = "消息")
}
```

`BadgeDefaults`：`Size = 6.dp`、`LargeSize = 16.dp`。

### `Tooltip`

```kotlin
@Composable
fun TooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    state: TooltipState = rememberTooltipState(isPersistent = false),
    enabled: Boolean = true,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    content: @Composable () -> Unit,
)

@Composable
fun RichTooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    state: TooltipState = rememberTooltipState(isPersistent = true),
    title: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    content: @Composable () -> Unit,
)
```

### `ScrollBar`（桌面/大列表）

```kotlin
@ExperimentalScrollBarApi
VerticalScrollBar(adapter = rememberScrollBarAdapter(lazyListState))

@ExperimentalScrollBarApi
HorizontalScrollBar(adapter = rememberScrollBarAdapter(scrollState))
```

需要在文件顶部加 `@file:OptIn(ExperimentalScrollBarApi::class)`。`ScrollBarDefaults`：`ThumbWidth = 3.64.dp`、`ThumbMinLength = 36.dp`。

---

## 7. 颜色选择

```kotlin
@Composable
fun ColorPalette(
    color: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
    rows: Int = 7,
    hueColumns: Int = 12,
    includeGrayColumn: Boolean = true,
    showPreview: Boolean = true,
    cornerRadius: Dp = 16.dp,
    indicatorRadius: Dp = 10.dp,
)

@Composable
fun ColorPicker(
    color: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
    showPreview: Boolean = true,
    hapticEffect: SliderDefaults.SliderHapticEffect = SliderDefaults.DefaultHapticEffect,
    colorSpace: ColorSpace = ColorSpace.HSV,
)
```

主题定制页标准组合：`OverlayDialog` + `ColorPalette` / `ColorPicker`，或 `ColorPickerDialog` 模式（参见 XiaomiHelper 的 `ColorPickerDialog.kt`）。

---

---

> 📂 **本文件只覆盖基础组件（`basic.*`）**。浮层（`overlay.*` / `window.*`）、下拉数据结构、
> 以及 Overlay/Window 两族的对照，见 **`components-overlay.md`**。
## 8. 组件速查：默认值一览

| 组件 | 常量 | 值 |
| :-- | :-- | :-- |
| `CardDefaults` | `CornerRadius` / `InsideMargin` | `16.dp` / `PaddingValues(0.dp)` |
| `ButtonDefaults` | `MinWidth` / `MinHeight` / `CornerRadius` / `InsideMargin` | `58.dp` / `40.dp` / `16.dp` / `PaddingValues(horizontal = 16.dp, vertical = 13.dp)` |
| `IconButtonDefaults` | `MinWidth` / `MinHeight` / `CornerRadius` | `40.dp` / `40.dp` / `40.dp` |
| `TextFieldDefaults` | `CornerRadius` / `InsideMargin` | `16.dp` / `DpSize(16.dp, 16.dp)` |
| `BasicComponentDefaults` | `InsideMargin` | `PaddingValues(16.dp)` |
| `SmallTitleDefaults` | `InsideMargin` | `PaddingValues(28.dp, 8.dp)` |
| `TopAppBarDefaults` | `TitlePadding` / `CollapsedHeight` / `SmallTopAppBarCenterHeight` | `26.dp` / `52.dp` / `50.dp` |
| `NavigationBarDefaults` | `ItemHeight` / `IconSize` / `UnselectedAlpha` | `64.dp` / `26.dp` / `0.4f` |
| `NavigationRailDefaults` | `MinWidth` / `ExpandedWidth` | `80.dp` / `240.dp` |
| `TabRowDefaults` | `TabRowHeight` / `TabRowCornerRadius` | `42.dp` / `12.dp` |
| `SliderDefaults` | `MinHeight` | `28.dp` |
| `DividerDefaults` | `Thickness` | `0.75.dp` |
| `SnackbarDefaults` | `CornerRadius` / `InsideMargin` | `16.dp` / `PaddingValues(all = 12.dp)` |
| `DialogDefaults` | `MaxWidth` / `CornerRadius` / `insideMargin` | `420.dp` / `32.dp` / `DpSize(24.dp, 24.dp)` |
| `BottomSheetDefaults` | `cornerRadius` / `maxWidth` / `insideMargin` | `28.dp` / `640.dp` / `DpSize(24.dp, 0.dp)` |
| `BadgeDefaults` | `Size` / `LargeSize` | `6.dp` / `16.dp` |
| `FloatingActionButtonDefaults` | `MinWidth` / `MinHeight` / `ShadowElevation` | `60.dp` / `60.dp` / `4.dp` |
| `FloatingToolbarDefaults` | `CornerRadius` / `OutSidePadding` | `50.dp` / `PaddingValues(12.dp, 8.dp)` |
| `ProgressIndicatorDefaults` | 线性高 / 圆形直径 / 圆形描边 | `6.dp` / `30.dp` / `4.dp` |
| `ListPopupDefaults` | `MinWidth` / `MinPopupHeight` | `200.dp` / `50.dp` |
| `DropdownDefaults` | `MinHeight` / `MinWidth` | `56.dp` / `200.dp` |
| `SearchBarDefaults` | `InsideMargin` / `InputFieldMinHeight` | `DpSize(12.dp, 0.dp)` / `45.dp` |
| `ScrollBarDefaults` | `ThumbWidth` / `ThumbMinLength` | `3.64.dp` / `36.dp` |
| `SquircleDefaults` | `Extension` | `1.1f` |
