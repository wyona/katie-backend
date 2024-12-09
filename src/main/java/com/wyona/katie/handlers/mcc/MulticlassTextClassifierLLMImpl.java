package com.wyona.katie.handlers.mcc;

import com.wyona.katie.handlers.GenerateProvider;
import com.wyona.katie.handlers.MistralAIGenerate;
import com.wyona.katie.handlers.OllamaGenerate;
import com.wyona.katie.handlers.OpenAIGenerate;
import com.wyona.katie.models.*;
import com.wyona.katie.services.BackgroundProcessService;
import com.wyona.katie.services.ClassificationRepositoryService;
import com.wyona.katie.services.GenerativeAIService;
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
    GenerativeAIService generativeAIService;

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

        if (dataset.getLabels().length == 0) {
            log.warn("No labels configured for domain '" + domain.getId() + "'!");
            return hitLabels.toArray(new HitLabel[0]);
        }

        List<PromptMessage> promptMessages = new ArrayList<>();
        promptMessages.add(new PromptMessage(PromptMessageRole.USER, getPrompt(text, dataset.getLabels(), domain)));
        log.info("Prompt: " + promptMessages.get(0).getContent());

        String completedText = null;
        GenerateProvider generateProvider = generativeAIService.getGenAIImplementation(completionImpl);
        String model = generativeAIService.getCompletionModel(completionImpl);
        String apiToken = generativeAIService.getApiToken(completionImpl);
        if (generateProvider != null) {
            completedText = generateProvider.getCompletion(promptMessages, model, temperature, apiToken);
        } else {
            log.error("Completion provider '" + completionImpl + "' not implemented yet!");
        }

        log.info("Completed text: " + completedText);

        if (!completedText.contains(NOT_APPLICABLE)) {
            // INFO: Split answer into classifications and verify labels, such that LLM does not invent categories, like for example "Passwort-Reset (SK)"
            String[] possibleCategories = completedText.split(",");

            for (String possibleCategory : possibleCategories) {
                Classification classification = searchClassification(possibleCategory, dataset.getLabels());
                if (classification != null) {
                    if (!isDuplicate(classification, hitLabels)) {
                        HitLabel hitLabel = new HitLabel(classification, -1);
                        hitLabels.add(hitLabel);
                    }
                } else {
                    log.info("No such classification '" + possibleCategory + "'!");
                }
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
     * @param text Text to be classified / labeled
     * @param labels Possible classifications / labels
     */
    private String getPrompt(String text, Classification[] labels, Context domain) {
        boolean withDescriptionsOnly = false; // TODO: Make configurable
        // TODO: Scalability!
        StringBuilder listOfLabels = new StringBuilder();
        for (Classification label : labels) {
            if (withDescriptionsOnly) {
                if (label.getDescription() != null) {
                    listOfLabels.append(" - " + label.getTerm());
                    listOfLabels.append(" (" + label.getDescription() + ")");
                    listOfLabels.append("\n");
                }
            } else {
                listOfLabels.append(" - " + label.getTerm());
                if (label.getDescription() != null) {
                    listOfLabels.append(" (" + label.getDescription() + ")");
                }
                listOfLabels.append("\n");
            }
        }

        String configuredPrompt = getPromptFromConfig(domain);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Please assign the following text (\"Text\") to one of the following possible categories:\n\n");
        prompt.append(listOfLabels);
        prompt.append("\nReturn the category that matches best. If none of these categories provide a good match, then answer with \"" + NOT_APPLICABLE + "\".");
        prompt.append("\n\nText: " + text);

        return prompt.toString();
    }

    /**
     * Get prompt from system or domain configuration
     */
    private String getPromptFromConfig(Context domain) {
        // TODO: Make prompt configurable resp. even configurable per domain
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please assign the following text (\"Text\") to one of the following possible categories:\n\n{{LABELS}}");
        prompt.append("\nReturn the category that matches best. If none of these categories provide a good match, then answer with \"" + NOT_APPLICABLE + "\".");
        prompt.append("\n\nText: {{TEXT}}");
        return prompt.toString();
    }

    /**
     * @param query Query, e.g. "Identität" or "Zugang (SK)" or "Passwort-Reset (SK)"
     */
    private Classification searchClassification(String query, Classification[] classifications) {
        for (Classification classification : classifications) {
            if (classification.getTerm().contains(query)) {
                return classification;
            }
        }
        return null;
    }

    /**
     * @return true when classification is already contained and false otherwise
     */
    private boolean isDuplicate(Classification classification, List<HitLabel> hitLabels) {
        for (HitLabel hitLabel : hitLabels) {
            if (hitLabel.getLabel().getKatieId().equals(classification.getKatieId())) {
                return true;
            }
        }
        return false;
    }
}
