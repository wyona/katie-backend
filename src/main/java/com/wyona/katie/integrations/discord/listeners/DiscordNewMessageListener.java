package com.wyona.katie.integrations.discord.listeners;

import com.wyona.katie.integrations.CommonMessageSender;
import com.wyona.katie.models.*;
import com.wyona.katie.models.discord.DiscordEvent;
import com.wyona.katie.services.*;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Discord new message listener
 */
@Slf4j
@Component
public class DiscordNewMessageListener extends CommonMessageSender {

    public static final String DELIMITER = ":";

    private final int ANSWER_MAX_LENGTH = 2000;

    @Autowired
    private QuestionAnsweringService qaService;

    @Autowired
    private ContextService domainService;

    @Autowired
    private DataRepositoryService dataRepoService;

    @Autowired
    private MailerService mailerService;

    @Autowired
    private QuestionAnalyzerService questionAnalyzerService;

    @Autowired
    private DataRepositoryService dataRepositoryService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ResourceBundleMessageSource messageSource;

    @Autowired
    private MessageSender messageSender;

    @Autowired
    private AuthenticationService authService;

    @Value("${discord.katie.username}")
    private String usernameTechnicalUser;

    @Value("${slack.number.of.questions.limit}")
    private int numberOfQuestionsLimit;

    public DiscordNewMessageListener(Optional<GatewayDiscordClient> client) {
        log.info("Init Discord new message listener ...");

        if (!client.isEmpty()) {
            client.get().on(MessageCreateEvent.class, this::handle).subscribe();
        } else {
            log.warn("No GatewayDiscordClient available.");
        }
    }

