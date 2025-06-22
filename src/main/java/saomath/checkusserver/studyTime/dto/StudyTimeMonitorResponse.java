package saomath.checkusserver.studyTime.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "날짜별 학생 공부시간 모니터링 응답")
public class StudyTimeMonitorResponse {
    
    @Schema(description = "조회 날짜", example = "2025-06-18")
    private LocalDate date;
    
    @Schema(description = "학생별 공부시간 정보")
    private List<StudentStudyInfo> students;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentStudyInfo {
        
        @Schema(description = "학생 ID", example = "1")
        private Long studentId;
        
        @Schema(description = "학생 이름", example = "김학생")
        private String studentName;
        
        @Schema(description = "학생 전화번호", example = "010-1234-5678")
        private String studentPhone;
        
        @Schema(description = "학생 현재 상태")
        private StudentCurrentStatus status;
        
        @Schema(description = "보호자 목록")
        private List<GuardianInfo> guardians;
        
        @Schema(description = "할당된 공부시간 목록")
        private List<AssignedStudyInfo> assignedStudyTimes;
        
        @Schema(description = "할당되지 않은 실제 접속 기록 목록")
        private List<UnassignedActualStudyInfo> unassignedActualStudyTimes;
    }
    
    @Schema(description = "학생 현재 상태")
    public enum StudentCurrentStatus {
        ATTENDING("출석"),
        ABSENT("결석"),
        NO_ASSIGNED_TIME("없음");
        
        private final String description;
        
        StudentCurrentStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuardianInfo {
        @Schema(description = "보호자 ID", example = "2")
        private Long guardianId;
        
        @Schema(description = "보호자 전화번호", example = "010-9876-5432")
        private String guardianPhone;
        
        @Schema(description = "보호자 관계", example = "부")
        private String relationship;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignedStudyInfo {
        @Schema(description = "할당된 공부시간 ID", example = "1")
        private Long assignedStudyTimeId;
        
        @Schema(description = "할당된 공부시간 제목", example = "수학 공부")
        private String title;
        
        @Schema(description = "할당된 공부시간 시작 시간", example = "2025-06-18T10:00:00")
        private LocalDateTime startTime;
        
        @Schema(description = "할당된 공부시간 종료 시간", example = "2025-06-18T12:00:00")
        private LocalDateTime endTime;
        
        @Schema(description = "이 할당된 공부시간에 연결된 실제 접속 기록 목록")
        private List<ConnectedActualStudyInfo> connectedActualStudyTimes;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectedActualStudyInfo {
        @Schema(description = "연결된 실제 접속 기록 ID", example = "1")
        private Long actualStudyTimeId;
        
        @Schema(description = "연결된 실제 접속 기록 시작 시간", example = "2025-06-18T10:05:00")
        private LocalDateTime startTime;
        
        @Schema(description = "연결된 실제 접속 기록 종료 시간", example = "2025-06-18T11:30:00")
        private LocalDateTime endTime;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnassignedActualStudyInfo {
        @Schema(description = "실제 접속 기록 ID", example = "2")
        private Long actualStudyTimeId;
        
        @Schema(description = "실제 접속 기록 시작 시간", example = "2025-06-18T14:00:00")
        private LocalDateTime startTime;
        
        @Schema(description = "실제 접속 기록 종료 시간", example = "2025-06-18T15:30:00")
        private LocalDateTime endTime;
    }
}
