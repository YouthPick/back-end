package com.bop.youthpick.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 정책 검색(Elasticsearch) 설정. {@code YouthpickApplication}의 {@code @ConfigurationPropertiesScan}이
 * 자동 등록하므로 별도 등록 코드가 필요 없다.
 *
 * <p>{@code enabled}가 false면 ES를 아예 호출하지 않고 기존 LIKE 검색만 쓴다 — ES가 없는 환경(테스트·CI)에서도
 * 앱이 그대로 뜨게 하기 위함이며, {@code youthpick.sync.scheduler.enabled}와 같은 "안전 기본값 off" 방식이다.
 *
 * <p>{@code alias}는 코드가 보는 인덱스 이름이다. 실제 인덱스는 policy_v1, policy_v2… 이고 이 alias가 그중 하나를
 * 가리킨다. 분석기 설정은 색인 시점에 굳으므로 매핑을 바꾸면 재색인이 필수인데, alias가 있으면 새 인덱스를 채운 뒤
 * alias만 옮겨 검색 중단 없이 전환할 수 있다.
 */
@ConfigurationProperties(prefix = "youthpick.search")
public record PolicySearchProperties(boolean enabled, String alias) {}
