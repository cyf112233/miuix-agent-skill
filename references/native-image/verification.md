# 不弹窗的验证

原则：**能用离屏证明的，绝不开窗**；必须开窗的那一次，要有明确的判定标准，不要"看起来没问题"。

---

## 1. `--selftest`：九个阶段，不进 `application {}`

关键设计：自检**不启动 Compose 的 `application {}`**，直接在 `main()` 里同步跑完，任何阶段失败立即写日志并返回退出码。这样即使 UI 层整体崩了，你也能知道崩在哪一层。

```kotlin
private fun log(msg: String) {
    File(appDir(), "selftest.log").appendText(msg + "\n")
}

fun selfTest(): Int {
    val logFile = File(appDir(), "selftest.log")
    logFile.delete()

    // 1 逻辑层初始化（JNI → Rust / 业务入口）
    step(1, "core init") { NativeCore.ensureInit() }

    // 2 字体资源（compose resources 真的进镜像了吗）
    step(2, "font bytes") {
        val bytes = Res.readBytes("files/font/misans_regular.ttf")
        check(bytes.size > 100_000) { "font too small: ${bytes.size}" }
    }

    // 3 题库解析（业务数据 + 序列化）
    step(3, "questions parse") { check(NativeCore.total() > 0) }

    // 4 字体注册进 Skia 字体管理器
    step(4, "skia typeface") {
        val tf = Typeface.makeFromData(Data.makeFromBytes(fontBytes))
        check(tf.familyName != null)
    }

    // 5 文本排版 —— 最灵敏的探针（P5 就在这里暴露）
    step(5, "paragraph layout") {
        val p = ParagraphBuilder(ParagraphStyle()).apply {
            pushStyle(TextStyle().setFontSize(24f).setTypeface(tf))
            addText("中文字形测试 ABC 123")
        }.build()
        p.layout(320f)
        check(p.height > 0f && p.longestLine > 0f)
    }

    // 6 光栅绘制 + 像素回读（CPU 路径）
    step(6, "raster surface") {
        val surface = Surface.makeRasterN32Premul(200, 120)
        surface.canvas.clear(0xFF3366CC.toInt())
        surface.canvas.drawString("刷题通", 10f, 60f, Font(tf, 28f), Paint())
        val px = surface.makeImageSnapshot().readPixels() ?: error("readPixels null")
        check(px.size == 200 * 120 * 4)
    }

    // 7 图片编解码（javajpeg/lcms 链）
    step(7, "image decode") { /* 用一张内嵌 png 走一遍 Image.makeFromEncoded */ }

    // 8 数据目录可写（绿色交付的前提）
    step(8, "data dir writable") {
        val d = File(appDir(), "data").apply { mkdirs() }
        File(d, ".probe").writeText("ok").also { it.delete() }
    }

    // 9 退出清理
    step(9, "cleanup") { /* 关 NativeCore、释放句柄 */ }
    return 0
}
```

每步包装（失败也写日志，不抛到顶层）：

```kotlin
private fun step(no: Int, name: String, body: () -> Unit) {
    try {
        body()
        log("[OK]   $no $name")
    } catch (t: Throwable) {
        log("[FAIL] $no $name\n${t.stackTraceToString()}")
        throw t
    }
}
```

**为什么第 5 步最重要**：一次 `Paragraph.layout()` 会穿过「字体管理器 → 文本整形（ICU）→ skia 绑定 → skiko 的 Java 回调」，P5 类的 JNI 缺失只能在这一步暴露。实测它卡过 3 次。

---

## 2. 帧率：`withFrameNanos` 采样帧间隔

不要用「数 3 秒内画面变了多少次」这种粗法。Compose 的 `MonotonicFrameClock` 能给出**每一帧提交的时刻**，用它算间隔分布。

```kotlin
@Composable
internal fun FrameProbe(label: String, warmupSec: Int, measureSec: Int, onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        val warmup = warmupSec * 1_000_000_000L           // 跳过首帧/字体栅格化/shader 编译
        val total  = (warmupSec + measureSec) * 1_000_000_000L
        val stamps = ArrayList<Long>(4096)
        val begin = System.nanoTime()

        while (true) {
            withFrameNanos {
                val now = System.nanoTime()
                if (now - begin > warmup) stamps.add(now)
            }
            if (System.nanoTime() - begin > total) break
        }
        // ... 统计（见下）
        onDone()
    }
}
```

