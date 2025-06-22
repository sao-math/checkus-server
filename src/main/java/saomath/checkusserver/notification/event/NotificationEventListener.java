package saomath.checkusserver.notification.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import saomath.checkusserver.entity.StudentGuardian;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.event.StudyAttendanceEvent;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.service.MultiChannelNotificationService;
import saomath.checkusserver.repository.StudentGuardianRepository;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 통합 알림 이벤트 리스너
 * 스터디룸 입장, 조기퇴장, 늦은 입장 등 실시간 이벤트 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    
    private final MultiChannelNotificationService notificationService;
    private final StudentGuardianRepository studentGuardianRepository;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    /**
     * 스터디룸 입장 이벤트 처리
     */
    @Async
    @EventListener
    public void handleStudyRoomEnterEvent(StudyRoomEnterEvent event) {
        log.info("스터디룸 입장 이벤트 수신 - 학생: {}, 시간: {}", 
            event.getStudentName(), event.getEnterTime());
        
        try {
            // 알림 변수 설정
            Map<String, String> variables = new HashMap<>();
            variables.put("이름", event.getStudentName());
            variables.put("입장시간", event.getEnterTime().toString()); // ISO 형식으로 전달
            
            // 멀티채널로 학생에게 알림 전송
            notificationService.sendNotification(
                event.getStudentId(),
                AlimtalkTemplate.STUDY_ROOM_ENTER.name(),
                variables
            ).thenAccept(success -> {
                if (success) {
                    log.debug("학생 입장 알림 전송 성공 - ID: {}", event.getStudentId());
                } else {
                    log.warn("학생 입장 알림 전송 실패 - ID: {}", event.getStudentId());
                }
            });
            
            // 학부모에게도 알림 전송
            sendToGuardians(event, variables);
            
        } catch (Exception e) {
            log.error("스터디룸 입장 알림 처리 중 오류", e);
        }
    }
    
    /**
     * 출석 관련 이벤트 처리 (조기퇴장, 늦은입장)
     */
    @Async
    @EventListener
    public void handleStudyAttendanceEvent(StudyAttendanceEvent event) {
        log.info("출석 이벤트 수신 - 타입: {}, 학생: {}, 시간: {}분", 
            event.getEventType(), event.getStudent().getName(), event.getMinutes());
        
        try {
            switch (event.getEventType()) {
                case EARLY_LEAVE:
                    handleEarlyLeaveEvent(event.getStudent(), event.getStudyTime(), event.getMinutes());
                    break;
                case LATE_ARRIVAL:
                    handleLateArrivalEvent(event.getStudent(), event.getStudyTime(), event.getMinutes());
                    break;
                default:
                    log.warn("알 수 없는 출석 이벤트 타입: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("출석 이벤트 처리 중 오류 - 타입: {}, 학생: {}", 
                event.getEventType(), event.getStudent().getName(), e);
        }
    }
    
    /**
     * 조기퇴장 이벤트 처리
     */
    private void handleEarlyLeaveEvent(User student, AssignedStudyTime studyTime, long remainingMinutes) {
        Map<String, String> variables = new HashMap<>();
        
        // 조기퇴장 알림 전송
        notificationService.sendNotification(
            student.getId(),
            AlimtalkTemplate.EARLY_LEAVE.name(),
            variables
        ).thenAccept(success -> {
            if (success) {
                log.debug("조기퇴장 알림 전송 성공 - 학생 ID: {}", student.getId());
            }
        });
        
        // 학부모에게도 알림
        sendToGuardiansByStudentId(student.getId(), AlimtalkTemplate.EARLY_LEAVE.name(), variables);
    }
    
    /**
     * 늦은입장 이벤트 처리  
     */
    private void handleLateArrivalEvent(User student, AssignedStudyTime studyTime, long lateMinutes) {
        Map<String, String> variables = new HashMap<>();
        // CSV D0005 버전은 늦은시간 변수만 사용
        variables.put("늦은시간", String.valueOf(lateMinutes));
        
        // 늦은입장 알림 전송
        notificationService.sendNotification(
            student.getId(),
            AlimtalkTemplate.LATE_ARRIVAL.name(),
            variables
        ).thenAccept(success -> {
            if (success) {
                log.debug("늦은입장 알림 전송 성공 - 학생 ID: {}", student.getId());
            }
        });
        
        // 학부모에게도 알림
        sendToGuardiansByStudentId(student.getId(), AlimtalkTemplate.LATE_ARRIVAL.name(), variables);
    }
    
    /**
     * 스터디룸 입장 시 학부모에게 알림 전송
     */
    private void sendToGuardians(StudyRoomEnterEvent event, Map<String, String> variables) {
        sendToGuardiansByStudentId(event.getStudentId(), AlimtalkTemplate.STUDY_ROOM_ENTER.name(), variables);
    }
    
    /**
     * 학생 ID로 학부모에게 알림 전송
     */
    private void sendToGuardiansByStudentId(Long studentId, String templateId, Map<String, String> variables) {
        List<StudentGuardian> guardians = studentGuardianRepository.findByStudentId(studentId);
        
        for (StudentGuardian guardian : guardians) {
            notificationService.sendNotification(
                guardian.getGuardian().getId(),
                templateId,
                variables
            ).thenAccept(success -> {
                if (success) {
                    log.debug("학부모 알림 전송 성공 - 학부모 ID: {}, 템플릿: {}", 
                        guardian.getGuardian().getId(), templateId);
                } else {
                    log.warn("학부모 알림 전송 실패 - 학부모 ID: {}, 템플릿: {}", 
                        guardian.getGuardian().getId(), templateId);
                }
            });
        }
    }
}
