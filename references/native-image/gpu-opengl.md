# GPU：OpenGL 后端、自建 WGL 上下文、离屏验证

---

## 1. 后端矩阵（Compose Desktop / skiko on Windows）

| `skiko.renderApi` | native-image | 依赖 | 适用 |
| :-- | :-- | :-- | :-- |
| `DIRECT3D` | ❌ 不可用 | D3D11 + 适配器枚举回调 | 不要用。JVM 下是 Windows 默认，native-image 下必坏 |
| `OPENGL` | ✅ | 系统 `opengl32.dll`（标准 C ABI） | **推荐默认** |
| `SOFTWARE_FAST` | ✅ | 无 | RDP / 虚拟显示 / 老显卡兜底 |
| `SOFTWARE_COMPAT` | ✅ | 无 | 只在怀疑光栅 bug 时对比 |

### 为什么 `DIRECT3D` 一定坏

skiko 的 D3D 后端在初始化时会**从 C++ 回调进 Java** 做适配器选择（`DirectXApi` / `InternalDirectXApi` 那套），这类回调依赖 `FindClass` 找到 `org.jetbrains.skiko.graphicapi.*` 并取方法 ID。它的失败是「静默降级」：不抛异常，直接认为不可用，然后落回软件渲染。

于是你看到的是「能跑、但 GPU 没生效」，日志里只有一条：

```text
java.lang.NullPointerException
    at org.jetbrains.skiko.Library._nAfterLoad(Native Method)
```

**这条 NPE 本身不致命**（是库加载后的探测），但它是「有东西没注册上」的信号。别花时间去修它——把后端切成 `OPENGL`，问题消失。

### 设置时机

必须在 **AWT/skiko 初始化之前**。放在 `main()` 第一行区域：

```kotlin
if (System.getProperty("org.graalvm.nativeimage.imagecode") != null &&
    System.getProperty("skiko.renderApi") == null) {
    System.setProperty("skiko.renderApi", "OPENGL")
}
```

`args` 手动解析兜底（native-image 下命令行 `-D` 不可靠）：

```kotlin
args.firstOrNull { it.startsWith("-Dskiko.renderApi=") }
    ?.let { System.setProperty("skiko.renderApi", it.substringAfter('=')) }
if (args.contains("--soft")) System.setProperty("skiko.renderApi", "SOFTWARE_FAST")
```

---

## 2. 为什么不能直接用 skiko 的离屏 GL 上下文

直觉做法：

```kotlin
val ctx = DirectContext.makeGL()          // 需要当前线程已有 current GL context
val surface = Surface.makeRenderTarget(ctx, true, imageInfo)
```

它要求**当前线程已经 `wglMakeCurrent` 过一个 GL 上下文**。skiko 提供了 `OpenGLOffscreenContext` 来做这件事（内部 `wglCreateContext`），但：

```text
java.lang.UnsatisfiedLinkError: 'long org.jetbrains.skiko.Redrawer$...'
```

**实测：这个原生函数在 skiko 0.156.1 的 `skiko-windows-x64.dll` 里根本没导出**，JVM 上跑也一样 `UnsatisfiedLinkError`。也就是说这不是 native-image 的问题，是上游没编进去（`OpenGLOffscreenContext` 只在部分平台/macOS 编译）。

自己验证一下（30 秒）：

```bat
dumpbin /exports skiko-windows-x64.dll | findstr /i "opengl offscreen"
:: 输出为空 = 没导出
```

所以想在不弹窗的前提下证明 GPU 可用，只有两条路：

1. 开一个真窗口，让它渲染，然后截图 —— 违背「不打扰用户」。
2. **自己写一个 JNI 桥建 GL 上下文**，只用于离屏。

---

## 3. 自建 WGL 上下文（`glctx.c`）

思路：`CreateWindowEx(WS_POPUP)` 建一个 16×16 的窗口，**从不 `ShowWindow`**（所以用户完全看不到），在其上 `ChoosePixelFormat` + `wglCreateContext` + `wglMakeCurrent`，然后交给 Skia。

