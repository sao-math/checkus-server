package saomath.checkusserver.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
@Schema(description = "디스코드 봇용 공부 시작 기록 요청")
public class RecordStudyStartRequest {
    
    @NotNull(message = "학생 ID는 필수입니다")
    @Schema(description = "학생 ID", example = "1")
    private Long studentId;
    
    @NotNull(message = "시작 시간은 필수입니다")
    @Schema(description = "접속 시작 시간", example = "2025-06-01T10:05:00")
    private LocalDateTime startTime;
    
    @Schema(description = "접속 소스", example = "discord", defaultValue = "discord")
    private String source = "discord";
}
