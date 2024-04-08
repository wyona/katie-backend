package com.wyona.katie.handlers.mcc;

import com.wyona.katie.models.*;
import com.wyona.katie.services.BackgroundProcessService;
import com.wyona.katie.services.ClassificationRepositoryService;
import com.wyona.katie.services.Utils;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.doccat.*;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * https://opennlp.apache.org/docs/1.7.2/manual/opennlp.html#tools.doccat
 * https://blog.datumbox.com/machine-learning-tutorial-the-max-entropy-text-classifier/
 */
@Slf4j
@Component
public class MulticlassTextClassifierMaximumEntropyImpl implements MulticlassTextClassifier {

    @Autowired
    ClassificationRepositoryService classificationRepoService;

    @Autowired
    BackgroundProcessService backgroundProcessService;

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#predictLabels(Context, String, int)
     */
    public HitLabel[] predictLabels(Context domain, String text, int limit) throws Exception {
        InputStream in = new FileInputStream(getModelFile(domain));
        DoccatModel model = new DoccatModel(in);
        in.close();

        DocumentCategorizerME categorizerME = new DocumentCategorizerME(model);
        String[] texts = new String[1];
        texts[0] = text;
        double[] outcomes = categorizerME.categorize(texts);
        String category = categorizerME.getBestCategory(outcomes);

        HitLabel[] hitLabels = new HitLabel[1];
        hitLabels[0] = new HitLabel(new Classification(null, null, category), -1);

        return hitLabels;
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
        log.info("Retrain ...");
        try {
            // TODO: Replace dataset file creation, by "RepoInputStreamFactory"
            backgroundProcessService.updateProcessStatus(bgProcessId, "Generate dataset file ...");
            createDatasetFileForMaxEntropyClassifier(domain, classificationRepoService.getDataset(domain, false, 0, -1));
            File datasetFile = getDatasetFile(domain);
            ObjectStream<String> lineStream = new PlainTextByLineStream(new MarkableFileInputStreamFactory(datasetFile), StandardCharsets.UTF_8);
            ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

            backgroundProcessService.updateProcessStatus(bgProcessId, "Train classifier ...");
            String textLanguage = "eng"; // TODO
            DoccatModel model = DocumentCategorizerME.train(textLanguage, sampleStream, TrainingParameters.defaultParams(), new DoccatFactory());

            backgroundProcessService.updateProcessStatus(bgProcessId, "Save trained model ...");
            OutputStream out = new BufferedOutputStream(new FileOutputStream(getModelFile(domain)));
            model.serialize(out);
            out.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     *
     */
    private File getModelFile(Context domain) {
        return new File(getClassifierMaxEntDirectory(domain),"max-ent.bin");
    }

    /**
     *
     */
    private File getDatasetFile(Context domain) {
        return new File(getClassifierMaxEntDirectory(domain), "en-docs.txt");
    }

    /**
     *
     */
    private File getClassifierMaxEntDirectory(Context domain) {
        File maxEntClassificationDir = new File(domain.getContextDirectory(),"classifier-max-entropy");
        if (!maxEntClassificationDir.isDirectory()) {
            maxEntClassificationDir.mkdirs();
        }
        return maxEntClassificationDir;
    }

    /**
     * Only temporarily, create dataset file for max entropy classifier
     */
    private void createDatasetFileForMaxEntropyClassifier(Context domain, ClassificationDataset dataset) throws Exception {
        File datasetFile = getDatasetFile(domain);
        FileWriter out = new FileWriter(datasetFile);
        for (TextSample sample : dataset.getSamples()) {
            String line = sample.getClassification().getKatieId() + " " + Utils.replaceNewLines(sample.getText(), " ") + "\n";
            out.write(line);
            out.write(line); // WARN: Duplicate line to generate enough trainig data
        }
        out.close();
    }
}
