# -*- coding: utf-8 -*-
"""minimum_should_match 값 비교 (개발용 도구 — 서비스 코드 아님).

여러 단어를 입력했을 때 몇 개가 맞아야 통과시킬지를 정하는 값이다.
느슨하면 '청년' 하나만 맞아도 전부 걸려 필터 구실을 못 하고,
빡빡하면 기준선의 0건 문제로 되돌아간다. 그 사이를 실측으로 고른다.

같은 검색어 묶음에 값만 바꿔가며 건수를 재고, 다음 둘을 함께 본다.
  - 0건이 되는 검색어 수      (너무 빡빡한지)
  - 전체 대비 비율이 큰 검색어 (너무 느슨한지 — 사실상 목록 조회)

사용:
  python es/helper/tune_msm.py
"""
import json
import sys
import urllib.request

ES = "http://localhost:9200"
INDEX = "policy"
TODAY = "2026-09-06"
FIELDS = ["title^3", "keywords^2", "organizationName^2", "description", "supportContent", "sidoNames.text"]

CANDIDATES = ["1", "2<70%", "2<-1", "3<70%", "75%", "100%"]

QUERIES = [
    "서울 청년 취업",
    "청년 취업 서울",
    "청년 월세 지원",
    "월세 지원 청년",
    "청년 일자리",
    "사업 지원",
    "일자리를 찾고 있어요",
    "면접 정장 지원",
    "K디지털",
    "청년",
]


def post(path, body):
    req = urllib.request.Request(
        ES + path,
        data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    return json.loads(urllib.request.urlopen(req).read().decode("utf-8"))


def count(keyword, msm):
    """서비스와 같은 필터를 걸고 센다 — 필터 없이 재면 마감된 정책까지 섞여 숫자가 오염된다."""
    body = {
        "size": 0,
        "track_total_hits": True,
        "query": {
            "bool": {
                "filter": [
                    {"term": {"visibility": "VISIBLE"}},
                    {"term": {"adminHidden": False}},
                    {"bool": {"should": [
                        {"bool": {"must_not": {"exists": {"field": "applicationEndDate"}}}},
                        {"range": {"applicationEndDate": {"gte": TODAY}}}], "minimum_should_match": 1}},
                    {"bool": {"should": [
                        {"exists": {"field": "applicationEndDate"}},
                        {"bool": {"must_not": {"exists": {"field": "businessPeriodEnd"}}}},
                        {"range": {"businessPeriodEnd": {"gte": TODAY}}}], "minimum_should_match": 1}},
                ],
                "must": [{"multi_match": {
                    "query": keyword, "fields": FIELDS,
                    "type": "best_fields", "minimum_should_match": msm}}],
            }
        },
    }
    return post(f"/{INDEX}/_search", body)["hits"]["total"]["value"]


def main():
    total = count("", "1") if False else None
    # 필터만 통과하는 전체 모수 — '너무 느슨한지' 판단의 분모
    body = {"size": 0, "track_total_hits": True, "query": {"match_all": {}}}
    corpus = post(f"/{INDEX}/_search", body)["hits"]["total"]["value"]

    header = "검색어".ljust(22) + "".join(m.rjust(9) for m in CANDIDATES)
    print(header)
    print("-" * len(header))
    zeros = {m: 0 for m in CANDIDATES}
    floods = {m: 0 for m in CANDIDATES}
    for q in QUERIES:
        row = q.ljust(22)
        for m in CANDIDATES:
            c = count(q, m)
            zeros[m] += c == 0
            floods[m] += c > corpus * 0.3
            row += str(c).rjust(9)
        print(row)

    print("-" * len(header))
    print("0건 검색어".ljust(22) + "".join(str(zeros[m]).rjust(9) for m in CANDIDATES))
    print("전체30%초과".ljust(22) + "".join(str(floods[m]).rjust(9) for m in CANDIDATES))
    print(f"\n색인 문서 {corpus}건 기준")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
