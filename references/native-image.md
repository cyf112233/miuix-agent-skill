# 打包成 exe：GraalVM native-image（Compose Desktop）

> 本章是「界面写完之后」的那一段：把 Compose Multiplatform / Miuix 桌面应用编成单个 exe，GPU 渲染，冷启动 300 ms 级。
> 界面该怎么长（骨架、间距、颜色、组件）看 `../SKILL.md` 与同目录的其余文件，**两章互不依赖，按需跳读**。

**适用范围与边界，先说清楚，避免白干：**

| 能做 | 不能做 / 别承诺 |
| :-- | :-- |
| Compose Desktop + Miuix + skiko → 单个 `exe`（~120–150 MB） | 需要 `Class.forName` 动态加载任意类、反射扫包、Spring 式依赖注入的工程 |
| JNI 进 Rust / C++ 逻辑层（`System.loadLibrary` + 手写 JNI 桥） | 运行期 `-javaagent`、字节码增强、动态生成类 |
| Windows x64（Linux/macOS 同理，坑不同） | 32 位、ARM64（未验证，别照抄本文的参数） |
| 启动 300–450 ms、内存 ~200 MB、稳态 60 fps | 冷启动比 JVM 首启更慢的错觉——AOT 无 JIT 预热，**首帧就是满速**，这是它相对 wasm/JVM 的真实优势 |

**只对 Desktop target 有意义**：Android 走 R8/APK，Wasm/iOS 各有各的产物体，不适用本章。

> 本章以 **Windows x64** 为主线。**Linux 的流程与坑有实质差异**（JDK 选型、JNI 收集方式、原生库布局、渲染后端黑名单），已在 X11 + 无 GPU 环境完整跑通一遍，见 [`references/native-image/linux.md`](linux.md)——跨平台前先读它，别照抄本章的参数。

**一句话工作流**：`gradle 出 classpath` → `扫原生库的类字面量生成 jni-config` → `生成 @argfile` → `native-image` → **离屏自检** → 交付。

---

## 1. 前置：必须满足三个硬条件

1. **JDK 必须是 Liberica NIK「full」版**，不是 `standard`、不是 GraalVM CE 官方包。
   - `full` 才带完整 AWT 原生库：`bin\awt.dll`、`bin\jawt.dll`、`bin\fontmanager.dll`、`bin\freetype.dll`、`bin\lcms.dll`。
   - 验证：`ls <NIK_HOME>/bin | grep -E 'awt|jawt'` 必须全部存在。
   - 用 standard 版会在窗口阶段报 `NoClassDefFoundError: java/awt/Canvas`，且**没有** `jawt.dll` 可补。
2. **必须有 MSVC 链接器**（Build Tools 即可，不需要完整 VS）：`link.exe` + Windows SDK。
3. **`native-image.cmd` 所在的 `bin` 与 MSVC 的 `bin` 都要在 PATH**，且 `INCLUDE` / `LIB` 指到 MSVC + SDK。
   - 不要依赖 `vcvars64.bat`：它内部会调 `reg.exe`/`vswhere`，在某些受限账户或沙箱里会被拦截，报错信息还极具误导性。**显式设环境变量**，见 `references/native-image/toolchain.md`。

> 缺任何一个，后面所有 JNI 排查都是浪费时间——先跑 `references/native-image/toolchain.md` 的「5 行自检」。

---

## 2. 构建流水线

```text
① gradle: compileKotlinJvm + jvmProcessResources + printRuntimeCp
        ↓  build/classes/kotlin/jvm/main + build/processedResources/jvm/main + runtime-cp.txt
② python scripts/native-image/scan-class-literals.py <每个原生 .dll>  →  jni-config-*.json
        ↓  扫 skiko-windows-x64.dll（skia 的 JNI 类）、awt.dll（AWT 的 JNI 类）
③ python scripts/native-image/gen-args.py --proj . --main <MainKt> --out build/app --gui
        →  build/ni-args-gui-app.txt
        ↓  这里解决 classpath 超 8191 字符、资源 include、JNI 配置清单
④ native-image.cmd @build/ni-args-gui-app.txt            →  exe + 十几个 dll
⑤ 离屏自检（--selftest / --gpu）→ 真开窗一眼 → 交付

# Linux 多一步（实测必需）：
# ②' java -agentlib:native-image-agent=config-output-dir=ni-agent -cp <cp> MainKt
#     扫 .so 抓不到「把 jclass 当参数」的 native 方法（如 SurfaceData.pData），
#     只有运行期追踪能补全。合并见 scripts/native-image/merge-ni-config.py
```

