package saomath.checkusserver.notification.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 알림 전송 서비스 인터페이스
 * 다양한 채널(알림톡, 디스코드 등)로 알림을 전송할 수 있도록 추상화
 */
public interface NotificationService {
    
    /**
     * 알림 전송
     * @param recipient 수신자 정보 (전화번호, 디스코드 ID 등)
     * @param templateId 템플릿 ID
     * @param variables 템플릿 변수
     * @return 전송 성공 여부
     */
    CompletableFuture<Boolean> sendNotification(String recipient, String templateId, Map<String, String> variables);
    
    /**
     * 대량 알림 전송
     * @param recipients 수신자 목록
     * @param templateId 템플릿 ID
     * @param variables 템플릿 변수
     * @return 성공한 전송 수
     */
    CompletableFuture<Integer> sendBulkNotification(String[] recipients, String templateId, Map<String, String> variables);
    
    /**
     * 알림 채널 타입
     */
    NotificationChannel getChannel();
    
    /**
     * 알림 채널 enum
     */
    enum NotificationChannel {
        ALIMTALK("알림톡"),
        DISCORD("디스코드"),
        EMAIL("이메일"),
        SMS("문자");
        
        private final String displayName;
        
        NotificationChannel(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
