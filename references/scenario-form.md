# 场景 · 表单 / 编辑

用户输入并提交的页面。结构固定：

```
顶栏：左「取消/关闭」  ← 右「保存/完成」
内容：LazyColumn
  ├─ 字段 1  Card { TextField }
  ├─ 字段 2  Card { TextField }
  ├─ 分组标题 SmallTitle + Card { 选择类设置项 }
  └─ 校验错误提示（紧跟在出错字段所在分组之后）
```

---

## 1. 完整骨架

```kotlin
@Composable
fun EditScreen(
    initialName: String,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var remark by rememberSaveable { mutableStateOf("") }
    var sync by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val nameError = when {
        name.isBlank() -> "名称不能为空"
        name.length > 20 -> "名称最多 20 个字符"
        else -> null
    }
    val canSave = nameError == null && !saving

    Scaffold(
        // 键盘弹出时整体上移：加在 Scaffold 上，而不是 LazyColumn 上
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = "编辑",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Close, contentDescription = "取消")
                    }
                },
                actions = {
                    if (saving) {
                        // 保存中：用图标按钮位显示进度，避免按钮宽度跳变
                        IconButton(onClick = {}, enabled = false) {
                            InfiniteProgressIndicator(size = 20.dp)
                        }
                    } else {
                        // 注意：这里不用 enabled = canSave 静默禁用；
                        // 让按钮可点，不满足时把错误显示出来（见 §3「校验」）
                        IconButton(onClick = {
                            if (canSave) onSave(name) else showErrors = true
                        }) {
                            Icon(MiuixIcons.Ok, contentDescription = "保存")
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

            item { SmallTitle("基本信息") }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "名称",
                        singleLine = true,
                        // Card 的 insideMargin 默认是 0，必须自己加内边距
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    )
                    HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    TextField(
                        value = remark,
                        onValueChange = { remark = it },
                        label = "备注",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    )
                }
            }

            // 校验错误：紧跟在字段分组之后，与 SmallTitle 左对齐（28.dp）
            nameError?.let { error ->
                item {
                    Text(
                        text = error,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 28.dp, end = 28.dp, bottom = 8.dp),
                    )
                }
            }

            item { SmallTitle("高级") }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    SwitchPreference(
                        title = "启用同步",
                        summary = "保存后立即上传",
                        checked = sync,
                        onCheckedChange = { sync = it },
                    )
                }
            }

            item { Spacer(Modifier.size(12.dp)) }
        }
    }
}
```

### 顶栏按钮的语义

| 位置 | 图标 | 含义 | 行为 |
| :-- | :-- | :-- | :-- |
| `navigationIcon` | `MiuixIcons.Close` | 取消 | `navigator.pop()`；有改动时可加二次确认 |
| `navigationIcon` | `MiuixIcons.Back` | 返回（等同取消） | 用 `Close` 更明确 |
| `actions` | `MiuixIcons.Ok` | 保存 | 提交；`enabled = canSave` |
| `actions` | `TextButton(text = "保存")` | 保存（文字更清楚） | 有足够空间时优先文字 |

两个真实项目（InstallerX `MiuixEditPage`、Updater-KMP `LoginDialog`）都用「`Close` 取消 + `Ok`/文字保存」。

---

## 2. 字段规范

| 项 | 约定 |
| :-- | :-- |
| 容器 | 每个（或每组）字段包在 `Card(12.dp + bottom 12.dp)` 里 |
| 字段内边距 | `Modifier.padding(16.dp)` —— **`Card` 的 `insideMargin` 默认是 0，必须自己加** |
| 分组标题 | `SmallTitle`，与字段卡对齐（默认 28.dp 缩进） |
| 标签 | `label` 必填（"名称"、"备注"），不要只靠 placeholder |
| 单行/多行 | 短文本 `singleLine = true`；长文本不设 `singleLine`，配 `minLines = 3` |
| `TextField` 圆角 | 默认 `TextFieldDefaults.CornerRadius` = 16.dp，不要改 |
| `TextField` 内边距 | 默认 `DpSize(16.dp, 16.dp)`，不要改 |
| 字段间分隔线 | 同卡片内多个 `TextField` 之间用 `HorizontalDivider(padding(horizontal = 16.dp))` |
| 必填标记 | 写在 `label` 里（"名称 *"），不要另加星号图标 |

