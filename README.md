# miuix-agent-skill

给 AI 编程 agent 用的 **Miuix** 技能包：让 agent 按统一规范产出 Miuix（小米 HyperOS · Compose Multiplatform）界面，而不是凭记忆瞎写 API；界面写完之后，同一份 skill 还教它把桌面应用打成单文件 exe。

**两条主线，按需读其中一条：**

| 主线 | 回答的问题 | 入口 |
| :-- | :-- | :-- |
| 界面怎么长 | Scaffold 骨架、间距栅格、Preference 分组、配色、空/错/权限态、自适应 | `SKILL.md` §1–§7 + `references/` |
| 怎么交付成 exe | GraalVM native-image 打包、JNI 注册、OpenGL 渲染、启动/帧率验证、排障 | `SKILL.md` §8 + `references/native-image.md` |

前者对 Android / iOS / Desktop / Wasm 通用；后者只对 **Desktop target**（Windows x64 实测）有意义。两条主线互不依赖，做界面不必读打包章。

## 结构

```text
miuix-agent-skill/
├── SKILL.md                     # 唯一入口：铁律、场景路由、工作流、自检、工具、§8 打包
├── references/
│   ├── scenario-*.md            # 8 个通用场景的结构（主框架/首页/列表/信息/表单/状态/自适应/引导）
│   ├── components*.md  theme.md  layout.md  preferences.md  navigation.md
│   ├── icons.md  effects.md  recipes.md  status-colors.md  setup.md
│   ├── pitfalls.md              # 界面侧反模式与迁移表
│   ├── showcase/                # 11 个编译通过的样张源码
│   ├── native-image.md          # 打包章总纲（原 miuix-native-image skill）
│   └── native-image/            # 打包细节：toolchain / jni / gpu-opengl / pitfalls / verification
└── scripts/
    ├── miuix-lint.mjs           # 界面 lint（E2/E3 + W1–W9）
    └── native-image/            # scan-class-literals.py / gen-args.py / ni-env.bat
```

## 样张：编译过的界面源码

`references/showcase/` 是 11 个**对 miuix 0.9.4 编译通过**的完整页面（首页、设置、列表、关于、表单、空态、引导、状态卡三态、深浅色主题），
`miuix-lint` 跑出 0 error / 0 warning。文档里的结构、间距、颜色，都以这份源码为准——不贴截图，直接看代码。

## 安装

