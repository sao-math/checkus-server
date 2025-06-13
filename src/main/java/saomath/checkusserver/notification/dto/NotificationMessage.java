package saomath.checkusserver.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 플랫폼에 무관한 알림 메시지 데이터
 */
@Data
@Builder
public class NotificationMessage {
    
    /**
     * 알림 유형
     */
    public enum NotificationType {
        UPCOMING_STUDY("공부 시작 10분 전"),
        STUDY_START("공부 시작 시간"),
        MISSED_STUDY("미접속 알림"),
        DAILY_TASK("오늘의 할일"),
        EARLY_LEAVE("조기 퇴장"),
        LATE_ARRIVAL("늦은 입장");
        
        private final String description;
        
        NotificationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 알림 유형
     */
    private NotificationType type;
    
    /**
     * 제목
     */
    private String title;
    
    /**
     * 메인 메시지
     */
    private String message;
    
    /**
     * 추가 데이터 (과목명, 시간 등)
     */
    private Map<String, Object> data;
    
    /**
     * 발송 시간
     */
    private LocalDateTime timestamp;
    
    /**
     * 우선순위 (1: 낮음, 2: 보통, 3: 높음)
     */
    private int priority;
    
    /**
     * 수신자 식별자 (플랫폼별로 다를 수 있음)
     */
    private String recipientId;
    
    /**
     * 수신자 이름
     */
    private String recipientName;
} 