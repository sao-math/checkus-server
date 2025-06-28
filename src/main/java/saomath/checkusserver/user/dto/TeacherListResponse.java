package saomath.checkusserver.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import saomath.checkusserver.auth.domain.UserRole;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherListResponse {
    private Long id;
    private String username;
    private String name;
    private String phoneNumber;
    private String discordId;
    private LocalDateTime createdAt;
    private UserRole.RoleStatus status;
    private List<ClassInfo> classes; // 담당 반 목록

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassInfo {
        private Long id;
        private String name;
    }
} 