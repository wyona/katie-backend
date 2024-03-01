package com.wyona.katie.services;

import com.wyona.katie.ai.models.FloatVector;
import com.wyona.katie.models.*;
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
import java.util.UUID;

/**
 * https://medium.com/@juanc.olamendy/unlocking-the-power-of-text-classification-with-embeddings-7bcbb5912790
 * TODO: Compare with https://ai.google.dev/examples/train_text_classifier_embeddings
 */
@Slf4j
@Component
public class ClassificationServiceEmbeddingsCentroidsImpl implements ClassificationService {

    @Autowired
    private LuceneCodecFactory luceneCodecFactory;

    @Autowired
    private EmbeddingsService embeddingsService;

    @Autowired
    private DataRepositoryService dataRepoService;

    private static final EmbeddingsImpl EMBEDDINGS_IMPL = EmbeddingsImpl.SBERT;

    private static final String UUID_FIELD = "uuid";
    private static final String LABEL_FIELD = "label";
    private static final String VECTOR_FIELD = "vector";

    private static final String SAMPLE_INDEX = "lucene-classifications";
    private static final String CENTROID_INDEX = "lucene-centroids";

    /**
     * @see com.wyona.katie.services.ClassificationService#predictLabels(Context, String) 
     */
    public HitLabel[] predictLabels(Context domain, String text) throws Exception {
        float[] queryVector = embeddingsService.getEmbedding(text, EMBEDDINGS_IMPL, null, EmbeddingType.SEARCH_QUERY, null);

        // TODO: Combine both search results!
        // TODO: centroids can be very close to each other, which means a query vector can be very close to a a wrong centroid and at the same time very close to an invidual sample vector associated with the correct centroid.
        if (true) {
            return searchSimilarSampleVectors(domain, queryVector);
        } else {
            return searchSimilarCentroidVectors(domain, queryVector);
        }
    }

    /**
     * @see com.wyona.katie.services.ClassificationService#getLabels(Context, int, int) 
     */
    public String[] getLabels(Context domain, int offset, int limit) throws Exception {
        return null;
    }

