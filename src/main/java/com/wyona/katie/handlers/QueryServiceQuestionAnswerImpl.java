package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import com.wyona.katie.services.JwtService;
import com.wyona.katie.services.Utils;
import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Question / Answer implementation connecting with a third-party query service using a custom Katie connector webapp, e.g. https://github.com/wyona/katie-weaviate/tree/master/katie-weaviate-connector
 */
@Slf4j
@Component
public class QueryServiceQuestionAnswerImpl implements QuestionAnswerHandler {

    @Autowired
    JwtService jwtService;

    /**
     * @return query service URL, e.g. "http://localhost:8383/api/v2"
     */
    private String getBaseURL(Context domain) {
        return domain.getQueryServiceUrl();
    }

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) {
        log.info("Query service implementation of deleting tenant ...");
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = getHttpHeaders();
        HttpEntity<String> request = new HttpEntity<String>(headers);

        String requestUrl = getBaseURL(domain) + "/tenant/" + domain.getId();
        log.info("Try to delete Object: " + requestUrl);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.DELETE, request, JsonNode.class);
            //JsonNode bodyNode = response.getBody();
            //log.info("JSON: " + bodyNode);
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 404) {
                log.warn("Tenant associated with Katie domain '" + domain.getId() + "' does not exist, therefore does not need to be deleted.");
            } else if (e.getRawStatusCode() == 403) {
                log.error("Not authorized to access '" + requestUrl + "'!");
            } else {
                log.error("Unexpected status code: " + e.getRawStatusCode());
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * @see QuestionAnswerHandler#createTenant(Context)
     */
    public String createTenant(Context domain) {
        StringBuilder body = new StringBuilder("{");
        body.append("\"id\":\"" + domain.getId() + "\",");
        body.append("\"name\":\"" + domain.getName() + "\"");
        body.append("}");

        log.info("Request body: " + body);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(body.toString(), headers);

        String baseURL = getBaseURL(domain);
        String requestUrl = baseURL + "/tenant";
        log.info("Try to add Tenant: " + requestUrl);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 403) {
                log.error("Not authorized to access '" + requestUrl + "'!");
            } else {
                log.error(e.getMessage(), e);
            }
            // INFO: Do not return null
        }

        return baseURL;
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.info("Index QnA: " + qna.getQuestion() + " | " + qna.getUuid());

        StringBuilder body = new StringBuilder("{");
        body.append("\"answer\":\"" + Utils.escape(Utils.replaceNewLines(qna.getAnswer(), " ")) + "\",");
        body.append("\"question\":\"" + Utils.escape(Utils.replaceNewLines(qna.getQuestion(), " ")) + "\",");
        body.append("\"uuid\":\"" + qna.getUuid() + "\"");
        body.append("}");

        log.info("Request body: " + body);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(body.toString(), headers);

        String requestUrl = getBaseURL(domain) + "/qna/" + domain.getId();
        log.info("Try to add QnA: " + requestUrl);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 403) {
                log.error("Not authorized to access '" + requestUrl + "'!");
            } else {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public void train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Implement REST interface to train several QnAs at the same time.");
        for (QnA qna: qnas) {
            train(qna, domain, indexAlternativeQuestions);
        }
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
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = getHttpHeaders();
        HttpEntity<String> request = new HttpEntity<String>(headers);

        String requestUrl = getBaseURL(domain) + "/qna/" + domain.getId() + "/" + uuid;
        log.info("Try to delete QnA: " + requestUrl);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.DELETE, request, JsonNode.class);
            //JsonNode bodyNode = response.getBody();
            //log.info("JSON: " + bodyNode);
            return true;
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 404) {
                log.warn("QnA '" + uuid + "' associated with Katie domain '" + domain.getId() + "' does not exist, therefore does not need to be deleted.");
            } else if (e.getRawStatusCode() == 403) {
                log.error("Not authorized to access '" + requestUrl + "'!");
            } else {
                log.error("Unexpected status code: " + e.getRawStatusCode());
            }
            return false;
        }
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(Sentence, Context, int)
     */
    public Hit[] getAnswers(Sentence question, Context domain, int limit) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode bodyRequest = mapper.createObjectNode();
        bodyRequest.put("text", question.getSentence());

        log.info("Request body: " + bodyRequest.toString());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(bodyRequest.toString(), headers);

        String requestUrl = getBaseURL(domain) + "/ask/" + domain.getId();
        log.info("Try to ask question: " + requestUrl);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);

            List<Hit> answers = new ArrayList<Hit>();

            if (bodyNode.isArray()) {
                Date dateAnswered = null;
                Date dateAnswerModified = null;

                for (JsonNode entry: bodyNode) {
                    String _answer = null;
                    if (entry.has("answer") && !entry.get("answer").isNull()) {
                        _answer = entry.get("answer").asText();
                    }

                    String uuid = null;
                    if (entry.has("uuid") && (!entry.get("uuid").isNull() || !entry.get("uuid").isEmpty())) {
                        uuid = entry.get("uuid").asText();
                        // INFO: Overwrite answer with Katie UUID
                        _answer = Answer.AK_UUID_COLON + uuid;
                    } else {
                        log.info("No Katie QnA UUID returned.");
                    }

                    log.info("QnA UUID: " + uuid);
                    log.info("QnA Answer: " + _answer);

                    String url = null;
                    if (entry.has("url") && !entry.get("url").isNull()) {
                        url = entry.get("url").asText();
                    }

                    boolean isPublic = !domain.getAnswersGenerallyProtected();
                    // TODO: Set permissions
                    double score = -1; // TODO: Get score
                    answers.add(new Hit(new Answer(question.getSentence(), _answer, null, url, question.getClassifications(),null, null, dateAnswered, dateAnswerModified, null, domain.getId(), uuid, null, null, isPublic, null, true, null), score));
                }
            } else {
                log.error("No UUIDs received from connector.");
            }

            return answers.toArray(new Hit[0]);
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 403) {
                log.error("Not authorized to access '" + requestUrl + "'!");
            } else {
                log.error(e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(String, List, Context, int)
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context domain, int limit) {
        log.info("Get answer from query service implementation for question '" + question + "' ...");
        Sentence sentence = new Sentence(question, null, classifications);
        // Set classification
        return getAnswers(sentence, domain, limit);
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        try {
            headers.setBearerAuth(generateToken());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return headers;
    }

    /**
     *
     */
    private String generateToken() throws Exception {
        JWTPayload jwtPayload = new JWTPayload();
        jwtPayload.setIss("https://ukatie.com");
        HashMap<String, String> claims = new HashMap<String, String>();
        claims.put("TODO", "TODO");
        jwtPayload.setPrivateClaims(claims);

        return jwtService.generateJWT(jwtPayload, 3600, null);
    }
}
