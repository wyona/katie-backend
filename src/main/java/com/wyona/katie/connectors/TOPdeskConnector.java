package com.wyona.katie.connectors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        if (pl.getRequestType() == 2) {
            String incidentId = pl.getIncidentId();

            // TODO: Use getVisibleReplies(...)
            //List<String> visibleReplies = getVisibleReplies(incidentId, processId, ksMeta);

            backgroundProcessService.updateProcessStatus(processId, "Get visible replies of TOPdesk incident '" + incidentId + "' ...");
            String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents/number/" + incidentId + "/progresstrail";
            JsonNode bodyNode = getData(requestUrl, ksMeta, processId);
            if (bodyNode.isArray()) {
                backgroundProcessService.updateProcessStatus(processId, "Incident contains " + bodyNode.size() + " answers.");
                boolean visibleReplies = false;
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
        } else if (pl.getRequestType() == 1) {
            backgroundProcessService.updateProcessStatus(processId, "Import one particular incident ...");
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
        } else if (pl.getRequestType() == 4) {
            getAnalyticsOfIncidents(processId, ksMeta);
        } else if (pl.getRequestType() == 0) {
            backgroundProcessService.updateProcessStatus(processId, "Import batch of incidents ...");
            // TODO: Replace code below by getting all subcategories and then get a certain number of incidents per subcategory as training samples
            // INFO: See "Returns a list of incidents" https://developers.topdesk.com/explorer/?page=incident#/incident/get_incidents
            int offset = 0; // TODO: Introduce pagination
            int limit = ksMeta.getTopDeskIncidentsRetrievalLimit();
            backgroundProcessService.updateProcessStatus(processId, "Get maximum " + limit + " incidents as classification training samples ...");

            // TODO: Use getListOfIncidentIDs(...)
            // List<String> ids = getListOfIncidentIDs(offset, limit, processId, ksMeta);

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
        } else if (pl.getRequestType() == 3) {
            boolean testRun = false; // INFO: When true, then do not delete obsolete or add new categories and also do not retrain classifier
            if (pl.getIsTestRun() != null) {
                testRun = pl.getIsTestRun();
                if (testRun) {
                    backgroundProcessService.updateProcessStatus(processId, "Test run is performed that does not make any changes");
                }
            }
            List<Classification> topDeskLabels = new ArrayList<>();

            backgroundProcessService.updateProcessStatus(processId, "Sync categories / subcategories ...");

            // INFO: Get all categories and subcategories from TOPdesk, see https://developers.topdesk.com/explorer/?page=incident#/general/get_incidents_subcategories
            String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents/subcategories";
            JsonNode bodyNode = getData(requestUrl, ksMeta, processId);
            if (bodyNode.isArray()) {
                for (int i = 0; i < bodyNode.size(); i++) {
                    JsonNode subcategoryNode = bodyNode.get(i);
                    log.info("Subcategory Id: " + subcategoryNode.get("id").asText());
                    JsonNode categoryNode = subcategoryNode.get("category");
                    String labelName = categoryNode.get("name").asText() + ", " + subcategoryNode.get("name").asText();
                    String foreignId = categoryNode.get("id").asText() + CATEGORY_SUBCATEGORY_SEPARATOR + subcategoryNode.get("id").asText();
                    Classification topDeskLabel = new Classification(labelName, foreignId, null);
                    topDeskLabels.add(topDeskLabel);
                }
            }

            try {
                ClassificationDataset dataset = classificationService.getDataset(domain, true, 0, 10000);
                Classification[] labels = dataset.getLabels();

                // INFO: Check for obsolete categories / subcategories
                boolean labelsDeleted = false;
                for (Classification label : labels) {
                    log.info("Label Id: " + label.getId());
                    boolean labelExistsInTopDesk = false;
                    for (Classification topDeskLabel : topDeskLabels) {
                        if (topDeskLabel.getId().equals(label.getId())) {
                            labelExistsInTopDesk = true;
                            log.info("Label exists in TOPdesk: " + label.getId() + ", " + label.getTerm());
                            break;
                        }
                    }
                    if (!labelExistsInTopDesk) {
                        if (!testRun) {
                            labelsDeleted = true;
                            classificationService.removeClassification(domain, label);
                            backgroundProcessService.updateProcessStatus(processId, "Label '" + label.getTerm() + "' removed, because label does not exist anymore in TOPdesk.");
                        } else {
                            backgroundProcessService.updateProcessStatus(processId, "Test run, therefore do not remove label '" + label.getTerm() + "' (" + label.getId() + ").");
                        }
                    }
                }

                if (!labelsDeleted) {
                    backgroundProcessService.updateProcessStatus(processId, "No labels deleted.");
                }

                // INFO: Check for new categories / subcategories
                boolean labelsAdded = false;
                for (Classification topDeskLabel : topDeskLabels) {
                    boolean subcategoryIsNew = true;
                    for (Classification label : labels) {
                        if (label.getId().equals(topDeskLabel.getId())) {
                            subcategoryIsNew = false;
                            break;
                        }
                    }
                    if (subcategoryIsNew) {
                        backgroundProcessService.updateProcessStatus(processId, "New category / subcategory detected: " + topDeskLabel.getTerm());

                        String subcategoryId = topDeskLabel.getId().split(CATEGORY_SUBCATEGORY_SEPARATOR)[1];
                        int maxNumberOfSamplesPerCategory = 3; // TODO: Make configurable
                        if (testRun) {
                            backgroundProcessService.updateProcessStatus(processId, "Test run, but anyway, get maximum " + maxNumberOfSamplesPerCategory + " samples from TOPdesk for category / subcategory '" + topDeskLabel.getTerm() + "' ...");
                        } else {
                            backgroundProcessService.updateProcessStatus(processId, "Get maximum " + maxNumberOfSamplesPerCategory + " samples from TOPdesk for category / subcategory '" + topDeskLabel.getTerm() + "' ...");
                        }
                        List<TextSample> samples = getIncidentsAsClassificationSamplePerSubcategory(subcategoryId, maxNumberOfSamplesPerCategory, ksMeta, processId);
                        if (samples.size() > 0) {
                            for (TextSample sample : samples) {
                                if (!testRun) {
                                    labelsAdded = true;
                                    classificationService.importSample(domain, sample);
                                } else {
                                    backgroundProcessService.updateProcessStatus(processId, "Test run, therefore do not import sample for new label '" + topDeskLabel.getTerm() + "'.");
                                }
                            }
                        } else {
                            // INFO: Category will not be added, because no samples available!
                            backgroundProcessService.updateProcessStatus(processId, "No text samples could be retrieved for category / subcategory '" + topDeskLabel.getTerm() + "' (" + topDeskLabel.getId() + ")!", BackgroundProcessStatusType.WARN);
                        }
                    }
                }

                if (!labelsAdded) {
                    backgroundProcessService.updateProcessStatus(processId, "No labels added.");
                }

                // TODO: Implement batch removal / ingestion and move retraining into classification service
                if (labelsDeleted || labelsAdded) {
                    if (!testRun) {
                        backgroundProcessService.updateProcessStatus(processId, "Retrain classifier ...");
                        MulticlassTextClassifier classifier = classificationService.getClassifier(domain.getClassifierImpl());
                        classifier.retrain(domain, processId);
                    } else {
                        backgroundProcessService.updateProcessStatus(processId, "Test run, therefore do not retrain classifier.");
                    }
                } else {
                    backgroundProcessService.updateProcessStatus(processId, "Classifier not retrained, because neither labels removed nor added.");
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.warn("No such request type '" + pl.getRequestType() + "' implemented!");
        }

        return null;
    }

    /**
     *
     */
    public void addComment(String incidentId, String processId, KnowledgeSourceMeta ksMeta, String text) {
        // TODO: Add text as incident comment
        String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents/number/" + incidentId + "/progresstrail";
        JsonNode bodyNode = getData(requestUrl, ksMeta, processId);
    }

    /**
     *
     */
    private void getAnalyticsOfIncidents(String processId, KnowledgeSourceMeta ksMeta) {
        int offset = 0; // TODO: Introduce pagination
        int limit = ksMeta.getTopDeskIncidentsRetrievalLimit();
        backgroundProcessService.updateProcessStatus(processId, "Get Analytics of incidents (Batch size: " + limit + ") ...");

        Map<Integer, Integer> numberOfTotalMessages = new HashMap<>();
        Map<Integer, Integer> numberOfVisibleMessages = new HashMap<>();

        List<String> ids = getListOfIncidentIDs(offset, limit, processId, ksMeta);
        for (String id : ids) {
            log.info("Get TOPdesk incident '" + id + "' to analyze ...");
            Integer[] numbers = getNumberOfMessagesOfIncident(id, processId, ksMeta);
            Integer totalNumberOfMessagesOfIncident = numbers[0];
            Integer numberOfVisibleMessagesOfIncident= numbers[1];
            backgroundProcessService.updateProcessStatus(processId, "Incident '" + id + "' has " + numberOfVisibleMessagesOfIncident + " visible messages.");
            log.info("Incident '" + id + "' has " + numberOfVisibleMessagesOfIncident + " visible messages.");

            numberOfTotalMessages = increaseCounter(numberOfTotalMessages, totalNumberOfMessagesOfIncident);
            StringBuilder distributionTotalMessages = new StringBuilder();
            for (Map.Entry<Integer, Integer> entry : numberOfTotalMessages.entrySet()) {
                distributionTotalMessages.append("(" + entry.getKey() + "," + entry.getValue() + ") ");
            }
            backgroundProcessService.updateProcessStatus(processId, "Distribution of total messages: " + distributionTotalMessages.toString());

            numberOfVisibleMessages = increaseCounter(numberOfVisibleMessages, numberOfVisibleMessagesOfIncident);
            StringBuilder distributionVisibleMessages = new StringBuilder();
            for (Map.Entry<Integer, Integer> entry : numberOfVisibleMessages.entrySet()) {
                distributionVisibleMessages.append("(" + entry.getKey() + "," + entry.getValue() + ") ");
            }
            backgroundProcessService.updateProcessStatus(processId, "Distribution of visible messages: " + distributionVisibleMessages.toString());
        }
    }

    /**
     * Increase Y value
     * @param counter Counter, e.g. "(0,0) (1,3) (2, 5) (3, 4) (4, 6) ... (12, 1)"
     * @param value X value
     * @return updated counter
     */
    private Map<Integer, Integer> increaseCounter(Map<Integer, Integer> counter, Integer value) {
        for (Map.Entry<Integer, Integer> entry : counter.entrySet()) {
            if (entry.getKey() == value) {
                counter.put(value, entry.getValue() + 1);
                return counter;
            }
        }

        counter.put(value, 1);

        return counter;
    }

    /**
     * Get total number of messages and number of visible messages of a incident
     * @return total number of messages and number of visible messages
     */
    private Integer[] getNumberOfMessagesOfIncident(String incidentId, String processId, KnowledgeSourceMeta ksMeta) {
        Integer totalNumberOfMessages = 0;
        Integer numberOfVisibleMessages= 0;

        List<String> visibleReplies = new ArrayList<>();

        backgroundProcessService.updateProcessStatus(processId, "Get visible messages of TOPdesk incident '" + incidentId + "' ...");
        String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents/number/" + incidentId + "/progresstrail";
        JsonNode bodyNode = getData(requestUrl, ksMeta, processId);
        if (bodyNode != null && bodyNode.isArray()) {
            totalNumberOfMessages = bodyNode.size();
            backgroundProcessService.updateProcessStatus(processId, "Incident '" + incidentId + "' has a total of " + totalNumberOfMessages + " messages.");
            for (int i = 0; i < bodyNode.size(); i++) {
                JsonNode entryNode = bodyNode.get(i);
                boolean invisibleForCaller = entryNode.get("invisibleForCaller").asBoolean();
                if (!invisibleForCaller) {
                    if (entryNode.has("memoText")) {
                        String _answer = entryNode.get("memoText").asText();
                        log.info("Response to user: " + _answer);
                        visibleReplies.add(_answer);
                    }
                }
            }

            if (visibleReplies.size() == 0) {
                log.warn("Incident '" + incidentId + "' does not contain any visible messages yet.");
            } else {
                numberOfVisibleMessages = visibleReplies.size();
            }
        } else {
            log.warn("No body received for " + requestUrl);
        }

        Integer[] numbers = new Integer[2];
        numbers[0] = totalNumberOfMessages;
        numbers[1] = numberOfVisibleMessages;

        return numbers;
    }

    /**
     *
     */
    private List<String> getListOfIncidentIDs(int offset, int limit, String processId, KnowledgeSourceMeta ksMeta) {
        List<String> ids = new ArrayList<>();
        String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents?fields=number&pageStart=" + offset + "&pageSize=" + limit;
        JsonNode bodyNode = getData(requestUrl, ksMeta, processId);
        if (bodyNode.isArray()) {
            // TODO: Consider concurrent requests, but beware of scalability of TOPdesk!
            for (int i = 0; i < bodyNode.size(); i++) {
                JsonNode numberNode = bodyNode.get(i);
                String incidentNumber = numberNode.get("number").asText();
                ids.add(incidentNumber);
            }
        } else {
            log.error("No TOPdesk incidents available!");
        }
        return ids;
    }

    /**
     * Get incidents for a particular subcategory and return as text samples
     * @param subcategory Subcategory Id, e.g. "545ac83b-4d79-4386-9e5f-cc5213ede3cf"
     * @param maxNumberOfSamples Maximum number of samples
     */
    private List<TextSample> getIncidentsAsClassificationSamplePerSubcategory(String subcategory, int maxNumberOfSamples, KnowledgeSourceMeta ksMeta, String processId) throws Exception {
        List<TextSample> samples = new ArrayList<>();
        String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents?query=subcategory.id==" + subcategory + "&fields=number,status&pageStart=0&pageSize=" + maxNumberOfSamples;
        JsonNode rootNode = getData(requestUrl, ksMeta, processId);
        if (rootNode != null && rootNode.isArray() && rootNode.size() > 0) {
            for (int i = 0; i < rootNode.size(); i++) {
                JsonNode entryNode = rootNode.get(i);
                String incidentId = entryNode.get("number").asText();
                String incidentStatus = entryNode.get("status").asText();
                // TODO: Consider checking status firstLine / secondLine
                TextSample sample = getIncidentAsClassificationSample(incidentId, ksMeta, processId);
                samples.add(sample);
            }
        }
        return samples;
    }

    /**
     * Generate text sample from incident
     * @param incidentId Incident Id, e.g. "I-240605-0858"
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
     * @param ksMeta TODO
     * @param processId Background process Id
     * @return TODO
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
        //headers.set("Accept", "application/json;odata=verbose");

        //log.info("Basic Auth Credentials: U: " + ksMeta.getTopDeskUsername() + ", P: " + ksMeta.getTopDeskAPIPassword());
        headers.setBasicAuth(ksMeta.getTopDeskUsername(), ksMeta.getTopDeskAPIPassword());

        return headers;
    }
}
