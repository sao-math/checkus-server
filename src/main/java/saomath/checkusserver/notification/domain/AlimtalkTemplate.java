package saomath.checkusserver.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum AlimtalkTemplate {
    
    // ========== 기능명세서 정의 템플릿 ==========
    
    // 공부 시작 10분 전 알림 - D0001
    STUDY_REMINDER_10MIN("D0001", 
        "[사오수학]\n" +
        "#{이름} 학생, \n" +
        "곧 공부 시작할 시간이에요!  \n" +
        "지금부터 10분 뒤 학습 시작입니다. \n" +
        "오늘도 빠짐없이 계획을 끝내볼까요?"),
    
    // 공부 시작 알림 - D0002
    STUDY_START("D0002",
        "[사오수학]\n" +
        "#{이름} 학생, \n" +
        "공부 시작할 시간입니다!\n" +
        "스터디룸에 입장해 주세요."),
    
    // 미입장 알림 - D0003
    NO_SHOW("D0003",
        "[사오수학]\n" +
        "#{이름} 학생\n" +
        "아직 스터디룸 입장이 확인되지 \n" +
        "않았습니다.\n" +
        "빠르게 입장해주세요!\n" +
        "*오늘 학습 스케줄 이행이 힘든 경우\n" +
        "담당선생님께 꼭 연락해주세요!"),
    
    // 스터디룸 입장 완료 - D0004
    STUDY_ROOM_ENTER("D0004",
        "[사오수학] #{이름} 학생 스터디룸 입장이 확인되었습니다. \n" +
        "⏰ 입장 시간: #{입장시간}\n\n" +
        "오늘도 열심히 공부해봐요! \n" +
        "계획된 학습을 차근차근 완료해보세요."),
    
    // 오늘의 할일 알림 (아침) - S0001
    TODAY_TASKS("S0001",
        "[사오수학]\n" +
        "#{이름} 학생\n" +
        "오늘의 학습 계획 도착!\n" +
        "📝 오늘의 과제\n" +
        "#{1}\n" +
        "⏰ 미완료 과제\n" +
        "#{2}\n" +
        "*과제 이행이 어려운 경우 \n" +
        "담임 선생님께 꼭 계획 조정을 \n" +
        "요청해 주세요!"),
    
    // 전날 미완료 할일 알림 (저녁) - S0002
    YESTERDAY_INCOMPLETE_EVENING("S0002",
        "[사오수학]\n" +
        "#{이름} 학생\n" +
        "아직 미이행 된 과제가 있어요!\n" +
        "⏰ 미완료 과제\n" +
        "#{1}\n" +
        "*과제 이행이 어려운 경우 \n" +
        "담임 선생님께 꼭 계획 조정을 \n" +
        "요청해 주세요!"),
    
    // ========== 확장 템플릿 (향후 기능용) ==========
    
    // 조기퇴장 알림 - D0006
    EARLY_LEAVE("D0006",
        "[사오수학]\n" +
        "공부 시간이 아직 남아있습니다.\n" +
        "다시 돌아와서 마저 공부해 주세요! 📖\n" +
        "*오늘 할 일을 마무리 한 경우는 조기 퇴장해도 좋습니다!"),
    
    // 늦은입장 알림 - D0005  
    LATE_ARRIVAL("D0005",
        "[사오수학]\n" +
        "⏰ 늦은 입장 안내\n" +
        "🕐 늦은 시간: #{늦은시간}분\n" +
        "늦었지만 열심히 공부해 주세요!\n" +
        "다음부터는 시간을 지켜주세요 😊");
    
    private final String templateCode;
    private final String templateMessage;
    
    /**
     * 학생 역할에서 변경 불가능한 채널 정보
     * 노션 설정에서 "변경불가"로 표시된 항목들
     */
    private static final Map<AlimtalkTemplate, Set<String>> STUDENT_READONLY_CHANNELS = Map.of(
        // ON(변경불가) - 카톡과 디코 모두 변경 불가
        STUDY_REMINDER_10MIN, Set.of("alimtalk", "discord"),
        NO_SHOW, Set.of("alimtalk", "discord"),
        
        // OFF(변경불가) - 디코만 ON, 카톡은 OFF 고정
        STUDY_START, Set.of("alimtalk"),  // 카톡만 변경 불가 (OFF 고정)
        STUDY_ROOM_ENTER, Set.of("alimtalk"),  // 카톡만 변경 불가 (OFF 고정)
        LATE_ARRIVAL, Set.of("alimtalk"),  // 카톡만 변경 불가 (OFF 고정)
        EARLY_LEAVE, Set.of("alimtalk"),  // 카톡만 변경 불가 (OFF 고정)
        
        // 학습 알림들은 카톡만 변경 불가 (ON 고정), 디코는 변경 가능
        TODAY_TASKS, Set.of("alimtalk"),
        YESTERDAY_INCOMPLETE_EVENING, Set.of("alimtalk")
    );
    
    /**
     * 학생 역할에서 특정 채널이 변경 불가능한지 확인
     * @param deliveryMethod 전송 방법 ("alimtalk", "discord")
     * @return 변경 불가능하면 true
     */
    public boolean isReadOnlyForStudent(String deliveryMethod) {
        Set<String> readOnlyChannels = STUDENT_READONLY_CHANNELS.get(this);
        return readOnlyChannels != null && readOnlyChannels.contains(deliveryMethod);
    }
    
    /**
     * 학생 역할에서 변경 불가능한 모든 채널 조회
     * @return 변경 불가능한 채널 집합
     */
    public Set<String> getReadOnlyChannelsForStudent() {
        return STUDENT_READONLY_CHANNELS.getOrDefault(this, Set.of());
    }
    
    /**
     * 각 템플릿의 사용자 친화적인 설명 반환
     */
    public String getDescription() {
        switch (this) {
            case STUDY_REMINDER_10MIN:
                return "공부 시작 10분 전 알림";
            case STUDY_START:
                return "공부 시작 시간 알림";
            case NO_SHOW:
                return "미접속 알림";
            case STUDY_ROOM_ENTER:
                return "스터디룸 입장 알림";
            case TODAY_TASKS:
                return "오늘의 할일 알림 (아침)";
            case YESTERDAY_INCOMPLETE_EVENING:
                return "전날 미완료 할일 알림 (저녁)";
            case EARLY_LEAVE:
                return "조기퇴장 알림";
            case LATE_ARRIVAL:
                return "늦은입장 알림";
            default:
                return this.name();
        }
    }
}
