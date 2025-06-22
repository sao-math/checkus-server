package saomath.checkusserver.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import saomath.checkusserver.discord.service.DiscordBotService;
import saomath.checkusserver.notification.event.UnknownUserJoinEvent;

import java.time.format.DateTimeFormatter;

/**
 * 알 수 없는 사용자 입장 이벤트를 처리하는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnknownUserNotificationListener {

    private final DiscordBotService discordBotService;

    /**
     * 알 수 없는 사용자가 음성 채널에 입장했을 때 디스코드 채널에 알림 메시지 전송
     */
    @Async
    @EventListener
    public void handleUnknownUserJoin(UnknownUserJoinEvent event) {
        log.info("알 수 없는 사용자 입장 이벤트 처리: {}", event.getDiscordUsername());

        try {
            String message = createNotificationMessage(event);
            
            discordBotService.sendNotificationMessage(message)
                .thenAccept(success -> {
                    if (success) {
                        log.info("알 수 없는 사용자 알림 전송 성공: {}", event.getDiscordUsername());
                    } else {
                        log.warn("알 수 없는 사용자 알림 전송 실패: {}", event.getDiscordUsername());
                    }
                })
                .exceptionally(throwable -> {
                    log.error("알 수 없는 사용자 알림 전송 중 오류 발생: {}", event.getDiscordUsername(), throwable);
                    return null;
                });

        } catch (Exception e) {
            log.error("알 수 없는 사용자 알림 처리 중 오류 발생: {}", event.getDiscordUsername(), e);
        }
    }

    /**
     * 알림 메시지 생성
     */
    private String createNotificationMessage(UnknownUserJoinEvent event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        return String.format(
            "⚠️ **미등록 사용자 음성채널 입장 알림**\n\n" +
            "• **사용자**: %s (%s)\n" +
            "• **디스코드 ID**: %s\n" +
            "• **입장 채널**: %s\n" +
            "• **서버**: %s\n" +
            "• **입장 시간**: %s\n" +
            "• **현재 채널 인원**: %d명\n\n" +
            "📋 이 사용자는 시스템에 등록되어 있지 않습니다. 필요 시 사용자 등록을 진행해주세요.",
            event.getDiscordDisplayName(),
            event.getDiscordUsername(),
            event.getDiscordUserId(),
            event.getChannelName(),
            event.getGuildName(),
            event.getJoinTime().format(formatter),
            event.getCurrentChannelMembers()
        );
    }
} 