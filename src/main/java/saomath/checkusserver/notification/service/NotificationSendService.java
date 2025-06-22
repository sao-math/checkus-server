package saomath.checkusserver.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.common.exception.BusinessException;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
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

        // 3. 메시지 생성
        String message = createMessage(request, student);

        // 4. 실제 발송
        boolean success = sendMessage(request.getDeliveryMethod(), recipient, message, request.getTemplateId());

        if (!success) {
            throw new BusinessException("알림 발송에 실패했습니다.");
        }

        // 5. 응답 생성
        return NotificationSendResponse.builder()
                .deliveryMethod(request.getDeliveryMethod())
                .recipient(recipient)
                .sentMessage(message)
                .templateUsed(request.getTemplateId())
                .build();
    }

    /**
     * 사용 가능한 템플릿 목록 조회
     */
    public List<NotificationTemplateDto> getAvailableTemplates() {
        return Arrays.stream(AlimtalkTemplate.values())
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 수신자 정보 검증 및 조회
     */
    private String validateAndGetRecipient(User student, String deliveryMethod) {
        if ("alimtalk".equalsIgnoreCase(deliveryMethod)) {
            if (student.getPhoneNumber() == null || student.getPhoneNumber().trim().isEmpty()) {
                throw new BusinessException("학생의 전화번호가 등록되지 않았습니다.");
            }
            return student.getPhoneNumber();
        } else if ("discord".equalsIgnoreCase(deliveryMethod)) {
            if (student.getDiscordId() == null || student.getDiscordId().trim().isEmpty()) {
                throw new BusinessException("학생의 디스코드 ID가 등록되지 않았습니다.");
            }
            return student.getDiscordId();
        } else {
            throw new BusinessException("지원하지 않는 발송 방법입니다.");
        }
    }

    /**
     * 메시지 생성 (템플릿 또는 자유 메시지)
     */
    private String createMessage(NotificationSendRequest request, User student) {
        if (request.hasCustomMessage()) {
            // 자유 메시지 사용
            return request.getCustomMessage();
        } else {
            // 템플릿 사용
            return createTemplateMessage(request.getTemplateId(), student);
        }
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
     * 실제 메시지 발송
     */
    private boolean sendMessage(String deliveryMethod, String recipient, String message, String templateId) {
        try {
            if ("alimtalk".equalsIgnoreCase(deliveryMethod)) {
                // 카카오톡 발송
                if (templateId != null) {
                    // 템플릿 사용시
                    AlimtalkTemplate template = AlimtalkTemplate.valueOf(templateId);
                    Map<String, String> variables = extractVariablesFromMessage(message, template.getTemplateMessage());
                    return directAlimtalkService.sendAlimtalk(recipient, template, variables);
                } else {
                    // 이론상 여기는 도달하지 않음 (카카오톡은 자유 메시지 불가)
                    return false;
                }
            } else if ("discord".equalsIgnoreCase(deliveryMethod)) {
                // 디스코드 발송
                if (templateId != null) {
                    return discordNotificationService.sendNotification(recipient, templateId, new HashMap<>()).join();
                } else {
                    // 자유 메시지 - 임시로 CUSTOM 템플릿 ID 사용
                    return discordNotificationService.sendNotification(recipient, "CUSTOM", Map.of("message", message)).join();
                }
            }
            
            return false;
        } catch (Exception e) {
            log.error("메시지 발송 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 메시지에서 변수 추출 (역변환)
     */
    private Map<String, String> extractVariablesFromMessage(String actualMessage, String templateMessage) {
        Map<String, String> variables = new HashMap<>();
        
        // 간단한 구현: 실제로는 더 정교한 파싱이 필요할 수 있음
        Pattern pattern = Pattern.compile("#\\{([^}]+)\\}");
        Matcher templateMatcher = pattern.matcher(templateMessage);
        
        while (templateMatcher.find()) {
            String varName = templateMatcher.group(1);
            // 실제 메시지에서 해당 위치의 값을 찾아서 추출
            // 여기서는 간단히 기본값 사용
            variables.put(varName, "추출된값");
        }
        
        return variables;
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
