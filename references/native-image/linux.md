# Linux 篇：Compose Desktop + Miuix 的 native-image（实测）

> 本篇不是把 Windows 篇改几个路径得来的，而是**在 Linux 上从零构建了一遍**，记下真实发生的每一步。
> 凡标「实测」的都有对应日志或数据；标「未验证」的不要当结论用。

## 0. 这篇验证了什么

| 项 | 状态 | 证据 |
| :-- | :-- | :-- |
| JVM 上 miuix 0.9.4 界面在 Linux 渲染 | ✅ 实测 | Xvfb 截图：顶栏 + `SwitchPreference` + `Card` 列表，中文无豆腐块 |
| 同一份代码编成 native-image | ✅ 实测 | 5 分钟出镜像，产物 106.8 MB + 8 个 `.so` |
| native 镜像里界面正常渲染 | ✅ 实测 | 同一张截图，与 JVM 版逐像素同布局 |
| `skiko.renderApi=OPENGL` 在 Linux 真的走 GL | ⚠️ 有条件 | 软渲染器 `llvmpipe` 被 skiko 黑名单拒绝；`softpipe` 下确认走 GL（无 fallback 警告） |
| 真 GPU（NVIDIA/AMD/Intel）下的 GPU 渲染 | ❌ 未验证 | 本机无 GPU；结论只覆盖软件 GL |
| 启动耗时、体积 | ✅ 实测 | 0.12–0.13 s 到首帧；139 MB 交付目录 |
| 高 DPI / 多显示器 / Wayland | ❌ 未验证 | 环境是 Xvfb + X11，无缩放 |
| 中文输入法 | ❌ 未验证 | 只验证了中文**渲染** |

**一句话**：Linux 上这条路是通的，但「JNI 配置」和「skiko 黑名单」两件事与 Windows 完全不同，照抄 Windows 篇会卡住。

---

## 1. 环境（实测用到的完整版本）

| 组件 | 版本 | 说明 |
| :-- | :-- | :-- |
| 系统 | Ubuntu 24.04，kernel 6.6，x86_64 | 无 GPU 的容器 |
| JDK / native-image | **GraalVM for JDK 21.0.12**（Oracle GraalVM，`download.oracle.com/graalvm/21/latest`） | 见 §3：**不需要** BellSoft NIK full |
| Kotlin | 2.4.20 | miuix 0.9.4 的 stdlib 依赖就是 2.4.20 |
| Compose Multiplatform | 1.12.2 | miuix 0.9.4 依赖 foundation 1.12 |
| Miuix | 0.9.4（`miuix-ui` / `-preference` / `-icons`） | 桌面产物坐标带 `-desktop` 后缀 |
| skiko | **0.150.2**（Compose 1.12.2 传递依赖） | 注意：不是 Windows 篇里的 0.156.1 |
| kotlinx-coroutines-swing | 1.11.0 | 桌面 `Dispatchers.Main` |
| Gradle | 9.3.0 | |
| 显示 | Xvfb `1600x1000x24`（`:99`） | 无窗口管理器 |
| OpenGL | Mesa 25.2.8：`llvmpipe` → GL **4.5** core；`softpipe` → GL **3.3** core | 均为 CPU 光栅 |
| 编译 | gcc 13.3 + zlib 头 | native-image 链接期需要 |

系统包（缺一个就编不出/起不来）：

```bash
apt-get install -y build-essential zlib1g-dev \
  libx11-6 libxext6 libxrender1 libxtst6 libxi6 \
  libfontconfig1 libfreetype6 fonts-noto-cjk mesa-utils
```

---

## 2. 与 Windows 篇的差异（先看这张表）

