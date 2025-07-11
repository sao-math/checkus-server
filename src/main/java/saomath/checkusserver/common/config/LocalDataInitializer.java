package saomath.checkusserver.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.auth.domain.*;
import saomath.checkusserver.auth.repository.PermissionRepository;
import saomath.checkusserver.auth.repository.RolePermissionRepository;
import saomath.checkusserver.auth.repository.RoleRepository;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.classroom.repository.StudentClassRepository;
import saomath.checkusserver.studyTime.domain.*;
import saomath.checkusserver.studyTime.repository.*;
import saomath.checkusserver.task.domain.TaskType;
import saomath.checkusserver.task.repository.TaskTypeRepository;
import saomath.checkusserver.user.domain.RoleConstants;
import saomath.checkusserver.school.domain.School;
import saomath.checkusserver.user.domain.StudentGuardian;
import saomath.checkusserver.user.domain.StudentProfile;
import saomath.checkusserver.school.repository.SchoolRepository;
import saomath.checkusserver.user.repository.StudentGuardianRepository;
import saomath.checkusserver.user.repository.StudentProfileRepository;
import saomath.checkusserver.user.service.UserRoleService;
import org.springframework.security.crypto.password.PasswordEncoder;
import saomath.checkusserver.weeklySchedule.domain.WeeklySchedule;
import saomath.checkusserver.weeklySchedule.repository.WeeklyScheduleRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Profile({"local"})
@RequiredArgsConstructor
public class LocalDataInitializer implements CommandLineRunner {

    @Value("${test-data.student.phone:}")
    private String testStudentPhone;
    
    @Value("${test-data.student.discord-id:}")
    private String testStudentDiscordId;

    @Value("${test-data.guardian.phone:}")
    private String testGuardianPhone;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final SchoolRepository schoolRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final ActivityRepository activityRepository;
    private final StudentClassRepository studentClassRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final AssignedStudyTimeRepository assignedStudyTimeRepository;
    private final ActualStudyTimeRepository actualStudyTimeRepository;

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
        
        if (isTestDataConfigured()) {
            log.info("실제 테스트 데이터 사용 (설정 파일에서 읽음)");
        } else {
            log.info("더미 테스트 데이터 사용 (설정 파일 미설정)");
        }
        
        // 학교 조회
        School ihyeon = schoolRepository.findByName("이현중").orElseThrow();
        School songok = schoolRepository.findByName("손곡중").orElseThrow();
        School sinbong = schoolRepository.findByName("신봉중").orElseThrow();
        School suji = schoolRepository.findByName("수지중").orElseThrow();
        School seowon = schoolRepository.findByName("서원중").orElseThrow();
        
        // 관리자 계정
        User admin = createUserWithRole("admin", "관리자", "010-0000-0000", "Password123!", "admin#1234", RoleConstants.ADMIN);
        
        // 교사 계정들
        User teacher1 = createUserWithRole("teacher1", "김선생", "010-1111-1111", "Password123!", "teacher1#1234", RoleConstants.TEACHER);
        User teacher2 = createUserWithRole("teacher2", "이선생", "010-1111-2222", "Password123!", "teacher2#5678", RoleConstants.TEACHER);
        User teacher3 = createUserWithRole("teacher3", "박선생", "010-1111-3333", "Password123!", "teacher3#9012", RoleConstants.TEACHER);
        
        // 학생 계정들 20명 생성
        String[] studentNames = {
            "김학생", "이학생", "박학생", "최학생", "정학생", "강학생", "조학생", "윤학생", "장학생", "임학생",
            "한학생", "오학생", "서학생", "신학생", "권학생", "황학생", "안학생", "송학생", "류학생", "전학생"
        };
        
        School[] schools = {ihyeon, songok, sinbong, suji, seowon};
        StudentProfile.Gender[] genders = {StudentProfile.Gender.MALE, StudentProfile.Gender.FEMALE};
        int[] grades = {1, 2, 3};
        
        User[] students = new User[20];
        