### 输入类型的正确写法

```kotlin
// 单行文本 + 下一步
TextField(value, onChange, label = "账号", singleLine = true,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))

// 多行
TextField(value, onChange, label = "备注", minLines = 3,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default))

// 数字
TextField(value, onChange, label = "端口", singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

// 密码（含显隐）
var visible by rememberSaveable { mutableStateOf(false) }
TextField(
    value = pwd,
    onValueChange = { pwd = it },
    label = "密码",
    singleLine = true,
    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
    trailingIcon = {
        IconButton(onClick = { visible = !visible }) {
            Icon(if (visible) MiuixIcons.Hide else MiuixIcons.Show, contentDescription = if (visible) "隐藏" else "显示")
        }
    },
)
```

`KeyboardOptions` / `KeyboardActions` / `ImeAction` / `KeyboardType` 来自
`androidx.compose.foundation.text.*` 与 `androidx.compose.ui.text.input.*`（这些**不是** Material，可以用）。

---

## 3. 校验

三条规则：

1. **校验逻辑与状态放在状态层或 `remember` 派生**，不要在 `onClick` 里临时判断。
2. **错误文案紧跟在出错的分组之后**，用 `colorScheme.error` + `textStyles.footnote1`，左对齐到 `28.dp`。
3. **同一时刻只显示当前第一个错误**，不要一次列一堆。

```kotlin
val errors = remember(name, port) {
    buildList {
        if (name.isBlank()) add("名称不能为空")
        if (port.toIntOrNull() !in 1..65535) add("端口需在 1–65535 之间")
    }
}
val canSave = errors.isEmpty()
```

需要「点击保存才暴露错误」时：

```kotlin
var showErrors by remember { mutableStateOf(false) }
val canSave = errors.isEmpty()
IconButton(
    onClick = {
        // 注意 onSave 的签名是 (String) -> Unit，必须带参数
        if (canSave) onSave(name) else showErrors = true
    },
) {
    Icon(MiuixIcons.Ok, contentDescription = "保存")   // IconButton 的 content 是必填
}
// 渲染时：if (showErrors) errors.firstOrNull()?.let { ErrorText(it) }
```

> 「静默禁用保存按钮」对用户不友好，但没有错误提示时又是唯一选择。
> 推荐：**按钮始终可点**，点击时不满足则把错误显示出来并聚焦第一个出错字段。

---

## 4. 提交与结果反馈

```kotlin
// 状态层暴露 saving / error / success
LaunchedEffect(state.saveResult) {
    when (val r = state.saveResult) {
        is SaveResult.Success -> { snackbarHostState.showSnackbar("已保存"); onBack() }
        is SaveResult.Error -> snackbarHostState.showSnackbar(r.message)
        else -> Unit
    }
}
```

| 阶段 | UI |
| :-- | :-- |
| 提交中 | 保存按钮位置换成 `InfiniteProgressIndicator(size = 20.dp)`（保持宽度不跳）；输入框保持可见 |
| 成功 | `SnackbarHostState.showSnackbar("已保存")` + `navigator.pop()` |
| 失败 | `Snackbar` 显示原因；**不要**关页面，让用户能重试 |
| 需要确认 | `OverlayDialog` 二次确认（破坏性/不可逆操作） |

不要用 Toast（不可跨平台，且无 Snackbar 的操作能力）。

---

## 5. 草稿与离开保护

| 场景 | 做法 |
| :-- | :-- |
| 配置变更（旋转） | `rememberSaveable` |
| 进程死亡恢复 | `SavedStateHandle`（ViewModel） |
| 长表单 | 每次改动写 ViewModel（`onValueChange` 直接 dispatch），不要只留在 Composable 局部 |
| 有未保存改动时返回 | `navigationIcon` 先校验 `isDirty`，是则弹 `OverlayDialog` 询问「放弃/继续编辑」 |

```kotlin
val isDirty = name != initialName || remark.isNotBlank()

navigationIcon = {
    IconButton(onClick = { if (isDirty) showDiscardDialog = true else onBack() }) {
        Icon(MiuixIcons.Close, contentDescription = "取消")
    }
}
```

