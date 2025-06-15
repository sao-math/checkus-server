package saomath.checkusserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.event.StudyAttendanceEvent;
import saomath.checkusserver.notification.channel.NotificationChannel;
import saomath.checkusserver.notification.dto.NotificationMessage;
import saomath.checkusserver.repository.AssignedStudyTimeRepository;
import saomath.checkusserver.repository.ActualStudyTimeRepository;
import saomath.checkusserver.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 통합 알림 서비스
 * 다양한 채널(Discord, KakaoTalk 등)을 통한 알림 발송 관리
 */
@Slf4j
@Service
public class NotificationService {

    private final AssignedStudyTimeRepository assignedStudyTimeRepository;
    private final ActualStudyTimeRepository actualStudyTimeRepository;
    private final UserRepository userRepository;
    private final List<NotificationChannel> notificationChannels;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public NotificationService(
            AssignedStudyTimeRepository assignedStudyTimeRepository,
            ActualStudyTimeRepository actualStudyTimeRepository,
            UserRepository userRepository,
            List<NotificationChannel> notificationChannels) {
        this.assignedStudyTimeRepository = assignedStudyTimeRepository;
        this.actualStudyTimeRepository = actualStudyTimeRepository;
        this.userRepository = userRepository;
        this.notificationChannels = notificationChannels;
        
        log.info("NotificationService 초기화됨. 활성화된 채널 수: {}", 
                notificationChannels.stream().mapToLong(ch -> ch.isEnabled() ? 1 : 0).sum());
    }

    /**
     * 출석 관련 이벤트 처리 (이벤트 리스너)
     * 순환 의존성을 방지하기 위해 이벤트 기반으로 처리
     */
    @EventListener
    @Async
    public void handleStudyAttendanceEvent(StudyAttendanceEvent event) {
        try {
            switch (event.getEventType()) {
                case EARLY_LEAVE:
                    sendEarlyLeaveNotification(event.getStudent(), event.getStudyTime(), event.getMinutes());
                    break;
                case LATE_ARRIVAL:
                    sendLateArrivalNotification(event.getStudent(), event.getStudyTime(), event.getMinutes());
                    break;
            }
        } catch (Exception e) {
            log.error("출석 이벤트 처리 중 오류 발생: eventType={}, student={}", 
                    event.getEventType(), event.getStudent().getUsername(), e);
        }
    }

    /**
     * 조기 퇴장 알림 발송
     */
    public void sendEarlyLeaveNotification(User student, AssignedStudyTime studyTime, long remainingMinutes) {
        if (student.getDiscordId() == null) {
            log.warn("디스코드 ID가 없는 학생: {}", student.getUsername());
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("subject", studyTime.getTitle());
        data.put("remainingMinutes", remainingMinutes);
        data.put("endTime", studyTime.getEndTime());

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.EARLY_LEAVE)
                .title("조기 퇴장 감지")
                .recipientId(student.getDiscordId())
                .recipientName(student.getUsername())
                .priority(3)
                .data(data)
                .build();

        sendToAllChannels(notification);
    }

    /**
     * 늦은 입장 알림 발송
     */
    public void sendLateArrivalNotification(User student, AssignedStudyTime studyTime, long lateMinutes) {
        if (student.getDiscordId() == null) {
            log.warn("디스코드 ID가 없는 학생: {}", student.getUsername());
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("subject", studyTime.getTitle());
        data.put("lateMinutes", lateMinutes);
        data.put("endTime", studyTime.getEndTime());

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.LATE_ARRIVAL)
                .title("늦은 입장 안내")
                .recipientId(student.getDiscordId())
                .recipientName(student.getUsername())
                .priority(2)
                .data(data)
                .build();

        sendToAllChannels(notification);
    }

    /**
     * 공부 시작 10분 전 알림 체크
     */
    public void checkUpcomingStudyTimes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesLater = now.plusMinutes(10);
        
        List<AssignedStudyTime> upcomingStudyTimes = assignedStudyTimeRepository
                .findStartingBetween(tenMinutesLater.minusMinutes(1), tenMinutesLater.plusMinutes(1));
        