**注意**：`withFrameNanos` 自己会请求下一帧，所以在界面上跑它等于「持续重绘」。这测的是**渲染管线吞吐上限**，不是「静止时的 CPU 占用」。要测真实负载，另配一个每帧改状态的滚动页（见 §3）。

统计（这才是能说明问题的部分）：

```kotlin
val n = stamps.size
val span = (stamps[n - 1] - stamps[0]) / 1e9
val avgFps = (n - 1) / span

val deltas = ArrayList<Double>(n - 1)
for (i in 1 until n) deltas.add((stamps[i] - stamps[i - 1]) / 1e6)   // ms
val sorted = deltas.sorted()
fun pct(p: Double) = sorted[((sorted.size - 1) * p).toInt().coerceIn(0, sorted.size - 1)]

val p50 = pct(0.50); val p90 = pct(0.90); val p99 = pct(0.99)
val hitches = deltas.count { it > p50 * 2.0 }    // 60Hz 下阈值 ≈ 33ms，即掉到 30fps
```

输出与判读：

```text
帧数 = 361
时长 = 6.02 s
平均 fps = 60.0
帧间隔 p50 = 16.67 ms   (理论 60.00 fps)
帧间隔 p90 = 16.71 ms
帧间隔 p99 = 17.02 ms
帧间隔 max = 34.10 ms
卡顿帧(>2×p50) = 2   (0.55%)
```

| 指标 | 好 | 需要注意 | 不可接受 |
| :-- | :-- | :-- | :-- |
| p50 | ≈ 1000/刷新率 | 1.2–1.5 倍 | > 2 倍 |
| p99 | < 1.2 × p50 | < 2 × p50 | > 2 × p50 |
| 卡顿帧占比 | < 1% | < 5% | > 5% |

**为什么不用平均 fps 单独下结论**：软件渲染在三秒计数里能跑出 64.7 fps（比 GPU 的 59.7 还高），因为它不受 vsync 限制。但它的 p99 更差，滚动时能感觉到顿。**帧间隔分布比平均值有用一个数量级。**

---

## 3. 真实负载：滚动压测

空转的 60 fps 没有意义。要复现「刷题时上下滑列表」的负载：

```kotlin
@Composable
internal fun ScrollBench() {
    val st = rememberLazyListState()
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
            st.scrollBy(6f)                          // 每帧 6px，60Hz 下约 360px/s
            if (st.firstVisibleItemIndex > 960) st.scrollToItem(0)
        }
    }
    LazyColumn(state = st, /* 1000 项，卡片 + 中文文本 */) { items(1000) { ... } }
}
```

要点：

- **必须让滚动跟帧绑定**（在 `withFrameNanos` 之后 `scrollBy`），否则测的是「定时器频率」不是「渲染能力」。
- 每项要有真实内容（背景、圆角、文本），空 `Box` 不触发文本绘制。
- 1000 项足够让 `LazyColumn` 的回收逻辑工作起来。

---

## 4. 窗口渲染计数（不打扰用户）

窗口放屏幕外，跑固定时长，数提交的帧数：

```kotlin
val state = rememberWindowState(position = WindowPosition(-32000.dp, -32000.dp))
```

实测（3 秒）：

| 后端 | 帧数 | 说明 |
| :-- | :-- | :-- |
| OPENGL | 179 | ≈ 59.7 fps，受 vsync 限制 |
| SOFTWARE_FAST | 194 | ≈ 64.7 fps，不受 vsync 限制 |

**局限**：这只能证明「渲染管线在出帧」，**证明不了 `swapBuffers` 到屏幕那一步**。而且窗口在屏幕外时 DWM 可能降频，数据偏保守——保守方向是安全的。

---

## 5. 启动耗时与内存

PowerShell（不需要管理员）：