**第 ② 步是整件事的核心**，不是可选优化。原因见下一节。

`@argfile` 不是洁癖：Windows `cmd.exe` 单条命令行上限 **8191 字符**，而 Compose + miuix + skiko 的运行时 classpath 实测 **13.8k 字符**。直接写 `-cp` 会被静默截断，表现为「镜像编出来了但只有 34 MB、里面没有 Compose、一跑就 `NoClassDefFoundError: androidx/compose/runtime/Composer`」。**看到 exe 明显偏小，先怀疑 classpath 被截断。**

---

## 3. 封闭世界 JNI：四类注册，缺一类就崩

native-image 是封闭世界：原生代码里 `FindClass("java/awt/Canvas")` 这种**按字符串找类**的调用，如果该类没在构建期登记，运行时返回 `null`，紧接着就是 `NoClassDefFoundError` 或 `NullPointerException`，而且堆栈通常停在 `Native Method`，看不出是谁干的。

要登记的不是「你写的类」，而是**第三方原生库里硬编码的类名字符串**。做法是直接从 dll 里把类名捞出来（见 `scripts/native-image/scan-class-literals.py`）。

| # | 注册对象 | 数量（实测） | 漏了会怎样 |
| :-- | :-- | :-- | :-- |
| 1 | skiko 的 JNI 类（`skiko-windows-x64.dll` 里的类字面量） | **52** | `Paragraph.layout()` 阶段 `FindClass` 拿到 null，崩在文本排版，**窗口都出不来** |
| 2 | AWT 的 JNI 类（`awt.dll` 里的类字面量） | **146** | `NoClassDefFoundError: java/awt/Canvas`，堆栈在 `sun.awt.windows.WToolkit.eventLoop(Native Method)` |
| 3 | 你自己的 JNI 桥（如 `demo.GlBridge`） | 按需 | `UnsatisfiedLinkError` 或方法找不到 |
| 4 | skia 基础绑定（`org.jetbrains.skia.*` 直接调 JNI 的那些） | 31 / 441 | 图片编解码、字体、Surface 相关随机崩 |

**只注册第 4 类是不够的**——这是最常见的误判：`jni-config.json` 里已经有 31 个 `org.jetbrains.skia.*`，看起来「JNI 配好了」，但 skiko 的 Kotlin 层（`org.jetbrains.skiko.*`、`org.jetbrains.skia.Paragraph` 等）不在里面，因为它们是 skiko dll 用字符串找的，不在 skia 的绑定表里。

### 扫描器的判定逻辑（为什么这样扫）

```python
# 只认「至少两段、且以已知 Java 包根开头」的字面量
pat = rb'(?:java|javax|sun|com/sun|org/jetbrains)/[A-Za-z0-9_$]+(?:/[A-Za-z0-9_$]+)+'
```

- **必须要求至少两段**（`java/awt/Canvas` 而不是 `java`），否则会把 dll 里的普通字符串和单段包名全捞进来。
- **必须扫描业务 dll 与 skiko dll 两份**，它们的字符串表是分开的。
- 扫出来的结果填 `allDeclaredMethods/Fields/Constructors: true`。**不要偷懒只开 `allDeclaredMethods`** —— JNI 的 `GetFieldID` 会用到字段。

### 注册完 AWT 会撞上的第二个坑

注册 146 个 AWT 类后，构建期静态分析会连到两个不可达元素：

```text
java.lang.System.initSystemClassLoader
java.lang.System.props
```

