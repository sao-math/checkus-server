package saomath.checkusserver.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.ActualStudyTime;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
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
@DisplayName("StudyTimeService 새 기능 테스트")
class StudyTimeServiceNewFeatureTest {

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
    @DisplayName("할당된 시간 범위 내 접속시 즉시 연결")
    void recordStudyStart_Success_WithinAssignedTime() {
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
    @DisplayName("할당된 시간 범위 밖 접속시 미연결")
    void recordStudyStart_Success_OutsideAssignedTime() {
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
    @DisplayName("세션 시작 시 연결 처리 - 할당된 공부시간을 찾을 수 없는 경우")
    void connectSessionOnStart_AssignedNotFound() {
        // Given
        Long assignedStudyTimeId = 999L;

        when(assignedStudyTimeRepository.findById(assignedStudyTimeId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            studyTimeService.connectSessionOnStart(assignedStudyTimeId);
        });

        verify(assignedStudyTimeRepository).findById(assignedStudyTimeId);
        verify(actualStudyTimeRepository, never())
                .findByStudentIdAndEndTimeIsNullOrderByStartTimeDesc(any());
        verify(actualStudyTimeRepository, never()).save(any(ActualStudyTime.class));
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
}
