package saomath.checkusserver.notification.service;

import net.infobank.client.InfobankClient;
import net.infobank.client.data.request.AlimtalkRequest;
import net.infobank.client.data.response.AlimtalkResponse;
import net.infobank.client.data.code.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import saomath.checkusserver.notification.config.BizgoProperties;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Slf4j
//@Service
@RequiredArgsConstructor
public class AlimtalkServiceImpl implements AlimtalkService {
    
    private final InfobankClient infobankClient;
    private final BizgoProperties bizgoProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public boolean sendAlimtalk(String phoneNumber, AlimtalkTemplate template, Map<String, String> variables) {
        try {
            // 템플릿 메시지에 변수 치환
            String message = replaceVariables(template.getTemplateMessage(), variables);
            
            log.info("알림톡 발송 시작 - 수신자: {}, 템플릿: {}", phoneNumber, template.name());
            log.info("SenderKey: {}", bizgoProperties.getSenderKey());
            log.info("TemplateCode: {}", template.getTemplateCode());
            log.info("Message: {}", message);
            log.info("MessageType.AT value: {}", MessageType.AT);
            log.info("MessageType.AT toString: {}", MessageType.AT.toString());
            
            // 다른 MessageType enum 값들 확인
            try {
                for (MessageType type : MessageType.values()) {
                    log.info("Available MessageType: {} = {}", type.name(), type.toString());
                }
            } catch (Exception e) {
                log.warn("MessageType enum 값 확인 실패", e);
            }
            
            // 알림톡 요청 생성
            AlimtalkRequest request = AlimtalkRequest.builder()
                    .to(phoneNumber)
                    .senderKey(bizgoProperties.getSenderKey())
                    .templateCode(template.getTemplateCode())
                    .text(message)
                    .msgType(MessageType.AT)
                    .build();
            
            // 요청 객체를 JSON으로 직렬화해서 로깅
            try {
                String requestJson = objectMapper.writeValueAsString(request);
                log.info("AlimtalkRequest JSON: {}", requestJson);
            } catch (Exception e) {
                log.warn("AlimtalkRequest JSON 직렬화 실패", e);
            }
            
            // 알림톡 발송
            log.info("InfobankClient.send() 호출 시작");
            AlimtalkResponse response = infobankClient.send(request);
            log.info("InfobankClient.send() 호출 완료");
            
            if ("0000".equals(response.getCode())) {
                log.info("알림톡 발송 성공 - 수신자: {}, 템플릿: {}, msgKey: {}", 
                    phoneNumber, template.name(), response.getMsgKey());
                return true;
            } else {
                log.error("알림톡 발송 실패 - 수신자: {}, 템플릿: {}, 응답코드: {}, 메시지: {}", 
                    phoneNumber, template.name(), response.getCode(), response.getResult());
                return false;
            }
            
        } catch (Exception e) {
            log.error("알림톡 발송 중 오류 발생 - 수신자: {}, 템플릿: {}", phoneNumber, template.name(), e);
            return false;
        }
    }
    
    @Override
    public int sendBulkAlimtalk(String[] phoneNumbers, AlimtalkTemplate template, Map<String, String> variables) {
        int successCount = 0;
        
        for (String phoneNumber : phoneNumbers) {
            if (sendAlimtalk(phoneNumber, template, variables)) {
                successCount++;
            }
            
            // Rate limit 방지를 위한 짧은 지연
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        log.info("알림톡 대량 발송 완료 - 템플릿: {}, 전체: {}건, 성공: {}건", 
            template.name(), phoneNumbers.length, successCount);
        
        return successCount;
    }
    
    /**
     * 템플릿 메시지의 변수를 실제 값으로 치환
     */
    private String replaceVariables(String template, Map<String, String> variables) {
        String result = template;
        
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "#{" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }
        
        return result;
    }
}
