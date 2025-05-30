package com.wyona.katie.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.handlers.*;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Generative AI service
 */
@Slf4j
@Component
public class GenerativeAIService {

    @Autowired
    private LearningCoachService learningCoachService;

    @Autowired
    private OpenAIGenerate openAIGenerate;
    @Autowired
    private AlephAlphaGenerate alephAlphaGenerate;
    @Autowired
    private MistralAIGenerate mistralAIGenerate;
    @Autowired
    private OllamaGenerate ollamaGenerate;

    private static final String CHAT_HISTORY_FILENAME = "history.json";

    /**
     * Get GenAI implementation
     */
    public GenerateProvider getGenAIImplementation(CompletionImpl impl) {
        if (impl.equals(CompletionImpl.ALEPH_ALPHA)) {
            return alephAlphaGenerate;
        } else if (impl.equals(CompletionImpl.OPENAI)) {
            return openAIGenerate;
        } else if (impl.equals(CompletionImpl.MISTRAL_AI)) {
            return mistralAIGenerate;
        } else if (impl.equals(CompletionImpl.OLLAMA)) {
            return ollamaGenerate;
        } else {
            log.error("No such completion implemention supported yet: " + impl);
            return null;
        }
    }

    /**
     * Get completion message
     * @param domain Katie domain associated with conversation
     * @param chatCompletionsRequest Request containing user message or suggestion chosen by user
     * @param user User having conversation with LLM
     * @return completion message
     */
    public String getCompletion(Context domain, ChatCompletionsRequest chatCompletionsRequest, User user) throws Exception {
        CompletionImpl completionImpl = domain.getCompletionConfig().getCompletionImpl();
        if (completionImpl == CompletionImpl.UNSET) {
            String warnMsg = "Domain '" + domain.getId() + "' has no completion implementation configured!";
            log.warn(warnMsg);
            return warnMsg;
        } else {
            log.info("Domain '" + domain.getId() + "' has '" + completionImpl + "' configured as completion implementation.");
        }

        ChatHistory history = getConversationHistory(domain, chatCompletionsRequest.getConversation_id());
        if (history == null) {
            history = new ChatHistory(user.getId());
            saveHistory(history, domain, chatCompletionsRequest.getConversation_id());
        }

        ChosenSuggestion chosenSuggestion = chatCompletionsRequest.getchosen_suggestion();
        if (chosenSuggestion != null) {
            log.info("Chosen suggestion Id: " + chosenSuggestion.getIndex());
            appendMessageToConversationHistory(domain, chatCompletionsRequest.getConversation_id(), PromptMessageRoleLowerCase.system, learningCoachService.getSystemPrompt(chosenSuggestion));
            appendMessageToConversationHistory(domain, chatCompletionsRequest.getConversation_id(), PromptMessageRoleLowerCase.user, learningCoachService.getSuggestionText(chosenSuggestion.getIndex()));
        } else if (chatCompletionsRequest.getSuggestions().length > 0) {
            // TODO
            appendMessageToConversationHistory(domain, chatCompletionsRequest.getConversation_id(), PromptMessageRoleLowerCase.system, "TODO");
        } else {
            log.info("No suggestion provided.");
            PromptMessageWithRoleLowerCase mostRecentUserMessage = chatCompletionsRequest.getMessages()[chatCompletionsRequest.getMessages().length - 1];
            appendMessageToConversationHistory(domain, chatCompletionsRequest.getConversation_id(), PromptMessageRoleLowerCase.user, mostRecentUserMessage.getContent());
        }

        String completedText = "Hi, this is a mock response from Katie :-)";
        if (false) {
            log.info("Return mock completion ...");
        } else {
            log.info("Get completion from LLM ...");
            Double temperature = 0.7;
            if (chatCompletionsRequest.getTemperature() != null) {
                temperature = chatCompletionsRequest.getTemperature();
            }

            log.info("Conversation history contains " + chatCompletionsRequest.getMessages().length + " messages.");
            ChatHistory chatHistory = getConversationHistory(domain, chatCompletionsRequest.getConversation_id());
            List<PromptMessage> promptMessages = new ArrayList<>();
            for (PromptMessageWithRoleLowerCase msg : chatHistory.getMessages()) {
                // TODO: Do not always send complete history!!!
                promptMessages.add(new PromptMessage(PromptMessageRole.fromString(msg.getRole().toString()), msg.getContent()));
            }

            GenerateProvider generateProvider = getGenAIImplementation(completionImpl);
            completedText = generateProvider.getCompletion(promptMessages, null, domain.getCompletionConfig(), temperature).getText();
        }

        appendMessageToConversationHistory(domain, chatCompletionsRequest.getConversation_id(), PromptMessageRoleLowerCase.assistant, completedText);
        return completedText;
    }

    /**
     * Get a particular conversation history
     */
    private ChatHistory getConversationHistory(Context domain, String conversationId) throws Exception {
        File conversationDir = new File(domain.getConversationsDirectory(), conversationId);
        File historyFile = new File(conversationDir, CHAT_HISTORY_FILENAME);
        if (historyFile.isFile()) {
            ObjectMapper objectMapper = new ObjectMapper();
            ChatHistory history = objectMapper.readValue(historyFile, ChatHistory.class);
            return history;
        } else {
            return null;
        }
    }

    /**
     * Append message to conversation history
     */
    private void appendMessageToConversationHistory(Context domain, String conversationId, PromptMessageRoleLowerCase role, String message) {
        log.info("Add message to conversation '" + conversationId + "' ...");

        try {
            ChatHistory history = getConversationHistory(domain, conversationId);
            // TODO: Add timestamp
            history.appendMessage(new PromptMessageWithRoleLowerCase(role, message));
            saveHistory(history, domain, conversationId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     *
     */
    private void saveHistory(ChatHistory history, Context domain, String conversationId) {
        File conversationDir = new File(domain.getConversationsDirectory(), conversationId);
        if (!conversationDir.isDirectory()) {
            conversationDir.mkdirs();
        }
        File historyFile = new File(conversationDir, CHAT_HISTORY_FILENAME);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(historyFile, history);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
