# 坑表 P1–P20

按「症状 → 根因 → 修法 → 怎么确认修好了」写。日志片段是实测原文，可以直接拿去 grep。

---

## P1 · 构建报找不到编译器 / vcvars 被拦

**症状**

```text
Error: No C compiler found. Please install a C compiler and try again.
```

或者你调 `vcvars64.bat` 时报：

```text
The system cannot find the path specified.
'vswhere' is not recognized as an internal or external command
```

**根因**：机器上没有 MSVC，或 `vcvars64.bat` 内部要通过 `vswhere.exe` / `reg.exe` 定位 VS 安装，受限账户下被拦。

**修法**

1. 装 Build Tools（只要 C++ 工作负载，3.4 GB）：

```bat
winget install Microsoft.VisualStudio.2022.BuildTools --override "--quiet --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended"
```

2. **不要用 vcvars**，直接设 `INCLUDE` / `LIB` / `PATH`，见 `toolchain.md` §2。

**确认**：`where link.exe` 指向 `...\VC\Tools\MSVC\<ver>\bin\Hostx64\x64\link.exe`，且能编出一个 return 0 的 C 程序。

---

## P2 · exe 只有 30 多 MB，跑起来没有 Compose

**症状**：构建"成功"，产物 34 MB（正常应 120 MB+），运行报

```text
java.lang.NoClassDefFoundError: androidx/compose/runtime/Composer
```

**根因**：classpath 超过 Windows `cmd.exe` 单条命令 **8191 字符**上限，被静默截断。Compose + miuix + skiko 的运行时 classpath 实测 **13.8k 字符**。

**修法**：用 @argfile。

```bat
native-image.cmd @build\ni-args-gui-app.txt
```

生成脚本在 `scripts/native-image/gen-args.py`，它会打印 classpath 字符数：

```text
argfile  -> D:/proj/build/ni-args-gui-app.txt
classpath -> 13854 字符（超过 cmd 8191 上限，必须走 argfile）
```

**确认**：产物 ≥ 120 MB；或用二进制扫一眼有没有 Compose 的类名：

```bash
python -c "d=open('app.exe','rb').read(); print(d.count(b'androidx/compose/runtime/Composer'))"
# 应 > 0
```

---

## P3 · classpath 变量是空的 / exe 编出来没有业务代码

**症状**：批处理里 `set /p CP=<runtime-cp.txt` 之后 `echo %CP%` 是空的。

**根因**：`runtime-cp.txt` 末尾带换行；或 `for /f` 读到末尾空行把变量覆盖成空。

**修法**

- 生成时**不写结尾换行**（`writeText` 不加 `\n`）。
- 用 `set /p` 只读第一行：

```bat
set "CP="
for /f "usebackq delims=" %%i in ("build\runtime-cp.txt") do if not defined CP set "CP=%%i"
```

**确认**：`echo %CP%` 长度 > 10000（`echo %CP% | find /c ";"` 不是办法，直接打印前 100 字符看是否以盘符开头）。

---

## P4 · `_nAfterLoad` 的 NullPointerException

**症状**

```text
java.lang.NullPointerException
    at org.jetbrains.skiko.Library._nAfterLoad(Native Method)
```

**根因**：skiko 库加载成功后回调 Java 做能力探测（Direct3D 适配器），而探测依赖的 JNI 类没注册上 → 探测返回 null → NPE。

**重要**：**这条 NPE 本身无害**。它是「有东西没注册」的烟雾，不是火。修它的正确方式是解决真正的注册缺失（P5/P6），或者直接切 `OPENGL` 后端让它不再探测 D3D。

**不要**：给 `Library` 加 try-catch 或全局异常处理器来"消除"它——那只会让真正的问题更难查。

**确认**：修好 P5/P6 后这条可能仍然打印一次，属正常。判据不是「日志干净」，是「窗口能开、能出帧」。

---

## P5 · 窗口不出，崩在文本排版（skiko 的 52 个类）

**症状**：离屏自检跑到排版阶段就挂；真开窗直接没有窗口。

```text
Exception in thread "main" java.lang.NullPointerException
    at org.jetbrains.skia.impl.Managed._nDispose(Native Method)
```

或者进程直接退出，无 Java 堆栈。

**根因**：`skiko-windows-x64.dll` 里用 `FindClass` 找约 **52** 个类（`java.io.OutputStream`、`java.awt.*`、`org.jetbrains.skia.*` 的一部分），未注册时 `Paragraph.layout()` 走不下去。

**修法**

```bash
python scripts/native-image/scan-class-literals.py skiko-windows-x64.dll -o native-image/jni-config-literals.json
```