| 维度 | Windows（原文） | Linux（实测） |
| :-- | :-- | :-- |
| JDK 发行版 | 必须 Liberica NIK **full**，standard 缺 `jawt.dll` | **GraalVM 官方包就够**：自带 `libawt.so` / `libawt_xawt.so` / `libjawt.so` / `libfontmanager.so` |
| AWT 原生库从哪来 | 从 JDK `bin\` 拷 | native-image **自己产出** 8 个 `.so`（`libawt.so`、`libawt_headless.so`、`libawt_xawt.so`、`libfontmanager.so`、`libjavajpeg.so`、`liblcms.so`、`libjava.so`、`libjvm.so`） |
| 唯一要手拷的库 | `bin\jawt.dll` | `lib/libjawt.so`（7.6 KB，GraalVM **不**产出它） |
| 库的搜索路径 | `java.home` 拼 `bin\` | `java.home` 拼 **`lib/`**（`hostOs.isWindows ? "bin" : "lib"`） |
| exe 改名 | 会崩：按输出名定位 DLL | 实测改名后照常启动（用 `LD_LIBRARY_PATH` 定位 `.so`） |
| classpath 分隔符 | `;` | **`:`**（脚本里写死 `;` 会得到一条 9000 字符的「单个路径」） |
| 命令行上限 | 8191 字符（必须 argfile） | `ARG_MAX` 2 MB，但 classpath 实测 **9477 字符**，还是建议 argfile（可复现） |
| GUI 无黑框 | `/SUBSYSTEM:WINDOWS` + `/ENTRY` | 无对应开关，控制台程序在 X11 下没有「黑框」问题 |
| DPI | 要嵌 `RT_MANIFEST`（PerMonitorV2） | 未验证；X11 下走 `sun.java2d.uiScale` / 环境缩放 |
| 字体 | 系统自带 MiSans/雅黑 | **必须**装 fontconfig + CJK 字体，缺了不是豆腐块而是直接挂（§10） |
| `java.library.path` | 镜像里为空 → 需绝对路径 load | 镜像里是**构建机默认值**（`/usr/lib64:/lib64:/lib:/usr/lib`），仍不含 exe 目录 |

---

## 3. 工具链：GraalVM 21 就够，不用找 NIK full

Windows 篇要求 NIK **full**，是因为 Windows 版 GraalVM 不带 `awt.dll` / `jawt.dll`。**Linux 版 GraalVM 自带全套 AWT 原生库**，实测 `$JAVA_HOME/lib/` 下有：

```text
libawt.so  libawt_headless.so  libawt_xawt.so  libfontmanager.so  libjawt.so  liblcms.so
```

验证（3 行）：

```bash
GVM=/opt/graalvm-jdk-21
ls $GVM/lib | grep -E 'libawt|libjawt|libfontmanager|liblcms'   # 6 个都在
$GVM/bin/native-image --version                                  # native-image 21.x
```

> 用 Oracle GraalVM 还是 Liberica NIK full 在 Linux 上都可以；本篇只验证了前者。
> 生产分发注意 Oracle GraalVM 的 GFTC 条款，Liberica NIK（GPL+CE）更宽松——这一条是许可问题，不是技术问题。

---

## 4. 构建流水线（Linux 版）

```text
① gradle: compileKotlinJvm + jvmProcessResources + printRuntimeCp
        ↓  build/classes/kotlin/main + build/resources/main + build/runtime-cp.txt
② python scripts/native-image/scan-class-literals.py \
       libskiko-linux-x64.so $JAVA_HOME/lib/libawt.so $JAVA_HOME/lib/libawt_xawt.so \
       $JAVA_HOME/lib/libfontmanager.so -o native-image/jni-config.json
