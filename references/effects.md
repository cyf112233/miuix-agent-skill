# 视觉效果与工具修饰符

导入根：`top.yukonga.miuix.kmp.utils.*`、`top.yukonga.miuix.kmp.squircle.*`、`top.yukonga.miuix.kmp.blur.*`。

---

## 1. 滚动：越界回弹

```kotlin
@Stable
fun Modifier.overScrollVertical(
    nestedScrollToParent: Boolean = true,
    isEnabled: () -> Boolean = { true },
): Modifier

@Stable
fun Modifier.overScrollHorizontal(
    nestedScrollToParent: Boolean = true,
    isEnabled: () -> Boolean = { true },
): Modifier
```

```kotlin
LazyColumn(
    modifier = Modifier
        .fillMaxSize()
        .overScrollVertical()
        .nestedScroll(scrollBehavior.nestedScrollConnection),  // 必须在 overScrollVertical 之后
    overscrollEffect = null,                                    // 关掉 Compose 默认效果
) { /* ... */ }
```

- 只在**按压/拖拽手势**期间生效；鼠标滚轮、键盘滚动原样透传（没有松手信号，无法收束回弹）。
- 想全项目自动生效：新版 `MiuixTheme` 已把 `MiuixOverscrollFactory` 设为默认 `OverscrollFactory`，所有标准可滚动组件无需修饰符。**唯一例外**是「内容没超出容器」时不会触发（因为没到真实边界）——这种页面仍需 `overScrollVertical()`。
- 关掉某个滚动组件的效果：`overscrollEffect = null`；关掉整棵子树：`CompositionLocalProvider(LocalOverscrollFactory provides null) { }`。

## 2. 滚动到边界触觉

```kotlin
fun Modifier.scrollEndHaptic(
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
): Modifier
```

```kotlin
LazyColumn(
    modifier = Modifier
        .fillMaxSize()
        .scrollEndHaptic()
        .overScrollVertical(),
) { /* ... */ }
```

快速甩到顶/底时触发一次震动确认。其它可选值见 `androidx.compose.ui.hapticfeedback.HapticFeedbackType`。

> 标准顺序（放一起时）：`overScrollVertical()` → `scrollEndHaptic()` → `nestedScroll(...)`。三者缺一都可接受，但顺序不要颠倒，也不要加两次。

## 3. 按压反馈

```kotlin
@Stable
fun Modifier.pressable(
    interactionSource: MutableInteractionSource?,   // 必填，无默认值（可以显式传 null）
    indication: Indication? = null,                 // 注意：默认是 null，不是 SinkFeedback()
    enabled: Boolean = true,
    role: Role? = null,
    delay: Long? = TAP_INDICATION_DELAY,
)
```

⚠️ **`interactionSource` 没有默认值，所以裸写 `Modifier.pressable()` 会编译失败。** 另外官方文档 `utils.md` 把
`indication` 的默认值写成了 `SinkFeedback()`，与 0.9.4 源码不符（源码是 `null`）——以源码为准：

```kotlin
// 要点：interactionSource 必填；要下沉反馈需显式传 SinkFeedback()
Box(
    modifier = Modifier
        .pressable(interactionSource = null, indication = SinkFeedback())
        .background(MiuixTheme.colorScheme.primaryContainer)
        .size(100.dp),
)
```

| `PressFeedbackType` | 效果 |
| :-- | :-- |
| `None` | 无 |
| `Sink` | 按下时轻微缩小（`SinkFeedback`） |
| `Tilt` | 按触摸位置倾斜（`TiltFeedback`） |

```kotlin
// Card 内置开关（推荐）
Card(onClick = { open() }, pressFeedbackType = PressFeedbackType.Sink) { /* ... */ }

// 自定义组件
Box(
    modifier = Modifier
        .pressable(interactionSource = interactionSource, indication = TiltFeedback())
        .combinedClickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = { },
            onLongClick = { },
        )
        .background(MiuixTheme.colorScheme.primaryContainer)
        .size(100.dp),
)
```

