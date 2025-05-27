package saomath.checkusserver.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import saomath.checkusserver.auth.ValidationUtils;

public class ValidPhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        // 초기화 로직 (필요시)
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        return ValidationUtils.isValidPhoneNumber(phoneNumber);
    }
}
