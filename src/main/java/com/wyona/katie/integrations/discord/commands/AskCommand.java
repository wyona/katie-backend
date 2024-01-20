package com.wyona.katie.integrations.discord.commands;

import com.wyona.katie.models.ChannelType;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.ResponseAnswer;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.QuestionAnsweringService;
import com.wyona.katie.services.Utils;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.ResponseAnswer;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class AskCommand implements SlashCommand {

    @Autowired
    private QuestionAnsweringService qaService;

    @Autowired
    private ContextService domainService;

    @Override
    public String getName() {
        return "ask";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        /*
        Since slash command options are optional according to discord, we will wrap it into the following function
        that gets the value of our option as a String without chaining several .get() on all the optional values

        In this case, there is no fear it will return empty/null as this is marked "required: true" in our json.
         */
        String question = event.getOption("question")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .get(); //This is warning us that we didn't check if its present, we can ignore this on required options

        log.info("Reply to the slash command, with the answer to the question the user was asking ...");

        String topAnswer = null;
        Context domain = null;
        try {
            //String guildId = event.getInteraction().getGuildId();
            String channelId = event.getInteraction().getChannelId().asString();
            log.info("TODO: Get Katie domain for discord channel id '" + channelId + "' ...");
            domain = domainService.getContext("ROOT"); // TODO: Get domain from guild_id and channel_Id
            boolean checkAuthorization = false; // TODO
            String messageId = null; // TODO
            String channelRequestId = null; // TODO
            List<ResponseAnswer> answers = qaService.getAnswers(question, null, messageId, domain, new Date(), null, ChannelType.UNDEFINED, channelRequestId, -1, -1, checkAuthorization, null, false);
            if (answers != null && answers.size() > 0) {
                topAnswer = answers.get(0).getAnswer();
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        // TODO: Handle null pointer

        // TODO: Add buttons, whereas see NewMessageListener

        return  event.reply()
            .withEphemeral(true)
            .withContent(Utils.convertToMarkdown(topAnswer)); // INFO: Convert to Markdown https://support.discord.com/hc/en-us/articles/210298617-Markdown-Text-101-Chat-Formatting-Bold-Italic-Underline-
    }
}
