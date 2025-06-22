package saomath.checkusserver;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.auth.domain.Permission;
import saomath.checkusserver.auth.domain.Role;
import saomath.checkusserver.school.domain.School;
import saomath.checkusserver.studyTime.domain.TaskType;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.auth.repository.PermissionRepository;
import saomath.checkusserver.auth.repository.RoleRepository;
import saomath.checkusserver.school.repository.SchoolRepository;
import saomath.checkusserver.studyTime.repository.TaskTypeRepository;
import saomath.checkusserver.auth.repository.UserRepository;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Profile({"test"})
public class TestDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final SchoolRepository schoolRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeProductionData() {
        log.info("Starting production data initialization...");

        initializeRoles();
        initializePermissions();
        initializeSchools();
        initializeTaskTypes();
        initializeTestUser();

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

    private void initializeTestUser() {
        if (userRepository.count() == 0) {
            log.info("Initializing test user...");

            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setName("테스트 사용자");
            testUser.setPhoneNumber("010-1234-5678");
            testUser.setPassword("password123"); // 실제 환경에서는 암호화 필요

            userRepository.save(testUser);
            log.info("Created test user");
        } else {
            log.info("Users already exist, skipping initialization");
        }
    }
}