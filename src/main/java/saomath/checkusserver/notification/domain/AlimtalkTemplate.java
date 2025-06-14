package saomath.checkusserver.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlimtalkTemplate {
    
    // 공부 시작 10분 전 알림
    STUDY_REMINDER_10MIN("study_reminder_10min", 
        "공부 시작 10분 전입니다!\n\n" +
        "학생: #{studentName}\n" +
        "과목: #{activityName}\n" +
        "시간: #{startTime} ~ #{endTime}\n\n" +
        "곧 공부를 시작해주세요!"),
    
    // 공부 시작 알림
    STUDY_START("study_start",
        "공부 시작 시간입니다!\n\n" +
        "학생: #{studentName}\n" +
        "과목: #{activityName}\n" +
        "시간: #{startTime} ~ #{endTime}\n\n" +
        "지금 스터디룸에 입장해주세요!"),
    
    // 스터디룸 입장 알림
    STUDY_ROOM_ENTER("study_room_enter",
        "스터디룸 입장 완료\n\n" +
        "학생: #{studentName}\n" +
        "입장 시간: #{enterTime}\n\n" +
        "오늘도 열심히 공부해봐요!"),
    
    // 미접속 알림
    NO_SHOW("no_show",
        "공부 시간 미접속 알림\n\n" +
        "학생: #{studentName}\n" +
        "예정 시간: #{startTime} ~ #{endTime}\n\n" +
        "아직 스터디룸에 접속하지 않았습니다.\n" +
        "확인 부탁드립니다."),
    
    // 오늘의 할일 알림 (아침)
    TODAY_TASKS("today_tasks",
        "오늘의 할일\n\n" +
        "학생: #{studentName}\n" +
        "오늘 할일: #{taskCount}개\n\n" +
        "#{taskList}\n\n" +
        "오늘도 화이팅!"),
    
    // 전날 미완료 할일 알림 (아침)
    YESTERDAY_INCOMPLETE_MORNING("yesterday_incomplete_morning",
        "어제 미완료 할일 알림\n\n" +
        "학생: #{studentName}\n" +
        "미완료 할일: #{incompleteCount}개\n\n" +
        "#{taskList}\n\n" +
        "오늘 꼭 완료해주세요!"),
    
    // 전날 미완료 할일 알림 (저녁)
    YESTERDAY_INCOMPLETE_EVENING("yesterday_incomplete_evening",
        "미완료 할일 확인\n\n" +
        "학생: #{studentName}\n" +
        "미완료 할일: #{incompleteCount}개\n\n" +
        "#{taskList}\n\n" +
        "내일까지 꼭 완료해주세요!");
    
    private final String templateCode;
    private final String templateMessage;
}
