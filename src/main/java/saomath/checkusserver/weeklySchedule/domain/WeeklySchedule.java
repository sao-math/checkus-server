package saomath.checkusserver.weeklySchedule.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.studyTime.domain.Activity;

import java.time.LocalTime;

@Entity
@Table(name = "weekly_schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 1=월요일, 2=화요일, ..., 7=일요일

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // 연관 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", insertable = false, updatable = false)
    private Activity activity;

    // 요일 Enum 정의
    public enum DayOfWeek {
        MONDAY(1, "월요일"),
        TUESDAY(2, "화요일"),
        WEDNESDAY(3, "수요일"),
        THURSDAY(4, "목요일"),
        FRIDAY(5, "금요일"),
        SATURDAY(6, "토요일"),
        SUNDAY(7, "일요일");

        private final int value;
        private final String korean;

        DayOfWeek(int value, String korean) {
            this.value = value;
            this.korean = korean;
        }

        public int getValue() {
            return value;
        }

        public String getKorean() {
            return korean;
        }

        public static DayOfWeek fromValue(int value) {
            for (DayOfWeek day : values()) {
                if (day.value == value) {
                    return day;
                }
            }
            throw new IllegalArgumentException("Invalid day of week value: " + value);
        }
    }
}
