# 场景 · 状态页（空 / 加载 / 错误 / 权限 / 引导）

任何「有内容的页面」都必须能表达这 5 种非正常状态。它们**不是额外页面**，而是同一个页面的分支。

| 状态 | 什么时候出现 | 表现 |
| :-- | :-- | :-- |
| **环境不满足** | 模块未激活 / 无权限 / 版本不支持 | 进入即弹 `OverlayDialog`（或整页引导） |
| **加载中** | 首次取数 | 居中转圈 |
| **错误** | 取数失败 | 居中错误图标 + 原因 + 「重试」 |
| **空** | 取数成功但没数据 | 居中插画 + 一句说明（+ 主操作） |
| **正常** | — | 渲染内容 |

---

## 1. 两个层次

### 页面级（整页替换）

用 `LazyColumn` + `item { Box(Modifier.fillParentMaxSize()) }`，让状态占满视口并居中：

```kotlin
LazyColumn(
    modifier = Modifier
        .fillMaxSize()
        .overScrollVertical()
        .scrollEndHaptic()
        .nestedScroll(scrollBehavior.nestedScrollConnection),
    contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
    overscrollEffect = null,
) {
    when (state) {
        Loading -> item { StateBox { InfiniteProgressIndicator() } }
        is Error -> item { StateBox { ErrorState(state.message, onRetry) } }
        Empty -> item { StateBox { EmptyState(kind = EmptyKind.NoData, onAction = onAction) } }
        is Ready -> items(state.items, key = { it.id }) { ListItemCard(it) }
    }
}

/**
 * 注意：接收者是 LazyItemScope —— 因为 fillParentMaxSize() 是 LazyItemScope 的扩展，
 * 写成顶层 fun 会 unresolved。只能这样声明，或在 item { } 里直接内联。
 */
@Composable
private fun LazyItemScope.StateBox(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}
```

`fillParentMaxSize()` 是 `LazyItemScope` 的扩展：让这一项占满**视口**（而不是剩余空间），所以能真正居中。
没有它就只能靠 `fillMaxWidth()` + 固定高度，效果差很多。**也正因为它是作用域扩展，接收者必须写成
`LazyItemScope`，不能写成顶层 `fun`。**

### 区块级（列表中的某一段）

只需要替换某一段时，在 `LazyColumn` 里放一个 `item { Card { ... } }`，
用「提示卡」而不是整页替换（`scenario-home.md` 的 `TipCard` / `scenario-info.md` 的 `WarningCard`）。

---

## 2. 加载中

```kotlin
@Composable
fun LoadingState() {
    InfiniteProgressIndicator(color = MiuixTheme.colorScheme.primary)
}
```

- **就用转圈**。8 个项目没有一个做骨架屏（shimmer）——Miuix 的观感里转圈更原生。
- 首次加载用整页居中转圈；**已经有内容**的刷新（下拉刷新/加载更多）用局部小转圈。
- 不要用 `LinearProgressIndicator` 表示「不确定进度」（它是给确定性进度用的）。
- 加载超过 ~300ms 再显示，避免闪烁；数据秒回时直接渲染内容（可选优化）。

---

## 3. 空状态

**三种空要区分文案**，这是最容易被忽略、也最影响体验的一点：

| 类型 | 文案示例 | 主操作 |
| :-- | :-- | :-- |
| 本来就没数据 | 「还没有任何记录」 | 「去创建」 |
| 搜索/筛选无结果 | 「没有匹配「$query」的结果」 | 「清除筛选」 |
| 缺少权限导致查不到 | 「未授予存储权限，无法列出文件」 | 「授予权限」 |

```kotlin
@Composable
fun ColumnScope.EmptyState(
    kind: EmptyKind,
    query: String = "",
    onAction: () -> Unit,
) {
    val (text, actionText) = when (kind) {
        EmptyKind.NoData -> "还没有任何记录" to "去创建"
        EmptyKind.NoResult -> "没有匹配「$query」的结果" to "清除筛选"
        EmptyKind.NoPermission -> "未授予存储权限，无法列出文件" to "授予权限"
    }

    Icon(
        imageVector = when (kind) {
            EmptyKind.NoResult -> MiuixIcons.Search
            EmptyKind.NoPermission -> MiuixIcons.Lock
            EmptyKind.NoData -> MiuixIcons.All
        },
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MiuixTheme.colorScheme.onBackgroundVariant,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = text,
        style = MiuixTheme.textStyles.body1,
        color = MiuixTheme.colorScheme.onBackgroundVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = onAction, colors = ButtonDefaults.buttonColorsPrimary()) {
        Text(actionText)
    }
}
```

