# 버전은 Spring Boot 가 관리하는 클라이언트 버전과 맞춘다.
# Spring Boot 3.5.16 → spring-data-elasticsearch 5.5.13 → elasticsearch-java 8.18.8
FROM docker.elastic.co/elasticsearch/elasticsearch:8.18.8

# 한국어 형태소 분석기. 공식 이미지에는 없어서 직접 설치한다.
# --batch : 설치 중 뜨는 확인 프롬프트를 자동 승인 (없으면 빌드가 멈춘다)
RUN bin/elasticsearch-plugin install --batch analysis-nori
