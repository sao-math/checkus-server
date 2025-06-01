package saomath.checkusserver.controller;

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
import saomath.checkusserver.dto.StudentDetailResponse;
import saomath.checkusserver.dto.StudentListResponse;
import saomath.checkusserver.entity.StudentProfile;
import saomath.checkusserver.exception.ResourceNotFoundException;
import saomath.checkusserver.service.StudentService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@Tag(name = "Student", description = "학생 관련 API")
public class StudentController {

    private final StudentService studentService;

    @Operation(
            summary = "학생 목록 조회",
            description = "필터링 옵션을 사용하여 학생 목록을 조회합니다. 모든 필터는 선택사항입니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "학생 목록 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "학생 목록 응답",
                                            value = """
                                {
                                  "success": true,
                                  "message": "학생 목록 조회 성공",
                                  "data": [
                                    {
                                      "id": 4,
                                      "name": "박학생",
                                      "phoneNumber": "010-1111-2222",
                                      "studentPhoneNumber": "010-2222-1111",
                                      "school": "이현중",
                                      "grade": 2,
                                      "classes": ["수학심화반", "과학반"],
                                      "status": "ENROLLED",
                                      "guardians": [
                                        {
                                          "id": 5,
                                          "name": "박학부모",
                                          "phoneNumber": "010-1111-2222",
                                          "relationship": "모"
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
                    )
            }
    )
    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResponseBase<List<StudentListResponse>>> getStudents(
            @RequestParam(name = "classId", required = false) 
            @Parameter(name = "classId", description = "반 ID로 필터링") Long classId,
            @RequestParam(name = "grade", required = false) 
            @Parameter(name = "grade", description = "학년으로 필터링 (1-6)") Integer grade,
            @RequestParam(name = "status", required = false) 
            @Parameter(name = "status", description = "학생 상태로 필터링") StudentProfile.StudentStatus status,
            @RequestParam(name = "schoolId", required = false) 
            @Parameter(name = "schoolId", description = "학교 ID로 필터링") Long schoolId) {

        try {
            log.info("학생 목록 조회 요청 - classId: {}, grade: {}, status: {}, schoolId: {}", 
                    classId, grade, status, schoolId);

            List<StudentListResponse> students = studentService.getFilteredStudents(classId, grade, status, schoolId);

            log.info("학생 목록 조회 성공 - 조회된 학생 수: {}", students.size());
            
            return ResponseEntity.ok(ResponseBase.success("학생 목록 조회 성공", students));

        } catch (Exception e) {
            log.error("학생 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error("학생 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "학생 상세 정보 조회",
            description = "특정 학생의 상세 정보를 조회합니다. 학생 프로필, 소속 반, 학부모 정보를 포함합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "학생 상세 정보 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "학생 상세 정보 응답",
                                            value = """
                                {
                                  "success": true,
                                  "message": "학생 상세 정보 조회 성공",
                                  "data": {
                                    "id": 4,
                                    "username": "student1",
                                    "name": "박학생",
                                    "phoneNumber": "010-2222-1111",
                                    "discordId": "student1#1234",
                                    "createdAt": "2024-01-01T00:00:00",
                                    "status": "ENROLLED",
                                    "school": "이현중",
                                    "schoolId": 1,
                                    "grade": 2,
                                    "gender": "MALE",
                                    "classes": [
                                      {
                                        "id": 1,
                                        "name": "수학심화반"
                                      },
                                      {
                                        "id": 2,
                                        "name": "과학반"
                                      }
                                    ],
                                    "guardians": [
                                      {
                                        "id": 5,
                                        "name": "박학부모",
                                        "phoneNumber": "010-1111-2222",
                                        "relationship": "모"
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
                            description = "학생을 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                {
                                  "success": false,
                                  "message": "학생을 찾을 수 없습니다.",
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
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResponseBase<StudentDetailResponse>> getStudentDetail(
            @PathVariable("id") Long studentId) {

        try {
            log.info("학생 상세 정보 조회 요청 - studentId: {}", studentId);

            StudentDetailResponse student = studentService.getStudentDetail(studentId);

            log.info("학생 상세 정보 조회 성공 - studentId: {}, name: {}", studentId, student.getName());
            
            return ResponseEntity.ok(ResponseBase.success("학생 상세 정보 조회 성공", student));

        } catch (ResourceNotFoundException e) {
            log.warn("학생을 찾을 수 없음 - studentId: {}", studentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBase.error(e.getMessage()));
        } catch (Exception e) {
            log.error("학생 상세 정보 조회 실패 - studentId: {}", studentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBase.error("학생 상세 정보 조회에 실패했습니다: " + e.getMessage()));
        }
    }
}
