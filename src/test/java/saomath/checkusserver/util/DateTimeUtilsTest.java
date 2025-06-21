package saomath.checkusserver.util;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class DateTimeUtilsTest {
    
    @Test
    void formatToKoreanTime_오전시간_정상변환() {
        // Given: UTC 00:40 (한국시간 09:40)
        LocalDateTime utcTime = LocalDateTime.of(2025, 6, 21, 0, 40);
        
        // When
        String result = DateTimeUtils.formatToKoreanTime(utcTime);
        
        // Then
        assertThat(result).isEqualTo("오전 9:40");
    }
    
    @Test
    void formatToKoreanTime_오후시간_정상변환() {
        // Given: UTC 09:40 (한국시간 18:40)
        LocalDateTime utcTime = LocalDateTime.of(2025, 6, 21, 9, 40);
        
        // When
        String result = DateTimeUtils.formatToKoreanTime(utcTime);
        
        // Then
        assertThat(result).isEqualTo("오후 6:40");
    }
    
    @Test
    void formatToKoreanTime_자정_정상변환() {
        // Given: UTC 15:00 (한국시간 00:00)
        LocalDateTime utcTime = LocalDateTime.of(2025, 6, 21, 15, 0);
        
        // When
        String result = DateTimeUtils.formatToKoreanTime(utcTime);
        
        // Then
        assertThat(result).isEqualTo("오전 12:00");
    }
    
    @Test
    void formatToKoreanTime_null입력시_빈문자열반환() {
        // When
        String result = DateTimeUtils.formatToKoreanTime(null);
        
        // Then
        assertThat(result).isEmpty();
    }
}
