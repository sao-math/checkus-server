package saomath.checkusserver.discord.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import saomath.checkusserver.discord.entity.VoiceChannelEvent;
import saomath.checkusserver.studyTime.domain.AssignedStudyTime;
import saomath.checkusserver.studyTime.domain.ActualStudyTime;
import saomath.checkusserver.auth.domain.User;
import saomath.checkusserver.notification.event.StudyAttendanceEvent;
import saomath.checkusserver.notification.event.StudyRoomEnterEvent;
import saomath.checkusserver.notification.event.UnknownUserJoinEvent;
import saomath.checkusserver.notification.event.UserDiscordIdChangeEvent;
import saomath.checkusserver.studyTime.repository.AssignedStudyTimeRepository;
import saomath.checkusserver.auth.repository.UserRepository;
import saomath.checkusserver.studyTime.service.StudyTimeService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 음성채널 이벤트를 관리하는 서비스
 * 이벤트 발행을 통해 알림 시스템과 분리
 */
@Slf4j
@Service
public class VoiceChannelEventService {
    
    private final UserRepository userRepository;
    private final AssignedStudyTimeRepository assignedStudyTimeRepository;
    private final StudyTimeService studyTimeService;
    private final ApplicationEventPublisher eventPublisher;
    
    // 현재 음성채널에 있는 사용자들을 추적 (channelId -> Set<userId>)
    private final Map<String, List<String>> currentVoiceChannelMembers = new ConcurrentHashMap<>();
    
    // 최근 이벤트들을 메모리에 저장 (추후 데이터베이스 연동 시 제거 예정)
    private final List<VoiceChannelEvent> recentEvents = new ArrayList<>();
    private static final int MAX_RECENT_EVENTS = 100;

