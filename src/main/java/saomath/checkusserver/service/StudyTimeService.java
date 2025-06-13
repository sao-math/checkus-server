package saomath.checkusserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.entity.Activity;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.ActualStudyTime;
import saomath.checkusserver.exception.ResourceNotFoundException;
import saomath.checkusserver.exception.BusinessException;
import saomath.checkusserver.repository.ActivityRepository;
import saomath.checkusserver.repository.AssignedStudyTimeRepository;
import saomath.checkusserver.repository.ActualStudyTimeRepository;
import saomath.checkusserver.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class StudyTimeService {

    private final AssignedStudyTimeRepository assignedStudyTimeRepository;
    private final ActualStudyTimeRepository actualStudyTimeRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Autowired
    public StudyTimeService(
            AssignedStudyTimeRepository assignedStudyTimeRepository,
            ActualStudyTimeRepository actualStudyTimeRepository,
            ActivityRepository activityRepository,
            UserRepository userRepository
    ) {
        this.assignedStudyTimeRepository = assignedStudyTimeRepository;
        this.actualStudyTimeRepository = actualStudyTimeRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
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
        return assignedStudyTimeRepository.findByStudentIdAndStartTimeBetween(
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
     * @param studentId 학생 ID
     * @param startTime 접속 시작 시간
     * @param source 접속 소스 (discord)
     * @return 생성된 실제 공부 시간 기록
     */
    public ActualStudyTime recordStudyStart(Long studentId, LocalDateTime startTime, String source) {
        validateUser(studentId);
        
        // 해당 시간에 배정된 공부시간이 있는지 확인
        List<AssignedStudyTime> assignedList = assignedStudyTimeRepository
                .findByStudentIdAndStartTimeBetween(studentId, 
                        startTime.minusMinutes(30), startTime.plusMinutes(30));
        
        Long assignedStudyTimeId = null;
        if (!assignedList.isEmpty()) {
            // 가장 가까운 배정 시간 찾기
            assignedStudyTimeId = assignedList.get(0).getId();
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
     * 알림용: 곧 시작할 공부 시간을 조회합니다.
     * @return 10분 후 또는 지금 시작하는 공부 시간 목록
     */
    @Transactional(readOnly = true)
    public List<AssignedStudyTime> getUpcomingStudyTimes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesBefore = now.minusMinutes(10);
        LocalDateTime tenMinutesAfter = now.plusMinutes(10);
        
        return assignedStudyTimeRepository.findUpcomingStudyTimesV2(
                tenMinutesBefore, now, tenMinutesAfter);
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
        
        if (startTime.isBefore(LocalDateTime.now().minusDays(1))) {
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
