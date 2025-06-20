package saomath.checkusserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.dto.StudyTimeMonitorResponse;
import saomath.checkusserver.entity.*;
import saomath.checkusserver.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudyTimeService 모니터링 기능 테스트")
class StudyTimeServiceMonitorTest {

    @Mock
    private AssignedStudyTimeRepository assignedStudyTimeRepository;

    @Mock
    private ActualStudyTimeRepository actualStudyTimeRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentGuardianRepository studentGuardianRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @InjectMocks
    private StudyTimeService studyTimeService;

    private User testStudent;
    private User testGuardian;
    private AssignedStudyTime testAssignedStudyTime;
    private ActualStudyTime testConnectedActualStudyTime;
    private ActualStudyTime testUnassignedActualStudyTime;
    private StudentGuardian testStudentGuardian;

    @BeforeEach
    void setUp() {
        // 테스트용 데이터 생성
        testStudent = User.builder()
                .id(1L)
                .name("김학생")
                .phoneNumber("010-1234-5678")
                .build();

        testGuardian = User.builder()
                .id(2L)
                .name("김부모")
                .phoneNumber("010-9876-5432")
                .build();

        testAssignedStudyTime = AssignedStudyTime.builder()
                .id(1L)
                .studentId(1L)
                .title("수학 공부")
                .startTime(LocalDateTime.parse("2025-06-18T10:00:00"))
                .endTime(LocalDateTime.parse("2025-06-18T12:00:00"))
                .build();

        testConnectedActualStudyTime = ActualStudyTime.builder()
                .id(1L)
                .studentId(1L)
                .assignedStudyTimeId(1L)
                .startTime(LocalDateTime.parse("2025-06-18T10:05:00"))
                .endTime(LocalDateTime.parse("2025-06-18T11:30:00"))
                .source("discord")
                .build();

        testUnassignedActualStudyTime = ActualStudyTime.builder()
                .id(2L)
                .studentId(1L)
                .assignedStudyTimeId(null)
                .startTime(LocalDateTime.parse("2025-06-18T20:00:00"))
                .endTime(LocalDateTime.parse("2025-06-18T21:30:00"))
                .source("discord")
                .build();

        testStudentGuardian = new StudentGuardian();
        testStudentGuardian.setStudent(testStudent);
        testStudentGuardian.setGuardian(testGuardian);
        testStudentGuardian.setRelationship("부");
    }

    @Test
    @DisplayName("성공: 날짜별 학생 모니터링 정보 조회")
    void getStudyTimeMonitorByDate_Success() {
        // given
        LocalDate targetDate = LocalDate.parse("2025-06-18");
        
        when(userRepository.findAllStudents()).thenReturn(Arrays.asList(testStudent));
        when(studentGuardianRepository.findByStudentId(1L)).thenReturn(Arrays.asList(testStudentGuardian));
        when(assignedStudyTimeRepository.findByStudentIdAndStartTimeBetweenWithDetails(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testAssignedStudyTime));
        when(actualStudyTimeRepository.findByAssignedStudyTimeId(1L))
                .thenReturn(Arrays.asList(testConnectedActualStudyTime));
        when(actualStudyTimeRepository.findByStudentIdAndDateRangeAndAssignedStudyTimeIdIsNull(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testUnassignedActualStudyTime));

        // when
        StudyTimeMonitorResponse result = studyTimeService.getStudyTimeMonitorByDate(targetDate);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDate()).isEqualTo(targetDate);
        assertThat(result.getStudents()).hasSize(1);

        StudyTimeMonitorResponse.StudentStudyInfo studentInfo = result.getStudents().get(0);
        assertThat(studentInfo.getStudentId()).isEqualTo(1L);
        assertThat(studentInfo.getStudentName()).isEqualTo("김학생");
        assertThat(studentInfo.getStudentPhone()).isEqualTo("010-1234-5678");
        assertThat(studentInfo.getStatus()).isEqualTo(StudyTimeMonitorResponse.StudentCurrentStatus.NO_ASSIGNED_TIME);

        // 보호자 정보 확인
        assertThat(studentInfo.getGuardians()).hasSize(1);
        StudyTimeMonitorResponse.GuardianInfo guardianInfo = studentInfo.getGuardians().get(0);
        assertThat(guardianInfo.getGuardianId()).isEqualTo(2L);
        assertThat(guardianInfo.getGuardianPhone()).isEqualTo("010-9876-5432");
        assertThat(guardianInfo.getRelationship()).isEqualTo("부");

