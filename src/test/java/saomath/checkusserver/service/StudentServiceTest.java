package saomath.checkusserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.classroom.domain.ClassEntity;
import saomath.checkusserver.classroom.domain.StudentClass;
import saomath.checkusserver.classroom.repository.StudentClassRepository;
import saomath.checkusserver.user.domain.RoleConstants;
import saomath.checkusserver.school.domain.School;
import saomath.checkusserver.user.domain.StudentGuardian;
import saomath.checkusserver.user.domain.StudentProfile;
import saomath.checkusserver.user.dto.StudentDetailResponse;
import saomath.checkusserver.user.dto.StudentListResponse;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.user.repository.StudentGuardianRepository;
import saomath.checkusserver.user.repository.StudentProfileRepository;
import saomath.checkusserver.user.service.StudentService;
import saomath.checkusserver.user.service.UserRoleService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private StudentClassRepository studentClassRepository;

    @Mock
    private StudentGuardianRepository studentGuardianRepository;

    @Mock
    private UserRoleService userRoleService;

    @InjectMocks
    private StudentService studentService;

    private User mockStudent;
    private StudentProfile mockStudentProfile;
    private School mockSchool;
    private ClassEntity mockClass;
    private User mockGuardian;

    @BeforeEach
    void setUp() {
        // Mock 데이터 설정
        mockSchool = School.builder()
                .id(1L)
                .name("이현중")
                .build();

        mockStudent = User.builder()
                .id(4L)
                .username("student1")
                .name("박학생")
                .phoneNumber("010-2222-1111")
                .discordId("student1#1234")
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0, 0))
                .build();

        mockStudentProfile = StudentProfile.builder()
                .userId(4L)
                .user(mockStudent)
                .status(StudentProfile.StudentStatus.ENROLLED)
                .school(mockSchool)
                .grade(2)
                .gender(StudentProfile.Gender.MALE)
                .build();

        mockClass = ClassEntity.builder()
                .id(1L)
                .name("수학심화반")
                .build();

        mockGuardian = User.builder()
                .id(5L)
                .name("박학부모")
                .phoneNumber("010-1111-2222")
                .build();
    }

    @Test
    @DisplayName("필터링된 학생 목록 조회 - 성공")
    void getFilteredStudents_Success() {
        // Given
        when(userRepository.findStudentsWithFilters(eq(1L), eq(2), eq(StudentProfile.StudentStatus.ENROLLED), eq(1L)))
                .thenReturn(Arrays.asList(mockStudent));
        when(studentProfileRepository.findByUserId(4L))
                .thenReturn(Optional.of(mockStudentProfile));
        when(studentClassRepository.findByStudentId(4L))
                .thenReturn(Arrays.asList(createMockStudentClass()));
        when(studentGuardianRepository.findByStudentId(4L))
                .thenReturn(Arrays.asList(createMockStudentGuardian()));

        // When
        List<StudentListResponse> result = studentService.getFilteredStudents(1L, 2, StudentProfile.StudentStatus.ENROLLED, 1L);

        // Then
        assertThat(result).hasSize(1);
        StudentListResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(4L);
        assertThat(response.getName()).isEqualTo("박학생");
        assertThat(response.getSchool()).isEqualTo("이현중");
        assertThat(response.getGrade()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(StudentProfile.StudentStatus.ENROLLED);
        assertThat(response.getClasses()).contains("수학심화반");
        assertThat(response.getGuardians()).hasSize(1);
        assertThat(response.getGuardians().get(0).getName()).isEqualTo("박학부모");
    }

    @Test
    @DisplayName("전체 학생 목록 조회 - 필터 없음")
    void getFilteredStudents_NoFilter_Success() {
        // Given
        when(userRepository.findAllStudents())
                .thenReturn(Arrays.asList(mockStudent));
        when(studentProfileRepository.findByUserId(4L))
                .thenReturn(Optional.of(mockStudentProfile));
        when(studentClassRepository.findByStudentId(4L))
                .thenReturn(Arrays.asList(createMockStudentClass()));
        when(studentGuardianRepository.findByStudentId(4L))
                .thenReturn(Arrays.asList(createMockStudentGuardian()));

        // When
        List<StudentListResponse> result = studentService.getFilteredStudents(null, null, null, null);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("박학생");
    }

    @Test
    @DisplayName("학생 상세 정보 조회 - 성공")
    void getStudentDetail_Success() {
        // Given
        when(userRepository.findByIdAndNotDeleted(4L))
                .thenReturn(Optional.of(mockStudent));
        when(userRoleService.getActiveRoles(4L))
                .thenReturn(Arrays.asList(RoleConstants.STUDENT));
        when(studentProfileRepository.findByUserId(4L))
                .thenReturn(Optional.of(mockStudentProfile));
        when(studentClassRepository.findByStudentId(4L))
                .thenReturn(Arrays.asList(createMockStudentClass()));
        when(studentGuardianRepository.findByStudentId(4L))
                .thenReturn(Arrays.asList(createMockStudentGuardian()));

        // When
        StudentDetailResponse result = studentService.getStudentDetail(4L);

        // Then
        assertThat(result.getId()).isEqualTo(4L);
        assertThat(result.getUsername()).isEqualTo("student1");
        assertThat(result.getName()).isEqualTo("박학생");
        assertThat(result.getSchool()).isEqualTo("이현중");
        assertThat(result.getGrade()).isEqualTo(2);
        assertThat(result.getStatus()).isEqualTo(StudentProfile.StudentStatus.ENROLLED);
        assertThat(result.getClasses()).hasSize(1);
        assertThat(result.getClasses().get(0).getName()).isEqualTo("수학심화반");
        assertThat(result.getGuardians()).hasSize(1);
        assertThat(result.getGuardians().get(0).getName()).isEqualTo("박학부모");
    }

    @Test
    @DisplayName("학생 상세 정보 조회 - 학생을 찾을 수 없음")
    void getStudentDetail_StudentNotFound() {
        // Given
        when(userRepository.findByIdAndNotDeleted(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> studentService.getStudentDetail(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("학생을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("학생 상세 정보 조회 - 학생 역할이 없음")
    void getStudentDetail_NotStudent() {
        // Given
        when(userRepository.findByIdAndNotDeleted(4L))
                .thenReturn(Optional.of(mockStudent));
        when(userRoleService.getActiveRoles(4L))
                .thenReturn(Arrays.asList(RoleConstants.TEACHER)); // 학생이 아님

        // When & Then
        assertThatThrownBy(() -> studentService.getStudentDetail(4L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("해당 사용자는 학생이 아닙니다");
    }

    @Test
    @DisplayName("학생 상세 정보 조회 - 학생 프로필이 없음")
    void getStudentDetail_StudentProfileNotFound() {
        // Given
        when(userRepository.findByIdAndNotDeleted(4L))
                .thenReturn(Optional.of(mockStudent));
        when(userRoleService.getActiveRoles(4L))
                .thenReturn(Arrays.asList(RoleConstants.STUDENT));
        when(studentProfileRepository.findByUserId(4L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> studentService.getStudentDetail(4L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("학생 프로필을 찾을 수 없습니다");
    }

    private StudentClass createMockStudentClass() {
        StudentClass studentClass = new StudentClass();
        studentClass.setStudent(mockStudent);
        studentClass.setClassEntity(mockClass);
        
        // 복합키 설정
        StudentClass.StudentClassId id = new StudentClass.StudentClassId();
        id.setStudentId(mockStudent.getId());
        id.setClassId(mockClass.getId());
        studentClass.setId(id);
        
        return studentClass;
    }

    private StudentGuardian createMockStudentGuardian() {
        StudentGuardian studentGuardian = new StudentGuardian();
        studentGuardian.setStudent(mockStudent);
        studentGuardian.setGuardian(mockGuardian);
        studentGuardian.setRelationship("모");
        
        // 복합키 설정
        StudentGuardian.StudentGuardianId id = new StudentGuardian.StudentGuardianId();
        id.setStudentId(mockStudent.getId());
        id.setGuardianId(mockGuardian.getId());
        studentGuardian.setId(id);
        
        return studentGuardian;
    }
}
