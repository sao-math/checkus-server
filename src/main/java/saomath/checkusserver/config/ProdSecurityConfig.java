package saomath.checkusserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("prod")  // 운영 환경에서만 적용
public class ProdSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()  // 헬스체크 허용
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)  // REST API용 CSRF 비활성화
                .httpBasic(Customizer.withDefaults())  // 기본 HTTP 인증
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.deny())  // 운영에서는 iframe 차단
                        .contentTypeOptions(Customizer.withDefaults())
                        //.httpStrictTransportSecurity(hstsConfig -> hstsConfig
                        //        .maxAgeInSeconds(31536000)
                        //)
                );

        return http.build();
    }
}