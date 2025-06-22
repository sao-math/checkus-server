package saomath.checkusserver.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "알림 템플릿 정보")
public class NotificationTemplateDto {
    
    @Schema(description = "템플릿 ID", example = "STUDY_REMINDER_10MIN")
    private String id;
    
    @Schema(description = "템플릿 이름", example = "공부 시작 10분 전 알림")
    private String name;
    
    @Schema(description = "템플릿 설명", example = "공부 시작 10분 전 알림")
    private String description;
    
    @Schema(description = "템플릿 미리보기 (변수 치환 전)")
    private String previewMessage;
    
    @Schema(description = "필요한 변수 목록")
    private List<String> requiredVariables;
}
