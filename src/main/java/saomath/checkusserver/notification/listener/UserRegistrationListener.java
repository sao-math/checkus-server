package saomath.checkusserver.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import saomath.checkusserver.discord.service.VoiceChannelEventService;
import saomath.checkusserver.notification.event.UserRegisteredEvent;

/**
 * 사용자 등록 이벤트를 처리하는 리스너
 * 트랜잭션 커밋 후에 Discord 채널 확인을 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegistrationListener {

    private final VoiceChannelEventService voiceChannelEventService;

    /**
     * 트랜잭션 커밋 후 사용자 등록 이벤트 처리
     * Discord 채널에 있는지 확인하고 기록 시작
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegistered(UserRegisteredEvent event) {
        try {
            log.info("트랜잭션 커밋 후 Discord 채널 확인 시작: user={}, eventType={}", 
                    event.getUser().getUsername(), event.getEventType());

            if ("REGISTER".equals(event.getEventType())) {
                // 신규 가입자의 경우
                voiceChannelEventService.checkAndStartRecordingForNewUser(event.getUser());
                
            } else if ("DISCORD_ID_UPDATE".equals(event.getEventType())) {
                // Discord ID 변경의 경우
                checkAndStartRecordingForUpdatedDiscordId(event.getUser(), event.getOldDiscordId());
            }

        } catch (Exception e) {
            log.error("사용자 등록 후 Discord 채널 확인 중 오류 발생: user={}", 
                    event.getUser().getUsername(), e);
            // 오류가 발생해도 시스템 전체에는 영향 없음
        }
    }

    /**
     * Discord ID 업데이트 후 음성채널 확인 및 기록 시작
     */
    private void checkAndStartRecordingForUpdatedDiscordId(saomath.checkusserver.auth.domain.User user, String oldDiscordId) {
        try {
            String newDiscordId = user.getDiscordId();
            
            if (newDiscordId == null || newDiscordId.trim().isEmpty()) {
                log.debug("사용자 {}의 Discord ID가 제거되었습니다.", user.getUsername());
                return;
            }
            
            if (oldDiscordId == null || oldDiscordId.trim().isEmpty()) {
                log.info("사용자 {}에게 새로운 Discord ID가 추가되었습니다: {}", user.getUsername(), newDiscordId);
            } else {
                log.info("사용자 {}의 Discord ID가 변경되었습니다: {} -> {}", user.getUsername(), oldDiscordId, newDiscordId);
            }
            
            // 새로운 Discord ID로 현재 음성채널에 있는지 확인하고 기록 시작
            voiceChannelEventService.checkAndStartRecordingForNewUser(user);
            
        } catch (Exception e) {
            log.error("사용자 {}의 Discord ID 업데이트 후 음성채널 확인 중 오류 발생", user.getUsername(), e);
        }
    }
} 