package saomath.checkusserver.notification.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import saomath.checkusserver.notification.domain.DeliveryMethod;
import saomath.checkusserver.notification.dto.NotificationSendRequest;

public class NotificationSendRequestValidator implements ConstraintValidator<ValidNotificationSendRequest, NotificationSendRequest> {

    @Override
    public void initialize(ValidNotificationSendRequest constraintAnnotation) {
    }

    @Override
    public boolean isValid(NotificationSendRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return false;
        }

        // deliveryMethod 유효성 검사
        if (!DeliveryMethod.isValid(request.getDeliveryMethod())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("지원하지 않는 발송 방법입니다. (alimtalk, discord만 허용)")
                    .addPropertyNode("deliveryMethod")
                    .addConstraintViolation();
            return false;
        }

        // templateId와 customMessage 중 하나만 있어야 함
        boolean hasTemplate = hasTemplate(request);
        boolean hasCustomMessage = hasCustomMessage(request);

        if (!hasTemplate && !hasCustomMessage) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("templateId와 customMessage 중 하나는 필수입니다.")
                    .addConstraintViolation();
            return false;
        }

        if (hasTemplate && hasCustomMessage) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("templateId와 customMessage 중 하나만 입력해주세요.")
                    .addConstraintViolation();
            return false;
        }

        // customMessage는 discord에서만 가능
        if (hasCustomMessage && !isDiscord(request)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("customMessage는 discord 방식에서만 사용 가능합니다.")
                    .addPropertyNode("customMessage")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
    
    private boolean hasTemplate(NotificationSendRequest request) {
        return request.getTemplateId() != null && !request.getTemplateId().trim().isEmpty();
    }
    
    private boolean hasCustomMessage(NotificationSendRequest request) {
        return request.getCustomMessage() != null && !request.getCustomMessage().trim().isEmpty();
    }
    
    private boolean isDiscord(NotificationSendRequest request) {
        return DeliveryMethod.DISCORD.equals(DeliveryMethod.fromValue(request.getDeliveryMethod()));
    }
}
