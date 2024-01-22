package com.wyona.katie.integrations.discord.commands;

import com.wyona.katie.integrations.discord.listeners.DiscordNewMessageListener;
import com.wyona.katie.models.ChannelAction;
import com.wyona.katie.services.Utils;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Invite Katie to channel
 */
@Component
public class InviteCommand implements SlashCommand {

    @Autowired
    private ResourceBundleMessageSource messageSource;

    @Override
    public String getName() {
        return "invite"; // INFO: Also see src/main/resources/discord/commands/invite.json
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String guildId = event.getInteraction().getGuildId().get().asString();
        String channelId = event.getInteraction().getChannelId().asString();

        // TODO: Add buttons
        ActionRow buttons = getButtons(guildId, channelId, "en");
        //client.get().getChannelById(Snowflake.of(channelId)).ofType(MessageChannel.class).flatMap(channel -> channel.createMessage(msg).withMessageReference(Snowflake.of(msgId)).withComponents(buttons)).subscribe();

        String msg = messageSource.getMessage("hi.my.name.is.katie.ask.create.connect.domain", null, new Locale("en")) + " (Guild Id: " + guildId + ", Channel Id: " + channelId + ")";
        // We reply to the invite command and make sure it is ephemeral (only the command user can see it)
        return event.reply()
            .withEphemeral(true)
            .withContent(msg);
    }

    /**
     *
     */
    private ActionRow getButtons(String guildId, String channelId, String language) {
        String createNewDomain = messageSource.getMessage("create.new.domain", null, Utils.getLocale(language)) + " ...";
        String connectWithExistingDomain = messageSource.getMessage("connect.with.existing.domain", null, Utils.getLocale(language)) + " ...";

        Button createNewDomainButton = Button.primary(ChannelAction.CREATE_DOMAIN.toString() + DiscordNewMessageListener.DELIMITER + guildId + DiscordNewMessageListener.DELIMITER + channelId, createNewDomain);
        Button connectWithExistingDomainButton = Button.primary(ChannelAction.CONNECT_DOMAIN.toString() + DiscordNewMessageListener.DELIMITER + guildId + DiscordNewMessageListener.DELIMITER + channelId, connectWithExistingDomain);

        return ActionRow.of(createNewDomainButton, connectWithExistingDomainButton);
    }
}
