# 场景 · 引导与权限（多步向导 / 权限申请 / 首次使用）

**与 `scenario-state.md` 的分工**：

| | `scenario-state.md` | 本文件 |
| :-- | :-- | :-- |
| 性质 | **被动**：环境不满足，告诉用户为什么 | **主动**：带用户一步步完成设置 |
| 形式 | 整页状态 / 进入即弹窗 | 多步向导、权限申请、功能开关引导 |
| 结束条件 | 用户忽略或去修复 | 用户走完全部步骤，写一个「已完成」标记 |

---

## 1. 多步向导（Wizard）

结构：`HorizontalPager` + 顶部进度 + 底部按钮区。**按钮区固定在 `Scaffold` 的 `bottomBar`**，
否则最后一步的按钮会被内容顶出屏幕。

```kotlin
@Composable
fun SetupWizard(
    onFinish: () -> Unit,
    onSkip: () -> Unit,
) {
    val steps = remember {
        listOf(
            WizardStep("欢迎", "本向导会帮你完成三步基础设置。"),
            WizardStep("权限", "需要通知权限才能接收提醒。"),
            WizardStep("完成", "一切就绪，可以开始使用了。"),
        )
    }
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "初始设置",
                actions = {
                    TextButton(text = "跳过", onClick = onSkip)
                },
            )
        },
        bottomBar = {
            // 按钮区固定在底部，不随 pager 滑动
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (pagerState.currentPage > 0) {
                        TextButton(
                            text = "上一步",
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    TextButton(
                        text = if (pagerState.currentPage == steps.lastIndex) "开始使用" else "下一步",
                        onClick = {
                            if (pagerState.currentPage == steps.lastIndex) onFinish()
                            else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(steps[page].title, style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = steps[page].body,
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // 步骤指示器：用点，不要用 TabRow（TabRow 是导航语义，会诱导点击跳步）
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(steps.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MiuixTheme.colorScheme.primary
                                else MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.4f),
                            ),
                    )
                }
            }
        }
    }
}
```

| 项 | 约定 |
| :-- | :-- |
| 步骤数 | **2–4 步**。超过 4 步说明你在做说明书，拆成后续的功能内引导 |
| 按钮区 | 放 `Scaffold` 的 `bottomBar`，固定不动 |
| 「上一步」 | 第一步不显示（不要显示成禁用，直接不出现） |
| 「跳过」 | 顶栏 `actions` 里必须有；用户有权拒绝向导 |
| 步骤指示器 | 圆点（自绘），**不要用 `TabRow`**——它是导航控件，会让用户以为可以跳步 |
| 每步内容 | 标题 `title3` + 说明 `body1`（`onBackgroundVariant`），居中；插图可选 |
| 完成标记 | `onFinish()` 里持久化，见 §4 |

---

## 2. 权限申请

权限流程是**三段式**：申请 → 处理结果 → 用新权限重新取数（**第三步最容易漏**）。

```kotlin
@Composable
fun NotificationPermissionGate(onGranted: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(PermissionState.NotApplicable) }
    var asked by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        state = if (granted) PermissionState.Granted else PermissionState.Denied
        if (granted) onGranted()          // ③ 拿到权限后刷新数据，不要只改本地标记
    }

    // 首次进入时同步已有授权状态
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        state = if (granted) PermissionState.Granted else PermissionState.Denied
    }

    // 用提示卡而不是弹窗（权限是可选能力，不该打断）
    if (state == PermissionState.Denied && !asked) {
        TipCard(
            text = "开启通知权限后，才能在有新内容时提醒你。",
            onDismiss = { asked = true },
            action = {
                TextButton(
                    text = "授予权限",
                    onClick = {
                        asked = true
                        // Android 13+ 需要 POST_NOTIFICATIONS；低版本直接算已授权
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onGranted()
                        }
                    },
                )
            },
        )
    }
}
```

### 权限状态要有四种，不要用 Boolean

```kotlin
enum class PermissionState { Granted, Denied, PermanentlyDenied, NotApplicable }
```

Android 无法直接查询 `PermanentlyDenied`；用 `shouldShowRequestPermissionRationale` 间接判断：

```kotlin
val permanentlyDenied = !granted &&
    !shouldShowRequestPermissionRationale(activity, permission) &&
    askedBefore
```

三种状态的 UI 完全不同：

| 状态 | UI |
| :-- | :-- |
| `Granted` | 什么都不显示，直接用能力 |
| `Denied`（还能再问） | 提示卡 / 空态里的「授予权限」按钮 → 再次 `launch()` |
| `PermanentlyDenied` | 文案改成「请到系统设置中开启」+ 按钮跳应用设置页（`ACTION_APPLICATION_DETAILS_SETTINGS`）——**不要再 `launch()`，系统会直接拒绝且不再弹窗** |
| `NotApplicable` | 该平台/版本不需要此权限，静默跳过 |

跳应用设置页：

```kotlin
val intent = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", context.packageName, null),
)
context.startActivity(intent)
```

---

## 3. 功能引导（可关闭的提示卡）

用于「功能存在但用户不知道」的场景。规范：

