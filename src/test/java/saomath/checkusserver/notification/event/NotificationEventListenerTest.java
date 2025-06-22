package saomath.checkusserver.notification.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.user.domain.StudentGuardian;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.event.StudyAttendanceEvent;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.service.MultiChannelNotificationService;
import saomath.checkusserver.user.repository.StudentGuardianRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("통합 알림 이벤트 리스너 테스트")
class NotificationEventListenerTest {

    @Mock
    private MultiChannelNotificationService notificationService;
    
    @Mock
    private StudentGuardianRepository studentGuardianRepository;
    
    private NotificationEventListener eventListener;

    @BeforeEach
    void setUp() {
        eventListener = new NotificationEventListener(notificationService, studentGuardianRepository);
    }

    @Test
    @DisplayName("스터디룸 입장 이벤트 - 정상 처리")
    void handleStudyRoomEnterEvent_Success() {
        // Given
        StudyRoomEnterEvent event = StudyRoomEnterEvent.builder()
            .studentId(1L)
            .studentName("테스트학생")
            .enterTime(LocalDateTime.now())
            .build();
            
        StudentGuardian guardian = createMockStudentGuardian();
        when(studentGuardianRepository.findByStudentId(1L))
            .thenReturn(List.of(guardian));
        when(notificationService.sendNotification(anyLong(), anyString(), any(Map.class)))
            .thenReturn(CompletableFuture.completedFuture(true));

        // When
        eventListener.handleStudyRoomEnterEvent(event);

        // Then
        verify(notificationService, times(2)).sendNotification(anyLong(), 
            eq(AlimtalkTemplate.STUDY_ROOM_ENTER.name()), any(Map.class));
        verify(studentGuardianRepository).findByStudentId(1L);
    }

    @Test
    @DisplayName("조기퇴장 이벤트 - 정상 처리")
    void handleEarlyLeaveEvent_Success() {
        // Given
        User student = createMockStudent();
        AssignedStudyTime studyTime = createMockAssignedStudyTime();
        StudyAttendanceEvent event = new StudyAttendanceEvent(
            this, StudyAttendanceEvent.EventType.EARLY_LEAVE, student, studyTime, 30L
        );
        
        StudentGuardian guardian = createMockStudentGuardian();
        when(studentGuardianRepository.findByStudentId(1L))
            .thenReturn(List.of(guardian));
        when(notificationService.sendNotification(anyLong(), anyString(), any(Map.class)))
            .thenReturn(CompletableFuture.completedFuture(true));

        // When
        eventListener.handleStudyAttendanceEvent(event);

        // Then
        verify(notificationService, times(2)).sendNotification(anyLong(), 
            eq(AlimtalkTemplate.EARLY_LEAVE.name()), any(Map.class));
    }

    @Test
    @DisplayName("늦은입장 이벤트 - 정상 처리")
    void handleLateArrivalEvent_Success() {
        // Given
        User student = createMockStudent();
        AssignedStudyTime studyTime = createMockAssignedStudyTime();
        StudyAttendanceEvent event = new StudyAttendanceEvent(
            this, StudyAttendanceEvent.EventType.LATE_ARRIVAL, student, studyTime, 15L
        );
        
        StudentGuardian guardian = createMockStudentGuardian();
        when(studentGuardianRepository.findByStudentId(1L))
            .thenReturn(List.of(guardian));
        when(notificationService.sendNotification(anyLong(), anyString(), any(Map.class)))
            .thenReturn(CompletableFuture.completedFuture(true));

        // When
        eventListener.handleStudyAttendanceEvent(event);

        // Then
        verify(notificationService, times(2)).sendNotification(anyLong(), 
            eq(AlimtalkTemplate.LATE_ARRIVAL.name()), any(Map.class));
    }

    @Test
    @DisplayName("학부모 없는 경우 - 학생에게만 알림")
    void handleEvent_NoGuardian() {
        // Given
        StudyRoomEnterEvent event = StudyRoomEnterEvent.builder()
            .studentId(1L)
            .studentName("테스트학생")
            .enterTime(LocalDateTime.now())
            .build();
            
        when(studentGuardianRepository.findByStudentId(1L))
            .thenReturn(List.of());
        when(notificationService.sendNotification(anyLong(), anyString(), any(Map.class)))
            .thenReturn(CompletableFuture.completedFuture(true));

        // When
        eventListener.handleStudyRoomEnterEvent(event);

        // Then
        verify(notificationService, times(1)).sendNotification(eq(1L), 
            eq(AlimtalkTemplate.STUDY_ROOM_ENTER.name()), any(Map.class));
    }

    @Test
    @DisplayName("알림 전송 실패 시 로깅")
    void handleEvent_NotificationFailure() {
        // Given
        StudyRoomEnterEvent event = StudyRoomEnterEvent.builder()
            .studentId(1L)
            .studentName("테스트학생")
            .enterTime(LocalDateTime.now())
            .build();
            
        when(studentGuardianRepository.findByStudentId(1L))
            .thenReturn(List.of());
        when(notificationService.sendNotification(anyLong(), anyString(), any(Map.class)))
            .thenReturn(CompletableFuture.completedFuture(false));

        // When
        eventListener.handleStudyRoomEnterEvent(event);

        // Then
        verify(notificationService).sendNotification(anyLong(), anyString(), any(Map.class));
    }

    private User createMockStudent() {
        User student = new User();
        student.setId(1L);
        student.setName("테스트학생");
        return student;
    }

    private AssignedStudyTime createMockAssignedStudyTime() {
        AssignedStudyTime studyTime = new AssignedStudyTime();
        studyTime.setId(1L);
        studyTime.setTitle("수학 공부");
        studyTime.setEndTime(LocalDateTime.now().plusHours(1));
        return studyTime;
    }

    private StudentGuardian createMockStudentGuardian() {
        User guardian = new User();
        guardian.setId(2L);
        guardian.setName("테스트학부모");
        
        StudentGuardian studentGuardian = new StudentGuardian();
        studentGuardian.setGuardian(guardian);
        return studentGuardian;
    }
}
