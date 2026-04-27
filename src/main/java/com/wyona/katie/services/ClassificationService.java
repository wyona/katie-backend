package com.wyona.katie.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifier;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifierEmbeddingsCentroidsImpl;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifierLLMImpl;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifierMaximumEntropyImpl;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Classification service to predict labels for text(s)
 */
@Slf4j
@Component
public class ClassificationService {

    @Autowired
    private DataRepositoryService dataRepoService;

    @Autowired
    private ClassificationRepositoryService classificationRepoService;

    @Autowired
    private MulticlassTextClassifierEmbeddingsCentroidsImpl classifierEmbeddingsCentroid;

    @Autowired
    private MulticlassTextClassifierMaximumEntropyImpl classifierMaximumEntropy;

    @Autowired
    private MulticlassTextClassifierLLMImpl classifierLLM;

    @Autowired
    private BackgroundProcessService backgroundProcessService;

    @Autowired
    private ResourceBundleMessageSource messageSource;

    @Autowired
    private JwtService jwtService;

    /**
     * Predict labels for a text and a particular domain
     * @param domain Domain object
     * @param text Text to be classified / labeled
     * @param limit Limit of returned labels
     * @return array of suggested labels
     */
    public HitLabel[] predictLabels(Context domain, String text, int limit) throws Exception {

        // Predict labels for a text
        HitLabel[] hitLabels = getClassifier(domain.getClassifierImpl()).predictLabels(domain, text, limit);

        // Get additional information of predicted labels
        for (HitLabel hitLabel : hitLabels) {
            String labelKatieId = hitLabel.getLabel().getKatieId();
            Classification classification = classificationRepoService.getClassification(domain, labelKatieId);
            if (classification != null) {
                hitLabel.getLabel().setTerm(classification.getTerm());
                hitLabel.getLabel().setId(classification.getId());
            } else {
                log.error("No such classification '" + labelKatieId + "'!");
            }
        }

        return hitLabels;
    }

