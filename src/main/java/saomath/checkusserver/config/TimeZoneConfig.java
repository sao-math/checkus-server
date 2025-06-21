package saomath.checkusserver.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {
    
    @PostConstruct
    public void init() {
        // 애플리케이션 전역 시간대를 UTC로 설정
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
