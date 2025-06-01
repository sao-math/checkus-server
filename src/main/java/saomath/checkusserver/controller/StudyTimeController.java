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
import saomath.checkusserver.auth.CustomUserPrincipal;
import saomath.checkusserver.auth.dto.ResponseBase;
import saomath.checkusserver.dto.*;
import saomath.checkusserver.entity.Activity;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.ActualStudyTime;
import saomath.checkusserver.service.StudyTimeService;

import java.time.LocalDateTime;
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
            // 현재 인증된 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            Long teacherId = principal.getId();
            
            AssignedStudyTime result = studyTimeService.assignStudyTime(
                    request.getStudentId(),
                    request.getActivityId(),
                    request.getStartTime(),
                    request.getEndTime(),
                    teacherId
            );
            
            AssignedStudyTimeResponse response = convertToAssignedResponse(result);
            
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
            @Parameter(description = "배정 ID") @PathVariable Long id,
            @Valid @RequestBody UpdateStudyTimeRequest request) {
        
        try {
            AssignedStudyTime result = studyTimeService.updateAssignedStudyTime(
                    id,
                    request.getActivityId(),
                    request.getStartTime(),
                    request.getEndTime()
            );
            
            AssignedStudyTimeResponse response = convertToAssignedResponse(result);
            
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
            @Parameter(description = "배정 ID") @PathVariable Long id) {
        
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
            @Parameter(description = "학생 ID") @PathVariable Long studentId,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
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
            @Parameter(description = "학생 ID") @PathVariable Long studentId,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
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
            @Parameter(description = "배정 ID") @PathVariable Long assignedId) {
        
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
        summary = "디스코드 봇용 - 공부 시작 기록",
        description = "디스코드 봇이 학생의 공부 시작을 기록합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/record/start")
    public ResponseEntity<ResponseBase<ActualStudyTimeResponse>> recordStudyStart(
            @Valid @RequestBody RecordStudyStartRequest request) {
        
        try {
            ActualStudyTime result = studyTimeService.recordStudyStart(
                    request.getStudentId(),
                    request.getStartTime(),
                    request.getSource()
            );
            
            ActualStudyTimeResponse response = convertToActualResponse(result);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseBase.success("공부 시작이 성공적으로 기록되었습니다.", response));
                    
        } catch (Exception e) {
            log.error("공부 시작 기록 실패", e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "디스코드 봇용 - 공부 종료 기록",
        description = "디스코드 봇이 학생의 공부 종료를 기록합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/record/{actualStudyTimeId}/end")
    public ResponseEntity<ResponseBase<ActualStudyTimeResponse>> recordStudyEnd(
            @Parameter(description = "실제 공부 시간 기록 ID") @PathVariable Long actualStudyTimeId,
            @Valid @RequestBody RecordStudyEndRequest request) {
        
        try {
            ActualStudyTime result = studyTimeService.recordStudyEnd(
                    actualStudyTimeId,
                    request.getEndTime()
            );
            
            ActualStudyTimeResponse response = convertToActualResponse(result);
            
            return ResponseEntity.ok(
                    ResponseBase.success("공부 종료가 성공적으로 기록되었습니다.", response));
                    
        } catch (Exception e) {
            log.error("공부 종료 기록 실패: actualStudyTimeId={}", actualStudyTimeId, e);
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
        summary = "알림용 - 곧 시작할 공부 시간 조회",
        description = "알림 시스템에서 사용하는 곧 시작할 공부 시간 목록을 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/upcoming")
    public ResponseEntity<ResponseBase<List<AssignedStudyTimeResponse>>> getUpcomingStudyTimes() {
        
        try {
            List<AssignedStudyTime> results = studyTimeService.getUpcomingStudyTimes();
            
            List<AssignedStudyTimeResponse> responses = results.stream()
                    .map(this::convertToAssignedResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(
                    ResponseBase.success("곧 시작할 공부 시간을 성공적으로 조회했습니다.", responses));
                    
        } catch (Exception e) {
            log.error("곧 시작할 공부 시간 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ResponseBase.error(e.getMessage()));
        }
    }

    // Helper methods for converting entities to responses
    private AssignedStudyTimeResponse convertToAssignedResponse(AssignedStudyTime assignedStudyTime) {
        AssignedStudyTimeResponse response = new AssignedStudyTimeResponse();
        response.setId(assignedStudyTime.getId());
        response.setStudentId(assignedStudyTime.getStudentId());
        response.setActivityId(assignedStudyTime.getActivityId());
        response.setStartTime(assignedStudyTime.getStartTime());
        response.setEndTime(assignedStudyTime.getEndTime());
        response.setAssignedBy(assignedStudyTime.getAssignedBy());
        
        // 연관 엔티티가 로드되어 있으면 이름도 설정
        if (assignedStudyTime.getStudent() != null) {
            response.setStudentName(assignedStudyTime.getStudent().getName());
        }
        if (assignedStudyTime.getActivity() != null) {
            response.setActivityName(assignedStudyTime.getActivity().getName());
        }
        if (assignedStudyTime.getAssignedByUser() != null) {
            response.setAssignedByName(assignedStudyTime.getAssignedByUser().getName());
        }
        
        return response;
    }

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

    private ActivityResponse convertToActivityResponse(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getName(),
                activity.getIsStudyAssignable()
        );
    }
}
