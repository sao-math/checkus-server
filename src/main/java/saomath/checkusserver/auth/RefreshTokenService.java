package saomath.checkusserver.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.auth.jwt.JwtTokenProvider;
import saomath.checkusserver.entity.RefreshToken;
import saomath.checkusserver.common.exception.InvalidTokenException;
import saomath.checkusserver.repository.RefreshTokenRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 리프레시 토큰 저장
     */
    @Transactional
    public RefreshToken saveRefreshToken(String token, Long userId) {
        // 기존 사용자의 모든 리프레시 토큰 폐기
        refreshTokenRepository.revokeAllByUserId(userId);
        
        // 새 리프레시 토큰 저장
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUserId(userId);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7일 후 만료
        refreshToken.setIsRevoked(false);
        
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * 리프레시 토큰 유효성 검증 및 조회
     */
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String token) {
        // JWT 레벨 검증
        if (!jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isRefreshToken(token)) {
            throw new InvalidTokenException("유효하지 않은 리프레시 토큰입니다.");
        }

        // DB에서 토큰 조회
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndIsRevokedFalse(token)
                .orElseThrow(() -> new InvalidTokenException("리프레시 토큰을 찾을 수 없거나 폐기되었습니다."));

        // 만료 여부 확인
        if (refreshToken.isExpired()) {
            throw new InvalidTokenException("만료된 리프레시 토큰입니다.");
        }

        return refreshToken;
    }

    /**
     * 리프레시 토큰 폐기
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.revokeByToken(token);
    }

    /**
     * 사용자의 모든 리프레시 토큰 폐기
     */
    @Transactional
    public void revokeAllRefreshTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    /**
     * 만료된 토큰 정리 (스케줄러에서 사용)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        int deletedCount = refreshTokenRepository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired/revoked refresh tokens", deletedCount);
        }
    }

    /**
     * 리프레시 토큰 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean existsValidRefreshToken(String token) {
        return refreshTokenRepository.existsByTokenAndIsRevokedFalse(token);
    }
}
