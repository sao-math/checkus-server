package saomath.checkusserver.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import saomath.checkusserver.auth.domain.Role;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.domain.UserRole;
import saomath.checkusserver.auth.repository.RoleRepository;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.auth.repository.UserRoleRepository;
import saomath.checkusserver.school.domain.School;
import saomath.checkusserver.school.repository.SchoolRepository;
import saomath.checkusserver.user.domain.StudentProfile;
import saomath.checkusserver.user.repository.StudentProfileRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository 재원 학생 조회 테스트")
class UserRepositoryEnrolledStudentsTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    private Role studentRole;
    private School testSchool;

    @BeforeEach
    void setUp() {
        // 테스트용 학교 생성 (기존 확인 후)
        testSchool = schoolRepository.findByName("테스트 학교")
                .orElseGet(() -> {
                    School school = School.builder()
                            .name("테스트 학교")
                            .build();
                    return schoolRepository.save(school);
                });

        // 학생 역할 생성 (기존 확인 후)
        studentRole = roleRepository.findByName("STUDENT")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("STUDENT");
                    return roleRepository.save(role);
                });
    }

    @Test
    @DisplayName("findAllStudents - 모든 상태의 학생이 조회되어야 한다 (학생 관리용)")
    void findAllStudents_ShouldReturnAllStudents() {
        // Given: 다양한 상태의 학생들 생성
        User enrolledStudent = createStudentWithStatus(StudentProfile.StudentStatus.ENROLLED, "재원학생");
        User waitingStudent = createStudentWithStatus(StudentProfile.StudentStatus.WAITING, "대기학생");
        User withdrawnStudent = createStudentWithStatus(StudentProfile.StudentStatus.WITHDRAWN, "퇴원학생");
        User inquiryStudent = createStudentWithStatus(StudentProfile.StudentStatus.INQUIRY, "문의학생");
        User unregisteredStudent = createStudentWithStatus(StudentProfile.StudentStatus.UNREGISTERED, "미등록학생");

        // When: 모든 학생 조회
        List<User> result = userRepository.findAllStudents();

        // Then: 모든 상태의 학생이 조회되어야 함
        assertThat(result).hasSize(5);
        assertThat(result).extracting(User::getName)
                .containsExactlyInAnyOrder("재원학생", "대기학생", "퇴원학생", "문의학생", "미등록학생");
    }

    @Test
    @DisplayName("findAllEnrolledStudents - 재원 상태인 학생만 조회되어야 한다 (모니터링용)")
    void findAllEnrolledStudents_ShouldReturnOnlyEnrolledStudents() {
        // Given: 다양한 상태의 학생들 생성
        User enrolledStudent = createStudentWithStatus(StudentProfile.StudentStatus.ENROLLED, "재원학생");
        User waitingStudent = createStudentWithStatus(StudentProfile.StudentStatus.WAITING, "대기학생");
        User withdrawnStudent = createStudentWithStatus(StudentProfile.StudentStatus.WITHDRAWN, "퇴원학생");
        User inquiryStudent = createStudentWithStatus(StudentProfile.StudentStatus.INQUIRY, "문의학생");
        User unregisteredStudent = createStudentWithStatus(StudentProfile.StudentStatus.UNREGISTERED, "미등록학생");

        // When: 재원 학생만 조회
        List<User> result = userRepository.findAllEnrolledStudents();

        // Then: 재원 상태인 학생만 조회되어야 함
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(enrolledStudent.getId());
        assertThat(result.get(0).getName()).isEqualTo("재원학생");
    }

    @Test
    @DisplayName("findAllEnrolledStudents - 재원 상태이지만 STUDENT 역할이 INACTIVE인 경우 조회되지 않아야 한다")
    void findAllEnrolledStudents_ShouldNotReturnInactiveStudentRole() {
        // Given: 재원 상태이지만 역할이 비활성화된 학생
        User enrolledButInactiveStudent = createUser("비활성재원학생");
        createStudentProfile(enrolledButInactiveStudent, StudentProfile.StudentStatus.ENROLLED);
        createUserRole(enrolledButInactiveStudent, UserRole.RoleStatus.PENDING); // INACTIVE

        // When: 재원 학생만 조회
        List<User> result = userRepository.findAllEnrolledStudents();

        // Then: 조회되지 않아야 함
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllEnrolledStudents - STUDENT 역할이 ACTIVE이지만 StudentProfile이 없는 경우 조회되지 않아야 한다")
    void findAllEnrolledStudents_ShouldNotReturnStudentsWithoutProfile() {
        // Given: STUDENT 역할은 있지만 StudentProfile이 없는 사용자
        User userWithoutProfile = createUser("프로필없는사용자");
        createUserRole(userWithoutProfile, UserRole.RoleStatus.ACTIVE);
        // StudentProfile 생성하지 않음

        // When: 재원 학생만 조회
        List<User> result = userRepository.findAllEnrolledStudents();

        // Then: 조회되지 않아야 함
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllEnrolledStudents - 여러 재원 학생이 있는 경우 모두 조회되어야 한다")
    void findAllEnrolledStudents_ShouldReturnAllEnrolledStudents() {
        // Given: 여러 재원 학생 생성
        User enrolledStudent1 = createStudentWithStatus(StudentProfile.StudentStatus.ENROLLED, "재원학생1");
        User enrolledStudent2 = createStudentWithStatus(StudentProfile.StudentStatus.ENROLLED, "재원학생2");
        User enrolledStudent3 = createStudentWithStatus(StudentProfile.StudentStatus.ENROLLED, "재원학생3");
        
        // 다른 상태 학생도 섞어서 생성
        createStudentWithStatus(StudentProfile.StudentStatus.WAITING, "대기학생");
        createStudentWithStatus(StudentProfile.StudentStatus.WITHDRAWN, "퇴원학생");

        // When: 재원 학생만 조회
        List<User> result = userRepository.findAllEnrolledStudents();

        // Then: 재원 학생 3명만 조회되어야 함
        assertThat(result).hasSize(3);
        assertThat(result).extracting(User::getName)
                .containsExactlyInAnyOrder("재원학생1", "재원학생2", "재원학생3");
    }

    // Helper Methods
    private User createStudentWithStatus(StudentProfile.StudentStatus status, String name) {
        User user = createUser(name);
        createStudentProfile(user, status);
        createUserRole(user, UserRole.RoleStatus.ACTIVE);
        return user;
    }

    private User createUser(String name) {
        User user = User.builder()
                .username(name.toLowerCase().replaceAll("\\s", "") + System.currentTimeMillis())
                .name(name)
                .phoneNumber("010-" + String.format("%04d", (int)(Math.random() * 10000)) + "-" + String.format("%04d", (int)(Math.random() * 10000)))
                .password("hashedPassword")
                .discordId("discord_" + name)
                .build();
        return userRepository.save(user);
    }

    private void createStudentProfile(User user, StudentProfile.StudentStatus status) {
        StudentProfile profile = StudentProfile.builder()
                .user(user)  // @MapsId로 인해 userId 자동 설정
                .status(status)
                .school(testSchool)
                .grade(1)
                .gender(StudentProfile.Gender.OTHER)
                .build();
        studentProfileRepository.save(profile);
    }

    private void createUserRole(User user, UserRole.RoleStatus status) {
        UserRole.UserRoleId id = new UserRole.UserRoleId(user.getId(), studentRole.getId());
        UserRole userRole = new UserRole(id, user, studentRole, status);
        userRoleRepository.save(userRole);
    }
}
