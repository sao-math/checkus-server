package saomath.checkusserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.dto.AssignStudyTimeRequest;
import saomath.checkusserver.entity.Activity;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.repository.ActivityRepository;
import saomath.checkusserver.repository.AssignedStudyTimeRepository;
import saomath.checkusserver.repository.UserRepository;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("StudyTimeController 최적화 통합 테스트")
public class StudyTimeControllerOptimizationTest {

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

    private User testStudent;
    private User testTeacher;
    private Activity testActivity;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        testStudent = User.builder()
                .username("test_student")
                .name("테스트학생")
                .phoneNumber("010-1234-5678")
                .password("password123")
                .build();
        testStudent = userRepository.save(testStudent);

        testTeacher = User.builder()
                .username("test_teacher")
                .name("테스트선생")
                .phoneNumber("010-9876-5432")
                .password("password123")
                .build();
        testTeacher = userRepository.save(testTeacher);

        testActivity = Activity.builder()
                .name("테스트공부")
                .isStudyAssignable(true)
                .build();
        testActivity = activityRepository.save(testActivity);
    }

    @Test
    @WithMockUser(username = "test_teacher", roles = {"TEACHER"})
    @DisplayName("공부 시간 배정 시 연관 엔티티 정보가 포함되어 반환됨")
    void assignStudyTime_ReturnsWithAssociatedEntities() throws Exception {
        // Given
        AssignStudyTimeRequest request = new AssignStudyTimeRequest();
        request.setStudentId(testStudent.getId());
        request.setTitle("통합테스트 공부시간");
        request.setActivityId(testActivity.getId());
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(3));

        // When & Then
        mockMvc.perform(post("/study-time/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentName").value("테스트학생"))
                .andExpect(jsonPath("$.data.activityName").value("테스트공부"))
                .andExpect(jsonPath("$.data.assignedByName").value("테스트선생"))
                .andExpect(jsonPath("$.data.isStudyAssignable").value(true));
    }

    @Test
    @WithMockUser(username = "test_teacher", roles = {"TEACHER"})
    @DisplayName("학생별 배정된 공부 시간 조회 시 연관 엔티티가 포함됨")
    void getAssignedStudyTimes_ReturnsWithAssociatedEntities() throws Exception {
        // Given - 테스트 데이터 생성
        AssignedStudyTime testAssignment = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("테스트 할당")
                .activityId(testActivity.getId())
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(3))
                .assignedBy(testTeacher.getId())
                .build();
        assignedStudyTimeRepository.save(testAssignment);

        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        // When & Then
        mockMvc.perform(get("/study-time/assigned/student/" + testStudent.getId())
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].studentName").value("테스트학생"))
                .andExpect(jsonPath("$.data[0].activityName").value("테스트공부"))
                .andExpect(jsonPath("$.data[0].assignedByName").value("테스트선생"));
    }

    @Test
    @WithMockUser(username = "test_teacher", roles = {"TEACHER"})
    @DisplayName("곧 시작할 공부 시간 조회 시 연관 엔티티가 포함됨")
    void getUpcomingStudyTimes_ReturnsWithAssociatedEntities() throws Exception {
        // Given - 곧 시작할 공부 시간 생성
        AssignedStudyTime upcomingAssignment = AssignedStudyTime.builder()
                .studentId(testStudent.getId())
                .title("곧 시작할 공부")
                .activityId(testActivity.getId())
                .startTime(LocalDateTime.now().plusMinutes(5)) // 5분 후 시작
                .endTime(LocalDateTime.now().plusMinutes(65))
                .assignedBy(testTeacher.getId())
                .build();
        assignedStudyTimeRepository.save(upcomingAssignment);

        // When & Then
        mockMvc.perform(get("/study-time/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].studentName").value("테스트학생"))
                .andExpect(jsonPath("$.data[0].activityName").value("테스트공부"))
                .andExpect(jsonPath("$.data[0].assignedByName").value("테스트선생"));
    }

    @Test
    @WithMockUser(username = "test_teacher", roles = {"TEACHER"})
    @DisplayName("다중 배정 조회 시 N+1 문제 없이 조회됨")
    void getMultipleAssignments_WithoutNPlusOneProblem() throws Exception {
        // Given - 여러 개의 배정 생성
        for (int i = 0; i < 5; i++) {
            AssignedStudyTime assignment = AssignedStudyTime.builder()
                    .studentId(testStudent.getId())
                    .title("배정 " + (i + 1))
                    .activityId(testActivity.getId())
                    .startTime(LocalDateTime.now().plusHours(i + 1))
                    .endTime(LocalDateTime.now().plusHours(i + 2))
                    .assignedBy(testTeacher.getId())
                    .build();
            assignedStudyTimeRepository.save(assignment);
        }

        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        // When & Then
        mockMvc.perform(get("/study-time/assigned/student/" + testStudent.getId())
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(5))
                // 모든 항목에 연관 엔티티 정보가 있는지 확인
                .andExpect(jsonPath("$.data[0].studentName").value("테스트학생"))
                .andExpect(jsonPath("$.data[0].activityName").value("테스트공부"))
                .andExpect(jsonPath("$.data[0].assignedByName").value("테스트선생"))
                .andExpect(jsonPath("$.data[4].studentName").value("테스트학생"))
                .andExpect(jsonPath("$.data[4].activityName").value("테스트공부"))
                .andExpect(jsonPath("$.data[4].assignedByName").value("테스트선생"));
    }
}
