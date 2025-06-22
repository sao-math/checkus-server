package saomath.checkusserver.notification.event;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

/**
 * 알 수 없는 사용자가 음성 채널에 입장했을 때 발생하는 이벤트
 */
@Getter
@Builder
public class UnknownUserJoinEvent {
    private final String discordUserId;        // 디스코드 사용자 ID
    private final String discordUsername;      // 디스코드 사용자명
    private final String discordDisplayName;   // 디스코드 표시명
    private final String guildId;              // 디스코드 서버 ID
    private final String guildName;            // 디스코드 서버명
    private final String channelId;            // 음성 채널 ID
    private final String channelName;          // 음성 채널명
    private final LocalDateTime joinTime;      // 입장 시간
    private final int currentChannelMembers;   // 현재 채널 인원 수
} 