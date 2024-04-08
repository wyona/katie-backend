package com.wyona.katie.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 *
 */
@Slf4j
@Component
public class ClassificationRepositoryService {

    @Autowired
    private DataRepositoryService dataRepoService;

    /**
     * Get dataset (labels and samples)
     * @param domain
     * @param labelsOnly When set to true, then return only labels and no samples
     * @param offset Offset of returned samples
     * @param limit Limit of returned samples
     */
    public ClassificationDataset getDataset(Context domain, boolean labelsOnly, int offset, int limit) throws Exception {
        log.info("Get classification dataset of domain '" + domain.getId() + "' ...");
        ClassificationDataset dataset = new ClassificationDataset(domain.getName());

        File classifcationsDir = getClassifcationsDir(domain);
        if (!classifcationsDir.isDirectory()) {
            log.warn("No classifications dataset imported yet!");
            return dataset;
        }

        File[] dirs = classifcationsDir.listFiles();

        for (File labelDir : dirs) {
            if (labelDir.isDirectory()) {
                String labelKatieId = labelDir.getName();
                Classification classification = getClassification(domain, labelKatieId);

                File samplesDir = getSamplesDir(domain, labelKatieId);
                File[] samplesFiles = samplesDir.listFiles();
                classification.setFrequency(samplesFiles.length);
                log.debug(classification.getFrequency() + " samples exists for classification '" + classification.getTerm() + "' / " + classification.getId() + " / " + classification.getKatieId());

                if (!labelsOnly) {
                    for (File sampleFile : samplesFiles) {
                        String sampleText = readSampleText(sampleFile);
                        String sampleId = sampleFile.getName().substring(0, sampleFile.getName().indexOf(".json"));
                        TextSample sample = new TextSample(sampleId, sampleText, classification);
                        dataset.addSample(sample);
                    }
                }

                dataset.addLabel(classification);
            }
        }

        return dataset;
    }

    /**
     *
     */
    public void saveSample(Context domain, TextSample sample) throws Exception {
        String katieId = getLabelKatieId(sample.getClassification().getId());

        sample.getClassification().setKatieId(katieId);

        File labelDir = getLabelDir(domain, katieId);
        if (!labelDir.isDirectory()) {
            labelDir.mkdirs();
            File labelFile = getLabelFile(domain, katieId);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(labelFile, sample.getClassification());
        }

        log.info("Save sample '" + sample.getId() + "' ...");
        File samplesDir = getSamplesDir(domain, katieId);
        if (!samplesDir.isDirectory()) {
            samplesDir.mkdirs();
        }
        File sampleFile = getSampleFile(domain, katieId, sample.getId());
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(sampleFile, sample);
;    }

    /**
     * @param labelKatieId Label Id assigned by Katie, e.g. "64e3bb24-1522-4c49-8f82-f99b34a82062"
     * @return label name, e.g. "Managed Device Services, MacOS Clients"
     */
    public Classification getClassification(Context domain, String labelKatieId) {
        File metaFile = getLabelFile(domain, labelKatieId);
        if (metaFile.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                Classification classification = mapper.readValue(metaFile, Classification.class);
                return classification;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        log.error("No such classification: " + metaFile.getAbsolutePath());
        return null;
    }


    /**
     *
     */
    private String readSampleText(File sampleFile) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TextSample sample = mapper.readValue(sampleFile, TextSample.class);
        return sample.getText();
    }

    /**
     *
     */
    private File getClassifcationsDir(Context domain) {
        return new File(domain.getClassificationsDirectory(),"classifications-dataset");
    }

    /**
     * @param labelKatieId Label Id assigned by Katie
     */
    private File getLabelDir(Context domain, String labelKatieId) {
        return new File(getClassifcationsDir(domain), labelKatieId);
    }

    /**
     * @param labelKatieId Label Id assigned by Katie
     * @return file containing information about label
     */
    private File getLabelFile(Context domain, String labelKatieId) {
        return new File(getLabelDir(domain, labelKatieId), "meta.json");
    }

    /**
     * @param labelKatieId Label Id assigned by Katie
     */
    private File getSamplesDir(Context domain, String labelKatieId) {
        return new File(getLabelDir(domain, labelKatieId),"samples");
    }

    /**
     * @param labelKatieId Label Id assigned by Katie
     */
    private File getSampleFile(Context domain, String labelKatieId, String sampleId) {
        return new File(getSamplesDir(domain, labelKatieId), sampleId + ".json");
    }

    /**
     *
     */
    private String getLabelKatieId(String labelForeignId) {
        return "" + labelForeignId.hashCode();
    }
}
