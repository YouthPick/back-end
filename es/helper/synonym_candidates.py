# -*- coding: utf-8 -*-
"""동의어 후보 검증 (개발용 도구 — 서비스 코드 아님).

동의어는 재현율을 올리는 대신 정밀도를 깎는다. 감으로 넣으면 "취업"을 찾는 사람에게
관계없는 정책이 섞이므로, 후보마다 실제 색인에서 다음을 재고 넣을지 판단한다.

  - 각 단어가 몇 건을 잡는가 (0건이면 그 단어는 데이터에 없는 것 — 사전에 넣어도 의미 없음)
  - 두 단어의 문서 집합이 얼마나 겹치는가
      겹침이 이미 높다  → 동의어로 묶을 실익이 적다
      겹침이 낮다        → 묶으면 그만큼 새로 걸린다(= 살리는 건수)

사용:
  python es/helper/synonym_candidates.py            # 후보 그룹 전부 검증
  python es/helper/synonym_candidates.py 취업 채용   # 임의의 단어들만 검증
"""
import itertools
import json
import sys
import urllib.request

ES = "http://localhost:9200"
INDEX = "policy"
FIELDS = ["title^3", "keywords^2", "organizationName^2", "description", "supportContent"]

# 검증할 후보 그룹. 기준선 측정(docs/2026-09-05-검색-기준선-측정.md)의 유형 A 와
# 실사용 검색 로그에서 뽑았다.
CANDIDATES = [
    ["일자리", "취업", "채용", "구직", "일거리"],
    ["주거", "주택", "월세", "임대", "전세"],
    ["지원금", "보조금", "수당", "장려금"],
    ["창업", "스타트업", "창업자"],
    ["학자금", "등록금", "장학금"],
    ["대출", "융자", "이자"],
    ["알바", "아르바이트"],
    ["자격증", "응시료", "시험"],
    ["교육", "훈련", "연수"],
    ["상담", "컨설팅", "멘토링"],
]


def post(path, body):
    req = urllib.request.Request(
        ES + path,
        data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    return json.loads(urllib.request.urlopen(req).read().decode("utf-8"))


def doc_ids(word):
    """그 단어가 걸리는 문서 id 집합. 동의어 적용 전 상태를 본다."""
    body = {
        "size": 2000,
        "_source": False,
        "query": {"multi_match": {"query": word, "fields": FIELDS, "type": "best_fields"}},
    }
    return {h["_id"] for h in post(f"/{INDEX}/_search", body)["hits"]["hits"]}


def tokens(word):
    body = {"analyzer": "ko_index", "text": word}
    return [t["token"] for t in post(f"/{INDEX}/_analyze", body)["tokens"]]


def report(group):
    print(f"\n### {' / '.join(group)}")
    ids = {}
    for word in group:
        ids[word] = doc_ids(word)
        mark = "  ⚠ 데이터에 없음" if not ids[word] else ""
        print(f"  {word:<8} {len(ids[word]):>5}건  토큰={tokens(word)}{mark}")

    print("  겹침(자카드) / 묶으면 새로 걸리는 건수")
    for a, b in itertools.combinations(group, 2):
        sa, sb = ids[a], ids[b]
        if not sa or not sb:
            continue
        jaccard = len(sa & sb) / len(sa | sb)
        print(f"    {a:>6} ↔ {b:<6} 겹침 {jaccard:5.0%}   {a}→+{len(sb - sa):<4} {b}→+{len(sa - sb)}")


def main():
    groups = [sys.argv[1:]] if len(sys.argv) > 1 else CANDIDATES
    for group in groups:
        report(group)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