    /**
     * Handle new message
     * @param event
     * @return answer to message
     */
    public Mono<Message> handle(MessageCreateEvent event) {
        log.info("Handle Discord MessageCreateEvent '" + event.getMessage() + "' ...");

        Message message = event.getMessage();

        if (message.getAuthor().map(User::isBot).orElse(true)) {
            if (message.getAuthor().isEmpty()) {
                log.info("Message was sent by bot, therefore ignore in order to prevent loops!");
            } else {
                log.info("Message was sent by bot '" + message.getAuthor() + "', therefore ignore in order to prevent loops!");
            }
            return Mono.empty();
        }

        if (!message.getMessageReference().isEmpty()) {
            log.debug("Event seems to be a 'create thread event', referencing the channel '" + message.getMessageReference() + "' ...");

            String referencedMessageId = message.getMessageReference().get().getMessageId().get().asString();
            String referencedChannelId = message.getMessageReference().get().getChannelId().asString();
            String referencedGuildId = message.getMessageReference().get().getGuildId().get().asString();
            log.info("Event seems to be a 'create thread event', referencing the message '" + referencedMessageId + "' inside the channel: " + referencedGuildId + " / " + referencedChannelId);

            long threadChannelId = message.getChannelId().asLong();
            log.info("Thread channel Id: " + threadChannelId);
            try {
                dataRepositoryService.linkDiscordThreadChannel(referencedMessageId, Long.toString(threadChannelId));
                DiscordEvent discordEvent = dataRepositoryService.getDiscordConversationValues(referencedMessageId);

                String domainId = getDomainId(referencedGuildId, Long.parseLong(referencedChannelId));
                if (domainId != null) {
                    try {
                        log.info("New Discord thread (Domain: " + domainId + ", Referenced Channel: " + referencedChannelId + ", Channel Request Id: " + discordEvent.getChannelRequestId() + ", Thread Channel Id: " + threadChannelId + ").");
                        //domainService.saveThreadMessage(domainService.getContext(domainId), referencedChannelId, discordEvent.getChannelRequestId(), "" + threadChannelId, "TODO: Thread created");
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                } else {
                    log.error("No Katie domain linked with referenced guild Id / channel Id '" + referencedGuildId + "' / '" + referencedChannelId + "'!");
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            return Mono.empty();
        }

        ResponseAnswer topAnswer = null;
        Context domain = null;
        try {
            String guildId = event.getGuildId().get().asString(); // TODO: Catch null pointer of get()
            long channelId = event.getMessage().getChannelId().asLong();
            String domainId = getDomainId(guildId, channelId);
            if (domainId == null) {
                log.info("No Katie domain linked directly with Discord guild id '" + guildId + "' and channel id '" + channelId + "'. Check whether channel Id might be the Id of a thread channel ...");
                DiscordEvent discordEvent = dataRepositoryService.getDiscordConversationValuesForThreadChannelId(Long.toString(channelId));
                if (discordEvent != null) {
                    log.info("Event: " + event);
                    if (discordEvent.getDomainId() != null) {
                        log.info("Save thread message (Author: " + message.getAuthor() + "): " + message.getContent());
                        domainService.saveThreadMessage(domainService.getContext(discordEvent.getDomainId()), discordEvent.getChannelId(), discordEvent.getChannelRequestId(), "" + channelId, message.getContent());
                        return Mono.empty();
                    } else {
                        log.warn("Discord guild / channel '" + guildId + " / " + channelId + "' does not seem to be connected with a Katie domain! Use the '/invite' command to invite Katie into channel ...");
                        return Mono.empty();
                    }
                } else {
                    log.warn("Neither Katie Domain Id available nor does it seem to be a thread message: " + message.getContent());
                    return Mono.empty();
                }
            }

            log.info("Load domain '" + domainId + "' ...");
            domain = domainService.getContext(domainId);

            String discordUserName = message.getAuthor().map(User::getUsername).get();
            String discordUserId = message.getAuthor().map(User::getId).get().asString();
            log.info("Discord User: " + discordUserName + " / " + discordUserId);
            try {
                authService.login(usernameTechnicalUser, null);
                log.info("Signed in as technical Katie user '" + authService.getUserId() + "', whereas Discord user is '" + discordUserName + " / " + discordUserId + "'.");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            if (message.getContent().isEmpty()) {
                log.warn("Discord message is empty!");
            }
            AnalyzedMessage analyzedMessage = questionAnalyzerService.analyze(message.getContent(), domain);
            if (!analyzedMessage.getContainsQuestions()) {
                log.info("No question detected, therefore do not answer.");
                return Mono.empty();
            }

            int count = dataRepoService.getQuestionCountDuringLast24Hours(domain.getId(), ChannelType.DISCORD);
            log.info(count + " questions have been asked for the domain '" + domain.getId() + "' during the last 24 hours by members of the Discord team '" + guildId + "'. The Discord user '" + discordUserId + "' is asking another question ...");
            if (count >= numberOfQuestionsLimit) { // TODO: Get limit from domain configuration
                //String subject = "WARNING: The limit of questions has been reached";
                String msgToAdmin = "WARNING: The limit of " + numberOfQuestionsLimit + " questions during the last 24 hours for the domain '" + domain.getId() + "' and channel type '" + ChannelType.DISCORD + "' has been reached.";
                log.warn(msgToAdmin);
                mailerService.notifyAdministrator(msgToAdmin, null, domain.getMailSenderEmail(), false);
                return Mono.empty();
            }

            analyticsService.logMessageReceived(domainId, ChannelType.DISCORD, "" + channelId);

            String channelRequestId = java.util.UUID.randomUUID().toString();
            String messageId = message.getId().asString();
            dataRepositoryService.addDiscordConversationValues(channelRequestId, domainId, guildId, Long.toString(channelId), messageId);
            String remoteAddress = "TODO:Discord";

            List<String> classifications = new ArrayList<String>();
            if (analyzedMessage.getQuestionsAndContexts().size() > 1) {
                log.info("TODO: Handle multiple questions ...");
            }
            String question = analyzedMessage.getMessage(); // TODO: Query builder
            List<ResponseAnswer> answers = qaService.getAnswers(question, false, classifications, messageId, domain, new Date(), remoteAddress, ChannelType.DISCORD, channelRequestId, -1, -1, true, null, false, false, false);
            if (answers != null && answers.size() > 0) {
                topAnswer = answers.get(0);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return message.getChannel().flatMap(channel -> channel.createMessage(Utils.convertToMarkdown(e.getMessage())));
        }

        if (topAnswer != null) {
            if (domain.getAnswersMustBeApprovedByModerator()) {
                log.info("Answers of domain '" + domain.getId() + "' must be approved by moderator, therefore do not reply directly.");
                if (domain.getInformUserReModeration()) {
                    log.info("Inform user re moderation ...");
                    String[] args = new String[1];
                    args[0] = domain.getName();
                    return message.getChannel().flatMap(channel -> channel.createMessage(Utils.convertToMarkdown(messageSource.getMessage("responses.need.approval", args, new Locale("en")))));
                } else {
                    log.info("Do not inform user re moderation.");
                    return Mono.empty();
                }
            }

            String lang = "en"; // TODO: Get language of user or channel

            String _answer = getAnswerInclMetaInformation(topAnswer.getAnswer(), topAnswer.getOriginalQuestion(), topAnswer.getDateOriginalQuestion(), Utils.getLocale(lang), topAnswer.getUrl(), "<br/>", "<hr/>");
            String answer = Utils.convertToMarkdown(_answer);

            if (answer.length() > ANSWER_MAX_LENGTH) {
                log.warn("Answer is longer (" + answer.length() + ") than max length allowed by Discord (" + ANSWER_MAX_LENGTH + ")! Answer will be shortened ...");

                String linkToCompleteAnswer = domain.getHost() + "/#/read-answer?domain-id=" + domain.getId() + "&uuid=" + topAnswer.getUuid();
                String htmlLink = "<a href=\"" + linkToCompleteAnswer + "\">Complete Answer</a> ";
                htmlLink = Utils.convertToMarkdown(htmlLink);

                answer = answer.substring(0, ANSWER_MAX_LENGTH - 5 - htmlLink.length()) + " ... " + htmlLink;

                // TODO: Add MetaInformation, whereas make sure total answer is not too long!
            }

            final String finalAnswer = answer;

            log.info("Return answer to Discord as Markdown: " + answer);

            final String questionUuid = topAnswer.getQuestionUUID();
            return message.getChannel().flatMap(channel -> channel.createMessage(msg -> messageSender.createAnswer(msg, finalAnswer, questionUuid)));
        } else {
            log.info("No answer available.");
            if (domain.getInformUserNoAnswerAvailable()) {
                log.info("Inform user that no answer available ...");
                return message.getChannel().flatMap(channel -> channel.createMessage(Utils.convertToMarkdown(messageSource.getMessage("sorry.do.not.know.an.answer", null, new Locale("en")))));
            } else {
                return Mono.empty();
            }
        }
    }

    /**
     * Get Katie domain Id
     * @param guildId Guild Id, e.g. '996391257549053952'
     * @param channelId Channel Id, e.g. '996391611275694131'
     * @return Katie domain Id, e.g. "ceb2d2f2-2c1d-49be-a751-eed4be19e021"
     */
    private String getDomainId(String guildId, long channelId) {
        log.info("Get Katie domain for discord guild id '" + guildId + "' and channel id '" + channelId + "' ...");
        return dataRepositoryService.getDomainIdForDiscordGuildChannel(guildId, Long.toString(channelId));
    }
}
