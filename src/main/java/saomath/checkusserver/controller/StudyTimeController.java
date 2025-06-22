package saomath.checkusserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import saomath.checkusserver.auth.domain.CustomUserPrincipal;
import saomath.checkusserver.auth.dto.ResponseBase;
import saomath.checkusserver.dto.ActualStudyTimeResponse;
import saomath.checkusserver.dto.ActivityResponse;
import saomath.checkusserver.dto.AssignedStudyTimeResponse;
import saomath.checkusserver.dto.AssignStudyTimeRequest;
import saomath.checkusserver.dto.StudyTimeMonitorResponse;
import saomath.checkusserver.dto.UpdateStudyTimeRequest;
import saomath.checkusserver.entity.Activity;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.ActualStudyTime;
import saomath.checkusserver.service.StudyTimeService;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/study-time")
@RequiredArgsConstructor
@Tag(name = "Study Time Management", description = "공부 시간 관리 API")
public class StudyTimeController {

    private final StudyTimeService studyTimeService;

    @Operation(
        summary = "공부 시간 배정",
        description = "선생님이 학생에게 공부 시간을 배정합니다.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "공부 시간 배정 성공",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "배정 성공",
                        value = """
                        {
                          "success": true,
                          "message": "공부 시간이 성공적으로 배정되었습니다.",
                          "data": {
                            "id": 1,
                            "studentId": 1,
                            "studentName": "김학생",
                            "activityId": 1,
                            "activityName": "수학 공부",
                            "startTime": "2025-06-01T10:00:00",
                            "endTime": "2025-06-01T12:00:00",
                            "assignedBy": 2,
                            "assignedByName": "이선생"
                          }
                        }
                        """
                    )
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "배정 실패 - 시간 겹침 또는 잘못된 입력",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "시간 겹침 오류",
                        value = """
                        {
                          "success": false,
                          "message": "해당 시간대에 이미 배정된 공부 시간이 있습니다.",
                          "data": null
                        }
                        """
                    )
                )
            )
        }
    )
    @PostMapping("/assign")
    public ResponseEntity<ResponseBase<AssignedStudyTimeResponse>> assignStudyTime(
            @Valid @RequestBody AssignStudyTimeRequest request) {
        
        try {
            // Spring Security에서 자동으로 주입된 인증 정보 사용
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserPrincipal)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseBase.error("인증이 필요합니다."));
            }
            
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            Long teacherId = principal.getId();
            
            AssignedStudyTime result = studyTimeService.assignStudyTime(
                    request.getStudentId(),
                    request.getTitle(),
                    request.getActivityId(),
                    request.getStartTime(),
                    request.getEndTime(),
                    teacherId
            );
            
            // 연관 엔티티와 함께 다시 조회하여 Response 생성
            AssignedStudyTime resultWithDetails = studyTimeService.getAssignedStudyTimeWithDetails(result.getId());
            AssignedStudyTimeResponse response = convertToAssignedResponse(resultWithDetails != null ? resultWithDetails : result);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseBase.success("공부 시간이 성공적으로 배정되었습니다.", response));
                    
        } catch (Exception e) {
            log.error("공부 시간 배정 실패", e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "배정된 공부 시간 수정",
        description = "기존에 배정된 공부 시간을 수정합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{id}")
    public ResponseEntity<ResponseBase<AssignedStudyTimeResponse>> updateAssignedStudyTime(
            @Parameter(description = "배정 ID") @PathVariable("id") Long id,
            @Valid @RequestBody UpdateStudyTimeRequest request) {
        
        try {
            AssignedStudyTime result = studyTimeService.updateAssignedStudyTime(
                    id,
                    request.getTitle(),
                    request.getActivityId(),
                    request.getStartTime(),
                    request.getEndTime()
            );
            
            // 연관 엔티티와 함께 다시 조회하여 Response 생성
            AssignedStudyTime resultWithDetails = studyTimeService.getAssignedStudyTimeWithDetails(result.getId());
            AssignedStudyTimeResponse response = convertToAssignedResponse(resultWithDetails != null ? resultWithDetails : result);
            
            return ResponseEntity.ok(
                    ResponseBase.success("공부 시간이 성공적으로 수정되었습니다.", response));
                    
        } catch (Exception e) {
            log.error("공부 시간 수정 실패: id={}", id, e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "배정된 공부 시간 삭제",
        description = "배정된 공부 시간을 삭제합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseBase<String>> deleteAssignedStudyTime(
            @Parameter(description = "배정 ID") @PathVariable("id") Long id) {
        
        try {
            studyTimeService.deleteAssignedStudyTime(id);
            
            return ResponseEntity.ok(
                    ResponseBase.success("공부 시간이 성공적으로 삭제되었습니다.", "success"));
                    
        } catch (Exception e) {
            log.error("공부 시간 삭제 실패: id={}", id, e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "학생별 배정된 공부 시간 조회",
        description = "특정 기간의 학생별 배정된 공부 시간을 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/assigned/student/{studentId}")
    public ResponseEntity<ResponseBase<List<AssignedStudyTimeResponse>>> getAssignedStudyTimes(
            @Parameter(description = "학생 ID") @PathVariable("studentId") Long studentId,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            List<AssignedStudyTime> results = studyTimeService.getAssignedStudyTimesByStudentAndDateRange(
                    studentId, startDate, endDate);
            
            List<AssignedStudyTimeResponse> responses = results.stream()
                    .map(this::convertToAssignedResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(
                    ResponseBase.success("배정된 공부 시간을 성공적으로 조회했습니다.", responses));
                    
        } catch (Exception e) {
            log.error("배정된 공부 시간 조회 실패: studentId={}", studentId, e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "학생별 실제 공부 시간 조회",
        description = "특정 기간의 학생별 실제 공부 시간을 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/actual/student/{studentId}")
    public ResponseEntity<ResponseBase<List<ActualStudyTimeResponse>>> getActualStudyTimes(
            @Parameter(description = "학생 ID") @PathVariable("studentId") Long studentId,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            List<ActualStudyTime> results = studyTimeService.getActualStudyTimesByStudentAndDateRange(
                    studentId, startDate, endDate);
            
            List<ActualStudyTimeResponse> responses = results.stream()
                    .map(this::convertToActualResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(
                    ResponseBase.success("실제 공부 시간을 성공적으로 조회했습니다.", responses));
                    
        } catch (Exception e) {
            log.error("실제 공부 시간 조회 실패: studentId={}", studentId, e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "특정 배정의 실제 접속 기록 조회",
        description = "특정 배정된 공부 시간의 실제 접속 기록을 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/actual/assigned/{assignedId}")
    public ResponseEntity<ResponseBase<List<ActualStudyTimeResponse>>> getActualStudyTimesByAssigned(
            @Parameter(description = "배정 ID") @PathVariable("assignedId") Long assignedId) {
        
        try {
            List<ActualStudyTime> results = studyTimeService.getActualStudyTimesByAssignedId(assignedId);
            
            List<ActualStudyTimeResponse> responses = results.stream()
                    .map(this::convertToActualResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(
                    ResponseBase.success("실제 접속 기록을 성공적으로 조회했습니다.", responses));
                    
        } catch (Exception e) {
            log.error("실제 접속 기록 조회 실패: assignedId={}", assignedId, e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }



    @Operation(
        summary = "공부 배정 가능한 활동 목록 조회",
        description = "공부 시간 배정에 사용할 수 있는 활동 목록을 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/activities")
    public ResponseEntity<ResponseBase<List<ActivityResponse>>> getStudyAssignableActivities() {
        
        try {
            List<Activity> activities = studyTimeService.getStudyAssignableActivities();
            
            List<ActivityResponse> responses = activities.stream()
                    .map(this::convertToActivityResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(
                    ResponseBase.success("활동 목록을 성공적으로 조회했습니다.", responses));
                    
        } catch (Exception e) {
            log.error("활동 목록 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "날짜별 학생 공부시간 모니터링 조회",
        description = "특정 날짜의 모든 학생 공부시간 모니터링 정보를 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "모니터링 정보 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "모니터링 조회 성공",
                        value = """
                        {
                          "success": true,
                          "message": "학생 모니터링 정보를 성공적으로 조회했습니다.",
                          "data": {
                            "date": "2025-06-18",
                            "students": [
                              {
                                "studentId": 1,
                                "studentName": "김학생",
                                "studentPhone": "010-1234-5678",
                                "status": "ATTENDING",
                                "guardians": [
                                  {
                                    "guardianId": 2,
                                    "guardianPhone": "010-9876-5432",
                                    "relationship": "부"
                                  }
                                ],
                                "assignedStudyTimes": [
                                  {
                                    "assignedStudyTimeId": 1,
                                    "title": "수학 공부",
                                    "startTime": "2025-06-18T10:00:00",
                                    "endTime": "2025-06-18T12:00:00",
                                    "connectedActualStudyTimes": [
                                      {
                                        "actualStudyTimeId": 1,
                                        "startTime": "2025-06-18T10:05:00",
                                        "endTime": "2025-06-18T11:30:00"
                                      }
                                    ]
                                  }
                                ],
                                "unassignedActualStudyTimes": []
                              }
                            ]
                          }
                        }
                        """
                    )
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "잘못된 날짜 형식",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "날짜 형식 오류",
                        value = """
                        {
                          "success": false,
                          "message": "날짜 형식이 올바르지 않습니다. yyyy-MM-dd 형식으로 입력해주세요.",
                          "data": null
                        }
                        """
                    )
                )
            )
        }
    )
    @GetMapping("/monitor/{date}")
    public ResponseEntity<ResponseBase<StudyTimeMonitorResponse>> getStudyTimeMonitor(
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", example = "2025-06-18") 
            @PathVariable("date") String dateStr
            // TODO: 향후 구현 예정 - 쿼리 파라미터로 필터링
            // @RequestParam(value = "classId", required = false) Long classId,
            // @RequestParam(value = "studentId", required = false) Long studentId,
            // @RequestParam(value = "teacherId", required = false) Long teacherId
    ) {
        
        try {
            // 날짜 파싱
            LocalDate date;
            try {
                date = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest()
                        .body(ResponseBase.error("날짜 형식이 올바르지 않습니다. yyyy-MM-dd 형식으로 입력해주세요."));
            }
            
            StudyTimeMonitorResponse result = studyTimeService.getStudyTimeMonitorByDate(date);
            
            return ResponseEntity.ok(
                    ResponseBase.success("학생 모니터링 정보를 성공적으로 조회했습니다.", result));
                    
        } catch (Exception e) {
            log.error("학생 모니터링 정보 조회 실패: date={}", dateStr, e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    // Helper methods for converting entities to responses
    // TODO: Controller에 Entity → DTO 변환 로직이 있는 것은 안티패턴
    // TODO: Service에서 DTO를 반환하거나 별도 Mapper 클래스로 분리 필요
    // TODO: 현재 비즈니스 로직이 Controller 계층에 섞여있어 재사용성과 테스트성이 떨어짐
    private AssignedStudyTimeResponse convertToAssignedResponse(AssignedStudyTime assignedStudyTime) {
        AssignedStudyTimeResponse response = new AssignedStudyTimeResponse();
        response.setId(assignedStudyTime.getId());
        response.setStudentId(assignedStudyTime.getStudentId());
        response.setTitle(assignedStudyTime.getTitle());
        response.setActivityId(assignedStudyTime.getActivityId());
        response.setStartTime(assignedStudyTime.getStartTime());
        response.setEndTime(assignedStudyTime.getEndTime());
        response.setAssignedBy(assignedStudyTime.getAssignedBy());
        
        // 연관 엔티티가 로드되어 있으면 이름도 설정
        try {
            if (assignedStudyTime.getStudent() != null) {
                response.setStudentName(assignedStudyTime.getStudent().getName());
            }
        } catch (Exception e) {
            log.debug("Student 엔티티 로드 실패: {}", e.getMessage());
        }
        
        try {
            if (assignedStudyTime.getActivity() != null) {
                response.setActivityName(assignedStudyTime.getActivity().getName());
                response.setIsStudyAssignable(assignedStudyTime.getActivity().getIsStudyAssignable());
            }
        } catch (Exception e) {
            log.debug("Activity 엔티티 로드 실패: {}", e.getMessage());
        }
        
        try {
            if (assignedStudyTime.getAssignedByUser() != null) {
                response.setAssignedByName(assignedStudyTime.getAssignedByUser().getName());
            }
        } catch (Exception e) {
            log.debug("AssignedByUser 엔티티 로드 실패: {}", e.getMessage());
        }
        
        return response;
    }

    // TODO: 이 헬퍼 메서드도 Controller에 있으면 안됨 - 별도 Mapper로 분리 필요
    private ActualStudyTimeResponse convertToActualResponse(ActualStudyTime actualStudyTime) {
        ActualStudyTimeResponse response = new ActualStudyTimeResponse();
        response.setId(actualStudyTime.getId());
        response.setStudentId(actualStudyTime.getStudentId());
        response.setAssignedStudyTimeId(actualStudyTime.getAssignedStudyTimeId());
        response.setStartTime(actualStudyTime.getStartTime());
        response.setEndTime(actualStudyTime.getEndTime());
        response.setSource(actualStudyTime.getSource());
        
        // 연관 엔티티가 로드되어 있으면 이름도 설정
        if (actualStudyTime.getStudent() != null) {
            response.setStudentName(actualStudyTime.getStudent().getName());
        }
        
        return response;
    }

    // TODO: 이 헬퍼 메서드도 Controller에 있으면 안됨 - 별도 Mapper로 분리 필요
    private ActivityResponse convertToActivityResponse(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getName(),
                activity.getIsStudyAssignable()
        );
    }
}
