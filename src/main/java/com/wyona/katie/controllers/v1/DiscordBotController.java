package com.wyona.katie.controllers.v1;

import com.wyona.katie.integrations.discord.services.DiscordDomainService;
import com.wyona.katie.models.discord.DiscordDomainMapping;
import com.wyona.katie.models.Error;
import com.wyona.katie.services.AuthenticationService;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.DataRepositoryService;
import com.wyona.katie.services.QuestionAnsweringService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.AccessDeniedException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

/**
 * Discord bot controller
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/discord") 
public class DiscordBotController {

    @Autowired
    private QuestionAnsweringService qaService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private DataRepositoryService dataRepositoryService;

    @Autowired
    private DiscordDomainService discordDomainService;

    @Value("${discord.public_key}")
    private String publicKey;

    /**
     *
     */
    @Autowired
    public DiscordBotController() {
    }

    /**
     * REST interface to handle Discord Bot interactions
     * https://discord.com/developers/docs/interactions/receiving-and-responding
     * {"application_id":"936031366443859989","id":"996062835757035631","token":"aW50...eWs5cnROT1V5Rmo0","type":1,"user":{
     *   "avatar":"cad1b786b45ce32186c8afd4620373d9","avatar_decoration":null,"discriminator":"6901","id":"691680354964340797","public_flags":0,"username":"michaelwech
     *   ner"},"version":1}
     */
    @RequestMapping(value = "/interaction", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary="Handle Discord bot interaction")
    public ResponseEntity<?> handleEvents(@RequestBody String payload,
        HttpServletRequest request) {

        // https://discord.com/developers/docs/interactions/receiving-and-responding#security-and-authorization
        String xSignatureTimestamp = request.getHeader("x-signature-timestamp");
        String xSignatureEd25519 = request.getHeader("x-signature-ed25519");
        if (!authenticationService.isDiscordSignatureValid(payload, xSignatureTimestamp, xSignatureEd25519, publicKey)) {
            log.error("Bad request signature!");
            return new ResponseEntity<>(new Error("Bad request signature", "UNAUTHORIZED"), HttpStatus.UNAUTHORIZED);
        }

        log.info("Received interaction: " + payload);

        log.info("Acknowledge interaction ...");

        // TODO: Acknowledge type according to request

        return new ResponseEntity<>("{\"type\":1}", HttpStatus.OK);
    }

    /**
     * Approve connection of a Discord guild/channel with a Katie domain
     */
    @RequestMapping(value = "/approve-guild-channel-domain", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary="Approve connection of a Discord guild/channel with a Katie domain")
    public ResponseEntity<?> connectGuildChannelWithDomain(
            @Parameter(name = "token", description = "JWT token containing info re guild Id, channel Id (private claims: guild_id, channel_id) TEMP: Payload requires domain_id, team_id, channel_id", required = true)
            @RequestParam(value = "token", required = true) String token,
            HttpServletRequest request) {
        try {
            discordDomainService.approveMapping(token);

            // TODO: Send notification to Discord channel, that connection has been approved
            /*
            SlackDomainMapping mapping = domainService.approveMapping(token);

            if (mapping != null) {
                messageSender.sendNotificationThatConnectionGotApproved(mapping);
                return new ResponseEntity<>("{\"status\":\"APPROVED\"}", HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Error("Approval failed", "APPROVAL_FAILED"), HttpStatus.INTERNAL_SERVER_ERROR);
            }
             */

            return new ResponseEntity<>("{\"status\":\"APPROVED\"}", HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Disconnect a Discord guild/channel from a domain
     */
    @RequestMapping(value = "/disconnect-guild-channel-domain", method = RequestMethod.DELETE, produces = "application/json")
    @Operation(summary="Disconnect a Discord guild/channel from a domain")
    public ResponseEntity<?> disconnectGuildChannelFromDomain(
            @Parameter(name = "guild_id", description = "Discord guild Id",required = true)
            @RequestParam(value = "guild_id", required = true) String guildId,
            @Parameter(name = "channel_id", description = "Discord channel Id",required = true)
            @RequestParam(value = "channel_id", required = true) String channelId,
            HttpServletRequest request) {
        try {
            boolean removedSuccessfully = discordDomainService.removeMapping(guildId, channelId);
            if (removedSuccessfully) {
                return new ResponseEntity<>("{}", HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Get all Discord channels which are connected with a particular Katie domain
     */
    @RequestMapping(value = "/domain/{id}/channels", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary="Get all Discord channels which are connected with a particular Katie domain")
    public ResponseEntity<?> getChannels(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request
    ) {
        try {
            DiscordDomainMapping[] mappings = discordDomainService.getDisordChannelsForDomain(id);
            return new ResponseEntity<>(mappings, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Get all Katie domains which are connected with Discord channels
     */
    @RequestMapping(value = "/domains", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary="Get all Katie domains which are connected with Discord channels")
    public ResponseEntity<?> getDomains(
            HttpServletRequest request
    ) {
        try {
            DiscordDomainMapping[] mappings = discordDomainService.getAllDomains(true);
            return new ResponseEntity<>(mappings, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }
    }
}
