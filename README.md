# miuix-agent-skill

给 AI 编程 agent 用的 **Miuix** 界面构建技能包。装进 agent 的 skills 目录后，它会按统一的布局、配色与组件规范产出 Miuix（小米 HyperOS 设计规范 · Compose Multiplatform）界面，而不是凭记忆瞎写 API。

```
用户：用 miuix 写一个设置页
agent：→ 读 SKILL.md 选定「设置页」场景 → 按 preferences.md §1 的标准结构产出
```

## 它解决什么

让 AI 写出**观感统一**的 Miuix 代码，而不是「能跑但不像原生」的代码：

- **按 8 个通用场景组织**：主框架/多 Tab、首页仪表盘、通用列表、信息展示、表单编辑、状态页、自适应（大屏/折叠屏/RTL）、引导与权限；设置页单独成篇。
- **数值是共识不是臆造**：卡片外边距 12.dp、卡片内 16.dp、分组标题 28.dp（= 12 + 16，与卡片文字严格对齐）等，并**标注每条规则的来源等级**（库默认值 / 多数派惯例 / 单项目偏好 / 已被证伪）。
- **把坑写进文档**：`Overlay*` 必须宿主在 `Scaffold` 内、图标是扩展属性必须逐个 import、`Icon` 在有色容器里必须显式给 `tint`（`FloatingActionButton` 不传递内容色）、旧版 `extra.Super*` 到 0.9.x 的迁移表等。
- **附带 lint**：零依赖脚本，扫 Material 残留、硬编码颜色、缺 `scrollBehavior`、图标缺 import 等，并给出 `文件:行` 与修法。

## 安装

用 [vercel-labs/skills](https://github.com/vercel-labs/skills) 的通用 CLI（支持 Claude Code、Codex、Cursor、OpenCode 等 80+ agent）：

```bash
# 装到当前项目
npx skills add cyf112233/miuix-agent-skill

# 装到全局
npx skills add cyf112233/miuix-agent-skill -g

# 只装到指定 agent
npx skills add cyf112233/miuix-agent-skill -a claude-code -a cursor -a opencode

# 先看看仓库里有什么，不安装
npx skills add cyf112233/miuix-agent-skill --list
```

装完用 `npx skills list` 查看、`npx skills update` 升级、`npx skills remove miuix-ui` 卸载。
不想安装、只想临时用一次：

```bash
npx skills use cyf112233/miuix-agent-skill --skill miuix-ui | claude
```

<details>
<summary>如果目标 agent 不在 CLI 的列表里</summary>

skill 只是一个目录约定：`<agent 的 skills 根>/miuix-ui/SKILL.md`。用 git 放进对应目录即可，例如
`git clone https://github.com/cyf112233/miuix-agent-skill.git ~/.dsh/skills/miuix-ui`。
常见根：`.agents/skills/`、`~/.agents/skills/`、`~/.claude/skills/`、`~/.dsh/skills/`。

</details>

## 结构

```
SKILL.md                       入口：14 条铁律、骨架形状、来源等级、8 场景路由表、自检清单
references/
  scenarios.md                 ★ 场景总纲：决策树、共享骨架、间距栅格、import 基线、状态层契约
  scenario-{shell,home,list,info,form,state,adaptive,onboarding}.md
  preferences.md               设置页体系（规则最硬，单独成篇）
  components.md                基础组件签名 + 默认值一览
  components-overlay.md        overlay.* / window.* 两族、下拉数据结构、选型对照
  layout.md / theme.md / setup.md / navigation.md / icons.md / effects.md
  recipes.md                   跨场景横向配方（弹窗、抽屉、动作菜单、Material 迁移映射）
  pitfalls.md                  反模式、编译错误速查、旧版 API 迁移表
scripts/miuix-lint.mjs         零依赖静态检查
```

## 用法

装好后不需要特殊命令，agent 会根据任务自动加载（`SKILL.md` 的 `description` 里写了触发词：`miuix`、`MiuixTheme`、`HyperOS UI`、`小米风格`、`写个设置页` …）。

手工让 agent 加载时，直接说：

> 用 miuix 写一个带分组开关的设置页，顶部有大标题折叠

想让它先自检，可以要求跑 lint：

```bash
node scripts/miuix-lint.mjs <你的项目目录>
```

检查项：`M1` Material 残留 / `E2` 已移除的旧 miuix API / `E3` 图标缺 import（编译错误）/ `W1` 硬编码颜色 / `W2` 顶栏缺 `scrollBehavior` / `W3` 列表缺滚动修饰符 / `W4` 弹层缺 Scaffold 宿主（跨文件识别共享宿主）/ `W5`–`W7` 间距与 insets。
只有 `E2`/`E3` 是 error。所有检查都可用 `// miuix-lint:allow` 或 `// miuix-lint:allow-<检查项>` 豁免。

## 已知边界（请如实看待）

本仓库的准确性经过了**对抗式自查**，边界如下：

| 已机器校验 | 未校验 |
| :-- | :-- |
| 组件/图标/颜色/文本样式/`Defaults` 成员是否存在 | 代码能否真的编译（未经真实工程编译验证） |
| 具名参数名、**逐重载必填参数**、枚举值 | 作用域误用（`RowScope`/`LazyItemScope`）、`@OptIn` 要求、类型不匹配 |
| 表格里**声称的默认值**是否等于库源码 | 运行时观感（是否真的对齐、手势是否可用） |

校验脚本不随仓库分发（它依赖本地克隆的库源码），校验结果与已知问题记录在开发笔记中。

另外，规则来源已按强度分级。**标为「单项目偏好」的写法只是某个项目的选择，不应当作规范**；`SKILL.md` §2 有完整的来源等级表与「已被证伪的说法」清单。前期版本确实把单项目偏好写成了「8 个项目共识」，已更正。

## 来源与致谢

约定与数值来自对以下公开项目源码的阅读与统计，以及 Miuix 官方文档与官方 example：

- [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)（库本体 · 官方 example · 官方文档，Apache-2.0）
- [InstallerX-Revived](https://github.com/wxxsfxyzm/InstallerX-Revived) · [lyricon](https://github.com/proify/lyricon) · [Updater-KMP](https://github.com/YuKongA/Updater-KMP) · [HyperStar](https://github.com/YunZiA/HyperStar) · [XiaomiHelper](https://github.com/HowieHChen/XiaomiHelper) · [OShin](https://github.com/suqi8/OShin) · [StatusBarLyric](https://github.com/Block-Network/StatusBarLyric) · [Payload-Dumper-Compose](https://github.com/rcmiku/Payload-Dumper-Compose)

这些都是独立项目，各自保留其原有许可证；本仓库只从中归纳约定与度量，代码示例是**重新编写的示例**而非拷贝。本仓库与上述任何项目及小米公司均无隶属或背书关系。

## License

[Apache License 2.0](LICENSE)。

**再分发或发布衍生作品时，必须保留 `LICENSE` 与 `NOTICE`（含作者署名），并声明你修改过哪些内容** —— 见 LICENSE 第 4 条。内部使用不受署名约束（任何开源协议都无法强制这一点）。

```
Copyright 2026 cyf112233 <frank13967973981@163.com>
```
