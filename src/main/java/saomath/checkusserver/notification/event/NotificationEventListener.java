package saomath.checkusserver.notification.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import saomath.checkusserver.entity.StudentGuardian;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.service.MultiChannelNotificationService;
import saomath.checkusserver.notification.service.NotificationService;
import saomath.checkusserver.repository.StudentGuardianRepository;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 알림 이벤트 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    
    private final MultiChannelNotificationService notificationService;
    private final StudentGuardianRepository studentGuardianRepository;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    @Async
    @EventListener
    public void handleStudyRoomEnterEvent(StudyRoomEnterEvent event) {
        log.info("스터디룸 입장 이벤트 수신 - 학생: {}, 시간: {}", 
            event.getStudentName(), event.getEnterTime());
        
        try {
            // 알림 변수 설정
            Map<String, String> variables = new HashMap<>();
            variables.put("studentName", event.getStudentName());
            variables.put("enterTime", event.getEnterTime().format(TIME_FORMATTER));
            
            // 학생에게 알림 전송
            notificationService.sendNotification(
                event.getStudentId(),
                AlimtalkTemplate.STUDY_ROOM_ENTER.name(),
                variables
            ).thenAccept(success -> {
                if (success) {
                    log.debug("학생 입장 알림 전송 성공 - ID: {}", event.getStudentId());
                }
            });
            
            // 학부모에게도 알림 전송
            sendToGuardians(event, variables);
            
        } catch (Exception e) {
            log.error("스터디룸 입장 알림 처리 중 오류", e);
        }
    }
    
    /**
     * 학부모에게 알림 전송
     */
    private void sendToGuardians(StudyRoomEnterEvent event, Map<String, String> variables) {
        List<StudentGuardian> guardians = studentGuardianRepository.findByStudentId(event.getStudentId());
        
        for (StudentGuardian guardian : guardians) {
            notificationService.sendNotification(
                guardian.getGuardian().getId(),
                AlimtalkTemplate.STUDY_ROOM_ENTER.name(),
                variables
            ).thenAccept(success -> {
                if (success) {
                    log.debug("학부모 입장 알림 전송 성공 - 학부모 ID: {}", guardian.getGuardian().getId());
                }
            });
        }
    }
}
