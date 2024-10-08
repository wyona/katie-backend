package com.wyona.katie.integrations.discord.commands;

import com.wyona.katie.integrations.discord.listeners.DiscordNewMessageListener;
import com.wyona.katie.integrations.discord.services.DiscordDomainService;
import com.wyona.katie.models.ChannelAction;
import com.wyona.katie.models.discord.DiscordDomainMapping;
import com.wyona.katie.services.Utils;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Invite Katie to channel
 */
@Slf4j
@Component
public class InviteCommand implements SlashCommand {

    @Autowired
    private ResourceBundleMessageSource messageSource;

    @Autowired
    private DiscordDomainService discordDomainService;

    @Value("${new.context.mail.body.host}")
    private String katieHost;

    @Override
    public String getName() {
        return "invite"; // INFO: Also see src/main/resources/discord/commands/invite.json
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String guildId = event.getInteraction().getGuildId().get().asString();
        String channelId = event.getInteraction().getChannelId().asString();

        String domainId = alreadyConnectedWithADomain(guildId, channelId);
        if (domainId != null) {
            // TODO: Use messageSource / i18n
            return event.reply()
                    .withEphemeral(true)
                    .withContent("Channel " + channelId + " (Guild Id: " + guildId + ") is already connected with domain " + katieHost + "/#/domain/" + domainId);
        }

        // TODO: Add buttons
        ActionRow buttons = getButtons(guildId, channelId, "en");
        //client.get().getChannelById(Snowflake.of(channelId)).ofType(MessageChannel.class).flatMap(channel -> channel.createMessage(msg).withMessageReference(Snowflake.of(msgId)).withComponents(buttons)).subscribe();

        String[] args = new String[2];
        args[0] = guildId;
        args[1] = channelId;
        String msg = messageSource.getMessage("hi.my.name.is.katie.ask.create.connect.domain", args, new Locale("en"));
        // We reply to the invite command and make sure it is ephemeral (only the command user can see it)
        return event.reply()
            .withEphemeral(true)
            .withContent(msg);
    }

    /**
     * Check whether channel already connected with Katie domain
     * @return domain Id when already connected and null otherwise
     */
    private String alreadyConnectedWithADomain(String guildId, String channelId) {
        try {
            log.info("Check whether channel already connected with Katie domain");
            DiscordDomainMapping[] mappings = discordDomainService.getAllDomains(false);
            for (DiscordDomainMapping mapping : mappings) {
                if (mapping.getGuildId().equals(guildId) && mapping.getChannelId().equals(channelId)) {
                    log.warn("Discord channel '" + guildId + " / " + channelId+ "' already connected with Katie domain '" + mapping.getDomainId() + "'.");
                    return mapping.getDomainId();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
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
