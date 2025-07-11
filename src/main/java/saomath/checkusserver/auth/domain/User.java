package saomath.checkusserver.auth.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

// Spring Boot 3.0 이후 사용
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 50)
    private String username;

    @Column(length = 255)
    private String name;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(length = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private String password;

    @Column(name = "discord_id", length = 100)
    private String discordId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 사용자가 삭제되었는지 확인
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 사용자를 논리적으로 삭제
     */
    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 사용자 삭제를 복구
     */
    public void restore() {
        this.deletedAt = null;
    }
}