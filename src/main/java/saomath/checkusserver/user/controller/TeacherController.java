package saomath.checkusserver.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import saomath.checkusserver.auth.dto.ResponseBase;
import saomath.checkusserver.user.dto.TeacherListResponse;
import saomath.checkusserver.user.dto.TeacherDetailResponse;
import saomath.checkusserver.user.service.TeacherService;
import saomath.checkusserver.common.exception.ResourceNotFoundException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
@Tag(name = "Teacher", description = "교사 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class TeacherController {

    private final TeacherService teacherService;

    @Operation(
            summary = "교사 목록 조회",
            description = "승인된 교사들의 목록을 조회합니다. 담당 반 정보와 함께 반환됩니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "교사 목록 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "교사 목록 응답",
                                            value = """
                                {
                                  "success": true,
                                  "message": "교사 목록 조회 성공",
                                  "data": [
                                    {
                                      "id": 2,
                                      "username": "teacher1",
                                      "name": "김선생님",
                                      "phoneNumber": "010-1234-5678",
                                      "discordId": "teacher1#1234",
                                      "createdAt": "2024-01-01T00:00:00",
                                      "status": "ACTIVE",
                                      "classes": [
                                        {
                                          "id": 1,
                                          "name": "고1 수학"
                                        },
                                        {
                                          "id": 2,
                                          "name": "고2 수학"
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                {
                                  "success": false,
                                  "message": "인증이 필요합니다.",
                                  "data": null
                                }
                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "권한 부족",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                {
                                  "success": false,
                                  "message": "접근 권한이 없습니다.",
                                  "data": null
                                }
                                """
                                    )
                            )
                    )
            }
    )
    @GetMapping
    public ResponseEntity<ResponseBase<List<TeacherListResponse>>> getTeachers(
            @RequestParam(name = "status", required = false, defaultValue = "ACTIVE") 
            @Parameter(name = "status", description = "교사 상태로 필터링 (ACTIVE, SUSPENDED)") String status) {

        try {
            log.info("교사 목록 조회 요청 - status: {}", status);

            List<TeacherListResponse> teachers = teacherService.getActiveTeachers(status);

            log.info("교사 목록 조회 성공 - 조회된 교사 수: {}", teachers.size());
            
            return ResponseEntity.ok(ResponseBase.success("교사 목록 조회 성공", teachers));

        } catch (Exception e) {
            log.error("교사 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error("교사 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "교사 상세 정보 조회",
            description = "특정 교사의 상세 정보를 조회합니다. 담당 반 정보와 역할 상태를 포함합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "교사 상세 정보 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "교사 상세 정보 응답",
                                            value = """
                                {
                                  "success": true,
                                  "message": "교사 상세 정보 조회 성공",
                                  "data": {
                                    "id": 2,
                                    "username": "teacher1",
                                    "name": "김선생님",
                                    "phoneNumber": "010-1234-5678",
                                    "discordId": "teacher1#1234",
                                    "createdAt": "2024-01-01T00:00:00",
                                    "status": "ACTIVE",
                                    "classes": [
                                      {
                                        "id": 1,
                                        "name": "고1 수학",
                                        "studentCount": 15
                                      },
                                      {
                                        "id": 2,
                                        "name": "고2 수학",
                                        "studentCount": 18
                                      }
                                    ]
                                  }
                                }
                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "교사를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                {
                                  "success": false,
                                  "message": "교사를 찾을 수 없습니다.",
                                  "data": null
                                }
                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                {
                                  "success": false,
                                  "message": "인증이 필요합니다.",
                                  "data": null
                                }
                                """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ResponseBase<TeacherDetailResponse>> getTeacherDetail(
            @PathVariable("id") Long teacherId) {

        try {
            log.info("교사 상세 정보 조회 요청 - teacherId: {}", teacherId);

            TeacherDetailResponse teacher = teacherService.getTeacherDetail(teacherId);

            log.info("교사 상세 정보 조회 성공 - teacherId: {}", teacherId);
            
            return ResponseEntity.ok(ResponseBase.success("교사 상세 정보 조회 성공", teacher));

        } catch (ResourceNotFoundException e) {
            log.error("교사를 찾을 수 없음 - teacherId: {}", teacherId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBase.error("교사를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("교사 상세 정보 조회 실패 - teacherId: {}", teacherId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error("교사 상세 정보 조회에 실패했습니다: " + e.getMessage()));
        }
    }
} 