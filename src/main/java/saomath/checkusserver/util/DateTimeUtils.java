package saomath.checkusserver.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {
    
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final ZoneId UTC = ZoneId.of("UTC");
    // 로케일에 의존하지 않는 24시간 형식 사용
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm").withZone(KST);
    
    /**
     * UTC LocalDateTime을 한국 시간 문자열로 변환
     */
    public static String formatToKoreanTime(LocalDateTime utcDateTime) {
        if (utcDateTime == null) return "";
        
        ZonedDateTime utcZoned = utcDateTime.atZone(UTC);
        ZonedDateTime kstTime = utcZoned.withZoneSameInstant(KST);
        
        int hour = kstTime.getHour();
        int minute = kstTime.getMinute();
        
        // 12시간 형식으로 변환하고 오전/오후 직접 처리
        String amPm = hour < 12 ? "오전" : "오후";
        int displayHour = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour);
        
        return String.format("%s %d:%02d", amPm, displayHour, minute);
    }
    
    /**
     * 현재 UTC 시간 반환
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(UTC);
    }
}
