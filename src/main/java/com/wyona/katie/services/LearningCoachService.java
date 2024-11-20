package com.wyona.katie.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.models.ChosenSuggestion;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.PromptMessageWithRoleLowerCase;
import com.wyona.katie.models.learningcoach.ConversationStarter;
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
     *
     */
    private File getConversationStartersDir() {
        return new File(volumeBasePath, "learning-coach/conversation-starter-prompts");
    }

    /**
     *
     */
    public ConversationStarter[] getConversationStarters() {
        // TODO: Get starters for a particular user
        List<ConversationStarter> starters = new ArrayList<>();

        File[] starterFiles = getConversationStartersDir().listFiles();
        ObjectMapper objectMapper = new ObjectMapper();
        for (File starterFile : starterFiles) {
            try {
                ConversationStarter conversationStarter = objectMapper.readValue(starterFile, ConversationStarter.class);
                starters.add(conversationStarter);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return starters.toArray(new ConversationStarter[0]);
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
