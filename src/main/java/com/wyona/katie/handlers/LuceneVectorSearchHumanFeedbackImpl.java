package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import com.wyona.katie.services.EmbeddingsService;
import com.wyona.katie.services.LuceneCodecFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ArrayList;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.KnnVectorField;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.KnnVectorQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;

import java.nio.file.Paths;
import java.io.File;

/**
 * Index and retrieve human feedback using Lucene Vector Search
 */
@Slf4j
@Component
public class LuceneVectorSearchHumanFeedbackImpl implements HumanFeedbackHandler {

    @Autowired
    private EmbeddingsService embeddingsService;

    @Autowired
    private LuceneCodecFactory luceneCodecFactory;

    private static final EmbeddingsImpl EMBEDDINGS_IMPL = EmbeddingsImpl.SBERT;
    private static final EmbeddingValueType VECTOR_VALUE_TYPE = EmbeddingValueType.float32;

    private static final String UUID_FIELD = "uuid";
    private static final String RATING_FIELD = "rating";
    private static final String VECTOR_FIELD = "vector";
    
    /**
     * @see HumanFeedbackHandler#indexHumanFeedback(String, String, Context, int, User)
     */
    public void indexHumanFeedback(String question, String answerUuid, Context domain, int rating, User user) throws Exception {
        log.info("Index human feedback ...");

        IndexWriterConfig iwc = new IndexWriterConfig();
        iwc.setCodec(luceneCodecFactory.getCodec(VECTOR_VALUE_TYPE));
        IndexWriter writer = null;
        try {
            writer = new IndexWriter(getIndexDirectory(domain), iwc);

            indexQuestionAsVector(writer, question, answerUuid, rating, domain);

            // writer.forceMerge(1);
            writer.close();
        } catch (Exception e){
            closeIndexWriter(writer);
            throw e;
        }
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
     * @see HumanFeedbackHandler#getHumanFeedback(String, Context)
     */
    public Rating[] getHumanFeedback(String question, Context domain) throws Exception {
        log.info("Consider human feedback: Get embedding for question ...");
        Vector queryVector = embeddingsService.getEmbedding(question, EMBEDDINGS_IMPL, null, EmbeddingType.SEARCH_QUERY, VECTOR_VALUE_TYPE, null);
        int k = 7; // INFO: The number of documents to find


        List<Rating> ratings = new ArrayList<Rating>();
        try {
            IndexReader indexReader = DirectoryReader.open(getIndexDirectory(domain));
            IndexSearcher searcher = new IndexSearcher(indexReader);

            Query query = new KnnVectorQuery(VECTOR_FIELD, ((FloatVector)queryVector).getValues(), k);

            TopDocs topDocs = searcher.search(query, k);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = indexReader.document(scoreDoc.doc);
                String answerUuid = doc.get(UUID_FIELD);
                int ratingScore = Integer.parseInt(doc.get(RATING_FIELD));
                log.info("Vector found with answer UUID '" + answerUuid + "' and confidence score (" + domain.getVectorSimilarityMetric() + ") '" + scoreDoc.score + "'.");

                Rating rating = new Rating();
                rating.setQnauuid(answerUuid);
                rating.setRating(ratingScore);
                ratings.add(rating);
            }
            indexReader.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return ratings.toArray(new Rating[0]);
    }

    /**
     * Index question as vector
     * @param question Asked question
     */
    private void indexQuestionAsVector(IndexWriter writer, String question, String answerUuid, int rating, Context domain) throws Exception {
        Document doc = new Document();

        Field uuidField = new StringField(UUID_FIELD, answerUuid, Field.Store.YES);
        doc.add(uuidField);

        Field ratingField = new StringField(RATING_FIELD, "" + rating, Field.Store.YES);
        doc.add(ratingField);

        // TODO: Check input sequence length and log warning when text is too long:
        //  https://www.sbert.net/examples/applications/computing-embeddings/README.html#input-sequence-length
        //  https://docs.cohere.ai/docs/embeddings#how-embeddings-are-obtained
        Vector vector = embeddingsService.getEmbedding(question, EMBEDDINGS_IMPL, null, EmbeddingType.SEARCH_QUERY, VECTOR_VALUE_TYPE, null);

        FieldType vectorFieldType = KnnVectorField.createFieldType(vector.getDimension(), domain.getVectorSimilarityMetric());
        // TODO: Use KnnFloatVectorField
        KnnVectorField vectorField = new KnnVectorField(VECTOR_FIELD, ((FloatVector)vector).getValues(), vectorFieldType);
        doc.add(vectorField);

        log.info("Add vector with " + vector.getDimension() + " dimensions to Lucene index ...");
        writer.addDocument(doc);
    }

    /**
     * Get file system directory path containing Lucene vector index
     * @param domain Domain associated with Lucene index
     * @return absolute directory path containing Lucene vector index
     */
    private String getVectorIndexDir(Context domain) {
        File indexDir = new File(domain.getContextDirectory(), "lucene-vector-index-human-feedback");
        if (!indexDir.isDirectory()) {
            indexDir.mkdirs();
        }
        return indexDir.getAbsolutePath();
    }

    /**
     *
     */
    private Directory getIndexDirectory(Context domain) throws Exception {
        String indexPath = getVectorIndexDir(domain);
        return FSDirectory.open(Paths.get(indexPath));
    }
}