③ java -agentlib:native-image-agent=config-output-dir=ni-agent -cp <cp> MainKt   ← Linux 必须加这一步
④ 合并 ②③ → native-image/config/{jni,reflect,resource}-config.json
⑤ native-image @native-image/ni-args.txt
⑥ Xvfb/真桌面下跑 --selftest，截图，量化
```

第 ③ 步是 Linux 与 Windows 最大的方法论差异，原因见 §6。

### 4.1 classpath 分隔符

Linux 用 `:`。Windows 篇的 `printRuntimeCp` 写法（`joinToString(";")`）在 Linux 上会把**整条 classpath 拼成一个不存在的路径**，报错却像「找不到类」：

```kotlin
// build.gradle.kts —— 跨平台写法
val sep = System.getProperty("path.separator")   // Windows ";", Linux ":"
val cp = configurations.runtimeClasspath.get().files.joinToString(sep) { it.absolutePath }
```

本仓库的 `scripts/native-image/gen-args.py` 已按 `os.name` 自动选分隔符，并且会把误写成 `;` 的 `runtime-cp.txt` 自动改写（附告警）。

### 4.2 镜像里的 classpath 长度

实测 Compose 1.12.2 + miuix 0.9.4 + skiko 0.150.2：`runtime-cp.txt` 本身 **9477 字符**（约 50 个 jar），加上 classes/resources 后 argfile 里的 `-cp` 是 **9520 字符**。
Linux 不受 8191 限制，但一份 argfile 仍然值得：参数可复现、可 diff、可提交进仓库。

### 4.3 argfile（实测可用）

```text
-J-Xmx8g
-cp
<build/classes/kotlin/main>:<build/resources/main>:<50 个 jar>
MainKt
--no-fallback
-H:+UnlockExperimentalVMOptions
-Djava.awt.headless=false
-H:+ReportExceptionStackTraces
--report-unsupported-elements-at-runtime
--initialize-at-build-time=kotlin.DeprecationLevel
-H:+UseServiceLoaderFeature
-H:ConfigurationFileDirectories=native-image/config
-H:IncludeResources=.*\.(ttf|otf|properties|xml|png|jpg|json|txt|bin)$
-H:IncludeResources=META-INF/services/.*$
-H:IncludeResources=tzdb\.dat$
-H:IncludeResources=.*\.res$
--enable-url-protocols=http,https
-o build/native/miuix-linux-demo
```

实测构建：**4 分 50 秒 – 5 分 52 秒**，峰值 RSS **5.5 GB**（32 核机器，`-J-Xmx8g`）。
耗时分布：analysis 78 s / parsing 36 s / compiling 197 s / link 其余。**编译阶段占大头**，加核有用。

---

## 5. 产物：多出 8 个 `.so`，少一个 `libjawt.so`

```text
build/native/
├── miuix-linux-demo            106.8 MB   ← 主程序（单文件，但不是「一个文件就能跑」）
├── libawt.so                   0.9 MB   ┐
├── libawt_headless.so          0.04 MB  │
├── libawt_xawt.so              0.5 MB   │  native-image 自动产出，
├── libfontmanager.so           1.9 MB   │  必须与主程序同目录
├── libjavajpeg.so              0.2 MB   │
├── liblcms.so                  0.5 MB   │
├── libjava.so                  0.01 MB  │  shim
└── libjvm.so                   0.01 MB  ┘  shim
```

这跟 Windows 的「exe + 十几个 DLL」是同一回事，只是 Linux 由 GraalVM 现造，不从 JDK 拷。
**`libjawt.so` 不在上面**——GraalVM 不产出它，但 skiko 要 `dlopen` 它（见 §8）。

---

## 6. 核心差异：AWT 的 JNI 面，光扫 `.so` 扫不全

### 6.1 扫出来多少（实测）

| 文件 | 扫到的类字面量 |
| :-- | :-- |
| `libskiko-linux-x64.so` | 48 |
| `libawt.so` | 46 |
| `libawt_xawt.so` | 54 |
| `libfontmanager.so` | 13 |
| 去重合计 | **147**（含 1 个必须剔除的 `java.lang.Thread`，见 §7） |

数量和 Windows 的 52 + 146 完全不是一个量级——**Linux 版的 AWT 原生库字面量少得多**，因为它大量使用「直接把 `jclass` 传进 native 方法」的写法，而不是 `FindClass("...")`。

### 6.2 于是崩在 `SurfaceData.pData`

第一次跑 native 产物（147 条全注册）：

```text
NoClassDefFoundError: Could not initialize class sun.awt.X11.XToolkit
        at sun.awt.PlatformGraphicsInfo.createToolkit(PlatformGraphicsInfo.java:41)
```

只有一层壳，看不到根因。加几行探针（`-Dorg.graalvm.nativeimage.imagecode` 非空时先手动 `Toolkit.getDefaultToolkit()` 并打印 cause 链）才拿到：

```text
java.lang.NoSuchFieldError: sun.java2d.SurfaceData.pData
        at JNIFunctions$Support.getFieldID(JNIFunctions.java:1864)
        at sun.java2d.SurfaceData.initIDs(Native Method)
        at sun.java2d.SurfaceData.<clinit>(SurfaceData.java:120)
```

**为什么扫不到**：`Java_sun_java2d_SurfaceData_initIDs` 拿的是**方法参数里的 `jclass`**，不是 `FindClass` 的字符串。二进制里根本没有 `sun/java2d/SurfaceData` 这个字面量（`strings libawt.so | grep SurfaceData` 只能看到 `Java_sun_java2d_SurfaceData_initIDs` 这种符号名）。扫不出来，但你**必须**登记它的字段，否则 `GetFieldID` 返回 null → 静态块炸 → `XToolkit` 初始化失败 → 窗口根本起不来。

### 6.3 解法：用 tracing agent 补，不是在坑里一个个猜

```bash
java -agentlib:native-image-agent=config-output-dir=ni-agent \
     -cp "<cp>" -Dskiko.renderApi=OPENGL MainKt --frames=60
