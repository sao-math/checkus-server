package saomath.checkusserver.discord.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import saomath.checkusserver.discord.config.DiscordProperties;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@ConditionalOnProperty(name = "discord.bot.enabled", havingValue = "true")
public class DiscordBotService {

    private final JDA jda;
    private final DiscordProperties discordProperties;

    public DiscordBotService(
            JDA jda, 
            DiscordProperties discordProperties) {
        this.jda = jda;
        this.discordProperties = discordProperties;
    }

    @PostConstruct
    public void initialize() {
        if (discordProperties.isEnabled()) {
            log.info("Discord bot is enabled and initialized");
        } else {
            log.info("Discord bot is disabled");
        }
    }

    public JDA getJda() {
        return jda;
    }

    /**
     * 사용자에게 개인 메시지를 전송합니다.
     * @param userId 디스코드 사용자 ID
     * @param message 전송할 메시지
     * @return 성공 여부
     */
    public CompletableFuture<Boolean> sendDirectMessage(String userId, String message) {
        if (!discordProperties.isEnabled()) {
            log.debug("Discord bot이 비활성화되어 메시지 전송을 건너뜁니다. 사용자 ID: {}", userId);
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 사용자 조회
                User user = jda.getUserById(userId);
                if (user == null) {
                    log.warn("디스코드 사용자를 찾을 수 없습니다. ID: {}", userId);
                    return false;
                }

                // 개인 채널 열기
                PrivateChannel privateChannel = user.openPrivateChannel().complete();
                if (privateChannel == null) {
                    log.warn("개인 채널을 열 수 없습니다. 사용자 ID: {}", userId);
                    return false;
                }

                // 메시지 전송
                privateChannel.sendMessage(message).complete();
                log.debug("디스코드 DM 전송 성공: 사용자={}, 메시지 길이={}", 
                        user.getName(), message.length());
                return true;

            } catch (Exception e) {
                log.error("디스코드 DM 전송 실패: 사용자 ID={}", userId, e);
                return false;
            }
        });
    }

    /**
     * 특정 디스코드 채널에 메시지를 전송합니다.
     * @param channelId 디스코드 채널 ID
     * @param message 전송할 메시지
     * @return 성공 여부
     */
    public CompletableFuture<Boolean> sendChannelMessage(String channelId, String message) {
        if (!discordProperties.isEnabled()) {
            log.debug("Discord bot이 비활성화되어 채널 메시지 전송을 건너뜁니다. 채널 ID: {}", channelId);
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 채널 조회
                TextChannel channel = jda.getTextChannelById(channelId);
                if (channel == null) {
                    log.warn("디스코드 채널을 찾을 수 없습니다. ID: {}", channelId);
                    return false;
                }

                // 메시지 전송
                channel.sendMessage(message).complete();
                log.debug("디스코드 채널 메시지 전송 성공: 채널={}, 메시지 길이={}", 
                        channel.getName(), message.length());
                return true;

            } catch (Exception e) {
                log.error("디스코드 채널 메시지 전송 실패: 채널 ID={}", channelId, e);
                return false;
            }
        });
    }

    /**
     * 설정된 알림 채널에 메시지를 전송합니다.
     * @param message 전송할 메시지
     * @return 성공 여부
     */
    public CompletableFuture<Boolean> sendNotificationMessage(String message) {
        String notificationChannelId = discordProperties.getNotificationChannelId();
        
        if (notificationChannelId == null || notificationChannelId.isEmpty()) {
            log.debug("알림 채널 ID가 설정되지 않아 메시지 전송을 건너뜁니다.");
            return CompletableFuture.completedFuture(false);
        }
        
        return sendChannelMessage(notificationChannelId, message);
    }

    /**
     * 여러 사용자에게 동일한 메시지를 전송합니다.
     * @param userIds 디스코드 사용자 ID 목록
     * @param message 전송할 메시지
     * @return 성공한 전송 수
     */
    public CompletableFuture<Integer> sendBroadcastMessage(Iterable<String> userIds, String message) {
        if (!discordProperties.isEnabled()) {
            log.debug("Discord bot이 비활성화되어 브로드캐스트를 건너뜁니다.");
            return CompletableFuture.completedFuture(0);
        }

        return CompletableFuture.supplyAsync(() -> {
            int successCount = 0;
            for (String userId : userIds) {
                try {
                    boolean success = sendDirectMessage(userId, message).get();
                    if (success) {
                        successCount++;
                    }
                    // 메시지 간 딜레이 (Discord API 제한 고려)
                    Thread.sleep(100);
                } catch (Exception e) {
                    log.error("브로드캐스트 중 오류 발생: 사용자 ID={}", userId, e);
                }
            }
            
            log.info("브로드캐스트 완료: 성공={} 건", successCount);
            return successCount;
        });
    }
}