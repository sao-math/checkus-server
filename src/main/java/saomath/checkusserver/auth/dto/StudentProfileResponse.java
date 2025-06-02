package saomath.checkusserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import saomath.checkusserver.entity.StudentProfile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "학생 프로필 정보")
public class StudentProfileResponse {
    
    @Schema(description = "학생 상태", example = "ENROLLED")
    private StudentProfile.StudentStatus status;
    
    @Schema(description = "학교 정보")
    private SchoolResponse school;
    
    @Schema(description = "학년", example = "2")
    private Integer grade;
    
    @Schema(description = "성별", example = "MALE")
    private StudentProfile.Gender gender;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "학교 정보")
    public static class SchoolResponse {
        @Schema(description = "학교 ID", example = "1")
        private Long id;
        
        @Schema(description = "학교명", example = "이현중")
        private String name;
    }
}