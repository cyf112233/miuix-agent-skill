# 图标

## 1. 体系

| | 基础图标 `miuix-ui` | 扩展图标 `miuix-icons` |
| :-- | :-- | :-- |
| 包 | `top.yukonga.miuix.kmp.icon.basic` | `top.yukonga.miuix.kmp.icon.extended` |
| 数量 | 7 个 | **156** 个 |
| 粗细 | 只有 `MiuixIcons.Basic.X` | `Light` / `Normal` / `Regular` / `Medium` / `Demibold` |

`MiuixIcons` 只是**命名空间对象**，真正的图标是**扩展属性**：

```kotlin
// MiuixIcons.kt（库内部）
object MiuixIcons {
    object Basic
    object Light
    object Normal
    object Regular
    object Medium
    object Demibold
}

// 每个图标文件（库内部）
val MiuixIcons.Home: ImageVector get() = MiuixIcons.Regular.Home
val MiuixIcons.Light.Home: ImageVector get() = /* ... */
```

**结论：使用哪个图标就必须在文件里 import 哪个名字**（`MiuixIcons.Home` 是 `top.yukonga.miuix.kmp.icon.extended.Home` 这个扩展属性带来的，不 import 就解析不到）。逐个 import 是推荐做法；**星号导入技术上也能带进扩展属性**，但可读性差、也不利于静态检查，本 skill 不用。

```kotlin
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.basic.ArrowRight

Icon(MiuixIcons.Settings, contentDescription = "设置")               // Regular（默认）
Icon(MiuixIcons.Light.Settings, contentDescription = "设置")         // 细
Icon(MiuixIcons.Basic.ArrowRight, contentDescription = null)         // 基础集
```

## 2. 粗细语义

| 访问 | wght | 用途建议 |
| :-- | :-- | :-- |
| `MiuixIcons.Light.X` | 250 | 大尺寸装饰图标 |
| `MiuixIcons.Normal.X` | 305 | 次级信息 |
| `MiuixIcons.X` / `MiuixIcons.Regular.X` | 330 | **默认，绝大多数场景** |
| `MiuixIcons.Medium.X` | 430 | 强调 |
| `MiuixIcons.Demibold.X` | 500 | 小尺寸下需要更清晰时 |

## 3. `Icon` 组件

```kotlin
@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
)
// 另有 bitmap / painter / ColorProducer 重载
```

```kotlin
// 装饰性图标（相邻已有文字说明）→ contentDescription = null
Icon(MiuixIcons.Info, contentDescription = null, tint = MiuixTheme.colorScheme.onBackgroundVariant)

// 纯图标按钮 → 必须有 contentDescription（无障碍）
IconButton(onClick = onBack) {
    Icon(MiuixIcons.Back, contentDescription = "返回")
}

// 显式尺寸（列表/设置项左侧惯例 24.dp）
Icon(
    imageVector = MiuixIcons.Settings,
    contentDescription = null,
    modifier = Modifier.size(24.dp),
)
```

惯例：

- 设置项 `startAction` 里的图标：`contentDescription = null`（行文本已说明）。
- 顶栏 `actions` / `navigationIcon` 里的图标：写具体描述。
- 图标颜色：跟随 `LocalContentColor`（`IconButton`/`Preference` 会自动提供），需要强调时传 `tint = MiuixTheme.colorScheme.primary`。

## 4. 可用图标全表

### 基础集（`top.yukonga.miuix.kmp.icon.basic`）

`ArrowRight` `ArrowUpDown` `Check` `Close` `Search` `SearchCleanup` `Sidebar`

### 扩展集（`top.yukonga.miuix.kmp.icon.extended`）

