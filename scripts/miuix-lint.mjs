#!/usr/bin/env node
// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 cyf112233
//
// miuix-agent-skill — https://github.com/cyf112233/miuix-agent-skill
// miuix-lint — 零依赖静态检查，专治「AI 写的 Miuix 代码」常见问题。
//
//   node miuix-lint.mjs <目录> [更多目录…] [--quiet] [--max=N]
//
// 退出码：0 = 无 error；1 = 有 error；2 = 用法错误。
//
// 检查项
//   M1 androidx.compose.material3 / material.icons 残留（warning）
//   E2 使用了已移除的旧版 miuix API（附迁移建议）
//   E3 MiuixIcons.Xxx 缺少 import（编译期必错）
//   W1 硬编码颜色（Color(0xFF…) / Color.Red 等）
//   W2 TopAppBar 未传 scrollBehavior
//   W3 LazyColumn/LazyRow 缺少 overScrollVertical() / overscrollEffect = null
//   W4 Overlay* 弹层所在文件没有 Scaffold
//   W5 Card 使用了非 12.dp 的横向 padding
//   W6 Card 内的 HorizontalDivider 没有 horizontal padding
//   W7 有 TopAppBar 的页面又用了 statusBarsPadding()
//   I1 文本用了 Color.Black / Color.White（深色模式必然出问题）

import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, relative, basename } from 'node:path';

const args = process.argv.slice(2);
const flags = new Set(args.filter((a) => a.startsWith('--')));
const roots = args.filter((a) => !a.startsWith('--'));
const QUIET = flags.has('--quiet');

if (roots.length === 0) {
  console.error('用法: node miuix-lint.mjs <目录> [更多目录…] [--quiet]');
  process.exit(2);
}

const SKIP_DIRS = new Set(['build', '.git', '.gradle', '.idea', 'node_modules', 'out', 'dist']);

function walkKt(dir, out = []) {
  let entries;
  try { entries = readdirSync(dir, { withFileTypes: true }); } catch { return out; }
  for (const e of entries) {
    if (e.isDirectory()) {
      if (SKIP_DIRS.has(e.name)) continue;
      walkKt(join(dir, e.name), out);
    } else if (e.name.endsWith('.kt')) {
      out.push(join(dir, e.name));
    }
  }
  return out;
}

const LEGACY = [
  ['miuix.kmp.extra.SuperSwitch', 'SwitchPreference（top.yukonga.miuix.kmp.preference）'],
  ['miuix.kmp.extra.SuperArrow', 'ArrowPreference'],
  ['miuix.kmp.extra.SuperCheckbox', 'CheckboxPreference'],
  ['miuix.kmp.extra.SuperRadioButton', 'RadioButtonPreference'],
  ['miuix.kmp.extra.SuperSlider', 'SliderPreference'],
  ['miuix.kmp.extra.SuperDropdown', 'OverlayDropdownPreference'],
  ['miuix.kmp.extra.SuperSpinner', 'OverlaySpinnerPreference'],
  ['miuix.kmp.extra.SuperDialog', 'OverlayDialog'],
  ['miuix.kmp.extra.SuperBottomSheet', 'OverlayBottomSheet'],
  ['miuix.kmp.extra.DropdownImpl', 'top.yukonga.miuix.kmp.basic.DropdownImpl'],
  ['miuix.kmp.extra.CheckboxLocation', 'top.yukonga.miuix.kmp.preference.CheckboxLocation'],
  ['miuix.kmp.basic.ListPopup', 'ListPopupColumn / OverlayListPopup'],
  ['miuix.kmp.icon.icons.', 'icon.extended.* / icon.basic.*（逐个 import）'],
  ['MiuixPopupUtil', '用 Overlay* 组件的 show 状态控制'],
  ['miuix.kmp.utils.getWindowSize', 'BoxWithConstraints / LocalWindowInfo'],
  ['miuix.kmp.utils.G2RoundedCornerShape', 'Modifier.squircleClip / squircleSurface'],
  ['miuix.kmp.theme.miuixShape', 'RoundedCornerShape / squircle 修饰符'],
  ['miuix.kmp.theme.miuixUnevenShape', 'RoundedCornerShape / squircle 修饰符'],
  ['miuix.kmp.utils.BackHandler', 'androidx.navigationevent.compose.NavigationBackHandler'],
  ['miuix.kmp.interfaces.HoldDownInteraction', 'holdDownState 参数 / Modifier.pressable'],
  ['miuix.kmp.basic.LazyColumn', 'androidx.compose.foundation.lazy.LazyColumn'],
];

