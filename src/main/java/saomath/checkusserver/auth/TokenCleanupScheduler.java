package saomath.checkusserver.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenService refreshTokenService;

    /**
     * 매일 새벽 2시에 만료된 리프레시 토큰 정리
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        log.info("만료된 리프레시 토큰 정리 작업 시작");
        try {
            refreshTokenService.cleanupExpiredTokens();
            log.info("만료된 리프레시 토큰 정리 작업 완료");
        } catch (Exception e) {
            log.error("만료된 리프레시 토큰 정리 작업 실패", e);
        }
    }

    /**
     * 1시간마다 만료된 토큰 정리 (개발/테스트용)
     */
    @Scheduled(fixedRate = 3600000) // 1시간 = 3600000ms
    public void cleanupExpiredTokensHourly() {
        try {
            refreshTokenService.cleanupExpiredTokens();
        } catch (Exception e) {
            log.debug("토큰 정리 중 오류 발생 (무시 가능): {}", e.getMessage());
        }
    }
}
