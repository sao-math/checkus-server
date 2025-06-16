package saomath.checkusserver.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import saomath.checkusserver.entity.NotificationSetting;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.entity.UserRole;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.domain.DefaultNotificationSetting;
import saomath.checkusserver.repository.NotificationSettingRepository;
import saomath.checkusserver.repository.UserRepository;
import saomath.checkusserver.repository.UserRoleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 사용자 알림 설정 조회 서비스 구현체
 * 기본값 + 예외 저장 방식 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {
    
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
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
        
        // 사용자 역할 확인
        String userRole = getUserPrimaryRole(userId);
        DefaultNotificationSetting defaultSetting = DefaultNotificationSetting.getByRole(userRole);
        
        // 처리할 템플릿 목록 결정
        List<AlimtalkTemplate> templatesToProcess = getTemplatesToProcess(templateId);
        
        // 예외 설정 조회 (DB에서)
        Map<String, NotificationSetting> exceptionSettings = getExceptionSettings(userId, templateId);
        
        // 각 템플릿에 대해 최종 설정 생성
        for (AlimtalkTemplate template : templatesToProcess) {
            preferences.addAll(buildPreferencesForTemplate(user, template, defaultSetting, exceptionSettings));
        }
        
        return preferences;
    }
    
    @Override
    public List<NotificationPreference> getAllUserPreferences(Long userId) {
        return getUserPreferences(userId, null);
    }
    
    /**
     * 사용자의 주 역할 조회
     */
    private String getUserPrimaryRole(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserIdAndStatus(userId, UserRole.RoleStatus.ACTIVE);
        
        // 우선순위: STUDENT > GUARDIAN > TEACHER
        for (UserRole userRole : userRoles) {
            String roleName = userRole.getRole().getName();
            if ("STUDENT".equals(roleName)) {
                return "STUDENT";
            }
        }
        
        for (UserRole userRole : userRoles) {
            String roleName = userRole.getRole().getName();
            if ("GUARDIAN".equals(roleName)) {
                return "GUARDIAN";
            }
        }
        
        for (UserRole userRole : userRoles) {
            String roleName = userRole.getRole().getName();
            if ("TEACHER".equals(roleName)) {
                return "TEACHER";
            }
        }
        
        // 기본값은 STUDENT
        log.warn("사용자 {}의 활성 역할을 찾을 수 없어 STUDENT로 처리합니다.", userId);
        return "STUDENT";
    }
    
    /**
     * 처리할 템플릿 목록 결정
     */
    private List<AlimtalkTemplate> getTemplatesToProcess(String templateId) {
        if (templateId != null) {
            try {
                return List.of(AlimtalkTemplate.valueOf(templateId));
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 템플릿 ID: {}", templateId);
                return List.of();
            }
        }
        
        // 모든 템플릿 반환
        return List.of(AlimtalkTemplate.values());
    }
    
    /**
     * DB에서 예외 설정 조회
     */
    private Map<String, NotificationSetting> getExceptionSettings(Long userId, String templateId) {
        List<NotificationSetting> settings;
        if (templateId != null) {
            settings = notificationSettingRepository.findByUserIdAndTemplateName(userId, templateId);
        } else {
            settings = notificationSettingRepository.findByUserId(userId);
        }
        
        // templateName + deliveryMethod 를 key로 하는 Map 생성
        return settings.stream()
            .collect(Collectors.toMap(
                setting -> setting.getTemplateName() + ":" + setting.getDeliveryMethod(),
                setting -> setting,
                (existing, replacement) -> existing // 중복 시 기존 값 유지
            ));
    }
    
    /**
     * 특정 템플릿에 대한 최종 설정 생성
     */
    private List<NotificationPreference> buildPreferencesForTemplate(
            User user, 
            AlimtalkTemplate template, 
            DefaultNotificationSetting defaultSetting,
            Map<String, NotificationSetting> exceptionSettings) {
        
        List<NotificationPreference> preferences = new ArrayList<>();
        
        // 가능한 모든 채널 확인
        Set<String> allChannels = Set.of("alimtalk", "discord", "email", "sms");
        
        for (String channel : allChannels) {
            String key = template.name() + ":" + channel;
            NotificationSetting exceptionSetting = exceptionSettings.get(key);
            
            boolean isEnabled;
            if (exceptionSetting != null) {
                // DB에 예외 설정이 있으면 그것을 사용
                isEnabled = exceptionSetting.getIsEnabled();
            } else {
                // 예외 설정이 없으면 기본값 사용
                isEnabled = defaultSetting.isDefaultEnabled(template, channel);
            }
            
            // 활성화되어 있고, 사용자가 해당 채널을 사용할 수 있는 경우만 추가
            if (isEnabled && canUseChannel(user, channel)) {
                String recipient = getRecipientForChannel(user, channel);
                if (recipient != null) {
                    preferences.add(NotificationPreference.builder()
                        .userId(user.getId())
                        .channel(mapDeliveryMethodToChannel(channel))
                        .recipient(recipient)
                        .enabled(true)
                        .build());
                }
            }
        }
        
        return preferences;
    }
    
    /**
     * 사용자가 해당 채널을 사용할 수 있는지 확인
     */
    private boolean canUseChannel(User user, String channel) {
        switch (channel) {
            case "alimtalk":
            case "sms":
                return user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty();
            case "discord":
                return user.getDiscordId() != null && !user.getDiscordId().isEmpty();
            case "email":
                // 이메일 필드가 추가되면 여기에 추가
                return false;
            default:
                return false;
        }
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
