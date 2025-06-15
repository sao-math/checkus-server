package saomath.checkusserver.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import saomath.checkusserver.auth.dto.ResponseBase;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.exception.BusinessException;
import saomath.checkusserver.notification.dto.NotificationTestRequest;
import saomath.checkusserver.notification.dto.StudyRoomEnterNotificationRequest;
import saomath.checkusserver.notification.event.StudyRoomEnterEvent;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.service.DirectAlimtalkService;
import saomath.checkusserver.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "알림 관리 API")
public class NotificationController {
    
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final DirectAlimtalkService directAlimtalkService;
    
    @Operation(
        summary = "스터디룸 입장 알림 수동 발송",
        description = "선생님이 수동으로 스터디룸 입장 알림을 발송합니다.",
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
                          "message": "스터디룸 입장 알림이 발송되었습니다.",
                          "data": null
                        }
                        """
                    )
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "발송 실패",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "학생 없음",
                        value = """
                        {
                          "success": false,
                          "message": "학생을 찾을 수 없습니다.",
                          "data": null
                        }
                        """
                    )
                )
            )
        }
    )
    @PostMapping("/study-room/enter")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ResponseBase<Void>> sendStudyRoomEnterNotification(
            @Valid @RequestBody StudyRoomEnterNotificationRequest request) {
        
        try {
            User student = userRepository.findById(request.getStudentId())
                    .orElseThrow(() -> new BusinessException("학생을 찾을 수 없습니다."));
            
            // 스터디룸 입장 이벤트 발행
            StudyRoomEnterEvent event = StudyRoomEnterEvent.builder()
                    .studentId(student.getId())
                    .studentName(student.getName())
                    .discordId(student.getDiscordId())
                    .enterTime(LocalDateTime.now())
                    .channelName(request.getChannelName() != null ? 
                        request.getChannelName() : "스터디룸")
                    .build();
            
            eventPublisher.publishEvent(event);
            
            log.info("스터디룸 입장 알림 수동 발송 - 학생: {}, 채널: {}", 
                student.getName(), event.getChannelName());
            
            return ResponseEntity.ok(
                ResponseBase.success("스터디룸 입장 알림이 발송되었습니다.", null));
                
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        } catch (Exception e) {
            log.error("스터디룸 입장 알림 발송 실패", e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error("알림 발송 중 오류가 발생했습니다."));
        }
    }
    
    @Operation(
        summary = "알림 테스트 발송",
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
}