| 项 | 值 |
| :-- | :-- |
| 图标 | `48.dp`，`onBackgroundVariant`；有品牌插画/Lottie 时用 100.dp |
| 图标与文案间距 | `12.dp` |
| 文案 | `textStyles.body1`，`onBackgroundVariant`，**居中** |
| 文案与按钮间距 | `16.dp` |
| 按钮 | `Button` + `buttonColorsPrimary()`，一句话动词短语 |
| 左右内边距 | `32.dp`（防止长文案贴边） |
| 不做的事 | 不要给空状态做下拉刷新（没有内容可刷）、不要显示「没有更多了」 |

---

## 4. 错误状态

```kotlin
@Composable
fun ColumnScope.ErrorState(message: String, onRetry: () -> Unit) {
    Icon(
        imageVector = MiuixIcons.Report,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MiuixTheme.colorScheme.error,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "出错了",
        style = MiuixTheme.textStyles.headline1,
        color = MiuixTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = message,
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onBackgroundVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = onRetry, colors = ButtonDefaults.buttonColorsPrimary()) {
        Text("重试")
    }
    Spacer(Modifier.height(8.dp))
    TextButton(
        text = "复制错误详情",
        onClick = { copyToClipboard(message) },
    )
}
```

规则：

- 错误图标用 `colorScheme.error`，标题用 `onBackground`，**技术细节（异常信息）用弱化色**。
- 一定给「重试」；长按/按钮提供「复制错误详情」，方便用户报 issue。
- 网络类错误文案要具体（"无法连接到服务器" 而不是 "操作失败"）。
- 不要弹 `OverlayDialog` 报错——列表页的失败是常态，弹窗会打断用户。

---

## 5. 环境不满足 / 权限（进入即引导）

这是工具类/Xposed 类应用的第一屏体验，XiaomiHelper 用了 4 个独立 `OverlayDialog`：

```kotlin
LaunchedEffect(envState) {
    when {
        envState.canWork -> hideAll()
        !envState.isModuleActivated -> showInactive.value = true
        !envState.isModuleEnabled -> showDisabled.value = true
        !envState.isRootRequirementSatisfied -> showRoot.value = true
        !envState.isSystemVersionRequirementSatisfied -> showSystemVersion.value = true
    }
}

OverlayDialog(
    show = showInactive.value,
    title = "模块未激活",
    summary = "请在 LSPosed 中启用本模块，并勾选需要生效的应用，然后重启这些应用。",
    onDismissRequest = { showInactive.value = false },
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton("忽略", onClick = { showInactive.value = false }, modifier = Modifier.weight(1f))
        TextButton(
            text = "去激活",
            onClick = { openLSPosed(); showInactive.value = false },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
}
```

| 规则 | 说明 |
| :-- | :-- |
| **优先级判定** | 用 `when` 从上到下判定，一次只显示最高优先级那个（不要叠 4 个弹窗） |
| **必须有出口** | 至少一个「忽略/稍后」，不能把用户锁死 |
| **动词明确** | 「去激活」「去授权」「去设置」，不要写「确定」 |
| **启动防闪** | `LaunchedEffect` 里可加 `delay(500)`，避免启动瞬间弹窗 |
| **跳转兜底** | `startActivity` 要 try/catch，目标不存在时改成复制说明文字 |
| **权限请求** | 用平台 API 申请（`rememberLauncherForActivityResult`），成功后**重新取数**，不要只改本地标记 |

权限状态用一个显式的枚举，不要散在多个 `Boolean` 里：

```kotlin
enum class PermissionState { Granted, Denied, PermanentlyDenied, NotApplicable }
```

---

## 6. 状态优先级（必须遵守）

```
环境不满足（弹窗/整页引导）
    ↓ 满足后
加载中
    ↓ 完成
错误（可重试）
    ↓ 成功
空（区分三种空）
    ↓ 有数据
内容
```

