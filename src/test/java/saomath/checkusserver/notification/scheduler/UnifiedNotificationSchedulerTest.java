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
@DisplayName("통합 알림 스케줄러 테스트 (중복 방지)")
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
    @DisplayName("공부 시작 10분 전 알림 - 정확한 시간 매칭")
    void sendStudyReminder10Min_ExactTimeMatching() {
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
        verify(targetService).getStudyTargetsForTime(argThat(dateTime -> 
            dateTime.getSecond() == 0 && dateTime.getNano() == 0
        ));
        verify(notificationService, times(1)).sendNotification(anyLong(), anyString(), any(Map.class));
        verify(notificationService, times(1)).sendNotificationToChannel(anyString(), anyString(), any(Map.class), any());
    }

    @Test
    @DisplayName("공부 시작 시간 알림 - 정확한 시간 매칭")
    void sendStudyStartNotificationAndConnectSessions_ExactTimeMatching() {
        // Given
        NotificationTargetService.StudyTarget target = createMockStudyTarget();
        when(targetService.getStudyTargetsForTime(any(LocalDateTime.class)))
            .thenReturn(List.of(target));
        when(notificationService.sendNotification(anyLong(), anyString(), any(Map.class)))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(notificationService.sendNotificationToChannel(anyString(), anyString(), any(Map.class), any()))
            .thenReturn(CompletableFuture.completedFuture(true));
        // 스케줄러에서 getAssignedStudyTimesByDateRange가 두 번 호출됨 (즉시 연결용 + 지연 연결용)
        when(studyTimeService.getAssignedStudyTimesByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of());

        // When
        scheduler.sendStudyStartNotificationAndConnectSessions();

        // Then
        verify(targetService).getStudyTargetsForTime(argThat(dateTime -> 
            dateTime.getSecond() == 0 && dateTime.getNano() == 0
        ));
        verify(notificationService, times(1)).sendNotification(anyLong(), anyString(), any(Map.class));
        verify(notificationService, times(1)).sendNotificationToChannel(anyString(), anyString(), any(Map.class), any());
        // 즉시 연결용 1번 + 지연 연결용 1번 = 총 2번 호출
        verify(studyTimeService, times(2)).getAssignedStudyTimesByDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("대상자가 없을 때 알림 발송하지 않음")
    void sendStudyReminder10Min_NoTargets() {
        // Given
        when(targetService.getStudyTargetsForTime(any(LocalDateTime.class)))
            .thenReturn(List.of());

        // When
        scheduler.sendStudyReminder10Min();

        // Then
        verify(targetService).getStudyTargetsForTime(any(LocalDateTime.class));
        verify(notificationService, never()).sendNotification(anyLong(), anyString(), any(Map.class));
        verify(notificationService, never()).sendNotificationToChannel(anyString(), anyString(), any(Map.class), any());
    }

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

    @Test
    @DisplayName("중복 방지 - 대상자가 없을 때 알림 발송하지 않음")
    void preventDuplicateNotifications() {
        // Given
        when(targetService.getStudyTargetsForTime(any(LocalDateTime.class)))
            .thenReturn(List.of()); // 대상자 없음

        // When
        scheduler.sendStudyReminder10Min();

        // Then
        verify(targetService, times(1)).getStudyTargetsForTime(any(LocalDateTime.class));
        verify(notificationService, never()).sendNotification(anyLong(), anyString(), any(Map.class));
        verify(notificationService, never()).sendNotificationToChannel(anyString(), anyString(), any(Map.class), any());
    }

    private NotificationTargetService.StudyTarget createMockStudyTarget() {
        return NotificationTargetService.StudyTarget.builder()
            .studentId(1L)
            .studentName("테스트학생")
            .parentPhone("010-1234-5678")
            .parentNotificationEnabled(true)
            .startTime(LocalDateTime.now().plusMinutes(10))
            .endTime(LocalDateTime.now().plusMinutes(70))
            .build();
    }

    private NotificationTargetService.StudyTarget createMockStudyTargetWithTime(LocalDateTime startTime) {
        return NotificationTargetService.StudyTarget.builder()
            .studentId(1L)
            .studentName("테스트학생")
            .parentPhone("010-1234-5678")
            .parentNotificationEnabled(true)
            .startTime(startTime)
            .endTime(startTime.plusMinutes(60))
            .build();
    }

    private NotificationTargetService.NoShowTarget createMockNoShowTarget() {
        return NotificationTargetService.NoShowTarget.builder()
            .studentId(1L)
            .studentName("테스트학생")
            .parentPhone("010-1234-5678")
            .studentNotificationEnabled(true)
            .parentNotificationEnabled(true)
            .startTime(LocalDateTime.now().minusMinutes(15))
            .endTime(LocalDateTime.now().plusMinutes(45))
            .build();
    }
}
