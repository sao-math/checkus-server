package saomath.checkusserver.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("mySecretKey12345678901234567890123456789012345678901234567890");
        jwtProperties.setAccessTokenExpiration(3600000L); // 1시간
        jwtProperties.setRefreshTokenExpiration(604800000L); // 7일

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    @DisplayName("액세스 토큰 생성 및 검증")
    void generateAndValidateAccessToken_Success() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        List<String> roles = Arrays.asList("STUDENT", "USER");

        // When
        String accessToken = jwtTokenProvider.generateAccessToken(userId, username, roles);

        // Then
        assertNotNull(accessToken);
        assertTrue(jwtTokenProvider.validateToken(accessToken));
        assertTrue(jwtTokenProvider.isAccessToken(accessToken));
        assertFalse(jwtTokenProvider.isRefreshToken(accessToken));

        assertEquals(username, jwtTokenProvider.getUsernameFromToken(accessToken));
        assertEquals(roles, jwtTokenProvider.getRolesFromToken(accessToken));
    }

    @Test
    @DisplayName("리프레시 토큰 생성 및 검증")
    void generateAndValidateRefreshToken_Success() {
        // Given
        String username = "testuser";

        // When
        String refreshToken = jwtTokenProvider.generateRefreshToken(username);

        // Then
        assertNotNull(refreshToken);
        assertTrue(jwtTokenProvider.validateToken(refreshToken));
        assertTrue(jwtTokenProvider.isRefreshToken(refreshToken));
        assertFalse(jwtTokenProvider.isAccessToken(refreshToken));

        assertEquals(username, jwtTokenProvider.getUsernameFromToken(refreshToken));
    }

    @Test
    @DisplayName("잘못된 토큰 검증 실패")
    void validateInvalidToken_Fail() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }

    @Test
    @DisplayName("토큰 만료 확인")
    void checkTokenExpiration() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        List<String> roles = Arrays.asList("STUDENT");

        // 매우 짧은 만료 시간 설정 (테스트용)
        jwtProperties.setAccessTokenExpiration(1L); // 1ms
        JwtTokenProvider shortExpiryProvider = new JwtTokenProvider(jwtProperties);

        String token = shortExpiryProvider.generateAccessToken(userId, username, roles);

        // When & Then
        // 토큰이 매우 빠르게 만료되므로 약간의 지연 후 확인
        try {
            Thread.sleep(10); // 10ms 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 만료된 토큰은 검증에 실패해야 함
        assertFalse(shortExpiryProvider.validateToken(token));
    }
}