    /**
     * Retrain classifier with human preferences dataset
     * @param preferences Human preferences
     * @param bgProcessId Background process Id
     */
    @Async
    public void retrain(Context domain, List<HumanPreferenceLabel> preferences, String bgProcessId, String userId) {
        backgroundProcessService.startProcess(bgProcessId, "Retrain classifier '" + domain.getClassifierImpl() + "' for domain '" + domain.getId() + "'.", userId);

        MulticlassTextClassifier classifier = getClassifier(domain.getClassifierImpl());

        if (preferences != null && preferences.size() > 0) {
            backgroundProcessService.updateProcessStatus(bgProcessId, "Retrain classifier with human preferences dataset ...");
            // TODO: Get all approved ratings and do batch training
            for (HumanPreferenceLabel preference : preferences) {
                if (preference.getChosenLabel() != null) {
                    if (preference.getMeta().getApproved()) {
                        String ratingId = preference.getMeta().getId();

                        String clientMsgId = preference.getMeta().getClientMessageId();

                        Classification classification = new Classification();
                        classification.setKatieId(preference.getChosenLabel().getKatieId());
                        classification.setId(preference.getChosenLabel().getId());
                        classification.setTerm(preference.getChosenLabel().getTerm());

                        TextSample sample = new TextSample(clientMsgId, preference.getText(), classification);
                        try {
                            if (sample.getClassification().getKatieId() != null && sample.getId() != null && sample.getId() != "null") {
                                backgroundProcessService.updateProcessStatus(bgProcessId, "Add sample: '" + sample.getText() + "' (Label: " + sample.getClassification().getTerm() + ", Katie Id: " + sample.getClassification().getKatieId() + ", Client message Id: " + sample.getId() + ")");
                                log.info("Add sample '" + sample.getClassification().getKatieId() + "' / '" + sample.getId() + "' ...");
                                importSample(domain, sample);

                                // INFO: Retrain classifier with approved sample
                                TextSample[] samples = new TextSample[1];
                                samples[0] = sample;
                                classifier.train(domain, samples);
                                backgroundProcessService.updateProcessStatus(bgProcessId, "Classifier " + classifier.getClass().getName() + " retrained with new sample(s).");

                                // TODO: Only delete when "local" preference dataset is being used
                                log.info("Remove rating '" + ratingId + "' from local preference dataset ...");
                                boolean deleted = removeRatingOfPredictedLabels(domain, ratingId);
                                if (deleted) {
                                    backgroundProcessService.updateProcessStatus(bgProcessId, "Rating '" + ratingId + "' deleted from local human preference dataset.");
                                } else {
                                    backgroundProcessService.updateProcessStatus(bgProcessId, "Rating '" + ratingId + "' not deleted from local human preference dataset!", BackgroundProcessStatusType.WARN);
                                }
                            } else {
                                String warnMsg = "Trying to add human preference rating '" + ratingId + "' as sample, but either Katie Id '" + sample.getClassification().getKatieId() + "' or sample Id (client message Id) '" + sample.getId() + "' is null!";
                                log.warn(warnMsg);
                                backgroundProcessService.updateProcessStatus(bgProcessId, warnMsg, BackgroundProcessStatusType.WARN);
                            }
                        } catch (Exception e) {
                            backgroundProcessService.updateProcessStatus(bgProcessId, e.getMessage(), BackgroundProcessStatusType.ERROR);
                            log.error(e.getMessage(), e);
                        }
                    } else {
                        backgroundProcessService.updateProcessStatus(bgProcessId, "Human preference of text '" + preference.getText() + "' with Katie label Id '" + preference.getChosenLabel().getKatieId() + "' has not yet been approved!", BackgroundProcessStatusType.WARN);
                    }
                } else {
                    backgroundProcessService.updateProcessStatus(bgProcessId, "Human preference of text '" + preference.getText() + "' has no chosen label!", BackgroundProcessStatusType.WARN);
                }
            }
        } else {
            backgroundProcessService.updateProcessStatus(bgProcessId, "No preference dataset provided.");
            //backgroundProcessService.updateProcessStatus(bgProcessId, "No preference dataset provided, therefore use existing samples only for retraining classifier.");
        }

        /* INFO: We already train the individual samples above
        try {
            if (false) {
                backgroundProcessService.updateProcessStatus(bgProcessId, "Retrain classifier ...");
                classifier.retrain(domain, bgProcessId);
            } else {
                backgroundProcessService.updateProcessStatus(bgProcessId, "Retraining classifier disabled", BackgroundProcessStatusType.WARN);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            backgroundProcessService.updateProcessStatus(bgProcessId, e.getMessage(), BackgroundProcessStatusType.ERROR);
        }
         */
        backgroundProcessService.stopProcess(bgProcessId, domain.getId());
    }

    /**
     * Import classification sample / observation (text and label)
     */
    public void importSample(Context domain, TextSample sample) throws Exception {
        classificationRepoService.saveSample(domain, sample);
    }

    /**
     * Remove classification / label from training dataset
     */
    public void removeClassification(Context domain, Classification classification) throws Exception {
        classificationRepoService.removeClassification(domain, classification);

        // TODO: Retrain classifier, whereas only retrain for batch removal
        //MulticlassTextClassifier classifier = getClassifier(domain.getClassifierImpl());
        //classifier.retrain(domain, null);
    }

    /**
     * Get classifier implementation
     */
    public MulticlassTextClassifier getClassifier(ClassificationImpl impl) {
        if (impl.equals(ClassificationImpl.MAX_ENTROPY)) {
            return classifierMaximumEntropy;
        } else if (impl.equals(ClassificationImpl.LLM)) {
            return classifierLLM;
        } else {
            return classifierEmbeddingsCentroid;
        }
    }

    /**
     * Get classification dataset (labels and samples)
     * @param domain
     * @param labelsOnly When set to true, then return only labels and no samples
     * @param offset Offset of returned samples
     * @param limit Limit of returned samples
     */
    public ClassificationDataset getDataset(Context domain, boolean labelsOnly, int offset, int limit) throws Exception {
        return classificationRepoService.getDataset(domain, labelsOnly, offset, limit);
    }

