package saomath.checkusserver.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import saomath.checkusserver.study.controller.StudyTimeController;
import saomath.checkusserver.study.service.StudyTimeService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StudyTimeController.class, 
            excludeAutoConfiguration = {
                    SecurityAutoConfiguration.class, 
                    UserDetailsServiceAutoConfiguration.class,
                    SecurityFilterAutoConfiguration.class
            })
@DisplayName("GlobalExceptionHandler 단위 테스트")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyTimeService studyTimeService;

    @MockitoBean
    private saomath.checkusserver.auth.jwt.JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("MethodArgumentTypeMismatchException 처리 - 잘못된 날짜 형식")
    void handleMethodArgumentTypeMismatchException_InvalidDateFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/study-time/assigned/student/1")
                .param("startDate", "invalid-date")
                .param("endDate", "2025-06-02T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("파라미터 'startDate'의 값이 올바르지 않습니다. LocalDateTime 형식이어야 합니다."))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException 처리 - 잘못된 숫자 형식")
    void handleMethodArgumentTypeMismatchException_InvalidNumberFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/study-time/assigned/student/invalid-number")
                .param("startDate", "2025-06-01T00:00:00")
                .param("endDate", "2025-06-02T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("파라미터 'studentId'의 값이 올바르지 않습니다. Long 형식이어야 합니다."))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException 처리 - 둘 다 잘못된 형식")
    void handleMethodArgumentTypeMismatchException_MultipleInvalidParams() throws Exception {
        // When & Then
        mockMvc.perform(get("/study-time/assigned/student/invalid-number")
                .param("startDate", "invalid-date")
                .param("endDate", "2025-06-02T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("파라미터")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("형식이어야 합니다")))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
