package saomath.checkusserver.notification.service;

import net.infobank.client.InfobankClient;
import net.infobank.client.data.request.AlimtalkRequest;
import net.infobank.client.data.response.AlimtalkResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.notification.config.BizgoProperties;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlimtalkServiceImplTest {
    
    @Mock
    private InfobankClient infobankClient;
    
    @Mock
    private BizgoProperties bizgoProperties;
    
    @InjectMocks
    private AlimtalkServiceImpl alimtalkService;
    
    private static final String TEST_SENDER_KEY = "test-sender-key";
    private static final String TEST_PHONE_NUMBER = "01012345678";
    
    @BeforeEach
    void setUp() {
        when(bizgoProperties.getSenderKey()).thenReturn(TEST_SENDER_KEY);
    }
    
    @Test
    @DisplayName("알림톡 발송 성공 테스트")
    void sendAlimtalk_Success() throws Exception {
        // Given
        AlimtalkResponse successResponse = mock(AlimtalkResponse.class);
        when(successResponse.getCode()).thenReturn("0000");
        when(successResponse.getMsgKey()).thenReturn("test-msg-key");
        when(infobankClient.send(any(AlimtalkRequest.class))).thenReturn(successResponse);

        Map<String, String> variables = new HashMap<>();
        variables.put("studentName", "홍길동");
        variables.put("activityName", "수학");
        variables.put("startTime", "15:00");
        variables.put("endTime", "17:00");
        
        // When
        boolean result = alimtalkService.sendAlimtalk(
            TEST_PHONE_NUMBER, 
            AlimtalkTemplate.STUDY_REMINDER_10MIN, 
            variables
        );
        
        // Then
        assertThat(result).isTrue();
        
        ArgumentCaptor<AlimtalkRequest> captor = ArgumentCaptor.forClass(AlimtalkRequest.class);
        verify(infobankClient).send(captor.capture());
        
        AlimtalkRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.getTo()).isEqualTo(TEST_PHONE_NUMBER);
        assertThat(capturedRequest.getSenderKey()).isEqualTo(TEST_SENDER_KEY);
        assertThat(capturedRequest.getText()).contains("홍길동", "수학", "15:00", "17:00");
    }
    
    @Test
    @DisplayName("알림톡 발송 실패 테스트")
    void sendAlimtalk_Failure() throws Exception {
        // Given
        AlimtalkResponse failResponse = mock(AlimtalkResponse.class);
        when(failResponse.getCode()).thenReturn("E001");
        when(failResponse.getResult()).thenReturn("발송 실패");
        when(infobankClient.send(any(AlimtalkRequest.class))).thenReturn(failResponse);
        
        Map<String, String> variables = new HashMap<>();
        
        // When
        boolean result = alimtalkService.sendAlimtalk(
            TEST_PHONE_NUMBER, 
            AlimtalkTemplate.STUDY_START, 
            variables
        );
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("알림톡 발송 중 예외 발생 테스트")
    void sendAlimtalk_Exception() throws Exception {
        // Given
        when(infobankClient.send(any(AlimtalkRequest.class)))
            .thenThrow(new RuntimeException("네트워크 오류"));
        
        Map<String, String> variables = new HashMap<>();
        
        // When
        boolean result = alimtalkService.sendAlimtalk(
            TEST_PHONE_NUMBER, 
            AlimtalkTemplate.STUDY_START, 
            variables
        );
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("대량 알림톡 발송 테스트")
    void sendBulkAlimtalk() throws Exception {
        // Given
        String[] phoneNumbers = {"01012345678", "01087654321", "01011112222"};
        
        AlimtalkResponse successResponse = mock(AlimtalkResponse.class);
        when(successResponse.getCode()).thenReturn("0000");
        
        AlimtalkResponse failResponse = mock(AlimtalkResponse.class);
        when(failResponse.getCode()).thenReturn("E001");
        
        // 첫 번째와 세 번째는 성공, 두 번째는 실패
        when(infobankClient.send(any(AlimtalkRequest.class)))
            .thenReturn(successResponse)
            .thenReturn(failResponse)
            .thenReturn(successResponse);
        
        Map<String, String> variables = new HashMap<>();
        variables.put("studentName", "테스트");
        variables.put("taskCount", "5");
        variables.put("taskList", "과제 목록");
        
        // When
        int successCount = alimtalkService.sendBulkAlimtalk(
            phoneNumbers, 
            AlimtalkTemplate.TODAY_TASKS, 
            variables
        );
        
        // Then
        assertThat(successCount).isEqualTo(2);
        verify(infobankClient, times(3)).send(any(AlimtalkRequest.class));
    }
    
    @Test
    @DisplayName("템플릿 변수 치환 테스트")
    void replaceVariables() throws Exception {
        // Given
        Map<String, String> variables = new HashMap<>();
        variables.put("studentName", "김철수");
        variables.put("enterTime", "14:30");
        
        AlimtalkResponse successResponse = mock(AlimtalkResponse.class);
        when(successResponse.getCode()).thenReturn("0000");
        when(infobankClient.send(any(AlimtalkRequest.class))).thenReturn(successResponse);
        
        // When
        alimtalkService.sendAlimtalk(
            TEST_PHONE_NUMBER, 
            AlimtalkTemplate.STUDY_ROOM_ENTER, 
            variables
        );
        
        // Then
        ArgumentCaptor<AlimtalkRequest> captor = ArgumentCaptor.forClass(AlimtalkRequest.class);
        verify(infobankClient).send(captor.capture());
        
        String sentText = captor.getValue().getText();
        assertThat(sentText).contains("김철수");
        assertThat(sentText).contains("14:30");
        assertThat(sentText).doesNotContain("#{");
    }
}
