package saomath.checkusserver.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import saomath.checkusserver.auth.ValidationUtils;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // 초기화 로직 (필요시)
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        return ValidationUtils.isValidPassword(password);
    }
}