    /**
     * @see com.wyona.katie.services.ClassificationService#train(Context, TextItem[])
     */
    public void train(Context domain, TextItem[] samples) throws Exception {
        for (TextItem sample : samples) {
            log.info("Train Sample: Text: " + sample.getText() + ", Label: " + sample.getLabel());
            try {
                trainSample(domain, sample);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * @param queryVector Embedding vector of text to be classified
     * @return labels of similar sample vectors
     */
    private HitLabel[] searchSimilarSampleVectors(Context domain, float[] queryVector) throws Exception {
        List<HitLabel> labels = new ArrayList<>();
        IndexReader indexReader = DirectoryReader.open(getIndexDirectory(domain, SAMPLE_INDEX));
        IndexSearcher searcher = new IndexSearcher(indexReader);
        int k = 7; // INFO: The number of documents to find
        Query query = new KnnVectorQuery(VECTOR_FIELD, queryVector, k);

        TopDocs topDocs = searcher.search(query, k);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexReader.document(scoreDoc.doc);
            String uuid = doc.get(UUID_FIELD);
            int label = Integer.parseInt(doc.get(LABEL_FIELD));
            log.info("Sample vector found with UUID '" + uuid + "' and confidence score (" + domain.getVectorSimilarityMetric() + ") '" + scoreDoc.score + "'.");

            labels.add(new HitLabel(new Classification(null, label), scoreDoc.score));
        }
        indexReader.close();

        return labels.toArray(new HitLabel[0]);
    }

    /**
     * @param queryVector Embedding vector of text to be classified
     * @return labels of similar centroid vectors
     */
    private HitLabel[] searchSimilarCentroidVectors(Context domain, float[] queryVector) throws Exception {
        List<HitLabel> labels = new ArrayList<>();

        IndexReader indexReader = DirectoryReader.open(getIndexDirectory(domain, CENTROID_INDEX));
        IndexSearcher searcher = new IndexSearcher(indexReader);
        int k = 7; // INFO: The number of documents to find
        Query query = new KnnVectorQuery(VECTOR_FIELD, queryVector, k);

        TopDocs topDocs = searcher.search(query, k);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexReader.document(scoreDoc.doc);
            int label = Integer.parseInt(doc.get(LABEL_FIELD));
            log.info("Centroid vector found with label '" + label + "' and confidence score (" + domain.getVectorSimilarityMetric() + ") '" + scoreDoc.score + "'.");

            labels.add(new HitLabel(new Classification(null, label), scoreDoc.score));
        }
        indexReader.close();

        return labels.toArray(new HitLabel[0]);
    }

    /**
     *
     */
    private void trainSample(Context domain, TextItem sample) throws Exception {
        log.info("Train classification sample ...");

        float[] sampleVector = embeddingsService.getEmbedding(sample.getText(), EMBEDDINGS_IMPL, null, EmbeddingType.SEARCH_QUERY, null);
        String uuid = UUID.randomUUID().toString(); // TODO: Set UUID, either generate one or get from QnA
        indexSampleVector(uuid, "" + sample.getLabel(), sampleVector, domain);

        File embeddingsDir = getEmbeddingsDir(domain, sample.getLabel());
        if (!embeddingsDir.isDirectory()) {
            embeddingsDir.mkdirs();
        }
        File file = new File(embeddingsDir, uuid + ".json");
        // TODO: Check input sequence length and log warning when text is too long:
        //  https://www.sbert.net/examples/applications/computing-embeddings/README.html#input-sequence-length
        //  https://docs.cohere.ai/docs/embeddings#how-embeddings-are-obtained
        dataRepoService.saveEmbedding(sampleVector, sample.getText(), file);

        FloatVector centroid = getCentroid(domain, sample.getLabel());
        // TODO: Centroid will have length less than 1, resp. is not normalized. When using cosine similarity, then this should not be an issue, but otherwise?!
        // TODO: See createFieldType() re similarity metric
        File centroidFile = new File(getLabelDir(domain, sample.getLabel()), "centroid.json");
        dataRepoService.saveEmbedding(centroid.getValues(), "" + sample.getLabel(), centroidFile);
        indexCentroidVector("" + sample.getLabel(), centroid.getValues(), domain);
    }

    /**
     * Add vector of sample text to index
     */
    private void indexSampleVector(String uuid, String label, float[] vector, Context domain) throws Exception {
        IndexWriterConfig iwc = new IndexWriterConfig();
        iwc.setCodec(luceneCodecFactory.getCodec());
        IndexWriter writer = null;
        try {
            writer = new IndexWriter(getIndexDirectory(domain, SAMPLE_INDEX), iwc);
            Document doc = new Document();

            Field uuidField = new StringField(UUID_FIELD, uuid, Field.Store.YES);
            doc.add(uuidField);

            Field labelField = new StringField(LABEL_FIELD, label, Field.Store.YES);
            doc.add(labelField);

            FieldType vectorFieldType = KnnVectorField.createFieldType(vector.length, domain.getVectorSimilarityMetric());
            KnnVectorField vectorField = new KnnVectorField(VECTOR_FIELD, vector, vectorFieldType);
            doc.add(vectorField);

            log.info("Add vector with " + vector.length + " dimensions to Lucene '" + SAMPLE_INDEX + "' index ...");
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
     */
    private void indexCentroidVector(String label, float[] vector, Context domain) throws Exception {
        delete(label, domain, CENTROID_INDEX);

        IndexWriterConfig iwc = new IndexWriterConfig();
        iwc.setCodec(luceneCodecFactory.getCodec());
        IndexWriter writer = null;
        try {
            writer = new IndexWriter(getIndexDirectory(domain, CENTROID_INDEX), iwc);

            Document doc = new Document();

            Field labelField = new StringField(LABEL_FIELD, label, Field.Store.YES);
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
    public boolean delete(String label, Context domain, String indexName) throws Exception {

        try {
            IndexReader reader = DirectoryReader.open(getIndexDirectory(domain, indexName));
            int numberOfDocsBeforeDeleting = reader.numDocs();
            log.info("Number of documents: " + numberOfDocsBeforeDeleting);
            //log.info("Number of deleted documents: " + reader.numDeletedDocs());
            reader.close();

            log.info("Delete documents with label '" + label + "' from index '" + indexName + "' of domain '" + domain.getId() + "' ...");
            IndexWriterConfig iwc = new IndexWriterConfig();
            //iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            iwc.setCodec(luceneCodecFactory.getCodec());

            IndexWriter writer = null;
            try {
                writer = new IndexWriter(getIndexDirectory(domain, indexName), iwc);
                Term term = new Term(LABEL_FIELD, label);
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
    private FloatVector getCentroid(Context domain, int label) throws Exception {
        File[] embeddingFiles = getEmbeddingsDir(domain, label).listFiles();
        List<FloatVector> embeddings = new ArrayList<>();
        for (File file : embeddingFiles) {
            FloatVector embedding = new FloatVector(dataRepoService.readEmbedding(file));
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
        File indexDir = new File(domain.getContextDirectory(), indexName);
        if (!indexDir.isDirectory()) {
            indexDir.mkdirs();
        }
        String indexPath = indexDir.getAbsolutePath();
        return FSDirectory.open(Paths.get(indexPath));
    }

    /**
     *
     */
    private File getLabelDir(Context domain, int label) {
        return new File(domain.getContextDirectory(),"classifications/" + label);
    }

    /**
     *
     */
    private File getEmbeddingsDir(Context domain, int label) {
        return new File(getLabelDir(domain, label),"embeddings");
    }
}
