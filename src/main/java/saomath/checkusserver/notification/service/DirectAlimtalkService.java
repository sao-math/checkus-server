package saomath.checkusserver.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import saomath.checkusserver.notification.config.BizgoProperties;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectAlimtalkService implements AlimtalkService {
    
    private final BizgoProperties bizgoProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    
    // 토큰 캐시
    private volatile String cachedToken;
    private volatile LocalDateTime tokenExpireTime;
    private final Object tokenLock = new Object();
    
    public boolean sendAlimtalk(String phoneNumber, AlimtalkTemplate template, Map<String, String> variables) {
        try {
            // 전화번호 유효성 검사 및 하이픈 제거
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                log.warn("알림톡 발송 실패 - 전화번호가 비어있음");
                return false;
            }
            String cleanPhoneNumber = phoneNumber.replaceAll("[-\\s]", "");  // 하이픈과 공백 제거
            
            // 템플릿 메시지에 변수 치환
            String message = replaceVariables(template.getTemplateMessage(), variables);
            
            log.info("직접 HTTP 알림톡 발송 시작 - 수신자: {} -> {}, 템플릿: {}", phoneNumber, cleanPhoneNumber, template.name());
            log.info("SenderKey: {}", bizgoProperties.getSenderKey());
            log.info("TemplateCode: {}", template.getTemplateCode());
            log.info("Message: {}", message);
            
            // 요청 바디 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("senderKey", bizgoProperties.getSenderKey());
            requestBody.put("msgType", "AT");
            requestBody.put("to", cleanPhoneNumber);  // 정제된 전화번호 사용
            requestBody.put("templateCode", template.getTemplateCode());
            requestBody.put("text", message);
            
            // 버튼 정보 추가 (템플릿에 등록된 링크 버튼)
            if (template == AlimtalkTemplate.STUDY_REMINDER_10MIN || 
                template == AlimtalkTemplate.STUDY_START || 
                template == AlimtalkTemplate.NO_SHOW) {
                
                List<Map<String, Object>> buttons = new ArrayList<>();
                Map<String, Object> button = new HashMap<>();
                button.put("type", "WL");  // 웹 링크
                button.put("name", "스터디룸 입장");
                button.put("urlMobile", "https://discord.gg/dNzx8YB4re");
                button.put("urlPc", "");  // 빈 문자열로 설정
                buttons.add(button);
                
                requestBody.put("button", buttons);  // buttons가 아니라 button
            }
            
            // JSON으로 직렬화
            String requestJson = objectMapper.writeValueAsString(requestBody);
            log.info("Request JSON: {}", requestJson);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            headers.set("Authorization", "Bearer " + getAccessToken());
            
            // HTTP 요청 생성
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            
            // API 호출
            String url = bizgoProperties.getApi().getBaseUrl() + "/v1/send/alimtalk";
            log.info("API URL: {}", url);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            log.info("응답 상태: {}", response.getStatusCode());
            log.info("응답 바디: {}", response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK) {
                // JSON 응답 파싱
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                String code = responseJson.path("code").asText();
                log.info("알림톡 API 코드: {}", code);
                
                // 성공 코드 체크 (A000 또는 0000)
                if ("A000".equals(code) || "0000".equals(code)) {
                    log.info("직접 HTTP 알림톡 발송 성공 - 수신자: {} -> {}, 템플릿: {}", phoneNumber, cleanPhoneNumber, template.name());
                    return true;
                } else {
                    log.error("알림톡 발송 실패 - 코드: {}, 메시지: {}", code, responseJson.path("result").asText());
                    return false;
                }
            } else {
                log.error("직접 HTTP 알림톡 발송 실패 - 상태코드: {}, 응답: {}", 
                    response.getStatusCode(), response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            log.error("직접 HTTP 알림톡 발송 중 오류 발생 - 수신자: {}, 템플릿: {}", phoneNumber, template.name(), e);
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
    
    /**
     * 액세스 토큰 조회 (캐시 사용)
     */
    private String getAccessToken() throws Exception {
        // 토큰이 유효한지 확인
        if (cachedToken != null && tokenExpireTime != null && 
            LocalDateTime.now().isBefore(tokenExpireTime.minusMinutes(5))) {
            return cachedToken;
        }
        
        synchronized (tokenLock) {
            // 다시 한 번 확인 (다른 스레드에서 갱신했을 수 있음)
            if (cachedToken != null && tokenExpireTime != null && 
                LocalDateTime.now().isBefore(tokenExpireTime.minusMinutes(5))) {
                return cachedToken;
            }
            
            log.info("새로운 액세스 토큰 발급 요청");
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-IB-Client-Id", bizgoProperties.getApi().getClientId());
            headers.set("X-IB-Client-Passwd", bizgoProperties.getApi().getClientPassword());
            headers.set("Accept", "application/json");
            
            // HTTP 요청 생성
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            // API 호출
            String url = bizgoProperties.getApi().getBaseUrl() + "/v1/auth/token";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                log.info("인증 API 응답: {}", response.getBody());
                
                String code = responseJson.path("code").asText();
                log.info("인증 API 코드: {}", code);
                
                if ("A000".equals(code)) {  // 성공 코드가 A000임
                    // data 객체에서 token 필드 추출
                    JsonNode dataNode = responseJson.path("data");
                    String token = dataNode.path("token").asText();
                    
                    // 다른 가능한 필드명들도 확인
                    if (token.isEmpty()) {
                        token = dataNode.path("accessToken").asText();
                    }
                    if (token.isEmpty()) {
                        token = dataNode.path("access_token").asText();
                    }
                    
                    log.info("추출된 토큰: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
                    
                    if (!token.isEmpty()) {
                        cachedToken = token;
                        tokenExpireTime = LocalDateTime.now().plusHours(23);
                        
                        log.info("액세스 토큰 발급 성공");
                        return token;
                    } else {
                        throw new RuntimeException("토큰 발급 성공이지만 토큰 값을 찾을 수 없음. 응답: " + response.getBody());
                    }
                } else {
                    throw new RuntimeException("토큰 발급 실패 - 코드: " + code + ", 메시지: " + responseJson.path("result").asText());
                }
            } else {
                throw new RuntimeException("토큰 발급 API 호출 실패: " + response.getStatusCode());
            }
        }
    }
}