加进 `-H:JNIConfigurationFiles`。

**确认**：自检里对一段中文跑 `Paragraph.layout()`，不抛异常且尺寸 > 0。**这是最灵敏的探针**，因为它一次性覆盖字体管理器、文本整形、skia 绑定三条链。

---

## P6 · 窗口一开就崩：`NoClassDefFoundError: java/awt/Canvas`

**症状**

```text
Exception in thread "AWT-EventQueue-0" java.lang.NoClassDefFoundError: java/awt/Canvas
    at sun.awt.windows.WToolkit.eventLoop(Native Method)
    at sun.awt.windows.WToolkit.run(WToolkit.java:347)
```

**根因**：`awt.dll` 的 native 代码里有 **146** 个 Java 类名的字面量，AWT 事件循环启动时逐个 `FindClass`。

**修法**

```bash
python scripts/native-image/scan-class-literals.py awt.dll -o native-image/jni-config-awt.json
```

**确认**：窗口能出现（哪怕内容空白）。注意这个错会**掩盖** P8（jawt 路径），修完这个才看得到下一个。

---

## P7 · 注册 AWT 后构建失败：`System.initSystemClassLoader` 不可达

**症状**

```text
Error: Detected an instance of the class java.lang.System being reached
       through an unsupported element: java.lang.System.initSystemClassLoader
```

**根因**：146 个 AWT 类进镜像后，构建期把 `java.lang.System` 的静态初始化链展开了。

**修法**

```text
--report-unsupported-elements-at-runtime
```

**确认**：构建通过；运行期窗口、字体、中文均正常（实测这两个元素不会被访问）。

**不要**用 `--initialize-at-build-time=java.lang.System`，会引发连锁失败。

---

## P8 · `Cannot open (null)\bin\jawt.dll`

**症状**

```text
Cannot open (null)\bin\jawt.dll
```

窗口可能仍能开（如果不需要 JAWT），但一旦走 JAWT 路径就崩。

**根因**：native-image 里 `System.getProperty("java.home")` 是 **null**。skiko 的 `jawt.cc` 用 `java.home + "\bin\jawt.dll"` 拼路径。

**修法（两件事一起做）**

1. `main()` 最开头设 `java.home`：

```kotlin
if (System.getProperty("java.home") == null) {
    val dir = runCatching {
        ProcessHandle.current().info().command().orElse(null)
            ?.let { File(it).absoluteFile.parentFile }
    }.getOrNull() ?: File(System.getProperty("user.dir") ?: ".")
    System.setProperty("java.home", dir.absolutePath)
}
```

2. 交付目录建 `bin\`，放 `jawt.dll`：

```text
dist/
├── app.exe
├── (12 个 JDK dll)
└── bin/jawt.dll
```

**确认**：日志里不再出现 `Cannot open`，窗口能开且能渲染。

**上游背景**：这是 skiko 的已知问题（skiko#580 / Liberica#94），官方解法是静态链接 jawt，需要改 skiko 构建。

---

## P9 · 改了 exe 名字后启动即崩

**症状**：`cdj.exe` 能跑，改名成 `刷题通.exe` 后双击闪退（GUI 子系统下无任何输出）。

**根因**：GraalVM 用**构建输出名**定位同目录的原生库（`loadLibraryRelative` 拼 `<imageName>.dll` 以及 JDK dll 的相对路径）。改名后找不到 `awt.dll` / `jvm.dll`。

**修法**

- **exe 文件名保持不变**。
- 要中文名：改快捷方式（`.lnk`）的显示名，或做一个 `启动.bat` 里 `start "" cdj.exe`。
- 确实要换名：重新构建并指定 `-o 新名字`。

**确认**：改名后仍能启动（如果还崩，说明还有别的相对路径假设）。

---

## P10 · 双击后黑框一闪 / 黑框一直开着

**症状**：GUI 程序启动时弹控制台窗口；关掉黑框等于杀进程。

**根因**：默认生成的是 Windows **控制台**子系统程序。

**修法**

```text
-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS
-H:NativeLinkerOption=/ENTRY:mainCRTStartup
```

**代价**：没有控制台就没有 stderr。所以**排查期先编控制台版**，稳定后再切。

**确认**：双击无黑框，进程正常起来且窗口出现。

**变体**：如果链接报 `LNK2019 unresolved external symbol main`，去掉 `/ENTRY:mainCRTStartup` 只留 `/SUBSYSTEM:WINDOWS`。

---

## P11 · `kotlin.DeprecationLevel was unintentionally initialized at build time`

**症状**

```text
Error: Classes that should be initialized at run time got initialized during image building:
 kotlin.DeprecationLevel was unintentionally initialized at build time.
