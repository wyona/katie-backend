package com.wyona.katie.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifier;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifierEmbeddingsCentroidsImpl;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifierLLMImpl;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifierMaximumEntropyImpl;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
     * Retrain classifier, using existing samples and with new samples from human preferences dataset
     * @param preferences Optional human preferences
     * @param trainPercentage How many samples used to train, e.g. 80% (and 20% for testing)
     * @param bgProcessId Background process Id
     */
    @Async
    public void retrain(Context domain, List<HumanPreferenceLabel> preferences, int trainPercentage, String bgProcessId, String userId) {
        backgroundProcessService.startProcess(bgProcessId, "Retrain classifier '" + domain.getClassifierImpl() + "' for domain '" + domain.getId() + "'.", userId);

        MulticlassTextClassifier classifier = getClassifier(domain.getClassifierImpl());

        if (preferences != null && preferences.size() > 0) {
            backgroundProcessService.updateProcessStatus(bgProcessId, "Enhance training dataset using preference dataset ...");
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
     * Get preferences / ratings of predicted labels of a particular domain
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