    /**
     * Remove rating of predicted labels
     * @return true when rating was deleted successfully
     */
    public boolean removeRatingOfPredictedLabels(Context domain, String ratingId) {
        File ratingFile = dataRepoService.getRatingOfPredictedClassificationsFile(ratingId, domain);
        return ratingFile.delete();
    }

    /**
     *
     */
    @Async
    public void importClassificationDataset(ClassificationDataset dataset, Context domain, String processId, String userId) throws Exception {
        backgroundProcessService.startProcess(processId, "Import classification dataset", userId);
        backgroundProcessService.updateProcessStatus(processId, "Import classification samples ...");
        for (TextSample sample : dataset.getSamples()) {
            importSample(domain, sample);
        }
        backgroundProcessService.updateProcessStatus(processId, "Import finished");
        backgroundProcessService.stopProcess(processId, domain.getId());
    }

    /**
     * TODO: Does this functionality make sense actually?!
     */
    public void trainClassifier(Classification classification, Answer qna, Context domain) {
        if (false) { // TODO: Make configurable
            TextSample[] samples = new TextSample[1];
            TextSample sample = new TextSample(qna.getUuid(), qna.getAnswer(), classification);
            try {
                importSample(domain, sample);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Classify a text
     * @param domain Domain
     * @param text Text, e.g. "When was Michael born?"
     * @param clientMessageId Foreign message id, e.g. "TODO"
     * @param limit Maximum number of labels returned
     * @param language User / Moderator language
     * @return array of taxonomy terms (e.g. "birthdate", "michael") or classifications
     */
    public PredictedLabelsResponse classifyText(Context domain, String text, String clientMessageId, int limit, String language, User user) throws Exception {
        HitLabel[] labels = predictLabels(domain, text, limit);

        String uuid = dataRepoService.logPredictedLabels(domain, text, clientMessageId, labels, domain.getClassifierImpl());

        PredictedLabelsResponse response = new PredictedLabelsResponse();

        response.setRequestUuid(uuid);
        response.setPredictedLabels(labels);
        response.setClassificationImpl(domain.getClassifierImpl());
        if (domain.getClassifierImpl().equals(ClassificationImpl.LLM)) {
            response.setCompletionConfig(domain.getCompletionConfig(true));
        }
        response.setPredictedLabelsAsTopDeskHtml(getPredictedLabelsAsTopDeskHtml(labels, domain, uuid, language));

        return response;
    }

    /**
     * @param logEntryUUID Log entry UUID (for feedback URLs)
     * @param language User / Moderator language
     */
    private String getPredictedLabelsAsTopDeskHtml(HitLabel[] predictedLabels, Context domain, String logEntryUUID, String language) {
        //ContentType.TEXT_TOPDESK_HTML
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>");
        for (HitLabel hitLabel : predictedLabels) {
            sb.append("<li>" + hitLabel.getLabel().getTerm() + " (Score: " + hitLabel.getScore() + ")</li>");
        }
        sb.append("</ul>");

        String yes = messageSource.getMessage("feedback.yes", null, new Locale(language));
        String no = messageSource.getMessage("feedback.no", null, new Locale(language));
        int tokenValidityInSeconds = 259200; // INFO: 3 days valid, in case of a weekend
        String jwtToken = generateJWTAccessToken("/" + domain.getId() + "/classification/labels", JwtService.SCOPE_READ_LABELS, tokenValidityInSeconds);
        sb.append("<p>" + messageSource.getMessage("labels.helpful", null, new Locale(language)) + "</p><p>" + yes + ": <a href=\"" + labelsHelpfulLink(domain, logEntryUUID) + "\">" + labelsHelpfulLink(domain, logEntryUUID) + "</a></p><p>" + no + ": <a href=\"" + labelsNotHelpfulLink(domain, logEntryUUID, jwtToken) + "\">" + labelsNotHelpfulLink(domain, logEntryUUID, jwtToken) + "</a></p>");

        return Utils.convertHtmlToTOPdeskHtml(sb.toString());
    }

    /**
     * Generate access token for a particular endpoint and scope
     * @param endpoint Rest interface endpoint, e.g. "/similarity-sentences"
     * @param scope Scope of access tokem, e.g. "get-sentence-similarity"
     * @param seconds Token validity in seconds, e.g. 3600 (60 minutes)
     * @return JWT token
     */
    private String generateJWTAccessToken(String endpoint, String scope, long seconds) {
        JWTPayload jwtPayload = new JWTPayload();
        jwtPayload.setIss("Katie");
        HashMap<String, String> claims = new HashMap<String, String>();
        claims.put(jwtService.JWT_CLAIM_ENDPOINT, endpoint);
        claims.put(jwtService.JWT_CLAIM_SCOPE, scope);

        jwtPayload.setPrivateClaims(claims);

        try {
            return jwtService.generateJWT(jwtPayload, seconds, null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @param requestUUID Request UUID, e.g. "54c3222e-bffa-491e-bd63-489f2f6cc3e0"
     */
    private String labelsHelpfulLink(Context domain, String requestUUID) {
        return domain.getHost() + "/#/domain/" + domain.getId() + "/feedback/predicted-labels/" + requestUUID + "/rate?helpful=true";
    }

    /**
     * @param requestUUID Request UUID, e.g. "54c3222e-bffa-491e-bd63-489f2f6cc3e0"
     */
    private String labelsNotHelpfulLink(Context domain, String requestUUID, String jwtToken) {
        String link = domain.getHost() + "/#/domain/" +domain.getId() + "/feedback/predicted-labels/" + requestUUID + "/rate?helpful=false";
        if (jwtToken != null) {
            link = link + "&token=" + jwtToken;
        }
        return link;
    }

    /**
     * Get preferences / ratings of predicted labels of a particular domain
     * @param domain Domain containing preferences / ratings of predicted labels
     * @param getChosen When true, then return ratings where label was not rejected
     * @param getRejected When true, then return ratings where label was rejected
     * @return preferences / ratings of predicted labels
     */
    public HumanPreferenceLabel[] getRatingsOfPredictedLabels(Context domain, boolean getChosen, boolean getRejected) throws Exception {
        log.info("Get chosen labels: " + getChosen);
        log.info("Get rejected labels: " + getRejected);
        List<HumanPreferenceLabel> preferences = new ArrayList<>();

        File ratingsDir = domain.getRatingsOfPredictedLabelsDirectory();
        File[] ratingFiles = ratingsDir.listFiles();
        ObjectMapper mapper = new ObjectMapper();
        if (ratingFiles != null) {
            for (File ratingFile : ratingFiles) {
                HumanPreferenceLabel humanPreference = mapper.readValue(ratingFile, HumanPreferenceLabel.class);
                if (humanPreference.getChosenLabel() != null) {
                    Classification classification = classificationRepoService.getClassification(domain, humanPreference.getChosenLabel().getKatieId());
                    if (classification != null) {
                        humanPreference.getChosenLabel().setId(classification.getId());
                    } else {
                        log.warn("No such label with Katie Id '" + humanPreference.getChosenLabel().getKatieId() + "'! Label probably got deleted.");
                    }
                }
                if (humanPreference.getRejectedLabel() != null) {
                    Classification classification = classificationRepoService.getClassification(domain, humanPreference.getRejectedLabel().getKatieId());
                    if (classification != null) {
                        humanPreference.getRejectedLabel().setId(classification.getId());
                    } else {
                        log.warn("No such label with Katie Id '" + humanPreference.getRejectedLabel().getKatieId() + "'! Label probably got deleted.");
                    }
                }

                if (humanPreference.getRejectedLabel() != null) {
                    log.info("Rejected label: " + humanPreference.getRejectedLabel().getTerm());
                    if (getRejected) {
                        preferences.add(humanPreference);
                    }
                } else {
                    log.info("Chosen label: " + humanPreference.getChosenLabel().getTerm());
                    if (getChosen) {
                        preferences.add(humanPreference);
                    }
                }
            }
        } else {
            log.warn("No preferences / ratings yet.");
        }

        return preferences.toArray(new HumanPreferenceLabel[0]);
    }
}
