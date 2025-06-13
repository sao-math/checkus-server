package saomath.checkusserver.notification.formatter;

import saomath.checkusserver.notification.dto.NotificationMessage;

/**
 * 메시지 포맷터 인터페이스
 * 플랫폼별 메시지 포맷팅 추상화
 */
public interface MessageFormatter {
    
    /**
     * 지원하는 플랫폼 유형
     */
    String getPlatformType();
    
    /**
     * NotificationMessage를 플랫폼별 텍스트로 변환
     * @param notification 알림 메시지 데이터
     * @return 플랫폼에 맞는 포맷의 텍스트
     */
    String format(NotificationMessage notification);
    
    /**
     * 이모지 지원 여부
     */
    default boolean supportsEmoji() {
        return true;
    }
    
    /**
     * 마크다운 지원 여부
     */
    default boolean supportsMarkdown() {
        return false;
    }
    
    /**
     * 최대 메시지 길이
     */
    default int getMaxMessageLength() {
        return 2000; // 기본값
    }
} 