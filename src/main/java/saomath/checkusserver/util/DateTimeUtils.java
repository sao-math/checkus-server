package saomath.checkusserver.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {
    
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final DateTimeFormatter KST_FORMATTER = DateTimeFormatter.ofPattern("a h:mm").withZone(KST);
    
    /**
     * UTC LocalDateTime을 한국 시간 문자열로 변환
     */
    public static String formatToKoreanTime(LocalDateTime utcDateTime) {
        if (utcDateTime == null) return "";
        ZonedDateTime utcZoned = utcDateTime.atZone(UTC);
        return KST_FORMATTER.format(utcZoned);
    }
    
    /**
     * 현재 UTC 시간 반환
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(UTC);
    }
}
