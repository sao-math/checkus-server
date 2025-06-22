package saomath.checkusserver.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import saomath.checkusserver.common.validation.ValidationUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationUtils 테스트")
class ValidationUtilsTest {

    @Test
    @DisplayName("유효한 비밀번호 테스트")
    void validPassword_Success() {
        // Given & When & Then
        assertTrue(ValidationUtils.isValidPassword("Test123!@#"));
        assertTrue(ValidationUtils.isValidPassword("MyPass1234!"));
        assertTrue(ValidationUtils.isValidPassword("Secure@Pass1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "test123",          // 특수문자 없음
        "TEST123!",         // 소문자 없음
        "testpass!",        // 대문자 없음
        "TestPass!",        // 숫자 없음
        "123456!@",         // 문자 없음
        "Test1!",           // 8자 미만
        "",                 // 빈 문자열
    })
    @DisplayName("유효하지 않은 비밀번호 테스트")
    void invalidPassword_Fail(String password) {
        assertFalse(ValidationUtils.isValidPassword(password));
    }

    @Test
    @DisplayName("유효한 전화번호 테스트")
    void validPhoneNumber_Success() {
        // Given & When & Then
        assertTrue(ValidationUtils.isValidPhoneNumber("010-1234-5678"));
        assertTrue(ValidationUtils.isValidPhoneNumber("010-0000-0000"));
        assertTrue(ValidationUtils.isValidPhoneNumber("010-9999-9999"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "01012345678",      // 하이픈 없음
        "010-12345-678",    // 잘못된 형식
        "011-1234-5678",    // 010이 아님
        "010-123-5678",     // 중간 자리수 틀림
        "010-1234-567",     // 마지막 자리수 틀림
        "",                 // 빈 문자열
        "010-abcd-5678",    // 문자 포함
    })
    @DisplayName("유효하지 않은 전화번호 테스트")
    void invalidPhoneNumber_Fail(String phoneNumber) {
        assertFalse(ValidationUtils.isValidPhoneNumber(phoneNumber));
    }

    @Test
    @DisplayName("유효한 사용자명 테스트")
    void validUsername_Success() {
        // Given & When & Then
        assertTrue(ValidationUtils.isValidUsername("testuser"));
        assertTrue(ValidationUtils.isValidUsername("test_user"));
        assertTrue(ValidationUtils.isValidUsername("user123"));
        assertTrue(ValidationUtils.isValidUsername("Test_User_123"));
        assertTrue(ValidationUtils.isValidUsername("abcd")); // 4자 최소
        assertTrue(ValidationUtils.isValidUsername("abcdefghij1234567890")); // 20자 최대
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "abc",              // 4자 미만
        "abcdefghij1234567890a", // 21자 (20자 초과)
        "test-user",        // 하이픈 포함
        "test user",        // 공백 포함
        "test@user",        // 특수문자 포함
        "테스트유저",         // 한글 포함
        "",                 // 빈 문자열
    })
    @DisplayName("유효하지 않은 사용자명 테스트")
    void invalidUsername_Fail(String username) {
        assertFalse(ValidationUtils.isValidUsername(username));
    }

    @Test
    @DisplayName("null 값 처리 테스트")
    void nullValue_Test() {
        assertFalse(ValidationUtils.isValidPassword(null));
        assertFalse(ValidationUtils.isValidPhoneNumber(null));
        assertFalse(ValidationUtils.isValidUsername(null));
    }
}
