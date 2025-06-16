package saomath.checkusserver.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 그룹화된 알림 설정 DTO
 * 하나의 알림 유형에 대해 여러 전송 방법을 지원
 */
@Data
@Builder
public class NotificationSettingGroupDto {
    private NotificationTypeDto notificationType;
    private boolean isEnabled;
    private Map<String, NotificationSettingDto> deliveryMethods;
    private int advanceMinutes;
} 