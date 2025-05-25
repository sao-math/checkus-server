package saomath.checkusserver.discord.command;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.springframework.stereotype.Component;
import saomath.checkusserver.discord.config.DiscordProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class CommandManager {

    private final Map<String, SlashCommand> commands = new HashMap<>();
    private final DiscordProperties discordProperties;

    public CommandManager(List<SlashCommand> slashCommands, DiscordProperties discordProperties) {
        this.discordProperties = discordProperties;
        
        for (SlashCommand command : slashCommands) {
            registerCommand(command);
        }
        log.info("Registered {} slash commands", commands.size());
    }

    private void registerCommand(SlashCommand command) {
        commands.put(command.getName(), command);
        log.info("Registered command: {}", command.getName());
    }

    public SlashCommand getCommand(String name) {
        return commands.get(name);
    }

    public List<SlashCommand> getAllCommands() {
        return new ArrayList<>(commands.values());
    }

    public CompletableFuture<Void> registerCommandsToDiscord(JDA jda) {
        if (discordProperties.getGuildId() == null || discordProperties.getGuildId().isEmpty()) {
            log.warn("Guild ID is not configured, commands will not be registered");
            return CompletableFuture.completedFuture(null);
        }

        List<CommandData> commandDataList = new ArrayList<>();
        for (SlashCommand command : commands.values()) {
            commandDataList.add(command.getCommandData());
        }

        CommandListUpdateAction updateAction = jda.getGuildById(discordProperties.getGuildId())
                .updateCommands();
        
        if (!commandDataList.isEmpty()) {
            updateAction = updateAction.addCommands(commandDataList);
        }

        return updateAction.submit()
                .thenAccept(cmds -> log.info("Successfully registered {} commands to Discord", cmds.size()));
    }
}