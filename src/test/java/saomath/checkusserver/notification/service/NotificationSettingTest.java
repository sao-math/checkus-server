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
import saomath.checkusserver.notification.NotificationSetting;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.repository.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 알림 설정 기능 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NotificationSettingTest {
    
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
    
    /**
     * 고유한 username 생성 헬퍼 메서드
     */
    private String generateUniqueUsername() {
        return "testuser_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    @Test
    public void testBasicNotificationSettingCRUD() {
        // Given: 테스트 사용자 생성
        User testUser = new User();
        testUser.setUsername(generateUniqueUsername());
        testUser.setName("테스트사용자");
        testUser.setPhoneNumber("010-1234-5678");
        testUser.setDiscordId("123456789012345678");
        testUser.setPassword("password");
        userRepository.save(testUser);
        
        // 학생 역할 할당
        assignStudentRole(testUser);
        
        // When: 알림 설정 생성
        NotificationSetting setting = new NotificationSetting();
        setting.setUserId(testUser.getId());
        setting.setTemplateName(AlimtalkTemplate.STUDY_REMINDER_10MIN.name());
        setting.setDeliveryMethod("alimtalk");
        setting.setIsEnabled(true);
        setting.setAdvanceMinutes(10);
        notificationSettingRepository.save(setting);
        
        // Then: 조회 테스트
        List<NotificationSetting> userSettings = notificationSettingRepository.findByUserId(testUser.getId());
        assertThat(userSettings).hasSize(1);
        assertThat(userSettings.get(0).getTemplateName()).isEqualTo(AlimtalkTemplate.STUDY_REMINDER_10MIN.name());
        assertThat(userSettings.get(0).getDeliveryMethod()).isEqualTo("alimtalk");
    }
    
    @Test
    public void testNotificationPreferenceServiceDefaultBehavior() {
        // Given: 전화번호와 디스코드 ID가 있는 사용자
        User testUser = new User();
        testUser.setUsername(generateUniqueUsername());
        testUser.setName("테스트사용자2");
        testUser.setPhoneNumber("010-9876-5432");
        testUser.setDiscordId("987654321098765432");
        testUser.setPassword("password");
        userRepository.save(testUser);
        
        // When: DB에 설정이 없는 상태에서 STUDY_REMINDER_10MIN 알림 설정 조회 (학생 기본값: alimtalk + discord 모두 활성화)
        List<NotificationPreference> preferences = notificationPreferenceService
            .getUserPreferences(testUser.getId(), AlimtalkTemplate.STUDY_REMINDER_10MIN.name());
        
        // Then: 기본적으로 알림톡과 디스코드 모두 활성화되어야 함
        assertThat(preferences).hasSizeGreaterThanOrEqualTo(1);
        
        // 전화번호가 있으므로 알림톡 설정이 있어야 함
        boolean hasAlimtalk = preferences.stream()
            .anyMatch(p -> p.getRecipient().equals(testUser.getPhoneNumber()));
        assertThat(hasAlimtalk).isTrue();
        
        // 디스코드 ID가 있으므로 디스코드 설정도 있어야 함
        boolean hasDiscord = preferences.stream()
            .anyMatch(p -> p.getRecipient().equals(testUser.getDiscordId()));
        assertThat(hasDiscord).isTrue();
    }
    
    @Test
    public void testNotificationPreferenceServiceWithDatabaseSettings() {
        // Given: 테스트 사용자 생성 (디스코드 ID 없이)
        User testUser = new User();
        testUser.setUsername(generateUniqueUsername());
        testUser.setName("테스트사용자3");
        testUser.setPhoneNumber("010-1111-2222");
        testUser.setDiscordId(null); // 디스코드 ID 없음
        testUser.setPassword("password");
        userRepository.save(testUser);
        
        // DB에 알림 설정 추가 (TODAY_TASKS의 discord를 비활성화 - 예외 설정)
        NotificationSetting discordSetting = new NotificationSetting();
        discordSetting.setUserId(testUser.getId());
        discordSetting.setTemplateName(AlimtalkTemplate.TODAY_TASKS.name());
        discordSetting.setDeliveryMethod("discord");
        discordSetting.setIsEnabled(false); // 기본값은 true이지만 예외로 false로 설정
        notificationSettingRepository.save(discordSetting);
        
        // When: 알림 설정 조회
        List<NotificationPreference> preferences = notificationPreferenceService
            .getUserPreferences(testUser.getId(), AlimtalkTemplate.TODAY_TASKS.name());
        
        // Then: alimtalk만 활성화되어야 함 (discord는 예외 설정으로 비활성화, 디스코드 ID도 없음)
        assertThat(preferences).hasSize(1);
        assertThat(preferences.get(0).getRecipient()).isEqualTo(testUser.getPhoneNumber());
    }
    
    @Test
    public void testFindSpecificNotificationSetting() {
        // Given: 테스트 사용자 및 설정 생성
        User testUser = new User();
        testUser.setUsername(generateUniqueUsername());
        testUser.setName("테스트사용자4");
        testUser.setPhoneNumber("010-3333-4444");
        testUser.setPassword("password");
        userRepository.save(testUser);
        
        NotificationSetting setting = new NotificationSetting();
        setting.setUserId(testUser.getId());
        setting.setTemplateName(AlimtalkTemplate.NO_SHOW.name());
        setting.setDeliveryMethod("alimtalk");
        setting.setIsEnabled(false); // 비활성화
        notificationSettingRepository.save(setting);
        
        // When: 특정 설정 조회
        Optional<NotificationSetting> foundSetting = notificationSettingRepository
            .findByUserIdAndTemplateNameAndDeliveryMethod(
                testUser.getId(), 
                AlimtalkTemplate.NO_SHOW.name(), 
                "alimtalk"
            );
        
        // Then: 설정이 조회되고 비활성화 상태여야 함
        assertThat(foundSetting).isPresent();
        assertThat(foundSetting.get().getIsEnabled()).isFalse();
    }
    
    /**
     * 사용자에게 학생 역할을 할당하는 헬퍼 메소드
     */
    private void assignStudentRole(User user) {
        Role studentRole = roleRepository.findByName("STUDENT")
            .orElseGet(() -> {
                Role role = new Role();
                role.setName("STUDENT");
                return roleRepository.save(role);
            });
        
        UserRole userRole = new UserRole();
        UserRole.UserRoleId id = new UserRole.UserRoleId();
        id.setUserId(user.getId());
        id.setRoleId(studentRole.getId());
        userRole.setId(id);
        userRole.setUser(user);
        userRole.setRole(studentRole);
        userRole.setStatus(UserRole.RoleStatus.ACTIVE);
        userRoleRepository.save(userRole);
    }
}