```c
#include <jni.h>
#include <windows.h>
#include <GL/gl.h>

typedef struct { HWND hwnd; HDC hdc; HGLRC rc; } GlCtx;

JNIEXPORT jlong JNICALL Java_demo_GlBridge_nCreate(JNIEnv *env, jobject self) {
    WNDCLASSA wc; ZeroMemory(&wc, sizeof(wc));
    wc.lpfnWndProc = DefWindowProcA;
    wc.hInstance = GetModuleHandleA(NULL);
    wc.lpszClassName = "quizapp_glbridge";
    RegisterClassA(&wc);

    HWND hwnd = CreateWindowExA(0, "quizapp_glbridge", "quizapp_glbridge", WS_POPUP,
                                0, 0, 16, 16, NULL, NULL, wc.hInstance, NULL);
    if (!hwnd) return 0;

    HDC hdc = GetDC(hwnd);
    PIXELFORMATDESCRIPTOR pfd; ZeroMemory(&pfd, sizeof(pfd));
    pfd.nSize = sizeof(pfd); pfd.nVersion = 1;
    pfd.dwFlags = PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL | PFD_DOUBLEBUFFER;
    pfd.iPixelType = PFD_TYPE_RGBA;
    pfd.cColorBits = 32; pfd.cDepthBits = 24;

    int pf = ChoosePixelFormat(hdc, &pfd);
    if (!pf || !SetPixelFormat(hdc, pf, &pfd)) { ReleaseDC(hwnd, hdc); DestroyWindow(hwnd); return 0; }

    HGLRC rc = wglCreateContext(hdc);
    if (!rc) { ReleaseDC(hwnd, hdc); DestroyWindow(hwnd); return 0; }
    if (!wglMakeCurrent(hdc, rc)) { wglDeleteContext(rc); ReleaseDC(hwnd, hdc); DestroyWindow(hwnd); return 0; }

    GlCtx *ctx = (GlCtx *)malloc(sizeof(GlCtx));
    if (!ctx) return 0;
    ctx->hwnd = hwnd; ctx->hdc = hdc; ctx->rc = rc;
    return (jlong)(intptr_t)ctx;
}

/* which: 0=GL_VENDOR 1=GL_RENDERER 2=GL_VERSION 3=GL_SHADING_LANGUAGE_VERSION */
JNIEXPORT jstring JNICALL Java_demo_GlBridge_nInfo(JNIEnv *env, jobject self, jint which) {
    GLenum e = GL_VENDOR;
    if (which == 1) e = GL_RENDERER;
    else if (which == 2) e = GL_VERSION;
    else if (which == 3) e = 0x8B8C;
    const char *s = (const char *)glGetString(e);
    return (*env)->NewStringUTF(env, s ? s : "(null)");
}

JNIEXPORT void JNICALL Java_demo_GlBridge_nDestroy(JNIEnv *env, jobject self, jlong p) {
    GlCtx *ctx = (GlCtx *)(intptr_t)p;
    if (!ctx) return;
    wglMakeCurrent(NULL, NULL);
    if (ctx->rc) wglDeleteContext(ctx->rc);
    if (ctx->hdc) ReleaseDC(ctx->hwnd, ctx->hdc);
    if (ctx->hwnd) DestroyWindow(ctx->hwnd);
    free(ctx);
}
```

编译：

```bat
cl /nologo /LD /O2 glctx.c /Fe:glctx.dll /link opengl32.lib user32.lib gdi32.lib /INCLUDE:Java_demo_GlBridge_nCreate
```

> `/INCLUDE:` 是必要的：`/OPT:REF` 会把「没有 Java 侧引用」的导出符号裁掉，裁了之后 `System.loadLibrary` 成功但方法找不到。加 `/INCLUDE` 保证导出保留。

Java 侧：

```kotlin
object GlBridge {
    init { System.loadLibrary("glctx") }      // 放在 exe 同级或 PATH 里
    external fun nCreate(): Long
    external fun nInfo(which: Int): String
    external fun nDestroy(p: Long)
}
```

**关键约束**：`DirectContext.makeGL()` 必须与 `nCreate()` **在同一线程**调用（GL 上下文是线程绑定的）。写成：

