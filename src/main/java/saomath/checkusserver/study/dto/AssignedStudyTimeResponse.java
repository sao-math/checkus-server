package saomath.checkusserver.study.dto;

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

    //todo null로 날라옴
    //{
    //    "success": true,
    //    "message": "공부 시간이 성공적으로 배정되었습니다.",
    //    "data": {
    //        "id": 4,
    //        "studentId": 4,
    //        "studentName": null,
    //        "title": "영어 자습",
    //        "activityId": 2,
    //        "activityName": null,
    //        "isStudyAssignable": null,
    //        "startTime": "2025-06-25T07:00:00",
    //        "endTime": "2025-06-25T09:00:00",
    //        "assignedBy": 1,
    //        "assignedByName": null
    //    }
    //}
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
