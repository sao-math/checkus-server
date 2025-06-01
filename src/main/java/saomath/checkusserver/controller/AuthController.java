package saomath.checkusserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
import saomath.checkusserver.auth.ValidationUtils;
import saomath.checkusserver.auth.dto.*;
import saomath.checkusserver.service.UserRoleService;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;
    private final UserRoleService userRoleService;

    @Operation(
        summary = "학생 회원가입", 
        description = "학생 전용 회원가입 엔드포인트. 관리자 승인이 필요합니다.",
        responses = {
            @ApiResponse(
                responseCode = "201", 
                description = "회원가입 성공",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "학생 회원가입 성공",
                        value = """
                        {
                          "success": true,
                          "message": "학생 회원가입이 완료되었습니다.",
                          "data": {
                            "userId": 2,
                            "username": "student123",
                            "message": "학생 회원가입이 완료되었습니다. 관리자 승인을 기다려주세요."
                          }
                        }
                        """
                    )
                )
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "입력 오류 또는 중복 데이터",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "사용자명 중복 오류",
                        value = """
                        {
                          "success": false,
                          "message": "이미 사용 중인 사용자명입니다: student123",
                          "data": null
                        }
                        """
                    )
                )
            )
        }
    )
    @PostMapping("/register/student")
    public ResponseEntity<ResponseBase<RegisterResponse>> registerStudent(
            @Valid @RequestBody StudentRegisterRequest request) {
        
        try {
            RegisterResponse response = authService.registerStudent(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseBase.success("학생 회원가입이 완료되었습니다.", response));
        } catch (Exception e) {
            log.error("학생 회원가입 실패", e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(summary = "학부모 회원가입", description = "학부모 전용 회원가입 엔드포인트")
    @PostMapping("/register/guardian")
    public ResponseEntity<ResponseBase<RegisterResponse>> registerGuardian(
            @Valid @RequestBody GuardianRegisterRequest request) {
        
        try {
            RegisterResponse response = authService.registerGuardian(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseBase.success("학부모 회원가입이 완료되었습니다.", response));
        } catch (Exception e) {
            log.error("학부모 회원가입 실패", e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(summary = "교사 회원가입", description = "교사 전용 회원가입 엔드포인트")
    @PostMapping("/register/teacher")
    public ResponseEntity<ResponseBase<RegisterResponse>> registerTeacher(
            @Valid @RequestBody TeacherRegisterRequest request) {
        
        try {
            RegisterResponse response = authService.registerTeacher(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseBase.success("교사 회원가입이 완료되었습니다.", response));
        } catch (Exception e) {
            log.error("교사 회원가입 실패", e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "로그인", 
        description = "사용자명과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "로그인 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResponseBase.class),
                    examples = @ExampleObject(
                        name = "로그인 성공 예시",
                        value = """
                        {
                          "success": true,
                          "message": "로그인이 완료되었습니다.",
                          "data": {
                            "userId": 1,
                            "username": "admin",
                            "name": "김철수",
                            "roles": ["ADMIN"],
                            "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
                            "refreshToken": "eyJhbGciOiJIUzM4NCJ9...",
                            "tokenType": "Bearer"
                          }
                        }
                        """
                    )
                )
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "로그인 실패 - 잘못된 인증 정보",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "로그인 실패 예시",
                        value = """
                        {
                          "success": false,
                          "message": "로그인에 실패했습니다. 사용자명과 비밀번호를 확인해주세요.",
                          "data": null
                        }
                        """
                    )
                )
            )
        }
    )
    @PostMapping("/login")
public ResponseEntity<ResponseBase<LoginResponseSecure>> login(
    @Valid @RequestBody LoginRequest request,
    HttpServletResponse response) {
    
    log.debug("로그인 요청 수신: username={}", request.getUsername ());
    
    try {
    LoginResponse loginResponse = authService.login(request);
    
    log.debug("로그인 성공: username={}, userId={}", request.getUsername(), loginResponse.getUserId());
    
    // RefreshToken을 HttpOnly 쿠키로 설정 (SameSite 포함)
    String refreshTokenCookieValue = String.format(
        "refreshToken=%s; HttpOnly; Secure; Path=/; Max-Age=%d; SameSite=Strict",
        loginResponse.getRefreshToken(),
        7 * 24 * 60 * 60
    );
    response.addHeader("Set-Cookie", refreshTokenCookieValue);
    
    // AccessToken만 포함된 응답 생성
    LoginResponseSecure secureResponse = new LoginResponseSecure(
    loginResponse.getUserId(),
    loginResponse.getUsername(),
        loginResponse.getName(),
        loginResponse.getRoles(),
        loginResponse.getAccessToken(),
            "Bearer"
    );
    
    return ResponseEntity.ok(ResponseBase.success("로그인이 완료되었습니다.", secureResponse));
    } catch (Exception e) {
        log.error("로그인 실패: username={}, error={}", request.getUsername(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseBase.error(e.getMessage()));
    }
}


    @Operation(
        summary = "토큰 리프레시", 
        description = "HttpOnly 쿠키의 리프레시 토큰으로 새로운 액세스 토큰을 발급받습니다.",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "토큰 리프레시 성공",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "토큰 리프레시 성공",
                        value = """
                        {
                          "success": true,
                          "message": "토큰이 갱신되었습니다.",
                          "data": {
                            "accessToken": "eyJhbGciOiJIUzM4NCJ9..."
                          }
                        }
                        """
                    )
                )
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "리프레시 토큰이 유효하지 않음",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "리프레시 토큰 오류",
                        value = """
                        {
                          "success": false,
                          "message": "유효하지 않은 리프레시 토큰입니다.",
                          "data": null
                        }
                        """
                    )
                )
            )
        }
    )
    @PostMapping("/refresh")
    public ResponseEntity<ResponseBase<TokenRefreshResponseSecure>> refreshToken(
            @CookieValue(value = "refreshToken", required = true) String refreshToken,
            HttpServletResponse response) {
        
        try {
            // 기존 TokenRefreshRequest 객체 생성
            TokenRefreshRequest request = new TokenRefreshRequest();
            request.setRefreshToken(refreshToken);
            
            TokenRefreshResponse tokenResponse = authService.refreshToken(request);
            
            // 새로운 RefreshToken을 쿠키로 업데이트 (SameSite 포함)
            String newRefreshTokenCookieValue = String.format(
                "refreshToken=%s; HttpOnly; Secure; Path=/; Max-Age=%d; SameSite=Strict",
                tokenResponse.getRefreshToken(),
                7 * 24 * 60 * 60
            );
            response.addHeader("Set-Cookie", newRefreshTokenCookieValue);
            
            // AccessToken만 포함된 응답
            TokenRefreshResponseSecure secureResponse = new TokenRefreshResponseSecure(
                tokenResponse.getAccessToken()
            );
            
            return ResponseEntity.ok(ResponseBase.success("토큰이 갱신되었습니다.", secureResponse));
        } catch (Exception e) {
            log.error("토큰 리프레시 실패", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(summary = "로그아웃", description = "로그아웃 및 리프레시 토큰 폐기 (HttpOnly 쿠키 제거)")
    @PostMapping("/logout")
    public ResponseEntity<ResponseBase<String>> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        
        try {
            // RefreshToken이 있으면 서버에서 폐기
            if (refreshToken != null) {
                authService.logout(refreshToken);
            }
            
            // RefreshToken 쿠키 제거 (SameSite 포함)
            String expiredCookieValue = "refreshToken=; HttpOnly; Secure; Path=/; Max-Age=0; SameSite=Strict";
            response.addHeader("Set-Cookie", expiredCookieValue);
            
            return ResponseEntity.ok(ResponseBase.success("로그아웃이 완료되었습니다.", "success"));
        } catch (Exception e) {
            log.error("로그아웃 실패", e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "내 정보 조회", 
        description = "현재 로그인한 사용자의 기본 정보를 조회합니다. JWT 토큰이 필요합니다.",
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
                          "message": null,
                          "data": {
                            "id": 1,
                            "username": "admin",
                            "name": "김철수",
                            "phoneNumber": null,
                            "discordId": null,
                            "roles": ["ADMIN"],
                            "createdAt": null
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

            return ResponseEntity.ok(ResponseBase.success(userInfo));
            
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "사용자명 중복 확인", 
        description = "회원가입 전 사용자명 중복 여부를 확인합니다.",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "사용자명 사용 가능",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "사용 가능한 사용자명",
                        value = """
                        {
                          "success": true,
                          "message": "사용 가능한 사용자명입니다.",
                          "data": true
                        }
                        """
                    )
                )
            ),
            @ApiResponse(
                responseCode = "200", 
                description = "사용자명 이미 사용 중",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "이미 사용 중인 사용자명",
                        value = """
                        {
                          "success": true,
                          "message": "이미 사용 중인 사용자명입니다.",
                          "data": false
                        }
                        """
                    )
                )
            )
        }
    )
    @GetMapping("/check-username")
    public ResponseEntity<ResponseBase<Boolean>> checkUsername(
            @Parameter(description = "확인할 사용자명") 
            @RequestParam("username") String username) {
        
        try {
            // 사용자명 형식 검증
            if (!ValidationUtils.isValidUsername(username)) {
                return ResponseEntity.badRequest()
                        .body(ResponseBase.error(ValidationUtils.getUsernameRequirements()));
            }
            
            boolean available = !authService.isUsernameExists(username);
            String message = available ? "사용 가능한 사용자명입니다." : "이미 사용 중인 사용자명입니다.";
            
            return ResponseEntity.ok(ResponseBase.success(message, available));
            
        } catch (Exception e) {
            log.error("사용자명 중복 확인 실패: {}", username, e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(summary = "전화번호 중복 확인", description = "회원가입 전 전화번호 중복 여부 확인")
    @GetMapping("/check-phone")
    public ResponseEntity<ResponseBase<Boolean>> checkPhoneNumber(
            @Parameter(description = "확인할 전화번호") 
            @RequestParam("phoneNumber") String phoneNumber) {
        
        try {
            // 전화번호 형식 검증
            if (!ValidationUtils.isValidPhoneNumber(phoneNumber)) {
                return ResponseEntity.badRequest()
                        .body(ResponseBase.error(ValidationUtils.getPhoneNumberRequirements()));
            }
            
            boolean available = !authService.isPhoneNumberExists(phoneNumber);
            String message = available ? "사용 가능한 전화번호입니다." : "이미 등록된 전화번호입니다.";
            
            return ResponseEntity.ok(ResponseBase.success(message, available));
            
        } catch (Exception e) {
            log.error("전화번호 중복 확인 실패: {}", phoneNumber, e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }
}
