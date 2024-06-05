package com.wyona.katie.connectors;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.wyona.katie.handlers.mcc.MulticlassTextClassifier;
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

    private static final String CATEGORY_SUBCATEGORY_SEPARATOR = "_";

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

        // TODO: Make request types configurable using payload
        //int requestType = 0; // INFO: Import batch of incidents, e.g. 1000 incidents
        //int requestType = 1; // INFO: Import one particular incident
        //int requestType = 2; // INFO: Get visible replies of a particular incident
        int requestType = 3; // INFO: Sync categories / subcategories

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
            try {
                TextSample sample = getIncidentAsClassificationSample(incidentId, ksMeta, processId);
                if (sample != null) {
                    classificationService.importSample(domain, sample);
                } else {
                    log.warn("Incident '" + incidentId + "' not imported.");
                }
            } catch (Exception e) {
                backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
                log.error(e.getMessage(), e);
            }
        } else if (requestType == 0) {
            // INFO: See "Returns a list of incidents" https://developers.topdesk.com/explorer/?page=incident#/incident/get_incidents
            int offset = 0; // TODO: Introduce pagination
            int limit = ksMeta.getTopDeskIncidentsRetrievalLimit();
            backgroundProcessService.updateProcessStatus(processId, "Get maximum " + limit + " incidents as classification training samples ...");
            String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents?fields=number&pageStart=" + offset + "&pageSize=" + limit;
            JsonNode bodyNode = getData(requestUrl, ksMeta, processId);
            log.info("Get individual incidents ...");
            if (bodyNode.isArray()) {
                // TODO: Consider concurrent requests, but beware of scalability of TOPdesk!
                for (int i = 0; i < bodyNode.size(); i++) {
                    JsonNode numberNode = bodyNode.get(i);
                    String incidentNumber = numberNode.get("number").asText();
                    log.info("Get incident '" + incidentNumber + "' as classification training sample ...");
                    try {
                        TextSample sample = getIncidentAsClassificationSample(incidentNumber, ksMeta, processId);
                        if (sample != null) {
                            classificationService.importSample(domain, sample);
                        } else {
                            log.warn("Incident '" + incidentNumber + "' not imported.");
                        }
                    } catch (Exception e) {
                        backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
                        log.error(e.getMessage(), e);
                    }
                }

                boolean trainClassifier = false;
                if (trainClassifier) {
                    backgroundProcessService.updateProcessStatus(processId, "Train classifier with imported samples ...");
                } else {
                    backgroundProcessService.updateProcessStatus(processId, "Classifier training disabled.");
                }
                try {
                    //classificationService.retrain(domain, 80, null, null); // TODO: Do not start another thread
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } else if (requestType == 3) {
            List<String> subcategoryIDs = new ArrayList<>();

            backgroundProcessService.updateProcessStatus(processId, "Sync categories / subcategories ...");
            String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents/subcategories";
            JsonNode bodyNode = getData(requestUrl, ksMeta, processId);
            if (bodyNode.isArray()) {
                for (int i = 0; i < bodyNode.size(); i++) {
                    JsonNode subcategoryNode = bodyNode.get(i);
                    log.info("Subcategory Id: " + subcategoryNode.get("id").asText());
                    JsonNode categoryNode = subcategoryNode.get("category");
                    subcategoryIDs.add(categoryNode.get("id").asText() + CATEGORY_SUBCATEGORY_SEPARATOR + subcategoryNode.get("id").asText());
                }
            }

            try {
                ClassificationDataset dataset = classificationService.getDataset(domain, true, 0, 10000);
                Classification[] labels = dataset.getLabels();
                boolean labelsDeleted = false;
                for (Classification label : labels) {
                    log.info("Label Id: " + label.getId());
                    boolean labelExistsInTopDesk = false;
                    for (String id : subcategoryIDs) {
                        if (id.equals(label.getId())) {
                            labelExistsInTopDesk = true;
                            log.info("Label exists in TOPdesk: " + label.getId() + ", " + label.getTerm());
                            break;
                        }
                    }
                    if (!labelExistsInTopDesk) {
                        labelsDeleted = true;
                        classificationService.removeClassification(domain, label);
                        backgroundProcessService.updateProcessStatus(processId, "Label '" + label.getTerm() + "' removed from Classifier.");
                    }
                }

                // TODO: Implement batch removal and move retraining into classification service
                if (labelsDeleted) {
                    backgroundProcessService.updateProcessStatus(processId, "Retrain classifier ...");
                    MulticlassTextClassifier classifier = classificationService.getClassifier(domain.getClassifierImpl());
                    classifier.retrain(domain, processId);
                } else {
                    backgroundProcessService.updateProcessStatus(processId, "No labels deleted.");
                }

                // INFO: Check for categories / subcategories
                for (String subcategoryId : subcategoryIDs) {
                    boolean subcategoryIsNew = true;
                    for (Classification label : labels) {
                        if (label.getId().equals(subcategoryId)) {
                            subcategoryIsNew = false;
                            break;
                        }
                    }
                    if (subcategoryIsNew) {
                        backgroundProcessService.updateProcessStatus(processId, "New category / subcategory detected: " + subcategoryId);
                        // TODO: Add new category / subcategory as classification
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.warn("No such request type '" + requestType + "' implemented!");
        }

        return null;
    }

    /**
     * Generate text sample from incident
     */
    private TextSample getIncidentAsClassificationSample(String incidentId, KnowledgeSourceMeta ksMeta, String processId) throws Exception{
        String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents/number/" + incidentId;
        JsonNode bodyNode = getData(requestUrl, ksMeta, processId);
        String logMsg = "Get categories and answer(s) of TOPdesk incident '" + incidentId + "' ...";
        log.info(logMsg);
        backgroundProcessService.updateProcessStatus(processId, logMsg);

        String humanRequest = bodyNode.get("request").asText();
        log.info("Human request: " + humanRequest);

        Classification category  = null;
        if (bodyNode.hasNonNull("category")) {
            JsonNode categoryNode = bodyNode.get("category");
            category = new Classification(categoryNode.get("name").asText(), categoryNode.get("id").asText(), null);
            log.info("Category: " + category.getTerm());
        } else {
            String msg = "Incident '" + incidentId + "' does not have a category!";
            backgroundProcessService.updateProcessStatus(processId, msg, BackgroundProcessStatusType.WARN);
            return null;
        }

        Classification subcategory = null;
        if (bodyNode.hasNonNull("subcategory")) {
            JsonNode subcategoryNode = bodyNode.get("subcategory");
            subcategory = new Classification(subcategoryNode.get("name").asText(), subcategoryNode.get("id").asText(), null);
        } else {
            String msg = "Incident '" + incidentId + "' does not have a subcategory!";
            backgroundProcessService.updateProcessStatus(processId, msg, BackgroundProcessStatusType.WARN);
            return null;
        }
        log.info("Subcategory: " + subcategory.getTerm());

        return new TextSample(incidentId, humanRequest, getLabel(category, subcategory));
    }

    /**
     * Get data from TOPdesk
     * @param url request URL, e.g. "https://topdesk.wyona.com/tas/api/incidents?fields=number&pageStart=0&pageSize=100"
     * @param processId Background process Id
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
     * Combine category and subcategory to one label
     */
    private Classification getLabel(Classification category, Classification subcategory) {
        String className = category.getTerm() + ", " + subcategory.getTerm();
        String classId = category.getId() + CATEGORY_SUBCATEGORY_SEPARATOR + subcategory.getId();
        Classification classification = new Classification(className, classId, null);
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
