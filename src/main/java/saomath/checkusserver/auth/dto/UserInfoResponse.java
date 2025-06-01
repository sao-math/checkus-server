package saomath.checkusserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 정보 응답 DTO")
public class UserInfoResponse {
    
    @Schema(description = "사용자 ID", example = "1")
    private Long id;
    
    @Schema(description = "사용자명", example = "admin")
    private String username;
    
    @Schema(description = "사용자 이름", example = "김철수")
    private String name;
    
    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phoneNumber;
    
    @Schema(description = "디스코드 ID", example = "username#1234")
    private String discordId;
    
    @Schema(description = "사용자 역할 목록", example = "[\"ADMIN\", \"TEACHER\"]")
    private List<String> roles;
    
    @Schema(description = "계정 생성일시", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "학생 프로필 정보 (학생인 경우에만 포함)", nullable = true)
    private StudentProfileResponse studentProfile;

    // 기존 생성자 (연결 정보 없이)
    public UserInfoResponse(Long id, String username, String name, String phoneNumber,
                           String discordId, List<String> roles, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.discordId = discordId;
        this.roles = roles;
        this.createdAt = createdAt;
        this.studentProfile = null;
    }
}