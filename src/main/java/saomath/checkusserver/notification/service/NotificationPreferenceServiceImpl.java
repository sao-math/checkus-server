package saomath.checkusserver.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 알림 설정 조회 서비스 구현체
 * TODO: NotificationSetting 엔티티 구현 후 실제 DB 조회로 변경
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {
    
    private final UserRepository userRepository;
    
    @Override
    public List<NotificationPreference> getUserPreferences(Long userId, String templateId) {
        List<NotificationPreference> preferences = new ArrayList<>();
        
        // 사용자 조회
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return preferences;
        }
        
        // TODO: 실제 DB에서 알림 설정 조회
        // 임시로 모든 사용자에게 알림톡 활성화
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            preferences.add(NotificationPreference.builder()
                .userId(userId)
                .channel(NotificationService.NotificationChannel.ALIMTALK)
                .recipient(user.getPhoneNumber())
                .enabled(true)
                .build());
        }
        
        // 디스코드 ID가 있으면 디스코드 알림도 활성화
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
        // TODO: 실제 구현
        return getUserPreferences(userId, null);
    }
}
