package saomath.checkusserver.notification.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.ActualStudyTime;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.service.AlimtalkService;
import saomath.checkusserver.notification.service.NotificationTargetService;
import saomath.checkusserver.service.StudyTimeService;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class AlimtalkScheduler {
    
    private final AlimtalkService alimtalkService;
    private final NotificationTargetService targetService;
    private final StudyTimeService studyTimeService;
    
    /**
     * 매 분마다 실행: 공부 시작 10분 전 알림
     */
    @Scheduled(cron = "0 * * * * *")
    public void sendStudyReminder10Min() {
        log.debug("공부 시작 10분 전 알림 체크 시작");
        
        LocalDateTime targetTime = LocalDateTime.now().plusMinutes(10);
        List<NotificationTargetService.StudyTarget> targets = 
            targetService.getStudyTargetsForTime(targetTime);
        
        for (NotificationTargetService.StudyTarget target : targets) {
            Map<String, String> variables = Map.of(
                "studentName", target.getStudentName(),
                "activityName", target.getActivityName(),
                "startTime", target.getFormattedStartTime(),
                "endTime", target.getFormattedEndTime()
            );
            
            // 학생에게 알림
            sendNotification(target.getStudentPhone(), AlimtalkTemplate.STUDY_REMINDER_10MIN, variables);
            
            // 학부모에게도 알림 (설정된 경우)
            if (target.isParentNotificationEnabled()) {
                sendNotification(target.getParentPhone(), AlimtalkTemplate.STUDY_REMINDER_10MIN, variables);
            }
        }
        
        log.debug("공부 시작 10분 전 알림 발송 완료 - {}건", targets.size());
    }
    
    /**
     * 매 분마다 실행: 공부 시작 시간 알림 + 세션 연결 체크
     */
    @Scheduled(cron = "0 * * * * *")
    public void sendStudyStartNotificationAndConnectSessions() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("공부 시작 시간 알림 및 세션 연결 체크 시작: {}", now);
        
        // 1. 공부 시작 시간 알림 발송
        List<NotificationTargetService.StudyTarget> targets = 
            targetService.getStudyTargetsForTime(now);
        
        for (NotificationTargetService.StudyTarget target : targets) {
            Map<String, String> variables = Map.of(
                "studentName", target.getStudentName(),
                "activityName", target.getActivityName(),
                "startTime", target.getFormattedStartTime(),
                "endTime", target.getFormattedEndTime()
            );
            
            sendNotification(target.getStudentPhone(), AlimtalkTemplate.STUDY_START, variables);
            
            if (target.isParentNotificationEnabled()) {
                sendNotification(target.getParentPhone(), AlimtalkTemplate.STUDY_START, variables);
            }
        }
        
        log.debug("공부 시작 시간 알림 발송 완료 - {}건", targets.size());
        
        // 2. 10분 전에 시작된 공부시간의 세션 연결 체크
        LocalDateTime tenMinutesAgo = now.minusMinutes(10);
        List<AssignedStudyTime> assignedStudyTimes = studyTimeService.getAssignedStudyTimesByDateRange(
                tenMinutesAgo.minusMinutes(1), 
                tenMinutesAgo.plusMinutes(1)
        );
        
        int connectedSessions = 0;
        for (AssignedStudyTime assignedStudyTime : assignedStudyTimes) {
            try {
                ActualStudyTime connected = studyTimeService.connectPreviousOngoingSession(assignedStudyTime.getId());
                if (connected != null) {
                    connectedSessions++;
                    log.info("이전 진행중인 세션 연결 성공: 할당 ID={}, 학생 ID={}, 실제 세션 ID={}", 
                            assignedStudyTime.getId(), assignedStudyTime.getStudentId(), connected.getId());
                }
            } catch (Exception e) {
                log.error("세션 연결 실패: 할당 ID={}, 학생 ID={}", 
                        assignedStudyTime.getId(), assignedStudyTime.getStudentId(), e);
            }
        }
        
        log.debug("세션 연결 체크 완료 - 대상: {}건, 연결: {}건", assignedStudyTimes.size(), connectedSessions);
    }
    
    /**
     * 매일 아침 8시: 오늘의 할일 알림
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendTodayTasksNotification() {
        log.info("오늘의 할일 알림 시작");
        
        List<NotificationTargetService.TaskTarget> targets = 
            targetService.getTodayTaskTargets();
        
        for (NotificationTargetService.TaskTarget target : targets) {
            Map<String, String> variables = Map.of(
                "studentName", target.getStudentName(),
                "taskCount", String.valueOf(target.getTaskCount()),
                "taskList", target.getTaskListString()
            );
            
            sendNotification(target.getStudentPhone(), AlimtalkTemplate.TODAY_TASKS, variables);
            
            if (target.isParentNotificationEnabled()) {
                sendNotification(target.getParentPhone(), AlimtalkTemplate.TODAY_TASKS, variables);
            }
        }
        
        log.info("오늘의 할일 알림 발송 완료 - {}건", targets.size());
    }
    
    /**
     * 매일 아침 8시 30분: 전날 미완료 할일 알림 (아침)
     */
    @Scheduled(cron = "0 30 8 * * *")
    public void sendYesterdayIncompleteMorning() {
        log.info("전날 미완료 할일 알림 (아침) 시작");
        
        List<NotificationTargetService.TaskTarget> targets = 
            targetService.getYesterdayIncompleteTaskTargets();
        
        for (NotificationTargetService.TaskTarget target : targets) {
            if (target.getTaskCount() > 0) {
                Map<String, String> variables = Map.of(
                    "studentName", target.getStudentName(),
                    "incompleteCount", String.valueOf(target.getTaskCount()),
                    "taskList", target.getTaskListString()
                );
                
                sendNotification(target.getStudentPhone(), 
                    AlimtalkTemplate.YESTERDAY_INCOMPLETE_MORNING, variables);
                
                if (target.isParentNotificationEnabled()) {
                    sendNotification(target.getParentPhone(), 
                        AlimtalkTemplate.YESTERDAY_INCOMPLETE_MORNING, variables);
                }
            }
        }
        
        log.info("전날 미완료 할일 알림 (아침) 발송 완료");
    }
    
    /**
     * 매일 저녁 8시: 전날 미완료 할일 알림 (저녁)
     */
    @Scheduled(cron = "0 0 20 * * *")
    public void sendYesterdayIncompleteEvening() {
        log.info("전날 미완료 할일 알림 (저녁) 시작");
        
        List<NotificationTargetService.TaskTarget> targets = 
            targetService.getYesterdayIncompleteTaskTargets();
        
        for (NotificationTargetService.TaskTarget target : targets) {
            if (target.getTaskCount() > 0) {
                Map<String, String> variables = Map.of(
                    "studentName", target.getStudentName(),
                    "incompleteCount", String.valueOf(target.getTaskCount()),
                    "taskList", target.getTaskListString()
                );
                
                sendNotification(target.getStudentPhone(), 
                    AlimtalkTemplate.YESTERDAY_INCOMPLETE_EVENING, variables);
                
                if (target.isParentNotificationEnabled()) {
                    sendNotification(target.getParentPhone(), 
                        AlimtalkTemplate.YESTERDAY_INCOMPLETE_EVENING, variables);
                }
            }
        }
        
        log.info("전날 미완료 할일 알림 (저녁) 발송 완료");
    }
    
    /**
     * 매 5분마다 실행: 미접속 체크 (공부 시작 후 15분)
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void checkNoShow() {
        log.debug("미접속 체크 시작");
        
        LocalDateTime checkTime = LocalDateTime.now().minusMinutes(15);
        List<NotificationTargetService.NoShowTarget> targets = 
            targetService.getNoShowTargets(checkTime);
        
        for (NotificationTargetService.NoShowTarget target : targets) {
            Map<String, String> variables = Map.of(
                "studentName", target.getStudentName(),
                "startTime", target.getFormattedStartTime(),
                "endTime", target.getFormattedEndTime()
            );
            
            // 미접속 알림은 주로 학부모에게 발송
            if (target.isParentNotificationEnabled()) {
                sendNotification(target.getParentPhone(), AlimtalkTemplate.NO_SHOW, variables);
            }
            
            // 학생에게도 발송 설정된 경우
            if (target.isStudentNotificationEnabled()) {
                sendNotification(target.getStudentPhone(), AlimtalkTemplate.NO_SHOW, variables);
            }
        }
        
        log.debug("미접속 체크 완료 - {}건", targets.size());
    }
    
    /**
     * 알림 발송 헬퍼 메서드
     */
    private void sendNotification(String phoneNumber, AlimtalkTemplate template, Map<String, String> variables) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return;
        }
        
        try {
            boolean success = alimtalkService.sendAlimtalk(phoneNumber, template, variables);
            if (!success) {
                log.warn("알림톡 발송 실패 - 수신자: {}, 템플릿: {}", phoneNumber, template.name());
            }
        } catch (Exception e) {
            log.error("알림톡 발송 중 오류 - 수신자: {}, 템플릿: {}", phoneNumber, template.name(), e);
        }
    }
}
