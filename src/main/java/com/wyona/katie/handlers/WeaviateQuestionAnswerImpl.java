package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import com.wyona.katie.services.MailerService;
import com.wyona.katie.services.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpHost;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import technology.semi.weaviate.client.Config;
import technology.semi.weaviate.client.WeaviateClient;
import technology.semi.weaviate.client.base.Result;
import technology.semi.weaviate.client.v1.misc.model.Meta;

import org.apache.commons.codec.binary.Base64;

/**
 * Question/answer implementation based on Weaviate (https://www.semi.technology/developers/weaviate/current/)
 */
@Slf4j
@Component
public class WeaviateQuestionAnswerImpl implements QuestionAnswerHandler {

    @Value("${weaviate.host}")
    private String weaviateHostDefault;

    @Value("${weaviate.results.limit}")
    private int limit;

    @Value("${weaviate.basic.auth.username}")
    private String basicAuthUsername;

    @Value("${weaviate.basic.auth.password}")
    private String basicAuthPassword;

    @Value("${weaviate.api.key}")
    private String apiKey;

    @Autowired
    private MailerService mailerService;

    private static final String CLAZZ_QUESTION = "Question";
    private static final String FIELD_QUESTION = "question";
    private static final String CLAZZ_ANSWER = "Answer";
    private static final String FIELD_ANSWER = "answer";
    private static final String CLAZZ_TENANT = "Tenant";
    private static final String FIELD_TENANT = "tenant";

