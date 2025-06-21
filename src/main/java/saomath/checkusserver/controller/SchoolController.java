package saomath.checkusserver.controller;

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
import saomath.checkusserver.dto.SchoolRequest;
import saomath.checkusserver.dto.SchoolResponse;
import saomath.checkusserver.exception.DuplicateResourceException;
import saomath.checkusserver.service.SchoolService;

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
                                      "name": "이현중"
                                    },
                                    {
                                      "id": 2,
                                      "name": "손곡중"
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
                                    "name": "새로운중학교"
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
} 