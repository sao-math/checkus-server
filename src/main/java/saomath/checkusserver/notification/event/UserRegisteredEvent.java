package saomath.checkusserver.notification.event;

import lombok.Getter;
import saomath.checkusserver.auth.domain.User;

/**
 * 사용자 등록 완료 이벤트
 * 트랜잭션 커밋 후 Discord 채널 확인을 위한 이벤트
 */
@Getter
public class UserRegisteredEvent {
    private final User user;
    private final String eventType; // "REGISTER" 또는 "DISCORD_ID_UPDATE"
    private final String oldDiscordId; // Discord ID 변경 시에만 사용

    public UserRegisteredEvent(User user, String eventType) {
        this.user = user;
        this.eventType = eventType;
        this.oldDiscordId = null;
    }

    public UserRegisteredEvent(User user, String eventType, String oldDiscordId) {
        this.user = user;
        this.eventType = eventType;
        this.oldDiscordId = oldDiscordId;
    }
} 