        // 할당된 공부시간 정보 확인
        assertThat(studentInfo.getAssignedStudyTimes()).hasSize(1);
        StudyTimeMonitorResponse.AssignedStudyInfo assignedInfo = studentInfo.getAssignedStudyTimes().get(0);
        assertThat(assignedInfo.getAssignedStudyTimeId()).isEqualTo(1L);
        assertThat(assignedInfo.getTitle()).isEqualTo("수학 공부");
        assertThat(assignedInfo.getStartTime()).isEqualTo(LocalDateTime.parse("2025-06-18T10:00:00"));
        assertThat(assignedInfo.getEndTime()).isEqualTo(LocalDateTime.parse("2025-06-18T12:00:00"));

        // 연결된 실제 접속 기록 확인
        assertThat(assignedInfo.getConnectedActualStudyTimes()).hasSize(1);
        StudyTimeMonitorResponse.ConnectedActualStudyInfo connectedInfo = assignedInfo.getConnectedActualStudyTimes().get(0);
        assertThat(connectedInfo.getActualStudyTimeId()).isEqualTo(1L);
        assertThat(connectedInfo.getStartTime()).isEqualTo(LocalDateTime.parse("2025-06-18T10:05:00"));
        assertThat(connectedInfo.getEndTime()).isEqualTo(LocalDateTime.parse("2025-06-18T11:30:00"));

