package saomath.checkusserver.auth.dto;

import org.junit.jupiter.api.Test;
import saomath.checkusserver.auth.domain.UserRole;
import saomath.checkusserver.user.dto.UserRoleResponse;

import static org.assertj.core.api.Assertions.assertThat;

class UserRoleResponseTest {

    @Test
    void testConstructorWithoutStudentProfile() {
        // Given
        Long userId = 1L;
        String username = "teacher01";
        String name = "김선생";
        Long roleId = 2L;
        String roleName = "TEACHER";
        UserRole.RoleStatus status = UserRole.RoleStatus.PENDING;

        // When
        UserRoleResponse response = new UserRoleResponse(
            userId, username, name, roleId, roleName, status
        );

        // Then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo(username);
        assertThat(response.getName()).isEqualTo(name);
        assertThat(response.getRoleId()).isEqualTo(roleId);
        assertThat(response.getRoleName()).isEqualTo(roleName);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getStatusDescription()).isEqualTo("승인 대기");
        assertThat(response.getSchoolName()).isNull();
        assertThat(response.getGrade()).isNull();
    }

    @Test
    void testConstructorWithStudentProfile() {
        // Given
        Long userId = 2L;
        String username = "student01";
        String name = "김학생";
        Long roleId = 3L;
        String roleName = "STUDENT";
        UserRole.RoleStatus status = UserRole.RoleStatus.PENDING;
        String schoolName = "서울고등학교";
        Integer grade = 2;

        // When
        UserRoleResponse response = new UserRoleResponse(
            userId, username, name, roleId, roleName, status, schoolName, grade
        );

        // Then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo(username);
        assertThat(response.getName()).isEqualTo(name);
        assertThat(response.getRoleId()).isEqualTo(roleId);
        assertThat(response.getRoleName()).isEqualTo(roleName);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getStatusDescription()).isEqualTo("승인 대기");
        assertThat(response.getSchoolName()).isEqualTo(schoolName);
        assertThat(response.getGrade()).isEqualTo(grade);
    }

    @Test
    void testConstructorWithStudentProfileButNullValues() {
        // Given - 학생이지만 학교 정보가 없는 경우
        Long userId = 3L;
        String username = "student02";
        String name = "박학생";
        Long roleId = 3L;
        String roleName = "STUDENT";
        UserRole.RoleStatus status = UserRole.RoleStatus.PENDING;
        String schoolName = null;
        Integer grade = null;

        // When
        UserRoleResponse response = new UserRoleResponse(
            userId, username, name, roleId, roleName, status, schoolName, grade
        );

        // Then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo(username);
        assertThat(response.getName()).isEqualTo(name);
        assertThat(response.getRoleId()).isEqualTo(roleId);
        assertThat(response.getRoleName()).isEqualTo(roleName);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getStatusDescription()).isEqualTo("승인 대기");
        assertThat(response.getSchoolName()).isNull();
        assertThat(response.getGrade()).isNull();
    }
}
