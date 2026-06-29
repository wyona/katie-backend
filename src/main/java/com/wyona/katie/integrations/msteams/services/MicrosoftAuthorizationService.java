package com.wyona.katie.integrations.msteams.services;

import com.wyona.katie.config.RestProxyTemplate;
import com.wyona.katie.services.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Microsoft specific authorization service
 * TODO: Generalize this service, because it can also be used for other OAuth providers, like for example Google, Meta/Facebook, etc.
 */
@Slf4j
@Component
public class MicrosoftAuthorizationService {

    @Autowired
    private RestProxyTemplate restProxyTemplate;

    /**
     * See https://learn.microsoft.com/en-us/graph/auth-v2-service?tabs=http#token-request
     *
     * @param oauthUrl OAuth URL, e.g. "https://login.microsoftonline.com/botframework.com/oauth2/v2.0/token" or "https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token"
     * @param grantType grant_type must be "client_credentials"
     * @param clientId App / client Id, e.g. "aaa8c4a1-d204-468f-ac6e-540b26b3a122"
     * @param clientSecret App / client secret, whereas see https://app.katie.qa/ms-teams.html
     * @param code TODO
     * @param redirectUri TODO
     * @param scope Scope, e.g. "https://api.botframework.com/.default" or "https://graph.microsoft.com/.default"
     */
    public String getAccessToken(String oauthUrl, String grantType, String clientId, String clientSecret, String code, String redirectUri, String scope) {
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

        String body = "grant_type=" + grantType + "&client_id=" + clientId + "&scope=" + scope;
        if (clientSecret != null) {
            body = body + "&client_secret=" + clientSecret;
        }
        if (code !=null) {
            body = body + "&code=" + code;
        }
        if (redirectUri != null) {
            body = body + "&redirect_uri=" + redirectUri;
        }
        log.info("Request body: " + body);
        HttpEntity<String> request = new HttpEntity<String>(body, headers);

        try {
            log.info("Try to get access token: " + oauthUrl);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(oauthUrl, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            //log.debug("JSON: " + bodyNode);
            String accessToken = bodyNode.get("access_token").asText();
            log.info("Token received :-)");
            return accessToken;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get user email from OAuth provider
     * @param oauthUrl OAuth provider user info URL, e.g., https://openidconnect.googleapis.com/v1/userinfo
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
            log.debug("JSON: " + bodyNode);
            String email = bodyNode.get("email").asText();
            return email;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
