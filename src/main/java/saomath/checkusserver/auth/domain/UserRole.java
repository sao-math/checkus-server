package saomath.checkusserver.auth.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;

@Entity
@Table(name = "user_role")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {
    
    @EmbeddedId
    private UserRoleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoleStatus status = RoleStatus.PENDING;

    public enum RoleStatus {
        PENDING("승인 대기"),
        ACTIVE("활성화됨"),
        SUSPENDED("일시 정지"),
        EXPIRED("만료됨");

        private final String description;

        RoleStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRoleId implements java.io.Serializable {
        @Column(name = "user_id")
        private Long userId;

        @Column(name = "role_id")
        private Long roleId;
    }
}
