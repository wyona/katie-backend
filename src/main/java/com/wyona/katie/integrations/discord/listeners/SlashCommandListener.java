package com.wyona.katie.integrations.discord.listeners;

import com.wyona.katie.integrations.discord.commands.SlashCommand;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class SlashCommandListener {

    private final Collection<SlashCommand> commands;

    public SlashCommandListener(List<SlashCommand> slashCommands, Optional<GatewayDiscordClient> client) {
        log.info("Init Discord slash command listener ...");
        commands = slashCommands;

        if (!client.isEmpty()) {
            client.get().on(ChatInputInteractionEvent.class, this::handle).subscribe();
        } else {
            log.warn("No GatewayDiscordClient available.");
        }
    }

    /**
     * Handle Slash command, e.g. "/ask" or "/ping"
     */
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        log.info("Handle Discord slash command event '" + event.getCommandName() + "' ...");
        //Convert our list to a flux that we can iterate through
        return Flux.fromIterable(commands)
            //Filter out all commands that don't match the name this event is for
            .filter(command -> command.getName().equals(event.getCommandName()))
            //Get the first (and only) item in the flux that matches our filter
            .next()
            //Have our command class handle all logic related to its specific command.
            .flatMap(command -> command.handle(event));
    }
}
