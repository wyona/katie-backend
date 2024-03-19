package com.wyona.katie.services;

import com.wyona.katie.handlers.mcc.MulticlassTextClassifier;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifierEmbeddingsCentroidsImpl;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifierMaximumEntropyImpl;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * Predict labels for a text and a particular domain
     * @param domain Domain object
     * @param text Text
     * @return array of suggested labels
     */
    public HitLabel[] predictLabels(Context domain, String text) throws Exception {
        HitLabel[] hitLabels = getClassifier(getClassificationImpl()).predictLabels(domain, text);
        for (HitLabel hitLabel : hitLabels) {
            String labelId = hitLabel.getLabel().getId();
            hitLabel.getLabel().setTerm(classificationRepoService.getLabelName(domain, labelId));
        }
        return hitLabels;
    }

    /**
     * @param trainPercentage How many samples used to train, e.g. 80% (and 20% for testing)
     */
    public void retrain(Context domain, int trainPercentage) throws Exception {
        MulticlassTextClassifier classifier = getClassifier(getClassificationImpl());
        classifier.retrain(domain);
    }

    /**
     * Train classifier with samples (texts and labels)
     */
    public void train(Context domain, TextSample[] samples) throws Exception {
        for (TextSample sample : samples) {
            log.info("Train Sample: Text: " + sample.getText() + ", Class Name / Label: " + sample.getClassification().getTerm() + ", Class Id: " + sample.getClassification().getId());
            try {
                classificationRepoService.saveSample(domain, sample);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        getClassifier(getClassificationImpl()).train(domain, samples);
    }

    /**
     * Get classifier implementation
     */
    public ClassificationImpl getClassificationImpl() {
        // TODO: Make configurable per domain
        return ClassificationImpl.CENTROID_MATCHING;
        //return ClassificationImpl.MAX_ENTROPY;
    }

    /**
     *
     */
    public MulticlassTextClassifier getClassifier(ClassificationImpl impl) {
        if (impl.equals(ClassificationImpl.MAX_ENTROPY)) {
            return classifierMaximumEntropy;
        } else {
            return classifierEmbeddingsCentroid;
        }
    }

    /**
     * Get dataset
     */
    public ClassificationDataset getDataset(Context domain, int offset, int limit) throws Exception {
        return classificationRepoService.getDataset(domain, offset, limit);
    }
}
