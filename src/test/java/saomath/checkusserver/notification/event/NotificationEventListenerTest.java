package saomath.checkusserver.notification.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.entity.StudentGuardian;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.service.MultiChannelNotificationService;
import saomath.checkusserver.repository.StudentGuardianRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {
    
    @Mock
    private MultiChannelNotificationService notificationService;
    
    @Mock
    private StudentGuardianRepository studentGuardianRepository;
    
    @InjectMocks
    private NotificationEventListener eventListener;
    
    @BeforeEach
    void setUp() {
        when(notificationService.sendNotification(anyLong(), anyString(), any(Map.class)))
            .thenReturn(CompletableFuture.completedFuture(true));
    }
    
    @Test
    @DisplayName("스터디룸 입장 이벤트 처리 테스트")
    void handleStudyRoomEnterEvent() {
        // Given
        LocalDateTime enterTime = LocalDateTime.now();
        StudyRoomEnterEvent event = StudyRoomEnterEvent.builder()
            .studentId(1L)
            .studentName("홍길동")
            .discordId("discord123")
            .enterTime(enterTime)
            .channelName("수학 스터디룸")
            .build();
        
        // 학부모 정보 설정
        User guardian = new User();
        guardian.setId(2L);
        guardian.setName("홍부모");
        
        StudentGuardian studentGuardian = new StudentGuardian();
        studentGuardian.setGuardian(guardian);
        
        when(studentGuardianRepository.findByStudentId(1L))
            .thenReturn(Arrays.asList(studentGuardian));
        
        // When
        eventListener.handleStudyRoomEnterEvent(event);
        
        // Then
        // 학생에게 알림 발송 확인
        ArgumentCaptor<Map<String, String>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).sendNotification(
            eq(1L),
            eq(AlimtalkTemplate.STUDY_ROOM_ENTER.name()),
            variablesCaptor.capture()
        );
        
        Map<String, String> capturedVariables = variablesCaptor.getValue();
        assertThat(capturedVariables).containsEntry("studentName", "홍길동");
        assertThat(capturedVariables).containsKey("enterTime");
        
        // 학부모에게도 알림 발송 확인
        verify(notificationService).sendNotification(
            eq(2L),
            eq(AlimtalkTemplate.STUDY_ROOM_ENTER.name()),
            any(Map.class)
        );
    }
    
    @Test
    @DisplayName("학부모가 없는 경우에도 정상 처리")
    void handleStudyRoomEnterEventWithoutGuardian() {
        // Given
        StudyRoomEnterEvent event = StudyRoomEnterEvent.builder()
            .studentId(1L)
            .studentName("김철수")
            .discordId("discord456")
            .enterTime(LocalDateTime.now())
            .channelName("영어 스터디룸")
            .build();
        
        when(studentGuardianRepository.findByStudentId(1L))
            .thenReturn(Arrays.asList());
        
        // When
        eventListener.handleStudyRoomEnterEvent(event);
        
        // Then
        // 학생에게만 알림 발송
        verify(notificationService, times(1)).sendNotification(
            anyLong(),
            anyString(),
            any(Map.class)
        );
    }
}
