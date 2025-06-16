package saomath.checkusserver.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.entity.NotificationSetting;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.repository.NotificationSettingRepository;
import saomath.checkusserver.repository.UserRepository;

import java.util.List;
import java.util.Optional;

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
    private NotificationSettingRepository notificationSettingRepository;
    
    @Autowired
    private NotificationPreferenceService notificationPreferenceService;
    
    @Test
    public void testBasicNotificationSettingCRUD() {
        // Given: 테스트 사용자 생성
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setName("테스트사용자");
        testUser.setPhoneNumber("010-1234-5678");
        testUser.setDiscordId("123456789012345678");
        testUser.setPassword("password");
        userRepository.save(testUser);
        
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
        testUser.setUsername("testuser2");
        testUser.setName("테스트사용자2");
        testUser.setPhoneNumber("010-9876-5432");
        testUser.setDiscordId("987654321098765432");
        testUser.setPassword("password");
        userRepository.save(testUser);
        
        // When: DB에 설정이 없는 상태에서 알림 설정 조회
        List<NotificationPreference> preferences = notificationPreferenceService
            .getUserPreferences(testUser.getId(), AlimtalkTemplate.STUDY_START.name());
        
        // Then: 기본적으로 알림톡과 디스코드 모두 활성화되어야 함
        assertThat(preferences).hasSizeGreaterThanOrEqualTo(1);
        
        // 전화번호가 있으므로 알림톡 설정이 있어야 함
        boolean hasAlimtalk = preferences.stream()
            .anyMatch(p -> p.getRecipient().equals(testUser.getPhoneNumber()));
        assertThat(hasAlimtalk).isTrue();
    }
    
    @Test
    public void testNotificationPreferenceServiceWithDatabaseSettings() {
        // Given: 테스트 사용자 생성
        User testUser = new User();
        testUser.setUsername("testuser3");
        testUser.setName("테스트사용자3");
        testUser.setPhoneNumber("010-1111-2222");
        testUser.setDiscordId("111111111111111111");
        testUser.setPassword("password");
        userRepository.save(testUser);
        
        // DB에 알림 설정 추가 (알림톡만 활성화)
        NotificationSetting alimtalkSetting = new NotificationSetting();
        alimtalkSetting.setUserId(testUser.getId());
        alimtalkSetting.setTemplateName(AlimtalkTemplate.STUDY_START.name());
        alimtalkSetting.setDeliveryMethod("alimtalk");
        alimtalkSetting.setIsEnabled(true);
        notificationSettingRepository.save(alimtalkSetting);
        
        // When: 알림 설정 조회
        List<NotificationPreference> preferences = notificationPreferenceService
            .getUserPreferences(testUser.getId(), AlimtalkTemplate.STUDY_START.name());
        
        // Then: DB 설정이 반영되어 알림톡만 활성화되어야 함
        assertThat(preferences).hasSize(1);
        assertThat(preferences.get(0).getRecipient()).isEqualTo(testUser.getPhoneNumber());
    }
    
    @Test
    public void testFindSpecificNotificationSetting() {
        // Given: 테스트 사용자 및 설정 생성
        User testUser = new User();
        testUser.setUsername("testuser4");
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
}
