package saomath.checkusserver.discord.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import saomath.checkusserver.discord.entity.VoiceChannelEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 음성채널 이벤트를 관리하는 서비스
 */
@Slf4j
@Service
public class VoiceChannelEventService {
    
    // 현재 음성채널에 있는 사용자들을 추적 (channelId -> Set<userId>)
    private final Map<String, List<String>> currentVoiceChannelMembers = new ConcurrentHashMap<>();
    
    // 최근 이벤트들을 메모리에 저장 (추후 데이터베이스 연동 시 제거 예정)
    private final List<VoiceChannelEvent> recentEvents = new ArrayList<>();
    private static final int MAX_RECENT_EVENTS = 100;

    /**
     * 음성채널 이벤트를 처리하고 저장
     */
    public void processVoiceChannelEvent(VoiceChannelEvent event) {
        log.info("음성채널 이벤트 처리: {}", event);
        
        // 현재 채널 멤버 상태 업데이트
        updateChannelMemberState(event);
        
        // 이벤트 저장 (현재는 메모리에만, 추후 DB 저장)
        saveEvent(event);
        
        // 추후 데이터베이스 비교 로직이 들어갈 부분
        // TODO: 데이터베이스에서 해당 시간대에 음성채널에 있어야 할 학생 목록과 비교
        checkAttendanceCompliance(event);
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
     * 출석 규정 준수 여부 확인 (추후 구현 예정)
     */
    private void checkAttendanceCompliance(VoiceChannelEvent event) {
        // TODO: 데이터베이스에서 현재 시간대에 해당 음성채널에 있어야 할 학생 목록 조회
        // TODO: 현재 채널에 있는 학생들과 비교
        // TODO: 미출석자 또는 잘못된 채널 참여자 감지
        
        log.debug("출석 규정 준수 여부 확인 - 사용자: {}, 채널: {}", 
                event.getUsername(), event.getChannelName());
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