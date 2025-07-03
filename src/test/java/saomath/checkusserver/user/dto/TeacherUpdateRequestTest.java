package saomath.checkusserver.user.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TeacherUpdateRequest DTO 테스트")
class TeacherUpdateRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.afterPropertiesSet();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("유효한 TeacherUpdateRequest - 성공")
    void validTeacherUpdateRequest_Success() {
        // given
        TeacherUpdateRequest request = new TeacherUpdateRequest();
        request.setName("김선생님");
        request.setPhoneNumber("010-1234-5678");
        request.setDiscordId("teacher#1234");
        request.setClassIds(Arrays.asList(1L, 2L, 3L));

        // when
        Set<ConstraintViolation<TeacherUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("모든 필드 null - 성공 (optional)")
    void allFieldsNull_Success() {
        // given
        TeacherUpdateRequest request = new TeacherUpdateRequest();
        // 모든 필드를 null로 유지

        // when
        Set<ConstraintViolation<TeacherUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty(); // 모든 필드가 optional이므로 유효해야 함
    }

    @Test
    @DisplayName("이름 길이 초과 - 실패")
    void nameTooLong_Fail() {
        // given
        TeacherUpdateRequest request = new TeacherUpdateRequest();
        request.setName("가".repeat(101)); // 100자 초과

        // when
        Set<ConstraintViolation<TeacherUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<TeacherUpdateRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("name");
        assertThat(violation.getMessage()).contains("100자를 초과할 수 없습니다");
    }

    @Test
    @DisplayName("전화번호 형식 오류 - 실패")
    void invalidPhoneNumberFormat_Fail() {
        // given
        TeacherUpdateRequest request = new TeacherUpdateRequest();
        request.setPhoneNumber("01012345678"); // 하이픈 없는 형식

        // when
        Set<ConstraintViolation<TeacherUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<TeacherUpdateRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("phoneNumber");
        assertThat(violation.getMessage()).contains("올바른 전화번호 형식이 아닙니다");
    }

    @Test
    @DisplayName("Discord ID 길이 초과 - 실패")
    void discordIdTooLong_Fail() {
        // given
        TeacherUpdateRequest request = new TeacherUpdateRequest();
        request.setDiscordId("a".repeat(101)); // 100자 초과

        // when
        Set<ConstraintViolation<TeacherUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        ConstraintViolation<TeacherUpdateRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("discordId");
        assertThat(violation.getMessage()).contains("100자를 초과할 수 없습니다");
    }

    @Test
    @DisplayName("여러 유효성 검사 오류 - 실패")
    void multipleValidationErrors_Fail() {
        // given
        TeacherUpdateRequest request = new TeacherUpdateRequest();
        request.setName("가".repeat(101)); // 이름 길이 초과
        request.setPhoneNumber("invalid-phone"); // 잘못된 전화번호 형식
        request.setDiscordId("a".repeat(101)); // Discord ID 길이 초과

        // when
        Set<ConstraintViolation<TeacherUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(3);
        
        // 각 필드별 오류 확인
        boolean nameError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        boolean phoneError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("phoneNumber"));
        boolean discordError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("discordId"));
                
        assertThat(nameError).isTrue();
        assertThat(phoneError).isTrue();
        assertThat(discordError).isTrue();
    }

    @Test
    @DisplayName("유효한 전화번호 형식들 - 성공")
    void validPhoneNumberFormats_Success() {
        // given
        String[] validPhoneNumbers = {
                "010-1234-5678",
                "011-1234-5678", 
                "016-1234-5678",
                "017-1234-5678",
                "018-1234-5678",
                "019-1234-5678"
        };

        for (String phoneNumber : validPhoneNumbers) {
            TeacherUpdateRequest request = new TeacherUpdateRequest();
            request.setPhoneNumber(phoneNumber);

            // when
            Set<ConstraintViolation<TeacherUpdateRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("잘못된 전화번호 형식들 - 실패")
    void invalidPhoneNumberFormats_Fail() {
        // given
        String[] invalidPhoneNumbers = {
                "01012345678",      // 하이픈 없음
                "010-123-5678",     // 중간 자리수 부족
                "010-12345-5678",   // 중간 자리수 초과
                "010-1234-567",     // 마지막 자리수 부족
                "010-1234-56789",   // 마지막 자리수 초과
                "02-1234-5678",     // 시작 번호가 01x가 아님
                "010 1234 5678",    // 공백으로 구분
                "010.1234.5678",    // 점으로 구분
                ""                  // 빈 문자열
        };

        for (String phoneNumber : invalidPhoneNumbers) {
            TeacherUpdateRequest request = new TeacherUpdateRequest();
            request.setPhoneNumber(phoneNumber);

            // when
            Set<ConstraintViolation<TeacherUpdateRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
            ConstraintViolation<TeacherUpdateRequest> violation = violations.iterator().next();
            assertThat(violation.getPropertyPath().toString()).isEqualTo("phoneNumber");
        }
    }

    @Test
    @DisplayName("빈 담당 반 목록 - 성공")
    void emptyClassList_Success() {
        // given
        TeacherUpdateRequest request = new TeacherUpdateRequest();
        request.setClassIds(Arrays.asList()); // 빈 리스트

        // when
        Set<ConstraintViolation<TeacherUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("담당 반 목록 null - 성공")
    void nullClassList_Success() {
        // given
        TeacherUpdateRequest request = new TeacherUpdateRequest();
        request.setClassIds(null);

        // when
        Set<ConstraintViolation<TeacherUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("경계값 테스트 - 이름 정확히 100자")
    void nameExactly100Characters_Success() {
        // given
        TeacherUpdateRequest request = new TeacherUpdateRequest();
        request.setName("가".repeat(100)); // 정확히 100자

        // when
        Set<ConstraintViolation<TeacherUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("경계값 테스트 - Discord ID 정확히 100자")
    void discordIdExactly100Characters_Success() {
        // given
        TeacherUpdateRequest request = new TeacherUpdateRequest();
        request.setDiscordId("a".repeat(100)); // 정확히 100자

        // when
        Set<ConstraintViolation<TeacherUpdateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }
}
