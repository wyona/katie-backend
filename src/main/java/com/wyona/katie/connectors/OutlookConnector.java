package com.wyona.katie.connectors;

import com.wyona.katie.config.RestProxyTemplate;
import com.wyona.katie.integrations.msteams.services.MicrosoftAuthorizationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.wyona.katie.models.*;
import com.wyona.katie.services.BackgroundProcessService;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.SegmentationService;
import com.wyona.katie.services.UtilsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Slf4j
@Component
public class OutlookConnector implements Connector {

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

    private final int MAX_ENTRIES = 10;

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("Do not return answers from Outlook connector, because Katie is indexing the Outlook content by itself");
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

        // INFO: https://learn.microsoft.com/en-us/graph/api/resources/onenote-api-overview?view=graph-rest-1.0
        // https://learn.microsoft.com/en-us/graph/api/resources/onenote?view=graph-rest-1.0
        //String location = "me";
        //String location = "users/michael.wechner@wyona.com";
        //String location = "users/MichaelWechner@Wyona.onmicrosoft.com";
        //String location = "users/energieberatung@zuerich.ch";

        // String location = "sites/{id}"

        // String location = "groups/{id}"
        // Login to Microsoft Graph Explorer with for example ugz-su-katie@zuerich.ch
        // https://graph.microsoft.com/v1.0/me/memberOf?$search="displayName:Energie"
        // groups/c5a3125f-f85a-472a-8561-db2cf74396ea
        String location = ksMeta.getMsGraphUrlLocation();

        String token = getAPIToken(ksMeta);
        if (token == null) {
            log.error("API token could not be retrieved, therefore Outlook messages cannot be updated!");
            return qnas;
        }

        // INFO: Get list of OneNote pages
        // INFO: https://learn.microsoft.com/en-us/graph/api/resources/onenote-api-overview?view=graph-rest-1.0
        // INFO: https://learn.microsoft.com/en-us/graph/api/resources/onenote?view=graph-rest-1.0
        String version = "v1.0";
        //String version = "beta";
        //String urlMessages = "https://graph.microsoft.com/" + version + "/" + location + "/messages";
        String urlMessages = "https://graph.microsoft.com/" + version + "/" + location + "/messages?$top=3";
        //String urlMessages = "https://graph.microsoft.com/" + version + "/" + location + "/mailfolders/inbox/messages";
        backgroundProcessService.updateProcessStatus(processId, "Get messages from " + urlMessages);
        Counter counter = new Counter();
        List<OneNotePage> messages = getListOfMessages(urlMessages, token, processId, counter);
        log.info("Number of Outlook messages: " + messages.size());
        backgroundProcessService.updateProcessStatus(processId, "Total number of Outlook messages: " + messages.size());

        return qnas;
    }

    /**
     * Get list of all Outlook messages
     * @param url URL to request messages, e.g. "https://graph.microsoft.com/v1.0/users/energieberatung@zuerich.ch/messages"
     *            https://graph.microsoft.com/v1.0/users/MichaelWechner@Wyona.onmicrosoft.com/messages?$top=10&$skip=10
     * @param apiToken Microsoft Graph API Token
     */
    private List<OneNotePage> getListOfMessages(String url, String apiToken, String processId, Counter counter) {
        List<OneNotePage> pages = new ArrayList<OneNotePage>();

        RestTemplate restTemplate = restProxyTemplate.getRestTemplate();

        HttpHeaders headers = getHttpHeaders(apiToken, "application/json", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            log.info("Get messages from Outlook: " + url);

            String tunnelingDisabledSchemes = System.getProperty("jdk.http.auth.tunneling.disabledSchemes");
            log.info("jdk.http.auth.tunneling.disabledSchemes: " + tunnelingDisabledSchemes);
            String proxyingDisabledSchemes = System.getProperty("jdk.http.auth.proxying.disabledSchemes");
            log.info("jdk.http.auth.proxying.disabledSchemes: " + proxyingDisabledSchemes);

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);

            JsonNode valueNode = bodyNode.get("value");
            if (valueNode.isArray()) {
                for (int i = 0; i < valueNode.size(); i++) {
                    if (counter.getValue() >= MAX_ENTRIES) {
                        break;
                    }
                    JsonNode emailNode = valueNode.get(i);
                    if (emailNode.has("from")) {
                        String fromEmail = emailNode.get("from").get("emailAddress").get("address").asText();
                        log.info("Email from: " + fromEmail);
                        String fromName = emailNode.get("from").get("emailAddress").get("name").asText();
                        String subject = emailNode.get("subject").asText();
                        backgroundProcessService.updateProcessStatus(processId, "From: " + fromEmail + ", Subject: " + subject);
                        pages.add(new OneNotePage(fromName, fromEmail, null, subject));
                    } else {
                        log.info("Ignore");
                        backgroundProcessService.updateProcessStatus(processId, "Ignore entry", BackgroundProcessStatusType.WARN);
                    }
                    counter.increase();
                }
                if (pages.size() == 0) {
                    log.warn("No emails for '" + url + "'!");
                    backgroundProcessService.updateProcessStatus(processId, "No emails", BackgroundProcessStatusType.WARN);
                }
            } else {
                log.warn("No node named 'value' containing emails!");
                backgroundProcessService.updateProcessStatus(processId, "Np node named 'value'", BackgroundProcessStatusType.WARN);
            }

            if (counter.getValue() >= MAX_ENTRIES) {
                backgroundProcessService.updateProcessStatus(processId, "Max number of entries (" + MAX_ENTRIES + ") reached, therefore abort dumping.");
                return pages;
            }

            // INFO: https://learn.microsoft.com/en-us/graph/api/onenote-list-pages?view=graph-rest-1.0&tabs=http#optional-query-parameters
            // INFO: https://learn.microsoft.com/en-us/graph/query-parameters?tabs=http#skip-parameter
            if (bodyNode.has("@odata.nextLink")) {
                String urlNextEmails = bodyNode.get("@odata.nextLink").asText();
                urlNextEmails = urlNextEmails.replace("%24", "$");
                // INFO: https://graph.microsoft.com/v1.0/users/MichaelWechner@Wyona.onmicrosoft.com/messages?$top=10&$skip=10
                log.info("Get more emails: " + urlNextEmails);
                backgroundProcessService.updateProcessStatus(processId, "Get more emails: " + urlNextEmails);
                List<OneNotePage> morePages = getListOfMessages(urlNextEmails, apiToken, processId, counter);
                for (OneNotePage page : morePages) {
                    pages.add(page);
                }
            } else {
                log.info("No more emails available.");
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
