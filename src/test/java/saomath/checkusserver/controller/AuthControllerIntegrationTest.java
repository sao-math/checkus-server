package saomath.checkusserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.auth.dto.StudentRegisterRequest;
import saomath.checkusserver.repository.SchoolRepository;
import saomath.checkusserver.repository.StudentProfileRepository;
import saomath.checkusserver.repository.UserRepository;
import saomath.checkusserver.util.TestDataFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("AuthController 통합 테스트")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private StudentProfileRepository studentProfileRepository;
    
    @Autowired
    private SchoolRepository schoolRepository;
    
    @BeforeEach
    void setUp() {
        // 테스트 전 데이터 정리
        studentProfileRepository.deleteAll();
        userRepository.deleteAll();
        schoolRepository.deleteAll();
        
        // 테스트 데이터 팩토리 카운터 리셋
        TestDataFactory.resetCounter();
    }

    @Test
    @DisplayName("학생 회원가입 API 테스트")
    void registerStudent_Integration_Test() throws Exception {
        // Given
        StudentRegisterRequest request = TestDataFactory.createStudentRegisterRequest();

        // When & Then
        mockMvc.perform(post("/api/auth/register/student")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(request.getUsername()))
                .andExpect(jsonPath("$.message").value("학생 회원가입이 완료되었습니다."));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 회원가입 실패 테스트")
    void registerStudent_InvalidPassword_Fail() throws Exception {
        // Given
        StudentRegisterRequest request = TestDataFactory.createInvalidPasswordStudentRequest();

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

    @Test
    @DisplayName("중복된 사용자명으로 회원가입 실패 테스트")
    void registerStudent_DuplicateUsername_Fail() throws Exception {
        // Given - 첫 번째 학생 회원가입
        StudentRegisterRequest firstRequest = TestDataFactory.createStudentRegisterRequest();
        mockMvc.perform(post("/api/auth/register/student")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Given - 같은 사용자명으로 두 번째 회원가입 시도
        StudentRegisterRequest duplicateRequest = TestDataFactory.createStudentRegisterRequest();
        duplicateRequest.setUsername(firstRequest.getUsername()); // 중복 사용자명

        // When & Then
        mockMvc.perform(post("/api/auth/register/student")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 사용자명입니다: " + firstRequest.getUsername()));
    }

    @Test
    @DisplayName("중복된 전화번호로 회원가입 실패 테스트")
    void registerStudent_DuplicatePhoneNumber_Fail() throws Exception {
        // Given - 첫 번째 학생 회원가입
        StudentRegisterRequest firstRequest = TestDataFactory.createStudentRegisterRequest();
        mockMvc.perform(post("/api/auth/register/student")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Given - 같은 전화번호로 두 번째 회원가입 시도
        StudentRegisterRequest duplicateRequest = TestDataFactory.createStudentRegisterRequest();
        duplicateRequest.setPhoneNumber(firstRequest.getPhoneNumber()); // 중복 전화번호

        // When & Then
        mockMvc.perform(post("/api/auth/register/student")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 등록된 전화번호입니다: " + firstRequest.getPhoneNumber()));
    }
}
