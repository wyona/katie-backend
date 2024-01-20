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
public class ConfluenceConnector implements Connector {

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("Do not return answers from Confluence, because Katie is indexing the Confluence content by itself");
        List<Hit> hits = new ArrayList<Hit>();
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
     * @param id Confluence document Id, e.g. "167182562"
     */
    public Answer contextualize(String id, KnowledgeSourceMeta ksMeta) {
        // https://developer.atlassian.com/cloud/confluence/rest/v2/intro/
        // https://developer.atlassian.com/cloud/confluence/rest/v2/api-group-page/#api-pages-id-get
        // https://wyona.atlassian.net/wiki/api/v2/pages/167182562?body-format=storage

        String bodyFormat = "storage";
        //String bodyFormat = "atlas_doc_format";

        // INFO: https://developer.atlassian.com/cloud/confluence/rest/v2/intro/
        String requestUrl = ksMeta.getConfluenceBaseUrl() + "/wiki/api/v2/pages/" + id + "?body-format=" + bodyFormat;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders(ksMeta);
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            log.info("Get page from Confluence: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);

            String originalQuestion = bodyNode.get("title").asText();
            String spaceId = bodyNode.get("spaceId").asText();

            // TODO: Add classifications, see https://katie.qa/#/trained-questions-answers/20275809-8c17-4f3d-a3f7-0657c1435e54/003c06a2-24dd-417b-9246-9b42f3b896ec/edit

            String url = null; // INFO: Will be set using payload

            Date itemCreated = new Date(); // TODO: Field "createdAt"
            Date itemModified = new Date(); // TODO

            String answer = bodyNode.get("body").get("storage").get("value").asText();

            Answer _answer = new Answer(null, answer, null, url, null, null, null, null, itemModified, null, null, null, originalQuestion, itemCreated, true, null, false, null);
            return _answer;
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 401 || e.getRawStatusCode() == 403) {
                log.error("Not authorized to access '" + requestUrl + "'!");
            }
            log.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders(KnowledgeSourceMeta ksMeta) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        try {
            // https://developer.atlassian.com/cloud/jira/platform/basic-auth-for-rest-apis/
            headers.setBasicAuth(ksMeta.getConfluenceUsername(), ksMeta.getConfluenceToken());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return headers;
    }
}
