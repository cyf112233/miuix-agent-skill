#!/usr/bin/env python3
"""生成 GraalVM native-image 的 @argfile。

必须用 argfile，不能把参数写在命令行上：
Windows cmd.exe 单条命令上限 8191 字符，而 Compose + miuix + skiko 的运行时
classpath 实测 13.8k 字符。直接写会被静默截断，结果是「编译成功但 exe 只有
34 MB、里面没有 Compose、一跑就 NoClassDefFoundError」。

用法：
    # 控制台版（排查期用，能看到 stderr）
    python gen-args.py --proj . --main com.quiz.MainKt --out build/app

    # GUI 版（无黑框，最终交付）
    python gen-args.py --proj . --main com.quiz.MainKt --out build/app --gui

    # 指定 JNI 配置（默认自动扫描 native-image/*.json）
    python gen-args.py --proj . --main com.quiz.MainKt --out build/app --gui \
        --jni native-image/jni-config.json,native-image/jni-config-literals.json

然后：
    native-image.cmd @build/ni-args-gui-app.txt
"""

import argparse
import glob
import os
import sys


def build_full_cp(proj):
    """拼接镜像用的完整 classpath：业务 classes + 资源 + 运行时依赖。"""
    cp_file = os.path.join(proj, "build", "runtime-cp.txt")
    if not os.path.isfile(cp_file):
        sys.exit(
            "找不到 %s\n"
            "先跑：gradlew.bat printRuntimeCp（见 references/native-image/toolchain.md §3.3）" % cp_file
        )

    # 注意：strip() 很重要。文件末尾带换行时，Windows 的 for /f 读到空行会把
    # 变量覆盖成空，classpath 变成空，编出 34 MB 的空壳。
    runtime_cp = open(cp_file, encoding="utf-8").read().strip()
    if not runtime_cp:
        sys.exit("runtime-cp.txt 是空的：检查 printRuntimeCp 任务的输出")

    parts = [
        os.path.join(proj, "build", "classes", "kotlin", "jvm", "main"),
        os.path.join(proj, "build", "processedResources", "jvm", "main"),
    ]
    for p in parts:
        if not os.path.isdir(p):
            sys.exit(
                "缺少目录：%s\n"
                "先跑：gradlew.bat compileKotlinJvm jvmProcessResources" % p
            )
    parts.append(runtime_cp)
    return ";".join(parts)


def collect_jni(proj, explicit):
    if explicit:
        return [x.strip() for x in explicit.split(",") if x.strip()]
    found = sorted(glob.glob(os.path.join(proj, "native-image", "jni-config*.json")))
    return found


def main():
    ap = argparse.ArgumentParser(description="生成 native-image @argfile")
    ap.add_argument("--proj", default=".", help="项目根目录（含 build/ 与放 jni-config 的 native-image/）")
    ap.add_argument("--main", required=True, help="主类，如 com.quiz.MainKt")
    ap.add_argument("--out", required=True, help="输出 exe 路径（不带 .exe）")
    ap.add_argument("--gui", action="store_true", help="GUI 子系统（无控制台黑框）")
    ap.add_argument("--jni", default=None, help="逗号分隔的 jni-config 列表；默认自动扫描")
    ap.add_argument("--xmx", default="5g", help="构建期堆上限（默认 5g）")
    ap.add_argument("--extra", default=None, help="追加的额外参数（逗号分隔）")
    args = ap.parse_args()

    proj = os.path.abspath(args.proj)
    full_cp = build_full_cp(proj)
    jni_files = collect_jni(proj, args.jni)

    if not jni_files:
        print("!! 没有找到任何 jni-config*.json —— 运行期很可能崩在 FindClass/P6",
              file=sys.stderr)

    # 相对路径能让 argfile 在项目目录内移动，但 native-image 对相对路径的
    # 解释基准是「当前工作目录」，所以这里统一用相对 proj 的路径并在 bat 里 cd 到 proj。
    def rel(p):
        try:
            return os.path.relpath(p, proj)
        except ValueError:
            return p

    a = [
        "-J-Xmx" + args.xmx,
        "-cp", full_cp,
        args.main,
        "--no-fallback",
        "-H:+UnlockExperimentalVMOptions",
        # AWT 需要非 headless；native-image 下 -D 是构建期写入镜像默认属性
        "-Djava.awt.headless=false",
        "-H:+ReportExceptionStackTraces",
        # 注册 AWT 的 146 个 JNI 类后，构建期会连到 java.lang.System 的两个
        # 不可达元素（initSystemClassLoader / props）。它们运行期不会被访问，
        # 交给运行时再报，否则镜像编不出来。
        "--report-unsupported-elements-at-runtime",
        # Kotlin 注解枚举被构建期提前初始化，显式放行
        "--initialize-at-build-time=kotlin.DeprecationLevel",
        # 字体 / 题库 / 配置 / 图标。缺 ttf 的表现是界面文字全豆腐块或空白。
        r"-H:IncludeResources=.*\.(ttf|otf|properties|xml|png|jpg|json|txt|bin)$",
        # kotlinx-datetime / java.time 的时区数据，缺了取本地日期会抛
        r"-H:IncludeResources=tzdb\.dat$",
        r"-H:IncludeResources=.*\.res$",
        "--enable-url-protocols=http,https",
    ]

    if jni_files:
        a.append("-H:JNIConfigurationFiles=" + ",".join(rel(p) for p in jni_files))

    # GUI 参数必须是链接期选项，写成 -H:NativeLinkerOption=
    if args.gui:
        a += [
            "-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS",
            "-H:NativeLinkerOption=/ENTRY:mainCRTStartup",
        ]

    if args.extra:
        a += [x.strip() for x in args.extra.split(",") if x.strip()]

    a += ["-o", args.out]

    # argfile 名带上输出名，避免不同目标互相覆盖（也避免测试时冲掉正式构建配置）
    tag = os.path.basename(args.out).replace(".exe", "") or "image"
    name = "ni-args-%s-%s.txt" % ("gui" if args.gui else "console", tag)
    arg_file = os.path.join(proj, "build", name)
    os.makedirs(os.path.dirname(arg_file), exist_ok=True)
    with open(arg_file, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(a) + "\n")

    print("argfile  -> %s" % arg_file)
    print("classpath -> %d 字符%s" % (
        len(full_cp), "" if len(full_cp) < 7000 else "（超过 cmd 8191 上限，必须走 argfile）"))
    print("jni      -> %d 份：%s" % (len(jni_files), ", ".join(os.path.basename(p) for p in jni_files)))
    print("子系统   -> %s" % ("WINDOWS（无控制台）" if args.gui else "CONSOLE（有 stderr）"))
    print("\n构建：native-image.cmd @%s" % os.path.join("build", name))
    return 0


if __name__ == "__main__":
    sys.exit(main())
