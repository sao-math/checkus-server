package saomath.checkusserver.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "주간 시간표 등록/수정 요청")
public class WeeklyScheduleRequest {

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

    @NotNull(message = "요일은 필수입니다")
    @Min(value = 1, message = "요일은 1(월요일)부터 7(일요일) 사이여야 합니다")
    @Max(value = 7, message = "요일은 1(월요일)부터 7(일요일) 사이여야 합니다")
    @Schema(description = "요일 (1=월요일, 2=화요일, ..., 7=일요일)", example = "1")
    private Integer dayOfWeek;

    @NotNull(message = "시작 시간은 필수입니다")
    @Schema(description = "시작 시간", example = "09:00:00")
    private LocalTime startTime;

    @NotNull(message = "종료 시간은 필수입니다")
    @Schema(description = "종료 시간", example = "10:30:00")
    private LocalTime endTime;
}
