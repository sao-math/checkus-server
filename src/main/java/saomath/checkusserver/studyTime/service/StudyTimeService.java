package saomath.checkusserver.studyTime.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.studyTime.domain.Activity;
import saomath.checkusserver.studyTime.domain.AssignedStudyTime;
import saomath.checkusserver.studyTime.domain.ActualStudyTime;
import saomath.checkusserver.common.exception.ResourceNotFoundException;
import saomath.checkusserver.common.exception.BusinessException;
import saomath.checkusserver.studyTime.repository.ActivityRepository;
import saomath.checkusserver.studyTime.repository.AssignedStudyTimeRepository;
import saomath.checkusserver.studyTime.repository.ActualStudyTimeRepository;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.user.repository.StudentGuardianRepository;
import saomath.checkusserver.user.repository.StudentProfileRepository;
import saomath.checkusserver.studyTime.dto.StudyTimeMonitorResponse;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.user.domain.StudentGuardian;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class StudyTimeService {

    private final AssignedStudyTimeRepository assignedStudyTimeRepository;
    private final ActualStudyTimeRepository actualStudyTimeRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final StudentProfileRepository studentProfileRepository;
    private static final Logger log = LoggerFactory.getLogger(StudyTimeService.class);

    @Autowired
    public StudyTimeService(
            AssignedStudyTimeRepository assignedStudyTimeRepository,
            ActualStudyTimeRepository actualStudyTimeRepository,
            ActivityRepository activityRepository,
            UserRepository userRepository,
            StudentGuardianRepository studentGuardianRepository,
            StudentProfileRepository studentProfileRepository
    ) {
        this.assignedStudyTimeRepository = assignedStudyTimeRepository;
        this.actualStudyTimeRepository = actualStudyTimeRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.studentGuardianRepository = studentGuardianRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    /**
     * 특정 기간의 모든 배정된 공부 시간을 조회합니다.
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 배정된 공부 시간 목록
     */
    @Transactional(readOnly = true)
    public List<AssignedStudyTime> getAssignedStudyTimesByDateRange(
            LocalDateTime startDate, LocalDateTime endDate) {
        validateTimeRangeForQuery(startDate, endDate);
        return assignedStudyTimeRepository.findStartingBetweenWithDetails(startDate, endDate);
    }

    /**
     * 학생에게 공부 시간을 배정합니다.
     * @param studentId 학생 ID
     * @param activityId 활동 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param assignedBy 배정한 선생님 ID
     * @return 배정된 공부 시간
     */
    public AssignedStudyTime assignStudyTime(Long studentId, String title, Long activityId, 
                                           LocalDateTime startTime, LocalDateTime endTime, 
                                           Long assignedBy) {
        // 입력 검증
        validateStudyTimeInput(studentId, title, activityId, startTime, endTime, assignedBy);
        
        // 시간 겹침 체크
        List<AssignedStudyTime> overlapping = assignedStudyTimeRepository
                .findOverlappingStudyTimes(studentId, startTime, endTime);
        
        if (!overlapping.isEmpty()) {
            throw new BusinessException("해당 시간대에 이미 배정된 공부 시간이 있습니다.");
        }

        AssignedStudyTime assignedStudyTime = AssignedStudyTime.builder()
                .studentId(studentId)
                .title(title)
                .activityId(activityId)
                .startTime(startTime)
                .endTime(endTime)
                .assignedBy(assignedBy)
                .build();

        return assignedStudyTimeRepository.save(assignedStudyTime);
    }

    /**
     * 배정된 공부 시간을 수정합니다.
     * @param id 배정 ID
     * @param activityId 활동 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 수정된 공부 시간
     */
    public AssignedStudyTime updateAssignedStudyTime(Long id, String title, Long activityId, 
                                                   LocalDateTime startTime, LocalDateTime endTime) {
        AssignedStudyTime existing = assignedStudyTimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("배정된 공부 시간을 찾을 수 없습니다."));

        // 제목 업데이트
        if (title != null && !title.trim().isEmpty()) {
            existing.setTitle(title.trim());
        }

        // 활동 검증
        if (activityId != null) {
            validateActivity(activityId);
            existing.setActivityId(activityId);
        }

        // 시간 검증 및 겹침 체크
        if (startTime != null && endTime != null) {
            validateTimeRangeForAssignment(startTime, endTime);
            
            // 본인 제외하고 겹침 체크
            List<AssignedStudyTime> overlapping = assignedStudyTimeRepository
                    .findOverlappingStudyTimes(existing.getStudentId(), startTime, endTime);
            overlapping.removeIf(ast -> ast.getId().equals(id));
            
            if (!overlapping.isEmpty()) {
                throw new BusinessException("해당 시간대에 이미 배정된 공부 시간이 있습니다.");
            }
            
            existing.setStartTime(startTime);
            existing.setEndTime(endTime);
        }

        return assignedStudyTimeRepository.save(existing);
    }

    /**
     * 배정된 공부 시간을 삭제합니다.
     * @param id 배정 ID
     */
    public void deleteAssignedStudyTime(Long id) {
        if (!assignedStudyTimeRepository.existsById(id)) {
            throw new ResourceNotFoundException("배정된 공부 시간을 찾을 수 없습니다.");
        }
        assignedStudyTimeRepository.deleteById(id);
    }

    /**
     * 학생별 특정 기간의 배정된 공부 시간을 조회합니다.
     * @param studentId 학생 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 배정된 공부 시간 목록
     */
    @Transactional(readOnly = true)
    public List<AssignedStudyTime> getAssignedStudyTimesByStudentAndDateRange(
            Long studentId, LocalDateTime startDate, LocalDateTime endDate) {
        validateUser(studentId);
        validateTimeRangeForQuery(startDate, endDate);
        return assignedStudyTimeRepository.findByStudentIdAndStartTimeBetweenWithDetails(
                studentId, startDate, endDate);
    }

    /**
     * 학생별 특정 기간의 실제 공부 시간을 조회합니다.
     * @param studentId 학생 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 실제 공부 시간 목록
     */
    @Transactional(readOnly = true)
    public List<ActualStudyTime> getActualStudyTimesByStudentAndDateRange(
            Long studentId, LocalDateTime startDate, LocalDateTime endDate) {
        validateUser(studentId);
        validateTimeRangeForQuery(startDate, endDate);
        return actualStudyTimeRepository.findByStudentIdAndDateRange(
                studentId, startDate, endDate);
    }

    /**
     * 특정 배정 시간의 실제 접속 기록을 조회합니다.
     * @param assignedStudyTimeId 배정 시간 ID
     * @return 실제 공부 시간 목록
     */
    @Transactional(readOnly = true)
    public List<ActualStudyTime> getActualStudyTimesByAssignedId(Long assignedStudyTimeId) {
        return actualStudyTimeRepository.findByAssignedStudyTimeId(assignedStudyTimeId);
    }

    /**
     * 할당 시간 밖의 학생 접속 기록을 조회합니다.
     * @param studentId 학생 ID
     * @return 할당되지 않은 실제 공부 시간 목록
     */
    @Transactional(readOnly = true)
    public List<ActualStudyTime> getUnassignedActualStudyTimes(Long studentId) {
        validateUser(studentId);
        return actualStudyTimeRepository.findByStudentIdAndAssignedStudyTimeIdIsNull(studentId);
    }

    /**
     * 디스코드 봇용: 학생 접속 시작을 기록합니다.
     * 세션 시작 시간 <= 현재 시간 <= 세션 종료 시간인 할당된 세션에 연결합니다.
     * @param studentId 학생 ID
     * @param startTime 접속 시작 시간
     * @param source 접속 소스 (discord)
     * @return 생성된 실제 공부 시간 기록
     */
    public ActualStudyTime recordStudyStart(Long studentId, LocalDateTime startTime, String source) {
        validateUser(studentId);
        
        // 정확히 할당된 시간 범위 내에서만 연결 (startTime <= 접속시간 <= endTime)
        List<AssignedStudyTime> assignedList = assignedStudyTimeRepository
                .findByStudentIdAndTimeRange(studentId, startTime);
        
        Long assignedStudyTimeId = null;
        if (!assignedList.isEmpty()) {
            // 할당된 시간 범위 내에 접속한 경우 연결
            assignedStudyTimeId = assignedList.get(0).getId();
            log.info("할당된 공부시간에 연결: 학생 ID={}, 할당 ID={}, 접속시간={}", 
                    studentId, assignedStudyTimeId, startTime);
        } else {
            log.info("할당된 공부시간 범위 밖 접속: 학생 ID={}, 접속시간={}", studentId, startTime);
        }
        
        ActualStudyTime actualStudyTime = ActualStudyTime.builder()
                .studentId(studentId)
                .assignedStudyTimeId(assignedStudyTimeId)
                .startTime(startTime)
                .source(source)
                .build();
        
        return actualStudyTimeRepository.save(actualStudyTime);
    }

    /**
     * 디스코드 봇용: 학생 접속 종료를 기록합니다.
     * @param actualStudyTimeId 실제 공부 시간 ID
     * @param endTime 접속 종료 시간
     * @return 수정된 실제 공부 시간 기록
     */
    public ActualStudyTime recordStudyEnd(Long actualStudyTimeId, LocalDateTime endTime) {
        ActualStudyTime actualStudyTime = actualStudyTimeRepository.findById(actualStudyTimeId)
                .orElseThrow(() -> new ResourceNotFoundException("실제 공부 시간 기록을 찾을 수 없습니다."));
        
        actualStudyTime.setEndTime(endTime);
        return actualStudyTimeRepository.save(actualStudyTime);
    }

    /**
     * 디스코드 봇용: 학생 ID로 진행 중인 공부 세션을 종료합니다.
     * @param studentId 학생 ID
     * @param endTime 접속 종료 시간
     * @return 종료된 실제 공부 시간 기록들
     */
    public List<ActualStudyTime> recordStudyEndByStudentId(Long studentId, LocalDateTime endTime) {
        validateUser(studentId);
        
        // 진행 중인 공부 세션들 조회 (endTime이 null인 것들)
        List<ActualStudyTime> ongoingSessions = actualStudyTimeRepository
                .findByStudentIdAndEndTimeIsNullOrderByStartTimeDesc(studentId);
        
        if (ongoingSessions.isEmpty()) {
            log.warn("종료할 진행 중인 공부 세션이 없습니다. 학생 ID: {}", studentId);
            return new ArrayList<>();
        }
        
        // 모든 진행 중인 세션을 종료
        List<ActualStudyTime> endedSessions = new ArrayList<>();
        for (ActualStudyTime session : ongoingSessions) {
            session.setEndTime(endTime);
            ActualStudyTime saved = actualStudyTimeRepository.save(session);
            endedSessions.add(saved);
            log.info("공부 세션 종료: 학생 ID={}, 시작={}, 종료={}", 
                    studentId, session.getStartTime(), endTime);
        }
        
        return endedSessions;
    }

    /**
     * 알림용: 곧 시작할 공부 시간을 조회합니다.
     * @return 10분 후 또는 지금 시작하는 공부 시간 목록
     */
    @Transactional(readOnly = true)
    public List<AssignedStudyTime> getUpcomingStudyTimes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesBefore = now.minusMinutes(10);
        LocalDateTime tenMinutesAfter = now.plusMinutes(10);
        
        return assignedStudyTimeRepository.findUpcomingStudyTimesWithDetails(
                tenMinutesBefore, tenMinutesAfter);
    }

    /**
     * 스케줄러용: 세션 시작 시 학생이 먼저 접속해 있었던 경우 연결 처리합니다.
     * @param assignedStudyTimeId 할당된 공부시간 ID
     * @return 연결된 실제 공부시간 기록 (없으면 null)
     */
    public ActualStudyTime connectSessionOnStart(Long assignedStudyTimeId) {
        AssignedStudyTime assignedStudyTime = assignedStudyTimeRepository.findById(assignedStudyTimeId)
                .orElseThrow(() -> new ResourceNotFoundException("할당된 공부시간을 찾을 수 없습니다."));
        
        Long studentId = assignedStudyTime.getStudentId();
        LocalDateTime assignedStartTime = assignedStudyTime.getStartTime();
        
        // 현재 진행중인 ActualStudyTime 조회 (endTime이 null인 것들)
        List<ActualStudyTime> ongoingSessions = actualStudyTimeRepository
                .findByStudentIdAndEndTimeIsNullOrderByStartTimeDesc(studentId);
        
        if (ongoingSessions.isEmpty()) {
            log.debug("진행중인 세션이 없음: 할당 ID={}, 학생 ID={}", assignedStudyTimeId, studentId);
            return null;
        }
        
        // 가장 최근 진행중인 세션 선택
        ActualStudyTime currentSession = ongoingSessions.get(0);
        
        if (currentSession.getAssignedStudyTimeId() != null) {
            // 2-1. 기존 ActualStudyTime이 다른 할당에 연결된 경우
            // 기존 세션 종료 후 새로운 세션 생성
            currentSession.setEndTime(assignedStartTime);
            actualStudyTimeRepository.save(currentSession);
            
            log.info("기존 세션 종료 후 새 세션 생성: 기존 세션 ID={}, 종료시간={}, 할당 ID={}", 
                    currentSession.getId(), assignedStartTime, assignedStudyTimeId);
            
            // 새로운 ActualStudyTime 생성
            ActualStudyTime newSession = ActualStudyTime.builder()
                    .studentId(studentId)
                    .assignedStudyTimeId(assignedStudyTimeId)
                    .startTime(assignedStartTime)
                    .source(currentSession.getSource())
                    .build();
            
            ActualStudyTime saved = actualStudyTimeRepository.save(newSession);
            log.info("새 세션 생성 완료: 세션 ID={}, 할당 ID={}, 시작시간={}", 
                    saved.getId(), assignedStudyTimeId, assignedStartTime);
            
            return saved;
            
        } else {
            // 2-2. 기존 ActualStudyTime이 미할당 상태인 경우
            // 해당 세션을 새 할당에 연결
            currentSession.setAssignedStudyTimeId(assignedStudyTimeId);
            ActualStudyTime saved = actualStudyTimeRepository.save(currentSession);
            
            log.info("미할당 세션을 새 할당에 연결: 세션 ID={}, 할당 ID={}, 세션 시작시간={}, 할당 시작시간={}", 
                    saved.getId(), assignedStudyTimeId, currentSession.getStartTime(), assignedStartTime);
            
            return saved;
        }
    }

    /**
     * ID로 배정된 공부 시간을 연관 엔티티와 함께 조회합니다.
     * @param id 배정 ID
     * @return 연관 엔티티가 포함된 배정된 공부 시간
     */
    @Transactional(readOnly = true)
    public AssignedStudyTime getAssignedStudyTimeWithDetails(Long id) {
        return assignedStudyTimeRepository.findByIdWithDetails(id);
    }

    /**
     * 공부 배정 가능한 활동 목록을 조회합니다.
     * @return 활동 목록
     */
    @Transactional(readOnly = true)
    public List<Activity> getStudyAssignableActivities() {
        return activityRepository.findByIsStudyAssignableTrue();
    }

    /**
     * 모든 활동 목록을 조회합니다.
     * @return 활동 목록
     */
    @Transactional(readOnly = true)
    public List<Activity> getAllActivities() {
        return activityRepository.findAll();
    }

    /**
     * 활동을 생성합니다.
     * @param name 활동명
     * @param isStudyAssignable 공부 배정 가능 여부
     * @return 생성된 활동
     */
    public Activity createActivity(String name, Boolean isStudyAssignable) {
        if (activityRepository.existsByName(name)) {
            throw new BusinessException("이미 존재하는 활동명입니다.");
        }

        Activity activity = Activity.builder()
                .name(name)
                .isStudyAssignable(isStudyAssignable != null ? isStudyAssignable : false)
                .build();

        return activityRepository.save(activity);
    }

    /**
     * 시간 범위별 학생 공부시간 모니터링 정보를 조회합니다. (기존 버전 - 호환성 유지)
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return 시간 범위별 학생 모니터링 응답
     */
    @Transactional(readOnly = true)
    public StudyTimeMonitorResponse getStudyTimeMonitorByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        // 최적화된 버전으로 위임
        return getStudyTimeMonitorByTimeRangeOptimized(startTime, endTime);
    }

    /**
     * 날짜별 학생 공부시간 모니터링 정보를 조회합니다.
     * @param date 조회할 날짜
     * @return 날짜별 학생 모니터링 응답
     */
    @Transactional(readOnly = true)
    public StudyTimeMonitorResponse getStudyTimeMonitorByDate(LocalDate date) {
        // 날짜를 시간 범위로 변환 (0시부터 다음날 6시까지)
        LocalDateTime startTime = date.atTime(0, 0, 0);
        LocalDateTime endTime = date.plusDays(1).atTime(6, 0, 0);
        
        StudyTimeMonitorResponse response = getStudyTimeMonitorByTimeRangeOptimized(startTime, endTime);
        
        // 기존 API 호환성을 위해 date 필드도 설정
        response.setDate(date);
        
        return response;
    }
    
    /**
     * 시간 범위별 학생 공부시간 모니터링 정보를 조회합니다. (최적화된 버전)
     * N+1 쿼리 문제를 해결하기 위해 배치 쿼리와 메모리 그룹핑을 사용합니다.
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return 시간 범위별 학생 모니터링 응답
     */
    @Transactional(readOnly = true)
    public StudyTimeMonitorResponse getStudyTimeMonitorByTimeRangeOptimized(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRangeForQuery(startTime, endTime);
        
        LocalDateTime now = LocalDateTime.now();
        
        // 1. 모든 재원 중인 학생 조회 (1개 쿼리) - 스터디 모니터링용
        List<User> allStudents = userRepository.findAllEnrolledStudents();
        if (allStudents.isEmpty()) {
            return createEmptyResponse(startTime, endTime);
        }
        
        // 학생 ID 리스트 추출
        List<Long> studentIds = allStudents.stream()
                .map(User::getId)
                .collect(Collectors.toList());
        
        // 대량 데이터 처리를 위한 배치 크기 설정 (IN 절 최대 크기 제한)
        final int BATCH_SIZE = 1000;
        List<List<Long>> studentIdBatches = partitionList(studentIds, BATCH_SIZE);
        
        // 2. 배치로 모든 관련 데이터 조회 (배치당 4개 쿼리)
        Map<Long, List<StudentGuardian>> guardianMap = new HashMap<>();
        Map<Long, List<AssignedStudyTime>> assignedStudyTimeMap = new HashMap<>();
        Map<Long, List<ActualStudyTime>> unassignedActualMap = new HashMap<>();
        List<AssignedStudyTime> allAssignedStudyTimes = new ArrayList<>();
        
        for (List<Long> batch : studentIdBatches) {
            // 보호자 정보 배치 조회
            List<StudentGuardian> guardians = studentGuardianRepository.findByStudentIds(batch);
            guardians.forEach(sg -> 
                guardianMap.computeIfAbsent(sg.getStudent().getId(), k -> new ArrayList<>()).add(sg)
            );
            
            // 할당된 공부시간 배치 조회
            List<AssignedStudyTime> assignedTimes = assignedStudyTimeRepository
                    .findByStudentIdsAndDateRangeWithDetails(batch, startTime, endTime);
            assignedTimes.forEach(ast -> {
                assignedStudyTimeMap.computeIfAbsent(ast.getStudentId(), k -> new ArrayList<>()).add(ast);
                allAssignedStudyTimes.add(ast);
            });
            
            // 할당되지 않은 실제 접속 기록 배치 조회
            List<ActualStudyTime> unassignedActuals = actualStudyTimeRepository
                    .findByStudentIdsAndDateRangeAndAssignedStudyTimeIdIsNull(batch, startTime, endTime);
            unassignedActuals.forEach(aat -> 
                unassignedActualMap.computeIfAbsent(aat.getStudentId(), k -> new ArrayList<>()).add(aat)
            );
        }
        
        // 3. 할당된 공부시간에 연결된 실제 접속 기록 배치 조회
        Map<Long, List<ActualStudyTime>> connectedActualMap = new HashMap<>();
        if (!allAssignedStudyTimes.isEmpty()) {
            List<Long> assignedStudyTimeIds = allAssignedStudyTimes.stream()
                    .map(AssignedStudyTime::getId)
                    .collect(Collectors.toList());
            
            List<List<Long>> assignedIdBatches = partitionList(assignedStudyTimeIds, BATCH_SIZE);
            for (List<Long> batch : assignedIdBatches) {
                List<ActualStudyTime> connectedActuals = actualStudyTimeRepository
                        .findByAssignedStudyTimeIds(batch);
                connectedActuals.forEach(cat -> 
                    connectedActualMap.computeIfAbsent(cat.getAssignedStudyTimeId(), k -> new ArrayList<>()).add(cat)
                );
            }
        }
        
        // 4. 메모리에서 데이터 조합
        List<StudyTimeMonitorResponse.StudentStudyInfo> studentInfos = allStudents.stream()
                .map(student -> buildStudentStudyInfo(
                        student,
                        guardianMap.getOrDefault(student.getId(), Collections.emptyList()),
                        assignedStudyTimeMap.getOrDefault(student.getId(), Collections.emptyList()),
                        unassignedActualMap.getOrDefault(student.getId(), Collections.emptyList()),
                        connectedActualMap,
                        now
                ))
                .collect(Collectors.toList());
        
        StudyTimeMonitorResponse response = new StudyTimeMonitorResponse();
        response.setStartTime(startTime);
        response.setEndTime(endTime);
        response.setStudents(studentInfos);
        
        log.info("재원 중인 학생 모니터링 데이터 조회 완료: 재원생 {}명, 배치 {}개", 
                allStudents.size(), studentIdBatches.size());
        
        return response;
    }

    /**
     * 학생의 보호자 정보를 조회합니다.
     * @param studentId 학생 ID
     * @return 보호자 정보 목록
     */
    private List<StudyTimeMonitorResponse.GuardianInfo> getGuardianInfos(Long studentId) {
        List<StudentGuardian> studentGuardians = studentGuardianRepository.findByStudentId(studentId);
        
        return studentGuardians.stream()
                .map(sg -> new StudyTimeMonitorResponse.GuardianInfo(
                        sg.getGuardian().getId(),
                        sg.getGuardian().getPhoneNumber(),
                        sg.getRelationship()
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * 학생의 현재 상태를 결정합니다.
     * @param assignedStudyTimes 할당된 공부시간 목록
     * @param unassignedActuals 할당되지 않은 실제 접속 기록 목록
     * @param connectedActualMap 연결된 실제 접속 기록 맵
     * @param now 현재 시간
     * @return 학생 현재 상태
     */
    private StudyTimeMonitorResponse.StudentCurrentStatus determineStudentStatus(
            List<AssignedStudyTime> assignedStudyTimes, 
            List<ActualStudyTime> unassignedActuals, 
            Map<Long, List<ActualStudyTime>> connectedActualMap,
            LocalDateTime now) {
        
        // 현재 시간에 할당된 공부시간이 있는지 확인
        AssignedStudyTime currentAssigned = assignedStudyTimes.stream()
                .filter(assigned -> !now.isBefore(assigned.getStartTime()) && !now.isAfter(assigned.getEndTime()))
                .findFirst()
                .orElse(null);
        
        if (currentAssigned != null) {
            // 현재 할당된 시간이 있음
            // 해당 할당에 연결된 현재 진행중인 접속 기록이 있는지 확인 (이미 가져온 데이터 사용)
            List<ActualStudyTime> connectedActuals = connectedActualMap
                    .getOrDefault(currentAssigned.getId(), Collections.emptyList());
            
            boolean isCurrentlyAttending = connectedActuals.stream()
                    .anyMatch(actual -> actual.getEndTime() == null || 
                                       (!now.isBefore(actual.getStartTime()) && 
                                        (actual.getEndTime() == null || !now.isAfter(actual.getEndTime()))));
            
            return isCurrentlyAttending ? 
                    StudyTimeMonitorResponse.StudentCurrentStatus.ATTENDING : 
                    StudyTimeMonitorResponse.StudentCurrentStatus.ABSENT;
        } else {
            // 현재 할당된 시간이 없음
            return StudyTimeMonitorResponse.StudentCurrentStatus.NO_ASSIGNED_TIME;
        }
    }

    // 최적화를 위한 헬퍼 메서드들
    
    /**
     * 리스트를 지정된 크기의 배치로 분할합니다.
     * @param list 분할할 리스트
     * @param batchSize 배치 크기
     * @return 분할된 배치 리스트
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }
    
    /**
     * 빈 응답 객체를 생성합니다.
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 빈 모니터링 응답
     */
    private StudyTimeMonitorResponse createEmptyResponse(LocalDateTime startTime, LocalDateTime endTime) {
        StudyTimeMonitorResponse response = new StudyTimeMonitorResponse();
        response.setStartTime(startTime);
        response.setEndTime(endTime);
        response.setStudents(Collections.emptyList());
        return response;
    }
    
    /**
     * 학생의 공부 정보를 구성합니다.
     * @param student 학생 정보
     * @param guardians 보호자 목록
     * @param assignedStudyTimes 할당된 공부시간 목록
     * @param unassignedActuals 할당되지 않은 실제 접속 기록 목록
     * @param connectedActualMap 연결된 실제 접속 기록 맵
     * @param now 현재 시간
     * @return 학생 공부 정보
     */
    private StudyTimeMonitorResponse.StudentStudyInfo buildStudentStudyInfo(
            User student,
            List<StudentGuardian> guardians,
            List<AssignedStudyTime> assignedStudyTimes,
            List<ActualStudyTime> unassignedActuals,
            Map<Long, List<ActualStudyTime>> connectedActualMap,
            LocalDateTime now) {
        
        StudyTimeMonitorResponse.StudentStudyInfo studentInfo = new StudyTimeMonitorResponse.StudentStudyInfo();
        studentInfo.setStudentId(student.getId());
        studentInfo.setStudentName(student.getName());
        studentInfo.setStudentPhone(student.getPhoneNumber());
        
        // 보호자 정보 변환
        List<StudyTimeMonitorResponse.GuardianInfo> guardianInfos = guardians.stream()
                .map(sg -> new StudyTimeMonitorResponse.GuardianInfo(
                        sg.getGuardian().getId(),
                        sg.getGuardian().getPhoneNumber(),
                        sg.getRelationship()
                ))
                .collect(Collectors.toList());
        studentInfo.setGuardians(guardianInfos);
        
        // 할당된 공부시간 정보 변환
        List<StudyTimeMonitorResponse.AssignedStudyInfo> assignedInfos = assignedStudyTimes.stream()
                .map(assignedTime -> {
                    StudyTimeMonitorResponse.AssignedStudyInfo assignedInfo = new StudyTimeMonitorResponse.AssignedStudyInfo();
                    assignedInfo.setAssignedStudyTimeId(assignedTime.getId());
                    assignedInfo.setTitle(assignedTime.getTitle());
                    assignedInfo.setStartTime(assignedTime.getStartTime());
                    assignedInfo.setEndTime(assignedTime.getEndTime());
                    
                    // 연결된 실제 접속 기록 조회
                    List<ActualStudyTime> connectedActuals = connectedActualMap
                            .getOrDefault(assignedTime.getId(), Collections.emptyList());
                    
                    List<StudyTimeMonitorResponse.ConnectedActualStudyInfo> connectedInfos = connectedActuals.stream()
                            .map(actual -> new StudyTimeMonitorResponse.ConnectedActualStudyInfo(
                                    actual.getId(),
                                    actual.getStartTime(),
                                    actual.getEndTime()
                            ))
                            .collect(Collectors.toList());
                    
                    assignedInfo.setConnectedActualStudyTimes(connectedInfos);
                    return assignedInfo;
                })
                .collect(Collectors.toList());
        studentInfo.setAssignedStudyTimes(assignedInfos);
        
        // 할당되지 않은 실제 접속 기록 변환
        List<StudyTimeMonitorResponse.UnassignedActualStudyInfo> unassignedInfos = unassignedActuals.stream()
                .map(actual -> new StudyTimeMonitorResponse.UnassignedActualStudyInfo(
                        actual.getId(),
                        actual.getStartTime(),
                        actual.getEndTime()
                ))
                .collect(Collectors.toList());
        studentInfo.setUnassignedActualStudyTimes(unassignedInfos);
        
        // 학생 현재 상태 결정
        StudyTimeMonitorResponse.StudentCurrentStatus status = determineStudentStatus(
                assignedStudyTimes, unassignedActuals, connectedActualMap, now);
        studentInfo.setStatus(status);
        
        return studentInfo;
    }

    // 검증 메서드들
    private void validateStudyTimeInput(Long studentId, String title, Long activityId, 
                                      LocalDateTime startTime, LocalDateTime endTime, 
                                      Long assignedBy) {
        validateUser(studentId);
        validateUser(assignedBy);
        validateTitle(title);
        validateActivity(activityId);
        validateTimeRangeForAssignment(startTime, endTime);
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId);
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException("일정 제목은 필수입니다.");
        }
        if (title.length() > 255) {
            throw new BusinessException("일정 제목은 255자를 초과할 수 없습니다.");
        }
    }

    private void validateActivity(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("활동을 찾을 수 없습니다. ID: " + activityId));
        
        if (!activity.getIsStudyAssignable()) {
            throw new BusinessException("해당 활동은 공부 시간으로 배정할 수 없습니다.");
        }
    }

    private void validateTimeRangeForAssignment(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new BusinessException("시작 시간이 종료 시간보다 늦을 수 없습니다.");
        }
        
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("과거 시간으로는 공부 시간을 배정할 수 없습니다.");
        }
    }

    private void validateTimeRangeForQuery(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new BusinessException("시작 시간이 종료 시간보다 늦을 수 없습니다.");
        }
        
        // 조회용이므로 과거 시간 제한 없음
        // 단, 너무 오래된 데이터 조회 방지 (1년 전까지만)
        if (startTime.isBefore(LocalDateTime.now().minusYears(1))) {
            throw new BusinessException("조회 가능한 기간을 초과했습니다. 최대 1년 전까지 조회 가능합니다.");
        }
    }
}
