#!/usr/bin/env python3
"""从原生 dll 里扫出 Java 类名字面量，生成 native-image 的 jni-config.json。

为什么需要：第三方原生库（skiko、awt）在 C/C++ 侧用 FindClass("java/awt/Canvas")
按字符串找类。native-image 是封闭世界，没登记过的类会返回 null，运行期直接崩，
而且堆栈停在 Native Method，看不出是谁干的。构建期完全不会报错。

用法：
    # 扫单个 dll
    python scan-class-literals.py skiko-windows-x64.dll -o jni-config-literals.json

    # 扫多个 dll 合并成一份（推荐，一次搞定 skiko + awt）
    python scan-class-literals.py skiko-windows-x64.dll awt.dll -o jni-config-runtime.json

    # 追加到已有文件（保留手工条目）
    python scan-class-literals.py awt.dll -o jni-config.json --merge

    # 只看统计不写文件
    python scan-class-literals.py skiko-windows-x64.dll --dry-run

实测数量（供对照，不是标准）：
    skiko-windows-x64.dll → 49 条
    awt.dll               → 146 条
"""

import argparse
import json
import os
import re
import sys

# 包根白名单。不白名单会把 PE 段名、错误路径、随机字符串全捞进来。
PKG_ROOTS = (
    "java", "javax", "jdk", "sun", "com/sun", "org/jetbrains",
    "gnu", "org/w3c", "org/xml", "org/ietf", "com/quiz",
)

# 至少两段（java/awt/Canvas 而不是 java），允许 $ 表示内部类。
# 用 bytes 正则直接在二进制上跑，比先 decode 再正则快得多，也不会被坏字节卡住。
PATTERN = re.compile(
    rb"(?:" + b"|".join(p.encode() for p in PKG_ROOTS) + rb")"
    rb"/[A-Za-z_$][A-Za-z0-9_$]*(?:/[A-Za-z_$][A-Za-z0-9_$]*)+"
)

# 这些路径出现在字符串里通常是资源定位符，不是 FindClass 目标。
# 注意：不要用「短小写段」这类启发式去过滤——`com/sun/...` 会被误杀，
# 而 com/sun 恰恰是 awt.dll 的主要目标之一。
BLOCK = re.compile(
    r"(?:^|/)(?:resources|META-INF|WEB-INF)(?:/|$)"
)


def scan_file(path):
    """返回该文件里的候选类名（斜杠形式）。"""
    with open(path, "rb") as f:
        data = f.read()
    hits = set()
    for m in PATTERN.finditer(data):
        raw = m.group().decode("ascii")
        # 去掉尾部可能粘连的垃圾（正则已经限制字符集，这里兜底）
        raw = raw.rstrip("/")
        if raw.count("/") < 1:
            continue
        if BLOCK.search(raw):
            continue
        hits.add(raw)
    return hits


def to_entry(slash_name, full=True):
    """斜杠形式 → jni-config 条目（点号形式，成员全开）。"""
    e = {"name": slash_name.replace("/", ".")}
    if full:
        e["allDeclaredConstructors"] = True
        e["allDeclaredMethods"] = True
        e["allDeclaredFields"] = True
    return e


def main():
    ap = argparse.ArgumentParser(
        description="扫 dll 里的 Java 类字面量，生成 native-image jni-config.json"
    )
    ap.add_argument("dlls", nargs="+", help="要扫的原生库（.dll / .so / .dylib）")
    ap.add_argument("-o", "--out", help="输出的 jni-config.json 路径")
    ap.add_argument("--merge", action="store_true",
                    help="与输出文件里已有的条目合并（按 name 去重）")
    ap.add_argument("--dry-run", action="store_true", help="只打印统计，不写文件")
    ap.add_argument("--filter", default=None,
                    help="只保留子串匹配的类名（如 java.awt）")
    ap.add_argument("--exclude", default=None,
                    help="排除子串匹配的类名（如 com.sun.java.swing）")
    ap.add_argument("--min-count", type=int, default=0,
                    help="每个 dll 至少扫到多少条才继续（低于视为扫错，退出码 2）")
    args = ap.parse_args()

    all_hits = {}
    total_raw = 0
    for dll in args.dlls:
        if not os.path.isfile(dll):
            print("!! 文件不存在: %s" % dll, file=sys.stderr)
            return 1
        hits = scan_file(dll)
        total_raw += len(hits)
        print("%-40s %4d 条" % (os.path.basename(dll), len(hits)))
        if len(hits) < args.min_count:
            print("!! %s 只扫到 %d 条（< min-count %d），可能扫错了文件"
                  % (os.path.basename(dll), len(hits), args.min_count), file=sys.stderr)
            return 2
        for h in hits:
            all_hits.setdefault(h, set()).add(os.path.basename(dll))

    # 过滤
    names = sorted(all_hits)
    if args.filter:
        names = [n for n in names if args.filter.replace(".", "/") in n]
    if args.exclude:
        ex = args.exclude.replace(".", "/")
        names = [n for n in names if ex not in n]

    print("-" * 56)
    print("合计（去重后）: %d 条" % len(names))

    # 按包统计，便于人工判断哪些是噪声
    buckets = {}
    for n in names:
        pkg = "/".join(n.split("/")[:2])
        buckets[pkg] = buckets.get(pkg, 0) + 1
    for pkg in sorted(buckets, key=lambda k: -buckets[k])[:12]:
        print("  %-40s %d" % (pkg, buckets[pkg]))

    if args.dry_run:
        print("\n(dry-run，未写文件)")
        for n in names:
            print("  " + n.replace("/", "."))
        return 0

    entries = [to_entry(n) for n in names]

    if args.merge and args.out and os.path.isfile(args.out):
        try:
            old = json.load(open(args.out, encoding="utf-8"))
        except Exception as e:
            print("!! 读已有文件失败（%s），改为覆盖" % e, file=sys.stderr)
            old = []
        seen = {e.get("name") for e in old}
        added = [e for e in entries if e["name"] not in seen]
        entries = old + added
        print("合并：已有 %d 条，新增 %d 条" % (len(old), len(added)))

    if not args.out:
        print("!! 没有 -o，未写文件（用 --dry-run 可以只看结果）", file=sys.stderr)
        return 1

    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(entries, f, ensure_ascii=False, indent=0)
        f.write("\n")
    print("\n写入 %s（%d 条）" % (args.out, len(entries)))
    print("别忘了加进 native-image 参数：")
    print("  -H:JNIConfigurationFiles=%s" % args.out.replace("/", "\\"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
