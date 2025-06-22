package saomath.checkusserver.notification.service;

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
import saomath.checkusserver.notification.domain.NotificationSetting;
import saomath.checkusserver.notification.repository.NotificationSettingRepository;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.domain.DefaultNotificationSetting;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 기본값 + 예외 저장 방식 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DefaultNotificationSettingTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserRoleRepository userRoleRepository;
    
    @Autowired
    private NotificationSettingRepository notificationSettingRepository;
    
    @Autowired
    private NotificationPreferenceService notificationPreferenceService;
    
    private String generateUniqueUsername() {
        return "testuser_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    @Test
    public void testDefaultNotificationSettingEnum() {
        // Given: 학생 기본 설정 확인
        DefaultNotificationSetting studentSetting = DefaultNotificationSetting.STUDENT;
        
        // When: 학습 알림에 대한 기본 채널 조회
        Set<String> studyReminderChannels = studentSetting.getDefaultChannels(AlimtalkTemplate.STUDY_REMINDER_10MIN);
        
        // Then: 노션 설정에 따라 알림톡 + 디스코드 모두 활성화
        assertThat(studyReminderChannels).contains("alimtalk", "discord");
        
        // When: 미입장 알림 확인
        Set<String> noShowChannels = studentSetting.getDefaultChannels(AlimtalkTemplate.NO_SHOW);
        
        // Then: 노션 설정에 따라 알림톡 + 디스코드 모두 활성화
        assertThat(noShowChannels).contains("alimtalk", "discord");
        
        //설정바뀌어 삭제
//        // When: 공부 시작 알림 확인
//        Set<String> studyStartChannels = studentSetting.getDefaultChannels(AlimtalkTemplate.STUDY_START);
//        
//        // Then: 디스코드만 활성화 (카톡은 OFF 고정)
//        assertThat(studyStartChannels).contains("discord");
//        assertThat(studyStartChannels).doesNotContain("alimtalk");
        
        // When: 학부모 기본 설정 확인
        DefaultNotificationSetting guardianSetting = DefaultNotificationSetting.GUARDIAN;
        Set<String> guardianChannels = guardianSetting.getDefaultChannels(AlimtalkTemplate.STUDY_REMINDER_10MIN);
        
        // Then: 학부모는 모든 알림이 기본 비활성화
        assertThat(guardianChannels).isEmpty();
    }
    
    @Test
    public void testStudentDefaultBehavior() {
        // Given: 학생 사용자 생성
        User student = createTestUser();
        Role studentRole = createOrGetRole("STUDENT");
        createUserRole(student, studentRole, UserRole.RoleStatus.ACTIVE);
        
        // When: DB에 예외 설정 없이 알림 설정 조회
        List<NotificationPreference> preferences = notificationPreferenceService
            .getUserPreferences(student.getId(), AlimtalkTemplate.STUDY_REMINDER_10MIN.name());
        
        // Then: 기본값에 따라 알림톡 + 디스코드 모두 활성화
        assertThat(preferences).hasSize(2);
        
        boolean hasAlimtalk = preferences.stream()
            .anyMatch(p -> p.getChannel() == NotificationService.NotificationChannel.ALIMTALK);
        boolean hasDiscord = preferences.stream()
            .anyMatch(p -> p.getChannel() == NotificationService.NotificationChannel.DISCORD);
        
        assertThat(hasAlimtalk).isTrue();
        assertThat(hasDiscord).isTrue();
    }
    
    @Test
    public void testGuardianDefaultBehavior() {
        // Given: 학부모 사용자 생성
        User guardian = createTestUser();
        Role guardianRole = createOrGetRole("GUARDIAN");
        createUserRole(guardian, guardianRole, UserRole.RoleStatus.ACTIVE);
        
        // When: 미입장 알림 설정 조회
        List<NotificationPreference> preferences = notificationPreferenceService
            .getUserPreferences(guardian.getId(), AlimtalkTemplate.NO_SHOW.name());
        
        // Then: 학부모는 기본적으로 모든 알림이 비활성화
        assertThat(preferences).isEmpty();
    }
    
    @Test
    public void testExceptionOverrideDefault() {
        // Given: 학생 사용자 생성
        User student = createTestUser();
        Role studentRole = createOrGetRole("STUDENT");
        createUserRole(student, studentRole, UserRole.RoleStatus.ACTIVE);
        
        // 예외 설정: 학습 알림을 알림톡만 받도록 설정 (기본값은 알림톡+디스코드)
        NotificationSetting exceptionSetting = new NotificationSetting();
        exceptionSetting.setUserId(student.getId());
        exceptionSetting.setTemplateName(AlimtalkTemplate.STUDY_REMINDER_10MIN.name());
        exceptionSetting.setDeliveryMethod("discord");
        exceptionSetting.setIsEnabled(false); // 디스코드 비활성화
        notificationSettingRepository.save(exceptionSetting);
        
        // When: 알림 설정 조회
        List<NotificationPreference> preferences = notificationPreferenceService
            .getUserPreferences(student.getId(), AlimtalkTemplate.STUDY_REMINDER_10MIN.name());
        
        // Then: 알림톡만 활성화되어야 함 (디스코드는 예외 설정으로 비활성화)
        assertThat(preferences).hasSize(1);
        assertThat(preferences.get(0).getChannel()).isEqualTo(NotificationService.NotificationChannel.ALIMTALK);
    }
    
    @Test
    public void testMultipleRolePriority() {
        // Given: 학생이면서 선생님인 사용자 (학생 우선순위가 높음)
        User user = createTestUser();
        Role studentRole = createOrGetRole("STUDENT");
        Role teacherRole = createOrGetRole("TEACHER");
        createUserRole(user, studentRole, UserRole.RoleStatus.ACTIVE);
        createUserRole(user, teacherRole, UserRole.RoleStatus.ACTIVE);
        
        // When: 학습 알림 설정 조회
        List<NotificationPreference> preferences = notificationPreferenceService
            .getUserPreferences(user.getId(), AlimtalkTemplate.STUDY_REMINDER_10MIN.name());
        
        // Then: 학생 기본값이 적용되어야 함 (알림톡 + 디스코드)
        assertThat(preferences).hasSize(2);
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
