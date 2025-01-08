package com.wyona.katie.handlers;

import com.wyona.katie.models.CompletionTool;
import com.wyona.katie.models.PromptMessage;

import java.util.List;

/**
 * Generate / complete text provider, e.g. OpenAI, Ollama, Mistral, Aleph Alpha, HuggingChat, Open Assistant, ...
 */
public interface GenerateProvider {

    /**
     * Generate / complete text
     * @param promptMessages Prompt messages, e.g. "Please answer the following question 'Why ...' based on the following context 'According to ...'."
     * @param tools Tools / functions
     * @param model Model name, e.g. OpenAI's "gpt-3.5-turbo"
     * @param temperature What sampling temperature to use, between 0.0 and 1.0. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic.
     * @param apiToken API token
     * @return generated / completed text
     */
    public String getCompletion(List<PromptMessage> promptMessages, List<CompletionTool> tools, String model, Double temperature, String apiToken) throws Exception;
}
