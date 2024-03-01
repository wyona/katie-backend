package com.wyona.katie.services;

import com.wyona.katie.ai.models.FloatVector;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
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

    /**
     * @see com.wyona.katie.services.ClassificationService#predictLabels(Context, String) 
     */
    public String[] predictLabels(Context domain, String text) throws Exception {
        List<String> labels = new ArrayList<String>();

        float[] queryVector = embeddingsService.getEmbedding(text, EMBEDDINGS_IMPL, null, EmbeddingType.SEARCH_QUERY, null);
        int k = 7; // INFO: The number of documents to find

        IndexReader indexReader = DirectoryReader.open(getIndexDirectory(domain));
        IndexSearcher searcher = new IndexSearcher(indexReader);
        Query query = new KnnVectorQuery(VECTOR_FIELD, queryVector, k);

        TopDocs topDocs = searcher.search(query, k);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexReader.document(scoreDoc.doc);
            String uuid = doc.get(UUID_FIELD);
            int label = Integer.parseInt(doc.get(LABEL_FIELD));
            log.info("Vector found with UUID '" + uuid + "' and confidence score (" + domain.getVectorSimilarityMetric() + ") '" + scoreDoc.score + "'.");

            labels.add("" + label);
        }
        indexReader.close();

        return labels.toArray(new String[0]);
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
     *
     */
    public void trainSample(Context domain, TextItem sample) throws Exception {
        log.info("Train classification sample ...");

        File embeddingsDir = new File(domain.getContextDirectory(),"classifications/" + sample.getLabel() + "/embeddings/");
        if (!embeddingsDir.isDirectory()) {
            embeddingsDir.mkdirs();
        }
        String uuid = UUID.randomUUID().toString(); // TODO: Set UUID, either generate one or get from QnA
        File file = new File(embeddingsDir, uuid + ".json");
        // TODO: Check input sequence length and log warning when text is too long:
        //  https://www.sbert.net/examples/applications/computing-embeddings/README.html#input-sequence-length
        //  https://docs.cohere.ai/docs/embeddings#how-embeddings-are-obtained
        float[] vector = embeddingsService.getEmbedding(sample.getText(), EMBEDDINGS_IMPL, null, EmbeddingType.SEARCH_QUERY, null);
        dataRepoService.saveEmbedding(vector, sample.getText(), file);
        FloatVector centroid = getCentroid(domain, sample.getLabel());
        log.info("Centroid: " + centroid);

        // TODO: Re-index centroid instead sample vector

        indexVector(uuid, "" + sample.getLabel(), vector, domain);
    }

    /**
     * Add vector to index
     */
    private void indexVector(String uuid, String label, float[] vector, Context domain) throws Exception {
        IndexWriterConfig iwc = new IndexWriterConfig();
        iwc.setCodec(luceneCodecFactory.getCodec());
        IndexWriter writer = null;
        try {
            writer = new IndexWriter(getIndexDirectory(domain), iwc);
            Document doc = new Document();

            Field uuidField = new StringField(UUID_FIELD, uuid, Field.Store.YES);
            doc.add(uuidField);

            Field labelField = new StringField(LABEL_FIELD, label, Field.Store.YES);
            doc.add(labelField);

            FieldType vectorFieldType = KnnVectorField.createFieldType(vector.length, domain.getVectorSimilarityMetric());
            KnnVectorField vectorField = new KnnVectorField(VECTOR_FIELD, vector, vectorFieldType);
            doc.add(vectorField);

            log.info("Add vector with " + vector.length + " dimensions to Lucene index ...");
            writer.addDocument(doc);

            // writer.forceMerge(1);
            writer.close();
        } catch (Exception e) {
            closeIndexWriter(writer);
            throw e;
        }
    }

    /**
     * Get centroid for a particular label
     */
    private FloatVector getCentroid(Context domain, int label) throws Exception {
        File embeddingsDir = new File(domain.getContextDirectory(),"classifications/" + label + "/embeddings/");
        File[] embeddingFiles = embeddingsDir.listFiles();
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
     *
     */
    private Directory getIndexDirectory(Context domain) throws Exception {
        File indexDir = new File(domain.getContextDirectory(), "lucene-classifications");
        if (!indexDir.isDirectory()) {
            indexDir.mkdirs();
        }
        String indexPath = indexDir.getAbsolutePath();
        return FSDirectory.open(Paths.get(indexPath));
    }
}
