package com.bop.youthpick.policy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.policy.entity.Region;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class RegionRepositoryTest {

    @Autowired private RegionRepository regionRepository;

    @Test
    void 시도명_시군구명_순으로_한글_가나다_정렬한다() {
        regionRepository.save(Region.create("42000", "강원특별자치도", "강원특별자치도"));
        regionRepository.save(Region.create("42150", "강원특별자치도", "춘천시"));
        regionRepository.save(Region.create("42130", "강원특별자치도", "강릉시"));
        regionRepository.save(Region.create("11000", "서울특별시", "서울특별시"));
        regionRepository.save(Region.create("11740", "서울특별시", "강동구"));

        List<Region> result = regionRepository.findAllByOrderBySidoNameAscNameAsc();

        assertThat(result)
                .extracting(Region::getSidoName, Region::getName)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("강원특별자치도", "강릉시"),
                        org.assertj.core.groups.Tuple.tuple("강원특별자치도", "강원특별자치도"),
                        org.assertj.core.groups.Tuple.tuple("강원특별자치도", "춘천시"),
                        org.assertj.core.groups.Tuple.tuple("서울특별시", "강동구"),
                        org.assertj.core.groups.Tuple.tuple("서울특별시", "서울특별시"));
    }
}