        for (int i = 0; i < 20; i++) {
            String studentPhone = hasValue(testStudentPhone) && i == 0 ? testStudentPhone : String.format("010-2222-%04d", 1111 + i);
            String studentDiscordId = hasValue(testStudentDiscordId) && i == 0 ? testStudentDiscordId : String.format("student%d#%04d", i + 1, 1234 + i);
        
            students[i] = createUserWithRole(
                String.format("student%d", i + 1), 
                studentNames[i], 
                studentPhone, 
                "Password123!", 
                studentDiscordId, 
                RoleConstants.STUDENT
            );
            
            // 학교, 학년, 성별을 다양하게 분산
            School school = schools[i % schools.length];
            int grade = grades[i % grades.length];
            StudentProfile.Gender gender = genders[i % genders.length];
            
            createStudentProfile(students[i], StudentProfile.StudentStatus.ENROLLED, school, grade, gender);
        }
        
        // 학부모 계정들 (처음 5명의 학생에 대해서만)
        String guardianPhone = hasValue(testGuardianPhone) ? testGuardianPhone : "010-3333-1111";
        User guardian1 = createUserWithRole("parent1", "김학부모", guardianPhone, "Password123!", "parent1#1234", RoleConstants.GUARDIAN);
        User guardian2 = createUserWithRole("parent2", "이학부모", "010-3333-2222", "Password123!", "parent2#5678", RoleConstants.GUARDIAN);
        User guardian3 = createUserWithRole("parent3", "박학부모", "010-3333-3333", "Password123!", "parent3#9012", RoleConstants.GUARDIAN);
        User guardian4 = createUserWithRole("parent4", "김어머니", "010-3333-4444", "Password123!", "parent4#3456", RoleConstants.GUARDIAN);
        User guardian5 = createUserWithRole("parent5", "이어머니", "010-3333-5555", "Password123!", "parent5#7890", RoleConstants.GUARDIAN);

        // 활동 종류(학원, 자습)
        Activity academy = activityRepository.save(Activity.builder().name("학원").isStudyAssignable(false).build());
        Activity selfStudy = activityRepository.save(Activity.builder().name("자습").isStudyAssignable(true).build());

        // 처음 5명의 학생에 대해 학부모 관계 설정
        User[] guardians = {guardian1, guardian2, guardian3, guardian4, guardian5};
        for (int i = 0; i < 5; i++) {
        studentGuardianRepository.save(StudentGuardian.builder()
                .id(new StudentGuardian.StudentGuardianId(students[i].getId(), guardians[i].getId()))
                .student(students[i])
                .guardian(guardians[i])
            .relationship("father")
            .build());
        }

        // 첫 번째 학생에게만 주간 고정 일정 추가
        weeklyScheduleRepository.save(WeeklySchedule.builder()
            .studentId(students[0].getId())
            .title("수학 학원")
            .activityId(academy.getId())
            .dayOfWeek(1)
            .startTime(java.time.LocalTime.of(16, 0))
            .endTime(java.time.LocalTime.of(18, 0))
            .build());
        weeklyScheduleRepository.save(WeeklySchedule.builder()
            .studentId(students[0].getId())
            .title("수학 숙제")
            .activityId(academy.getId())
            .dayOfWeek(3)
            .startTime(java.time.LocalTime.of(18, 0))
            .endTime(java.time.LocalTime.of(19, 0))
            .build());
        weeklyScheduleRepository.save(WeeklySchedule.builder()
            .studentId(students[0].getId())
            .title("영어 자습")
            .activityId(selfStudy.getId())
            .dayOfWeek(3)
            .startTime(java.time.LocalTime.of(16, 0))
            .endTime(java.time.LocalTime.of(18, 0))
            .build());

