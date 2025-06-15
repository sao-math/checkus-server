package saomath.checkusserver.notification.channel;

import saomath.checkusserver.notification.dto.NotificationMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 알림 채널 인터페이스
 * 다양한 플랫폼(디스코드, 카카오톡 등)에 대한 추상화
 */
public interface NotificationChannel {
    
    /**
     * 채널 유형 반환
     */
    String getChannelType();
    
    /**
     * 채널이 활성화되어 있는지 확인
     */
    boolean isEnabled();
    
    /**
     * 단일 메시지 전송
     * @param message 전송할 메시지
     * @return 전송 성공 여부
     */
    CompletableFuture<Boolean> sendMessage(NotificationMessage message);
    
    /**
     * 여러 메시지 일괄 전송
     * @param messages 전송할 메시지 목록
     * @return 성공한 전송 수
     */
    CompletableFuture<Integer> sendBatchMessages(List<NotificationMessage> messages);
    
    /**
     * 수신자 ID 유효성 검증
     * @param recipientId 수신자 ID
     * @return 유효 여부
     */
    boolean validateRecipient(String recipientId);
    
    /**
     * 채널별 메시지 포맷팅 지원 여부
     * @return 포맷팅 지원 여부
     */
    default boolean supportsRichFormatting() {
        return false;
    }
    
    /**
     * 채널별 우선순위 처리 지원 여부
     * @return 우선순위 지원 여부
     */
    default boolean supportsPriority() {
        return false;
    }
} 