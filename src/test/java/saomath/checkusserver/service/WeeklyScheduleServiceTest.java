package saomath.checkusserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.dto.WeeklySchedulePeriodResponse;
import saomath.checkusserver.dto.WeeklyScheduleRequest;
import saomath.checkusserver.dto.WeeklyScheduleResponse;
import saomath.checkusserver.entity.Activity;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.entity.WeeklySchedule;
import saomath.checkusserver.common.exception.BusinessException;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.repository.ActivityRepository;
import saomath.checkusserver.repository.UserRepository;
import saomath.checkusserver.repository.WeeklyScheduleRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyScheduleServiceTest {

    @Mock
    private WeeklyScheduleRepository weeklyScheduleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private WeeklyScheduleService weeklyScheduleService;

    private WeeklySchedule mockSchedule;
    private WeeklyScheduleRequest mockRequest;
    private User mockStudent;
    private Activity mockActivity;

    @BeforeEach
    void setUp() {
        // Mock 데이터 설정
        mockStudent = User.builder()
                .id(1L)
                .name("김학생")
                .build();

        mockActivity = Activity.builder()
                .id(1L)
                .name("수학 공부")
                .isStudyAssignable(true)
                .build();

        mockSchedule = WeeklySchedule.builder()
                .id(1L)
                .studentId(1L)
                .title("수학 공부")
                .activityId(1L)
                .dayOfWeek(1) // 월요일
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .student(mockStudent)
                .activity(mockActivity)
                .build();

        mockRequest = new WeeklyScheduleRequest(
                1L, // studentId
                "수학 공부", // title
                1L, // activityId
                1,  // dayOfWeek (월요일)
                LocalTime.of(9, 0),   // startTime
                LocalTime.of(10, 30)  // endTime
        );
    }

    @Test
    @DisplayName("학생 주간 시간표 조회 - 성공")
    void getWeeklyScheduleByStudent_Success() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        when(weeklyScheduleRepository.findByStudentIdWithDetails(1L))
                .thenReturn(Arrays.asList(mockSchedule));
        when(activityRepository.findById(1L)).thenReturn(Optional.of(mockActivity)); // Mock 추가

        // When
        List<WeeklyScheduleResponse> result = weeklyScheduleService.getWeeklyScheduleByStudent(1L);

        // Then
        assertThat(result).hasSize(1);
        WeeklyScheduleResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStudentId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("수학 공부");
        assertThat(response.getActivityName()).isEqualTo("수학 공부");
        assertThat(response.getIsStudyAssignable()).isEqualTo(true);
        assertThat(response.getDayOfWeek()).isEqualTo(1);
        assertThat(response.getDayOfWeekName()).isEqualTo("월요일");
    }

    @Test
    @DisplayName("학생 주간 시간표 조회 - 학생을 찾을 수 없음")
    void getWeeklyScheduleByStudent_StudentNotFound() {
        // Given
        when(userRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> weeklyScheduleService.getWeeklyScheduleByStudent(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("학생을 찾을 수 없습니다. ID: 999");
    }

    @Test
    @DisplayName("주간 시간표 등록 - 성공")
    void createWeeklySchedule_Success() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        when(activityRepository.existsById(1L)).thenReturn(true);
        when(weeklyScheduleRepository.countOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(0L);
        when(weeklyScheduleRepository.save(any(WeeklySchedule.class)))
                .thenReturn(mockSchedule);
        when(weeklyScheduleRepository.findById(1L))
                .thenReturn(Optional.of(mockSchedule));
        when(activityRepository.findById(1L)).thenReturn(Optional.of(mockActivity)); // Mock 추가

        // When
        WeeklyScheduleResponse result = weeklyScheduleService.createWeeklySchedule(mockRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStudentId()).isEqualTo(1L);
        assertThat(result.getActivityId()).isEqualTo(1L);
        assertThat(result.getDayOfWeek()).isEqualTo(1);
        verify(weeklyScheduleRepository).save(any(WeeklySchedule.class));
    }

    @Test
    @DisplayName("주간 시간표 등록 - 학생을 찾을 수 없음")
    void createWeeklySchedule_StudentNotFound() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> weeklyScheduleService.createWeeklySchedule(mockRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("학생을 찾을 수 없습니다. ID: 1");
    }

    @Test
    @DisplayName("주간 시간표 등록 - 활동을 찾을 수 없음")
    void createWeeklySchedule_ActivityNotFound() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        when(activityRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> weeklyScheduleService.createWeeklySchedule(mockRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("활동을 찾을 수 없습니다. ID: 1");
    }

    @Test
    @DisplayName("주간 시간표 등록 - 시간 겹침")
    void createWeeklySchedule_TimeOverlap() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        when(activityRepository.existsById(1L)).thenReturn(true);
        when(weeklyScheduleRepository.countOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(1L); // 겹치는 시간표 존재

        // When & Then
        assertThatThrownBy(() -> weeklyScheduleService.createWeeklySchedule(mockRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 시간대에 이미 등록된 시간표가 있습니다.");
    }

    @Test
    @DisplayName("주간 시간표 등록 - 잘못된 시간 (시작시간이 종료시간보다 늦음)")
    void createWeeklySchedule_InvalidTime() {
        // Given
        WeeklyScheduleRequest invalidRequest = new WeeklyScheduleRequest(
                1L, "수학 공부", 1L, 1,
                LocalTime.of(11, 0),  // 시작시간
                LocalTime.of(10, 0)   // 종료시간 (시작시간보다 빠름)
        );
        when(userRepository.existsById(1L)).thenReturn(true);
        when(activityRepository.existsById(1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> weeklyScheduleService.createWeeklySchedule(invalidRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("시작 시간은 종료 시간보다 빨라야 합니다.");
    }

    @Test
    @DisplayName("주간 시간표 수정 - 성공")
    void updateWeeklySchedule_Success() {
        // Given
        when(weeklyScheduleRepository.findById(1L)).thenReturn(Optional.of(mockSchedule));
        when(userRepository.existsById(1L)).thenReturn(true);
        when(activityRepository.existsById(1L)).thenReturn(true);
        when(weeklyScheduleRepository.countOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(0L);
        when(weeklyScheduleRepository.save(any(WeeklySchedule.class)))
                .thenReturn(mockSchedule);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(mockActivity)); // Mock 추가

        // When
        WeeklyScheduleResponse result = weeklyScheduleService.updateWeeklySchedule(1L, mockRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(weeklyScheduleRepository).save(any(WeeklySchedule.class));
    }

    @Test
    @DisplayName("주간 시간표 수정 - 시간표를 찾을 수 없음")
    void updateWeeklySchedule_ScheduleNotFound() {
        // Given
        when(weeklyScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> weeklyScheduleService.updateWeeklySchedule(999L, mockRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("시간표를 찾을 수 없습니다. ID: 999");
    }

    @Test
    @DisplayName("주간 시간표 삭제 - 성공")
    void deleteWeeklySchedule_Success() {
        // Given
        when(weeklyScheduleRepository.existsById(1L)).thenReturn(true);

        // When
        weeklyScheduleService.deleteWeeklySchedule(1L);

        // Then
        verify(weeklyScheduleRepository).deleteById(1L);
    }

    @Test
    @DisplayName("주간 시간표 삭제 - 시간표를 찾을 수 없음")
    void deleteWeeklySchedule_ScheduleNotFound() {
        // Given
        when(weeklyScheduleRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> weeklyScheduleService.deleteWeeklySchedule(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("시간표를 찾을 수 없습니다. ID: 999");
    }

    @Test
    @DisplayName("특정 기간 시간표 조회 - 성공")
    void getWeeklyScheduleForPeriod_Success() {
        // Given
        LocalDate startDate = LocalDate.of(2025, 6, 2); // 월요일
        when(weeklyScheduleRepository.findByStudentIdWithDetails(1L))
                .thenReturn(Arrays.asList(mockSchedule));
        when(activityRepository.findById(1L)).thenReturn(Optional.of(mockActivity)); // Mock 추가

        // When
        List<WeeklySchedulePeriodResponse> result = weeklyScheduleService.getWeeklyScheduleForPeriod(
                1L, startDate, 7);

        // Then
        assertThat(result).hasSize(1);
        WeeklySchedulePeriodResponse response = result.get(0);
        assertThat(response.getStudentId()).isEqualTo(1L);
        assertThat(response.getActivityId()).isEqualTo(1L);
        assertThat(response.getDayOfWeek()).isEqualTo(1);
        assertThat(response.getActualStartTime().toLocalDate()).isEqualTo(startDate);
        assertThat(response.getActualStartTime().toLocalTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    @DisplayName("특정 기간 시간표 조회 - 해당 요일 없음")
    void getWeeklyScheduleForPeriod_NoMatchingDay() {
        // Given
        LocalDate startDate = LocalDate.of(2025, 6, 3); // 화요일 (시간표는 월요일만 있음)
        when(weeklyScheduleRepository.findByStudentIdWithDetails(1L))
                .thenReturn(Arrays.asList(mockSchedule)); // 월요일 시간표만 존재

        // When
        List<WeeklySchedulePeriodResponse> result = weeklyScheduleService.getWeeklyScheduleForPeriod(
                1L, startDate, 1); // 화요일 하루만 조회

        // Then
        assertThat(result).isEmpty(); // 화요일에 해당하는 시간표가 없음
    }
}
