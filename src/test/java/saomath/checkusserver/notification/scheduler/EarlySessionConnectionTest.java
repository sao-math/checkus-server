package saomath.checkusserver.notification.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.entity.*;
import saomath.checkusserver.repository.*;
import saomath.checkusserver.service.StudyTimeService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("일찍 들어온 세션 연결 테스트")
class EarlySessionConnectionTest {

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
    @DisplayName("할당 시간 이전에 들어온 세션이 할당 생성 시 즉시 연결되어야 함")
    void shouldConnectEarlySessionWhenAssigningStudyTime() {
        // Given: 09:30에 학생이 접속 (할당 시간 이전)
        LocalDateTime earlyConnectionTime = LocalDateTime.of(2025, 6, 21, 9, 30);
        ActualStudyTime earlySession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(earlyConnectionTime)
                .source("discord")
                .build();
        earlySession = actualStudyTimeRepository.save(earlySession);

        // When: 09:40-10:40 공부시간 할당
        LocalDateTime assignedStartTime = LocalDateTime.of(2025, 6, 21, 9, 40);
        LocalDateTime assignedEndTime = LocalDateTime.of(2025, 6, 21, 10, 40);

        AssignedStudyTime assignedStudyTime = studyTimeService.assignStudyTime(
                testStudent.getId(),
                "수학 공부",
                testActivity.getId(),
                assignedStartTime,
                assignedEndTime,
                testStudent.getId()
        );

        // Then: 기존 세션이 할당된 공부시간에 연결되어야 함
        ActualStudyTime updatedSession = actualStudyTimeRepository.findById(earlySession.getId()).orElse(null);
        assertThat(updatedSession).isNotNull();
        assertThat(updatedSession.getAssignedStudyTimeId()).isEqualTo(assignedStudyTime.getId());
        assertThat(updatedSession.getStartTime()).isEqualTo(earlyConnectionTime);
        assertThat(updatedSession.getEndTime()).isNull(); // 아직 진행중
    }

    @Test
    @DisplayName("스케줄러에서 할당 시간 이전 세션을 연결할 수 있어야 함")
    void shouldConnectEarlySessionInScheduler() {
        // Given: 09:30에 학생이 접속하고 09:40에 공부시간이 할당되었지만 연결되지 않은 상황
        LocalDateTime earlyConnectionTime = LocalDateTime.of(2025, 6, 21, 9, 30);
        ActualStudyTime earlySession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(earlyConnectionTime)
                .source("discord")
                .build();
        earlySession = actualStudyTimeRepository.save(earlySession);

        LocalDateTime assignedStartTime = LocalDateTime.of(2025, 6, 21, 9, 40);
        LocalDateTime assignedEndTime = LocalDateTime.of(2025, 6, 21, 10, 40);

        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(assignedStartTime)
                .endTime(assignedEndTime)
                .assignedBy(testStudent.getId())
                .build();
        assignedStudyTime = assignedStudyTimeRepository.save(assignedStudyTime);

        // When: 스케줄러에서 이전 세션 연결 시도
        ActualStudyTime connectedSession = studyTimeService.connectPreviousOngoingSession(assignedStudyTime.getId());

        // Then: 세션이 성공적으로 연결되어야 함
        assertThat(connectedSession).isNotNull();
        assertThat(connectedSession.getId()).isEqualTo(earlySession.getId());
        assertThat(connectedSession.getAssignedStudyTimeId()).isEqualTo(assignedStudyTime.getId());
        assertThat(connectedSession.getStartTime()).isEqualTo(earlyConnectionTime);
    }

    @Test
    @DisplayName("여러 이전 세션이 있을 때 가장 최근 세션을 연결해야 함")
    void shouldConnectLatestEarlySession() {
        // Given: 여러 시점에 학생이 접속했다가 나갔다가 다시 접속
        LocalDateTime firstConnection = LocalDateTime.of(2025, 6, 21, 9, 0);
        LocalDateTime secondConnection = LocalDateTime.of(2025, 6, 21, 9, 20);
        LocalDateTime thirdConnection = LocalDateTime.of(2025, 6, 21, 9, 35); // 가장 최근

        // 첫 번째 세션 (종료됨)
        ActualStudyTime firstSession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(firstConnection)
                .endTime(LocalDateTime.of(2025, 6, 21, 9, 15))
                .source("discord")
                .build();
        actualStudyTimeRepository.save(firstSession);

        // 두 번째 세션 (종료됨)
        ActualStudyTime secondSession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(secondConnection)
                .endTime(LocalDateTime.of(2025, 6, 21, 9, 30))
                .source("discord")
                .build();
        actualStudyTimeRepository.save(secondSession);

        // 세 번째 세션 (진행중)
        ActualStudyTime thirdSession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(thirdConnection)
                .source("discord")
                .build();
        thirdSession = actualStudyTimeRepository.save(thirdSession);

        LocalDateTime assignedStartTime = LocalDateTime.of(2025, 6, 21, 9, 40);
        LocalDateTime assignedEndTime = LocalDateTime.of(2025, 6, 21, 10, 40);

        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(assignedStartTime)
                .endTime(assignedEndTime)
                .assignedBy(testStudent.getId())
                .build();
        assignedStudyTime = assignedStudyTimeRepository.save(assignedStudyTime);

        // When: 이전 세션 연결 시도
        ActualStudyTime connectedSession = studyTimeService.connectPreviousOngoingSession(assignedStudyTime.getId());

        // Then: 가장 최근이고 진행중인 세션이 연결되어야 함
        assertThat(connectedSession).isNotNull();
        assertThat(connectedSession.getId()).isEqualTo(thirdSession.getId());
        assertThat(connectedSession.getStartTime()).isEqualTo(thirdConnection);
        assertThat(connectedSession.getAssignedStudyTimeId()).isEqualTo(assignedStudyTime.getId());
    }

