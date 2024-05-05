package com.wyona.katie.integrations.discord.services;

import com.wyona.katie.models.Role;
import com.wyona.katie.models.User;
import com.wyona.katie.models.discord.DiscordDomainMapping;
import com.wyona.katie.models.slack.ConnectStatus;
import com.wyona.katie.models.slack.JWTClaims;
import com.wyona.katie.models.slack.SlackDomainMapping;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.DataRepositoryService;
import com.wyona.katie.services.JwtService;
import com.wyona.katie.models.discord.DiscordDomainMapping;
import com.wyona.katie.models.slack.ConnectStatus;
import com.wyona.katie.models.slack.JWTClaims;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.AccessDeniedException;

/**
 * Discord specific domain service
 */
@Slf4j
@Component
public class DiscordDomainService {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DataRepositoryService dataRepositoryService;

    @Autowired
    private ContextService domainService;

    /**
     * Approve connection between Discord guild/channel and Katie domain
     * @param token JWT containing Discord guild id and channel id
     */
    public void approveMapping(String token) throws AccessDeniedException, Exception {
        if (!jwtService.isJWTValid(token, null)) {
            throw new AccessDeniedException("Token is not valid!");
        }

        String domainId = jwtService.getJWTClaimValue(token, JWTClaims.DOMAIN_ID);
        if (!domainService.existsContext(domainId)) {
            throw new Exception("No such Katie domain '" + domainId + "'!");
        }

        String guildId = jwtService.getJWTClaimValue(token, JWTClaims.TEAM_ID);
        String channelId = jwtService.getJWTClaimValue(token, JWTClaims.CHANNEL_ID);

        dataRepositoryService.addDomainIdDiscordMapping(domainId, guildId, channelId, ConnectStatus.APPROVED, null);

        /*
        SlackDomainMapping mapping = getDomainMappingForSlackTeamChannel(teamId, channelId);
        if (mapping != null && mapping.getApprovalToken().equals(token)) {
            if (mapping.getStatus() == ConnectStatus.APPROVED) {
                log.info("Mapping between Slack team/channel and Katie domain is already approved.");
            } else if (mapping.getStatus() == ConnectStatus.NEEDS_APPROVAL) {
                dataRepoService.updateSlackConnectMappingStatus(teamId, channelId, ConnectStatus.APPROVED);
                try {
                    User technicalUser = iamService.getUserByUsername(usernameTechnicalUser, false, false);
                    contextService.addMember(technicalUser.getId(), false, false, mapping.getDomainId());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                log.info("Connection between Katie domain '" + mapping.getDomainId() + "' and Slack team / channel '" + teamId + " / " + channelId+ "' has been approved.");
            } else {
                log.warn("Connection status: " + mapping.getStatus());
            }
        } else {
            log.info("No mapping for team / channel '" + teamId + " / " + channelId + "'.");
        }

         */
    }

    /**
     * Disconnect a Discord team/channel from a domain
     * @return true when mapping was removed successfully and false otherwise
     */
    public boolean removeMapping(String guildId, String channelId) throws AccessDeniedException {
        String domainId = dataRepositoryService.getDomainIdForDiscordGuildChannel(guildId, channelId);
        if (!domainService.isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        if (domainId != null) {
            log.info("Remove Discord domain mapping ...");
            try {
                dataRepositoryService.removeDomainIdDiscordGuildChannelMapping(guildId, channelId);
                return true;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return false;
            }
        } else {
            log.warn("No mapping for guild / channel '" + guildId + " / " + channelId + "'.");
            return false;
        }
    }

    /**
     * Get all Discord channels for a particular Katie domain
     */
    public DiscordDomainMapping[] getDisordChannelsForDomain(String domainId) throws  AccessDeniedException {
        if (domainService.isMemberOrAdmin(domainId)) {
            DiscordDomainMapping[] mappings = dataRepositoryService.getDiscordDomainMappingForDomain(domainId);
            return mappings;
        } else {
            throw new AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }
    }

    /**
     * Get all domains which are connected with Discord channels
     * @param checkAuthorization True when authorization must be checked
     */
    public DiscordDomainMapping[] getAllDomains(boolean checkAuthorization) throws  AccessDeniedException {
        if (checkAuthorization && !domainService.isAdmin()) {
            throw new AccessDeniedException("User has not role " + Role.ADMIN + "!");
        } else {
            DiscordDomainMapping[] mappings = dataRepositoryService.getAllDomainsConnectedWithDiscordChannels();
            return mappings;
        }
    }
}
