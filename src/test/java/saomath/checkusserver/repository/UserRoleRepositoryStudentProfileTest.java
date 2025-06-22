package saomath.checkusserver.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import saomath.checkusserver.auth.domain.Role;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.domain.UserRole;
import saomath.checkusserver.auth.dto.UserRoleResponse;
import saomath.checkusserver.auth.repository.UserRoleRepository;
import saomath.checkusserver.entity.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRoleRepositoryStudentProfileTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    void testFindUserRoleResponsesByRoleNameAndStatus_WithStudentProfile() {
        // Given
        // 1. 학교 생성
        School school = School.builder()
                .name("서울고등학교")
                .build();
        entityManager.persist(school);

        // 2. 역할 생성
        Role studentRole = new Role();
        studentRole.setName("STUDENT");
        entityManager.persist(studentRole);

        // 3. 사용자 생성
        User student = User.builder()
                .username("student01")
                .name("김학생")
                .password("password")
                .build();
        entityManager.persist(student);

        // 4. 학생 프로필 생성
        StudentProfile studentProfile = StudentProfile.builder()
                .userId(student.getId())
                .user(student)
                .school(school)
                .grade(2)
                .status(StudentProfile.StudentStatus.ENROLLED)
                .gender(StudentProfile.Gender.MALE)
                .build();
        entityManager.persist(studentProfile);

        // 5. 사용자 역할 생성 (PENDING 상태)
        UserRole userRole = new UserRole();
        UserRole.UserRoleId userRoleId = new UserRole.UserRoleId();
        userRoleId.setUserId(student.getId());
        userRoleId.setRoleId(studentRole.getId());
        userRole.setId(userRoleId);
        userRole.setUser(student);
        userRole.setRole(studentRole);
        userRole.setStatus(UserRole.RoleStatus.PENDING);
        entityManager.persist(userRole);

        entityManager.flush();
        entityManager.clear();

        // When
        List<UserRoleResponse> responses = userRoleRepository
                .findUserRoleResponsesByRoleNameAndStatus("STUDENT", UserRole.RoleStatus.PENDING);

        // Then
        assertThat(responses).hasSize(1);
        UserRoleResponse response = responses.get(0);
        assertThat(response.getUserId()).isEqualTo(student.getId());
        assertThat(response.getUsername()).isEqualTo("student01");
        assertThat(response.getName()).isEqualTo("김학생");
        assertThat(response.getRoleName()).isEqualTo("STUDENT");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getSchoolName()).isEqualTo("서울고등학교");
        assertThat(response.getGrade()).isEqualTo(2);
    }

    @Test
    void testFindUserRoleResponsesByRoleNameAndStatus_WithoutStudentProfile() {
        // Given
        // 1. 역할 생성
        Role teacherRole = new Role();
        teacherRole.setName("TEACHER");
        entityManager.persist(teacherRole);

        // 2. 사용자 생성 (교사)
        User teacher = User.builder()
                .username("teacher01")
                .name("김선생")
                .password("password")
                .build();
        entityManager.persist(teacher);

        // 3. 사용자 역할 생성 (PENDING 상태)
        UserRole userRole = new UserRole();
        UserRole.UserRoleId userRoleId = new UserRole.UserRoleId();
        userRoleId.setUserId(teacher.getId());
        userRoleId.setRoleId(teacherRole.getId());
        userRole.setId(userRoleId);
        userRole.setUser(teacher);
        userRole.setRole(teacherRole);
        userRole.setStatus(UserRole.RoleStatus.PENDING);
        entityManager.persist(userRole);

        entityManager.flush();
        entityManager.clear();

        // When
        List<UserRoleResponse> responses = userRoleRepository
                .findUserRoleResponsesByRoleNameAndStatus("TEACHER", UserRole.RoleStatus.PENDING);

        // Then
        assertThat(responses).hasSize(1);
        UserRoleResponse response = responses.get(0);
        assertThat(response.getUserId()).isEqualTo(teacher.getId());
        assertThat(response.getUsername()).isEqualTo("teacher01");
        assertThat(response.getName()).isEqualTo("김선생");
        assertThat(response.getRoleName()).isEqualTo("TEACHER");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getSchoolName()).isNull(); // 교사는 학교 정보 없음
        assertThat(response.getGrade()).isNull(); // 교사는 학년 정보 없음
    }

    @Test
    void testFindUserRoleResponsesByRoleNameAndStatus_StudentWithoutSchoolInfo() {
        // Given
        // 1. 역할 생성
        Role studentRole = new Role();
        studentRole.setName("STUDENT");
        entityManager.persist(studentRole);

        // 2. 사용자 생성
        User student = User.builder()
                .username("student02")
                .name("박학생")
                .password("password")
                .build();
        entityManager.persist(student);

        // 3. 학생 프로필 생성 (학교 정보 없음)
        StudentProfile studentProfile = StudentProfile.builder()
                .userId(student.getId())
                .user(student)
                .school(null) // 학교 정보 없음
                .grade(null) // 학년 정보 없음
                .status(StudentProfile.StudentStatus.INQUIRY)
                .gender(StudentProfile.Gender.FEMALE)
                .build();
        entityManager.persist(studentProfile);

        // 4. 사용자 역할 생성 (PENDING 상태)
        UserRole userRole = new UserRole();
        UserRole.UserRoleId userRoleId = new UserRole.UserRoleId();
        userRoleId.setUserId(student.getId());
        userRoleId.setRoleId(studentRole.getId());
        userRole.setId(userRoleId);
        userRole.setUser(student);
        userRole.setRole(studentRole);
        userRole.setStatus(UserRole.RoleStatus.PENDING);
        entityManager.persist(userRole);

        entityManager.flush();
        entityManager.clear();

        // When
        List<UserRoleResponse> responses = userRoleRepository
                .findUserRoleResponsesByRoleNameAndStatus("STUDENT", UserRole.RoleStatus.PENDING);

        // Then
        assertThat(responses).hasSize(1);
        UserRoleResponse response = responses.get(0);
        assertThat(response.getUserId()).isEqualTo(student.getId());
        assertThat(response.getUsername()).isEqualTo("student02");
        assertThat(response.getName()).isEqualTo("박학생");
        assertThat(response.getRoleName()).isEqualTo("STUDENT");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getSchoolName()).isNull(); // 학교 정보 없음
        assertThat(response.getGrade()).isNull(); // 학년 정보 없음
    }
}
