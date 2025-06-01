package saomath.checkusserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import saomath.checkusserver.auth.CustomUserPrincipal;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.dto.AssignStudyTimeRequest;
import saomath.checkusserver.dto.RecordStudyStartRequest;
import saomath.checkusserver.dto.RecordStudyEndRequest;
import saomath.checkusserver.dto.UpdateStudyTimeRequest;
import saomath.checkusserver.entity.Activity;
import saomath.checkusserver.entity.ActualStudyTime;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.repository.ActivityRepository;
import saomath.checkusserver.repository.ActualStudyTimeRepository;
import saomath.checkusserver.repository.AssignedStudyTimeRepository;
import saomath.checkusserver.repository.UserRepository;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
@DisplayName("StudyTimeController 통합 테스트")
class StudyTimeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private AssignedStudyTimeRepository assignedStudyTimeRepository;

    @Autowired
    private ActualStudyTimeRepository actualStudyTimeRepository;

    private User student;
    private User teacher;
    private Activity activity;

    @BeforeEach
    void setUp() {
        // 데이터 정리
        actualStudyTimeRepository.deleteAll();
        assignedStudyTimeRepository.deleteAll();
        activityRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트 데이터 생성
        student = createTestUser("student1", "김학생", "010-1111-1111");
        teacher = createTestUser("teacher1", "이선생", "010-2222-2222");
        activity = createTestActivity("수학 공부", true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("공부 시간 배정 API 테스트")
    void assignStudyTime_Success() throws Exception {
        // Given - 인증 정보 설정
        setSecurityContext(teacher, "TEACHER");
        
        AssignStudyTimeRequest request = new AssignStudyTimeRequest();
        request.setStudentId(student.getId());
        request.setActivityId(activity.getId());
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(3));

        // When & Then
        mockMvc.perform(post("/study-time/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공부 시간이 성공적으로 배정되었습니다."))
                .andExpect(jsonPath("$.data.studentId").value(student.getId()))
                .andExpect(jsonPath("$.data.activityId").value(activity.getId()));
    }

    @Test
    @WithMockUser(username = "teacher1", authorities = {"TEACHER"})
    @DisplayName("공부 시간 배정 실패 - 잘못된 입력")
    void assignStudyTime_InvalidInput_Fail() throws Exception {
        // Given - 종료 시간이 시작 시간보다 이른 경우
        AssignStudyTimeRequest request = new AssignStudyTimeRequest();
        request.setStudentId(student.getId());
        request.setActivityId(activity.getId());
        request.setStartTime(LocalDateTime.now().plusHours(3));
        request.setEndTime(LocalDateTime.now().plusHours(1)); // 잘못된 시간

        // When & Then
        mockMvc.perform(post("/study-time/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("공부 시간 배정 실패 - 시간 겹침")
    void assignStudyTime_TimeOverlap_Fail() throws Exception {
        // Given - 인증 정보 설정
        setSecurityContext(teacher, "TEACHER");
        
        // Given - 기존 배정 시간 생성
        LocalDateTime baseTime = LocalDateTime.now().plusHours(1);
        AssignedStudyTime existing = AssignedStudyTime.builder()
                .studentId(student.getId())
                .activityId(activity.getId())
                .startTime(baseTime)
                .endTime(baseTime.plusHours(2))
                .assignedBy(teacher.getId())
                .build();
        assignedStudyTimeRepository.save(existing);

        // Given - 겹치는 시간으로 새로운 배정 시도
        AssignStudyTimeRequest request = new AssignStudyTimeRequest();
        request.setStudentId(student.getId());
        request.setActivityId(activity.getId());
        request.setStartTime(baseTime.plusHours(1)); // 겹치는 시간
        request.setEndTime(baseTime.plusHours(3));

        // When & Then
        mockMvc.perform(post("/study-time/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("해당 시간대에 이미 배정된 공부 시간이 있습니다."));
    }

    @Test
    @WithMockUser(username = "teacher1", authorities = {"TEACHER"})
    @DisplayName("배정된 공부 시간 수정 API 테스트")
    void updateAssignedStudyTime_Success() throws Exception {
        // Given - 기존 배정 시간 생성
        AssignedStudyTime existing = AssignedStudyTime.builder()
                .studentId(student.getId())
                .activityId(activity.getId())
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(3))
                .assignedBy(teacher.getId())
                .build();
        AssignedStudyTime saved = assignedStudyTimeRepository.save(existing);

        // Given - 수정 요청
        UpdateStudyTimeRequest request = new UpdateStudyTimeRequest();
        request.setActivityId(activity.getId());
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setEndTime(LocalDateTime.now().plusHours(4));

        // When & Then
        mockMvc.perform(put("/study-time/" + saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공부 시간이 성공적으로 수정되었습니다."));
    }

    @Test
    @WithMockUser(username = "teacher1", authorities = {"TEACHER"})
    @DisplayName("배정된 공부 시간 삭제 API 테스트")
    void deleteAssignedStudyTime_Success() throws Exception {
        // Given - 기존 배정 시간 생성
        AssignedStudyTime existing = AssignedStudyTime.builder()
                .studentId(student.getId())
                .activityId(activity.getId())
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(3))
                .assignedBy(teacher.getId())
                .build();
        AssignedStudyTime saved = assignedStudyTimeRepository.save(existing);

        // When & Then
        mockMvc.perform(delete("/study-time/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공부 시간이 성공적으로 삭제되었습니다."));
    }

    @Test
    @WithMockUser(username = "teacher1", authorities = {"TEACHER"})
    @DisplayName("존재하지 않는 배정 시간 삭제 실패 테스트")
    void deleteAssignedStudyTime_NotFound_Fail() throws Exception {
        // When & Then
        mockMvc.perform(delete("/study-time/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "student1", authorities = {"STUDENT"})
    @DisplayName("학생별 배정된 공부 시간 조회 API 테스트")
    void getAssignedStudyTimes_Success() throws Exception {
        // Given - 배정 시간 생성
        LocalDateTime startDate = LocalDateTime.now();
        AssignedStudyTime assigned = AssignedStudyTime.builder()
                .studentId(student.getId())
                .activityId(activity.getId())
                .startTime(startDate.plusHours(1))
                .endTime(startDate.plusHours(3))
                .assignedBy(teacher.getId())
                .build();
        assignedStudyTimeRepository.save(assigned);

        // When & Then
        mockMvc.perform(get("/study-time/assigned/student/" + student.getId())
                .param("startDate", startDate.toString())
                .param("endDate", startDate.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("배정된 공부 시간을 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].studentId").value(student.getId()));
    }

    @Test
    @WithMockUser(username = "student1", authorities = {"STUDENT"})
    @DisplayName("학생별 실제 공부 시간 조회 API 테스트")
    void getActualStudyTimes_Success() throws Exception {
        // Given - 실제 공부 시간 생성
        LocalDateTime startDate = LocalDateTime.now();
        ActualStudyTime actual = ActualStudyTime.builder()
                .studentId(student.getId())
                .startTime(startDate.plusHours(1))
                .endTime(startDate.plusHours(2))
                .source("discord")
                .build();
        actualStudyTimeRepository.save(actual);

        // When & Then
        mockMvc.perform(get("/study-time/actual/student/" + student.getId())
                .param("startDate", startDate.toString())
                .param("endDate", startDate.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("실제 공부 시간을 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].studentId").value(student.getId()));
    }

    @Test
    @WithMockUser(username = "discord-bot", authorities = {"BOT"})
    @DisplayName("디스코드 봇용 공부 시작 기록 API 테스트")
    void recordStudyStart_Success() throws Exception {
        // Given
        RecordStudyStartRequest request = new RecordStudyStartRequest();
        request.setStudentId(student.getId());
        request.setStartTime(LocalDateTime.now());
        request.setSource("discord");

        // When & Then
        mockMvc.perform(post("/study-time/record/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공부 시작이 성공적으로 기록되었습니다."))
                .andExpect(jsonPath("$.data.studentId").value(student.getId()))
                .andExpect(jsonPath("$.data.source").value("discord"));
    }

    @Test
    @WithMockUser(username = "discord-bot", authorities = {"BOT"})
    @DisplayName("디스코드 봇용 공부 종료 기록 API 테스트")
    void recordStudyEnd_Success() throws Exception {
        // Given - 공부 시작 기록 생성
        ActualStudyTime actual = ActualStudyTime.builder()
                .studentId(student.getId())
                .startTime(LocalDateTime.now().minusHours(1))
                .source("discord")
                .build();
        ActualStudyTime saved = actualStudyTimeRepository.save(actual);

        // Given - 종료 요청
        RecordStudyEndRequest request = new RecordStudyEndRequest();
        request.setEndTime(LocalDateTime.now());

        // When & Then
        mockMvc.perform(put("/study-time/record/" + saved.getId() + "/end")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공부 종료가 성공적으로 기록되었습니다."));
    }

    @Test
    @WithMockUser(username = "teacher1", authorities = {"TEACHER"})
    @DisplayName("공부 배정 가능한 활동 목록 조회 API 테스트")
    void getStudyAssignableActivities_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/study-time/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("활동 목록을 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("수학 공부"))
                .andExpect(jsonPath("$.data[0].isStudyAssignable").value(true));
    }

    @Test
    @WithMockUser(username = "system", authorities = {"ADMIN"})
    @DisplayName("곧 시작할 공부 시간 조회 API 테스트")
    void getUpcomingStudyTimes_Success() throws Exception {
        // Given - 곧 시작할 배정 시간 생성
        LocalDateTime now = LocalDateTime.now();
        AssignedStudyTime upcoming = AssignedStudyTime.builder()
                .studentId(student.getId())
                .activityId(activity.getId())
                .startTime(now.plusMinutes(5)) // 5분 후 시작
                .endTime(now.plusHours(1))
                .assignedBy(teacher.getId())
                .build();
        assignedStudyTimeRepository.save(upcoming);

        // When & Then
        mockMvc.perform(get("/study-time/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("곧 시작할 공부 시간을 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("인증 없이 API 접근 실패 테스트")
    void accessWithoutAuth_Fail() throws Exception {
        // When & Then
        mockMvc.perform(get("/study-time/activities"))
                .andExpect(status().isUnauthorized());
    }

    // Helper methods
    private User createTestUser(String username, String name, String phoneNumber) {
        User user = User.builder()
                .username(username)
                .name(name)
                .phoneNumber(phoneNumber)
                .password("encodedPassword")
                .build();
        return userRepository.save(user);
    }

    private Activity createTestActivity(String name, boolean isStudyAssignable) {
        Activity activity = Activity.builder()
                .name(name)
                .isStudyAssignable(isStudyAssignable)
                .build();
        return activityRepository.save(activity);
    }

    private void setSecurityContext(User user, String role) {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getName(),
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role)),
                true, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true  // accountNonLocked
        );
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
