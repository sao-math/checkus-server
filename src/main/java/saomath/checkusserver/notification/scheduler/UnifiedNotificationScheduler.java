package saomath.checkusserver.notification.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.ActualStudyTime;
import saomath.checkusserver.exception.ResourceNotFoundException;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.service.MultiChannelNotificationService;
import saomath.checkusserver.notification.service.NotificationService;
import saomath.checkusserver.notification.service.NotificationTargetService;
import saomath.checkusserver.service.StudyTimeService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 통합 알림 스케줄러
 * 알림톡, 디스코드 등 모든 채널을 통합하여 관리
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class UnifiedNotificationScheduler {
    
    private final MultiChannelNotificationService notificationService;
    private final NotificationTargetService targetService;
    private final StudyTimeService studyTimeService;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
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
                "이름", target.getStudentName()
            );
            
            // 멀티채널로 학생에게 알림 전송
            notificationService.sendNotification(
                target.getStudentId(), 
                AlimtalkTemplate.STUDY_REMINDER_10MIN.name(), 
                variables
            ).thenAccept(success -> {
                if (success) {
                    log.debug("10분 전 알림 전송 성공 - 학생 ID: {}", target.getStudentId());
                } else {
                    log.warn("10분 전 알림 전송 실패 - 학생 ID: {}", target.getStudentId());
                }
            });
            
            // 학부모에게도 알림 (설정된 경우)
            if (target.isParentNotificationEnabled() && target.getParentPhone() != null) {
                sendDirectAlimtalkToParent(target.getParentPhone(), AlimtalkTemplate.STUDY_REMINDER_10MIN, variables);
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
                "이름", target.getStudentName()
            );
            
            // 멀티채널로 학생에게 알림 전송
            notificationService.sendNotification(
                target.getStudentId(),
                AlimtalkTemplate.STUDY_START.name(),
                variables
            ).thenAccept(success -> {
                if (success) {
                    log.debug("공부 시작 알림 전송 성공 - 학생 ID: {}", target.getStudentId());
                }
            });
            
            // 학부모에게도 알림 (설정된 경우)
            if (target.isParentNotificationEnabled() && target.getParentPhone() != null) {
                sendDirectAlimtalkToParent(target.getParentPhone(), AlimtalkTemplate.STUDY_START, variables);
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
            } catch (ResourceNotFoundException e) {
                log.warn("할당된 공부시간을 찾을 수 없어 세션 연결 스킵: 할당 ID={}, 오류: {}", 
                        assignedStudyTime.getId(), e.getMessage());
            } catch (Exception e) {
                log.error("세션 연결 실패: 할당 ID={}, 학생 ID={}", 
                        assignedStudyTime.getId(), assignedStudyTime.getStudentId(), e);
            }
        }
        
        log.debug("세션 연결 체크 완료 - 대상: {}건, 연결: {}건", assignedStudyTimes.size(), connectedSessions);
    }
    
