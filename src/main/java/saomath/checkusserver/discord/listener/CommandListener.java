package saomath.checkusserver.discord.listener;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import saomath.checkusserver.discord.command.CommandManager;
import saomath.checkusserver.discord.command.SlashCommand;

@Slf4j
@Component
public class CommandListener extends ListenerAdapter {

    private final CommandManager commandManager;

    public CommandListener(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        log.info("Bot is ready! Registering commands...");
        commandManager.registerCommandsToDiscord(event.getJDA())
                .exceptionally(throwable -> {
                    log.error("Failed to register commands", throwable);
                    return null;
                });
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        SlashCommand command = commandManager.getCommand(commandName);

        if (command == null) {
            log.warn("Unknown command: {}", commandName);
            event.reply("Unknown command").setEphemeral(true).queue();
            return;
        }

        try {
            log.info("Executing command: {} by user: {}", commandName, event.getUser().getAsTag());
            command.execute(event);
        } catch (Exception e) {
            log.error("Error executing command: {}", commandName, e);
            if (event.isAcknowledged()) {
                event.getHook().sendMessage("An error occurred while executing the command").setEphemeral(true).queue();
            } else {
                event.reply("An error occurred while executing the command").setEphemeral(true).queue();
            }
        }
    }
}