这**不是**你代码的问题，是 AWT 的 `static` 初始化链在构建期被展开。两个取舍：

- 加 `--report-unsupported-elements-at-runtime`（推荐）：构建通过，运行时若真碰到才抛。实测这两个元素运行期不会被访问，窗口、字体、输入法都正常。
- 或者 `--initialize-at-build-time=java.lang.System` 之类逐个压——**不要**，System 类的构建期初始化会引发连锁问题。

---

## 4. 渲染后端：为什么是 OPENGL，以及你多半需要自己写一个 GL 桥

skiko 在 Windows 上的后端有 `DIRECT3D`、`OPENGL`、`SOFTWARE_FAST`、`SOFTWARE_COMPAT`。

| 后端 | native-image 下可用性 | 说明 |
| :-- | :-- | :-- |
| `DIRECT3D` | ❌ | 适配器/设备回调通过 JNI 从 D3D 侧回调进 Java，原生镜像里这条路径注册不全，表现为启动后探测阶段 NPE，然后退回软件渲染。**别指望它** |
| `OPENGL` | ✅ 窗口路径可用 | 走 WGL，系统 `opengl32.dll` 是标准 C ABI，不需要额外注册。这是推荐默认值 |
| `SOFTWARE_FAST` | ✅ | CPU 光栅，永远能跑；RDP / 虚拟显示 / 老显卡的兜底 |
| `SOFTWARE_COMPAT` | ✅ | 比 FAST 慢，仅在怀疑光栅路径有 bug 时对比用 |

设置方式（**在 `main()` 最开头、AWT 初始化之前**）：

```kotlin
private const val DEFAULT_RENDER_API = "OPENGL"

fun main(args: Array<String>) {
    // 1) 先修 java.home —— 见 §5，skiko 靠它拼 jawt.dll 路径
    // 2) 再设后端
    if (System.getProperty("org.graalvm.nativeimage.imagecode") != null &&
        System.getProperty("skiko.renderApi") == null) {
        System.setProperty("skiko.renderApi", DEFAULT_RENDER_API)
    }
    // 3) 应急开关：native-image 下 -D 传递不可靠，手动解析兜底
    args.firstOrNull { it.startsWith("-Dskiko.renderApi=") }
        ?.let { System.setProperty("skiko.renderApi", it.substringAfter('=')) }
    if (args.contains("--soft")) System.setProperty("skiko.renderApi", "SOFTWARE_FAST")
}
```

> **`-Dskiko.renderApi=...` 在 native-image 里不要指望**：GraalVM 默认不把命令行 `-D` 当系统属性（除非 `-H:+UseArgsFileAndSystemProperties` 一类选项，且行为随版本变）。手动解析 argv 是三行代码的事，比赌版本行为可靠。

### GPU 离屏验证要自己写 JNI 桥

想在不弹窗的前提下证明「GPU 真的在跑」，最自然是 `DirectContext.makeGL()` + `Surface.makeRenderTarget()` + `readPixels()`。但 **skiko 0.156 没有导出 `OpenGLOffscreenContext` 的原生函数**——`System.loadLibrary("skiko-windows-x64")` 之后调它，**JVM 上也是 `UnsatisfiedLinkError`**（不是 native-image 的问题，是上游没编译进 dll）。

所以要么放弃离屏 GPU 验证，要么自己建上下文：`CreateWindowEx(WS_POPUP)` + `ChoosePixelFormat` + `wglCreateContext` + `wglMakeCurrent`，**全程不 `ShowWindow`**，窗口在 (0,0) 16×16，用户看不到。

- 现成实现：`references/native-image/gpu-opengl.md`（含完整 `glctx.c`、JNI 命名约定、Skia 侧怎么把它包成 `DirectContext`）。
- 验证产出：`GL_VENDOR/GL_RENDERER/GL_VERSION` + 一回 `readPixels` 的字节数 + 存一张 PNG。
- 实测能拿到：`GL 4.6`、`NVIDIA/Intel/…`、`GPU_RENDER_OK`。

### RDP / 云桌面务必留后路

