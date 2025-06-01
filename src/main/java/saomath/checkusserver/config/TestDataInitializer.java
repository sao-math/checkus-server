package saomath.checkusserver.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.entity.*;
import saomath.checkusserver.repository.*;
import saomath.checkusserver.auth.AuthService;
import saomath.checkusserver.auth.dto.StudentRegisterRequest;
import saomath.checkusserver.auth.dto.TeacherRegisterRequest;
import saomath.checkusserver.auth.dto.GuardianRegisterRequest;
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
    private final AuthService authService;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("테스트 데이터 초기화 시작...");
        
        // 기존 데이터가 있으면 스킵
        if (userRepository.count() > 0) {
            log.info("이미 데이터가 존재합니다. 초기화를 스킵합니다.");
            log.info("현재 사용자 수: {}", userRepository.count());
            return;
        }

        // 각 단계를 별도 트랜잭션으로 실행
        try {
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
            
        } catch (Exception e) {
            log.error("테스트 데이터 초기화 중 오류 발생", e);
            throw e;
        }
    }

    @Transactional
    protected void initializeRolesAndPermissions() {
        log.info("역할 및 권한 초기화...");
        
        try {
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
            
            log.info("역할 및 권한 초기화 완료");
        } catch (Exception e) {
            log.error("역할 및 권한 초기화 실패", e);
            throw e;
        }
    }

    @Transactional
    protected void initializeSchools() {
        log.info("학교 데이터 초기화...");
        
        try {
            String[] schools = {"이현중", "손곡중", "신봉중", "수지중", "서원중", "홍천중", 
                              "성서중", "상현중", "정평중", "한빛중", "성복중", "문정중", "소현중", "계원예중"};
            
            for (String schoolName : schools) {
                createSchool(schoolName);
            }
            
            log.info("학교 데이터 초기화 완료");
        } catch (Exception e) {
            log.error("학교 데이터 초기화 실패", e);
            throw e;
        }
    }

    @Transactional
    protected void initializeTaskTypes() {
        log.info("과제 유형 초기화...");
        
        try {
            createTaskType("테스트");
            createTaskType("개념");
            log.info("과제 유형 초기화 완료");
        } catch (Exception e) {
            log.error("과제 유형 초기화 실패", e);
            throw e;
        }
    }

    protected void initializeUsers() {
        log.info("사용자 데이터 초기화...");
        
        // 관리자 계정 (별도 트랜잭션)
        createAdminAccount();
        
        // 교사 계정들 (각각 별도 트랜잭션)
        createSingleTeacher("teacher2", "이선생", "010-1111-2222", "Password123!", "teacher2#5678");
        createSingleTeacher("teacher1", "김선생", "010-1111-1111", "Password123!", "teacher1#1234");

        // 학생 계정들 (각각 별도 트랜잭션)
        createSingleStudent("student1", "박학생", "010-2222-1111", "Password123!", "student1#1234", "이현중", 2, StudentProfile.Gender.MALE);
        createSingleStudent("student2", "최학생", "010-2222-2222", "Password123!", "student2#5678", "이현중", 2, StudentProfile.Gender.FEMALE);
        createSingleStudent("student3", "정학생", "010-2222-3333", "Password123!", "student3#9012", "손곡중", 1, StudentProfile.Gender.MALE);
        
        // 학부모 계정들 (각각 별도 트랜잭션)
        createSingleGuardian("parent1", "박학부모", "010-3333-1111", "Password123!", "parent1#1234");
        createSingleGuardian("parent2", "최학부모", "010-3333-2222", "Password123!", "parent2#5678");
        createSingleGuardian("parent3", "정학부모", "010-3333-3333", "Password123!", "parent3#9012");
        
        // 테스트 계정들을 승인 상태로 변경
        approveTestAccounts();
        
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void createAdminAccount() {
        try {
            log.info("관리자 계정 생성 시작...");
            User admin = createAdminUser("admin", "관리자", "010-0000-0000", "admin123", "admin#1234");
            userRoleService.assignRole(admin, RoleConstants.ADMIN, UserRole.RoleStatus.ACTIVE);
            log.info("관리자 계정 생성 완료: {}", admin.getUsername());
        } catch (Exception e) {
            log.error("관리자 계정 생성 실패", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void createSingleTeacher(String username, String name, String phoneNumber, String password, String discordId) {
        try {
            log.info("교사 계정 생성 시작: {}", username);
            
            TeacherRegisterRequest request = new TeacherRegisterRequest();
            request.setUsername(username);
            request.setName(name);
            request.setPhoneNumber(phoneNumber);
            request.setPassword(password);
            request.setDiscordId(discordId);
            
            authService.registerTeacher(request);
            log.info("교사 계정 생성 완료: {}", username);
        } catch (Exception e) {
            log.error("교사 계정 생성 실패: {}", username, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void createSingleStudent(String username, String name, String phoneNumber, String password, String discordId, 
                                     String schoolName, int grade, StudentProfile.Gender gender) {
        try {
            log.info("학생 계정 생성 시작: {}", username);
            
            StudentRegisterRequest request = new StudentRegisterRequest();
            request.setUsername(username);
            request.setName(name);
            request.setPhoneNumber(phoneNumber);
            request.setPassword(password);
            request.setDiscordId(discordId);
            request.setSchoolName(schoolName);
            request.setGrade(grade);
            request.setGender(gender);
            
            authService.registerStudent(request);
            log.info("학생 계정 생성 완료: {}", username);
        } catch (Exception e) {
            log.error("학생 계정 생성 실패: {}", username, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void createSingleGuardian(String username, String name, String phoneNumber, String password, String discordId) {
        try {
            log.info("학부모 계정 생성 시작: {}", username);
            
            GuardianRegisterRequest request = new GuardianRegisterRequest();
            request.setUsername(username);
            request.setName(name);
            request.setPhoneNumber(phoneNumber);
            request.setPassword(password);
            request.setDiscordId(discordId);
            
            authService.registerGuardian(request);
            log.info("학부모 계정 생성 완료: {}", username);
        } catch (Exception e) {
            log.error("학부모 계정 생성 실패: {}", username, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void approveTestAccounts() {
        log.info("테스트 계정 승인 처리 시작...");
        
        String[] testUsernames = {"teacher1", "teacher2", "student1", "student2", "student3", "parent1", "parent2", "parent3"};
        
        for (String username : testUsernames) {
            try {
                User user = userRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    log.info("계정 승인 처리 중: {}", username);
                    
                    // PENDING 상태의 UserRole을 ACTIVE로 변경
                    List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
                    for (UserRole ur : userRoles) {
                        if (UserRole.RoleStatus.PENDING.equals(ur.getStatus())) {
                            ur.setStatus(UserRole.RoleStatus.ACTIVE);
                            userRoleRepository.save(ur);
                            log.info("UserRole 상태 변경 완료: {} -> ACTIVE", username);
                        }
                    }
                    
                    // 학생의 경우 StudentProfile 상태도 ENROLLED로 변경
                    if (username.startsWith("student")) {
                        studentProfileRepository.findByUserId(user.getId())
                            .ifPresent(profile -> {
                                profile.setStatus(StudentProfile.StudentStatus.ENROLLED);
                                studentProfileRepository.save(profile);
                                log.info("StudentProfile 상태 변경 완료: {} -> ENROLLED", username);
                            });
                    }
                    
                    log.info("계정 승인 완료: {}", username);
                } else {
                    log.warn("사용자를 찾을 수 없음: {}", username);
                }
            } catch (Exception e) {
                log.error("계정 승인 실패: {}", username, e);
            }
        }
        
        log.info("테스트 계정 승인 처리 완료");
    }

    // Helper methods
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

    private User createAdminUser(String username, String name, String phoneNumber, String password, String discordId) {
        User user = User.builder()
            .username(username)
            .name(name)
            .phoneNumber(phoneNumber)
            .password(passwordEncoder.encode(password))
            .discordId(discordId)
            .build();
        return userRepository.save(user);
    }
}