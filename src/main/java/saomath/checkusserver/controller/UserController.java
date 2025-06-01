package saomath.checkusserver.controller;

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
import saomath.checkusserver.auth.CustomUserPrincipal;
import saomath.checkusserver.auth.dto.ResponseBase;
import saomath.checkusserver.auth.dto.UserInfoResponse;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.service.UserRoleService;
import saomath.checkusserver.service.UserService;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;
    private final UserRoleService userRoleService;

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인한 사용자의 전체 정보를 조회합니다. JWT 토큰이 필요합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "사용자 정보 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "내 정보 조회 성공",
                                            value = """
                        {
                          "success": true,
                          "message": "사용자 정보 조회 성공",
                          "data": {
                            "id": 1,
                            "username": "admin",
                            "name": "김철수",
                            "phoneNumber": "010-1234-5678",
                            "discordId": "admin#1234",
                            "roles": ["ADMIN"],
                            "createdAt": "2024-01-01T00:00:00"
                          }
                        }
                        """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패 - 토큰이 없거나 유효하지 않음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "인증 실패",
                                            value = """
                        {
                          "success": false,
                          "message": "인증되지 않은 사용자입니다.",
                          "data": null
                        }
                        """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "사용자를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "사용자 없음",
                                            value = """
                        {
                          "success": false,
                          "message": "사용자를 찾을 수 없습니다.",
                          "data": null
                        }
                        """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResponseBase<UserInfoResponse>> getCurrentUser() {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.debug("Authentication: {}", authentication != null ? authentication.getClass().getSimpleName() : "null");

            if (authentication != null) {
                log.debug("Principal type: {}", authentication.getPrincipal().getClass().getSimpleName());
                log.debug("Principal: {}", authentication.getPrincipal());
                log.debug("Authorities: {}", authentication.getAuthorities());
            }

            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserPrincipal)) {
                log.warn("Authentication failed - authentication: {}, principal type: {}",
                        authentication != null ? "present" : "null",
                        authentication != null ? authentication.getPrincipal().getClass().getSimpleName() : "null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseBase.error("인증되지 않은 사용자입니다."));
            }

            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            log.debug("User principal: ID={}, Username={}", userPrincipal.getId(), userPrincipal.getUsername());

            // UserService를 통해 최신 사용자 정보 조회
            User user = userService.getUserById(userPrincipal.getId())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 사용자 역할 조회 (최신 정보)
            var roles = userRoleService.getActiveRoles(user.getId());

            // 모든 정보를 포함한 응답 생성
            UserInfoResponse userInfo = new UserInfoResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getName(),
                    user.getPhoneNumber(),    // ✅ 전화번호 포함
                    user.getDiscordId(),      // ✅ Discord ID 포함
                    roles,
                    user.getCreatedAt()       // ✅ 생성일 포함
            );

            log.info("사용자 정보 조회 성공: userId={}, username={}", user.getId(), user.getUsername());
            return ResponseEntity.ok(ResponseBase.success("사용자 정보 조회 성공", userInfo));

        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(summary = "프로필 업데이트", description = "사용자 프로필 정보 업데이트")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/profile")
    public ResponseEntity<ResponseBase<String>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserPrincipal)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseBase.error("인증되지 않은 사용자입니다."));
            }

            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();

            // TODO: UserService를 통해 프로필 업데이트 로직 구현 필요
            // userService.updateProfile(userPrincipal.getId(), request);

            return ResponseEntity.ok(ResponseBase.success("프로필이 업데이트되었습니다.", "success"));

        } catch (Exception e) {
            log.error("프로필 업데이트 실패", e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    // 프로필 업데이트 요청 DTO (임시)
    public static class UpdateProfileRequest {
        private String name;
        private String phoneNumber;
        private String discordId;

        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getDiscordId() { return discordId; }
        public void setDiscordId(String discordId) { this.discordId = discordId; }
    }
}