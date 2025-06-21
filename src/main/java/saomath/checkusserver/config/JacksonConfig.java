package saomath.checkusserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {
    
    // UTC 시간대 표시를 위한 포맷터 (Z 포함)
    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    // 역직렬화용 포맷터 (입력은 다양한 형식 허용)
    private static final DateTimeFormatter DESERIALIZER_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        JavaTimeModule module = new JavaTimeModule();
        
        // 역직렬화는 기존 형식 유지 (호환성)
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DESERIALIZER_FORMATTER));
        
        // 직렬화는 UTC 표시자 Z 포함
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(UTC_FORMATTER));
        
        return new ObjectMapper()
                .registerModule(module)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setTimeZone(TimeZone.getTimeZone("UTC"));
    }
}
