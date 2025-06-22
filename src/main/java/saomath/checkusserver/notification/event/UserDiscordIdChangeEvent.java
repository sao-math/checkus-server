package saomath.checkusserver.notification.event;

import lombok.Builder;
import lombok.Getter;
import saomath.checkusserver.auth.domain.User;

import java.time.LocalDateTime;

/**
 * Discord ID 변경 이벤트
 * 사용자의 Discord ID가 변경되거나 삭제될 때 발행
 */
@Getter
@Builder
public class UserDiscordIdChangeEvent {
    private final User user;
    private final String oldDiscordId;
    private final String newDiscordId;
    private final String channelId;
    private final String channelName;
    private final LocalDateTime changeTime;
    private final ChangeType changeType;
    
    public enum ChangeType {
        ADDED,    // Discord ID 신규 추가
        CHANGED,  // Discord ID 변경
        REMOVED   // Discord ID 삭제
    }
} 