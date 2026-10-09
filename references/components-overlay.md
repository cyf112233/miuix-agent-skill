# 浮层组件手册（`overlay.*` / `window.*` / 菜单 / 弹窗）

基础组件见 `components.md`。本文件覆盖：`Overlay*` 族、`Window*` 族、下拉数据结构、菜单与弹窗底层工具。

**一句话选型**：需要跟随应用内容/转场 → 用 `Overlay*`（宿主必须在某个 `Scaffold` 的 composition 内）；
需要真正的平台级窗口（可超出应用窗口、不需要 `Scaffold`）→ 用 `Window*`。

---
## 1. `Overlay*` 族（宿主必须在 `Scaffold` 内）

**优先用 `Overlay*`**：跨平台行为一致，跟随应用内容与转场。`Window*` 是同 API 的「真实系统窗口」版本（Android 上可超出应用窗口边界），仅在需要真正独立窗口时使用。

### `OverlayDialog`

```kotlin
@Composable
fun OverlayDialog(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    titleColor: Color = DialogDefaults.titleColor(),
    summary: String? = null,
    summaryColor: Color = DialogDefaults.summaryColor(),
    backgroundColor: Color = DialogDefaults.backgroundColor(),
    enableWindowDim: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    outsideMargin: DpSize = DialogDefaults.outsideMargin,   // DpSize(12.dp, 12.dp)
    insideMargin: DpSize = DialogDefaults.insideMargin,     // DpSize(24.dp, 24.dp)
    defaultWindowInsetsPadding: Boolean = true,
    renderInRootScaffold: Boolean = true,
    maxWidth: Dp = DialogDefaults.MaxWidth,                 // 420.dp
    largeScreen: Boolean? = null,
    cornerRadius: Dp? = null,                               // 默认 DialogDefaults.CornerRadius = 32.dp
    content: @Composable () -> Unit,
)
```

```kotlin
var show by remember { mutableStateOf(false) }

Scaffold {
    ArrowPreference(title = "退出登录", onClick = { show = true })

    OverlayDialog(
        show = show,
        title = "退出登录",
        summary = "确认退出当前账号？",
        onDismissRequest = { show = false },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton("取消", onClick = { show = false }, modifier = Modifier.weight(1f))
            TextButton(
                text = "退出",
                onClick = { logout(); show = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}
```

> ⚠️ `content: @Composable () -> Unit` 是**必填**（没有默认值），所以**不能**写无 content 的 `OverlayDialog(show, title, summary, onDismissRequest)` —— 那会编译失败。纯文本提示也必须给 content，标准写法是自己在 content 里放按钮行：

```kotlin
OverlayDialog(
    show = show,
    title = "提示",
    summary = "配置已保存。",
    onDismissRequest = { show = false },
) {
    TextButton(
        text = "知道了",
        onClick = { show = false },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
}
```

### `OverlayBottomSheet`

```kotlin
@Composable
fun OverlayBottomSheet(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    backgroundColor: Color = BottomSheetDefaults.backgroundColor(),
    enableWindowDim: Boolean = true,
    cornerRadius: Dp = BottomSheetDefaults.cornerRadius,        // 28.dp
    sheetMaxWidth: Dp = BottomSheetDefaults.maxWidth,          // 640.dp
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    outsideMargin: DpSize = BottomSheetDefaults.outsideMargin, // DpSize(0.dp, 0.dp)
    insideMargin: DpSize = BottomSheetDefaults.insideMargin,   // DpSize(24.dp, 0.dp)
    defaultWindowInsetsPadding: Boolean = true,
    dragHandleColor: Color = BottomSheetDefaults.dragHandleColor(),
    allowDismiss: Boolean = true,
    enableNestedScroll: Boolean = true,
    renderInRootScaffold: Boolean = true,
    content: @Composable () -> Unit,
)
```

```kotlin
OverlayBottomSheet(
    show = showSheet,
    title = "选择操作",
    onDismissRequest = { showSheet = false },
) {
    Column(Modifier.fillMaxWidth()) {
        ArrowPreference(title = "选项一", onClick = { showSheet = false })
        ArrowPreference(title = "选项二", onClick = { showSheet = false })
        TextButton(
            text = "关闭",
            onClick = { showSheet = false },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
    }
}
```

### `OverlayListPopup` / `OverlayCascadingListPopup`

```kotlin
@Composable
fun OverlayListPopup(
    show: Boolean,
    popupModifier: Modifier = Modifier,
    popupPositionProvider: PopupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.Start,
    enableWindowDim: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    maxHeight: Dp? = null,
    minWidth: Dp = ListPopupDefaults.MinWidth,   // 200.dp
    renderInRootScaffold: Boolean = true,
    content: @Composable () -> Unit,
)

@Composable
fun OverlayCascadingListPopup(
    show: Boolean,
    entries: List<DropdownEntry>,
    onDismissRequest: () -> Unit,
    /* ... alignment, maxHeight, minWidth, dropdownColors, collapseOnSelection ... */
)
```

`ListPopupDefaults`：`MinWidth = 200.dp`、`MinPopupHeight = 50.dp`、`DropdownPositionProvider`。

