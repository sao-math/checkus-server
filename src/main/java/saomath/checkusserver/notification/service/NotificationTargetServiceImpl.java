package saomath.checkusserver.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.user.domain.StudentGuardian;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.repository.ActualStudyTimeRepository;
import saomath.checkusserver.repository.AssignedStudyTimeRepository;
import saomath.checkusserver.user.repository.StudentGuardianRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationTargetServiceImpl implements NotificationTargetService {
    
    private final AssignedStudyTimeRepository assignedStudyTimeRepository;
    private final ActualStudyTimeRepository actualStudyTimeRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    
    @Override
    public List<StudyTarget> getStudyTargetsForTime(LocalDateTime targetTime) {
        // 정확히 해당 분(minute)의 공부 일정만 조회 (초/나노초는 0으로 정규화)
        LocalDateTime exactMinute = targetTime.withSecond(0).withNano(0);
        List<AssignedStudyTime> studyTimes = assignedStudyTimeRepository
            .findByStartTimeWithDetails(exactMinute);
        
        log.debug("공부 일정 대상자 조회 - 대상시간: {}, 조회결과: {}건", exactMinute, studyTimes.size());
        
        return studyTimes.stream()
            .map(this::convertToStudyTarget)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TaskTarget> getTodayTaskTargets() {
        // TODO: AssignedTask 엔티티가 구현되면 작성
        // 임시로 빈 리스트 반환
        return new ArrayList<>();
    }
    
    @Override
    public List<TaskTarget> getYesterdayIncompleteTaskTargets() {
        // TODO: AssignedTask 엔티티가 구현되면 작성
        // 임시로 빈 리스트 반환
        return new ArrayList<>();
    }
    
    @Override
    public List<NoShowTarget> getNoShowTargets(LocalDateTime startTime) {
        // 시작 시간 기준으로 공부 일정 조회 (정확한 시간 매칭)
        LocalDateTime exactMinute = startTime.withSecond(0).withNano(0);
        List<AssignedStudyTime> studyTimes = assignedStudyTimeRepository
            .findByStartTimeWithDetails(exactMinute);
        
        List<NoShowTarget> noShowTargets = new ArrayList<>();
        
        for (AssignedStudyTime studyTime : studyTimes) {
            // 실제 공부 시간 기록이 있는지 확인
            boolean hasAttended = actualStudyTimeRepository
                .existsByAssignedStudyTimeId(studyTime.getId());
            
            if (!hasAttended) {
                noShowTargets.add(convertToNoShowTarget(studyTime));
            }
        }
        
        return noShowTargets;
    }
    
    private StudyTarget convertToStudyTarget(AssignedStudyTime assignedStudyTime) {
        User student = assignedStudyTime.getStudent();
        String parentPhone = getParentPhone(student.getId());
        
        return StudyTarget.builder()
            .studentId(student.getId())
            .studentName(student.getName())
            .studentPhone(student.getPhoneNumber())
            .parentPhone(parentPhone)
            .activityName(assignedStudyTime.getActivity() != null ? 
                assignedStudyTime.getActivity().getName() : "공부")
            .startTime(assignedStudyTime.getStartTime())
            .endTime(assignedStudyTime.getEndTime())
            .parentNotificationEnabled(isNotificationEnabled(student.getId(), "PARENT"))
            .studentNotificationEnabled(isNotificationEnabled(student.getId(), "STUDENT"))
            .build();
    }
    
    private NoShowTarget convertToNoShowTarget(AssignedStudyTime assignedStudyTime) {
        User student = assignedStudyTime.getStudent();
        String parentPhone = getParentPhone(student.getId());
        
        return NoShowTarget.builder()
            .studentId(student.getId())
            .studentName(student.getName())
            .studentPhone(student.getPhoneNumber())
            .parentPhone(parentPhone)
            .startTime(assignedStudyTime.getStartTime())
            .endTime(assignedStudyTime.getEndTime())
            .parentNotificationEnabled(isNotificationEnabled(student.getId(), "PARENT"))
            .studentNotificationEnabled(isNotificationEnabled(student.getId(), "STUDENT"))
            .build();
    }
    
    private String getParentPhone(Long studentId) {
        // 학부모 정보 조회 (첫 번째 보호자의 전화번호 반환)
        List<StudentGuardian> guardians = studentGuardianRepository
            .findByStudentId(studentId);
        
        if (!guardians.isEmpty()) {
            User guardian = guardians.get(0).getGuardian();
            return guardian.getPhoneNumber();
        }
        
        return null;
    }
    
    private boolean isNotificationEnabled(Long userId, String recipientType) {
        // TODO: NotificationSetting 엔티티가 구현되면 실제 설정값 조회
        // 임시로 true 반환
        return true;
    }
}
