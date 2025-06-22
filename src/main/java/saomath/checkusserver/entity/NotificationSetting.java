package saomath.checkusserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import saomath.checkusserver.auth.domain.User;

/**
 * 사용자별 알림 설정 Entity
 * 
 * 사용자가 어떤 알림을 어떤 채널로 받을지 설정
 * - 알림 유형별로 활성화/비활성화 가능
 * - 채널별로 설정 가능 (알림톡, 디스코드 등)
 * - 알림 시점 조정 가능 (몇 분 전 알림)
 */
@Entity
@Table(name = "notification_setting", uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_notification_setting", 
        columnNames = {"user_id", "template_name", "delivery_method"}
    )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSetting {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 사용자 ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 알림 템플릿 이름 (AlimtalkTemplate enum name)
     * 예: "STUDY_REMINDER_10MIN", "STUDY_START", "NO_SHOW" 등
     */
    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;
    
    /**
     * 전송 방법/채널
     * "alimtalk", "discord", "email", "sms" 등
     */
    @Column(name = "delivery_method", nullable = false, length = 50)
    private String deliveryMethod;
    
    /**
     * 알림 활성화 여부
     */
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;
    
    /**
     * 몇 분 전에 알림을 보낼지 (기본 0분)
     * 예: 10분 전 알림의 경우 10
     */
    @Column(name = "advance_minutes")
    @Builder.Default
    private Integer advanceMinutes = 0;
    
    /**
     * 사용자 엔티티와의 관계
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
