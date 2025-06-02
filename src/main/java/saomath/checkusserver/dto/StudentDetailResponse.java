package saomath.checkusserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import saomath.checkusserver.dto.common.StudentProfileInfo;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailResponse {
    private Long id;
    private String username;
    private String name;
    private String phoneNumber;
    private String discordId;
    private LocalDateTime createdAt;
    
    // 학생 프로필 정보 (공통 구조 사용)
    private StudentProfileInfo profile;
    
    // 소속 반 정보
    private List<ClassResponse> classes;
    
    // 학부모 정보
    private List<GuardianResponse> guardians;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassResponse {
        private Long id;
        private String name;
    }
}
