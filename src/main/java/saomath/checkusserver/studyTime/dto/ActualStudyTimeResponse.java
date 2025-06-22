package saomath.checkusserver.studyTime.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "실제 공부 시간 응답")
public class ActualStudyTimeResponse {
    
    @Schema(description = "실제 접속 기록 ID", example = "1")
    private Long id;
    
    @Schema(description = "학생 ID", example = "1")
    private Long studentId;
    
    @Schema(description = "학생 이름", example = "김학생")
    private String studentName;
    
    @Schema(description = "배정된 공부 시간 ID", example = "1")
    private Long assignedStudyTimeId;
    
    @Schema(description = "접속 시작 시간", example = "2025-06-01T10:05:00")
    private LocalDateTime startTime;
    
    @Schema(description = "접속 종료 시간", example = "2025-06-01T11:30:00")
    private LocalDateTime endTime;
    
    @Schema(description = "접속 소스", example = "discord")
    private String source;
}
