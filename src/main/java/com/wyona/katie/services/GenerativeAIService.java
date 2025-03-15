package com.wyona.katie.services;

import com.wyona.katie.handlers.*;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
     * @param chosenSuggestion TODO
     * @param chatCompletionsRequest TODO
     * @return completion message
     */
    public String getCompletion(Context domain, ChosenSuggestion chosenSuggestion, ChatCompletionsRequest chatCompletionsRequest) throws Exception {
        CompletionImpl completionImpl = domain.getCompletionConfig().getCompletionImpl();
        //completionImpl = CompletionImpl.OLLAMA;
        if (completionImpl == CompletionImpl.UNSET) {
            String warnMsg = "Domain '" + domain.getId() + "' has no completion implementation configured!";
            log.warn(warnMsg);
            return warnMsg;
        } else {
            log.info("Domain '" + domain.getId() + "' has '" + completionImpl + "' configured as completion implementation.");
        }
        GenerateProvider generateProvider = getGenAIImplementation(completionImpl);
        String model = domain.getCompletionConfig().getModel();

        List<PromptMessage> promptMessages = new ArrayList<>();

        if (chosenSuggestion != null) {
            log.info("Chosen suggestion Id: " + chosenSuggestion.getIndex());
            promptMessages.add(new PromptMessage(PromptMessageRole.SYSTEM, learningCoachService.getSystemPrompt(chosenSuggestion)));
            // TODO: Remember that conversation was started with suggestion
            appendMessageToConversationHistory(domain, chatCompletionsRequest.getConversation_id(), PromptMessageRoleLowerCase.system.toString(), learningCoachService.getSystemPrompt(chosenSuggestion));
        } else {
            log.info("No suggestion provided.");
            // TODO: Check whether conversation was started with a suggestion and if so, then add suggestion to beginning of conversation
        }

        log.info("Conversation history contains " + chatCompletionsRequest.getMessages().length + " messages.");
        getConversationHistory(domain, chatCompletionsRequest.getConversation_id());
        for (PromptMessageWithRoleLowerCase msg : chatCompletionsRequest.getMessages()) {
            promptMessages.add(new PromptMessage(PromptMessageRole.fromString(msg.getRole().toString()), msg.getContent()));
        }

        Double temperature = 0.7;
        if (chatCompletionsRequest.getTemperature() != null) {
            temperature = chatCompletionsRequest.getTemperature();
        }

        String apiToken = domain.getCompletionConfig().getApiKey();

        String completedText = "Hi, this is a mock response from Katie :-)";
        if (false) {
            log.info("Return mock completion ...");
        } else {
            log.info("Get completion from LLM ...");
            completedText = generateProvider.getCompletion(promptMessages, null, model, temperature, apiToken).getText();
        }

        appendMessageToConversationHistory(domain, chatCompletionsRequest.getConversation_id(), PromptMessageRoleLowerCase.assistant.toString(), completedText);
        return completedText;
    }

    /**
     *
     */
    private void getConversationHistory(Context domain, String conversationId) {
        // TODO
    }

    /**
     * Append message to conversation history
     */
    private void appendMessageToConversationHistory(Context domain, String conversationId, String role, String message) {
        log.info("TODO: Add nessage to conversation '" + conversationId + "' ...");
    }
}
