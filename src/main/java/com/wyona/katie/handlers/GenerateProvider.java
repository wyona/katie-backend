package com.wyona.katie.handlers;

/**
 * Generate / complete text provider, e.g. OpenAI, Aleph Alpha, HuggingChat, Open Assistant, ...
 */
public interface GenerateProvider {

    /**
     * Generate / complete text
     * @param prompt Prompt, e.g. "Please answer the following question 'Why ...' based on the following context 'According to ...'."
     * @param model Model name, e.g. OpenAI's "gpt-3.5-turbo"
     * @param temperature What sampling temperature to use, between 0.0 and 1.0. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic.
     * @param apiToken API token
     * @return generated / completed text
     */
    public String getCompletion(String prompt, String model, Double temperature, String apiToken) throws Exception;
}
