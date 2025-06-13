package saomath.checkusserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.notification.channel.NotificationChannel;
import saomath.checkusserver.notification.dto.NotificationMessage;
import saomath.checkusserver.repository.AssignedStudyTimeRepository;
import saomath.checkusserver.repository.ActualStudyTimeRepository;
import saomath.checkusserver.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private AssignedStudyTimeRepository assignedStudyTimeRepository;

    @Mock
    private ActualStudyTimeRepository actualStudyTimeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationChannel notificationChannel;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        when(notificationChannel.isEnabled()).thenReturn(true);
        when(notificationChannel.getChannelType()).thenReturn("DISCORD");
        when(notificationChannel.sendMessage(any())).thenReturn(CompletableFuture.completedFuture(true));
        
        notificationService = new NotificationService(
                assignedStudyTimeRepository,
                actualStudyTimeRepository,
                userRepository,
                List.of(notificationChannel)
        );
    }

    @Test
    void testSendEarlyLeaveNotification() {
        // Given
        User student = createTestStudent();
        AssignedStudyTime studyTime = createTestStudyTime();
        long remainingMinutes = 30L;

        // When
        notificationService.sendEarlyLeaveNotification(student, studyTime, remainingMinutes);

        // Then
        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationChannel).sendMessage(captor.capture());
        
        NotificationMessage notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationMessage.NotificationType.EARLY_LEAVE);
        assertThat(notification.getTitle()).isEqualTo("조기 퇴장 감지");
        assertThat(notification.getRecipientId()).isEqualTo("123456789");
        assertThat(notification.getRecipientName()).isEqualTo("김학생");
        assertThat(notification.getPriority()).isEqualTo(3);
        
        Map<String, Object> data = notification.getData();
        assertThat(data.get("subject")).isEqualTo("수학");
        assertThat(data.get("remainingMinutes")).isEqualTo(30L);
        assertThat(data.get("endTime")).isEqualTo(studyTime.getEndTime());
    }

    @Test
    void testSendLateArrivalNotification() {
        // Given
        User student = createTestStudent();
        AssignedStudyTime studyTime = createTestStudyTime();
        long lateMinutes = 15L;

        // When
        notificationService.sendLateArrivalNotification(student, studyTime, lateMinutes);

        // Then
        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationChannel).sendMessage(captor.capture());
        
        NotificationMessage notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationMessage.NotificationType.LATE_ARRIVAL);
        assertThat(notification.getTitle()).isEqualTo("늦은 입장 안내");
        assertThat(notification.getRecipientId()).isEqualTo("123456789");
        assertThat(notification.getRecipientName()).isEqualTo("김학생");
        assertThat(notification.getPriority()).isEqualTo(2);
        
        Map<String, Object> data = notification.getData();
        assertThat(data.get("subject")).isEqualTo("수학");
        assertThat(data.get("lateMinutes")).isEqualTo(15L);
        assertThat(data.get("endTime")).isEqualTo(studyTime.getEndTime());
    }

    @Test
    void testSendEarlyLeaveNotification_NoDiscordId() {
        // Given
        User student = createTestStudent();
        student.setDiscordId(null);
        AssignedStudyTime studyTime = createTestStudyTime();

        // When
        notificationService.sendEarlyLeaveNotification(student, studyTime, 30L);

        // Then
        verify(notificationChannel, never()).sendMessage(any());
    }

    @Test
    void testSendNotificationToChannel_Success() {
        // Given
        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.STUDY_START)
                .title("Test")
                .recipientId("123456789")
                .build();

        // When
        CompletableFuture<Boolean> result = notificationService.sendNotificationToChannel(notification, "DISCORD");

        // Then
        assertThat(result.join()).isTrue();
        verify(notificationChannel).sendMessage(notification);
    }

    @Test
    void testSendNotificationToChannel_ChannelNotFound() {
        // Given
        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.STUDY_START)
                .title("Test")
                .recipientId("123456789")
                .build();

        // When
        CompletableFuture<Boolean> result = notificationService.sendNotificationToChannel(notification, "KAKAO");

        // Then
        assertThat(result.join()).isFalse();
        verify(notificationChannel, never()).sendMessage(any());
    }

    @Test
    void testCheckUpcomingStudyTimes() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesLater = now.plusMinutes(10);
        
        AssignedStudyTime studyTime = createTestStudyTime();
        studyTime.setStartTime(tenMinutesLater);
        
        User student = createTestStudent();
        
        when(assignedStudyTimeRepository.findStartingBetween(any(), any()))
                .thenReturn(List.of(studyTime));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        // When
        notificationService.checkUpcomingStudyTimes();

        // Then
        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationChannel).sendMessage(captor.capture());
        
        NotificationMessage notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationMessage.NotificationType.UPCOMING_STUDY);
        assertThat(notification.getTitle()).isEqualTo("공부 시작 10분 전");
        assertThat(notification.getPriority()).isEqualTo(2);
    }

    @Test
    void testCheckCurrentStudyTimes() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        AssignedStudyTime studyTime = createTestStudyTime();
        studyTime.setStartTime(now);
        
        User student = createTestStudent();
        
        when(assignedStudyTimeRepository.findStartingBetween(any(), any()))
                .thenReturn(List.of(studyTime));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        // When
        notificationService.checkCurrentStudyTimes();

        // Then
        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationChannel).sendMessage(captor.capture());
        
        NotificationMessage notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationMessage.NotificationType.STUDY_START);
        assertThat(notification.getTitle()).isEqualTo("공부 시작 시간");
        assertThat(notification.getPriority()).isEqualTo(3);
    }

    @Test
    void testCheckMissedStudyTimes() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesAgo = now.minusMinutes(10);
        
        AssignedStudyTime studyTime = createTestStudyTime();
        studyTime.setStartTime(tenMinutesAgo);
        
        User student = createTestStudent();
        
        when(assignedStudyTimeRepository.findStartedWithoutAttendance(any(), any()))
                .thenReturn(List.of(studyTime));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        // When
        notificationService.checkMissedStudyTimes();

        // Then
        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationChannel).sendMessage(captor.capture());
        
        NotificationMessage notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationMessage.NotificationType.MISSED_STUDY);
        assertThat(notification.getTitle()).isEqualTo("미접속 알림");
        assertThat(notification.getPriority()).isEqualTo(3);
    }

    @Test
    void testSendDailyTaskNotification() {
        // Given
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime startOfDay = today.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        User student = createTestStudent();
        List<AssignedStudyTime> todayStudyTimes = List.of(
                createTestStudyTime(),
                createTestStudyTime()
        );
        
        when(userRepository.findAllStudents()).thenReturn(List.of(student));
        when(assignedStudyTimeRepository.findByStudentIdAndStartTimeBetween(1L, startOfDay, endOfDay))
                .thenReturn(todayStudyTimes);

        // When
        notificationService.sendDailyTaskNotification();

        // Then
        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationChannel).sendMessage(captor.capture());
        
        NotificationMessage notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationMessage.NotificationType.DAILY_TASK);
        assertThat(notification.getTitle()).isEqualTo("오늘의 공부 일정");
        assertThat(notification.getPriority()).isEqualTo(1);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> studyTimes = (List<Map<String, Object>>) notification.getData().get("studyTimes");
        assertThat(studyTimes).hasSize(2);
    }

    @Test
    void testMultipleChannels() {
        // Given
        NotificationChannel channel2 = mock(NotificationChannel.class);
        when(channel2.isEnabled()).thenReturn(true);
        when(channel2.getChannelType()).thenReturn("KAKAO");
        when(channel2.sendMessage(any())).thenReturn(CompletableFuture.completedFuture(true));
        
        notificationService = new NotificationService(
                assignedStudyTimeRepository,
                actualStudyTimeRepository,
                userRepository,
                List.of(notificationChannel, channel2)
        );
        
        User student = createTestStudent();
        AssignedStudyTime studyTime = createTestStudyTime();

        // When
        notificationService.sendEarlyLeaveNotification(student, studyTime, 30L);

        // Then
        verify(notificationChannel).sendMessage(any());
        verify(channel2).sendMessage(any());
    }

    private User createTestStudent() {
        User user = new User();
        user.setId(1L);
        user.setUsername("김학생");
        user.setDiscordId("123456789");
        return user;
    }

    private AssignedStudyTime createTestStudyTime() {
        AssignedStudyTime studyTime = new AssignedStudyTime();
        studyTime.setId(1L);
        studyTime.setStudentId(1L);
        studyTime.setTitle("수학");
        studyTime.setStartTime(LocalDateTime.now().plusMinutes(10));
        studyTime.setEndTime(LocalDateTime.now().plusMinutes(70));
        return studyTime;
    }
} 