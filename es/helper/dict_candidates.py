# -*- coding: utf-8 -*-
"""nori 사용자 사전 후보 추출 (개발용 도구 — 서비스 코드 아님).

정책 제목/키워드를 공백 단위 단어로 쪼갠 뒤 각 단어를 nori 로 분석해,
**nori 가 제대로 인식하지 못한 단어**를 찾는다.

판정 기준: 한 단어가 3조각 이상으로 쪼개지면서 **한 글자 토큰**을 만들어내면
nori 사전에 없는 단어일 가능성이 높다.
  청년도약계좌 -> 청년 · 도 · 약 · 계좌   (4조각, 한 글자 2개)  → 후보

사용:
  python es/helper/dict_candidates.py titles.txt
  python es/helper/dict_candidates.py titles.txt --min-count 3
"""
import argparse
import json
import re
import sys
import urllib.request
from collections import Counter

ES = "http://localhost:9200"
HANGUL = re.compile(r"[가-힣]")
STRIP = "()[]{}<>,.·:;\"'/-~!?★☆「」『』…"


def tokenize(word):
    body = {"tokenizer": "nori_tokenizer", "text": word}
    req = urllib.request.Request(
        f"{ES}/_analyze",
        data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req) as res:
        return [t["token"] for t in json.loads(res.read().decode("utf-8"))["tokens"]]


def words_of(lines):
    """공백으로 쪼갠 뒤 구두점을 털어낸 한글 단어들."""
    counter = Counter()
    for line in lines:
        for raw in line.split():
            word = raw.strip(STRIP)
            # 3글자 미만은 쪼개질 여지가 적고, 한글이 없으면 nori 대상이 아니다
            if len(word) >= 3 and HANGUL.search(word):
                counter[word] += 1
    return counter


def main():
    p = argparse.ArgumentParser()
    p.add_argument("file", help="한 줄에 제목 하나")
    p.add_argument("--min-count", type=int, default=2, help="이 횟수 이상 등장한 단어만")
    args = p.parse_args()

    with open(args.file, encoding="utf-8") as f:
        lines = [line.strip() for line in f if line.strip()]

    counter = words_of(lines)
    targets = [(w, c) for w, c in counter.items() if c >= args.min_count]
    print(f"제목 {len(lines)}건 / 고유 단어 {len(counter)}개 / 분석 대상 {len(targets)}개\n")

    broken = []
    for i, (word, count) in enumerate(targets, 1):
        if i % 200 == 0:
            print(f"  ... {i}/{len(targets)}", file=sys.stderr)
        tokens = tokenize(word)
        singles = [t for t in tokens if len(t) == 1 and HANGUL.match(t)]
        # 3조각 이상 + 한 글자 토큰 존재 = nori 가 모르는 단어
        if len(tokens) >= 3 and singles:
            broken.append((count, len(singles), word, tokens))

    broken.sort(key=lambda x: (-x[0], -x[1]))
    print(f"사전 후보 {len(broken)}개 (등장 많은 순)\n")
    print(f"{'횟수':>4}  {'단어':<20} 분해 결과")
    print("-" * 78)
    for count, _, word, tokens in broken:
        print(f"{count:>4}  {word:<20} {' · '.join(tokens)}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
