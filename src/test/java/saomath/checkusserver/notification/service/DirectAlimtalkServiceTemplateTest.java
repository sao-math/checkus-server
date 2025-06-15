package saomath.checkusserver.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.notification.config.BizgoProperties;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("DirectAlimtalkService 템플릿 변수 치환 테스트")
class DirectAlimtalkServiceTemplateTest {
    
    private DirectAlimtalkService directAlimtalkService;
    private BizgoProperties bizgoProperties;
    
    @BeforeEach
    void setUp() {
        // Mock properties 설정
        bizgoProperties = new BizgoProperties();
        BizgoProperties.Api api = new BizgoProperties.Api();
        api.setBaseUrl("https://test-api.com");
        api.setClientId("test-client-id");
        api.setClientPassword("test-password");
        bizgoProperties.setApi(api);
        bizgoProperties.setSenderKey("test-sender-key");
        
        directAlimtalkService = new DirectAlimtalkService(bizgoProperties);
    }
    
    @Test
    @DisplayName("템플릿 변수 치환 - 이름 변수")
    void replaceVariables_Name() throws Exception {
        // Given
        String templateMessage = "[사오수학]\n#{이름} 학생\n스터디룸 입장이 확인되었습니다.";
        Map<String, String> variables = Map.of("이름", "김철수");
        
        // When - 리플렉션을 사용해 private 메서드 호출
        java.lang.reflect.Method method = DirectAlimtalkService.class
            .getDeclaredMethod("replaceVariables", String.class, Map.class);
        method.setAccessible(true);
        String result = (String) method.invoke(directAlimtalkService, templateMessage, variables);
        
        // Then
        assertThat(result).contains("김철수");
        assertThat(result).doesNotContain("#{이름}");
    }
    
    @Test
    @DisplayName("템플릿 변수 치환 - 여러 변수")
    void replaceVariables_MultipleVariables() throws Exception {
        // Given
        String templateMessage = "[사오수학]\n#{이름} 학생, \n곧 공부 시작할 시간이에요!\n#{시간}에 #{과목} 공부를 시작해주세요.";
        Map<String, String> variables = Map.of(
            "이름", "홍길동",
            "시간", "15:00",
            "과목", "수학"
        );
        
        // When
        java.lang.reflect.Method method = DirectAlimtalkService.class
            .getDeclaredMethod("replaceVariables", String.class, Map.class);
        method.setAccessible(true);
        String result = (String) method.invoke(directAlimtalkService, templateMessage, variables);
        
        // Then
        assertThat(result).contains("홍길동", "15:00", "수학");
        assertThat(result).doesNotContain("#{이름}", "#{시간}", "#{과목}");
    }
    
    @Test
    @DisplayName("템플릿 변수 치환 - 변수가 없는 경우")
    void replaceVariables_NoVariables() throws Exception {
        // Given
        String templateMessage = "[사오수학]\n공부 시간입니다!";
        Map<String, String> variables = Map.of();
        
        // When
        java.lang.reflect.Method method = DirectAlimtalkService.class
            .getDeclaredMethod("replaceVariables", String.class, Map.class);
        method.setAccessible(true);
        String result = (String) method.invoke(directAlimtalkService, templateMessage, variables);
        
        // Then
        assertThat(result).isEqualTo(templateMessage);
    }
    
    @Test
    @DisplayName("템플릿 변수 치환 - 일부 변수만 있는 경우")
    void replaceVariables_PartialVariables() throws Exception {
        // Given
        String templateMessage = "[사오수학]\n#{이름} 학생\n#{시간}에 공부 시작\n#{미존재} 변수는 그대로";
        Map<String, String> variables = Map.of(
            "이름", "이영희",
            "시간", "14:30"
        );
        
        // When
        java.lang.reflect.Method method = DirectAlimtalkService.class
            .getDeclaredMethod("replaceVariables", String.class, Map.class);
        method.setAccessible(true);
        String result = (String) method.invoke(directAlimtalkService, templateMessage, variables);
        
        // Then
        assertThat(result).contains("이영희", "14:30");
        assertThat(result).doesNotContain("#{이름}", "#{시간}");
        assertThat(result).contains("#{미존재}"); // 치환되지 않은 변수는 그대로 남음
    }
    
    @Test
    @DisplayName("템플릿 변수 치환 - STUDY_REMINDER_10MIN 템플릿")
    void replaceVariables_StudyReminder10MinTemplate() throws Exception {
        // Given
        AlimtalkTemplate template = AlimtalkTemplate.STUDY_REMINDER_10MIN;
        Map<String, String> variables = Map.of("이름", "박민수");
        
        // When
        java.lang.reflect.Method method = DirectAlimtalkService.class
            .getDeclaredMethod("replaceVariables", String.class, Map.class);
        method.setAccessible(true);
        String result = (String) method.invoke(directAlimtalkService, template.getTemplateMessage(), variables);
        
        // Then
        assertThat(result).contains("박민수");
        assertThat(result).doesNotContain("#{이름}");
        assertThat(result).contains("사오수학"); // 템플릿 기본 내용 확인
    }
}
