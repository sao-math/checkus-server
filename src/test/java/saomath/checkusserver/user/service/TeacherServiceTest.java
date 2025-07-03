package saomath.checkusserver.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.domain.UserRole;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.classroom.domain.ClassEntity;
import saomath.checkusserver.classroom.domain.TeacherClass;
import saomath.checkusserver.classroom.repository.ClassRepository;
import saomath.checkusserver.classroom.repository.TeacherClassRepository;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.user.domain.RoleConstants;
import saomath.checkusserver.user.dto.TeacherDetailResponse;
import saomath.checkusserver.user.dto.TeacherListResponse;
import saomath.checkusserver.user.dto.TeacherUpdateRequest;
import saomath.checkusserver.util.TestDataFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeacherService 단위 테스트")
class TeacherServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeacherClassRepository teacherClassRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private UserRoleService userRoleService;

    @InjectMocks
    private TeacherService teacherService;

    private User teacherUser;
    private ClassEntity classEntity1;
    private ClassEntity classEntity2;
    private TeacherClass teacherClass1;
    private TeacherClass teacherClass2;

    @BeforeEach
    void setUp() {
        TestDataFactory.resetCounter();
        
        // 테스트용 교사 사용자 생성
        teacherUser = TestDataFactory.createTeacher("teacher1", "김선생님", "010-1234-5678");
        teacherUser.setId(1L);
        teacherUser.setCreatedAt(LocalDateTime.now());

        // 테스트용 반 생성
        classEntity1 = ClassEntity.builder()
                .id(1L)
                .name("고1 수학")
                .build();

        classEntity2 = ClassEntity.builder()
                .id(2L)
                .name("고2 수학")
                .build();

        // 테스트용 교사-반 연결 생성
        teacherClass1 = TeacherClass.builder()
                .id(new TeacherClass.TeacherClassId(1L, 1L))
                .teacher(teacherUser)
                .classEntity(classEntity1)
                .build();

        teacherClass2 = TeacherClass.builder()
                .id(new TeacherClass.TeacherClassId(1L, 2L))
                .teacher(teacherUser)
                .classEntity(classEntity2)
                .build();
    }

    @Test
    @DisplayName("활성화된 교사 목록 조회 - 성공")
    void getActiveTeachers_Success() {
        // given
        List<User> teachers = Arrays.asList(teacherUser);
        List<TeacherClass> teacherClasses = Arrays.asList(teacherClass1, teacherClass2);

        given(userRepository.findTeachersByStatus(UserRole.RoleStatus.ACTIVE))
                .willReturn(teachers);
        given(teacherClassRepository.findByTeacherId(1L))
                .willReturn(teacherClasses);
        given(userRoleService.getTeacherRoleStatus(1L))
                .willReturn(UserRole.RoleStatus.ACTIVE);

        // when
        List<TeacherListResponse> result = teacherService.getActiveTeachers("ACTIVE");

        // then
        assertThat(result).hasSize(1);
        TeacherListResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("김선생님");
        assertThat(response.getStatus()).isEqualTo(UserRole.RoleStatus.ACTIVE);
        assertThat(response.getClasses()).hasSize(2);
        assertThat(response.getClasses().get(0).getName()).isEqualTo("고1 수학");
        assertThat(response.getClasses().get(1).getName()).isEqualTo("고2 수학");
    }

    @Test
    @DisplayName("교사 목록 조회 - 잘못된 상태값으로 기본값 사용")
    void getActiveTeachers_InvalidStatus_UsesDefault() {
        // given
        given(userRepository.findTeachersByStatus(UserRole.RoleStatus.ACTIVE))
                .willReturn(Collections.emptyList());

        // when
        List<TeacherListResponse> result = teacherService.getActiveTeachers("INVALID_STATUS");

        // then
        verify(userRepository).findTeachersByStatus(UserRole.RoleStatus.ACTIVE);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("교사 상세 정보 조회 - 성공")
    void getTeacherDetail_Success() {
        // given
        List<String> roles = Arrays.asList(RoleConstants.TEACHER);
        List<TeacherClass> teacherClasses = Arrays.asList(teacherClass1, teacherClass2);

        given(userRepository.findById(1L)).willReturn(Optional.of(teacherUser));
        given(userRoleService.getActiveRoles(1L)).willReturn(roles);
        given(teacherClassRepository.findByTeacherId(1L)).willReturn(teacherClasses);
        given(userRoleService.getTeacherRoleStatus(1L)).willReturn(UserRole.RoleStatus.ACTIVE);

        // when
        TeacherDetailResponse result = teacherService.getTeacherDetail(1L);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("김선생님");
        assertThat(result.getUsername()).isEqualTo("teacher1");
        assertThat(result.getPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(result.getStatus()).isEqualTo(UserRole.RoleStatus.ACTIVE);
        assertThat(result.getClasses()).hasSize(2);
        
        TeacherDetailResponse.ClassDetailInfo class1 = result.getClasses().get(0);
        assertThat(class1.getId()).isEqualTo(1L);
        assertThat(class1.getName()).isEqualTo("고1 수학");
        assertThat(class1.getStudentCount()).isEqualTo(0); // TODO: 실제 구현 시 수정 필요
    }

    @Test
    @DisplayName("교사 상세 정보 조회 - 존재하지 않는 교사")
    void getTeacherDetail_NotFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> teacherService.getTeacherDetail(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("교사를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("교사 상세 정보 조회 - 교사 역할이 없는 사용자")
    void getTeacherDetail_NotTeacher() {
        // given
        List<String> roles = Arrays.asList("STUDENT"); // 교사 역할 없음

        given(userRepository.findById(1L)).willReturn(Optional.of(teacherUser));
        given(userRoleService.getActiveRoles(1L)).willReturn(roles);

        // when & then
        assertThatThrownBy(() -> teacherService.getTeacherDetail(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("해당 사용자는 교사가 아닙니다");
    }

    @Test
    @DisplayName("교사 정보 수정 - 기본 정보만 수정")
    void updateTeacher_BasicInfo_Success() {
        // given
        TeacherUpdateRequest updateRequest = new TeacherUpdateRequest();
        updateRequest.setName("수정된 이름");
        updateRequest.setPhoneNumber("010-9999-8888");
        updateRequest.setDiscordId("newdiscord#1234");

        List<String> roles = Arrays.asList(RoleConstants.TEACHER);
        List<TeacherClass> teacherClasses = Arrays.asList(teacherClass1);

        given(userRepository.findById(1L)).willReturn(Optional.of(teacherUser));
        given(userRoleService.getActiveRoles(1L)).willReturn(roles);
        given(userRepository.save(any(User.class))).willReturn(teacherUser);
        
        // getTeacherDetail 호출을 위한 모킹
        given(teacherClassRepository.findByTeacherId(1L)).willReturn(teacherClasses);
        given(userRoleService.getTeacherRoleStatus(1L)).willReturn(UserRole.RoleStatus.ACTIVE);

        // when
        TeacherDetailResponse result = teacherService.updateTeacher(1L, updateRequest);

        // then
        assertThat(teacherUser.getName()).isEqualTo("수정된 이름");
        assertThat(teacherUser.getPhoneNumber()).isEqualTo("010-9999-8888");
        assertThat(teacherUser.getDiscordId()).isEqualTo("newdiscord#1234");
        verify(userRepository).save(teacherUser);
    }

    @Test
    @DisplayName("교사 정보 수정 - 담당 반 변경")
    void updateTeacher_WithClassUpdate_Success() {
        // given
        TeacherUpdateRequest updateRequest = new TeacherUpdateRequest();
        updateRequest.setClassIds(Arrays.asList(2L, 3L)); // 새로운 반 배정

        List<String> roles = Arrays.asList(RoleConstants.TEACHER);
        List<TeacherClass> existingClasses = Arrays.asList(teacherClass1, teacherClass2);

        ClassEntity classEntity3 = ClassEntity.builder()
                .id(3L)
                .name("고3 수학")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(teacherUser));
        given(userRoleService.getActiveRoles(1L)).willReturn(roles);
        given(teacherClassRepository.findByTeacherId(1L)).willReturn(existingClasses);
        given(classRepository.findById(2L)).willReturn(Optional.of(classEntity2));
        given(classRepository.findById(3L)).willReturn(Optional.of(classEntity3));
        given(userRepository.save(any(User.class))).willReturn(teacherUser);

        // getTeacherDetail 호출을 위한 모킹
        List<TeacherClass> newTeacherClasses = Arrays.asList(
                TeacherClass.builder()
                        .id(new TeacherClass.TeacherClassId(1L, 2L))
                        .teacher(teacherUser)
                        .classEntity(classEntity2)
                        .build()
        );
        given(teacherClassRepository.findByTeacherId(1L)).willReturn(newTeacherClasses);
        given(userRoleService.getTeacherRoleStatus(1L)).willReturn(UserRole.RoleStatus.ACTIVE);

        // when
        TeacherDetailResponse result = teacherService.updateTeacher(1L, updateRequest);

        // then
        verify(teacherClassRepository).deleteAll(existingClasses);
        verify(teacherClassRepository, times(2)).save(any(TeacherClass.class));
    }

    @Test
    @DisplayName("교사 정보 수정 - 존재하지 않는 교사")
    void updateTeacher_NotFound() {
        // given
        TeacherUpdateRequest updateRequest = new TeacherUpdateRequest();
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> teacherService.updateTeacher(999L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("교사를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("교사 삭제 - 성공 (역할 비활성화)")
    void deleteTeacher_Success() {
        // given
        List<String> roles = Arrays.asList(RoleConstants.TEACHER);
        List<TeacherClass> teacherClasses = Arrays.asList(teacherClass1, teacherClass2);

        given(userRepository.findById(1L)).willReturn(Optional.of(teacherUser));
        given(userRoleService.getActiveRoles(1L)).willReturn(roles);
        given(teacherClassRepository.findByTeacherId(1L)).willReturn(teacherClasses);

        // when
        teacherService.deleteTeacher(1L);

        // then
        verify(userRoleService).suspendRole(1L, RoleConstants.TEACHER);
        verify(teacherClassRepository).deleteAll(teacherClasses);
    }

    @Test
    @DisplayName("교사 삭제 - 존재하지 않는 교사")
    void deleteTeacher_NotFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> teacherService.deleteTeacher(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("교사를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("교사 삭제 - 교사 역할이 없는 사용자")
    void deleteTeacher_NotTeacher() {
        // given
        List<String> roles = Arrays.asList("STUDENT");
        given(userRepository.findById(1L)).willReturn(Optional.of(teacherUser));
        given(userRoleService.getActiveRoles(1L)).willReturn(roles);

        // when & then
        assertThatThrownBy(() -> teacherService.deleteTeacher(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("해당 사용자는 교사가 아닙니다");
    }
}
