package saomath.checkusserver.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.entity.NotificationSetting;
import saomath.checkusserver.entity.Role;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.entity.UserRole;
import saomath.checkusserver.exception.BusinessException;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.dto.NotificationSettingGroupDto;
import saomath.checkusserver.notification.dto.NotificationSettingUpdateDto;
import saomath.checkusserver.repository.NotificationSettingRepository;
import saomath.checkusserver.repository.UserRepository;
import saomath.checkusserver.repository.UserRoleRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPreferenceService 통합 테스트")
class NotificationPreferenceServiceIntegrationTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserRoleRepository userRoleRepository;
    
    @Mock
    private NotificationSettingRepository notificationSettingRepository;
    
    @InjectMocks
    private NotificationPreferenceServiceImpl notificationPreferenceService;
    
    private User testUser;
    private UserRole studentRole;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .name("테스트학생")
            .phoneNumber("01012345678")
            .discordId("testuser#1234")
            .build();
            
        Role role = new Role();
        role.setId(1L);
        role.setName("STUDENT");
            
        UserRole.UserRoleId userRoleId = new UserRole.UserRoleId(1L, 1L);
        studentRole = new UserRole();
        studentRole.setId(userRoleId);
        studentRole.setRole(role);
        studentRole.setStatus(UserRole.RoleStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("그룹화된 알림 설정 조회 - 기본값만 사용하는 경우")
    void getGroupedNotificationSettings_OnlyDefaults() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRoleRepository.findByUserIdAndStatus(1L, UserRole.RoleStatus.ACTIVE))
            .thenReturn(List.of(studentRole));
        when(notificationSettingRepository.findByUserId(1L))
            .thenReturn(List.of()); // 예외 설정 없음
        
        // when
        List<NotificationSettingGroupDto> result = notificationPreferenceService.getGroupedNotificationSettings(1L);
        
        // then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(AlimtalkTemplate.values().length);
        
        // STUDY_REMINDER_10MIN 템플릿 확인 (학생 기본값: alimtalk + discord 활성화)
        NotificationSettingGroupDto studyReminder = result.stream()
            .filter(group -> group.getNotificationType().getId().equals("STUDY_REMINDER_10MIN"))
            .findFirst()
            .orElseThrow();
            
        assertThat(studyReminder.getDeliveryMethods()).containsKey("alimtalk");
        assertThat(studyReminder.getDeliveryMethods()).containsKey("discord");
        assertThat(studyReminder.getDeliveryMethods().get("alimtalk").isEnabled()).isTrue();
        assertThat(studyReminder.getDeliveryMethods().get("discord").isEnabled()).isTrue();
    }
    
    @Test
    @DisplayName("그룹화된 알림 설정 조회 - 예외 설정이 있는 경우")
    void getGroupedNotificationSettings_WithExceptions() {
        // given
        NotificationSetting exceptionSetting = NotificationSetting.builder()
            .id(1L)
            .userId(1L)
            .templateName("STUDY_REMINDER_10MIN")
            .deliveryMethod("alimtalk")
            .isEnabled(false) // 기본값과 다르게 비활성화
            .advanceMinutes(5)
            .build();
            
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRoleRepository.findByUserIdAndStatus(1L, UserRole.RoleStatus.ACTIVE))
            .thenReturn(List.of(studentRole));
        when(notificationSettingRepository.findByUserId(1L))
            .thenReturn(List.of(exceptionSetting));
        
        // when
        List<NotificationSettingGroupDto> result = notificationPreferenceService.getGroupedNotificationSettings(1L);
        
        // then
        NotificationSettingGroupDto studyReminder = result.stream()
            .filter(group -> group.getNotificationType().getId().equals("STUDY_REMINDER_10MIN"))
            .findFirst()
            .orElseThrow();
            
        // alimtalk는 예외 설정으로 비활성화, discord는 기본값으로 활성화
        assertThat(studyReminder.getDeliveryMethods().get("alimtalk").isEnabled()).isFalse();
        assertThat(studyReminder.getDeliveryMethods().get("discord").isEnabled()).isTrue();
        //assertThat(studyReminder.getDeliveryMethods().get("alimtalk").getAdvanceMinutes()).isEqualTo(5);
    }
    
    @Test
    @DisplayName("알림 설정 업데이트 - 기존 설정 수정")
    void updateNotificationSetting_ExistingSetting() {
        // given
        NotificationSetting existingSetting = NotificationSetting.builder()
            .id(1L)
            .userId(1L)
            .templateName("STUDY_START")
            .deliveryMethod("alimtalk")
            .isEnabled(true)
            .advanceMinutes(0)
            .build();
            
        NotificationSettingUpdateDto updateDto = new NotificationSettingUpdateDto();
        updateDto.setEnabled(false);
        updateDto.setAdvanceMinutes(10);
        
        when(notificationSettingRepository.findByUserIdAndTemplateNameAndDeliveryMethod(1L, "STUDY_START", "alimtalk"))
            .thenReturn(Optional.of(existingSetting));
        when(notificationSettingRepository.save(any(NotificationSetting.class)))
            .thenReturn(existingSetting);
        
        // when
        notificationPreferenceService.updateNotificationSetting(1L, "STUDY_START", "alimtalk", updateDto);
        
        // then
        assertThat(existingSetting.getIsEnabled()).isFalse();
        assertThat(existingSetting.getAdvanceMinutes()).isEqualTo(10);
        verify(notificationSettingRepository).save(existingSetting);
    }
    
    @Test
    @DisplayName("채널명 표준화 테스트 - kakao를 alimtalk으로 변환")
    void updateNotificationSetting_ChannelStandardization() {
        // given
        NotificationSettingUpdateDto updateDto = new NotificationSettingUpdateDto();
        updateDto.setEnabled(true);
        
        when(notificationSettingRepository.findByUserIdAndTemplateNameAndDeliveryMethod(1L, "STUDY_START", "alimtalk"))
            .thenReturn(Optional.empty());
        when(notificationSettingRepository.save(any(NotificationSetting.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // when - "kakao"로 요청하지만 내부적으로 "alimtalk"으로 변환되어야 함
        notificationPreferenceService.updateNotificationSetting(1L, "STUDY_START", "kakao", updateDto);
        
        // then - alimtalk으로 조회했는지 확인 (내부적으로 hasNotificationSetting도 호출되므로 2번 호출됨)
        verify(notificationSettingRepository, times(2)).findByUserIdAndTemplateNameAndDeliveryMethod(1L, "STUDY_START", "alimtalk");
    }
    
    @Test
    @DisplayName("알림 설정 존재 여부 확인")
    void hasNotificationSetting() {
        // given
        NotificationSetting setting = NotificationSetting.builder()
            .id(1L)
            .userId(1L)
            .templateName("STUDY_START")
            .deliveryMethod("alimtalk")
            .build();
            
        when(notificationSettingRepository.findByUserIdAndTemplateNameAndDeliveryMethod(1L, "STUDY_START", "alimtalk"))
            .thenReturn(Optional.of(setting));
        when(notificationSettingRepository.findByUserIdAndTemplateNameAndDeliveryMethod(1L, "STUDY_START", "discord"))
            .thenReturn(Optional.empty());
        
        // when & then
        assertThat(notificationPreferenceService.hasNotificationSetting(1L, "STUDY_START", "alimtalk")).isTrue();
        assertThat(notificationPreferenceService.hasNotificationSetting(1L, "STUDY_START", "discord")).isFalse();
    }
}
