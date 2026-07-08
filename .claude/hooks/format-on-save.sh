#!/usr/bin/env bash
# PostToolUse(Edit|Write) 훅: .java 파일을 저장하면 Spotless로 포맷한다.
# Spotless가 build.gradle에 없으면 조용히 통과한다(no-op).
# 훅 stdin은 Claude Code가 주는 JSON. tool_input.file_path 를 뽑는다.

set -euo pipefail

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"

file_path="$(python3 -c '
import json, sys
try:
    data = json.load(sys.stdin)
except Exception:
    print(""); sys.exit(0)
ti = data.get("tool_input", {}) or {}
print(ti.get("file_path", "") or "")
' 2>/dev/null || true)"

# .java 파일이 아니면 아무것도 하지 않는다.
case "$file_path" in
  *.java) ;;
  *) exit 0 ;;
esac

# Spotless 미설정이면 no-op.
if ! grep -q "spotless" "$PROJECT_DIR/build.gradle" 2>/dev/null; then
  exit 0
fi

cd "$PROJECT_DIR"
# JAVA_HOME이 없으면 gradle이 실패하므로, 있을 때만 시도한다.
if [ -z "${JAVA_HOME:-}" ] && ! command -v java >/dev/null 2>&1; then
  echo "format-on-save: JAVA_HOME 미설정으로 스킵" >&2
  exit 0
fi

./gradlew spotlessApply "-PspotlessFiles=$(printf '%s' "$file_path" | sed 's/[.[\*^$/]/\\&/g')" >/dev/null 2>&1 || true
exit 0
