package saomath.checkusserver.notification.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.notification.service.MultiChannelNotificationService;
import saomath.checkusserver.notification.service.NotificationTargetService;
import saomath.checkusserver.service.StudyTimeService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("통합 알림 스케줄러 테스트")
class UnifiedNotificationSchedulerTest {

    @Mock
    private MultiChannelNotificationService notificationService;
    
    @Mock
    private NotificationTargetService targetService;
    
    @Mock
    private StudyTimeService studyTimeService;
    
    private UnifiedNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new UnifiedNotificationScheduler(notificationService, targetService, studyTimeService);
    }

    @Test
    @DisplayName("공부 시작 10분 전 알림 - 정상 처리")
    void sendStudyReminder10Min_Success() {
        // Given
        NotificationTargetService.StudyTarget target = createMockStudyTarget();
        when(targetService.getStudyTargetsForTime(any(LocalDateTime.class)))
            .thenReturn(List.of(target));
        when(notificationService.sendNotification(anyLong(), anyString(), any(Map.class)))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(notificationService.sendNotificationToChannel(anyString(), anyString(), any(Map.class), any()))
            .thenReturn(CompletableFuture.completedFuture(true));

        // When
        scheduler.sendStudyReminder10Min();

        // Then
        verify(targetService).getStudyTargetsForTime(any(LocalDateTime.class));
        verify(notificationService, times(1)).sendNotification(anyLong(), anyString(), any(Map.class));
        verify(notificationService, times(1)).sendNotificationToChannel(anyString(), anyString(), any(Map.class), any());
    }

    @Test
    @DisplayName("공부 시작 시간 알림 - 정상 처리")
    void sendStudyStartNotificationAndConnectSessions_Success() {
        // Given
        NotificationTargetService.StudyTarget target = createMockStudyTarget();
        when(targetService.getStudyTargetsForTime(any(LocalDateTime.class)))
            .thenReturn(List.of(target));
        when(notificationService.sendNotification(anyLong(), anyString(), any(Map.class)))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(notificationService.sendNotificationToChannel(anyString(), anyString(), any(Map.class), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(studyTimeService.getAssignedStudyTimesByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of());

        // When
        scheduler.sendStudyStartNotificationAndConnectSessions();

        // Then
        verify(targetService).getStudyTargetsForTime(any(LocalDateTime.class));
        verify(notificationService, times(1)).sendNotification(anyLong(), anyString(), any(Map.class));
        verify(notificationService, times(1)).sendNotificationToChannel(anyString(), anyString(), any(Map.class), any());
        verify(studyTimeService).getAssignedStudyTimesByDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
    }

//    @Test
//    @DisplayName("오늘의 할일 알림 - 정상 처리")
//    void sendTodayTasksNotification_Success() {
//        // Given
//        NotificationTargetService.TaskTarget target = createMockTaskTarget();
//        when(targetService.getTodayTaskTargets())
//            .thenReturn(List.of(target));
//        when(notificationService.sendNotification(anyLong(), anyString(), any(Map.class)))
//            .thenReturn(CompletableFuture.completedFuture(true));
//        when(notificationService.sendNotificationToChannel(anyString(), anyString(), any(Map.class), any()))
//            .thenReturn(CompletableFuture.completedFuture(true));
//
//        // When
//        scheduler.sendTodayTasksNotification();
//
//        // Then
//        verify(targetService).getTodayTaskTargets();
//        verify(notificationService, times(1)).sendNotification(anyLong(), anyString(), any(Map.class));
//        verify(notificationService, times(1)).sendNotificationToChannel(anyString(), anyString(), any(Map.class), any());
//    }

    @Test
    @DisplayName("미접속 체크 - 정상 처리")
    void checkNoShow_Success() {
        // Given
        NotificationTargetService.NoShowTarget target = createMockNoShowTarget();
        when(targetService.getNoShowTargets(any(LocalDateTime.class)))
            .thenReturn(List.of(target));
        when(notificationService.sendNotification(anyLong(), anyString(), any(Map.class)))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(notificationService.sendNotificationToChannel(anyString(), anyString(), any(Map.class), any()))
            .thenReturn(CompletableFuture.completedFuture(true));

        // When
        scheduler.checkNoShow();

        // Then
        verify(targetService).getNoShowTargets(any(LocalDateTime.class));
        verify(notificationService, times(1)).sendNotification(anyLong(), anyString(), any(Map.class));
        verify(notificationService, times(1)).sendNotificationToChannel(anyString(), anyString(), any(Map.class), any());
    }

    @Test
    @DisplayName("알림 전송 실패 시 로깅")
    void sendNotification_Failure() {
        // Given
        NotificationTargetService.StudyTarget target = createMockStudyTarget();
        when(targetService.getStudyTargetsForTime(any(LocalDateTime.class)))
            .thenReturn(List.of(target));
        when(notificationService.sendNotification(anyLong(), anyString(), any(Map.class)))
            .thenReturn(CompletableFuture.completedFuture(false));
        when(notificationService.sendNotificationToChannel(anyString(), anyString(), any(Map.class), any()))
            .thenReturn(CompletableFuture.completedFuture(false));

        // When
        scheduler.sendStudyReminder10Min();

        // Then
        verify(notificationService, times(1)).sendNotification(anyLong(), anyString(), any(Map.class));
        verify(notificationService, times(1)).sendNotificationToChannel(anyString(), anyString(), any(Map.class), any());
    }

    private NotificationTargetService.StudyTarget createMockStudyTarget() {
        return NotificationTargetService.StudyTarget.builder()
            .studentId(1L)
            .studentName("테스트학생")
            .parentPhone("010-1234-5678")
            .parentNotificationEnabled(true)
            .build();
    }

    private NotificationTargetService.TaskTarget createMockTaskTarget() {
        return NotificationTargetService.TaskTarget.builder()
            .studentId(1L)
            .studentName("테스트학생")
            .parentPhone("010-1234-5678")
            .parentNotificationEnabled(true)
            .taskCount(3)
            .taskTitles(List.of("수학 문제집 10페이지", "영어 단어 암기", "물리 실험 보고서"))
            .build();
    }

    private NotificationTargetService.NoShowTarget createMockNoShowTarget() {
        return NotificationTargetService.NoShowTarget.builder()
            .studentId(1L)
            .studentName("테스트학생")
            .parentPhone("010-1234-5678")
            .studentNotificationEnabled(true)
            .parentNotificationEnabled(true)
            .build();
    }
}
