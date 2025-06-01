package saomath.checkusserver.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import saomath.checkusserver.entity.Activity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("ActivityRepository 테스트")
class ActivityRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ActivityRepository activityRepository;

    @Test
    @DisplayName("공부 배정 가능한 활동만 조회 테스트")
    void findByIsStudyAssignableTrue_Success() {
        // Given
        Activity studyActivity1 = Activity.builder()
                .name("수학 공부")
                .isStudyAssignable(true)
                .build();

        Activity studyActivity2 = Activity.builder()
                .name("영어 공부")
                .isStudyAssignable(true)
                .build();

        Activity nonStudyActivity = Activity.builder()
                .name("휴식")
                .isStudyAssignable(false)
                .build();

        entityManager.persistAndFlush(studyActivity1);
        entityManager.persistAndFlush(studyActivity2);
        entityManager.persistAndFlush(nonStudyActivity);

        // When
        List<Activity> result = activityRepository.findByIsStudyAssignableTrue();

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(Activity::getIsStudyAssignable));
        assertTrue(result.stream().anyMatch(a -> a.getName().equals("수학 공부")));
        assertTrue(result.stream().anyMatch(a -> a.getName().equals("영어 공부")));
        assertFalse(result.stream().anyMatch(a -> a.getName().equals("휴식")));
    }

    @Test
    @DisplayName("활동명 존재 여부 확인 테스트")
    void existsByName_Success() {
        // Given
        Activity activity = Activity.builder()
                .name("과학 실험")
                .isStudyAssignable(true)
                .build();
        entityManager.persistAndFlush(activity);

        // When & Then
        assertTrue(activityRepository.existsByName("과학 실험"));
        assertFalse(activityRepository.existsByName("존재하지 않는 활동"));
    }

    @Test
    @DisplayName("빈 결과 테스트 - 공부 배정 가능한 활동이 없는 경우")
    void findByIsStudyAssignableTrue_EmptyResult() {
        // Given
        Activity nonStudyActivity1 = Activity.builder()
                .name("휴식")
                .isStudyAssignable(false)
                .build();

        Activity nonStudyActivity2 = Activity.builder()
                .name("간식")
                .isStudyAssignable(false)
                .build();

        entityManager.persistAndFlush(nonStudyActivity1);
        entityManager.persistAndFlush(nonStudyActivity2);

        // When
        List<Activity> result = activityRepository.findByIsStudyAssignableTrue();

        // Then
        assertEquals(0, result.size());
    }
}
