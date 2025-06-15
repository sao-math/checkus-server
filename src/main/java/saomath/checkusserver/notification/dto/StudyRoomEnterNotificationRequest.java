package saomath.checkusserver.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "스터디룸 입장 알림 요청")
public class StudyRoomEnterNotificationRequest {
    
    @NotNull(message = "학생 ID는 필수입니다.")
    @Schema(description = "학생 ID", example = "1", required = true)
    private Long studentId;
    
    @Schema(description = "채널명", example = "수학 스터디룸")
    private String channelName;
}
