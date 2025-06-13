package saomath.checkusserver.discord.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import saomath.checkusserver.discord.entity.VoiceChannelEvent;
import saomath.checkusserver.entity.AssignedStudyTime;
import saomath.checkusserver.entity.ActualStudyTime;
import saomath.checkusserver.entity.User;
import saomath.checkusserver.repository.AssignedStudyTimeRepository;
import saomath.checkusserver.repository.UserRepository;
import saomath.checkusserver.service.StudyTimeService;
import saomath.checkusserver.service.NotificationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 음성채널 이벤트를 관리하는 서비스
 */
@Slf4j
@Service
public class VoiceChannelEventService {
    
    private final UserRepository userRepository;
    private final AssignedStudyTimeRepository assignedStudyTimeRepository;
    private final StudyTimeService studyTimeService;
    private final NotificationService notificationService;
    
    // 현재 음성채널에 있는 사용자들을 추적 (channelId -> Set<userId>)
    private final Map<String, List<String>> currentVoiceChannelMembers = new ConcurrentHashMap<>();
    
    // 최근 이벤트들을 메모리에 저장 (추후 데이터베이스 연동 시 제거 예정)
    private final List<VoiceChannelEvent> recentEvents = new ArrayList<>();
    private static final int MAX_RECENT_EVENTS = 100;

    public VoiceChannelEventService(
            UserRepository userRepository,
            AssignedStudyTimeRepository assignedStudyTimeRepository,
            StudyTimeService studyTimeService,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.assignedStudyTimeRepository = assignedStudyTimeRepository;
        this.studyTimeService = studyTimeService;
        this.notificationService = notificationService;
    }

    /**
     * 음성채널 이벤트를 처리하고 저장
     */
    public void processVoiceChannelEvent(VoiceChannelEvent event) {
        log.info("음성채널 이벤트 처리: {}", event);
        
        // 현재 채널 멤버 상태 업데이트
        updateChannelMemberState(event);
        
        // 이벤트 저장 (현재는 메모리에만, 추후 DB 저장)
        saveEvent(event);
        
        // 디스코드 사용자를 시스템 사용자로 연동
        Optional<User> userOpt = userRepository.findByDiscordId(event.getUserId());
        if (userOpt.isEmpty()) {
            log.warn("디스코드 ID {}에 해당하는 시스템 사용자를 찾을 수 없습니다.", event.getUserId());
            return;
        }
        
        User user = userOpt.get();
        
        // 실제 공부 시간 기록
        recordActualStudyTime(user.getId(), event);
        
        // 출석 규정 준수 여부 확인
        checkAttendanceCompliance(user, event);
    }

    /**
     * 실제 공부 시간 기록
     */
    private void recordActualStudyTime(Long studentId, VoiceChannelEvent event) {
        try {
            switch (event.getEventType()) {
                case JOIN:
                case MOVE:
                    // 입장 또는 이동 시 공부 시작 기록
                    ActualStudyTime studyStart = studyTimeService.recordStudyStart(
                            studentId, event.getTimestamp(), "discord");
                    log.info("공부 시작 기록됨: 학생 ID={}, 시작 시간={}", 
                            studentId, event.getTimestamp());
                    break;
                    
                case LEAVE:
                    // 퇴장 시 진행 중인 공부 시간 종료
                    List<ActualStudyTime> endedSessions = studyTimeService.recordStudyEndByStudentId(
                            studentId, event.getTimestamp());
                    log.info("공부 종료 기록됨: 학생 ID={}, 종료된 세션 수={}, 종료 시간={}", 
                            studentId, endedSessions.size(), event.getTimestamp());
                    break;
            }
        } catch (Exception e) {
            log.error("공부 시간 기록 중 오류 발생: 학생 ID={}, 이벤트={}", 
                    studentId, event.getEventType(), e);
        }
    }

    /**
     * 출석 규정 준수 여부 확인
     */
    private void checkAttendanceCompliance(User user, VoiceChannelEvent event) {
        try {
            LocalDateTime eventTime = event.getTimestamp();
            Long studentId = user.getId();
            
            // 현재 시간에 할당된 공부 시간 조회
            List<AssignedStudyTime> currentAssignments = assignedStudyTimeRepository
                    .findCurrentStudyTimes(studentId, eventTime);
            
            // 입장 30분 전부터 입장 30분 후까지의 할당된 공부 시간도 확인
            List<AssignedStudyTime> nearbyAssignments = assignedStudyTimeRepository
                    .findOverlappingStudyTimes(studentId, 
                            eventTime.minusMinutes(30), eventTime.plusMinutes(30));
            
            switch (event.getEventType()) {
                case JOIN:
                case MOVE:
                    handleJoinCompliance(user, event, currentAssignments, nearbyAssignments);
                    break;
                    
                case LEAVE:
                    handleLeaveCompliance(user, event, currentAssignments);
                    break;
            }
        } catch (Exception e) {
            log.error("출석 확인 중 오류 발생: 사용자={}, 이벤트={}", 
                    user.getUsername(), event.getEventType(), e);
        }
    }
    
