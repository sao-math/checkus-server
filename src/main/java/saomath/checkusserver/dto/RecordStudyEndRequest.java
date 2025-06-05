package saomath.checkusserver.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
@Schema(description = "디스코드 봇용 공부 종료 기록 요청")
public class RecordStudyEndRequest {
    
    @NotNull(message = "종료 시간은 필수입니다")
    @Schema(description = "접속 종료 시간", example = "2025-06-01T11:30:00")
    private LocalDateTime endTime;
}
