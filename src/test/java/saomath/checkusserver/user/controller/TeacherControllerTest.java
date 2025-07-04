package saomath.checkusserver.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import saomath.checkusserver.auth.domain.UserRole;
import saomath.checkusserver.auth.jwt.JwtTokenProvider;
import saomath.checkusserver.auth.jwt.JwtAuthenticationEntryPoint;
import saomath.checkusserver.auth.jwt.JwtAuthenticationFilter;
import saomath.checkusserver.auth.service.CustomUserDetailsService;
import saomath.checkusserver.common.config.SecurityConfig;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.user.dto.TeacherDetailResponse;
import saomath.checkusserver.user.dto.TeacherListResponse;
import saomath.checkusserver.user.dto.TeacherUpdateRequest;
import saomath.checkusserver.user.service.TeacherService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeacherController.class)
@Import(SecurityConfig.class)
@DisplayName("TeacherController 통합 테스트")
class TeacherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TeacherService teacherService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private TeacherListResponse teacherListResponse;
    private TeacherDetailResponse teacherDetailResponse;

    @BeforeEach
    void setUp() {
        // 테스트용 교사 목록 응답 데이터
        TeacherListResponse.ClassInfo classInfo1 = new TeacherListResponse.ClassInfo(1L, "고1 수학");
        TeacherListResponse.ClassInfo classInfo2 = new TeacherListResponse.ClassInfo(2L, "고2 수학");

        teacherListResponse = new TeacherListResponse();
        teacherListResponse.setId(1L);
        teacherListResponse.setUsername("teacher1");
        teacherListResponse.setName("김선생님");
        teacherListResponse.setPhoneNumber("010-1234-5678");
        teacherListResponse.setDiscordId("teacher1#1234");
        teacherListResponse.setCreatedAt(LocalDateTime.now());
        teacherListResponse.setStatus(UserRole.RoleStatus.ACTIVE);
        teacherListResponse.setClasses(Arrays.asList(classInfo1, classInfo2));

        // 테스트용 교사 상세 응답 데이터
        TeacherDetailResponse.ClassDetailInfo classDetailInfo1 = 
                new TeacherDetailResponse.ClassDetailInfo(1L, "고1 수학", 15);
        TeacherDetailResponse.ClassDetailInfo classDetailInfo2 = 
                new TeacherDetailResponse.ClassDetailInfo(2L, "고2 수학", 18);

        teacherDetailResponse = new TeacherDetailResponse();
        teacherDetailResponse.setId(1L);
        teacherDetailResponse.setUsername("teacher1");
        teacherDetailResponse.setName("김선생님");
        teacherDetailResponse.setPhoneNumber("010-1234-5678");
        teacherDetailResponse.setDiscordId("teacher1#1234");
        teacherDetailResponse.setCreatedAt(LocalDateTime.now());
        teacherDetailResponse.setStatus(UserRole.RoleStatus.ACTIVE);
        teacherDetailResponse.setClasses(Arrays.asList(classDetailInfo1, classDetailInfo2));
    }

    @Test
    @DisplayName("GET /teachers - 교사 목록 조회 성공")
    @WithMockUser(roles = {"ADMIN", "TEACHER"})
    void getTeachers_Success() throws Exception {
        // given
        List<TeacherListResponse> teachers = Arrays.asList(teacherListResponse);
        given(teacherService.getActiveTeachers("ACTIVE")).willReturn(teachers);

        // when & then
        mockMvc.perform(get("/teachers")
                        .param("status", "ACTIVE"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("교사 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("김선생님"))
                .andExpect(jsonPath("$.data[0].username").value("teacher1"))
                .andExpect(jsonPath("$.data[0].phoneNumber").value("010-1234-5678"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].classes").isArray())
                .andExpect(jsonPath("$.data[0].classes[0].name").value("고1 수학"))
                .andExpect(jsonPath("$.data[0].classes[1].name").value("고2 수학"));
    }

    @Test
    @DisplayName("GET /teachers - 기본 상태값으로 조회")
    @WithMockUser(roles = {"ADMIN"})
    void getTeachers_DefaultStatus() throws Exception {
        // given
        given(teacherService.getActiveTeachers("ACTIVE")).willReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/teachers"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("GET /teachers - 서비스 예외 발생")
    @WithMockUser(roles = {"ADMIN"})
    void getTeachers_ServiceException() throws Exception {
        // given
        given(teacherService.getActiveTeachers(anyString()))
                .willThrow(new RuntimeException("서비스 오류"));

        // when & then
        mockMvc.perform(get("/teachers"))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("교사 목록 조회에 실패했습니다: 서비스 오류"));
    }

    @Test
    @DisplayName("GET /teachers/{id} - 교사 상세 정보 조회 성공")
    @WithMockUser(roles = {"ADMIN", "TEACHER"})
    void getTeacherDetail_Success() throws Exception {
        // given
        given(teacherService.getTeacherDetail(1L)).willReturn(teacherDetailResponse);

        // when & then
        mockMvc.perform(get("/teachers/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("교사 상세 정보 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("김선생님"))
                .andExpect(jsonPath("$.data.classes").isArray())
                .andExpect(jsonPath("$.data.classes[0].studentCount").value(15))
                .andExpect(jsonPath("$.data.classes[1].studentCount").value(18));
    }

    @Test
    @DisplayName("GET /teachers/{id} - 존재하지 않는 교사")
    @WithMockUser(roles = {"ADMIN"})
    void getTeacherDetail_NotFound() throws Exception {
        // given
        given(teacherService.getTeacherDetail(999L))
                .willThrow(new ResourceNotFoundException("교사를 찾을 수 없습니다"));

        // when & then
        mockMvc.perform(get("/teachers/999"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("교사를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("PUT /teachers/{id} - 교사 정보 수정 성공")
    @WithMockUser(roles = {"ADMIN"})
    void updateTeacher_Success() throws Exception {
        // given
        TeacherUpdateRequest updateRequest = new TeacherUpdateRequest();
        updateRequest.setName("수정된 이름");
        updateRequest.setPhoneNumber("010-9999-8888");
        updateRequest.setDiscordId("newdiscord#1234");
        updateRequest.setClassIds(Arrays.asList(1L, 2L));

        TeacherDetailResponse updatedResponse = new TeacherDetailResponse();
        updatedResponse.setId(1L);
        updatedResponse.setName("수정된 이름");
        updatedResponse.setPhoneNumber("010-9999-8888");
        updatedResponse.setDiscordId("newdiscord#1234");

        given(teacherService.updateTeacher(eq(1L), any(TeacherUpdateRequest.class)))
                .willReturn(updatedResponse);

        // when & then
        mockMvc.perform(put("/teachers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("교사 정보 수정 성공"))
                .andExpect(jsonPath("$.data.name").value("수정된 이름"))
                .andExpect(jsonPath("$.data.phoneNumber").value("010-9999-8888"));
    }

    @Test
    @DisplayName("PUT /teachers/{id} - 잘못된 전화번호 형식")
    @WithMockUser(roles = {"ADMIN"})
    void updateTeacher_InvalidPhoneNumber() throws Exception {
        // given
        TeacherUpdateRequest updateRequest = new TeacherUpdateRequest();
        updateRequest.setPhoneNumber("01012345678"); // 잘못된 형식

        // when & then
        mockMvc.perform(put("/teachers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /teachers/{id} - 존재하지 않는 교사 수정")
    @WithMockUser(roles = {"ADMIN"})
    void updateTeacher_NotFound() throws Exception {
        // given
        TeacherUpdateRequest updateRequest = new TeacherUpdateRequest();
        updateRequest.setName("수정된 이름");

        given(teacherService.updateTeacher(eq(999L), any(TeacherUpdateRequest.class)))
                .willThrow(new ResourceNotFoundException("교사를 찾을 수 없습니다"));

        // when & then
        mockMvc.perform(put("/teachers/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("교사를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("DELETE /teachers/{id} - 교사 삭제 성공")
    @WithMockUser(roles = {"ADMIN"})
    void deleteTeacher_Success() throws Exception {
        // given
        willDoNothing().given(teacherService).deleteTeacher(1L);

        // when & then
        mockMvc.perform(delete("/teachers/1")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("교사 삭제 성공"))
                .andExpect(jsonPath("$.data").value("success"));
    }

    @Test
    @DisplayName("DELETE /teachers/{id} - 존재하지 않는 교사 삭제")
    @WithMockUser(roles = {"ADMIN"})
    void deleteTeacher_NotFound() throws Exception {
        // given
        willThrow(new ResourceNotFoundException("교사를 찾을 수 없습니다"))
                .given(teacherService).deleteTeacher(999L);

        // when & then
        mockMvc.perform(delete("/teachers/999")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("교사를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("DELETE /teachers/{id} - 서비스 예외 발생")
    @WithMockUser(roles = {"ADMIN"})
    void deleteTeacher_ServiceException() throws Exception {
        // given
        willThrow(new RuntimeException("삭제 중 오류 발생"))
                .given(teacherService).deleteTeacher(1L);

        // when & then
        mockMvc.perform(delete("/teachers/1")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("교사 삭제에 실패했습니다: 삭제 중 오류 발생"));
    }

    @Test
    @DisplayName("GET /teachers - 인증 없이 접근")
    void getTeachers_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/teachers"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /teachers/{id} - CSRF 토큰 없이 요청")
    @WithMockUser(roles = {"ADMIN"})
    void updateTeacher_WithoutCsrf() throws Exception {
        // given
        TeacherUpdateRequest updateRequest = new TeacherUpdateRequest();
        updateRequest.setName("수정된 이름");

        // when & then
        mockMvc.perform(put("/teachers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /teachers/{id} - 빈 요청 본문")
    @WithMockUser(roles = {"ADMIN"})
    void updateTeacher_EmptyBody() throws Exception {
        // when & then
        mockMvc.perform(put("/teachers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isOk()); // 모든 필드가 optional이므로 성공해야 함
    }

    @Test
    @DisplayName("PUT /teachers/{id} - 잘못된 JSON 형식")
    @WithMockUser(roles = {"ADMIN"})
    void updateTeacher_InvalidJson() throws Exception {
        // when & then
        mockMvc.perform(put("/teachers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid json"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /teachers/{id} - 잘못된 ID 형식")
    @WithMockUser(roles = {"ADMIN"})
    void getTeacherDetail_InvalidIdFormat() throws Exception {
        // when & then
        mockMvc.perform(get("/teachers/invalid"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /teachers/{id} - 이름 길이 초과")
    @WithMockUser(roles = {"ADMIN"})
    void updateTeacher_NameTooLong() throws Exception {
        // given
        TeacherUpdateRequest updateRequest = new TeacherUpdateRequest();
        updateRequest.setName("가".repeat(101)); // 100자 초과

        // when & then
        mockMvc.perform(put("/teachers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /teachers/{id} - Discord ID 길이 초과")
    @WithMockUser(roles = {"ADMIN"})
    void updateTeacher_DiscordIdTooLong() throws Exception {
        // given
        TeacherUpdateRequest updateRequest = new TeacherUpdateRequest();
        updateRequest.setDiscordId("a".repeat(101)); // 100자 초과

        // when & then
        mockMvc.perform(put("/teachers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
