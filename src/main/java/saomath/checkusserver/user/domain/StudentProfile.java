package saomath.checkusserver.user.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import saomath.checkusserver.auth.domain.User;

@Entity
@Table(name = "student_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @MapsId
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StudentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    private Integer grade;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    public enum StudentStatus {
        INQUIRY("문의"),
        COUNSELING_SCHEDULED("상담예약"),
        ENROLLED("재원"),
        WAITING("대기"),
        WITHDRAWN("퇴원"),
        UNREGISTERED("미등록");

        private final String description;

        StudentStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum Gender {
        MALE("남"),
        FEMALE("여"),
        OTHER("기타");

        private final String description;

        Gender(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
