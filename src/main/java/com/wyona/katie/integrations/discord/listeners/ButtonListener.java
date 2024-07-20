package com.wyona.katie.integrations.discord.listeners;

import com.wyona.katie.integrations.CommonMessageSender;
import com.wyona.katie.models.*;
import com.wyona.katie.services.*;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class ButtonListener extends CommonMessageSender {

    @Autowired
    private ContextService domainService;

    @Autowired
    private DataRepositoryService dataRepositoryService;

    @Autowired
    private QuestionAnsweringService qaService;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private ResourceBundleMessageSource messageSource;

    @Value("${new.context.mail.body.host}")
    private String katieHost;

    @Value("${discord.katie.username}")
    private String usernameTechnicalUser;

    public ButtonListener(Optional<GatewayDiscordClient> client) {
        log.info("Init Discord button listener ...");
        String lang = "en"; // TODO

        if (!client.isEmpty()) {
            // INFO: See https://docs.discord4j.com/interactions/application-commands/ and search for deferReply()
            client.get().on(ButtonInteractionEvent.class, event -> event.deferReply().then(handle(event, lang))).subscribe();
        } else {
            log.warn("No GatewayDiscordClient available.");
        }
    }

    /**
     * Handle button interaction
     */
    private Mono<Message> handle(ButtonInteractionEvent event, String lang) {
        log.info("Handle Discord button event '" + event.getMessage() + "' ...");

        String[] buttonIdQuestionUUID = event.getCustomId().split(DiscordNewMessageListener.DELIMITER);

        ChannelAction buttonClicked = ChannelAction.valueOf(buttonIdQuestionUUID[0]);
        log.info("Button clicked: " + buttonClicked);
        String questionUuid = buttonIdQuestionUUID[1];
        log.info("Question UUID: " + questionUuid);

        AskedQuestion askedQuestion = null;
        try {
            askedQuestion = domainService.getAskedQuestionByUUID(questionUuid);
            log.info("Asked question: " + askedQuestion.getQuestion());
        } catch (Exception e) {
            // INFO: Several Katie servers can be registered with a channel. When a user clicks on a button, then all servers will receive a request, but only the server which generated the answer and generated the question UUID will be able to handle the request.
            log.error(e.getMessage());
            log.info("Do not reply");
            return null;
            //return Mono.empty();
        }

        try {
            Context domain = domainService.getContext(askedQuestion.getDomainId());
            log.info("Domain: " + domain.getName() + " / " + domain.getId());

            if (buttonClicked.equals(ChannelAction.THUMB_UP)) {
                domainService.thumbUpDown(askedQuestion, true, domain);
                log.info("Thumb up saved successfully.");
                return event.createFollowup(messageSource.getMessage("thanks.for.positive.feedback", null, Utils.getLocale(lang)));
            } else if (buttonClicked.equals(ChannelAction.THUMB_DOWN)) {
                domainService.thumbUpDown(askedQuestion, false, domain);
                log.info("Thumb down saved successfully.");
                return event.createFollowup(messageSource.getMessage("thanks.for.negative.feedback", null, Utils.getLocale(lang)));
            } else if (buttonClicked.equals(ChannelAction.ENTER_BETTER_ANSWER)) {
                return event.createFollowup("Please submit your better answer ...");
            } else if (buttonClicked.equals(ChannelAction.SEE_MORE_ANSWERS)) {
                return event.createFollowup(getSeeMoreAnswers(domain, askedQuestion.getQuestion()));
            } else if (buttonClicked.equals(ChannelAction.MORE_INFO)) {
                return event.createFollowup(getMore(domain));
            } else {
                return event.createFollowup("Exception: No such button '" + buttonClicked + "' implemented!");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return event.createFollowup("Exception by Katie host '" + katieHost + "': " + e.getMessage());
        }
    }

    /**
     *
     */
    private String getMore(Context domain) {
        // TODO: Improve "More" ...
        return Utils.convertToMarkdown("The answer is based on the Katie domain  <a href=\"" + domain.getHost() + "/#/domain/" + domain.getId() + "\">" + domain.getName() + "</a>.");
    }

    /**
     * Get more answers
     * @param question Asked question
     */
    private String getSeeMoreAnswers(Context domain, String question) {
        try {
            authService.login(usernameTechnicalUser, null);
            log.info("Signed in as technical Katie user '" + authService.getUserId() + "'.");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        String lang = "en";
        List<String> classifications = new ArrayList<String>();
        String remoteAddress = "TODO:Discord";
        String channelRequestId = java.util.UUID.randomUUID().toString();
        try {
            if (false) { // TODO
                String guildId = "TODO";
                String channelId = "TODO";
                String messageId = "TODO";
                dataRepositoryService.addDiscordConversationValues(channelRequestId, domain.getId(), guildId, channelId, messageId);
            }

            int offset = 1;
            int limit = 10;
            String messageId = null; // TODO
            List<ResponseAnswer> answers = qaService.getAnswers(question, false, classifications, messageId, domain, new Date(), remoteAddress, ChannelType.DISCORD, channelRequestId, limit, offset, true, null,false, false, false);

            StringBuilder response = new StringBuilder();
            response.append("<div>");
            for (int i = 0; i < answers.size(); i++) {
                ResponseAnswer answer = answers.get(i);
                String _answer = getAnswerInclMetaInformation(answer.getAnswer(), answer.getOriginalQuestion(), answer.getDateOriginalQuestion(), Utils.getLocale(lang), answer.getUrl(), "<br/>", "");
                _answer = "<div>" + _answer + "</div>";
                if (i < answers.size() - 1) {
                    _answer = _answer + "<span>---------------------------</span>";
                }

                if (response.length() + _answer.length() < 2000) {
                    response.append(_answer);
                } else {
                    log.info("Remaining answers dropped, because Discord only allows max 2000 characters.");
                    break;
                }
            }
            response.append("</div>");

            return Utils.convertToMarkdown(response.toString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Utils.convertToMarkdown("Exception: " + e.getMessage());
        }
    }
}