实现上就是**一个 `when` 分支到底**，不要用一层层 `if` 叠加（会出现「加载中同时显示空状态」这类 bug）：

```kotlin
when {
    !envReady -> EnvGuide()
    isRefreshing && items.isEmpty() -> LoadingState()
    error != null && items.isEmpty() -> ErrorState(error, ::retry)
    items.isEmpty() -> EmptyState(kind, query, ::onAction)
    else -> Content(items)
}
```

注意 `&& items.isEmpty()`：**已有内容时的刷新失败不要清空列表**，改成 `Snackbar` 提示（否则用户已经看到的内容会突然消失）。

---

## 7. 引导（首次使用）

| 形式 | 用法 |
| :-- | :-- |
| 顶部提示卡 | 首页/列表顶部一张 `TipCard`，可关闭；关闭状态用 `rememberSaveable` 或偏好项持久化 |
| 进入即弹窗 | 只在**必须**先做某件事时用（如未激活模块），且必须可忽略 |
| 空状态里的引导 | 空数据 + 一句「怎么做」+ 主按钮，最自然 |
| 三步说明 | 用 `SmallTitle` + 三张卡（1/2/3），不要做轮播图 |

持久化「已看过引导」：

```kotlin
var seenGuide by rememberSaveable { mutableStateOf(false) }          // 单次会话
// 或写偏好项，跨启动记住
val seen by prefs.observeBoolean("seen_guide", false).collectAsState(initial = false)
```

---

## 8. 常见错误

| 错误 | 后果 | 正确 |
| :-- | :-- | :-- |
| 状态用 `Box(Modifier.fillMaxSize())` 放在 `LazyColumn` 外 | 与列表布局混用，滚动行为异常 | `item { Box(Modifier.fillParentMaxSize()) }` |
| 空/错误状态忘了居中 | 内容挤在顶部 | `contentAlignment = Alignment.Center` + `fillParentMaxSize()` |
| 三种空状态用同一文案 | 用户不知道是没数据还是没权限 | 按 `EmptyKind` 区分文案与操作 |
| 刷新失败清空已有列表 | 内容突然消失 | `error != null && items.isEmpty()` 才整页报错，否则 `Snackbar` |
| 用 `OverlayDialog` 报列表加载失败 | 打断用户、需手动关闭 | 整页错误态 + 重试按钮 |
| 4 个环境弹窗同时判定 | 叠窗/互相覆盖 | `when` 只取最高优先级 |
| 引导弹窗没有「忽略」 | 用户被锁死 | 提供出口 |
| 加载中用 `LinearProgressIndicator` | 语义错误（那是确定性进度） | `InfiniteProgressIndicator` |
| 给空状态加「下拉刷新」 | 无内容可刷，误导 | 空状态不给刷新，或刷新只在有内容时启用 |
| 硬编码 `Color.Gray` 做空态图标色 | 深色模式不可读 | `colorScheme.onBackgroundVariant` |

---

## 9. 真实参考

> 📍 **provenance 说明**：下表路径是产出本 skill 的机器上的克隆（`/root/miuix/repos/`），**仅用于追溯结论出处**。
> 换一台机器这些路径就不存在 —— **不要把「去读这些文件」当作任务步骤**，它们不在你的工作区。
> 需要抄实现时，以本 skill 的片段 + 你自己项目的既有代码为准。


| 内容 | 文件 |
| :-- | :-- |
| 加载 / 空状态（含 Lottie 插画）+ 状态机分支 | `/root/miuix/repos/lyricon/app/src/main/kotlin/io/github/proify/lyricon/app/ui/tabs/ProviderPage.kt` |
| 环境不满足的 4 个引导弹窗 + 优先级判定 | `/root/miuix/repos/XiaomiHelper/app/src/main/kotlin/dev/lackluster/mihelper/app/screen/MainPage.kt` |
| 提示卡 / 警告卡 / 错误文本块 | `/root/miuix/repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/miuix/widgets/MiuixCards.kt` |
| 权限申请对话框 | `/root/miuix/repos/Payload-Dumper-Compose/app/src/main/java/com/rcmiku/payload/dumper/compose/ui/component/RequestPermissionDialog.kt` |
| 空 / 错误 / 加载三态 + 下拉刷新的完整页面 | `/root/miuix/repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/miuix/settings/config/apply/MiuixApplyPage.kt` |
