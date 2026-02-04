package com.wyona.katie.handlers;

import com.wyona.katie.models.Vector;
import com.wyona.katie.services.MailerService;
import com.wyona.katie.services.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

import org.apache.http.HttpHost;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.codec.binary.Base64;

/**
 *
 */
@Slf4j
@Component
public class SentenceBERTQuestionAnswerImpl implements QuestionAnswerHandler, EmbeddingsProvider {

    public static final String DENSE_MODEL = "dense_model";
    public static final String SPARSE_MODEL = "sparse_model";
    public static final String VERSION = "version";

    @Value("${sbert.hostname}")
    private String sbertHostname;

    @Value("${sbert.scheme}")
    private String sbertScheme;

    @Value("${sbert.port}")
    private String sbertPort;

    @Value("${sbert.basic.auth.username}")
    private String sbertBasicAuthUsername;

    @Value("${sbert.basic.auth.password}")
    private String sbertBasicAuthPassword;

    @Autowired
    private MailerService mailerService;

    private static final String DATE_KATIE_FIELD = "date_katie";
    private static final String TYPE = "question_answer";
    private static String ELASTICSEARCH_DATE_PATTERN = "yyyyMMdd'T'HHmmss.SSSZ";

    private static final String CONTEXT_ID_FIELD = "context_id";
    private static final String QUESTION_FIELD = "question";
    private static final String ANSWER_ID_FIELD = "answer";
    private static final String EXTERNAL_ID = "ext_id";

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) { 
        log.info("SentenceBERT implementation of deleting tenant ...");
        deleteIndex(domain);
    }

    /**
     * @see QuestionAnswerHandler#createTenant(Context)
     */
    public String createTenant(Context domain) {
        log.info("SentenceBERT implementation of creating tenant ...");
        return createCorpus();
    }

    /**
     * @return version of SentenceBERT REST service (e.g. "1.11.0") and name of dense embeddings model used by SentenceBERT (e.g. "all-mpnet-base-v2")
     */
    public Map<String, String> getVersionAndModel() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            String requestUrl = getHttpHost() + "/api/v1/model";
            log.info("Check whether Sentence-BERT is alive and get configured model names and version of Sentence-BERT: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);

            Map<String, String> map = new HashMap<String, String>();
            map.put(VERSION, bodyNode.get("version").asText());
            map.put(DENSE_MODEL, bodyNode.get("model-name").asText());
            if (bodyNode.has("sparse-model-name")) {
                map.put(SPARSE_MODEL, bodyNode.get("sparse-model-name").asText());
            } else {
                map.put(SPARSE_MODEL, "info_not_available");
            }

            return map;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        log.warn("SentenceBERT '" + getHttpHost() + "' seems to be down!");
        mailerService.notifyAdministrator("WARNING: The SentenceBERT Server at '" + getHttpHost() + "' seems to be DOWN", null, null, false);
        return null;
    }

    /**
     * @see EmbeddingsProvider#getEmbedding(String, String, EmbeddingType, EmbeddingValueType, String)
     */
    public Vector getEmbedding(String sentence, String model, EmbeddingType embeddingType, EmbeddingValueType valueType, String apiToken) throws Exception {
        log.debug("Get embedding from SentenceBERT for sentence '" + sentence + "' ...");

        // TODO: Implement rate limit, e.g. using https://github.com/bucket4j/bucket4j 8.3.0

        FloatVector vector = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode parentNode = mapper.createObjectNode();
            parentNode.put("sentence", sentence);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders();
            headers.set("Content-Type", "application/json; charset=UTF-8");
            HttpEntity<String> request = new HttpEntity<String>(parentNode.toString(), headers);

            String requestUrl = getHttpHost() + "/api/v1/sentence/embedding";
            log.info("Get embedding: " + requestUrl + " (Body: " + parentNode.toString() + ")");
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.debug("JSON: " + bodyNode);

            //String modelName = bodyNode.get("model-name").asText();
            //log.info("Model: " + modelName);

            JsonNode embeddingNode = bodyNode.get("embedding");
            if (embeddingNode.isArray()) {
                vector = new FloatVector(embeddingNode.size());
                log.info("Response contains embedding with " + vector.getDimension() + " dimensions.");

                for (int i = 0; i < vector.getDimension(); i++) {
                    vector.set(i, Float.parseFloat(embeddingNode.get(i).asText()));
                }
            } else {
                log.warn("Response did not contain embedding!");
            }
        } catch(Exception e) {
            log.error("Something went wrong while trying to get embedding from SentenceBERT for text '" + sentence + "'!");
            log.error(e.getMessage(), e);
            throw e;
        }

        /* Debug values
        if (vector != null) {
            for (int i = 0; i < vector.length; i++) {
                log.info(i + ": " + vector[i]);
            }
        }
         */

        return vector;
    }

    /**
     * TODO
     */
    public Map<Integer, Float> getSparseEmbedding(String text) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode requestBodyNode = mapper.createObjectNode();
        requestBodyNode.put("sentence", text);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(requestBodyNode.toString(), headers);

        String requestUrl = getHttpHost() + "/api/v1/sentence/sparse-embedding";
        log.info("Get sparse embedding: " + requestUrl + " (Request body: " + requestBodyNode.toString() + ")");
        ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
        JsonNode responseBodyNode = response.getBody();
        log.info("JSON: " + responseBodyNode);

        Map<Integer, Float> sparseEmbedding = new HashMap<>();

        JsonNode embeddingNode = responseBodyNode.get("embeddings");
        JsonNode indicesNode = embeddingNode.get("indices");
        JsonNode mapNode = indicesNode.get(0);
        JsonNode keysNode = indicesNode.get(1);
        JsonNode valuesNode = embeddingNode.get("values");
        if (mapNode.isArray()) {
            for (int i = 0; i < mapNode.size(); i++) {
                if (mapNode.get(i).asInt() == 0) { // INFO: Only get the first sparse embedding
                    sparseEmbedding.put(keysNode.get(i).asInt(), Float.parseFloat(valuesNode.get(i).asText()));
                } else {
                    log.info("Ignore sparse embedding key: " + keysNode.get(i).asInt());
                }
            }
        }

        return sparseEmbedding;
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.info("Train QnA using SentenceBERT implementation ...");

        if (qna.getQuestion() != null) {
            indexSentence(qna.getQuestion(), qna.getUuid(), domain.getSentenceBERTCorpusId());
        } else {
            log.info("QnA '" + qna.getUuid() + "' has no question yet associated with.");
        }

        // INFO: Index alternative questions
        log.debug("Number of alternative questions: " + qna.getAlternativeQuestions().length);
        if (indexAlternativeQuestions) {
            for (String aQuestion : qna.getAlternativeQuestions()) {
                log.info("Index alternative question '" + aQuestion + "' ...");
                indexSentence(aQuestion, qna.getUuid(), domain.getSentenceBERTCorpusId());
            }
        } else {
            if (qna.getAlternativeQuestions().length > 0) {
                log.info("QnA '" + qna.getUuid() + "' has " + qna.getAlternativeQuestions().length + " alternative question(s), but do not index them.");
            }
        }

        if (qna.getAnswerClientSideEncryptionAlgorithm() == null) {
            indexSentence(sanitize(qna.getAnswer()), qna.getUuid(), domain.getSentenceBERTCorpusId());
        } else {
            log.info("Answer of QnA '" + qna.getUuid() + "' is encrypted, therefore do not index answer.");
        }
    }

    /**
     * Sanitize text, such that it can be processed by SentenceBERT ...
     */
    private String sanitize(String text) {
        return Utils.replaceNewLines(text, " ").trim();
    }

    /**
     * @param sentence Sentence, e.g. "Was mache ich, wenn mir jemand droht"
     * @param qnaId QnA UUID, e.g. "6db21d14-8789-41f7-a551-763572c07208"
     * @param corpusId Corpus Id, e.g. "7c45ddf8-f878-49cc-b830-8a75fc8e9189"
     */
    private void indexSentence(String sentence, String qnaId, String corpusId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode sentenceNode = mapper.createObjectNode();
            sentenceNode.put("sentence", sentence);
            sentenceNode.put(EXTERNAL_ID, Answer.AK_UUID_COLON + qnaId);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders();
            headers.set("Content-Type", "application/json; charset=UTF-8");
            HttpEntity<String> request = new HttpEntity<String>(sentenceNode.toString(), headers);

            String requestUrl = getHttpHost() + "/api/v1/corpus/" + corpusId + "/sentence";
            log.info("Train sentence: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public QnA[] train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode parentNode = mapper.createArrayNode();

        for (int i = 0; i < qnas.length; i++) {
            if (qnas[i].getQuestion() != null) {
                ObjectNode questionNode = mapper.createObjectNode();
                questionNode.put("sentence", qnas[i].getQuestion());
                questionNode.put(EXTERNAL_ID, Answer.AK_UUID_COLON + qnas[i].getUuid());

                parentNode.add(questionNode);
            } else {
                log.info("QnA '" + qnas[i].getUuid() + "' has no question yet associated with.");
            }

            if (indexAlternativeQuestions) {
                for (String aQuestion : qnas[i].getAlternativeQuestions()) {
                    ObjectNode aQuestionNode = mapper.createObjectNode();
                    aQuestionNode.put("sentence", aQuestion);
                    aQuestionNode.put(EXTERNAL_ID, Answer.AK_UUID_COLON + qnas[i].getUuid());

                    parentNode.add(aQuestionNode);
                }
            } else {
                if (qnas[i].getAlternativeQuestions().length > 0) {
                    log.info("Do not index alternative questions of QnA '" + qnas[i].getUuid() + "'.");
                }
            }

            if (qnas[i].getAnswerClientSideEncryptionAlgorithm() == null) {
                ObjectNode answerNode = mapper.createObjectNode();
                answerNode.put("sentence", sanitize(qnas[i].getAnswer()));
                answerNode.put(EXTERNAL_ID, Answer.AK_UUID_COLON + qnas[i].getUuid());

                parentNode.add(answerNode);
            } else {
                log.info("Answer of QnA '" + qnas[i].getUuid() + "' is encrypted, therefore do not index answer.");
            }
        }

        log.info("Body containing questions, alternative questions, etc. of QnAs: " + parentNode.toString());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(parentNode.toString(), headers);

        String requestUrl = getHttpHost() + "/api/v1/corpus/" + domain.getSentenceBERTCorpusId()+ "/sentences";
        log.info("Train " + qnas.length + " QnAs at once: " + requestUrl);
        ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
        log.info("Response status: " + response.getStatusCode());
        JsonNode bodyNode = response.getBody();
        log.info("Response JSON: " + bodyNode);

        // TODO: Only return QnAs which got trained successfully
        return qnas;
    }

    /**
     * @see QuestionAnswerHandler#retrain(QnA, Context, boolean)
     */
    public void retrain(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Delete/train is just a workaround, implement retrain by itself");
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
        log.warn("Delete QnA with uuid '" + uuid + "' from corpus ...");
        String akUuid = Answer.AK_UUID_COLON + uuid;

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = getHttpHeaders();
            HttpEntity<String> request = new HttpEntity<String>(headers);

            String requestUrl = getHttpHost() + "/api/v1/corpus/" + domain.getSentenceBERTCorpusId()+ "/sentence/" + akUuid;
            log.info("Delete question: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.DELETE, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);
            return true;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(Sentence, Context, int)
     */
    public Hit[] getAnswers(Sentence question, Context context, int limit) {
        log.info("TODO: Consider using entities!");
        return getAnswers(question.getSentence(), question.getClassifications(), context, limit);
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(String, List, Context, int)
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context context, int limit) {
        List<Hit> answers = new ArrayList<Hit>();

        log.info("Get answer from SentenceBERT implementation ...");

        try {
            // INFO: See https://www.yulup.com/en/projects/2924e29b-c744-4dbb-9e3f-e4b46831779c/bdd-scenarios/bdd-004.html, curl -XGET http://localhost:9200/askkatie/_search?q=context_id:'ROOT'

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode queryNode = mapper.createObjectNode();
            queryNode.put("sentence", question);
            if (limit > 0) {
                // INFO: The same QnA UUID can be indexed at least three times: question, alternative question, answer
                // Whereas there could be an arbitrary number of alternative questions, so we might want to set the multiplier even greater than 3
                int multiplier = 3;
                queryNode.put("limit", multiplier * limit);
            }
            // INFO: SentenceBERT is using cosine distance, which means when vectors are the same, then the distance is 0
            // INFO: Only return hits, where the distance is smaller than the threshold
            queryNode.put("threshold", 0.95); // TODO: Make configurable

            log.info("SentenceBERT query: " + queryNode.toString());

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders();
            headers.set("Content-Type", "application/json; charset=UTF-8");
            HttpEntity<String> request = new HttpEntity<String>(queryNode.toString(), headers);

            String requestUrl = getHttpHost() + "/api/v1/corpus/" + context.getSentenceBERTCorpusId()+ "/similar";
            log.info("Get answers: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);
            JsonNode similarSentencesNode = bodyNode.get("similar-sentences");
            if (similarSentencesNode.isArray()) {
                for (JsonNode similarSentenceNode: similarSentencesNode) {
                    String _question = similarSentenceNode.get("sentence").asText();
                    String _answer = similarSentenceNode.get(EXTERNAL_ID).asText();
                    log.info("Question/Answer: '" + _question + "' / " + _answer);

                    Date dateAnswered = null;
                    Date dateAnswerModified = null;
                    Date dateOriginalQuestionSubmitted = null;

                    double distance = Double.parseDouble(similarSentenceNode.get("distance").asText());

                    if (distance <= context.getSentenceBERTDistanceThreshold()) {
                        String uuid = Answer.removePrefix(_answer);
                        if (uuid != null) {
                            double score = distance; // INFO: The smaller the distance, the greater the similarity
                            answers.add(new Hit(new Answer(question, _answer, null,null, classifications,null, null, dateAnswered, dateAnswerModified, null, context.getId(), uuid, _question, dateOriginalQuestionSubmitted, true, null, true, null), score));
                        } else {
                            log.warn("Do not add answer '" + _answer + "', because it does not contain Katie prefix!");
                        }
                    } else {
                        log.info("Do not add answer '" + _answer + "', because distance '" + distance + "' of hit is above configured threshold '" + context.getSentenceBERTDistanceThreshold() + "'.");
                    }
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return answers.toArray(new Hit[0]);
    }

    /**
     * Get SentenceBERT (Sentence Transformers) host
     * @return host, e.g., "https://sbert.katie.qa:443"
     */
    public HttpHost getHttpHost() {
        return new HttpHost(sbertHostname, Integer.parseInt(sbertPort), sbertScheme);
    }

    /**
     * Delete SentenceBERT corpus associated with Katie domain Id
     * @param domain Domain within Katie, e.g. "5bd57b92-da98-422f-8ad6-6670b9c69184"
     */
    private void deleteIndex(Context domain) {
        String indexName = domain.getSentenceBERTCorpusId();

        try {
            log.info("Delete index '" + indexName + "' ...");
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders();
            HttpEntity<String> request = new HttpEntity<String>(headers);

            String requestUrl = getHttpHost() + "/api/v1/corpus/" + indexName;
            log.info("Delete index: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.DELETE, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Check whether corpus exists and in case it does not exist, then create a corpus with an appropriate mapping
     * @return corpus Id, e.g. "5bd57b92-da98-422f-8ad6-6670b9c69184"
     */
    private String createCorpus() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            String requestUrl = getHttpHost() + "/api/v1/corpus/new";
            log.info("Create corpus: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);

            String corpusId = bodyNode.get("id").asText();
            return corpusId;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * Check whether Sentence-BERT is alive
     * @param endpoint Health endpoint, e.g. "/api/v1/health"
     * @return true when alive and false otherwise
     */
    public boolean isAlive(String endpoint) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            String requestUrl = getHttpHost() + endpoint;
            log.info("Check whether Sentence-BERT is alive: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);

            if (bodyNode.get("status").asText().equals("UP")) {
                return true;
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        log.warn("SentenceBERT '" + getHttpHost() + "' seems to be down!");
        mailerService.notifyAdministrator("WARNING: The SentenceBERT Server at '" + getHttpHost() + "' seems to be DOWN", null, null, false);
        return false;
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        // TODO: Make content type settable as argument
        //headers.set("Content-Type", "application/json; charset=UTF-8");
        if (sbertBasicAuthUsername != null && sbertBasicAuthUsername.length() > 0) {
            String auth = sbertBasicAuthUsername + ":" + sbertBasicAuthPassword;
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            //log.info("SentenceBERT Authorization: " + authHeader); // INFO: Used by https://updown.io/checks
            headers.set("Authorization", authHeader);
        }
        return headers;
    }
}
