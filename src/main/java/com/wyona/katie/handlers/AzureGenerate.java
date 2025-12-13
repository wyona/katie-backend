package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 *
 */
@Slf4j
@Component
public class AzureGenerate implements GenerateProvider {

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
        CompletionResponse response = new CompletionResponse("TODO");
        log.warn("Implementation not finished yet!");
        return response;
    }
}
