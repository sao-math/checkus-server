package saomath.checkusserver.notification.service;

import net.infobank.client.InfobankClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("실제 API 호출 테스트 - 필요시에만 활성화")
class AlimtalkServiceIntegrationTest {
    
    @Autowired
    private AlimtalkService alimtalkService;
    
    @Autowired
    private InfobankClient infobankClient;
    
    @Test
    @DisplayName("실제 알림톡 발송 테스트")
    void sendRealAlimtalk() {
        // Given
        String phoneNumber = "01012345678"; // 실제 수신 가능한 번호로 변경
        
        Map<String, String> variables = new HashMap<>();
        variables.put("studentName", "테스트학생");
        variables.put("activityName", "수학");
        variables.put("startTime", "15:00");
        variables.put("endTime", "17:00");
        
        // When
        boolean result = alimtalkService.sendAlimtalk(
            phoneNumber,
            AlimtalkTemplate.STUDY_REMINDER_10MIN,
            variables
        );
        
        // Then
        assertThat(result).isTrue();
        
        // 실제 발송 후 잠시 대기 (선택사항)
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Test
    @DisplayName("실제 API 연결 테스트")
    void testApiConnection() throws Exception {
        // InfobankClient가 제대로 생성되고 API에 연결 가능한지 확인
        assertThat(infobankClient).isNotNull();
        
        // 토큰 발급 테스트 등을 수행할 수 있음
    }
}
