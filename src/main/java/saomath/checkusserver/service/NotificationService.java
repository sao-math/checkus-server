package saomath.checkusserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.notification.channel.NotificationChannel;
import saomath.checkusserver.notification.dto.NotificationMessage;
import saomath.checkusserver.repository.AssignedStudyTimeRepository;
import saomath.checkusserver.repository.ActualStudyTimeRepository;
import saomath.checkusserver.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 확장 가능한 알림 관리 서비스
 * 다양한 플랫폼(디스코드, 카카오톡 등)을 통한 알림 전송 지원
 */
@Slf4j
@Service
@Transactional
public class NotificationService {

    private final AssignedStudyTimeRepository assignedStudyTimeRepository;
    private final ActualStudyTimeRepository actualStudyTimeRepository;
    private final UserRepository userRepository;
    private final List<NotificationChannel> notificationChannels;

    public NotificationService(
            AssignedStudyTimeRepository assignedStudyTimeRepository,
            ActualStudyTimeRepository actualStudyTimeRepository,
            UserRepository userRepository,
            List<NotificationChannel> notificationChannels) {
        this.assignedStudyTimeRepository = assignedStudyTimeRepository;
        this.actualStudyTimeRepository = actualStudyTimeRepository;
        this.userRepository = userRepository;
        this.notificationChannels = notificationChannels;
        
        log.info("알림 서비스 초기화: 활성 채널 {} 개", 
                notificationChannels.stream().mapToLong(ch -> ch.isEnabled() ? 1 : 0).sum());
    }

    /**
     * 공부 시작 10분 전 알림
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    public void checkUpcomingStudyTimes() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime tenMinutesLater = now.plusMinutes(10);
            
            List<AssignedStudyTime> upcomingStudyTimes = assignedStudyTimeRepository
                    .findStartingBetween(tenMinutesLater.minusMinutes(1), tenMinutesLater.plusMinutes(1));
            
            for (AssignedStudyTime studyTime : upcomingStudyTimes) {
                sendUpcomingStudyNotification(studyTime);
            }
            
            if (!upcomingStudyTimes.isEmpty()) {
                log.info("10분 전 알림 전송 완료: {} 건", upcomingStudyTimes.size());
            }
        } catch (Exception e) {
            log.error("공부 시작 10분 전 알림 처리 중 오류 발생", e);
        }
    }

    /**
     * 공부 시작 시간 알림
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    public void checkCurrentStudyTimes() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            List<AssignedStudyTime> currentStudyTimes = assignedStudyTimeRepository
                    .findStartingBetween(now.minusMinutes(1), now.plusMinutes(1));
            
            for (AssignedStudyTime studyTime : currentStudyTimes) {
                sendStudyStartNotification(studyTime);
            }
            
            if (!currentStudyTimes.isEmpty()) {
                log.info("공부 시작 알림 전송 완료: {} 건", currentStudyTimes.size());
            }
        } catch (Exception e) {
            log.error("공부 시작 알림 처리 중 오류 발생", e);
        }
    }

    /**
     * 미접속 학생 감지 알림
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    public void checkMissedStudyTimes() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime tenMinutesAgo = now.minusMinutes(10);
            
            List<AssignedStudyTime> missedStudyTimes = assignedStudyTimeRepository
                    .findStartedWithoutAttendance(tenMinutesAgo.minusMinutes(1), tenMinutesAgo.plusMinutes(1));
            
            for (AssignedStudyTime studyTime : missedStudyTimes) {
                sendMissedStudyNotification(studyTime);
            }
            
            if (!missedStudyTimes.isEmpty()) {
                log.info("미접속 알림 전송 완료: {} 건", missedStudyTimes.size());
            }
        } catch (Exception e) {
            log.error("미접속 알림 처리 중 오류 발생", e);
        }
    }

    /**
     * 오늘의 할일 알림 (아침 8시)
     */
    @Scheduled(cron = "0 0 8 * * *") // 매일 아침 8시
    public void sendDailyTaskNotification() {
        try {
            LocalDateTime today = LocalDateTime.now();
            LocalDateTime startOfDay = today.toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            
            List<User> students = userRepository.findAllStudents();
            
            for (User student : students) {
                List<AssignedStudyTime> todayStudyTimes = assignedStudyTimeRepository
                        .findByStudentIdAndStartTimeBetween(student.getId(), startOfDay, endOfDay);
                
                if (!todayStudyTimes.isEmpty()) {
                    sendDailyTaskSummary(student, todayStudyTimes);
                }
            }
            
            log.info("오늘의 할일 알림 전송 완료: {} 명", students.size());
        } catch (Exception e) {
            log.error("오늘의 할일 알림 처리 중 오류 발생", e);
        }
    }