        for (AssignedStudyTime studyTime : upcomingStudyTimes) {
            userRepository.findById(studyTime.getStudentId()).ifPresent(student -> {
                if (student.getDiscordId() != null) {
                    sendUpcomingStudyNotification(student, studyTime);
                }
            });
        }
    }
    
    /**
     * 공부 시작 시간 알림 체크
     */
    public void checkCurrentStudyTimes() {
        LocalDateTime now = LocalDateTime.now();
        
        List<AssignedStudyTime> currentStudyTimes = assignedStudyTimeRepository
                .findStartingBetween(now.minusMinutes(1), now.plusMinutes(1));
        
        for (AssignedStudyTime studyTime : currentStudyTimes) {
            userRepository.findById(studyTime.getStudentId()).ifPresent(student -> {
                if (student.getDiscordId() != null) {
                    sendStudyStartNotification(student, studyTime);
                }
            });
        }
    }
    
    /**
     * 미접속 알림 체크 (시작 시간 10분 후)
     */
    public void checkMissedStudyTimes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesAgo = now.minusMinutes(10);
        
        List<AssignedStudyTime> missedStudyTimes = assignedStudyTimeRepository
                .findStartedWithoutAttendance(tenMinutesAgo.minusMinutes(1), tenMinutesAgo.plusMinutes(1));
        
        for (AssignedStudyTime studyTime : missedStudyTimes) {
            userRepository.findById(studyTime.getStudentId()).ifPresent(student -> {
                if (student.getDiscordId() != null) {
                    sendMissedStudyNotification(student, studyTime);
                }
            });
        }
    }
    
    /**
     * 일일 할일 알림 발송
     */
    public void sendDailyTaskNotification() {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime startOfDay = today.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        List<User> students = userRepository.findAllStudents();
        
        for (User student : students) {
            if (student.getDiscordId() != null) {
                List<AssignedStudyTime> todayStudyTimes = assignedStudyTimeRepository
                        .findByStudentIdAndStartTimeBetween(student.getId(), startOfDay, endOfDay);
                
                if (!todayStudyTimes.isEmpty()) {
                    sendDailyTaskNotification(student, todayStudyTimes);
                }
            }
        }
    }

    private void sendUpcomingStudyNotification(User student, AssignedStudyTime studyTime) {
        Map<String, Object> data = new HashMap<>();
        data.put("subject", studyTime.getTitle());
        data.put("startTime", studyTime.getStartTime());
        data.put("endTime", studyTime.getEndTime());

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.UPCOMING_STUDY)
                .title("공부 시작 10분 전")
                .recipientId(student.getDiscordId())
                .recipientName(student.getUsername())
                .priority(2)
                .data(data)
                .build();

        sendToAllChannels(notification);
    }

    private void sendStudyStartNotification(User student, AssignedStudyTime studyTime) {
        Map<String, Object> data = new HashMap<>();
        data.put("subject", studyTime.getTitle());
        data.put("startTime", studyTime.getStartTime());
        data.put("endTime", studyTime.getEndTime());

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.STUDY_START)
                .title("공부 시작 시간")
                .recipientId(student.getDiscordId())
                .recipientName(student.getUsername())
                .priority(3)
                .data(data)
                .build();

        sendToAllChannels(notification);
    }

    private void sendMissedStudyNotification(User student, AssignedStudyTime studyTime) {
        Map<String, Object> data = new HashMap<>();
        data.put("subject", studyTime.getTitle());
        data.put("startTime", studyTime.getStartTime());
        data.put("endTime", studyTime.getEndTime());

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.MISSED_STUDY)
                .title("미접속 알림")
                .recipientId(student.getDiscordId())
                .recipientName(student.getUsername())
                .priority(3)
                .data(data)
                .build();

        sendToAllChannels(notification);
    }

    private void sendDailyTaskNotification(User student, List<AssignedStudyTime> studyTimes) {
        Map<String, Object> data = new HashMap<>();
        data.put("date", LocalDateTime.now().toLocalDate());
        data.put("studyTimes", studyTimes.stream()
                .map(st -> Map.of(
                        "subject", st.getTitle(),
                        "startTime", st.getStartTime(),
                        "endTime", st.getEndTime()
                )).toList());

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.DAILY_TASK)
                .title("오늘의 공부 일정")
                .recipientId(student.getDiscordId())
                .recipientName(student.getUsername())
                .priority(1)
                .data(data)
                .build();

        sendToAllChannels(notification);
    }

    /**
     * 모든 활성화된 채널에 알림 발송
     */
    private void sendToAllChannels(NotificationMessage notification) {
        for (NotificationChannel channel : notificationChannels) {
            if (channel.isEnabled()) {
                try {
                    channel.sendMessage(notification);
                } catch (Exception e) {
                    log.error("알림 발송 실패: channel={}, notification={}", 
                            channel.getChannelType(), notification.getTitle(), e);
                }
            }
        }
    }

    /**
     * 특정 채널로 알림 발송
     */
    public CompletableFuture<Boolean> sendNotificationToChannel(NotificationMessage notification, String channelType) {
        return notificationChannels.stream()
                .filter(channel -> channel.getChannelType().equals(channelType))
                .findFirst()
                .map(channel -> {
                    if (channel.isEnabled()) {
                        return channel.sendMessage(notification);
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .orElse(CompletableFuture.completedFuture(false));
    }
} 