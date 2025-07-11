package saomath.checkusserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.auth.domain.Role;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.domain.UserRole;
import saomath.checkusserver.auth.repository.RoleRepository;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.auth.repository.UserRoleRepository;
import saomath.checkusserver.school.domain.School;
import saomath.checkusserver.school.repository.SchoolRepository;
import saomath.checkusserver.studyTime.dto.StudyTimeMonitorResponse;
import saomath.checkusserver.studyTime.service.StudyTimeService;
import saomath.checkusserver.user.domain.StudentProfile;
import saomath.checkusserver.user.repository.StudentProfileRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("StudyTimeService 재원생 모니터링 테스트")
class StudyTimeServiceEnrolledStudentsTest {

    @Autowired
    private StudyTimeService studyTimeService;

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
    @DisplayName("모니터링 조회 시 재원 상태인 학생만 포함되어야 한다")
    void getStudyTimeMonitorByTimeRange_ShouldIncludeOnlyEnrolledStudents() {
        // Given: 다양한 상태의 학생들 생성
        User enrolledStudent1 = createStudentWithStatus(StudentProfile.StudentStatus.ENROLLED, "재원학생1");
        User enrolledStudent2 = createStudentWithStatus(StudentProfile.StudentStatus.ENROLLED, "재원학생2");
        User waitingStudent = createStudentWithStatus(StudentProfile.StudentStatus.WAITING, "대기학생");
        User withdrawnStudent = createStudentWithStatus(StudentProfile.StudentStatus.WITHDRAWN, "퇴원학생");
        User inquiryStudent = createStudentWithStatus(StudentProfile.StudentStatus.INQUIRY, "문의학생");

        // When: 모니터링 데이터 조회 (재원생만 조회됨)
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now().plusHours(1);
        StudyTimeMonitorResponse response = studyTimeService.getStudyTimeMonitorByTimeRange(startTime, endTime);

        // Then: 재원 상태인 학생만 포함되어야 함
        assertThat(response.getStudents()).hasSize(2);
        
        List<String> studentNames = response.getStudents().stream()
                .map(StudyTimeMonitorResponse.StudentStudyInfo::getStudentName)
                .toList();
        
        assertThat(studentNames).containsExactlyInAnyOrder("재원학생1", "재원학생2");
        assertThat(studentNames).doesNotContain("대기학생", "퇴원학생", "문의학생");
    }

    @Test
    @DisplayName("재원 상태이지만 STUDENT 역할이 비활성화된 경우 모니터링에 포함되지 않아야 한다")
    void getStudyTimeMonitorByTimeRange_ShouldNotIncludeInactiveStudentRole() {
        // Given: 재원 상태이지만 역할이 비활성화된 학생
        User enrolledButInactiveStudent = createUser("비활성재원학생");
        createStudentProfile(enrolledButInactiveStudent, StudentProfile.StudentStatus.ENROLLED);
        createUserRole(enrolledButInactiveStudent, UserRole.RoleStatus.PENDING); // 비활성

        // 정상적인 재원 학생도 하나 생성
        createStudentWithStatus(StudentProfile.StudentStatus.ENROLLED, "정상재원학생");

        // When: 모니터링 데이터 조회
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now().plusHours(1);
        StudyTimeMonitorResponse response = studyTimeService.getStudyTimeMonitorByTimeRange(startTime, endTime);

        // Then: 활성화된 재원 학생만 포함되어야 함
        assertThat(response.getStudents()).hasSize(1);
        assertThat(response.getStudents().get(0).getStudentName()).isEqualTo("정상재원학생");
    }

    @Test
    @DisplayName("STUDENT 역할이 활성화되어 있지만 StudentProfile이 없는 경우 모니터링에 포함되지 않아야 한다")
    void getStudyTimeMonitorByTimeRange_ShouldNotIncludeStudentsWithoutProfile() {
        // Given: STUDENT 역할은 있지만 StudentProfile이 없는 사용자
        User userWithoutProfile = createUser("프로필없는사용자");
        createUserRole(userWithoutProfile, UserRole.RoleStatus.ACTIVE);
        // StudentProfile 생성하지 않음

        // 정상적인 재원 학생도 하나 생성
        createStudentWithStatus(StudentProfile.StudentStatus.ENROLLED, "정상재원학생");

        // When: 모니터링 데이터 조회
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now().plusHours(1);
        StudyTimeMonitorResponse response = studyTimeService.getStudyTimeMonitorByTimeRange(startTime, endTime);

        // Then: StudentProfile이 있는 재원 학생만 포함되어야 함
        assertThat(response.getStudents()).hasSize(1);
        assertThat(response.getStudents().get(0).getStudentName()).isEqualTo("정상재원학생");
    }

    @Test
    @DisplayName("재원 학생이 없는 경우 빈 목록을 반환해야 한다")
    void getStudyTimeMonitorByTimeRange_ShouldReturnEmptyWhenNoEnrolledStudents() {
        // Given: 재원 학생이 없고 다른 상태의 학생들만 존재
        createStudentWithStatus(StudentProfile.StudentStatus.WAITING, "대기학생");
        createStudentWithStatus(StudentProfile.StudentStatus.WITHDRAWN, "퇴원학생");
        createStudentWithStatus(StudentProfile.StudentStatus.INQUIRY, "문의학생");

        // When: 모니터링 데이터 조회
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now().plusHours(1);
        StudyTimeMonitorResponse response = studyTimeService.getStudyTimeMonitorByTimeRange(startTime, endTime);

        // Then: 빈 목록이 반환되어야 함
        assertThat(response.getStudents()).isEmpty();
        assertThat(response.getStartTime()).isEqualTo(startTime);
        assertThat(response.getEndTime()).isEqualTo(endTime);
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
