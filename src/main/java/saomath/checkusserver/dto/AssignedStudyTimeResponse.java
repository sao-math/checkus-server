package saomath.checkusserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "배정된 공부 시간 응답")
public class AssignedStudyTimeResponse {
    
    @Schema(description = "배정 ID", example = "1")
    private Long id;
    
    @Schema(description = "학생 ID", example = "1")
    private Long studentId;
    
    @Schema(description = "학생 이름", example = "김학생")
    private String studentName;
    
    @Schema(description = "일정 제목", example = "수학 공부")
    private String title;
    
    @Schema(description = "활동 ID", example = "1")
    private Long activityId;
    
    @Schema(description = "활동 이름", example = "수학 공부")
    private String activityName;
    
    @Schema(description = "학습 시간 할당 가능 여부", example = "true")
    private Boolean isStudyAssignable;
    
    @Schema(description = "시작 시간", example = "2025-06-01T10:00:00")
    private LocalDateTime startTime;
    
    @Schema(description = "종료 시간", example = "2025-06-01T12:00:00")
    private LocalDateTime endTime;
    
    @Schema(description = "배정한 선생님 ID", example = "2")
    private Long assignedBy;
    
    @Schema(description = "배정한 선생님 이름", example = "이선생")
    private String assignedByName;
}
