package saomath.checkusserver.notification.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 개별 알림 설정 DTO
 */
@Data
@Builder
public class NotificationSettingDto {
    private String id;
    private String userId;
    private String notificationTypeId;
    private boolean isEnabled;
    private String deliveryMethod;
    private int advanceMinutes;
    private NotificationTypeDto notificationType;
} 