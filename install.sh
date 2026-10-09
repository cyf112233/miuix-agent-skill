#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 cyf112233 <frank13967973981@163.com>
#
# 把本仓库安装为一个 agent skill。
#
# 背景：skill 不是 Node 库，没有 npm 式的「通用安装」。它只是一个目录约定：
#       <skills 根>/<skill 名>/SKILL.md（顶部带 name/description frontmatter）。
#       本脚本的作用就是帮你把仓库放到正确的目录，并做安装后校验。
#
#   bash install.sh                 # → $DSH_HOME/skills/miuix-ui（默认，只装这一个根）
#   bash install.sh --agents        # → ~/.agents/skills/miuix-ui
#   bash install.sh --claude        # → ~/.claude/skills/miuix-ui
#   bash install.sh --project       # → <git 项目根>/.dsh/skills/miuix-ui
#   bash install.sh --dir <路径>     # → <路径>/miuix-ui
#   bash install.sh --all           # → 以上所有「已存在的父目录」都装一份
#   bash install.sh --uninstall …   # 卸载（作用范围与上面选择器一致）
#
# 也可以完全不使用本脚本，直接 clone 成技能目录：
#   git clone <repo-url> ~/.dsh/skills/miuix-ui
set -euo pipefail

SKILL_NAME="miuix-ui"
SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

[[ -f "$SRC/SKILL.md" ]] || { echo "找不到 $SRC/SKILL.md —— 请在仓库根目录运行" >&2; exit 1; }

SELECTORS=(); UNINSTALL=0; CUSTOM_DIR=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --agents)    SELECTORS+=("agents") ;;
    --claude)    SELECTORS+=("claude") ;;
    --project)   SELECTORS+=("project") ;;
    --all)       SELECTORS+=("all") ;;
    --dir)       SELECTORS+=("custom"); CUSTOM_DIR="${2:?--dir 需要路径}"; shift ;;
    --uninstall) UNINSTALL=1 ;;
    -h|--help)   sed -n '2,22p' "$0"; exit 0 ;;
    *) echo "未知参数: $1" >&2; exit 2 ;;
  esac
  shift
done
# 只给了 --uninstall 时，默认作用于全局根（与不带选择器安装的默认一致）
[[ ${#SELECTORS[@]} -gt 0 ]] || SELECTORS=("global")

project_root() { git rev-parse --show-toplevel 2>/dev/null || pwd; }

# 展开选择器为实际的目标根列表（去重）
ROOTS=()
add_root() { local r="$1"; [[ -n "$r" ]] || return 0
  for x in "${ROOTS[@]:-}"; do [[ "$x" == "$r" ]] && return 0; done; ROOTS+=("$r"); }
for sel in "${SELECTORS[@]}"; do
  case "$sel" in
    global)  add_root "${DSH_HOME:-$HOME/.dsh}/skills" ;;
    agents)  add_root "$HOME/.agents/skills" ;;
    claude)  add_root "$HOME/.claude/skills" ;;
    project) add_root "$(project_root)/.dsh/skills" ;;
    custom)  add_root "$CUSTOM_DIR" ;;
    all)
      add_root "${DSH_HOME:-$HOME/.dsh}/skills"
      add_root "$HOME/.agents/skills"
      add_root "$HOME/.claude/skills"
      # 项目级只在确实处于 git 仓库时才装
      git rev-parse --show-toplevel >/dev/null 2>&1 && add_root "$(project_root)/.dsh/skills"
      ;;
  esac
done

# ---- 安全护栏：只允许操作 <root>/<SKILL_NAME>，且拒绝危险路径 ----
safe_target() {
  local root="$1" target
  target="$root/$SKILL_NAME"
  case "$root" in
    ""|"/"|"."|"$HOME"|"$HOME/"*) : ;;   # $HOME 本身不允许作为 root
  esac
  [[ "$root" == "$HOME" || "$root" == "/" || -z "$root" ]] && {
    echo "拒绝操作危险路径作为 skills 根: '$root'" >&2; exit 3; }
  [[ "$(basename "$target")" == "$SKILL_NAME" ]] || {
    echo "内部错误：目标不是 $SKILL_NAME 目录: $target" >&2; exit 3; }
  printf '%s' "$target"
}

status=0
for root in "${ROOTS[@]}"; do
  target="$(safe_target "$root")"

  if [[ "$UNINSTALL" == "1" ]]; then
    if [[ -d "$target" ]]; then rm -rf "$target"; echo "已卸载 $target"
    else echo "未安装（跳过）$target"; fi
    continue
  fi

  # 源与目标同一个目录（clone 进 skills 目录的常见情形）→ 无需复制
  src_real="$(cd "$SRC" && pwd -P)"
  if [[ -d "$target" ]] && [[ "$src_real" == "$(cd "$target" && pwd -P)" ]]; then
    echo "已在位（源即目标，未改动）$target"; continue
  fi

  mkdir -p "$root"
  rm -rf "$target"; mkdir -p "$target"
  for item in SKILL.md references scripts; do
    [[ -e "$SRC/$item" ]] && cp -r "$SRC/$item" "$target/"
  done

  # 安装后校验
  if ! head -1 "$target/SKILL.md" | grep -q '^---$'; then
    echo "❌ 校验失败（缺 frontmatter）: $target" >&2; status=1; continue
  fi
  if [[ "$(grep -m1 '^name:' "$target/SKILL.md" || true)" != "name: $SKILL_NAME" ]]; then
    echo "❌ 校验失败（name 不是 $SKILL_NAME）: $target" >&2; status=1; continue
  fi
  grep -q '^description:' "$target/SKILL.md" || {
    echo "❌ 校验失败（缺 description）: $target" >&2; status=1; continue; }

  echo "已安装 $SKILL_NAME → $target"
done

[[ "$UNINSTALL" == "1" ]] || cat <<'TIP'

提示：skill 是目录约定，不是 npm 包——没有统一的 install 命令，各 agent 只认自己的 skills 根。
      若你的 agent 根不在上面几项里，用 --dir 指定，或直接：
        git clone <repo-url> <你的 skills 根>/miuix-ui
TIP
exit "$status"
