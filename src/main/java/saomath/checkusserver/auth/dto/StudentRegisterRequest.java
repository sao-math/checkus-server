package saomath.checkusserver.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import saomath.checkusserver.entity.StudentProfile;

@Data
@EqualsAndHashCode(callSuper = true)
public class StudentRegisterRequest extends BaseRegisterRequest {
    
    @NotBlank(message = "학교명은 필수입니다.")
    private String schoolName;
    
    @NotNull(message = "학년은 필수입니다.")
    @Min(value = 1, message = "학년은 1 이상이어야 합니다.")
    @Max(value = 12, message = "학년은 12 이하여야 합니다.")
    private Integer grade;
    
    @NotNull(message = "성별은 필수입니다.")
    private StudentProfile.Gender gender;
}
