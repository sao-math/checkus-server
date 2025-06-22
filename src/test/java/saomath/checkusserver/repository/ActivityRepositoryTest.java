package saomath.checkusserver.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.studyTime.domain.Activity;
import saomath.checkusserver.studyTime.repository.ActivityRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ActivityRepository 테스트")
class ActivityRepositoryTest {

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

        activityRepository.save(studyActivity1);
        activityRepository.save(studyActivity2);
        activityRepository.save(nonStudyActivity);

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
        activityRepository.save(activity);

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

        activityRepository.save(nonStudyActivity1);
        activityRepository.save(nonStudyActivity2);

        // When
        List<Activity> result = activityRepository.findByIsStudyAssignableTrue();

        // Then
        assertEquals(0, result.size());
    }
}
