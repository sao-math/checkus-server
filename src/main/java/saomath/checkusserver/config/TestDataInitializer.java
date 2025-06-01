package saomath.checkusserver.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.entity.*;
import saomath.checkusserver.repository.*;
import saomath.checkusserver.service.UserRoleService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Profile({"test", "local"}) // test, local 프로파일에서만 실행
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final SchoolRepository schoolRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("테스트 데이터 초기화 시작...");
        
        // 기존 데이터가 있으면 스킵
        if (userRepository.count() > 0) {
            log.info("이미 데이터가 존재합니다. 초기화를 스킵합니다.");
            log.info("현재 사용자 수: {}", userRepository.count());
            return;
        }

        initializeRolesAndPermissions();
        initializeSchools();
        initializeTaskTypes();
        initializeUsers();
        
        // 생성 결과 로그
        log.info("데이터 초기화 완료!");
        log.info("생성된 사용자 수: {}", userRepository.count());
        log.info("생성된 역할 수: {}", roleRepository.count());
        log.info("생성된 권한 수: {}", permissionRepository.count());
        log.info("생성된 학교 수: {}", schoolRepository.count());
        log.info("테스트 데이터 초기화 완료!");
    }

    private void initializeRolesAndPermissions() {
        log.info("역할 및 권한 초기화...");
        
        // 역할 생성
        Role adminRole = createRole("ADMIN");
        Role teacherRole = createRole("TEACHER");
        Role studentRole = createRole("STUDENT");
        Role guardianRole = createRole("GUARDIAN");

        // 권한 생성
        Permission teacherManagement = createPermission("TEACHER_MANAGEMENT");
        Permission studentManagement = createPermission("STUDENT_MANAGEMENT");
        Permission classManagement = createPermission("CLASS_MANAGEMENT");
        Permission taskDbManagement = createPermission("TASK_DB_MANAGEMENT");
        Permission taskSubmit = createPermission("TASK_SUBMIT");

        // 역할별 권한 할당
        assignPermissionsToRole(adminRole, Arrays.asList(
            teacherManagement, studentManagement, classManagement, taskDbManagement, taskSubmit
        ));
        
        assignPermissionsToRole(teacherRole, Arrays.asList(
            studentManagement, classManagement, taskDbManagement, taskSubmit
        ));
        
        assignPermissionsToRole(studentRole, Arrays.asList(taskSubmit));
        assignPermissionsToRole(guardianRole, Arrays.asList());
    }

    private void initializeSchools() {
        log.info("학교 데이터 초기화...");
        
        String[] schools = {"이현중", "손곡중", "신봉중", "수지중", "서원중", "홍천중", 
                          "성서중", "상현중", "정평중", "한빛중", "성복중", "문정중", "소현중", "계원예중"};
        
        for (String schoolName : schools) {
            createSchool(schoolName);
        }
    }

    private void initializeTaskTypes() {
        log.info("과제 유형 초기화...");
        
        createTaskType("테스트");
        createTaskType("개념");
    }

    private void initializeUsers() {
        log.info("사용자 데이터 초기화...");
        
        // 학교 조회
        School ihyeon = schoolRepository.findByName("이현중").orElseThrow();
        School songok = schoolRepository.findByName("손곡중").orElseThrow();
        
        // 관리자 계정
        User admin = createUserWithRole("admin", "관리자", "010-0000-0000", "admin123", "admin#1234", RoleConstants.ADMIN);
        
        // 교사 계정들
        User teacher1 = createUserWithRole("teacher1", "김선생", "010-1111-1111", "teacher123", "teacher1#1234", RoleConstants.TEACHER);
        User teacher2 = createUserWithRole("teacher2", "이선생", "010-1111-2222", "teacher123", "teacher2#5678", RoleConstants.TEACHER);
        
        // 학생 계정들 + 프로필
        User student1 = createUserWithRole("student1", "박학생", "010-2222-1111", "student123", "student1#1234", RoleConstants.STUDENT);
        createStudentProfile(student1, StudentProfile.StudentStatus.ENROLLED, ihyeon, 2, StudentProfile.Gender.MALE);
        
        User student2 = createUserWithRole("student2", "최학생", "010-2222-2222", "student123", "student2#5678", RoleConstants.STUDENT);
        createStudentProfile(student2, StudentProfile.StudentStatus.ENROLLED, ihyeon, 2, StudentProfile.Gender.FEMALE);
        
        User student3 = createUserWithRole("student3", "정학생", "010-2222-3333", "student123", "student3#9012", RoleConstants.STUDENT);
        createStudentProfile(student3, StudentProfile.StudentStatus.ENROLLED, songok, 1, StudentProfile.Gender.MALE);
        
        // 학부모 계정들
        User guardian1 = createUserWithRole("parent1", "박학부모", "010-3333-1111", "parent123", "parent1#1234", RoleConstants.GUARDIAN);
        User guardian2 = createUserWithRole("parent2", "최학부모", "010-3333-2222", "parent123", "parent2#5678", RoleConstants.GUARDIAN);
        User guardian3 = createUserWithRole("parent3", "정학부모", "010-3333-3333", "parent123", "parent3#9012", RoleConstants.GUARDIAN);
        
        log.info("=== 테스트 계정 정보 ===");
        log.info("관리자: admin / admin123");
        log.info("교사1: teacher1 / teacher123 (김선생)");
        log.info("교사2: teacher2 / teacher123 (이선생)");
        log.info("학생1: student1 / student123 (박학생)");
        log.info("학생2: student2 / student123 (최학생)");
        log.info("학생3: student3 / student123 (정학생)");
        log.info("학부모1: parent1 / parent123 (박학부모)");
        log.info("학부모2: parent2 / parent123 (최학부모)");
        log.info("학부모3: parent3 / parent123 (정학부모)");
        log.info("======================");
    }

    // Helper methods - 모든 계정을 동일한 간단한 방식으로 생성
    private User createUserWithRole(String username, String name, String phoneNumber, String password, String discordId, String roleName) {
        // 1. 사용자 생성 (비밀번호 암호화 포함)
        User user = User.builder()
            .username(username)
            .name(name)
            .phoneNumber(phoneNumber)
            .password(passwordEncoder.encode(password)) // 암호화는 유지
            .discordId(discordId)
            .build();
        user = userRepository.save(user);
        
        // 2. 역할 할당 (바로 ACTIVE 상태)
        userRoleService.assignRole(user, roleName, UserRole.RoleStatus.ACTIVE);
        
        log.info("{} 계정 생성 완료: {}", roleName, username);
        return user;
    }

    private void createStudentProfile(User student, StudentProfile.StudentStatus status, School school, int grade, StudentProfile.Gender gender) {
        StudentProfile profile = new StudentProfile();
        profile.setUser(student);
        profile.setStatus(status);
        profile.setSchool(school);
        profile.setGrade(grade);
        profile.setGender(gender);
        studentProfileRepository.save(profile);
    }

    private Role createRole(String name) {
        Role role = new Role();
        role.setName(name);
        return roleRepository.save(role);
    }

    private Permission createPermission(String name) {
        Permission permission = new Permission();
        permission.setName(name);
        return permissionRepository.save(permission);
    }

    private void assignPermissionsToRole(Role role, List<Permission> permissions) {
        for (Permission permission : permissions) {
            RolePermission.RolePermissionId id = new RolePermission.RolePermissionId();
            id.setRoleId(role.getId());
            id.setPermissionId(permission.getId());
            
            RolePermission rolePermission = new RolePermission();
            rolePermission.setId(id);
            rolePermission.setRole(role);
            rolePermission.setPermission(permission);
            
            rolePermissionRepository.save(rolePermission);
        }
    }

    private School createSchool(String name) {
        School school = new School();
        school.setName(name);
        return schoolRepository.save(school);
    }

    private TaskType createTaskType(String name) {
        TaskType taskType = new TaskType();
        taskType.setName(name);
        return taskTypeRepository.save(taskType);
    }
}