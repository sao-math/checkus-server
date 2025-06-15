package saomath.checkusserver.notification.formatter;

import org.springframework.stereotype.Component;
import saomath.checkusserver.notification.dto.NotificationMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 디스코드용 메시지 포맷터
 * 디스코드 마크다운 형식으로 메시지 변환
 */
@Component
public class DiscordMessageFormatter implements MessageFormatter {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM월 dd일 HH:mm");
    
    @Override
    public String getPlatformType() {
        return "DISCORD";
    }
    
    @Override
    public boolean supportsMarkdown() {
        return true;
    }
    
    @Override
    public int getMaxMessageLength() {
        return 2000;
    }
    
    @Override
    public String format(NotificationMessage notification) {
        return switch (notification.getType()) {
            case UPCOMING_STUDY -> formatUpcomingStudy(notification);
            case STUDY_START -> formatStudyStart(notification);
            case MISSED_STUDY -> formatMissedStudy(notification);
            case DAILY_TASK -> formatDailyTask(notification);
            case EARLY_LEAVE -> formatEarlyLeave(notification);
            case LATE_ARRIVAL -> formatLateArrival(notification);
        };
    }
    
    private String formatUpcomingStudy(NotificationMessage notification) {
        Map<String, Object> data = notification.getData();
        String subject = (String) data.get("subject");
        LocalDateTime startTime = (LocalDateTime) data.get("startTime");
        LocalDateTime endTime = (LocalDateTime) data.get("endTime");
        
        return String.format(
                "🔔 **공부 시작 10분 전 알림**\n\n" +
                "📚 **과목**: %s\n" +
                "⏰ **시작 시간**: %s\n" +
                "⏱️ **종료 시간**: %s\n\n" +
                "곧 공부 시간입니다. 준비해 주세요! 💪",
                subject,
                startTime.format(TIME_FORMATTER),
                endTime.format(TIME_FORMATTER)
        );
    }
    
    private String formatStudyStart(NotificationMessage notification) {
        Map<String, Object> data = notification.getData();
        String subject = (String) data.get("subject");
        LocalDateTime endTime = (LocalDateTime) data.get("endTime");
        
        return String.format(
                "🚀 **공부 시작 시간입니다!**\n\n" +
                "📚 **과목**: %s\n" +
                "⏱️ **종료 시간**: %s\n\n" +
                "지금 음성 채널에 입장해서 공부를 시작하세요! 🎯",
                subject,
                endTime.format(TIME_FORMATTER)
        );
    }
    
    private String formatMissedStudy(NotificationMessage notification) {
        Map<String, Object> data = notification.getData();
        String subject = (String) data.get("subject");
        LocalDateTime startTime = (LocalDateTime) data.get("startTime");
        LocalDateTime endTime = (LocalDateTime) data.get("endTime");
        
        return String.format(
                "⚠️ **미접속 알림**\n\n" +
                "📚 **과목**: %s\n" +
                "⏰ **시작 시간**: %s (10분 경과)\n" +
                "⏱️ **종료 시간**: %s\n\n" +
                "아직 공부를 시작하지 않으셨습니다.\n" +
                "지금이라도 음성 채널에 입장해 주세요! 🏃‍♂️",
                subject,
                startTime.format(TIME_FORMATTER),
                endTime.format(TIME_FORMATTER)
        );
    }
    
    @SuppressWarnings("unchecked")
    private String formatDailyTask(NotificationMessage notification) {
        Map<String, Object> data = notification.getData();
        List<Map<String, Object>> studyTimes = (List<Map<String, Object>>) data.get("studyTimes");
        
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("🌅 **오늘의 공부 일정**\n\n");
        
        for (int i = 0; i < studyTimes.size(); i++) {
            Map<String, Object> studyTime = studyTimes.get(i);
            String subject = (String) studyTime.get("subject");
            LocalDateTime startTime = (LocalDateTime) studyTime.get("startTime");
            LocalDateTime endTime = (LocalDateTime) studyTime.get("endTime");
            
            messageBuilder.append(String.format(
                    "%d. **%s**\n   ⏰ %s ~ %s\n\n",
                    i + 1,
                    subject,
                    startTime.format(TIME_FORMATTER),
                    endTime.format(TIME_FORMATTER)
            ));
        }
        
        messageBuilder.append("오늘도 화이팅! 💪✨");
        return messageBuilder.toString();
    }
    
    private String formatEarlyLeave(NotificationMessage notification) {
        Map<String, Object> data = notification.getData();
        String subject = (String) data.get("subject");
        Long remainingMinutes = (Long) data.get("remainingMinutes");
        LocalDateTime endTime = (LocalDateTime) data.get("endTime");
        
        return String.format(
                "⚠️ **조기 퇴장 감지**\n\n" +
                "📚 **과목**: %s\n" +
                "⏱️ **남은 시간**: %d분\n" +
                "⏰ **종료 예정 시간**: %s\n\n" +
                "공부 시간이 아직 남아있습니다.\n" +
                "다시 돌아와서 마저 공부해 주세요! 📖",
                subject,
                remainingMinutes,
                endTime.format(TIME_FORMATTER)
        );
    }
    
    private String formatLateArrival(NotificationMessage notification) {
        Map<String, Object> data = notification.getData();
        String subject = (String) data.get("subject");
        Long lateMinutes = (Long) data.get("lateMinutes");
        LocalDateTime endTime = (LocalDateTime) data.get("endTime");
        
        return String.format(
                "⏰ **늦은 입장 안내**\n\n" +
                "📚 **과목**: %s\n" +
                "🕐 **늦은 시간**: %d분\n" +
                "⏱️ **종료 시간**: %s\n\n" +
                "늦었지만 열심히 공부해 주세요!\n" +
                "다음부터는 시간을 지켜주세요 😊",
                subject,
                lateMinutes,
                endTime.format(TIME_FORMATTER)
        );
    }
} 