package saomath.checkusserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;

@Entity
@Table(name = "teacher_class")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherClass {
    @EmbeddedId
    private TeacherClassId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teacherId")
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("classId")
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherClassId {
        @Column(name = "teacher_id")
        private Long teacherId;

        @Column(name = "class_id")
        private Long classId;
    }
}
