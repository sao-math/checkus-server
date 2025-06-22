package saomath.checkusserver.notification.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotificationSendRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidNotificationSendRequest {
    String message() default "잘못된 알림 발송 요청입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