通用（[vercel-labs/skills](https://github.com/vercel-labs/skills)，支持 Claude Code、Codex、Cursor、OpenCode 等 80+ agent）：

```bash
# 项目级
npx skills add cyf112233/miuix-agent-skill

# 全局级
npx skills add cyf112233/miuix-agent-skill -g
```

DeepSeek Harness 全局（该 CLI 的全局目录 DSH 不读，DSH 的全局根是 `~/.dsh/skills`）：

```bash
git clone https://github.com/cyf112233/miuix-agent-skill.git ~/.dsh/skills/miuix-ui
```

装好后直接提需求即可，例如「用 miuix 写一个带分组开关的设置页」，或「把这个 Compose 应用打成单文件 exe」。

## 内容

**界面（`SKILL.md` §1–§7）**：8 个通用场景——主框架/多 Tab、首页仪表盘、通用列表、信息展示、表单编辑、状态页、自适应（大屏/折叠屏/RTL）、引导与权限——加设置页，每个都给可抄的完整结构。另含组件签名与默认值、主题配色、**状态卡的红/黄/绿**（Miuix 没有 success/warning token，这里给出四个真实项目的取值与规则）、`miuix-nav` 导航、图标、旧版 API 迁移表、反模式清单，以及一个零依赖 lint：

```bash
node scripts/miuix-lint.mjs <你的项目目录>
```

**打包（`SKILL.md` §8 + `references/native-image.md`）**：Compose Desktop + Miuix + skiko → 单个 150 MB exe、冷启动 300 ms 级、稳态 60 fps。含三个硬条件（Liberica NIK **full** / MSVC / 显式环境变量）、五步流水线、**封闭世界 JNI 的四类注册**（skiko 52 + AWT 146 + 自建桥 + skia 31）、`java.home` 为 null 导致的 `Cannot open (null)\bin\jawt.dll`、`@argfile` 绕开 cmd 8191 字符上限、`DIRECT3D` 静默降级、自建 WGL 上下文做离屏 GPU 验证、`--selftest` 九阶段与帧率量化，以及 P1–P20 坑表：

```bash
python scripts/native-image/scan-class-literals.py skiko-windows-x64.dll -o native-image/jni-config-literals.json
python scripts/native-image/gen-args.py --proj . --main com.example.MainKt --out build/app --gui
```

界面侧的规则标注了来源强度（库默认值 / 多数派惯例 / 单项目偏好 / 已被证伪），**单项目偏好不当规范**。

## 边界

**界面**

- **符号、必填参数、枚举值、声称的默认值**：机器校验（206 个片段，974 个函数的 API 模型）
- **内置样张**：编译通过、渲染通过、lint 0 error / 0 warning
- **仍需真机**：insets、手势、动画、RTL、字体缩放（样张只在 Desktop target 编译，没有真机验证）

**打包**

- **只验证了 Windows x64**。Linux/macOS 的 native-image + AWT/skiko 是另一套坑，参数不能直接照抄
- **`DIRECT3D` 没解决**，只给了绕开方案（`OPENGL`）；skiko 缺 `OpenGLOffscreenContext` 原生导出是上游问题，这里给的是自建桥
- 版本组合（NIK 23.1.8 / Compose 1.13.0-beta02 / miuix 0.9.4 / skiko 0.156.1）是实测值，升级后请重跑 `--selftest`

## 本次变更（合并 `miuix-native-image`）

原独立 skill `miuix-native-image` 并入本仓库，作为「界面写完之后」的一章：

| 变更 | 说明 |
| :-- | :-- |
| 新增 `references/native-image.md` | 原 skill 的 `SKILL.md` 正文，去掉独立 frontmatter，编号改为 §1–§10，路径改为章内相对路径 |
| 新增 `references/native-image/` | 原 5 个参考文件，内容未改，仅统一脚本路径 |
| 新增 `scripts/native-image/` | 原 3 个脚本，内容未改 |
| 修改 `SKILL.md` | `description` 增加打包触发词；新增 §8 打包章（只做路由与高频坑，不复述细节）；参考索引与自检清单各加一项 |
| 未改动 | 原有 `references/`、`showcase/`、`miuix-lint.mjs`、`image/` 全部未动 |

合并原则：**一个 skill 一个入口，深度内容走 references 渐进加载**。若拆成两个 skill，`description` 里的触发词会互相竞争，且「界面已经写完、只差打包」这种上下文在两者之间会丢；放进同一 skill 后，§8 只占 ~50 行，做界面时不必读。

## 许可

[Apache-2.0](LICENSE)，署名见 [NOTICE](NOTICE)；再分发需保留这两份并声明修改。

约定来自 Miuix 官方 example 与文档，以及 [InstallerX-Revived](https://github.com/wxxsfxyzm/InstallerX-Revived)、[lyricon](https://github.com/proify/lyricon)、[Updater-KMP](https://github.com/YuKongA/Updater-KMP)、[HyperStar](https://github.com/YunZiA/HyperStar)、[XiaomiHelper](https://github.com/HowieHChen/XiaomiHelper)、[OShin](https://github.com/suqi8/OShin)、[StatusBarLyric](https://github.com/Block-Network/StatusBarLyric)、[Payload-Dumper-Compose](https://github.com/rcmiku/Payload-Dumper-Compose) 的公开源码；与这些项目及小米公司无隶属关系。
