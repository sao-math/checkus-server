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
import saomath.checkusserver.study.domain.*;
import saomath.checkusserver.study.repository.*;
import saomath.checkusserver.user.domain.RoleConstants;
import saomath.checkusserver.school.domain.School;
import saomath.checkusserver.user.domain.StudentGuardian;
import saomath.checkusserver.user.domain.StudentProfile;
import saomath.checkusserver.school.repository.SchoolRepository;
import saomath.checkusserver.user.repository.StudentGuardianRepository;
import saomath.checkusserver.user.repository.StudentProfileRepository;
import saomath.checkusserver.user.service.UserRoleService;
import org.springframework.security.crypto.password.PasswordEncoder;

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
        
        // 관리자 계정
        User admin = createUserWithRole("admin", "관리자", "010-0000-0000", "Password123!", "admin#1234", RoleConstants.ADMIN);
        
        // 교사 계정들
        User teacher1 = createUserWithRole("teacher1", "김선생", "010-1111-1111", "Password123!", "teacher1#1234", RoleConstants.TEACHER);
        User teacher2 = createUserWithRole("teacher2", "이선생", "010-1111-2222", "Password123!", "teacher2#5678", RoleConstants.TEACHER);
        
        // 학생 계정들 + 프로필 (환경변수가 있으면 실제 데이터 사용)
        String studentPhone = hasValue(testStudentPhone) ? testStudentPhone : "010-2222-1111";
        String studentDiscordId = hasValue(testStudentDiscordId) ? testStudentDiscordId : "student1#1234";
        
        User student1 = createUserWithRole("student1", "박학생", studentPhone, "Password123!", studentDiscordId, RoleConstants.STUDENT);
        createStudentProfile(student1, StudentProfile.StudentStatus.ENROLLED, ihyeon, 2, StudentProfile.Gender.MALE);
        
        User student2 = createUserWithRole("student2", "최학생", "010-2222-2222", "Password123!", "student2#5678", RoleConstants.STUDENT);
        createStudentProfile(student2, StudentProfile.StudentStatus.ENROLLED, ihyeon, 2, StudentProfile.Gender.FEMALE);
        
        User student3 = createUserWithRole("student3", "정학생", "010-2222-3333", "Password123!", "student3#9012", RoleConstants.STUDENT);
        createStudentProfile(student3, StudentProfile.StudentStatus.ENROLLED, songok, 1, StudentProfile.Gender.MALE);
        
        // 학부모 계정들 (환경변수가 있으면 실제 데이터 사용)
        String guardianPhone = hasValue(testGuardianPhone) ? testGuardianPhone : "010-3333-1111";

        User guardian1 = createUserWithRole("parent1", "박학부모", guardianPhone, "Password123!", "parent1#1234", RoleConstants.GUARDIAN);
        User guardian2 = createUserWithRole("parent2", "최학부모", "010-3333-2222", "Password123!", "parent2#5678", RoleConstants.GUARDIAN);
        User guardian3 = createUserWithRole("parent3", "정학부모", "010-3333-3333", "Password123!", "parent3#9012", RoleConstants.GUARDIAN);
        User guardian4 = createUserWithRole("parent4", "박어머니", "010-3333-4444", "Password123!", "parent4#3456", RoleConstants.GUARDIAN);

        // 활동 종류(학원, 자습)
        Activity academy = activityRepository.save(Activity.builder().name("학원").isStudyAssignable(false).build());
        Activity selfStudy = activityRepository.save(Activity.builder().name("자습").isStudyAssignable(true).build());

