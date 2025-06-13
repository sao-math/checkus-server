package saomath.checkusserver.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import saomath.checkusserver.dto.ApiResponse;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.notification.event.StudyRoomEnterEvent;
import saomath.checkusserver.repository.UserRepository;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    
    @PostMapping("/study-room/enter")
    @Operation(summary = "스터디룸 입장 알림 전송", description = "수동으로 스터디룸 입장 알림을 전송합니다.")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendStudyRoomEnterNotification(
            @Parameter(description = "학생 ID") @RequestParam Long studentId,
            @Parameter(description = "채널명") @RequestParam(required = false, defaultValue = "스터디룸") String channelName) {
        
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다: " + studentId));
        
        // 스터디룸 입장 이벤트 발행
        StudyRoomEnterEvent event = StudyRoomEnterEvent.builder()
                .studentId(student.getId())
                .studentName(student.getName())
                .discordId(student.getDiscordId())
                .enterTime(LocalDateTime.now())
                .channelName(channelName)
                .build();
        
        eventPublisher.publishEvent(event);
        
        log.info("스터디룸 입장 알림 수동 발송 - 학생: {}, 채널: {}", student.getName(), channelName);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    @PostMapping("/test/{templateId}")
    @Operation(summary = "알림 테스트 전송", description = "특정 템플릿으로 테스트 알림을 전송합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendTestNotification(
            @Parameter(description = "템플릿 ID") @PathVariable String templateId,
            @Parameter(description = "수신자 전화번호") @RequestParam String phoneNumber) {
        
        // TODO: 테스트 알림 발송 구현
        log.info("테스트 알림 발송 - 템플릿: {}, 수신자: {}", templateId, phoneNumber);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
