package saomath.checkusserver.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidUsernameValidator implements ConstraintValidator<ValidUsername, String> {

    @Override
    public void initialize(ValidUsername constraintAnnotation) {
        // 초기화 로직 (필요시)
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        return ValidationUtils.isValidUsername(username);
    }
}
