package com.wyona.katie.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.wyona.katie.models.*;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;

import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.GetLoadStateReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.response.InsertResp;
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

        int VECTOR_DIMENSION = 5;

        MilvusClientV2 client = getMilvusClient(domain);

        try {
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
        } finally {
            client.close();
        }

        return domain.getMilvusBaseUrl();
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.error("TODO: Implement method train()!");

        MilvusClientV2 client = getMilvusClient(domain);
        String collectionName = getCollectionName(domain);
        try {
            qna.getClassifications();
            String classification = "TODO";
            String[] embedding = null; // TODO
            Gson gson = new Gson();
            List<JsonObject> data = Arrays.asList(
                    gson.fromJson("{\"" + UUID_FIELD + "\": \"" + qna.getUuid() + "\", \"" + VECTOR_FIELD + "\": [0.3580376395471989, -0.6023495712049978, 0.18414012509913835, -0.26286205330961354, 0.9029438446296592], \"" + CLASSIFICATION_FIELD + "\": \"" + classification + "\"}", JsonObject.class)
            );
            InsertReq insertReq = InsertReq.builder()
                    .collectionName(collectionName)
                    .data(data)
                    .build();

            InsertResp insertResp = client.insert(insertReq);
            log.info("Milvus insert response: " + insertResp);
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
        log.error("TODO: Implement method getAnswers()!");

        List<Hit> answers = new ArrayList<Hit>();

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
}
