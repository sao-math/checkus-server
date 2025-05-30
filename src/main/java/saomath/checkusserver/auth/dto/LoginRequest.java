package saomath.checkusserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "로그인 요청 DTO")
public class LoginRequest {
    
    @NotBlank(message = "사용자명은 필수입니다.")
    @Schema(description = "사용자명", example = "admin", required = true)
    private String username;
    
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Schema(description = "비밀번호", example = "Password123!", required = true)
    private String password;
}
