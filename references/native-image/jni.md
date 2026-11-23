# 封闭世界 JNI：为什么窗口一开就崩

这是 native-image 打包 Compose Desktop 唯一真正难的部分。搞明白了，剩下的都是琐事。

---

## 1. 机制：`FindClass` 在镜像里查的是一张构建期定死的表

JVM 里 `FindClass("java/awt/Canvas")` 走的是 classloader 的运行时查找——classpath 里有就能找到。

native-image 里没有 classpath，也没有运行时加载器。类在**构建期**被静态分析决定「进不进镜像」（reachability）。`FindClass` 实现变成查一张写死的表：

```text
未注册 → 返回 NULL（并可能抛 ClassNotFoundException，取决于调用点是否检查）
已注册 → 返回镜像里的 Class 对象
```

而 `jni-config.json` 干的事是：**把类标记为 JNI 可达，并为它生成方法/字段的 ID 表**（因为 `GetMethodID` / `GetFieldID` 也要查静态表）。

所以「类没注册」的表现千奇百怪，取决于调用点有没有检查返回值：

| 库的写法 | 失败表现 |
| :-- | :-- |
| 检查了 null | `java.lang.ClassNotFoundException: java.awt.Canvas`，堆栈干净 |
| 没检查，直接用 | `NullPointerException`，堆栈指向一行无关的 Kotlin 代码 |
| `GetMethodID` 拿到 null | `NoSuchMethodError` |
| 在 native 侧解引用 | **直接段错误 / 进程秒退**，无任何 Java 堆栈 |

实测见过的两种：

```text
# AWT 事件循环（skiko 的 jawt 路径触发 WToolkit 初始化）
java.lang.NoClassDefFoundError: java/awt/Canvas
    at sun.awt.windows.WToolkit.eventLoop(Native Method)
    at sun.awt.windows.WToolkit.run(WToolkit.java:347)

# skiko 的库加载回调（这条 NPE 本身无害，但它是"类找不到"的告警烟雾）
java.lang.NullPointerException
    at org.jetbrains.skiko.Library._nAfterLoad(Native Method)
```

---

## 2. 两种 JNI 绑定：决定「要不要注册」

**先分清，否则会白注册一堆。**

### 2.1 静态绑定（`Java_pkg_Class_method` 命名导出）

```c
JNIEXPORT void JNICALL Java_demo_GlBridge_nCreate(JNIEnv *env, jobject self);
```

- dll 导出符号名里就带类名。调用时 JVM 按名字直接找符号，**不需要 jni-config**。
- **但类本身必须活在镜像里**（否则 `System.loadLibrary` 后调用会 `UnsatisfiedLinkError`，因为根本没有这个 Java 类去触发加载）。
- 结论：**你自己写的 JNI 桥，只要保证类被可达即可**；实践中仍然给它写一份 jni-config（1 条），成本为零、省掉一类玄学。

### 2.2 动态绑定（native 侧 `FindClass` + `GetMethodID`）

```c
jclass cls = (*env)->FindClass(env, "java/awt/Canvas");   // ← 这里返回 NULL
jmethodID m = (*env)->GetMethodID(env, cls, "<init>", "(Lsun/awt/...;)V");
```

- **这一类才是坑的来源**：类名是 dll 里的字符串，构建期分析看不到，必须扫出来手动登记。
- skiko（`skiko-windows-x64.dll`）和 AWT（`awt.dll`）大量使用这种模式：C++ 侧要回调进 Java（`Paragraph`、`Canvas`、`Surface`、事件分发）。

---

## 3. 四份 jni-config，各管一段

实测一个 Compose 桌面应用需要的配置：

| 文件 | 条目 | 内容 | 来源 |
| :-- | :-- | :-- | :-- |
| `jni-config.json` | 31 | `org.jetbrains.skia.*` 基础绑定 | skiko jar 自带的 reflect/jni 配置，或构建期 tracing agent 采集 |
| `jni-config-literals.json` | **52** | skiko dll 里的类字面量 | `scan-class-literals.py skiko-windows-x64.dll` |
| `jni-config-awt.json` | **146** | AWT dll 里的类字面量 | `scan-class-literals.py awt.dll` |
| `jni-config-glbridge.json` | 1 | 自己的 JNI 桥 | 手写 |

全部塞进一行：

```text
-H:JNIConfigurationFiles=native-image\jni-config.json,native-image\jni-config-literals.json,native-image\jni-config-glbridge.json,native-image\jni-config-awt.json
```

JSON 格式（数组，每项一个类）：

```json
[
  {
    "name": "java.awt.Canvas",
    "allDeclaredConstructors": true,
    "allDeclaredMethods": true,
    "allDeclaredFields": true
  }
]
```

字段语义：

