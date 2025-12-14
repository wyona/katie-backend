package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 *
 */
@Slf4j
@Component
public class AzureGenerate implements GenerateProvider {

    @Autowired
    AzureOpenAiChatModel azureOpenAiChatModel;

    /**
     * @see GenerateProvider#getAssistant(String, String, String, List, CompletionConfig)
     */
    public CompletionAssistant getAssistant(String id, String name, String instructions, List<CompletionTool> tools, CompletionConfig completionConfig) throws Exception {
        log.error("Not implemented yet!");
        return null;
    }

    /**
     * @see GenerateProvider#getCompletion(List, CompletionAssistant, CompletionConfig, Double)
     */
    public CompletionResponse getCompletion(List<PromptMessage> promptMessages, CompletionAssistant assistant, CompletionConfig completionConfig, Double temperature) throws Exception {
        return getCompletionUsingSpringAI(promptMessages, assistant, completionConfig, temperature);
    }

    /**
     * Get completion using Spring AI implementation
     */
    private CompletionResponse getCompletionUsingSpringAI(List<PromptMessage> promptMessages, CompletionAssistant assistant, CompletionConfig completionConfig, Double temperature) throws Exception {
        log.info("Spring AI Azure OpenAI implementation ...");
        PromptMessage firstMessage = promptMessages.get(0);

        ChatResponse response = azureOpenAiChatModel.call(
                new Prompt(
                        firstMessage.getContent(),
                        AzureOpenAiChatOptions.builder()
                                .deploymentName(completionConfig.getModel())
                                .temperature(temperature)
                                .build()
                )
        );
        return new CompletionResponse(response.getResult().toString());
    }
}
