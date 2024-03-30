package com.wyona.katie.handlers.mcc;

import com.wyona.katie.models.*;
import com.wyona.katie.services.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * https://medium.com/@juanc.olamendy/unlocking-the-power-of-text-classification-with-embeddings-7bcbb5912790
 * TODO: Compare with https://ai.google.dev/examples/train_text_classifier_embeddings
 */
@Slf4j
@Component
public class MulticlassTextClassifierEmbeddingsCentroidsImpl implements MulticlassTextClassifier {

    @Autowired
    private LuceneCodecFactory luceneCodecFactory;

    @Autowired
    private EmbeddingsService embeddingsService;

    @Autowired
    private DataRepositoryService dataRepoService;

    @Autowired
    private ClassificationRepositoryService classificationRepoService;

    @Autowired
    private BackgroundProcessService backgroundProcessService;

    private static final EmbeddingsImpl EMBEDDINGS_IMPL = EmbeddingsImpl.SBERT;
    private static final EmbeddingValueType VECTOR_VALUE_TYPE = EmbeddingValueType.float32;

    private static final String UUID_FIELD = "uuid";
    private static final String LABEL_UUID_FIELD = "label";
    private static final String VECTOR_FIELD = "vector";

    private static final String SAMPLE_INDEX = "lucene-samples";
    private static final String CENTROID_INDEX = "lucene-centroids";

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#predictLabels(Context, String, int)
     */
    public HitLabel[] predictLabels(Context domain, String text, int limit) throws Exception {
        Vector queryVector = embeddingsService.getEmbedding(text, EMBEDDINGS_IMPL, null, EmbeddingType.SEARCH_QUERY, VECTOR_VALUE_TYPE, null);

        // TODO: Consider to combine both search results!
        // TODO: centroids can be very close to each other, which means a query vector can be very close to a a wrong centroid and at the same time very close to an invidual sample vector associated with the correct centroid.
        if (true) {
            log.info("Predict labels for text associated with domain '" + domain.getId() + "' by finding similar samples ...");
            return searchSimilarSampleVectors(domain, queryVector, limit);
        } else {
            log.info("Predict labels for text associated with domain '" + domain.getId() + "' by finding similar centroids ...");
            return searchSimilarCentroidVectors(domain, queryVector, limit);
        }
    }

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#train(Context, TextSample[])
     */
    public void train(Context domain, TextSample[] samples) throws Exception {
        train(domain, samples, null);
    }

    /**
     *
     */
    private void train(Context domain, TextSample[] samples, String bgProcessId) throws Exception {
        int counter = 0;
        final int BATCH_SIZE = 100;
        for (TextSample sample : samples) {
            log.info("Train Sample: Text: " + sample.getText() + ", Class Name / Label: " + sample.getClassification().getTerm() + ", Class Id: " + sample.getClassification().getId());
            try {
                trainSample(domain, sample);
                counter++;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            // INFO: Log progress
            if (counter % BATCH_SIZE == 0) {
                backgroundProcessService.updateProcessStatus(bgProcessId, counter + " samples trained, " + (samples.length - counter) + " samples remaining");
            }
        }
    }

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#retrain(Context, String)
     */
    public void retrain(Context domain, String bgProcessId) throws Exception {
        log.info("Retrain ...");
        backgroundProcessService.updateProcessStatus(bgProcessId, "Load dataset ...");
        ClassificationDataset dataset = classificationRepoService.getDataset(domain, 0, -1);

        backgroundProcessService.updateProcessStatus(bgProcessId, "Train classifier ...");
        train(domain, dataset.getSamples(), bgProcessId);
        backgroundProcessService.updateProcessStatus(bgProcessId, "Training of classifier finished.");
    }

    /**
     * @param queryVector Embedding vector of text to be classified
     * @return labels of similar sample vectors
     */
    private HitLabel[] searchSimilarSampleVectors(Context domain, Vector queryVector, int limit) throws Exception {
        List<HitLabel> labels = new ArrayList<>();
        IndexReader indexReader = DirectoryReader.open(getIndexDirectory(domain, SAMPLE_INDEX));
        IndexSearcher searcher = new IndexSearcher(indexReader);
        // TODO: k should be greater than limit
        int k = 2 * limit; // INFO: The number of documents to find
        Query query = new KnnVectorQuery(VECTOR_FIELD, ((FloatVector)queryVector).getValues(), k);

        TopDocs topDocs = searcher.search(query, limit);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexReader.document(scoreDoc.doc);
            String uuid = doc.get(UUID_FIELD);
            String labelUuid = doc.get(LABEL_UUID_FIELD);
            log.info("Sample vector found with UUID '" + uuid + "' and confidence score (" + domain.getVectorSimilarityMetric() + ") '" + scoreDoc.score + "'.");

            labels.add(new HitLabel(new Classification(null, labelUuid), scoreDoc.score));
        }
        indexReader.close();

        return labels.toArray(new HitLabel[0]);
    }

