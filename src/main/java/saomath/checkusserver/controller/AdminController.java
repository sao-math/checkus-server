package saomath.checkusserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import saomath.checkusserver.auth.dto.ResponseBase;
import saomath.checkusserver.auth.dto.UserRoleResponse;
import saomath.checkusserver.entity.UserRole;
import saomath.checkusserver.service.UserRoleService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 전용 API")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserRoleService userRoleService;

    @Operation(summary = "역할 승인 요청 목록 조회", description = "특정 역할의 승인 대기 중인 사용자 목록 조회")
    @GetMapping("/role-requests")
    public ResponseEntity<ResponseBase<List<UserRoleResponse>>> getRoleRequests(
            @Parameter(
                name = "roleName",
                description = "역할명 (STUDENT, TEACHER, GUARDIAN)",
                required = true,
                example = "STUDENT"
            )
            @RequestParam(name = "roleName", required = true) String roleName) {
        
        try {
            // 최적화된 DTO 직접 조회 사용
            List<UserRoleResponse> responseList = userRoleService.getPendingRoleRequestsOptimized(roleName);
            
            return ResponseEntity.ok(ResponseBase.success(
                    roleName + " 역할 승인 요청 목록을 조회했습니다.", 
                    responseList));
        } catch (Exception e) {
            log.error("역할 승인 요청 목록 조회 실패: {}", roleName, e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(summary = "역할 승인", description = "사용자의 역할 승인 (PENDING -> ACTIVE)")
    @PostMapping("/approve-role")
    public ResponseEntity<ResponseBase<String>> approveRole(
            @Parameter(
                name = "userId",
                description = "사용자 ID",
                required = true,
                example = "1"
            )
            @RequestParam(name = "userId", required = true) Long userId,
            @Parameter(
                name = "roleName",
                description = "역할명",
                required = true,
                example = "STUDENT"
            )
            @RequestParam(name = "roleName", required = true) String roleName) {
        
        try {
            userRoleService.approveRole(userId, roleName);
            return ResponseEntity.ok(ResponseBase.success(
                    "사용자 ID " + userId + "의 " + roleName + " 역할이 승인되었습니다.", 
                    "success"));
        } catch (Exception e) {
            log.error("역할 승인 실패: userId={}, roleName={}", userId, roleName, e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(summary = "역할 일시정지", description = "사용자의 역할 일시정지 (ACTIVE -> SUSPENDED)")
    @PostMapping("/suspend-role")
    public ResponseEntity<ResponseBase<String>> suspendRole(
            @Parameter(
                name = "userId",
                description = "사용자 ID",
                required = true,
                example = "1"
            )
            @RequestParam(name = "userId", required = true) Long userId,
            @Parameter(
                name = "roleName", 
                description = "역할명",
                required = true,
                example = "STUDENT"
            )
            @RequestParam(name = "roleName", required = true) String roleName) {
        
        try {
            userRoleService.suspendRole(userId, roleName);
            return ResponseEntity.ok(ResponseBase.success(
                    "사용자 ID " + userId + "의 " + roleName + " 역할이 일시정지되었습니다.", 
                    "success"));
        } catch (Exception e) {
            log.error("역할 일시정지 실패: userId={}, roleName={}", userId, roleName, e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }
    
    @Operation(summary = "역할 직접 할당", description = "관리자가 사용자에게 역할을 직접 할당 (바로 ACTIVE 상태)")
    @PostMapping("/assign-role")
    public ResponseEntity<ResponseBase<String>> assignRole(
            @Parameter(
                name = "userId",
                description = "사용자 ID",
                required = true,
                example = "1"
            )
            @RequestParam(name = "userId", required = true) Long userId,
            @Parameter(
                name = "roleName",
                description = "할당할 역할명 (STUDENT, TEACHER, GUARDIAN)",
                required = true,
                example = "STUDENT"
            )
            @RequestParam(name = "roleName", required = true) String roleName) {
        
        try {
            userRoleService.assignRoleDirectly(userId, roleName);
            return ResponseEntity.ok(ResponseBase.success(
                    "사용자 ID " + userId + "에게 " + roleName + " 역할을 할당했습니다.", 
                    "success"));
        } catch (Exception e) {
            log.error("역할 직접 할당 실패: userId={}, roleName={}", userId, roleName, e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(summary = "사용자 역할 조회", description = "특정 사용자의 모든 역할 조회")
    @GetMapping("/user-roles/{userId}")
    public ResponseEntity<ResponseBase<List<UserRoleResponse>>> getUserRoles(
            @Parameter(
                name = "userId",
                description = "사용자 ID",
                required = true,
                example = "1"
            )
            @PathVariable("userId") Long userId) {
        
        try {
            List<UserRole> userRoles = userRoleService.getAllUserRoles(userId);
            List<UserRoleResponse> responseList = userRoles.stream()
                    .map(UserRoleResponse::fromEntity)
                    .collect(Collectors.toList());
                    
            return ResponseEntity.ok(ResponseBase.success(
                    "사용자 ID " + userId + "의 역할 목록을 조회했습니다.", 
                    responseList));
        } catch (Exception e) {
            log.error("사용자 역할 조회 실패: userId={}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }
}
