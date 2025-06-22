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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import saomath.checkusserver.auth.domain.CustomUserPrincipal;
import saomath.checkusserver.auth.dto.ResponseBase;
import saomath.checkusserver.common.exception.BusinessException;
import saomath.checkusserver.notification.dto.*;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.service.DirectAlimtalkService;
import saomath.checkusserver.notification.service.NotificationPreferenceService;
import saomath.checkusserver.notification.service.NotificationSendService;

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
    private final NotificationSendService notificationSendService;

    @Operation(
        summary = "직접 알림 발송",
        description = "선생님이나 관리자가 특정 학생에게 직접 알림을 발송합니다.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "알림 발송 성공",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "발송 성공",
                        value = """
                        {
                          "success": true,
                          "message": "알림이 성공적으로 발송되었습니다.",
                          "data": {
                            "deliveryMethod": "alimtalk",
                            "recipient": "01012345678",
                            "sentMessage": "[사오수학]\n홍길동 학생, \n곧 공부 시작할 시간이에요!",
                            "templateUsed": "STUDY_REMINDER_10MIN"
                          }
                        }
                        """
                    )
                )
            )
        }
    )
    @PostMapping("/send")
    public ResponseEntity<ResponseBase<NotificationSendResponse>> sendNotification(
            @Valid @RequestBody NotificationSendRequest request) {

        try {
            NotificationSendResponse response = notificationSendService.sendNotification(request);
            
            log.info("알림 발송 성공 - 학생ID: {}, 방법: {}, 템플릿: {}", 
                    request.getStudentId(), request.getDeliveryMethod(), request.getTemplateId());
            
            return ResponseEntity.ok(
                ResponseBase.success("알림이 성공적으로 발송되었습니다.", response));
                
        } catch (BusinessException e) {
            log.warn("알림 발송 실패 - 비즈니스 오류: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        } catch (Exception e) {
            log.error("알림 발송 실패 - 시스템 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error("알림 발송 중 오류가 발생했습니다."));
        }
    }

    @Operation(
        summary = "사용 가능한 템플릿 목록 조회",
        description = "알림 발송에 사용할 수 있는 템플릿 목록을 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/templates")
    public ResponseEntity<ResponseBase<List<NotificationTemplateDto>>> getNotificationTemplates() {
        try {
            List<NotificationTemplateDto> templates = notificationSendService.getAvailableTemplates();
            return ResponseEntity.ok(ResponseBase.success("템플릿 목록 조회 성공", templates));
        } catch (Exception e) {
            log.error("템플릿 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseBase.error("템플릿 목록 조회에 실패했습니다."));
        }
    }

    @Operation(
        summary = "사용자별 그룹화된 알림 설정 조회",
        description = "각 알림 유형별로 카카오톡과 디스코드 설정을 분리하여 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/settings/grouped")
    @SecurityRequirement(name = "bearerAuth")
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
