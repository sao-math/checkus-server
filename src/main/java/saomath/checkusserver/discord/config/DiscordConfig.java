package saomath.checkusserver.discord.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "discord.bot.enabled", havingValue = "true")
public class DiscordConfig {

    private final DiscordProperties discordProperties;
    private JDA jda;

    public DiscordConfig(DiscordProperties discordProperties) {
        this.discordProperties = discordProperties;
    }

    @Bean
    public JDA jda(List<ListenerAdapter> eventListeners) throws InterruptedException {
        log.info("Initializing Discord bot");
        JDABuilder builder = JDABuilder.createDefault(discordProperties.getToken())
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT
                );

        // 이벤트 리스너 등록
        for (ListenerAdapter listener : eventListeners) {
            builder.addEventListeners(listener);
            log.info("Registered event listener: {}", listener.getClass().getSimpleName());
        }

        jda = builder.build().awaitReady();
        log.info("Discord bot started as {}", jda.getSelfUser().getAsTag());
        return jda;
    }

    @PreDestroy
    public void shutdown() {
        if (jda != null) {
            log.info("Shutting down Discord bot");
            jda.shutdown();
        }
    }
}