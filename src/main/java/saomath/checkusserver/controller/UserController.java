package saomath.checkusserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import saomath.checkusserver.auth.CustomUserPrincipal;
import saomath.checkusserver.auth.dto.ApiResponse;
import saomath.checkusserver.auth.dto.UserInfoResponse;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    @Operation(summary = "내 상세 정보 조회", description = "현재 로그인한 사용자의 상세 정보 조회")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserProfile() {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserPrincipal)) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("인증되지 않은 사용자입니다."));
            }

            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            
            // TODO: UserService를 통해 상세 정보 조회 로직 구현 필요
            UserInfoResponse userInfo = new UserInfoResponse();
            userInfo.setId(userPrincipal.getId());
            userInfo.setUsername(userPrincipal.getUsername());
            userInfo.setName(userPrincipal.getName());
            // 추가 정보는 UserService에서 조회

            return ResponseEntity.ok(ApiResponse.success("사용자 정보를 조회했습니다.", userInfo));
            
        } catch (Exception e) {
            log.error("사용자 프로필 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "프로필 업데이트", description = "사용자 프로필 정보 업데이트")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<String>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserPrincipal)) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("인증되지 않은 사용자입니다."));
            }

            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            
            // TODO: UserService를 통해 프로필 업데이트 로직 구현 필요
            
            return ResponseEntity.ok(ApiResponse.success("프로필이 업데이트되었습니다.", "success"));
            
        } catch (Exception e) {
            log.error("프로필 업데이트 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
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
