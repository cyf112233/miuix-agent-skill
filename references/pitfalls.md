# 反模式、常见错误与版本迁移

---

## 1. 反模式清单（AI 最容易犯的那些）

### 主题与颜色

| ❌ 反模式 | 后果 | ✅ 正确 |
| :-- | :-- | :-- |
| `Text("…", color = Color.Black)` | 深色模式不可见 | 不传 `color`（默认跟主题），或 `MiuixTheme.colorScheme.onBackground` |
| `Card(colors = CardDefaults.defaultColors(color = Color(0xFF1A3825)))` | 深色模式刺眼 | 用 `tertiaryContainer` / `errorContainer` 等语义色；品牌色必须给明暗两套并注释 |
| `MaterialTheme.colorScheme.primary` | 编译失败或拿到 Material 的值（两套主题不一致） | `MiuixTheme.colorScheme.primary` |
| `MaterialTheme.typography.bodyLarge` | 同上 | `MiuixTheme.textStyles.body1` |
| 每个页面自己 `MiuixTheme(colors = …)` | 嵌套覆盖、颜色来源混乱 | 只在 `AppTheme` 里包一次；局部改色才再包（并注释原因） |

### 组件

| ❌ 反模式 | 后果 | ✅ 正确 |
| :-- | :-- | :-- |
| `import androidx.compose.material3.*` | 观感与 Miuix 不一致、双份 `LocalContentColor`/`LocalIndication` | 换 Miuix 同名组件 |
| `Row { Text("开关"); Switch(checked, onCheckedChange) }` | 无整行点击、无按下态、内边距不对齐 | `SwitchPreference(title, checked, onCheckedChange)` |
| 手搓 `Row` 实现设置行 | 同上一行 | `BasicComponent(title, summary, startAction, endActions, onClick)` |
| 把 `BasicComponent` 的内部 `Layout` 改成 `Row + weight(1f)` | **文字逐字竖排**（官方明确记录过的坑） | 保持原实现 |
| `Icons.Default.Settings` | 图标风格不一致 | `MiuixIcons.Settings`（记得逐个 import） |
| `Icon(MiuixIcons.Home)` 未 import | `Unresolved reference` | `import top.yukonga.miuix.kmp.icon.extended.Home` |
| `Icon(MiuixIcons.Link, contentDescription = "")` | 无障碍 lint 报错 | 装饰性用 `null`，功能性写文案 |
| 在 `ArrowPreference(endActions = { SwitchPreference(...) })` 里塞一个 Preference | **运行时崩**：`IllegalArgumentException: maxWidth must be >= than minWidth`（`BasicComponent` 的 `maxIntrinsicHeight` 里） | 开关行直接 `SwitchPreference(startAction = …)`；箭头行直接 `ArrowPreference`。`endActions` 只放 `Icon`/`Text` 这类叶子节点 |
| 主按钮直接 `Button(onClick)` | **默认灰底**（`secondaryVariant`），看起来像禁用 | `colors = ButtonDefaults.buttonColors(color = colorScheme.primary, contentColor = colorScheme.onPrimary)` |
| 给每个快捷入口配一种颜色（绿/黄/红/紫圆形图标底） | 一眼不像原生；Miuix 只有 primary + error 两个强语义色 | 图标底统一 `tertiaryContainer` + `primary`，或 `surfaceContainerHigh` + `onSurface`；状态色只留给状态卡（`status-colors.md`） |

### 布局

| ❌ 反模式 | 后果 | ✅ 正确 |
| :-- | :-- | :-- |
| `Column(Modifier.verticalScroll(rememberScrollState()))` 做设置页 | 无懒加载、无越界回弹、无滚动吸附 | `LazyColumn` + 滚动修饰符 |
| `LazyColumn(Modifier.padding(horizontal = 12.dp))` | 分割线/标题与卡片文字错位 | 横向间距放在 `Card` 上 |
| `LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp))` | 组内条目也被撑开 | 组间距用 `Card` 的 `padding(bottom = 12.dp)` |
| 分组 `Card` 内、Preference 行之间的 `HorizontalDivider()` 全宽 | 与文字错位 | `Modifier.fillMaxWidth().padding(horizontal = 16.dp)`。内容卡内部的分割线则相反，用全宽 |
| 忘了 `scrollBehavior` 传 `TopAppBar` | 不折叠、不吸附 | `TopAppBar(scrollBehavior = scrollBehavior)` |
| 忘了 `.nestedScroll(scrollBehavior.nestedScrollConnection)` | 顶栏不随滚动变化 | 加上，且放在 `scrollEndHaptic()` 之后 |
| 手写 `Modifier.statusBarsPadding()` 于有 `TopAppBar` 的页面 | 双重 inset，内容下移 | 只用 `paddingValues.calculateTopPadding()` |
| `Card(cornerRadius = 12.dp)` 偏离设计 | 圆角不统一 | 用默认 16.dp |

