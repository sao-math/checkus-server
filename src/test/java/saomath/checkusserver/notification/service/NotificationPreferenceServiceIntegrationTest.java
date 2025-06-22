package saomath.checkusserver.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.notification.NotificationSetting;
import saomath.checkusserver.auth.domain.Role;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.domain.UserRole;
import saomath.checkusserver.common.exception.BusinessException;
import saomath.checkusserver.notification.domain.AlimtalkTemplate;
import saomath.checkusserver.notification.dto.NotificationSettingGroupDto;
import saomath.checkusserver.notification.dto.NotificationSettingUpdateDto;
import saomath.checkusserver.repository.NotificationSettingRepository;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.auth.repository.UserRoleRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

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
            .templateName("TODAY_TASKS") // 변경 가능한 템플릿 사용
            .deliveryMethod("discord") // 변경 가능한 채널 사용
            .isEnabled(true)
            .advanceMinutes(0)
            .build();
            
        NotificationSettingUpdateDto updateDto = new NotificationSettingUpdateDto();
        updateDto.setEnabled(false);
        updateDto.setAdvanceMinutes(10);
        
        when(userRoleRepository.findByUserIdAndStatus(1L, UserRole.RoleStatus.ACTIVE))
            .thenReturn(List.of(studentRole)); // 학생 역할 설정
        when(notificationSettingRepository.findByUserIdAndTemplateNameAndDeliveryMethod(1L, "TODAY_TASKS", "discord"))
            .thenReturn(Optional.of(existingSetting));
        when(notificationSettingRepository.save(any(NotificationSetting.class)))
            .thenReturn(existingSetting);
        
        // when
        notificationPreferenceService.updateNotificationSetting(1L, "TODAY_TASKS", "discord", updateDto);
        
        // then
        assertThat(existingSetting.getIsEnabled()).isFalse();
        assertThat(existingSetting.getAdvanceMinutes()).isEqualTo(10);
        verify(notificationSettingRepository).save(existingSetting);
    }
    
    @Test
    @DisplayName("채널명 표준화 테스트 - 변경 가능한 설정 사용")
    void updateNotificationSetting_ChannelStandardization() {
        // given
        NotificationSettingUpdateDto updateDto = new NotificationSettingUpdateDto();
        updateDto.setEnabled(false); // TODAY_TASKS의 discord는 변경 가능하므로 비활성화 가능
        
        when(userRoleRepository.findByUserIdAndStatus(1L, UserRole.RoleStatus.ACTIVE))
            .thenReturn(List.of(studentRole)); // 학생 역할 설정
        when(notificationSettingRepository.findByUserIdAndTemplateNameAndDeliveryMethod(
            eq(1L), eq("TODAY_TASKS"), eq("discord")))
            .thenReturn(Optional.empty());
        when(notificationSettingRepository.save(any(NotificationSetting.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // when - discord 설정 변경 (변경 가능)
        notificationPreferenceService.updateNotificationSetting(1L, "TODAY_TASKS", "discord", updateDto);
        
        // then - 정상적으로 저장되어야 함
        ArgumentCaptor<NotificationSetting> captor = ArgumentCaptor.forClass(NotificationSetting.class);
        verify(notificationSettingRepository).save(captor.capture());
        
        NotificationSetting saved = captor.getValue();
        assertThat(saved.getDeliveryMethod()).isEqualTo("discord");
        assertThat(saved.getIsEnabled()).isFalse();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getTemplateName()).isEqualTo("TODAY_TASKS");
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
    
    @Test
    @DisplayName("변경 불가능한 설정 변경 시도 시 예외 발생")
    void updateNotificationSetting_ReadOnlySettingThrowsException() {
        // given
        NotificationSettingUpdateDto updateDto = new NotificationSettingUpdateDto();
        updateDto.setEnabled(false); // STUDY_REMINDER_10MIN의 alimtalk은 ON 고정이므로 변경 불가
        
        when(userRoleRepository.findByUserIdAndStatus(1L, UserRole.RoleStatus.ACTIVE))
            .thenReturn(List.of(studentRole)); // 학생 역할 설정
        
        // when & then
        assertThatThrownBy(() -> {
            notificationPreferenceService.updateNotificationSetting(1L, "STUDY_REMINDER_10MIN", "alimtalk", updateDto);
        })
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("변경할 수 없습니다")
        .hasMessageContaining("카카오톡")
        .hasMessageContaining("고정: 활성화");
        
        // 저장 메소드가 호출되지 않았는지 확인
        verify(notificationSettingRepository, never()).save(any(NotificationSetting.class));
    }
    
    @Test
    @DisplayName("그룹화된 알림 설정에 changeable 필드 포함 확인")
    void getGroupedNotificationSettings_IncludesChangeableField() {
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
        
        // STUDY_REMINDER_10MIN 템플릿 확인 (변경 불가능)
        NotificationSettingGroupDto studyReminder = result.stream()
            .filter(group -> group.getNotificationType().getId().equals("STUDY_REMINDER_10MIN"))
            .findFirst()
            .orElseThrow();
            
        // alimtalk과 discord 모두 변경 불가능해야 함
        assertThat(studyReminder.getDeliveryMethods().get("alimtalk").isChangeable()).isFalse();
        assertThat(studyReminder.getDeliveryMethods().get("discord").isChangeable()).isFalse();
        
        // TODAY_TASKS 템플릿 확인 (일부 변경 가능)
        NotificationSettingGroupDto todayTasks = result.stream()
            .filter(group -> group.getNotificationType().getId().equals("TODAY_TASKS"))
            .findFirst()
            .orElseThrow();
            
        // alimtalk은 변경 불가, discord는 변경 가능
        assertThat(todayTasks.getDeliveryMethods().get("alimtalk").isChangeable()).isFalse();
        assertThat(todayTasks.getDeliveryMethods().get("discord").isChangeable()).isTrue();
    }
    
    @Test
    @DisplayName("학부모 역할은 모든 설정이 변경 가능")
    void getGroupedNotificationSettings_GuardianCanChangeAll() {
        // given - 학부모 역할 설정
        Role guardianRole = new Role();
        guardianRole.setId(2L);
        guardianRole.setName("GUARDIAN");
        
        UserRole.UserRoleId guardianRoleId = new UserRole.UserRoleId(1L, 2L);
        UserRole userGuardianRole = new UserRole();
        userGuardianRole.setId(guardianRoleId);
        userGuardianRole.setRole(guardianRole);
        userGuardianRole.setStatus(UserRole.RoleStatus.ACTIVE);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRoleRepository.findByUserIdAndStatus(1L, UserRole.RoleStatus.ACTIVE))
            .thenReturn(List.of(userGuardianRole)); // 학부모 역할
        when(notificationSettingRepository.findByUserId(1L))
            .thenReturn(List.of()); // 예외 설정 없음
        
        // when
        List<NotificationSettingGroupDto> result = notificationPreferenceService.getGroupedNotificationSettings(1L);
        
        // then
        assertThat(result).isNotEmpty();
        
        // STUDY_REMINDER_10MIN 템플릿 확인 (학부모는 제한 없음)
        NotificationSettingGroupDto studyReminder = result.stream()
            .filter(group -> group.getNotificationType().getId().equals("STUDY_REMINDER_10MIN"))
            .findFirst()
            .orElseThrow();
            
        // 학부모는 모든 설정을 변경할 수 있어야 함
        assertThat(studyReminder.getDeliveryMethods().get("alimtalk").isChangeable()).isTrue();
        assertThat(studyReminder.getDeliveryMethods().get("discord").isChangeable()).isTrue();
    }
}