---

## 6. 键盘处理

| 问题 | 解法 |
| :-- | :-- |
| 键盘盖住底部输入框 | `Scaffold(modifier = Modifier.imePadding())` —— **加在 Scaffold 上**（InstallerX 的做法） |
| `Next` 不能跳到下一个输入框 | `imeAction = ImeAction.Next` + `KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })` |
| 最后一个输入框无法收起键盘 | `imeAction = ImeAction.Done` + `onDone = { focusManager.clearFocus() }` |
| 点空白处收键盘 | 需要时给容器加 `pointerInput { detectTapGestures { focusManager.clearFocus() } }` |
| 底部弹层被键盘遮挡 | `OverlayBottomSheet` + `Modifier.imePadding()` |

⚠️ **不要**同时给 `Scaffold` 和 `LazyColumn` 加 `imePadding()`，会双倍上移。

---

## 7. 表单里的设置项

表单中经常混入开关/选择项，**直接用 Preference 组件**，不要手搓：

```kotlin
Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
    SwitchPreference(title = "启用高级模式", checked = advanced, onCheckedChange = { advanced = it })
    HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
    OverlayDropdownPreference(
        title = "优先级",
        items = listOf("低", "中", "高"),
        selectedIndex = priority,
        onSelectedIndexChange = { priority = it },
    )
    HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
    SliderPreference(
        title = "超时",
        value = timeout,
        valueText = "${timeout.toInt()} s",
        valueRange = 1f..60f,
        onValueChange = { timeout = it },
    )
}
```

---

## 8. 常见错误

| 错误 | 后果 | 正确 |
| :-- | :-- | :-- |
| `TextField` 直接放 `Card` 里不加 `padding` | 文字贴边（`Card` 的 `insideMargin = 0`） | `Modifier.padding(16.dp)` |
| `imePadding()` 同时加在 `Scaffold` 和 `LazyColumn` | 键盘弹出时内容双倍上移 | 只加在 `Scaffold` |
| 顶栏用 `Back` 作为取消 | 语义模糊（用户以为会保存） | 用 `Close` |
| 保存按钮静默禁用且无错误提示 | 用户不知道哪里填错了 | 可点 + 点击后显示错误 |
| 校验错误用 `Color.Red` | 深色模式不可读 | `MiuixTheme.colorScheme.error` |
| 错误文案贴在字段旁边（`Row` 内） | 布局拥挤、换行错乱 | 放在字段分组之后，左对齐 28.dp |
| 表单状态只放 Composable 局部 | 旋转/退后台丢失 | `rememberSaveable` / ViewModel |
| 失败也 `navigator.pop()` | 用户输入丢失且不知道失败 | 留在页面 + `Snackbar` |
| 用 Toast 反馈 | 跨平台缺失、不可操作 | `SnackbarHost` |

---

## 9. 真实参考

> 📍 **provenance 说明**：下表路径是产出本 skill 的机器上的克隆（`/root/miuix/repos/`），**仅用于追溯结论出处**。
> 换一台机器这些路径就不存在 —— **不要把「去读这些文件」当作任务步骤**，它们不在你的工作区。
> 需要抄实现时，以本 skill 的片段 + 你自己项目的既有代码为准。


| 内容 | 文件 |
| :-- | :-- |
| 编辑页（`Close` 取消 + `Ok` 保存 + `imePadding` 在 Scaffold + 分组字段） | `/root/miuix/repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/miuix/settings/config/edit/MiuixEditPage.kt` |
| 登录表单（密码显隐 + `ImeAction.Next` 焦点链 + 双因素 + 记住密码） | `/root/miuix/repos/Updater-KMP/app/shared/src/commonMain/kotlin/ui/LoginDialog.kt` |
| 输入对话框（`OverlayDialog` + `TextField`） | `/root/miuix/repos/Payload-Dumper-Compose/app/src/main/java/com/rcmiku/payload/dumper/compose/ui/component/InputDialog.kt` |
| 应用页（表单 + 校验 + 提交的另一种组织方式） | `/root/miuix/repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/miuix/settings/config/apply/MiuixApplyPage.kt` |
