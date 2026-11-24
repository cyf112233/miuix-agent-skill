#!/usr/bin/env python3
"""生成 GraalVM native-image 的 @argfile（Windows / Linux / macOS）。

必须用 argfile，不能把参数写在命令行上：
Windows cmd.exe 单条命令上限 8191 字符，而 Compose + miuix + skiko 的运行时
classpath 实测 13.8k 字符（Linux 实测 9.5k）。直接写会被静默截断，结果是「编译成功但
exe 只有 34 MB、里面没有 Compose、一跑就 NoClassDefFoundError」。

用法：
    # 控制台版（排查期用，能看到 stderr）
    python gen-args.py --proj . --main com.quiz.MainKt --out build/app

    # Windows GUI 版（无黑框，最终交付）
    python gen-args.py --proj . --main com.quiz.MainKt --out build/app --gui

    # 指定 JNI 配置（默认用 native-image/config/ 目录，其次自动扫 native-image/jni-config*.json）
    python gen-args.py --proj . --main com.quiz.MainKt --out build/app \
        --config-dir native-image/config

    # Linux：补 java.lang.Thread 的两个 VM-owned 符号（见 references/native-image/linux.md §7）
    python gen-args.py --proj . --main com.quiz.MainKt --out build/app \
        --link-obj native-image/stub/thread_stub.o

然后：
    native-image.cmd @build/ni-args-gui-app.txt      # Windows
    native-image @build/ni-args-console-app.txt      # Linux / macOS

平台差异（本脚本自动处理）：
    - classpath 分隔符：Windows ';'，Linux/macOS ':'
    - GUI 子系统：只有 Windows 有（/SUBSYSTEM:WINDOWS + /ENTRY），其它平台忽略 --gui
    - 产物名：Windows 习惯不带后缀（native-image 自动补 .exe），Linux 直接给文件名
"""

import argparse
import glob
import os
import sys


def detect_target(explicit):
    if explicit and explicit != "auto":
        return explicit
    if os.name == "nt":
        return "windows"
    if sys.platform.startswith("linux"):
        return "linux"
    return "macos"


def build_full_cp(proj, sep):
    """拼接镜像用的完整 classpath：业务 classes + 资源 + 运行时依赖。"""
    cp_file = os.path.join(proj, "build", "runtime-cp.txt")
    if not os.path.isfile(cp_file):
        sys.exit(
            "找不到 %s\n"
            "先跑：gradlew printRuntimeCp（见 references/native-image/toolchain.md §3.3）" % cp_file
        )

    # 注意：strip() 很重要。文件末尾带换行时，Windows 的 for /f 读到空行会把
    # 变量覆盖成空，classpath 变成空，编出 34 MB 的空壳。
    runtime_cp = open(cp_file, encoding="utf-8").read().strip()
    if not runtime_cp:
        sys.exit("runtime-cp.txt 是空的：检查 printRuntimeCp 任务的输出")

    # Windows 版 Gradle 脚本常写死 ';'，在 Linux 上会变成「一条不存在的路径」。
    # 这里直接改写并告警：比让用户在 ClassNotFoundException 里绕圈便宜。
    if sep == ":" and runtime_cp.count(";") >= 3 and ":" not in runtime_cp:
        runtime_cp = runtime_cp.replace(";", ":")
        print("!! runtime-cp.txt 用的是 Windows 分隔符 ';'，已自动转成 ':'。"
              "根治办法：printRuntimeCp 里改用 System.getProperty(\"path.separator\")",
              file=sys.stderr)

    parts = [
        os.path.join(proj, "build", "classes", "kotlin", "jvm", "main"),   # KMP: jvmMain
        os.path.join(proj, "build", "classes", "kotlin", "main"),           # 纯 kotlin("jvm")
        os.path.join(proj, "build", "resources", "main"),
        os.path.join(proj, "build", "processedResources", "jvm", "main"),
    ]
    existing = [p for p in parts if os.path.isdir(p)]
    if not existing:
        sys.exit(
            "缺少 classes 目录（build/classes/kotlin/jvm/main 或 build/resources/main）\n"
            "先跑：gradlew compileKotlinJvm jvmProcessResources"
        )
    return sep.join(existing + [runtime_cp])


def collect_jni(proj, explicit):
    if explicit:
        return [x.strip() for x in explicit.split(",") if x.strip()]
    return sorted(glob.glob(os.path.join(proj, "native-image", "jni-config*.json")))


