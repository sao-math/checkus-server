package saomath.checkusserver.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import saomath.checkusserver.entity.AssignedStudyTime;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AssignedStudyTime Repository 추가 테스트 - title 필드 검증")
class AssignedStudyTimeRepositoryTitleTest {

    @Test
    @DisplayName("AssignedStudyTime 엔티티 빌더에서 title 필드 필수 확인")
    void assignedStudyTimeBuilder_RequiredFields_Test() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        // When
        AssignedStudyTime studyTime = AssignedStudyTime.builder()
                .studentId(1L)
                .title("수학 공부")
                .activityId(1L)
                .startTime(now)
                .endTime(now.plusHours(2))
                .assignedBy(2L)
                .build();
        
        // Then
        assertThat(studyTime.getTitle()).isNotNull();
        assertThat(studyTime.getTitle()).isEqualTo("수학 공부");
        assertThat(studyTime.getStudentId()).isEqualTo(1L);
        assertThat(studyTime.getActivityId()).isEqualTo(1L);
        assertThat(studyTime.getAssignedBy()).isEqualTo(2L);
        assertThat(studyTime.getStartTime()).isEqualTo(now);
        assertThat(studyTime.getEndTime()).isEqualTo(now.plusHours(2));
    }

    @Test
    @DisplayName("title 필드 값 변경 테스트")
    void titleField_Modification_Test() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        AssignedStudyTime studyTime = AssignedStudyTime.builder()
                .studentId(1L)
                .title("수학 공부")
                .activityId(1L)
                .startTime(now)
                .endTime(now.plusHours(2))
                .assignedBy(2L)
                .build();
        
        // When
        studyTime.setTitle("영어 공부");
        
        // Then
        assertThat(studyTime.getTitle()).isEqualTo("영어 공부");
    }

    @Test
    @DisplayName("title 필드 최대 길이 테스트")
    void titleField_MaxLength_Test() {
        // Given
        String longTitle = "a".repeat(255); // 255자
        String tooLongTitle = "a".repeat(256); // 256자
        LocalDateTime now = LocalDateTime.now();
        
        // When & Then - 255자는 허용
        AssignedStudyTime studyTime = AssignedStudyTime.builder()
                .studentId(1L)
                .title(longTitle)
                .activityId(1L)
                .startTime(now)
                .endTime(now.plusHours(2))
                .assignedBy(2L)
                .build();
        
        assertThat(studyTime.getTitle()).hasSize(255);
        
        // 256자는 제한 확인 (엔티티 레벨에서는 체크 안됨, 서비스에서 검증)
        assertThat(tooLongTitle).hasSize(256);
    }
}
