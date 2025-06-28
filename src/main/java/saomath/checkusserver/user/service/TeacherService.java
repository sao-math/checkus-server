package saomath.checkusserver.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.domain.UserRole;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.classroom.domain.TeacherClass;
import saomath.checkusserver.classroom.repository.TeacherClassRepository;
import saomath.checkusserver.classroom.repository.ClassRepository;
import saomath.checkusserver.user.domain.RoleConstants;
import saomath.checkusserver.user.dto.TeacherDetailResponse;
import saomath.checkusserver.user.dto.TeacherListResponse;
import saomath.checkusserver.user.dto.TeacherUpdateRequest;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.common.exception.BusinessException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherService {

    private final UserRepository userRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final ClassRepository classRepository;
    private final UserRoleService userRoleService;

    /**
     * 활성화된 교사 목록을 조회합니다.
     * 
     * @param statusFilter 상태 필터 (ACTIVE, SUSPENDED)
     * @return 교사 목록
     */
    public List<TeacherListResponse> getActiveTeachers(String statusFilter) {
        log.debug("교사 목록 조회 - status: {}", statusFilter);

        UserRole.RoleStatus status;
        try {
            status = UserRole.RoleStatus.valueOf(statusFilter.toUpperCase());
        } catch (IllegalArgumentException e) {
            status = UserRole.RoleStatus.ACTIVE; // 기본값
        }

        List<User> teachers = userRepository.findTeachersByStatus(status);
        
        log.info("조회된 교사 수: {}", teachers.size());

        return teachers.stream()
                .map(this::convertToTeacherListResponse)
                .collect(Collectors.toList());
    }

    /**
     * 교사 상세 정보를 조회합니다.
     * 
     * @param teacherId 교사 ID
     * @return 교사 상세 정보
     * @throws ResourceNotFoundException 교사를 찾을 수 없는 경우
     */
    public TeacherDetailResponse getTeacherDetail(Long teacherId) {
        log.debug("교사 상세 정보 조회 - teacherId: {}", teacherId);

        // 교사 존재 여부 확인
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("교사를 찾을 수 없습니다. ID: " + teacherId));

        // 교사 역할 확인
        List<String> roles = userRoleService.getActiveRoles(teacherId);
        if (!roles.contains(RoleConstants.TEACHER)) {
            throw new ResourceNotFoundException("해당 사용자는 교사가 아닙니다. ID: " + teacherId);
        }

        // 담당 반 정보 조회
        List<TeacherDetailResponse.ClassDetailInfo> classes = teacherClassRepository.findByTeacherId(teacherId)
                .stream()
                .map(tc -> new TeacherDetailResponse.ClassDetailInfo(
                        tc.getClassEntity().getId(),
                        tc.getClassEntity().getName(),
                        getStudentCountInClass(tc.getClassEntity().getId()) // 반별 학생 수 계산
                ))
                .collect(Collectors.toList());

        // 교사 역할 상태 조회
        UserRole.RoleStatus roleStatus = userRoleService.getTeacherRoleStatus(teacherId);

        TeacherDetailResponse response = new TeacherDetailResponse();
        response.setId(teacher.getId());
        response.setUsername(teacher.getUsername());
        response.setName(teacher.getName());
        response.setPhoneNumber(teacher.getPhoneNumber());
        response.setDiscordId(teacher.getDiscordId());
        response.setCreatedAt(teacher.getCreatedAt());
        response.setStatus(roleStatus);
        response.setClasses(classes);

        log.info("교사 상세 정보 조회 성공 - teacherId: {}, name: {}, classes: {}", 
                teacherId, teacher.getName(), classes.size());

        return response;
    }

    /**
     * 교사 정보를 수정합니다.
     * 
     * @param teacherId 교사 ID
     * @param updateRequest 수정할 교사 정보
     * @return 수정된 교사 상세 정보
     * @throws ResourceNotFoundException 교사를 찾을 수 없는 경우
     */
    @Transactional
    public TeacherDetailResponse updateTeacher(Long teacherId, TeacherUpdateRequest updateRequest) {
        log.debug("교사 정보 수정 - teacherId: {}", teacherId);

        // 교사 존재 여부 확인
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("교사를 찾을 수 없습니다. ID: " + teacherId));

        // 교사 역할 확인
        List<String> roles = userRoleService.getActiveRoles(teacherId);
        if (!roles.contains(RoleConstants.TEACHER)) {
            throw new ResourceNotFoundException("해당 사용자는 교사가 아닙니다. ID: " + teacherId);
        }

        // 기본 정보 업데이트
        if (updateRequest.getName() != null) {
            teacher.setName(updateRequest.getName());
        }
        if (updateRequest.getPhoneNumber() != null) {
            teacher.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        if (updateRequest.getDiscordId() != null) {
            teacher.setDiscordId(updateRequest.getDiscordId());
        }

        // 담당 반 정보 업데이트
        if (updateRequest.getClassIds() != null) {
            updateTeacherClasses(teacherId, updateRequest.getClassIds());
        }

        // 저장
        userRepository.save(teacher);

        log.info("교사 정보 수정 성공 - teacherId: {}, name: {}", teacherId, teacher.getName());

        // 수정된 정보 반환
        return getTeacherDetail(teacherId);
    }

    /**
     * 교사를 삭제합니다. (실제로는 비활성화)
     * 
     * @param teacherId 교사 ID
     * @throws ResourceNotFoundException 교사를 찾을 수 없는 경우
     */
    @Transactional
    public void deleteTeacher(Long teacherId) {
        log.debug("교사 삭제 - teacherId: {}", teacherId);

        // 교사 존재 여부 확인
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("교사를 찾을 수 없습니다. ID: " + teacherId));

        // 교사 역할 확인
        List<String> roles = userRoleService.getActiveRoles(teacherId);
        if (!roles.contains(RoleConstants.TEACHER)) {
            throw new ResourceNotFoundException("해당 사용자는 교사가 아닙니다. ID: " + teacherId);
        }

        // 교사 역할을 SUSPENDED로 변경 (실제 삭제 대신 비활성화)
        userRoleService.suspendRole(teacherId, RoleConstants.TEACHER);

        // 담당 반 연결 해제
        List<TeacherClass> teacherClasses = teacherClassRepository.findByTeacherId(teacherId);
        teacherClassRepository.deleteAll(teacherClasses);

        log.info("교사 삭제(비활성화) 성공 - teacherId: {}, name: {}", teacherId, teacher.getName());
    }

    /**
     * 교사 담당 반 정보를 업데이트합니다.
     */
    private void updateTeacherClasses(Long teacherId, List<Long> classIds) {
        // 기존 담당 반 연결 삭제
        List<TeacherClass> existingClasses = teacherClassRepository.findByTeacherId(teacherId);
        teacherClassRepository.deleteAll(existingClasses);

        // 새로운 담당 반 연결 생성
        if (classIds != null && !classIds.isEmpty()) {
            User teacher = userRepository.findById(teacherId).orElseThrow();
            
            for (Long classId : classIds) {
                classRepository.findById(classId).ifPresent(classEntity -> {
                    TeacherClass teacherClass = TeacherClass.builder()
                            .id(new TeacherClass.TeacherClassId(teacherId, classId))
                            .teacher(teacher)
                            .classEntity(classEntity)
                            .build();
                    teacherClassRepository.save(teacherClass);
                });
            }
        }
    }

    /**
     * 교사 목록 응답 객체로 변환합니다.
     */
    private TeacherListResponse convertToTeacherListResponse(User teacher) {
        // 담당 반 정보 조회
        List<TeacherListResponse.ClassInfo> classes = teacherClassRepository.findByTeacherId(teacher.getId())
                .stream()
                .map(tc -> new TeacherListResponse.ClassInfo(
                        tc.getClassEntity().getId(),
                        tc.getClassEntity().getName()
                ))
                .collect(Collectors.toList());

        // 교사 역할 상태 조회
        UserRole.RoleStatus roleStatus = userRoleService.getTeacherRoleStatus(teacher.getId());

        TeacherListResponse response = new TeacherListResponse();
        response.setId(teacher.getId());
        response.setUsername(teacher.getUsername());
        response.setName(teacher.getName());
        response.setPhoneNumber(teacher.getPhoneNumber());
        response.setDiscordId(teacher.getDiscordId());
        response.setCreatedAt(teacher.getCreatedAt());
        response.setStatus(roleStatus);
        response.setClasses(classes);

        return response;
    }

    /**
     * 특정 반의 학생 수를 조회합니다.
     */
    private Integer getStudentCountInClass(Long classId) {
        // TODO: StudentClassRepository를 통해 실제 학생 수 조회
        // 현재는 임시로 0 반환
        return 0;
    }
} 