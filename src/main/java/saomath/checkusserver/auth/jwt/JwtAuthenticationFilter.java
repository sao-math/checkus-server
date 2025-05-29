package saomath.checkusserver.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.info("Processing request: {} {}", request.getMethod(), requestURI);

        try {
            String jwt = getJwtFromRequest(request);
            log.info("JWT from request: {}", jwt != null ? "present" : "absent");

            if (StringUtils.hasText(jwt)) {
                log.info("Validating JWT token");
                // JWT 검증을 단계별로 수행
                if (jwtTokenProvider.validateToken(jwt)) {
                    log.info("JWT token is valid");
                    if (jwtTokenProvider.isAccessToken(jwt)) {
                        log.info("JWT is access token, setting authentication");
                        setAuthentication(request, jwt);
                    } else {
                        log.info("JWT is not access token");
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    log.info("Invalid or expired JWT token");
                    SecurityContextHolder.clearContext();
                }
            } else {
                log.info("No JWT token found in request");
            }

        } catch (io.jsonwebtoken.JwtException ex) {
            // JWT 관련 예외만 명시적으로 처리
            log.warn("JWT processing failed: {}", ex.getMessage());
            SecurityContextHolder.clearContext();

        } catch (Exception ex) {
            // 기타 예외는 로그만 남기고 계속 진행
            log.error("Unexpected error in JWT filter", ex);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(HttpServletRequest request, String jwt) {
        log.info("Starting setAuthentication with JWT");
        try {
            log.info("Extracting data from JWT token");
            Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
            String username = jwtTokenProvider.getUsernameFromToken(jwt);
            List<String> roles = jwtTokenProvider.getRolesFromToken(jwt);

            log.info("JWT token data: userId={}, username={}, roles={}", userId, username, roles);

            if (userId == null) {
                log.warn("User ID not found in JWT token, falling back to username-only authentication");
                // 기존 방식으로 fallback
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Set fallback authentication with username: {}", username);
                return;
            }

            log.info("Creating authorities for roles: {}", roles);
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            log.info("Creating CustomUserPrincipal with userId: {}, username: {}", userId, username);
            // CustomUserPrincipal 객체 생성
            saomath.checkusserver.auth.CustomUserPrincipal userPrincipal = 
                new saomath.checkusserver.auth.CustomUserPrincipal(
                    userId,
                    username,
                    null, // password는 JWT에서 제공하지 않음
                    null, // name도 JWT에서 제공하지 않음 (필요시 추가 가능)
                    authorities,
                    true, // enabled
                    true, // accountNonExpired
                    true, // credentialsNonExpired
                    true  // accountNonLocked
                );

            log.info("Created CustomUserPrincipal: ID={}, Username={}", userPrincipal.getId(), userPrincipal.getUsername());

            log.info("Creating UsernamePasswordAuthenticationToken");
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userPrincipal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            log.info("Setting authentication in SecurityContext");
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("Successfully authenticated user: {} (ID: {}) with roles: {}", username, userId, roles);

        } catch (Exception ex) {
            log.error("Failed to set authentication: {}", ex.getMessage(), ex);
            SecurityContextHolder.clearContext();
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}