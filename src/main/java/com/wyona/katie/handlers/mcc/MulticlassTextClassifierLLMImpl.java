package com.wyona.katie.handlers.mcc;

import com.wyona.katie.handlers.GenerateProvider;
import com.wyona.katie.handlers.MistralAIGenerate;
import com.wyona.katie.handlers.OllamaGenerate;
import com.wyona.katie.handlers.OpenAIGenerate;
import com.wyona.katie.models.*;
import com.wyona.katie.services.BackgroundProcessService;
import com.wyona.katie.services.ClassificationRepositoryService;
import com.wyona.katie.services.GenerativeAIService;
import com.wyona.katie.services.XMLService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final static String NOT_APPLICABLE = "N/A";
    private final static String PLACEHOLDER_LABELS = "LABELS";
    private final static String PLACEHOLDER_TEXT = "TEXT";
    private final static String PLACEHOLDER_LIMIT = "LIMIT";

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
        promptMessages.add(new PromptMessage(PromptMessageRole.USER, getPrompt(text, dataset.getLabels(), limit, domain)));
        log.info("Prompt: " + promptMessages.get(0).getContent());

        String completedText = null;
        CompletionImpl completionImpl = domain.getCompletionImpl();
        GenerateProvider generateProvider = generativeAIService.getGenAIImplementation(completionImpl);
        if (generateProvider != null) {
            // TODO: Use tool call!
            String model = generativeAIService.getCompletionModel(completionImpl);
            String apiToken = generativeAIService.getApiToken(completionImpl);
            completedText = generateProvider.getCompletion(promptMessages, null, null, model, temperature, apiToken).getText();
        } else {
            String errorMsg = "Completion provider '" + completionImpl + "' not implemented yet! Make sure that the domain configuration attribute '" + XMLService.CONTEXT_GENERATIVE_AI_IMPLEMENTATION_ATTR + "' is set correctly.";
            log.error(errorMsg);
            throw new Exception(errorMsg);
        }

        log.info("Completed text: " + completedText);

        if (completedText != null && !completedText.contains(NOT_APPLICABLE)) {

            List<String> uuids = extractUUIDs(completedText);
            if (uuids.size() > 0) {
                for (String uuid : uuids) {
                    for (Classification classification : dataset.getLabels()) {
                        if (classification.getKatieId().equals(uuid)) {
                            if (!isDuplicate(classification, hitLabels)) {
                                // TODO: Set a score
                                HitLabel hitLabel = new HitLabel(classification, -1);
                                hitLabels.add(hitLabel);
                            }
                        }
                    }
                }
            } else {
                log.warn("No UUID could be extracted from completed text!");
            }
        } else {
            log.info("No category matched.");
        }

        return hitLabels.toArray(new HitLabel[0]);
    }

    /**
     * Extract UUIDs from completed text
     * @param completedText Completed text containing category UUID, e.g. "The text 'Ich kann mich mit meinem MacBook Pro nicht mehr mit dem Wireless verbinden, können Sie mir helfen?' best matches the category 'Netzwerk (SK), Connectivity Wireless (SK)' with Category Id: 0e708532-69fd-4e69-8f96-11ef5f31d567."
     * @return list of UUIDs (UUID example "0e708532-69fd-4e69-8f96-11ef5f31d567")
     */
    private List<String> extractUUIDs(String completedText) {
        List<String> uuids = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}"). matcher(completedText.toLowerCase());
        while (matcher.find()) {
            String uuid = matcher.group();
            log.info("Group: " + uuid);
            uuids.add(uuid);
        }
        return uuids;
    }

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#train(Context, TextSample[])
     */
    public void train(Context domain, TextSample[] samples) throws Exception {
        log.warn("TODO: Implement train() method of " + getClass().getName());
    }

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#retrain(Context, String)
     */
    public void retrain(Context domain, String bgProcessId) throws Exception {
        String logMsg = "TODO: Implement retrain() method of " + getClass().getName();
        log.info(logMsg);
        backgroundProcessService.updateProcessStatus(bgProcessId, logMsg);
    }

    /**
     * https://huggingface.co/docs/transformers/main/tasks/prompting#text-classification
     * @param text Text to be classified / labeled
     * @param labels Possible classifications / labels
     * @param limit Limit of returned labels
     * @param domain Domain associated with classification
     */
    private String getPrompt(String text, Classification[] labels, int limit, Context domain) {
        boolean withDescriptionsOnly = false; // TODO: Make configurable
        // TODO: Scalability!
        StringBuilder listOfLabels = new StringBuilder();
        for (Classification label : labels) {
            if (withDescriptionsOnly) {
                if (label.getDescription() != null) {
                    listOfLabels.append(" - " + label.getTerm());
                    listOfLabels.append(" (" + label.getDescription() + ")");
                    listOfLabels.append(" (Category Id: " + label.getKatieId() + ")");
                    listOfLabels.append("\n");
                }
            } else {
                listOfLabels.append(" - " + label.getTerm());
                if (label.getDescription() != null) {
                    listOfLabels.append(" (" + label.getDescription() + ")");
                }
                listOfLabels.append(" (Category Id: " + label.getKatieId() + ")");
                listOfLabels.append("\n");
            }
        }

        String prompt = getPromptFromConfig(domain);
        prompt = prompt.replaceAll("\\{\\{" + PLACEHOLDER_LABELS + "\\}\\}", listOfLabels.toString());
        prompt = prompt.replaceAll("\\{\\{" + PLACEHOLDER_TEXT + "\\}\\}", text);
        prompt = prompt.replaceAll("\\{\\{" + PLACEHOLDER_LIMIT + "\\}\\}", "" + limit);

        return prompt;
    }

    /**
     * Get prompt from system or domain configuration
     */
    private String getPromptFromConfig(Context domain) {
        // TODO: Make prompt configurable resp. even configurable per domain
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please assign the following text (\"Text\") to one of the following possible categories:\n\n{{" + PLACEHOLDER_LABELS+ "}}");
        prompt.append("\nReturn {{" + PLACEHOLDER_LIMIT + "}} categories and its IDs that match best. If none of the listed categories provide a good match, then answer with \"" + NOT_APPLICABLE + "\".");
        prompt.append("\n\nText: {{" + PLACEHOLDER_TEXT + "}}");
        return prompt.toString();
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
