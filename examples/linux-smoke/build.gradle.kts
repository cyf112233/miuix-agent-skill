// Linux 冒烟样例：Compose Desktop 1.12.2 + miuix 0.9.4 + GraalVM native-image（21）
// 版本组合是实测能跑通的那一套，换版本请重跑 --frames 自检。
plugins {
    kotlin("jvm") version "2.4.20"
    id("org.jetbrains.compose") version "1.12.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

kotlin {
    jvmToolchain(21)   // miuix 0.9.4 是 Java 21 字节码，运行也必须 21+
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.4")
    // 桌面端 Dispatchers.Main 实现；缺了它 native-image 里任何 scope.launch 都会崩
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
}

// native-image 需要运行期 classpath 清单（gen-args.py 消费这个文件）
tasks.register("printRuntimeCp") {
    dependsOn("compileKotlin", "processResources")
    doLast {
        // 分隔符必须跟平台走：Linux/macOS 是 ':'，Windows 是 ';'
        val sep = System.getProperty("path.separator")
        val cp = configurations.runtimeClasspath.get().files.joinToString(sep) { it.absolutePath }
        val f = layout.buildDirectory.file("runtime-cp.txt").get().asFile
        f.parentFile.mkdirs()
        f.writeText(cp.trim())
        println("runtime-cp -> ${f.absolutePath} (${cp.length} chars, sep=$sep)")
    }
}
