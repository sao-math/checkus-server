package saomath.checkusserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;

@Entity
@Table(name = "student_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
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

    // Constructor for easier creation
    public StudentProfile(User user, StudentStatus status, School school, Integer grade, Gender gender) {
        this.user = user;
        this.userId = user.getId();
        this.status = status;
        this.school = school;
        this.grade = grade;
        this.gender = gender;
    }

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
