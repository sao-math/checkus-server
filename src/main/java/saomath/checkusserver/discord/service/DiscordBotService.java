package saomath.checkusserver.discord.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import saomath.checkusserver.discord.config.DiscordProperties;

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
}