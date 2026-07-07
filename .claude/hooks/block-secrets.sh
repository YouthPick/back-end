#!/usr/bin/env bash
# PreToolUse(Bash) 훅: 위험한 파괴 명령과 시크릿 파일 접근을 차단한다.
# 차단 시 exit code 2 + stderr 사유. Claude Code가 사유를 읽고 명령을 막는다.

set -uo pipefail

command_str="$(python3 -c '
import json, sys
try:
    data = json.load(sys.stdin)
except Exception:
    print(""); sys.exit(0)
ti = data.get("tool_input", {}) or {}
print(ti.get("command", "") or "")
' 2>/dev/null || true)"

[ -z "$command_str" ] && exit 0

block() {
  echo "block-secrets: 차단됨 — $1" >&2
  exit 2
}

# 1) 파괴적 rm -rf (루트/홈/현재경로 통째 삭제)
if printf '%s' "$command_str" | grep -Eq 'rm[[:space:]]+(-[a-zA-Z]*[rR][a-zA-Z]*[[:space:]]+)+(-[a-zA-Z]*f[a-zA-Z]*[[:space:]]+)?(/|~|\$HOME|\.)([[:space:]]|$)'; then
  block "위험한 rm -rf 대상(루트/홈/현재 디렉토리 전체). 삭제 대상을 구체적 경로로 좁혀라."
fi
if printf '%s' "$command_str" | grep -Eq 'rm[[:space:]]+-[a-zA-Z]*[rR][a-zA-Z]*f|rm[[:space:]]+-[a-zA-Z]*f[a-zA-Z]*[rR]'; then
  if printf '%s' "$command_str" | grep -Eq '(/|~|\$HOME)([[:space:]]|$)|[[:space:]]\*([[:space:]]|$)'; then
    block "광범위 삭제로 보이는 rm -rf. 경로를 명시적으로 좁혀라."
  fi
fi

# 2) 시크릿 파일 내용 노출 (.env 등을 출력/전송)
if printf '%s' "$command_str" | grep -Eq '(cat|less|more|head|tail|cp|scp|curl|nc|xxd|base64|strings)[^|]*\.env(\.[a-zA-Z]+)?([[:space:]]|$)'; then
  # .env.example 은 placeholder라 허용
  if ! printf '%s' "$command_str" | grep -Eq '\.env\.example'; then
    block "시크릿 파일(.env) 내용 노출 시도. .env는 커밋·출력·전송하지 않는다."
  fi
fi

# 3) .env 를 git에 추가
if printf '%s' "$command_str" | grep -Eq 'git[[:space:]]+add[^|]*\.env([[:space:]]|$)'; then
  if ! printf '%s' "$command_str" | grep -Eq '\.env\.example'; then
    block ".env를 git에 추가하려는 시도. .env는 커밋 금지."
  fi
fi

exit 0
