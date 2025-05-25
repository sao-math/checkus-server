package saomath.checkusserver.discord.listener;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // 봇 메시지 무시
        if (event.getAuthor().isBot()) {
            return;
        }

        String content = event.getMessage().getContentRaw();
        
        // 간단한 핑-퐁 응답
        if (content.equalsIgnoreCase("ping")) {
            log.info("Received ping from {}", event.getAuthor().getAsTag());
            event.getChannel().sendMessage("Pong!").queue();
        }
    }
}