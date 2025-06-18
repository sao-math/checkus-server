package saomath.checkusserver.notification.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AlimtalkTemplate의 변경 불가능 설정 테스트
 * 노션 알림 설정에서 "변경불가" 표시된 항목들에 대한 테스트
 */
public class AlimtalkTemplateReadOnlyTest {
    
    @Test
    public void testStudentReadOnlyChannels() {
        // 공부시작 10분전 알림 - 카톡: ON(변경불가), 디코: ON(변경불가)
        assertThat(AlimtalkTemplate.STUDY_REMINDER_10MIN.isReadOnlyForStudent("alimtalk")).isTrue();
        assertThat(AlimtalkTemplate.STUDY_REMINDER_10MIN.isReadOnlyForStudent("discord")).isTrue();
        
        // 미입장 알림 - 카톡: ON(변경불가), 디코: ON(변경불가)
        assertThat(AlimtalkTemplate.NO_SHOW.isReadOnlyForStudent("alimtalk")).isTrue();
        assertThat(AlimtalkTemplate.NO_SHOW.isReadOnlyForStudent("discord")).isTrue();
        
        // 공부시작 알림 - 카톡: OFF(변경불가), 디코: ON(변경불가) → 카톡만 변경 불가
        assertThat(AlimtalkTemplate.STUDY_START.isReadOnlyForStudent("alimtalk")).isTrue();
        assertThat(AlimtalkTemplate.STUDY_START.isReadOnlyForStudent("discord")).isFalse();
        
        // 스터디룸 입장 완료 - 카톡: OFF(변경불가), 디코: ON(변경불가) → 카톡만 변경 불가
        assertThat(AlimtalkTemplate.STUDY_ROOM_ENTER.isReadOnlyForStudent("alimtalk")).isTrue();
        assertThat(AlimtalkTemplate.STUDY_ROOM_ENTER.isReadOnlyForStudent("discord")).isFalse();
        
        // 학습 알림(아침) - 카톡: ON(변경불가), 디코: ON(변경가능) → 카톡만 변경 불가
        assertThat(AlimtalkTemplate.TODAY_TASKS.isReadOnlyForStudent("alimtalk")).isTrue();
        assertThat(AlimtalkTemplate.TODAY_TASKS.isReadOnlyForStudent("discord")).isFalse();
        
        // 학습 알림(저녁) - 카톡: ON(변경불가), 디코: ON(변경가능) → 카톡만 변경 불가
        assertThat(AlimtalkTemplate.YESTERDAY_INCOMPLETE_EVENING.isReadOnlyForStudent("alimtalk")).isTrue();
        assertThat(AlimtalkTemplate.YESTERDAY_INCOMPLETE_EVENING.isReadOnlyForStudent("discord")).isFalse();
        
        // 늦은 입장 안내 - 카톡: OFF(변경불가), 디코: ON(변경불가) → 카톡만 변경 불가
        assertThat(AlimtalkTemplate.LATE_ARRIVAL.isReadOnlyForStudent("alimtalk")).isTrue();
        assertThat(AlimtalkTemplate.LATE_ARRIVAL.isReadOnlyForStudent("discord")).isFalse();
        
        // 조기 퇴장 안내 - 카톡: OFF(변경불가), 디코: ON(변경불가) → 카톡만 변경 불가
        assertThat(AlimtalkTemplate.EARLY_LEAVE.isReadOnlyForStudent("alimtalk")).isTrue();
        assertThat(AlimtalkTemplate.EARLY_LEAVE.isReadOnlyForStudent("discord")).isFalse();
    }
    
    @Test
    public void testGetReadOnlyChannelsForStudent() {
        // 완전히 변경 불가능한 템플릿들
        Set<String> studyReminderReadOnly = AlimtalkTemplate.STUDY_REMINDER_10MIN.getReadOnlyChannelsForStudent();
        assertThat(studyReminderReadOnly).containsExactlyInAnyOrder("alimtalk", "discord");
        
        Set<String> noShowReadOnly = AlimtalkTemplate.NO_SHOW.getReadOnlyChannelsForStudent();
        assertThat(noShowReadOnly).containsExactlyInAnyOrder("alimtalk", "discord");
        
        // 부분적으로 변경 불가능한 템플릿들 (카톡만 변경 불가)
        Set<String> studyStartReadOnly = AlimtalkTemplate.STUDY_START.getReadOnlyChannelsForStudent();
        assertThat(studyStartReadOnly).containsExactly("alimtalk");
        
        Set<String> todayTasksReadOnly = AlimtalkTemplate.TODAY_TASKS.getReadOnlyChannelsForStudent();
        assertThat(todayTasksReadOnly).containsExactly("alimtalk");
    }
    
    @Test
    public void testTemplateDescriptions() {
        // 각 템플릿의 설명이 올바른지 확인
        assertThat(AlimtalkTemplate.STUDY_REMINDER_10MIN.getDescription()).isEqualTo("공부 시작 10분 전 알림");
        assertThat(AlimtalkTemplate.STUDY_START.getDescription()).isEqualTo("공부 시작 시간 알림");
        assertThat(AlimtalkTemplate.NO_SHOW.getDescription()).isEqualTo("미접속 알림");
        assertThat(AlimtalkTemplate.STUDY_ROOM_ENTER.getDescription()).isEqualTo("스터디룸 입장 알림");
        assertThat(AlimtalkTemplate.TODAY_TASKS.getDescription()).isEqualTo("오늘의 할일 알림 (아침)");
        assertThat(AlimtalkTemplate.YESTERDAY_INCOMPLETE_EVENING.getDescription()).isEqualTo("전날 미완료 할일 알림 (저녁)");
        assertThat(AlimtalkTemplate.LATE_ARRIVAL.getDescription()).isEqualTo("늦은입장 알림");
        assertThat(AlimtalkTemplate.EARLY_LEAVE.getDescription()).isEqualTo("조기퇴장 알림");
    }
    
    @Test 
    public void testNonExistentChannelReturnsFalse() {
        // 존재하지 않는 채널에 대해서는 false 반환
        assertThat(AlimtalkTemplate.STUDY_REMINDER_10MIN.isReadOnlyForStudent("email")).isFalse();
        assertThat(AlimtalkTemplate.STUDY_REMINDER_10MIN.isReadOnlyForStudent("sms")).isFalse();
        assertThat(AlimtalkTemplate.STUDY_REMINDER_10MIN.isReadOnlyForStudent("push")).isFalse();
    }
}
