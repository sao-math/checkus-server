package saomath.checkusserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saomath.checkusserver.notification.NotificationSetting;

import java.util.List;
import java.util.Optional;

/**
 * 알림 설정 Repository
 */
@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    
    /**
     * 특정 사용자의 모든 알림 설정 조회
     */
    List<NotificationSetting> findByUserId(Long userId);
    
    /**
     * 특정 사용자의 활성화된 알림 설정만 조회
     */
    List<NotificationSetting> findByUserIdAndIsEnabledTrue(Long userId);
    
    /**
     * 특정 사용자의 특정 템플릿에 대한 알림 설정 조회
     */
    List<NotificationSetting> findByUserIdAndTemplateName(Long userId, String templateName);
    
    /**
     * 특정 사용자의 특정 템플릿에 대한 활성화된 알림 설정 조회
     */
    List<NotificationSetting> findByUserIdAndTemplateNameAndIsEnabledTrue(Long userId, String templateName);
    
    /**
     * 특정 사용자의 특정 템플릿, 특정 채널에 대한 설정 조회
     */
    Optional<NotificationSetting> findByUserIdAndTemplateNameAndDeliveryMethod(
            Long userId, String templateName, String deliveryMethod);
    
    /**
     * 특정 템플릿을 사용하는 모든 활성화된 설정 조회 (전체 발송용)
     */
    @Query("SELECT ns FROM NotificationSetting ns WHERE ns.templateName = :templateName AND ns.isEnabled = true")
    List<NotificationSetting> findActiveSettingsByTemplate(@Param("templateName") String templateName);
    
    /**
     * 특정 채널을 사용하는 모든 활성화된 설정 조회
     */
    List<NotificationSetting> findByDeliveryMethodAndIsEnabledTrue(String deliveryMethod);
    
    /**
     * 사용자의 모든 알림 설정 삭제
     */
    void deleteByUserId(Long userId);
    
    /**
     * 사용자의 특정 템플릿 알림 설정 삭제
     */
    void deleteByUserIdAndTemplateName(Long userId, String templateName);
}