```powershell
$exe = "D:\app\cdj.exe"
Get-Process cdj -ErrorAction SilentlyContinue | Stop-Process -Force
for ($i=1; $i -le 3; $i++) {
  $sw = [Diagnostics.Stopwatch]::StartNew()
  $p = Start-Process $exe -PassThru
  $win = -1
  while ($sw.ElapsedMilliseconds -lt 15000) {
    $p.Refresh()
    if ($p.MainWindowHandle -ne 0) { $win = $sw.ElapsedMilliseconds; break }
    Start-Sleep -Milliseconds 10
  }
  $m1 = [math]::Round($p.WorkingSet64/1MB)
  Start-Sleep -Seconds 3
  $p.Refresh()
  $m2 = [math]::Round($p.WorkingSet64/1MB)
  "run$i : win=${win}ms ws=$m1/$m2 MB priv=$([math]::Round($p.PrivateMemorySize64/1MB))MB"
  Stop-Process -Id $p.Id -Force
  Start-Sleep -Milliseconds 800
}
```

实测（150 MB 的真实应用）：

```text
run1 : win=448ms ws=199/199 MB priv=129MB     ← 首启最慢（文件缓存冷）
run2 : win=297ms ws=199/199 MB priv=129MB
run3 : win=326ms ws=199/199 MB priv=129MB
```

**测三次取最小**是合理的：用户的实际感受接近热启动。

---

## 6. 辅助工具（沙箱环境下的替代方案）

很多时候 `PowerShell Add-Type`（加载 `System.Drawing` / `System.Windows.Forms`）会被安全策略拦，截图和窗口操作都做不了。用 MSVC 自己写，三个几十行的 C 文件就够：

| 工具 | 作用 | 关键 API |
| :-- | :-- | :-- |
| `screencap.exe out.bmp` | 全屏截图 | `GetDC(NULL)` + `BitBlt` + 手写 BMP 头 |
| `winctl.exe <pid> <x> <y> <w> <h>` | 按 pid 找到顶层窗口并摆位置/改尺寸 | `EnumWindows` + `GetWindowThreadProcessId` + `SetWindowPos` |
| `click.exe x y [x y ...]` | 模拟点击（验证交互链路） | `SetCursorPos` + `mouse_event` |

`winctl` 的存在价值：高 DPI 下窗口可能跑到屏幕外（P14），而窗口在屏幕外时截图看不到内容。用 `winctl` 把它拽回 (0,0) 再截图，就能看清界面。

**编译（一次配好环境变量后）：**

```bat
cl /nologo /O2 screencap.c /Fe:screencap.exe /link user32.lib gdi32.lib
```

> BMP 头要自己写：`BITMAPFILEHEADER`(14) + `BITMAPINFOHEADER`(40)，注意**行是自底向上**且每行按 4 字节对齐。写错的表现是图片上下颠倒或花屏。

---

## 7. 截图判读（人眼替代不了，但可以辅助）

让多模态模型看截图时，问**具体**问题，不要问「正常吗」：

```text
「这是 Windows 应用截图。请回答：
 1) 窗口标题是什么？
 2) 界面上所有可见的中文文字，逐条列出。
 3) 是否有豆腐块（方框）或乱码？
 4) 底部导航栏有几项，分别是什么？」
```

能明确回答出「刷题通 / 开始刷题 / 共 300 题 / 首页 题库 错题 统计 设置」这种细节，才算真的渲染正确。回答含糊（"看起来是一个应用界面"）= 图不对或太小。

---

## 8. 验证清单（交付前逐条打勾）

- [ ] `--selftest` 九阶段全 `[OK]`，`selftest.log` 无 `[FAIL]`
- [ ] 第 5 步（paragraph layout）通过 —— 这是 JNI 的最终判据
- [ ] `--gpu` 打印 `GL_VERSION ≥ 2.0` 且 `GPU_RENDER_OK`，`gpu-proof.png` 能打开
- [ ] 屏幕外窗口 3 秒帧数 ≈ 60×3
- [ ] 启动 < 600 ms（三次取最小）
- [ ] 真开窗看过一次：标题、中文、无豆腐块、交互可点
- [ ] `--soft` 路径也能起（RDP 兜底）
- [ ] 干净机器 + 任意工作目录跑过一次
- [ ] 关掉程序无残留进程
