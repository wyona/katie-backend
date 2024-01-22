package com.wyona.katie.integrations.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Test command
 */
@Component
public class PingCommand implements SlashCommand {
    @Override
    public String getName() {
        return "ping"; // INFO: Also see src/main/resources/discord/commands/ping.json
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        //We reply to the command with "Pong!" and make sure it is ephemeral (only the command user can see it)
        return event.reply()
            .withEphemeral(true)
            .withContent("Pong!");
    }
}
