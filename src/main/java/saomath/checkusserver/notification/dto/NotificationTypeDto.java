package saomath.checkusserver.notification.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 알림 유형 DTO (간소화됨)
 * name 필드 제거 - id와 중복되므로 description만 유지
 */
@Data
@Builder
public class NotificationTypeDto {
    /**
     * 알림 유형 ID (예: D0001, S0001)
     */
    private String id;
    
    /**
     * 사용자에게 보여줄 설명
     */
    private String description;
} 