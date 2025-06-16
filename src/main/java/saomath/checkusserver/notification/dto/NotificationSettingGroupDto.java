package saomath.checkusserver.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 그룹화된 알림 설정 DTO (간소화됨)
 * 하나의 알림 유형에 대해 여러 전송 방법을 지원
 * isEnabled 필드 제거 - 개별 채널 설정으로 판단
 */
@Data
@Builder
public class NotificationSettingGroupDto {
    /**
     * 알림 유형 정보 (간소화됨)
     */
    private NotificationTypeDto notificationType;
    
    /**
     * 전송 방법별 설정 (key: 채널명, value: 설정)
     */
    private Map<String, NotificationSettingDto> deliveryMethods;
    
    /**
     * 공통 사전 알림 시간 (분)
     */
    private int advanceMinutes;
} 