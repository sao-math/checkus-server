package saomath.checkusserver.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 멀티 채널 알림 서비스
 * 사용자 설정에 따라 알림톡, 디스코드 등 여러 채널로 알림 전송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiChannelNotificationService {
    
    private final List<NotificationService> notificationServices;
    private final NotificationPreferenceService preferenceService;
    
    /**
     * 사용자의 알림 설정에 따라 적절한 채널로 알림 전송
     */
    public CompletableFuture<Boolean> sendNotification(Long userId, String templateId, Map<String, String> variables) {
        // 사용자의 알림 채널 설정 조회
        List<NotificationPreference> preferences = preferenceService.getUserPreferences(userId, templateId);
        
        if (preferences.isEmpty()) {
            log.debug("사용자 {}의 알림 설정이 없습니다.", userId);
            return CompletableFuture.completedFuture(false);
        }
        
        // 각 채널로 알림 전송
        List<CompletableFuture<Boolean>> futures = preferences.stream()
            .map(pref -> sendToChannel(pref, templateId, variables))
            .collect(Collectors.toList());
        
        // 모든 알림 전송 완료 후 하나라도 성공하면 true 반환
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .anyMatch(success -> success));
    }
    
    /**
     * 특정 채널로 알림 전송
     */
    private CompletableFuture<Boolean> sendToChannel(NotificationPreference preference, 
                                                    String templateId, 
                                                    Map<String, String> variables) {
        // 해당 채널의 NotificationService 찾기
        NotificationService service = notificationServices.stream()
            .filter(s -> s.getChannel() == preference.getChannel())
            .findFirst()
            .orElse(null);
        
        if (service == null) {
            log.warn("알림 채널 {}에 대한 서비스를 찾을 수 없습니다.", preference.getChannel());
            return CompletableFuture.completedFuture(false);
        }
        
        // 알림 전송
        return service.sendNotification(preference.getRecipient(), templateId, variables)
            .exceptionally(ex -> {
                log.error("알림 전송 실패 - 채널: {}, 수신자: {}", 
                    preference.getChannel(), preference.getRecipient(), ex);
                return false;
            });
    }
    
    /**
     * 특정 채널로만 알림 전송
     */
    public CompletableFuture<Boolean> sendNotificationToChannel(String recipient, 
                                                               String templateId, 
                                                               Map<String, String> variables,
                                                               NotificationService.NotificationChannel channel) {
        NotificationService service = notificationServices.stream()
            .filter(s -> s.getChannel() == channel)
            .findFirst()
            .orElse(null);
        
        if (service == null) {
            log.warn("알림 채널 {}에 대한 서비스를 찾을 수 없습니다.", channel);
            return CompletableFuture.completedFuture(false);
        }
        
        return service.sendNotification(recipient, templateId, variables);
    }
}
