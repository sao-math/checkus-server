package saomath.checkusserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 리프레시 응답 DTO (RefreshToken은 HttpOnly 쿠키로 제공)")
public class TokenRefreshResponseSecure {
    
    @Schema(description = "새로운 액세스 토큰", example = "eyJhbGciOiJIUzM4NCJ9...")
    private String accessToken;
}
