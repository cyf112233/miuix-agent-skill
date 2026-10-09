# 导航

## 0. 选型

| 方案 | 何时用 |
| :-- | :-- |
| **`miuix-nav`**（`top.yukonga.miuix.kmp:miuix-nav`） | **默认推荐**。零第三方依赖（不依赖 androidx navigation3），连续深度栈，1:1 跟手返回手势，原生 Miuix 转场 |
| androidx `navigation-compose` | 项目已有大量既有代码、需要 type-safe routes + deep link 生态 |
| 自研/状态机（`var screen by remember { mutableStateOf(...) }`） | 只有 2–3 个层级、没有真实返回栈需求的小工具 |

**新项目一律用 `miuix-nav`。** 它与 Miuix 的圆角裁剪、调暗 scrim、滑动返回手感是配套设计的。

---

## 1. 最小可用示例

```kotlin
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack

@Serializable
sealed interface Route : NavKey {
    @Serializable data object Home : Route
    @Serializable data class Detail(val id: String) : Route
}

@Composable
fun App() {
    AppTheme {
        val backStack = rememberNavBackStack<Route>(Route.Home)
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
        ) {
            entry<Route.Home> {
                HomeScreen(onOpen = { id -> backStack.add(Route.Detail(id)) })
            }
            entry<Route.Detail> { route ->
                DetailScreen(route.id, onBack = { backStack.removeLastOrNull() })
            }
        }
    }
}
```

### 三条硬性要求

1. **路由必须 `@Serializable`**。`rememberNavBackStack` 用 `kotlinx.serialization` 保存/恢复返回栈；key 类型没标 `@Serializable` 会在首次组合直接抛 `SerializationException`。
2. **必须显式传超类型**：`rememberNavBackStack<Route>(Route.Home)`。
   写成 `rememberNavBackStack(Route.Home)` 会把 `T` 推断为 `Route.Home` 单例类型，之后 push 其它子类型时**保存/恢复阶段**才报错（Android 上表现为退后台崩溃）。
3. **key 的 `toString()` 必须由值派生**（用 `data object` / `data class`）。保留默认恒等 `toString()` 的路由能跑通运行时会话，但进程死亡后字符串变了，该条目的 `rememberSaveable` 状态会静默重置。

`rememberNavBackStack` 返回 `NavBackStack`（本质是 `SnapshotStateList<NavKey>`），可以直接 `add` / `removeLastOrNull` / `removeAt`。

---

## 2. `Navigator` 封装（推荐，照抄官方 example）

`miuix-nav` 的核心不做「连点防抖」，**这是应用层职责**：同一路由值连续推两次是重复 contentKey，会被运行时拒绝。用一个极薄的 `Navigator` 包住返回栈即可：

```kotlin
class Navigator(
    val backStack: NavBackStack,
) {
    /** 幂等 push：已在栈上则跳过（避免双击推入重复路由） */
    fun push(key: NavKey) {
        if (key !in backStack) backStack.add(key)
    }

    fun replace(key: NavKey) {
        if (backStack.isNotEmpty()) backStack[backStack.lastIndex] = key else backStack.add(key)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun popUntil(predicate: (NavKey) -> Boolean) {
        while (backStack.size > 1 && !predicate(backStack.last())) backStack.removeAt(backStack.lastIndex)
    }

    fun current() = backStack.lastOrNull()
    fun backStackSize() = backStack.size
}
```

再挂到 `CompositionLocal` 上，避免层层传参（官方 example 的做法）：

```kotlin
val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No navigator provided") }

@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack<Route>(Route.Home)
    val navigator = remember(backStack) { Navigator(backStack) }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavDisplay(backStack = backStack, onBack = { navigator.pop() }) {
            entry<Route.Home> { HomeScreen() }
            entry<Route.Settings> { SettingsPage(onBack = { navigator.pop() }) }
        }
    }
}

// 页面内
val navigator = LocalNavigator.current
IconButton(onClick = { navigator.pop() }) { Icon(MiuixIcons.Back, contentDescription = "返回") }
```

