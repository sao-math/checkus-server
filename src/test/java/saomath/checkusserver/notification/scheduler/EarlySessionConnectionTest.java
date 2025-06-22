package saomath.checkusserver.notification.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.studyTime.domain.Activity;
import saomath.checkusserver.studyTime.domain.ActualStudyTime;
import saomath.checkusserver.studyTime.domain.AssignedStudyTime;
import saomath.checkusserver.studyTime.repository.ActivityRepository;
import saomath.checkusserver.studyTime.repository.ActualStudyTimeRepository;
import saomath.checkusserver.studyTime.repository.AssignedStudyTimeRepository;
import saomath.checkusserver.studyTime.service.StudyTimeService;

import java.time.LocalDateTime;

import static java.time.LocalTime.now;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("세션 연결 로직 테스트")
class SessionConnectionTest {

    @Autowired
    private StudyTimeService studyTimeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private AssignedStudyTimeRepository assignedStudyTimeRepository;

    @Autowired
    private ActualStudyTimeRepository actualStudyTimeRepository;

    private User testStudent;
    private Activity testActivity;

    @BeforeEach
    void setUp() {
        // 테스트 학생 생성
        testStudent = User.builder()
                .username("test_student")
                .name("테스트학생")
                .phoneNumber("010-1234-5678")
                .password("password")
                .discordId("123456789")
                .build();
        testStudent = userRepository.save(testStudent);

        // 테스트 활동 생성
        testActivity = Activity.builder()
                .name("수학")
                .isStudyAssignable(true)
                .build();
        testActivity = activityRepository.save(testActivity);
    }

    @Test
    @DisplayName("할당 생성 시에는 자동 연결하지 않아야 함")
    void shouldNotAutoConnectWhenAssigningStudyTime() {
        // Given: 할당 시간 이전에 접속한 세션
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earlyConnectionTime = now.plusMinutes(10);
        ActualStudyTime earlySession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(earlyConnectionTime)
                .source("discord")
                .build();
        earlySession = actualStudyTimeRepository.save(earlySession);

        // When: 공부시간 할당 (새로운 로직에서는 자동 연결하지 않음)
        LocalDateTime assignedStartTime = now.plusMinutes(20);
        LocalDateTime assignedEndTime = now.plusMinutes(80);

        AssignedStudyTime assignedStudyTime = studyTimeService.assignStudyTime(
                testStudent.getId(),
                "수학 공부",
                testActivity.getId(),
                assignedStartTime,
                assignedEndTime,
                testStudent.getId()
        );

        // Then: 세션이 자동으로 연결되지 않아야 함
        ActualStudyTime updatedSession = actualStudyTimeRepository.findById(earlySession.getId()).orElse(null);
        assertThat(updatedSession).isNotNull();
        assertThat(updatedSession.getAssignedStudyTimeId()).isNull(); // 연결되지 않음
        assertThat(updatedSession.getStartTime()).isEqualTo(earlyConnectionTime);
        assertThat(updatedSession.getEndTime()).isNull(); // 아직 진행중
    }

    @Test
    @DisplayName("스케줄러에서 세션 시작 시 연결 처리를 할 수 있어야 함")
    void shouldConnectSessionOnStartInScheduler() {
        // Given: 학생이 먼저 접속하고 나중에 공부시간이 할당된 상황
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earlyConnectionTime = now.plusMinutes(10);
        ActualStudyTime earlySession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(earlyConnectionTime)
                .source("discord")
                .build();
        earlySession = actualStudyTimeRepository.save(earlySession);

        LocalDateTime assignedStartTime = now.plusMinutes(20);
        LocalDateTime assignedEndTime = now.plusMinutes(80);

        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(assignedStartTime)
                .endTime(assignedEndTime)
                .assignedBy(testStudent.getId())
                .build();
        assignedStudyTime = assignedStudyTimeRepository.save(assignedStudyTime);

        // When: 스케줄러에서 세션 시작 시 연결 처리
        ActualStudyTime connectedSession = studyTimeService.connectSessionOnStart(assignedStudyTime.getId());

        // Then: 세션이 성공적으로 연결되어야 함
        assertThat(connectedSession).isNotNull();
        assertThat(connectedSession.getId()).isEqualTo(earlySession.getId());
        assertThat(connectedSession.getAssignedStudyTimeId()).isEqualTo(assignedStudyTime.getId());
        assertThat(connectedSession.getStartTime()).isEqualTo(earlyConnectionTime);
    }

