package saomath.checkusserver.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import saomath.checkusserver.entity.NotificationSetting;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.repository.NotificationSettingRepository;
import saomath.checkusserver.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 알림 설정 조회 서비스 구현체
 * DB 기반 알림 설정 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {
    
    private final UserRepository userRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    
    @Override
    public List<NotificationPreference> getUserPreferences(Long userId, String templateId) {
        List<NotificationPreference> preferences = new ArrayList<>();
        
        // 사용자 조회
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("사용자를 찾을 수 없습니다. userId: {}", userId);
            return preferences;
        }
        
        // DB에서 알림 설정 조회
        List<NotificationSetting> settings;
        if (templateId != null) {
            settings = notificationSettingRepository.findByUserIdAndTemplateNameAndIsEnabledTrue(userId, templateId);
        } else {
            settings = notificationSettingRepository.findByUserIdAndIsEnabledTrue(userId);
        }
        
        // DB에 설정이 있으면 그것을 사용
        if (!settings.isEmpty()) {
            for (NotificationSetting setting : settings) {
                String recipient = getRecipientForChannel(user, setting.getDeliveryMethod());
                if (recipient != null) {
                    preferences.add(NotificationPreference.builder()
                        .userId(userId)
                        .channel(mapDeliveryMethodToChannel(setting.getDeliveryMethod()))
                        .recipient(recipient)
                        .enabled(setting.getIsEnabled())
                        .build());
                }
            }
            return preferences;
        }
        
        // DB에 설정이 없으면 기본 설정 사용 (기존 로직 유지)
        log.debug("사용자 {}에 대한 알림 설정이 없어 기본 설정을 사용합니다.", userId);
        
        // 알림톡 기본 활성화 (전화번호가 있는 경우)
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            preferences.add(NotificationPreference.builder()
                .userId(userId)
                .channel(NotificationService.NotificationChannel.ALIMTALK)
                .recipient(user.getPhoneNumber())
                .enabled(true)
                .build());
        }
        
        // 디스코드 기본 활성화 (디스코드 ID가 있는 경우)
        if (user.getDiscordId() != null && !user.getDiscordId().isEmpty()) {
            preferences.add(NotificationPreference.builder()
                .userId(userId)
                .channel(NotificationService.NotificationChannel.DISCORD)
                .recipient(user.getDiscordId())
                .enabled(true)
                .build());
        }
        
        return preferences;
    }
    
    @Override
    public List<NotificationPreference> getAllUserPreferences(Long userId) {
        return getUserPreferences(userId, null);
    }
    
    /**
     * 채널에 따른 수신자 정보 조회
     */
    private String getRecipientForChannel(User user, String deliveryMethod) {
        switch (deliveryMethod.toLowerCase()) {
            case "alimtalk":
            case "kakao":
                return user.getPhoneNumber();
            case "discord":
                return user.getDiscordId();
            case "email":
                // 이메일 필드가 있다면 추가
                return null;
            case "sms":
                return user.getPhoneNumber();
            default:
                log.warn("알 수 없는 delivery method: {}", deliveryMethod);
                return null;
        }
    }
    
    /**
     * deliveryMethod를 NotificationChannel enum으로 매핑
     */
    private NotificationService.NotificationChannel mapDeliveryMethodToChannel(String deliveryMethod) {
        switch (deliveryMethod.toLowerCase()) {
            case "alimtalk":
            case "kakao":
                return NotificationService.NotificationChannel.ALIMTALK;
            case "discord":
                return NotificationService.NotificationChannel.DISCORD;
            case "email":
                return NotificationService.NotificationChannel.EMAIL;
            case "sms":
                return NotificationService.NotificationChannel.SMS;
            default:
                log.warn("알 수 없는 delivery method: {}, ALIMTALK으로 기본 설정", deliveryMethod);
                return NotificationService.NotificationChannel.ALIMTALK;
        }
    }
}
