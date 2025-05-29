package saomath.checkusserver.auth;

import java.util.regex.Pattern;

public class ValidationUtils {
    
    // 비밀번호: 8자 이상, 대문자+소문자+숫자+특수문자 포함
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$"
    );
    
    // 전화번호: 010-0000-0000 형태
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^010-\\d{4}-\\d{4}$"
    );
    
    // 사용자명: 4-20자, 영문/숫자/언더스코어만 가능
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_]{4,20}$"
    );

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static String getPasswordRequirements() {
        return "비밀번호는 8자 이상이며, 대문자, 소문자, 숫자, 특수문자를 모두 포함해야 합니다.";
    }

    public static String getPhoneNumberRequirements() {
        return "전화번호는 010-0000-0000 형태로 입력해주세요.";
    }

    public static String getUsernameRequirements() {
        return "사용자명은 4-20자의 영문자, 숫자, 언더스코어만 사용 가능합니다.";
    }
}
