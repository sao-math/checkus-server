package saomath.checkusserver.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.studyTime.domain.Activity;
import saomath.checkusserver.studyTime.domain.ActualStudyTime;
import saomath.checkusserver.studyTime.domain.AssignedStudyTime;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.studyTime.repository.ActivityRepository;
import saomath.checkusserver.studyTime.repository.ActualStudyTimeRepository;
import saomath.checkusserver.studyTime.repository.AssignedStudyTimeRepository;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.studyTime.service.StudyTimeService;
import saomath.checkusserver.util.TestDataFactory;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class StudyTimeServiceConnectionTest {

    @Autowired
    private StudyTimeService studyTimeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActualStudyTimeRepository actualStudyTimeRepository;

    @Autowired
    private AssignedStudyTimeRepository assignedStudyTimeRepository;

    @Test
    @DisplayName("할당 시점에 기존 접속중인 세션이 자동으로 연결되지 않아야 한다")
    void shouldNotAutoConnectExistingSessionsWhenAssigning() {
        // Given
        User student = TestDataFactory.createStudent("student1", "학생1", "01012345678");
        User teacher = TestDataFactory.createTeacher("teacher1", "선생님1", "01087654321");
        student = userRepository.save(student);
        teacher = userRepository.save(teacher);

        Activity activity = Activity.builder()
                .name("수학 공부")
                .isStudyAssignable(true)
                .build();
        activity = activityRepository.save(activity);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionStart1 = now.plusMinutes(10); // 10분 후 시작
        LocalDateTime sessionEnd1 = now.plusMinutes(20);   // 20분 후 종료
        LocalDateTime sessionStart2 = now.plusMinutes(30); // 30분 후 시작, 현재 진행중

        // 학생이 먼저 접속 (2개 세션)
        ActualStudyTime session1 = ActualStudyTime.builder()
                .studentId(student.getId())
                .startTime(sessionStart1)
                .endTime(sessionEnd1)
                .source("discord")
                .assignedStudyTimeId(null) // 아직 할당되지 않음
                .build();

        ActualStudyTime session2 = ActualStudyTime.builder()
                .studentId(student.getId())
                .startTime(sessionStart2)
                .endTime(null) // 현재 진행중
                .source("discord")
                .assignedStudyTimeId(null) // 아직 할당되지 않음
                .build();

        actualStudyTimeRepository.save(session1);
        actualStudyTimeRepository.save(session2);

        // When: 선생님이 해당 시간대에 공부시간 할당
        LocalDateTime assignedStart = now.plusMinutes(5); // 세션들을 포함하는 범위
        LocalDateTime assignedEnd = now.plusMinutes(65);

        AssignedStudyTime assigned = studyTimeService.assignStudyTime(
                student.getId(),
                "할당된 수학 공부",
                activity.getId(),
                assignedStart,
                assignedEnd,
                teacher.getId()
        );

        // Then: 새로운 로직에서는 기존 세션들이 자동으로 연결되지 않아야 함
        List<ActualStudyTime> connectedSessions = actualStudyTimeRepository
                .findByAssignedStudyTimeId(assigned.getId());

        assertThat(connectedSessions).hasSize(0); // 자동 연결되지 않음
        
        // 원본 세션들은 여전히 할당되지 않은 상태여야 함
        ActualStudyTime updatedSession1 = actualStudyTimeRepository.findById(session1.getId()).orElseThrow();
        ActualStudyTime updatedSession2 = actualStudyTimeRepository.findById(session2.getId()).orElseThrow();
        
        assertThat(updatedSession1.getAssignedStudyTimeId()).isNull(); // 연결되지 않음
        assertThat(updatedSession2.getAssignedStudyTimeId()).isNull(); // 연결되지 않음
    }

    @Test
    @DisplayName("할당 범위 밖의 세션은 연결되지 않아야 한다")
    void shouldNotConnectSessionsOutsideAssignedRange() {
        // Given
        User student = TestDataFactory.createStudent("student2", "학생2", "01012345679");
        User teacher = TestDataFactory.createTeacher("teacher2", "선생님2", "01087654322");
        student = userRepository.save(student);
        teacher = userRepository.save(teacher);

        Activity activity = Activity.builder()
                .name("영어 공부")
                .isStudyAssignable(true)
                .build();
        activity = activityRepository.save(activity);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionStart = now.plusMinutes(10); // 할당 범위 밖

        // 할당 범위 밖에서 접속
        ActualStudyTime outsideSession = ActualStudyTime.builder()
                .studentId(student.getId())
                .startTime(sessionStart)
                .endTime(now.plusMinutes(20))
                .source("discord")
                .assignedStudyTimeId(null)
                .build();

        actualStudyTimeRepository.save(outsideSession);

        // When: 나중 시간대에 공부시간 할당
        LocalDateTime assignedStart = now.plusMinutes(40);
        LocalDateTime assignedEnd = now.plusMinutes(80);

        AssignedStudyTime assigned = studyTimeService.assignStudyTime(
                student.getId(),
                "할당된 영어 공부",
                activity.getId(),
                assignedStart,
                assignedEnd,
                teacher.getId()
        );

        // Then: 범위 밖 세션은 연결되지 않아야 함
        List<ActualStudyTime> connectedSessions = actualStudyTimeRepository
                .findByAssignedStudyTimeId(assigned.getId());

        assertThat(connectedSessions).isEmpty();
        
        // 원본 세션은 여전히 할당되지 않은 상태
        ActualStudyTime stillUnassigned = actualStudyTimeRepository.findById(outsideSession.getId()).orElseThrow();
        assertThat(stillUnassigned.getAssignedStudyTimeId()).isNull();
    }

    @Test
    @DisplayName("connectSessionOnStart 메서드가 직접 호출되어도 올바르게 동작해야 한다")
    void shouldConnectSessionsWhenDirectlyCallingConnectMethod() {
        // Given
        User student = TestDataFactory.createStudent("student3", "학생3", "01012345680");
        User teacher = TestDataFactory.createTeacher("teacher3", "선생님3", "01087654323");
        student = userRepository.save(student);
        teacher = userRepository.save(teacher);

        Activity activity = Activity.builder()
                .name("과학 공부")
                .isStudyAssignable(true)
                .build();
        activity = activityRepository.save(activity);

        LocalDateTime now = LocalDateTime.now();

        // 먼저 할당 생성 (자동 연결 없이)
        AssignedStudyTime assigned = AssignedStudyTime.builder()
                .studentId(student.getId())
                .title("과학 실험")
                .activityId(activity.getId())
                .startTime(now.plusMinutes(10))
                .endTime(now.plusMinutes(70))
                .assignedBy(teacher.getId())
                .build();
        assigned = assignedStudyTimeRepository.save(assigned);

        // 기존 세션 생성
        ActualStudyTime session = ActualStudyTime.builder()
                .studentId(student.getId())
                .startTime(now.plusMinutes(20)) // 할당 범위 내
                .endTime(null) // 진행중
                .source("discord")
                .assignedStudyTimeId(null)
                .build();
        session = actualStudyTimeRepository.save(session);

        // When: 직접 연결 메서드 호출
        ActualStudyTime connectedSession = studyTimeService.connectSessionOnStart(assigned.getId());

        // Then
        assertThat(connectedSession).isNotNull();
        assertThat(connectedSession.getId()).isEqualTo(session.getId());
        assertThat(connectedSession.getAssignedStudyTimeId()).isEqualTo(assigned.getId());
    }

    @Test
    @DisplayName("연결할 세션이 없을 때 null을 반환해야 한다")
    void shouldReturnNullWhenNoSessionsToConnect() {
        // Given
        User student = TestDataFactory.createStudent("student4", "학생4", "01012345681");
        User teacher = TestDataFactory.createTeacher("teacher4", "선생님4", "01087654324");
        student = userRepository.save(student);
        teacher = userRepository.save(teacher);

        Activity activity = Activity.builder()
                .name("음악 공부")
                .isStudyAssignable(true)
                .build();
        activity = activityRepository.save(activity);

        LocalDateTime now = LocalDateTime.now();

        // 할당만 생성 (기존 세션 없음)
        AssignedStudyTime assigned = AssignedStudyTime.builder()
                .studentId(student.getId())
                .title("음악 이론")
                .activityId(activity.getId())
                .startTime(now.plusMinutes(10))
                .endTime(now.plusMinutes(70))
                .assignedBy(teacher.getId())
                .build();
        assigned = assignedStudyTimeRepository.save(assigned);

        // When: 연결 시도
        ActualStudyTime connectedSession = studyTimeService.connectSessionOnStart(assigned.getId());

        // Then
        assertThat(connectedSession).isNull();
    }
}