- `name`：**点号分隔**的全限定名（不是 `java/awt/Canvas` 斜杠形式）。
- `allDeclaredConstructors/Methods/Fields`：为该类生成全部成员的 JNI ID。JNI 场合**三个都要开**——`GetFieldID` 用字段表，`GetMethodID` 用方法表，`NewObject` 用构造器表。
- `methods: [{name, parameterTypes}]`：需要精确控制体积时才用；排查阶段不要省。
- 内部类用 `$`：`java.awt.event.MouseEvent$1`。

---

## 4. 扫描器：怎么从 dll 里把类名捞出来

见 `scripts/native-image/scan-class-literals.py`。核心就一条正则：

```python
pat = rb'(?:java|javax|sun|com/sun|org/jetbrains)/[A-Za-z0-9_$]+(?:/[A-Za-z0-9_$]+)+'
```

**为什么必须这么写：**

| 约束 | 原因 |
| :-- | :-- |
| 必须 `至少两段` | dll 里存在大量单段字符串（`java`、`sun`、`Canvas`），会把噪声全捞进来 |
| 包根白名单 | 不白名单会把 PE 段名、错误信息里的路径（`com/sun/...` 反而要）都算进来；白名单同时保证漏扫概率低 |
| 允许 `$` | 内部类 |
| 两份 dll 分别扫 | skiko dll 与 awt dll 的字符串表互不相干，只扫一个会漏一半 |

**扫出来之后不要直接全信。** 有噪声是正常的（例如 `java/lang/String` 这种已经内建在镜像里的类，注册了也无害；但 `com/sun/java/swing/...` 里可能有一半根本用不到）。判定原则：

- **先全量注册跑通，再谈裁剪。** 排查阶段体积不重要。
- 裁剪时用二分：删一半 → 跑离屏自检 → 逐步收敛。**不要靠"看起来用不到"来猜。**

实测数字供对照：`awt.dll` 捞 146 条，`skiko-windows-x64.dll` 捞 52 条；两者合计 194 条（去重后），其中真正被运行期 `FindClass` 命中的不到一半，但**无法在构建期确定是哪些**。

---

## 5. 注册 AWT 之后的连锁反应

146 个 AWT 类进镜像后，构建期静态分析会把 `java.lang.System` 的静态初始化链展开，撞上两个「不可达」：

```text
Error: Detected an instance of the class java.lang.System being reached
       through java.lang.System.initSystemClassLoader / System.props
```

修法：`--report-unsupported-elements-at-runtime`。

```text
--report-unsupported-elements-at-runtime
```

- 含义：这类元素不报错，留到运行期真正访问时才抛。
- 实测：`initSystemClassLoader` 与 `props` 在 GUI 应用生命周期里**不会被访问**（AWT 在 native-image 下走的是另一条初始化路径），窗口/字体/中文/输入都正常。
- 反面做法：`--initialize-at-build-time=java.lang.System` 会引发一连串新的构建期初始化错误（`System.out` 为 null 之类），**不要**。

---

## 6. 判定「坑修好了没有」

不要靠「编译通过了」判断。按这个顺序：

```text
① 离屏自检里跑一次真实文本排版
   在 application {} 之前，构造 Paragraph + layout(320f)
   通过条件：不抛任何 Throwable，且 width/height > 0
   ← 这一条卡过 3 次，是最灵敏的探针

② 窗口放屏幕外，跑 3 秒，数帧
   通过条件：帧数 ≈ 60 × 秒数，且没有异常打印

③ 真开窗看一眼
   通过条件：标题栏、中文、颜色都对
```

`--selftest` 的九阶段实现见 `verification.md`。

---

## 7. 常见误判清单

| 误判 | 事实 |
| :-- | :-- |
| 「`jni-config.json` 里已经有 31 个 skia 类，JNI 配好了」 | skia 的绑定表 ≠ skiko 的字符串查找表，后者在 dll 里，需要单独扫 |
| 「编译成功 = JNI 没问题」 | JNI 配置缺失**完全不影响构建**，只在运行期炸 |
| 「用 `-H:+ReportExceptionStackTraces` 就能看到是哪个类」 | 能看到 Java 侧堆栈，但 `FindClass` 失败的类名不在堆栈里——它在 dll 的字符串表里，得扫 |
| 「加 `--enable-all-security-services` / `-H:+JNI` 能解决」 | `-H:+JNI` 在 21+ 默认就开，它不是「注册所有类」的意思 |
| 「把 `-Djava.awt.headless=false` 加在命令行就行」 | native-image 下 `-D` 不生效，需要 `-Djava.awt.headless=false` 作为**构建期**参数（它会写进镜像的默认属性），或在 `main()` 里 `System.setProperty` 且早于 AWT 初始化 |