```
Add           AddCircle     AddFolder     Alarm         Album         All
Answer        AppRecording  Back          Background    Backup        BankCards
Blocklist     CallRecording Carrier       ChevronBackward ChevronForward Clear
Close         Close2        CloudFill     Community     Contacts      ContactsBook
ContactsCircle ConvertFile  Copy          Create        Cut           Delete
Download      Edit          Email         ExpandLess    ExpandMore    Favorites
FavoritesFill File          FileDownloads Filter        Folder        FolderFill
Forward       GridView      Help          Hide          Home          HorizontalSplit
Image         Import        Info          Layers        Link          ListView
Location      Lock          MapAlbum      Merge         Messages      Mic
MicSlash      MindMap       Months        More          MoreCircle    MoveFile
Music         Notes         NotesFill     Ok            Paste         Pause
Phone         Photos        Pin           Play          Playlist      Promotions
Recent        Recording     RecordingTape Redo          Refresh       Remove
RemoveContact Rename        Replace       Reply         ReplyAll      Report
Reset         RotateLeft    Scan          ScreenCapture ScreenMirroring Search
SearchDevice  SelectAll     Send          Settings      Share         Show
Sidebar       Sort          Stopwatch     Store         Tasks         Th1 … Th31
Theme         Timer         TopDownloads  Translate     Trim          Tune
Undo          Unlock        Unpin         Update        UploadCloud   VerticalSplit
VolumeOff     VolumeUp      Weeks         WorldClock    Years         ZoomOut
```

常用对照：

| 语义 | 图标 |
| :-- | :-- |
| 返回 | `Back`（RTL 需 `graphicsLayer { scaleX = -1f }`） |
| 关闭 / 取消 | `Close`（顶栏左侧关闭用 `MiuixIcons.Regular.Close`） |
| 确认 / 成功 | `Ok` / `Check` |
| 更多 | `More` / `MoreCircle` |
| 设置 | `Settings` / `Tune` |
| 搜索 | `Search` / `SearchCleanup` |
| 删除 | `Delete` / `Remove` |
| 编辑 | `Edit` / `Create` |
| 添加 | `Add` / `AddCircle` |
| 信息 / 帮助 | `Info` / `Help` |
| 刷新 / 更新 | `Refresh` / `Update` |
| 下载 / 上传 | `Download` / `UploadCloud` |
| 文件夹 / 文件 | `Folder` / `File` |
| 图片 / 相册 | `Image` / `Photos` / `Album` |
| 音乐 / 播放 | `Music` / `Play` / `Pause` |
| 锁 / 解锁 | `Lock` / `Unlock` |
| 显示 / 隐藏 | `Show` / `Hide` |
| 主题 / 外观 | `Theme` / `Background` |
| 收藏 | `Favorites` / `FavoritesFill` |
| 时间 | `Timer` / `Stopwatch` / `Recent` |
| 排序 / 筛选 | `Sort` / `Filter` |

## 5. 自定义图标

Miuix 的 `Icon` 接受任意 `ImageVector` / `ImageBitmap` / `Painter`，所以可以直接用 `androidx.compose.ui.graphics.vector.ImageVector.Builder` 或 `material-icons-extended` 的 `ImageVector`（**只取图形，不要引入 Material 组件**）：

```kotlin
// 推荐：用 Compose 资源 + painterResource
Icon(
    painter = painterResource(Res.drawable.ic_custom),
    contentDescription = null,
    modifier = Modifier.size(24.dp),
)
```

需要「跟随 Miuix 粗细体系」时，最省事的办法是优先从内置扩展集里找语义接近的图标；只有确实必要时才自建 `ImageVector`，并把 `defaultWidth/Height = 24.dp`、`viewport = 24f` 对齐，保证与内置图标视觉尺寸一致。

## 6. 常见错误

| 错误 | 现象 | 修复 |
| :-- | :-- | :-- |
| `import top.yukonga.miuix.kmp.icon.extended.*`（星号） | 能编译，但可读性差、静态检查困难 | 逐个 import 用到的图标（推荐） |
| 忘了 `implementation("…:miuix-icons:…")` | `Unresolved reference: Home` 等 | 加依赖 |
| 用 `androidx.compose.material.icons.Icons.Default.X` | 视觉与 Miuix 不一致，且引入 Material | 换 `MiuixIcons.*` |
| `contentDescription = ""` | 无障碍报错（lint）/ 读屏异常 | 装饰性用 `null`，功能性写文案 |
| 手写 `Modifier.size(16.dp)` 给顶栏图标 | 与设计尺寸不符 | 用默认尺寸（`IconButton` 内不传 size），或 `24.dp` |
