package saomath.checkusserver.notification.event;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

/**
 * 스터디룸 입장 이벤트
 */
@Getter
@Builder
public class StudyRoomEnterEvent {
    private final Long studentId;
    private final String studentName;
    private final String discordId;
    private final LocalDateTime enterTime;
    private final String channelName;
}
