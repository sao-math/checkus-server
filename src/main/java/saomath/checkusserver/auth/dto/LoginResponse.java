package saomath.checkusserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "로그인 응답 DTO")
public class LoginResponse {
    
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    @Schema(description = "사용자명", example = "admin")
    private String username;
    
    @Schema(description = "사용자 이름", example = "김철수")
    private String name;
    
    @Schema(description = "사용자 역할 목록", example = "[\"ADMIN\"]")
    private List<String> roles;
    
    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzM4NCJ9...")
    private String accessToken;
    
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzM4NCJ9...")
    private String refreshToken;
    
    @Schema(description = "토큰 타입", example = "Bearer", defaultValue = "Bearer")
    private final String tokenType = "Bearer";
}