    /**
     * Check whether Weaviate is alive
     * @param endpoint API endpoint "/v1" or "/v1/.well-known/live" to do health check
     * @return true when alive and false otherwise
     */
    public boolean isAlive(String endpoint) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            String requestUrl = getHost(null) + endpoint;
            log.info("Check whether Weaviate is alive: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);

            if (bodyNode.get("links") != null) {
                return true;
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        log.warn("Weaviate '" + getHost(null) + "' seems to be down!");
        mailerService.notifyAdministrator("WARNING: The Weaviate Server at '" + getHost(null) + "' seems to be DOWN", null, null, false);
        return false;
    }

    /**
     * @return Weaviate version, e.g. "1.17.1"
     */
    public String getVersion(Context domain) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            String requestUrl = getHost(domain) + "/v1/meta";
            log.info("Get Weaviate version: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);

            return bodyNode.get("version").asText();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }

        /*
        HttpHost httpHost= getHttpHost(domain);
        log.info("Scheme, hostname, port: " + httpHost.getSchemeName() + ", " + httpHost.getHostName() + ", " + httpHost.getPort());
        Map<String, String> headers = new HashMap<String, String>();
        if (basicAuthUsername != null && basicAuthUsername.length() > 0) {
            String auth = basicAuthUsername + ":" + basicAuthPassword;
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            headers.put("Authorization", authHeader);
        }
        Config config = new Config(httpHost.getSchemeName(), httpHost.getHostName() + ":" + httpHost.getPort(), headers);
        WeaviateClient client = new WeaviateClient(config);

        Result<Meta> meta = client.misc().metaGetter().run();
        if (meta.getError() == null) {
            log.info("Weaviate meta.hostname: " + meta.getResult().getHostname());
            String version = meta.getResult().getVersion();
            log.info("Weaviate meta.version: " + version);
            log.info("Weaviate meta.modules: " + meta.getResult().getModules());

            return version;
        } else {
            log.error("Message: " + meta.getError().getMessages());
        }

        return null;

         */
    }

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) {
        log.info("Weaviate implementation of deleting tenant ...");

        // https://www.semi.technology/developers/weaviate/current/restful-api-references/objects.html#delete-a-data-object

        // INFO: Delete all referenced Questions and Answers
        String[] referencedObjects = getReferencedQuestionsAndAnswers(domain);
        if (referencedObjects != null) {
            for (String uuid: referencedObjects) {
                try {
                    deleteObject(uuid, domain);
                } catch(Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } else {
            log.info("No referenced Questions or Answers found for domain '" + domain.getId() + "'.");
        }

        // INFO: Delete tenant itself
        deleteObject(domain.getId(), domain);
    }

    /**
     * Get all UUIDs of referenced Questions and Answers of a particular domain
     * @return array of UUIDs of referenced Questions and Answers of a particular domain
     */
    private String[] getReferencedQuestionsAndAnswers(Context domain) {
        List<String> uuids = new ArrayList<String>();

        // INFO: GraphQL
        StringBuilder graphQL = new StringBuilder();

        graphQL.append("{Get{");

        graphQL.append(CLAZZ_QUESTION + "(where: {operator:Equal, valueString:\\\"" + domain.getId() + "\\\", path: [\\\"" + FIELD_TENANT + "\\\", \\\"" + CLAZZ_TENANT + "\\\", \\\"id\\\"]}) {");
        graphQL.append("_additional{");
        graphQL.append("id");
        graphQL.append("}"); // INFO: End _additional
        graphQL.append("},"); // INFO: End Question

        graphQL.append(CLAZZ_ANSWER  + "(where: {operator:Equal, valueString:\\\"" + domain.getId() + "\\\", path: [\\\"" + FIELD_TENANT + "\\\", \\\"" + CLAZZ_TENANT + "\\\", \\\"id\\\"]}) {");
        graphQL.append("_additional{");
        graphQL.append("id");
        graphQL.append("}"); // INFO: End _additional
        graphQL.append("}"); // INFO: End Answer

        graphQL.append("}}"); // INFO: End Get/GraphQL


        log.info("GraphQL: " + graphQL);

        StringBuilder query = new StringBuilder("{\"query\":\"");
        query.append(graphQL);
        query.append("\"}");

        log.info("Query: " + query);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(query.toString(), headers);

        String requestUrl = getHttpHost(domain) + "/v1/graphql";
        log.info("Get referenced Questions from Weaviate: " + requestUrl);
        ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
        JsonNode bodyNode = response.getBody();
        log.info("JSON response: " + bodyNode);

        JsonNode errorsNode = bodyNode.get("errors");
        if (bodyNode.has("errors")) {
            log.error("Error(s) occured  while trying to get referenced questions and answers!");
            if (errorsNode.isArray()) {
                for (JsonNode errorNode : errorsNode) {
                    String errorMsg = errorNode.get("message").asText();
                    log.error("Weaviate error message: " + errorMsg);
                }
            }
        }

        JsonNode dataNode = bodyNode.get("data");
        log.info("Data node: " + dataNode);

        JsonNode questionNodes = dataNode.get("Get").get(CLAZZ_QUESTION);
        if (questionNodes.isArray()) {
            for (JsonNode objectNode: questionNodes) {
                JsonNode additionalNode = objectNode.get("_additional");
                String uuid = additionalNode.get("id").asText();
                log.info("Question: " + uuid);
                uuids.add(uuid);
            }
        } else {
            log.info("No Questions referenced by tenant / domain '" + domain.getId() + "'.");
        }

        JsonNode answerNodes = dataNode.get("Get").get(CLAZZ_ANSWER);
        if (answerNodes.isArray()) {
            for (JsonNode objectNode: answerNodes) {
                JsonNode additionalNode = objectNode.get("_additional");
                String uuid = additionalNode.get("id").asText();
                log.info("Answer: " + uuid);
                uuids.add(uuid);
            }
        } else {
            log.info("No Answers referenced by tenant / domain '" + domain.getId() + "'.");
        }

        return uuids.toArray(new String[0]);
    }

    /**
     * @see QuestionAnswerHandler#createTenant(Context)
     */
    public String createTenant(Context domain) {
        /* SCHEMA
https://www.semi.technology/developers/weaviate/current/tutorials/how-to-create-a-schema.html
         */

        log.info("Create Weaviate tenant for Katie domain: " + domain.getId());

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode clazzNode = mapper.createObjectNode();
        clazzNode.put("class", CLAZZ_TENANT);
        clazzNode.put("id", domain.getId());

        ObjectNode propertiesNode = mapper.createObjectNode();
        propertiesNode.put("name", domain.getName());
        clazzNode.put("properties", propertiesNode);

        log.info("Tenant: " + clazzNode.toString());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(clazzNode.toString(), headers);

        domain.setWeaviateQueryUrl(getHost(null));
        String requestUrl = getHttpHost(domain) + "/v1/objects";
        log.info("Try to add Tenant to Weaviate: " + requestUrl);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);
        } catch(HttpClientErrorException e) {
            log.error(e.getMessage(), e);
        }
        return domain.getWeaviateQueryUrl();
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.info("Index QnA: " + qna.getQuestion() + " | " + qna.getAnswer() + " | " + qna.getUuid());

        // https://www.semi.technology/developers/weaviate/current/tutorials/how-to-import-data.html

        if (qna.getQuestion() != null) {
            ObjectNode questionObject = getQuestionOrAnswerObject(domain, CLAZZ_QUESTION, FIELD_QUESTION, qna.getQuestion(), qna.getUuid());
            log.info("Try to add Question object: " + questionObject.toString());
            importObject(questionObject, domain);
        } else {
            log.info("QnA '" + qna.getUuid() + "' has no question yet associated with.");
        }

        // INFO: Also index alternative questions
        String[] altQuestions = qna.getAlternativeQuestions();
        if (altQuestions != null && altQuestions.length > 0) {
            if (indexAlternativeQuestions) {
                for (String altQuestion : altQuestions) {
                    ObjectNode altQuestionObject = getQuestionOrAnswerObject(domain, CLAZZ_QUESTION, FIELD_QUESTION, altQuestion, qna.getUuid());
                    log.info("Try to add alternative question object: " + altQuestionObject.toString());
                    importObject(altQuestionObject, domain);
                }
            } else {
                if (qna.getAlternativeQuestions().length > 0) {
                    log.info("QnA '" + qna.getUuid() + "' has " + qna.getAlternativeQuestions().length + " alternative question(s), but do not index them.");
                }
            }
        }

        if (qna.getAnswerClientSideEncryptionAlgorithm() == null) {
            ObjectNode answerObject = getQuestionOrAnswerObject(domain, CLAZZ_ANSWER, FIELD_ANSWER, qna.getAnswer(), qna.getUuid());
            log.info("Try to add Answer object: " + answerObject.toString());
            importObject(answerObject, domain);
        } else {
            // INFO: If the knowledge base only contains one QnA and the answer of this QnA is encrypted and not Answer object is being created, then a query will fail.
            log.info("Answer of QnA '" + qna.getUuid() + "' is encrypted, therefore do not index answer.");
        }
    }

    /**
     * Create an object
     * @param object Object data including ID of object
     */
    private void importObject(ObjectNode object, Context domain) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(object.toString(), headers);

        String requestUrl = getHttpHost(domain) + "/v1/objects";
        log.info("Try to add object to Weaviate: " + requestUrl);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);
        } catch(HttpClientErrorException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * https://www.semi.technology/developers/weaviate/current/tutorials/how-to-import-data.html#add-a-data-object
     *
     * @param clazzName Class name, e.g. "Question" or "Answer"
     * @param fieldName Field name, e.g. "question" or "answer"
     */
    private ObjectNode getQuestionOrAnswerObject(Context domain, String clazzName, String fieldName, String text, String uuid) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();

        objectNode.put("class", clazzName);
        objectNode.put("id", UUID.randomUUID().toString());

        ObjectNode propertiesNode = mapper.createObjectNode();
        propertiesNode.put("qnaId", uuid);
        propertiesNode.put(fieldName, text);

        ArrayNode tenants = mapper.createArrayNode();
        ObjectNode beaconNode = mapper.createObjectNode();
        beaconNode.put("beacon", "weaviate://localhost/" + domain.getId());
        tenants.add(beaconNode);

        propertiesNode.put(FIELD_TENANT, tenants);
        objectNode.put("properties", propertiesNode);

        return objectNode;
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public QnA[] train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Finish implementation!");
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
        String[] ids = getQuestionsAndAnswers(uuid, domain);
        boolean allDeleted = true;
        for (String id: ids) {
            if (!deleteObject(id, domain)) {
                allDeleted = false;
            }
        }
        return allDeleted;
    }

    /**
     * Get UUIDs of all Questions and Answers associated with a particular QnA
     * @param qnaUuid UUID of QnA
     * @return array of UUIDs of all Questions and Answers associated with a particular QnA
     */
    private String[] getQuestionsAndAnswers(String qnaUuid, Context domain) {
        List<String> uuids = new ArrayList<String>();

        // INFO: GraphQL
        StringBuilder graphQL = new StringBuilder();

        graphQL.append("{Get{");

        // TODO: When upgrading from Weaviate 1.8.0 to 1.17.1 one might has to replace valueString by valueText
        //graphQL.append(CLAZZ_QUESTION + "(where: {operator:Equal, valueText:\\\"" + qnaUuid + "\\\", path: [\\\"qnaId\\\"]}) {");
        graphQL.append(CLAZZ_QUESTION + "(where: {operator:Equal, valueString:\\\"" + qnaUuid + "\\\", path: [\\\"qnaId\\\"]}) {");
        graphQL.append("_additional{");
        graphQL.append("id");
        graphQL.append("}"); // INFO: End _additional
        graphQL.append("},"); // INFO: End Question

        // TODO: When upgrading from Weaviate 1.8.0 to 1.17.1 one might has to replace valueString by valueText
        //graphQL.append(CLAZZ_ANSWER + "(where: {operator:Equal, valueText:\\\"" + qnaUuid + "\\\", path: [\\\"qnaId\\\"]}) {");
        graphQL.append(CLAZZ_ANSWER + "(where: {operator:Equal, valueString:\\\"" + qnaUuid + "\\\", path: [\\\"qnaId\\\"]}) {");
        graphQL.append("_additional{");
        graphQL.append("id");
        graphQL.append("}"); // INFO: End _additional
        graphQL.append("}"); // INFO: End Answer

        graphQL.append("}}"); // INFO: End Get/GraphQL


        log.info("GraphQL: " + graphQL);

        StringBuilder query = new StringBuilder("{\"query\":\"");
        query.append(graphQL);
        query.append("\"}");

        log.info("Query: " + query);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(query.toString(), headers);

        String requestUrl = getHttpHost(domain) + "/v1/graphql";
        log.info("Get UUIDs of all Questions and Answers associated with a particular QnA from Weaviate: " + requestUrl);
        ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
        JsonNode bodyNode = response.getBody();
        log.info("JSON response: " + bodyNode);

        JsonNode errorsNode = bodyNode.get("errors");
        if (bodyNode.has("errors")) {
            log.error("Error(s) occured  while trying to get referenced questions and answers!");
            if (errorsNode.isArray()) {
                for (JsonNode errorNode : errorsNode) {
                    String errorMsg = errorNode.get("message").asText();
                    log.error("Weaviate error message: " + errorMsg);
                }
            }
        }

        JsonNode dataNode = bodyNode.get("data");
        log.info("Data node: " + dataNode);

        JsonNode questionNodes = dataNode.get("Get").get(CLAZZ_QUESTION);
        if (questionNodes.isArray()) {
            for (JsonNode objectNode: questionNodes) {
                JsonNode additionalNode = objectNode.get("_additional");
                String uuid = additionalNode.get("id").asText();
                log.info("Question: " + uuid);
                uuids.add(uuid);
            }
        } else {
            log.info("No Questions referenced by tenant / domain '" + domain.getId() + "'.");
        }

        JsonNode answerNodes = dataNode.get("Get").get(CLAZZ_ANSWER);
        if (answerNodes.isArray()) {
            for (JsonNode objectNode: answerNodes) {
                JsonNode additionalNode = objectNode.get("_additional");
                String uuid = additionalNode.get("id").asText();
                log.info("Answer: " + uuid);
                uuids.add(uuid);
            }
        } else {
            log.info("No Answers referenced by tenant / domain '" + domain.getId() + "'.");
        }

        return uuids.toArray(new String[0]);
    }

    /**
     * @param uuid UUID of object
     * @param domain Domain object is associated with
     */
    private boolean deleteObject(String uuid, Context domain) {
        // https://www.semi.technology/developers/weaviate/current/restful-api-references/objects.html#delete-a-data-object

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = getHttpHeaders();
        HttpEntity<String> request = new HttpEntity<String>(headers);

        String requestUrl = getHttpHost(domain) + "/v1/objects/" + uuid;
        log.info("Try to delete Object: " + requestUrl);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.DELETE, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);
            return true;
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 404) {
                log.warn("Object '" + uuid + "' does not exist, therefore does not need to be deleted.");
                return true;
            } else {
                log.error("Unexpected status code: " + e.getRawStatusCode());
                return false;
            }
        }
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(Sentence, Context, int)
     */
    public Hit[] getAnswers(Sentence question, Context domain, int limit) {
        log.info("TODO: Consider using entities!");
        return getAnswers(question.getSentence(), question.getClassifications(), domain, limit);
    }

    /**
     * Query using Weaviate Java client
     */
    private Answer[] query(String question, Context domain) {
        HttpHost httpHost= getHttpHost(domain);
        log.info("Scheme, hostname, port: " + httpHost.getSchemeName() + ", " + httpHost.getHostName() + ", " + httpHost.getPort());
        Config config = new Config(httpHost.getSchemeName(), httpHost.getHostName() + ":" + httpHost.getPort());
        // TODO: Implement BasicAuth by adding an Authorization header to the config
        WeaviateClient client = new WeaviateClient(config);

        Result<Meta> meta = client.misc().metaGetter().run();
        if (meta.getError() == null) {
            log.info("Weaviate meta.hostname: " + meta.getResult().getHostname());
            log.info("Weaviate meta.version: " + meta.getResult().getVersion());
            log.info("Weaviate meta.modules: " + meta.getResult().getModules());
        } else {
            log.error("Message: " + meta.getError().getMessages());
        }

        // TODO: See
        // https://github.com/semi-technologies/weaviate-java-client
        // https://app.katie.qa/#/read-answer?domain-id=ceb2d2f2-2c1d-49be-a751-eed4be19e021&uuid=f7411ac0-b728-4c4c-be9f-2c8ad5ff67e8
        // https://github.com/wyona/spring-boot-hello-world-rest/blob/master/src/main/java/org/wyona/webapp/controllers/v2/KatieMockupConnectorController.java
        //client.graphQL().get();

        return null;
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(String, List, Context, int)
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context domain, int limit) {
        log.info("Get answers from Weaviate implementation for question '" + question + "' ...");

        String preparedQuestion = Utils.replaceNewLines(Utils.removeDoubleQuotes(Utils.removeBackslashes(question)), " ");
        //String preparedQuestion = Utils.escapeForJSON(question); // INFO: Escaping a backslash does not work with Weaviate

        log.info("Sanitized question: " + preparedQuestion);

        //query(preparedQuestion, domain); // TODO: Replace below code with query method using Weaviate client library

        // https://www.semi.technology/developers/weaviate/current/tutorials/how-to-query-data.html
        // https://www.semi.technology/developers/weaviate/current/modules/qna-transformers.html
        // https://www.semi.technology/developers/weaviate/current/tutorials/how-to-perform-a-semantic-search.html

        if (limit > 0) {
            log.info("TODO: Limit returned hits.");
        }

        // INFO: GraphQL
        StringBuilder graphQL = new StringBuilder();

        // INFO: https://www.semi.technology/developers/weaviate/current/modules/qna-transformers.html
        graphQL.append("{Get{");

        //graphQL.append("Question(limit: " + limit + ") {");
        //graphQL.append("Question(where: {operator:Equal, valueString:\\\"" + domain.getId() + "\\\", path: [\\\"tenant\\\", \\\"Tenant\\\", \\\"id\\\"]}, limit: " + limit + ") {");
        graphQL.append(CLAZZ_QUESTION + "(ask: {question:\\\"" + preparedQuestion + "\\\",certainty: " + domain.getWeaviateCertaintyThreshold() + "}, where: {operator:Equal, valueString:\\\"" + domain.getId() + "\\\", path: [\\\"" + FIELD_TENANT + "\\\", \\\"" + CLAZZ_TENANT + "\\\", \\\"id\\\"]}, limit: " + limit + ") {");
        graphQL.append(FIELD_QUESTION);
        graphQL.append(" qnaId");
        //graphQL.append(" answer");
        graphQL.append(" _additional{");
        graphQL.append("certainty");
        graphQL.append(" id");
        graphQL.append(" answer {result}");
        graphQL.append("}"); // INFO: End _additional
        graphQL.append("},"); // INFO: End Question

        graphQL.append(CLAZZ_ANSWER + "(ask: {question:\\\"" + preparedQuestion + "\\\",certainty: " + domain.getWeaviateCertaintyThreshold() + "}, where: {operator:Equal, valueString:\\\"" + domain.getId() + "\\\", path: [\\\"" + FIELD_TENANT + "\\\", \\\"" + CLAZZ_TENANT + "\\\", \\\"id\\\"]}, limit: " + limit + ") {");
        graphQL.append(FIELD_ANSWER);
        graphQL.append(" qnaId");
        //graphQL.append(" answer");
        graphQL.append(" _additional{");
        graphQL.append("certainty");
        graphQL.append(" id");
        graphQL.append(" answer {result}");
        graphQL.append("}"); // INFO: End _additional
        graphQL.append("}"); // INFO: End Answer

        graphQL.append("}}"); // INFO: End Get/GraphQL

        log.info("GraphQL: " + graphQL);

        StringBuilder query = new StringBuilder("{\"query\":\"");
        query.append(graphQL);
        query.append("\"}");

        log.info("Query: " + query);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(query.toString(), headers);

        String requestUrl = getHttpHost(domain) + "/v1/graphql";

        log.info("Get answers from Weaviate: " + requestUrl);
        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
        } catch(HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            return new Hit[0];
        }

        JsonNode bodyNode = response.getBody();
        log.info("JSON response: " + bodyNode);

        JsonNode errorsNode = bodyNode.get("errors");
        if (errorsNode != null) {
            String errMsg = errorsNode.get(0).get("message").asText();
            log.error("Weaviate error response message: " + errMsg);
            return new Hit[0];
        }

        JsonNode dataNode = bodyNode.get("data");
        log.info("Data node: " + dataNode);

        List<UuidCertainty> qnaUuids = new ArrayList<UuidCertainty>();

        log.info("Get QnA uuids from Question class ...");
        JsonNode questionsNode = dataNode.get("Get").get(CLAZZ_QUESTION);
        if (questionsNode.isArray()) {
            for (JsonNode objectNode: questionsNode) {
                String _question = objectNode.get(FIELD_QUESTION).asText();
                String uuid = objectNode.get("qnaId").asText();
                JsonNode additionalNode = objectNode.get("_additional");
                double certainty = additionalNode.get("certainty").asDouble();

                // INFO: There might be alternative questions, which are referencing the same UUIDs
                qnaUuids = mergeWithoutSorting(qnaUuids, new UuidCertainty(uuid, certainty));
                //qnaUuids.add(new UuidCertainty(uuid, certainty));

                //String questionId = additionalNode.get("id").asText();
                log.info("QnA: " + _question + " " + uuid + " " + certainty);

                // TODO: Does this check make sense, because we already tell Weaviate above to apply the threshold?!
                if (certainty > domain.getWeaviateCertaintyThreshold()) {
                    log.info("Certainty '" + certainty + "' is greater than threshold '" + domain.getWeaviateCertaintyThreshold() + "'.");
                } else {
                    log.info("Certainty '" + certainty + "' is less than threshold '" + domain.getWeaviateCertaintyThreshold() + "'.");
                }
            }
        } else {
            log.info("No questions matching query.");
        }

        log.info("Get QnA uuids from Answer class ...");
        JsonNode answersNode = dataNode.get("Get").get(CLAZZ_ANSWER);
        if (answersNode.isArray()) {
            for (JsonNode objectNode: answersNode) {
                String answer = objectNode.get(FIELD_ANSWER).asText();
                String uuid = objectNode.get("qnaId").asText();
                JsonNode additionalNode = objectNode.get("_additional");
                double certainty = additionalNode.get("certainty").asDouble();
                qnaUuids = mergeWithoutSorting(qnaUuids, new UuidCertainty(uuid, certainty));

                //String answerId = additionalNode.get("id").asText();
                log.info("QnA: " + answer + " " + uuid + " " + certainty);

                if (certainty > domain.getWeaviateCertaintyThreshold()) {
                    log.info("Certainty '" + certainty + "' is greater than threshold '" + domain.getWeaviateCertaintyThreshold() + "'.");
                } else {
                    log.info("Certainty '" + certainty + "' is less than threshold '" + domain.getWeaviateCertaintyThreshold() + "'.");
                }
            }
        } else {
            log.info("No answers matching query.");
        }

        Collections.sort(qnaUuids, UuidCertainty.CertaintyComparator);

        List<Hit> answers = new ArrayList<Hit>();
        Date dateAnswered = null;
        Date dateAnswerModified = null;
        Date dateOriginalQuestionSubmitted = null;
        for (UuidCertainty uuidCertainty: qnaUuids) {
            log.info("UUID/Certainty: " + uuidCertainty.getUuid() + ", " + uuidCertainty.getCertainty());
            String _answer = Answer.AK_UUID_COLON + uuidCertainty.getUuid();
            answers.add(new Hit(new Answer(question, _answer, null, null, classifications, null, null, dateAnswered, dateAnswerModified, null, domain.getId(), uuidCertainty.getUuid(), question, dateOriginalQuestionSubmitted, true, null, true, null), uuidCertainty.getCertainty()));
        }

        log.info(answers.size() + " answers found with certainty > " + domain.getWeaviateCertaintyThreshold());

        return answers.toArray(new Hit[0]);
    }

    /**
     * Merge UUID into existing list of UUIDs, but without sorting by certainty
     * @param uuids Existing list of UUIDs
     * @param uuid UUID to be merged into existing list
     * @return merged list of UUIDs
     */
    private List<UuidCertainty> mergeWithoutSorting(List<UuidCertainty> uuids, UuidCertainty uuid) {
        log.info("Merge uuid '" + uuid.getUuid() + " / "+ uuid.getCertainty() + "' with existing list ...");

        List<UuidCertainty> merged = new ArrayList<UuidCertainty>();

        boolean alreadyAdded = false;
        for (UuidCertainty current: uuids) {
            if (current.getUuid().equals(uuid.getUuid())) {
                if (uuid.getCertainty() > current.getCertainty()) {
                    merged.add(uuid);
                } else {
                    merged.add(current);
                }
                alreadyAdded = true;
            } else {
                merged.add(current);
            }
        }
        if (!alreadyAdded) {
            merged.add(uuid);
        }

        return merged;
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        if (basicAuthUsername != null && basicAuthUsername.length() > 0) {
            String auth = basicAuthUsername + ":" + basicAuthPassword;
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            //log.info("Weaviate Authorization: " + authHeader); // INFO: Used by https://updown.io/checks
            headers.set("Authorization", authHeader);
        } else if (apiKey != null && apiKey.length() > 0) {
            headers.set("Authorization", "Bearer " + apiKey);
        }

        return headers;
    }

    /**
     * @param domain Optional domain
     * @return host, e.g. "http://0.0.0.0:8080"
     */
    public String getHost(Context domain) {
        if (domain == null) {
            log.info("No Katie domain provided, therefore return default Weaviate host configuration.");
            return weaviateHostDefault;
        } else {
            return getHttpHost(domain).toHostString();
        }
    }

    /**
     * Get Weaviate host, e.g. 'https://katie.semi.network' or 'https://weaviate.ukatie.com'
     */
    private HttpHost getHttpHost(Context domain) {
        try {
            log.info("Weaviate URL: " + domain.getWeaviateQueryUrl());
            java.net.URL url = new java.net.URL(domain.getWeaviateQueryUrl());
            log.info("Weaviate URL parts: " + url.getPort() + " " + url.getProtocol() + " " + url.getHost());
            if (url.getPort() < 0) {
                log.info("TODO: If port -1, then set port to 443 for https and 80 for http");
            }
            return new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}

/**
 *
 */
class UuidCertainty {

    private String uuid;
    private double certainty;

    /**
     *
     */
    public UuidCertainty(String uuid, double certainty) {
        this.uuid = uuid;
        this.certainty = certainty;
    }

    /**
     *
     */
    public String getUuid() {
        return uuid;
    }

    /**
     *
     */
    public double getCertainty() {
        return certainty;
    }

    /**
     *
     */
    public static Comparator<UuidCertainty> CertaintyComparator = new Comparator<UuidCertainty>() {

        @Override
        public int compare(UuidCertainty u1, UuidCertainty u2) {
            double c1 = u1.getCertainty();
            double c2 = u2.getCertainty();
            if (c1 > c2) {
                return -1;
            } else if (c1 == c2) {
                return 0;
            } else {
                return 1;
            }
        }
    };
}
