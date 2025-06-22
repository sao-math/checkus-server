package saomath.checkusserver.studyTime.dto;

import lombok.Data;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
@Schema(description = "공부 시간 배정 수정 요청")
public class UpdateStudyTimeRequest {
    
    @Size(max = 255, message = "일정 제목은 255자를 초과할 수 없습니다")
    @Schema(description = "일정 제목", example = "수학 공부")
    private String title;
    
    @Schema(description = "활동 ID", example = "2")
    private Long activityId;
    
    @Schema(description = "시작 시간", example = "2025-06-01T11:00:00")
    private LocalDateTime startTime;
    
    @Schema(description = "종료 시간", example = "2025-06-01T13:00:00")
    private LocalDateTime endTime;
}
