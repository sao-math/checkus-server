package saomath.checkusserver.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import saomath.checkusserver.auth.CustomUserPrincipal;
import saomath.checkusserver.auth.dto.ResponseBase;
import saomath.checkusserver.common.exception.BusinessException;
import saomath.checkusserver.notification.dto.*;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.service.DirectAlimtalkService;
import saomath.checkusserver.notification.service.NotificationPreferenceService;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "알림 관리 API")
public class NotificationController {

    private final DirectAlimtalkService directAlimtalkService;
    private final NotificationPreferenceService notificationPreferenceService;

    @Operation(
        summary = "직접 알림 발송",
        description = "관리자가 특정 템플릿으로 테스트 알림을 발송합니다.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "테스트 알림 발송 성공",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "발송 성공",
                        value = """
                        {
                          "success": true,
                          "message": "테스트 알림이 발송되었습니다.",
                          "data": null
                        }
                        """
                    )
                )
            )
        }
    )
    @PostMapping("/test")
    public ResponseEntity<ResponseBase<Void>> sendTestNotification(
            @Valid @RequestBody NotificationTestRequest request) {

        try {
            // 템플릿 ID를 AlimtalkTemplate enum으로 변환
            AlimtalkTemplate template;
            try {
                template = AlimtalkTemplate.valueOf(request.getTemplateId());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ResponseBase.error("지원하지 않는 템플릿 ID입니다: " + request.getTemplateId()));
            }

            // 테스트용 변수 설정
            Map<String, String> variables = new HashMap<>();
            variables.put("이름", "테스트학생");
            variables.put("1", "수학 문제집 10페이지\n영어 단어 암기");
            variables.put("2", "과학 실험 보고서");
            variables.put("입장시간", "15:30");

            // 알림톡 발송
            boolean success = directAlimtalkService.sendAlimtalk(
                request.getPhoneNumber(),
                template,
                variables
            );

            if (success) {
                log.info("테스트 알림 발송 성공 - 템플릿: {}, 수신자: {}",
                    request.getTemplateId(), request.getPhoneNumber());
                return ResponseEntity.ok(
                    ResponseBase.success("테스트 알림이 발송되었습니다.", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ResponseBase.error("알림 발송에 실패했습니다."));
            }

        } catch (Exception e) {
            log.error("테스트 알림 발송 실패", e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error("테스트 알림 발송 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "사용자별 그룹화된 알림 설정 조회",
        description = "각 알림 유형별로 카카오톡과 디스코드 설정을 분리하여 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/settings/grouped")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('GUARDIAN')")
    public ResponseEntity<ResponseBase<List<NotificationSettingGroupDto>>> getGroupedNotificationSettings() {
        try {
            Long userId = getCurrentUserId();
            
            List<NotificationSettingGroupDto> groupedSettings = notificationPreferenceService.getGroupedNotificationSettings(userId);
            
            return ResponseEntity.ok(ResponseBase.success("그룹화된 알림 설정 조회 성공", groupedSettings));
        } catch (Exception e) {
            log.error("그룹화된 알림 설정 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseBase.error("그룹화된 알림 설정 조회에 실패했습니다."));
        }
    }

    @Operation(
        summary = "특정 알림 유형의 특정 채널 설정 업데이트",
        description = "알림 유형별로 카카오톡과 디스코드를 독립적으로 설정할 수 있습니다."
    )
    @PutMapping("/settings/grouped/{notificationTypeId}/{deliveryMethod}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('GUARDIAN')")
    public ResponseEntity<ResponseBase<String>> updateNotificationSettingGroup(
            @PathVariable("notificationTypeId") String notificationTypeId,
            @PathVariable("deliveryMethod") String deliveryMethod,
            @RequestBody NotificationSettingUpdateDto updateDto) {
        try {
            Long userId = getCurrentUserId();
            
            notificationPreferenceService.updateNotificationSetting(userId, notificationTypeId, deliveryMethod, updateDto);
            
            return ResponseEntity.ok(ResponseBase.success("알림 설정이 업데이트되었습니다."));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                .body(ResponseBase.error(e.getMessage()));
        } catch (Exception e) {
            log.error("알림 설정 업데이트 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseBase.error("알림 설정 업데이트에 실패했습니다."));
        }
    }

    // Helper methods
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Authentication: {}", authentication != null ? authentication.getClass().getSimpleName() : "null");
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserPrincipal)) {
            log.warn("Authentication failed - authentication: {}, principal type: {}",
                    authentication != null ? "present" : "null",
                    authentication != null ? authentication.getPrincipal().getClass().getSimpleName() : "null");
            throw new BusinessException("Authentication failed");
        }
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
        //todo refactor...
    }
}
