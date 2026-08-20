package com.wyona.katie.integrations.msteams.services;

import com.wyona.katie.config.RestProxyTemplate;
import com.wyona.katie.services.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Microsoft specific authorization service
 * TODO: Generalize this service, because it can also be used for other OAuth providers, like for example Google, Meta/Facebook, etc.
 */
@Slf4j
@Component
public class MicrosoftAuthorizationService {

    public static final String ACCESS_TOKEN = "access_token";
    public static final String ID_TOKEN = "id_token";

    @Autowired
    private RestProxyTemplate restProxyTemplate;

    /**
     * Get access token and ID token
     * See https://learn.microsoft.com/en-us/graph/auth-v2-service?tabs=http#token-request
     *
     * @param oauthUrl OAuth URL, e.g. "https://login.microsoftonline.com/botframework.com/oauth2/v2.0/token" or "https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token"
     * @param grantType grant_type must be "client_credentials" or "authorization_code"
     * @param clientId App / client Id, e.g. "aaa8c4a1-d204-468f-ac6e-540b26b3a122"
     * @param clientSecret App / client secret, whereas see https://app.katie.qa/ms-teams.html
     * @param code TODO
     * @param redirectUri TODO
     * @param scope Scope, e.g. "https://api.botframework.com/.default" or "https://graph.microsoft.com/.default"
     * @return access token and ID token
     */
    public Map<String, String> getAccessAndIDToken(String oauthUrl, String grantType, String clientId, String clientSecret, String code, String redirectUri, String scope) {
        /*
          Test with Postman:

          POST https://login.microsoftonline.com/botframework.com/oauth2/v2.0/token
            Body x-www-form-urlencoded
              grant_type = client_credentials
              client_id = ...
              client_secret = ...
              scope = https://api.botframework.com/.default
        */

        log.info("Get API token from '" + oauthUrl + "' for grant type '" + grantType + "' and for client Id '" + clientId + "' and client secret '" + Utils.obfuscateSecret(clientSecret) + "' and scope '" + scope + "' ...");

        RestTemplate restTemplate = restProxyTemplate.getRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        String body = "grant_type=" + grantType + "&scope=" + scope;
        if (clientId != null) {
            body = body + "&client_id=" + clientId;
        } else {
            log.info("No client id provided.");
        }
        if (clientSecret != null) {
            body = body + "&client_secret=" + clientSecret;
        } else {
            log.info("No client secret provided");
        }
        if (code !=null) {
            body = body + "&code=" + code;
        } else {
            log.info("No code provided.");
        }
        if (redirectUri != null) {
            body = body + "&redirect_uri=" + redirectUri;
        } else {
            log.info("No redirect URI provided.");
        }
        log.info("Request body: " + body);
        HttpEntity<String> request = new HttpEntity<String>(body, headers);

        try {
            log.info("Try to get access token: " + oauthUrl);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(oauthUrl, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            //log.info("JSON: " + bodyNode);

            Map<String, String> tokens = new HashMap<>();

            if (bodyNode.has(ACCESS_TOKEN)) {
                String accessToken = bodyNode.get(ACCESS_TOKEN).asText();
                tokens.put(ACCESS_TOKEN, accessToken);
                log.info("Access token received :-)");
            }

            if (bodyNode.has(ID_TOKEN)) {
                String idToken = bodyNode.get(ID_TOKEN).asText();
                tokens.put(ID_TOKEN, idToken);
                log.info("ID token received :-)");
            }

            return tokens;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get user email from OAuth provider
     * @param oauthUrl OAuth provider user info URL, e.g., https://openidconnect.googleapis.com/v1/userinfo or https://graph.microsoft.com/oidc/userinfo
     * @param accessToken Access token
     * @return email address of user
     */
    public String getUserEMail(String oauthUrl, String accessToken) {
        // curl -H "Authorization: Bearer $ACCESS_TOKEN" https://openidconnect.googleapis.com/v1/userinfo

        RestTemplate restTemplate = restProxyTemplate.getRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.setBearerAuth(accessToken);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            log.info("Try to get user email: " + oauthUrl);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(oauthUrl, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            //log.debug("JSON: " + bodyNode);
            String email = bodyNode.get("email").asText();
            return email;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * IMPORTANT: One can also get the SAM from the ID token directly
     * Get Security Account Manager (SAM) Account Name
     * @param accessToken Access token
     * @return SAM account name, e.g., "mwechn"
     */
    public String getSAMAccountName(String accessToken) {
        // Microsoft Graph: https://graph.microsoft.com/v1.0/me?$select=onPremisesSamAccountName,userPrincipalName,displayName

        RestTemplate restTemplate = restProxyTemplate.getRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON)) ;
        headers.setBearerAuth(accessToken);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            String url = "https://graph.microsoft.com/v1.0/me?$select=onPremisesSamAccountName,userPrincipalName,displayName";
            log.info("Try to get SAM account name: " + url);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);
            JsonNode accountNameNode = bodyNode.get("onPremisesSamAccountName");
            String accountName = accountNameNode != null && !accountNameNode.isNull() ? accountNameNode.asText() : null;
            return accountName;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get groups of user
     * @param accessToken Access token
     * @return list of groups
     */
    public List<String> getGroups(String accessToken) {
        RestTemplate restTemplate = restProxyTemplate.getRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON)) ;
        headers.setBearerAuth(accessToken);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            String url = "https://graph.microsoft.com/v1.0/me/memberOf/microsoft.graph.group?$select=id,displayName,securityEnabled";
            log.info("Try to get groups: " + url);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);
            List<String> groups = new ArrayList<String>();

            if (bodyNode != null && bodyNode.has("value")) {
                JsonNode groupsArray = bodyNode.get("value");

                // Loop through each group entry found inside the array
                for (JsonNode groupNode : groupsArray) {
                    String groupId = groupNode.path("id").asText();
                    String groupName = groupNode.path("displayName").asText();
                    //boolean isSecurityGroup = groupNode.path("securityEnabled").asBoolean();
                    groups.add(groupId);
                }
            }

            return groups;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
