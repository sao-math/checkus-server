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
    @DisplayName("공부 시간 배정 성공 (새로운 로직: 자동 연결하지 않음)")
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
                .id(100L)
                .studentId(studentId)
                .title("수학 공부")
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
        AssignedStudyTime result = studyTimeService.assignStudyTime(studentId, "수학 공부", activityId, startTime, endTime, teacherId);

        // Then
        assertNotNull(result);
        assertEquals(studentId, result.getStudentId());
        assertEquals("수학 공부", result.getTitle());
        assertEquals(activityId, result.getActivityId());
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertEquals(teacherId, result.getAssignedBy());

        verify(userRepository).existsById(studentId);
        verify(userRepository).existsById(teacherId);
        verify(activityRepository).findById(activityId);
        verify(assignedStudyTimeRepository).findOverlappingStudyTimes(studentId, startTime, endTime);
        verify(assignedStudyTimeRepository).save(any(AssignedStudyTime.class));
        
        // 새로운 로직에서는 자동 연결을 하지 않으므로 추가 메서드 호출이 없어야 함
        verify(assignedStudyTimeRepository, never()).findById(100L);
        verify(actualStudyTimeRepository, never()).findByStudentIdAndEndTimeIsNullOrderByStartTimeDesc(any());
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
            studyTimeService.assignStudyTime(studentId, "수학 공부", activityId, startTime, endTime, teacherId);
        });

        verify(assignedStudyTimeRepository, never()).save(any(AssignedStudyTime.class));
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
    @DisplayName("학생별 기간 공부 시간 조회 성공")
    void getAssignedStudyTimesByStudentAndDateRange_Success() {
        // Given
        Long studentId = 1L;
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(7);

        List<AssignedStudyTime> expectedResult = List.of(
                AssignedStudyTime.builder().studentId(studentId).title("수학 공부").build()
        );

        when(userRepository.existsById(studentId)).thenReturn(true);
        when(assignedStudyTimeRepository.findByStudentIdAndStartTimeBetweenWithDetails(studentId, startDate, endDate))
                .thenReturn(expectedResult);

        // When
        List<AssignedStudyTime> result = studyTimeService.getAssignedStudyTimesByStudentAndDateRange(
                studentId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).existsById(studentId);
        verify(assignedStudyTimeRepository).findByStudentIdAndStartTimeBetweenWithDetails(studentId, startDate, endDate);
    }

    @Test
    @DisplayName("전체 기간별 배정된 공부 시간 조회 성공")
    void getAssignedStudyTimesByDateRange_Success() {
        // Given
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(7);

        List<AssignedStudyTime> expectedResult = List.of(
                AssignedStudyTime.builder().studentId(1L).title("수학 공부").build(),
                AssignedStudyTime.builder().studentId(2L).title("영어 공부").build()
        );

        when(assignedStudyTimeRepository.findStartingBetweenWithDetails(startDate, endDate))
                .thenReturn(expectedResult);

        // When
        List<AssignedStudyTime> result = studyTimeService.getAssignedStudyTimesByDateRange(
                startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(assignedStudyTimeRepository).findStartingBetweenWithDetails(startDate, endDate);
    }

    @Test
    @DisplayName("디스코드 봇용 공부 시작 기록 - 할당된 시간 범위 내 접속")
    void recordStudyStart_WithinAssignedTime() {
        // Given
        Long studentId = 1L;
        LocalDateTime startTime = LocalDateTime.now();
        String source = "discord";
        Long assignedStudyTimeId = 10L;

        List<AssignedStudyTime> assignedList = List.of(
                AssignedStudyTime.builder()
                        .id(assignedStudyTimeId)
                        .studentId(studentId)
                        .startTime(startTime.minusMinutes(10))
                        .endTime(startTime.plusMinutes(50))
                        .build()
        );

        ActualStudyTime expectedResult = ActualStudyTime.builder()
                .studentId(studentId)
                .assignedStudyTimeId(assignedStudyTimeId)
                .startTime(startTime)
                .source(source)
                .build();

        when(userRepository.existsById(studentId)).thenReturn(true);
        when(assignedStudyTimeRepository.findByStudentIdAndTimeRange(studentId, startTime))
                .thenReturn(assignedList);
        when(actualStudyTimeRepository.save(any(ActualStudyTime.class))).thenReturn(expectedResult);

        // When
        ActualStudyTime result = studyTimeService.recordStudyStart(studentId, startTime, source);

        // Then
        assertNotNull(result);
        assertEquals(studentId, result.getStudentId());
        assertEquals(assignedStudyTimeId, result.getAssignedStudyTimeId());
        assertEquals(startTime, result.getStartTime());
        assertEquals(source, result.getSource());

        verify(userRepository).existsById(studentId);
        verify(assignedStudyTimeRepository).findByStudentIdAndTimeRange(studentId, startTime);
        verify(actualStudyTimeRepository).save(any(ActualStudyTime.class));
    }

    @Test
    @DisplayName("디스코드 봇용 공부 시작 기록 - 할당된 시간 범위 밖 접속")
    void recordStudyStart_OutsideAssignedTime() {
        // Given
        Long studentId = 1L;
        LocalDateTime startTime = LocalDateTime.now();
        String source = "discord";

        ActualStudyTime expectedResult = ActualStudyTime.builder()
                .studentId(studentId)
                .assignedStudyTimeId(null) // 할당되지 않음
                .startTime(startTime)
                .source(source)
                .build();

        when(userRepository.existsById(studentId)).thenReturn(true);
        when(assignedStudyTimeRepository.findByStudentIdAndTimeRange(studentId, startTime))
                .thenReturn(new ArrayList<>());
        when(actualStudyTimeRepository.save(any(ActualStudyTime.class))).thenReturn(expectedResult);

        // When
        ActualStudyTime result = studyTimeService.recordStudyStart(studentId, startTime, source);

        // Then
        assertNotNull(result);
        assertEquals(studentId, result.getStudentId());
        assertNull(result.getAssignedStudyTimeId()); // 할당되지 않음
        assertEquals(startTime, result.getStartTime());
        assertEquals(source, result.getSource());

        verify(userRepository).existsById(studentId);
        verify(assignedStudyTimeRepository).findByStudentIdAndTimeRange(studentId, startTime);
        verify(actualStudyTimeRepository).save(any(ActualStudyTime.class));
    }

    @Test
    @DisplayName("세션 시작 시 연결 처리 성공")
    void connectSessionOnStart_Success() {
        // Given
        Long assignedStudyTimeId = 1L;
        Long studentId = 10L;
        LocalDateTime assignedStartTime = LocalDateTime.now();
        LocalDateTime sessionStartTime = assignedStartTime.minusMinutes(30);

        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .id(assignedStudyTimeId)
                .studentId(studentId)
                .startTime(assignedStartTime)
                .endTime(assignedStartTime.plusHours(2))
                .build();

        ActualStudyTime ongoingSession = ActualStudyTime.builder()
                .id(100L)
                .studentId(studentId)
                .startTime(sessionStartTime)
                .endTime(null) // 아직 진행중
                .assignedStudyTimeId(null) // 아직 할당되지 않음
                .source("discord")
                .build();

        List<ActualStudyTime> ongoingSessions = List.of(ongoingSession);

        ActualStudyTime expectedResult = ActualStudyTime.builder()
                .id(100L)
                .studentId(studentId)
                .startTime(sessionStartTime)
                .endTime(null)
                .assignedStudyTimeId(assignedStudyTimeId) // 연결됨
                .source("discord")
                .build();

        when(assignedStudyTimeRepository.findById(assignedStudyTimeId))
                .thenReturn(Optional.of(assignedStudyTime));
        when(actualStudyTimeRepository.findByStudentIdAndEndTimeIsNullOrderByStartTimeDesc(studentId))
                .thenReturn(ongoingSessions);
        when(actualStudyTimeRepository.save(any(ActualStudyTime.class)))
                .thenReturn(expectedResult);

        // When
        ActualStudyTime result = studyTimeService.connectSessionOnStart(assignedStudyTimeId);

        // Then
        assertNotNull(result);
        assertEquals(studentId, result.getStudentId());
        assertEquals(assignedStudyTimeId, result.getAssignedStudyTimeId());
        assertEquals(sessionStartTime, result.getStartTime());
        assertNull(result.getEndTime());

        verify(assignedStudyTimeRepository).findById(assignedStudyTimeId);
        verify(actualStudyTimeRepository).findByStudentIdAndEndTimeIsNullOrderByStartTimeDesc(studentId);
        verify(actualStudyTimeRepository).save(any(ActualStudyTime.class));
    }

    @Test
    @DisplayName("세션 시작 시 연결 처리 - 진행중인 세션이 없는 경우")
    void connectSessionOnStart_NoSession() {
        // Given
        Long assignedStudyTimeId = 1L;
        Long studentId = 10L;
        LocalDateTime assignedStartTime = LocalDateTime.now();

        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .id(assignedStudyTimeId)
                .studentId(studentId)
                .startTime(assignedStartTime)
                .endTime(assignedStartTime.plusHours(2))
                .build();

        when(assignedStudyTimeRepository.findById(assignedStudyTimeId))
                .thenReturn(Optional.of(assignedStudyTime));
        when(actualStudyTimeRepository.findByStudentIdAndEndTimeIsNullOrderByStartTimeDesc(studentId))
                .thenReturn(new ArrayList<>());

        // When
        ActualStudyTime result = studyTimeService.connectSessionOnStart(assignedStudyTimeId);

        // Then
        assertNull(result);

        verify(assignedStudyTimeRepository).findById(assignedStudyTimeId);
        verify(actualStudyTimeRepository).findByStudentIdAndEndTimeIsNullOrderByStartTimeDesc(studentId);
        verify(actualStudyTimeRepository, never()).save(any(ActualStudyTime.class));
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
}