### 弹层与状态

| ❌ 反模式 | 后果 | ✅ 正确 |
| :-- | :-- | :-- |
| `OverlayDialog` 写在 `Scaffold` 之外 | 弹不出来（可能无报错） | 放进 `Scaffold` 的内容 lambda |
| `OverlayDialog` 写在 `LazyColumn` 的 `item {}` 里 | item 回收时弹窗消失 | 作为 `LazyColumn` 的兄弟节点 |
| `items(list)` 不给 `key` | 增删时状态错位、动画异常 | `items(list, key = { it.id })` |
| 用 `mutableStateOf` 存需要跨配置变更的表单值 | 旋转/进程恢复后丢失 | `rememberSaveable` / ViewModel |
| 在 `Composable` 里直接做网络/IO | 每次重组都请求 | `LaunchedEffect` / ViewModel |

---

## 2. 版本迁移：旧 API → `0.9.x`

**旧项目（`< 0.9.0`）里继续沿用旧 API 是可以的，但不要在新代码里写旧 API。** 下面是精确映射（依据 StatusBarLyric 0.4.3、Payload-Dumper-Compose 0.3.4、OShin 0.5.2 的实际 import 整理）。

### 组件改名与模块搬移

| 旧（`≤ 0.8`） | 新（`0.9.x`） |
| :-- | :-- |
| `top.yukonga.miuix.kmp.extra.SuperSwitch` | `top.yukonga.miuix.kmp.preference.SwitchPreference` |
| `top.yukonga.miuix.kmp.extra.SuperArrow` | `top.yukonga.miuix.kmp.preference.ArrowPreference` |
| `top.yukonga.miuix.kmp.extra.SuperCheckbox` | `top.yukonga.miuix.kmp.preference.CheckboxPreference` |
| `top.yukonga.miuix.kmp.extra.SuperRadioButton` | `top.yukonga.miuix.kmp.preference.RadioButtonPreference` ⚠️ 8 个项目中 **0 次出现**，此行属对称性推测，未经验证 |
| `top.yukonga.miuix.kmp.extra.SuperSlider` | `top.yukonga.miuix.kmp.preference.SliderPreference` ⚠️ 同上，未经验证 |
| `top.yukonga.miuix.kmp.extra.SuperDropdown` | `top.yukonga.miuix.kmp.preference.OverlayDropdownPreference` |
| `top.yukonga.miuix.kmp.extra.SuperSpinner` | `top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference` ⚠️ 未经验证 |
| `top.yukonga.miuix.kmp.extra.SuperDialog` | `top.yukonga.miuix.kmp.overlay.OverlayDialog` |
| `top.yukonga.miuix.kmp.extra.SuperBottomSheet` | `top.yukonga.miuix.kmp.overlay.OverlayBottomSheet` ⚠️ 未经验证 |
| `top.yukonga.miuix.kmp.extra.DropdownImpl` | `top.yukonga.miuix.kmp.basic.DropdownImpl` |
| `top.yukonga.miuix.kmp.extra.CheckboxLocation` | `top.yukonga.miuix.kmp.preference.CheckboxLocation`（同名，但**枚举值也变了**：旧 `Left`/`Right` → 新 `Start`/`End`） |
| `top.yukonga.miuix.kmp.basic.ListPopup` | `top.yukonga.miuix.kmp.overlay.OverlayListPopup`（需 Scaffold 宿主）/ `top.yukonga.miuix.kmp.window.WindowListPopup`（不需要）。**注意：`ListPopupColumn` 在旧版就与 `ListPopup` 共存，它不是替代品** |
| `SpinnerEntry` | `DropdownItem`（库内保留了 `typealias SpinnerEntry = DropdownItem`） |
| `SpinnerColors` | `DropdownColors`（同上，有 typealias） |

