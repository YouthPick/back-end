package com.bop.youthpick.policy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 지역(시군구) 마스터. 행안부 법정동코드 ~250행 1회 적재. 코드는 국가 표준 불변값이라 대리키 없이 code가 PK (통화코드 KRW와 같은 급). */
@Entity
@Table(name = "regions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

    @Id
    @Column(length = 10)
    private String code;

    @Column(name = "sido_name", length = 30, nullable = false)
    private String sidoName;

    @Column(length = 50, nullable = false)
    private String name;

    public static Region create(String code, String sidoName, String name) {
        Region region = new Region();
        region.code = code;
        region.sidoName = sidoName;
        region.name = name;
        return region;
    }
}
