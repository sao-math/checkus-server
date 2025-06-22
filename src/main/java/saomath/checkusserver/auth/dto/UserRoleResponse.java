package saomath.checkusserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import saomath.checkusserver.entity.UserRole;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 역할 응답 DTO")
public class UserRoleResponse {
    
    // JPQL 생성자 쿼리용 생성자 (상태 enum을 문자열로 변환)
    public UserRoleResponse(Long userId, String username, String name, 
                           Long roleId, String roleName, 
                           UserRole.RoleStatus status) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.roleId = roleId;
        this.roleName = roleName;
        this.status = status.name();
        this.statusDescription = status.getDescription();
    }
    
    // 학생 프로필 정보를 포함한 JPQL 생성자 쿼리용 생성자
    public UserRoleResponse(Long userId, String username, String name, 
                           Long roleId, String roleName, 
                           UserRole.RoleStatus status,
                           String schoolName, Integer grade) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.roleId = roleId;
        this.roleName = roleName;
        this.status = status.name();
        this.statusDescription = status.getDescription();
        this.schoolName = schoolName;
        this.grade = grade;
    }

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    @Schema(description = "사용자명", example = "student123")
    private String username;
    
    @Schema(description = "사용자 이름", example = "김철수")
    private String name;
    
    @Schema(description = "역할 ID", example = "2")
    private Long roleId;
    
    @Schema(description = "역할명", example = "STUDENT")
    private String roleName;
    
    @Schema(description = "역할 상태", example = "PENDING")
    private String status;
    
    @Schema(description = "역할 상태 설명", example = "승인 대기")
    private String statusDescription;
    
    @Schema(description = "학교명 (학생인 경우에만)", example = "서울고등학교")
    private String schoolName;
    
    @Schema(description = "학년 (학생인 경우에만)", example = "2")
    private Integer grade;



    //TODO 이거 여기 있어도 되나

    // UserRole 엔티티에서 DTO로 변환하는 정적 메서드
    public static UserRoleResponse fromEntity(UserRole userRole) {
        UserRoleResponse response = new UserRoleResponse();
        response.setUserId(userRole.getId().getUserId());
        response.setUsername(userRole.getUser().getUsername());
        response.setName(userRole.getUser().getName());
        response.setRoleId(userRole.getId().getRoleId());
        response.setRoleName(userRole.getRole().getName());
        response.setStatus(userRole.getStatus().name());
        response.setStatusDescription(userRole.getStatus().getDescription());
        // schoolName과 grade는 이 메서드에서는 설정하지 않음 (null로 유지)
        return response;
    }
}
