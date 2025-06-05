package saomath.checkusserver.entity;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "activity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "is_study_assignable")
    @Builder.Default
    private Boolean isStudyAssignable = false;
}
