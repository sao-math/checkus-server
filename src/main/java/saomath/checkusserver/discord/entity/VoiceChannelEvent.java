package saomath.checkusserver.discord.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 음성채널 참여 이벤트 정보를 담는 엔티티
 */
@Data
@Builder
public class VoiceChannelEvent {
    
    /** 이벤트 유형 */
    public enum EventType {
        JOIN,    // 입장
        LEAVE,   // 퇴장
        MOVE     // 이동
    }
    
    private String userId;              // 디스코드 사용자 ID
    private String username;            // 사용자명
    private String displayName;         // 서버에서의 표시명
    private String guildId;             // 서버(길드) ID
    private String guildName;           // 서버명
    private String channelId;           // 음성채널 ID
    private String channelName;         // 음성채널명
    private String previousChannelId;   // 이전 채널 ID (이동 시에만)
    private String previousChannelName; // 이전 채널명 (이동 시에만)
    private EventType eventType;        // 이벤트 유형
    private LocalDateTime timestamp;    // 이벤트 발생 시간
    private int currentChannelMembers;  // 현재 채널 인원 수
    
    /**
     * 현재 시간으로 타임스탬프 설정
     */
    public static class VoiceChannelEventBuilder {
        public VoiceChannelEventBuilder withCurrentTimestamp() {
            this.timestamp = LocalDateTime.now();
            return this;
        }
    }
}