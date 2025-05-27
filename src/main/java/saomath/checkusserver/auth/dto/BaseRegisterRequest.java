package saomath.checkusserver.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import saomath.checkusserver.validation.ValidPassword;
import saomath.checkusserver.validation.ValidPhoneNumber;
import saomath.checkusserver.validation.ValidUsername;

@Data
public abstract class BaseRegisterRequest {
    
    @NotBlank(message = "사용자명은 필수입니다.")
    @ValidUsername
    private String username;
    
    @NotBlank(message = "비밀번호는 필수입니다.")
    @ValidPassword
    private String password;
    
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
    private String name;
    
    @NotBlank(message = "전화번호는 필수입니다.")
    @ValidPhoneNumber
    private String phoneNumber;
    
    // Discord ID는 선택사항
    private String discordId;
}
