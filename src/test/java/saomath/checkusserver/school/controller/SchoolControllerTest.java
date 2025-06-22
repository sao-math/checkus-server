package saomath.checkusserver.school.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import saomath.checkusserver.auth.jwt.JwtTokenProvider;
import saomath.checkusserver.auth.service.CustomUserDetailsService;
import saomath.checkusserver.common.exception.BusinessException;
import saomath.checkusserver.common.exception.DuplicateResourceException;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.school.dto.SchoolRequest;
import saomath.checkusserver.school.dto.SchoolResponse;
import saomath.checkusserver.school.service.SchoolService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SchoolController.class)
@DisplayName("SchoolController 테스트")
class SchoolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SchoolService schoolService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /schools - 학교 목록 조회 성공")
    void getSchools_ShouldReturnSchoolList() throws Exception {
        // given
        List<SchoolResponse> schools = Arrays.asList(
                new SchoolResponse(1L, "이현중학교", 15L),
                new SchoolResponse(2L, "손곡중학교", 8L)
        );
        when(schoolService.getAllSchools()).thenReturn(schools);

        // when & then
        mockMvc.perform(get("/schools"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학교 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("이현중학교"))
                .andExpect(jsonPath("$.data[0].studentCount").value(15))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("손곡중학교"))
                .andExpect(jsonPath("$.data[1].studentCount").value(8));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("POST /schools - 학교 생성 성공")
    void createSchool_ShouldSucceed_WhenValidRequest() throws Exception {
        // given
        SchoolRequest request = new SchoolRequest("새로운중학교");
        SchoolResponse response = new SchoolResponse(3L, "새로운중학교", 0L);
        when(schoolService.createSchool(any(SchoolRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/schools")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학교가 성공적으로 생성되었습니다."))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.name").value("새로운중학교"))
                .andExpect(jsonPath("$.data.studentCount").value(0));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("POST /schools - 학교 생성 실패 (중복된 학교명)")
    void createSchool_ShouldFail_WhenDuplicateName() throws Exception {
        // given
        SchoolRequest request = new SchoolRequest("이현중학교");
        when(schoolService.createSchool(any(SchoolRequest.class)))
                .thenThrow(new DuplicateResourceException("이미 존재하는 학교명입니다: 이현중학교"));

        // when & then
        mockMvc.perform(post("/schools")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 존재하는 학교명입니다: 이현중학교"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("DELETE /schools/{schoolId} - 학교 삭제 성공")
    void deleteSchool_ShouldSucceed_WhenValidId() throws Exception {
        // given
        Long schoolId = 1L;
        doNothing().when(schoolService).deleteSchool(schoolId);

        // when & then
        mockMvc.perform(delete("/schools/{schoolId}", schoolId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학교가 성공적으로 삭제되었습니다."))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("DELETE /schools/{schoolId} - 학교 삭제 실패 (학교를 찾을 수 없음)")
    void deleteSchool_ShouldFail_WhenSchoolNotFound() throws Exception {
        // given
        Long schoolId = 999L;
        doThrow(new ResourceNotFoundException("학교를 찾을 수 없습니다: " + schoolId))
                .when(schoolService).deleteSchool(schoolId);

        // when & then
        mockMvc.perform(delete("/schools/{schoolId}", schoolId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("학교를 찾을 수 없습니다: " + schoolId));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("DELETE /schools/{schoolId} - 학교 삭제 실패 (연결된 학생이 있음)")
    void deleteSchool_ShouldFail_WhenStudentsConnected() throws Exception {
        // given
        Long schoolId = 1L;
        doThrow(new BusinessException("연결된 학생이 있어 학교를 삭제할 수 없습니다. 학생 수: 5"))
                .when(schoolService).deleteSchool(schoolId);

        // when & then
        mockMvc.perform(delete("/schools/{schoolId}", schoolId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("연결된 학생이 있어 학교를 삭제할 수 없습니다. 학생 수: 5"));
    }

    @Test
    @DisplayName("POST /schools - 인증 없이 학교 생성 시도")
    void createSchool_ShouldFail_WhenNotAuthenticated() throws Exception {
        // given
        SchoolRequest request = new SchoolRequest("새로운중학교");

        // when & then
        mockMvc.perform(post("/schools")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /schools/{schoolId} - 인증 없이 학교 삭제 시도")
    void deleteSchool_ShouldFail_WhenNotAuthenticated() throws Exception {
        // given
        Long schoolId = 1L;

        // when & then
        mockMvc.perform(delete("/schools/{schoolId}", schoolId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}