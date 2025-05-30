package saomath.checkusserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
@DisplayName("관리자 컨트롤러 통합 테스트")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 테스트 전 설정
    }

    @Test
    @DisplayName("관리자 권한 없이 역할 요청 조회 시 403 에러")
    void getRoleRequests_WithoutAdminRole_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/admin/role-requests")
                .param("roleName", "STUDENT"))
                .andExpect(status().isUnauthorized()); // JWT 토큰 없음
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("관리자 권한으로 역할 요청 조회 성공")
    void getRoleRequests_WithAdminRole_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/admin/role-requests")
                .param("roleName", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("역할 직접 할당 API 테스트")
    void assignRole_WithAdminRole_ShouldWork() throws Exception {
        mockMvc.perform(post("/admin/assign-role")
                .param("userId", "1")
                .param("roleName", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("일반 사용자 권한으로 역할 할당 시도 시 권한 없음 에러")
    @WithMockUser(roles = "USER")
    void assignRole_WithUserRole_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/admin/assign-role")
                .param("userId", "1")
                .param("roleName", "STUDENT"))
                .andExpect(status().isForbidden());
    }
}
