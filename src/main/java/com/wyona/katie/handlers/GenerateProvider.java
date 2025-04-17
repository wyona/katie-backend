package com.wyona.katie.handlers;

import com.wyona.katie.models.*;

import java.util.List;

/**
 * Generate / complete text provider, e.g. OpenAI, Ollama, Mistral, Aleph Alpha, HuggingChat, Open Assistant, ...
 */
public interface GenerateProvider {

    /**
     * Generate / complete text
     * @param promptMessages Prompt messages, e.g. "Please answer the following question 'Why ...' based on the following context 'According to ...'."
     * @param assistant Optional assistant
     * @param completionConfig Completion configuration, including model, API key, host, ...
     * @param temperature What sampling temperature to use, between 0.0 and 1.0. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic.
     * @return generated / completed text, including citations, tool / function arguments, etc.
     */
    public CompletionResponse getCompletion(List<PromptMessage> promptMessages, CompletionAssistant assistant, CompletionConfig completionConfig, Double temperature) throws Exception;

    /**
     * Get assistant to help generate / complete text
     * @param id Assistant Id, e.g. "asst_hB84DejccTGxt3hI8xqsGykJ"
     * @param completionConfig Completion configuration, including model, API key, host, ...
     */
    public CompletionAssistant getAssistant(String id, String name, String instructions, List<CompletionTool> tools, CompletionConfig completionConfig) throws Exception;
}
