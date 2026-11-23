# 工具链：NIK full + MSVC + Gradle

本文只讲「让 `native-image.cmd` 能成功链接出一个 exe」所需的最小环境。实测版本组合见文末。

---

## 1. Liberica NIK：必须是 full

GraalVM 的发行版有好几种，做 Compose Desktop 只认一种：

| 发行版 | 带 AWT 原生库 | 能否出窗口 |
| :-- | :-- | :-- |
| GraalVM CE / Oracle GraalVM | 部分（`java.desktop` 模块在，但 AWT 原生 dll 缺 `jawt.dll`） | ❌ |
| Liberica NIK **standard** | ❌ | ❌（`NoClassDefFoundError: java/awt/Canvas`，且无 `jawt.dll` 可补） |
| Liberica NIK **full** | ✅ | ✅ |

**验证（一定要在开始前跑）：**

```bash
NIK=~/nik/bellsoft-liberica-vm-full-openjdk21-23.1.8
ls "$NIK/bin" | grep -Ei '^(awt|jawt|fontmanager|freetype|lcms|jsound|javajpeg)\.dll$'
```

期望输出（7 个都在）：

```text
awt.dll
freetype.dll
fontmanager.dll
javaaccessbridge.dll
javajpeg.dll
jawt.dll
jsound.dll
lcms.dll
```

少一个就换包。**不要**试图从别的 JDK 拷 `jawt.dll` 凑——它与 `jvm.dll` 的 ABI 绑定。

确认 NIK 版本：

```bash
"$NIK/bin/native-image.cmd" --version
# 期望：native-image 21.0.8 2025-xx-xx  (Liberica NIK 23.1.8)
```

---

## 2. MSVC：不要用 vcvars

`vcvars64.bat` 会调用 `vswhere.exe` / `reg.exe`。在受限账户、CI、沙箱、以及部分云教室终端上，这些会被拦截或找不到，报的错五花八门（`The system cannot find the path specified`、`vswhere not found`），让你误以为是 VS 没装好。

**直接设环境变量**，只依赖文件系统：

```bat
set "MSVC=D:\BuildTools\VC\Tools\MSVC\14.44.35207"
set "SDK=C:\Program Files (x86)\Windows Kits\10"
set "SDKVER=10.0.26100.0"
set "PATH=%MSVC%\bin\Hostx64\x64;%NIK_HOME%\bin;%PATH%"
set "INCLUDE=%MSVC%\include;%SDK%\Include\%SDKVER%\ucrt;%SDK%\Include\%SDKVER%\shared;%SDK%\Include\%SDKVER%\um;%SDK%\Include\%SDKVER%\winrt;%SDK%\Include\%SDKVER%\cppwinrt"
set "LIB=%MSVC%\lib\x64;%SDK%\Lib\%SDKVER%\ucrt\x64;%SDK%\Lib\%SDKVER%\um\x64"
```

要点：

- `SDKVER` 是 SDK 目录下的子目录名，自己 `dir` 一下实际值，**别照抄**。
- `Hostx64\x64`：宿主 x64、目标 x64。ARM64 机器上是 `Hostarm64\x64`。
- 安装体积：Build Tools 只勾「使用 C++ 的桌面开发」，约 3.4 GB。
- 无交互安装：`winget install Microsoft.VisualStudio.2022.BuildTools --override "--quiet --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended"`（需要一次 UAC）。

**5 行自检**（在同一个 cmd 里，设完变量后）：

```bat
where link.exe
where cl.exe
where native-image.cmd
echo #include ^<stdio.h^> > %TEMP%\t.c && echo int main(void){return 0;} >> %TEMP%\t.c && cl /nologo %TEMP%\t.c /Fe:%TEMP%\t.exe && %TEMP%\t.exe && echo MSVC_OK
```

`link.exe` 必须解析到 MSVC 的 `Hostx64\x64`，不能是 Git 自带的 `link`（那是 `ln` 的别名，会报莫名其妙的错）。

---

## 3. Gradle：镜像 + wrapper 预置

