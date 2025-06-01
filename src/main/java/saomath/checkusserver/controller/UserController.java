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
import saomath.checkusserver.auth.dto.StudentProfileResponse;
import saomath.checkusserver.auth.dto.UserInfoResponse;
import saomath.checkusserver.entity.RoleConstants;
import saomath.checkusserver.entity.StudentProfile;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.repository.StudentProfileRepository;
import saomath.checkusserver.service.UserRoleService;
import saomath.checkusserver.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;
    private final UserRoleService userRoleService;
    private final StudentProfileRepository studentProfileRepository;

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인한 사용자의 전체 정보를 조회합니다. 학생인 경우 학생 프로필 정보도 포함됩니다. JWT 토큰이 필요합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "사용자 정보 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "일반 사용자 (교사/관리자/학부모)",
                                                    value = """
                                {
                                  "success": true,
                                  "message": "사용자 정보 조회 성공",
                                  "data": {
                                    "id": 1,
                                    "username": "teacher1",
                                    "name": "김선생",
                                    "phoneNumber": "010-1111-1111",
                                    "discordId": "teacher1#1234",
                                    "roles": ["TEACHER"],
                                    "createdAt": "2024-01-01T00:00:00",
                                    "studentProfile": null
                                  }
                                }
                                """
                                            ),
                                            @ExampleObject(
                                                    name = "학생 사용자",
                                                    value = """
                                {
                                  "success": true,
                                  "message": "사용자 정보 조회 성공",
                                  "data": {
                                    "id": 4,
                                    "username": "student1",
                                    "name": "박학생",
                                    "phoneNumber": "010-2222-1111",
                                    "discordId": "student1#1234",
                                    "roles": ["STUDENT"],
                                    "createdAt": "2024-01-01T00:00:00",
                                    "studentProfile": {
                                      "status": "ENROLLED",
                                      "school": {
                                        "id": 1,
                                        "name": "이현중"
                                      },
                                      "grade": 2,
                                      "gender": "MALE"
                                    }
                                  }
                                }
                                """
                                            )
                                    }
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
            List<String> roles = userRoleService.getActiveRoles(user.getId());

            // 기본 사용자 정보로 응답 생성
            UserInfoResponse userInfo = new UserInfoResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getName(),
                    user.getPhoneNumber(),
                    user.getDiscordId(),
                    roles,
                    user.getCreatedAt()
            );

            // 학생인 경우 StudentProfile 정보 추가
            if (roles.contains(RoleConstants.STUDENT)) {
                studentProfileRepository.findByUserId(user.getId())
                        .ifPresent(studentProfile -> {
                            StudentProfileResponse.SchoolResponse schoolResponse = new StudentProfileResponse.SchoolResponse(
                                    studentProfile.getSchool().getId(),
                                    studentProfile.getSchool().getName()
                            );

                            StudentProfileResponse profileResponse = new StudentProfileResponse(
                                    studentProfile.getStatus(),
                                    schoolResponse,
                                    studentProfile.getGrade(),
                                    studentProfile.getGender()
                            );

                            userInfo.setStudentProfile(profileResponse);
                            log.debug("학생 프로필 정보 추가: userId={}, school={}, grade={}",
                                    user.getId(), studentProfile.getSchool().getName(), studentProfile.getGrade());
                        });
            }

            log.info("사용자 정보 조회 성공: userId={}, username={}, roles={}, hasStudentProfile={}",
                    user.getId(), user.getUsername(), roles, userInfo.getStudentProfile() != null);

            return ResponseEntity.ok(ResponseBase.success("사용자 정보 조회 성공", userInfo));

        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error(e.getMessage()));
        }
    }
}