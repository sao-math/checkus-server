package saomath.checkusserver.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 알림톡 전송 서비스 (NotificationService 구현체)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlimtalkNotificationService implements NotificationService {
    
    private final AlimtalkService alimtalkService;
    
    @Override
    public CompletableFuture<Boolean> sendNotification(String recipient, String templateId, Map<String, String> variables) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AlimtalkTemplate template = AlimtalkTemplate.valueOf(templateId);
                return alimtalkService.sendAlimtalk(recipient, template, variables);
            } catch (IllegalArgumentException e) {
                log.error("잘못된 템플릿 ID: {}", templateId);
                return false;
            } catch (Exception e) {
                log.error("알림톡 전송 중 오류 발생", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Integer> sendBulkNotification(String[] recipients, String templateId, Map<String, String> variables) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AlimtalkTemplate template = AlimtalkTemplate.valueOf(templateId);
                return alimtalkService.sendBulkAlimtalk(recipients, template, variables);
            } catch (IllegalArgumentException e) {
                log.error("잘못된 템플릿 ID: {}", templateId);
                return 0;
            } catch (Exception e) {
                log.error("알림톡 대량 전송 중 오류 발생", e);
                return 0;
            }
        });
    }
    
    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.ALIMTALK;
    }
}
