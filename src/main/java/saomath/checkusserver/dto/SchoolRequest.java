package saomath.checkusserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "학교 생성 요청 DTO")
public class SchoolRequest {
    
    @NotBlank(message = "학교명은 필수입니다.")
    @Size(min = 1, max = 100, message = "학교명은 1자 이상 100자 이하여야 합니다.")
    @Schema(description = "학교명", example = "이현중", required = true)
    private String name;
} 