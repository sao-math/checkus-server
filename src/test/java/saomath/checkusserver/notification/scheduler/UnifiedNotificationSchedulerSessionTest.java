package saomath.checkusserver.notification.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.notification.service.MultiChannelNotificationService;
import saomath.checkusserver.notification.service.NotificationTargetService;
import saomath.checkusserver.repository.*;
import saomath.checkusserver.study.domain.Activity;
import saomath.checkusserver.study.domain.ActualStudyTime;
import saomath.checkusserver.study.domain.AssignedStudyTime;
import saomath.checkusserver.study.service.StudyTimeService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("통합 알림 스케줄러 세션 연결 테스트")
class UnifiedNotificationSchedulerSessionTest {

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

    @MockitoBean
    private MultiChannelNotificationService notificationService;

    @MockitoBean
    private NotificationTargetService targetService;

    @Autowired
    private UnifiedNotificationScheduler scheduler;

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

        // Mock 설정
        when(notificationService.sendNotification(any(), any(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(true));
    }

    @Test
    @DisplayName("스케줄러 실행 시 즉시 세션 연결이 먼저 수행되어야 함")
    void shouldConnectSessionsImmediatelyInScheduler() {
        // Given: 할당 시간 이전에 접속한 세션
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime earlyConnectionTime = now.minusMinutes(10);

        ActualStudyTime earlySession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(earlyConnectionTime)
                .source("discord")
                .build();
        earlySession = actualStudyTimeRepository.save(earlySession);

        // 현재 시간에 시작하는 할당
        AssignedStudyTime currentAssignment = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(now)
                .endTime(now.plusHours(1))
                .assignedBy(testStudent.getId())
                .build();
        currentAssignment = assignedStudyTimeRepository.save(currentAssignment);

        // 알림 대상 Mock 설정
        NotificationTargetService.StudyTarget target = NotificationTargetService.StudyTarget.builder()
                .studentId(testStudent.getId())
                .studentName(testStudent.getName())
                .startTime(now)
                .endTime(now.plusHours(1))
                .studentNotificationEnabled(true)
                .parentNotificationEnabled(false)
                .build();

        when(targetService.getStudyTargetsForTime(now)).thenReturn(Arrays.asList(target));

        // When: 스케줄러 실행
        scheduler.sendStudyStartNotificationAndConnectSessions();

        // Then: 이전 세션이 현재 할당에 연결되어야 함
        ActualStudyTime updatedSession = actualStudyTimeRepository.findById(earlySession.getId()).orElse(null);
        assertThat(updatedSession).isNotNull();
        assertThat(updatedSession.getAssignedStudyTimeId()).isEqualTo(currentAssignment.getId());

        // And: 알림도 발송되어야 함
        verify(notificationService).sendNotification(eq(testStudent.getId()), any(), any());
    }

    @Test
    @DisplayName("이미 연결된 세션은 기존 세션 종료 후 새 세션을 생성해야 함")
    void shouldNotDuplicateConnectionForAlreadyConnectedSession() {
        // Given: 이미 연결된 세션
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        
        AssignedStudyTime assignment = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(now)
                .endTime(now.plusHours(1))
                .assignedBy(testStudent.getId())
                .build();
        assignment = assignedStudyTimeRepository.save(assignment);

        ActualStudyTime alreadyConnectedSession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(now.minusMinutes(10))
                .assignedStudyTimeId(assignment.getId()) // 이미 연결됨
                .source("discord")
                .build();
        actualStudyTimeRepository.save(alreadyConnectedSession);

        // Mock 설정 (알림 대상 없음)
        when(targetService.getStudyTargetsForTime(now)).thenReturn(Arrays.asList());

        // When: 스케줄러 실행
        scheduler.sendStudyStartNotificationAndConnectSessions();

        // Then: 기존 세션이 종료되고 새 세션이 생성되어야 함 (Case 2-1 로직)
        List<ActualStudyTime> connectedSessions = actualStudyTimeRepository
                .findByAssignedStudyTimeId(assignment.getId());
        
        assertThat(connectedSessions).hasSize(2); // 종료된 세션 + 새 세션
        
        // 기존 세션은 종료되어야 함
        ActualStudyTime oldSession = connectedSessions.stream()
                .filter(s -> s.getEndTime() != null)
                .findFirst()
                .orElse(null);
        assertThat(oldSession).isNotNull();
        assertThat(oldSession.getEndTime()).isEqualTo(now);
        
        // 새 세션은 할당 시작 시간에 생성되어야 함
        ActualStudyTime newSession = connectedSessions.stream()
                .filter(s -> s.getEndTime() == null)
                .findFirst()
                .orElse(null);
        assertThat(newSession).isNotNull();
        assertThat(newSession.getStartTime()).isEqualTo(now);
    }

    @Test
    @DisplayName("여러 학생의 세션을 각각 올바르게 연결해야 함")
    void shouldConnectSessionsForMultipleStudentsCorrectly() {
        // Given: 두 명의 학생
        User student2 = User.builder()
                .username("test_student2")
                .name("테스트학생2")
                .phoneNumber("010-9876-5432")
                .password("password")
                .discordId("987654321")
                .build();
        student2 = userRepository.save(student2);

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        // 첫 번째 학생의 이전 세션
        ActualStudyTime student1EarlySession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(now.minusMinutes(15))
                .source("discord")
                .build();
        student1EarlySession = actualStudyTimeRepository.save(student1EarlySession);

        // 두 번째 학생의 이전 세션
        ActualStudyTime student2EarlySession = ActualStudyTime.builder()
                .studentId(student2.getId())
                .startTime(now.minusMinutes(5))
                .source("discord")
                .build();
        student2EarlySession = actualStudyTimeRepository.save(student2EarlySession);

        // 각 학생의 현재 할당
        AssignedStudyTime assignment1 = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("수학 공부")
                .activityId(testActivity.getId())
                .startTime(now)
                .endTime(now.plusHours(1))
                .assignedBy(testStudent.getId())
                .build();
        assignment1 = assignedStudyTimeRepository.save(assignment1);

        AssignedStudyTime assignment2 = AssignedStudyTime.builder()
                .studentId(student2.getId())
                .title("영어 공부")
                .activityId(testActivity.getId())
                .startTime(now)
                .endTime(now.plusHours(1))
                .assignedBy(student2.getId())
                .build();
        assignment2 = assignedStudyTimeRepository.save(assignment2);

        // Mock 설정 (알림 대상 없음)
        when(targetService.getStudyTargetsForTime(now)).thenReturn(Arrays.asList());

        // When: 스케줄러 실행
        scheduler.sendStudyStartNotificationAndConnectSessions();

        // Then: 각 학생의 세션이 올바른 할당에 연결되어야 함
        ActualStudyTime updatedSession1 = actualStudyTimeRepository.findById(student1EarlySession.getId()).orElse(null);
        assertThat(updatedSession1).isNotNull();
        assertThat(updatedSession1.getAssignedStudyTimeId()).isEqualTo(assignment1.getId());

        ActualStudyTime updatedSession2 = actualStudyTimeRepository.findById(student2EarlySession.getId()).orElse(null);
        assertThat(updatedSession2).isNotNull();
        assertThat(updatedSession2.getAssignedStudyTimeId()).isEqualTo(assignment2.getId());
    }

    @Test
    @DisplayName("즉시 연결과 지연 연결이 모두 정상 작동해야 함")
    void shouldHandleBothImmediateAndDelayedConnections() {
        // Given: 현재 시간에 시작하는 할당만 (스케줄러는 현재 시간 할당만 처리함)
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        // 현재 시간 할당 (즉시 연결 대상)
        AssignedStudyTime currentAssignment = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("현재 수학 공부")
                .activityId(testActivity.getId())
                .startTime(now)
                .endTime(now.plusHours(1))
                .assignedBy(testStudent.getId())
                .build();
        currentAssignment = assignedStudyTimeRepository.save(currentAssignment);

        // 현재 할당에 연결될 세션
        ActualStudyTime currentSession = ActualStudyTime.builder()
                .studentId(testStudent.getId())
                .startTime(now.minusMinutes(5))
                .source("discord")
                .build();
        currentSession = actualStudyTimeRepository.save(currentSession);

        // Mock 설정 (알림 대상 없음)
        when(targetService.getStudyTargetsForTime(now)).thenReturn(Arrays.asList());

        // When: 스케줄러 실행
        scheduler.sendStudyStartNotificationAndConnectSessions();

        // Then: 현재 시간 세션이 적절한 할당에 연결되어야 함
        ActualStudyTime updatedCurrentSession = actualStudyTimeRepository.findById(currentSession.getId()).orElse(null);
        assertThat(updatedCurrentSession).isNotNull();
        assertThat(updatedCurrentSession.getAssignedStudyTimeId()).isEqualTo(currentAssignment.getId());
    }
}