```

跑一遍真实 UI（要真的渲染、真的排版、真的开关一次），agent 会记下运行期所有 `FindClass` / `GetFieldID` / `GetMethodID` / 资源 / 反射：

```text
jni-config.json         111 条（其中就有 sun.java2d.SurfaceData + fields[pData, valid]）
reflect-config.json      43 条
resource-config.json      3 条
proxy-config.json         0 条
```

把**扫描结果 ∪ agent 结果**合并（本仓库 `scripts/native-image/merge-ni-config.py`）：

```text
147（扫，剔除 Thread 后 146） ∪ 111（agent） → 168 条
```

然后 `-H:ConfigurationFileDirectories=native-image/config`（一次带上 jni/reflect/resource/proxy 四份），`XToolkit` 初始化就通了，日志里出现我们自己的 `AWT_TOOLKIT_OK`。

> **结论（可迁移到任何平台）**：`.so`/`.dll` 字面量扫描只是**起点**，它抓的是 `FindClass("...")` 这一类；
> AWT/Swing 里还有大量「把 jclass 当参数」的 native 方法，只有**运行期追踪**能抓全。
> 两条腿走路：扫（构建期、静态）+ agent（运行期、动态），缺一条就会在窗口阶段崩。

---

## 7. `java.lang.Thread`：注册也错、不注册也错

这是 Linux 上最拧巴的一个坑，三种状态都实测过：

| 做法 | 构建 | 运行 |
| :-- | :-- | :-- |
| 不注册（扫到后删掉） | ✅ 链接成功 | ❌ `NoClassDefFoundError: java/lang/Thread`，堆栈在 `XToolkit.waitForEvents(Native Method)`（AWT 事件循环每轮都 `FindClass("java/lang/Thread")`） |
| 注册 + `allDeclaredMethods: true` | ❌ 链接失败：`undefined reference to Java_java_lang_Thread_getNextThreadIdOffset` / `..._getStackTrace0` | — |
| 注册但**只给字段**（`allDeclaredFields: true`，其余 false） | ✅ 链接成功 | ⚠️ 能跑、能出图，但 Toolkit 线程每轮抛 `IllegalMonitorStateException`（实测日志：`WARNING: Exception on Toolkit thread`） |
| 注册全 + **自带两个符号的 stub** | ✅ 链接成功 | ✅ 干净，无 `IllegalMonitorStateException` |

推荐最后一种。两个符号在 VM 内部实现，`libjava.a` 里没有，所以链接期缺；AWT 只拿 `Class` 对象、**不会真的调用**这两个方法，补个空实现即可：

```c
// native-image/stub/thread_stub.c
#include <jni.h>
JNIEXPORT jlong JNICALL Java_java_lang_Thread_getNextThreadIdOffset(JNIEnv *e, jclass c) { return 16; }
JNIEXPORT jobjectArray JNICALL Java_java_lang_Thread_getStackTrace0(JNIEnv *e, jobject t) { return NULL; }
```

```bash
gcc -c -fPIC -I$JAVA_HOME/include -I$JAVA_HOME/include/linux thread_stub.c -o thread_stub.o
# 然后加进 argfile
-H:NativeLinkerOption=/abs/path/thread_stub.o
```

> 别用「注册 Thread 但不带方法」那一档凑合：能出图，但 Toolkit 线程每轮抛异常，
> 输入法、拖拽、窗口事件这些依赖 Toolkit 线程的功能迟早出问题。要么补 stub，要么把 AWT 换成不依赖 Thread 的路径（不存在）。

---

## 8. 运行期原生库：三个目录、一个环境变量

实测的交付布局（能跑起来的最小集合）：

```text
dist/
├── miuix-linux-demo          # 主程序（不可省）
├── libawt.so  libawt_headless.so  libawt_xawt.so
├── libfontmanager.so  libjavajpeg.so  liblcms.so
├── libjava.so  libjvm.so            # 与主程序同目录：GraalVM 用绝对路径 dlopen
├── lib/
│   ├── libjawt.so                   # ← 从 $JAVA_HOME/lib 拷，GraalVM 不产出
│   └── libskiko-linux-x64.so        # ← 从 skiko-awt-runtime-linux-x64-*.jar 解出
└── run.sh
```

`run.sh`：

```bash
#!/usr/bin/env bash
cd "$(dirname "$0")"
export LD_LIBRARY_PATH="$(pwd):$(pwd)/lib:${LD_LIBRARY_PATH}"
exec ./miuix-linux-demo "$@"
```

三件事要对上，缺一个就是「能启动、一画就崩」：

1. **`java.home`**：镜像里是 `null`。skiko 的 Linux 实现按 `java.home + "/lib/libjawt.so"` 找 JAWT，`null` 时得到 `/lib/libjawt.so`（相对根目录），报：

   ```text
   Cannot open /path/lib/libjawt.so: cannot open shared object file: No such file or directory
   ```
   **紧接着是 segfault（exit 134），不是异常**——所以别把它当「警告」忽略。修法同 Windows 篇：`main()` 第一件事把 `java.home` 设成 exe 目录。

2. **`lib/libjawt.so`**：`cp $JAVA_HOME/lib/libjawt.so dist/lib/`。它依赖 `libawt.so` / `libawt_xawt.so` / `libjava.so`，这些在主程序目录，所以 `LD_LIBRARY_PATH` 必须包含 exe 目录。

3. **skiko 的 `.so`**：镜像里没有 jar，skiko 的「从资源里解压」这条路走不通。二选一：

   ```bash
   # A. 放 java.home/lib/（零代码，推荐）
   cp libskiko-linux-x64.so dist/lib/
   # B. 显式指定（代码里设，native-image 下命令行 -D 不生效）
   System.setProperty("skiko.library.path", File(appDir(), "lib").absolutePath)
   ```

实测确认 `System.load(绝对路径)` 在镜像里可用（我们的探针依次打印 `LOAD_FILE_OK awt / awt_xawt / fontmanager / lcms`），所以「绝对路径 + 自己 load」永远是最稳的兜底。

---

## 9. 渲染后端：`skiko.renderApi=OPENGL` 在无 GPU 机器上会被**主动拒绝**

### 9.1 现象

`-Dskiko.renderApi=OPENGL`，Xvfb + Mesa llvmpipe（GL 4.5 core，`glxinfo` 一切正常），程序启动后输出：

```text
[SKIKO] warn: Fallback to next API
org.jetbrains.skiko.RenderException: Cannot create Linux GL context
```

然后照常出图、帧率还挺高（263 fps）。**看起来「GL 上下文创建失败」，其实 GL 上下文是建成功了的。**

### 9.2 根因（skiko 0.150.2 源码）

`LinuxOpenGLRedrawer` 的顺序是：`createContext()` → `makeCurrent()` → 读 `GL_RENDERER` → `isVideoCardSupported(...)`：

```kotlin
// skiko-awt-0.150.2-sources.jar
if (context == 0L) throw RenderException("Cannot create Linux GL context")
it.makeCurrent(context)
adapterName.also { adapter ->
    if (adapter != null && !isVideoCardSupported(GraphicsApi.OPENGL, hostOs, adapter))
        throw RenderException("Cannot create Linux GL context")   // ← 同一个消息
}
```

而黑名单里写着：

```kotlin
// jvmMain/org/jetbrains/skiko/GraphicsApi.jvm.kt
NotSupportedAdapter(os = OS.Linux, api = GraphicsApi.OPENGL, pattern = "llvmpipe", exactPattern = false)
NotSupportedAdapter(os = OS.Linux, api = GraphicsApi.OPENGL, pattern = "virgl",   exactPattern = false)
```

**所以**：只要 `GL_RENDERER` 以 `llvmpipe` 或 `virgl` 开头（无 GPU 的容器 / 云桌面 / VirGL 虚拟化），skiko 就主动放弃 GL，回退到软件渲染，并且**只打一行 warn**。别把「Cannot create Linux GL context」当成 GLX 配置问题去调 Mesa——调不出来。

### 9.3 想在无 GPU 环境下验证 GL 路径：换掉渲染器名字

`GALLIUM_DRIVER=softpipe`（Mesa 的参考光栅器，GL 3.3，**不在黑名单里**）：

```bash
DISPLAY=:99 GALLIUM_DRIVER=softpipe LD_LIBRARY_PATH=".:lib" ./miuix-linux-demo --frames=60
```

实测：**不再出现 fallback 警告**，帧率 14.3 fps、p50 74 ms——慢，但这是**真的走了 GL 管线的 CPU 光栅**，用来验证「GL 路径能不能跑通」足够，用来测性能没意义。

### 9.4 三种路径的实测数据（同一份二进制，同一台机器）

| 配置 | 是否走 GL | 60 帧耗时 | 帧率 | p50 |
| :-- | :-- | :-- | :-- | :-- |
| `OPENGL` + llvmpipe | ❌ 被黑名单拒绝，回退 Skia CPU | 228 ms | 263 fps | 2 ms |
| `OPENGL` + softpipe | ✅ 真 GL 3.3 | 4185 ms | 14.3 fps | 74 ms |
| JVM + llvmpipe（对照） | ❌ 同样回退 | 1474 ms/180 帧 | 122 fps | 5 ms |

> **263 fps 不是好消息**：那是「Xvfb 无 vsync + Skia CPU 光栅」的组合，数字好看但没有显示同步。
> 真实机器上请以「p50 ≈ 刷新周期（16.7 ms）且 p99 不爆」为准，而不是拿无头环境的 fps 当性能指标。

### 9.5 结论怎么写进交付

- 有 GPU 的常规 Linux 桌面：`OPENGL` 默认值可用（本篇无 GPU，**未**在真 GPU 上验证）。
- 容器 / 云桌面 / 虚拟机（llvmpipe、virgl）：`OPENGL` 会被静默降级成软件渲染，**功能正常但吃 CPU**。
- 想让用户自己选：给两个启动脚本（`run.sh` / `run-soft.sh` 传 `--soft`），README 写「远程桌面/无 GPU → 软件渲染」。

---

## 10. 字体：缺字体会**挂**，不是豆腐块

把 fontconfig 指到一个空目录（模拟目标机没装中文字体）：

```bash
FONTCONFIG_FILE=/tmp/nofonts/fonts.conf ./miuix-linux-demo --frames=30
```

实测结果（不是画方块）：

```text
java.lang.IllegalStateException: Could not load font
        at androidx.compose.ui.text.font.TypefaceRequestCache.runCached(FontFamilyResolver.kt:214)
        ...
        at ParagraphBuilder.build(ParagraphBuilder.skiko.kt:419)
