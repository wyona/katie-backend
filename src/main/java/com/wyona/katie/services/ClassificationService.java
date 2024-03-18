package com.wyona.katie.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.ai.models.TextEmbedding;
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
    private MulticlassTextClassifierEmbeddingsCentroidsImpl classifier;

    @Autowired
    private MulticlassTextClassifierMaximumEntropyImpl classifierMaximumEntropy;

    /**
     * Predict labels for a text and a particular domain
     * @param domain Domain object
     * @param text Text
     * @return array of suggested labels
     */
    public HitLabel[] predictLabels(Context domain, String text) throws Exception {
        HitLabel[] hitLabels = classifier.predictLabels(domain, text);
        for (HitLabel hitLabel : hitLabels) {
            String labelId = hitLabel.getLabel().getId();
            hitLabel.getLabel().setTerm(getLabelName(domain, labelId));
        }
        return hitLabels;
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

        classifier.train(domain, samples);
    }

    /**
     * Get classifier implementation
     */
    public ClassificationImpl getClassificationImpl() {
        return ClassificationImpl.CENTROID_MATCHING;
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

                File samplesDir = getEmbeddingsDir(domain, labelId);
                File[] embeddingFiles = samplesDir.listFiles();
                classification.setFrequency(embeddingFiles.length);
                log.debug(classification.getFrequency() + " samples exists for classification '" + classification.getTerm() + "' / " + labelId);
                for (File embeddingFile : embeddingFiles) {
                    TextEmbedding textEmbedding = dataRepoService.readEmbedding(embeddingFile);
                    String sampleId = embeddingFile.getName().substring(0, embeddingFile.getName().indexOf(".json"));
                    TextSample sample = new TextSample(sampleId, textEmbedding.getText(), classification);
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

        // TODO: Save sample
        log.info("TODO: Save sample ...");
    }

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
    private File getEmbeddingsDir(Context domain, String classId) {
        return new File(getLabelDir(domain, classId),"embeddings");
    }
}