    /**
     * 조기 퇴장 알림 (즉시 전송용)
     */
    public void sendEarlyLeaveNotification(User student, AssignedStudyTime studyTime, long remainingMinutes) {
        if (student.getDiscordId() == null || student.getDiscordId().trim().isEmpty()) {
            log.warn("학생의 디스코드 ID가 설정되지 않았습니다. 학생: {}", student.getUsername());
            return;
        }

        Map<String, Object> data = Map.of(
                "subject", studyTime.getTitle(),
                "remainingMinutes", remainingMinutes,
                "endTime", studyTime.getEndTime()
        );

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.EARLY_LEAVE)
                .title("조기 퇴장 감지")
                .message("공부 시간이 아직 남아있습니다.")
                .data(data)
                .timestamp(LocalDateTime.now())
                .priority(3) // 높은 우선순위
                .recipientId(student.getDiscordId())
                .recipientName(student.getUsername())
                .build();

        sendNotificationToAllChannels(notification);
        log.warn("조기 퇴장 알림 전송: 학생={}, 과목={}, 남은 시간={}분", 
                student.getUsername(), studyTime.getTitle(), remainingMinutes);
    }

    /**
     * 늦은 입장 알림 (즉시 전송용)
     */
    public void sendLateArrivalNotification(User student, AssignedStudyTime studyTime, long lateMinutes) {
        if (student.getDiscordId() == null || student.getDiscordId().trim().isEmpty()) {
            log.warn("학생의 디스코드 ID가 설정되지 않았습니다. 학생: {}", student.getUsername());
            return;
        }

        Map<String, Object> data = Map.of(
                "subject", studyTime.getTitle(),
                "lateMinutes", lateMinutes,
                "endTime", studyTime.getEndTime()
        );

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.LATE_ARRIVAL)
                .title("늦은 입장 안내")
                .message("늦었지만 열심히 공부해 주세요!")
                .data(data)
                .timestamp(LocalDateTime.now())
                .priority(2) // 보통 우선순위
                .recipientId(student.getDiscordId())
                .recipientName(student.getUsername())
                .build();

        sendNotificationToAllChannels(notification);
        log.info("늦은 입장 알림 전송: 학생={}, 과목={}, 늦은 시간={}분", 
                student.getUsername(), studyTime.getTitle(), lateMinutes);
    }

    // Private helper methods
    
    private void sendUpcomingStudyNotification(AssignedStudyTime studyTime) {
        Optional<User> studentOpt = userRepository.findById(studyTime.getStudentId());
        if (studentOpt.isEmpty() || studentOpt.get().getDiscordId() == null) {
            return;
        }

        User student = studentOpt.get();
        Map<String, Object> data = Map.of(
                "subject", studyTime.getTitle(),
                "startTime", studyTime.getStartTime(),
                "endTime", studyTime.getEndTime()
        );

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.UPCOMING_STUDY)
                .title("공부 시작 10분 전")
                .message("곧 공부 시간입니다. 준비해 주세요!")
                .data(data)
                .timestamp(LocalDateTime.now())
                .priority(2)
                .recipientId(student.getDiscordId())
                .recipientName(student.getUsername())
                .build();

        sendNotificationToAllChannels(notification);
    }

    private void sendStudyStartNotification(AssignedStudyTime studyTime) {
        Optional<User> studentOpt = userRepository.findById(studyTime.getStudentId());
        if (studentOpt.isEmpty() || studentOpt.get().getDiscordId() == null) {
            return;
        }

        User student = studentOpt.get();
        Map<String, Object> data = Map.of(
                "subject", studyTime.getTitle(),
                "endTime", studyTime.getEndTime()
        );

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.STUDY_START)
                .title("공부 시작 시간")
                .message("지금 음성 채널에 입장해서 공부를 시작하세요!")
                .data(data)
                .timestamp(LocalDateTime.now())
                .priority(3)
                .recipientId(student.getDiscordId())
                .recipientName(student.getUsername())
                .build();

        sendNotificationToAllChannels(notification);
    }

    private void sendMissedStudyNotification(AssignedStudyTime studyTime) {
        Optional<User> studentOpt = userRepository.findById(studyTime.getStudentId());
        if (studentOpt.isEmpty() || studentOpt.get().getDiscordId() == null) {
            return;
        }

        User student = studentOpt.get();
        Map<String, Object> data = Map.of(
                "subject", studyTime.getTitle(),
                "startTime", studyTime.getStartTime(),
                "endTime", studyTime.getEndTime()
        );

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.MISSED_STUDY)
                .title("미접속 알림")
                .message("아직 공부를 시작하지 않으셨습니다.")
                .data(data)
                .timestamp(LocalDateTime.now())
                .priority(3)
                .recipientId(student.getDiscordId())
                .recipientName(student.getUsername())
                .build();

        sendNotificationToAllChannels(notification);
    }

    private void sendDailyTaskSummary(User student, List<AssignedStudyTime> todayStudyTimes) {
        if (student.getDiscordId() == null || student.getDiscordId().trim().isEmpty()) {
            return;
        }

        List<Map<String, Object>> studyTimeData = todayStudyTimes.stream()
                .map(st -> Map.<String, Object>of(
                        "subject", st.getTitle(),
                        "startTime", st.getStartTime(),
                        "endTime", st.getEndTime()
                ))
                .toList();

        Map<String, Object> data = Map.of("studyTimes", studyTimeData);

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.DAILY_TASK)
                .title("오늘의 공부 일정")
                .message("오늘도 화이팅!")
                .data(data)
                .timestamp(LocalDateTime.now())
                .priority(1)
                .recipientId(student.getDiscordId())
                .recipientName(student.getUsername())
                .build();

        sendNotificationToAllChannels(notification);
    }

    /**
     * 모든 활성 채널로 알림 전송
     */
    private void sendNotificationToAllChannels(NotificationMessage notification) {
        for (NotificationChannel channel : notificationChannels) {
            if (channel.isEnabled()) {
                CompletableFuture<Boolean> result = channel.sendMessage(notification);
                result.whenComplete((success, throwable) -> {
                    if (throwable != null) {
                        log.error("알림 전송 실패 - 채널: {}, 사용자: {}", 
                                channel.getChannelType(), notification.getRecipientId(), throwable);
                    } else if (!success) {
                        log.warn("알림 전송 실패 - 채널: {}, 사용자: {}", 
                                channel.getChannelType(), notification.getRecipientId());
                    }
                });
            }
        }
    }

    /**
     * 특정 채널로만 알림 전송 (테스트용)
     */
    public CompletableFuture<Boolean> sendNotificationToChannel(
            NotificationMessage notification, String channelType) {
        
        return notificationChannels.stream()
                .filter(channel -> channel.getChannelType().equals(channelType) && channel.isEnabled())
                .findFirst()
                .map(channel -> channel.sendMessage(notification))
                .orElse(CompletableFuture.completedFuture(false));
    }
} 