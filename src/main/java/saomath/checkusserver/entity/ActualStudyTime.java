package saomath.checkusserver.entity;

import lombok.*;
import jakarta.persistence.*;
import saomath.checkusserver.auth.domain.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "actual_study_time")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualStudyTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "assigned_study_time_id")
    private Long assignedStudyTimeId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(length = 255)
    private String source; // 'discord', 'manual' 등

    // 연관 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_study_time_id", insertable = false, updatable = false)
    private AssignedStudyTime assignedStudyTime;
}