//    /**
//     * 매일 아침 8시: 오늘의 할일 알림
//     */
//    @Scheduled(cron = "0 0 8 * * *")
//    public void sendTodayTasksNotification() {
//        log.info("오늘의 할일 알림 시작");
//
//        List<NotificationTargetService.TaskTarget> targets =
//            targetService.getTodayTaskTargets();
//
//        for (NotificationTargetService.TaskTarget target : targets) {
//            Map<String, String> variables = Map.of(
//                "이름", target.getStudentName(),
//                "1", target.getTaskListString(),
//                "2", "" // 미완료 과제는 빈 값으로 설정 (오늘 시작이므로)
//            );
//
//            // 멀티채널로 학생에게 알림 전송
//            notificationService.sendNotification(
//                target.getStudentId(),
//                AlimtalkTemplate.TODAY_TASKS.name(),
//                variables
//            ).thenAccept(success -> {
//                if (success) {
//                    log.debug("오늘의 할일 알림 전송 성공 - 학생 ID: {}", target.getStudentId());
//                }
//            });
//
//            // 학부모에게도 알림 (설정된 경우)
//            if (target.isParentNotificationEnabled() && target.getParentPhone() != null) {
//                sendDirectAlimtalkToParent(target.getParentPhone(), AlimtalkTemplate.TODAY_TASKS, variables);
//            }
//        }
//
//        log.info("오늘의 할일 알림 발송 완료 - {}건", targets.size());
//    }
//
//    /**
//     * 매일 아침 8시 30분: 전날 미완료 할일 알림 (아침)
//     */
//    @Scheduled(cron = "0 30 8 * * *")
//    public void sendYesterdayIncompleteMorning() {
//        log.info("전날 미완료 할일 알림 (아침) 시작");
//
//        List<NotificationTargetService.TaskTarget> targets =
//            targetService.getYesterdayIncompleteTaskTargets();
//
//        for (NotificationTargetService.TaskTarget target : targets) {
//            if (target.getTaskCount() > 0) {
//                Map<String, String> variables = Map.of(
//                    "이름", target.getStudentName(),
//                    "1", "", // 오늘의 과제는 빈 값
//                    "2", target.getTaskListString() // 미완료 과제
//                );
//
//                // 멀티채널로 학생에게 알림 전송
//                notificationService.sendNotification(
//                    target.getStudentId(),
//                    AlimtalkTemplate.TODAY_TASKS.name(),
//                    variables
//                ).thenAccept(success -> {
//                    if (success) {
//                        log.debug("전날 미완료 할일 알림(아침) 전송 성공 - 학생 ID: {}", target.getStudentId());
//                    }
//                });
//
//                // 학부모에게도 알림 (설정된 경우)
//                if (target.isParentNotificationEnabled() && target.getParentPhone() != null) {
//                    sendDirectAlimtalkToParent(target.getParentPhone(), AlimtalkTemplate.TODAY_TASKS, variables);
//                }
//            }
//        }
//
//        log.info("전날 미완료 할일 알림 (아침) 발송 완료");
//    }
//
//    /**
//     * 매일 저녁 8시: 전날 미완료 할일 알림 (저녁)
//     */
//    @Scheduled(cron = "0 0 20 * * *")
//    public void sendYesterdayIncompleteEvening() {
//        log.info("전날 미완료 할일 알림 (저녁) 시작");
//
//        List<NotificationTargetService.TaskTarget> targets =
//            targetService.getYesterdayIncompleteTaskTargets();
//
//        for (NotificationTargetService.TaskTarget target : targets) {
//            if (target.getTaskCount() > 0) {
//                Map<String, String> variables = Map.of(
//                    "이름", target.getStudentName(),
//                    "1", target.getTaskListString() // 미완료 과제
//                );
//
//                // 멀티채널로 학생에게 알림 전송
//                notificationService.sendNotification(
//                    target.getStudentId(),
//                    AlimtalkTemplate.YESTERDAY_INCOMPLETE_EVENING.name(),
//                    variables
//                ).thenAccept(success -> {
//                    if (success) {
//                        log.debug("전날 미완료 할일 알림(저녁) 전송 성공 - 학생 ID: {}", target.getStudentId());
//                    }
//                });
//
//                // 학부모에게도 알림 (설정된 경우)
//                if (target.isParentNotificationEnabled() && target.getParentPhone() != null) {
//                    sendDirectAlimtalkToParent(target.getParentPhone(), AlimtalkTemplate.YESTERDAY_INCOMPLETE_EVENING, variables);
//                }
//            }
//        }
//
//        log.info("전날 미완료 할일 알림 (저녁) 발송 완료");
//    }
    
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
                "이름", target.getStudentName()
            );
            
            // 학생에게도 발송 설정된 경우
            if (target.isStudentNotificationEnabled()) {
                notificationService.sendNotification(
                    target.getStudentId(),
                    AlimtalkTemplate.NO_SHOW.name(),
                    variables
                ).thenAccept(success -> {
                    if (success) {
                        log.debug("미접속 알림 전송 성공 - 학생 ID: {}", target.getStudentId());
                    }
                });
            }
            
            // 미접속 알림은 주로 학부모에게 발송
            if (target.isParentNotificationEnabled() && target.getParentPhone() != null) {
                sendDirectAlimtalkToParent(target.getParentPhone(), AlimtalkTemplate.NO_SHOW, variables);
            }
        }
        
        log.debug("미접속 체크 완료 - {}건", targets.size());
    }
    
    /**
     * 학부모에게 직접 알림톡 전송 헬퍼 메서드
     */
    private void sendDirectAlimtalkToParent(String parentPhone, AlimtalkTemplate template, Map<String, String> variables) {
        if (parentPhone == null || parentPhone.isEmpty()) {
            return;
        }
        
        // 전화번호 마스킹 (로그용)
        String maskedPhone = parentPhone.length() > 7 
            ? parentPhone.substring(0, 3) + "****" + parentPhone.substring(7)
            : "****";
        
        // 알림톡 채널로만 전송 (학부모는 주로 카카오톡 사용)
        CompletableFuture<Boolean> future = notificationService.sendNotificationToChannel(
            parentPhone,
            template.name(),
            variables,
            NotificationService.NotificationChannel.ALIMTALK
        );
        
        if (future != null) {
            future.thenAccept(success -> {
                if (success) {
                    log.debug("학부모 알림톡 전송 성공 - 전화번호: {}, 템플릿: {}", 
                        maskedPhone, template.name());
                } else {
                    log.warn("학부모 알림톡 전송 실패 - 전화번호: {}, 템플릿: {}", 
                        maskedPhone, template.name());
                }
            });
        } else {
            log.error("학부모 알림톡 전송 실패 - sendNotificationToChannel이 null 반환 - 전화번호: {}, 템플릿: {}", 
                maskedPhone, template.name());
        }
    }
}
