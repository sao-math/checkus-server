package saomath.checkusserver.studyTime.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
@Schema(description = "공부 시간 배정 요청")
public class AssignStudyTimeRequest {
    
    @NotNull(message = "학생 ID는 필수입니다")
    @Schema(description = "학생 ID", example = "1")
    private Long studentId;
    
    @NotBlank(message = "일정 제목은 필수입니다")
    @Size(max = 255, message = "일정 제목은 255자를 초과할 수 없습니다")
    @Schema(description = "일정 제목", example = "수학 공부")
    private String title;
    
    @NotNull(message = "활동 ID는 필수입니다")
    @Schema(description = "활동 ID", example = "1")
    private Long activityId;
    
    @NotNull(message = "시작 시간은 필수입니다")
    @Schema(description = "시작 시간", example = "2025-06-01T10:00:00")
    private LocalDateTime startTime;
    
    @NotNull(message = "종료 시간은 필수입니다")
    @Schema(description = "종료 시간", example = "2025-06-01T12:00:00")
    private LocalDateTime endTime;
}