## 2. 下拉数据结构（`DropdownItem` / `DropdownEntry`）

```kotlin
@Stable
data class DropdownEntry(
    val items: List<DropdownItem>,
    val enabled: Boolean = true,
)

@Stable
data class DropdownItem(
    val text: String,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val onClick: (() -> Unit)? = null,
    val icon: @Composable ((Modifier) -> Unit)? = null,   // 注意：是 lambda，不是 ImageVector
    val summary: String? = null,
    val children: List<DropdownItem>? = null,             // 非空 → 变成二级级联入口
) {
    // 兼容旧写法的次构造函数
    constructor(
        icon: @Composable ((Modifier) -> Unit)? = null,
        title: String? = null,
        summary: String? = null,
    )
}

DropdownItem(text = "选项", selected = true, onClick = { })
DropdownItem(title = "选项")  // 兼容写法，等价 text = title
DropdownEntry(items = listOf(DropdownItem(...), DropdownItem(...)))
```

- **二级级联**靠 `DropdownItem.children`，不是 `DropdownEntry` 的额外字段。
- `DropdownItem.icon` 类型是 `@Composable (Modifier) -> Unit`，要写 `icon = { m -> Icon(..., modifier = m) }`。
- 用 `DropdownEntry` / `List<DropdownEntry>` 的组件（`Overlay*Cascading*`）支持级联；用 `List<String>` 的重载只支持平铺。
- `children` 列表建议 `remember` 稳定化，避免不必要重组。
- `DropdownDefaults`：`MinHeight = 56.dp`、`MinWidth = 200.dp`、`MaxItemTextWidth = 216.dp`、`InsideHorizontalPadding = 20.dp`。

---

## 3. `Window*` 族（真正的平台窗口，不需要 `Scaffold`）

与 `Overlay*` 一一对应，参数基本一致，差别：

| | `Overlay*` | `Window*` |
| :-- | :-- | :-- |
| 承载 | 应用内 `Scaffold` 弹层 | 真实平台窗口 |
| 需要 `Scaffold` | **是** | 否 |
| 跨平台一致性 | 高 | 依赖平台实现 |
| 超出应用窗口 | 否 | 是 |

```kotlin
WindowDialog(show = show, title = "标题", summary = "说明", onDismissRequest = { show = false }) { /* ... */ }
WindowBottomSheet(show = show, title = "标题", onDismissRequest = { show = false }) { /* ... */ }
WindowDropdownPreference(...)
WindowSpinnerPreference(...)
WindowListPopup(...)
WindowDropdownMenu(...) / WindowIconDropdownMenu(...) / WindowIconCascadingDropdownMenu(...)
```

**默认真空里选 `Overlay*`。**

---

---

## 4. `OverlayDropdownPopup` / `OverlayDropdownDialog`（手动控制弹层）

`Overlay*Preference` 已经把「点一行 → 弹出选项」封装好了，通常不用直接调这两个。
需要自己控制弹出时机（例如点顶栏图标弹选项、或把选项浮在自定义锚点上）时才用：

```kotlin
// 浮层形式：贴在锚点旁
@Composable
fun OverlayDropdownPopup(
    entries: List<DropdownEntry>,
    show: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    maxHeight: Dp?,
    dropdownColors: DropdownColors,
    renderInRootScaffold: Boolean,
    collapseOnSelection: Boolean = entries.size <= 1,
)

// 弹窗形式：居中对话框，适合选项多/需要标题与确认按钮
@Composable
fun OverlayDropdownDialog(
    entries: List<DropdownEntry>,
    title: String,
    dialogButtonString: String,
    show: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    dropdownColors: DropdownColors,
    popupModifier: Modifier = Modifier,
    renderInRootScaffold: Boolean = true,
    collapseOnSelection: Boolean = entries.size <= 1,
)
```

`window.WindowDropdownPopup` / `WindowDropdownDialog` 是它们的窗口版，参数少一个 `renderInRootScaffold`。

---

## 5. Overlay / Window 两族对照（选错会白写很多代码）

| | `Overlay*` | `Window*` |
| :-- | :-- | :-- |
| 承载 | 应用内 `Scaffold` 的弹层 | 真实平台窗口 |
| `requiresScaffoldHost` | **true**（必须有 `Scaffold`） | **false**（不需要 `Scaffold`） |
| 跟随应用转场/旋转 | 是，天然一体 | 否，独立窗口 |
| 可超出应用窗口边界 | 否 | 是 |
| 跨平台一致性 | 高（同一套 Compose 实现） | 依赖平台实现 |
| 典型用法 | 绝大多数对话框/抽屉/下拉 | 需要系统级窗口语义时 |

⚠️ **真实项目的实际选择并不统一**：InstallerX 0.9.4 用了 **23 次 `WindowDialog`、0 次 `overlay.OverlayDialog`**；
而 lyricon / Updater-KMP 主要用 `Overlay*`。所以**先看项目既有代码用哪族，跟着走**，不要凭本文件硬选。

两者共同的参数语义（`show` / `onDismissRequest` / `onDismissFinished` / `enableWindowDim` / `outsideMargin` / `insideMargin`）是一致的。
