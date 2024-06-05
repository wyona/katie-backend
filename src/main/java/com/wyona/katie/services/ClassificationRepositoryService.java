package com.wyona.katie.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 */
@Slf4j
@Component
public class ClassificationRepositoryService {

    @Autowired
    private DataRepositoryService dataRepoService;

    private final static String SEPARATOR = ",";

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
        String katieId = getOrAddLabelKatieId(domain, sample.getClassification().getId());

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
     * Remove classification / label from training dataset
     */
    public void removeClassification(Context domain, Classification classification) throws Exception {
        File samplesDir = getSamplesDir(domain, classification.getKatieId());
        // TODO: Return removed sample IDs

        File labelDir = getLabelDir(domain, classification.getKatieId());
        FileUtils.deleteDirectory(labelDir);

        removeLabelKatieId(domain, classification.getKatieId());
    }

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
     * Get or add Katie label Id for a particular foreign Id
     * @param foreignId Foreign Id, e.g. "64e3bb24-1522-4c49-8f82-f99b34a82062" or "https://jena.apache.org/2f61f866-bcd8-4db3-833b-37e6f7877e52"
     * @return Katie Id, e.g. "abb6edd3-34a9-4a84-b12a-13d5dfd8152f"
     */
    private String getOrAddLabelKatieId(Context domain, String foreignId) throws Exception {
        String labelId = null;

        File idTableFile = getIdTableFile(domain);
        List<String> entries = new ArrayList<>();
        if (idTableFile.isFile()) {
            entries = readIdTable(idTableFile);
            labelId = labelIdAlreadyExists(foreignId, entries);
            if (labelId != null) {
                return labelId;
            }
        }

        labelId = UUID.randomUUID().toString();
        entries.add(labelId + SEPARATOR + foreignId);
        writeIdTable(idTableFile, entries);

        return labelId;
    }

    /**
     *
     */
    private void removeLabelKatieId(Context domain, String labelKatieId) throws Exception {
        File idTableFile = getIdTableFile(domain);
        if (idTableFile.isFile()) {
            List<String> entries = readIdTable(idTableFile);
            List<String> newEntries = new ArrayList<>();
            for (String entry : entries) {
                String[] katieId_foreignId = entry.split(SEPARATOR);
                if (katieId_foreignId[0].equals(labelKatieId)) {
                    log.info("Drop entry '" + entry + "'.");
                } else {
                    newEntries.add(entry);
                }
            }

            writeIdTable(idTableFile, newEntries);
        } else {
            log.error("No such file '" + idTableFile.getAbsolutePath() + "'!");
        }
    }

    /**
     *
     */
    private File getIdTableFile(Context domain) {
        File classifcationsDir = domain.getClassificationsDirectory();
        if (!classifcationsDir.isDirectory()) {
            classifcationsDir.mkdirs();
        }

        File idTableFile = new File(classifcationsDir, "id-table.txt");

        return idTableFile;
    }

    /**
     * Get Katie label Id for a particular foreign Id
     * @param foreignId Foreign Id
     * @param entries List of entries (Entry example "83d0ba12-9c3b-483e-bfca-48303bb26c37,eaef049b-2cbf-4a16-835e-dd7769dc99ca_402c0ad2-acd5-4025-b6ae-bdf3ffec6fbc")
     * @return Katie id if it already exists and otherwise null
     */
    private String labelIdAlreadyExists(String foreignId, List<String> entries) {
        for (String entry : entries) {
            String[] katieId_foreignId = entry.split(SEPARATOR);
            if (katieId_foreignId[1].equals(foreignId)) {
                return katieId_foreignId[0];
            }
        }
        return null;
    }

    /**
     *
     */
    private List<String> readIdTable(File idTableFile) throws Exception {
        List<String> entries = new ArrayList<>();

        FileInputStream in = new FileInputStream(idTableFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        while((line = br.readLine()) != null) {
            entries.add(line);
        }
        br.close();
        in.close();

        return entries;
    }

    /**
     *
     */
    private void writeIdTable(File idTableFile, List<String> entries) throws Exception {
        FileOutputStream out = new FileOutputStream(idTableFile);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
        for (String entry : entries) {
            bw.write(entry);
            bw.newLine();
        }
        bw.close();
        out.close();
    }
}
