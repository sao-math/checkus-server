package saomath.checkusserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import saomath.checkusserver.auth.CustomUserDetailsService;
import saomath.checkusserver.auth.jwt.JwtTokenProvider;
import saomath.checkusserver.dto.WeeklySchedulePeriodResponse;
import saomath.checkusserver.dto.WeeklyScheduleRequest;
import saomath.checkusserver.dto.WeeklyScheduleResponse;
import saomath.checkusserver.common.exception.BusinessException;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.service.WeeklyScheduleService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeeklyScheduleController.class)
class WeeklyScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeeklyScheduleService weeklyScheduleService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private WeeklyScheduleResponse mockScheduleResponse;
    private WeeklyScheduleRequest mockScheduleRequest;
    private WeeklySchedulePeriodResponse mockPeriodResponse;

    @BeforeEach
    void setUp() {
        // Mock 데이터 설정
        mockScheduleResponse = new WeeklyScheduleResponse();
        mockScheduleResponse.setId(1L);
        mockScheduleResponse.setStudentId(1L);
        mockScheduleResponse.setTitle("수학 공부");
        mockScheduleResponse.setActivityId(1L);
        mockScheduleResponse.setActivityName("자습");
        mockScheduleResponse.setIsStudyAssignable(true);
        mockScheduleResponse.setDayOfWeek(1);
        mockScheduleResponse.setDayOfWeekName("월요일");
        mockScheduleResponse.setStartTime(LocalTime.of(9, 0));
        mockScheduleResponse.setEndTime(LocalTime.of(10, 30));

        mockScheduleRequest = new WeeklyScheduleRequest(
                1L,           // studentId
                "수학 공부",   // title
                1L,           // activityId
                1,            // dayOfWeek
                LocalTime.of(9, 0),    // startTime
                LocalTime.of(10, 30)   // endTime
        );

        mockPeriodResponse = new WeeklySchedulePeriodResponse();
        mockPeriodResponse.setId(1L);
        mockPeriodResponse.setStudentId(1L);
        mockPeriodResponse.setTitle("수학 공부");
        mockPeriodResponse.setActivityId(1L);
        mockPeriodResponse.setActivityName("자습");
        mockPeriodResponse.setIsStudyAssignable(true);
        mockPeriodResponse.setActualStartTime(LocalDateTime.of(2025, 6, 2, 9, 0));
        mockPeriodResponse.setActualEndTime(LocalDateTime.of(2025, 6, 2, 10, 30));
        mockPeriodResponse.setDayOfWeek(1);
        mockPeriodResponse.setDayOfWeekName("월요일");
    }

    @Test
    @DisplayName("학생 주간 시간표 조회 - 성공")
    @WithMockUser(roles = "TEACHER")
    void getWeeklySchedule_Success() throws Exception {
        // Given
        when(weeklyScheduleService.getWeeklyScheduleByStudent(1L))
                .thenReturn(Arrays.asList(mockScheduleResponse));

        // When & Then
        mockMvc.perform(get("/weekly-schedule/student/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주간 시간표 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("수학 공부"))
                .andExpect(jsonPath("$.data[0].activityName").value("자습"))
                .andExpect(jsonPath("$.data[0].isStudyAssignable").value(true))
                .andExpect(jsonPath("$.data[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$.data[0].dayOfWeekName").value("월요일"));
    }

    @Test
    @DisplayName("학생 주간 시간표 조회 - 학생을 찾을 수 없음")
    @WithMockUser(roles = "TEACHER")
    void getWeeklySchedule_StudentNotFound() throws Exception {
        // Given
        when(weeklyScheduleService.getWeeklyScheduleByStudent(999L))
                .thenThrow(new ResourceNotFoundException("학생을 찾을 수 없습니다. ID: 999"));

        // When & Then
        mockMvc.perform(get("/weekly-schedule/student/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("학생을 찾을 수 없습니다. ID: 999"));
    }

    @Test
    @DisplayName("주간 시간표 등록 - 성공")
    @WithMockUser(roles = "TEACHER")
    void createWeeklySchedule_Success() throws Exception {
        // Given
        when(weeklyScheduleService.createWeeklySchedule(any(WeeklyScheduleRequest.class)))
                .thenReturn(mockScheduleResponse);

        // When & Then
        mockMvc.perform(post("/weekly-schedule")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockScheduleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주간 시간표가 성공적으로 등록되었습니다."))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("주간 시간표 등록 - 시간 겹침")
    @WithMockUser(roles = "TEACHER")
    void createWeeklySchedule_TimeOverlap() throws Exception {
        // Given
        when(weeklyScheduleService.createWeeklySchedule(any(WeeklyScheduleRequest.class)))
                .thenThrow(new BusinessException("해당 시간대에 이미 등록된 시간표가 있습니다."));

        // When & Then
        mockMvc.perform(post("/weekly-schedule")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockScheduleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("해당 시간대에 이미 등록된 시간표가 있습니다."));
    }

    @Test
    @DisplayName("주간 시간표 등록 - 유효성 검증 실패")
    @WithMockUser(roles = "TEACHER")
    void createWeeklySchedule_ValidationFailed() throws Exception {
        // Given - 잘못된 요일 (0은 유효하지 않음)
        WeeklyScheduleRequest invalidRequest = new WeeklyScheduleRequest(
                1L, "수학 공부", 1L, 0, // dayOfWeek = 0 (유효하지 않음)
                LocalTime.of(9, 0), LocalTime.of(10, 30)
        );

        // When & Then
        mockMvc.perform(post("/weekly-schedule")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주간 시간표 수정 - 성공")
    @WithMockUser(roles = "TEACHER")
    void updateWeeklySchedule_Success() throws Exception {
        // Given
        when(weeklyScheduleService.updateWeeklySchedule(eq(1L), any(WeeklyScheduleRequest.class)))
                .thenReturn(mockScheduleResponse);

        // When & Then
        mockMvc.perform(put("/weekly-schedule/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockScheduleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주간 시간표가 성공적으로 수정되었습니다."))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("주간 시간표 수정 - 시간표를 찾을 수 없음")
    @WithMockUser(roles = "TEACHER")
    void updateWeeklySchedule_ScheduleNotFound() throws Exception {
        // Given
        when(weeklyScheduleService.updateWeeklySchedule(eq(999L), any(WeeklyScheduleRequest.class)))
                .thenThrow(new ResourceNotFoundException("시간표를 찾을 수 없습니다. ID: 999"));

        // When & Then
        mockMvc.perform(put("/weekly-schedule/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockScheduleRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("시간표를 찾을 수 없습니다. ID: 999"));
    }

    @Test
    @DisplayName("주간 시간표 삭제 - 성공")
    @WithMockUser(roles = "TEACHER")
    void deleteWeeklySchedule_Success() throws Exception {
        // Given - void 메서드이므로 별도 설정 없음

        // When & Then
        mockMvc.perform(delete("/weekly-schedule/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주간 시간표가 성공적으로 삭제되었습니다."))
                .andExpect(jsonPath("$.data").value("success"));
    }

    @Test
    @DisplayName("주간 시간표 삭제 - 시간표를 찾을 수 없음")
    @WithMockUser(roles = "TEACHER")
    void deleteWeeklySchedule_ScheduleNotFound() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("시간표를 찾을 수 없습니다. ID: 999"))
                .when(weeklyScheduleService).deleteWeeklySchedule(999L);

        // When & Then
        mockMvc.perform(delete("/weekly-schedule/999")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("시간표를 찾을 수 없습니다. ID: 999"));
    }

    @Test
    @DisplayName("특정 기간 시간표 조회 - 성공")
    @WithMockUser(roles = "TEACHER")
    void getWeeklyScheduleForPeriod_Success() throws Exception {
        // Given
        when(weeklyScheduleService.getWeeklyScheduleForPeriod(eq(1L), any(LocalDate.class), eq(7)))
                .thenReturn(Arrays.asList(mockPeriodResponse));

        // When & Then
        mockMvc.perform(get("/weekly-schedule/student/1/period")
                        .param("startDate", "2025-06-02")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("기간별 시간표 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("수학 공부"))
                .andExpect(jsonPath("$.data[0].actualStartTime").value("2025-06-02T09:00:00"))
                .andExpect(jsonPath("$.data[0].dayOfWeekName").value("월요일"));
    }

    @Test
    @DisplayName("특정 기간 시간표 조회 - 기본값 사용")
    @WithMockUser(roles = "TEACHER")
    void getWeeklyScheduleForPeriod_DefaultDays() throws Exception {
        // Given
        when(weeklyScheduleService.getWeeklyScheduleForPeriod(eq(1L), any(LocalDate.class), eq(7)))
                .thenReturn(Arrays.asList(mockPeriodResponse));

        // When & Then
        mockMvc.perform(get("/weekly-schedule/student/1/period")
                        .param("startDate", "2025-06-02")
                        // days 파라미터 생략 (기본값 7 사용)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("주간 시간표 조회 - 인증 없음")
    void getWeeklySchedule_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/weekly-schedule/student/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("주간 시간표 등록 - 인증 없음")
    void createWeeklySchedule_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/weekly-schedule")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockScheduleRequest)))
                .andExpect(status().isUnauthorized());
    }
}
