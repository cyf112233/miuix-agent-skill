#!/usr/bin/env python3
"""把「扫 .so/.dll 得到的 jni-config」与「tracing agent 跑出来的配置」合并成一份配置目录。

为什么需要两步（Linux 实测结论）：
    - 扫二进制只能抓到 `FindClass("java/awt/Canvas")` 这类**字符串**字面量；
    - AWT 里还有大量「直接把 jclass 当参数传进 native 方法」的写法（典型：
      sun.java2d.SurfaceData.initIDs → GetFieldID("pData")），二进制里没有类名字符串，
      扫描器永远扫不到，只有运行期 agent 追得到。
    两条腿走路：静态扫 + 动态追，缺一条就会崩在窗口初始化阶段。

用法：
    # 1) 先扫
    python scan-class-literals.py libskiko-linux-x64.so $JAVA_HOME/lib/libawt.so \
        $JAVA_HOME/lib/libawt_xawt.so -o native-image/jni-config.scan.json

    # 2) 再追（JVM 上跑一遍真实 UI）
    java -agentlib:native-image-agent=config-output-dir=ni-agent -cp <cp> MainKt

    # 3) 合并成 native-image 配置目录
    python merge-ni-config.py --scan native-image/jni-config.scan.json \
        --agent ni-agent --out native-image/config

    然后 native-image 加：-H:ConfigurationFileDirectories=native-image/config

Linux 专属处理：ensure java.lang.Thread 被完整注册（见 references/native-image/linux.md §7），
并在最后提醒补 thread_stub.o。
"""

import argparse
import json
import os
import shutil
import sys

AGENT_FILES = (
    "jni-config.json",
    "reflect-config.json",
    "resource-config.json",
    "proxy-config.json",
    "serialization-config.json",
)


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def merge_entries(base, extra):
    """按 name 合并，fields/methods 取并集；"allDeclared*" 优先级取或。"""
    by_name = {e["name"]: dict(e) for e in base}
    for e in extra:
        name = e.get("name")
        if not name:
            continue
        if name not in by_name:
            by_name[name] = dict(e)
            continue
        cur = by_name[name]
        for key in ("fields", "methods", "queriedFields", "queriedMethods"):
            if key in e:
                cur.setdefault(key, [])
                seen = {x.get("name") for x in cur[key]}
                for item in e[key]:
                    if item.get("name") not in seen:
                        cur[key].append(item)
                        seen.add(item.get("name"))
        for flag in ("allDeclaredConstructors", "allDeclaredMethods", "allDeclaredFields",
                     "allPublicConstructors", "allPublicMethods", "allPublicFields",
                     "allDeclaredClasses", "allPublicClasses"):
            if e.get(flag):
                cur[flag] = True
    return sorted(by_name.values(), key=lambda x: x["name"])


def ensure_thread_linux(entries):
    """AWT 的 XToolkit.waitForEvents 会 FindClass("java/lang/Thread")。

    - 不注册：运行期 NoClassDefFoundError（堆栈在 Native Method）
    - 只注册字段：能跑，但 Toolkit 线程每轮抛 IllegalMonitorStateException
    - 全注册：链接期缺 Java_java_lang_Thread_getNextThreadIdOffset / getStackTrace0，
      需要自带 stub（thread_stub.c，见 linux.md §7）
    这里选「全注册」，并在返回值里告诉调用方去补 stub。
    """
    entries = [e for e in entries if e.get("name") != "java.lang.Thread"]
    entries.append({
        "name": "java.lang.Thread",
        "allDeclaredConstructors": True,
        "allDeclaredMethods": True,
        "allDeclaredFields": True,
    })
    entries.sort(key=lambda x: x["name"])
    return entries


def main():
    ap = argparse.ArgumentParser(description="合并扫描结果与 tracing agent 配置")
    ap.add_argument("--scan", required=True, help="scan-class-literals.py 产出的 jni-config.json")
    ap.add_argument("--agent", default="ni-agent", help="agent 的 config-output-dir（默认 ni-agent）")
    ap.add_argument("--out", default="native-image/config", help="输出目录（默认 native-image/config）")
    ap.add_argument("--platform", default=None, choices=["linux", "windows", "macos"],
                    help="默认按当前系统判断")
    ap.add_argument("--no-thread-fix", action="store_true",
                    help="不做 java.lang.Thread 的 Linux 兜底处理")
    args = ap.parse_args()

    if args.platform:
        platform = args.platform
    elif os.name == "nt":
        platform = "windows"
    elif sys.platform.startswith("linux"):
        platform = "linux"
    else:
        platform = "macos"

    scan = load(args.scan)
    agent_jni_path = os.path.join(args.agent, "jni-config.json")
    agent_jni = load(agent_jni_path) if os.path.isfile(agent_jni_path) else []
    if not agent_jni:
        print("!! 没找到 %s —— 只合并扫描结果。强烈建议先跑一遍 agent：" % agent_jni_path,
              file=sys.stderr)
        print("   java -agentlib:native-image-agent=config-output-dir=%s -cp <cp> MainKt"
              % args.agent, file=sys.stderr)

    merged = merge_entries(scan, agent_jni)
    scan_n = len({e["name"] for e in scan})
    agent_n = len({e["name"] for e in agent_jni})
    print("扫描 %d 条 + agent %d 条 → 合并后 %d 条" % (scan_n, agent_n, len(merged)))

    need_stub = False
    if platform == "linux" and not args.no_thread_fix:
        before = len(merged)
        merged = ensure_thread_linux(merged)
        need_stub = True
        print("Linux 兜底：java.lang.Thread 已全量注册（条目数 %d → %d）" % (before, len(merged)))

    os.makedirs(args.out, exist_ok=True)
    with open(os.path.join(args.out, "jni-config.json"), "w", encoding="utf-8") as f:
        json.dump(merged, f, ensure_ascii=False, indent=2)
        f.write("\n")

    copied = []
    for name in AGENT_FILES:
        if name == "jni-config.json":
            continue
        src = os.path.join(args.agent, name)
        if os.path.isfile(src):
            shutil.copyfile(src, os.path.join(args.out, name))
            copied.append(name)

    print("写入 %s（jni-config.json + %s）" % (args.out, ", ".join(copied) or "无其他配置"))
    print("native-image 参数：-H:ConfigurationFileDirectories=%s" % args.out)
    if need_stub:
        print("\n还需要给 java.lang.Thread 的两个 VM-owned native 方法补符号（否则链接期报"
              " undefined reference）：")
        print("  gcc -c -fPIC -I$JAVA_HOME/include -I$JAVA_HOME/include/linux "
              "thread_stub.c -o thread_stub.o")
        print("  -H:NativeLinkerOption=/abs/path/thread_stub.o")
    return 0


if __name__ == "__main__":
    sys.exit(main())
