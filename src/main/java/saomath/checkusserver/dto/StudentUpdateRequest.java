package saomath.checkusserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import saomath.checkusserver.entity.StudentProfile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "학생 정보 수정 요청")
public class StudentUpdateRequest {

    @Schema(description = "학생 이름", example = "박학생")
    @Size(min = 1, max = 50, message = "이름은 1자 이상 50자 이하여야 합니다")
    private String name;

    @Schema(description = "학생 전화번호", example = "010-2222-1111")
    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
    private String phoneNumber;

    @Schema(description = "디스코드 ID", example = "student1#1234")
    @Size(max = 100, message = "디스코드 ID는 100자 이하여야 합니다")
    private String discordId;

    @Schema(description = "학생 프로필 정보")
    @Valid
    private StudentProfileUpdateRequest profile;

    @Schema(description = "소속 반 ID 목록")
    private List<Long> classIds;

    @Schema(description = "학부모 정보 목록")
    @Valid
    private List<GuardianUpdateRequest> guardians;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "학생 프로필 수정 정보")
    public static class StudentProfileUpdateRequest {

        @Schema(description = "학생 상태", example = "ENROLLED")
        private StudentProfile.StudentStatus status;

        @Schema(description = "학교 ID", example = "1")
        private Long schoolId;

        @Schema(description = "학년", example = "2")
        private Integer grade;

        @Schema(description = "성별", example = "MALE")
        private StudentProfile.Gender gender;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "학부모 정보 수정")
    public static class GuardianUpdateRequest {

        @Schema(description = "학부모 ID (기존 학부모 수정 시)", example = "5")
        private Long id;

        @Schema(description = "학부모 이름", example = "박학부모")
        @Size(min = 1, max = 50, message = "학부모 이름은 1자 이상 50자 이하여야 합니다")
        private String name;

        @Schema(description = "학부모 전화번호", example = "010-1111-2222")
        @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
        private String phoneNumber;

        @Schema(description = "관계", example = "모")
        @Size(max = 20, message = "관계는 20자 이하여야 합니다")
        private String relationship;
    }
}
