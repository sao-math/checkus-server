package saomath.checkusserver.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import saomath.checkusserver.notification.dto.NotificationSendRequest;
import saomath.checkusserver.notification.dto.NotificationSendResponse;
import saomath.checkusserver.notification.dto.NotificationTemplateDto;
import saomath.checkusserver.notification.service.NotificationSendService;
import saomath.checkusserver.notification.service.DirectAlimtalkService;
import saomath.checkusserver.notification.service.NotificationPreferenceService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationSendService notificationSendService;

    @MockitoBean
    private DirectAlimtalkService directAlimtalkService;

    @MockitoBean
    private NotificationPreferenceService notificationPreferenceService;

    private NotificationSendRequest validRequest;
    private NotificationSendResponse mockResponse;

    @BeforeEach
    void setUp() {
        validRequest = new NotificationSendRequest();
        validRequest.setStudentId(123L);
        validRequest.setDeliveryMethod("alimtalk");
        validRequest.setTemplateId("STUDY_REMINDER_10MIN");

        mockResponse = NotificationSendResponse.builder()
                .deliveryMethod("alimtalk")
                .recipient("01012345678")
                .sentMessage("[사오수학]\n홍길동 학생, \n곧 공부 시작할 시간이에요!")
                .templateUsed("STUDY_REMINDER_10MIN")
                .build();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void sendNotification_Success_WithTemplate() throws Exception {
        // Given
        when(notificationSendService.sendNotification(any(NotificationSendRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/notifications/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림이 성공적으로 발송되었습니다."))
                .andExpect(jsonPath("$.data.deliveryMethod").value("alimtalk"))
                .andExpect(jsonPath("$.data.recipient").value("01012345678"))
                .andExpect(jsonPath("$.data.templateUsed").value("STUDY_REMINDER_10MIN"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_TEACHER")
    void sendNotification_Success_WithCustomMessage() throws Exception {
        // Given
        NotificationSendRequest customRequest = new NotificationSendRequest();
        customRequest.setStudentId(123L);
        customRequest.setDeliveryMethod("discord");
        customRequest.setCustomMessage("안녕하세요! 오늘 과제 확인 부탁드립니다.");

        NotificationSendResponse customResponse = NotificationSendResponse.builder()
                .deliveryMethod("discord")
                .recipient("student_discord_id")
                .sentMessage("안녕하세요! 오늘 과제 확인 부탁드립니다.")
                .templateUsed(null)
                .build();

        when(notificationSendService.sendNotification(any(NotificationSendRequest.class)))
                .thenReturn(customResponse);

        // When & Then
        mockMvc.perform(post("/notifications/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deliveryMethod").value("discord"))
                .andExpect(jsonPath("$.data.templateUsed").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void sendNotification_Forbidden_WrongRole() throws Exception {
        // When & Then
        mockMvc.perform(post("/notifications/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void sendNotification_Unauthorized_NoAuthentication() throws Exception {
        // Given - 인증 없이 요청
        
        // When & Then - 인증 부족으로 401 예상
        mockMvc.perform(post("/notifications/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void sendNotification_BadRequest_MissingStudentId() throws Exception {
        // Given
        NotificationSendRequest invalidRequest = new NotificationSendRequest();
        invalidRequest.setDeliveryMethod("alimtalk");
        invalidRequest.setTemplateId("STUDY_REMINDER_10MIN");

        // When & Then
        mockMvc.perform(post("/notifications/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_TEACHER")
    void getNotificationTemplates_Success() throws Exception {
        // Given
        List<NotificationTemplateDto> mockTemplates = Arrays.asList(
                NotificationTemplateDto.builder()
                        .id("STUDY_REMINDER_10MIN")
                        .name("공부 시작 10분 전 알림")
                        .description("공부 시작 10분 전 알림")
                        .previewMessage("[사오수학]\n#{이름} 학생, \n곧 공부 시작할 시간이에요!")
                        .requiredVariables(Arrays.asList("이름"))
                        .build(),
                NotificationTemplateDto.builder()
                        .id("STUDY_START")
                        .name("공부 시작 시간 알림")
                        .description("공부 시작 시간 알림")
                        .previewMessage("[사오수학]\n#{이름} 학생, \n공부 시작할 시간입니다!")
                        .requiredVariables(Arrays.asList("이름"))
                        .build()
        );

        when(notificationSendService.getAvailableTemplates()).thenReturn(mockTemplates);

        // When & Then
        mockMvc.perform(get("/notifications/templates")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("템플릿 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value("STUDY_REMINDER_10MIN"))
                .andExpect(jsonPath("$.data[0].name").value("공부 시작 10분 전 알림"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getNotificationTemplates_Forbidden_WrongRole() throws Exception {
        // When & Then
        mockMvc.perform(get("/notifications/templates")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
