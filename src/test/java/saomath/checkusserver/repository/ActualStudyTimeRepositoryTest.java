package saomath.checkusserver.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import saomath.checkusserver.entity.Activity;
import saomath.checkusserver.entity.ActualStudyTime;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("ActualStudyTimeRepository 테스트")
class ActualStudyTimeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ActualStudyTimeRepository actualStudyTimeRepository;

    @Test
    @DisplayName("배정 시간별 실제 접속 기록 조회 테스트")
    void findByAssignedStudyTimeId_Success() {
        // Given
        User student = User.builder()
                .username("student1")
                .name("학생1")
                .phoneNumber("010-1111-1111")
                .password("password")
                .build();
        entityManager.persistAndFlush(student);

        User teacher = User.builder()
                .username("teacher1")
                .name("선생님1")
                .phoneNumber("010-2222-2222")
                .password("password")
                .build();
        entityManager.persistAndFlush(teacher);

        Activity activity = Activity.builder()
                .name("수학 공부")
                .isStudyAssignable(true)
                .build();
        entityManager.persistAndFlush(activity);

        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .studentId(student.getId())
                .activityId(activity.getId())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(2))
                .assignedBy(teacher.getId())
                .build();
        entityManager.persistAndFlush(assignedStudyTime);

        ActualStudyTime actualStudyTime1 = ActualStudyTime.builder()
                .studentId(student.getId())
                .assignedStudyTimeId(assignedStudyTime.getId())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusMinutes(30))
                .source("discord")
                .build();

        ActualStudyTime actualStudyTime2 = ActualStudyTime.builder()
                .studentId(student.getId())
                .assignedStudyTimeId(assignedStudyTime.getId())
                .startTime(LocalDateTime.now().plusMinutes(40))
                .endTime(LocalDateTime.now().plusHours(1))
                .source("discord")
                .build();

        entityManager.persistAndFlush(actualStudyTime1);
        entityManager.persistAndFlush(actualStudyTime2);

        // When
        List<ActualStudyTime> result = actualStudyTimeRepository
                .findByAssignedStudyTimeId(assignedStudyTime.getId());

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(ast -> 
                ast.getAssignedStudyTimeId().equals(assignedStudyTime.getId())));
    }

    @Test
    @DisplayName("학생별 기간별 실제 공부 시간 조회 테스트")
    void findByStudentIdAndDateRange_Success() {
        // Given
        User student = User.builder()
                .username("student1")
                .name("학생1")
                .phoneNumber("010-1111-1111")
                .password("password")
                .build();
        entityManager.persistAndFlush(student);

        LocalDateTime now = LocalDateTime.now();

        ActualStudyTime studyTime1 = ActualStudyTime.builder()
                .studentId(student.getId())
                .startTime(now.plusHours(1))
                .endTime(now.plusHours(2))
                .source("discord")
                .build();

        ActualStudyTime studyTime2 = ActualStudyTime.builder()
                .studentId(student.getId())
                .startTime(now.plusDays(1))
                .endTime(now.plusDays(1).plusHours(1))
                .source("discord")
                .build();

        ActualStudyTime studyTime3 = ActualStudyTime.builder()
                .studentId(student.getId())
                .startTime(now.plusDays(5)) // 범위 밖
                .endTime(now.plusDays(5).plusHours(1))
                .source("discord")
                .build();

        entityManager.persistAndFlush(studyTime1);
        entityManager.persistAndFlush(studyTime2);
        entityManager.persistAndFlush(studyTime3);

        // When
        List<ActualStudyTime> result = actualStudyTimeRepository
                .findByStudentIdAndDateRange(
                        student.getId(),
                        now,
                        now.plusDays(3)
                );

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(ast -> ast.getStudentId().equals(student.getId())));
        assertTrue(result.stream().allMatch(ast -> 
                ast.getStartTime().isBefore(now.plusDays(3))));
    }

    @Test
    @DisplayName("할당 시간 밖 접속 기록 조회 테스트")
    void findByStudentIdAndAssignedStudyTimeIdIsNull_Success() {
        // Given
        User student = User.builder()
                .username("student1")
                .name("학생1")
                .phoneNumber("010-1111-1111")
                .password("password")
                .build();
        entityManager.persistAndFlush(student);

        User teacher = User.builder()
                .username("teacher1")
                .name("선생님1")
                .phoneNumber("010-2222-2222")
                .password("password")
                .build();
        entityManager.persistAndFlush(teacher);

        Activity activity = Activity.builder()
                .name("수학 공부")
                .isStudyAssignable(true)
                .build();
        entityManager.persistAndFlush(activity);

        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .studentId(student.getId())
                .activityId(activity.getId())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(2))
                .assignedBy(teacher.getId())
                .build();
        entityManager.persistAndFlush(assignedStudyTime);

        // 할당된 시간의 접속
        ActualStudyTime assignedStudy = ActualStudyTime.builder()
                .studentId(student.getId())
                .assignedStudyTimeId(assignedStudyTime.getId())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .source("discord")
                .build();

        // 할당되지 않은 시간의 접속
        ActualStudyTime unassignedStudy = ActualStudyTime.builder()
                .studentId(student.getId())
                .assignedStudyTimeId(null)
                .startTime(LocalDateTime.now().plusHours(3))
                .endTime(LocalDateTime.now().plusHours(4))
                .source("discord")
                .build();

        entityManager.persistAndFlush(assignedStudy);
        entityManager.persistAndFlush(unassignedStudy);

        // When
        List<ActualStudyTime> result = actualStudyTimeRepository
                .findByStudentIdAndAssignedStudyTimeIdIsNull(student.getId());

        // Then
        assertEquals(1, result.size());
        assertEquals(unassignedStudy.getId(), result.get(0).getId());
        assertNull(result.get(0).getAssignedStudyTimeId());
    }
}