> 用 `pressable` + `combinedClickable` 时必须**共享同一个 `interactionSource`**，否则反馈不跟随按压。

---

## 4. 平滑圆角（squircle）

`miuix-squircle` 已被 `miuix-ui` 传递包含。开启时用运行时着色器画超椭圆（squircle）轮廓；不支持运行时的平台自动回退为等半径圆角。

```kotlin
@Composable fun Modifier.squircleSurface(color: Color, cornerRadius: Dp, extension: Float = SquircleDefaults.Extension): Modifier
@Composable fun Modifier.squircleSurface(color: Color, topStart: Dp, topEnd: Dp, bottomEnd: Dp, bottomStart: Dp, extension: Float = ...): Modifier
@Composable fun Modifier.squircleClip(cornerRadius: Dp, extension: Float = ...): Modifier
@Composable fun Modifier.squircleClip(topStart: Dp, topEnd: Dp, bottomEnd: Dp, bottomStart: Dp, extension: Float = ...): Modifier
@Composable fun Modifier.squircleBorder(width: Dp, color: Color, cornerRadius: Dp, extension: Float = ...): Modifier
@Composable fun Modifier.squircleBackground(color: Color, ...): Modifier
@Composable fun Modifier.absoluteSquircleBackground(clip: Boolean = true, color: Color = ..., ...): Modifier  // 物理角，不受 RTL 翻转
```

```kotlin
// 填充 + 裁剪（等价于 .clip(RoundedCornerShape(r)).background(color)，但轮廓是 squircle）
Box(
    modifier = Modifier
        .squircleSurface(
            color = MiuixTheme.colorScheme.surfaceContainer,
            cornerRadius = 16.dp,
        )
        .padding(16.dp),
) { Text("内容") }

// 只裁剪（已有背景色时）
Box(Modifier.squircleClip(24.dp).background(MiuixTheme.colorScheme.primary))

// 描边
Box(
    Modifier
        .squircleBorder(width = 1.dp, color = MiuixTheme.colorScheme.outline, cornerRadius = 16.dp)
        .padding(12.dp),
)

// 只在需要真正 Shape 值时用普通圆角（dropShadow / Surface 的 shape 参数）
RoundedCornerShape(16.dp)
```

- `SquircleDefaults.Extension = 1.1f`（可用 1f–2f，值越大越「方」）。
- `isSquircleEnabled()` 可查询当前是否真的启用了（运行时着色器可用 + `LocalSquircleEnabled`）。
- **不要**在组件里硬编码 `RoundedCornerShape` 来做卡片底色；Miuix 的 `Card`/`Button` 内部已经用 squircle。

---

## 5. 模糊与背景捕获（miuix-blur）

模糊的本质是「先捕获一片内容作为 backdrop，再把 backdrop 模糊后画在某个形状里」。

### 5.1 捕获 backdrop

```kotlin
@Composable
fun rememberLayerBackdrop(
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),
    onDraw: ContentDrawScope.() -> Unit = DefaultOnDraw,
): LayerBackdrop

fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier
```

```kotlin
val backdrop = rememberLayerBackdrop {
    drawRect(MiuixTheme.colorScheme.surface)   // 打底，避免透明处出现黑边
    drawContent()
}

Box(
    modifier = Modifier
        .fillMaxSize()
        .layerBackdrop(backdrop),   // 捕获这个 Box 的内容
) {
    LazyColumn { /* 内容 */ }

    // 叠在上面的顶栏做毛玻璃
    TopAppBar(
        title = "标题",
        modifier = Modifier.textureBlur(
            backdrop = backdrop,
            shape = RectangleShape,
            blurRadius = 25f,
            colors = BlurDefaults.blurColors(
                blendColors = listOf(
                    BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.8f)),
                ),
            ),
        ),
    )
}
```

### 5.2 应用模糊