### 图标包重组

| 旧 | 新 |
| :-- | :-- |
| `top.yukonga.miuix.kmp.icon.icons.useful.Back` | `top.yukonga.miuix.kmp.icon.extended.Back` |
| `top.yukonga.miuix.kmp.icon.icons.Search` | `top.yukonga.miuix.kmp.icon.**basic**.Search`（`Search` 是 7 个内建图标之一，不在 extended） |
| `top.yukonga.miuix.kmp.icon.icons.ImmersionMore` | `top.yukonga.miuix.kmp.icon.extended.More` |
| `top.yukonga.miuix.kmp.icon.icons.basic.Check` | `top.yukonga.miuix.kmp.icon.basic.Check` |
| `top.yukonga.miuix.kmp.icon.icons.basic.ArrowUpDownIntegrated` | `top.yukonga.miuix.kmp.icon.basic.ArrowUpDown` |

### 已被移除、需要换实现

| 旧 API | 替代方案 |
| :-- | :-- |
| `top.yukonga.miuix.kmp.utils.getWindowSize()` | `BoxWithConstraints`（拿 `maxWidth/maxHeight`）或 `LocalWindowInfo.current.containerDpSize` |
| `top.yukonga.miuix.kmp.utils.G2RoundedCornerShape(...)` | `Modifier.squircleClip(...)` / `squircleSurface(...)`，或 `RoundedCornerShape(...)` |
| `top.yukonga.miuix.kmp.theme.miuixShape()` / `miuixUnevenShape()` | `RoundedCornerShape(...)` / squircle 修饰符；需要设备圆角用 `getRoundedCorner()` / `rememberNavSystemCornerRadius()` |
| `MiuixPopupUtil.Companion.dismissDialog` / `dismissPopup` | 用 `Overlay*` 组件自带的 `show: Boolean` 状态控制；不再主动「dismiss」 |
| `top.yukonga.miuix.kmp.utils.BackHandler` | `androidx.navigationevent.compose.NavigationBackHandler`（官方 example 用法）或平台 `BackHandler` |

> ⚠️ **以下两个 API 曾被本 skill 错误列入「已移除」，实际仍在 0.9.4，不要迁移：**
> - `top.yukonga.miuix.kmp.interfaces.HoldDownInteraction` —— 存在（`interfaces/HoldDownInteraction.kt`，public interface，配 `Hold`/`Release`）。
> - `top.yukonga.miuix.kmp.basic.DropdownArrowEndAction` —— 存在（`basic/Dropdown.kt`，`RowScope.DropdownArrowEndAction`），InstallerX 0.9.4 正在使用。
>
> 犯错原因：用「旧项目 import 过、新项目没 import」反推「已移除」。**没有 import 只说明没人用，不等于 API 消失**；判断移除必须以库源码为准。 |

| `top.yukonga.miuix.kmp.basic.LazyColumn` | `androidx.compose.foundation.lazy.LazyColumn`（Miuix 不再导出） |


### `NavigationBar` 的 API 变更（重点）

```kotlin
// ❌ 旧（≤ 0.8）
val items = listOf(NavigationItem("首页", icon), NavigationItem("设置", icon))
NavigationBar(items = items, selected = selected, onClick = { selected = it })

// ✅ 新（0.9.x）
NavigationBar {
    items.forEachIndexed { index, item ->
        NavigationBarItem(
            selected = selected == index,
            onClick = { selected = index },
            icon = item.icon,
            label = item.label,
        )
    }
}
```

`NavigationItem(label, icon)` 数据类保留；`NavigationBarItem` 是新增的 `RowScope` 扩展。

### 主题接线方式的变更

旧项目普遍是：

```kotlin
MiuixTheme(colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) { content() }
```

这个写法在 `0.9.x` 仍然完全可用。**新项目建议用 `ThemeController`**，才能拿到 Monet 动态取色、明暗强制覆盖、调色板风格等能力（见 `theme.md` §1）。

