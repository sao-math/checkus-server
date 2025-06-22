package saomath.checkusserver.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.study.domain.Activity;
import saomath.checkusserver.study.domain.AssignedStudyTime;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.study.repository.ActivityRepository;
import saomath.checkusserver.study.repository.AssignedStudyTimeRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AssignedStudyTimeRepository 테스트")
class AssignedStudyTimeRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private AssignedStudyTimeRepository assignedStudyTimeRepository;

    @Test
    @DisplayName("학생별 기간별 배정 시간 조회 테스트")
    void findByStudentIdAndStartTimeBetween_Success() {
        // Given
        User student = User.builder()
                .username("student1")
                .name("학생1")
                .phoneNumber("010-1111-1111")
                .password("password")
                .build();
        userRepository.save(student);

        User teacher = User.builder()
                .username("teacher1")
                .name("선생님1")
                .phoneNumber("010-2222-2222")
                .password("password")
                .build();
        userRepository.save(teacher);

        Activity activity = Activity.builder()
                .name("수학 공부")
                .isStudyAssignable(true)
                .build();
        activityRepository.save(activity);

        LocalDateTime now = LocalDateTime.now();
        AssignedStudyTime studyTime1 = AssignedStudyTime.builder()
                .studentId(student.getId())
                .title("수학 공부")
                .activityId(activity.getId())
                .startTime(now.plusHours(1))
                .endTime(now.plusHours(3))
                .assignedBy(teacher.getId())
                .build();

        AssignedStudyTime studyTime2 = AssignedStudyTime.builder()
                .studentId(student.getId())
                .title("영어 공부")
                .activityId(activity.getId())
                .startTime(now.plusDays(1))
                .endTime(now.plusDays(1).plusHours(2))
                .assignedBy(teacher.getId())
                .build();

        assignedStudyTimeRepository.save(studyTime1);
        assignedStudyTimeRepository.save(studyTime2);

        // When
        List<AssignedStudyTime> result = assignedStudyTimeRepository
                .findByStudentIdAndStartTimeBetween(
                        student.getId(),
                        now,
                        now.plusDays(2)
                );

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(ast -> ast.getStudentId().equals(student.getId())));
    }

    @Test
    @DisplayName("시간 겹침 체크 테스트")
    void findOverlappingStudyTimes_Success() {
        // Given
        User student = User.builder()
                .username("student2")
                .name("학생2")
                .phoneNumber("010-1111-1111")
                .password("password")
                .build();
        userRepository.save(student);

        User teacher = User.builder()
                .username("teacher2")
                .name("선생님2")
                .phoneNumber("010-2222-2222")
                .password("password")
                .build();
        userRepository.save(teacher);

        Activity activity = Activity.builder()
                .name("수학 공부")
                .isStudyAssignable(true)
                .build();
        activityRepository.save(activity);

        LocalDateTime baseTime = LocalDateTime.now().plusHours(1);
        
        // 10:00 ~ 12:00 기존 배정
        AssignedStudyTime existing = AssignedStudyTime.builder()
                .studentId(student.getId())
                .title("수학 공부")
                .activityId(activity.getId())
                .startTime(baseTime)
                .endTime(baseTime.plusHours(2))
                .assignedBy(teacher.getId())
                .build();
        assignedStudyTimeRepository.save(existing);

        // When - 11:00 ~ 13:00 새로운 배정 시도 (겹침)
        List<AssignedStudyTime> overlapping = assignedStudyTimeRepository
                .findOverlappingStudyTimes(
                        student.getId(),
                        baseTime.plusHours(1),
                        baseTime.plusHours(3)
                );

        // Then
        assertEquals(1, overlapping.size());
        assertEquals(existing.getId(), overlapping.get(0).getId());
    }

    @Test
    @DisplayName("시간 겹침 없음 테스트")
    void findOverlappingStudyTimes_NoOverlap() {
        // Given
        User student = User.builder()
                .username("student3")
                .name("학생3")
                .phoneNumber("010-1111-1111")
                .password("password")
                .build();
        userRepository.save(student);

        User teacher = User.builder()
                .username("teacher3")
                .name("선생님3")
                .phoneNumber("010-2222-2222")
                .password("password")
                .build();
        userRepository.save(teacher);

        Activity activity = Activity.builder()
                .name("수학 공부")
                .isStudyAssignable(true)
                .build();
        activityRepository.save(activity);

        LocalDateTime baseTime = LocalDateTime.now().plusHours(1);
        
        // 10:00 ~ 12:00 기존 배정
        AssignedStudyTime existing = AssignedStudyTime.builder()
                .studentId(student.getId())
                .title("수학 공부")
                .activityId(activity.getId())
                .startTime(baseTime)
                .endTime(baseTime.plusHours(2))
                .assignedBy(teacher.getId())
                .build();
        assignedStudyTimeRepository.save(existing);

        // When - 13:00 ~ 15:00 새로운 배정 시도 (겹치지 않음)
        List<AssignedStudyTime> overlapping = assignedStudyTimeRepository
                .findOverlappingStudyTimes(
                        student.getId(),
                        baseTime.plusHours(3),
                        baseTime.plusHours(5)
                );

        // Then
        assertEquals(0, overlapping.size());
    }

    @Test
    @DisplayName("곧 시작할 공부 시간 조회 테스트")
    void findUpcomingStudyTimes_Success() {
        // Given
        User student = User.builder()
                .username("student4")
                .name("학생4")
                .phoneNumber("010-1111-1111")
                .password("password")
                .build();
        userRepository.save(student);

        User teacher = User.builder()
                .username("teacher4")
                .name("선생님4")
                .phoneNumber("010-2222-2222")
                .password("password")
                .build();
        userRepository.save(teacher);

        Activity activity = Activity.builder()
                .name("수학 공부")
                .isStudyAssignable(true)
                .build();
        activityRepository.save(activity);

        LocalDateTime now = LocalDateTime.now();
        
        // 5분 후 시작 (10분 전 알림 범위)
        AssignedStudyTime upcomingIn5Min = AssignedStudyTime.builder()
                .studentId(student.getId())
                .title("수학 공부")
                .activityId(activity.getId())
                .startTime(now.plusMinutes(5))
                .endTime(now.plusMinutes(65))
                .assignedBy(teacher.getId())
                .build();

        // 지금 시작
        AssignedStudyTime startingNow = AssignedStudyTime.builder()
                .studentId(student.getId())
                .title("영어 공부")
                .activityId(activity.getId())
                .startTime(now)
                .endTime(now.plusHours(1))
                .assignedBy(teacher.getId())
                .build();

        // 1시간 후 시작 (범위 밖)
        AssignedStudyTime futureStudy = AssignedStudyTime.builder()
                .studentId(student.getId())
                .title("과학 공부")
                .activityId(activity.getId())
                .startTime(now.plusHours(1))
                .endTime(now.plusHours(2))
                .assignedBy(teacher.getId())
                .build();

        assignedStudyTimeRepository.save(upcomingIn5Min);
        assignedStudyTimeRepository.save(startingNow);
        assignedStudyTimeRepository.save(futureStudy);

        // When
        List<AssignedStudyTime> upcoming = assignedStudyTimeRepository
                .findUpcomingStudyTimesV2(
                        now.minusMinutes(10),
                        now,
                        now.plusMinutes(10)  // 10분 후까지로 확장
                );

        // Then
        assertEquals(2, upcoming.size());
        assertTrue(upcoming.stream().anyMatch(ast -> ast.getId().equals(upcomingIn5Min.getId())));
        assertTrue(upcoming.stream().anyMatch(ast -> ast.getId().equals(startingNow.getId())));
    }
}
