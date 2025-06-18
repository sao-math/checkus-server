package saomath.checkusserver.notification.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 개별 알림 설정 DTO (간소화됨)
 * 그룹 내에서 사용되므로 중복 정보 제거
 */
@Data
@Builder
public class NotificationSettingDto {
    /**
     * 설정 ID (업데이트용, 없으면 null)
     */
    private String id;
    
    /**
     * 해당 채널의 활성화 여부
     */
    private boolean enabled;
} 