package com.wyona.katie.connectors;

import com.wyona.katie.config.RestProxyTemplate;
import com.wyona.katie.integrations.msteams.services.MicrosoftAuthorizationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.wyona.katie.models.*;
import com.wyona.katie.services.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 */
@Slf4j
@Component
public class OneNoteConnector implements Connector {

    @Autowired
    SegmentationService segmentationService;

    @Autowired
    MicrosoftAuthorizationService microsoftAuthorizationService;

    @Autowired
    RestProxyTemplate restProxyTemplate;

    @Autowired
    private ContextService domainService;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private BackgroundProcessService backgroundProcessService;

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("Do not return answers from OneNote connector, because Katie is indexing the OneNote content by itself");
        List<Hit> hits = new ArrayList<Hit>();
        return hits.toArray(new Hit[0]);
    }

    /**
     * @see Connector#update(Context, KnowledgeSourceMeta, WebhookPayload, String)
     */
    public List<Answer> update(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayload payload, String processId) {
        // https://learn.microsoft.com/en-us/graph/throttling
        // https://learn.microsoft.com/en-us/graph/connecting-external-content-api-limits
        // https://learn.microsoft.com/en-us/graph/throttling-limits
        // TODO: Do not use hard coded throttle time, but check for header "Retry-After" and throttle dynamically
        int throttleTimeInMillis = 1000;

        List<Answer> qnas = new ArrayList<Answer>();

        WebhookPayloadOneNote payloadOneNote = (WebhookPayloadOneNote) payload;

        // INFO: https://learn.microsoft.com/en-us/graph/api/resources/onenote-api-overview?view=graph-rest-1.0
        // https://learn.microsoft.com/en-us/graph/api/resources/onenote?view=graph-rest-1.0
        //String location = "me";
        //String location = "users/michael.wechner@wyona.com";
        //String location = "users/MichaelWechner@Wyona.onmicrosoft.com";

        // String location = "sites/{id}"

        // String location = "groups/{id}"
        // Login to Microsoft Graph Explorer with for example ugz-su-katie@zuerich.ch
        // https://graph.microsoft.com/v1.0/me/memberOf?$search="displayName:Energie"
        // groups/c5a3125f-f85a-472a-8561-db2cf74396ea
        String location = ksMeta.getOneNoteLocation();
        //String location = ksMeta.getMsGraphUrlLocation(); // TODO

        String token = getAPIToken(ksMeta);
        if (token == null) {
            log.error("API token could not be retrieved, therefore OneNote pages cannot be updated!");
            return qnas;
        }

        // INFO: Get list of OneNote pages
        // INFO: https://learn.microsoft.com/en-us/graph/api/resources/onenote-api-overview?view=graph-rest-1.0
        // INFO: https://learn.microsoft.com/en-us/graph/api/resources/onenote?view=graph-rest-1.0
        String version = "v1.0";
        //String version = "beta";
        // WARN: Do NOT use $top query parameter to increase number of returned pages, because there will be no "@odata.nextLink" within the response to get additional pages
        String urlPages = "https://graph.microsoft.com/" + version + "/" + location + "/onenote/pages";
        //String urlPages = "https://graph.microsoft.com/" + version + "/" + location + "/onenote/pages?$count=true";
        List<OneNotePage> pages = getListOfPages(urlPages, token, processId);
        log.info("Number of OneNote pages: " + pages.size());
        backgroundProcessService.updateProcessStatus(processId, "Total number of OneNote pages: " + pages.size());

        // INFO: Dump OneNote pages
        if (true) { // TODO
            backgroundProcessService.updateProcessStatus(processId,"Retrieve and dump OneNote pages ...");
            if (throttleTimeInMillis > 0) {
                backgroundProcessService.updateProcessStatus(processId, "Throttle set to " + throttleTimeInMillis + " milliseconds.");
            }
            int maxPages = 200; // TODO: Make configurable
            int counter = 0;
            int logProgressCounter = 20;
            for (OneNotePage page : pages) {
                if (counter >= maxPages) {
                    backgroundProcessService.updateProcessStatus(processId, "Max number of pages (" + maxPages + ") reached, therefore abort dumping.", BackgroundProcessStatusType.WARN);
                    break;
                }
                String title = page.getTitle();
                log.info("Dump OneNote page '" + title + "' (" + page.getContentURL() + ") ...");
                try {
                    String urlPageContent = page.getContentURL();
                    String webUrl = page.getWebURL();
                    domainService.deletePreviouslyImportedChunks(webUrl, domain);
                    File dumpFile = utilsService.dumpContent(domain, new URI(urlPageContent), token);
                    ContentType contentType = null; // TODO: Get content type
                    domainService.saveMetaInformation(urlPageContent, webUrl, new Date(), contentType, domain);

                    String body = extractText(dumpFile);
                    String[] chunks = generateSegments(body);
                    for (String chunk : chunks) {
                        qnas.add(new Answer(null, chunk, ContentType.TEXT_PLAIN, page.getWebURL(), null, null, null, null, null, null, null, null, page.getParentTitle() + " " + title, null, false, null, false, null));
                    }
                    log.info("Number of chunks for this page: " + chunks.length);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
                }

                counter++;
                if (counter == logProgressCounter) {
                    backgroundProcessService.updateProcessStatus(processId, logProgressCounter + " pages in total dumped.");
                    logProgressCounter = logProgressCounter + 20;
                }

                if (throttleTimeInMillis > 0) {
                    try {
                        log.info("Sleep for " + throttleTimeInMillis + " milliseconds ...");
                        Thread.sleep(throttleTimeInMillis);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }

            backgroundProcessService.updateProcessStatus(processId, "Dumping completed.");
        } else {
            log.info("Dumping of pages disabled.");
            backgroundProcessService.updateProcessStatus(processId, "Dumping of pages disabled.", BackgroundProcessStatusType.WARN);
        }

        return qnas;
    }

    /**
     *
     */
    private String extractText(File dumpFile) throws Exception {
        String content = Utils.convertInputStreamToString(new FileInputStream(dumpFile));
        String text = Utils.stripHTML(content, true, true);
        log.info("Extracted text: " + text);
        // TODO: Save extracted Text
        return text.trim();
    }

    /**
     *
     */
    private String[] generateSegments(String text) throws Exception {
        // INFO: Do not chunk
        List<String> chunks = new ArrayList<String>();
        chunks.add(text);

        //List<String> chunks = segmentationService.getSegments(text);
        //List<String> chunks = segmentationService.getSegmentsUsingAI21(text);

        return chunks.toArray(new String[0]);
    }

    /**
     * Get list of all OneNote pages
     * @param url URL to request pages, e.g. "https://graph.microsoft.com/v1.0/groups/c5a3125f-f85a-472a-8561-db2cf74396ea/onenote/pages" or "https://graph.microsoft.com/v1.0/groups/c5a3125f-f85a-472a-8561-db2cf74396ea/onenote/pages?$skip=20"
     * @param apiToken Microsoft Graph API Token
     */
    private List<OneNotePage> getListOfPages(String url, String apiToken, String processId) {
        List<OneNotePage> pages = new ArrayList<OneNotePage>();

        RestTemplate restTemplate = restProxyTemplate.getRestTemplate();

        HttpHeaders headers = getHttpHeaders(apiToken, "application/json", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            log.info("Get pages from OneNote: " + url);

            String tunnelingDisabledSchemes = System.getProperty("jdk.http.auth.tunneling.disabledSchemes");
            log.info("jdk.http.auth.tunneling.disabledSchemes: " + tunnelingDisabledSchemes);
            String proxyingDisabledSchemes = System.getProperty("jdk.http.auth.proxying.disabledSchemes");
            log.info("jdk.http.auth.proxying.disabledSchemes: " + proxyingDisabledSchemes);

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);

            JsonNode pagesNode = bodyNode.get("value");
            if (pagesNode.isArray()) {
                for (int i = 0; i < pagesNode.size(); i++) {
                    JsonNode pageNode = pagesNode.get(i);
                    String title = pageNode.get("title").asText();
                    String contentUrl = pageNode.get("contentUrl").asText();
                    String webUrl = pageNode.get("links").get("oneNoteWebUrl").get("href").asText();
                    String parentTitle = pageNode.get("parentSection").get("displayName").asText();
                    pages.add(new OneNotePage(title, contentUrl, webUrl, parentTitle));
                }
                if (pages.size() == 0) {
                    log.warn("No pages for '" + url + "'!");
                }
            } else {
                log.warn("No node named 'value' containing pages!");
            }

            // INFO: https://learn.microsoft.com/en-us/graph/api/onenote-list-pages?view=graph-rest-1.0&tabs=http#optional-query-parameters
            // INFO: https://learn.microsoft.com/en-us/graph/query-parameters?tabs=http#skip-parameter
            if (bodyNode.has("@odata.nextLink")) {
                String urlNextPages = bodyNode.get("@odata.nextLink").asText();
                log.info("Get more pages: " + urlNextPages);
                backgroundProcessService.updateProcessStatus(processId, "Get more pages: " + urlNextPages);
                List<OneNotePage> morePages = getListOfPages(urlNextPages, apiToken, processId);
                for (OneNotePage page : morePages) {
                    pages.add(page);
                }
            } else {
                log.info("No more pages available.");
            }
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 401 || e.getRawStatusCode() == 403) {
                String msg = "Not authorized to access '" + url + "'!";
                log.error(msg);
                backgroundProcessService.updateProcessStatus(processId, msg, BackgroundProcessStatusType.ERROR);
            }
            log.error(e.getMessage(), e);
            backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
        }

        return pages;
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders(String apiToken, String accept, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", accept);
        headers.set("Content-Type", contentType);
        headers.setBearerAuth(apiToken);

        return headers;
    }

    /**
     * Get Microsoft Graph API token
     * @return API token, e.g. "EwCQA8l6BAAUAOyDv0l6P ....PXWWxyWIlxx9c/7KAC"
     */
    private String getAPIToken(KnowledgeSourceMeta ksMeta) {
        String token = ksMeta.getMicrosoftGraphApiToken();
        if (token != null) {
            log.info("Use configured API token ...");
        } else {
            log.warn("No API token available, try to get token using OAuth ...");

            String tenant = ksMeta.getMsTenant();
            // INFO: Azure Portal -> Azure Active Directory -> App registrations
            String clientId = ksMeta.getMsClientId();
            String clientSecret = ksMeta.getMsClientSecret();

            /*
            // Wyona, MichaelWechner@Wyona.onmicrosoft.com
            String tenant = "c5dce9b8-8095-444d-9730-5ccb69b43413";
            String clientId = "046d7c6c-c3c0-40f0-93a1-abcd73ce5cbe";
            String clientSecret = "SECRET";
             */

            /*
            // Default, michael.wechner@wyona.com
            String tenant = "1435b95f-4910-4050-b49d-888120d126ee";
            String clientId = "0f9c0bb6-a0fc-4380-83d5-c33d12551dbe";
            String clientSecret = "SECRET";
             */

            // TODO: Consider using property "ms.oauth.url" resp. "ms.single-tenant.oauth.url"
            String oauthUrl = "https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/token";

            String grantType = "client_credentials";

            // INFO: Azure Portal -> Azure Active Directory -> App registrations -> App -> API permssions: Notes.Read.All (Application)
            // INFO: Make sure to click on "Grant admin consent for ..."
            String scope = "https://graph.microsoft.com/.default";

            token = microsoftAuthorizationService.getAccessToken(oauthUrl, grantType, clientId, clientSecret, scope);
        }

        // INFO: https://jwt.ms/ or https://jwt.io/
        //log.info("DEBUG: " + token);
        return token;
    }
}
