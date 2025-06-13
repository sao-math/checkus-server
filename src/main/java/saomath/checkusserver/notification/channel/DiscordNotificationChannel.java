package saomath.checkusserver.notification.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import saomath.checkusserver.discord.service.DiscordBotService;
import saomath.checkusserver.notification.dto.NotificationMessage;
import saomath.checkusserver.notification.formatter.MessageFormatter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 디스코드 알림 채널 구현
 */
@Slf4j
@Component
public class DiscordNotificationChannel implements NotificationChannel {
    
    private final DiscordBotService discordBotService;
    private final MessageFormatter messageFormatter;
    
    public DiscordNotificationChannel(
            DiscordBotService discordBotService,
            @Qualifier("discordMessageFormatter") MessageFormatter messageFormatter) {
        this.discordBotService = discordBotService;
        this.messageFormatter = messageFormatter;
    }
    
    @Override
    public String getChannelType() {
        return "DISCORD";
    }
    
    @Override
    public boolean isEnabled() {
        return discordBotService != null;
    }
    
    @Override
    public CompletableFuture<Boolean> sendMessage(NotificationMessage message) {
        if (!isEnabled()) {
            log.warn("디스코드 채널이 비활성화되어 있습니다.");
            return CompletableFuture.completedFuture(false);
        }
        
        if (!validateRecipient(message.getRecipientId())) {
            log.warn("유효하지 않은 디스코드 사용자 ID: {}", message.getRecipientId());
            return CompletableFuture.completedFuture(false);
        }
        
        try {
            // 메시지 포맷팅
            String formattedMessage = messageFormatter.format(message);
            
            // 메시지 길이 확인
            if (formattedMessage.length() > messageFormatter.getMaxMessageLength()) {
                log.warn("메시지가 너무 깁니다. 길이: {}, 최대: {}", 
                        formattedMessage.length(), messageFormatter.getMaxMessageLength());
                formattedMessage = truncateMessage(formattedMessage, messageFormatter.getMaxMessageLength());
            }
            
            // 디스코드 DM 전송
            return discordBotService.sendDirectMessage(message.getRecipientId(), formattedMessage);
            
        } catch (Exception e) {
            log.error("디스코드 메시지 전송 실패: 사용자={}, 알림 유형={}", 
                    message.getRecipientId(), message.getType(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    @Override
    public CompletableFuture<Integer> sendBatchMessages(List<NotificationMessage> messages) {
        if (!isEnabled()) {
            log.warn("디스코드 채널이 비활성화되어 있습니다.");
            return CompletableFuture.completedFuture(0);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            int successCount = 0;
            for (NotificationMessage message : messages) {
                try {
                    boolean success = sendMessage(message).get();
                    if (success) {
                        successCount++;
                    }
                    // Discord API 제한 고려 딜레이
                    Thread.sleep(100);
                } catch (Exception e) {
                    log.error("배치 전송 중 오류 발생: 사용자={}", message.getRecipientId(), e);
                }
            }
            
            log.info("디스코드 배치 전송 완료: 성공={}/{}", successCount, messages.size());
            return successCount;
        });
    }
    
    @Override
    public boolean validateRecipient(String recipientId) {
        if (recipientId == null || recipientId.trim().isEmpty()) {
            return false;
        }
        
        try {
            // 디스코드 사용자 ID는 숫자여야 함
            Long.parseLong(recipientId);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    @Override
    public boolean supportsRichFormatting() {
        return messageFormatter.supportsMarkdown();
    }
    
    @Override
    public boolean supportsPriority() {
        return false; // 디스코드 DM은 우선순위 미지원
    }
    
    /**
     * 메시지 길이 제한에 맞게 자르기
     */
    private String truncateMessage(String message, int maxLength) {
        if (message.length() <= maxLength) {
            return message;
        }
        
        String suffix = "\n\n... (메시지가 잘렸습니다)";
        int targetLength = maxLength - suffix.length();
        
        return message.substring(0, targetLength) + suffix;
    }
} 