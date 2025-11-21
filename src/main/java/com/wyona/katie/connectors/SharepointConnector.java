package com.wyona.katie.connectors;

import com.wyona.katie.config.RestProxyTemplate;
import com.wyona.katie.integrations.msteams.services.MicrosoftAuthorizationService;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.wyona.katie.models.*;
import com.wyona.katie.services.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Search inside SharePoint and sync SharePoint
 */
@Slf4j
@Component
public class SharepointConnector implements Connector {

    @Autowired
    MicrosoftAuthorizationService microsoftAuthorizationService;

    @Autowired
    RestProxyTemplate restProxyTemplate;

    @Autowired
    private BackgroundProcessService backgroundProcessService;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private ContextService domainService;

    @Autowired
    SegmentationService segmentationService;

    private final String MS_GRAPH_BASE_URL = "https://graph.microsoft.com";

    final int MAX_QNAS_PER_ITEM = 3; // 50 // TODO: Make configurable!

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("Get answers from Sharepoint connector ...");
        List<Hit> hits = new ArrayList<Hit>();

        // INFO: https://learn.microsoft.com/en-us/sharepoint/dev/general-development/sharepoint-search-rest-api-overview
        // https://learn.microsoft.com/en-us/sharepoint/dev/sp-add-ins/get-to-know-the-sharepoint-rest-service
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode bodyRequest = mapper.createObjectNode();
        ObjectNode requestNode = mapper.createObjectNode();
        requestNode.put("Querytext", question.getSentence());
        requestNode.put("RowLimit", 20);
        requestNode.put("ClientType", "ContentSearchRegular");
        bodyRequest.put("request", requestNode);

        log.info("Request body: " + bodyRequest.toString());

        String requestUrl = "https://wyona.sharepoint.com/sites/KatieTest/_api/search/postquery";
        //String requestUrl = "https://wyona.sharepoint.com/sites/KatieTest/_api/search/query?querytext='" + question.getSentence() + "'";

        RestTemplate restTemplate = restProxyTemplate.getRestTemplate();
        String apiToken = getAPIToken(ksMeta);
        HttpHeaders headers = getHttpHeaders(apiToken);
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Accept", "application/json;odata=verbose");
        HttpEntity<String> request = new HttpEntity<String>(bodyRequest.toString(), headers);
        //HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            log.info("Get results from " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            //ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);

            //String mockAnswer = "Mock answer";
            String mockAnswer = "Ezra is the youngest son of Michael";
            String url = "https://wyona.sharepoint.com/sites/KatieTest/SitePages/The%20sons%20of%20Michael.aspx";
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

