# -*- coding: utf-8 -*-
"""검색 품질 측정 (개발용 도구 — 서비스 코드 아님).

기준선 문서(docs/2026-09-05-검색-기준선-측정.md)의 검색어 26종을 실제 API로 호출해
건수와 1위 결과를 기록한다.

before/after 를 공정하게 비교하려면 같은 경로로 재야 한다. LIKE 시절 raw SQL 과
ES 질의를 직접 비교하면 마감·노출 필터 적용 여부가 달라 숫자가 오염된다. 그래서
SEARCH_ES_ENABLED 만 바꿔 앱을 두 번 띄우고, 같은 엔드포인트를 두 번 호출한다.

사용:
  python es/helper/measure_search.py --out mysql.json    # SEARCH_ES_ENABLED=false 로 띄운 뒤
  python es/helper/measure_search.py --out es.json       # SEARCH_ES_ENABLED=true  로 띄운 뒤
  python es/helper/measure_search.py --compare mysql.json es.json
"""
import argparse
import json
import sys
import time
import urllib.parse
import urllib.request

API = "http://localhost:8080/api/v1/policies"

# 기준선 문서의 26종. 유형별로 묶어 표에서 바로 읽히게 한다.
TERMS = [
    ("A 동의어", ["일자리", "취업", "채용", "구직", "일거리"]),
    ("A 동의어", ["주거", "주택", "월세", "임대"]),
    ("A 동의어", ["창업", "지원금", "보조금", "수당"]),
    ("B 띄어쓰기", ["청년 일자리", "청년일자리"]),
    ("C 어순", ["지원사업", "사업 지원", "청년 월세 지원", "월세 지원 청년", "일자리 청년"]),
    ("D 자연어", ["서울 청년 취업", "청년 취업 서울", "일자리를 찾고 있어요"]),
    ("E 표기변형", ["k-디지털", "K디지털", "디지털", "청년도약계좌", "도약계좌"]),
]


def search(keyword):
    qs = urllib.parse.urlencode({"keyword": keyword, "page": 1, "size": 1})
    started = time.perf_counter()
    with urllib.request.urlopen(f"{API}?{qs}", timeout=10) as res:
        body = json.loads(res.read().decode("utf-8"))
    elapsed_ms = (time.perf_counter() - started) * 1000
    items = body["data"]
    return {
        "total": body["meta"]["totalCount"],
        "top": items[0]["title"] if items else None,
        "ms": round(elapsed_ms, 1),
    }


def measure(out_path):
    results = {}
    for group, terms in TERMS:
        for term in terms:
            try:
                results[term] = {"group": group, **search(term)}
            except Exception as e:  # 서버가 죽었으면 나머지도 의미 없다
                print(f"실패: {term} — {e}", file=sys.stderr)
                sys.exit(1)
            row = results[term]
            print(f"  {term:<16} {row['total']:>6}건  {row['ms']:>6}ms  {row['top'] or '-'}")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)
    print(f"\n저장: {out_path} ({len(results)}종)")


def compare(before_path, after_path):
    before = json.load(open(before_path, encoding="utf-8"))
    after = json.load(open(after_path, encoding="utf-8"))

    print("| 유형 | 검색어 | LIKE | ES | 변화 |")
    print("|---|---|---:|---:|---|")
    zero_before = zero_after = 0
    for term, b in before.items():
        a = after.get(term, {})
        bt, at = b["total"], a.get("total", 0)
        zero_before += bt == 0
        zero_after += at == 0
        if bt == 0 and at > 0:
            change = f"**0건 해소 → {at}건**"
        elif bt > 0 and at == 0:
            change = "**회귀(0건)**"
        elif bt == at:
            change = "동일"
        else:
            change = f"{at - bt:+d}"
        print(f"| {b['group']} | `{term}` | {bt} | {at} | {change} |")

    total = len(before)
    print()
    print(f"0건 검색어: {zero_before}/{total} → {zero_after}/{total}")
    b_ms = sum(v["ms"] for v in before.values()) / total
    a_ms = sum(after[t]["ms"] for t in before if t in after) / total
    print(f"평균 응답: {b_ms:.1f}ms → {a_ms:.1f}ms")

    print("\n### 1위 결과가 바뀐 검색어")
    for term, b in before.items():
        a = after.get(term, {})
        if b.get("top") != a.get("top") and a.get("top"):
            print(f"- `{term}`: {b.get('top') or '(없음)'} → **{a['top']}**")


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--out")
    p.add_argument("--compare", nargs=2, metavar=("BEFORE", "AFTER"))
    args = p.parse_args()
    if args.compare:
        compare(*args.compare)
    elif args.out:
        measure(args.out)
    else:
        p.error("--out 또는 --compare 중 하나가 필요합니다")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
