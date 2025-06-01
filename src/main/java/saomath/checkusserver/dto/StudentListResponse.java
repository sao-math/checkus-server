package saomath.checkusserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import saomath.checkusserver.entity.StudentProfile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentListResponse {
    private Long id;
    private String name;
    private String phoneNumber;
    private String studentPhoneNumber;
    private String school;
    private Integer grade;
    private List<String> classes; // 소속 반 목록
    private StudentProfile.StudentStatus status;
    private List<GuardianResponse> guardians;
}
