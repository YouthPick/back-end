#!/usr/bin/env bash
# PreToolUse(Bash) 훅: 위험한 파괴 명령과 시크릿 파일 접근을 차단한다.
# 차단 시 exit code 2 + stderr 사유. Claude Code가 사유를 읽고 명령을 막는다.
#
# 정규식만으로는 `rm -r -f`, `rm --recursive --force`, `git add .` 같은 우회가
# 쉬우므로, python3 + shlex로 토큰을 파싱해 플래그/대상/서브명령을 판별한다.
# 프로그램은 -c 인자로 넘겨 stdin(훅 JSON)을 그대로 python에 전달한다.

set -uo pipefail

exec python3 -c '
import json, sys, shlex, os, re, subprocess

try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit(0)

cmd = (data.get("tool_input", {}) or {}).get("command", "") or ""
if not cmd.strip():
    sys.exit(0)

def block(msg):
    sys.stderr.write("block-secrets: 차단됨 — " + msg + "\n")
    sys.exit(2)

segments = re.split(r"[;\n]|&&|\|\||\||&", cmd)

BROAD_TARGETS = {"/", "~", "$HOME", ".", "./", "*", "./*", "~/", "/*", ".."}
ENV_RE = re.compile(r"^\.env(\..+)?$")
ASSIGN_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*=")

def is_env_file(arg):
    base = os.path.basename(arg)
    return bool(ENV_RE.fullmatch(base)) and base != ".env.example"

def dangerous_target(t):
    tt = t.rstrip("/")
    return (
        t in BROAD_TARGETS
        or tt in ("", "/", "~", "$HOME", ".", "..")
        or t.startswith(("/", "~", "$HOME"))
        or "*" in t
    )

for seg in segments:
    try:
        tokens = shlex.split(seg)
    except ValueError:
        tokens = seg.split()
    if not tokens:
        continue

    idx = 0
    while idx < len(tokens):
        t = tokens[idx]
        if t in ("sudo", "env") or ASSIGN_RE.match(t):
            idx += 1
            continue
        break
    if idx >= len(tokens):
        continue

    name = os.path.basename(tokens[idx])
    args = tokens[idx + 1:]

    # 1) 파괴적 rm (플래그 결합/분리/롱옵션 모두 탐지)
    if name == "rm":
        recursive = force = False
        targets = []
        for a in args:
            if a == "--":
                continue
            if a == "--recursive":
                recursive = True
            elif a == "--force":
                force = True
            elif a.startswith("--"):
                pass
            elif a.startswith("-") and len(a) > 1:
                flags = a[1:]
                if "r" in flags or "R" in flags:
                    recursive = True
                if "f" in flags:
                    force = True
            else:
                targets.append(a)
        if recursive and any(dangerous_target(x) for x in targets):
            block("위험한 rm -r 대상(루트/홈/현재 디렉토리/와일드카드). 삭제 대상을 구체적 경로로 좁혀라.")

    # 2) 시크릿 파일(.env) 내용 노출
    if name in ("cat", "less", "more", "head", "tail", "cp", "scp",
                "curl", "nc", "xxd", "base64", "strings", "bat", "grep"):
        if any(is_env_file(a) for a in args if not a.startswith("-")):
            block("시크릿 파일(.env) 내용 노출 시도. .env는 커밋·출력·전송하지 않는다.")

    # 3) git add 로 .env 스테이징 (명시적/광범위/force 모두 고려)
    if name == "git":
        # git 전역 옵션(-C <path>, -c <k=v>, --git-dir <path> 등)을 먼저 건너뛰고
        # 실제 서브명령을 찾는다. 값을 따로 받는 옵션은 다음 토큰까지 스킵.
        # (git -C x add ., git -c k=v add .env 우회 방지)
        VALUE_OPTS = ("-C", "-c", "--git-dir", "--work-tree", "--namespace", "--super-prefix")
        gi = 0
        while gi < len(args) and args[gi].startswith("-"):
            gi += 2 if args[gi] in VALUE_OPTS else 1
        subcmd = args[gi] if gi < len(args) else None
        add_args = args[gi + 1:]

        if subcmd == "add":
            positionals = [a for a in add_args if not a.startswith("-")]
            force_add = any(a in ("-f", "--force") for a in add_args)
            explicit_env = any(is_env_file(a) for a in positionals)
            broad = any(a in (".", "./", "-A", "--all", "-u", "--update", "*") for a in add_args)

            if explicit_env:
                block(".env를 git에 추가하려는 시도. .env는 커밋 금지.")
            if broad or force_add:
                status_cmd = ["git", "status", "--porcelain"]
                if force_add:
                    status_cmd.append("--ignored")
                status_cmd += ["--", ".env"]
                try:
                    out = subprocess.run(status_cmd, capture_output=True, text=True, timeout=5)
                    if out.stdout.strip():
                        block(".env가 스테이징 대상에 포함될 수 있음. .env는 커밋 금지.")
                except Exception:
                    pass

sys.exit(0)
'