    /**
     * @param queryVector Embedding vector of text to be classified
     * @return labels of similar centroid vectors
     */
    private HitLabel[] searchSimilarCentroidVectors(Context domain, Vector queryVector, int limit) throws Exception {
        List<HitLabel> labels = new ArrayList<>();

        IndexReader indexReader = DirectoryReader.open(getIndexDirectory(domain, CENTROID_INDEX));
        IndexSearcher searcher = new IndexSearcher(indexReader);
        // TODO: k should be greater than limit
        int k = 2 * limit; // INFO: The number of documents to find
        Query query = new KnnVectorQuery(VECTOR_FIELD, ((FloatVector)queryVector).getValues(), k);

        TopDocs topDocs = searcher.search(query, limit);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexReader.document(scoreDoc.doc);
            String labelUuid = doc.get(LABEL_UUID_FIELD);
            log.info("Centroid vector found with label ID '" + labelUuid + "' and confidence score (" + domain.getVectorSimilarityMetric() + ") '" + scoreDoc.score + "'.");

            labels.add(new HitLabel(new Classification(null, labelUuid), scoreDoc.score));
        }
        indexReader.close();

        return labels.toArray(new HitLabel[0]);
    }

    /**
     * Train classification sample
     */
    private void trainSample(Context domain, TextSample sample) throws Exception {
        if (sample.getClassification().getId() == null) {
            log.warn("No class ID available for class name '" + sample.getClassification().getTerm() + "', therefore do not train classifier!");
            return;
        }

        log.info("Train classification sample ...");

        String classId = sample.getClassification().getId();

        Vector sampleVector = embeddingsService.getEmbedding(sample.getText(), EMBEDDINGS_IMPL, null, EmbeddingType.SEARCH_QUERY, VECTOR_VALUE_TYPE, null);
        indexSampleVector(sample.getId(), classId, sampleVector, domain);

        File embeddingFile = getEmbeddingFile(domain, classId, sample.getId());
        // TODO: Check input sequence length and log warning when text is too long:
        //  https://www.sbert.net/examples/applications/computing-embeddings/README.html#input-sequence-length
        //  https://docs.cohere.ai/docs/embeddings#how-embeddings-are-obtained
        dataRepoService.saveEmbedding(sampleVector, sample.getText(), embeddingFile);

        FloatVector centroid = getCentroid(domain, classId);
        // TODO: Centroid will have length less than 1, resp. is not normalized. When using cosine similarity, then this should not be an issue, but otherwise?!
        // TODO: See createFieldType() re similarity metric
        File centroidFile = getCentroidFile(domain, classId);
        dataRepoService.saveEmbedding(centroid, "" + classId, centroidFile);
        indexCentroidVector("" + classId, centroid.getValues(), domain);
    }

    /**
     * Add vector of sample text to index
     * @param labelUuid UUID of label, e.g. "64e3bb24-1522-4c49-8f82-f99b34a82062"
     */
    private void indexSampleVector(String uuid, String labelUuid, Vector vector, Context domain) throws Exception {
        IndexWriterConfig iwc = new IndexWriterConfig();
        iwc.setCodec(luceneCodecFactory.getCodec(VECTOR_VALUE_TYPE));
        IndexWriter writer = null;
        try {
            writer = new IndexWriter(getIndexDirectory(domain, SAMPLE_INDEX), iwc);
            Document doc = new Document();

            Field uuidField = new StringField(UUID_FIELD, uuid, Field.Store.YES);
            doc.add(uuidField);

            Field labelField = new StringField(LABEL_UUID_FIELD, labelUuid, Field.Store.YES);
            doc.add(labelField);

            FieldType vectorFieldType = KnnVectorField.createFieldType(vector.getDimension(), domain.getVectorSimilarityMetric());
            // TODO: Use KnnFloatVectorField
            KnnVectorField vectorField = new KnnVectorField(VECTOR_FIELD, ((FloatVector)vector).getValues(), vectorFieldType);
            doc.add(vectorField);

            log.info("Add vector with " + vector.getDimension() + " dimensions to Lucene '" + SAMPLE_INDEX + "' index ...");
            writer.addDocument(doc);

            // writer.forceMerge(1);
            writer.close();
        } catch (Exception e) {
            closeIndexWriter(writer);
            throw e;
        }
    }

    /**
     * Add vector of centroid to index
     * @param labelUuid UUID of label
     */
    private void indexCentroidVector(String labelUuid, float[] vector, Context domain) throws Exception {
        delete(labelUuid, domain, CENTROID_INDEX);

        IndexWriterConfig iwc = new IndexWriterConfig();
        iwc.setCodec(luceneCodecFactory.getCodec(VECTOR_VALUE_TYPE));
        IndexWriter writer = null;
        try {
            writer = new IndexWriter(getIndexDirectory(domain, CENTROID_INDEX), iwc);

            Document doc = new Document();

            Field labelField = new StringField(LABEL_UUID_FIELD, labelUuid, Field.Store.YES);
            doc.add(labelField);

            FieldType vectorFieldType = KnnVectorField.createFieldType(vector.length, domain.getVectorSimilarityMetric());
            KnnVectorField vectorField = new KnnVectorField(VECTOR_FIELD, vector, vectorFieldType);
            doc.add(vectorField);

            log.info("Add vector with " + vector.length + " dimensions to Lucene index '" + CENTROID_INDEX + "' ...");
            writer.addDocument(doc);

            // writer.forceMerge(1);
            writer.close();
        } catch (Exception e) {
            closeIndexWriter(writer);
            throw e;
        }
    }

    /**
     * Delete vectors associated with a particular label
     */
    public boolean delete(String labelUuid, Context domain, String indexName) throws Exception {
        File indexDir = new File(getClassifierDir(domain), indexName);
        if (!indexDir.isDirectory()) {
            log.info("Index '" + indexName + "' does not exist yet, therefore we cannot delete an entry.");
            return false;
        }

        try {
            IndexReader reader = DirectoryReader.open(getIndexDirectory(domain, indexName));
            int numberOfDocsBeforeDeleting = reader.numDocs();
            log.info("Number of documents: " + numberOfDocsBeforeDeleting);
            //log.info("Number of deleted documents: " + reader.numDeletedDocs());
            reader.close();

            log.info("Delete documents with label ID '" + labelUuid + "' from index '" + indexName + "' of domain '" + domain.getId() + "' ...");
            IndexWriterConfig iwc = new IndexWriterConfig();
            //iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            iwc.setCodec(luceneCodecFactory.getCodec(VECTOR_VALUE_TYPE));

            IndexWriter writer = null;
            try {
                writer = new IndexWriter(getIndexDirectory(domain, indexName), iwc);
                Term term = new Term(LABEL_UUID_FIELD, labelUuid);
                writer.deleteDocuments(term);
                // writer.forceMerge(1);
                writer.close();
            } catch (Exception e){
                closeIndexWriter(writer);
                throw e;
            }

            reader = DirectoryReader.open(getIndexDirectory(domain, indexName));
            int numberOfDocsAfterDeleting = reader.numDocs();
            log.info("Number of documents: " + numberOfDocsAfterDeleting);
            log.info("Number of deleted documents: " + (numberOfDocsBeforeDeleting - numberOfDocsAfterDeleting));
            // TODO: Not sure whether the method numDeletedDocs() makes sense here
            //log.info("Number of deleted documents: " + reader.numDeletedDocs());
            reader.close();

            return true;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get centroid for a particular label
     */
    private FloatVector getCentroid(Context domain, String labelUuid) throws Exception {
        File[] embeddingFiles = getEmbeddingsDir(domain, labelUuid).listFiles();
        List<FloatVector> embeddings = new ArrayList<>();
        for (File file : embeddingFiles) {
            FloatVector embedding = new FloatVector(dataRepoService.readEmbedding(file).getVector());
            embeddings.add(embedding);
        }
        return UtilsService.getCentroid(embeddings.toArray(new FloatVector[0]));
    }

    /**
     *
     */
    private void closeIndexWriter(IndexWriter writer) {
        if (writer != null) {
            log.error("Something went wrong, but always make sure to close index writer ...");
            try {
                writer.close();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * @param indexName Name of index, e.g. "lucene-classifications"
     */
    private Directory getIndexDirectory(Context domain, String indexName) throws Exception {
        File indexDir = new File(getClassifierDir(domain), indexName);
        if (!indexDir.isDirectory()) {
            indexDir.mkdirs();
        }
        String indexPath = indexDir.getAbsolutePath();
        return FSDirectory.open(Paths.get(indexPath));
    }

    /**
     *
     */
    private File getClassifierDir(Context domain) {
        File classifierDir = new File(domain.getClassificationsDirectory(),"classifier-embeddings-centroid");
        if (!classifierDir.isDirectory()) {
            classifierDir.mkdirs();
        }
        return classifierDir;
    }

    /**
     *
     */
    private File getLabelDir(Context domain, String classId) {
        File labelDir = new File(getClassifierDir(domain), "labels/" + classId);
        if (!labelDir.isDirectory()) {
            labelDir.mkdirs();
        }
        return labelDir;
    }

    /**
     *
     */
    private File getEmbeddingsDir(Context domain, String classId) {
        File embeddingsDir = new File(getLabelDir(domain, classId),"embeddings");
        if (!embeddingsDir.isDirectory()) {
            embeddingsDir.mkdirs();
        }

        return embeddingsDir;
    }

    /**
     *
     */
    private File getEmbeddingFile(Context domain, String classId, String sampleId) {
        return new File(getEmbeddingsDir(domain, classId), sampleId + ".json");
    }

    /**
     *
     */
    private File getCentroidFile(Context domain, String classId) {
        return new File(getLabelDir(domain, classId), "centroid.json");
    }
}