```kotlin
val handle = GlBridge.nCreate()
check(handle != 0L) { "wglCreateContext failed" }
println("GL_VERSION = ${GlBridge.nInfo(2)}")   // 期望 >= 2.0
val ctx = DirectContext.makeGL()               // 此刻当前线程已有 current context
// ... Surface.makeRenderTarget / canvas / readPixels ...
ctx.close(); GlBridge.nDestroy(handle)
```

`nCreate` 里已经 `wglMakeCurrent` 了，所以不要额外调 skiko 的 `makeCurrent`。

---

## 4. 离屏验证：要断言什么

一个可靠的 `--gpu` 自检至少断言四件事：

| 断言 | 判据 | 失败含义 |
| :-- | :-- | :-- |
| 上下文建立 | `handle != 0` | 显卡/驱动/会话问题（RDP 常见） |
| GL 版本 | `GL_VERSION` 主版本 ≥ 2.0 | RDP 下常见 1.1 → 直接判定「不可用，走软件渲染」 |
| Skia GL 后端可用 | `DirectContext.makeGL()` 不抛 | skiko 的 GL 绑定没编进来 |
| 像素真读回来 | `readPixels` 返回的字节数 == w×h×4，且非全 0 | 只建了上下文但没真渲染 |

实测输出：

```text
GL_VENDOR   = Intel
GL_RENDERER = Intel(R) UHD Graphics 630
GL_VERSION  = 4.6 (Build 27.20.100.xxxx)
GLSL        = 4.60
surface 256x256 renderTarget OK
readPixels 262144 bytes, non-zero = 100%
gpu-proof.png written (256x256)
GPU_RENDER_OK
```

把 `gpu-proof.png` 存到 exe 同级，是可以给人看的物证。

---

## 5. 窗口路径 vs 离屏路径

两条路互不影响，别混着判断：

| | 离屏（`--gpu`） | 窗口（真开窗） |
| :-- | :-- | :-- |
| 谁建 GL 上下文 | 你的 `glctx.dll` | skiko（经 JAWT / `wglCreateContext`） |
| 依赖 `jawt.dll` | ❌ 不需要 | ✅ 需要，且依赖 `java.home` 正确 |
| 依赖 AWT JNI 注册 | ❌ 不需要 | ✅ 需要（146 类） |
| 能证明什么 | GPU 与驱动真的能跑 Skia | 整条 UI 链路（事件→组合→绘制→swapBuffers） |

**所以「离屏 GPU 通过」不等于「窗口能开」。** 反过来的顺序也对：先修 AWT JNI 让窗口能开，再谈 GPU。

---

## 6. 远程桌面 / 云桌面

RDP 会话里 GL 通常只有 **1.1**（Microsoft 的软件实现 `GDI Generic`）：

```text
GL_VERSION = 1.1.0
GL_RENDERER = GDI Generic
```

Skia 的 GL 后端要求 ≥ 2.0，`DirectContext.makeGL()` 会失败或渲染出空白。这不是 bug，是 RDP 的固有限制（除非开 GPU 虚拟化 + RemoteFX/AVC444，且客户端支持）。

处理方式：

1. 交付两个启动器：`启动.bat`（GPU）/ `启动-软件渲染.bat`（`--soft`）。
2. README 用一句话给判据：**直连/HDMI 投屏用 GPU；远程桌面、云终端、虚拟机用软件渲染。**
3. 软件渲染的帧率其实不差（见下表），1080p 的静态列表页几乎看不出区别。

---

## 7. 实测数据（1280×720，Intel UHD 630，60 Hz）

| 场景 | 后端 | 3 秒帧数 | 折算 fps |
| :-- | :-- | :-- | :-- |
| 窗口渲染（屏幕外位置） | OPENGL | 179 | 59.7 |
| 窗口渲染（屏幕外位置） | SOFTWARE_FAST | 194 | 64.7 |

软件渲染帧数略高是正常的：它不受 vsync 限制。**这不代表软件渲染更流畅**——真实体感由帧间隔的稳定性决定，而 GPU 路径的 p99 更平（见 `verification.md` 的统计方法）。
