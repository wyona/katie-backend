package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import com.wyona.katie.services.*;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Katie experimental retrieval implementation
 */
@Slf4j
@Component
public class KatieExperimentalRetrievalImpl implements QuestionAnswerHandler {

    EmbeddingValueType embeddingValueTypeFloat32 = EmbeddingValueType.float32;
    VectorSimilarityFunction vectorSimilarityFunctionCosine = VectorSimilarityFunction.COSINE;
    EmbeddingsImpl embeddingsImpl = EmbeddingsImpl.SBERT;
    String model = null;

    @Autowired
    private LuceneCodecFactory luceneCodecFactory;

    @Autowired
    private EmbeddingsService embeddingsService;

    private static final String ID_FIELD = "id";
    private static final String TEXT_FIELD = "text";
    private static final String VECTOR_FIELD = "vector";

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) {
        log.info("Delete tenant ...");
        File indexDir = getIndexDirectoryPath(domain);
        if (indexDir.isDirectory()) {
            log.info("Delete directory recursively: " + indexDir.getAbsolutePath());
            try {
                FileUtils.deleteDirectory(indexDir);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * @see QuestionAnswerHandler#createTenant(Context)
     */
    public String createTenant(Context domain) {
        log.info("Create tenant, nothing to create though yet.");
        return null;
    }

    /**
     * @see QuestionAnswerHandler#delete(String, Context)
     */
    public boolean delete(String uuid, Context domain) {
        log.error("TODO: Implement delete(String, Context) ...");
        return false;
    }

    /**
     * @see QuestionAnswerHandler#retrain(QnA, Context, boolean)
     */
    public void retrain(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.error("TODO: Finish implementation of retrain(QnA, Context, boolean) ...");

        // TODO: Consider deleting old references
        train(qna, domain, indexAlternativeQuestions);
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public QnA[] train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Finish implementation to index more than one QnA at the same time!");
        for (QnA qna: qnas) {
            train(qna, domain, indexAlternativeQuestions);
        }

        // TODO: Only return QnAs which got trained successfully
        return qnas;
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.error("TODO: Finish implementation of train(QnA, Context, boolean) ...");
        Language languageOfQnA = null;
        try {
            List<CardKeyword> keywords = KeywordsExtractor.getKeywordsV2(Utils.getContent(qna), languageOfQnA, true);

            IndexWriter writer = null;
            writer = new IndexWriter(getIndexDirectory(domain), getIndexWriterConfig());

            for (CardKeyword keyword : keywords) {
                log.info("Keyword: " + keyword);
                indedTextAsVector(writer, keyword.getStem(), qna.getUuid());
            }

            writer.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(Sentence, Context, int)
     */
    public Hit[] getAnswers(Sentence question, Context context, int limit) {
        List<Hit> hits = new ArrayList<>();
        log.error("TODO: Implement getAnswers(Sentence, Context, int) ...");

        Language languageOfQuestion = null;
        try {
            List<CardKeyword> keywords = KeywordsExtractor.getKeywordsV2(question.getSentence(), languageOfQuestion, true);
            for (CardKeyword keyword : keywords) {
                log.info("Keyword: " + keyword);
                hits = vectorSimilaritySearch(keyword.getStem(), context);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return hits.toArray(new Hit[0]);
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(String, List, Context, int)
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context domain, int limit) {
        log.error("TODO: Implement getAnswers(String, List, Context, int) ...");
        return null;
    }

    /**
     *
     */
    private List<Hit> vectorSimilaritySearch(String text, Context domain) {
        List<Hit> hits = new ArrayList<>();

        try {
            Vector queryVector = embeddingsService.getEmbedding(text, embeddingsImpl, model, EmbeddingType.SEARCH_QUERY, embeddingValueTypeFloat32, null, null, null);
            IndexReader indexReader = DirectoryReader.open(getIndexDirectory(domain));
            StoredFields storedFields = indexReader.storedFields();
            IndexSearcher searcher = new IndexSearcher(indexReader);
            int k = 100;
            Query query = query = new KnnFloatVectorQuery(VECTOR_FIELD, ((FloatVector)queryVector).getValues(), k);
            TopDocs topDocs = searcher.search(query, k);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = storedFields.document(scoreDoc.doc);
                String id = doc.get(ID_FIELD);
                String text_field_value = doc.get(TEXT_FIELD);
                log.info("Dense vector embedding found with ID '" + id + "', text '" + text_field_value + "' and  and confidence score (" + vectorSimilarityFunctionCosine + ") '" + scoreDoc.score + "'.");
                Date dateAnswered = null;
                Date dateAnswerModified = null;
                Date dateOriginalQuestionSubmitted = null;
                String path = Answer.AK_UUID_COLON + id;
                hits.add(new Hit(new Answer(text, path, null, null, null, null, null, dateAnswered, dateAnswerModified, null, domain.getId(), id, text_field_value, dateOriginalQuestionSubmitted, true, null, true, null), scoreDoc.score));
            }
            indexReader.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return hits;
    }

    /**
     *
     */
    private IndexWriterConfig getIndexWriterConfig() {
        IndexWriterConfig iwc = new IndexWriterConfig();
        //iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        iwc.setCodec(luceneCodecFactory.getCodec(embeddingValueTypeFloat32));
        return iwc;
    }

    /**
     *
     */
    private Directory getIndexDirectory(Context domain) throws Exception {
        File indexDir = getIndexDirectoryPath(domain);
        if (!indexDir.isDirectory()) {
            indexDir.mkdirs();
        }
        return FSDirectory.open(Paths.get(indexDir.getAbsolutePath()));
    }

    /**
     *
     */
    private File getIndexDirectoryPath(Context domain) {
        return new File(domain.getContextDirectory(), "katie-experimental-lucene-vector-index");
    }

    /**
     *
     */
    private void indedTextAsVector(IndexWriter writer, String text, String id) {
        Document doc = new Document();

        Field idField = new StringField(ID_FIELD, id, Field.Store.YES);
        doc.add(idField);

        Field textField = new StringField(TEXT_FIELD, text, Field.Store.YES);
        doc.add(textField);

        try {
            Vector vector = embeddingsService.getEmbedding(text, embeddingsImpl, model, EmbeddingType.SEARCH_DOCUMENT, embeddingValueTypeFloat32, null, null, null);
            FieldType vectorFieldType = new CustomVectorFieldType(vector.getDimension(), vectorSimilarityFunctionCosine, VectorEncoding.FLOAT32);
            Field vectorField = new KnnFloatVectorField(VECTOR_FIELD, ((FloatVector)vector).getValues(), vectorFieldType);
            doc.add(vectorField);

            writer.addDocument(doc);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
    }
}