        // 20명의 학생에게 다양한 스터디 타임 데이터 추가
        addActualStudyTime(students, selfStudy, teacher1, teacher2, teacher3);
    }

    private void addActualStudyTime(User[] students, Activity selfStudy, User teacher1, User teacher2, User teacher3) {
        // 현재 시간을 기준으로 다양한 우선순위 패턴 생성
        LocalDateTime now = LocalDateTime.now();
        log.info("현재 시간 기준 샘플 데이터 추가: {}", now);

        User[] teachers = {teacher1, teacher2, teacher3};
        
        // 20명의 학생을 5가지 우선순위 패턴으로 분배
        for (int i = 0; i < students.length; i++) {
            User student = students[i];
            User assignedTeacher = teachers[i % teachers.length];
            
            // 우선순위별로 4명씩 분배
            int priorityGroup = i / 4; // 0~4 (각 그룹당 4명)
            int studentInGroup = i % 4; // 그룹 내 순서
            
            switch (priorityGroup) {
                case 0: // Priority 1: 현재 공부해야 하는데 결석인 학생 (4명)
                    addCurrentlyAbsentPattern(student, selfStudy, assignedTeacher, now, studentInGroup);
                    break;
                case 1: // Priority 2: 곧 공부해야 하는 학생 (30분 이내) (4명)
                    addUpcomingStudyPattern(student, selfStudy, assignedTeacher, now, studentInGroup);
                    break;
                case 2: // Priority 3: 현재 공부 중이고 출석한 학생 (4명)
                    addCurrentlyAttendingPattern(student, selfStudy, assignedTeacher, now, studentInGroup);
                    break;
                case 3: // Priority 4: 나머지 다음 공부 시간이 있는 학생 (4명)
                    addFutureStudyPattern(student, selfStudy, assignedTeacher, now, studentInGroup);
                    break;
                case 4: // Priority 5: 공부 시간 할당이 없는 학생 (4명)
                    addNoAssignmentPattern(student, selfStudy, now, studentInGroup);
                    break;
            }
        }

        log.info("현재 시간 기준 샘플 데이터 추가 완료");
        log.info("Priority 1(현재 결석) 4명, Priority 2(곧 시작) 4명, Priority 3(현재 출석) 4명, Priority 4(미래 할당) 4명, Priority 5(할당 없음) 4명");
    }

    // Priority 1: 현재 공부해야 하는데 결석인 학생
    private void addCurrentlyAbsentPattern(User student, Activity selfStudy, User teacher, LocalDateTime now, int index) {
        // 현재 시간 기준으로 30분 전부터 30분 후까지의 시간대에 할당
        LocalDateTime startTime = now.minusMinutes(30 + (index * 10)); // 30분 전부터 10분씩 차이
        LocalDateTime endTime = startTime.plusHours(2);
        
        AssignedStudyTime assigned = assignedStudyTimeRepository.save(AssignedStudyTime.builder()
            .studentId(student.getId())
            .title("수학 자습 (현재 결석)")
            .activityId(selfStudy.getId())
            .startTime(startTime)
            .endTime(endTime)
            .assignedBy(teacher.getId())
            .build());

        // 접속 기록 없음 (완전 결석)
        log.info("Priority 1 - 학생 {}: 현재 공부해야 하는데 결석 ({} ~ {})", 
                student.getName(), startTime.toLocalTime(), endTime.toLocalTime());
    }

    // Priority 2: 곧 공부해야 하는 학생 (30분 이내)
    private void addUpcomingStudyPattern(User student, Activity selfStudy, User teacher, LocalDateTime now, int index) {
        // 5분~25분 후에 시작하는 시간대
        LocalDateTime startTime = now.plusMinutes(5 + (index * 5)); // 5, 10, 15, 20분 후
        LocalDateTime endTime = startTime.plusHours(2);
        
        assignedStudyTimeRepository.save(AssignedStudyTime.builder()
            .studentId(student.getId())
            .title("영어 자습 (곧 시작)")
            .activityId(selfStudy.getId())
            .startTime(startTime)
            .endTime(endTime)
            .assignedBy(teacher.getId())
            .build());

        // 미래 시간이므로 접속 기록 없음
        log.info("Priority 2 - 학생 {}: 곧 공부 시작 ({} ~ {})", 
                student.getName(), startTime.toLocalTime(), endTime.toLocalTime());
    }

    // Priority 3: 현재 공부 중이고 출석한 학생
    private void addCurrentlyAttendingPattern(User student, Activity selfStudy, User teacher, LocalDateTime now, int index) {
        // 현재 시간을 포함하는 시간대
        LocalDateTime startTime = now.minusMinutes(60 + (index * 10)); // 1시간 전부터
        LocalDateTime endTime = now.plusMinutes(60 + (index * 10)); // 1시간 후까지
        
        AssignedStudyTime assigned = assignedStudyTimeRepository.save(AssignedStudyTime.builder()
            .studentId(student.getId())
            .title("과학 자습 (현재 출석)")
            .activityId(selfStudy.getId())
            .startTime(startTime)
            .endTime(endTime)
            .assignedBy(teacher.getId())
            .build());

        // 시작 시간부터 현재까지 접속 중
        actualStudyTimeRepository.save(ActualStudyTime.builder()
            .studentId(student.getId())
            .assignedStudyTimeId(assigned.getId())
            .startTime(startTime.plusMinutes(5)) // 5분 늦게 시작
            .endTime(null) // 아직 접속 중 (종료 시간 없음)
            .source("discord")
            .build());

        log.info("Priority 3 - 학생 {}: 현재 출석 중 ({} ~ {})", 
                student.getName(), startTime.toLocalTime(), endTime.toLocalTime());
    }

    // Priority 4: 나머지 다음 공부 시간이 있는 학생
    private void addFutureStudyPattern(User student, Activity selfStudy, User teacher, LocalDateTime now, int index) {
        // 1시간~3시간 후에 시작하는 시간대
        LocalDateTime startTime = now.plusHours(1 + index); // 1, 2, 3, 4시간 후
        LocalDateTime endTime = startTime.plusHours(2);
        
        assignedStudyTimeRepository.save(AssignedStudyTime.builder()
            .studentId(student.getId())
            .title("국어 자습 (미래 할당)")
            .activityId(selfStudy.getId())
            .startTime(startTime)
            .endTime(endTime)
            .assignedBy(teacher.getId())
            .build());

        // 과거 시간대에 완료된 접속 기록 추가 (선택적)
        if (index < 2) {
            LocalDateTime pastStart = now.minusHours(3 + index);
            LocalDateTime pastEnd = pastStart.plusHours(1);
            
            actualStudyTimeRepository.save(ActualStudyTime.builder()
                .studentId(student.getId())
                .assignedStudyTimeId(null) // 자유 접속
                .startTime(pastStart)
                .endTime(pastEnd)
                .source("discord")
                .build());
        }

        log.info("Priority 4 - 학생 {}: 미래 할당 ({} ~ {})", 
                student.getName(), startTime.toLocalTime(), endTime.toLocalTime());
    }

    // Priority 5: 공부 시간 할당이 없는 학생
    private void addNoAssignmentPattern(User student, Activity selfStudy, LocalDateTime now, int index) {
        // 할당된 시간 없음, 과거의 자유 접속 기록만
        LocalDateTime pastStart = now.minusHours(2 + index);
        LocalDateTime pastEnd = pastStart.plusMinutes(45);
        
        actualStudyTimeRepository.save(ActualStudyTime.builder()
            .studentId(student.getId())
            .assignedStudyTimeId(null) // 자유 접속
            .startTime(pastStart)
            .endTime(pastEnd)
            .source("discord")
            .build());

        // 일부 학생은 추가 자유 접속 기록
        if (index % 2 == 0) {
            LocalDateTime morningStart = now.minusHours(5 + index);
            LocalDateTime morningEnd = morningStart.plusMinutes(30);
            
            actualStudyTimeRepository.save(ActualStudyTime.builder()
                .studentId(student.getId())
                .assignedStudyTimeId(null)
                .startTime(morningStart)
                .endTime(morningEnd)
                .source("discord")
                .build());
        }

        log.info("Priority 5 - 학생 {}: 할당 없음 (자유 접속만)", student.getName());
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
    
    /**
     * 환경변수가 설정되어 있는지 확인
     */
    private boolean isTestDataConfigured() {
        return hasValue(testStudentPhone) && hasValue(testStudentDiscordId) && hasValue(testGuardianPhone);
    }
    
    /**
     * 문자열이 비어있지 않은지 확인
     */
    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }
}