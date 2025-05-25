package saomath.checkusserver.discord.command.impl;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;
import saomath.checkusserver.discord.command.SlashCommand;

@Slf4j
@Component
public class PingCommand implements SlashCommand {

    @Override
    public CommandData getCommandData() {
        return Commands.slash("ping", "Replies with Pong and latency information");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        long startTime = System.currentTimeMillis();
        
        event.reply("Pinging...").queue(response -> {
            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            long apiLatency = event.getJDA().getGatewayPing();
            
            response.editOriginal(String.format(
                "Pong! Latency: %dms. API Latency: %dms",
                latency, apiLatency
            )).queue();
            
            log.info("Ping command executed. Latency: {}ms, API Latency: {}ms", 
                    latency, apiLatency);
        });
    }
}