远程桌面会话里 OpenGL 常常只暴露 **1.1** 软件实现，`wglCreateContext` 能成功但 Skia 的 GL 后端要求 ≥ 2.0，结果是**窗口空白或直接崩**。

- 交付必须同时给 `启动.bat`（默认 GPU）和 `启动-软件渲染.bat`（`--soft`）。
- 判断依据写进 README，让使用者自己会选：**直连显示器/HDMI 投屏 → GPU；远程桌面/云终端 → 软件渲染**。

---

## 5. `java.home` 为 null：那个 `Cannot open (null)\bin\jawt.dll`

native-image 里 `System.getProperty("java.home")` 是 **null**（没有 JDK 目录的概念）。而 skiko 的 `jawt.cc` 是这么找库的：

```c
// 简化示意（skiko 上游实现）
snprintf(path, "%s\\bin\\jawt.dll", getJavaHome());   // java.home 为 null → "(null)\bin\jawt.dll"
LoadLibraryA(path);                                    // 失败
```

于是你看到：

```text
Cannot open (null)\bin\jawt.dll
```

**这个错误不会让程序立刻退出**（skiko 只是拿不到 JAWT，软件渲染还能跑），但一旦走到需要 `JAWT_GetAWT` 的窗口路径就崩。上游方案是静态链接 jawt，需要改 skiko 构建——不现实。

**三行修法**（`main()` 第一件事）：

```kotlin
if (System.getProperty("java.home") == null) {
    val exeDir = runCatching {
        ProcessHandle.current().info().command().orElse(null)?.let { File(it).absoluteFile.parentFile }
    }.getOrNull() ?: File(System.getProperty("user.dir") ?: ".")
    System.setProperty("java.home", exeDir.absolutePath)
}
```

配套交付要求：**exe 同级建 `bin\` 目录，放 `jawt.dll`**。

```text
dist/
├── cdj.exe
├── *.dll              # 从 native-image 输出目录全部拷过来（awt/fontmanager/freetype/lcms/jsound/jvm/…）
└── bin/
    └── jawt.dll       # ← 不能删，删了窗口路径崩