```

进程**卡住不退出**，窗口不出现（`timeout` 124）。因为 `TypefaceRequestCache` 拿不到任何 fallback 字体，`ResolveResult` 里数组为空 → `NoSuchElementException: Array is empty`。

交付要求：

- 目标机必须有 `fontconfig` + 至少一套 CJK 字体（`fonts-noto-cjk` 或文泉驿）。
- 要绝对可控就把字体打进镜像并 `-H:IncludeResources=.*\.(ttf|otf)$`，再用 `FontFamily(Font(resource = ...))` 显式加载；系统字体只做兜底。
- 这条在 Windows 上不成立（系统自带字体），是 Linux 的独立风险。

---

## 11. Compose 自己的错误弹窗，在镜像里会二次崩

没有字体时还叠了一层：

```text
java.lang.Error: no ComponentUI class for:
  androidx.compose.ui.window.WindowExceptionHandlerFactory_desktopKt$showErrorDialog$pane$1
```

Compose Desktop 捕获到 UI 异常后会弹一个 **Swing** 的 `JOptionPane`，而 native-image 里没人用过 Swing，L&F 类没进镜像 → 弹窗自己炸，原始错误被埋。

所以：

- `application { }` 外层必须自己 `runCatching` + 写日志文件（见 Windows 篇 §3 的三个兜底位置）；
- 或者把 Swing 也注册进镜像（`reflect-config` + Swing 资源），但那是为了「弹窗好看」付的镜像体积，不划算；
- 交付判据：异常时**只写日志、不弹窗**。

---

## 12. 量化基线（Linux，无 GPU）

| 指标 | 实测 |
| :-- | :-- |
| 构建耗时 | 4 分 50 秒 – 5 分 52 秒（32 核，`-J-Xmx8g`） |
| 构建峰值 RSS | 5.5 GB |
| 主程序 | **106.8 MB** |
| JDK `.so` 合计 | 4.16 MB（8 个） |
| skiko `.so` | 28.3 MB |
| `libjawt.so` | 7.6 KB |
| 交付目录合计 | **139.2 MB** |
| 启动（到首帧 + 退出，`--frames=1`） | **0.12 / 0.12 / 0.13 s** |
| JVM 对照（到窗口可见） | 62–73 ms（`WINDOW_READY`） |
| 帧率 | 见 §9.4（无 GPU 环境下不具备参考价值） |

---

## 13. 坑表（Linux 专属）

| # | 症状 | 根因 | 修法 |
| :-- | :-- | :-- | :-- |
| L1 | `NoClassDefFoundError: Could not initialize class sun.awt.X11.XToolkit` | `NoSuchFieldError: sun.java2d.SurfaceData.pData`，`GetFieldID` 没登记 | **tracing agent** 补 jni-config（扫 `.so` 扫不到），见 §6 |
| L2 | 链接期 `undefined reference to Java_java_lang_Thread_getNextThreadIdOffset` | 注册了 `java.lang.Thread` 且 `allDeclaredMethods: true`，这两个方法由 VM 实现 | 见 §7：全注册 + 补 stub `.o`（推荐），或只注册字段（有 `IllegalMonitorStateException` 副作用） |
| L3 | `NoClassDefFoundError: java/lang/Thread`，堆栈在 `XToolkit.waitForEvents(Native Method)` | 把 `Thread` 从 jni-config 里删了 | 同上，注册回去 |
| L4 | `Cannot open .../lib/libjawt.so` 后**段错误**（exit 134） | `java.home` 为 null + 没带 `libjawt.so` | `main()` 设 `java.home` = exe 目录；`cp $JAVA_HOME/lib/libjawt.so dist/lib/` |
| L5 | `Cannot open <dir>/libskiko-linux-x64.so` | 镜像里没 jar，skiko 无法解压自身 | 放 `lib/libskiko-linux-x64.so` 或设 `skiko.library.path` |
| L6 | `[SKIKO] warn: Fallback to next API` + `Cannot create Linux GL context`，但帧率很高 | llvmpipe/virgl 被 skiko 黑名单 | 接受回退（软件渲染）或换真 GPU；验证 GL 路径用 `GALLIUM_DRIVER=softpipe` |
| L7 | `Could not load font` → 进程卡死 | 目标机无 fontconfig/CJK 字体 | 装字体，或把 ttf 打进镜像显式加载 |
| L8 | `no ComponentUI class for: ...showErrorDialog...` | 未注册 Swing，Compose 的错误弹窗自爆 | 自己 `runCatching` + 写日志，别依赖弹窗 |
| L9 | 编译能过，运行 `UnsupportedClassVersionError`（class file 65） | miuix 0.9.4 是 Java 21 字节码，用 JDK 20 跑 | 构建与运行都用 JDK 21+ |
| L10 | `classpath` 变成一条不存在的路径 / `ClassNotFoundException` | 用了 Windows 的 `;` 分隔符 | `:` 或 `path.separator` |

---

## 14. 和 Windows 篇共享的部分

这些**不需要**在 Linux 上重新踩：

- `java.home` 为 null 的修法（路径从 `bin\` 变 `lib/`，思路一样）；
- 渲染后端必须在 `application { }` **之前**设（skiko 第一次建 Surface 时读取）；
- classpath 走 `@argfile`；
- 资源要 `-H:IncludeResources`（ttf / json / `META-INF/services` / `tzdb.dat`）；
- `kotlinx-coroutines-swing` + `-H:+UseServiceLoaderFeature`：**实测镜像里含 `SwingDispatcherFactory`(×5) / `MainDispatcherFactory`(×8) / `kotlinx.coroutines.swing`(×18)**，`Dispatchers.Main` 可用；
- `--report-unsupported-elements-at-runtime`、`--initialize-at-build-time=kotlin.DeprecationLevel`；
- 窗口尺寸按屏幕可用区域收（Linux 上取 `Toolkit.getScreenSize()` × `sun.java2d.uiScale`，本篇未在缩放环境验证）。

## 15. 未验证清单（别当结论）

- 真 GPU（NVIDIA/AMD/Intel）下的 `OPENGL` 表现与帧率；
- Wayland（本篇全程 X11/Xvfb）、多显示器、`sun.java2d.uiScale` 分数缩放；
- 中文输入法（fcitx/ibus）在镜像里的可用性——`XTest`/`XIM` 相关类可能需要额外注册；
- ARM64、musl（Alpine）——`-H:NativeLinkerOption` 与 glibc 相关参数都要重测；
- 打 AppImage/deb 的分发（本篇只做了目录版）。
