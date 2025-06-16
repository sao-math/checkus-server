package saomath.checkusserver.notification.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 알림 유형 DTO
 */
@Data
@Builder
public class NotificationTypeDto {
    private String id;
    private String name;
    private String description;
} 