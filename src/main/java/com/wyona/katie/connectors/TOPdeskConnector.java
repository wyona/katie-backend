package com.wyona.katie.connectors;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.wyona.katie.models.*;
import com.wyona.katie.services.BackgroundProcessService;
import com.wyona.katie.services.ClassificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * TOPdesk Connector
 */
@Slf4j
@Component
public class TOPdeskConnector implements Connector {

    @Autowired
    private BackgroundProcessService backgroundProcessService;

    @Autowired
    private ClassificationService classificationService;

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        List<Hit> hits = new ArrayList<Hit>();

        if (false) {
            log.info("Get answers from TOPdesk connector ...");

            // INFO: https://developers.topdesk.com/explorer/?page=general#/search/get_search
            String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/search?index=incidents&query=" + question.getSentence();
            log.info("Request URL: " + requestUrl);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders(ksMeta);
            HttpEntity<String> request = new HttpEntity<String>(headers);

            try {
                ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
                JsonNode bodyNode = response.getBody();
                log.info("JSON response: " + bodyNode);

            /*
            JsonNode dataNode = bodyNode.get("data");
            JsonNode getNode = dataNode.get("Get");
            JsonNode articlesNode = getNode.get("Articles");
            if (articlesNode.isArray()) {
                for (int i = 0; i < articlesNode.size(); i++) {
                    JsonNode resultNode = articlesNode.get(i);
                    String _answer = resultNode.get("title").asText() + " --- " + resultNode.get("text").asText();
                    String url = resultNode.get("url").asText();
                    JsonNode additionalNode = resultNode.get("_additional");
                    double score = additionalNode.get("distance").asDouble();
                    Answer answer = new Answer(question.getSentence(), _answer, null, url, null, null, null, null, null, null, null, null, null, null, true, null, false, null);
                    Hit hit = new Hit(answer, score);
                    hits.add(hit);
                }
            }
            */
            } catch (HttpClientErrorException e) {
                log.error(e.getMessage(), e);
                if (e.getRawStatusCode() == 403) {
                    Hit hit = new Hit(getAnswerContainingErrorMsg(question.getSentence(), "Katie is not authorized to access TOPdesk service '" + ksMeta.getName() + "' configured within Katie domain '" + ksMeta.getDomainId() + "'!"), 0.0);
                    hits.add(hit);
                }
                // INFO: Do not return null
            } catch (HttpServerErrorException e) {
                log.error(e.getMessage(), e);
                if (e.getRawStatusCode() == 500) {
                    Hit hit = new Hit(getAnswerContainingErrorMsg(question.getSentence(), "Katie received an Internal Server Error from TOPdesk service '" + ksMeta.getName() + "' configured within Katie domain '" + ksMeta.getDomainId() + "'!"), 0.0);
                    hits.add(hit);
                }
                // INFO: Do not return null
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                // INFO: Do not return null
                Hit hit = new Hit(getAnswerContainingErrorMsg(question.getSentence(), e.getMessage()), 0.0);
                hits.add(hit);
            }
        }

