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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Katie experimental retrieval implementation
 */
@Slf4j
@Component
public class KatieExperimentalRetrievalImpl implements QuestionAnswerHandler {

    EmbeddingValueType embeddingValueTypeFloat32 = EmbeddingValueType.float32;
    VectorSimilarityFunction vectorSimilarityFunctionCosine = VectorSimilarityFunction.COSINE;

    // TODO: Make embedding configuration configurable
    //EmbeddingsImpl embeddingsImpl = EmbeddingsImpl.SBERT;
    //String model = null;
    EmbeddingsImpl embeddingsImpl = EmbeddingsImpl.OLLAMA;
    String model = "qwen3-embedding:0.6b";

    @Autowired
    private LuceneCodecFactory luceneCodecFactory;

    @Autowired
    private EmbeddingsService embeddingsService;

    @Autowired
    private OpenAIGenerate openAIGenerate;

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
        log.info("Train QnA '" + qna.getUuid() + "' ...");
        Language languageOfQnA = null;
        try {
            List<String> chunks = getChunks(Utils.getContent(qna), languageOfQnA, domain);
            //List<String> chunks = getKeywords(Utils.getContent(qna), languageOfQnA);

            IndexWriter writer = null;
            writer = new IndexWriter(getIndexDirectory(domain), getIndexWriterConfig());

            for (String chunk: chunks) {
                log.info("Chunk: " + chunk);
                indedTextAsVector(writer, chunk, qna.getUuid());
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
        log.error("Find answers ...");

        List<Hit> hits = vectorSimilaritySearch(question.getSentence(), context);

        /*
        Map<String, Double> sumOfScoresById = new HashMap<>();

        Language languageOfQuestion = null;
        try {
            // TODO: Do not split question into keywords
            List<CardKeyword> keywords = KeywordsExtractor.getKeywordsV2(question.getSentence(), languageOfQuestion, true);
            for (CardKeyword keyword : keywords) {
                log.info("Keyword: " + keyword);
                for (Hit hit : vectorSimilaritySearch(keyword.getStem(), context)) {
                    sumOfScoresById.merge(hit.getAnswer().getUuid(), hit.getScore(), Double::sum);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        Map<String, Double> sortedMapDescending = sumOfScoresById.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        List<Hit> hits = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sortedMapDescending.entrySet()) {
            log.info("Sum of scores by Id: " + entry.getKey()  + ": " + entry.getValue());
            String path = Answer.AK_UUID_COLON + entry.getKey();
            Date dateAnswered = null;
            Date dateAnswerModified = null;
            Date dateOriginalQuestionSubmitted = null;
            Hit hit = new Hit(new Answer(question.getSentence(), path, null, null, null, null, null, dateAnswered, dateAnswerModified, null, context.getId(), entry.getKey(), null, dateOriginalQuestionSubmitted, true, null, true, null), entry.getValue());
            hits.add(hit);
        }

         */

        if (limit > 0 && hits.size() > limit) {
            log.info("Only return " + limit + " hits.");
            hits = hits.subList(0, limit);
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
     * Find similar texts
     * @param text Text for which similar texts should be found
     * @param domain Domain containing vector embedding index
     * @return similar texts
     */
    private List<Hit> vectorSimilaritySearch(String text, Context domain) {
        List<Hit> hits = new ArrayList<>();

        try {
            Vector queryVector = embeddingsService.getEmbedding(text, embeddingsImpl, model, EmbeddingType.SEARCH_QUERY, embeddingValueTypeFloat32, null, null, null);
            IndexReader indexReader = DirectoryReader.open(getIndexDirectory(domain));
            StoredFields storedFields = indexReader.storedFields();
            IndexSearcher searcher = new IndexSearcher(indexReader);
            int k = 10;
            //int k = 50;
            Query query = new KnnFloatVectorQuery(VECTOR_FIELD, ((FloatVector)queryVector).getValues(), k);
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

                Hit hit = new Hit(new Answer(text, path, null, null, null, null, null, dateAnswered, dateAnswerModified, null, domain.getId(), id, text_field_value, dateOriginalQuestionSubmitted, true, null, true, null), scoreDoc.score);
                hits.add(hit);
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
     * Decompose text into semantically independent pieces of information (semantic decomposition, fact-based chunking, Subject → Relationship → Object)
     * @param text Text to be decomposed, e.g., "Michael likes oranges, apples and red bicycles"
     * @return independent pieces of information, e.g., "Michael likes oranges", "Michael likes apples", "Michael likes red bicycles"
     */
    private List<String> getChunks(String text, Language language, Context domain) throws Exception {
        List<String> chunks = new ArrayList<>();

        String SEPARATOR = "###";

        StringBuilder PROMPT = new StringBuilder("Decompose the following text into semantically independent pieces of information (Subject -> Relationship -> Object)");
        PROMPT.append(", as for example 'Michael likes oranges, apples and red bicycles' should be decomposed into the three chunks 'Michael likes oranges', 'Michael likes apples' and 'Michael likes red bicycles'.");
        PROMPT.append(" Separate the independent pieces by thre hashtags'" + SEPARATOR + "'.");
        PROMPT.append(" The Text to be decomposed is: " + text);
        List<PromptMessage> promptMessages = new ArrayList<>();

        promptMessages.add(new PromptMessage(PromptMessageRole.USER, PROMPT.toString()));
        // TODO: See MulticlassTextClassifierLLMImpl
        String completedText = openAIGenerate.getCompletion(promptMessages, null, domain.getCompletionConfig(false), 0.7).getText();
        String[] pieces = completedText.split(SEPARATOR);
        if (pieces != null && pieces.length > 0) {
            for (String piece : pieces) {
                chunks.add(piece);
            }
        } else {
            log.error("No pieces!");
        }

        return chunks;
    }

    /**
     * Split text into keywords
     * @param text Text to be split into keywords, e.g., "Michael likes oranges, apples and red bicycles"
     * @return keywords, e.g., "Michael", "oranges", "apples", "red", "bicycles"
     */
    private List<String> getKeywords(String text, Language language) throws Exception {
        List<String> chunks = new ArrayList<>();

        List<CardKeyword> keywords = KeywordsExtractor.getKeywordsV2(text, language, true);
        for (CardKeyword keyword : keywords) {
            chunks.add(keyword.getStem());
        }

        return chunks;
    }

    /**
     * Index text as vector embedding
     * @param text Text for which vector embedding should be generated
     * @param id Id of QnA provided text is associated with
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
        }
    }
}