> 需要同一路由的**多个实例**（如推入 3 个详情页）时，给路由带唯一值：`Route.Detail(id = uniqueId)`，不要依赖重复 key。

---

## 3. `NavDisplay`

```kotlin
@Composable
fun NavDisplay(
    backStack: NavBackStack,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { backStack.removeLastOrNull() },
    transition: NavTransition = NavTransitions.MiuixDefault,
    effects: NavDisplayEffects = NavDisplayEffects(),
    content: NavEntryBuilder.() -> Unit,
)

@Composable @NonRestartableComposable
fun NavDisplay(
    navController: NavController,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { navController.pop() },
    transition: NavTransition = NavTransitions.MiuixDefault,
    effects: NavDisplayEffects = NavDisplayEffects(),
    content: NavEntryBuilder.() -> Unit,
)
```

### `entry<T>`

```kotlin
NavDisplay(backStack) {
    entry<Route.Home> { HomeScreen() }                       // 无参路由：lambda 无参数
    entry<Route.Detail> { route -> DetailScreen(route.id) }  // 带参路由：lambda 收到路由实例
    entry<Route.Sheet>(transition = NavTransitions.Modal, swipeDismiss = NavSwipeDirection.TopToBottom) { Sheet() }
    entry<Route.Confirm>(swipeDismiss = NavSwipeDirection.None) { Confirm() } // 仅按钮返回
    entry<Route.Item>(contentKey = { route -> route.id }) { ItemScreen() }    // 自定义状态作用域键
}
```

---

## 4. 转场

| 预设 | 效果 |
| :-- | :-- |
| `NavTransitions.MiuixDefault`（默认） | 全宽滑动 + 四分之一宽视差 + 被覆盖层轻微透明衰减 |
| `NavTransitions.Modal` | 上滑，下层保留可见（底部抽屉式页面） |
| `NavTransitions.None` | 瞬时无动画 |

```kotlin
// 全局
NavDisplay(backStack, transition = NavTransitions.Modal) { /* ... */ }

// 按路由覆盖
entry<Route.Detail>(transition = NavTransitions.Modal) { DetailScreen() }
```

自定义转场是「深度的纯函数」，写在 `graphicsLayer` 风格的延迟读取块里（读 `relativeDepth` 不触发重组）：

```kotlin
val fade = navGraphicsTransition { scope ->
    val d = scope.relativeDepth
    alpha = if (d <= 0f) (1f + d).coerceIn(0f, 1f) else 1f - d.coerceIn(0f, 1f)
}
```

`NavTransitionScope` 暴露：`relativeDepth`、`role`、`change`、`gesture`、`isRunning`、`settle`、`layoutSize`、`layoutDirection`、`density`。

**连续出入栈**：整栈由单个 `animatedTop` 驱动，一次 push/pop 多层会播放为一段连续运动：

```kotlin
backStack.add(Route.Detail("1")); backStack.add(Route.Detail("2")); backStack.add(Route.Detail("3"))
while (backStack.size > 1) backStack.removeAt(backStack.lastIndex) // 连续退栈
```

---

## 5. 横向滑动返回

`dismissDirection` **默认全部关闭**，必须按路由显式开启。

```kotlin
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection

NavDisplay(backStack) {
    entry<Route.Home>(swipeDismiss = NavSwipeDirection.LeftToRight) { HomeScreen() }
}
```

| 值 | 手指方向 |
| :-- | :-- |
| `LeftToRight` | 向右 —— **LTR 下的返回滑动** |
| `RightToLeft` | 向左 —— RTL 下的返回滑动 |
| `TopToBottom` | 向下 —— 底部上滑 Modal 的关闭 |
| `BottomToTop` | 向上 |
| `None`（默认） | 禁用 |

方向是**物理屏幕方向，不随 layout direction 镜像**，所以要自己判断：

```kotlin
val swipeBack = when {
    !enableSwipeBack -> NavSwipeDirection.None
    LocalLayoutDirection.current == LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
    else -> NavSwipeDirection.LeftToRight
}
```

---

## 6. 正交效果 `NavDisplayEffects`

