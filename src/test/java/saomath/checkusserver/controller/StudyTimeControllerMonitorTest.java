package saomath.checkusserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import saomath.checkusserver.study.dto.StudyTimeMonitorResponse;
import saomath.checkusserver.study.service.StudyTimeService;
import saomath.checkusserver.study.controller.StudyTimeController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudyTimeController.class)
@ActiveProfiles("test")
@DisplayName("StudyTimeController 모니터링 엔드포인트 테스트")
class StudyTimeControllerMonitorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyTimeService studyTimeService;

    // JWT 관련 Mock Bean 추가 (보안 설정 때문에 필요)
    @MockitoBean
    private saomath.checkusserver.auth.jwt.JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private StudyTimeMonitorResponse mockResponse;

    @BeforeEach
    void setUp() {
        // 테스트용 모니터링 응답 데이터 생성
        mockResponse = createMockMonitorResponse();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("성공: 날짜별 학생 모니터링 정보 조회")
    void getStudyTimeMonitor_Success() throws Exception {
        // given
        String targetDate = "2025-06-18";
        when(studyTimeService.getStudyTimeMonitorByDate(any(LocalDate.class)))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/study-time/monitor/{date}", targetDate)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학생 모니터링 정보를 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.data.date").value("2025-06-18"))
                .andExpect(jsonPath("$.data.students").isArray())
                .andExpect(jsonPath("$.data.students[0].studentId").value(1))
                .andExpect(jsonPath("$.data.students[0].studentName").value("김학생"))
                .andExpect(jsonPath("$.data.students[0].status").value("ATTENDING"))
                .andExpect(jsonPath("$.data.students[0].guardians").isArray())
                .andExpect(jsonPath("$.data.students[0].assignedStudyTimes").isArray())
                .andExpect(jsonPath("$.data.students[0].unassignedActualStudyTimes").isArray());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("실패: 잘못된 날짜 형식")
    void getStudyTimeMonitor_InvalidDateFormat() throws Exception {
        // given
        String invalidDate = "2025-13-32"; // 잘못된 날짜

        // when & then
        mockMvc.perform(get("/study-time/monitor/{date}", invalidDate)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("날짜 형식이 올바르지 않습니다. yyyy-MM-dd 형식으로 입력해주세요."))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("실패: 문자열 날짜 형식")
    void getStudyTimeMonitor_StringDateFormat() throws Exception {
        // given
        String invalidDate = "invalid-date";

        // when & then
        mockMvc.perform(get("/study-time/monitor/{date}", invalidDate)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("날짜 형식이 올바르지 않습니다. yyyy-MM-dd 형식으로 입력해주세요."));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("성공: 빈 학생 목록")
    void getStudyTimeMonitor_EmptyStudentList() throws Exception {
        // given
        String targetDate = "2025-06-18";
        StudyTimeMonitorResponse emptyResponse = new StudyTimeMonitorResponse();
        emptyResponse.setDate(LocalDate.parse(targetDate));
        emptyResponse.setStudents(Arrays.asList());

        when(studyTimeService.getStudyTimeMonitorByDate(any(LocalDate.class)))
                .thenReturn(emptyResponse);

        // when & then
        mockMvc.perform(get("/study-time/monitor/{date}", targetDate)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.students").isArray())
                .andExpect(jsonPath("$.data.students").isEmpty());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("성공: 여러 학생 상태 확인")
    void getStudyTimeMonitor_MultipleStudentStatuses() throws Exception {
        // given
        String targetDate = "2025-06-18";
        StudyTimeMonitorResponse multiStatusResponse = createMultiStatusResponse();
        
        when(studyTimeService.getStudyTimeMonitorByDate(any(LocalDate.class)))
                .thenReturn(multiStatusResponse);

        // when & then
        mockMvc.perform(get("/study-time/monitor/{date}", targetDate)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students[0].status").value("ATTENDING"))
                .andExpect(jsonPath("$.data.students[1].status").value("ABSENT"))
                .andExpect(jsonPath("$.data.students[2].status").value("NO_ASSIGNED_TIME"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("성공: 학생 권한으로도 조회 가능")
    void getStudyTimeMonitor_StudentRole() throws Exception {
        // given
        String targetDate = "2025-06-18";
        when(studyTimeService.getStudyTimeMonitorByDate(any(LocalDate.class)))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/study-time/monitor/{date}", targetDate)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("실패: 인증되지 않은 사용자")
    void getStudyTimeMonitor_Unauthenticated() throws Exception {
        // given
        String targetDate = "2025-06-18";

        // when & then
        mockMvc.perform(get("/study-time/monitor/{date}", targetDate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("실패: 서비스 예외 발생")
    void getStudyTimeMonitor_ServiceException() throws Exception {
        // given
        String targetDate = "2025-06-18";
        when(studyTimeService.getStudyTimeMonitorByDate(any(LocalDate.class)))
                .thenThrow(new RuntimeException("데이터베이스 연결 오류"));

        // when & then
        mockMvc.perform(get("/study-time/monitor/{date}", targetDate)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("데이터베이스 연결 오류"));
    }

    private StudyTimeMonitorResponse createMockMonitorResponse() {
        StudyTimeMonitorResponse response = new StudyTimeMonitorResponse();
        response.setDate(LocalDate.parse("2025-06-18"));

        // 학생 정보 생성
        StudyTimeMonitorResponse.StudentStudyInfo student = new StudyTimeMonitorResponse.StudentStudyInfo();
        student.setStudentId(1L);
        student.setStudentName("김학생");
        student.setStudentPhone("010-1234-5678");
        student.setStatus(StudyTimeMonitorResponse.StudentCurrentStatus.ATTENDING);

        // 보호자 정보
        StudyTimeMonitorResponse.GuardianInfo guardian = new StudyTimeMonitorResponse.GuardianInfo();
        guardian.setGuardianId(2L);
        guardian.setGuardianPhone("010-9876-5432");
        guardian.setRelationship("부");
        student.setGuardians(Arrays.asList(guardian));

        // 할당된 공부시간
        StudyTimeMonitorResponse.AssignedStudyInfo assignedStudy = new StudyTimeMonitorResponse.AssignedStudyInfo();
        assignedStudy.setAssignedStudyTimeId(1L);
        assignedStudy.setTitle("수학 공부");
        assignedStudy.setStartTime(LocalDateTime.parse("2025-06-18T10:00:00"));
        assignedStudy.setEndTime(LocalDateTime.parse("2025-06-18T12:00:00"));

        // 연결된 실제 접속 기록
        StudyTimeMonitorResponse.ConnectedActualStudyInfo connectedActual = new StudyTimeMonitorResponse.ConnectedActualStudyInfo();
        connectedActual.setActualStudyTimeId(1L);
        connectedActual.setStartTime(LocalDateTime.parse("2025-06-18T10:05:00"));
        connectedActual.setEndTime(LocalDateTime.parse("2025-06-18T11:30:00"));
        assignedStudy.setConnectedActualStudyTimes(Arrays.asList(connectedActual));

        student.setAssignedStudyTimes(Arrays.asList(assignedStudy));

        // 할당되지 않은 실제 접속 기록
        StudyTimeMonitorResponse.UnassignedActualStudyInfo unassignedActual = new StudyTimeMonitorResponse.UnassignedActualStudyInfo();
        unassignedActual.setActualStudyTimeId(2L);
        unassignedActual.setStartTime(LocalDateTime.parse("2025-06-18T20:00:00"));
        unassignedActual.setEndTime(LocalDateTime.parse("2025-06-18T21:30:00"));
        student.setUnassignedActualStudyTimes(Arrays.asList(unassignedActual));

        response.setStudents(Arrays.asList(student));
        return response;
    }

    private StudyTimeMonitorResponse createMultiStatusResponse() {
        StudyTimeMonitorResponse response = new StudyTimeMonitorResponse();
        response.setDate(LocalDate.parse("2025-06-18"));

        // 출석 학생
        StudyTimeMonitorResponse.StudentStudyInfo attendingStudent = new StudyTimeMonitorResponse.StudentStudyInfo();
        attendingStudent.setStudentId(1L);
        attendingStudent.setStudentName("김학생");
        attendingStudent.setStatus(StudyTimeMonitorResponse.StudentCurrentStatus.ATTENDING);
        attendingStudent.setGuardians(Arrays.asList());
        attendingStudent.setAssignedStudyTimes(Arrays.asList());
        attendingStudent.setUnassignedActualStudyTimes(Arrays.asList());

        // 결석 학생
        StudyTimeMonitorResponse.StudentStudyInfo absentStudent = new StudyTimeMonitorResponse.StudentStudyInfo();
        absentStudent.setStudentId(2L);
        absentStudent.setStudentName("이학생");
        absentStudent.setStatus(StudyTimeMonitorResponse.StudentCurrentStatus.ABSENT);
        absentStudent.setGuardians(Arrays.asList());
        absentStudent.setAssignedStudyTimes(Arrays.asList());
        absentStudent.setUnassignedActualStudyTimes(Arrays.asList());

        // 할당 시간 없는 학생
        StudyTimeMonitorResponse.StudentStudyInfo noAssignedStudent = new StudyTimeMonitorResponse.StudentStudyInfo();
        noAssignedStudent.setStudentId(3L);
        noAssignedStudent.setStudentName("박학생");
        noAssignedStudent.setStatus(StudyTimeMonitorResponse.StudentCurrentStatus.NO_ASSIGNED_TIME);
        noAssignedStudent.setGuardians(Arrays.asList());
        noAssignedStudent.setAssignedStudyTimes(Arrays.asList());
        noAssignedStudent.setUnassignedActualStudyTimes(Arrays.asList());

        response.setStudents(Arrays.asList(attendingStudent, absentStudent, noAssignedStudent));
        return response;
    }
}
