package saomath.checkusserver.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.classroom.domain.ClassEntity;
import saomath.checkusserver.classroom.domain.StudentClass;
import saomath.checkusserver.classroom.repository.ClassRepository;
import saomath.checkusserver.classroom.repository.StudentClassRepository;
import saomath.checkusserver.discord.service.VoiceChannelEventService;
import saomath.checkusserver.user.domain.RoleConstants;
import saomath.checkusserver.school.domain.School;
import saomath.checkusserver.user.domain.StudentGuardian;
import saomath.checkusserver.user.domain.StudentProfile;
import saomath.checkusserver.user.dto.GuardianResponse;
import saomath.checkusserver.user.dto.StudentDetailResponse;
import saomath.checkusserver.user.dto.StudentListResponse;
import saomath.checkusserver.user.dto.StudentUpdateRequest;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.school.repository.SchoolRepository;
import saomath.checkusserver.user.repository.StudentGuardianRepository;
import saomath.checkusserver.user.repository.StudentProfileRepository;
import saomath.checkusserver.notification.event.UserRegisteredEvent;
import org.springframework.context.ApplicationEventPublisher;

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
    private final SchoolRepository schoolRepository;
    private final ClassRepository classRepository;
    private final UserRoleService userRoleService;
    private final VoiceChannelEventService voiceChannelEventService;
    private final ApplicationEventPublisher applicationEventPublisher;

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
        response.setStatus(studentProfile.getStatus());
        response.setSchool(studentProfile.getSchool().getName());
        response.setSchoolId(studentProfile.getSchool().getId());
        response.setGrade(studentProfile.getGrade());
        response.setGender(studentProfile.getGender());
        response.setClasses(classes);
        response.setGuardians(guardians);

        log.info("학생 상세 정보 조회 성공 - studentId: {}, name: {}, classes: {}, guardians: {}", 
                studentId, student.getName(), classes.size(), guardians.size());

        return response;
    }

    /**
     * 학생 정보를 수정합니다.
     * 
     * @param studentId 학생 ID
     * @param updateRequest 수정할 학생 정보
     * @return 수정된 학생 상세 정보
     * @throws ResourceNotFoundException 학생을 찾을 수 없는 경우
     */
    @Transactional
    public StudentDetailResponse updateStudent(Long studentId, StudentUpdateRequest updateRequest) {
        log.debug("학생 정보 수정 - studentId: {}", studentId);

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

        // Discord ID 변경 여부 확인 (트랜잭션 커밋 후 처리를 위해 미리 저장)
        String oldDiscordId = student.getDiscordId();
        String newDiscordId = updateRequest.getDiscordId();
        boolean discordIdChanged = (newDiscordId != null && !newDiscordId.equals(oldDiscordId));

        // 기본 정보 업데이트
        updateBasicInfo(student, updateRequest);

        // 프로필 정보 업데이트
        updateProfileInfo(studentProfile, updateRequest.getProfile());

        // 반 정보 업데이트
        updateClassInfo(studentId, updateRequest.getClassIds());

        // 학부모 정보 업데이트
        updateGuardianInfo(studentId, updateRequest.getGuardians());

        // 저장
        userRepository.save(student);
        studentProfileRepository.save(studentProfile);
        
        // 즉시 DB 반영 (Discord 채널 확인 전에 필요)
        userRepository.flush();

        log.info("학생 정보 수정 성공 - studentId: {}, name: {}", studentId, student.getName());

        // Discord ID 변경된 경우 트랜잭션 커밋 후 음성채널 확인을 위한 이벤트 발행
        if (discordIdChanged) {
            applicationEventPublisher.publishEvent(new UserRegisteredEvent(student, "DISCORD_ID_UPDATE", oldDiscordId));
        }

        // 수정된 정보 반환
        return getStudentDetail(studentId);
    }

    /**
     * 기본 정보 업데이트
     */
    private void updateBasicInfo(User student, StudentUpdateRequest updateRequest) {
        if (updateRequest.getName() != null) {
            student.setName(updateRequest.getName());
        }
        if (updateRequest.getPhoneNumber() != null) {
            student.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        if (updateRequest.getDiscordId() != null) {
            student.setDiscordId(updateRequest.getDiscordId());
        }
    }

    /**
     * 프로필 정보 업데이트
     */
    private void updateProfileInfo(StudentProfile studentProfile, StudentUpdateRequest.StudentProfileUpdateRequest profileRequest) {
        if (profileRequest == null) {
            return;
        }

        if (profileRequest.getStatus() != null) {
            studentProfile.setStatus(profileRequest.getStatus());
        }
        if (profileRequest.getSchoolId() != null) {
            School school = schoolRepository.findById(profileRequest.getSchoolId())
                    .orElseThrow(() -> new ResourceNotFoundException("학교를 찾을 수 없습니다. ID: " + profileRequest.getSchoolId()));
            studentProfile.setSchool(school);
        }
        if (profileRequest.getGrade() != null) {
            studentProfile.setGrade(profileRequest.getGrade());
        }
        if (profileRequest.getGender() != null) {
            studentProfile.setGender(profileRequest.getGender());
        }
    }

    /**
     * 반 정보 업데이트
     */
    private void updateClassInfo(Long studentId, List<Long> classIds) {
        if (classIds == null) {
            return;
        }

        // 기존 반 정보 삭제
        List<StudentClass> existingClasses = studentClassRepository.findByStudentId(studentId);
        studentClassRepository.deleteAll(existingClasses);

        // 새로운 반 정보 추가
        for (Long classId : classIds) {
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("반을 찾을 수 없습니다. ID: " + classId));
            
            StudentClass.StudentClassId id = new StudentClass.StudentClassId(studentId, classId);
            StudentClass studentClass = StudentClass.builder()
                    .id(id)
                    .student(userRepository.findById(studentId).orElseThrow())
                    .classEntity(classEntity)
                    .build();
            
            studentClassRepository.save(studentClass);
        }
    }

    /**
     * 학부모 정보 업데이트
     */
    private void updateGuardianInfo(Long studentId, List<StudentUpdateRequest.GuardianUpdateRequest> guardianRequests) {
        if (guardianRequests == null) {
            return;
        }

        // 기존 학부모 정보 삭제
        List<StudentGuardian> existingGuardians = studentGuardianRepository.findByStudentId(studentId);
        studentGuardianRepository.deleteAll(existingGuardians);

        // 새로운 학부모 정보 추가
        for (StudentUpdateRequest.GuardianUpdateRequest guardianRequest : guardianRequests) {
            User guardian;
            
            if (guardianRequest.getId() != null) {
                // 기존 학부모 수정
                guardian = userRepository.findById(guardianRequest.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("학부모를 찾을 수 없습니다. ID: " + guardianRequest.getId()));
                
                // 학부모 정보 업데이트
                if (guardianRequest.getName() != null) {
                    guardian.setName(guardianRequest.getName());
                }
                if (guardianRequest.getPhoneNumber() != null) {
                    guardian.setPhoneNumber(guardianRequest.getPhoneNumber());
                }
                userRepository.save(guardian);
            } else {
                // 새로운 학부모 생성 (간단한 구현 - 실제로는 더 복잡한 로직 필요)
                guardian = User.builder()
                        .name(guardianRequest.getName())
                        .phoneNumber(guardianRequest.getPhoneNumber())
                        .build();
                guardian = userRepository.save(guardian);
            }

            // 학생-학부모 관계 생성
            StudentGuardian.StudentGuardianId id = new StudentGuardian.StudentGuardianId(studentId, guardian.getId());
            StudentGuardian studentGuardian = StudentGuardian.builder()
                    .id(id)
                    .student(userRepository.findById(studentId).orElseThrow())
                    .guardian(guardian)
                    .relationship(guardianRequest.getRelationship())
                    .build();
            
            studentGuardianRepository.save(studentGuardian);
        }
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
