package saomath.checkusserver.discord.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "discord.bot")
public class DiscordProperties {
    private String token;
    private String guildId;
    private boolean enabled;
}