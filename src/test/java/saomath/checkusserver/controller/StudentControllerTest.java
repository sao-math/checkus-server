package saomath.checkusserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import saomath.checkusserver.auth.service.CustomUserDetailsService;
import saomath.checkusserver.auth.jwt.JwtTokenProvider;
import saomath.checkusserver.user.dto.GuardianResponse;
import saomath.checkusserver.user.dto.StudentDetailResponse;
import saomath.checkusserver.user.dto.StudentListResponse;
import saomath.checkusserver.user.dto.StudentUpdateRequest;
import saomath.checkusserver.user.domain.StudentProfile;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.user.service.StudentService;
import saomath.checkusserver.user.controller.StudentController;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private StudentListResponse mockStudentListResponse;
    private StudentDetailResponse mockStudentDetailResponse;

    @BeforeEach
    void setUp() {
        // Mock 데이터 설정
        GuardianResponse guardian = new GuardianResponse(5L, "박학부모", "010-1111-2222", "모");
        
        mockStudentListResponse = new StudentListResponse(
                4L,
                "박학생",
                "010-1111-2222",
                "010-2222-1111",
                "이현중",
                2,
                Arrays.asList("수학심화반", "과학반"),
                StudentProfile.StudentStatus.ENROLLED,
                Arrays.asList(guardian)
        );

        StudentDetailResponse.ClassResponse classResponse1 = new StudentDetailResponse.ClassResponse(1L, "수학심화반");
        StudentDetailResponse.ClassResponse classResponse2 = new StudentDetailResponse.ClassResponse(2L, "과학반");

        mockStudentDetailResponse = new StudentDetailResponse(
                4L,
                "student1",
                "박학생",
                "010-2222-1111",
                "student1#1234",
                LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                StudentProfile.StudentStatus.ENROLLED,
                "이현중",
                1L,
                2,
                StudentProfile.Gender.MALE,
                Arrays.asList(classResponse1, classResponse2),
                Arrays.asList(guardian)
        );
    }

    @Test
    @DisplayName("학생 목록 조회 - 필터 없음")
    @WithMockUser(roles = "TEACHER")
    void getStudents_NoFilter_Success() throws Exception {
        // Given
        when(studentService.getFilteredStudents(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Arrays.asList(mockStudentListResponse));

        // When & Then
        mockMvc.perform(get("/students")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학생 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(4))
                .andExpect(jsonPath("$.data[0].name").value("박학생"))
                .andExpect(jsonPath("$.data[0].school").value("이현중"))
                .andExpect(jsonPath("$.data[0].grade").value(2))
                .andExpect(jsonPath("$.data[0].status").value("ENROLLED"));
    }

    @Test
    @DisplayName("학생 목록 조회 - 필터 적용")
    @WithMockUser(roles = "TEACHER")
    void getStudents_WithFilter_Success() throws Exception {
        // Given
        when(studentService.getFilteredStudents(eq(1L), eq(2), eq(StudentProfile.StudentStatus.ENROLLED), eq(1L)))
                .thenReturn(Arrays.asList(mockStudentListResponse));

        // When & Then
        mockMvc.perform(get("/students")
                        .param("classId", "1")
                        .param("grade", "2")
                        .param("status", "ENROLLED")
                        .param("schoolId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(4));
    }

    @Test
    @DisplayName("학생 상세 정보 조회 - 성공")
    @WithMockUser(roles = "TEACHER")
    void getStudentDetail_Success() throws Exception {
        // Given
        when(studentService.getStudentDetail(4L))
                .thenReturn(mockStudentDetailResponse);

        // When & Then
        mockMvc.perform(get("/students/4")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학생 상세 정보 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("student1"))
                .andExpect(jsonPath("$.data.name").value("박학생"))
                .andExpect(jsonPath("$.data.school").value("이현중"))
                .andExpect(jsonPath("$.data.classes").isArray())
                .andExpect(jsonPath("$.data.classes[0].name").value("수학심화반"))
                .andExpect(jsonPath("$.data.guardians").isArray())
                .andExpect(jsonPath("$.data.guardians[0].name").value("박학부모"));
    }

    @Test
    @DisplayName("학생 상세 정보 조회 - 학생을 찾을 수 없음")
    @WithMockUser(roles = "TEACHER")
    void getStudentDetail_NotFound() throws Exception {
        // Given
        when(studentService.getStudentDetail(999L))
                .thenThrow(new ResourceNotFoundException("학생을 찾을 수 없습니다. ID: 999"));

        // When & Then
        mockMvc.perform(get("/students/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("학생을 찾을 수 없습니다. ID: 999"));
    }

    @Test
    @DisplayName("학생 목록 조회 - 인증 없음")
    void getStudents_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/students")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("학생 상세 정보 조회 - 인증 없음")
    void getStudentDetail_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/students/4")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("학생 정보 수정 - 성공")
    @WithMockUser(roles = "TEACHER")
    void updateStudent_Success() throws Exception {
        // Given
        StudentUpdateRequest updateRequest = createMockUpdateRequest();
        when(studentService.updateStudent(eq(4L), any(StudentUpdateRequest.class)))
                .thenReturn(mockStudentDetailResponse);

        // When & Then
        mockMvc.perform(put("/students/4")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학생 정보 수정 성공"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.name").value("박학생"));
    }

    @Test
    @DisplayName("학생 정보 수정 - 학생을 찾을 수 없음")
    @WithMockUser(roles = "TEACHER")
    void updateStudent_NotFound() throws Exception {
        // Given
        StudentUpdateRequest updateRequest = createMockUpdateRequest();
        when(studentService.updateStudent(eq(999L), any(StudentUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("학생을 찾을 수 없습니다. ID: 999"));

        // When & Then
        mockMvc.perform(put("/students/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("학생을 찾을 수 없습니다. ID: 999"));
    }

    @Test
    @DisplayName("학생 정보 수정 - 인증 없음")
    void updateStudent_Unauthorized() throws Exception {
        // Given
        StudentUpdateRequest updateRequest = createMockUpdateRequest();

        // When & Then
        mockMvc.perform(put("/students/4")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("학생 정보 수정 - 유효성 검증 실패")
    @WithMockUser(roles = "TEACHER")
    void updateStudent_ValidationFailed() throws Exception {
        // Given - 잘못된 전화번호 형식
        StudentUpdateRequest invalidRequest = new StudentUpdateRequest();
        invalidRequest.setName("");
        invalidRequest.setPhoneNumber("invalid-phone");

        // When & Then
        mockMvc.perform(put("/students/4")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    private StudentUpdateRequest createMockUpdateRequest() {
        StudentUpdateRequest updateRequest = new StudentUpdateRequest();
        updateRequest.setName("박학생_수정");
        updateRequest.setPhoneNumber("010-3333-3333");
        updateRequest.setDiscordId("updated#1234");
        
        // 프로필 정보
        StudentUpdateRequest.StudentProfileUpdateRequest profile = new StudentUpdateRequest.StudentProfileUpdateRequest();
        profile.setStatus(StudentProfile.StudentStatus.ENROLLED);
        profile.setSchoolId(1L);
        profile.setGrade(3);
        profile.setGender(StudentProfile.Gender.MALE);
        updateRequest.setProfile(profile);
        
        // 반 정보
        updateRequest.setClassIds(Arrays.asList(1L, 2L));
        
        // 학부모 정보
        StudentUpdateRequest.GuardianUpdateRequest guardian = new StudentUpdateRequest.GuardianUpdateRequest();
        guardian.setId(5L);
        guardian.setName("박학부모_수정");
        guardian.setPhoneNumber("010-4444-4444");
        guardian.setRelationship("모");
        updateRequest.setGuardians(Arrays.asList(guardian));
        
        return updateRequest;
    }
}
