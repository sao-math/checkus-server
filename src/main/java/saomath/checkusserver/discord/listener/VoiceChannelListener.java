package saomath.checkusserver.discord.listener;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import saomath.checkusserver.discord.entity.VoiceChannelEvent;
import saomath.checkusserver.discord.service.VoiceChannelEventService;

@Slf4j
@Component
public class VoiceChannelListener extends ListenerAdapter {

    private final VoiceChannelEventService voiceChannelEventService;

    public VoiceChannelListener(VoiceChannelEventService voiceChannelEventService) {
        this.voiceChannelEventService = voiceChannelEventService;
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        Member member = event.getMember();
        AudioChannelUnion channelLeft = event.getChannelLeft();
        AudioChannelUnion channelJoined = event.getChannelJoined();

        // 사용자가 음성채널에 들어간 경우
        if (channelLeft == null && channelJoined != null) {
            handleVoiceChannelJoin(member, channelJoined);
        }
        // 사용자가 음성채널에서 나간 경우
        else if (channelLeft != null && channelJoined == null) {
            handleVoiceChannelLeave(member, channelLeft);
        }
        // 사용자가 음성채널을 이동한 경우
        else if (channelLeft != null && channelJoined != null) {
            handleVoiceChannelMove(member, channelLeft, channelJoined);
        }
    }

    /**
     * 사용자가 음성채널에 입장했을 때 처리
     */
    private void handleVoiceChannelJoin(Member member, AudioChannelUnion channel) {
        log.info("사용자 음성채널 입장 - 사용자: {} ({}), 채널: {} ({})", 
                member.getEffectiveName(), 
                member.getId(), 
                channel.getName(), 
                channel.getId());
        
        VoiceChannelEvent event = VoiceChannelEvent.builder()
                .userId(member.getId())
                .username(member.getUser().getName())
                .displayName(member.getEffectiveName())
                .guildId(member.getGuild().getId())
                .guildName(member.getGuild().getName())
                .channelId(channel.getId())
                .channelName(channel.getName())
                .eventType(VoiceChannelEvent.EventType.JOIN)
                .currentChannelMembers(channel.getMembers().size())
                .withCurrentTimestamp()
                .build();
        
        voiceChannelEventService.processVoiceChannelEvent(event);
    }

    /**
     * 사용자가 음성채널에서 퇴장했을 때 처리
     */
    private void handleVoiceChannelLeave(Member member, AudioChannelUnion channel) {
        log.info("사용자 음성채널 퇴장 - 사용자: {} ({}), 채널: {} ({})", 
                member.getEffectiveName(), 
                member.getId(), 
                channel.getName(), 
                channel.getId());
        
        VoiceChannelEvent event = VoiceChannelEvent.builder()
                .userId(member.getId())
                .username(member.getUser().getName())
                .displayName(member.getEffectiveName())
                .guildId(member.getGuild().getId())
                .guildName(member.getGuild().getName())
                .channelId(channel.getId())
                .channelName(channel.getName())
                .eventType(VoiceChannelEvent.EventType.LEAVE)
                .currentChannelMembers(channel.getMembers().size())
                .withCurrentTimestamp()
                .build();
        
        voiceChannelEventService.processVoiceChannelEvent(event);
    }

    /**
     * 사용자가 음성채널을 이동했을 때 처리
     */
    private void handleVoiceChannelMove(Member member, AudioChannelUnion channelLeft, AudioChannelUnion channelJoined) {
        log.info("사용자 음성채널 이동 - 사용자: {} ({}), 이전 채널: {} ({}), 새 채널: {} ({})", 
                member.getEffectiveName(), 
                member.getId(), 
                channelLeft.getName(), 
                channelLeft.getId(),
                channelJoined.getName(), 
                channelJoined.getId());
        
        VoiceChannelEvent event = VoiceChannelEvent.builder()
                .userId(member.getId())
                .username(member.getUser().getName())
                .displayName(member.getEffectiveName())
                .guildId(member.getGuild().getId())
                .guildName(member.getGuild().getName())
                .channelId(channelJoined.getId())
                .channelName(channelJoined.getName())
                .previousChannelId(channelLeft.getId())
                .previousChannelName(channelLeft.getName())
                .eventType(VoiceChannelEvent.EventType.MOVE)
                .currentChannelMembers(channelJoined.getMembers().size())
                .withCurrentTimestamp()
                .build();
        
        voiceChannelEventService.processVoiceChannelEvent(event);
    }

    /**
     * 특정 음성채널의 현재 멤버 목록을 가져오는 헬퍼 메소드
     */
    private void logCurrentChannelMembers(AudioChannelUnion channel) {
        if (channel != null) {
            log.info("채널 '{}' 현재 멤버 수: {}", channel.getName(), channel.getMembers().size());
            channel.getMembers().forEach(member -> 
                log.debug("  - {}", member.getEffectiveName())
            );
        }
    }
}