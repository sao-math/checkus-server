package saomath.checkusserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("local")
public class DevSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll()  // 모든 요청 허용
                )
                .csrf(AbstractHttpConfigurer::disable)  // CSRF 완전 비활성화
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable())  // X-Frame-Options 비활성화
                );

        return http.build();
    }
}