| 属性 | 默认 | 说明 |
| :-- | :-- | :-- |
| `enableCornerClip` | `true` | 顶层 entry 运动期间按平滑圆角裁剪 |
| `cornerClipRadius` | `0.dp` | 传 `rememberNavSystemCornerRadius()` 跟随设备屏幕圆角 |
| `cornerClipMode` | `Leading` | `Leading`（滑动式，只裁与屏幕边缘相接的前缘）/ `All`（整页缩放卡片式） |
| `dimAmount` | `0.5f` | 全屏调暗最大 alpha；`0f` 关闭 |
| `blockInputDuringTransition` | `false` | 转场中途屏蔽触摸 |
| `backdropColor` | `Unspecified` | 所有 entry 图层背后的纯色底（卡片式转场传 `colorScheme.background`） |

```kotlin
NavDisplay(
    backStack = backStack,
    effects = NavDisplayEffects(
        cornerClipRadius = rememberNavSystemCornerRadius(),
        cornerClipMode = NavCornerClipMode.All,
        dimAmount = 0.32f,
        backdropColor = MiuixTheme.colorScheme.background,
    ),
) { /* ... */ }
```

`NavDisplayEffects.Default` 即上表默认；`NavDisplayEffects.None` 全关。

---

## 7. 生命周期与 ViewModel

- 每个 entry 运行在**独立**的 `LifecycleOwner` + `ViewModelStoreOwner` 下：`viewModel()`、`collectAsStateWithLifecycle()` 天然按屏幕作用域。
- `RESUMED` 只属于「静止的唯一顶层」；被覆盖层、进入/离开层为 `STARTED`；正在移除的为 `CREATED`。
- **手势驱动期间所有层封顶 `STARTED`** —— 依赖 `RESUMED` 的逻辑不会因手指停在阈值附近反复触发。
- ViewModel store 由 display 持有：被覆盖的 entry 仍在栈上就保留 ViewModel；pop 后清空。

```kotlin
@Composable
fun DetailScreen(id: String) {
    val vm: DetailViewModel = viewModel()                 // 自动按 entry 作用域
    val state by vm.state.collectAsStateWithLifecycle()
    // ...
}
```

---

## 8. 嵌套 `NavDisplay`

可以在 entry 内部再放一个 `NavDisplay`（每个 tab 各持一栈）。返回/状态/生命周期都正确嵌套。

```kotlin
entry<Route.Tabs> {
    val innerBackStack = rememberNavBackStack<InnerRoute>(InnerRoute.List)
    NavDisplay(backStack = innerBackStack, onBack = { innerBackStack.removeLastOrNull() }) {
        entry<InnerRoute.List> { ListScreen() }
        entry<InnerRoute.Edit> { EditScreen() }
    }
}
```

规则：

- 内层栈只有在该 entry 是**交互顶层**时才优先消费返回；内层到根后穿透给外层。
- 内层的返回栈/状态/ViewModel 都属于宿主 entry 的命名空间，pop 宿主即整体销毁。
- **滑动冲突**：两个 display 都没消费位移时，**外层优先认领**。要把可交互内层栈放在**外层根 entry** 上，或在宿主路由用 `swipeDismiss = NavSwipeDirection.None` 关掉外层手势。

---

## 9. 多窗格（大屏）

`miuix-nav` 有意保持单一扁平栈；自适应在 `NavDisplay` 外围组合（完整代码见 `layout.md` §7）。要点：窗格必须 `clipToBounds()`、窗格内 `cornerClipRadius = 0.dp`、去掉侧栏已消费的 insets。

- 详情窗格**必须有一个占位根路由**（空返回栈会被拒绝）。
- 跨窗格迁移 key 会丢状态；要保住状态就用 app 级 `movableContentOf` 移动**整个** `NavDisplay`。
- `NavDisplay` 放在 `Row`/`Box` 里时会以自身容器测量，转场与手势自动以窗格为参照。

---

## 10. 向上一屏回传结果

v1 **没有**内置 `navigateForResult` / `setResult`。用你自己的状态机制，推荐在栈旁挂一个按 `requestKey` 寻址的总线：