```kotlin
fun Modifier.textureBlur(
    backdrop: Backdrop,
    shape: Shape,
    blurRadius: Float = BlurDefaults.BlurRadius,           // 20f（单位 dp，上限 150f）
    noiseCoefficient: Float = BlurDefaults.NoiseCoefficient, // 0.0045f，抗色带
    colors: BlurColors = BlurColors(),
    highlight: Highlight? = null,                           // 玻璃边缘高光
    contentBlendMode: ComposeBlendMode = ComposeBlendMode.SrcOver,
    enabled: Boolean = true,
): Modifier

// 横纵独立半径
fun Modifier.textureBlur(backdrop: Backdrop, shape: Shape, blurRadiusX: Float, blurRadiusY: Float, ...): Modifier

// 完整纹理效果（模糊 + 颜色混合）
fun Modifier.textureEffect(...): Modifier

// 渐进模糊（渐隐边缘）
fun Modifier.progressiveTextureBlur(...): Modifier
```

```kotlin
Box(
    Modifier
        .fillMaxWidth()
        .textureBlur(
            backdrop = backdrop,
            shape = RoundedCornerShape(20.dp),
            blurRadius = 25f,
            colors = BlurDefaults.blurColors(
                blendColors = listOf(
                    BlendColorEntry(color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)),
                ),
                saturation = 1.1f,
            ),
            highlight = if (isSystemInDarkTheme()) {
                Highlight.GlassStrokeMiddleDark
            } else {
                Highlight.GlassStrokeMiddleLight
            },
        )
)
```

### 5.3 关键参数

| 名称 | 说明 |
| :-- | :-- |
| `BlurDefaults.BlurRadius` | `20f` |
| `BlurDefaults.NoiseCoefficient` | `0.0045f` |
| `BlurDefaults.MaxBlurRadius` | `150f`（超出会被 clamp） |
| `BlurColors(blendColors, brightness, contrast, saturation)` | 模糊后的颜色调整；用 `BlurDefaults.blurColors(...)` 工厂构造 |
| `BlendColorEntry(color, blendMode)` | 单层混合色 |
| `Highlight.GlassStroke*Dark/Light` | 玻璃描边高光预设 |

### 5.4 使用规则

1. **先捕获，再模糊**：`rememberLayerBackdrop { drawRect(...); drawContent() }` 挂在内容容器上，模糊层必须是该容器的**子节点且绘制在其上**。
2. 模糊背景的组件**底色必须改成 `Color.Transparent`**，否则会把模糊盖住：
   ```kotlin
   val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
   TopAppBar(title = "标题", color = barColor, modifier = Modifier.textureBlur(...))
   ```
3. 深色/浅色高光要分开取（`Highlight.GlassStrokeMiddleDark` / `...Light`）。
4. **Android 需要 `minSdk 33`**；低版本要么不引入 `miuix-blur`，要么用 `enabled` 参数守卫。
5. 模糊会持续重绘，**不要**给长列表的每一项都加，只给顶栏/底栏/悬浮条这类固定数量的元素。

---

## 6. 弹出层底层工具

一般不需要直接用；`OverlayDialog` / `OverlayBottomSheet` / `OverlayListPopup` 都已封装。

> ⚠️ **下面这些不是 `utils.*` 的顶层函数，而是 `MiuixPopupUtils.Companion` 的成员**（`MiuixPopupUtils.kt` 里
> `class MiuixPopupUtils { companion object { … } }`）。也就是说要写成 `MiuixPopupUtils.DialogLayout(...)`
> 那种形式（或在 `Scaffold` 内直接用已封装的 `Overlay*`），**不要按顶层函数 import**。

