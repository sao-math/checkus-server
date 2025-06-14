package saomath.checkusserver.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "알림 테스트 요청")
public class NotificationTestRequest {
    
    @NotBlank(message = "템플릿 ID는 필수입니다.")
    @Schema(description = "알림 템플릿 ID", example = "STUDY_START", required = true)
    private String templateId;
    
    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 전화번호 형식이 아닙니다.")
    @Schema(description = "수신자 전화번호", example = "01012345678", required = true)
    private String phoneNumber;
}
