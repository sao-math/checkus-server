package saomath.checkusserver.notification.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.entity.*;
import saomath.checkusserver.notification.service.MultiChannelNotificationService;
import saomath.checkusserver.notification.service.NotificationTargetService;
import saomath.checkusserver.service.StudyTimeService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("통합 알림 스케줄러 Mock 테스트")
class UnifiedNotificationSchedulerMockTest {

    @Mock
    private MultiChannelNotificationService notificationService;

    @Mock
    private NotificationTargetService targetService;

    @Mock
    private StudyTimeService studyTimeService;

    @InjectMocks
    private UnifiedNotificationScheduler scheduler;

    private User testStudent;
    private Activity testActivity;
    private AssignedStudyTime testAssignment;
    private ActualStudyTime testSession;

    @BeforeEach
    void setUp() {
        // 테스트 학생 설정
        testStudent = User.builder()
                .username("test_student")
                .name("테스트학생")
                .phoneNumber("010-1234-5678")
                .password("password")
                .discordId("123456789")
                .build();
        testStudent.setId(1L);

        // 테스트 활동 설정
        testActivity = Activity.builder()
                .name("수학")
                .isStudyAssignable(true)
                .build();
        testActivity.setId(1L);

        // 테스트 할당 설정
        testAssignment = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .assignedBy(testStudent.getId())
                .build();
        testAssignment.setId(1L);
        testAssignment.setStudent(testStudent);
        testAssignment.setActivity(testActivity);

        // 테스트 세션 설정
        testSession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(LocalDateTime.now().minusMinutes(10))
                .source("discord")
                .build();
        testSession.setId(1L);

        // Mock 기본 설정
        when(notificationService.sendNotification(any(), any(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(true));
    }

    @Test
    @DisplayName("공부 시작 시간 알림 - 정확한 시간 매칭")
    void sendStudyStartNotificationAndConnectSessions_ExactTimeMatching() {
        // Given
        NotificationTargetService.StudyTarget target = createMockStudyTarget();
        when(targetService.getStudyTargetsForTime(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(target));
        when(notificationService.sendNotification(anyLong(), anyString(), any(Map.class)))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(notificationService.sendNotificationToChannel(anyString(), anyString(), any(Map.class), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        // 스케줄러에서 getAssignedStudyTimesByDateRange가 한 번 호출됨
        when(studyTimeService.getAssignedStudyTimesByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList());
        when(studyTimeService.connectSessionOnStart(anyLong()))
            .thenReturn(null);

        // When
        scheduler.sendStudyStartNotificationAndConnectSessions();

        // Then
        verify(targetService).getStudyTargetsForTime(argThat(dateTime -> 
            dateTime.getSecond() == 0 && dateTime.getNano() == 0
        ));
        verify(notificationService, times(1)).sendNotification(anyLong(), anyString(), any(Map.class));
        verify(notificationService, times(1)).sendNotificationToChannel(anyString(), anyString(), any(Map.class), any());
        // 한 번만 호출됨
        verify(studyTimeService, times(1)).getAssignedStudyTimesByDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("스케줄러 실행 시 세션 연결이 올바르게 수행되어야 함")
    void shouldConnectSessionsCorrectly() {
        // Given: 현재 시간에 시작하는 할당과 연결될 세션
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        testAssignment.setStartTime(now);
        testSession.setStartTime(now.minusMinutes(10));
        testSession.setAssignedStudyTimeId(testAssignment.getId());
        
        // 현재 시간 범위 조회
        when(studyTimeService.getAssignedStudyTimesByDateRange(
                eq(now.minusMinutes(1)), eq(now.plusMinutes(1))))
                .thenReturn(Arrays.asList(testAssignment));
                
        when(studyTimeService.connectSessionOnStart(testAssignment.getId()))
                .thenReturn(testSession);

        // 알림 대상 Mock 설정
        NotificationTargetService.StudyTarget target = NotificationTargetService.StudyTarget.builder()
                .studentId(testStudent.getId())
                .studentName(testStudent.getName())
                .startTime(now)
                .endTime(now.plusHours(1))
                .studentNotificationEnabled(true)
                .parentNotificationEnabled(false)
                .build();

        when(targetService.getStudyTargetsForTime(now)).thenReturn(Arrays.asList(target));

        // When: 스케줄러 실행
        scheduler.sendStudyStartNotificationAndConnectSessions();

        // Then: 호출 순서와 횟수 검증
        verify(studyTimeService, times(1)).getAssignedStudyTimesByDateRange(any(), any());
        verify(studyTimeService).connectSessionOnStart(testAssignment.getId());
        verify(notificationService).sendNotification(eq(testStudent.getId()), any(), any());
    }

    @Test
    @DisplayName("여러 학생의 세션을 각각 올바르게 연결해야 함")
    void shouldConnectSessionsForMultipleStudentsCorrectly() {
        // Given: 두 명의 학생과 각각의 할당
        User student2 = User.builder()
                .username("test_student2")
                .name("테스트학생2")
                .phoneNumber("010-9876-5432")
                .password("password")
                .discordId("987654321")
                .build();
        student2.setId(2L);

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        
        AssignedStudyTime assignment1 = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(now)
                .endTime(now.plusHours(1))
                .assignedBy(testStudent.getId())
                .build();
        assignment1.setId(1L);
        assignment1.setStudent(testStudent);

        AssignedStudyTime assignment2 = AssignedStudyTime.builder()
                .studentId(student2.getId())
                .title("영어 공부")
                .activityId(testActivity.getId())
                .startTime(now)
                .endTime(now.plusHours(1))
                .assignedBy(student2.getId())
                .build();
        assignment2.setId(2L);
        assignment2.setStudent(student2);

        ActualStudyTime session1 = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(now.minusMinutes(15))
                .source("discord")
                .build();
        session1.setId(1L);
        session1.setAssignedStudyTimeId(assignment1.getId());

        ActualStudyTime session2 = ActualStudyTime.builder()
                .studentId(student2.getId())
                .startTime(now.minusMinutes(5))
                .source("discord")
                .build();
        session2.setId(2L);
        session2.setAssignedStudyTimeId(assignment2.getId());

        // 현재 시간 범위 조회
        when(studyTimeService.getAssignedStudyTimesByDateRange(
                eq(now.minusMinutes(1)), eq(now.plusMinutes(1))))
                .thenReturn(Arrays.asList(assignment1, assignment2));
                
        when(studyTimeService.connectSessionOnStart(assignment1.getId()))
                .thenReturn(session1);
        when(studyTimeService.connectSessionOnStart(assignment2.getId()))
                .thenReturn(session2);

        // Mock 설정 (알림 대상 없음)
        when(targetService.getStudyTargetsForTime(now)).thenReturn(Collections.emptyList());

        // When: 스케줄러 실행
        scheduler.sendStudyStartNotificationAndConnectSessions();

        // Then: 각 학생의 세션이 올바른 할당에 연결되어야 함
        verify(studyTimeService).connectSessionOnStart(assignment1.getId());
        verify(studyTimeService).connectSessionOnStart(assignment2.getId());
    }

    @Test
    @DisplayName("여러 할당이 동시에 시작할 때 모두 올바르게 처리되어야 함")
    void shouldHandleMultipleSimultaneousAssignments() {
        // Given: 동시에 시작하는 두 할당
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        // 첫 번째 할당
        AssignedStudyTime assignment1 = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(now)
                .endTime(now.plusHours(1))
                .assignedBy(testStudent.getId())
                .build();
        assignment1.setId(1L);
        assignment1.setStudent(testStudent);

        // 두 번째 할당 (같은 시간)
        AssignedStudyTime assignment2 = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("영어 공부")
                .activityId(testActivity.getId())
                .startTime(now)
                .endTime(now.plusHours(1))
                .assignedBy(testStudent.getId())
                .build();
        assignment2.setId(2L);
        assignment2.setStudent(testStudent);

        // 각각에 연결될 세션들
        ActualStudyTime session1 = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(now.minusMinutes(5))
                .source("discord")
                .build();
        session1.setId(1L);
        session1.setAssignedStudyTimeId(assignment1.getId());

        ActualStudyTime session2 = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(now.minusMinutes(10))
                .source("discord")
                .build();
        session2.setId(2L);
        session2.setAssignedStudyTimeId(assignment2.getId());

        // 현재 시간 범위 조회
        when(studyTimeService.getAssignedStudyTimesByDateRange(
                eq(now.minusMinutes(1)), eq(now.plusMinutes(1))))
                .thenReturn(Arrays.asList(assignment1, assignment2));
                
        when(studyTimeService.connectSessionOnStart(assignment1.getId()))
                .thenReturn(session1);
        when(studyTimeService.connectSessionOnStart(assignment2.getId()))
                .thenReturn(session2);

        // Mock 설정 (알림 대상 없음)
        when(targetService.getStudyTargetsForTime(now)).thenReturn(Collections.emptyList());

        // When: 스케줄러 실행
        scheduler.sendStudyStartNotificationAndConnectSessions();

        // Then: 두 할당 모두 적절하게 처리되어야 함
        verify(studyTimeService).connectSessionOnStart(assignment1.getId());
        verify(studyTimeService).connectSessionOnStart(assignment2.getId());
    }

    @Test
    @DisplayName("알림 발송 시 세션 연결이 먼저 수행되어야 함")
    void shouldConnectSessionsBeforeNotification() {
        // Given: 현재 시간 할당과 알림 대상
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        testAssignment.setStartTime(now);
        
        // 세션 연결 Mock 설정
        when(studyTimeService.getAssignedStudyTimesByDateRange(
                eq(now.minusMinutes(1)), eq(now.plusMinutes(1))))
                .thenReturn(Arrays.asList(testAssignment));
                
        when(studyTimeService.connectSessionOnStart(testAssignment.getId()))
                .thenReturn(testSession);

        // 알림 대상 Mock 설정
        NotificationTargetService.StudyTarget target = NotificationTargetService.StudyTarget.builder()
                .studentId(testStudent.getId())
                .studentName(testStudent.getName())
                .startTime(now)
                .endTime(now.plusHours(1))
                .studentNotificationEnabled(true)
                .parentNotificationEnabled(false)
                .build();

        when(targetService.getStudyTargetsForTime(now)).thenReturn(Arrays.asList(target));

        // When: 스케줄러 실행
        scheduler.sendStudyStartNotificationAndConnectSessions();

        // Then: 세션 연결이 알림 발송보다 먼저 호출되어야 함
        verify(studyTimeService).connectSessionOnStart(testAssignment.getId());
        verify(notificationService).sendNotification(eq(testStudent.getId()), any(), any());
        
        // 호출 순서 검증을 위해 InOrder 사용
        var inOrder = inOrder(studyTimeService, notificationService);
        inOrder.verify(studyTimeService).connectSessionOnStart(testAssignment.getId());
        inOrder.verify(notificationService).sendNotification(eq(testStudent.getId()), any(), any());
    }

    private NotificationTargetService.StudyTarget createMockStudyTarget() {
        return NotificationTargetService.StudyTarget.builder()
                .studentId(testStudent.getId())
                .studentName(testStudent.getName())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .studentNotificationEnabled(true)
                .parentNotificationEnabled(true)
                .parentPhone("010-9999-9999")
                .build();
    }
}