const HARDCODED_OK = /\/\/\s*(miuix-lint:allow|noqa|品牌色|brand|必需)/i;
const WEIGHTS = new Set(['Basic', 'Light', 'Normal', 'Regular', 'Medium', 'Demibold']);

const problems = [];
function add(sev, file, line, code, msg) {
  problems.push({ sev, file: relative(process.cwd(), file), line, code, msg });
}

/** Blank out comments while preserving offsets. */
function mask(code) {
  const out = code.split('');
  let i = 0;
  while (i < out.length) {
    if (out[i] === '/' && out[i + 1] === '/') { while (i < out.length && out[i] !== '\n') out[i++] = ' '; }
    else if (out[i] === '/' && out[i + 1] === '*') {
      while (i < out.length && !(out[i] === '*' && out[i + 1] === '/')) { if (out[i] !== '\n') out[i] = ' '; i++; }
      if (i < out.length) { out[i++] = ' '; out[i++] = ' '; }
    } else if (out[i] === '"') {
      out[i++] = '"';
      while (i < out.length && out[i] !== '"') { if (out[i] !== '\n') out[i] = ' '; i++; }
      if (i < out.length) out[i++] = '"';
    } else i++;
  }
  return out.join('');
}

const lineOf = (text, idx) => text.slice(0, idx).split('\n').length;
const lineText = (text, idx) => text.split('\n')[lineOf(text, idx) - 1] ?? '';

const files = roots.flatMap((r) => (statSync(r).isDirectory() ? walkKt(r) : r.endsWith('.kt') ? [r] : []));