    /**
     * 입장 시 출석 확인
     */
    private void handleJoinCompliance(User user, VoiceChannelEvent event, 
                                    List<AssignedStudyTime> currentAssignments,
                                    List<AssignedStudyTime> nearbyAssignments) {
        
        if (currentAssignments.isEmpty() && nearbyAssignments.isEmpty()) {
            log.info("할당된 공부 시간 외 접속: 사용자={}, 시간={}", 
                    user.getUsername(), event.getTimestamp());
            return;
        }
        
        // 정시 입장 체크
        boolean isOnTime = currentAssignments.stream()
                .anyMatch(ast -> event.getTimestamp().isAfter(ast.getStartTime()) && 
                               event.getTimestamp().isBefore(ast.getStartTime().plusMinutes(5)));
        
        if (isOnTime) {
            log.info("정시 입장 확인: 사용자={}, 채널={}", 
                    user.getUsername(), event.getChannelName());
        } else {
            // 늦은 입장 체크
            boolean isLate = nearbyAssignments.stream()
                    .anyMatch(ast -> event.getTimestamp().isAfter(ast.getStartTime().plusMinutes(5)));
            
            if (isLate) {
                log.warn("늦은 입장 감지: 사용자={}, 늦은 시간={} 분", 
                        user.getUsername(), 
                        nearbyAssignments.get(0).getStartTime().until(event.getTimestamp(), 
                                java.time.temporal.ChronoUnit.MINUTES));
                // 늦은 입장 알림 발송
                long lateMinutes = nearbyAssignments.get(0).getStartTime()
                        .until(event.getTimestamp(), java.time.temporal.ChronoUnit.MINUTES);
                notificationService.sendLateArrivalNotification(user, nearbyAssignments.get(0), lateMinutes);
            }
        }
    }
    
    /**
     * 퇴장 시 출석 확인
     */
    private void handleLeaveCompliance(User user, VoiceChannelEvent event, 
                                     List<AssignedStudyTime> currentAssignments) {
        
        if (!currentAssignments.isEmpty()) {
            // 조기 퇴장 감지
            boolean isEarlyLeave = currentAssignments.stream()
                    .anyMatch(ast -> event.getTimestamp().isBefore(ast.getEndTime().minusMinutes(5)));
            
            if (isEarlyLeave) {
                log.warn("조기 퇴장 감지: 사용자={}, 남은 시간={} 분", 
                        user.getUsername(),
                        java.time.Duration.between(event.getTimestamp(), 
                                currentAssignments.get(0).getEndTime()).toMinutes());
                // 조기 퇴장 알림 발송
                long remainingMinutes = java.time.Duration.between(event.getTimestamp(), 
                        currentAssignments.get(0).getEndTime()).toMinutes();
                notificationService.sendEarlyLeaveNotification(user, currentAssignments.get(0), remainingMinutes);
            }
        }
    }

    /**
     * 현재 음성채널 멤버 상태 업데이트
     */
    private void updateChannelMemberState(VoiceChannelEvent event) {
        String channelId = event.getChannelId();
        String userId = event.getUserId();
        
        switch (event.getEventType()) {
            case JOIN:
                currentVoiceChannelMembers.computeIfAbsent(channelId, k -> new ArrayList<>()).add(userId);
                break;
                
            case LEAVE:
                List<String> members = currentVoiceChannelMembers.get(channelId);
                if (members != null) {
                    members.remove(userId);
                    if (members.isEmpty()) {
                        currentVoiceChannelMembers.remove(channelId);
                    }
                }
                break;
                
            case MOVE:
                // 이전 채널에서 제거
                if (event.getPreviousChannelId() != null) {
                    List<String> prevMembers = currentVoiceChannelMembers.get(event.getPreviousChannelId());
                    if (prevMembers != null) {
                        prevMembers.remove(userId);
                        if (prevMembers.isEmpty()) {
                            currentVoiceChannelMembers.remove(event.getPreviousChannelId());
                        }
                    }
                }
                // 새 채널에 추가
                currentVoiceChannelMembers.computeIfAbsent(channelId, k -> new ArrayList<>()).add(userId);
                break;
        }
        
        log.debug("채널 {} 현재 멤버 수: {}", 
                event.getChannelName(), 
                currentVoiceChannelMembers.getOrDefault(channelId, new ArrayList<>()).size());
    }

    /**
     * 이벤트를 저장
     */
    private void saveEvent(VoiceChannelEvent event) {
        recentEvents.add(event);
        
        // 최대 개수 초과 시 오래된 이벤트 제거
        if (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.remove(0);
        }
    }

    /**
     * 특정 채널의 현재 멤버 목록 조회
     */
    public List<String> getCurrentChannelMembers(String channelId) {
        return new ArrayList<>(currentVoiceChannelMembers.getOrDefault(channelId, new ArrayList<>()));
    }

    /**
     * 최근 이벤트 목록 조회
     */
    public List<VoiceChannelEvent> getRecentEvents() {
        return new ArrayList<>(recentEvents);
    }

    /**
     * 현재 모든 음성채널의 상태 조회
     */
    public Map<String, List<String>> getAllChannelMembers() {
        return new ConcurrentHashMap<>(currentVoiceChannelMembers);
    }
}