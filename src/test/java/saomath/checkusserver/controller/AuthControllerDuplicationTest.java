package saomath.checkusserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.auth.domain.User;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
@DisplayName("인증 중복 확인 컨트롤러 테스트")
class AuthControllerDuplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        // 테스트용 기존 사용자 생성
        User existingUser = User.builder()
                .username("existinguser")
                .name("기존 사용자")
                .phoneNumber("010-1234-5678")
                .password("hashedPassword")
                .discordId("existingDiscordId")
                .build();
        userRepository.save(existingUser);
    }

    @Test
    @DisplayName("사용자명 중복 확인 - 사용 가능한 사용자명")
    void checkUsername_Available() throws Exception {
        mockMvc.perform(get("/auth/check-username")
                .param("username", "newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("사용 가능한 사용자명입니다."))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("사용자명 중복 확인 - 이미 사용 중인 사용자명")
    void checkUsername_AlreadyExists() throws Exception {
        mockMvc.perform(get("/auth/check-username")
                .param("username", "existinguser"))
                .andExpect(status().isConflict()) // 409 Conflict
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 사용자명입니다."))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("사용자명 중복 확인 - 잘못된 형식")
    void checkUsername_InvalidFormat() throws Exception {
        mockMvc.perform(get("/auth/check-username")
                .param("username", "ab")) // 너무 짧은 사용자명
                .andExpect(status().isBadRequest()) // 400 Bad Request
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("전화번호 중복 확인 - 사용 가능한 전화번호")
    void checkPhoneNumber_Available() throws Exception {
        mockMvc.perform(get("/auth/check-phone")
                .param("phoneNumber", "010-9999-8888"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("사용 가능한 전화번호입니다."))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("전화번호 중복 확인 - 이미 등록된 전화번호")
    void checkPhoneNumber_AlreadyExists() throws Exception {
        mockMvc.perform(get("/auth/check-phone")
                .param("phoneNumber", "010-1234-5678"))
                .andExpect(status().isConflict()) // 409 Conflict
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 등록된 전화번호입니다."))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("전화번호 중복 확인 - 잘못된 형식")
    void checkPhoneNumber_InvalidFormat() throws Exception {
        mockMvc.perform(get("/auth/check-phone")
                .param("phoneNumber", "010-123-456")) // 잘못된 형식
                .andExpect(status().isBadRequest()) // 400 Bad Request
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("사용자명 중복 확인 - 대소문자 구분 테스트")
    void checkUsername_CaseSensitive() throws Exception {
        // 소문자로 저장된 사용자명과 대문자로 확인
        mockMvc.perform(get("/auth/check-username")
                .param("username", "EXISTINGUSER"))
                .andExpect(status().isOk()) // 대소문자를 구분하므로 사용 가능
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("전화번호 중복 확인 - 공백 포함 형식 테스트")
    void checkPhoneNumber_WithSpaces() throws Exception {
        mockMvc.perform(get("/auth/check-phone")
                .param("phoneNumber", "010 1234 5678")) // 공백 포함
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
