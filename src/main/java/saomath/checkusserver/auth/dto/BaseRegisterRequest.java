package saomath.checkusserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import saomath.checkusserver.validation.ValidPassword;
import saomath.checkusserver.validation.ValidPhoneNumber;
import saomath.checkusserver.validation.ValidUsername;

@Data
@Schema(description = "회원가입 기본 요청 DTO")
public abstract class BaseRegisterRequest {
    
    @NotBlank(message = "사용자명은 필수입니다.")
    @ValidUsername
    @Schema(description = "사용자명 (4-20자, 영문자+숫자+언더바)", example = "student123", required = true)
    private String username;
    
    @NotBlank(message = "비밀번호는 필수입니다.")
    @ValidPassword
    @Schema(description = "비밀번호 (8자 이상, 영문대소문자+숫자+특수문자 포함)", example = "Password123!", required = true)
    private String password;
    
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
    @Schema(description = "이름", example = "김철수", required = true, maxLength = 50)
    private String name;
    
    @NotBlank(message = "전화번호는 필수입니다.")
    @ValidPhoneNumber
    @Schema(description = "전화번호 (010-0000-0000 형식)", example = "010-1234-5678", required = true)
    private String phoneNumber;
    
    // Discord ID는 선택사항
    @Schema(description = "디스코드 ID (선택사항)", example = "username#1234")
    private String discordId;
}
