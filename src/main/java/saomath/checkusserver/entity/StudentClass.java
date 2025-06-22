package saomath.checkusserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import saomath.checkusserver.auth.domain.User;

@Entity
@Table(name = "student_class")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentClass {
    @EmbeddedId
    private StudentClassId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentId")
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("classId")
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentClassId {
        @Column(name = "student_id")
        private Long studentId;

        @Column(name = "class_id")
        private Long classId;
    }
}