    public VoiceChannelEventService(
            UserRepository userRepository,
            AssignedStudyTimeRepository assignedStudyTimeRepository,
            StudyTimeService studyTimeService,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.assignedStudyTimeRepository = assignedStudyTimeRepository;
        this.studyTimeService = studyTimeService;
        this.eventPublisher = eventPublisher;
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
            
            // 음성채널 입장 이벤트인 경우에만 알 수 없는 사용자 알림 발송
            if (event.getEventType() == VoiceChannelEvent.EventType.JOIN || 
                event.getEventType() == VoiceChannelEvent.EventType.MOVE) {
                publishUnknownUserJoinEvent(event);
            }
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
                    
                    // 스터디룸 입장 이벤트 발행
                    User user = userRepository.findById(studentId).orElseThrow();
                    publishStudyRoomEnterEvent(user, event);
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
                long lateMinutes = nearbyAssignments.get(0).getStartTime()
                        .until(event.getTimestamp(), java.time.temporal.ChronoUnit.MINUTES);
                        
                log.warn("늦은 입장 감지: 사용자={}, 늦은 시간={} 분", 
                        user.getUsername(), lateMinutes);
                
                // 늦은 입장 이벤트 발행
                eventPublisher.publishEvent(new StudyAttendanceEvent(
                        this, StudyAttendanceEvent.EventType.LATE_ARRIVAL, 
                        user, nearbyAssignments.get(0), lateMinutes));
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
                long remainingMinutes = java.time.Duration.between(event.getTimestamp(), 
                        currentAssignments.get(0).getEndTime()).toMinutes();
                        
                log.warn("조기 퇴장 감지: 사용자={}, 남은 시간={} 분", 
                        user.getUsername(), remainingMinutes);
                
                // 조기 퇴장 이벤트 발행
                eventPublisher.publishEvent(new StudyAttendanceEvent(
                        this, StudyAttendanceEvent.EventType.EARLY_LEAVE, 
                        user, currentAssignments.get(0), remainingMinutes));
            }
        }
    }

    /**
     * 스터디룸 입장 이벤트 발행
     */
    private void publishStudyRoomEnterEvent(User user, VoiceChannelEvent event) {
        StudyRoomEnterEvent enterEvent = StudyRoomEnterEvent.builder()
            .studentId(user.getId())
            .studentName(user.getName())
            .discordId(user.getDiscordId())
            .enterTime(event.getTimestamp())
            .channelName(event.getChannelName())
            .build();
        
        eventPublisher.publishEvent(enterEvent);
        log.debug("스터디룸 입장 이벤트 발행: 학생={}, 채널={}", 
            user.getName(), event.getChannelName());
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

    /**
     * 알 수 없는 사용자 입장 이벤트 발행
     */
    private void publishUnknownUserJoinEvent(VoiceChannelEvent event) {
        try {
            UnknownUserJoinEvent unknownUserEvent = UnknownUserJoinEvent.builder()
                .discordUserId(event.getUserId())
                .discordUsername(event.getUsername())
                .discordDisplayName(event.getDisplayName())
                .guildId(event.getGuildId())
                .guildName(event.getGuildName())
                .channelId(event.getChannelId())
                .channelName(event.getChannelName())
                .joinTime(event.getTimestamp())
                .currentChannelMembers(event.getCurrentChannelMembers())
                .build();
            
            eventPublisher.publishEvent(unknownUserEvent);
            log.info("알 수 없는 사용자 입장 이벤트 발행: 사용자={}, 채널={}", 
                event.getUsername(), event.getChannelName());
        } catch (Exception e) {
            log.error("알 수 없는 사용자 입장 이벤트 발행 중 오류 발생: 사용자={}", 
                event.getUsername(), e);
        }
    }

    /**
     * 새로 등록된 사용자가 현재 음성채널에 있는지 확인하고 공부 시간 기록 시작
     * 사용자 등록 후에 호출되어 이미 음성채널에 있던 사용자의 기록을 시작
     */
    public void checkAndStartRecordingForNewUser(User user) {
        if (user.getDiscordId() == null || user.getDiscordId().isEmpty()) {
            log.debug("사용자 {}는 Discord ID가 없어 음성채널 확인을 건너뜁니다.", user.getUsername());
            return;
        }

        try {
            // 현재 음성채널 멤버 맵에서 해당 사용자가 있는지 확인
            for (Map.Entry<String, List<String>> entry : currentVoiceChannelMembers.entrySet()) {
                String channelId = entry.getKey();
                List<String> members = entry.getValue();
                
                if (members.contains(user.getDiscordId())) {
                    log.info("새로 등록된 사용자 {}가 현재 음성채널 {}에 있음을 발견. 공부 시간 기록 시작", 
                            user.getUsername(), channelId);
                    
                    // 현재 시간으로 공부 시작 기록
                    LocalDateTime now = LocalDateTime.now();
                    ActualStudyTime studyStart = studyTimeService.recordStudyStart(
                            user.getId(), now, "discord");
                    
                    log.info("기존 음성채널 사용자 공부 시작 기록됨: 학생 ID={}, 시작 시간={}", 
                            user.getId(), now);
                    
                    // 스터디룸 입장 이벤트 발행 (채널 정보는 임시로 ID만 사용)
                    publishStudyRoomEnterEventForExistingUser(user, channelId, now);
                    
                    return; // 하나의 채널에만 있을 수 있으므로 찾으면 종료
                }
            }
            
            log.debug("새로 등록된 사용자 {}는 현재 음성채널에 없습니다.", user.getUsername());
            
        } catch (Exception e) {
            log.error("새로 등록된 사용자 {}의 음성채널 확인 중 오류 발생", user.getUsername(), e);
        }
    }

    /**
     * 기존 음성채널 사용자를 위한 스터디룸 입장 이벤트 발행
     */
    private void publishStudyRoomEnterEventForExistingUser(User user, String channelId, LocalDateTime enterTime) {
        try {
            StudyRoomEnterEvent enterEvent = StudyRoomEnterEvent.builder()
                .studentId(user.getId())
                .studentName(user.getName())
                .discordId(user.getDiscordId())
                .enterTime(enterTime)
                .channelName("음성채널-" + channelId) // 임시 채널명 (실제 채널명 조회 어려움)
                .build();
            
            eventPublisher.publishEvent(enterEvent);
            log.debug("기존 음성채널 사용자 입장 이벤트 발행: 학생={}, 채널={}", 
                user.getName(), channelId);
        } catch (Exception e) {
            log.error("기존 음성채널 사용자 입장 이벤트 발행 중 오류 발생: 사용자={}", 
                user.getUsername(), e);
        }
    }

    /**
     * 새로 등록된 사용자 음성채널 확인 (별도 트랜잭션)
     * 메인 트랜잭션이 커밋된 후 실행되어야 하는 경우 사용
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndStartRecordingForNewUserInNewTransaction(Long userId) {
        try {
            // DB에서 사용자 다시 조회 (커밋된 데이터 확인)
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
            
            checkAndStartRecordingForNewUser(user);
            
        } catch (Exception e) {
            log.error("별도 트랜잭션에서 사용자 ID {}의 음성채널 확인 중 오류 발생", userId, e);
        }
    }

    /**
     * Discord ID 변경으로 인한 기존 세션 정리
     * 기존 Discord ID로 진행 중이던 음성채널 상태를 정리하고 세션 종료
     */
    public void handleDiscordIdChangeCleanup(Long userId, String oldDiscordId, String newDiscordId) {
        try {
            log.info("Discord ID 변경 처리: 사용자 ID={}, 기존 ID={}, 새 ID={}", 
                    userId, oldDiscordId, newDiscordId);
            
            // 1. 기존 Discord ID가 있던 채널에서 제거 및 가상 LEAVE 이벤트 발행
            if (oldDiscordId != null && !oldDiscordId.trim().isEmpty()) {
                publishVirtualLeaveEventForDiscordIdChange(userId, oldDiscordId);
                
                // 기존 진행 중인 세션이 있다면 종료 (이미 UserRegistrationListener에서 처리되지만 안전장치)
                var endedSessions = studyTimeService.recordStudyEndByStudentId(userId, LocalDateTime.now());
                if (!endedSessions.isEmpty()) {
                    log.info("Discord ID 변경으로 {} 개의 진행 중인 세션을 추가로 종료했습니다.", endedSessions.size());
                }
            }
            
            // 2. 새로운 Discord ID로 현재 음성채널 확인
            if (newDiscordId != null && !newDiscordId.trim().isEmpty()) {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
                checkAndStartRecordingForNewUser(user);
            }
            
        } catch (Exception e) {
            log.error("Discord ID 변경 처리 중 오류 발생: 사용자 ID={}", userId, e);
        }
    }

    /**
     * Discord ID 변경 시 가상 LEAVE 이벤트 발행
     * 기존 Discord ID로 진행 중이던 세션에 대한 퇴장 알림을 위해
     * 
     * TODO: 향후 개선 사항
     * - 현재는 Discord ID 변경 시 가상 LEAVE 이벤트와 ID 변경을 함께 처리
     * - 향후에는 다음과 같이 분리하는 것이 좋음:
     *   1) 실제 음성채널 퇴장 이벤트: "김학생이 General 채널에서 나갔습니다"
     *   2) 관리 정보 이벤트: "김학생의 Discord ID가 변경되었습니다 (관리자 작업)"
     * - 이렇게 분리하면:
     *   a) 일반 사용자들은 실제 음성채널 활동만 보고
     *   b) 관리자들은 별도로 ID 변경 정보를 확인할 수 있음
     *   c) 메시지 채널도 분리 가능 (일반 알림 vs 관리자 알림)
     */
    private void publishVirtualLeaveEventForDiscordIdChange(Long userId, String oldDiscordId) {
        try {
            // 기존 Discord ID가 있던 채널 찾기
            String foundChannelId = null;
            String foundChannelName = null;
            
            for (Map.Entry<String, List<String>> entry : currentVoiceChannelMembers.entrySet()) {
                String channelId = entry.getKey();
                List<String> members = entry.getValue();
                
                if (members.contains(oldDiscordId)) {
                    foundChannelId = channelId;
                    foundChannelName = "음성채널-" + channelId; // 임시 채널명
                    break;
                }
            }
            
            if (foundChannelId != null) {
                // 사용자 정보 조회
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    // 가상 LEAVE 이벤트 생성 및 발행
                    VoiceChannelEvent virtualLeaveEvent = VoiceChannelEvent.builder()
                        .eventType(VoiceChannelEvent.EventType.LEAVE)
                        .userId(oldDiscordId)
                        .username(user.getUsername() + " (구 Discord ID)")
                        .displayName(user.getName() + " (구 Discord ID)")
                        .channelId(foundChannelId)
                        .channelName(foundChannelName)
                        .timestamp(LocalDateTime.now())
                        .currentChannelMembers(getCurrentChannelMembers(foundChannelId).size() - 1) // 제거 전 인원수
                        .build();
                    
                    log.info("Discord ID 변경으로 인한 가상 LEAVE 이벤트 발행: 사용자={}, 채널={}", 
                            user.getUsername(), foundChannelName);
                    
                    // 실제 이벤트 처리 (알림 발송 등)
                    recordActualStudyTime(userId, virtualLeaveEvent);
                    
                    // StudyRoom 퇴장 이벤트도 발행할 수 있음 (필요시)
                    // publishStudyRoomLeaveEvent(user, virtualLeaveEvent);
                    
                    // Discord ID 변경 알림 이벤트 발행
                    //TODO 지금은 필요 x
                    //publishDiscordIdChangeEvent(user, oldDiscordId, foundChannelId, foundChannelName);
                }
                
                // 채널에서 사용자 제거
                removeUserFromAllChannels(oldDiscordId);
                
            } else {
                log.debug("기존 Discord ID {}는 어떤 음성채널에도 없었습니다.", oldDiscordId);
            }
            
        } catch (Exception e) {
            log.error("Discord ID 변경 시 가상 LEAVE 이벤트 발행 중 오류 발생: oldDiscordId={}", oldDiscordId, e);
        }
    }

    /**
     * 특정 사용자를 모든 음성채널에서 제거
     * Discord ID 변경 시 기존 ID의 채널 상태 정리용
     */
    private void removeUserFromAllChannels(String discordUserId) {
        try {
            boolean removed = false;
            
            for (Map.Entry<String, List<String>> entry : currentVoiceChannelMembers.entrySet()) {
                String channelId = entry.getKey();
                List<String> members = entry.getValue();
                
                if (members.remove(discordUserId)) {
                    removed = true;
                    log.info("Discord ID {} 사용자를 채널 {}에서 제거했습니다.", discordUserId, channelId);
                    
                    // 채널이 비어있으면 채널 자체를 제거
                    if (members.isEmpty()) {
                        currentVoiceChannelMembers.remove(channelId);
                        log.debug("빈 채널 {} 제거됨", channelId);
                    }
                }
            }
            
            if (!removed) {
                log.debug("Discord ID {}는 어떤 음성채널에도 없었습니다.", discordUserId);
            }
            
        } catch (Exception e) {
            log.error("Discord ID {}를 채널에서 제거하는 중 오류 발생", discordUserId, e);
        }
    }

    /**
     * Discord ID 변경 알림 이벤트 발행
     */
    private void publishDiscordIdChangeEvent(User user, String oldDiscordId, String foundChannelId, String foundChannelName) {
        try {
            // 변경 타입 결정
            UserDiscordIdChangeEvent.ChangeType changeType;
            if (oldDiscordId == null || oldDiscordId.trim().isEmpty()) {
                changeType = UserDiscordIdChangeEvent.ChangeType.ADDED;
            } else if (user.getDiscordId() == null || user.getDiscordId().trim().isEmpty()) {
                changeType = UserDiscordIdChangeEvent.ChangeType.REMOVED;
            } else {
                changeType = UserDiscordIdChangeEvent.ChangeType.CHANGED;
            }
            
            UserDiscordIdChangeEvent discordIdChangeEvent = UserDiscordIdChangeEvent.builder()
                .user(user)
                .oldDiscordId(oldDiscordId)
                .newDiscordId(user.getDiscordId())
                .channelId(foundChannelId)
                .channelName(foundChannelName)
                .changeTime(LocalDateTime.now())
                .changeType(changeType)
                .build();
            
            eventPublisher.publishEvent(discordIdChangeEvent);
            log.info("Discord ID 변경 알림 이벤트 발행: 사용자={}, 변경 타입={}, 채널={}", 
                    user.getUsername(), changeType, foundChannelName);
        } catch (Exception e) {
            log.error("Discord ID 변경 알림 이벤트 발행 중 오류 발생: 사용자={}", 
                    user.getUsername(), e);
        }
    }
}