    @Test
    @DisplayName("기존 세션이 다른 할당에 연결된 경우 새 세션을 생성해야 함")
    void shouldCreateNewSessionWhenPreviousSessionIsAssigned() {
        // Given: 이미 다른 할당에 연결된 진행중인 세션
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earlyConnectionTime = now.plusMinutes(10);
        
        // 첫 번째 할당
        AssignedStudyTime firstAssignment = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("영어 공부")
                .activityId(testActivity.getId())
                .startTime(now.plusMinutes(5))
                .endTime(now.plusMinutes(35))
                .assignedBy(testStudent.getId())
                .build();
        firstAssignment = assignedStudyTimeRepository.save(firstAssignment);

        // 첫 번째 할당에 연결된 진행중인 세션
        ActualStudyTime existingSession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(earlyConnectionTime)
                .assignedStudyTimeId(firstAssignment.getId())
                .source("discord")
                .build();
        existingSession = actualStudyTimeRepository.save(existingSession);

        // 두 번째 할당
        LocalDateTime secondAssignmentStart = now.plusMinutes(35);
        AssignedStudyTime secondAssignment = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(secondAssignmentStart)
                .endTime(now.plusMinutes(95))
                .assignedBy(testStudent.getId())
                .build();
        secondAssignment = assignedStudyTimeRepository.save(secondAssignment);

        // When: 두 번째 할당 시작 시 연결 처리
        ActualStudyTime connectedSession = studyTimeService.connectSessionOnStart(secondAssignment.getId());

        // Then: 기존 세션이 종료되고 새 세션이 생성되어야 함
        assertThat(connectedSession).isNotNull();
        assertThat(connectedSession.getAssignedStudyTimeId()).isEqualTo(secondAssignment.getId());
        assertThat(connectedSession.getStartTime()).isEqualTo(secondAssignmentStart);
        
        // 기존 세션은 종료되어야 함
        ActualStudyTime updatedExistingSession = actualStudyTimeRepository.findById(existingSession.getId()).orElse(null);
        assertThat(updatedExistingSession).isNotNull();
        assertThat(updatedExistingSession.getEndTime()).isEqualTo(secondAssignmentStart);
        
        // 새 세션과 기존 세션은 다른 ID를 가져야 함
        assertThat(connectedSession.getId()).isNotEqualTo(existingSession.getId());
    }

    @Test
    @DisplayName("기존 세션이 미할당 상태인 경우 연결해야 함")
    void shouldConnectUnassignedOngoingSession() {
        // Given: 미할당 상태의 진행중인 세션
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earlyConnectionTime = now.plusMinutes(10);
        
        ActualStudyTime unassignedSession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(earlyConnectionTime)
                .source("discord")
                .build(); // assignedStudyTimeId는 null
        unassignedSession = actualStudyTimeRepository.save(unassignedSession);

        // 새로운 할당
        LocalDateTime assignedStartTime = now.plusMinutes(20);
        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(assignedStartTime)
                .endTime(now.plusMinutes(80))
                .assignedBy(testStudent.getId())
                .build();
        assignedStudyTime = assignedStudyTimeRepository.save(assignedStudyTime);

        // When: 할당 시작 시 연결 처리
        ActualStudyTime connectedSession = studyTimeService.connectSessionOnStart(assignedStudyTime.getId());

        // Then: 기존 세션이 새 할당에 연결되어야 함
        assertThat(connectedSession).isNotNull();
        assertThat(connectedSession.getId()).isEqualTo(unassignedSession.getId());
        assertThat(connectedSession.getAssignedStudyTimeId()).isEqualTo(assignedStudyTime.getId());
        assertThat(connectedSession.getStartTime()).isEqualTo(earlyConnectionTime);
        assertThat(connectedSession.getEndTime()).isNull(); // 여전히 진행중
    }

    @Test
    @DisplayName("진행중인 세션이 없으면 null을 반환해야 함")
    void shouldReturnNullWhenNoOngoingSession() {
        // Given: 진행중인 세션이 없는 상황
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime assignedStartTime = now.plusMinutes(20);
        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(assignedStartTime)
                .endTime(now.plusMinutes(80))
                .assignedBy(testStudent.getId())
                .build();
        assignedStudyTime = assignedStudyTimeRepository.save(assignedStudyTime);

        // When: 할당 시작 시 연결 처리
        ActualStudyTime connectedSession = studyTimeService.connectSessionOnStart(assignedStudyTime.getId());

        // Then: null을 반환해야 함
        assertThat(connectedSession).isNull();
    }

    @Test
    @DisplayName("학생 접속 시 정확한 시간 범위 매칭으로 연결해야 함")
    void shouldConnectWithExactTimeRangeMatching() {
        // Given: 할당된 공부시간 (현재 시간 기준으로 설정)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime assignedStartTime = now.plusMinutes(10);
        LocalDateTime assignedEndTime = now.plusMinutes(70);

        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(assignedStartTime)
                .endTime(assignedEndTime)
                .assignedBy(testStudent.getId())
                .build();
        assignedStudyTime = assignedStudyTimeRepository.save(assignedStudyTime);

        // When: 할당된 시간 범위 내에 접속
        LocalDateTime connectionTime = now.plusMinutes(30);
        ActualStudyTime actualStudyTime = studyTimeService.recordStudyStart(
                testStudent.getId(), 
                connectionTime, 
                "discord"
        );

        // Then: 할당된 공부시간에 연결되어야 함
        assertThat(actualStudyTime.getAssignedStudyTimeId()).isEqualTo(assignedStudyTime.getId());
        assertThat(actualStudyTime.getStartTime()).isEqualTo(connectionTime);
    }

    @Test
    @DisplayName("할당 시간 범위 밖 접속은 연결하지 않아야 함")
    void shouldNotConnectOutsideTimeRange() {
        // Given: 할당된 공부시간
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime assignedStartTime = now.plusMinutes(20);
        LocalDateTime assignedEndTime = now.plusMinutes(80);

        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(assignedStartTime)
                .endTime(assignedEndTime)
                .assignedBy(testStudent.getId())
                .build();
        assignedStudyTime = assignedStudyTimeRepository.save(assignedStudyTime);

        // When: 할당된 시간 범위 밖에 접속 (할당 시간 이전)
        LocalDateTime connectionTime = now.plusMinutes(10);
        ActualStudyTime actualStudyTime = studyTimeService.recordStudyStart(
                testStudent.getId(), 
                connectionTime, 
                "discord"
        );

        // Then: 할당된 공부시간에 연결되지 않아야 함
        assertThat(actualStudyTime.getAssignedStudyTimeId()).isNull();
        assertThat(actualStudyTime.getStartTime()).isEqualTo(connectionTime);
    }
}
