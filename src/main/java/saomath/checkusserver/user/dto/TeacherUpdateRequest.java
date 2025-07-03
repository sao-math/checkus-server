package saomath.checkusserver.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherUpdateRequest {
    
    @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다.")
    private String name;
    
    @Pattern(regexp = "^01[0-9]-[0-9]{4}-[0-9]{4}$", message = "올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)")
    private String phoneNumber;
    
    @Size(max = 100, message = "Discord ID는 100자를 초과할 수 없습니다.")
    private String discordId;
    
    private List<Long> classIds; // 담당 반 ID 목록
} 