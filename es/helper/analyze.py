# -*- coding: utf-8 -*-
"""ES _analyze 호출 헬퍼 (개발용 도구 — 서비스 코드 아님).

PowerShell 이 curl.exe 에 인자를 넘길 때 따옴표를 재해석해 공백에서 잘리고,
한글은 CP949 로 깨진다. 여기서 JSON 을 직접 만들어 UTF-8 로 보내 둘 다 우회한다.

사용:
  python es/helper/analyze.py "청년 일자리를 지원하는 정책"
  python es/helper/analyze.py --decompound mixed "청년도약계좌 가입 지원"
  python es/helper/analyze.py --tokenizer standard "청년도약계좌"
  python es/helper/analyze.py --analyzer ko_search --index policy "청년 일자리"
  python es/helper/analyze.py --compare "청년도약계좌 가입 지원"
"""
import argparse
import json
import sys
import urllib.request

ES = "http://localhost:9200"


def analyze(text, tokenizer=None, analyzer=None, index=None, decompound=None):
    """텍스트를 토큰 리스트로 변환한다."""
    body = {"text": text}
    if analyzer:
        body["analyzer"] = analyzer
    elif decompound:
        # tokenizer 를 문자열이 아니라 객체로 주면 옵션을 붙일 수 있다
        body["tokenizer"] = {"type": "nori_tokenizer", "decompound_mode": decompound}
    else:
        body["tokenizer"] = tokenizer or "nori_tokenizer"

    url = f"{ES}/{index}/_analyze" if index else f"{ES}/_analyze"
    req = urllib.request.Request(
        url,
        data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req) as res:
        result = json.loads(res.read().decode("utf-8"))
    return [t["token"] for t in result["tokens"]]


def main():
    p = argparse.ArgumentParser()
    p.add_argument("text", nargs="+")
    p.add_argument("--tokenizer")
    p.add_argument("--analyzer")
    p.add_argument("--index")
    p.add_argument("--decompound", choices=["none", "discard", "mixed"])
    p.add_argument(
        "--compare",
        action="store_true",
        help="standard 와 decompound 3종을 한 번에 비교",
    )
    args = p.parse_args()
    text = " ".join(args.text)

    if args.compare:
        print(text)
        print(f"  {'standard':<10} -> {' · '.join(analyze(text, tokenizer='standard'))}")
        for mode in ("none", "discard", "mixed"):
            tokens = analyze(text, decompound=mode)
            print(f"  {mode:<10} -> {' · '.join(tokens)}   ({len(tokens)}개)")
        return

    tokens = analyze(text, args.tokenizer, args.analyzer, args.index, args.decompound)
    print(f"{text}\n    -> {' · '.join(tokens)}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
