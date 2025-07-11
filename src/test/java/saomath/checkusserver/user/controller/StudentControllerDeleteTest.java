package saomath.checkusserver.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.user.service.StudentService;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
class StudentControllerDeleteTest {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private StudentService studentService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("학생 삭제 성공")
    @WithMockUser(roles = "TEACHER")
    void deleteStudent_Success() throws Exception {
        // Given
        setUp();
        Long studentId = 1L;
        doNothing().when(studentService).deleteStudent(studentId);

        // When & Then
        mockMvc.perform(delete("/students/{id}", studentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학생 삭제 성공"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(studentService).deleteStudent(studentId);
    }

    @Test
    @DisplayName("학생 삭제 실패 - 학생을 찾을 수 없음")
    @WithMockUser(roles = "TEACHER")
    void deleteStudent_StudentNotFound() throws Exception {
        // Given
        setUp();
        Long studentId = 999L;
        doThrow(new ResourceNotFoundException("학생을 찾을 수 없습니다. ID: " + studentId))
                .when(studentService).deleteStudent(studentId);

        // When & Then
        mockMvc.perform(delete("/students/{id}", studentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("학생을 찾을 수 없습니다. ID: " + studentId))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(studentService).deleteStudent(studentId);
    }

    @Test
    @DisplayName("학생 삭제 실패 - 인증되지 않은 사용자")
    void deleteStudent_Unauthorized() throws Exception {
        // Given
        setUp();
        Long studentId = 1L;

        // When & Then
        mockMvc.perform(delete("/students/{id}", studentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(studentService, never()).deleteStudent(any());
    }

    @Test
    @DisplayName("학생 복구 성공")
    @WithMockUser(roles = "TEACHER")
    void restoreStudent_Success() throws Exception {
        // Given
        setUp();
        Long studentId = 1L;
        doNothing().when(studentService).restoreStudent(studentId);

        // When & Then
        mockMvc.perform(post("/students/{id}/restore", studentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학생 복구 성공"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(studentService).restoreStudent(studentId);
    }

    @Test
    @DisplayName("학생 복구 실패 - 학생을 찾을 수 없음")
    @WithMockUser(roles = "TEACHER")
    void restoreStudent_StudentNotFound() throws Exception {
        // Given
        setUp();
        Long studentId = 999L;
        doThrow(new ResourceNotFoundException("학생을 찾을 수 없습니다. ID: " + studentId))
                .when(studentService).restoreStudent(studentId);

        // When & Then
        mockMvc.perform(post("/students/{id}/restore", studentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("학생을 찾을 수 없습니다. ID: " + studentId))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(studentService).restoreStudent(studentId);
    }

    @Test
    @DisplayName("학생 복구 실패 - 삭제되지 않은 학생")
    @WithMockUser(roles = "TEACHER")
    void restoreStudent_NotDeleted() throws Exception {
        // Given
        setUp();
        Long studentId = 1L;
        doThrow(new ResourceNotFoundException("삭제되지 않은 학생입니다. ID: " + studentId))
                .when(studentService).restoreStudent(studentId);

        // When & Then
        mockMvc.perform(post("/students/{id}/restore", studentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("삭제되지 않은 학생입니다. ID: " + studentId))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(studentService).restoreStudent(studentId);
    }
}
