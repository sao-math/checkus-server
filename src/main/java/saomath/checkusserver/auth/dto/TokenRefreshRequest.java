package saomath.checkusserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "토큰 리프레시 요청 DTO")
public class TokenRefreshRequest {
    
    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzM4NCJ9...", required = true)
    private String refreshToken;
}
