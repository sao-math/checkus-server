package saomath.checkusserver.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import saomath.checkusserver.discord.service.DiscordBotService;
import saomath.checkusserver.notification.event.UserDiscordIdChangeEvent;

import java.time.format.DateTimeFormatter;

/**
 * Discord ID 변경 이벤트에 대한 알림 처리 리스너
 * Discord 채널에 사용자 ID 변경 관련 알림 메시지 전송
 * 
 * TODO: 향후 개선 사항
 * - Discord ID 변경 알림과 실제 음성채널 입장/퇴장 알림을 분리
 * - 현재는 ID 변경 시 하나의 통합 메시지를 보내지만,
 *   실제로는 두 개의 별개 이벤트로 처리하는 것이 더 적절:
 *   1) "사용자가 음성채널에서 나갔습니다" (실제 활동)
 *   2) "사용자의 Discord ID가 변경되었습니다" (관리 정보)
 * - 이렇게 분리하면 사용자들이 실제 음성채널 활동과 관리 작업을 구분해서 볼 수 있음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordIdChangeNotificationListener {

    private final DiscordBotService discordBotService;

    /**
     * Discord ID 변경 이벤트 처리
     * 음성채널에서 퇴장/입장 알림 메시지 전송
     */
    @Async
    @EventListener
    public void handleDiscordIdChange(UserDiscordIdChangeEvent event) {
        try {
            log.info("Discord ID 변경 알림 처리: 사용자={}, 변경 타입={}", 
                    event.getUser().getUsername(), event.getChangeType());

            String message = createChangeNotificationMessage(event);
            
            discordBotService.sendNotificationMessage(message)
                .thenAccept(success -> {
                    if (success) {
                        log.info("Discord ID 변경 알림 전송 성공: 사용자={}", event.getUser().getUsername());
                    } else {
                        log.warn("Discord ID 변경 알림 전송 실패: 사용자={}", event.getUser().getUsername());
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Discord ID 변경 알림 전송 중 오류 발생: 사용자={}", event.getUser().getUsername(), throwable);
                    return null;
                });

        } catch (Exception e) {
            log.error("Discord ID 변경 알림 처리 중 오류 발생: 사용자={}", event.getUser().getUsername(), e);
        }
    }

    /**
     * Discord ID 변경 알림 메시지 생성
     */
    private String createChangeNotificationMessage(UserDiscordIdChangeEvent event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        String emoji;
        String title;
        String details;
        
        switch (event.getChangeType()) {
            case ADDED:
                emoji = "🆕";
                title = "새로운 Discord ID 등록";
                details = String.format(
                    "• **새 Discord ID**: %s\n" +
                    "• **음성채널**: %s",
                    event.getNewDiscordId(),
                    event.getChannelName() != null ? event.getChannelName() : "없음"
                );
                break;
                
            case CHANGED:
                emoji = "🔄";
                title = "Discord ID 변경";
                details = String.format(
                    "• **이전 Discord ID**: %s\n" +
                    "• **새 Discord ID**: %s\n" +
                    "• **이전 음성채널**: %s",
                    event.getOldDiscordId(),
                    event.getNewDiscordId(),
                    event.getChannelName() != null ? event.getChannelName() : "없음"
                );
                break;
                
            case REMOVED:
                emoji = "❌";
                title = "Discord ID 제거";
                details = String.format(
                    "• **제거된 Discord ID**: %s\n" +
                    "• **이전 음성채널**: %s",
                    event.getOldDiscordId(),
                    event.getChannelName() != null ? event.getChannelName() : "없음"
                );
                break;
                
            default:
                emoji = "ℹ️";
                title = "Discord ID 변경";
                details = "상세 정보 없음";
        }
        
        return String.format(
            "%s **%s**\n\n" +
            "• **사용자**: %s (%s)\n" +
            "• **변경 시간**: %s\n" +
            "%s\n\n" +
            "📋 사용자의 Discord 연동 정보가 업데이트되었습니다.",
            emoji,
            title,
            event.getUser().getName(),
            event.getUser().getUsername(),
            event.getChangeTime().format(formatter),
            details
        );
    }
} 