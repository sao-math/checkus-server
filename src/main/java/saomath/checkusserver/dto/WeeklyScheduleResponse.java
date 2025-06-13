package saomath.checkusserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "주간 시간표 응답")
public class WeeklyScheduleResponse {

    @Schema(description = "시간표 ID", example = "1")
    private Long id;

    @Schema(description = "학생 ID", example = "1")
    private Long studentId;

    @Schema(description = "일정 이름", example = "수학 공부")
    private String title;

    @Schema(description = "활동 ID", example = "1")
    private Long activityId;

    @Schema(description = "활동 이름", example = "자습")
    private String activityName;

    @Schema(description = "학습 시간 할당 가능 여부", example = "true")
    private Boolean isStudyAssignable;

    @Schema(description = "요일 (1=월요일, 2=화요일, ..., 7=일요일)", example = "1")
    private Integer dayOfWeek;

    @Schema(description = "요일 이름", example = "월요일")
    private String dayOfWeekName;

    @Schema(description = "시작 시간", example = "09:00:00")
    private LocalTime startTime;

    @Schema(description = "종료 시간", example = "10:30:00")
    private LocalTime endTime;
}
