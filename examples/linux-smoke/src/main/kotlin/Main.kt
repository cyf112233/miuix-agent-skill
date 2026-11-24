import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import java.io.File

private const val DEFAULT_RENDER_API = "OPENGL"

private fun log(msg: String) {
    println(msg)
    runCatching { File("run.log").appendText(msg + "\n") }
}

private fun appDir(): File =
    ProcessHandle.current().info().command().orElse(null)
        ?.let { File(it).absoluteFile.parentFile } ?: File(".").absoluteFile

/** native 镜像里 java.home 是 null，skiko 要用它拼 jawt 路径 */

private fun diagAwt() {
    log("LIBPATH=" + System.getProperty("java.library.path") + " SKIKO_LIB=" + System.getProperty("skiko.library.path"))
    val dir = appDir()
    for (n in listOf("awt", "awt_xawt", "fontmanager", "lcms")) {
        val f = File(dir, "lib$n.so")
        runCatching { System.load(f.absolutePath); log("LOAD_FILE_OK $n") }
            .onFailure { log("LOAD_FILE_FAIL $n -> $it") }
    }
    try {
        java.awt.Toolkit.getDefaultToolkit()
        log("AWT_TOOLKIT_OK")
    } catch (t: Throwable) {
        log("AWT_TOOLKIT_FAIL " + t)
        var c: Throwable? = t
        var i = 0
        while (c != null && i < 6) { log("CAUSE[$i] " + c); c = c.cause; i++ }
        t.printStackTrace()
    }
}

private fun fixJavaHome() {
    if (System.getProperty("java.home") == null) {
        System.setProperty("java.home", appDir().absolutePath)
    }
}

private fun installCrashHandlers() {
    val handler = Thread.UncaughtExceptionHandler { t, e ->
        val sw = java.io.StringWriter()
        e.printStackTrace(java.io.PrintWriter(sw))
        log("=== CRASH thread:${t.name} ===\n" + sw.toString())
    }
    Thread.setDefaultUncaughtExceptionHandler(handler)
    runCatching {
        java.awt.EventQueue.invokeLater {
            Thread.currentThread().uncaughtExceptionHandler = handler
        }
    }
}

fun main(args: Array<String>) {
    fixJavaHome()
    if (args.contains("--diag")) diagAwt()
    installCrashHandlers()

    val nativeImage = System.getProperty("org.graalvm.nativeimage.imagecode") != null
    if (System.getProperty("skiko.renderApi") == null) {
        System.setProperty("skiko.renderApi", DEFAULT_RENDER_API)
    }
    args.firstOrNull { it.startsWith("-Dskiko.renderApi=") }
        ?.let { System.setProperty("skiko.renderApi", it.substringAfter('=')) }
    if (args.contains("--soft")) System.setProperty("skiko.renderApi", "SOFTWARE_FAST")

    val framesTarget = args.firstOrNull { it.startsWith("--frames=") }
        ?.substringAfter('=')?.toIntOrNull()
    val shotsDir = args.firstOrNull { it.startsWith("--shots=") }?.substringAfter('=')

    log("BOOT nativeImage=$nativeImage renderApi=${System.getProperty("skiko.renderApi")} " +
        "java=${System.getProperty("java.version")} home=${System.getProperty("java.home")} " +
        "os=${System.getProperty("os.name")} headless=${System.getProperty("java.awt.headless")}")

    application {
        val state = rememberWindowState(size = DpSize(520.dp, 760.dp))
        Window(onCloseRequest = ::exitApplication, title = "Miuix · Linux 冒烟", state = state) {
            var gpu by remember { mutableStateOf(true) }
            var frames by remember { mutableIntStateOf(0) }
            val t0 = remember { System.nanoTime() }
            val intervals = remember { ArrayList<Long>() }

            LaunchedEffect(Unit) {
                log("WINDOW_READY at=${(System.nanoTime() - t0) / 1_000_000}ms")
                if (framesTarget != null) {
                    var last = 0L
                    var f = 0
                    while (f < framesTarget) {
                        withFrameNanos { now ->
                            // 首帧的时钟原点与后续帧不同，且偶发 0 值；只保留合理区间
                            val d = if (last == 0L) 0 else (now - last) / 1_000_000
                            if (d in 1L..60_000L) intervals.add(d)
                            last = now
                            f++
                            frames = f
                        }
                    }
                    val ms = (System.nanoTime() - t0) / 1_000_000
                    val sorted = intervals.sorted()
                    fun p(q: Double) = if (sorted.isEmpty()) 0L else sorted[(sorted.size * q).toInt().coerceAtMost(sorted.size - 1)]
                    log("FRAMES_DONE n=$f total=${ms}ms fps=${"%.1f".format(f * 1000.0 / ms)} " +
                        "p50=${p(0.5)}ms p90=${p(0.9)}ms p99=${p(0.99)}ms")
                    log("GL_MAPPED=" + File("/proc/self/maps").readLines().any { it.contains("libGL") })
                    if (shotsDir != null) {
                        runCatching {
                            File(shotsDir).mkdirs()
                            val img = java.awt.Robot().createScreenCapture(
                                java.awt.Rectangle(java.awt.Toolkit.getDefaultToolkit().screenSize)
                            )
                            javax.imageio.ImageIO.write(img, "png", File(shotsDir, "shot.png"))
                            log("SHOT_OK")
                        }.onFailure { log("SHOT_FAIL " + it) }
                    }
                    exitApplication()
                }
            }

            MiuixTheme(colors = lightColorScheme()) {
                val scrollBehavior = MiuixScrollBehavior()
                Scaffold(topBar = { SmallTopAppBar(title = "渲染冒烟", scrollBehavior = scrollBehavior) }) { inner ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(top = inner.calculateTopPadding()),
                        overscrollEffect = null,
                    ) {
                        item { SmallTitle("运行环境") }
                        item {
                            Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                                SwitchPreference(
                                    title = "GPU 渲染",
                                    summary = "skiko.renderApi=" + System.getProperty("skiko.renderApi"),
                                    checked = gpu,
                                    onCheckedChange = { gpu = it },
                                )
                                HorizontalDivider(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                                SwitchPreference(
                                    title = "帧计数",
                                    summary = "$frames 帧",
                                    checked = false,
                                    onCheckedChange = {},
                                )
                                HorizontalDivider(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                                SwitchPreference(
                                    title = "中文渲染",
                                    summary = "澎湃 OS 风格 · 字重与行高",
                                    checked = true,
                                    onCheckedChange = {},
                                )
                            }
                        }
                        item { SmallTitle("长列表（滚动压力）") }
                        items(40) { i ->
                            Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                                Text(
                                    text = "第 ${i + 1} 项 · 用来看滚动时是否掉帧",
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
