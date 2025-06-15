package saomath.checkusserver.notification.service;

import java.util.List;

/**
 * 사용자 알림 설정 조회 서비스
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
}
