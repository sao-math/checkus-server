package saomath.checkusserver.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import saomath.checkusserver.notification.validation.ValidNotificationSendRequest;

@Getter
@Setter
@ValidNotificationSendRequest
@Schema(description = "직접 알림 발송 요청")
public class NotificationSendRequest {
    
    @NotNull(message = "학생 ID는 필수입니다.")
    @Schema(description = "학생 ID", example = "123", required = true)
    private Long studentId;
    
    @NotBlank(message = "발송 방법은 필수입니다.")
    @Schema(description = "발송 방법", example = "alimtalk", allowableValues = {"alimtalk", "discord"}, required = true)
    private String deliveryMethod;
    
    @Schema(description = "템플릿 ID (AlimtalkTemplate enum)", example = "STUDY_REMINDER_10MIN")
    private String templateId;
    
    @Schema(description = "자유 메시지 (discord만 가능)", example = "안녕하세요! 오늘 과제 확인 부탁드립니다.")
    private String customMessage;
}
