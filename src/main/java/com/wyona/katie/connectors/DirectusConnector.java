package com.wyona.katie.connectors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.wyona.katie.models.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 *
 */
@Slf4j
@Component
public class DirectusConnector implements Connector {

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("Get answers from Directus connector ...");
        List<Hit> hits = new ArrayList<Hit>();

        /* INFO: Do not return answers from Directus, because Katie is indexing the Directus content itself
        // TODO: Use search URL
        String requestUrl = BASE_URL + "/items/" + COLLECTION + "/?filter[id]=5&fields=*,user_created.first_name,user_created.last_name,user_created.title";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            log.info("Get answers from Directus: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);

            //String mockAnswer = "Mock answer";
            String mockAnswer = "Onlinekommentar.ch, die gemeinnützige Plattform für Open-Access-Kommentare";
            String url = BASE_URL + "/assets/cf213721-82db-4fd2-91f7-06279af3e9c5?download";
            Answer answer = new Answer(question.getSentence(), mockAnswer, null, url, null, null, null, null, null, null, null, null, null, null, true, null, false, null);
            Hit hit = new Hit(answer, -3.14);
            hits.add(hit);
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 403) {
                log.error("Not authorized to access '" + requestUrl + "'!");
            } else {
                log.error(e.getMessage(), e);
            }
            // INFO: Do not return null
        }

         */

        return hits.toArray(new Hit[0]);
    }

    /**
     * @see Connector#update(Context, KnowledgeSourceMeta, WebhookPayload, String)
     */
    public List<Answer> update(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayload payload, String processId) {
        log.info("TODO: Implement");
        return null;
    }

    /**
     * Contextualize document
     * @param id Directus document Id
     */
    public Answer contextualize(String id, KnowledgeSourceMeta ksMeta) {
        String requestUrl = ksMeta.getDirectusBaseUrl() + "/items/" + ksMeta.getDirectusCollection() + "/?filter[id]=" + id + "&fields=*,user_created.first_name,user_created.last_name,user_created.title";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            log.info("Get item from Directus: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);
            JsonNode dataNode = bodyNode.get("data");
            if (dataNode.isArray() && dataNode.size() > 0) {
                JsonNode itemNode = dataNode.get(0);

                // TODO: Make field names configurable
                String originalQuestion = itemNode.get("Titel").asText();
                String docId = itemNode.get("Datei").asText();

                /*
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode docNode = mapper.createObjectNode();
                docNode.put("id", id);
                docNode.put("Titel", originalQuestion);

                String answer = docNode.toString();

                 */
                String answer = itemNode.toString();

                // TODO: Add classifications, see https://katie.qa/#/trained-questions-answers/20275809-8c17-4f3d-a3f7-0657c1435e54/003c06a2-24dd-417b-9246-9b42f3b896ec/edit

                String url = ksMeta.getDirectusBaseUrl() + "/assets/" + docId + "?download";
                // TODO: Consider contextualizing documented referenced by URL and returning multiple QnAs

                Date itemCreated = new Date(); // TODO: Field "Erschienen_am":"2021-12-17"
                Date itemModified = new Date(); // TODO: Field "date_updated":"2023-04-17T21:11:35.105Z"

                Answer _answer = new Answer(null, answer, null, url, null, null, null, null, itemModified, null, null, null, originalQuestion, itemCreated, true, null, false, null);
                return _answer;
            } else {
                log.warn("Node 'data' does not contain any items, maybe because item '" + id + "' is not published.");
            }
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 403) {
                log.error("Not authorized to access '" + requestUrl + "'!");
            } else {
                log.error(e.getMessage(), e);
            }
        }

        return null;
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json;odata=verbose");

        if (false) { // TODO: Check for token
            try {
                headers.setBearerAuth(getToken());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }


        return headers;
    }

    /**
     *
     */
    private String getToken() {
        return "TODO";
    }
}
