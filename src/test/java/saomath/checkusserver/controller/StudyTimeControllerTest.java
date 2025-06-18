package saomath.checkusserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import saomath.checkusserver.auth.CustomUserPrincipal;
import saomath.checkusserver.dto.AssignStudyTimeRequest;
import saomath.checkusserver.dto.RecordStudyStartRequest;
import saomath.checkusserver.entity.Activity;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.ActualStudyTime;
import saomath.checkusserver.exception.BusinessException;
import saomath.checkusserver.exception.ResourceNotFoundException;
import saomath.checkusserver.service.StudyTimeService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StudyTimeController.class, 
            excludeAutoConfiguration = {
                    SecurityAutoConfiguration.class, 
                    UserDetailsServiceAutoConfiguration.class,
                    SecurityFilterAutoConfiguration.class
            })
@DisplayName("StudyTimeController 단위 테스트")
class StudyTimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudyTimeService studyTimeService;

    // JWT 관련 Mock Bean 추가 (보안 설정 때문에 필요)
    @MockitoBean
    private saomath.checkusserver.auth.jwt.JwtTokenProvider jwtTokenProvider;

    private AssignedStudyTime mockAssignedStudyTime;
    private ActualStudyTime mockActualStudyTime;
    private Activity mockActivity;
    private saomath.checkusserver.entity.User mockStudent;
    private saomath.checkusserver.entity.User mockTeacher;

    @BeforeEach
    void setUp() {
        // Mock SecurityContext 설정
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
        
        when(principal.getId()).thenReturn(1L); // 테스트용 사용자 ID
        when(authentication.getPrincipal()).thenReturn(principal);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        
        // Mock 데이터 설정
        mockStudent = saomath.checkusserver.entity.User.builder()
                .id(1L)
                .name("김학생")
                .build();
        
        mockTeacher = saomath.checkusserver.entity.User.builder()
                .id(2L)
                .name("이선생")
                .build();
        
        mockActivity = Activity.builder()
                .id(1L)
                .name("수학 공부")
                .isStudyAssignable(true)
                .build();
        
        mockAssignedStudyTime = AssignedStudyTime.builder()
                .id(1L)
                .studentId(1L)
                .title("수학 공부")
                .activityId(1L)
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(3))
                .assignedBy(2L)
                .build();
        
        // 연관 엔티티 설정
        mockAssignedStudyTime.setStudent(mockStudent);
        mockAssignedStudyTime.setActivity(mockActivity);
        mockAssignedStudyTime.setAssignedByUser(mockTeacher);

        mockActualStudyTime = ActualStudyTime.builder()
                .id(1L)
                .studentId(1L)
                .assignedStudyTimeId(1L)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .source("discord")
                .build();
        
        mockActualStudyTime.setStudent(mockStudent);
    }
    
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("공부 시간 배정 성공 - 단위 테스트")
    void assignStudyTime_Success_UnitTest() throws Exception {
        // Given
        AssignStudyTimeRequest request = new AssignStudyTimeRequest();
        request.setStudentId(1L);
        request.setTitle("수학 공부");
        request.setActivityId(1L);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(3));

        when(studyTimeService.assignStudyTime(any(Long.class), any(String.class), any(Long.class), 
                any(LocalDateTime.class), any(LocalDateTime.class), any(Long.class)))
                .thenReturn(mockAssignedStudyTime);

        // When & Then
        mockMvc.perform(post("/study-time/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.studentId").value(1))
                .andExpect(jsonPath("$.data.studentName").value("김학생"))
                .andExpect(jsonPath("$.data.activityName").value("수학 공부"))
                .andExpect(jsonPath("$.data.assignedByName").value("이선생"));
    }

    @Test
    @DisplayName("공부 시간 배정 실패 - 비즈니스 예외")
    void assignStudyTime_BusinessException() throws Exception {
        // Given
        AssignStudyTimeRequest request = new AssignStudyTimeRequest();
        request.setStudentId(1L);
        request.setTitle("수학 공부");
        request.setActivityId(1L);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(3));

        when(studyTimeService.assignStudyTime(any(Long.class), any(String.class), any(Long.class), 
                any(LocalDateTime.class), any(LocalDateTime.class), any(Long.class)))
                .thenThrow(new BusinessException("해당 시간대에 이미 배정된 공부 시간이 있습니다."));

        // When & Then
        mockMvc.perform(post("/study-time/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("해당 시간대에 이미 배정된 공부 시간이 있습니다."));
    }

    @Test
    @DisplayName("배정된 공부 시간 삭제 성공")
    void deleteAssignedStudyTime_Success() throws Exception {
        // Given - Service 메서드가 정상적으로 실행되도록 설정 (void 메서드)

        // When & Then
        mockMvc.perform(delete("/study-time/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("success"));
    }

    @Test
    @DisplayName("존재하지 않는 배정 시간 삭제 실패")
    void deleteAssignedStudyTime_NotFound() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("배정된 공부 시간을 찾을 수 없습니다."))
                .when(studyTimeService).deleteAssignedStudyTime(999L);

        // When & Then
        mockMvc.perform(delete("/study-time/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("학생별 배정된 공부 시간 조회 성공")
    void getAssignedStudyTimes_Success() throws Exception {
        // Given
        when(studyTimeService.getAssignedStudyTimesByStudentAndDateRange(
                any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(mockAssignedStudyTime));

        // When & Then
        mockMvc.perform(get("/study-time/assigned/student/1")
                .param("startDate", "2025-06-01T00:00:00")
                .param("endDate", "2025-06-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("학생별 실제 공부 시간 조회 성공")
    void getActualStudyTimes_Success() throws Exception {
        // Given
        when(studyTimeService.getActualStudyTimesByStudentAndDateRange(
                any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(mockActualStudyTime));

        // When & Then
        mockMvc.perform(get("/study-time/actual/student/1")
                .param("startDate", "2025-06-01T00:00:00")
                .param("endDate", "2025-06-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("디스코드 봇용 공부 시작 기록 성공")
    void recordStudyStart_Success() throws Exception {
        // Given
        RecordStudyStartRequest request = new RecordStudyStartRequest();
        request.setStudentId(1L);
        request.setStartTime(LocalDateTime.now());
        request.setSource("discord");

        when(studyTimeService.recordStudyStart(any(Long.class), any(LocalDateTime.class), any(String.class)))
                .thenReturn(mockActualStudyTime);

        // When & Then
        mockMvc.perform(post("/study-time/record/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value(1))
                .andExpect(jsonPath("$.data.source").value("discord"));
    }

    @Test
    @DisplayName("디스코드 봇용 공부 시작 기록 실패 - 존재하지 않는 학생")
    void recordStudyStart_StudentNotFound() throws Exception {
        // Given
        RecordStudyStartRequest request = new RecordStudyStartRequest();
        request.setStudentId(999L);
        request.setStartTime(LocalDateTime.now());
        request.setSource("discord");

        when(studyTimeService.recordStudyStart(any(Long.class), any(LocalDateTime.class), any(String.class)))
                .thenThrow(new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: 999"));

        // When & Then
        mockMvc.perform(post("/study-time/record/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("공부 배정 가능한 활동 목록 조회 성공")
    void getStudyAssignableActivities_Success() throws Exception {
        // Given
        when(studyTimeService.getStudyAssignableActivities())
                .thenReturn(Arrays.asList(mockActivity));

        // When & Then
        mockMvc.perform(get("/study-time/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("수학 공부"))
                .andExpect(jsonPath("$.data[0].isStudyAssignable").value(true));
    }

    @Test
    @DisplayName("공부 배정 가능한 활동 목록 조회 - 빈 목록")
    void getStudyAssignableActivities_EmptyList() throws Exception {
        // Given
        when(studyTimeService.getStudyAssignableActivities())
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/study-time/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("곧 시작할 공부 시간 조회 성공")
    void getUpcomingStudyTimes_Success() throws Exception {
        // Given
        when(studyTimeService.getUpcomingStudyTimes())
                .thenReturn(Arrays.asList(mockAssignedStudyTime));

        // When & Then
        mockMvc.perform(get("/study-time/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("특정 배정의 실제 접속 기록 조회 성공")
    void getActualStudyTimesByAssigned_Success() throws Exception {
        // Given
        when(studyTimeService.getActualStudyTimesByAssignedId(1L))
                .thenReturn(Arrays.asList(mockActualStudyTime));

        // When & Then
        mockMvc.perform(get("/study-time/actual/assigned/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].assignedStudyTimeId").value(1));
    }

    @Test
    @DisplayName("잘못된 요청 데이터로 공부 시간 배정 실패")
    void assignStudyTime_InvalidRequest() throws Exception {
        // Given - 필수 필드 누락
        AssignStudyTimeRequest request = new AssignStudyTimeRequest();
        request.setStudentId(null); // 필수 필드 누락
        request.setTitle("수학 공부");
        request.setActivityId(1L);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(3));

        // When & Then
        mockMvc.perform(post("/study-time/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 날짜 형식으로 조회 실패")
    void getAssignedStudyTimes_InvalidDateFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/study-time/assigned/student/1")
                .param("startDate", "invalid-date")
                .param("endDate", "2025-06-02T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("과거 날짜로 배정된 공부 시간 조회 성공")
    void getAssignedStudyTimes_PastDates_Success() throws Exception {
        // Given
        when(studyTimeService.getAssignedStudyTimesByStudentAndDateRange(
                any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(mockAssignedStudyTime));

        // When & Then
        mockMvc.perform(get("/study-time/assigned/student/1")
                .param("startDate", "2025-05-01T00:00:00") // 과거 날짜
                .param("endDate", "2025-05-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("너무 오래된 데이터 조회 시 비즈니스 예외 발생")
    void getAssignedStudyTimes_TooOldDates_BusinessException() throws Exception {
        // Given
        when(studyTimeService.getAssignedStudyTimesByStudentAndDateRange(
                any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenThrow(new BusinessException("조회 가능한 기간을 초과했습니다. 최대 1년 전까지 조회 가능합니다."));

        // When & Then
        mockMvc.perform(get("/study-time/assigned/student/1")
                .param("startDate", "2023-01-01T00:00:00") // 2년 전
                .param("endDate", "2023-01-02T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("조회 가능한 기간을 초과했습니다. 최대 1년 전까지 조회 가능합니다."));
    }

    @Test
    @DisplayName("실제 공부 시간 과거 날짜 조회 성공")
    void getActualStudyTimes_PastDates_Success() throws Exception {
        // Given
        when(studyTimeService.getActualStudyTimesByStudentAndDateRange(
                any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(mockActualStudyTime));

        // When & Then
        mockMvc.perform(get("/study-time/actual/student/1")
                .param("startDate", "2025-05-01T00:00:00") // 과거 날짜
                .param("endDate", "2025-05-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }
}
