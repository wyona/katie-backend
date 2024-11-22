package com.wyona.katie.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.models.ChosenSuggestion;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.PromptMessageWithRoleLowerCase;
import com.wyona.katie.models.learningcoach.ConversationStarter;
import com.wyona.katie.models.learningcoach.Suggestion;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
public class LearningCoachService {

    @Value("${volume.base.path}")
    private String volumeBasePath;

    /**
     * Get directory containing conversation starter suggestions including corresponding prompts
     */
    private File getConversationStartersDir() {
        return new File(volumeBasePath, "learning-coach/conversation-starter-prompts");
    }

    /**
     * Get Ids of conversation starter suggestions configured for a particular user
     * @param userId User Id
     */
    private List<String> getConversationStarterIds(String userId) {
        List<String> ids = new ArrayList<>();

        File conversationStartersOfUser = new File(volumeBasePath, "learning-coach/users/" + userId + "/conversation-starters.xml");
        if (conversationStartersOfUser.isFile()) {
            // TODO: Get Ids from file
            ids.add("191aae92-a23a-4e98-b618-58818a8751f2");
        }

        return ids;
    }

    /**
     * @param userId Id of signed in user, e.g. "c0646e06-16f3-4c41-a7a0-2d1dbc10a67d"
     * @return array of conversation starter suggestions
     */
    public Suggestion[] getConversationStarters(String userId) {
        List<Suggestion> starters = new ArrayList<>();

        List<String> ids = new ArrayList<>();

        if (userId != null) {
            log.info("Get conversation starter suggestions configured for signed in user '" + userId + "' ...");
            ids = getConversationStarterIds(userId);
        } else {
            log.info("Get conversation starter suggestions for anonymous user ...");
        }

        if (ids.size() == 0) {
            log.info("Get default conversation starter suggestions ...");
            // TODO: Make configurable
            ids.add("1");
            ids.add("191aae92-a23a-4e98-b618-58818a8751f2");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        for (String id : ids) {
            File starterFile = new File(getConversationStartersDir(), "/" + id + ".json");
            if (starterFile.isFile()) {
                try {
                    ConversationStarter conversationStarter = objectMapper.readValue(starterFile, ConversationStarter.class);
                    starters.add(conversationStarter.getSuggestion());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.error("No such file '" + starterFile.getAbsolutePath() + "'!");
            }
        }

        return starters.toArray(new Suggestion[0]);
    }

    /**
     * Get system prompt for the chosen suggestion
     * @param chosenSuggestion Chosen suggestion, e.g. conversation starter "Let's learn together how to read an analog clock"
     * @return prompt, e.g."Explain the basic components of an analog clock in a clear and simple way."
     */
    public String getSystemPrompt(ChosenSuggestion chosenSuggestion) {
        //domain.getPromptMessages();
        File promptFile = new File(getConversationStartersDir(), chosenSuggestion.getIndex() + ".json");
        if (promptFile.exists()) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                ConversationStarter conversationStarter = objectMapper.readValue(promptFile, ConversationStarter.class);
                PromptMessageWithRoleLowerCase[] messages = conversationStarter.getMessages();
                return messages[0].getContent();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return "Tell the user, that an error occured when trying to read the system prompt.";
            }
        } else {
            log.warn("No such prompt file '" + promptFile.getAbsolutePath() + "'!");
            return "Tell the user, that no prompt for suggestion '" + chosenSuggestion.getIndex() + "' exists.";
        }
    }
}
