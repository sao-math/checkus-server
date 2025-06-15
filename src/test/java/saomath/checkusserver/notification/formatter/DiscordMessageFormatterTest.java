package saomath.checkusserver.notification.formatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import saomath.checkusserver.notification.dto.NotificationMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordMessageFormatterTest {

    private DiscordMessageFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new DiscordMessageFormatter();
    }

    @Test
    void testGetPlatformType() {
        assertThat(formatter.getPlatformType()).isEqualTo("DISCORD");
    }

    @Test
    void testSupportsMarkdown() {
        assertThat(formatter.supportsMarkdown()).isTrue();
    }

    @Test
    void testGetMaxMessageLength() {
        assertThat(formatter.getMaxMessageLength()).isEqualTo(2000);
    }

    @Test
    void testFormatUpcomingStudy() {
        // Given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 15, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 15, 17, 0);
        
        Map<String, Object> data = Map.of(
                "subject", "수학",
                "startTime", startTime,
                "endTime", endTime
        );

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.UPCOMING_STUDY)
                .data(data)
                .build();

        // When
        String result = formatter.format(notification);

        // Then
        assertThat(result)
                .contains("🔔 **공부 시작 10분 전 알림**")
                .contains("📚 **과목**: 수학")
                .contains("⏰ **시작 시간**: 15:00")
                .contains("⏱️ **종료 시간**: 17:00")
                .contains("곧 공부 시간입니다. 준비해 주세요! 💪");
    }

    @Test
    void testFormatStudyStart() {
        // Given
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 15, 17, 0);
        
        Map<String, Object> data = Map.of(
                "subject", "영어",
                "endTime", endTime
        );

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.STUDY_START)
                .data(data)
                .build();

        // When
        String result = formatter.format(notification);

        // Then
        assertThat(result)
                .contains("🚀 **공부 시작 시간입니다!**")
                .contains("📚 **과목**: 영어")
                .contains("⏱️ **종료 시간**: 17:00")
                .contains("지금 음성 채널에 입장해서 공부를 시작하세요! 🎯");
    }

    @Test
    void testFormatMissedStudy() {
        // Given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 15, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 15, 17, 0);
        
        Map<String, Object> data = Map.of(
                "subject", "과학",
                "startTime", startTime,
                "endTime", endTime
        );

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.MISSED_STUDY)
                .data(data)
                .build();

        // When
        String result = formatter.format(notification);

        // Then
        assertThat(result)
                .contains("⚠️ **미접속 알림**")
                .contains("📚 **과목**: 과학")
                .contains("⏰ **시작 시간**: 15:00 (10분 경과)")
                .contains("⏱️ **종료 시간**: 17:00")
                .contains("아직 공부를 시작하지 않으셨습니다.");
    }

    @Test
    void testFormatDailyTask() {
        // Given
        LocalDateTime time1Start = LocalDateTime.of(2024, 1, 15, 9, 0);
        LocalDateTime time1End = LocalDateTime.of(2024, 1, 15, 10, 0);
        LocalDateTime time2Start = LocalDateTime.of(2024, 1, 15, 14, 0);
        LocalDateTime time2End = LocalDateTime.of(2024, 1, 15, 16, 0);
        
        List<Map<String, Object>> studyTimes = List.of(
                Map.of("subject", "수학", "startTime", time1Start, "endTime", time1End),
                Map.of("subject", "영어", "startTime", time2Start, "endTime", time2End)
        );
        
        Map<String, Object> data = Map.of("studyTimes", studyTimes);

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.DAILY_TASK)
                .data(data)
                .build();

        // When
        String result = formatter.format(notification);

        // Then
        assertThat(result)
                .contains("🌅 **오늘의 공부 일정**")
                .contains("1. **수학**")
                .contains("⏰ 09:00 ~ 10:00")
                .contains("2. **영어**")
                .contains("⏰ 14:00 ~ 16:00")
                .contains("오늘도 화이팅! 💪✨");
    }

    @Test
    void testFormatEarlyLeave() {
        // Given
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 15, 17, 0);
        
        Map<String, Object> data = Map.of(
                "subject", "국어",
                "remainingMinutes", 30L,
                "endTime", endTime
        );

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.EARLY_LEAVE)
                .data(data)
                .build();

        // When
        String result = formatter.format(notification);

        // Then
        assertThat(result)
                .contains("⚠️ **조기 퇴장 감지**")
                .contains("📚 **과목**: 국어")
                .contains("⏱️ **남은 시간**: 30분")
                .contains("⏰ **종료 예정 시간**: 17:00")
                .contains("공부 시간이 아직 남아있습니다.");
    }

    @Test
    void testFormatLateArrival() {
        // Given
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 15, 17, 0);
        
        Map<String, Object> data = Map.of(
                "subject", "사회",
                "lateMinutes", 15L,
                "endTime", endTime
        );

        NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.LATE_ARRIVAL)
                .data(data)
                .build();

        // When
        String result = formatter.format(notification);

        // Then
        assertThat(result)
                .contains("⏰ **늦은 입장 안내**")
                .contains("📚 **과목**: 사회")
                .contains("🕐 **늦은 시간**: 15분")
                .contains("⏱️ **종료 시간**: 17:00")
                .contains("늦었지만 열심히 공부해 주세요!");
    }
} 