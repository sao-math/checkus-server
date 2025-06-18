package saomath.checkusserver.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.entity.*;
import saomath.checkusserver.exception.BusinessException;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.dto.NotificationSettingUpdateDto;
import saomath.checkusserver.repository.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 변경 불가능한 알림 설정에 대한 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NotificationPreferenceReadOnlyTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserRoleRepository userRoleRepository;
    
    @Autowired
    private NotificationPreferenceService notificationPreferenceService;
    
    private String generateUniqueUsername() {
        return "testuser_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    @Test
    public void testStudentCannotChangeReadOnlySettings() {
        // Given: 학생 사용자 생성
        User student = createTestUser();
        Role studentRole = createOrGetRole("STUDENT");
        createUserRole(student, studentRole, UserRole.RoleStatus.ACTIVE);
        
        // When & Then: 공부시작 10분전 알림의 카카오톡 설정을 OFF로 변경 시도 (변경불가 - ON 고정)
        NotificationSettingUpdateDto updateDto = new NotificationSettingUpdateDto();
        updateDto.setEnabled(false); // 기본값 true를 false로 변경 시도
        
        assertThatThrownBy(() -> {
            notificationPreferenceService.updateNotificationSetting(
                student.getId(), 
                AlimtalkTemplate.STUDY_REMINDER_10MIN.name(),
                "alimtalk",
                updateDto
            );
        })
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("변경할 수 없습니다")
        .hasMessageContaining("카카오톡")
        .hasMessageContaining("고정: 활성화");
    }
    
    @Test
    public void testStudentCannotChangeReadOnlyDiscordSettings() {
        // Given: 학생 사용자 생성
        User student = createTestUser();
        Role studentRole = createOrGetRole("STUDENT");
        createUserRole(student, studentRole, UserRole.RoleStatus.ACTIVE);
        
        // When & Then: 미입장 알림의 디스코드 설정을 OFF로 변경 시도 (변경불가 - ON 고정)
        NotificationSettingUpdateDto updateDto = new NotificationSettingUpdateDto();
        updateDto.setEnabled(false); // 기본값 true를 false로 변경 시도
        
        assertThatThrownBy(() -> {
            notificationPreferenceService.updateNotificationSetting(
                student.getId(), 
                AlimtalkTemplate.NO_SHOW.name(),
                "discord",
                updateDto
            );
        })
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("변경할 수 없습니다")
        .hasMessageContaining("디스코드");
    }
    
    @Test
    public void testStudentCannotEnableOffFixedSettings() {
        // Given: 학생 사용자 생성
        User student = createTestUser();
        Role studentRole = createOrGetRole("STUDENT");
        createUserRole(student, studentRole, UserRole.RoleStatus.ACTIVE);
        
        // When & Then: 공부시작 알림의 카카오톡 설정을 ON으로 변경 시도 (변경불가 - OFF 고정)
        NotificationSettingUpdateDto updateDto = new NotificationSettingUpdateDto();
        updateDto.setEnabled(true); // 기본값 false를 true로 변경 시도
        
        assertThatThrownBy(() -> {
            notificationPreferenceService.updateNotificationSetting(
                student.getId(), 
                AlimtalkTemplate.STUDY_START.name(),
                "alimtalk",
                updateDto
            );
        })
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("변경할 수 없습니다")
        .hasMessageContaining("카카오톡")
        .hasMessageContaining("고정: 비활성화");
    }
    
    @Test
    public void testStudentCanChangeFlexibleSettings() {
        // Given: 학생 사용자 생성
        User student = createTestUser();
        Role studentRole = createOrGetRole("STUDENT");
        createUserRole(student, studentRole, UserRole.RoleStatus.ACTIVE);
        
        // When & Then: 학습 알림(아침)의 디스코드 설정 변경 (변경가능)
        NotificationSettingUpdateDto updateDto = new NotificationSettingUpdateDto();
        updateDto.setEnabled(false); // 기본값 true를 false로 변경
        
        // 예외가 발생하지 않아야 함
        notificationPreferenceService.updateNotificationSetting(
            student.getId(), 
            AlimtalkTemplate.TODAY_TASKS.name(),
            "discord",
            updateDto
        );
        
        // 성공적으로 변경되었는지 확인 (실제 저장 여부는 다른 테스트에서 검증)
        assertThat(true).isTrue(); // 예외가 발생하지 않으면 성공
    }
    
    @Test
    public void testStudentCanKeepReadOnlySettingsAsDefault() {
        // Given: 학생 사용자 생성
        User student = createTestUser();
        Role studentRole = createOrGetRole("STUDENT");
        createUserRole(student, studentRole, UserRole.RoleStatus.ACTIVE);
        
        // When & Then: 변경 불가능한 설정을 기본값 그대로 유지하는 것은 허용
        NotificationSettingUpdateDto updateDto = new NotificationSettingUpdateDto();
        updateDto.setEnabled(true); // 기본값과 동일한 값으로 설정
        
        // 예외가 발생하지 않아야 함
        notificationPreferenceService.updateNotificationSetting(
            student.getId(), 
            AlimtalkTemplate.STUDY_REMINDER_10MIN.name(),
            "alimtalk",
            updateDto
        );
        
        assertThat(true).isTrue(); // 예외가 발생하지 않으면 성공
    }
    
    @Test
    public void testGuardianCanChangeAnySettings() {
        // Given: 학부모 사용자 생성
        User guardian = createTestUser();
        Role guardianRole = createOrGetRole("GUARDIAN");
        createUserRole(guardian, guardianRole, UserRole.RoleStatus.ACTIVE);
        
        // When & Then: 학부모는 모든 설정을 변경할 수 있음 (읽기 전용 제약 없음)
        NotificationSettingUpdateDto updateDto = new NotificationSettingUpdateDto();
        updateDto.setEnabled(true); // 기본값 false를 true로 변경
        
        // 예외가 발생하지 않아야 함
        notificationPreferenceService.updateNotificationSetting(
            guardian.getId(), 
            AlimtalkTemplate.STUDY_REMINDER_10MIN.name(),
            "alimtalk",
            updateDto
        );
        
        assertThat(true).isTrue(); // 예외가 발생하지 않으면 성공
    }
    
    @Test
    public void testTeacherCanChangeAnySettings() {
        // Given: 선생님 사용자 생성
        User teacher = createTestUser();
        Role teacherRole = createOrGetRole("TEACHER");
        createUserRole(teacher, teacherRole, UserRole.RoleStatus.ACTIVE);
        
        // When & Then: 선생님도 모든 설정을 변경할 수 있음 (읽기 전용 제약 없음)
        NotificationSettingUpdateDto updateDto = new NotificationSettingUpdateDto();
        updateDto.setEnabled(true); // 기본값 false를 true로 변경
        
        // 예외가 발생하지 않아야 함
        notificationPreferenceService.updateNotificationSetting(
            teacher.getId(), 
            AlimtalkTemplate.STUDY_REMINDER_10MIN.name(),
            "alimtalk",
            updateDto
        );
        
        assertThat(true).isTrue(); // 예외가 발생하지 않으면 성공
    }
    
    @Test
    public void testInvalidTemplateIdThrowsException() {
        // Given: 학생 사용자 생성
        User student = createTestUser();
        Role studentRole = createOrGetRole("STUDENT");
        createUserRole(student, studentRole, UserRole.RoleStatus.ACTIVE);
        
        // When & Then: 존재하지 않는 템플릿 ID로 업데이트 시도
        NotificationSettingUpdateDto updateDto = new NotificationSettingUpdateDto();
        updateDto.setEnabled(true);
        
        assertThatThrownBy(() -> {
            notificationPreferenceService.updateNotificationSetting(
                student.getId(), 
                "INVALID_TEMPLATE_ID",
                "alimtalk",
                updateDto
            );
        })
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("지원하지 않는 알림 유형입니다");
    }
    
    // Helper methods
    private User createTestUser() {
        User user = new User();
        user.setUsername(generateUniqueUsername());
        user.setName("테스트사용자");
        user.setPhoneNumber("010-1234-5678");
        user.setDiscordId("123456789012345678");
        user.setPassword("password");
        return userRepository.save(user);
    }
    
    private Role createOrGetRole(String roleName) {
        return roleRepository.findByName(roleName)
            .orElseGet(() -> {
                Role role = new Role();
                role.setName(roleName);
                return roleRepository.save(role);
            });
    }
    
    private void createUserRole(User user, Role role, UserRole.RoleStatus status) {
        UserRole userRole = new UserRole();
        UserRole.UserRoleId id = new UserRole.UserRoleId();
        id.setUserId(user.getId());
        id.setRoleId(role.getId());
        userRole.setId(id);
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setStatus(status);
        userRoleRepository.save(userRole);
    }
}