```

**根因**：Kotlin 的注解枚举被构建期分析连带初始化。

**修法**

```text
--initialize-at-build-time=kotlin.DeprecationLevel
```

**确认**：构建通过。若还报别的类，同样逐个加（用逗号分隔可以一次加多个）。

---

## P12 · `expect` / `actual` 编译不过

**症状**：接入共享 KMP 源码后报

```text
error: expect declaration 'xxx' has no actual declaration
```

或插件报 `'expect' declarations are only allowed in multiplatform projects`。

**根因**：模块用的是 `kotlin("jvm")`，不认 `expect/actual`。

**修法**：换 `kotlin("multiplatform")`，只声明一个 `jvm()` 目标：

```kotlin
plugins { kotlin("multiplatform") }
kotlin {
    jvmToolchain(21)
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }
    sourceSets {
        commonMain {
            kotlin.srcDir("path/to/shared/src/commonMain/kotlin")
            dependencies { /* compose, miuix, coroutines... */ }
        }
        jvmMain { dependencies { implementation(compose.desktop.currentOs) } }
    }
}
```

源码目录相应变成 `src/commonMain/kotlin` + `src/jvmMain/kotlin`，`actual` 放 `jvmMain`。

**确认**：`compileKotlinJvm` 通过。

---

## P13 · 字体全豆腐块 / 界面空白

**症状**：窗口开了，但没有文字，或者全是「口口口」。

**根因**（三种，逐级排查）

1. compose 资源目录没进 classpath（`MissingResourceException: font xxx`）；
2. 资源进了 classpath 但没进镜像（`-H:IncludeResources` 缺 ttf）；
3. 字体文件在，但字体家族名对不上（fallback 到了没有中文字形的默认字体）。

**修法**

```kotlin
// 资源必须放在 src/commonMain/composeResources/font/
// Res.font.xxx 要求这个固定位置
```

```text
-H:IncludeResources=.*\.(ttf|otf)$
```

运行时 classpath 必须含资源根：

```bash
# 调试期（JVM）
CP="build/classes/kotlin/jvm/main;build/processedResources/jvm/main;$(cat build/runtime-cp.txt)"
```

**确认**：`Res.font.misans_regular` 能 `readBytes()` 出与源文件一致的字节数（自检里加这一条）。

---

## P14 · 高 DPI 下窗口跑到屏幕外 / 只显示一半

**症状**：`rememberWindowState(size = DpSize(420.dp, 820.dp))`，在 125%–250% 缩放的机器上窗口超出屏幕，标题栏看不见、拖不动。

**根因**：`dp` 是**逻辑像素**，实际像素 = dp × `uiScale`。250% 缩放下 420×820 dp = 1050×2050 px，而屏幕可能只有 1280×720。

**修法**

1. 尺寸按屏幕可用区域夹一下：

```kotlin
val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
    .defaultScreenDevice.defaultConfiguration.bounds
