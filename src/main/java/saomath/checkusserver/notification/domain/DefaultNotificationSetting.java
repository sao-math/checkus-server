package saomath.checkusserver.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import saomath.checkusserver.entity.RoleConstants;

import java.util.Map;
import java.util.Set;

/**
 * 사용자 역할별 기본 알림 설정
 * 
 * 기본값 + 예외 저장 방식:
 * - 여기에 정의된 기본값을 사용하는 사용자는 DB에 레코드 없음
 * - 기본값과 다르게 설정한 경우에만 notification_setting 테이블에 저장
 */
@Getter
@RequiredArgsConstructor
public enum DefaultNotificationSetting {
    
    /**
     * 학생 기본 알림 설정
     * - 학습 관련 알림은 알림톡 + 디스코드 모두 활성화
     * - 실시간성이 중요한 알림 우선
     */
    STUDENT(RoleConstants.STUDENT, 
        Map.of(
            // 공부 관련 실시간 알림 (알림톡 + 디스코드)
            AlimtalkTemplate.STUDY_REMINDER_10MIN, Set.of("alimtalk", "discord"),
            AlimtalkTemplate.STUDY_START, Set.of("alimtalk", "discord"),
            AlimtalkTemplate.STUDY_ROOM_ENTER, Set.of("discord"), // 입장 완료는 디스코드만
            
            // 학습 관리 알림 (주로 알림톡)
            AlimtalkTemplate.TODAY_TASKS, Set.of("alimtalk"),
            AlimtalkTemplate.YESTERDAY_INCOMPLETE_EVENING, Set.of("alimtalk"),
            
            // 미입장은 학부모에게 주로 발송하므로 학생은 디스코드만
            AlimtalkTemplate.NO_SHOW, Set.of("discord"),
            
            // 확장 기능 (기본 비활성화)
            AlimtalkTemplate.EARLY_LEAVE, Set.of(),
            AlimtalkTemplate.LATE_ARRIVAL, Set.of()
        )
    ),
    
    /**
     * 학부모 기본 알림 설정  
     * - 자녀 학습 현황 모니터링 중심
     * - 알림톡 위주 (카카오톡 사용률 높음)
     */
    GUARDIAN(RoleConstants.GUARDIAN,
        Map.of(
            // 학습 시작 관련 (학부모는 확인용)
            AlimtalkTemplate.STUDY_REMINDER_10MIN, Set.of("alimtalk"),
            AlimtalkTemplate.STUDY_START, Set.of(), // 기본 비활성화
            AlimtalkTemplate.STUDY_ROOM_ENTER, Set.of("alimtalk"),
            
            // 학습 관리 현황
            AlimtalkTemplate.TODAY_TASKS, Set.of("alimtalk"),
            AlimtalkTemplate.YESTERDAY_INCOMPLETE_EVENING, Set.of("alimtalk"),
            
            // 미입장은 학부모에게 중요한 알림
            AlimtalkTemplate.NO_SHOW, Set.of("alimtalk"),
            
            // 확장 기능 (학부모에게도 유용)
            AlimtalkTemplate.EARLY_LEAVE, Set.of("alimtalk"),
            AlimtalkTemplate.LATE_ARRIVAL, Set.of("alimtalk")
        )
    ),
    
    /**
     * 선생님 기본 알림 설정
     * - 학생 관리 및 모니터링 중심
     * - 디스코드 + 알림톡 조합
     */
    TEACHER(RoleConstants.TEACHER,
        Map.of(
            // 선생님은 일반적으로 학습 시작 알림 불필요
            AlimtalkTemplate.STUDY_REMINDER_10MIN, Set.of(),
            AlimtalkTemplate.STUDY_START, Set.of(),
            AlimtalkTemplate.STUDY_ROOM_ENTER, Set.of("discord"), // 입장 확인용
            
            // 과제 관리
            AlimtalkTemplate.TODAY_TASKS, Set.of(),
            AlimtalkTemplate.YESTERDAY_INCOMPLETE_EVENING, Set.of(),
            
            // 미입장은 선생님도 알아야 함
            AlimtalkTemplate.NO_SHOW, Set.of("discord", "alimtalk"),
            
            // 학습 시간 관리
            AlimtalkTemplate.EARLY_LEAVE, Set.of("discord"),
            AlimtalkTemplate.LATE_ARRIVAL, Set.of("discord")
        )
    );
    
    private final String roleName;
    private final Map<AlimtalkTemplate, Set<String>> defaultSettings;
    
    /**
     * 사용자 역할에 따른 기본 설정 조회
     */
    public static DefaultNotificationSetting getByRole(String roleName) {
        for (DefaultNotificationSetting setting : values()) {
            if (setting.getRoleName().equals(roleName)) {
                return setting;
            }
        }
        // 기본값으로 학생 설정 반환
        return STUDENT;
    }
    
    /**
     * 특정 템플릿의 기본 활성화 채널 조회
     */
    public Set<String> getDefaultChannels(AlimtalkTemplate template) {
        return defaultSettings.getOrDefault(template, Set.of());
    }
    
    /**
     * 특정 템플릿이 기본적으로 활성화되어 있는지 확인
     */
    public boolean isDefaultEnabled(AlimtalkTemplate template, String deliveryMethod) {
        Set<String> channels = getDefaultChannels(template);
        return channels.contains(deliveryMethod);
    }
}
