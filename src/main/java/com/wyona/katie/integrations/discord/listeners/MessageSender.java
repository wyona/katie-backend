package com.wyona.katie.integrations.discord.listeners;

import com.wyona.katie.models.ChannelAction;
import com.wyona.katie.models.Emoji;
import com.wyona.katie.services.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
//import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
//import discord4j.core.object.entity.Message;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
//import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateMono;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.legacy.LegacyMessageCreateSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class MessageSender {

    @Autowired
    private ResourceBundleMessageSource messageSource;

    private final Optional<GatewayDiscordClient> client;

    public MessageSender(Optional<GatewayDiscordClient> client) {
        log.info("Init Discord message sender ...");

        this.client = client;
    }

    /**
     * Send message to a particular Discord channel
     * @param questionUuid TODO
     * @param answer Answer to question
     * @param channelId Discord channel Id
     * @param msgId Discord message Id of question
     */
    public void sendAnswer(String questionUuid, String answer, String channelId, String msgId) {
        String language = "en"; // TODO
        log.warn("TODO: Send reply as thread message (Msg Id: " + msgId + ") ...");
        log.info("Send message to Discord channel '" + channelId + "' ...");
        //client.rest().getChannelById(Snowflake.of(channelId)).createMessage(message).subscribe();
        //client.rest().getMessageById(Snowflake.of(channelId),Snowflake.of(msgId)).createReaction("\u2B06").subscribe();

        final String answerAsMarkdown = Utils.convertToMarkdown(answer);
        if (!client.isEmpty()) {
            client.get().getChannelById(Snowflake.of(channelId)).ofType(MessageChannel.class).flatMap(channel -> channel.createMessage(answerAsMarkdown).withMessageReference(Snowflake.of(msgId)).withComponents(getButtons(questionUuid, language))).subscribe();
        } else {
            log.warn("No GatewayDiscordClient available.");
        }
    }

    /**
     * @param answer Answer as markdown
     */
    protected LegacyMessageCreateSpec createAnswer(LegacyMessageCreateSpec msg, String answer, String questionUuid) {
        String language = "en"; // TODO
        msg.setContent(answer);
        msg.setComponents(getButtons(questionUuid, language));

        return msg;
    }

    /**
     *
     */
    private  ActionRow getButtons(String questionUuid, String language) {
        String correct = messageSource.getMessage("correct.answer", null, Utils.getLocale(language));
        String wrong = messageSource.getMessage("wrong.answer", null, Utils.getLocale(language));
        String provideBetterAnswer = messageSource.getMessage("provide.better.answer", null, Utils.getLocale(language));
        String seeMoreAnswers = messageSource.getMessage("see.more.answers", null, Utils.getLocale(language)) + " ...";
        String moreInfo = messageSource.getMessage("more.info", null, Utils.getLocale(language)) + " ...";

        Button thumbUpButton = Button.success(ChannelAction.THUMB_UP.toString() + DiscordNewMessageListener.DELIMITER + questionUuid, ReactionEmoji.unicode(Emoji.THUMB_UP), correct);
        Button thumbDownButton = Button.danger(ChannelAction.THUMB_DOWN.toString() + DiscordNewMessageListener.DELIMITER + questionUuid, ReactionEmoji.unicode(Emoji.THUMB_DOWN), wrong);
        Button provideBetterAnswerButton = Button.primary(ChannelAction.ENTER_BETTER_ANSWER.toString() + DiscordNewMessageListener.DELIMITER + questionUuid, provideBetterAnswer);
        Button moreAnswersButton = Button.primary(ChannelAction.SEE_MORE_ANSWERS.toString() + DiscordNewMessageListener.DELIMITER + questionUuid, seeMoreAnswers);
        Button moreInfoButton = Button.primary(ChannelAction.MORE_INFO.toString() + DiscordNewMessageListener.DELIMITER + questionUuid, moreInfo);

        return ActionRow.of(thumbUpButton, thumbDownButton, provideBetterAnswerButton, moreAnswersButton, moreInfoButton);
    }
}