val maxW = (bounds.width  / 2.0).dp   // uiScale 已被 Compose 计入 dp 换算
val state = rememberWindowState(
    size = DpSize(minOf(420.dp, maxW), minOf(820.dp, maxH)),
    position = WindowPosition(Alignment.Center),
)
```

2. 或者让窗口可缩放且给个保守初值（如 420×680），并**不要** `resizable = false`。
3. 调试期想看到完整布局：`-Dsun.java2d.uiScale=1`（但 native-image 下 `-D` 不可靠，需在 `main()` 里 `System.setProperty` 且早于 AWT）。

**确认**：在 250% 缩放的机器上，窗口完整可见、能拖动、能调整大小。

---

## P15 · 「GPU 没生效」但程序能跑

**症状**：界面正常，但性能像软件渲染；日志里有 P4 那条 NPE。

**根因**：默认后端是 `DIRECT3D`，在 native-image 下探测失败后**静默降级**到软件渲染。

**修法**：显式设 `OPENGL`（见 `gpu-opengl.md` §1）。

**确认**：`--gpu` 自检打印 `GL_VERSION ≥ 2.0` 且 `GPU_RENDER_OK`；或在窗口路径下用自建探针确认原生 GL 上下文可建。

---

## P16 · `OpenGLOffscreenContext` 的 `UnsatisfiedLinkError`

**症状**

```text
java.lang.UnsatisfiedLinkError: 'long org.jetbrains.skiko.OpenGLOffscreenContext...'
```

**根因**：skiko 0.156.1 的 Windows dll **没有导出**这个原生函数（上游只在部分平台编译）。JVM 上同样报错，所以不是 native-image 的问题。

**修法**：自建 WGL 上下文（`glctx.c`，见 `gpu-opengl.md` §3）。

**确认**：`dumpbin /exports skiko-windows-x64.dll | findstr /i opengl` 输出为空 → 证实上游没有；然后你的桥能拿到 `GL_VERSION`。

---

## P17 · 远程桌面里窗口空白 / 起不来

**症状**：本机跑得好，通过 RDP 连上去启动，窗口白屏或崩。

**根因**：RDP 会话的 OpenGL 通常只有 **1.1**（`GDI Generic`），Skia 的 GL 后端要求 ≥ 2.0。

**修法**：`--soft`（`SOFTWARE_FAST`）。交付两个启动器。

**确认**：RDP 下用 `启动-软件渲染.bat` 能正常出画面。

---

## P18 · GUI 版双击闪退，没有任何信息

**症状**：`/SUBSYSTEM:WINDOWS` 的 exe 双击后进程消失，看不到原因。

**根因**：没有控制台 = 没有 stderr。

**修法（按成本排序）**

1. 程序自己写日志文件（**最可靠**）：

```kotlin
val log = File(appDir(), "crash.log")
Thread.setDefaultUncaughtExceptionHandler { t, e ->
    log.appendText("${System.currentTimeMillis()} [$t]\n${e.stackTraceToString()}\n")
}
```

2. 命令行重定向 stderr 跑一次（GUI 子系统的 exe 在 cmd 里跑时 stderr 仍可重定向到文件）：

```bat
cdj.exe 2> run-err.log
```

3. 临时编一个控制台版（去掉 `/SUBSYSTEM:WINDOWS`）复现。

**确认**：`run-err.log` 或 `crash.log` 里能看到真实堆栈。

---

## P19 · Rust/C++ 的 JNI dll 加载不到

**症状**

```text
java.lang.UnsatisfiedLinkError: no quiz_desktop in java.library.path
```

**根因**：`System.loadLibrary` 只在 `java.library.path` 里找，而 native-image 下这个属性基本为空（也没有 `java.home`）。打包后当前目录也不一定是 exe 目录。

**修法**：用绝对路径 + 基于 exe 目录定位：

```kotlin
internal fun appDir(): File {
    val cmd = runCatching { ProcessHandle.current().info().command().orElse(null) }.getOrNull()
    return cmd?.let { File(it).absoluteFile.parentFile }
        ?: File(System.getProperty("user.dir") ?: ".").absoluteFile
}

System.load(File(appDir(), "quiz_desktop.dll").absolutePath)
```

调试期（JVM）再补一个回退：先试 `appDir()`，再试 `System.getProperty("user.dir")`。

**确认**：把 dll 放 exe 同级，双击启动可用；从任意工作目录启动也可用。

---

## P20 · 产物拷到别的机器就崩

**症状**：开发机上好好的，拷到教室机器上闪退。

**根因**（四个常见）

1. 只拷了 exe，没拷同目录的 12 个 JDK dll；
2. 漏了 `bin\jawt.dll`；
3. 自己的 JNI dll（`quiz_desktop.dll`）没带；
4. **用了 OpenGL**，而目标机器是 RDP / 无独显 / 显卡驱动太老。

**修法**

- 交付整个目录，README 写明「整个目录一起拷，别单独拷 exe」。
- 给两个启动器（GPU / 软件渲染）。
- 发布前在一台**没装 JDK** 的干净机器上跑一次——这是 native-image 唯一的意义所在。

**确认**：干净机器 + 任意工作目录 + 双击启动 → 窗口出现、数据写入 `data\`。

---

## 附：这些坑的排查顺序

遇到「native 版起不来」时，按这个顺序查，能少走 90% 弯路：

```text
① 产物体积正常吗（≥120 MB）？            否 → P2/P3
② 是控制台版还是 GUI 版？有 stderr 吗？   无 → P18，先换控制台版
③ 挂在哪一步？启动即退 / 无窗口 / 窗口空白 / 窗口正常
   - 启动即退、无堆栈           → P9（改名）、P19（dll 缺失）
   - 有 NoClassDefFoundError    → P6（awt）、P5（skiko）
   - 窗口不出、崩在排版         → P5
   - 窗口出现但空白             → P8（jawt）、P14（窗口在屏幕外）、P17（RDP）
   - 窗口正常但卡               → P15（降级到软件渲染）
④ 全部修好后，跑离屏自检 + 真开窗看一眼
```
