package saomath.checkusserver.study.domain;

import lombok.*;
import jakarta.persistence.*;
import saomath.checkusserver.auth.domain.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "assigned_study_time")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignedStudyTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "assigned_by", nullable = false)
    private Long assignedBy;

    // 연관 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", insertable = false, updatable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", insertable = false, updatable = false)
    private User assignedByUser;
}
