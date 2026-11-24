#!/usr/bin/env bash
# native 产物启动脚本。三件事缺一不可：
#   1) LD_LIBRARY_PATH 含 exe 目录（libjawt.so 依赖同目录的 libawt.so / libjava.so）
#   2) LD_LIBRARY_PATH 含 lib/（libskiko-linux-x64.so，skiko 靠 java.home/lib 找它）
#   3) 有 DISPLAY（X11 / Xvfb）
set -e
cd "$(dirname "$0")"
export LD_LIBRARY_PATH="$(pwd):$(pwd)/lib:${LD_LIBRARY_PATH:-}"
exec ./miuix-linux-demo "$@"
