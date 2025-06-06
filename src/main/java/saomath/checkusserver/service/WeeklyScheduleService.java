package saomath.checkusserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.dto.WeeklySchedulePeriodResponse;
import saomath.checkusserver.dto.WeeklyScheduleRequest;
import saomath.checkusserver.dto.WeeklyScheduleResponse;
import saomath.checkusserver.entity.Activity;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.entity.WeeklySchedule;
import saomath.checkusserver.exception.BusinessException;
import saomath.checkusserver.exception.ResourceNotFoundException;
import saomath.checkusserver.repository.ActivityRepository;
import saomath.checkusserver.repository.UserRepository;
import saomath.checkusserver.repository.WeeklyScheduleRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyScheduleService {

    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;

    /**
     * 학생의 주간 시간표 조회
     */
    public List<WeeklyScheduleResponse> getWeeklyScheduleByStudent(Long studentId) {
        log.debug("학생 주간 시간표 조회 - studentId: {}", studentId);

        // 학생 존재 여부 확인
        if (!userRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("학생을 찾을 수 없습니다. ID: " + studentId);
        }

        List<WeeklySchedule> schedules = weeklyScheduleRepository.findByStudentIdWithDetails(studentId);

        return schedules.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 주간 시간표 등록
     */
    @Transactional
    public WeeklyScheduleResponse createWeeklySchedule(WeeklyScheduleRequest request) {
        log.debug("주간 시간표 등록 - studentId: {}, dayOfWeek: {}", request.getStudentId(), request.getDayOfWeek());

        // 유효성 검증
        validateScheduleRequest(request);

        // 시간 겹침 확인
        validateTimeOverlap(request.getStudentId(), request.getDayOfWeek(), 
                           request.getStartTime(), request.getEndTime(), null);

        // 시간표 생성
        WeeklySchedule schedule = WeeklySchedule.builder()
                .studentId(request.getStudentId())
                .activityId(request.getActivityId())
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        WeeklySchedule savedSchedule = weeklyScheduleRepository.save(schedule);

        // 연관 엔티티와 함께 다시 조회
        WeeklySchedule scheduleWithDetails = weeklyScheduleRepository.findById(savedSchedule.getId())
                .orElseThrow(() -> new BusinessException("저장된 시간표를 조회할 수 없습니다."));

        log.info("주간 시간표 등록 성공 - id: {}, studentId: {}", savedSchedule.getId(), request.getStudentId());

        return convertToResponse(scheduleWithDetails);
    }

    /**
     * 주간 시간표 수정
     */
    @Transactional
    public WeeklyScheduleResponse updateWeeklySchedule(Long scheduleId, WeeklyScheduleRequest request) {
        log.debug("주간 시간표 수정 - scheduleId: {}", scheduleId);

        // 기존 시간표 조회
        WeeklySchedule existingSchedule = weeklyScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("시간표를 찾을 수 없습니다. ID: " + scheduleId));

        // 유효성 검증
        validateScheduleRequest(request);

        // 시간 겹침 확인 (현재 시간표 제외)
        validateTimeOverlap(request.getStudentId(), request.getDayOfWeek(), 
                           request.getStartTime(), request.getEndTime(), scheduleId);

        // 시간표 업데이트
        existingSchedule.setStudentId(request.getStudentId());
        existingSchedule.setActivityId(request.getActivityId());
        existingSchedule.setDayOfWeek(request.getDayOfWeek());
        existingSchedule.setStartTime(request.getStartTime());
        existingSchedule.setEndTime(request.getEndTime());

        WeeklySchedule updatedSchedule = weeklyScheduleRepository.save(existingSchedule);

        log.info("주간 시간표 수정 성공 - id: {}", scheduleId);

        return convertToResponse(updatedSchedule);
    }

    /**
     * 주간 시간표 삭제
     */
    @Transactional
    public void deleteWeeklySchedule(Long scheduleId) {
        log.debug("주간 시간표 삭제 - scheduleId: {}", scheduleId);

        if (!weeklyScheduleRepository.existsById(scheduleId)) {
            throw new ResourceNotFoundException("시간표를 찾을 수 없습니다. ID: " + scheduleId);
        }

        weeklyScheduleRepository.deleteById(scheduleId);

        log.info("주간 시간표 삭제 성공 - id: {}", scheduleId);
    }

    /**
     * 특정 기간의 시간표를 실제 날짜로 변환해서 조회
     */
    public List<WeeklySchedulePeriodResponse> getWeeklyScheduleForPeriod(Long studentId, LocalDate startDate, int days) {
        log.debug("기간별 시간표 조회 - studentId: {}, startDate: {}, days: {}", studentId, startDate, days);

        // 학생의 주간 시간표 조회
        List<WeeklySchedule> weeklySchedules = weeklyScheduleRepository.findByStudentIdWithDetails(studentId);

        List<WeeklySchedulePeriodResponse> result = new ArrayList<>();

        // 각 날짜에 대해 해당하는 시간표 찾기
        for (int i = 0; i < days; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            int dayOfWeek = currentDate.getDayOfWeek().getValue(); // 1=월요일, 7=일요일

            // 해당 요일의 시간표들 찾기
            List<WeeklySchedule> daySchedules = weeklySchedules.stream()
                    .filter(schedule -> schedule.getDayOfWeek().equals(dayOfWeek))
                    .collect(Collectors.toList());

            // 실제 날짜와 결합해서 응답 생성
            for (WeeklySchedule schedule : daySchedules) {
                WeeklySchedulePeriodResponse response = convertToPeriodResponse(schedule, currentDate);
                result.add(response);
            }
        }

        return result;
    }

    /**
     * 요청 유효성 검증
     */
    private void validateScheduleRequest(WeeklyScheduleRequest request) {
        // 학생 존재 여부 확인
        if (!userRepository.existsById(request.getStudentId())) {
            throw new ResourceNotFoundException("학생을 찾을 수 없습니다. ID: " + request.getStudentId());
        }

        // 활동 존재 여부 확인
        if (!activityRepository.existsById(request.getActivityId())) {
            throw new ResourceNotFoundException("활동을 찾을 수 없습니다. ID: " + request.getActivityId());
        }

        // 시간 유효성 확인
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessException("시작 시간은 종료 시간보다 빨라야 합니다.");
        }
    }

    /**
     * 시간 겹침 검증
     */
    private void validateTimeOverlap(Long studentId, Integer dayOfWeek, LocalTime startTime, 
                                   LocalTime endTime, Long excludeScheduleId) {
        Long excludeId = excludeScheduleId != null ? excludeScheduleId : -1L;
        
        long overlappingCount = weeklyScheduleRepository.countOverlappingSchedules(
                studentId, dayOfWeek, startTime, endTime, excludeId);

        if (overlappingCount > 0) {
            throw new BusinessException("해당 시간대에 이미 등록된 시간표가 있습니다.");
        }
    }

    /**
     * WeeklySchedule을 WeeklyScheduleResponse로 변환
     */
    private WeeklyScheduleResponse convertToResponse(WeeklySchedule schedule) {
        WeeklyScheduleResponse response = new WeeklyScheduleResponse();
        response.setId(schedule.getId());
        response.setStudentId(schedule.getStudentId());
        response.setActivityId(schedule.getActivityId());
        response.setDayOfWeek(schedule.getDayOfWeek());
        response.setStartTime(schedule.getStartTime());
        response.setEndTime(schedule.getEndTime());

        // 요일 이름 설정
        response.setDayOfWeekName(WeeklySchedule.DayOfWeek.fromValue(schedule.getDayOfWeek()).getKorean());

        // 연관 엔티티 정보 설정
        if (schedule.getStudent() != null) {
            response.setStudentName(schedule.getStudent().getName());
        }
        if (schedule.getActivity() != null) {
            response.setActivityName(schedule.getActivity().getName());
        }

        return response;
    }

    /**
     * WeeklySchedule을 WeeklySchedulePeriodResponse로 변환
     */
    private WeeklySchedulePeriodResponse convertToPeriodResponse(WeeklySchedule schedule, LocalDate date) {
        WeeklySchedulePeriodResponse response = new WeeklySchedulePeriodResponse();
        response.setId(schedule.getId());
        response.setStudentId(schedule.getStudentId());
        response.setActivityId(schedule.getActivityId());
        response.setDayOfWeek(schedule.getDayOfWeek());

        // 실제 날짜와 시간 결합
        response.setActualStartTime(LocalDateTime.of(date, schedule.getStartTime()));
        response.setActualEndTime(LocalDateTime.of(date, schedule.getEndTime()));

        // 요일 이름 설정
        response.setDayOfWeekName(WeeklySchedule.DayOfWeek.fromValue(schedule.getDayOfWeek()).getKorean());

        // 연관 엔티티 정보 설정
        if (schedule.getStudent() != null) {
            response.setStudentName(schedule.getStudent().getName());
        }
        if (schedule.getActivity() != null) {
            response.setActivityName(schedule.getActivity().getName());
        }

        return response;
    }
}
