# -*- coding: utf-8 -*-
"""정책 검색 인덱스 생성 (개발용 도구 — 서비스 코드 아님).

es/policy-index.json 을 읽어 새 인덱스를 만들고 alias 를 붙인다.

alias 를 쓰는 이유: analyzer 설정은 색인 시점에 굳어서 매핑을 바꾸면 재색인이 필수다.
코드는 alias(policy)만 보게 하고 실제 인덱스(policy_v1, policy_v2...)를 갈아끼우면
검색 중단 없이 전환할 수 있다.

사용:
  python es/helper/create_index.py                 # policy_v1 생성 + alias 부착
  python es/helper/create_index.py --version 2     # policy_v2 생성 (alias 는 안 옮김)
  python es/helper/create_index.py --switch 2      # alias 를 policy_v2 로 이동
  python es/helper/create_index.py --status        # 현재 인덱스/alias 확인
  python es/helper/create_index.py --delete 1      # policy_v1 삭제
"""
import argparse
import json
import sys
import urllib.error
import urllib.request

ES = "http://localhost:9200"
ALIAS = "policy"
MAPPING_FILE = "es/policy-index.json"


def call(method, path, body=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(
        f"{ES}{path}", data=data, method=method,
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(req) as res:
            text = res.read().decode("utf-8")
            return json.loads(text) if text else {}
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8")
        print(f"[{e.code}] {method} {path}", file=sys.stderr)
        try:
            print(json.dumps(json.loads(detail), ensure_ascii=False, indent=2), file=sys.stderr)
        except ValueError:
            print(detail, file=sys.stderr)
        sys.exit(1)


def status():
    print("=== 인덱스 ===")
    for line in call("GET", "/_cat/indices?format=json&v"):
        print(f"  {line['index']:<16} docs={line['docs.count']:<8} size={line['store.size']}")
    print("=== alias ===")
    aliases = call("GET", "/_cat/aliases?format=json")
    if not aliases:
        print("  (없음)")
    for a in aliases:
        print(f"  {a['alias']} -> {a['index']}")


def create(version):
    index = f"{ALIAS}_v{version}"
    with open(MAPPING_FILE, encoding="utf-8") as f:
        mapping = json.load(f)

    call("PUT", f"/{index}", mapping)
    print(f"인덱스 생성: {index}")

    # alias 가 아직 없을 때만 자동 부착 — 이미 있으면 --switch 로 명시적으로 옮긴다
    if not call("GET", "/_cat/aliases?format=json"):
        call("POST", "/_aliases", {"actions": [{"add": {"index": index, "alias": ALIAS}}]})
        print(f"alias 부착: {ALIAS} -> {index}")


def switch(version):
    index = f"{ALIAS}_v{version}"
    actions = [{"remove": {"index": "*", "alias": ALIAS}}, {"add": {"index": index, "alias": ALIAS}}]
    call("POST", "/_aliases", {"actions": actions})   # 원자적 — 중간에 alias 가 비는 순간이 없다
    print(f"alias 이동: {ALIAS} -> {index}")


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--version", type=int, default=1)
    p.add_argument("--switch", type=int)
    p.add_argument("--delete", type=int)
    p.add_argument("--status", action="store_true")
    args = p.parse_args()

    if args.status:
        status()
    elif args.switch:
        switch(args.switch)
        status()
    elif args.delete:
        call("DELETE", f"/{ALIAS}_v{args.delete}")
        print(f"삭제: {ALIAS}_v{args.delete}")
    else:
        create(args.version)
        status()


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
