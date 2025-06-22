package saomath.checkusserver.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "알림 발송 응답")
public class NotificationSendResponse {
    
    @Schema(description = "발송 방법", example = "alimtalk")
    private String deliveryMethod;
    
    @Schema(description = "수신자 (전화번호 또는 디스코드 ID)", example = "01012345678")
    private String recipient;
    
    @Schema(description = "실제 발송된 메시지 전문")
    private String sentMessage;
    
    @Schema(description = "사용된 템플릿 ID (자유 메시지시 null)", example = "STUDY_REMINDER_10MIN")
    private String templateUsed;
}
