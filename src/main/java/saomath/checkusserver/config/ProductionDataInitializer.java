package saomath.checkusserver.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import saomath.checkusserver.entity.*;
import saomath.checkusserver.repository.PermissionRepository;
import saomath.checkusserver.repository.RoleRepository;
import saomath.checkusserver.repository.SchoolRepository;
import saomath.checkusserver.repository.TaskTypeRepository;
import saomath.checkusserver.repository.UserRepository;
import saomath.checkusserver.service.UserRoleService;

import java.util.Arrays;
import java.util.List;

/**
 * 프로덕션 환경에서 기본 데이터를 초기화하는 컴포넌트
 * data.sql 대신 Java 코드로 데이터를 초기화합니다.
 */
@Component
@RequiredArgsConstructor
@Profile({"prod", "local"})
public class ProductionDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(ProductionDataInitializer.class);

    private final AdminProperties adminProperties;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final SchoolRepository schoolRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeProductionData() {
        log.info("Starting production data initialization...");
        
        initializeRoles();
        initializePermissions();
        initializeSchools();
        initializeTaskTypes();
        initializeAdminUser();
        
        log.info("Production data initialization completed.");
    }
    
    private void initializeRoles() {
        if (roleRepository.count() == 0) {
            log.info("Initializing roles...");
            
            List<Role> roles = Arrays.asList(
                new Role(null, "STUDENT"),
                new Role(null, "TEACHER"),
                new Role(null, "TUTOR"),
                new Role(null, "GUARDIAN"),
                new Role(null, "ADMIN")
            );
            
            roleRepository.saveAll(roles);
            log.info("Created {} roles", roles.size());
        } else {
            log.info("Roles already exist, skipping initialization");
        }
    }
    
    private void initializePermissions() {
        if (permissionRepository.count() == 0) {
            log.info("Initializing permissions...");
            
            List<Permission> permissions = Arrays.asList(
                new Permission(null, "READ_STUDENT_INFO"),
                new Permission(null, "WRITE_STUDENT_INFO"),
                new Permission(null, "ASSIGN_TASK"),
                new Permission(null, "VIEW_SCHEDULE")
            );
            
            permissionRepository.saveAll(permissions);
            log.info("Created {} permissions", permissions.size());
        } else {
            log.info("Permissions already exist, skipping initialization");
        }
    }
    
    private void initializeSchools() {
        if (schoolRepository.count() == 0) {
            log.info("Initializing schools...");
            
            List<School> schools = Arrays.asList(
                new School(null, "리플랜고등학교"),
                new School(null, "사오중학교")
            );
            
            schoolRepository.saveAll(schools);
            log.info("Created {} schools", schools.size());
        } else {
            log.info("Schools already exist, skipping initialization");
        }
    }
    
    private void initializeTaskTypes() {
        if (taskTypeRepository.count() == 0) {
            log.info("Initializing task types...");
            
            List<TaskType> taskTypes = Arrays.asList(
                new TaskType(null, "개념"),
                new TaskType(null, "테스트")
            );
            
            taskTypeRepository.saveAll(taskTypes);
            log.info("Created {} task types", taskTypes.size());
        } else {
            log.info("Task types already exist, skipping initialization");
        }
    }

    private void initializeAdminUser() {
        String adminPassword = adminProperties.getPasswordAsString();
        try {
            // 비밀번호가 설정되지 않았으면 스킵
            if (!StringUtils.hasText(adminPassword)) {
                log.warn("Admin password not configured, skipping admin user creation");
                return;
            }

            // 이미 관리자가 존재하면 스킵
            if (userRepository.count() > 0) {
                log.info("Users already exist, skipping admin user creation");
                return;
            }

            // 관리자 역할 조회
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

            log.info("Creating initial admin user...");

            // 관리자 계정 생성
            User adminUser = User.builder()
                    .username(adminProperties.getUsername())
                    .name(adminProperties.getName())
                    .password(passwordEncoder.encode(adminPassword))
                    .phoneNumber("000-0000-0000")  // 기본값
                    .build();

            // 사용자 저장
            User savedUser = userRepository.save(adminUser);

            // 관리자 역할 할당 (즉시 활성화)
            userRoleService.assignRole(savedUser, "ADMIN", UserRole.RoleStatus.ACTIVE);

            log.info("Initial admin user '{}' created successfully with ADMIN role",
                    adminProperties.getUsername());

        } catch (Exception e) {
            log.error("Failed to create admin user", e);
            throw new RuntimeException("Admin user creation failed", e);
        } finally {
            // 보안을 위해 메모리에서 비밀번호 제거
            adminProperties.clearPassword();
            adminPassword = null;
        }
    }
}