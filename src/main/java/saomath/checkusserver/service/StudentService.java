package saomath.checkusserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.dto.GuardianResponse;
import saomath.checkusserver.dto.StudentDetailResponse;
import saomath.checkusserver.dto.StudentListResponse;
import saomath.checkusserver.dto.common.StudentProfileInfo;
import saomath.checkusserver.entity.*;
import saomath.checkusserver.exception.ResourceNotFoundException;
import saomath.checkusserver.repository.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentClassRepository studentClassRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final UserRoleService userRoleService;

    /**
     * 필터링된 학생 목록을 조회합니다.
     * 
     * @param classId 반 ID (선택사항)
     * @param grade 학년 (선택사항)
     * @param status 상태 (선택사항)
     * @param schoolId 학교 ID (선택사항)
     * @return 필터링된 학생 목록
     */
    public List<StudentListResponse> getFilteredStudents(Long classId, Integer grade, 
                                                        StudentProfile.StudentStatus status, Long schoolId) {
        log.debug("학생 목록 조회 - classId: {}, grade: {}, status: {}, schoolId: {}", 
                 classId, grade, status, schoolId);

        List<User> students;
        
        // 필터가 하나라도 있으면 필터링 쿼리 사용, 없으면 전체 학생 조회
        if (classId != null || grade != null || status != null || schoolId != null) {
            students = userRepository.findStudentsWithFilters(classId, grade, status, schoolId);
        } else {
            students = userRepository.findAllStudents();
        }

        log.info("조회된 학생 수: {}", students.size());

        return students.stream()
                .map(this::convertToStudentListResponse)
                .collect(Collectors.toList());
    }

    /**
     * 학생 상세 정보를 조회합니다.
     * 
     * @param studentId 학생 ID
     * @return 학생 상세 정보
     * @throws ResourceNotFoundException 학생을 찾을 수 없는 경우
     */
    public StudentDetailResponse getStudentDetail(Long studentId) {
        log.debug("학생 상세 정보 조회 - studentId: {}", studentId);

        // 학생 존재 여부 확인
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다. ID: " + studentId));

        // 학생 역할 확인
        List<String> roles = userRoleService.getActiveRoles(studentId);
        if (!roles.contains(RoleConstants.STUDENT)) {
            throw new ResourceNotFoundException("해당 사용자는 학생이 아닙니다. ID: " + studentId);
        }

        // 학생 프로필 조회
        StudentProfile studentProfile = studentProfileRepository.findByUserId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("학생 프로필을 찾을 수 없습니다. ID: " + studentId));

        // 소속 반 정보 조회
        List<StudentDetailResponse.ClassResponse> classes = studentClassRepository.findByStudentId(studentId)
                .stream()
                .map(sc -> new StudentDetailResponse.ClassResponse(
                        sc.getClassEntity().getId(),
                        sc.getClassEntity().getName()
                ))
                .collect(Collectors.toList());

        // 학부모 정보 조회
        List<GuardianResponse> guardians = studentGuardianRepository.findByStudentId(studentId)
                .stream()
                .map(sg -> new GuardianResponse(
                        sg.getGuardian().getId(),
                        sg.getGuardian().getName(),
                        sg.getGuardian().getPhoneNumber(),
                        sg.getRelationship()
                ))
                .collect(Collectors.toList());

        StudentDetailResponse response = new StudentDetailResponse();
        response.setId(student.getId());
        response.setUsername(student.getUsername());
        response.setName(student.getName());
        response.setPhoneNumber(student.getPhoneNumber());
        response.setDiscordId(student.getDiscordId());
        response.setCreatedAt(student.getCreatedAt());
        
        // 학생 프로필 정보 설정
        StudentProfileInfo.SchoolInfo schoolInfo = new StudentProfileInfo.SchoolInfo(
                studentProfile.getSchool().getId(),
                studentProfile.getSchool().getName()
        );
        
        StudentProfileInfo profileInfo = new StudentProfileInfo();
        profileInfo.setStatus(studentProfile.getStatus());
        profileInfo.setSchool(schoolInfo);
        profileInfo.setGrade(studentProfile.getGrade());
        profileInfo.setGender(studentProfile.getGender());
        
        response.setProfile(profileInfo);
        response.setClasses(classes);
        response.setGuardians(guardians);

        log.info("학생 상세 정보 조회 성공 - studentId: {}, name: {}, classes: {}, guardians: {}", 
                studentId, student.getName(), classes.size(), guardians.size());

        return response;
    }

    /**
     * User 엔티티를 StudentListResponse로 변환합니다.
     */
    private StudentListResponse convertToStudentListResponse(User student) {
        try {
            // 학생 프로필 조회
            StudentProfile studentProfile = studentProfileRepository.findByUserId(student.getId()).orElse(null);
            
            // 소속 반 정보 조회
            List<String> classes = studentClassRepository.findByStudentId(student.getId())
                    .stream()
                    .map(sc -> sc.getClassEntity().getName())
                    .collect(Collectors.toList());

            // 학부모 정보 조회
            List<GuardianResponse> guardians = studentGuardianRepository.findByStudentId(student.getId())
                    .stream()
                    .map(sg -> new GuardianResponse(
                            sg.getGuardian().getId(),
                            sg.getGuardian().getName(),
                            sg.getGuardian().getPhoneNumber(),
                            sg.getRelationship()
                    ))
                    .collect(Collectors.toList());

            return new StudentListResponse(
                    student.getId(),
                    student.getName(),
                    guardians.isEmpty() ? null : guardians.get(0).getPhoneNumber(), // 첫 번째 학부모 전화번호
                    student.getPhoneNumber(),
                    studentProfile != null ? studentProfile.getSchool().getName() : null,
                    studentProfile != null ? studentProfile.getGrade() : null,
                    classes,
                    studentProfile != null ? studentProfile.getStatus() : null,
                    guardians
            );
        } catch (Exception e) {
            log.error("학생 정보 변환 실패 - studentId: {}", student.getId(), e);
            // 기본 정보만으로 응답 생성
            return new StudentListResponse(
                    student.getId(),
                    student.getName(),
                    null,
                    student.getPhoneNumber(),
                    null,
                    null,
                    List.of(),
                    null,
                    List.of()
            );
        }
    }
}