企业网/校园网里 `services.gradle.org` 常常超时。两个动作：

### 3.1 仓库镜像（`settings.gradle.kts`）

```kotlin
pluginManagement {
    repositories {
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        maven("https://maven.aliyun.com/repository/public")
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") // Compose 的 dev/beta 版
    }
}
```

> Compose `1.13.0-beta02` 这类 beta 版**只在 JetBrains 的 dev 仓库**，镜像里通常没有。把 `maven.pkg.jetbrains.space` 放在镜像之后作为兜底。

### 3.2 wrapper 手动预置

`gradlew` 首次运行要下载 `gradle-9.x-bin.zip`。下载不通时：

```bash
# 1) 拿到与 gradle-wrapper.properties 里 distributionUrl 完全一致的 zip
ls ~/.gradle/wrapper/dists/gradle-9.5.0-bin/<hash>/   # 已下载过的机器上直接拷
# 2) 放到目标机器对应目录（hash 由 distributionUrl 决定，先跑一次 gradlew 让它建目录）
```

目录形态必须是：

```text
~/.gradle/wrapper/dists/gradle-9.5.0-bin/<hash>/gradle-9.5.0/
~/.gradle/wrapper/dists/gradle-9.5.0-bin/<hash>/gradle-9.5.0-bin.zip
~/.gradle/wrapper/dists/gradle-9.5.0-bin/<hash>/gradle-9.5.0-bin.zip.ok   ← 少这个会重下
```

`.ok` 文件是关键：wrapper 只认这个标记。手工造一个空文件即可。

离线构建加 `--offline`，并确保 `GRADLE_USER_HOME` 指到有缓存的那个目录：

```bat
set "GRADLE_USER_HOME=D:\proj\build-work\.gradle-home"
gradlew.bat compileKotlinJvm jvmProcessResources --offline -q
```

### 3.3 两个必须跑的任务

```bash
gradlew.bat compileKotlinJvm jvmProcessResources printRuntimeCp
```

- `jvmProcessResources`：产出 `build/processedResources/jvm/main`，**compose 的字体资源在这里**（路径形如 `composeResources/<packageOfResClass>/files/font/xxx.ttf`）。只跑 `compileKotlinJvm` 的话资源目录是空的，运行时 `Res.font.*` 抛 `MissingResourceException`，界面空白。
- `printRuntimeCp`：把运行时 classpath 写到 `build/runtime-cp.txt`。它不是 native-image 提供的任务，要自己在 `build.gradle.kts` 里注册（下面的片段可直接抄）；`scripts/native-image/gen-args.py` 只是消费这个文件，并在缺失时明确报错：

```kotlin
tasks.register("printRuntimeCp") {
    val cp = kotlin.jvm().compilations.getByName("main").runtimeDependencyFiles
    val out = layout.buildDirectory.file("runtime-cp.txt")
    doLast {
        val f = out.get().asFile
        f.parentFile.mkdirs()
        // 注意：joinToString 不写结尾换行，否则 bat 里 for /f 读到空行
        f.writeText(cp!!.files.joinToString(File.pathSeparator) { it.absolutePath })
    }
}
```

> `runtime-cp.txt` **末尾不要换行**。Windows `for /f` 读最后一行空行会得到空字符串，`set /p` 更糟——会把变量设成空，导致 native-image 收到空 classpath，编出一个 34 MB 的「空壳 exe」。

---

## 4. 实测版本组合

| 组件 | 版本 |
| :-- | :-- |
| Liberica NIK | 23.1.8（JDK 21.0.8），full |
| MSVC Build Tools | 14.44.35207 |
| Windows SDK | 10.0.26100.0 |
| Gradle | 9.5.0 |
| Kotlin | 2.4.20 |
| Compose Multiplatform | 1.13.0-beta02 |
| miuix | 0.9.4 |
| skiko | 0.156.1 |
| Rust（JNI 逻辑层） | 1.100（`x86_64-pc-windows-msvc`，清华 rustup 镜像 + rsproxy crates） |
