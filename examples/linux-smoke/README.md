# linux-smoke：Linux 上的 Miuix + native-image 最小可复现样例

`references/native-image/linux.md` 里每个结论都来自这个工程。想验证「换了机器/换了版本还成不成立」，按下面顺序跑一遍即可。

内容：一个 520×760 的 Miuix 页面（`SmallTopAppBar` + `Card` + 3 个 `SwitchPreference` + 40 条卡片的 `LazyColumn`），
外加 `--frames=N`（帧计数后退出）、`--shots=DIR`（截屏）、`--soft`（软件渲染）、`--diag`（AWT 初始化探针）。

## 0. 前置

```bash
# JDK：GraalVM for JDK 21（自带 native-image 与 libawt*.so）
export JAVA_HOME=/opt/graalvm-jdk-21
$JAVA_HOME/bin/native-image --version

# 系统依赖
sudo apt-get install -y build-essential zlib1g-dev libx11-6 libxext6 libxrender1 \
     libxtst6 libxi6 libfontconfig1 libfreetype6 fonts-noto-cjk mesa-utils

# 无头环境用 Xvfb（有真桌面可跳过）
Xvfb :99 -screen 0 1600x1000x24 &
export DISPLAY=:99
```

## 1. 编译 + 导出 classpath

```bash
./gradlew printRuntimeCp          # → build/runtime-cp.txt（实测 9.5k 字符）
```

## 2. 扫原生库里的类字面量

```bash
python3 ../../scripts/native-image/scan-class-literals.py \
  native-image/libskiko-linux-x64.so \
  $JAVA_HOME/lib/libawt.so $JAVA_HOME/lib/libawt_xawt.so $JAVA_HOME/lib/libfontmanager.so \
  -o native-image/jni-config.scan.json
# 实测：48 + 46 + 54 + 13 → 去重 147 条
```

`libskiko-linux-x64.so` 从 skiko 的运行时 jar 里解出来：

```bash
unzip -o -j ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.skiko/\
skiko-awt-runtime-linux-x64/*/skiko-awt-runtime-linux-x64-*.jar libskiko-linux-x64.so -d native-image/
```

## 3. 跑 tracing agent（Linux 必需，别跳过）

```bash
java -agentlib:native-image-agent=config-output-dir=ni-agent \
  -cp "$(cat build/runtime-cp.txt):build/classes/kotlin/main:build/resources/main" \
  -Dskiko.renderApi=OPENGL MainKt --frames=60
```

它会记下扫描抓不到的那部分（典型：`sun.java2d.SurfaceData` 的 `pData` 字段）。

## 4. 合并配置

```bash
python3 ../../scripts/native-image/merge-ni-config.py \
  --scan native-image/jni-config.scan.json --agent ni-agent --out native-image/config
# 实测：146(扫，已剔除 Thread) + 111(agent) → 168，再补 java.lang.Thread → 169
```

## 5. 编译 stub（`java.lang.Thread` 的两个 VM-owned 符号）

```bash
gcc -c -fPIC -I$JAVA_HOME/include -I$JAVA_HOME/include/linux \
  native-image/stub/thread_stub.c -o native-image/stub/thread_stub.o
```

## 6. 生成 argfile 并构建

```bash
python3 ../../scripts/native-image/gen-args.py --proj . --main MainKt \
  --out build/native/miuix-linux-demo --link-obj native-image/stub/thread_stub.o

native-image @build/ni-args-console-miuix-linux-demo.txt
```

实测：4 分 50 秒 ~ 5 分 52 秒，峰值 RSS 5.5 GB，产物 106.8 MB + 8 个 `.so`。

## 7. 组装交付目录并运行

```bash
cd build/native
mkdir -p lib
cp ../../native-image/libskiko-linux-x64.so lib/
cp $JAVA_HOME/lib/libjawt.so lib/          # ← GraalVM 不产出它，缺了会段错误
cp ../../../run.sh .
./run.sh --frames=60 --shots=.
```

预期输出（`GALLIUM_DRIVER=softpipe` 时才是真 GL 路径）：

```text
WINDOW_READY at=0ms
FRAMES_DONE n=60 total=228ms fps=263.2 p50=2ms p90=4ms        # llvmpipe：被黑名单拒绝，回退软件渲染
FRAMES_DONE n=60 total=4185ms fps=14.3 p50=74ms               # softpipe：真 GL 3.3（CPU 光栅）
```

截图里应该是顶栏「渲染冒烟」+ 三行开关 + 40 张卡片，中文清晰无豆腐块。
