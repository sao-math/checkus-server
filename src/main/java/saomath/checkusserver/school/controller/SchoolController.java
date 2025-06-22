package saomath.checkusserver.school.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import saomath.checkusserver.auth.dto.ResponseBase;
import saomath.checkusserver.school.dto.SchoolRequest;
import saomath.checkusserver.school.dto.SchoolResponse;
import saomath.checkusserver.common.exception.DuplicateResourceException;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.common.exception.BusinessException;
import saomath.checkusserver.school.service.SchoolService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/schools")
@RequiredArgsConstructor
@Tag(name = "School", description = "학교 관련 API")
public class SchoolController {

    private final SchoolService schoolService;

    @Operation(
            summary = "학교 목록 조회",
            description = "전체 학교 목록을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "학교 목록 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "학교 목록 응답",
                                            value = """
                                {
                                  "success": true,
                                  "message": "학교 목록 조회 성공",
                                  "data": [
                                    {
                                      "id": 1,
                                      "name": "이현중",
                                      "studentCount": 15
                                    },
                                    {
                                      "id": 2,
                                      "name": "손곡중",
                                      "studentCount": 8
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
                    )
            }
    )
    @GetMapping
    public ResponseEntity<ResponseBase<List<SchoolResponse>>> getSchools() {
        try {
            log.info("학교 목록 조회 요청");

            List<SchoolResponse> schools = schoolService.getAllSchools();

            log.info("학교 목록 조회 성공 - 조회된 학교 수: {}", schools.size());
            
            return ResponseEntity.ok(ResponseBase.success("학교 목록 조회 성공", schools));

        } catch (Exception e) {
            log.error("학교 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error("학교 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "학교 생성",
            description = "새로운 학교를 생성합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "학교 생성 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "학교 생성 응답",
                                            value = """
                                {
                                  "success": true,
                                  "message": "학교가 성공적으로 생성되었습니다.",
                                  "data": {
                                    "id": 15,
                                    "name": "새로운중학교",
                                    "studentCount": 0
                                  }
                                }
                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (유효성 검사 실패)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                {
                                  "success": false,
                                  "message": "학교명은 필수입니다.",
                                  "data": null
                                }
                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "중복된 학교명",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                {
                                  "success": false,
                                  "message": "이미 존재하는 학교명입니다: 이현중",
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
    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResponseBase<SchoolResponse>> createSchool(
            @Valid @RequestBody SchoolRequest schoolRequest) {
        try {
            log.info("학교 생성 요청: {}", schoolRequest.getName());

            SchoolResponse createdSchool = schoolService.createSchool(schoolRequest);

            log.info("학교 생성 성공: {} (ID: {})", createdSchool.getName(), createdSchool.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseBase.success("학교가 성공적으로 생성되었습니다.", createdSchool));

        } catch (DuplicateResourceException e) {
            log.warn("중복된 학교명으로 생성 시도: {}", schoolRequest.getName());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseBase.error(e.getMessage()));
        } catch (Exception e) {
            log.error("학교 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error("학교 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "학교 삭제",
            description = "지정된 학교를 삭제합니다. 연결된 학생이 있는 경우 삭제할 수 없습니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "학교 삭제 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "학교 삭제 응답",
                                            value = """
                                {
                                  "success": true,
                                  "message": "학교가 성공적으로 삭제되었습니다.",
                                  "data": null
                                }
                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "연결된 학생이 있어 삭제할 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                {
                                  "success": false,
                                  "message": "연결된 학생이 있어 학교를 삭제할 수 없습니다. 학생 수: 5",
                                  "data": null
                                }
                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "학교를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                {
                                  "success": false,
                                  "message": "학교를 찾을 수 없습니다: 999",
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
    @DeleteMapping("/{schoolId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResponseBase<Void>> deleteSchool(@PathVariable Long schoolId) {
        try {
            log.info("학교 삭제 요청: schoolId={}", schoolId);

            schoolService.deleteSchool(schoolId);

            log.info("학교 삭제 성공: schoolId={}", schoolId);
            
            return ResponseEntity.ok(ResponseBase.success("학교가 성공적으로 삭제되었습니다.", null));

        } catch (ResourceNotFoundException e) {
            log.warn("존재하지 않는 학교 삭제 시도: schoolId={}", schoolId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBase.error(e.getMessage()));
        } catch (BusinessException e) {
            log.warn("연결된 학생이 있는 학교 삭제 시도: schoolId={}", schoolId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBase.error(e.getMessage()));
        } catch (Exception e) {
            log.error("학교 삭제 실패: schoolId={}", schoolId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error("학교 삭제에 실패했습니다: " + e.getMessage()));
        }
    }
} 