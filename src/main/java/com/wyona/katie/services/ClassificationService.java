package com.wyona.katie.services;

import com.wyona.katie.handlers.mcc.MulticlassTextClassifier;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifierEmbeddingsCentroidsImpl;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifierLLMImpl;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifierMaximumEntropyImpl;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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
        HitLabel[] hitLabels = getClassifier(domain.getClassifierImpl()).predictLabels(domain, text, limit);
        for (HitLabel hitLabel : hitLabels) {
            String labelKatieId = hitLabel.getLabel().getKatieId();
            Classification classification = classificationRepoService.getClassification(domain, labelKatieId);
            hitLabel.getLabel().setTerm(classification.getTerm());
            hitLabel.getLabel().setId(classification.getId());
        }
        return hitLabels;
    }

    /**
     * @param trainPercentage How many samples used to train, e.g. 80% (and 20% for testing)
     * @param bgProcessId Background process Id
     */
    @Async
    public void retrain(Context domain, int trainPercentage, String bgProcessId, String userId) {
        backgroundProcessService.startProcess(bgProcessId, "Retrain classifier '" + domain.getClassifierImpl() + "' for domain '" + domain.getId() + "'.", userId);
        MulticlassTextClassifier classifier = getClassifier(domain.getClassifierImpl());
        try {
            classifier.retrain(domain, bgProcessId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            backgroundProcessService.updateProcessStatus(bgProcessId, e.getMessage(), BackgroundProcessStatusType.ERROR);
        }
        backgroundProcessService.stopProcess(bgProcessId);
    }

    /**
     * Import classification sample / observation (text and label)
     */
    public void importSample(Context domain, TextSample sample) throws Exception {
        classificationRepoService.saveSample(domain, sample);
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
     * Get dataset
     * @param domain
     * @param labelsOnly When set to true, then return only labels and no samples
     * @param offset Offset of returned samples
     * @param limit Limit of returned samples
     */
    public ClassificationDataset getDataset(Context domain, boolean labelsOnly, int offset, int limit) throws Exception {
        return classificationRepoService.getDataset(domain, labelsOnly, offset, limit);
    }
}
