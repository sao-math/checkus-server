package saomath.checkusserver.notification.service;

import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 알림 설정
 */
@Getter
@Builder
public class NotificationPreference {
    private Long userId;
    private NotificationService.NotificationChannel channel;
    private String recipient;  // 채널별 수신자 정보 (전화번호, 디스코드 ID 등)
    private boolean enabled;
}
