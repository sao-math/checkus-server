package saomath.checkusserver.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.common.exception.BusinessException;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.domain.DeliveryMethod;
import saomath.checkusserver.notification.dto.NotificationSendRequest;
import saomath.checkusserver.notification.dto.NotificationSendResponse;
import saomath.checkusserver.notification.dto.NotificationTemplateDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSendService {

    private final UserRepository userRepository;
    private final DirectAlimtalkService directAlimtalkService;
    private final DiscordNotificationService discordNotificationService;

    /**
     * 직접 알림 발송
     */
    public NotificationSendResponse sendNotification(NotificationSendRequest request) {
        // 1. 학생 정보 조회
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new BusinessException("학생을 찾을 수 없습니다."));

        // 2. 발송 방법에 따른 수신자 정보 검증
        String recipient = validateAndGetRecipient(student, request.getDeliveryMethod());

        // 3. 템플릿 사용 여부에 따라 다른 발송 로직 사용
        String sentMessage;
        if (hasCustomMessage(request)) {
            // 자유 메시지 발송
            sentMessage = request.getCustomMessage();
            String result = sendCustomMessage(request.getDeliveryMethod(), recipient, sentMessage);
            if (result != null) {
                throw new BusinessException(result);
            }
        } else {
            // 템플릿 메시지 발송
            Map<String, String> variables = createVariablesFromStudent(student);
            sentMessage = createTemplateMessage(request.getTemplateId(), student);
            String result = sendTemplateMessage(request.getDeliveryMethod(), recipient, request.getTemplateId(), variables);
            if (result != null) {
                throw new BusinessException(result);
            }
        }

        // 4. 응답 생성
        return NotificationSendResponse.builder()
                .deliveryMethod(request.getDeliveryMethod())
                .recipient(recipient)
                .sentMessage(sentMessage)
                .templateUsed(request.getTemplateId())
                .build();
    }

    /**
     * 사용 가능한 템플릿 목록 조회 (D1, D2, D3만)
     */
    public List<NotificationTemplateDto> getAvailableTemplates() {
        // D1, D2, D3에 해당하는 템플릿만 필터링
        List<AlimtalkTemplate> allowedTemplates = Arrays.asList(
            AlimtalkTemplate.STUDY_REMINDER_10MIN,  // D0001 (D1)
            AlimtalkTemplate.STUDY_START,           // D0002 (D2)
            AlimtalkTemplate.NO_SHOW               // D0003 (D3)
        );
        
        return allowedTemplates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 수신자 정보 검증 및 조회
     */
    private String validateAndGetRecipient(User student, String deliveryMethodValue) {
        DeliveryMethod deliveryMethod = DeliveryMethod.fromValue(deliveryMethodValue);
        if (deliveryMethod == null) {
            throw new BusinessException("지원하지 않는 발송 방법입니다.");
        }
        
        switch (deliveryMethod) {
            case ALIMTALK:
                if (student.getPhoneNumber() == null || student.getPhoneNumber().trim().isEmpty()) {
                    throw new BusinessException("학생의 전화번호가 등록되지 않았습니다.");
                }
                return student.getPhoneNumber();
            case DISCORD:
                if (student.getDiscordId() == null || student.getDiscordId().trim().isEmpty()) {
                    throw new BusinessException("학생의 디스코드 ID가 등록되지 않았습니다.");
                }
                return student.getDiscordId();
            default:
                throw new BusinessException("지원하지 않는 발송 방법입니다.");
        }
    }
    
    private boolean hasCustomMessage(NotificationSendRequest request) {
        return request.getCustomMessage() != null && !request.getCustomMessage().trim().isEmpty();
    }

    /**
     * 템플릿 메시지 생성 (변수 자동 치환)
     */
    private String createTemplateMessage(String templateId, User student) {
        try {
            AlimtalkTemplate template = AlimtalkTemplate.valueOf(templateId);
            String message = template.getTemplateMessage();

            // 학생 정보로 변수 생성
            Map<String, String> variables = createVariablesFromStudent(student);

            // 변수 치환
            return replaceVariables(message, variables);

        } catch (IllegalArgumentException e) {
            throw new BusinessException("지원하지 않는 템플릿 ID입니다: " + templateId);
        }
    }

    /**
     * 학생 정보로부터 템플릿 변수 생성
     */
    private Map<String, String> createVariablesFromStudent(User student) {
        Map<String, String> variables = new HashMap<>();
        
        // 기본 정보
        variables.put("이름", student.getName() != null ? student.getName() : "학생");
        
        // 현재 시간 정보
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        variables.put("입장시간", now.format(timeFormatter));
        
        // 기본 과제 정보 (실제로는 DB에서 조회해야 함)
        variables.put("1", "오늘의 학습 계획이 있습니다.\n자세한 내용은 앱에서 확인해주세요.");
        variables.put("2", "미완료 과제가 있습니다.\n앱에서 확인 후 완료해주세요.");
        
        // 늦은 시간 (예시)
        variables.put("늦은시간", "5");
        
        return variables;
    }

    /**
     * 변수 치환
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
     * 자유 메시지 발송
     * @return 에러 메시지 (성공시 null, 실패시 구체적인 에러 메시지)
     */
    private String sendCustomMessage(String deliveryMethodValue, String recipient, String customMessage) {
        try {
            DeliveryMethod deliveryMethod = DeliveryMethod.fromValue(deliveryMethodValue);
            if (deliveryMethod == null) {
                String errorMsg = "지원하지 않는 발송 방법입니다: " + deliveryMethodValue;
                log.error(errorMsg);
                return errorMsg;
            }
            
            boolean success = false;
            String errorDetail = "";
            
            switch (deliveryMethod) {
                case ALIMTALK:
                    // 카카오톡은 자유 메시지 지원하지 않음
                    return "카카오톡 알림톡은 템플릿을 사용해야 합니다.";
                    
                case DISCORD:
                    try {
                        // 디스코드 자유 메시지 발송
                        success = discordNotificationService.sendNotification(recipient, "CUSTOM", Map.of("message", customMessage)).join();
                        if (!success) {
                            errorDetail = "디스코드 메시지 발송에 실패했습니다. 디스코드 ID를 확인해주세요.";
                        }
                    } catch (Exception e) {
                        log.error("디스코드 발송 중 오류: ", e);
                        return "디스코드 발송 중 오류가 발생했습니다: " + e.getMessage();
                    }
                    break;
                    
                default:
                    String errorMsg = "처리할 수 없는 발송 방법입니다: " + deliveryMethod;
                    log.error(errorMsg);
                    return errorMsg;
            }
            
            return success ? null : errorDetail;
            
        } catch (Exception e) {
            String errorMsg = "메시지 발송 중 시스템 오류가 발생했습니다: " + e.getMessage();
            log.error("메시지 발송 중 오류 발생", e);
            return errorMsg;
        }
    }

    /**
     * 템플릿 메시지 발송
     * @return 에러 메시지 (성공시 null, 실패시 구체적인 에러 메시지)
     */
    private String sendTemplateMessage(String deliveryMethodValue, String recipient, String templateId, Map<String, String> variables) {
        try {
            DeliveryMethod deliveryMethod = DeliveryMethod.fromValue(deliveryMethodValue);
            if (deliveryMethod == null) {
                String errorMsg = "지원하지 않는 발송 방법입니다: " + deliveryMethodValue;
                log.error(errorMsg);
                return errorMsg;
            }
            
            boolean success = false;
            String errorDetail = "";
            
            switch (deliveryMethod) {
                case ALIMTALK:
                    try {
                        // 카카오톡 템플릿 발송
                        AlimtalkTemplate template = AlimtalkTemplate.valueOf(templateId);
                        success = directAlimtalkService.sendAlimtalk(recipient, template, variables);
                        if (!success) {
                            errorDetail = "카카오톡 알림톡 발송에 실패했습니다. 전화번호를 확인해주세요.";
                        }
                    } catch (IllegalArgumentException e) {
                        return "지원하지 않는 템플릿 ID입니다: " + templateId;
                    }
                    break;
                    
                case DISCORD:
                    try {
                        // 디스코드 템플릿 발송
                        success = discordNotificationService.sendNotification(recipient, templateId, variables).join();
                        if (!success) {
                            errorDetail = "디스코드 메시지 발송에 실패했습니다. 디스코드 ID를 확인해주세요.";
                        }
                    } catch (Exception e) {
                        log.error("디스코드 발송 중 오류: ", e);
                        return "디스코드 발송 중 오류가 발생했습니다: " + e.getMessage();
                    }
                    break;
                    
                default:
                    String errorMsg = "처리할 수 없는 발송 방법입니다: " + deliveryMethod;
                    log.error(errorMsg);
                    return errorMsg;
            }
            
            return success ? null : errorDetail;
            
        } catch (Exception e) {
            String errorMsg = "메시지 발송 중 시스템 오류가 발생했습니다: " + e.getMessage();
            log.error("메시지 발송 중 오류 발생", e);
            return errorMsg;
        }
    }

    /**
     * AlimtalkTemplate을 DTO로 변환
     */
    private NotificationTemplateDto convertToDto(AlimtalkTemplate template) {
        return NotificationTemplateDto.builder()
                .id(template.name())
                .name(template.getDescription())
                .description(template.getDescription())
                .previewMessage(template.getTemplateMessage())
                .requiredVariables(extractRequiredVariables(template.getTemplateMessage()))
                .build();
    }

    /**
     * 템플릿에서 필요한 변수 목록 추출
     */
    private List<String> extractRequiredVariables(String templateMessage) {
        Pattern pattern = Pattern.compile("#\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(templateMessage);
        
        return matcher.results()
                .map(result -> result.group(1))
                .distinct()
                .collect(Collectors.toList());
    }
}
