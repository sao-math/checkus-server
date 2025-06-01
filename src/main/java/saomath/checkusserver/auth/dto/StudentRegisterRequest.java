package saomath.checkusserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import saomath.checkusserver.entity.StudentProfile;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "학생 회원가입 요청 DTO")
public class StudentRegisterRequest extends BaseRegisterRequest {
    
    @NotBlank(message = "학교명은 필수입니다.")
    @Schema(description = "학교명", example = "서울고등학교", required = true)
    private String schoolName;
    
    @NotNull(message = "학년은 필수입니다.")
    @Min(value = 1, message = "학년은 1 이상이어야 합니다.")
    @Max(value = 13, message = "학년은 13 이하여야 합니다.")
    @Schema(description = "학년 (1-13)", example = "11", required = true, minimum = "1", maximum = "12")
    private Integer grade;
    
    @NotNull(message = "성별은 필수입니다.")
    @Schema(description = "성별", example = "MALE", required = true, allowableValues = {"MALE", "FEMALE", "OTHER"})
    private StudentProfile.Gender gender;
}
