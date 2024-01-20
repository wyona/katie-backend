package com.wyona.katie.integrations.msteams.services;
  
import com.wyona.katie.models.JWT;
import com.wyona.katie.models.Role;
import com.wyona.katie.models.User;
import com.wyona.katie.models.msteams.MSTeamsDomainMapping;
import com.wyona.katie.models.slack.JWTClaims;
import com.wyona.katie.services.AuthenticationService;
import com.wyona.katie.services.JwtService;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.msteams.MSTeamsDomainMapping;
import com.wyona.katie.models.slack.JWTClaims;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wyona.katie.models.Context;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.DataRepositoryService;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;

/**
 * Microsoft specific domain service
 */
@Slf4j
@Component
public class MicrosoftDomainService {

    @Autowired
    private ContextService contextService;

    @Autowired
    private DataRepositoryService dataRepoService;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private JwtService jwtService;

    /**
     * Get all mappings between Katie and MS Teams
     */
    public MSTeamsDomainMapping[] getMappings() throws AccessDeniedException {
        if (isAdmin()) {
            MSTeamsDomainMapping[] mappings = dataRepoService.getDomainMappingsForMSTeams();
            return mappings;
        } else {
            throw new AccessDeniedException("User is either not signed in or has not role ADMIN!");
        }
    }

    /**
     *
     */
    public MSTeamsDomainMapping[] getMappings(String domainId, String teamId, String channelId) throws AccessDeniedException {
        if (isAdmin()) {
            MSTeamsDomainMapping[] allMappings = dataRepoService.getDomainMappingsForMSTeams();
            java.util.List<MSTeamsDomainMapping> mappings = new ArrayList<MSTeamsDomainMapping>();
            for (MSTeamsDomainMapping mapping: allMappings) {
                if (mapping.getDomainId().equals(domainId) && mapping.getTeamId().equals(teamId)) {
                    if (channelId != null) {
                        if (mapping.getChannelId().equals(channelId)) {
                            mappings.add(mapping);
                        } else {
                            log.info("Channel does not match for mapping between domain '" + domainId + "' and team '" + teamId + "'.");
                        }
                    } else {
                        log.info("No channel Id provided.");
                        mappings.add(mapping);
                    }
                }
            }
            return mappings.toArray(new MSTeamsDomainMapping[0]);
        } else {
            throw new AccessDeniedException("User is either not signed in or has not role ADMIN!");
        }
    }

    /**
     * @param token JWT containing team Id
     */
    public MSTeamsDomainMapping approveMapping(String token, String domainIdTmp) throws Exception {
        if (!jwtService.isJWTValid(token, null)) {
            throw new AccessDeniedException("Token is not valid!");
        }

        String teamId = jwtService.getJWTClaimValue(token, JWTClaims.TEAM_ID);

        // TODO: See src/main/java/com/wyona/katie/integrations/slack/services/DomainService.java#approveMapping(String)

        Context domain = null;
        if (domainIdTmp == null) {
            String name = "MS Team " + teamId;
            domain = contextService.createDomain(true, name, "AskKatie/MS Teams", false, null);
        } else {
            domain = contextService.getDomain(domainIdTmp);
        }

        connectDomainWithMSTeam(teamId, domain);

        MSTeamsDomainMapping[] mappings = getMappings(domain.getId(), teamId, null);

        return mappings[0];
    }

    /**
     *
     */
    public void removeMapping(String domainId, String teamId, String channelId) throws AccessDeniedException {
        if (!isAdmin()) {
            throw new AccessDeniedException("User is either not signed in or has not role ADMIN!");
        }
        try {
            dataRepoService.removeDomainIdMSTeamsMapping(domainId, teamId, channelId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Check whether user is signed in and if so whether user is administrator
     * @return true when user is administrator and false otherwise
     */
    private boolean isAdmin() {
        log.info("Check whether user is signed in and has role " + Role.ADMIN + " ...");

        User signedInUser = authService.getUser(false, false);
        if (signedInUser != null) {
            log.info("Signed in user: " + signedInUser.getUsername());

            if (signedInUser.getRole() == Role.ADMIN) {
                return true;
            } else {
                log.warn("User '" + signedInUser.getId() + "' has not role '" + Role.ADMIN + "'!");
                return false;
            }
        } else {
            log.warn("User is not signed in!");
            return false;
        }
    }

    /**
     * Get Katie domain associated with Microsoft team
     * @param team Team Id, e.g. '19:6b1b63b1617c4946959c334b2d428fc0@thread.tacv2'
     * @return domain or null when no domain linked yet with MS team Id
     */
    public Context getDomain(String team) {
        log.info("Check whether Katie domain exists for MS Teams Team Id '" + team + "' ...");

        String domainId = dataRepoService.getDomainIdForMSTeam(team);
        if (domainId == null) {
            log.info("No Katie domain linked yet with MS team Id '" + team + "'.");
            return null;
        } else {
            try {
                if (contextService.existsContext(domainId)) {
                    return contextService.getContext(domainId);
                } else {
                    log.info("Do some 'house cleaning', because Domain might have been deleted by Katie administrator");
                    try {
                        dataRepoService.removeDomainIdMSTeamsMapping(domainId, team, null);
                    } catch (Exception drsException) {
                        log.error(drsException.getMessage(), drsException);
                    }
                    return null;
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }
    }

    /**
     * Connect Katie domain with a particular "MS Teams" team
     * @param teamId Team Id, e.g. "19:6b1b63b1617c4946959c334b2d428fc0@thread.tacv2"
     */
    public void connectDomainWithMSTeam(String teamId, Context domain) throws Exception {
        try {
            dataRepoService.addDomainIdMSTeamsMapping(domain.getId(), teamId);
            log.info("Katie domain '" + domain.getId() + "' linked with MS Teams Id '" + teamId + "'.");
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
