# miuix-agent-skill

给 AI 编程 agent 用的 **Miuix** 界面构建技能包：让 agent 按统一的布局、配色、组件规范产出 Miuix（小米 HyperOS 设计规范 · Compose Multiplatform）界面，而不是凭记忆瞎写 API。

## 效果

以下界面由 **DeepSeek Harness** + **DeepSeek v4.1 Flash（high 思考强度）** 配合本 skill 生成，未做手工布局调整：

<table>
  <tr>
    <td><img src="image/01.jpg" width="220" alt=""></td>
    <td><img src="image/02.jpg" width="220" alt=""></td>
    <td><img src="image/03.jpg" width="220" alt=""></td>
  </tr>
  <tr>
    <td><img src="image/04.jpg" width="220" alt=""></td>
    <td><img src="image/05.jpg" width="220" alt=""></td>
    <td><img src="image/06.jpg" width="220" alt=""></td>
  </tr>
</table>

## 安装

用 [vercel-labs/skills](https://github.com/vercel-labs/skills) 的通用 CLI（支持 Claude Code、Codex、Cursor、OpenCode 等 80+ agent）：

```bash
npx skills add cyf112233/miuix-agent-skill            # 装到当前项目
npx skills add cyf112233/miuix-agent-skill -g         # 装到全局
npx skills add cyf112233/miuix-agent-skill --list     # 只看有什么，不安装
npx skills use cyf112233/miuix-agent-skill --skill miuix-ui | claude   # 不装，临时用一次
```

其它：`npx skills list` 查看、`npx skills update` 升级、`npx skills remove miuix-ui` 卸载。

<details>
<summary>DeepSeek Harness（DSH）用户注意</summary>

- **项目级可用**：CLI 默认装到 `./.agents/skills/`，而 `.agents/skills` 是 DSH 的发现根之一，装完即可用。
- **全局级要手动**：CLI 的 `-g` 落在 `~/.config/agents/skills/`，**DSH 不读这个路径**。改用 `git clone` 到 DSH 的全局根：

  ```bash
  git clone https://github.com/cyf112233/miuix-agent-skill.git ~/.dsh/skills/miuix-ui
  ```

- 兜底事实：skill 就是目录约定 `<skills 根>/miuix-ui/SKILL.md`，任何 agent 都可以这样放。

</details>

## 它做什么

- **8 个通用场景，每个都给可抄的完整结构**：主框架/多 Tab、首页仪表盘、通用列表、信息展示、表单编辑、状态页、自适应（大屏/折叠屏/RTL）、引导与权限；设置页规则最硬，单独成篇。
- **数值有出处，并且标了强度**：卡片外边距 12.dp、卡片内 16.dp、分组标题 28.dp（= 12 + 16，与卡片文字严格对齐）等；每条规则标注是「库默认值 / 多数派惯例 / 单项目偏好 / 已被证伪」，**不把单项目偏好当规范**。
- **把踩过的坑写进文档**：`Overlay*` 必须宿主在 `Scaffold` 内、图标是扩展属性必须逐个 import、有色容器里 `Icon` 必须显式给 `tint`（`FloatingActionButton` 不传递内容色）、`extra.Super*` → 0.9.x 的迁移表、`BasicComponent` 的布局不能改写成 `Row`。
- **附带 lint**：零依赖脚本，扫 Material 残留 / 已移除 API / 图标缺 import（编译错误）/ 硬编码颜色 / 顶栏缺 `scrollBehavior` / 弹层缺宿主，输出 `文件:行` + 修法。

## 结构

```
SKILL.md                     入口：14 条铁律、骨架形状、来源等级、8 场景路由表、交付前自检清单
references/
  scenarios.md               ★ 场景总纲：决策树、共享骨架、间距栅格、import 基线、状态层契约
  scenario-{shell,home,list,info,form,state,adaptive,onboarding}.md
  preferences.md             设置页体系（规则最硬）
  components.md              基础组件签名 + 默认值一览
  components-overlay.md      overlay.* / window.* 两族、下拉数据结构、选型对照
  layout.md theme.md setup.md navigation.md icons.md effects.md
  recipes.md                 跨场景横向配方（弹窗、抽屉、动作菜单、Material 迁移映射）
  pitfalls.md                反模式、编译错误速查、旧版 API 迁移表
scripts/miuix-lint.mjs       零依赖静态检查
```

## 用法

装好后 agent 会按任务自动加载（触发词写在 `SKILL.md` 的 `description` 里：`miuix`、`MiuixTheme`、`HyperOS UI`、`小米风格`、`写个设置页` …）。直接提需求即可：

> 用 miuix 写一个带分组开关的设置页，顶部要大标题折叠

需要它自检时，让它跑 lint：

```bash
node scripts/miuix-lint.mjs <你的项目目录>
```

检查项：`M1` Material 残留 · `E2` 已移除的旧 API · `E3` 图标缺 import · `W1` 硬编码颜色 · `W2` 顶栏缺 `scrollBehavior` · `W3` 列表缺滚动修饰符 · `W4` 弹层缺 Scaffold 宿主（跨文件识别共享宿主）· `W5`–`W7` 间距与 insets。
**只有 `E2`/`E3` 是 error**，其余是风格建议；任意检查都可用 `// miuix-lint:allow` 或 `// miuix-lint:allow-<检查项>` 豁免。

## 已知边界

准确性经过对抗式自查，边界如下：

| 已机器校验 | 未校验 |
| :-- | :-- |
| 组件/图标/颜色/文本样式/`Defaults` 成员是否存在 | **代码能否真的编译**（未经真实工程编译验证） |
| 具名参数、逐重载必填参数、枚举值 | 作用域误用（`RowScope`/`LazyItemScope`）、`@OptIn` 要求、类型不匹配 |
| 表格里声称的默认值是否等于库源码 | 运行时观感（是否真的对齐、手势是否可用） |

也就是说：**符号层可信，能编译与好看仍需你在真实工程里验证。** 校验脚本依赖本地克隆的库源码，不随仓库分发。

前期版本曾把单项目偏好写成「8 个项目共识」，现已更正并在 `SKILL.md` §2 附上「已被证伪的说法」清单。

## 来源与许可

约定与数值来自 Miuix 官方文档 / 官方 example，以及对以下公开项目源码的阅读与统计：
[compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix) ·
[InstallerX-Revived](https://github.com/wxxsfxyzm/InstallerX-Revived) ·
[lyricon](https://github.com/proify/lyricon) ·
[Updater-KMP](https://github.com/YuKongA/Updater-KMP) ·
[HyperStar](https://github.com/YunZiA/HyperStar) ·
[XiaomiHelper](https://github.com/HowieHChen/XiaomiHelper) ·
[OShin](https://github.com/suqi8/OShin) ·
[StatusBarLyric](https://github.com/Block-Network/StatusBarLyric) ·
[Payload-Dumper-Compose](https://github.com/rcmiku/Payload-Dumper-Compose)

这些是独立项目，各自保留原有许可证；本仓库只从中归纳约定与度量，代码示例是**重新编写的示例**而非拷贝。与上述项目及小米公司均无隶属或背书关系。

许可：[Apache License 2.0](LICENSE)。**再分发或发布衍生作品时必须保留 `LICENSE` 与 `NOTICE`（含署名）并声明修改**（LICENSE 第 4 条）；纯内部使用不受署名约束。

```
Copyright 2026 cyf112233
```
