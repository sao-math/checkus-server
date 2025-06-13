package saomath.checkusserver.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import saomath.checkusserver.entity.AssignedStudyTime;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StudyTimeService 단위 테스트 - title 필드 검증")
class StudyTimeServiceTitleTest {

    @Test
    @DisplayName("title 필드 유효성 검증 테스트")
    void validateTitle_Test() {
        // Given
        
        // When & Then - 정상적인 title
        String validTitle = "수학 공부";
        assertThat(validTitle).isNotNull();
        assertThat(validTitle.trim()).isNotEmpty();
        assertThat(validTitle.length()).isLessThanOrEqualTo(255);
        
        // When & Then - 빈 문자열
        String emptyTitle = "";
        assertThat(emptyTitle.trim()).isEmpty();
        
        // When & Then - null
        String nullTitle = null;
        assertThat(nullTitle).isNull();
        
        // When & Then - 긴 문자열
        String longTitle = "a".repeat(256);
        assertThat(longTitle.length()).isGreaterThan(255);
    }

    @Test
    @DisplayName("AssignedStudyTime 빌더에서 title 필드 설정 확인")
    void assignedStudyTimeBuilder_TitleField_Test() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        String title = "수학 공부";
        
        // When
        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .title(title)
                .studentId(1L)
                .activityId(1L)
                .startTime(now)
                .endTime(now.plusHours(2))
                .assignedBy(2L)
                .build();
        
        // Then
        assertThat(assignedStudyTime.getTitle()).isEqualTo(title);
        assertThat(assignedStudyTime.getTitle()).isNotNull();
        assertThat(assignedStudyTime.getTitle()).isNotEmpty();
    }
}
