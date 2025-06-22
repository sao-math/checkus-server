package saomath.checkusserver.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import saomath.checkusserver.studyTime.domain.AssignedStudyTime;
import saomath.checkusserver.auth.domain.User;

/**
 * 출석 관련 이벤트
 * 순환 의존성을 방지하기 위한 이벤트 기반 알림 시스템
 */
@Getter
public class StudyAttendanceEvent extends ApplicationEvent {
    
    public enum EventType {
        EARLY_LEAVE,
        LATE_ARRIVAL
    }
    
    private final EventType eventType;
    private final User student;
    private final AssignedStudyTime studyTime;
    private final long minutes; // 조기퇴장: 남은 시간, 늦은 입장: 늦은 시간
    
    public StudyAttendanceEvent(Object source, EventType eventType, User student, 
                              AssignedStudyTime studyTime, long minutes) {
        super(source);
        this.eventType = eventType;
        this.student = student;
        this.studyTime = studyTime;
        this.minutes = minutes;
    }
} 