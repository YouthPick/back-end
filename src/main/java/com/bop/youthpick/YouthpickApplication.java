package com.bop.youthpick;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class YouthpickApplication {

    public static void main(String[] args) {
        // JPA auditing(@CreatedDate 등)과 관리자 API의 KST 기간 필터가 타임존 없는
        // LocalDateTime.now()를 공유 기준으로 삼으므로, JVM 기본 타임존을 기동 시점에
        // 고정한다(빈 초기화보다 먼저 실행되도록 SpringApplication.run 이전에 둔다).
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(YouthpickApplication.class, args);
    }
}
