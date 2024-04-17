package com.wyona.katie.controllers.v1;

import com.wyona.katie.config.RestProxyTemplate;
import com.wyona.katie.integrations.msteams.services.MicrosoftDomainService;
import com.wyona.katie.models.KnowledgeSourceMeta;
import com.wyona.katie.models.msteams.MSTeamsDomainMapping;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.wyona.katie.config.RestProxyTemplate;
import com.wyona.katie.integrations.msteams.MicrosoftMessageSender;
import com.wyona.katie.integrations.msteams.services.MicrosoftDomainService;
import com.wyona.katie.models.Error;
import com.wyona.katie.models.KnowledgeSourceMeta;
import com.wyona.katie.models.msteams.MSTeamsDomainMapping;
import com.wyona.katie.models.msteams.MicrosoftBotMessage;
import com.wyona.katie.services.ContextService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.wyona.katie.models.Error;
import com.wyona.katie.models.msteams.MicrosoftBotMessage;
import com.wyona.katie.integrations.msteams.MicrosoftMessageSender;

import io.swagger.annotations.ApiOperation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.nio.file.AccessDeniedException;

//import java.security.KeyFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.EncodedKeySpec;
//import java.security.interfaces.RSAPublicKey;
import java.security.PublicKey;

/**
 * Microsoft bot controller
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/microsoft") 
public class MicrosoftBotController {

    @Autowired
    private MicrosoftMessageSender messageSender;

    @Autowired
    private MicrosoftDomainService msDomainService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ContextService domainService;

    @Autowired
    private RestProxyTemplate restProxyTemplate;

    @Value("${ms.client.id}")
    private String clientId;

    @Value("${ms.public_keys.url}")
    private String publicKeysUrl;

    @Value("${ms.redirect.uri}")
    private String redirectUri;

    /**
     *
     */
    @Autowired
    public MicrosoftBotController() {
    }

    /**
     * OAuth2 callback for Microsoft
     */
    @RequestMapping(value = "/oauth2-callback", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="MICROSOFT: OAuth2 callback for Microsoft")
    public ResponseEntity<?> oauth2Callback(
            @ApiParam(name = "session_state", value = "A unique value that identifies the current user session. This value is a GUID, but should be treated as an opaque value that is passed without examination.",required = true)
            @RequestParam(value = "session_state", required = true) String sessionState,
            @ApiParam(name = "state", value = "Allows you to prevent an attack by confirming that the state value coming from the response matches the one you sent.",required = true)
            @RequestParam(value = "state", required = true) String state,
            @ApiParam(name = "code", value = "Temporary authorization code in order to exchange for an access token",required = true)
            @RequestParam(value = "code", required = true) String code,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            // INFO: https://learn.microsoft.com/en-us/graph/auth-v2-user?tabs=http#authorization-response
            log.info("Session state: " + sessionState);
            log.info("State: " + state);
            log.info("Code: " + code);

            // INFO: See App Registrations: https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade
            // https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps/ApplicationMenuBlade/~/Overview/appId/046d7c6c-c3c0-40f0-93a1-abcd73ce5cbe/isMSAApp~/false

            String[] domainIdKnowledgesourceId = state.split(",");
            KnowledgeSourceMeta ksMeta = domainService.getKnowledgeSource(domainIdKnowledgesourceId[0], domainIdKnowledgesourceId[1]);
            String tenantId = ksMeta.getMsTenant();
            String clientId = ksMeta.getMsClientId();
            String clientSecret = ksMeta.getMsClientSecret();

            String scope = ksMeta.getMsScope();

            String accessToken = getAccessToken(code, tenantId, clientId, clientSecret, redirectUri, scope);
            log.info("Access token: " + accessToken);

            domainService.setMicrosoftGraphAPIToken(domainIdKnowledgesourceId[0], domainIdKnowledgesourceId[1], accessToken);

            return new ResponseEntity<>("{\"access-token\":\"" + accessToken + "\"}", HttpStatus.OK);

            /*
            HttpHeaders headers = new HttpHeaders();
            String redirectLandingPage = "/"; // TODO: See application property katie.redirect.landing.page
            headers.add("Location", redirectLandingPage);

            return new ResponseEntity<>(headers, HttpStatus.TEMPORARY_REDIRECT);
             */
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all Katie domains which are connected with an Azure Bot configuration
     */
    @RequestMapping(value = "/domains", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get all domains which are connected with Azure Bot / MS Teams")
    public ResponseEntity<?> getDomains() {
        try {
            MSTeamsDomainMapping[] mappings = msDomainService.getMappings();
            return new ResponseEntity<>(mappings, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Approve connection of a MS team with a Katie domain
     */
    @RequestMapping(value = "/connect-team-domain", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Approve connection of a MS team with a Katie domain")
    public ResponseEntity<?> connectTeamWithDomain(
            @ApiParam(name = "token", value = "JWT token containing info re team Id (private claims: team_id) to approve previously requested connection with domain", required = true)
            @RequestParam(value = "token", required = true) String token,
            @ApiParam(name = "domain-id", value = "Existing domain Id", required = false)
            @RequestParam(value = "domain-id", required = false) String domainIdTMP,
            HttpServletRequest request) {
        try {
            MSTeamsDomainMapping mapping = msDomainService.approveMapping(token, domainIdTMP);

            if (mapping != null) {
                // TODO: Send message to MS Teams, that team was connected with domain
                //messageSender.sendAnswer();
                //messageSender.sendResponse();
                return new ResponseEntity<>("{\"status\":\"" + mapping.getStatus() + "\"}", HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Error("Approval failed", "APPROVAL_FAILED"), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Disconnect a team/channel from a domain
     */
    @RequestMapping(value = "/disconnect-team-channel-domain", method = RequestMethod.DELETE, produces = "application/json")
    @ApiOperation(value="Disconnect a team/channel from a domain")
    public ResponseEntity<?> disconnectTeamChannelFromDomain(
            @ApiParam(name = "domain-id", value = "Katie Domain Id",required = true)
            @RequestParam(value = "domain-id", required = true) String domainId,
            @ApiParam(name = "team-id", value = "Team Id",required = true)
            @RequestParam(value = "team-id", required = true) String teamId,
            @ApiParam(name = "channel-id", value = "Channel Id",required = false)
            @RequestParam(value = "channel-id", required = false) String channelId,
            HttpServletRequest request) {
        try {
            MSTeamsDomainMapping[] mappings = msDomainService.getMappings(domainId, teamId, channelId);
            if (mappings != null) {
                for (MSTeamsDomainMapping mapping : mappings) {
                    log.info("Remove mapping: " + mapping);
                    msDomainService.removeMapping(domainId, teamId, channelId);
                }
            } else {
                log.info("No mapping for team / channel '" + teamId + " / " + channelId + "'.");
            }
            return new ResponseEntity<>("{}", HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * REST interface to handle Microsoft Bot messages
     * https://docs.microsoft.com/en-us/microsoftteams/platform/webhooks-and-connectors/how-to/add-outgoing-webhook
     */
/*
  POST /api/v1/micrososft/message HTTP/1.1..Host: 127.0.0.1:6060..Authorization: Bearer ew0KIC...qaoZW_DDw-FxbC1Q..User-Agent: BF-DirectLine (Microsoft-BotFramework/3.2 +https://botframework.com/ua)..x-ms-conversation-id: 5fXxxuMPf5gnSy2OnSl67-h..Content-Type: application/json; charset=utf-8..Request-Id: |6954d980-47f8d9d78d8a0d35.14.1...X-Forwarded-For: 13.94.244.42..X-Forwarded-Host: ukatie.com..X-Forwarded-Server: ukatie.com..Connection: Keep-Alive..Content-Length: 504....{"type":"message","id":"5fXxxuMPf5gnSy2OnSl67-h|0000001","timestamp":"2020-08-30T07:09:43.3133668Z","serviceUrl":"https://webchat.botframework.com/","channelId":"webchat","from":{"id":"97b40934-6f23-4705-83dd-a60b8451f784","name":"You"},"conversation":{"id":"5fXxxuMPf5gnSy2OnSl67-h"},"recipient":{"id":"askkatie@BwUXs4nRsbg","name":"Katie"},"textFormat":"plain","locale":"de-CH","text":"Test 2","channelData":{"clientActivityID":"15987713832991mh8krcwc1xj","clientTimestamp":"2020-08-30T07:09:43.299Z"}}
*/
    @RequestMapping(value = "/message", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="MICROSOFT: Handle Microsoft bot messages")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> handleEvents(@RequestBody MicrosoftBotMessage message,
        HttpServletRequest request) {

        log.info("Received message: " + message);

        // INFO: https://learn.microsoft.com/en-us/azure/bot-service/rest-api/bot-framework-rest-connector-authentication?view=azure-bot-service-4.0&tabs=multitenant#connector-to-bot
        // INFO: Verify that token sent by Microsoft Bot Connector Service is valid
        String jwtToken = jwtService.getJWT(request);
        if (jwtToken == null) {
            log.error("No JWT provided!");
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } else {
            String kid = jwtService.getHeaderValue(jwtToken, "kid");
            // TODO: Get "kid" value from token header
            PublicKey publicKey = getPublicKey(kid);
            if (publicKey != null) {
                if (jwtService.isJWTValid(jwtToken, publicKey)) {
                    log.info("Signature of JWT is valid :-)");
                    // https://docs.microsoft.com/en-us/azure/bot-service/rest-api/bot-framework-rest-connector-authentication?view=azure-bot-service-4.0#step-4-verify-the-jwt-token
                    // TODO Also check that issuer is "https://api.botframework.com"
                    String issuer = jwtService.getPayloadValue(jwtToken, "iss");
                    String audience = jwtService.getPayloadValue(jwtToken, "aud");
                    if (audience.equals(clientId)) {
                        log.info("Token is valid.");
                    } else {
                        log.error("Token does not not contain correct Microsoft App ID: " + audience);
                        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                    }
                } else {
                    log.error("Provided JWT is not valid!");
                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
            } else {
                log.error("No public key for kid value '" + kid + "'!");
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        }
        // INFO: Verification of token completed

        log.warn("TODO: Check whether Message was sent by a bot, if so, then do not answer, because it might create a question answering loop!.");

        // INFO: Response will be sent asynchronously
        messageSender.sendResponse(message);

        log.info("Acknowledge message ...");
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }

    /**
     * Get public key from Microsoft
     * https://docs.microsoft.com/en-us/azure/bot-service/rest-api/bot-framework-rest-connector-authentication?view=azure-bot-service-4.0#connector-to-bot-step-3
     * https://login.botframework.com/v1/.well-known/keys
     * @param kid For example "ZyGh1GbBL8xd1kOxRYchc1VPSQQ"
     * @return public key of Microsoft
     */
    private PublicKey getPublicKey(String kid) {
        // https://redthunder.blog/2017/06/08/jwts-jwks-kids-x5ts-oh-my/comment-page-1/
        try {
            // INFO: Use "kid" value to get "x5c" key from https://login.botframework.com/v1/.well-known/keys
            String publicKeyPem = getX5C(kid);
            
            byte[] keyContentAsBytes =  java.util.Base64.getMimeDecoder().decode(publicKeyPem);
            EncodedKeySpec keySpec = new X509EncodedKeySpec(keyContentAsBytes);


            java.security.cert.CertificateFactory fact = java.security.cert.CertificateFactory.getInstance("X.509");
            X509Certificate cer = (X509Certificate) fact.generateCertificate(new java.io.ByteArrayInputStream(keyContentAsBytes));
            return cer.getPublicKey();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @param kid For example "ZyGh1GbBL8xd1kOxRYchc1VPSQQ"
     * @return x5c value, e.g. "MIIGGDCCBACgAwIBAgIT ....xqbeAVddfCCVAQ=="
     */
    private String getX5C(String kid) {
        RestTemplate restTemplate = restProxyTemplate.getRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        //headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            log.info("Try to get x5c certificate: " + publicKeysUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(publicKeysUrl, org.springframework.http.HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.debug("JSON: " + bodyNode);
            JsonNode keys = bodyNode.get("keys");
            if (keys.isArray()) {
                for (int i = 0; i < keys.size(); i++) {
                    JsonNode keyNode = keys.get(i);
                    if (keyNode.get("kid").asText().equals(kid)) {
                        // TODO: Also check whether endorsements contains "msteams"
                        JsonNode certs = keyNode.get("x5c");
                        String x5c = certs.get(0).asText();
                        log.warn("TODO: Cache x5c: " + x5c);
                        return x5c;
                    } else {
                        log.debug("kid does not match");
                    }
                }
            }
            log.error("No x5c for kid value '" + kid + "'!");
            return null;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get bearer token, which allows to query SharePoint
     */
    private String getAccessToken(String code, String tenantId, String clientId, String clientSecret, String redirectUri, String scope) {
        try {
            StringBuilder body = new StringBuilder();
            body.append("code=" + code);
            body.append("&");
            body.append("client_id=" + clientId);
            body.append("&");
            body.append("client_secret=" + clientSecret);
            body.append("&");
            body.append("redirect_uri=" + redirectUri);
            body.append("&");
            body.append("grant_type=" + "authorization_code");
            body.append("&");
            body.append("scope=" + scope);

            // TODO: Consider using property "ms.oauth.url"
            String requestUrl = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
            log.info("Get Access token from '" + requestUrl + "' ...");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("Accept-Charset", "UTF-8");
            //headers.set("Content-Type", "application/x-www-form-urlencoded");
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
            //HttpEntity<String> request = new HttpEntity<String>(headers);
            HttpEntity<String> request = new HttpEntity<String>(body.toString(), headers);

            RestTemplate restTemplate = restProxyTemplate.getRestTemplate();

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(requestUrl, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("Response JSON: " + bodyNode);

            String _scope = bodyNode.get("scope").asText();
            log.info("Scope: " + _scope);
            String accessToken = bodyNode.get("access_token").asText();

            return accessToken;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
