package saomath.checkusserver.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AdminProperties.class)
public class PropertiesConfig {
    // AdminProperties를 빈으로 등록
}