//        // 반 정보(월수반)
//        ClassEntity monWedClass = classEntityRepository.save(ClassEntity.builder().name("월수반").build());
//        studentClassRepository.save(StudentClass.builder()
//            .id(new StudentClass.StudentClassId(student1.getId(), monWedClass.getId()))
//            .student(student1)
//            .classEntity(monWedClass)
//            .build());
        
        studentGuardianRepository.save(StudentGuardian.builder()
            .id(new StudentGuardian.StudentGuardianId(student1.getId(), guardian1.getId()))
            .student(student1)
            .guardian(guardian1)
            .relationship("father")
            .build());
            
        studentGuardianRepository.save(StudentGuardian.builder()
            .id(new StudentGuardian.StudentGuardianId(student1.getId(), guardian4.getId()))
            .student(student1)
            .guardian(guardian4)
            .relationship("mother")
            .build());

        // 주간 고정 일정(수학 학원, 수학 숙제, 영어 학원, 영어 숙제)
        // 예시: 월요일(1) 16:00~18:00 수학 학원, 수요일(3) 18:00~19:00 수학 숙제, 수요일(3) 16:00~18:00 영어 학원, 금요일(5) 18:00~19:00 영어 숙제
        weeklyScheduleRepository.save(WeeklySchedule.builder()
            .studentId(student1.getId())
            .title("수학 학원")
            .activityId(academy.getId())
            .dayOfWeek(1)
            .startTime(java.time.LocalTime.of(16, 0))
            .endTime(java.time.LocalTime.of(18, 0))
            .build());
        weeklyScheduleRepository.save(WeeklySchedule.builder()
            .studentId(student1.getId())
            .title("수학 숙제")
            .activityId(academy.getId())
            .dayOfWeek(3)
            .startTime(java.time.LocalTime.of(18, 0))
            .endTime(java.time.LocalTime.of(19, 0))
            .build());
        weeklyScheduleRepository.save(WeeklySchedule.builder()
            .studentId(student1.getId())
            .title("영어 자습")
            .activityId(selfStudy.getId())
            .dayOfWeek(3)
            .startTime(java.time.LocalTime.of(16, 0))
            .endTime(java.time.LocalTime.of(18, 0))
            .build());
        weeklyScheduleRepository.save(WeeklySchedule.builder()
            .studentId(student1.getId())
            .title("영어 숙제")
            .activityId(selfStudy.getId())
            .dayOfWeek(5)
            .startTime(java.time.LocalTime.of(18, 0))
            .endTime(java.time.LocalTime.of(19, 0))
            .build());

        //addActualStudyTime(student1, selfStudy, teacher1, student2, teacher2, student3);
    }

    private void addActualStudyTime(User student1, Activity selfStudy, User teacher1, User student2, User teacher2, User student3) {
        // 오늘 날짜로 세 학생의 다양한 샘플 데이터 추가
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        log.info("오늘 날짜 샘플 데이터 추가: {}", today.toLocalDate());

// ==== 박학생 - 다양한 출석 패턴 ====

// 1. 오전 10:00~12:00 수학 자습 (정상 출석)
        AssignedStudyTime parkMorning = assignedStudyTimeRepository.save(AssignedStudyTime.builder()
                .studentId(student1.getId())
                .title("수학 자습")
                .activityId(selfStudy.getId())
                .startTime(today.withHour(10).withMinute(0))
                .endTime(today.withHour(12).withMinute(0))
                .assignedBy(teacher1.getId())
                .build());

// 10:05~11:45 접속 (정상 출석)
        actualStudyTimeRepository.save(ActualStudyTime.builder()
                .studentId(student1.getId())
                .assignedStudyTimeId(parkMorning.getId())
                .startTime(today.withHour(10).withMinute(5))
                .endTime(today.withHour(11).withMinute(45))
                .source("discord")
                .build());

// 2. 오후 2:00~4:00 영어 자습 (중간 이탈 후 재접속)
        AssignedStudyTime parkAfternoon = assignedStudyTimeRepository.save(AssignedStudyTime.builder()
                .studentId(student1.getId())
                .title("영어 자습")
                .activityId(selfStudy.getId())
                .startTime(today.withHour(14).withMinute(0))
                .endTime(today.withHour(16).withMinute(0))
                .assignedBy(teacher1.getId())
                .build());

// 첫 번째 접속: 14:03~14:45 (42분)
        actualStudyTimeRepository.save(ActualStudyTime.builder()
                .studentId(student1.getId())
                .assignedStudyTimeId(parkAfternoon.getId())
                .startTime(today.withHour(14).withMinute(3))
                .endTime(today.withHour(14).withMinute(45))
                .source("discord")
                .build());

// 두 번째 접속: 15:10~15:50 (40분)
        actualStudyTimeRepository.save(ActualStudyTime.builder()
                .studentId(student1.getId())
                .assignedStudyTimeId(parkAfternoon.getId())
                .startTime(today.withHour(15).withMinute(10))
                .endTime(today.withHour(15).withMinute(50))
                .source("discord")
                .build());

// 3. 할당되지 않은 자유 접속 (오전 8:30~9:30)
        actualStudyTimeRepository.save(ActualStudyTime.builder()
                .studentId(student1.getId())
                .assignedStudyTimeId(null)
                .startTime(today.withHour(8).withMinute(30))
                .endTime(today.withHour(9).withMinute(30))
                .source("discord")
                .build());

// ==== 최학생 - 부분 출석 패턴 ====

// 1. 오전 11:00~13:00 과학 자습 (일부만 출석)
        AssignedStudyTime choiMorning = assignedStudyTimeRepository.save(AssignedStudyTime.builder()
                .studentId(student2.getId())
                .title("과학 자습")
                .activityId(selfStudy.getId())
                .startTime(today.withHour(11).withMinute(0))
                .endTime(today.withHour(13).withMinute(0))
                .assignedBy(teacher2.getId())
                .build());

// 11:30~12:00만 접속 (30분/120분)
        actualStudyTimeRepository.save(ActualStudyTime.builder()
                .studentId(student2.getId())
                .assignedStudyTimeId(choiMorning.getId())
                .startTime(today.withHour(11).withMinute(30))
                .endTime(today.withHour(12).withMinute(0))
                .source("discord")
                .build());

// 2. 오후 3:00~5:00 수학 자습 (완전 결석)
        AssignedStudyTime choiAfternoon = assignedStudyTimeRepository.save(AssignedStudyTime.builder()
                .studentId(student2.getId())
                .title("수학 자습")
                .activityId(selfStudy.getId())
                .startTime(today.withHour(15).withMinute(0))
                .endTime(today.withHour(17).withMinute(0))
                .assignedBy(teacher2.getId())
                .build());
// 이 시간에는 접속하지 않음 (완전 결석)

// 3. 할당되지 않은 자유 접속 (오후 6:00~7:00)
        actualStudyTimeRepository.save(ActualStudyTime.builder()
                .studentId(student2.getId())
                .assignedStudyTimeId(null)
                .startTime(today.withHour(18).withMinute(0))
                .endTime(today.withHour(19).withMinute(0))
                .source("discord")
                .build());

// ==== 정학생 - 할당된 시간 없음, 자유 접속만 ====

// 1. 자유 접속: 오전 9:00~10:30
        actualStudyTimeRepository.save(ActualStudyTime.builder()
                .studentId(student3.getId())
                .assignedStudyTimeId(null)
                .startTime(today.withHour(9).withMinute(0))
                .endTime(today.withHour(10).withMinute(30))
                .source("discord")
                .build());

// 2. 자유 접속: 오후 1:00~2:30
        actualStudyTimeRepository.save(ActualStudyTime.builder()
                .studentId(student3.getId())
                .assignedStudyTimeId(null)
                .startTime(today.withHour(13).withMinute(0))
                .endTime(today.withHour(14).withMinute(30))
                .source("discord")
                .build());

// 3. 자유 접속: 오후 4:00~5:45
        actualStudyTimeRepository.save(ActualStudyTime.builder()
                .studentId(student3.getId())
                .assignedStudyTimeId(null)
                .startTime(today.withHour(16).withMinute(0))
                .endTime(today.withHour(17).withMinute(45))
                .source("discord")
                .build());

        log.info("오늘 날짜 샘플 데이터 추가 완료");
        log.info("박학생: 할당된 공부시간 2개(정상출석 + 중간이탈후재접속), 자유접속 1개");
        log.info("최학생: 할당된 공부시간 2개(일부출석 + 완전결석), 자유접속 1개");
        log.info("정학생: 할당된 공부시간 없음, 자유접속 3개");
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