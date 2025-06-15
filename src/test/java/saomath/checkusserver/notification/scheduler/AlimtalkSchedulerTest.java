package saomath.checkusserver.notification.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.ActualStudyTime;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.service.AlimtalkService;
import saomath.checkusserver.notification.service.NotificationTargetService;
import saomath.checkusserver.service.StudyTimeService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlimtalkSchedulerTest {
    
    @Mock
    private AlimtalkService alimtalkService;
    
    @Mock
    private NotificationTargetService targetService;
    
    @Mock
    private StudyTimeService studyTimeService;
    
    @InjectMocks
    private AlimtalkScheduler scheduler;
    
    @BeforeEach
    void setUp() {
        when(alimtalkService.sendAlimtalk(anyString(), any(AlimtalkTemplate.class), any(Map.class)))
            .thenReturn(true);
    }
    
    @Test
    @DisplayName("공부 시작 10분 전 알림 발송 테스트")
    void sendStudyReminder10Min() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetTime = now.plusMinutes(10);
        
        NotificationTargetService.StudyTarget target = NotificationTargetService.StudyTarget.builder()
            .studentId(1L)
            .studentName("홍길동")
            .studentPhone("01012345678")
            .parentPhone("01087654321")
            .activityName("수학")
            .startTime(targetTime)
            .endTime(targetTime.plusHours(2))
            .parentNotificationEnabled(true)
            .studentNotificationEnabled(true)
            .build();
        
        when(targetService.getStudyTargetsForTime(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(target));
        
        // When
        scheduler.sendStudyReminder10Min();
        
        // Then
        verify(targetService).getStudyTargetsForTime(any(LocalDateTime.class));
        verify(alimtalkService, times(2)).sendAlimtalk(
            anyString(), 
            eq(AlimtalkTemplate.STUDY_REMINDER_10MIN), 
            any(Map.class)
        );
    }
    
    @Test
    @DisplayName("공부 시작 시간 알림 및 세션 연결 통합 테스트")
    void sendStudyStartNotificationAndConnectSessions() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesAgo = now.minusMinutes(10);
        
        // 알림 대상
        NotificationTargetService.StudyTarget target = NotificationTargetService.StudyTarget.builder()
            .studentId(1L)
            .studentName("김철수")
            .studentPhone("01011112222")
            .parentPhone("01033334444")
            .activityName("영어")
            .startTime(now)
            .endTime(now.plusHours(2))
            .parentNotificationEnabled(true)
            .studentNotificationEnabled(true)
            .build();
        
        // 세션 연결 대상
        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .id(1L)
                .studentId(2L)
                .title("수학 공부")
                .startTime(tenMinutesAgo)
                .endTime(tenMinutesAgo.plusHours(2))
                .build();
        
        ActualStudyTime connectedSession = ActualStudyTime.builder()
                .id(100L)
                .studentId(2L)
                .assignedStudyTimeId(1L)
                .startTime(tenMinutesAgo.minusMinutes(5))
                .source("discord")
                .build();
        
        when(targetService.getStudyTargetsForTime(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(target));
        when(studyTimeService.getAssignedStudyTimesByDateRange(
                any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(assignedStudyTime));
        when(studyTimeService.connectPreviousOngoingSession(1L))
            .thenReturn(connectedSession);
        
        // When
        scheduler.sendStudyStartNotificationAndConnectSessions();
        
        // Then
        // 1. 알림 발송 확인
        verify(targetService).getStudyTargetsForTime(any(LocalDateTime.class));
        verify(alimtalkService, times(2)).sendAlimtalk(
            anyString(), 
            eq(AlimtalkTemplate.STUDY_START), 
            any(Map.class)
        );
        
        // 2. 세션 연결 확인
        verify(studyTimeService).getAssignedStudyTimesByDateRange(
                any(LocalDateTime.class), any(LocalDateTime.class));
        verify(studyTimeService).connectPreviousOngoingSession(1L);
    }
    
    @Test
    @DisplayName("세션 연결 실패 시 예외 처리 테스트")
    void connectSessionsWithException() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesAgo = now.minusMinutes(10);
        
        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .id(1L)
                .studentId(1L)
                .title("수학 공부")
                .startTime(tenMinutesAgo)
                .endTime(tenMinutesAgo.plusHours(2))
                .build();
        
        when(targetService.getStudyTargetsForTime(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList());
        when(studyTimeService.getAssignedStudyTimesByDateRange(
                any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(assignedStudyTime));
        when(studyTimeService.connectPreviousOngoingSession(1L))
            .thenThrow(new RuntimeException("데이터베이스 오류"));
        
        // When - 예외가 발생해도 스케줄러는 중단되지 않음
        scheduler.sendStudyStartNotificationAndConnectSessions();
        
        // Then
        verify(studyTimeService).connectPreviousOngoingSession(1L);
    }
    
    @Test
    @DisplayName("연결할 세션이 없는 경우 테스트")
    void connectSessionsWithNoOngoingSession() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesAgo = now.minusMinutes(10);
        
        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .id(1L)
                .studentId(1L)
                .title("수학 공부")
                .startTime(tenMinutesAgo)
                .endTime(tenMinutesAgo.plusHours(2))
                .build();
        
        when(targetService.getStudyTargetsForTime(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList());
        when(studyTimeService.getAssignedStudyTimesByDateRange(
                any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(assignedStudyTime));
        when(studyTimeService.connectPreviousOngoingSession(1L))
            .thenReturn(null); // 연결할 세션 없음
        
        // When
        scheduler.sendStudyStartNotificationAndConnectSessions();
        
        // Then
        verify(studyTimeService).connectPreviousOngoingSession(1L);
    }
    
    @Test
    @DisplayName("오늘의 할일 알림 발송 테스트")
    void sendTodayTasksNotification() {
        // Given
        NotificationTargetService.TaskTarget target = NotificationTargetService.TaskTarget.builder()
            .studentId(1L)
            .studentName("이영희")
            .studentPhone("01055556666")
            .parentPhone("01077778888")
            .taskCount(3)
            .taskTitles(Arrays.asList("수학 문제집 10페이지", "영어 단어 암기", "국어 독서"))
            .parentNotificationEnabled(true)
            .studentNotificationEnabled(true)
            .build();
        
        when(targetService.getTodayTaskTargets())
            .thenReturn(Arrays.asList(target));
        
        // When
        scheduler.sendTodayTasksNotification();
        
        // Then
        verify(targetService).getTodayTaskTargets();
        verify(alimtalkService, times(2)).sendAlimtalk(
            anyString(), 
            eq(AlimtalkTemplate.TODAY_TASKS), 
            any(Map.class)
        );
    }
    
    @Test
    @DisplayName("미접속 체크 및 알림 발송 테스트")
    void checkNoShow() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkTime = now.minusMinutes(15);
        
        NotificationTargetService.NoShowTarget target = NotificationTargetService.NoShowTarget.builder()
            .studentId(1L)
            .studentName("박민수")
            .studentPhone("01099990000")
            .parentPhone("01011110000")
            .startTime(checkTime)
            .endTime(checkTime.plusHours(2))
            .parentNotificationEnabled(true)
            .studentNotificationEnabled(false)
            .build();
        
        when(targetService.getNoShowTargets(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(target));
        
        // When
        scheduler.checkNoShow();
        
        // Then
        verify(targetService).getNoShowTargets(any(LocalDateTime.class));
        // 학부모에게만 발송 (학생 알림 비활성화)
        verify(alimtalkService, times(1)).sendAlimtalk(
            eq("01011110000"), 
            eq(AlimtalkTemplate.NO_SHOW), 
            any(Map.class)
        );
    }
    
    @Test
    @DisplayName("알림 발송 실패 시 예외 처리 테스트")
    void sendNotificationWithException() {
        // Given
        when(alimtalkService.sendAlimtalk(anyString(), any(AlimtalkTemplate.class), any(Map.class)))
            .thenThrow(new RuntimeException("네트워크 오류"));
        
        NotificationTargetService.StudyTarget target = NotificationTargetService.StudyTarget.builder()
            .studentId(1L)
            .studentName("테스트")
            .studentPhone("01000000000")
            .parentPhone(null)
            .activityName("테스트")
            .startTime(LocalDateTime.now())
            .endTime(LocalDateTime.now().plusHours(1))
            .parentNotificationEnabled(false)
            .studentNotificationEnabled(true)
            .build();
        
        when(targetService.getStudyTargetsForTime(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(target));
        when(studyTimeService.getAssignedStudyTimesByDateRange(
                any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList());
        
        // When - 예외가 발생해도 스케줄러는 중단되지 않음
        scheduler.sendStudyStartNotificationAndConnectSessions();
        
        // Then
        verify(alimtalkService).sendAlimtalk(anyString(), any(AlimtalkTemplate.class), any(Map.class));
    }
    
    @Test
    @DisplayName("전화번호가 없는 경우 알림 발송 건너뛰기")
    void skipNotificationWhenPhoneNumberIsNull() {
        // Given
        NotificationTargetService.StudyTarget target = NotificationTargetService.StudyTarget.builder()
            .studentId(1L)
            .studentName("테스트")
            .studentPhone(null)  // 전화번호 없음
            .parentPhone("")     // 빈 문자열
            .activityName("테스트")
            .startTime(LocalDateTime.now())
            .endTime(LocalDateTime.now().plusHours(1))
            .parentNotificationEnabled(true)
            .studentNotificationEnabled(true)
            .build();
        
        when(targetService.getStudyTargetsForTime(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(target));
        when(studyTimeService.getAssignedStudyTimesByDateRange(
                any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList());
        
        // When
        scheduler.sendStudyStartNotificationAndConnectSessions();
        
        // Then - 전화번호가 없으므로 알림 발송 안됨
        verify(alimtalkService, never()).sendAlimtalk(anyString(), any(AlimtalkTemplate.class), any(Map.class));
    }
}
