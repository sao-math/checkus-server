package saomath.checkusserver.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.user.domain.StudentProfile;
import saomath.checkusserver.user.repository.StudentProfileRepository;
import saomath.checkusserver.school.domain.School;
import saomath.checkusserver.school.repository.SchoolRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StudentServiceDeleteTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private UserRoleService userRoleService;

    @Test
    @DisplayName("학생 삭제 성공 - 논리적 삭제")
    void deleteStudent_Success() {
        // Given
        School school = School.builder()
                .name("테스트학교")
                .build();
        school = schoolRepository.save(school);

        User student = User.builder()
                .username("test_student")
                .name("테스트학생")
                .phoneNumber("010-1234-5678")
                .password("password")
                .build();
        student = userRepository.save(student);

        StudentProfile profile = StudentProfile.builder()
                .user(student)
                .school(school)
                .grade(2)
                .status(StudentProfile.StudentStatus.ENROLLED)
                .gender(StudentProfile.Gender.MALE)
                .build();
        studentProfileRepository.save(profile);

        // 학생 역할 추가
        userRoleService.assignRoleDirectly(student.getId(), "STUDENT");

        // When
        studentService.deleteStudent(student.getId());

        // Then
        User deletedStudent = userRepository.findById(student.getId()).orElseThrow();
        assertThat(deletedStudent.isDeleted()).isTrue();
        assertThat(deletedStudent.getDeletedAt()).isNotNull();
        assertThat(deletedStudent.getDeletedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("학생 삭제 실패 - 존재하지 않는 학생")
    void deleteStudent_StudentNotFound() {
        // Given
        Long nonExistentStudentId = 999L;

        // When & Then
        assertThatThrownBy(() -> studentService.deleteStudent(nonExistentStudentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("학생을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("학생 삭제 실패 - 이미 삭제된 학생")
    void deleteStudent_AlreadyDeleted() {
        // Given
        School school = School.builder()
                .name("테스트학교")
                .build();
        school = schoolRepository.save(school);

        User student = User.builder()
                .username("test_student")
                .name("테스트학생")
                .phoneNumber("010-1234-5678")
                .password("password")
                .build();
        final User savedStudent = userRepository.save(student); // final 변수로 생성
        savedStudent.markAsDeleted(); // 미리 삭제 상태로 설정
        userRepository.save(savedStudent);

        StudentProfile profile = StudentProfile.builder()
                .user(savedStudent)
                .school(school)
                .grade(2)
                .status(StudentProfile.StudentStatus.ENROLLED)
                .gender(StudentProfile.Gender.MALE)
                .build();
        studentProfileRepository.save(profile);

        // 학생 역할 추가
        userRoleService.assignRoleDirectly(savedStudent.getId(), "STUDENT");

        // When & Then
        assertThatThrownBy(() -> studentService.deleteStudent(savedStudent.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("학생을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("학생 복구 성공")
    void restoreStudent_Success() {
        // Given
        School school = School.builder()
                .name("테스트학교")
                .build();
        school = schoolRepository.save(school);

        User student = User.builder()
                .username("test_student")
                .name("테스트학생")
                .phoneNumber("010-1234-5678")
                .password("password")
                .build();
        final User savedStudent = userRepository.save(student);
        savedStudent.markAsDeleted(); // 삭제 상태로 설정
        userRepository.save(savedStudent);

        StudentProfile profile = StudentProfile.builder()
                .user(savedStudent)
                .school(school)
                .grade(2)
                .status(StudentProfile.StudentStatus.ENROLLED)
                .gender(StudentProfile.Gender.MALE)
                .build();
        studentProfileRepository.save(profile);

        // When
        studentService.restoreStudent(savedStudent.getId());

        // Then
        User restoredStudent = userRepository.findById(savedStudent.getId()).orElseThrow();
        assertThat(restoredStudent.isDeleted()).isFalse();
        assertThat(restoredStudent.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("학생 복구 실패 - 존재하지 않는 학생")
    void restoreStudent_StudentNotFound() {
        // Given
        Long nonExistentStudentId = 999L;

        // When & Then
        assertThatThrownBy(() -> studentService.restoreStudent(nonExistentStudentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("학생을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("학생 복구 실패 - 삭제되지 않은 학생")
    void restoreStudent_NotDeleted() {
        // Given
        School school = School.builder()
                .name("테스트학교")
                .build();
        school = schoolRepository.save(school);

        User student = User.builder()
                .username("test_student")
                .name("테스트학생")
                .phoneNumber("010-1234-5678")
                .password("password")
                .build();
        final User savedStudent = userRepository.save(student); // 정상 상태

        StudentProfile profile = StudentProfile.builder()
                .user(savedStudent)
                .school(school)
                .grade(2)
                .status(StudentProfile.StudentStatus.ENROLLED)
                .gender(StudentProfile.Gender.MALE)
                .build();
        studentProfileRepository.save(profile);

        // When & Then
        assertThatThrownBy(() -> studentService.restoreStudent(savedStudent.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("삭제되지 않은 학생입니다");
    }

    @Test
    @DisplayName("삭제된 학생은 조회에서 제외됨")
    void getStudentDetail_DeletedStudentNotFound() {
        // Given
        School school = School.builder()
                .name("테스트학교")
                .build();
        school = schoolRepository.save(school);

        User student = User.builder()
                .username("test_student")
                .name("테스트학생")
                .phoneNumber("010-1234-5678")
                .password("password")
                .build();
        final User savedStudent = userRepository.save(student);

        StudentProfile profile = StudentProfile.builder()
                .user(savedStudent)
                .school(school)
                .grade(2)
                .status(StudentProfile.StudentStatus.ENROLLED)
                .gender(StudentProfile.Gender.MALE)
                .build();
        studentProfileRepository.save(profile);

        // 학생 역할 추가
        userRoleService.assignRoleDirectly(savedStudent.getId(), "STUDENT");

        // 학생 삭제
        studentService.deleteStudent(savedStudent.getId());

        // When & Then
        assertThatThrownBy(() -> studentService.getStudentDetail(savedStudent.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("학생을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("Repository 논리삭제 쿼리 테스트")
    void userRepository_SoftDeleteQuery() {
        // Given
        User activeUser = User.builder()
                .username("active_user")
                .name("활성사용자")
                .phoneNumber("010-1111-1111")
                .password("password")
                .build();
        activeUser = userRepository.save(activeUser);

        User deletedUser = User.builder()
                .username("deleted_user")
                .name("삭제된사용자")
                .phoneNumber("010-2222-2222")
                .password("password")
                .build();
        final User savedDeletedUser = userRepository.save(deletedUser);
        savedDeletedUser.markAsDeleted();
        userRepository.save(savedDeletedUser);

        // When & Then
        // 활성 사용자는 조회됨
        Optional<User> foundActiveUser = userRepository.findByIdAndNotDeleted(activeUser.getId());
        assertThat(foundActiveUser).isPresent();
        assertThat(foundActiveUser.get().getName()).isEqualTo("활성사용자");

        // 삭제된 사용자는 조회되지 않음
        Optional<User> foundDeletedUser = userRepository.findByIdAndNotDeleted(savedDeletedUser.getId());
        assertThat(foundDeletedUser).isEmpty();

        // 일반 findById로는 삭제된 사용자도 조회됨
        Optional<User> foundDeletedUserById = userRepository.findById(savedDeletedUser.getId());
        assertThat(foundDeletedUserById).isPresent();
        assertThat(foundDeletedUserById.get().isDeleted()).isTrue();
    }
}
