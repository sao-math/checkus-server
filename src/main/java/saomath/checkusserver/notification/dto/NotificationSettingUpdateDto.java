package saomath.checkusserver.notification.dto;

import lombok.Data;

/**
 * 알림 설정 업데이트 요청 DTO
 */
@Data
public class NotificationSettingUpdateDto {
    private boolean enabled;
    private Integer advanceMinutes;
    
    public boolean isEnabled() {
        return enabled;
    }
} 