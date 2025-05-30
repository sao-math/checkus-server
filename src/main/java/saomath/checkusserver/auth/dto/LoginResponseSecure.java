package saomath.checkusserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 응답 DTO (RefreshToken은 HttpOnly 쿠키로 제공)")
public class LoginResponseSecure {
    
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    @Schema(description = "사용자명", example = "admin")
    private String username;
    
    @Schema(description = "이름", example = "김철수")
    private String name;
    
    @Schema(description = "사용자 역할 목록", example = "[\"ADMIN\"]")
    private List<String> roles;
    
    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzM4NCJ9...")
    private String accessToken;
    
    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType = "Bearer";
}
