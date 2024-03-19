package com.wyona.katie.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifier;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifierEmbeddingsCentroidsImpl;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifierMaximumEntropyImpl;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * https://medium.com/@juanc.olamendy/unlocking-the-power-of-text-classification-with-embeddings-7bcbb5912790
 * TODO: Compare with https://ai.google.dev/examples/train_text_classifier_embeddings
 */
@Slf4j
@Component
public class ClassificationService {

    @Autowired
    private DataRepositoryService dataRepoService;

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
            hitLabel.getLabel().setTerm(getLabelName(domain, labelId));
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
                saveSample(domain, sample);
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
        log.info("Get classification dataset of domain '" + domain.getId() + "' ...");
        File classifcationsDir = getClassifcationsDir(domain);
        File[] dirs = classifcationsDir.listFiles();

        ClassificationDataset dataset = new ClassificationDataset(domain.getName());
        for (File labelDir : dirs) {
            if (labelDir.isDirectory()) {
                String labelId = labelDir.getName();
                Classification classification = new Classification(getLabelName(domain, labelId), labelId);

                File samplesDir = getSamplesDir(domain, labelId);
                File[] samplesFiles = samplesDir.listFiles();
                classification.setFrequency(samplesFiles.length);
                log.debug(classification.getFrequency() + " samples exists for classification '" + classification.getTerm() + "' / " + labelId);
                for (File sampleFile : samplesFiles) {
                    String sampleText = readSampleText(sampleFile);
                    String sampleId = sampleFile.getName().substring(0, sampleFile.getName().indexOf(".json"));
                    TextSample sample = new TextSample(sampleId, sampleText, classification);
                    dataset.addSample(sample);
                }

                dataset.addLabel(classification);
            }
        }

        return dataset;
    }

    /**
     *
     */
    private void saveSample(Context domain, TextSample sample) throws Exception {
        File labelDir = getLabelDir(domain, sample.getClassification().getId());
        if (!labelDir.isDirectory()) {
            labelDir.mkdirs();
            File metaFile = getMetaFile(domain, sample.getClassification().getId());
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(metaFile, sample.getClassification());
        }

        log.info("Save sample '" + sample.getId() + "' ...");
        File samplesDir = getSamplesDir(domain, sample.getClassification().getId());
        if (!samplesDir.isDirectory()) {
            samplesDir.mkdirs();
        }
        File sampleFile = getSampleFile(domain, sample.getClassification().getId(), sample.getId());
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(sampleFile, sample);
;    }

    /**
     * @param labelUuid Label ID, e.g. "64e3bb24-1522-4c49-8f82-f99b34a82062"
     * @return label name, e.g. "Managed Device Services, MacOS Clients"
     */
    private String getLabelName(Context domain, String labelUuid) {
        File metaFile = getMetaFile(domain, labelUuid);
        if (metaFile.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                Classification classification = mapper.readValue(metaFile, Classification.class);
                return classification.getTerm();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        return "No class name available";
    }

    /**
     *
     */
    private File getClassifcationsDir(Context domain) {
        return new File(domain.getContextDirectory(),"classifications");
    }

    /**
     *
     */
    private File getLabelDir(Context domain, String classId) {
        return new File(getClassifcationsDir(domain), classId);
    }

    /**
     *
     */
    private File getMetaFile(Context domain, String classId) {
        return new File(getLabelDir(domain, classId), "meta.json");
    }

    /**
     *
     */
    private File getSamplesDir(Context domain, String classId) {
        return new File(getLabelDir(domain, classId),"samples");
    }

    /**
     *
     */
    private File getSampleFile(Context domain, String classId, String sampleId) {
        return new File(getSamplesDir(domain, classId), sampleId + ".json");
    }

    /**
     *
     */
    private String readSampleText(File sampleFile) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TextSample sample = mapper.readValue(sampleFile, TextSample.class);
        return sample.getText();
    }
}
