package saomath.checkusserver.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.entity.Activity;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.ActualStudyTime;
import saomath.checkusserver.exception.BusinessException;
import saomath.checkusserver.exception.ResourceNotFoundException;
import saomath.checkusserver.repository.ActivityRepository;
import saomath.checkusserver.repository.AssignedStudyTimeRepository;
import saomath.checkusserver.repository.ActualStudyTimeRepository;
import saomath.checkusserver.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudyTimeService 테스트")
class StudyTimeServiceTest {

    @Mock
    private AssignedStudyTimeRepository assignedStudyTimeRepository;
    
    @Mock
    private ActualStudyTimeRepository actualStudyTimeRepository;
    
    @Mock
    private ActivityRepository activityRepository;
    
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudyTimeService studyTimeService;

    @Test
    @DisplayName("공부 시간 배정 성공")
    void assignStudyTime_Success() {
        // Given
        Long studentId = 1L;
        Long activityId = 1L;
        Long teacherId = 2L;
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusHours(2);

        Activity activity = Activity.builder()
                .id(activityId)
                .name("수학 공부")
                .isStudyAssignable(true)
                .build();

        AssignedStudyTime expectedResult = AssignedStudyTime.builder()
                .studentId(studentId)
                .activityId(activityId)
                .startTime(startTime)
                .endTime(endTime)
                .assignedBy(teacherId)
                .build();

        when(userRepository.existsById(studentId)).thenReturn(true);
        when(userRepository.existsById(teacherId)).thenReturn(true);
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(assignedStudyTimeRepository.findOverlappingStudyTimes(studentId, startTime, endTime))
                .thenReturn(new ArrayList<>());
        when(assignedStudyTimeRepository.save(any(AssignedStudyTime.class))).thenReturn(expectedResult);

        // When
        AssignedStudyTime result = studyTimeService.assignStudyTime(studentId, activityId, startTime, endTime, teacherId);

        // Then
        assertNotNull(result);
        assertEquals(studentId, result.getStudentId());
        assertEquals(activityId, result.getActivityId());
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertEquals(teacherId, result.getAssignedBy());

        verify(userRepository).existsById(studentId);
        verify(userRepository).existsById(teacherId);
        verify(activityRepository).findById(activityId);
        verify(assignedStudyTimeRepository).findOverlappingStudyTimes(studentId, startTime, endTime);
        verify(assignedStudyTimeRepository).save(any(AssignedStudyTime.class));
    }

