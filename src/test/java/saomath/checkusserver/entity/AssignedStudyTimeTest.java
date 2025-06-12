package saomath.checkusserver.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AssignedStudyTime 엔티티 테스트")
class AssignedStudyTimeTest {

    @Test
    @DisplayName("AssignedStudyTime 빌더 테스트 - title 필드 포함")
    void assignedStudyTimeBuilder_WithTitle_Success() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        // When
        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .title("수학 공부")
                .studentId(1L)
                .activityId(1L)
                .startTime(now)
                .endTime(now.plusHours(2))
                .assignedBy(2L)
                .build();
        
        // Then
        assertThat(assignedStudyTime.getTitle()).isEqualTo("수학 공부");
        assertThat(assignedStudyTime.getStudentId()).isEqualTo(1L);
        assertThat(assignedStudyTime.getActivityId()).isEqualTo(1L);
        assertThat(assignedStudyTime.getStartTime()).isEqualTo(now);
        assertThat(assignedStudyTime.getEndTime()).isEqualTo(now.plusHours(2));
        assertThat(assignedStudyTime.getAssignedBy()).isEqualTo(2L);
    }

    @Test
    @DisplayName("AssignedStudyTime equals 및 hashCode 테스트")
    void assignedStudyTimeEqualsAndHashCode_Test() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        AssignedStudyTime assignedStudyTime1 = AssignedStudyTime.builder()
                .id(1L)
                .title("수학 공부")
                .studentId(1L)
                .activityId(1L)
                .startTime(now)
                .endTime(now.plusHours(2))
                .assignedBy(2L)
                .build();
        
        AssignedStudyTime assignedStudyTime2 = AssignedStudyTime.builder()
                .id(1L)
                .title("수학 공부")
                .studentId(1L)
                .activityId(1L)
                .startTime(now)
                .endTime(now.plusHours(2))
                .assignedBy(2L)
                .build();
        
        // Then
        assertThat(assignedStudyTime1).isEqualTo(assignedStudyTime2);
        assertThat(assignedStudyTime1.hashCode()).isEqualTo(assignedStudyTime2.hashCode());
    }

    @Test
    @DisplayName("AssignedStudyTime toString 테스트")
    void assignedStudyTimeToString_Test() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .id(1L)
                .title("수학 공부")
                .studentId(1L)
                .activityId(1L)
                .startTime(now)
                .endTime(now.plusHours(2))
                .assignedBy(2L)
                .build();
        
        // When
        String result = assignedStudyTime.toString();
        
        // Then
        assertThat(result).contains("수학 공부");
        assertThat(result).contains("AssignedStudyTime");
    }
}
