package saomath.checkusserver.discord.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

/**
 * Discord 슬래시 명령어 인터페이스
 */
public interface SlashCommand {
    
    /**
     * 명령어 데이터를 반환합니다.
     */
    CommandData getCommandData();
    
    /**
     * 명령어가 실행될 때 호출됩니다.
     */
    void execute(SlashCommandInteractionEvent event);
    
    /**
     * 명령어의 이름을 반환합니다.
     */
    default String getName() {
        return getCommandData().getName();
    }
}