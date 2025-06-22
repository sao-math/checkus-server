package saomath.checkusserver.studyTime.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "활동 응답")
public class ActivityResponse {
    
    @Schema(description = "활동 ID", example = "1")
    private Long id;
    
    @Schema(description = "활동 이름", example = "수학 공부")
    private String name;
    
    @Schema(description = "공부 시간 배정 가능 여부", example = "true")
    private Boolean isStudyAssignable;
}
