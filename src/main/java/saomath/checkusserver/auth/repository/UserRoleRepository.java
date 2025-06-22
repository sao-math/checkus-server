package saomath.checkusserver.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saomath.checkusserver.auth.domain.UserRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {
    
    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.user JOIN FETCH ur.role WHERE ur.user.id = :userId")
    List<UserRole> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.user JOIN FETCH ur.role WHERE ur.user.id = :userId AND ur.role.name = :roleName")
    Optional<UserRole> findByUserIdAndRoleName(@Param("userId") Long userId, @Param("roleName") String roleName);
    
    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.user JOIN FETCH ur.role WHERE ur.user.id = :userId AND ur.status = :status")
    List<UserRole> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") UserRole.RoleStatus status);
    
    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.user JOIN FETCH ur.role WHERE ur.role.name = :roleName AND ur.status = :status")
    List<UserRole> findByRoleNameAndStatus(@Param("roleName") String roleName, @Param("status") UserRole.RoleStatus status);
    
    // 최적화된 DTO 직접 조회 쿼리 (학생 프로필 정보 포함)
    @Query("""
        SELECT new saomath.checkusserver.auth.dto.UserRoleResponse(
            ur.id.userId, u.username, u.name, 
            ur.id.roleId, r.name, ur.status,
            s.name, sp.grade
        )
        FROM UserRole ur 
        JOIN ur.user u 
        JOIN ur.role r 
        LEFT JOIN StudentProfile sp ON sp.userId = u.id
        LEFT JOIN sp.school s
        WHERE r.name = :roleName AND ur.status = :status
        """)
    List<saomath.checkusserver.auth.dto.UserRoleResponse> findUserRoleResponsesByRoleNameAndStatus(
        @Param("roleName") String roleName, 
        @Param("status") UserRole.RoleStatus status
    );
}
