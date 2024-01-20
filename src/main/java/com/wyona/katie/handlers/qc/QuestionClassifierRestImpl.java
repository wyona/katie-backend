package com.wyona.katie.handlers.qc;

import com.wyona.katie.models.*;
import com.wyona.katie.services.MailerService;
import com.wyona.katie.services.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class QuestionClassifierRestImpl implements QuestionClassifier, ClassificationHandler {

    @Autowired
    private MailerService mailerService;

    @Value("${questionClassifier.scheme}")
    private String questionClassifierScheme;

    @Value("${questionClassifier.hostname}")
    private String questionClassifierHostname;

    @Value("${questionClassifier.port}")
    private String questionClassifierPort;

    @Value("${questionClassifier.certainty.threshold}")
    private String questionClassifierCertaintyThreshold;

    @Value("${new.context.mail.body.host}")
    private String katieHost;

    private final int connectTimeout = 3000; // INFO: in milliseconds
    private final int readTimeout = 30000; // INFO: in milliseconds
    //private final int readTimeout = 5000; // INFO: in milliseconds

    /**
     * @see QuestionClassifier#analyze(String, Context)
     */
    public AnalyzedMessage analyze(String message, Context domain) {
        AnalyzedMessage analyzedMessage = new AnalyzedMessage(message, QuestionClassificationImpl.REST);
        // INFO: use classification service to check for questions without question marks
        // INFO: if some error happened, then false is returned
        String domainId = "NOT_AVAILABLE";
        if (domain != null) {
            domainId = domain.getId();
        }
        Optional<QuestionClassification> response = getClassification(message, domainId);
        if (response.isEmpty()) {
            analyzedMessage.setContainsQuestions(false);
            return analyzedMessage;
        } else {
            QuestionClassification questionClassification = response.get();
            //log.info("Question Classification Service used to determine if the input is a question. Input_type: " + questionClassification.getInputType() + ", Input_type_confidence: " + questionClassification.getInputTypeConfidence());
            if( questionClassification.isQuestion() ) {
                analyzedMessage.setContainsQuestions(true);
                //Sentence context = questionClassification.getContext();
                //analyzedMessage.addQuestionAndContext(context, null);
                for(QuestionSentence q : questionClassification.getQuestions()) {
                	analyzedMessage.addQuestionAndContext(q, questionClassification.getContext());
                }
                log.info("Returning Analyzed Message:\n" + analyzedMessage);

                return analyzedMessage;
            } else {
                log.info("Input '" + message + "' not classified as question!");
                analyzedMessage.setContainsQuestions(false);
                return analyzedMessage;
            }
        }
    }

    /**
     * Check whether Question Classifier is alive
     * @return true when alive and false otherwise
     */
    public boolean isAlive(String endpoint) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        HttpEntity<String> request = new HttpEntity<String>(headers);

        String requestUrl = getHost() + endpoint;
        try {
            log.info("Check whether Question Classifier is alive: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);

            if (bodyNode.get("status").asText().equals("UP")) {
                return true;
            }
        } catch(Exception e) {
            log.error(e.getMessage());
        }

        log.warn("Question Classifier '" + getHost() + "' seems to be down!");
        mailerService.notifyAdministrator("WARNING: The Question Classifier Service at '" + requestUrl + "' seems to be DOWN", null, null, false);
        return false;
    }

    /**
     * Get questionclassifier host, e.g. 'https://questionclassifier.ukatie.com' or 'http://localhost:5001'
     */
    private HttpHost getHost() {
        return new HttpHost(questionClassifierHostname, Integer.parseInt(questionClassifierPort), questionClassifierScheme);
    }

    /**
     * Get http headers
     * @return HttpHeaders
     */
    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        return headers;
    }

    /**
     * TODO: split in 2 methods with the network query separate for better testing
     * @see ClassificationHandler#getClassification(String, String)
     */
    @Override
    public Optional<QuestionClassification> getClassification(String input, String domainId) {

        log.info("Message (not sanitized): " + input);
        String sanitizedInput = Utils.escapeForJSON(input);
        //String sanitizedInput = Utils.replaceNewLines(Utils.escapeDoubleQuotes(Utils.escapeBackslashes(input)), " ");
        StringBuilder body = new StringBuilder("{\"input\": "+ "\""+ sanitizedInput + "\"" + "}");

        log.info("Request body: " + body);

        RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());

        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(body.toString(), headers);

        String requestUrl = getHost() + "/api/v1.2/ask/" + domainId + "/semantics";
        log.info("Try to get message classification: " + requestUrl);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);
            ArrayList<Entity> contextEntities = new ArrayList<Entity>();
            
            for(JsonNode ent : bodyNode.get("context_named_entities")) {
            	contextEntities.add(new Entity(ent.get("type").asText(), ent.get("text").asText()));
            }
            Sentence context = new Sentence(bodyNode.get("context").asText(), contextEntities, null);
            LinkedList<QuestionSentence> questions = new LinkedList<QuestionSentence>();
            for(JsonNode question : bodyNode.get("questions")) {
            	ArrayList<Entity> questionEntities = new ArrayList<Entity>();
            	for(JsonNode questionEntity : question.get("named_entities")) {
            		questionEntities.add(new Entity(questionEntity.get("type").asText(), questionEntity.get("text").asText()));
            	}
            	LinkedList<String> classifications = new LinkedList<String>();
            	classifications.add(question.get("subtype").asText());
            	LinkedList<Word> questionWords = new LinkedList<Word>();
            	for(JsonNode word : question.get("words")) {
            		questionWords.add(new Word(word.get("word").asText(), word.get("feats").asText(), word.get("upos").asText(),word.get("xpos").asText()));
            	}
            	QuestionSentence qs = new QuestionSentence(question.get("text").asText(), questionEntities, classifications, questionWords);
            	questions.add(qs);
            }
            boolean isQuestion = bodyNode.get("is_question").asBoolean();
            QuestionClassification qc = new QuestionClassification(context, contextEntities.stream().collect(Collectors.toCollection(LinkedList::new)), isQuestion, questions);
            return Optional.of(qc);
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 403) {
                log.error("Not authorized to access '" + requestUrl + "'!");
            } else if (e.getRawStatusCode() == 400) {
                log.error(e.getMessage(), e);
            } else {
                log.error(e.getMessage(), e);
            }
            mailerService.notifyAdministrator(getSubject(), getErrorMessageBody(e.getMessage(), input) + "\n\nStatus Code:\n\n" + e.getRawStatusCode(), null, false);
            return Optional.empty();
        } catch(ResourceAccessException e) {
            // INFO: Both timeout and not reachable exceptions are handled as ResourceAccessException by restTemplate
            log.error(e.getMessage(), e);
            if (e.getMessage().contains("Read timed out")) {
                log.info("Configured connect timeout in milliseconds: " + connectTimeout);
                log.info("Configured read timeout in milliseconds: " + readTimeout);
                mailerService.notifyAdministrator(getSubject(), getErrorMessageBody(e.getMessage(), input) + "\n\nConfigured read timeout in milliseconds:\n\n" + readTimeout, null, false);
            } else {
                mailerService.notifyAdministrator(getSubject(),  getErrorMessageBody(e.getMessage(), input), null, false);
            }
            // TODO: The method isAlive(String) should also detect when the Question Classifier is down
            return Optional.empty();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            mailerService.notifyAdministrator(getSubject(), getErrorMessageBody(e.getMessage(), input), null, false);
            return Optional.empty();
        }
    }

    /**
     * Get Error Subject
     */
    private String getSubject() {
        return "ERROR: Analyzing message by QuestionClassifierRestImpl failed";
    }

    /**
     *
     */
    private String getErrorMessageBody(String errorMsg, String userMsg) {
        StringBuilder body = new StringBuilder();

        body.append("Error Message:\n\n" + errorMsg);
        body.append("\n\n");
        body.append("Processed User Message:\n\n" + userMsg);
        body.append("\n\n");
        body.append("QuestionClassifierRestImpl Host:\n\n" + getHost());
        body.append("\n\n");
        body.append("Katie Host:\n\n" + katieHost);

        return body.toString();
    }

    /**
     * To configure a request timeout when querying a web service
     * @return clientHttpRequestFactory with timeout of 3000ms
     */
    private ClientHttpRequestFactory getClientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory
                = new HttpComponentsClientHttpRequestFactory();

        clientHttpRequestFactory.setConnectTimeout(connectTimeout);
        clientHttpRequestFactory.setReadTimeout(readTimeout);

        return clientHttpRequestFactory;
    }
}