        return hits.toArray(new Hit[0]);
    }

    /**
     * @see Connector#update(Context, KnowledgeSourceMeta, WebhookPayload, String)
     */
    public List<Answer> update(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayload payload, String processId) {
        List<Answer> qnas = new ArrayList<Answer>();

        String siteId = ksMeta.getSharepointSiteId();
        String webBaseUrl = ksMeta.getSharepointWebBaseUrl();

        String apiToken = getAPIToken(ksMeta);
        if (apiToken == null) {
            String errMsg = "Microsoft Graph API token could not be retrieved, therefore Sharepoint knowledge source '" + ksMeta.getId() + "' of domain '" + domain.getId() + "' cannot be updated!";
            log.error(errMsg);
            backgroundProcessService.updateProcessStatus(processId, errMsg, BackgroundProcessStatusType.ERROR);
            return qnas;
        }

        if (payload != null) {
            WebhookPayloadSharepoint payloadSharepoint = (WebhookPayloadSharepoint) payload;
            backgroundProcessService.updateProcessStatus(processId, "Sharepoint resource " + payloadSharepoint + " was modified.");
            log.info("Sharepoint resource " + payloadSharepoint.getValue()[0] + " was modified");

            if (ksMeta.getMsTenant().equals(payloadSharepoint.getValue()[0].getTenantId())) {
                // TODO: Check whether resource is a list
                getListDelta(siteId, payloadSharepoint.getValue()[0].getResource(), apiToken, processId);
            } else {
                log.warn("Tenant IDs do not match!");
            }
            return qnas;
        }

        backgroundProcessService.updateProcessStatus(processId, "Try to sync SharePoint site: " + siteId + " (" + webBaseUrl + ")");

        // TODO: Make configurable either by payload or meta config
        boolean retrieveSubSites = true;
        boolean retrievePages = true;
        boolean retrieveFoldersAndDocuments = true;
        boolean retrieveLists = true;
        int MAX_ITEMS = ksMeta.getMaxItems();

        try {
            List<JsonNode> siteItems = new ArrayList<JsonNode>();

            // INFO: Get list of all sub-sites (https://learn.microsoft.com/en-us/graph/api/site-list-subsites)
            if (retrieveSubSites) {
                String requestUrl = "/v1.0/sites/" + siteId + "/sites";
                siteItems = getSiteItems(siteItems, requestUrl, apiToken, processId);
            }

            // INFO: Get list of all pages
            if (retrievePages) {
                // WARN: Pages with contentType="Wiki Page" do not get listed, page with contentType="Site Page"! (see https://learn.microsoft.com/en-us/answers/questions/629474/get-wiki-page-content-from-sharepoint-using-graph)
                String requestUrl = "/beta/sites/" + siteId + "/pages";
                siteItems = getSiteItems(siteItems, requestUrl, apiToken, processId);
            }

            // INFO: Get list of all lists
            if (retrieveLists) {
                String requestUrl = "/v1.0/sites/" + siteId + "/lists";
                siteItems = getSiteItems(siteItems, requestUrl, apiToken, processId);
            }

            // INFO: Get list of all folders and documents
            if (retrieveFoldersAndDocuments) {
                // INFO: https://learn.microsoft.com/en-us/graph/api/driveitem-list-children?view=graph-rest-1.0&tabs=http#examples
                String requestUrl = "/beta/sites/" + siteId + "/drive/root/children";
                siteItems = getSiteItems(siteItems, requestUrl, apiToken, processId);
            }

            if (siteItems.size() > 0) {
                backgroundProcessService.updateProcessStatus(processId, "Total number of top level SharePoint items: " + siteItems.size());
                backgroundProcessService.updateProcessStatus(processId, "Retrieve and dump SharePoint items (pages, lists, folders, documents, ...) ...");
                Counter counterItems = new Counter();
                for (JsonNode itemNode : siteItems) {
                    if (counterItems.getValue() >= MAX_ITEMS) {
                        backgroundProcessService.updateProcessStatus(processId, "Max number of items (" + MAX_ITEMS + ") reached, therefore abort dumping.", BackgroundProcessStatusType.WARN);
                        break;
                    }

                    String itemId = itemNode.get("id").asText();
                    String webUrl = itemNode.get("webUrl").asText();

                    if (itemNode.has("pageLayout")) {
                        counterItems.increase();
                        String pageTitle = itemNode.get("title").asText();
                        log.info("Page: " + pageTitle + ", " + itemId);
                        Answer qna = getPage(siteId, itemId, apiToken, webBaseUrl, domain, processId);
                        if (qna != null) {
                            qnas.add(qna);
                        } else {
                            String msg = "No QnA could be generated for Sharepoint site '" + siteId + "' and page '" + itemId + "'!";
                            log.error(msg);
                            backgroundProcessService.updateProcessStatus(processId, msg, BackgroundProcessStatusType.ERROR);
                        }
                    } else if (itemNode.has("folder")) {
                        String folderName = itemNode.get("name").asText();
                        Answer[] qnasContainedByFolders = getDocuments(siteId, itemId, folderName, apiToken, webBaseUrl, domain, processId, MAX_ITEMS, counterItems);
                        for (Answer qna : qnasContainedByFolders) {
                            qnas.add(qna);
                        }
                    } else if (itemNode.has("file")) {
                        counterItems.increase();
                        String fileName = itemNode.get("name").asText();
                        String mimeType = itemNode.get("file").get("mimeType").asText();
                        List<Answer> _qnas = getDocument(siteId, itemId, apiToken, domain, webUrl, fileName, mimeType, processId);
                        if (_qnas != null) {
                            for (Answer qna : _qnas) {
                                qnas.add(qna);
                                // TODO: Check MAX_QNAS_PER_ITEM
                            }
                        } else {
                            String msg = "No QnA could be generated for Sharepoint site '" + siteId + "' and document '" + itemId + "'!";
                            log.error(msg);
                            backgroundProcessService.updateProcessStatus(processId, msg, BackgroundProcessStatusType.ERROR);
                        }
                    } else if (itemNode.has("list")) {
                        counterItems.increase();
                        String listName = itemNode.get("name").asText();
                        log.info("List: " + listName + ", " + itemId);
                        Answer qna = getList(siteId, itemId, apiToken, processId);
                        if (qna != null) {
                            qnas.add(qna);
                        } else {
                            String msg = "No QnA could be generated for Sharepoint site '" + siteId + "' and list '" + itemId + "' (" + listName + ")!";
                            log.error(msg);
                            backgroundProcessService.updateProcessStatus(processId, msg, BackgroundProcessStatusType.ERROR);
                        }
                    } else {
                        String msg = "No such SharePoint item type implemented: " + webUrl + " | " + itemId;
                        log.warn(msg);
                        backgroundProcessService.updateProcessStatus(processId, msg, BackgroundProcessStatusType.WARN);
                        if (itemNode.has("displayName")) {
                            String displayName = itemNode.get("displayName").asText();
                            msg = "Display name of unknown item type: " + displayName;
                            log.info(msg);
                            backgroundProcessService.updateProcessStatus(processId, msg);
                        }
                    }

                    backgroundProcessService.updateProcessStatus(processId, "Item Counter: " + counterItems);
                }
                backgroundProcessService.updateProcessStatus(processId, counterItems + " items processed.");
            } else {
                backgroundProcessService.updateProcessStatus(processId, "No SharePoint items available!", BackgroundProcessStatusType.WARN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
        }

        return qnas;
    }

    /**
     * Get items (sub-sites, lists, folders, documents, pages) from a Sharepoint site
     * @param url Item specific request URL, e.g. "/v1.0/sites/{site-id}/lists" or "/v1.0/sites/{site-id}/sites"
     */
    private List<JsonNode> getSiteItems(List<JsonNode> items, String url, String apiToken, String processId) {

        HttpHeaders headers = getHttpHeaders(apiToken);
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Accept", "application/json");
        HttpEntity<String> request = new HttpEntity<String>(headers);

        String requestUrl = MS_GRAPH_BASE_URL + url;

        RestTemplate restTemplate = restProxyTemplate.getRestTemplate();
        try {
            log.info("Get items from " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);

            JsonNode valueNode = bodyNode.get("value");
            if (valueNode.isArray() && valueNode.size() > 0) {
                for (int i = 0; i < valueNode.size(); i++) {
                    JsonNode itemNode = valueNode.get(i);
                    items.add(itemNode);
                }
            } else {
                log.warn("Not items retrieved for request '" + requestUrl + "'!");
            }
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 401 || e.getRawStatusCode() == 403) {
                String errMsg = "Not authorized to access '" + requestUrl + "'!";
                log.error(errMsg);
                backgroundProcessService.updateProcessStatus(processId, errMsg, BackgroundProcessStatusType.ERROR);
            } else {
                log.error(e.getMessage(), e);
                backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
            }
        }

        return items;
    }

    /**
     * Get Sharepoint list delta / changes
     */
    private void getListDelta(String siteId, String listId, String apiToken, String processId) {
        String contentUrl = MS_GRAPH_BASE_URL + "/v1.0/sites/" + siteId + "/lists/" + listId + "/items/delta";
        log.info("Get Sharepoint list delta / changes: " + contentUrl);
        backgroundProcessService.updateProcessStatus(processId, "Get Sharepoint list delta / changes: " + contentUrl);
        try {

            HttpHeaders headers = getHttpHeaders(apiToken);
            headers.set("Content-Type", "application/json; charset=UTF-8");
            headers.set("Accept", "application/json");
            HttpEntity<String> request = new HttpEntity<String>(headers);

            RestTemplate restTemplate = restProxyTemplate.getRestTemplate();
            try {
                ResponseEntity<JsonNode> response = restTemplate.exchange(contentUrl, HttpMethod.GET, request, JsonNode.class);
                JsonNode bodyNode = response.getBody();
                log.info("JSON response: " + bodyNode);

                /*
                JsonNode valueNode = bodyNode.get("value");
                if (valueNode.isArray() && valueNode.size() > 0) {
                    backgroundProcessService.updateProcessStatus(processId, "Total number of list items: " + valueNode.size());
                    backgroundProcessService.updateProcessStatus(processId, "Process list items ...");
                    for (int i = 0; i < valueNode.size(); i++) {
                        JsonNode itemNode = valueNode.get(i);
                        JsonNode fieldsNode = itemNode.get("fields");

                        // TODO: "Anfrage" is a custom field!
                        if (fieldsNode.has("Anfrage")) {
                            String question = fieldsNode.get("Anfrage").asText();
                            backgroundProcessService.updateProcessStatus(processId, question);
                        }
                    }
                } else {
                    backgroundProcessService.updateProcessStatus(processId, "No list items available", BackgroundProcessStatusType.WARN);
                }

                 */
            } catch(HttpClientErrorException e) {
                if (e.getRawStatusCode() == 403) {
                    log.error("Not authorized to access '" + contentUrl + "'!");
                } else {
                    log.error(e.getMessage(), e);
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Get Sharepoint list
     * @param listId List Id, e.g. "0b0db340-f8b0-4ad6-8ebd-3e165f78a2cd"
     */
    private Answer getList(String siteId, String listId, String apiToken, String processId) {
        String contentUrl = MS_GRAPH_BASE_URL + "/v1.0/sites/" + siteId + "/lists/" + listId + "/items?expand=fields";
        log.info("Dump and process list content: " + contentUrl);
        backgroundProcessService.updateProcessStatus(processId, "Dump and process list " + contentUrl);
        try {

            HttpHeaders headers = getHttpHeaders(apiToken);
            headers.set("Content-Type", "application/json; charset=UTF-8");
            headers.set("Accept", "application/json");
            HttpEntity<String> request = new HttpEntity<String>(headers);

            RestTemplate restTemplate = restProxyTemplate.getRestTemplate();
            try {
                ResponseEntity<JsonNode> response = restTemplate.exchange(contentUrl, HttpMethod.GET, request, JsonNode.class);
                JsonNode bodyNode = response.getBody();
                log.info("JSON response: " + bodyNode);

                JsonNode valueNode = bodyNode.get("value");
                if (valueNode.isArray() && valueNode.size() > 0) {
                    backgroundProcessService.updateProcessStatus(processId, "Total number of list items: " + valueNode.size());
                    backgroundProcessService.updateProcessStatus(processId, "Process list items ...");
                    for (int i = 0; i < valueNode.size(); i++) {
                        JsonNode itemNode = valueNode.get(i);
                        JsonNode fieldsNode = itemNode.get("fields");

                        // TODO: "Anfrage" is a custom field!
                        if (fieldsNode.has("Anfrage")) {
                            String question = fieldsNode.get("Anfrage").asText();
                            backgroundProcessService.updateProcessStatus(processId, question);
                        }
                    }
                } else {
                    backgroundProcessService.updateProcessStatus(processId, "No list items available", BackgroundProcessStatusType.WARN);
                }

                // TODO: HUGO

                //Answer qna = new Answer(null, description, ContentType.TEXT_PLAIN, webUrl, null, null, null, null, null, null, null, null, title, null, false, null, false, null);
                //return qna;
                return null;
            } catch(HttpClientErrorException e) {
                if (e.getRawStatusCode() == 403) {
                    log.error("Not authorized to access '" + contentUrl + "'!");
                } else {
                    log.error(e.getMessage(), e);
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * @param siteId Site Id, e.g. "ee203dc1-cfd8-4fce-8179-c11dd4db249c"
     * @param pageId Page Id, e.g. "73e548e7-57e8-4546-8a15-0560852ed55a"
     * @param webBaseUrl SharePoint web base URL, e.g. "https://wyona.sharepoint.com"
     */
    private Answer getPage(String siteId, String pageId, String apiToken, String webBaseUrl, Context domain, String processId) {
        String contentUrl = MS_GRAPH_BASE_URL + "/beta/sites/" + siteId + "/pages/" + pageId;
        try {

            HttpHeaders headers = getHttpHeaders(apiToken);
            headers.set("Content-Type", "application/json; charset=UTF-8");
            headers.set("Accept", "application/json");
            HttpEntity<String> request = new HttpEntity<String>(headers);

            RestTemplate restTemplate = restProxyTemplate.getRestTemplate();
            try {
                log.info("Get results from " + contentUrl);
                backgroundProcessService.updateProcessStatus(processId, "Get and process page: " + contentUrl);
                ResponseEntity<JsonNode> response = restTemplate.exchange(contentUrl, HttpMethod.GET, request, JsonNode.class);
                JsonNode bodyNode = response.getBody();
                log.info("JSON response: " + bodyNode);

                String title = bodyNode.get("title").asText();
                String webUrl = webBaseUrl + "/" + bodyNode.get("webUrl").asText();
                // TODO
                //File dumpFile = utilsService.dumpPageContent(domain, new URI(contentUrl), apiToken);
                ContentType contentType = null; // TODO: Get content type
                domainService.saveMetaInformation(contentUrl, webUrl, new Date(), contentType, domain);
                domainService.deletePreviouslyImportedChunks(webUrl, domain);

                String description = "NO_DESCRIPTION_AVAILABLE";
                if (bodyNode.has("description")) {
                    description = bodyNode.get("description").asText();
                    log.info("Page description: " + description);
                }

                Answer qna = new Answer(null, description, ContentType.TEXT_PLAIN, webUrl, null, null, null, null, null, null, null, null, title, null, false, null, false, null);
                return qna;
            } catch(HttpClientErrorException e) {
                if (e.getRawStatusCode() == 403) {
                    log.error("Not authorized to access '" + contentUrl + "'!");
                } else {
                    log.error(e.getMessage(), e);
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * Transform a document into a QnA or multiple QnAs
     * @param siteId SharePoint site Id, e.g. "43c98e69-7d22-4dc1-af38-4498240516e0"
     * @param fileId Document Id, e.g. "01X3SH2XSQWSMH4XZE7NDJDWPGASVM2CTJ"
     * @param fileName File name, e.g. "chat-climate-SSRN-id4414628.pdf"
     * @param mimeType Document mime type, e.g. "application/pdf"
     * @param processId Background process Id
     * @return list of QnAs
     */
    private List<Answer> getDocument(String siteId, String fileId, String apiToken, Context domain, String webUrl, String fileName, String mimeType, String processId) {
        List<Answer> qnas = new ArrayList<Answer>();
        try {
            // INFO: https://learn.microsoft.com/en-us/graph/api/driveitem-get-content?view=graph-rest-1.0&tabs=java
            // INFO: GET /sites/{siteId}/drive/items/{item-id}/content
            String contentUrl = MS_GRAPH_BASE_URL + "/beta/sites/" + siteId + "/drive/items/" + fileId + "/content";
            domainService.deletePreviouslyImportedChunks(webUrl, domain);
            File dumpFile = utilsService.dumpContent(domain, new URI(contentUrl), apiToken);
            ContentType contentType = null;
            try {
                contentType = ContentType.fromString(mimeType);
            } catch (Exception e) {
                log.warn("Content type '" + mimeType + "' not supported yet by Katie");
            }
            domainService.saveMetaInformation(contentUrl, webUrl, new Date(), contentType, domain);

            if (mimeType.equals("application/pdf")) {
                String msg = "Extract text from PDF document '" + contentUrl + "' ...";
                log.info(msg);
                backgroundProcessService.updateProcessStatus(processId, msg);

                PDDocument pdDoc = PDDocument.load(dumpFile);
                String body = new PDFTextStripper().getText(pdDoc);
                pdDoc.close();

                // TODO: Make text splitter configurable
                //List<String> chunks = segmentationService.splitBySentences(body, "en", 700, true);
                List<String> chunks = segmentationService.getSegments(body, '\n', 2000, 100);
                for (String chunk : chunks) {
                    qnas.add(new Answer(null, chunk, ContentType.TEXT_PLAIN, webUrl, null, null, null, null, null, null, null, null, fileName, null, false, null, false, null));
                }
                msg = "Number of chunks extracted from PDF document: " + chunks.size();
                log.info(msg);
                backgroundProcessService.updateProcessStatus(processId, msg);
            } else {
                String msg = "Text extraction from '" + contentUrl + "' of mime-type '" + mimeType + "' not implemented yet!";
                log.warn(msg);
                backgroundProcessService.updateProcessStatus(processId, msg, BackgroundProcessStatusType.WARN);
            }
            return qnas;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
            return null;
        }
    }

    /**
     * Transform documents of a folder into QnAs
     * @param siteId Site Id, e.g. "ee203dc1-cfd8-4fce-8179-c11dd4db249c"
     * @param folderId Folder Id, e.g. "73e548e7-57e8-4546-8a15-0560852ed55a"
     * @param webBaseUrl SharePoint web base URL, e.g. "https://wyona.sharepoint.com"
     * @return list / array of QnAs
     */
    private Answer[] getDocuments(String siteId, String folderId, String folderName, String apiToken, String webBaseUrl, Context domain, String processId, int maxItems, Counter counterItems) {
        List<Answer> qnas = new ArrayList<Answer>();

        // INFO: https://learn.microsoft.com/en-us/graph/api/driveitem-list-children?view=graph-rest-1.0&tabs=http
        log.warn("Get child items from folder '" + folderName + "' (" + folderId + ") ...");
        String requestUrl = MS_GRAPH_BASE_URL + "/beta/sites/" + siteId + "/drive/items/" + folderId + "/children";

        HttpHeaders headers = getHttpHeaders(apiToken);
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Accept", "application/json");
        HttpEntity<String> request = new HttpEntity<String>(headers);

        RestTemplate restTemplate = restProxyTemplate.getRestTemplate();
        try {
            log.info("Get results from " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);

            JsonNode valueNode = bodyNode.get("value");
            if (valueNode.isArray() && valueNode.size() > 0) {
                backgroundProcessService.updateProcessStatus(processId, "Total number of SharePoint items in folder '" + folderName + "': " + valueNode.size());
                backgroundProcessService.updateProcessStatus(processId, "Retrieve and dump SharePoint items (folders, documents, ...) ...");
                for (int i = 0; i < valueNode.size(); i++) {
                    if (counterItems.getValue() >= maxItems) {
                        backgroundProcessService.updateProcessStatus(processId, "Max number of items (" + maxItems + ") reached, therefore abort dumping.", BackgroundProcessStatusType.WARN);
                        break;
                    }

                    JsonNode itemNode = valueNode.get(i);
                    if (itemNode.has("folder")) {
                        String subfolderName = itemNode.get("name").asText();
                        String subfolderId = itemNode.get("id").asText();
                        Answer[] qnasContainedByFolders = getDocuments(siteId, subfolderId, subfolderName, apiToken, webBaseUrl, domain, processId, maxItems, counterItems);
                        for (Answer qna : qnasContainedByFolders) {
                            qnas.add(qna);
                        }
                    } else if (itemNode.has("file")) {
                        counterItems.increase();
                        String fileName = itemNode.get("name").asText();
                        String fileId = itemNode.get("id").asText();
                        String webUrl = itemNode.get("webUrl").asText();
                        String mimeType = itemNode.get("file").get("mimeType").asText();
                        List<Answer> _qnas = getDocument(siteId, fileId, apiToken, domain, webUrl, fileName, mimeType, processId);
                        if (_qnas != null) {
                            for (Answer qna : _qnas) {
                                qnas.add(qna);
                                // TODO: Check MAX_QNAS_PER_ITEM
                            }
                        } else {
                            String msg = "No QnA could be generated for Sharepoint site '" + siteId + "' and document '" + fileId + "'!";
                            log.error(msg);
                            backgroundProcessService.updateProcessStatus(processId, msg, BackgroundProcessStatusType.ERROR);
                        }
                    } else {
                        log.warn("No such SharePoint item type implemented!");
                    }
                    backgroundProcessService.updateProcessStatus(processId, "Item Counter: " + counterItems);
                }
            } else {
                backgroundProcessService.updateProcessStatus(processId, "No SharePoint items available in folder '" + folderName + "'!", BackgroundProcessStatusType.WARN);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return qnas.toArray(new Answer[0]);
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders(String apiToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);

        return headers;
    }


    /*
     https://learn.microsoft.com/en-us/graph/auth/auth-concepts
     https://stackoverflow.com/questions/63321532/sharepoint-rest-api-how-to-get-access-token
     https://learn.microsoft.com/en-us/answers/questions/607425/how-do-i-get-access-token-for-sharepoint-online-re
     https://global-sharepoint.com/sharepoint/in-4-steps-access-sharepoint-online-data-using-postman-tool/
     https://wyona.sharepoint.com/_layouts/15/appregnew.aspx
     https://wyona.sharepoint.com/_layouts/15/appinv.aspx
     Tenant Id: https://aad.portal.azure.com/#view/Microsoft_AAD_IAM/ActiveDirectoryMenuBlade/~/Properties
     */

    /**
     * Get Microsoft Graph API token
     * @return API token, e.g. "EwCQA8l6BAAUAOyDv0l6P ....PXWWxyWIlxx9c/7KAC"
     */
    private String getAPIToken(KnowledgeSourceMeta ksMeta) {
        String token = ksMeta.getMicrosoftGraphApiToken();
        if (token != null) {
            log.info("TODO: Check whether token is still valid!");
            log.info("Use Microsoft Graph API token '" + Utils.obfuscateSecret(token) + "' configured for knowledge source '" + ksMeta.getId() + "' of domain '" + ksMeta.getDomainId() + "' ...");
        } else {
            log.warn("No Microsoft Graph API token configured for knowledge source '" + ksMeta.getId() + "' of domain '" +ksMeta.getDomainId() + "', try to get token using OAuth ...");

            String tenant = ksMeta.getMsTenant();
            // INFO: Azure Portal -> Azure Active Directory -> App registrations
            String clientId = ksMeta.getMsClientId();
            String clientSecret = ksMeta.getMsClientSecret();

            String oauthUrl = "https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/token";

            String grantType = "client_credentials";

            // INFO: Azure Portal -> Azure Active Directory -> App registrations -> App -> API permssions: SharePointTenantSettings.Read.All (Application)
            // INFO: Make sure to click on "Grant admin consent for ..."
            String scope = MS_GRAPH_BASE_URL + "/.default";

            token = microsoftAuthorizationService.getAccessToken(oauthUrl, grantType, clientId, clientSecret, scope);
            if (token != null) {
                log.info("TODO: Cache token!");
            }
        }

        // INFO: https://jwt.ms/ or https://jwt.io/
        //log.info("DEBUG: " + token);
        return token;
    }
}

/**
 *
 */
class Counter {
    private int value = 0;

    /**
     *
     */
    public void increase() {
        value++;
    }

    /**
     *
     */
    public int getValue() {
        return value;
    }

    /**
     *
     */
    @Override
    public String toString() {
        return "" + value;
    }
}