```kotlin
class ResultBus {
    private val channels = mutableMapOf<String, MutableSharedFlow<Any?>>()
    fun channel(requestKey: String) = channels.getOrPut(requestKey) {
        MutableSharedFlow(replay = 1)   // replay = 1：pop 前发出的结果不会丢
    }
    fun send(requestKey: String, value: Any?) { channel(requestKey).tryEmit(value) }
}

// 调用方
val bus = remember { ResultBus() }
LaunchedEffect(Unit) { bus.channel("pick").collect { result -> /* 处理 */ } }
// 被调方 pop 前
bus.send("pick", value)
navigator.pop()
```

---

## 11. v1 明确不支持（不要提前依赖）

| 能力 | 状态 |
| :-- | :-- |
| Dialog / 底部表单作为「目的地」 | 不支持 —— 用 `OverlayDialog` / `OverlayBottomSheet`（`components.md` §8） |
| 跨目的地共享元素转场（SharedTransition） | 不支持 |
| KSP / 注解式路由注册 | 不支持 —— 用 `entry<T> { }` DSL |
| `NavController` 内置结果通道 | 不支持 —— 见 §10 |

---

## 12. 与其他导航共存

如果项目已经在用 androidx `navigation-compose`：**不要在同一处混用两套返回栈**。

- 迁移期可以外层用 androidx nav、内层某个入口换成 `NavDisplay`（嵌套是支持的，但只保证 Miuix display 内部的返回/状态正确）。
- 彻底切换时，把 `@Serializable` 的路由层级整体搬过来，`NavHost` → `NavDisplay`，`composable<T>` → `entry<T>`，`navController.navigate(r)` → `navigator.push(r)`，`popBackStack()` → `navigator.pop()`。

---

## 13. 手势返回底层：`PredictiveBackHandler`

`miuix-nav` 已经内置了返回手势（见 §5），**通常不需要自己调**。只有在「非导航场景」需要跟手的预测返回时
（例如自定义的侧滑面板、跟手关闭的浮层）才用它：

```kotlin
import top.yukonga.miuix.kmp.nav.gesture.PredictiveBackHandler

@Composable
fun SwipeToClosePanel(onClose: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler(
        enabled = true,
        onProgress = { events: Flow<NavBackEvent> ->
            // 跟随手指：events 流式上报进度，自己驱动 UI
            events.collect { event -> progress = event.progress }
        },
        onCommit = { onClose() },                    // 松手提交
        onCancel = { progress = 0f },                // 松手取消，回到静止态
    )

    // progress 用来驱动 translationX / alpha 等
}
```

签名（`top.yukonga.miuix.kmp.nav.gesture`）：

```kotlin
@Composable
fun PredictiveBackHandler(
    enabled: Boolean,
    onProgress: suspend (Flow<NavBackEvent>) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
)
```

平台差异（与 §5 的表一致）：Android / iOS 是**连续**手势（逐帧 progress）；Desktop 的 ESC 是**离散**触发，
不会有中间 progress；Web 的来源由调用方定义。

## 14. 条目级状态作用域：`EntryStateContent`

`miuix-nav` 的 `rememberSaveable` 默认按 entry 的 `contentKey` 分作用域。当你想在 **entry 内部**再切分出
一块独立可保存状态（例如同一页面里的两个可切换子视图各自记住状态），用 `EntryStateContent`：

```kotlin
import top.yukonga.miuix.kmp.nav.state.EntryStateContent

@Composable
fun DetailScreen(tab: String) {
    // 每个 tab 各自一份 rememberSaveable 作用域
    EntryStateContent(contentKey = tab) {
        val scroll = rememberSaveable { mutableIntStateOf(0) }
        // …
    }
}
```

```kotlin
@Composable
fun EntryStateContent(
    contentKey: Any,
    content: @Composable () -> Unit,
)
```

> 注意：`contentKey` 的 `toString()` 必须**由值派生且全栈唯一**（用 `data class` / `data object` / 字符串常量即可）。
> 两个不相等却打印出同一字符串的 key 会被运行时拒绝；用默认恒等 `toString()` 的类虽然能跑通会话，
> 但进程死亡后会解析出全新字符串，该块状态被静默重置（与 §1 对路由 key 的要求一致）。
