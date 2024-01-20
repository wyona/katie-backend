package com.wyona.katie.integrations.slack.services;

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
import com.wyona.katie.models.slack.ConnectStatus;
import com.wyona.katie.models.slack.JWTClaims;
import com.wyona.katie.models.slack.SlackDomainMapping;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.wyona.katie.models.Context;
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

    @Value("${slack.post.message.url}")
    private String postMessageURL;

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
     * Update bearer token of a particular Slack team
     * @param bearerToken Bearer / access token, such that Katie can send messages to Slack (https://api.slack.com/authentication/token-types#bot)
     */
    public void updateSlackBotToken(String teamId, String bearerToken, String userId) {
        try {
            dataRepoService.updateSlackBearerToken(teamId, bearerToken, userId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
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