        // 할당되지 않은 실제 접속 기록 확인
        assertThat(studentInfo.getUnassignedActualStudyTimes()).hasSize(1);
        StudyTimeMonitorResponse.UnassignedActualStudyInfo unassignedInfo = studentInfo.getUnassignedActualStudyTimes().get(0);
        assertThat(unassignedInfo.getActualStudyTimeId()).isEqualTo(2L);
        assertThat(unassignedInfo.getStartTime()).isEqualTo(LocalDateTime.parse("2025-06-18T20:00:00"));
        assertThat(unassignedInfo.getEndTime()).isEqualTo(LocalDateTime.parse("2025-06-18T21:30:00"));
    }

    @Test
    @DisplayName("성공: 학생 목록이 비어있는 경우")
    void getStudyTimeMonitorByDate_EmptyStudentList() {
        // given
        LocalDate targetDate = LocalDate.parse("2025-06-18");
        when(userRepository.findAllStudents()).thenReturn(Arrays.asList());

        // when
        StudyTimeMonitorResponse result = studyTimeService.getStudyTimeMonitorByDate(targetDate);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDate()).isEqualTo(targetDate);
        assertThat(result.getStudents()).isEmpty();
    }

    @Test
    @DisplayName("성공: 보호자가 없는 학생")
    void getStudyTimeMonitorByDate_NoGuardians() {
        // given
        LocalDate targetDate = LocalDate.parse("2025-06-18");
        
        when(userRepository.findAllStudents()).thenReturn(Arrays.asList(testStudent));
        when(studentGuardianRepository.findByStudentId(1L)).thenReturn(Arrays.asList());
        when(assignedStudyTimeRepository.findByStudentIdAndStartTimeBetweenWithDetails(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());
        when(actualStudyTimeRepository.findByStudentIdAndDateRangeAndAssignedStudyTimeIdIsNull(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());

        // when
        StudyTimeMonitorResponse result = studyTimeService.getStudyTimeMonitorByDate(targetDate);

        // then
        assertThat(result.getStudents()).hasSize(1);
        StudyTimeMonitorResponse.StudentStudyInfo studentInfo = result.getStudents().get(0);
        assertThat(studentInfo.getGuardians()).isEmpty();
        assertThat(studentInfo.getAssignedStudyTimes()).isEmpty();
        assertThat(studentInfo.getUnassignedActualStudyTimes()).isEmpty();
        assertThat(studentInfo.getStatus()).isEqualTo(StudyTimeMonitorResponse.StudentCurrentStatus.NO_ASSIGNED_TIME);
    }

    @Test
    @DisplayName("성공: 할당된 시간에 현재 출석 중인 학생 상태")
    void getStudyTimeMonitorByDate_AttendingStatus() {
        // given
        LocalDate targetDate = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        
        // 현재 시간이 포함된 할당 시간 생성
        AssignedStudyTime currentAssignedTime = AssignedStudyTime.builder()
                .id(1L)
                .studentId(1L)
                .title("현재 수업")
                .startTime(now.minusHours(1))  // 1시간 전 시작
                .endTime(now.plusHours(1))     // 1시간 후 종료
                .build();

        // 현재 진행중인 접속 기록 (endTime이 null)
        ActualStudyTime ongoingActual = ActualStudyTime.builder()
                .id(1L)
                .studentId(1L)
                .assignedStudyTimeId(1L)
                .startTime(now.minusMinutes(30))
                .endTime(null)  // 아직 종료되지 않음
                .source("discord")
                .build();

        when(userRepository.findAllStudents()).thenReturn(Arrays.asList(testStudent));
        when(studentGuardianRepository.findByStudentId(1L)).thenReturn(Arrays.asList());
        when(assignedStudyTimeRepository.findByStudentIdAndStartTimeBetweenWithDetails(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(currentAssignedTime));
        when(actualStudyTimeRepository.findByAssignedStudyTimeId(1L))
                .thenReturn(Arrays.asList(ongoingActual));
        when(actualStudyTimeRepository.findByStudentIdAndDateRangeAndAssignedStudyTimeIdIsNull(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());

        // when
        StudyTimeMonitorResponse result = studyTimeService.getStudyTimeMonitorByDate(targetDate);

        // then
        assertThat(result.getStudents()).hasSize(1);
        StudyTimeMonitorResponse.StudentStudyInfo studentInfo = result.getStudents().get(0);
        assertThat(studentInfo.getStatus()).isEqualTo(StudyTimeMonitorResponse.StudentCurrentStatus.ATTENDING);
    }

    @Test
    @DisplayName("성공: 할당된 시간이지만 미접속 중인 학생 상태")
    void getStudyTimeMonitorByDate_AbsentStatus() {
        // given
        LocalDate targetDate = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        
        // 현재 시간이 포함된 할당 시간 생성
        AssignedStudyTime currentAssignedTime = AssignedStudyTime.builder()
                .id(1L)
                .studentId(1L)
                .title("현재 수업")
                .startTime(now.minusHours(1))  // 1시간 전 시작
                .endTime(now.plusHours(1))     // 1시간 후 종료
                .build();

        when(userRepository.findAllStudents()).thenReturn(Arrays.asList(testStudent));
        when(studentGuardianRepository.findByStudentId(1L)).thenReturn(Arrays.asList());
        when(assignedStudyTimeRepository.findByStudentIdAndStartTimeBetweenWithDetails(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(currentAssignedTime));
        when(actualStudyTimeRepository.findByAssignedStudyTimeId(1L))
                .thenReturn(Arrays.asList());  // 연결된 접속 기록 없음
        when(actualStudyTimeRepository.findByStudentIdAndDateRangeAndAssignedStudyTimeIdIsNull(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());

        // when
        StudyTimeMonitorResponse result = studyTimeService.getStudyTimeMonitorByDate(targetDate);

        // then
        assertThat(result.getStudents()).hasSize(1);
        StudyTimeMonitorResponse.StudentStudyInfo studentInfo = result.getStudents().get(0);
        assertThat(studentInfo.getStatus()).isEqualTo(StudyTimeMonitorResponse.StudentCurrentStatus.ABSENT);
    }

    @Test
    @DisplayName("성공: 여러 학생이 있는 경우")
    void getStudyTimeMonitorByDate_MultipleStudents() {
        // given
        LocalDate targetDate = LocalDate.parse("2025-06-18");
        
        User student2 = User.builder()
                .id(2L)
                .name("이학생")
                .phoneNumber("010-2222-3333")
                .build();

        when(userRepository.findAllStudents()).thenReturn(Arrays.asList(testStudent, student2));
        when(studentGuardianRepository.findByStudentId(1L)).thenReturn(Arrays.asList(testStudentGuardian));
        when(studentGuardianRepository.findByStudentId(2L)).thenReturn(Arrays.asList());
        
        // 첫 번째 학생은 할당된 시간이 있음
        when(assignedStudyTimeRepository.findByStudentIdAndStartTimeBetweenWithDetails(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testAssignedStudyTime));
        when(actualStudyTimeRepository.findByAssignedStudyTimeId(1L))
                .thenReturn(Arrays.asList(testConnectedActualStudyTime));
        when(actualStudyTimeRepository.findByStudentIdAndDateRangeAndAssignedStudyTimeIdIsNull(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());
        
        // 두 번째 학생은 할당된 시간이 없음
        when(assignedStudyTimeRepository.findByStudentIdAndStartTimeBetweenWithDetails(
                eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());
        when(actualStudyTimeRepository.findByStudentIdAndDateRangeAndAssignedStudyTimeIdIsNull(
                eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());

        // when
        StudyTimeMonitorResponse result = studyTimeService.getStudyTimeMonitorByDate(targetDate);

        // then
        assertThat(result.getStudents()).hasSize(2);
        
        StudyTimeMonitorResponse.StudentStudyInfo firstStudent = result.getStudents().get(0);
        assertThat(firstStudent.getStudentId()).isEqualTo(1L);
        assertThat(firstStudent.getStudentName()).isEqualTo("김학생");
        assertThat(firstStudent.getGuardians()).hasSize(1);
        assertThat(firstStudent.getAssignedStudyTimes()).hasSize(1);
        
        StudyTimeMonitorResponse.StudentStudyInfo secondStudent = result.getStudents().get(1);
        assertThat(secondStudent.getStudentId()).isEqualTo(2L);
        assertThat(secondStudent.getStudentName()).isEqualTo("이학생");
        assertThat(secondStudent.getGuardians()).isEmpty();
        assertThat(secondStudent.getAssignedStudyTimes()).isEmpty();
        assertThat(secondStudent.getStatus()).isEqualTo(StudyTimeMonitorResponse.StudentCurrentStatus.NO_ASSIGNED_TIME);
    }

    @Test
    @DisplayName("성공: 할당된 시간에 여러 접속 기록이 있는 경우")
    void getStudyTimeMonitorByDate_MultipleConnectedActuals() {
        // given
        LocalDate targetDate = LocalDate.parse("2025-06-18");
        
        ActualStudyTime secondConnectedActual = ActualStudyTime.builder()
                .id(3L)
                .studentId(1L)
                .assignedStudyTimeId(1L)
                .startTime(LocalDateTime.parse("2025-06-18T11:35:00"))
                .endTime(LocalDateTime.parse("2025-06-18T12:00:00"))
                .source("discord")
                .build();

        when(userRepository.findAllStudents()).thenReturn(Arrays.asList(testStudent));
        when(studentGuardianRepository.findByStudentId(1L)).thenReturn(Arrays.asList());
        when(assignedStudyTimeRepository.findByStudentIdAndStartTimeBetweenWithDetails(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testAssignedStudyTime));
        when(actualStudyTimeRepository.findByAssignedStudyTimeId(1L))
                .thenReturn(Arrays.asList(testConnectedActualStudyTime, secondConnectedActual));
        when(actualStudyTimeRepository.findByStudentIdAndDateRangeAndAssignedStudyTimeIdIsNull(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());

        // when
        StudyTimeMonitorResponse result = studyTimeService.getStudyTimeMonitorByDate(targetDate);

        // then
        assertThat(result.getStudents()).hasSize(1);
        StudyTimeMonitorResponse.StudentStudyInfo studentInfo = result.getStudents().get(0);
        assertThat(studentInfo.getAssignedStudyTimes()).hasSize(1);
        
        StudyTimeMonitorResponse.AssignedStudyInfo assignedInfo = studentInfo.getAssignedStudyTimes().get(0);
        assertThat(assignedInfo.getConnectedActualStudyTimes()).hasSize(2);
        
        // 첫 번째 접속 기록 확인
        StudyTimeMonitorResponse.ConnectedActualStudyInfo firstActual = assignedInfo.getConnectedActualStudyTimes().get(0);
        assertThat(firstActual.getActualStudyTimeId()).isEqualTo(1L);
        assertThat(firstActual.getStartTime()).isEqualTo(LocalDateTime.parse("2025-06-18T10:05:00"));
        
        // 두 번째 접속 기록 확인
        StudyTimeMonitorResponse.ConnectedActualStudyInfo secondActual = assignedInfo.getConnectedActualStudyTimes().get(1);
        assertThat(secondActual.getActualStudyTimeId()).isEqualTo(3L);
        assertThat(secondActual.getStartTime()).isEqualTo(LocalDateTime.parse("2025-06-18T11:35:00"));
    }
}
