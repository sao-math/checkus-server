package saomath.checkusserver.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import saomath.checkusserver.discord.service.VoiceChannelEventService;
import saomath.checkusserver.notification.event.UserRegisteredEvent;
import saomath.checkusserver.studyTime.service.StudyTimeService;

import java.time.LocalDateTime;

/**
 * 사용자 등록 이벤트를 처리하는 리스너
 * 트랜잭션 커밋 후에 Discord 채널 확인을 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegistrationListener {

    private final VoiceChannelEventService voiceChannelEventService;
    private final StudyTimeService studyTimeService;

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
                handleDiscordIdUpdate(event.getUser(), event.getOldDiscordId());
            }

        } catch (Exception e) {
            log.error("사용자 등록 후 Discord 채널 확인 중 오류 발생: user={}", 
                    event.getUser().getUsername(), e);
            // 오류가 발생해도 시스템 전체에는 영향 없음
        }
    }

    /**
     * Discord ID 업데이트 처리
     * 1. 기존 Discord ID의 진행 중인 세션 종료
     * 2. 음성채널 상태 정리
     * 3. 새로운 Discord ID로 음성채널 확인 및 세션 시작
     */
    private void handleDiscordIdUpdate(saomath.checkusserver.auth.domain.User user, String oldDiscordId) {
        try {
            String newDiscordId = user.getDiscordId();
            
            log.info("Discord ID 업데이트 처리 시작: 사용자={}, 기존 ID={}, 새 ID={}", 
                    user.getUsername(), oldDiscordId, newDiscordId);
            
            // 1. 기존 Discord ID의 진행 중인 세션 종료
            if (oldDiscordId != null && !oldDiscordId.trim().isEmpty()) {
                log.info("기존 Discord ID {}의 진행 중인 스터디 세션을 종료합니다.", oldDiscordId);
                endActiveStudySessionsForUser(user.getId());
            }
            
            // 2. VoiceChannelEventService를 통한 종합 정리 (음성채널 상태 + 새 세션 시작)
            voiceChannelEventService.handleDiscordIdChangeCleanup(user.getId(), oldDiscordId, newDiscordId);
            
            // 3. 로그 정리
            if (newDiscordId == null || newDiscordId.trim().isEmpty()) {
                log.info("사용자 {}의 Discord ID가 제거되었습니다. 모든 관련 세션이 종료되었습니다.", user.getUsername());
            } else if (oldDiscordId == null || oldDiscordId.trim().isEmpty()) {
                log.info("사용자 {}에게 새로운 Discord ID가 추가되었습니다: {}", user.getUsername(), newDiscordId);
            } else {
                log.info("사용자 {}의 Discord ID 변경 처리가 완료되었습니다: {} -> {}", user.getUsername(), oldDiscordId, newDiscordId);
            }
            
        } catch (Exception e) {
            log.error("사용자 {}의 Discord ID 업데이트 처리 중 오류 발생", user.getUsername(), e);
        }
    }

    /**
     * 사용자의 진행 중인 모든 스터디 세션 종료
     */
    private void endActiveStudySessionsForUser(Long userId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            var endedSessions = studyTimeService.recordStudyEndByStudentId(userId, now);
            
            if (!endedSessions.isEmpty()) {
                log.info("Discord ID 변경으로 인해 사용자 ID {}의 {} 개 진행 중인 세션을 종료했습니다.", 
                        userId, endedSessions.size());
            } else {
                log.debug("사용자 ID {}의 진행 중인 세션이 없습니다.", userId);
            }
            
        } catch (Exception e) {
            log.error("사용자 ID {}의 진행 중인 세션 종료 중 오류 발생", userId, e);
        }
    }
} 