    @Test
    @DisplayName("이미 다른 할당에 연결된 세션은 연결하지 않아야 함")
    void shouldNotConnectAlreadyAssignedSession() {
        // Given: 이미 다른 할당에 연결된 세션
        LocalDateTime earlyConnectionTime = LocalDateTime.of(2025, 6, 21, 9, 30);
        
        // 첫 번째 할당
        AssignedStudyTime firstAssignment = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("영어 공부")
                .activityId(testActivity.getId())
                .startTime(LocalDateTime.of(2025, 6, 21, 9, 0))
                .endTime(LocalDateTime.of(2025, 6, 21, 9, 30))
                .assignedBy(testStudent.getId())
                .build();
        firstAssignment = assignedStudyTimeRepository.save(firstAssignment);

        // 이미 첫 번째 할당에 연결된 세션
        ActualStudyTime existingSession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(earlyConnectionTime)
                .assignedStudyTimeId(firstAssignment.getId())
                .source("discord")
                .build();
        actualStudyTimeRepository.save(existingSession);

        // 두 번째 할당
        AssignedStudyTime secondAssignment = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(LocalDateTime.of(2025, 6, 21, 9, 40))
                .endTime(LocalDateTime.of(2025, 6, 21, 10, 40))
                .assignedBy(testStudent.getId())
                .build();
        secondAssignment = assignedStudyTimeRepository.save(secondAssignment);

        // When: 두 번째 할당에 이전 세션 연결 시도
        ActualStudyTime connectedSession = studyTimeService.connectPreviousOngoingSession(secondAssignment.getId());

        // Then: 연결할 세션이 없어야 함 (이미 할당된 세션은 제외)
        assertThat(connectedSession).isNull();
    }

    @Test
    @DisplayName("할당 시간 범위 내 세션도 연결할 수 있어야 함")
    void shouldConnectSessionWithinAssignedTimeRange() {
        // Given: 할당 시간 범위 내에 접속한 세션
        LocalDateTime connectionTime = LocalDateTime.of(2025, 6, 21, 9, 45); // 할당 시간 내

        ActualStudyTime withinRangeSession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(connectionTime)
                .source("discord")
                .build();
        withinRangeSession = actualStudyTimeRepository.save(withinRangeSession);

        LocalDateTime assignedStartTime = LocalDateTime.of(2025, 6, 21, 9, 40);
        LocalDateTime assignedEndTime = LocalDateTime.of(2025, 6, 21, 10, 40);

        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(assignedStartTime)
                .endTime(assignedEndTime)
                .assignedBy(testStudent.getId())
                .build();
        assignedStudyTime = assignedStudyTimeRepository.save(assignedStudyTime);

        // When: 세션 연결 시도
        ActualStudyTime connectedSession = studyTimeService.connectPreviousOngoingSession(assignedStudyTime.getId());

        // Then: 범위 내 세션이 연결되어야 함
        assertThat(connectedSession).isNotNull();
        assertThat(connectedSession.getId()).isEqualTo(withinRangeSession.getId());
        assertThat(connectedSession.getAssignedStudyTimeId()).isEqualTo(assignedStudyTime.getId());
    }

    @Test
    @DisplayName("할당 생성 시 이전 세션과 범위 내 세션을 모두 연결해야 함")
    void shouldConnectBothEarlyAndWithinRangeSessions() {
        // Given: 할당 시간 이전 세션과 범위 내 세션이 모두 존재
        LocalDateTime earlyConnectionTime = LocalDateTime.of(2025, 6, 21, 9, 30);
        LocalDateTime withinRangeConnectionTime = LocalDateTime.of(2025, 6, 21, 9, 45);

        ActualStudyTime earlySession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(earlyConnectionTime)
                .source("discord")
                .build();
        earlySession = actualStudyTimeRepository.save(earlySession);

        ActualStudyTime withinRangeSession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(withinRangeConnectionTime)
                .endTime(LocalDateTime.of(2025, 6, 21, 10, 0)) // 종료된 세션
                .source("discord")
                .build();
        withinRangeSession = actualStudyTimeRepository.save(withinRangeSession);

        // When: 공부시간 할당
        LocalDateTime assignedStartTime = LocalDateTime.of(2025, 6, 21, 9, 40);
        LocalDateTime assignedEndTime = LocalDateTime.of(2025, 6, 21, 10, 40);

        AssignedStudyTime assignedStudyTime = studyTimeService.assignStudyTime(
                testStudent.getId(),
                "수학 공부",
                testActivity.getId(),
                assignedStartTime,
                assignedEndTime,
                testStudent.getId()
        );

        // Then: 두 세션 모두 연결되어야 함
        List<ActualStudyTime> connectedSessions = actualStudyTimeRepository
                .findByAssignedStudyTimeId(assignedStudyTime.getId());

        assertThat(connectedSessions).hasSize(2);
        assertThat(connectedSessions)
                .extracting(ActualStudyTime::getId)
                .containsExactlyInAnyOrder(earlySession.getId(), withinRangeSession.getId());
    }
}
