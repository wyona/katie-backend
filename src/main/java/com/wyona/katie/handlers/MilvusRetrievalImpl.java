package com.wyona.katie.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wyona.katie.models.*;

import com.wyona.katie.models.Vector;
import com.wyona.katie.services.EmbeddingsService;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;

import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.GetLoadStateReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.SearchResp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * Retrieval implementation based on Milvus
 */
@Slf4j
@Component
public class MilvusRetrievalImpl implements QuestionAnswerHandler {

    @Value("${milvus.host}")
    private String milvusHostDefault;

    @Autowired
    private EmbeddingsService embeddingsService;

    private static final String UUID_FIELD = "uuid";
    private static final String VECTOR_FIELD = "vector";
    private static final String CLASSIFICATION_FIELD = "classification";

    /**
     * Check whether Weaviate is alive
     * @param endpoint API endpoint "/v1" or "/v1/.well-known/live" to do health check
     * @return true when alive and false otherwise
     */
    public boolean isAlive(String endpoint) {
        log.error("TODO: Implement method isAlive()!");
        return true;
    }

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) {
        log.info("Drop Milvus collection ...");

        MilvusClientV2 client = getMilvusClient(domain);
        String collectionName = getCollectionName(domain);
        DropCollectionReq dropQuickSetupParam = DropCollectionReq.builder()
                .collectionName(collectionName)
                .build();

        try {
            client.dropCollection(dropQuickSetupParam);
        } finally {
            client.close();
        }

    }

    /**
     * @see QuestionAnswerHandler#createTenant(Context)
     */
    public String createTenant(Context domain) {;
        log.info("Create Milvus collection ...");
        domain.setMilvusBaseUrl(getHost(null));

        MilvusClientV2 client = getMilvusClient(domain);

        try {
            Vector dummyVector = getEmbedding("dummy", domain);
            int VECTOR_DIMENSION = dummyVector.getDimension();

            // INFO: Create schema
            CreateCollectionReq.CollectionSchema schema = client.createSchema();
            schema.addField(AddFieldReq.builder()
                    .fieldName(UUID_FIELD)
                    .dataType(DataType.VarChar)
                    .maxLength(512)
                    .isPrimaryKey(true)
                    .autoID(false)
                    .build());
            schema.addField(AddFieldReq.builder()
                    .fieldName(VECTOR_FIELD)
                    .dataType(DataType.FloatVector)
                    .dimension(VECTOR_DIMENSION)
                    .build());
            schema.addField(AddFieldReq.builder()
                    .fieldName(CLASSIFICATION_FIELD)
                    .dataType(DataType.VarChar)
                    .maxLength(512)
                    .build());

            // INFO: Set index parameters
            IndexParam indexParamForIdField = IndexParam.builder()
                    .fieldName(UUID_FIELD)
                    .indexType(IndexParam.IndexType.AUTOINDEX)
                    .build();

            IndexParam indexParamForVectorField = IndexParam.builder()
                    .fieldName(VECTOR_FIELD)
                    .indexType(IndexParam.IndexType.AUTOINDEX)
                    //.metricType(IndexParam.MetricType.COSINE)
                    .metricType(IndexParam.MetricType.L2)
                    .build();

            List<IndexParam> indexParams = new ArrayList<>();
            //indexParams.add(indexParamForIdField);
            indexParams.add(indexParamForVectorField);

            String collectionName = getCollectionName(domain);

            // INFO: Create collection
            CreateCollectionReq customizedSetupReq1 = CreateCollectionReq.builder()
                    .collectionName(collectionName)
                    .collectionSchema(schema)
                    .indexParams(indexParams)
                    .build();
            client.createCollection(customizedSetupReq1);

            // INFO: Check loading status
            GetLoadStateReq customSetupLoadStateReq1 = GetLoadStateReq.builder()
                    .collectionName(collectionName)
                    .build();
            Boolean loaded = client.getLoadState(customSetupLoadStateReq1);
            log.warn("Load status of Milvus collection '" + collectionName + "': " + loaded);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            client.close();
        }

        return domain.getMilvusBaseUrl();
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.error("TODO: Finish implementation of method train()!");

        MilvusClientV2 client = getMilvusClient(domain);
        String collectionName = getCollectionName(domain);
        try {
            qna.getClassifications();
            String classification = "TODO";

            // TODO: Also index question, whereas see Lucene implementation ...
            float[] embedding = ((FloatVector) getEmbedding(qna.getAnswer(), domain)).getValues();

            JsonObject obj = new JsonObject();
            obj.addProperty(UUID_FIELD, qna.getUuid());
            JsonArray vectorArray = new JsonArray();
            for (float f : embedding) {
                vectorArray.add(f);
            }
            obj.add(VECTOR_FIELD, vectorArray);
            obj.addProperty(CLASSIFICATION_FIELD, classification);

            List<JsonObject> data = Collections.singletonList(obj);
            //List<JsonObject> data = new ArrayList<>();
            //data.add(obj);

            InsertReq insertReq = InsertReq.builder()
                    .collectionName(collectionName)
                    .data(data)
                    .build();

            InsertResp insertResp = client.insert(insertReq);
            log.info("Milvus insert response: " + insertResp);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            client.close();
        }
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public QnA[] train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Finish implementation train(QnA[], Context, boolean)!");
        for (QnA qna: qnas) {
            train(qna, domain, indexAlternativeQuestions);
        }

        // TODO: Only return QnAs which got trained successfully
        return qnas;
    }

    /**
     * @see QuestionAnswerHandler#retrain(QnA, Context, boolean)
     */
    public void retrain(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Delete/train is just a workaround, implement retrain by itself");
        // TODO: https://www.semi.technology/developers/weaviate/current/restful-api-references/objects.html#update-a-data-object
        if (delete(qna.getUuid(), domain)) {
            train(qna, domain, indexAlternativeQuestions);
        } else {
            log.warn("QnA with UUID '" + qna.getUuid() + "' was not deleted and therefore was not retrained!");
        }
    }

    /**
     * @see QuestionAnswerHandler#delete(String, Context)
     */
    public boolean delete(String uuid, Context domain) {
        log.error("TODO: Implement method delete()!");

        // TODO

        return true;
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(Sentence, Context, int)
     */
    public Hit[] getAnswers(Sentence question, Context domain, int limit) {
        log.info("TODO: Consider using entities!");
        return getAnswers(question.getSentence(), question.getClassifications(), domain, limit);
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(String, List, Context, int)
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context domain, int limit) {
        log.error("TODO: Finish implementation of method getAnswers()!");

        List<Hit> answers = new ArrayList<Hit>();

        int k = 100;
        //if (limit > 0) {
        if (false) {
            // INFO: The same QnA UUID can be indexed at least three times: question, alternative question, answer
            // Whereas there could be an arbitrary number of alternative questions, so we might want to set the multiplier even greater than 3
            int multiplier = 3;
            k = multiplier * limit;
            log.info("External limit set to " + limit + ", therefore get " + k + " (" + multiplier + " times " + limit + ") nearest neighbours ...");
        } else {
            log.info("No external limit set, therefore get " + k + " nearest neighbours ...");
        }

        MilvusClientV2 client = getMilvusClient(domain);
        String collectionName = getCollectionName(domain);
        try {
            float[] queryEmbedding = ((FloatVector) getEmbedding(question, domain)).getValues();
            FloatVec queryVector = new FloatVec(queryEmbedding);
            SearchReq searchReq = SearchReq.builder()
                    .collectionName(collectionName)
                    .data(Collections.singletonList(queryVector))
                    .topK(k)
                    .build();

            SearchResp searchResp = client.search(searchReq);

            List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();
            for (List<SearchResp.SearchResult> results : searchResults) {
                log.info("Milvus TopK results:");
                for (SearchResp.SearchResult result : results) {
                    log.info("Result: " + result);
                    String uuid = result.getId().toString();
                    float score = result.getScore();
                    log.info("Vector found with UUID '" + uuid + "' and confidence score '" + score + "'.");
                    //log.info("Vector found with UUID '" + path + "' and confidence score (" + domain.getVectorSimilarityMetric() + ") '" + score + "'.");
                    String originalQuestionOfAnswer = null;
                    Date dateAnswered = null;
                    Date dateAnswerModified = null;
                    Date dateOriginalQuestionSubmitted = null;
                    String answer = Answer.AK_UUID_COLON + uuid;
                    answers.add(new Hit(new Answer(question, answer, null, null, classifications, null, null, dateAnswered, dateAnswerModified, null, domain.getId(), uuid, originalQuestionOfAnswer, dateOriginalQuestionSubmitted, true, null, true, null), score));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            client.close();
        }

        return answers.toArray(new Hit[0]);
    }

    /**
     * @param domain Optional domain
     * @return host, e.g. "http://0.0.0.0:8080"
     */
    private String getHost(Context domain) {
        if (domain != null) {
            return domain.getMilvusBaseUrl();
        } else {
            return milvusHostDefault;
        }
    }

    /**
     *
     */
    private MilvusClientV2 getMilvusClient(Context domain) {
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri(getHost(domain))
                .build();

        return new MilvusClientV2(connectConfig);
    }

    /**
     *
     */
    private String getCollectionName(Context domain) {
        // INFO: The first character of a collection name must be an underscore or letter. Also, collection name can only contain numbers, letters and underscores.
        return "katie_" + domain.getId().replaceAll("-", "_");
    }

    /**
     * Get vector / text embedding
     */
    private Vector getEmbedding(String text, Context domain) throws Exception {
        try {
            if (text.trim().length() == 0) {
                // TODO: Do we really want to index an empty string!?
                log.warn("Text is empty!");
            }
            Vector vector = embeddingsService.getEmbedding(text, domain, EmbeddingType.SEARCH_DOCUMENT, domain.getEmbeddingValueType());
            log.info("Vector: " + vector);
            return vector;
        } catch (Exception e) {
            log.error("Get embedding failed for text '" + text + "', therefore do not add embedding to Milvus vector index of domain '" + domain.getId() + "'.");
            throw e;
        }
    }
}
