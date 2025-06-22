package saomath.checkusserver.weeklySchedule.controller;

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
import org.springframework.web.bind.annotation.*;
import saomath.checkusserver.auth.dto.ResponseBase;
import saomath.checkusserver.studyTime.dto.ActivityResponse;
import saomath.checkusserver.weeklySchedule.dto.WeeklySchedulePeriodResponse;
import saomath.checkusserver.weeklySchedule.dto.WeeklyScheduleRequest;
import saomath.checkusserver.weeklySchedule.dto.WeeklyScheduleResponse;
import saomath.checkusserver.studyTime.domain.Activity;
import saomath.checkusserver.common.exception.BusinessException;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.weeklySchedule.service.WeeklyScheduleService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/weekly-schedule")
@RequiredArgsConstructor
@Tag(name = "Weekly Schedule", description = "주간 시간표 관리 API")
public class WeeklyScheduleController {

    private final WeeklyScheduleService weeklyScheduleService;

    @Operation(
            summary = "학생 주간 시간표 조회",
            description = "특정 학생의 주간 시간표를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "주간 시간표 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "주간 시간표 조회 응답",
                                            value = """
                                            {
                                              "success": true,
                                              "message": "주간 시간표 조회 성공",
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "studentId": 1,
                                                  "studentName": "김학생",
                                                  "title": "수학 공부",
                                                  "activityId": 1,
                                                  "activityName": "자습",
                                                  "isStudyAssignable": true,
                                                  "dayOfWeek": 1,
                                                  "dayOfWeekName": "월요일",
                                                  "startTime": "09:00:00",
                                                  "endTime": "10:30:00"
                                                }
                                              ]
                                            }
                                            """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "학생을 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                            {
                                              "success": false,
                                              "message": "학생을 찾을 수 없습니다. ID: 999",
                                              "data": null
                                            }
                                            """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/student/{studentId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResponseBase<List<WeeklyScheduleResponse>>> getWeeklySchedule(
            @Parameter(description = "학생 ID") @PathVariable("studentId") Long studentId) {

        try {
            log.info("주간 시간표 조회 요청 - studentId: {}", studentId);

            List<WeeklyScheduleResponse> schedules = weeklyScheduleService.getWeeklyScheduleByStudent(studentId);

            log.info("주간 시간표 조회 성공 - studentId: {}, 시간표 개수: {}", studentId, schedules.size());

            return ResponseEntity.ok(ResponseBase.success("주간 시간표 조회 성공", schedules));

        } catch (ResourceNotFoundException e) {
            log.warn("주간 시간표 조회 실패 - studentId: {}, 이유: {}", studentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBase.error(e.getMessage()));
        } catch (Exception e) {
            log.error("주간 시간표 조회 실패 - studentId: {}", studentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error("주간 시간표 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "주간 시간표 등록",
            description = "새로운 주간 시간표를 등록합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "주간 시간표 등록 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "등록 성공",
                                            value = """
                                            {
                                              "success": true,
                                              "message": "주간 시간표가 성공적으로 등록되었습니다.",
                                              "data": {
                                                "id": 1,
                                                "studentId": 1,
                                                "studentName": "김학생",
                                                "title": "수학 공부",
                                                "activityId": 1,
                                                "activityName": "자습",
                                                "isStudyAssignable": true,
                                                "dayOfWeek": 1,
                                                "dayOfWeekName": "월요일",
                                                "startTime": "09:00:00",
                                                "endTime": "10:30:00"
                                              }
                                            }
                                            """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "등록 실패 - 시간 겹침 또는 잘못된 입력",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "시간 겹침 오류",
                                            value = """
                                            {
                                              "success": false,
                                              "message": "해당 시간대에 이미 등록된 시간표가 있습니다.",
                                              "data": null
                                            }
                                            """
                                    )
                            )
                    )
            }
    )
    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResponseBase<WeeklyScheduleResponse>> createWeeklySchedule(
            @Valid @RequestBody WeeklyScheduleRequest request) {

        try {
            log.info("주간 시간표 등록 요청 - studentId: {}, dayOfWeek: {}", request.getStudentId(), request.getDayOfWeek());

            WeeklyScheduleResponse response = weeklyScheduleService.createWeeklySchedule(request);

            log.info("주간 시간표 등록 성공 - id: {}, studentId: {}", response.getId(), request.getStudentId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseBase.success("주간 시간표가 성공적으로 등록되었습니다.", response));

        } catch (BusinessException | ResourceNotFoundException e) {
            log.warn("주간 시간표 등록 실패 - 이유: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBase.error(e.getMessage()));
        } catch (Exception e) {
            log.error("주간 시간표 등록 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error("주간 시간표 등록에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "주간 시간표 수정",
            description = "기존 주간 시간표를 수정합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "주간 시간표 수정 성공"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "시간표를 찾을 수 없음"
                    )
            }
    )
    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResponseBase<WeeklyScheduleResponse>> updateWeeklySchedule(
            @Parameter(description = "시간표 ID") @PathVariable("id") Long id,
            @Valid @RequestBody WeeklyScheduleRequest request) {

        try {
            log.info("주간 시간표 수정 요청 - id: {}", id);

            WeeklyScheduleResponse response = weeklyScheduleService.updateWeeklySchedule(id, request);

            log.info("주간 시간표 수정 성공 - id: {}", id);

            return ResponseEntity.ok(ResponseBase.success("주간 시간표가 성공적으로 수정되었습니다.", response));

        } catch (ResourceNotFoundException e) {
            log.warn("주간 시간표 수정 실패 - id: {}, 이유: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBase.error(e.getMessage()));
        } catch (BusinessException e) {
            log.warn("주간 시간표 수정 실패 - id: {}, 이유: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBase.error(e.getMessage()));
        } catch (Exception e) {
            log.error("주간 시간표 수정 실패 - id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error("주간 시간표 수정에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "주간 시간표 삭제",
            description = "주간 시간표를 삭제합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "주간 시간표 삭제 성공"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "시간표를 찾을 수 없음"
                    )
            }
    )
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResponseBase<String>> deleteWeeklySchedule(
            @Parameter(description = "시간표 ID") @PathVariable("id") Long id) {

        try {
            log.info("주간 시간표 삭제 요청 - id: {}", id);

            weeklyScheduleService.deleteWeeklySchedule(id);

            log.info("주간 시간표 삭제 성공 - id: {}", id);

            return ResponseEntity.ok(ResponseBase.success("주간 시간표가 성공적으로 삭제되었습니다.", "success"));

        } catch (ResourceNotFoundException e) {
            log.warn("주간 시간표 삭제 실패 - id: {}, 이유: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBase.error(e.getMessage()));
        } catch (Exception e) {
            log.error("주간 시간표 삭제 실패 - id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error("주간 시간표 삭제에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "특정 기간 시간표 조회",
            description = "주간 시간표를 특정 기간의 실제 날짜로 변환해서 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "기간별 시간표 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "기간별 시간표 응답",
                                            value = """
                                            {
                                              "success": true,
                                              "message": "기간별 시간표 조회 성공",
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "studentId": 1,
                                                  "studentName": "김학생",
                                                  "title": "수학 공부",
                                                  "activityId": 1,
                                                  "activityName": "자습",
                                                  "isStudyAssignable": true,
                                                  "actualStartTime": "2025-06-02T09:00:00",
                                                  "actualEndTime": "2025-06-02T10:30:00",
                                                  "dayOfWeek": 1,
                                                  "dayOfWeekName": "월요일"
                                                }
                                              ]
                                            }
                                            """
                                    )
                            )
                    )
            }
    )

    //todo 이거 필요성 재고해보기 필요없어보임
    @GetMapping("/student/{studentId}/period")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResponseBase<List<WeeklySchedulePeriodResponse>>> getWeeklyScheduleForPeriod(
            @Parameter(description = "학생 ID") @PathVariable("studentId") Long studentId,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd)")
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회할 일수 (기본값: 7일)")
            @RequestParam(value = "days", defaultValue = "7") int days) {

        try {
            log.info("기간별 시간표 조회 요청 - studentId: {}, startDate: {}, days: {}", studentId, startDate, days);

            List<WeeklySchedulePeriodResponse> schedules = weeklyScheduleService.getWeeklyScheduleForPeriod(
                    studentId, startDate, days);

            log.info("기간별 시간표 조회 성공 - studentId: {}, 시간표 개수: {}", studentId, schedules.size());

            return ResponseEntity.ok(ResponseBase.success("기간별 시간표 조회 성공", schedules));

        } catch (ResourceNotFoundException e) {
            log.warn("기간별 시간표 조회 실패 - studentId: {}, 이유: {}", studentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBase.error(e.getMessage()));
        } catch (Exception e) {
            log.error("기간별 시간표 조회 실패 - studentId: {}", studentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error("기간별 시간표 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "주간 시간표 활동 조회",
            description = "주간 시간표에 사용할 수 있는 모든 활동을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "활동 목록 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "활동 목록 응답",
                                            value = """
                                            {
                                              "success": true,
                                              "message": "활동 목록 조회 성공",
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "name": "학원",
                                                  "isStudyAssignable": false
                                                },
                                                {
                                                  "id": 2,
                                                  "name": "자습",
                                                  "isStudyAssignable": true
                                                }
                                              ]
                                            }
                                            """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/activities")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResponseBase<List<ActivityResponse>>> getAllActivities() {

        try {
            log.info("주간시간표용 활동 목록 조회 요청");

            List<Activity> activities = weeklyScheduleService.getAllActivities();

            List<ActivityResponse> activityResponses = activities.stream()
                    .map(activity -> new ActivityResponse(
                            activity.getId(), 
                            activity.getName(), 
                            activity.getIsStudyAssignable()
                    ))
                    .collect(Collectors.toList());

            log.info("활동 목록 조회 성공 - 활동 개수: {}", activityResponses.size());

            return ResponseEntity.ok(ResponseBase.success("활동 목록 조회 성공", activityResponses));

        } catch (Exception e) {
            log.error("활동 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error("활동 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }
}
