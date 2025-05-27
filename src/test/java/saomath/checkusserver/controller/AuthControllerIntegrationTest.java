package saomath.checkusserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.auth.dto.StudentRegisterRequest;
import saomath.checkusserver.entity.StudentProfile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController 통합 테스트")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("학생 회원가입 API 테스트")
    void registerStudent_Integration_Test() throws Exception {
        // Given
        StudentRegisterRequest request = new StudentRegisterRequest();
        request.setUsername("teststudent");
        request.setPassword("Test123!@#");
        request.setName("테스트 학생");
        request.setPhoneNumber("010-1234-5678");
        request.setSchoolName("테스트 고등학교");
        request.setGrade(11);
        request.setGender(StudentProfile.Gender.MALE);

        // When & Then
        mockMvc.perform(post("/api/auth/register/student")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("teststudent"))
                .andExpect(jsonPath("$.message").value("학생 회원가입이 완료되었습니다."));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 회원가입 실패 테스트")
    void registerStudent_InvalidPassword_Fail() throws Exception {
        // Given
        StudentRegisterRequest request = new StudentRegisterRequest();
        request.setUsername("teststudent");
        request.setPassword("weakpass"); // 조건 미충족 비밀번호
        request.setName("테스트 학생");
        request.setPhoneNumber("010-1234-5678");
        request.setSchoolName("테스트 고등학교");
        request.setGrade(11);
        request.setGender(StudentProfile.Gender.MALE);

        // When & Then
        mockMvc.perform(post("/api/auth/register/student")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("사용자명 중복 확인 API 테스트")
    void checkUsername_Test() throws Exception {
        // When & Then - 존재하지 않는 사용자명
        mockMvc.perform(get("/api/auth/check-username")
                .param("username", "newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true)); // 사용 가능
    }

    @Test
    @DisplayName("전화번호 중복 확인 API 테스트")
    void checkPhoneNumber_Test() throws Exception {
        // When & Then - 존재하지 않는 전화번호
        mockMvc.perform(get("/api/auth/check-phone")
                .param("phoneNumber", "010-9999-9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true)); // 사용 가능
    }

    @Test
    @DisplayName("잘못된 전화번호 형식으로 중복 확인 테스트")
    void checkPhoneNumber_InvalidFormat_Test() throws Exception {
        // When & Then - 잘못된 형식
        mockMvc.perform(get("/api/auth/check-phone")
                .param("phoneNumber", "01012345678")) // 하이픈 없음
                .andExpect(status().isBadRequest());
    }
}