def main():
    ap = argparse.ArgumentParser(description="生成 native-image @argfile")
    ap.add_argument("--proj", default=".", help="项目根目录（含 build/ 与放 jni-config 的 native-image/）")
    ap.add_argument("--main", required=True, help="主类，如 com.quiz.MainKt")
    ap.add_argument("--out", required=True, help="输出路径（Windows 不带 .exe）")
    ap.add_argument("--gui", action="store_true", help="Windows：GUI 子系统（无控制台黑框）；其它平台忽略")
    ap.add_argument("--target", default="auto", choices=["auto", "windows", "linux", "macos"],
                    help="目标平台（默认按当前系统）")
    ap.add_argument("--jni", default=None, help="逗号分隔的 jni-config 列表；默认自动扫描")
    ap.add_argument("--config-dir", default=None,
                    help="native-image 配置目录（含 jni/reflect/resource-config.json），"
                         "默认用 native-image/config（存在时）")
    ap.add_argument("--link-obj", action="append", default=[],
                    help="额外链接的 .o/.obj（可重复），如 Linux 的 thread_stub.o")
    ap.add_argument("--xmx", default="5g", help="构建期堆上限（默认 5g）")
    ap.add_argument("--extra", default=None, help="追加的额外参数（逗号分隔）")
    args = ap.parse_args()

    target = detect_target(args.target)
    sep = ";" if target == "windows" else ":"
    proj = os.path.abspath(args.proj)
    full_cp = build_full_cp(proj, sep)

    def rel(p):
        try:
            return os.path.relpath(p, proj)
        except ValueError:
            return p

    config_dir = args.config_dir
    if config_dir is None:
        candidate = os.path.join(proj, "native-image", "config")
        if os.path.isdir(candidate):
            config_dir = candidate
    jni_files = [] if config_dir else collect_jni(proj, args.jni)

    if not jni_files and not config_dir:
        print("!! 没有找到任何 jni-config —— 运行期很可能崩在 FindClass"
              "（Windows P6 / Linux L1）", file=sys.stderr)

    a = [
        "-J-Xmx" + args.xmx,
        "-cp", full_cp,
        args.main,
        "--no-fallback",
        "-H:+UnlockExperimentalVMOptions",
        # AWT 需要非 headless；native-image 下 -D 是构建期写入镜像默认属性
        "-Djava.awt.headless=false",
        "-H:+ReportExceptionStackTraces",
        # 注册 AWT 的 JNI 类后，构建期会连到 java.lang.System 的两个不可达元素
        # （initSystemClassLoader / props）。它们运行期不会被访问，交给运行时再报。
        "--report-unsupported-elements-at-runtime",
        # Kotlin 注解枚举被构建期提前初始化，显式放行
        "--initialize-at-build-time=kotlin.DeprecationLevel",
        # 桌面 Dispatchers.Main 靠 META-INF/services 发现（kotlinx-coroutines-swing）
        "-H:+UseServiceLoaderFeature",
        # 字体 / 题库 / 配置 / 图标。缺 ttf 的表现：Windows 豆腐块，Linux 直接 Could not load font
        r"-H:IncludeResources=.*\.(ttf|otf|properties|xml|png|jpg|json|txt|bin)$",
        r"-H:IncludeResources=META-INF/services/.*$",
        # kotlinx-datetime / java.time 的时区数据
        r"-H:IncludeResources=tzdb\.dat$",
        r"-H:IncludeResources=.*\.res$",
        "--enable-url-protocols=http,https",
    ]

    if config_dir:
        a.append("-H:ConfigurationFileDirectories=" + rel(config_dir))
    elif jni_files:
        a.append("-H:JNIConfigurationFiles=" + ",".join(rel(p) for p in jni_files))

    for obj in args.link_obj:
        a.append("-H:NativeLinkerOption=" + os.path.abspath(obj))

    # GUI 参数是 Windows 链接期选项
    if args.gui:
        if target == "windows":
            a += [
                "-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS",
                "-H:NativeLinkerOption=/ENTRY:mainCRTStartup",
            ]
        else:
            print("note: --gui 只对 Windows 有意义，%s 上忽略（控制台程序即可）" % target)

    if args.extra:
        a += [x.strip() for x in args.extra.split(",") if x.strip()]

    a += ["-o", args.out]

    tag = os.path.basename(args.out).replace(".exe", "") or "image"
    name = "ni-args-%s-%s.txt" % ("gui" if args.gui and target == "windows" else "console", tag)
    arg_file = os.path.join(proj, "build", name)
    os.makedirs(os.path.dirname(arg_file), exist_ok=True)
    with open(arg_file, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(a) + "\n")

    print("target   -> %s（path separator %r）" % (target, sep))
    print("argfile  -> %s" % arg_file)
    print("classpath -> %d 字符%s" % (
        len(full_cp),
        "" if (target != "windows" or len(full_cp) < 7000) else "（超过 cmd 8191 上限，必须走 argfile）"))
    if config_dir:
        print("config   -> %s（ConfigurationFileDirectories）" % rel(config_dir))
    else:
        print("jni      -> %d 份：%s" % (len(jni_files),
                                         ", ".join(os.path.basename(p) for p in jni_files)))
    print("linker   -> %s" % (", ".join(os.path.basename(o) for o in args.link_obj) or "无额外 .o"))
    print("\n构建：native-image%s @%s" % (".cmd" if target == "windows" else "",
                                          os.path.join("build", name)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
