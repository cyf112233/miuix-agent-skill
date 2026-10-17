# miuix-agent-skill

给 AI 编程 agent 用的 **Miuix** 界面构建技能包：让 agent 按统一规范产出 Miuix（小米 HyperOS · Compose Multiplatform）界面，而不是凭记忆瞎写 API。

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

装好后直接提需求即可，例如「用 miuix 写一个带分组开关的设置页」。

## 内容

8 个通用场景——主框架/多 Tab、首页仪表盘、通用列表、信息展示、表单编辑、状态页、自适应（大屏/折叠屏/RTL）、引导与权限——加设置页，每个都给可抄的完整结构。另含组件签名与默认值、主题配色、**状态卡的红/黄/绿**（Miuix 没有 success/warning token，这里给出四个真实项目的取值与规则）、`miuix-nav` 导航、图标、旧版 API 迁移表、反模式清单，以及一个零依赖 lint：

```bash
node scripts/miuix-lint.mjs <你的项目目录>
```

规则标注了来源强度（库默认值 / 多数派惯例 / 单项目偏好 / 已被证伪），**单项目偏好不当规范**。

## 边界

- **符号、必填参数、枚举值、声称的默认值**：机器校验（206 个片段，974 个函数的 API 模型）
- **内置样张**：编译通过、渲染通过、lint 0 error / 0 warning
- **仍需真机**：insets、手势、动画、RTL、字体缩放（样张只在 Desktop target 编译，没有真机验证）
- 场景规则标注了来源强度（库默认值 / 多数派惯例 / 单项目偏好 / 已被证伪），**单项目偏好不当规范**

## 许可

[Apache-2.0](LICENSE)，署名见 [NOTICE](NOTICE)；再分发需保留这两份并声明修改。

约定来自 Miuix 官方 example 与文档，以及 [InstallerX-Revived](https://github.com/wxxsfxyzm/InstallerX-Revived)、[lyricon](https://github.com/proify/lyricon)、[Updater-KMP](https://github.com/YuKongA/Updater-KMP)、[HyperStar](https://github.com/YunZiA/HyperStar)、[XiaomiHelper](https://github.com/HowieHChen/XiaomiHelper)、[OShin](https://github.com/suqi8/OShin)、[StatusBarLyric](https://github.com/Block-Network/StatusBarLyric)、[Payload-Dumper-Compose](https://github.com/rcmiku/Payload-Dumper-Compose) 的公开源码；与这些项目及小米公司无隶属关系。