```kotlin
// 位于 MiuixPopupUtils.Companion 内（非顶层）
@Composable
fun DialogLayout(
    visible: MutableState<Boolean>,
    enterTransition: EnterTransition? = null,
    exitTransition: ExitTransition? = null,
    enableWindowDim: Boolean = true,
    enableAutoLargeScreen: Boolean = true,
    dimEnterTransition: EnterTransition? = null,
    dimExitTransition: ExitTransition? = null,
    dimAlpha: MutableState<Float>? = null,
    onDismissFinished: (() -> Unit)? = null,
    renderInRootScaffold: Boolean = true,
    content: (@Composable () -> Unit)? = null,
)

@Composable
fun PopupLayout(
    visible: MutableState<Boolean>,
    enterTransition: EnterTransition? = null,
    exitTransition: ExitTransition? = null,
    enableWindowDim: Boolean = true,
    enableBackHandler: Boolean = true,
    /* ... */
    renderInRootScaffold: Boolean = true,
    content: (@Composable () -> Unit)? = null,
)

@Composable
fun MiuixPopupHost()
```

`renderInRootScaffold = true`（默认）表示渲染到**最外层** `Scaffold` 的弹层里，这通常才是想要的（弹层不被中间布局裁剪）。

---

## 7. Pager 手势与弹簧切页

`HorizontalPager` 里嵌竖向列表时，横滑会被列表吃掉。用 `pagerGestureOverride` 解决：

```kotlin
val flingBehavior = PagerDefaults.flingBehavior(
    state = pagerState,
    snapAnimationSpec = PagerNavigationSpringSpec,
)

HorizontalPager(
    state = pagerState,
    modifier = Modifier.pagerGestureOverride(
        pagerState = pagerState,
        flingBehavior = flingBehavior,
        mode = PagerInterceptionMode.CrossAxis,   // 默认
    ),
    userScrollEnabled = false,                     // CrossAxis 模式必须为 false
    flingBehavior = flingBehavior,
    pageNestedScrollConnection = PagerGestureNestedScrollConnection,
) { page -> /* 页面内容（LazyColumn 等） */ }
```

| `PagerInterceptionMode` | 行为 |
| :-- | :-- |
| `CrossAxis`（默认） | 列表滚动/回弹时仍可横滑切页 |
| `Native` | 用 Compose 原生 Pager 手势 |
| `TapToHalt` | 列表惯性滚动时，首次横滑停列表，再次横滑切页 |

点击标签/导航项切页时用弹簧动画：

```kotlin
coroutineScope.launch { pagerState.springAnimateToPage(targetPage) }
```

配套还有 `PagerNavigationSpringSpec`、`PagerGestureNestedScrollConnection`、`springAnimateToPage`。

---

## 8. 平台与设备

```kotlin
enum class Platform { Android, IOS, Desktop, WasmJs, MacOS, Js }

expect fun platform(): Platform

@Composable expect fun getRoundedCorner(): Dp          // 设备屏幕物理圆角；桌面/Web 为 0.dp
@Composable expect fun rememberNavSystemCornerRadius(): Dp   // miuix-nav：导航转场裁剪用
@Composable expect fun platformDialogProperties(): DialogProperties
expect val hasFocusReassignBug: Boolean
```

```kotlin
if (platform() != Platform.MacOS) {
    // 只在非 macOS 上做的事
}

// 让转场圆角跟随设备屏幕
val navCornerRadius = rememberNavSystemCornerRadius()
```

> **注意**：旧版本（≤0.8）里的 `getWindowSize()`、`G2RoundedCornerShape()`、`miuixShape` 等在 0.9.x 已不存在。迁移见 `pitfalls.md`。

---

## 9. 主题级取色（动态取色）

```kotlin
expect fun platformDynamicColors(dark: Boolean): Colors
```

```kotlin
// 想让「跟随系统壁纸」与 Miuix 主题一致时
val base = platformDynamicColors(isSystemInDarkTheme())
```

一般不用直接调：`ThemeController(ColorSchemeMode.MonetSystem)` 内部已处理。真实项目里手动调用是为了**覆盖动态取色下的个别颜色**（例如 lyricon 把 Monet 下过重的 `dividerLine` 改回淡色）：

```kotlin
val base = platformDynamicColors(dark)
val colors = base.copy(dividerLine = if (dark) Color(0xFF393939) else Color(0xFFE0E0E0))
```
