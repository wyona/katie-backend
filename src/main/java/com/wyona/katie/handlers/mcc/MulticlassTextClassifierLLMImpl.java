package com.wyona.katie.handlers.mcc;

import com.wyona.katie.handlers.GenerateProvider;
import com.wyona.katie.handlers.MistralAIGenerate;
import com.wyona.katie.handlers.OllamaGenerate;
import com.wyona.katie.handlers.OpenAIGenerate;
import com.wyona.katie.models.*;
import com.wyona.katie.services.BackgroundProcessService;
import com.wyona.katie.services.ClassificationRepositoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Use LLM respectively a corresponding LLM prompt to classify text
 */
@Slf4j
@Component
public class MulticlassTextClassifierLLMImpl implements MulticlassTextClassifier {

    @Autowired
    ClassificationRepositoryService classificationRepositoryService;

    @Autowired
    BackgroundProcessService backgroundProcessService;

    @Autowired
    private MistralAIGenerate mistralAIGenerate;
    @Value("${mistral.api.key}")
    private String mistralAIKey;
    @Value("${mistral.ai.completion.model}")
    private String mistralAIModel;

    @Autowired
    private OllamaGenerate ollamaGenerate;
    @Value("${ollama.completion.model}")
    private String ollamaModel;

    @Autowired
    private OpenAIGenerate openAIGenerate;
    @Value("${openai.key}")
    private String openAIKey;
    @Value("${openai.generate.model}")
    private String openAIModel;

    @Value("${re_rank.llm.temperature}")
    private Double temperature;

    @Value("${re_rank.llm.impl}")
    private CompletionImpl completionImpl;

    private final static String NOT_APPLICABLE = "N/A";

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#predictLabels(Context, String, int)
     */
    public HitLabel[] predictLabels(Context domain, String text, int limit) throws Exception {
        List<HitLabel> hitLabels = new ArrayList<>();

        ClassificationDataset dataset = classificationRepositoryService.getDataset(domain, true, 0,-1);

        List<PromptMessage> promptMessages = new ArrayList<>();
        promptMessages.add(new PromptMessage(PromptMessageRole.USER, getPrompt(text, dataset.getLabels())));
        log.info("Prompt: " + promptMessages.get(0).getContent());

        // TODO: Create LLM service, which can also be used by LLMReRank,etc.
        GenerateProvider generateMistralCloud = mistralAIGenerate;
        GenerateProvider generateOllama = ollamaGenerate;
        GenerateProvider generateOpenAI = openAIGenerate;
        String completedText = null;
        if (completionImpl.equals(CompletionImpl.MISTRAL_AI)) {
            completedText = generateMistralCloud.getCompletion(promptMessages, mistralAIModel, temperature, mistralAIKey);
        } else if (completionImpl.equals(CompletionImpl.MISTRAL_OLLAMA)) {
            completedText = generateOllama.getCompletion(promptMessages, ollamaModel, temperature, null);
        } else if (completionImpl.equals(CompletionImpl.OPENAI)) {
            completedText = generateOpenAI.getCompletion(promptMessages, openAIModel, temperature, openAIKey);
        } else {
            log.error("Completion provider '" + completionImpl + "' not implemented yet!");
        }

        log.info("Completed text: " + completedText);

        if (!completedText.contains(NOT_APPLICABLE)) {
            // TODO: Split answer into classifications and verify labels, such that LLM does not invent categories, like for example "Passwort-Reset (SK)"
            String[] possibleCategories = completedText.split(",");

            for (String possibleCategory : possibleCategories) {
                Classification classification = new Classification(possibleCategory, "TODO");
                HitLabel hitLabel = new HitLabel(classification, -1);
                hitLabels.add(hitLabel);
            }
        } else {
            log.info("No category matched.");
        }

        return hitLabels.toArray(new HitLabel[0]);
    }

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#train(Context, TextSample[])
     */
    public void train(Context domain, TextSample[] samples) throws Exception {
        log.warn("TODO: Implement train method.");
    }

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#retrain(Context, String)
     */
    public void retrain(Context domain, String bgProcessId) throws Exception {
        log.info("TODO: Implement retrain method");
        backgroundProcessService.updateProcessStatus(bgProcessId, "TODO: Implement etrain method");
    }

    /**
     * https://huggingface.co/docs/transformers/main/tasks/prompting#text-classification
     */
    private String getPrompt(String text, Classification[] labels) {
        // TODO: Make prompt configurable per domain

        StringBuilder prompt = new StringBuilder();
        prompt.append("Please assign the following text (\"Text\") to one of the following possible categories:\n\n");
        // TODO: Scalability!
        for (Classification label : labels) {
            prompt.append(label.getTerm() + "\n");
        }
        prompt.append("\nReturn the category that matches best. If none of these categories provide a good match, then answer with \"" + NOT_APPLICABLE + "\".");
        prompt.append("\n\nText: " + text);
        return prompt.toString();
    }
}
