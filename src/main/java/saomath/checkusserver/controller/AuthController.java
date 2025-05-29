package saomath.checkusserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import saomath.checkusserver.auth.AuthService;
import saomath.checkusserver.auth.CustomUserPrincipal;
import saomath.checkusserver.auth.DomainUtils;
import saomath.checkusserver.auth.ValidationUtils;
import saomath.checkusserver.auth.dto.*;
import saomath.checkusserver.exception.BusinessException;
import saomath.checkusserver.service.UserRoleService;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;
    private final UserRoleService userRoleService;

    @Operation(summary = "회원가입", description = "도메인에 따라 학생/학부모/교사 회원가입을 처리합니다.")
    @PostMapping("/register")
    public ResponseEntity<saomath.checkusserver.auth.dto.ApiResponse<RegisterResponse>> register(
            @RequestBody Object registerRequestJson,
            HttpServletRequest request) {
        
        try {
            String userType = DomainUtils.getUserTypeFromRequest(request);
            log.info("회원가입 요청 - 도메인 타입: {}", userType);
            
            RegisterResponse response;
            
            // JSON을 적절한 DTO로 변환하는 로직은 프론트엔드에서 적절한 엔드포인트를 호출하도록 권장
            // 여기서는 각 타입별 전용 엔드포인트 사용을 권장하는 메시지 반환
            return ResponseEntity.badRequest()
                    .body(saomath.checkusserver.auth.dto.ApiResponse.error(
                        "회원가입은 타입별 전용 엔드포인트를 사용해주세요. /register/student, /register/guardian, /register/teacher"));
                    
        } catch (Exception e) {
            log.error("회원가입 실패", e);
            return ResponseEntity.badRequest()
                    .body(saomath.checkusserver.auth.dto.ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "학생 회원가입", description = "학생 전용 회원가입 엔드포인트")
    @PostMapping("/register/student")
    public ResponseEntity<ApiResponse<RegisterResponse>> registerStudent(
            @Valid @RequestBody StudentRegisterRequest request) {
        
        try {
            RegisterResponse response = authService.registerStudent(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("학생 회원가입이 완료되었습니다.", response));
        } catch (Exception e) {
            log.error("학생 회원가입 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "학부모 회원가입", description = "학부모 전용 회원가입 엔드포인트")
    @PostMapping("/register/guardian")
    public ResponseEntity<ApiResponse<RegisterResponse>> registerGuardian(
            @Valid @RequestBody GuardianRegisterRequest request) {
        
        try {
            RegisterResponse response = authService.registerGuardian(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("학부모 회원가입이 완료되었습니다.", response));
        } catch (Exception e) {
            log.error("학부모 회원가입 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "교사 회원가입", description = "교사 전용 회원가입 엔드포인트")
    @PostMapping("/register/teacher")
    public ResponseEntity<ApiResponse<RegisterResponse>> registerTeacher(
            @Valid @RequestBody TeacherRegisterRequest request) {
        
        try {
            RegisterResponse response = authService.registerTeacher(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("교사 회원가입이 완료되었습니다.", response));
        } catch (Exception e) {
            log.error("교사 회원가입 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "로그인", description = "사용자명과 비밀번호로 로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다.", response));
        } catch (Exception e) {
            log.error("로그인 실패: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "토큰 리프레시", description = "리프레시 토큰으로 새로운 액세스 토큰 발급")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        
        try {
            TokenRefreshResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(ApiResponse.success("토큰이 갱신되었습니다.", response));
        } catch (Exception e) {
            log.error("토큰 리프레시 실패", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "로그아웃", description = "로그아웃 및 리프레시 토큰 폐기")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestBody(required = false) LogoutRequest request) {
        
        try {
            String refreshToken = request != null ? request.getRefreshToken() : null;
            authService.logout(refreshToken);
            return ResponseEntity.ok(ApiResponse.success("로그아웃이 완료되었습니다.", "success"));
        } catch (Exception e) {
            log.error("로그아웃 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보 조회")
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser() {
        
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
                        .body(ApiResponse.error("인증되지 않은 사용자입니다."));
            }

            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            log.debug("User principal: ID={}, Username={}", userPrincipal.getId(), userPrincipal.getUsername());
            
            // 사용자 역할 조회
            var roles = userRoleService.getActiveRoles(userPrincipal.getId());

            UserInfoResponse userInfo = new UserInfoResponse(
                    userPrincipal.getId(),
                    userPrincipal.getUsername(),
                    userPrincipal.getName(),
                    null, // 보안상 전화번호는 별도 API에서 제공
                    null, // Discord ID도 별도 API에서 제공
                    roles,
                    null  // 생성일은 별도 API에서 제공
            );

            return ResponseEntity.ok(ApiResponse.success(userInfo));
            
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "사용자명 중복 확인", description = "회원가입 전 사용자명 중복 여부 확인")
    @GetMapping("/check-username")
    public ResponseEntity<saomath.checkusserver.auth.dto.ApiResponse<Boolean>> checkUsername(
            @Parameter(description = "확인할 사용자명") 
            @RequestParam("username") String username) {
        
        try {
            // 사용자명 형식 검증
            if (!ValidationUtils.isValidUsername(username)) {
                return ResponseEntity.badRequest()
                        .body(saomath.checkusserver.auth.dto.ApiResponse.error(ValidationUtils.getUsernameRequirements()));
            }
            
            boolean available = !authService.isUsernameExists(username);
            String message = available ? "사용 가능한 사용자명입니다." : "이미 사용 중인 사용자명입니다.";
            
            return ResponseEntity.ok(saomath.checkusserver.auth.dto.ApiResponse.success(message, available));
            
        } catch (Exception e) {
            log.error("사용자명 중복 확인 실패: {}", username, e);
            return ResponseEntity.badRequest()
                    .body(saomath.checkusserver.auth.dto.ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "전화번호 중복 확인", description = "회원가입 전 전화번호 중복 여부 확인")
    @GetMapping("/check-phone")
    public ResponseEntity<saomath.checkusserver.auth.dto.ApiResponse<Boolean>> checkPhoneNumber(
            @Parameter(description = "확인할 전화번호") 
            @RequestParam("phoneNumber") String phoneNumber) {
        
        try {
            // 전화번호 형식 검증
            if (!ValidationUtils.isValidPhoneNumber(phoneNumber)) {
                return ResponseEntity.badRequest()
                        .body(saomath.checkusserver.auth.dto.ApiResponse.error(ValidationUtils.getPhoneNumberRequirements()));
            }
            
            boolean available = !authService.isPhoneNumberExists(phoneNumber);
            String message = available ? "사용 가능한 전화번호입니다." : "이미 등록된 전화번호입니다.";
            
            return ResponseEntity.ok(saomath.checkusserver.auth.dto.ApiResponse.success(message, available));
            
        } catch (Exception e) {
            log.error("전화번호 중복 확인 실패: {}", phoneNumber, e);
            return ResponseEntity.badRequest()
                    .body(saomath.checkusserver.auth.dto.ApiResponse.error(e.getMessage()));
        }
    }
}