- 放在**使用该功能的位置**（首页对应入口上方、或该页面顶部），不要集中堆在一个「帮助」页。
- 必须可关闭，关闭状态**持久化**（否则每次启动都出现，用户会烦）。
- 文案结构：**能做什么 + 怎么触发**，一到两句。

```kotlin
@Composable
fun FeatureTipCard(
    tipKey: String,
    text: String,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.tertiaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MiuixIcons.Info,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onTertiaryContainer,
            )
            IconButton(onClick = onDismiss) {
                Icon(MiuixIcons.Close, contentDescription = "不再提示")
            }
        }
    }
}
```

---

## 4. 引导完成状态的持久化

跨启动记住「已看过」，**不要只用 `rememberSaveable`**（那只跨配置变更）：

```kotlin
// 方式 A：共享偏好（Android）
val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
var wizardDone by remember { mutableStateOf(prefs.getBoolean("wizard_done", false)) }

// 完成时
prefs.edit().putBoolean("wizard_done", true).apply()
wizardDone = true

// 方式 B：跨平台用 DataStore / multiplatform-settings，接口同构
```

什么时候该显示向导：

```kotlin
when {
    !wizardDone -> SetupWizard(onFinish = { prefs.edit().putBoolean("wizard_done", true).apply() }, onSkip = { /* 同样写标记 */ })
    else -> MainShell()
}
```

**跳过也算完成**（也要写标记），否则每次启动都会弹出用户已经拒绝过的向导。

---

## 5. 与状态页的协作

进入应用时的判定顺序（`scenario-state.md` §6 的扩展）：

```
1. 首次使用向导未完成？
   ├─ 是 → 多步向导（可跳过）
   └─ 否 ↓
2. 关键环境不满足（模块未激活 / 无 root / 版本不支持）？
   ├─ 是 → 进入即弹 OverlayDialog，可忽略
   └─ 否 ↓
3. 可选权限未授予？
   ├─ 是 → 顶部提示卡（不阻断）
   └─ 否 ↓
4. 加载中 → 错误（可重试）→ 空 → 内容
```

区分「关键」与「可选」是重点：

| | 关键（阻断） | 可选（提示卡） |
| :-- | :-- | :-- |
| 例子 | 模块未激活、无必要权限导致功能完全不可用 | 通知权限、定位精度、后台刷新 |
| 形式 | `OverlayDialog`，必须给「忽略」出口 | 顶部 `TipCard`，可关闭 |
| 能否继续用 | 能（但功能受限） | 能，完全不影响 |

---

## 6. 常见错误

| 错误 | 后果 | 正确 |
| :-- | :-- | :-- |
| 向导按钮放在 `HorizontalPager` 每页内部 | 各步按钮位置不一致、最后一步被顶出屏幕 | 按钮区放 `Scaffold` 的 `bottomBar` |
| 用 `TabRow` 做步骤指示器 | 导航语义，诱导跳步，且 TabRow 宽度不足会滚动 | 自绘圆点 |
| 申请权限成功后只改本地标记 | 数据仍是旧的，用户看不到效果 | 回调里重新取数 |
| 用 `Boolean` 表示权限状态 | 分不清「可再问」和「永久拒绝」 | `PermissionState` 枚举 |
| `PermanentlyDenied` 还去 `launch()` | 系统静默拒绝，用户以为按钮坏了 | 跳应用设置页 |
| 「跳过」不写完成标记 | 每次启动重复弹向导 | 跳过也算完成 |
| 用 `rememberSaveable` 记向导完成 | 重装/清数据后重复出现（符合预期），但**杀进程重启也会重弹** | 持久化到偏好项 |
| 向导步骤超过 4 步 | 用户走不完 | 拆成功能内引导 |
| 提示卡不可关闭 / 关闭不持久化 | 用户被反复打扰 | 可关闭 + 持久化 |
| 把可选权限做成阻断弹窗 | 用户被卡住 | 提示卡 |

---

## 7. 真实参考

> 📍 **provenance 说明**：下表路径是产出本 skill 的机器上的克隆（`/root/miuix/repos/`），**仅用于追溯结论出处**。
> 换一台机器这些路径就不存在 —— **不要把「去读这些文件」当作任务步骤**，它们不在你的工作区。
> 需要抄实现时，以本 skill 的片段 + 你自己项目的既有代码为准。

| 内容 | 文件 |
| :-- | :-- |
| 进入即弹的环境引导弹窗（4 种状态 + 优先级） | `/root/miuix/repos/XiaomiHelper/app/src/main/kotlin/dev/lackluster/mihelper/app/screen/MainPage.kt` |
| 权限申请对话框 | `/root/miuix/repos/Payload-Dumper-Compose/app/src/main/java/com/rcmiku/payload/dumper/compose/ui/component/RequestPermissionDialog.kt` |
| 提示卡 / 警告卡封装 | `/root/miuix/repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/miuix/widgets/MiuixCards.kt` |
| 空态里的「去授权」引导 | `/root/miuix/repos/lyricon/app/src/main/kotlin/io/github/proify/lyricon/app/ui/tabs/ProviderPage.kt`（`EmptyStateView`） |