```

**并且 exe 不能改名。** GraalVM 用「构建输出名」去定位同目录的原生库（`loadLibraryRelative` 拼的是 `<imageName>.dll` 与 JDK dll 的相对路径）。把 `cdj.exe` 改成 `刷题通.exe` 后启动，会找不到 `awt.dll` 直接死在入口。要中文名就改**快捷方式**的显示名，别改文件。

---

## 6. 交付形态：GUI 子系统、无黑框、数据目录

```text
-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS
-H:NativeLinkerOption=/ENTRY:mainCRTStartup
```

- 不加就是控制台程序：双击先弹一个黑框，关掉黑框等于杀进程，且 stderr 全屏刷。
- 加了之后**没有 stderr 可见通道**：排查期先用控制台版（不加这两行）跑，稳定后再切 GUI 版。或者构建两个 exe。
- 例外：某些情况下 `mainCRTStartup` 与 GraalVM 的入口 stub 冲突，报 `LNK2019 unresolved external symbol main`。此时只保留 `/SUBSYSTEM:WINDOWS`，让 GraalVM 自己选入口。

**用户数据放 exe 同级 `data\`**，不要用 `%APPDATA%`：

- 绿色交付（整目录拷贝即用）的前提；
- 云教室/机房机器常见「用户目录漫游或只读」，写 `%APPDATA%` 会静默失败；
- 换题库 = 往 `data\` 丢新文件，不用重编。

---

## 7. 验证矩阵（全部不弹窗，除最后一项）

| 层 | 手段 | 通过判据 |
| :-- | :-- | :-- |
| 逻辑层 | `--selftest`：不进 `application {}`，直接调业务入口并打印阶段 | 9 个阶段全 OK |
| 资源层 | 自检里 `readBytes(Res.font.xxx)` + 题库解析 | 字号/字节数与源文件一致 |
| 文本排版 | 自检里对一段中文跑 `Paragraph.layout()` | 不抛 `FindClass` 相关异常（**这一条卡住过 3 次**） |
| GPU | `--gpu`：自建 WGL 上下文 + Skia GL 后端 + `readPixels` | `GPU_RENDER_OK`，像素非全 0，`gpu-proof.png` 可打开 |
| 窗口渲染 | 窗口放屏幕外 `(-32000,-32000)` 后跑 3 秒并计数 | 帧数 ≈ 60×秒数，`renderApi` 是预期值 |
| 真开窗 | 必须真的看一眼（唯一需要人眼的项） | 标题栏/字体/颜色/中文无豆腐块 |
| 性能 | `withFrameNanos` 采样帧间隔 | p50 ≤ 刷新周期 ×1.2，卡顿帧占比 < 2% |

> 「窗口放屏幕外」这一招能测出渲染管线是否在出帧，但**测不出 swapBuffers 到屏幕那一步**。最后一步必须人眼看一次，别省。

实现细节（`--selftest` 的 9 个阶段、`withFrameNanos` 统计 p50/p90/p99、启动耗时与内存测量脚本）见 `references/native-image/verification.md`。

---

## 8. 实测基线（用于判断「是不是编坏了」）

环境：Windows 10 x64、Liberica NIK 23.1.8（JDK 21.0.8）、MSVC 14.44、Compose 1.13.0-beta02、miuix 0.9.4、skiko 0.156.1、Rust 逻辑层经 JNI。

| 指标 | 实测值 |
| :-- | :-- |
| 镜像体积（真实应用，含 3 个字重的 MiSans + 题库 JSON） | 150 MB（demo 版 122–128 MB） |
| 冷启动 → 窗口句柄有效 | **297 / 326 / 448 ms**（三次，首次最慢） |
| 工作集 / 私有内存 | 199 MB / 129 MB |
| 帧率（OPENGL，3 秒计数） | 179 帧 ≈ 59.7 fps |
| 帧率（SOFTWARE_FAST，3 秒计数） | 194 帧 ≈ 64.7 fps（不受 vsync 限制时略高） |
| 构建耗时（native-image 阶段） | 2 分 20 秒 ～ 3 分 12 秒（`-J-Xmx5g`） |
| 交付目录总大小 | 153 MB |

**体积主要来自**：skiko 的 skia 静态库 + JDK 的 AWT/字体/图像编解码。`-H:IncludeResources` 把 ttf/json 也打进去了，各占几 MB。不要试图用 `-Os` 之类硬压，`-Ob`（默认）已经够；压体积对启动几乎无收益。

---

## 9. 坑表（按症状查，详细版在 `references/native-image/pitfalls.md`）

| 症状 | 根因 | 修法 |
| :-- | :-- | :-- |
| `NoClassDefFoundError: java/awt/Canvas`，堆栈在 `WToolkit.eventLoop(Native Method)` | awt.dll 用字符串找 146 个类，未注册 | 扫 `awt.dll` → `jni-config-awt.json` |
| 窗口不出、崩在 `Paragraph.layout()` | skiko dll 找 52 个类，未注册 | 扫 `skiko-windows-x64.dll` → `jni-config-literals.json` |
| `Cannot open (null)\bin\jawt.dll` | `java.home` 为 null | `main()` 里设 `java.home` = exe 目录 + 交付 `bin\jawt.dll` |
| 构建报 `System.initSystemClassLoader` 不可达 | AWT 静态初始化链 | `--report-unsupported-elements-at-runtime` |
| 构建报 `kotlin.DeprecationLevel was unintentionally initialized at build time` | Kotlin 注解枚举被提前初始化 | `--initialize-at-build-time=kotlin.DeprecationLevel` |
| exe 只有 34 MB、跑起来没有 Compose | classpath 被 cmd 8191 截断 | @argfile |
| 双击闪退、无明显日志 | GUI 子系统吞了 stderr | 先编控制台版复现 |
| 换名后启动即崩、找不到 `awt.dll` | GraalVM 按输出名定位原生库 | 不改 exe 文件名，改快捷方式 |
| 远程桌面里窗口空白 | RDP 的 GL 只有 1.1 | `--soft` |
| 高 DPI 下窗口跑到屏幕外 | 逻辑尺寸 × 缩放 > 屏幕 | 见 `references/native-image/pitfalls.md` P14 |
| 字体全豆腐块 | compose 资源没进 classpath / 没进镜像 | `-H:IncludeResources=.*\.(ttf|otf)$` + 资源根目录正确 |
| `expect`/`actual` 编译不过 | 纯 `kotlin("jvm")` 不支持 | 换 `kotlin("multiplatform")`，只声明 `jvm()` |
| Linux：`NoClassDefFoundError: ... sun.awt.X11.XToolkit` | `NoSuchFieldError: sun.java2d.SurfaceData.pData`，扫 `.so` 扫不到该类 | tracing agent 补 jni-config（`linux.md` §6） |
| Linux：链接期 `undefined reference to Java_java_lang_Thread_get…` | 注册了 `java.lang.Thread` 且带方法表 | 全注册 + 自带 `thread_stub.o`（`linux.md` §7） |
| Linux：`Cannot open …/lib/libjawt.so` 后**段错误** | `java.home` 为 null、`libjawt.so` 没随包 | 设 `java.home` + 交付 `lib/libjawt.so`（`linux.md` §8） |
| Linux：`Could not load font`，进程卡死 | 目标机没有 fontconfig / CJK 字体 | 装字体或把 ttf 打进镜像（`linux.md` §10） |

---

## 10. 交付前自检

- [ ] NIK 是 **full** 版，`bin\` 下有 `awt.dll` / `jawt.dll` / `fontmanager.dll`
- [ ] `jni-config` 至少涵盖 **skiko 字面量 + awt 字面量 + 自己的桥 + skia 基础**，四份都进了 `-H:JNIConfigurationFiles`
- [ ] 走 `@argfile`，且打印过 classpath 字符数（应 > 10k）
- [ ] `main()` 第一件事设 `java.home`
- [ ] 交付目录有 `bin\jawt.dll`；exe **没有**改名
- [ ] 资源：ttf / json / 时区数据（`tzdb.dat`）都在 `-H:IncludeResources` 里
- [ ] 两个启动器（GPU / 软件渲染），README 写清何时用哪个
- [ ] 离屏自检全绿；真开窗看过一眼；关掉进程无残留
- [ ] 换一台没装 JDK 的机器跑过（这是 native-image 唯一的意义）

---

## 参考文件（本章）

| 文件 | 内容 |
| :-- | :-- |
| `references/native-image/toolchain.md` | Liberica NIK full 选型与验证、MSVC 环境变量（不依赖 vcvars）、Gradle 镜像与 wrapper 预置、5 行自检 |
| `references/native-image/jni.md` | 封闭世界 JNI 完整机制：四类注册、扫描器原理、jni-config 字段语义、`jniAccessible` 与 `--initialize-at-build-time` 的取舍 |
| `references/native-image/gpu-opengl.md` | Skia GL 后端、`glctx.c` 全文、JNI 命名与加载顺序、离屏 `readPixels` 验证、为什么不能直接用 `OpenGLOffscreenContext` |
| `references/native-image/pitfalls.md` | P1–P20 坑表：症状、堆栈原文、根因、修法、如何验证修好了 |
| `references/native-image/verification.md` | `--selftest` 九阶段实现、`withFrameNanos` 帧间隔统计、启动/内存测量脚本、离屏窗口计数法 |
| `references/native-image/linux.md` | **Linux 篇（实测）**：工具链、tracing agent、`java.lang.Thread`、原生库布局、llvmpipe 黑名单、字体、坑表 L1–L10 |

脚本（都在 `scripts/native-image/` 下，Python 3 / Windows bat，零第三方依赖）：

| 文件 | 用途 |
| :-- | :-- |
| `scripts/native-image/scan-class-literals.py` | 扫 dll 里的 Java 类路径字面量 → jni-config.json（可多文件合并、可去重） |
| `scripts/native-image/gen-args.py` | 参数化生成 native-image @argfile（classpath、资源、JNI 清单、GUI 子系统） |
| `scripts/native-image/ni-env.bat` | MSVC + NIK 环境变量模板（不调 vcvars） |
| `scripts/native-image/merge-ni-config.py` | 把扫描结果与 tracing agent 的配置合并成 `-H:ConfigurationFileDirectories` 目录（Linux 必需） |

---

## 本章从哪来（来源与实测）

内容全部来自一次真实的构建：一个 Compose Desktop + miuix 0.9.4 的答题应用（含 Rust 业务层、300 题题库、三个字重的 MiSans），用 Liberica NIK full 23.1.8 打成 150 MB 的 `cdj.exe`，默认 OpenGL 渲染。**不是「理论上应该行」。**

踩过并已解决的 20 个问题（与 `pitfalls.md` 的 P1–P20 一一对应）：

| # | 问题 |
| :-- | :-- |
| 1 | 机器上没有 C/C++ 编译器；`vcvars64.bat` 在受限账户下被 `reg.exe` 拦截 |
| 2 | Gradle distribution 下载不通（镜像 + wrapper 手动预置） |
| 3 | classpath 13.8k 字符，超 cmd 8191 上限 → 34 MB 空壳 exe |
| 4 | `runtime-cp.txt` 末尾换行导致变量被读空 |
| 5 | skiko dll 的 52 个类字面量未注册 → `Paragraph.layout()` 崩 |
| 6 | AWT 的 146 个类字面量未注册 → `NoClassDefFoundError: java/awt/Canvas` |
| 7 | 注册 AWT 后构建期连到 `System.initSystemClassLoader` → 需要 `--report-unsupported-elements-at-runtime` |
| 8 | `java.home` 为 null → `Cannot open (null)\bin\jawt.dll` |
| 9 | exe 改名后找不到 `awt.dll`（GraalVM 按输出名定位原生库） |
| 10 | 双击弹黑框 → `/SUBSYSTEM:WINDOWS`；代价是丢 stderr |
| 11 | `kotlin.DeprecationLevel was unintentionally initialized at build time` |
| 12 | 共享 KMP 源码的 `expect/actual` 在纯 `kotlin("jvm")` 工程里不合法 |
| 13 | compose 资源没进 classpath / 没进镜像 → 字体豆腐块 |
| 14 | 250% DPI 下 420×680dp 窗口跑到屏幕外 |
| 15 | `DIRECT3D` 在 native-image 下静默降级 |
| 16 | skiko 未导出 `OpenGLOffscreenContext` → 自写 `glctx.dll`（WGL + 隐藏窗口） |
| 17 | RDP 下 GL 只有 1.1 → `--soft` 兜底 |
| 18 | GUI 版闪退无日志 |
| 19 | Rust JNI dll 在 native-image 下 `java.library.path` 为空 |
| 20 | 拷到别的机器就崩（漏 dll / 漏 `bin\jawt.dll` / 目标机器是 RDP） |

**已知边界**：

- **Windows x64 与 Linux x64（X11、无 GPU）各验证一遍**。两边的 JDK 选型、JNI 收集方式、原生库布局、渲染后端行为都不同，参数不能互换；macOS 未验证。Linux 见 `references/native-image/linux.md`。
- **`DIRECT3D` 未解决**，只给了绕开方案（`OPENGL`）。
- **skiko 的 `OpenGLOffscreenContext` 缺失是上游问题**，本章给的是自建桥，不是修复上游。
- 帧率数据来自 1280×720、Intel UHD 630 的机器；高分辨率/高刷屏请自行复测。
- 文档里的版本号（NIK 23.1.8 / Compose 1.13.0-beta02 / miuix 0.9.4 / skiko 0.156.1）是实测组合，升级后请重新跑 `--selftest`。

> 本章与 `native-image/` 下的 5 个参考文件原为独立 skill（`miuix-native-image`），并入本仓库时只调整为「章 + 参考目录」的路径与编号，**技术与结论未改动**。