for (const file of files) {
  const raw = readFileSync(file, 'utf8');
  if (/(^|[\\/])(material|material3|md3)([\\/]|$)/i.test(file)) continue;  const code = mask(raw);
  const imports = [...code.matchAll(/^import\s+([\w.]+)(?:\s+as\s+(\w+))?/gm)].map((m) => m[1]);
  const importSet = new Set(imports);
  const isMiuixFile = /top\.yukonga\.miuix/.test(code);

  // ---- M1 Material leftovers
  for (const m of code.matchAll(/^import\s+(androidx\.compose\.material[\w.]*)/gm)) {
    add('M1', file, lineOf(code, m.index), lineText(code, m.index),
      `还在用 Material（${m[1]}）；Miuix 有对应组件，见 references/components.md`);
  }

  // ---- E2 legacy miuix API (word-boundary: don't flag ListPopupColumn for "ListPopup")
  for (const [needle, fix] of LEGACY) {
    const re = new RegExp(needle.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '(?![\\w])', 'g');
    for (const m of code.matchAll(re)) {
      add('E2', file, lineOf(code, m.index), lineText(code, m.index), `旧版 miuix API「${needle}」→ 改用 ${fix}`);
    }
  }

  // ---- E3 MiuixIcons without import
  if (isMiuixFile) {
    // names this file declares itself (a project vendoring its own icons)
    const localIcons = new Set(
      [...code.matchAll(/val\s+MiuixIcons(?:\.\w+)?\.(\w+)\s*:/g)].map((m) => m[1]),
    );
    for (const m of code.matchAll(/(?<![\w.])MiuixIcons(?:\.(\w+))?\.(\w+)/g)) {
      const [, weight, name] = m;
      if (weight && !WEIGHTS.has(weight)) continue;
      if (localIcons.has(name)) continue;
      // skip declarations (a project shipping its own icon: `val MiuixIcons.X: ImageVector`)
      const before = code.slice(Math.max(0, m.index - 60), m.index);
      const after = code.slice(m.index + m[0].length, m.index + m[0].length + 4);
      if (/\b(?:val|var|fun)\s+(?:[\w.]+\.)*$/.test(before)) continue;
      if (/^\s*:/.test(after)) continue;
      const need = [
        `top.yukonga.miuix.kmp.icon.extended.${name}`,
        `top.yukonga.miuix.kmp.icon.basic.${name}`,
      ];
      if (!need.some((n) => importSet.has(n))) {
        const pkg = weight === 'Basic' ? 'icon.basic' : 'icon.extended';
        add('E3', file, lineOf(code, m.index), lineText(code, m.index),
          `缺少 import：import top.yukonga.miuix.kmp.${pkg}.${name}（MiuixIcons.${weight ? weight + '.' : ''}${name} 是扩展属性）`);
      }
    }
  }

  // ---- W1 hardcoded colors
  for (const m of code.matchAll(/Color\(0x[0-9A-Fa-f]{6,8}\)/g)) {
    const lt = lineText(code, m.index);
    if (HARDCODED_OK.test(lt) || HARDCODED_OK.test(raw.split('\n')[lineOf(code, m.index) - 2] ?? '')) continue;
    add('W1', file, lineOf(code, m.index), lt,
      '硬编码颜色；优先用 MiuixTheme.colorScheme 的语义色。确需保留时在该行加 `// miuix-lint:allow` 并写明理由（逃生口就是 `miuix-lint:allow`）');
  }
  for (const m of code.matchAll(/\bColor\.(Black|White|Red|Green|Blue|Gray|Yellow|Magenta|Cyan)\b/g)) {
    // 允许 Color.Unspecified / Color.Transparent 之外的极少数场景，但字面色一定提醒
    add('W1', file, lineOf(code, m.index), lineText(code, m.index),
      `硬编码 Color.${m[1]}；改用 colorScheme 语义色（深色模式会不可读）`);
  }

  // ---- I1 text colors
  for (const m of code.matchAll(/\bText\s*\(([\s\S]{0,400}?)\)/g)) {
    if (/color\s*=\s*Color\.(Black|White)\b/.test(m[1])) {
      add('I1', file, lineOf(code, m.index), lineText(code, m.index), 'Text 用了 Color.Black/White，深色模式必然出问题');
    }
  }

  // ---- W2 TopAppBar without scrollBehavior
  for (const m of code.matchAll(/(?:Small)?TopAppBar\s*\(/g)) {
    const end = findCallEnd(code, m.index + m[0].length - 1);
    const call = code.slice(m.index, end);
    if (!/scrollBehavior\s*=/.test(call)) {
      add('W2', file, lineOf(code, m.index), lineText(code, m.index),
        'TopAppBar 未传 scrollBehavior；滚动折叠/吸附会失效（需 MiuixScrollBehavior()）');
    }
  }

  // ---- W3 lazy scroll modifiers
  for (const m of code.matchAll(/\b(LazyColumn|LazyRow|LazyVerticalGrid)\s*\(/g)) {
    const end = findCallEnd(code, m.index + m[0].length - 1);
    const call = code.slice(m.index, end);
    if (!/overScrollVertical|overScrollHorizontal|overscrollEffect\s*=\s*null|overScrollVertical\(/.test(call)) {
      add('W3', file, lineOf(code, m.index), lineText(code, m.index),
        `${m[1]} 缺少 Miuix 滚动修饰符：加 .overScrollVertical()（水平列表用 overScrollHorizontal()）`);
    }
    if (!/overscrollEffect\s*=\s*null/.test(call)) {
      add('W3', file, lineOf(code, m.index), lineText(code, m.index),
        `${m[1]} 未设置 overscrollEffect = null（会与 Miuix 越界回弹叠加）`);
    }
  }

  // ---- W4 Overlay* without a Scaffold host in the same file
  //  只在文件里出现 Overlay* 却完全没有 Scaffold 时提示。
  //  注意：window.Window* 族（WindowDialog/WindowBottomSheet/WindowListPopup…）requiresScaffoldHost = false，
  //  不需要 Scaffold，所以不检查它们。
  //  另：Tab 页自带 Scaffold 是允许的（官方 example 就这么做），本检查不禁止任何 Scaffold 用法。
  if (!/\bScaffold\s*\(/.test(code)) {
    for (const m of code.matchAll(/\bOverlay[A-Z]\w*\s*\(/g)) {
      add('W4', file, lineOf(code, m.index), lineText(code, m.index),
        `${m[0].trim()} 需要宿主在某个 Scaffold 内（Overlay* 的 requiresScaffoldHost = true）；本文件没有 Scaffold，确认调用点在别的 Scaffold 作用域里。window.Window* 族无此要求`);
    }
  }

  // ---- W5 Card horizontal padding outside the accepted set
  //  12.dp 是多数派惯例，16.dp 被多个项目用于「自由内容卡」（scenarios.md 明确允许），
  //  所以两者都不报；只有更离谱的值才提示。
  const CARD_H_PADDING_OK = new Set([12, 16]);
  for (const m of code.matchAll(/\bCard\s*\(/g)) {
    const end = findCallEnd(code, m.index + m[0].length - 1);
    const call = code.slice(m.index, end);
    for (const p of call.matchAll(/padding\s*\(\s*horizontal\s*=\s*([\d.]+)\.dp/g)) {
      const v = parseFloat(p[1]);
      if (!CARD_H_PADDING_OK.has(v)) {
        add('W5', file, lineOf(code, m.index + p.index), lineText(code, m.index + p.index),
          `Card 横向 padding 是 ${v}.dp；项目内应统一（优选 12.dp，自由内容卡可用 16.dp）`);
      }
    }
  }

  // ---- W6 HorizontalDivider scoping
  //  规则分作用域（见 preferences.md / scenario-list.md）：
  //    - 分组 Card 内、Preference 行之间 → 需要 padding(horizontal = 16.dp)
  //    - 内容卡内部（自己加 16.dp 内边距的那种）→ 应当全宽
  //  静态无法可靠区分，所以只在「裸 HorizontalDivider()」且同文件同时存在 Preference 组件时才提示。
  if (/\bCard\s*\(/.test(code) && /(?:Switch|Arrow|Checkbox|RadioButton|Slider|Overlay\w+)Preference\s*\(/.test(code)) {
    for (const m of code.matchAll(/HorizontalDivider\s*\(\s*\)/g)) {
      add('W6', file, lineOf(code, m.index), lineText(code, m.index),
        'HorizontalDivider 没有横向内边距。若它在「分组 Card 内的 Preference 行之间」，需要 Modifier.fillMaxWidth().padding(horizontal = 16.dp)；若在「内容卡内部」则全宽是对的——请确认作用域');
    }
  }

  // ---- W7 statusBarsPadding together with TopAppBar
  if (/(?:Small)?TopAppBar\s*\(/.test(code)) {
    for (const m of code.matchAll(/\.statusBarsPadding\(\)/g)) {
      add('W7', file, lineOf(code, m.index), lineText(code, m.index),
        '已有 TopAppBar 时不要再 statusBarsPadding()；用 paddingValues.calculateTopPadding()');
    }
  }
}

function findCallEnd(code, openParenIdx) {
  let depth = 0;
  for (let i = openParenIdx; i < code.length; i++) {
    const c = code[i];
    if (c === '(') depth++;
    else if (c === ')') { depth--; if (depth === 0) return i + 1; }
  }
  return code.length;
}

// ---------------------------------------------------------------- report
const order = { E2: 0, E3: 1, M1: 2, W1: 3, I1: 4, W2: 5, W3: 6, W4: 7, W5: 8, W6: 9, W7: 10 };
problems.sort((a, b) => a.file.localeCompare(b.file) || a.line - b.line || order[a.sev] - order[b.sev]);

const errors = problems.filter((p) => p.sev.startsWith('E'));
const warns = problems.filter((p) => !p.sev.startsWith('E'));

if (!QUIET) {
  let cur = '';
  for (const p of problems) {
    if (p.file !== cur) { console.log(`\n${p.file}`); cur = p.file; }
    console.log(`  ${String(p.line).padStart(4)}  ${p.sev}  ${p.msg}`);
    if (p.code && p.code.trim()) console.log(`        ${p.code.trim().slice(0, 120)}`);
  }
  if (problems.length) console.log('');
}

console.log(`扫描 ${files.length} 个 Kotlin 文件：${errors.length} 个 error，${warns.length} 个 warning。`);
if (errors.length) {
  console.log('error = 会编译失败或已被移除的 API：E2 旧版 API / E3 图标缺 import。');
}
console.log('warning = 规范问题：M1 Material 残留 / W1 硬编码颜色 / W2 顶栏缺 scrollBehavior / W3 列表缺滚动修饰符 / W4 弹层缺 Scaffold / W5-W7 间距与 insets。');
process.exit(errors.length ? 1 : 0);
