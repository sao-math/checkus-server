package saomath.checkusserver.user.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import saomath.checkusserver.auth.domain.User;

@Entity
@Table(name = "student_guardian")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGuardian {
    @EmbeddedId
    private StudentGuardianId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentId")
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("guardianId")
    @JoinColumn(name = "guardian_id")
    private User guardian;

    @Column(length = 20)
    private String relationship;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentGuardianId {
        @Column(name = "student_id")
        private Long studentId;

        @Column(name = "guardian_id")
        private Long guardianId;
    }
}
