package saomath.checkusserver.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import saomath.checkusserver.discord.service.DiscordBotService;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.util.DateTimeUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 디스코드 DM 전송 서비스 (NotificationService 구현체)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "discord.bot.enabled", havingValue = "true")
public class DiscordNotificationService implements NotificationService {
    
    private final DiscordBotService discordBotService;
    
    @Override
    public CompletableFuture<Boolean> sendNotification(String recipient, String templateId, Map<String, String> variables) {
        try {
            // 템플릿 메시지 생성
            String message = createMessageFromTemplate(templateId, variables);
            
            // 디스코드 DM 전송
            return discordBotService.sendDirectMessage(recipient, message);
            
        } catch (Exception e) {
            log.error("디스코드 알림 전송 중 오류 발생", e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    @Override
    public CompletableFuture<Integer> sendBulkNotification(String[] recipients, String templateId, Map<String, String> variables) {
        try {
            // 템플릿 메시지 생성
            String message = createMessageFromTemplate(templateId, variables);
            
            // 디스코드 브로드캐스트
            return discordBotService.sendBroadcastMessage(Arrays.asList(recipients), message);
            
        } catch (Exception e) {
            log.error("디스코드 대량 알림 전송 중 오류 발생", e);
            return CompletableFuture.completedFuture(0);
        }
    }
    
    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.DISCORD;
    }
    
    /**
     * 템플릿과 변수를 사용해 메시지 생성
     */
    private String createMessageFromTemplate(String templateId, Map<String, String> variables) {
        try {
            // AlimtalkTemplate enum 활용
            AlimtalkTemplate template = AlimtalkTemplate.valueOf(templateId);
            String message = template.getTemplateMessage();
            
            // 변수 복사본 생성 (원본 수정 방지)
            Map<String, String> processedVars = new HashMap<>(variables);
            
            // 입장시간이 있으면 한국 시간으로 변환
            if (processedVars.containsKey("입장시간")) {
                String entryTimeStr = processedVars.get("입장시간");
                try {
                    LocalDateTime entryTime = LocalDateTime.parse(entryTimeStr);
                    processedVars.put("입장시간", DateTimeUtils.formatToKoreanTime(entryTime));
                } catch (Exception e) {
                    // 파싱 실패시 원본 사용
                }
            }
            
            // 변수 치환
            for (Map.Entry<String, String> entry : processedVars.entrySet()) {
                String placeholder = "#{" + entry.getKey() + "}";
                message = message.replace(placeholder, entry.getValue());
            }
            
            // 디스코드 마크다운 형식으로 변환
            return convertToDiscordMarkdown(message);
            
        } catch (IllegalArgumentException e) {
            log.error("잘못된 템플릿 ID: {}", templateId);
            return "알림 메시지";
        }
    }
    
    /**
     * 메시지를 디스코드 마크다운 형식으로 변환
     */
    private String convertToDiscordMarkdown(String message) {
        // [체크어스] 태그를 볼드체로
        message = message.replace("[사오수학]", "**[사오수학]**");
        
        // 구분선 추가
        message = message.replace("\n\n", "\n━━━━━━━━━━━━━━━━━━━━━━\n");
        
        return message;
    }
}
