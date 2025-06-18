package saomath.checkusserver.notification.service;

import saomath.checkusserver.notification.dto.NotificationSettingGroupDto;
import saomath.checkusserver.notification.dto.NotificationSettingUpdateDto;

import java.util.List;

/**
 * 사용자 알림 설정 조회 및 관리 서비스
 */
public interface NotificationPreferenceService {
    
    /**
     * 사용자의 특정 알림에 대한 채널 설정 조회
     * @param userId 사용자 ID
     * @param templateId 알림 템플릿 ID
     * @return 활성화된 알림 채널 목록
     */
    List<NotificationPreference> getUserPreferences(Long userId, String templateId);
    
    /**
     * 사용자의 모든 알림 설정 조회
     * @param userId 사용자 ID
     * @return 모든 알림 설정 목록
     */
    List<NotificationPreference> getAllUserPreferences(Long userId);
    
    /**
     * 그룹화된 알림 설정 조회 (UI용)
     * 각 알림 템플릿별로 채널 설정을 그룹화하여 반환
     * @param userId 사용자 ID
     * @return 그룹화된 알림 설정 목록
     */
    List<NotificationSettingGroupDto> getGroupedNotificationSettings(Long userId);
    
    /**
     * 특정 알림 설정 생성/업데이트
     * @param userId 사용자 ID
     * @param templateId 알림 템플릿 ID
     * @param deliveryMethod 전송 방법 (alimtalk, discord 등)
     * @param updateDto 업데이트할 설정 정보
     */
    void updateNotificationSetting(Long userId, String templateId, String deliveryMethod, NotificationSettingUpdateDto updateDto);

    /**
     * 특정 알림 설정 존재 여부 확인
     * @param userId 사용자 ID
     * @param templateId 알림 템플릿 ID
     * @param deliveryMethod 전송 방법
     * @return 설정 존재 여부
     */
    boolean hasNotificationSetting(Long userId, String templateId, String deliveryMethod);
}
