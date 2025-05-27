package saomath.checkusserver.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import saomath.checkusserver.auth.ValidationUtils;

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