---

## 3. 常见编译错误速查

| 报错 | 原因 | 修复 |
| :-- | :-- | :-- |
| `Unresolved reference: SwitchPreference` | 没加 `miuix-preference` 依赖 | 加 `top.yukonga.miuix.kmp:miuix-preference:<version>` |
| `Unresolved reference: Home`（`MiuixIcons.Home`） | 没逐个 import 图标 | `import top.yukonga.miuix.kmp.icon.extended.Home` |
| `Unresolved reference: Back` | 同上（`Back` 是**扩展属性**，不在 `MiuixIcons` 对象里） | `import top.yukonga.miuix.kmp.icon.extended.Back` |
| `Classifier 'Scaffold' does not have a companion object` | 同时 import 了 Material 与 Miuix 的 `Scaffold` | 加别名或删掉 Material import |
| `SerializationException`（首次组合） | 路由类型没标 `@Serializable` | 给 `sealed interface Route` 和每个子类型都加 `@Serializable` |
| 退后台/进程死亡时崩溃（导航） | `rememberNavBackStack(key)` 未显式传超类型 | `rememberNavBackStack<Route>(Route.Home)` |
| `IllegalArgumentException: Duplicate content key` | 同一路由值被推入两次 | `Navigator.push` 里做幂等检查；需要多实例时给路由带唯一值 |
| `Unresolved reference: getWindowSize` / `G2RoundedCornerShape` / `miuixShape` | 用了旧版本 API | 见 §2 |
| `Unresolved reference: MiuixPopupUtil` | 旧版弹层工具已移除 | 用 `Overlay*` 的 `show` 状态 |
| 弹窗/下拉完全没出现 | 调用点不在任何 `Scaffold` 内 | 移进 `Scaffold` 的内容 lambda |
| 模糊没效果 | 组件自身底色不透明盖住了模糊 | 模糊层 `color = Color.Transparent` |
| `NoSuchMethodError` / 元数据版本错误 | Kotlin/Compose 版本旧于库要求 | 升 Kotlin 编译器与 Compose Multiplatform 版本 |

---

## 4. 性能与稳定性检查

| 检查项 | 做法 |
| :-- | :-- |
| 列表 key | `items(list, key = { it.id })` |
| lambda 稳定性 | 传给子组件的 lambda 不要每次重组新建（必要时 `remember`） |
| 长列表里的弹层 | `Overlay*` 不要放进 `item {}` |
| `remember` key | `remember(query) { filter(query) }`，别用空 key 缓存依赖值 |
| 模糊层数量 | 只给顶栏/底栏/悬浮条加，**不要**给每个列表项加 |
| 集合类型 | 稳定集合用 `kotlinx.collections.immutable` 的 `ImmutableList`/`PersistentList`，它们对 Compose 是 stable 的 |
| `@Immutable` | 只标在真正不可变的数据类上；含 lambda 字段的用 `@Stable`（lambda 相等性是引用比较，会破坏 `@Immutable` 契约） |
| 派生状态 | 用 `remember { derivedStateOf { … } }` 而不是每次重组重算 |
| 副作用 | 一次性动作用 `LaunchedEffect(key)`，清理用 `DisposableEffect` |

---

## 5. 无障碍与国际化红线

| 项 | 要求 |
| :-- | :-- |
| 纯图标按钮 | `contentDescription` 必须是可读文案（走字符串资源） |
| 装饰性图标 | `contentDescription = null`（不要用 `""`） |
| 颜色对比 | 不要用 `onBackgroundVariant` 承载关键信息（它本身是弱化色） |
| 文案 | Android 项目一律 `stringResource(R.string.…)`，不要硬编码中文字面量 |
| 尺寸 | 触控目标 ≥ 40.dp（`IconButtonDefaults.MinWidth/MinHeight = 40.dp`，不要擅自调小） |
| 深色模式 | 每个新页面都要在深色下过一遍；用语义色就不需要额外分支 |
| 动态字号 | 不要写死 `fontSize` 覆盖 `MiuixTheme.textStyles.*`（会让系统字号缩放失效） |
