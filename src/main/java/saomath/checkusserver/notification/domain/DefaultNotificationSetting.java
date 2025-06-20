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
     * 노션 알림 설정 데이터베이스 기준으로 설정됨
     * - ON(변경불가): 기본 활성화, 사용자 변경 불가
     * - ON(변경가능): 기본 활성화, 사용자 변경 가능
     * - OFF(변경불가): 기본 비활성화, 사용자 변경 불가
     */
    STUDENT(RoleConstants.STUDENT, 
        Map.of(
            // [학생] 공부시작 10분전 알림 - 카톡: ON(변경불가), 디코: ON(변경불가)
            AlimtalkTemplate.STUDY_REMINDER_10MIN, Set.of("alimtalk", "discord"),
            
            // [학생] 공부시작 알림 - 카톡: OFF(변경불가), 디코: ON(변경불가)
            AlimtalkTemplate.STUDY_START, Set.of("alimtalk", "discord"),
            
            // [학생] 스터디룸 입장 완료 - 카톡: OFF(변경불가), 디코: ON(변경불가)
            AlimtalkTemplate.STUDY_ROOM_ENTER, Set.of("discord"),
            
            // [학생] 미입장 알림 - 카톡: ON(변경불가), 디코: ON(변경불가)
            AlimtalkTemplate.NO_SHOW, Set.of("alimtalk", "discord"),
            
            // [학생] 학습 알림(아침) - 카톡: ON(변경불가), 디코: ON(변경가능)
            AlimtalkTemplate.TODAY_TASKS, Set.of("alimtalk", "discord"),
            
            // [학생] 학습 알림(저녁) - 카톡: ON(변경불가), 디코: ON(변경가능)
            AlimtalkTemplate.YESTERDAY_INCOMPLETE_EVENING, Set.of("alimtalk", "discord"),
            
            // [학생] 늦은 입장 안내 - 카톡: OFF(변경불가), 디코: ON(변경불가)
            AlimtalkTemplate.LATE_ARRIVAL, Set.of("discord"),
            
            // [학생] 조기 퇴장 안내 - 카톡: OFF(변경불가), 디코: ON(변경불가)
            AlimtalkTemplate.EARLY_LEAVE, Set.of("discord")
        )
    ),
    
    /**
     * 학부모 기본 알림 설정  
     * 노션에 학부모 관련 설정이 별도로 정의되지 않아 모든 알림을 기본 비활성화
     * 필요시 사용자가 개별적으로 활성화 가능
     */
    GUARDIAN(RoleConstants.GUARDIAN,
        Map.of(
            AlimtalkTemplate.STUDY_REMINDER_10MIN, Set.of(),
            AlimtalkTemplate.STUDY_START, Set.of(),
            AlimtalkTemplate.STUDY_ROOM_ENTER, Set.of(),
            AlimtalkTemplate.NO_SHOW, Set.of(),
            AlimtalkTemplate.TODAY_TASKS, Set.of(),
            AlimtalkTemplate.YESTERDAY_INCOMPLETE_EVENING, Set.of(),
            AlimtalkTemplate.LATE_ARRIVAL, Set.of(),
            AlimtalkTemplate.EARLY_LEAVE, Set.of()
        )
    ),
    
    /**
     * 선생님 기본 알림 설정
     * 노션에 선생님 관련 설정이 별도로 정의되지 않아 모든 알림을 기본 비활성화
     * 필요시 사용자가 개별적으로 활성화 가능
     */
    TEACHER(RoleConstants.TEACHER,
        Map.of(
            AlimtalkTemplate.STUDY_REMINDER_10MIN, Set.of(),
            AlimtalkTemplate.STUDY_START, Set.of(),
            AlimtalkTemplate.STUDY_ROOM_ENTER, Set.of(),
            AlimtalkTemplate.NO_SHOW, Set.of(),
            AlimtalkTemplate.TODAY_TASKS, Set.of(),
            AlimtalkTemplate.YESTERDAY_INCOMPLETE_EVENING, Set.of(),
            AlimtalkTemplate.LATE_ARRIVAL, Set.of(),
            AlimtalkTemplate.EARLY_LEAVE, Set.of()
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