        return hits.toArray(new Hit[0]);
    }

    /**
     * @see Connector#update(Context, KnowledgeSourceMeta, WebhookPayload, String)
     */
    public List<Answer> update(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayload payload, String processId) {
        WebhookPayloadTOPdesk pl = (WebhookPayloadTOPdesk) payload;

        //int requestType = 0;
        int requestType = 1;
        //int requestType = 2;

        if (requestType == 2) {
            boolean visibleReplies = false;
            String incidentId = pl.getIncidentId();
            String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents/number/" + incidentId + "/progresstrail";
            JsonNode bodyNode = getData(requestUrl, ksMeta, processId);
            if (bodyNode.isArray()) {
                backgroundProcessService.updateProcessStatus(processId, "Incident contains " + bodyNode.size() + " answers.");
                for (int i = 0; i < bodyNode.size(); i++) {
                    JsonNode entryNode = bodyNode.get(i);
                    boolean invisibleForCaller = entryNode.get("invisibleForCaller").asBoolean();
                    if (!invisibleForCaller) {
                        if (entryNode.has("memoText")) {
                            visibleReplies = true;
                            String _answer = entryNode.get("memoText").asText();
                            log.info("Response to user: " + _answer);
                            Answer answer = new Answer(null, _answer, null, null, null, null, null, null, null, null, null, null, null, null, true, null, false, null);
                            // TODO: Set chosenAnswer
                        }
                    }
                }

                if (!visibleReplies) {
                    log.warn("Incident '" + incidentId + "' does not contain any visible replies yet.");
                }
            }
        } else if (requestType == 1) {
            String incidentId = pl.getIncidentId();

            TextItem sample = getIncident(incidentId, ksMeta, processId);

            try {
                // INFO: Train classifier
                TextItem[] samples = new TextItem[1];
                samples[0] = sample;
                classificationService.train(domain, samples);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else if (requestType == 0) {
            // https://developers.topdesk.com/explorer/?page=incident#/incident/get_incidents
            String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents?fields=number&pageSize=10";
            JsonNode bodyNode = getData(requestUrl, ksMeta, processId);
            log.info("TODO: Get individual incidents ...");
        } else {
            log.warn("No such request type '" + requestType + "' implemented!");
        }

        return null;
    }

    /**
     *
     */
    private TextItem getIncident(String incidentId, KnowledgeSourceMeta ksMeta, String processId) {
        String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents/number/" + incidentId;
        JsonNode bodyNode = getData(requestUrl, ksMeta, processId);
        String logMsg = "Get categories and answer(s) of TOPdesk incident '" + incidentId + "' ...";
        log.info(logMsg);
        backgroundProcessService.updateProcessStatus(processId, logMsg);

        String humanRequest = bodyNode.get("request").asText();
        log.info("Human request: " + humanRequest);

        JsonNode categoryNode = bodyNode.get("category");
        Classification category = new Classification(categoryNode.get("name").asText(), categoryNode.get("id").asText());
        log.info("Category: " + category.getTerm());

        JsonNode subcategoryNode = bodyNode.get("subcategory");
        Classification subcategory = new Classification(subcategoryNode.get("name").asText(), subcategoryNode.get("id").asText());
        log.info("Subcategory: " + subcategory.getTerm());

        return new TextItem(humanRequest, getLabel(category, subcategory));
    }

    /**
     * Get data from TOPdesk
     */
    private JsonNode getData(String url, KnowledgeSourceMeta ksMeta, String processId) {
        log.info("Request URL: " + url);
        backgroundProcessService.updateProcessStatus(processId, "Request " + url);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders(ksMeta);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);
            return bodyNode;
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 404) {
                String logMsg = "No such resource '" + url + "'!";
                log.error(logMsg);
                backgroundProcessService.updateProcessStatus(processId, logMsg, BackgroundProcessStatusType.ERROR);
            }
            if (e.getRawStatusCode() == 401) {
                backgroundProcessService.updateProcessStatus(processId, "Authentication failed", BackgroundProcessStatusType.ERROR);
            }
            if (e.getRawStatusCode() == 403) {
            }
            log.error(e.getMessage(), e);
            backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
            // INFO: Do not return null
        } catch(HttpServerErrorException e) {
            log.error(e.getMessage(), e);
            if (e.getRawStatusCode() == 500) {
            }
            backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
            // INFO: Do not return null
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
            // INFO: Do not return null
        }

        return null;
    }

    /**
     *
     */
    private Classification getLabel(Classification category, Classification subcategory) {
        String className = category.getTerm() + ", " + subcategory.getTerm();
        String classId = category.getId() + "-" + subcategory.getId();
        Classification classification = new Classification(className, classId);
        return classification;
    }

    /**
     *
     */
    private Answer getAnswerContainingErrorMsg(String question, String errorMsg) {
        String url = null;
        Answer answer = new Answer(question, errorMsg, null, url, null, null, null, null, null, null, null, null, null, null, true, null, false, null);
        return answer;
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders(KnowledgeSourceMeta ksMeta) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        //log.info("Basic Auth Credentials: U: " + ksMeta.getTopDeskUsername() + ", P: " + ksMeta.getTopDeskAPIPassword());
        headers.setBasicAuth(ksMeta.getTopDeskUsername(), ksMeta.getTopDeskAPIPassword());

        return headers;
    }
}