    @Test
    @DisplayName("공부 시간 배정 실패 - 시간 겹침")
    void assignStudyTime_Fail_TimeOverlap() {
        // Given
        Long studentId = 1L;
        Long activityId = 1L;
        Long teacherId = 2L;
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusHours(2);

        Activity activity = Activity.builder()
                .id(activityId)
                .name("수학 공부")
                .isStudyAssignable(true)
                .build();

        List<AssignedStudyTime> overlappingTimes = List.of(
                AssignedStudyTime.builder().id(1L).studentId(studentId).build()
        );

        when(userRepository.existsById(studentId)).thenReturn(true);
        when(userRepository.existsById(teacherId)).thenReturn(true);
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(assignedStudyTimeRepository.findOverlappingStudyTimes(studentId, startTime, endTime))
                .thenReturn(overlappingTimes);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            studyTimeService.assignStudyTime(studentId, activityId, startTime, endTime, teacherId);
        });

        verify(assignedStudyTimeRepository, never()).save(any(AssignedStudyTime.class));
    }

    @Test
    @DisplayName("공부 시간 배정 실패 - 존재하지 않는 학생")
    void assignStudyTime_Fail_StudentNotFound() {
        // Given
        Long studentId = 999L;
        Long activityId = 1L;
        Long teacherId = 2L;
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusHours(2);

        when(userRepository.existsById(studentId)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            studyTimeService.assignStudyTime(studentId, activityId, startTime, endTime, teacherId);
        });

        verify(assignedStudyTimeRepository, never()).save(any(AssignedStudyTime.class));
    }

    @Test
    @DisplayName("공부 시간 배정 실패 - 배정 불가능한 활동")
    void assignStudyTime_Fail_ActivityNotAssignable() {
        // Given
        Long studentId = 1L;
        Long activityId = 1L;
        Long teacherId = 2L;
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusHours(2);

        Activity activity = Activity.builder()
                .id(activityId)
                .name("휴식")
                .isStudyAssignable(false)
                .build();

        when(userRepository.existsById(studentId)).thenReturn(true);
        when(userRepository.existsById(teacherId)).thenReturn(true);
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

        // When & Then
        assertThrows(BusinessException.class, () -> {
            studyTimeService.assignStudyTime(studentId, activityId, startTime, endTime, teacherId);
        });

        verify(assignedStudyTimeRepository, never()).save(any(AssignedStudyTime.class));
    }

    @Test
    @DisplayName("배정된 공부 시간 수정 성공")
    void updateAssignedStudyTime_Success() {
        // Given
        Long assignedId = 1L;
        Long activityId = 2L;
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusHours(2);

        AssignedStudyTime existing = AssignedStudyTime.builder()
                .id(assignedId)
                .studentId(1L)
                .activityId(1L)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .assignedBy(2L)
                .build();

        Activity activity = Activity.builder()
                .id(activityId)
                .name("영어 공부")
                .isStudyAssignable(true)
                .build();

        when(assignedStudyTimeRepository.findById(assignedId)).thenReturn(Optional.of(existing));
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(assignedStudyTimeRepository.findOverlappingStudyTimes(1L, startTime, endTime))
                .thenReturn(new ArrayList<>());
        when(assignedStudyTimeRepository.save(any(AssignedStudyTime.class))).thenReturn(existing);

        // When
        AssignedStudyTime result = studyTimeService.updateAssignedStudyTime(assignedId, activityId, startTime, endTime);

        // Then
        assertNotNull(result);
        verify(assignedStudyTimeRepository).findById(assignedId);
        verify(activityRepository).findById(activityId);
        verify(assignedStudyTimeRepository).save(any(AssignedStudyTime.class));
    }

    @Test
    @DisplayName("배정된 공부 시간 삭제 성공")
    void deleteAssignedStudyTime_Success() {
        // Given
        Long assignedId = 1L;
        when(assignedStudyTimeRepository.existsById(assignedId)).thenReturn(true);

        // When
        studyTimeService.deleteAssignedStudyTime(assignedId);

        // Then
        verify(assignedStudyTimeRepository).existsById(assignedId);
        verify(assignedStudyTimeRepository).deleteById(assignedId);
    }

    @Test
    @DisplayName("배정된 공부 시간 삭제 실패 - 존재하지 않는 ID")
    void deleteAssignedStudyTime_Fail_NotFound() {
        // Given
        Long assignedId = 999L;
        when(assignedStudyTimeRepository.existsById(assignedId)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            studyTimeService.deleteAssignedStudyTime(assignedId);
        });

        verify(assignedStudyTimeRepository, never()).deleteById(assignedId);
    }

    @Test
    @DisplayName("학생별 기간 공부 시간 조회 성공")
    void getAssignedStudyTimesByStudentAndDateRange_Success() {
        // Given
        Long studentId = 1L;
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(7);

        List<AssignedStudyTime> expectedResult = List.of(
                AssignedStudyTime.builder().studentId(studentId).build()
        );

        when(userRepository.existsById(studentId)).thenReturn(true);
        when(assignedStudyTimeRepository.findByStudentIdAndStartTimeBetween(studentId, startDate, endDate))
                .thenReturn(expectedResult);

        // When
        List<AssignedStudyTime> result = studyTimeService.getAssignedStudyTimesByStudentAndDateRange(
                studentId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).existsById(studentId);
        verify(assignedStudyTimeRepository).findByStudentIdAndStartTimeBetween(studentId, startDate, endDate);
    }

    @Test
    @DisplayName("디스코드 봇용 공부 시작 기록 성공")
    void recordStudyStart_Success() {
        // Given
        Long studentId = 1L;
        LocalDateTime startTime = LocalDateTime.now();
        String source = "discord";

        ActualStudyTime expectedResult = ActualStudyTime.builder()
                .studentId(studentId)
                .startTime(startTime)
                .source(source)
                .build();

        when(userRepository.existsById(studentId)).thenReturn(true);
        when(assignedStudyTimeRepository.findByStudentIdAndStartTimeBetween(
                eq(studentId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        when(actualStudyTimeRepository.save(any(ActualStudyTime.class))).thenReturn(expectedResult);

        // When
        ActualStudyTime result = studyTimeService.recordStudyStart(studentId, startTime, source);

        // Then
        assertNotNull(result);
        assertEquals(studentId, result.getStudentId());
        assertEquals(startTime, result.getStartTime());
        assertEquals(source, result.getSource());

        verify(userRepository).existsById(studentId);
        verify(actualStudyTimeRepository).save(any(ActualStudyTime.class));
    }

    @Test
    @DisplayName("디스코드 봇용 공부 종료 기록 성공")
    void recordStudyEnd_Success() {
        // Given
        Long actualStudyTimeId = 1L;
        LocalDateTime endTime = LocalDateTime.now();

        ActualStudyTime existing = ActualStudyTime.builder()
                .id(actualStudyTimeId)
                .studentId(1L)
                .startTime(LocalDateTime.now().minusHours(1))
                .source("discord")
                .build();

        when(actualStudyTimeRepository.findById(actualStudyTimeId)).thenReturn(Optional.of(existing));
        when(actualStudyTimeRepository.save(any(ActualStudyTime.class))).thenReturn(existing);

        // When
        ActualStudyTime result = studyTimeService.recordStudyEnd(actualStudyTimeId, endTime);

        // Then
        assertNotNull(result);
        verify(actualStudyTimeRepository).findById(actualStudyTimeId);
        verify(actualStudyTimeRepository).save(any(ActualStudyTime.class));
    }

    @Test
    @DisplayName("알림용 곧 시작할 공부 시간 조회 성공")
    void getUpcomingStudyTimes_Success() {
        // Given
        List<AssignedStudyTime> expectedResult = List.of(
                AssignedStudyTime.builder().id(1L).build(),
                AssignedStudyTime.builder().id(2L).build()
        );

        when(assignedStudyTimeRepository.findUpcomingStudyTimesV2(
                any(LocalDateTime.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(expectedResult);

        // When
        List<AssignedStudyTime> result = studyTimeService.getUpcomingStudyTimes();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(assignedStudyTimeRepository).findUpcomingStudyTimesV2(
                any(LocalDateTime.class), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("공부 배정 가능한 활동 목록 조회 성공")
    void getStudyAssignableActivities_Success() {
        // Given
        List<Activity> expectedResult = List.of(
                Activity.builder().id(1L).name("수학").isStudyAssignable(true).build(),
                Activity.builder().id(2L).name("영어").isStudyAssignable(true).build()
        );

        when(activityRepository.findByIsStudyAssignableTrue()).thenReturn(expectedResult);

        // When
        List<Activity> result = studyTimeService.getStudyAssignableActivities();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(Activity::getIsStudyAssignable));
        verify(activityRepository).findByIsStudyAssignableTrue();
    }

    @Test
    @DisplayName("활동 생성 성공")
    void createActivity_Success() {
        // Given
        String name = "과학 공부";
        Boolean isStudyAssignable = true;

        Activity expectedResult = Activity.builder()
                .id(1L)
                .name(name)
                .isStudyAssignable(isStudyAssignable)
                .build();

        when(activityRepository.existsByName(name)).thenReturn(false);
        when(activityRepository.save(any(Activity.class))).thenReturn(expectedResult);

        // When
        Activity result = studyTimeService.createActivity(name, isStudyAssignable);

        // Then
        assertNotNull(result);
        assertEquals(name, result.getName());
        assertEquals(isStudyAssignable, result.getIsStudyAssignable());

        verify(activityRepository).existsByName(name);
        verify(activityRepository).save(any(Activity.class));
    }

    @Test
    @DisplayName("활동 생성 실패 - 중복된 이름")
    void createActivity_Fail_DuplicateName() {
        // Given
        String name = "수학 공부";
        Boolean isStudyAssignable = true;

        when(activityRepository.existsByName(name)).thenReturn(true);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            studyTimeService.createActivity(name, isStudyAssignable);
        });

        verify(activityRepository).existsByName(name);
        verify(activityRepository, never()).save(any(Activity.class));
    }

    @Test
    @DisplayName("잘못된 시간 범위로 공부 시간 배정 실패")
    void assignStudyTime_Fail_InvalidTimeRange() {
        // Given
        Long studentId = 1L;
        Long activityId = 1L;
        Long teacherId = 2L;
        LocalDateTime startTime = LocalDateTime.now().plusHours(2);
        LocalDateTime endTime = LocalDateTime.now().plusHours(1); // 시작 시간보다 이른 종료 시간

        when(userRepository.existsById(studentId)).thenReturn(true);
        when(userRepository.existsById(teacherId)).thenReturn(true);

        Activity activity = Activity.builder()
                .id(activityId)
                .name("수학 공부")
                .isStudyAssignable(true)
                .build();
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

        // When & Then
        assertThrows(BusinessException.class, () -> {
            studyTimeService.assignStudyTime(studentId, activityId, startTime, endTime, teacherId);
        });

        verify(assignedStudyTimeRepository, never()).save(any(AssignedStudyTime.class));
    }
}
