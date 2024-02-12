package com.wyona.katie.integrations.slack.services;

import com.wyona.katie.models.slack.AccessCredentials;
import com.wyona.katie.models.Role;
import com.wyona.katie.models.User;
import com.wyona.katie.models.Username;
import com.wyona.katie.models.slack.ConnectStatus;
import com.wyona.katie.models.slack.JWTClaims;
import com.wyona.katie.models.slack.SlackDomainMapping;
import com.wyona.katie.services.IAMService;
import com.wyona.katie.services.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.wyona.katie.models.Context;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.DataRepositoryService;
import org.springframework.web.client.RestTemplate;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;

/**
 * Slack specific domain service
 */
@Slf4j
@Component
public class DomainService {

    @Value("${slack.access.token.endpoint}")
    private String tokenEndpointUrl;

    @Value("${slack.redirect.uri}")
    private String redirectUri;

    @Value("${slack.client.id}")
    private String katieSlackClientId;

    @Value("${slack.katie.username}")
    private String usernameTechnicalUser;

    @Autowired
    private ContextService contextService;

    @Autowired
    private DataRepositoryService dataRepoService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SlackClientService slackClientService;

    @Autowired
    private IAMService iamService;

    /**
     * Approve connection between Slack channel and Katie domain
     * @param token JWT containing Slack team id and channel id
     * @return Slack/Katie mapping when connection was approved and null otherwise
     */
    public SlackDomainMapping approveMapping(String token) throws AccessDeniedException {
        if (!jwtService.isJWTValid(token, null)) {
            throw new AccessDeniedException("Token is not valid!");
        }

        String teamId = jwtService.getJWTClaimValue(token, JWTClaims.TEAM_ID);
        String channelId = jwtService.getJWTClaimValue(token, JWTClaims.CHANNEL_ID);

        SlackDomainMapping mapping = getDomainMappingForSlackTeamChannel(teamId, channelId);
        if (mapping != null && mapping.getApprovalToken().equals(token)) {
            if (mapping.getStatus() == ConnectStatus.APPROVED) {
                log.info("Mapping between Slack team/channel and Katie domain is already approved.");
            } else if (mapping.getStatus() == ConnectStatus.NEEDS_APPROVAL) {
                dataRepoService.updateSlackConnectMappingStatus(teamId, channelId, ConnectStatus.APPROVED);
                try {
                    User technicalUser = iamService.getUserByUsername(new Username(usernameTechnicalUser), false, false);
                    contextService.addMember(technicalUser.getId(), false, false, null, mapping.getDomainId());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                log.info("Connection between Katie domain '" + mapping.getDomainId() + "' and Slack team / channel '" + teamId + " / " + channelId+ "' has been approved.");
            } else {
                log.warn("Connection status: " + mapping.getStatus());
            }

            return mapping;
        } else {
            log.info("No mapping for team / channel '" + teamId + " / " + channelId + "'.");
            return null;
        }
    }

    /**
     * Get Slack domain mapping for a particular team ID / channel Id
     */
    public SlackDomainMapping getDomainMappingForSlackTeamChannel(String teamId, String channelId) {
        return dataRepoService.getDomainMappingForSlackTeamChannel(teamId, channelId);
    }

    /**
     * Get all Slack domain mappings for a particular team
     */
    public SlackDomainMapping[] getDomainMappingsForSlackTeam(String teamId) {
        return dataRepoService.getDomainMappingsForSlackTeam(teamId);
    }

    /**
     * Get most recent messages from a particular Slack team / channel
     */
    public void sync(String teamId, String channelId) throws  AccessDeniedException {
        if (contextService.isAdmin()) {
            String token = dataRepoService.getSlackBearerTokenOfTeam(teamId);

            // TODO: Get messages from Slack channel

            log.info("Send message to Slack channel: " + channelId);
            slackClientService.sendHiFromKatieMessage(token, channelId);

            slackClientService.testAuth(token);

            //slackClientService.send(slackClientService.getResponseJSON(channelId, null, "Hello Katie :-)"), postMessageURL, token);
        } else {
            throw new AccessDeniedException("User is either not signed in or has not role ADMIN!");
        }
    }

    /**
     * Get all Slack channels for a particular Katie domain
     */
    public SlackDomainMapping[] getSlackChannelsForDomain(String domainId) throws  AccessDeniedException {
        if (contextService.isMemberOrAdmin(domainId)) {
            return dataRepoService.getSlackChannelsForDomain(domainId);
        } else {
            throw new AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }
    }

    /**
     * Disconnect a Slack team/channel from a domain
     */
    public void removeMapping(String teamId, String channelId) throws AccessDeniedException {
        if (!contextService.isAdmin()) {
            throw new AccessDeniedException("User is either not signed in or has not role ADMIN!");
        }
        try {
            dataRepoService.removeDomainIdSlackTeamChannelMapping(teamId, channelId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Reroute team/channel to another domain
     */
    public void rerouteMapping(String teamId, String channelId, String domainId) throws AccessDeniedException {
        if (!contextService.isAdmin()) {
            throw new AccessDeniedException("User is either not signed in or has not role ADMIN!");
        }
        try {
            dataRepoService.rerouteDomainIdSlackTeamChannelMapping(teamId, channelId, domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Remove mappings where Katie domains do not exist anymore
     * @param checkAuthorization Check whether user is authorized to remove mappings
     */
    public void removeObsoleteMappings(String teamId, boolean checkAuthorization) throws AccessDeniedException {
        if (checkAuthorization && !contextService.isAdmin()) {
            throw new AccessDeniedException("User is either not signed in or has not role ADMIN!");
        }

        log.info("Do some 'house cleaning', because some domains might have been deleted by Katie administrator");
        // TODO: Consider to check all mappings and not just the one linked with a particular team id
        SlackDomainMapping[] mappings = dataRepoService.getDomainMappingsForSlackTeam(teamId);
        for (SlackDomainMapping mapping: mappings) {
            if (!contextService.existsContext(mapping.getDomainId())) {
                try {
                    log.info("Remove mapping associated with team '" + teamId + "' and domain '" + mapping.getDomainId() + "' ...");
                    dataRepoService.removeDomainIdSlackTeamMapping(mapping.getDomainId());
                } catch(Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.info("Domain '" + mapping.getDomainId() + "' exists, therefore do not remove mapping of team / channel '" + teamId + " / " + mapping.getChannelId() + "'.");
            }
        }
    }

    /**
     * Get all mappings
     */
    public SlackDomainMapping[] getMappings() throws AccessDeniedException {
        if (contextService.isAdmin()) {
            SlackDomainMapping[] mappings = dataRepoService.getDomainMappingsForSlack();
            return mappings;
        } else {
            throw new AccessDeniedException("User is either not signed in or has not role ADMIN!");
        }
    }

    /**
     * Get Katie domain Ids associated with Slack team (workspace)
     */
    public String[] getDomainIds(String teamId) {
        SlackDomainMapping[] mappings = dataRepoService.getDomainMappingsForSlackTeam(teamId);
        if (mappings.length == 0) {
            log.info("No Katie domain linked yet with Slack team Id '" + teamId + "'.");
            return new String[0];
        } else {
            List<String> domainIds = new ArrayList<String>();
            for (SlackDomainMapping mapping: mappings) {
                String domainId = mapping.getDomainId();
                try {
                    if (contextService.existsContext(domainId)) {
                        domainIds.add(domainId);
                    } else {
                        log.info("Do some 'house cleaning', because Domain might have been deleted by Katie administrator");
                        try {
                            dataRepoService.removeDomainIdSlackTeamMapping(domainId);
                        } catch (Exception drsException) {
                            log.error(drsException.getMessage(), drsException);
                        }
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            return domainIds.toArray(new String[0]);
        }
    }

    /**
     * Get Katie domain associated with Slack team (workspace) / channel
     * @param teamId Team Id, e.g. 'T01848J69AP'
     * @param channelId Channel Id, e.g. 'C01BG53KWLA'
     * @return domain or null when no domain linked yet with Slack team Id
     */
    public Context getDomain(String teamId, String channelId) throws Exception {
        String domainId = dataRepoService.getDomainIdForSlackTeamChannel(teamId, channelId);

        if (domainId == null) {
            log.info("No Katie domain linked yet with Slack team Id '" + teamId + "' and channel Id '" + channelId + "'.");
            return null;
        } else {
            SlackDomainMapping mapping = dataRepoService.getDomainMappingForSlackTeamChannel(teamId, channelId);
            if (mapping != null && !mapping.getStatus().equals(ConnectStatus.APPROVED)) {
                String errMsg = "Connection between Katie domain '" + mapping.getDomainId() + "' and Slack team/channel '" + teamId + " / " + channelId + "' is not approved!";
                log.error(errMsg);
                throw new Exception(errMsg);
            }

            try {
                if (contextService.existsContext(domainId)) {
                    return contextService.getContext(domainId);
                } else {
                    log.info("Do some 'house cleaning', because Domain might have been deleted by Katie administrator");
                    try {
                        dataRepoService.removeDomainIdSlackTeamMapping(domainId);
                    } catch(Exception drsException) {
                        log.error(drsException.getMessage(), drsException);
                    }
                    return null;
                }
            } catch(Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }
    }

    /**
     *
     */
    public void updateSlackBotToken(String clientId, String clientSecret, String code, String state) throws Exception {
        AccessCredentials accessCredentials = getBotAccessToken(tokenEndpointUrl, code, clientId, clientSecret);
        if (accessCredentials != null) {
            //log.info("Bot access token: " + accessCredentials.getAccessTokeen());

            removeObsoleteMappings(accessCredentials.getTeamId(), false);

            String[] domainIds = getDomainIds(accessCredentials.getTeamId());

            // INFO: Maybe domain existed before, but got deleted by Katie administrator
            // INFO: Do not create domain here actually, but only when Katie gets invited to a particular channel, but save access token

            if (domainIds.length > 0) {
                log.info(domainIds.length + " Katie domains already exists for Slack team / channel '" + accessCredentials.getTeamId() + " / " + accessCredentials.getChannelId() + "'.");
            } else {
                log.info("No Katie domains exist yet for Slack team / channel '" + accessCredentials.getTeamId() + " / " + accessCredentials.getChannelId() + "'.");
            }

            // INFO: Update bearer token of a particular Slack team
            // Bearer / access token, such that Katie can send messages to Slack (https://api.slack.com/authentication/token-types#bot)
            dataRepoService.updateSlackBearerToken(accessCredentials.getTeamId(), accessCredentials.getAccessToken(), accessCredentials.getBotUserId());
        } else {
            String errorMsg = "No access token received!";
            log.error(errorMsg);
        }
    }

    /**
     * Get bot access token by sending a POST request to a token endpoint (see https://developers.google.com/identity/protocols/OpenIDConnect#exchangecode)
     * @param token_endpoint Token endpoint URL (https://slack.com/api/oauth.v2.access)
     * @param code Temporary authorization code in order to exchange for an access token, e.g. "40786315168.2889647873747.fra4c5e6e997127150223d1228d21439647775ze79a99762affe4a9fb10749e2"
     * @param clientId Slack client Id
     * @param clientSecret Slack client Secret
     * @return bot access token (https://api.slack.com/authentication/token-types#bot)
     */
    private AccessCredentials getBotAccessToken(String token_endpoint, String code, String clientId, String clientSecret) {
        try {
            StringBuilder qs = new StringBuilder("?");
            qs.append("code=" + code);
            qs.append("&");
            qs.append("client_id=" + clientId);
            qs.append("&");
            qs.append("client_secret=" + clientSecret);
            qs.append("&");
            if (clientId != katieSlackClientId) {
                qs.append("redirect_uri=" + redirectUri + "/" + clientId);
            } else {
                qs.append("redirect_uri=" + redirectUri);
            }
            qs.append("&");
            qs.append("grant_type=authorization_code");

            //java.net.URL url = new java.net.URL(token_endpoint);
            String requestUrl = token_endpoint + qs;
            log.info("Get Access and Id Token from '" + requestUrl + "' ...");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json; charset=UTF-8");
            headers.set("Accept-Charset", "UTF-8");
            HttpEntity<String> request = new HttpEntity<String>(headers);
            //HttpEntity<String> request = new HttpEntity<String>(body, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(requestUrl, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            //log.debug("Response JSON: " + bodyNode);
            // {"ok":true,"app_id":"A0184KMLJJE","authed_user":{"id":"U018A7XUWSY","scope":"im:history","access_token":"TOKEN_P","token_type":"user"},"scope":"channels:history,commands,im:history,chat:write,incoming-webhook,team:read","token_type":"bot","access_token":"TOKEN_B","bot_user_id":"U018505QFA6","team":{"id":"T01848J69AP","name":"Wyona Workspace"},"enterprise":null,"incoming_webhook":{"channel":"#test-katie-by-michael","channel_id":"C01BG53KWLA","configuration_url":"https://wyonaworkspace.slack.com/services/B00000000","url":"https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX"}}

            String okStatus = bodyNode.get("ok").asText();
            if (okStatus.equals("false")) {
                String error = bodyNode.get("error").asText();
                log.error(error);
                return null;
            }

            String teamId = bodyNode.get("team").get("id").asText();
            String teamName = bodyNode.get("team").get("name").asText();

            String channelId = bodyNode.get("incoming_webhook").get("channel_id").asText();
            String channelName = bodyNode.get("incoming_webhook").get("channel").asText();

            String botAccessToken = bodyNode.get("access_token").asText();
            //log.debug("Bot access token: " + botAccessToken);

            String botUserId = bodyNode.get("bot_user_id").asText();
            log.info("Bot user Id: " + botUserId);

            String appId = bodyNode.get("app_id").asText();
            log.info("App Id: " + appId);

            String userId = bodyNode.get("authed_user").get("id").asText();
            log.info("User Id: " + userId);

            return new AccessCredentials(teamId, teamName, channelId, channelName, botAccessToken, botUserId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create Katie domain for a particular Slack team / channel
     * @param teamId Team Id, e.g. "T01848J69AP"
     * @param teamName Team name, e.g. "Wyona Workspace"
     * @param channelId Channel Id, e.g. "C01BG53KWLA"
     * @param channelName Channel name, e.g. "#test-katie-by-michael"
     * @return newly created domain, which is connected with Slack team/workspace
     */
    public Context createDomainForSlackTeam(String teamId, String teamName, String channelId, String channelName) {
        //dataRepoService.addDomainIdSlackTeamMapping(teamId, bearerToken);

        StringBuilder name = new StringBuilder("Slack: Team ");
        if (teamName != null) {
            name.append(teamName + " (" + teamId + ")");
        } else {
            name.append(teamId);
        }
        name.append(" / Channel ");
        if (channelName != null) {
            name.append(channelName + " (" + channelId + ")");
        } else {
            name.append(channelId);
        }

        try {
            Context domain = contextService.createDomain(true, name.toString(), "AskKatie/Slack", false, null);
            if (domain != null) {
                connectDomainIdWithSlackTeamId(domain.getId(), teamId, channelId, ConnectStatus.APPROVED, null);
                return domain;
            } else {
                log.error("No domain was created for Slack team Id '" + teamId + "'!");
                return null;
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Add mapping between context/domain and Slack team (workspace?)
     * @param contextId Domain Id, e.g. 'wyona'
     * @param teamId Team Id, e.g. 'T01848J69AP'
     * @param channelId Channel Id, e.g. 'C01BG53KWLA'
     * @param status Status whether connection needs approval or has been approved or was discarded
     * @param token Token to match for approval
     */
    public void connectDomainIdWithSlackTeamId(String contextId, String teamId, String channelId, ConnectStatus status, String token) {
        try {
            dataRepoService.addDomainIdSlackTeamMapping(contextId, teamId, channelId, status, token);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
