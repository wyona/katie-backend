package com.wyona.katie.controllers.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.models.slack.SlackAppConfig;
import com.wyona.katie.services.AuthenticationService;

import com.wyona.katie.integrations.slack.SlackMessageSender;
import com.wyona.katie.integrations.slack.services.DomainService;
import com.wyona.katie.models.Error;
import com.wyona.katie.models.slack.SlackDomainMapping;
import com.wyona.katie.models.slack.SlackEventWrapper;
import com.wyona.katie.models.slack.SlackInteraction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Slack controller
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/slack") 
public class SlackController {

    @Value("${config.data_path}")
    private String configDataPath;

    @Value("${slack.client.id}")
    private String katieSlackClientId;

    @Value("${new.context.mail.body.host}")
    private String katieHost;

    @Value("${katie.redirect.landing.page}")
    private String redirectLandingPage;

    @Autowired
    private SlackMessageSender messageSender;

    @Autowired
    private DomainService domainService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private ResourceBundleMessageSource messageSource;

    /**
     *
     */
    @Autowired
    public SlackController() {
    }

    /**
     * Approve connection of a Slack team/channel with a Katie domain
     */
    @RequestMapping(value = "/connect-team-channel-domain", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Approve connection of a Slack team/channel with a Katie domain")
    public ResponseEntity<?> connectTeamChannelWithDomain(
            @ApiParam(name = "token", value = "JWT token containing info re team Id, channel Id (private claims: team_id, channel_id) to approve previously requested connection with domain", required = true)
            @RequestParam(value = "token", required = true) String token,
            HttpServletRequest request) {
        try {
            SlackDomainMapping mapping = domainService.approveMapping(token);

            if (mapping != null) {
                messageSender.sendNotificationThatConnectionGotApproved(mapping);
                return new ResponseEntity<>("{\"status\":\"APPROVED\"}", HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Error("Approval failed", "APPROVAL_FAILED"), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
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
            @ApiParam(name = "team-id", value = "Slack Team Id",required = true)
            @RequestParam(value = "team-id", required = true) String teamId,
            @ApiParam(name = "channel-id", value = "Slack Channel Id",required = true)
            @RequestParam(value = "channel-id", required = true) String channelId,
            HttpServletRequest request) {
        try {
            SlackDomainMapping mapping = domainService.getDomainMappingForSlackTeamChannel(teamId, channelId);
            if (mapping != null) {
                domainService.removeMapping(teamId, channelId);
            } else {
                log.info("No mapping for team / channel '" + teamId + " / " + channelId + "'.");
            }
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            //return new ResponseEntity<>("{}", HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Reroute team/channel to another domain
     */
    @RequestMapping(value = "/reroute-team-channel-domain", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Reroute team/channel to another domain")
    public ResponseEntity<?> reconnectTeamChannelFromDomain(
            @ApiParam(name = "team-id", value = "Slack Team Id",required = true)
            @RequestParam(value = "team-id", required = true) String teamId,
            @ApiParam(name = "channel-id", value = "Slack Channel Id",required = true)
            @RequestParam(value = "channel-id", required = true) String channelId,
            @ApiParam(name = "domain-id", value = "Katie Domain Id",required = true)
            @RequestParam(value = "domain-id", required = true) String domainId,
            HttpServletRequest request) {
        try {
            SlackDomainMapping mapping = domainService.getDomainMappingForSlackTeamChannel(teamId, channelId);
            if (mapping != null) {
                domainService.rerouteMapping(teamId, channelId, domainId);
            } else {
                log.info("No mapping for team / channel '" + teamId + " / " + channelId + "'.");
            }
            return new ResponseEntity<>("{}", HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     *
     */
    @RequestMapping(value = "/domains", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get all domains which are connected with Slack Teams / Channels")
    public ResponseEntity<?> getDomains() {
        try {
            SlackDomainMapping[] mappings = domainService.getMappings();
            return new ResponseEntity<>(mappings, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Get all Slack channels which are connected with a particular Katie domain
     */
    @RequestMapping(value = "/domain/{id}/channels", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get all Slack channels which are connected with a particular Katie domain")
    public ResponseEntity<?> getChannels(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request
    ) {
        try {
            SlackDomainMapping[] mappings = domainService.getSlackChannelsForDomain(id);
            return new ResponseEntity<>(mappings, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Get most recent messages from a particular Slack team / channel
     */
    @RequestMapping(value = "/sync/{team_id}/{channel_id}", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get most recent messages from a particular Slack team / channel")
    public ResponseEntity<?> sync(
            @ApiParam(name = "team_id", value = "Slack team Id",required = true)
            @PathVariable(value = "team_id", required = true) String teamId,
            @ApiParam(name = "channel_id", value = "Slack channel Id",required = true)
            @PathVariable(value = "channel_id", required = true) String channelId,
            HttpServletRequest request
    ) {
        try {
            domainService.sync(teamId, channelId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * OAuth2 callback for Slack
     */
    @RequestMapping(value = "/oauth2-callback", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="SLACK: OAuth2 callback for Slack for default App")
    public ResponseEntity<?> oauth2CallbackSlack(
            @ApiParam(name = "state", value = "Allows you to prevent an attack by confirming that the state value coming from the response matches the one you sent.",required = false)
            @RequestParam(value = "state", required = false) String state,
            @ApiParam(name = "code", value = "Temporary authorization code in order to exchange for an access token",required = true)
            @RequestParam(value = "code", required = true) String code,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            log.info("State: " + state);
            log.info("Code: " + code);

            domainService.updateSlackBotToken(katieSlackClientId, getClientSecret(katieSlackClientId), code, state);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", redirectLandingPage);

            return new ResponseEntity<>(headers, HttpStatus.TEMPORARY_REDIRECT);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * OAuth2 callback for Slack for custom App
     */
    @RequestMapping(value = "/oauth2-callback/{custom_client_id}", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="SLACK: OAuth2 callback for Slack for custom App")
    public ResponseEntity<?> oauth2CallbackSlackCustom(
            @ApiParam(name = "state", value = "Allows you to prevent an attack by confirming that the state value coming from the response matches the one you sent.",required = false)
            @RequestParam(value = "state", required = false) String state,
            @ApiParam(name = "code", value = "Temporary authorization code in order to exchange for an access token",required = true)
            @RequestParam(value = "code", required = true) String code,
            @ApiParam(name = "custom_client_id", value = "Custom Client ID",required = true)
            @PathVariable(value = "custom_client_id", required = true) String customClientId,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            log.info("State: " + state);
            log.info("Code: " + code);

            domainService.updateSlackBotToken(customClientId, getClientSecret(customClientId), code, state);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", redirectLandingPage);

            return new ResponseEntity<>(headers, HttpStatus.TEMPORARY_REDIRECT);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @param clientId Slacke App Client ID
     */
    private String getClientSecret(String clientId) {
        SlackAppConfig[] configs = getSlackConfigs(clientId);
        for (SlackAppConfig config : configs) {
            if (config.getClientId().equals(clientId)) {
                return config.getClientSecret();
            }
        }
        log.error("No config for Slack Client ID '" + clientId + "'!");
        return null;
    }

    /**
     * @param clientId Slacke App Client ID
     */
    private String[] getSigningSecret(String clientId) {
        // TODO: Use clientId
        List<String> signingSecrets = new ArrayList<>();
        SlackAppConfig[] configs = getSlackConfigs(clientId);
        for (SlackAppConfig config : configs) {
            signingSecrets.add(config.getSigningSecret());
        }
        return signingSecrets.toArray(new String[0]);
    }

    /**
     *
     */
    private SlackAppConfig[] getSlackConfigs(String clientId) {
        List<SlackAppConfig> configs = new ArrayList<>();

        File slackConfigFile = new File(configDataPath, "slack-apps.json");
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(slackConfigFile);
            if (rootNode.isArray()) {
                for (int i = 0; i < rootNode.size(); i++) {
                    JsonNode configNode = rootNode.get(i);
                    SlackAppConfig config = new SlackAppConfig();
                    config.setClientId(configNode.get("clientId").asText());
                    config.setClientSecret(configNode.get("clientSecret").asText());
                    config.setSigningSecret(configNode.get("signingSecret").asText());
                    configs.add(config);
                    log.info("Slack App Config: " + config);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return configs.toArray(new SlackAppConfig[0]);
    }

    /**
     * REST interface to handle Slack events (also see https://api.slack.com/apps/A0184KMLJJE/event-subscriptions)
     */
    @RequestMapping(value = "/events", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="SLACK: Handle Slack events")
    //public ResponseEntity<?> handleEvents(@RequestBody SlackEventWrapper event,
    public ResponseEntity<?> handleEvents(HttpServletRequest request) {

        // INFO: Request body is used for validating signature and processing actual request
        String payload = getRequestBody(request); // WARNING: Request body can only be read once!

        if (!isSignatureValid(request, payload)) {
            log.error("Signature of events request is not valid!");
            return new ResponseEntity<>(new Error("Signature invalid", "SIGNATURE_INVALID"), HttpStatus.BAD_REQUEST);
        }

        // TODO: https://www.yawintutor.com/json-string-to-java-object-array-using-json-b/
        Jsonb jsonb = JsonbBuilder.create();
        SlackEventWrapper event = jsonb.fromJson(payload, SlackEventWrapper.class);

        log.info("Try to handle Event: " + event);
        // Event: Token: hoMub0zJ8kMXIxgqFgrDIc3x, Type: event_callback, Team Id: T01848J69AP, Event Id: Ev017RBBMVHD, Event: Type: message, User: U018A80DU4C, Channel: D018UQW3A8G, Text: What time is it?

        if (event.getType().equals("url_verification")) {
            return new ResponseEntity<>("{\"challenge\":\"" + event.getChallenge() + "\"}", HttpStatus.OK);
        } else if (event.getType().equals("event_callback")) {
            if (event.getEvent().getType().equals("message")) {
                if (event.getEvent().getText() != null) {
                    if (event.getEvent().getSubtype() != null && event.getEvent().getSubtype().equals("bot_message") || event.getEvent().getBot_id() != null) {
                        log.info("Subtype: " + event.getEvent().getSubtype());
                        log.info("Bot Id: " + event.getEvent().getBot_id());
                        // INFO: Katie should not reply to herself, or when another bot goes wild and keeps sending questions! See for example https://stackoverflow.com/questions/40756524/slack-bot-replying-to-his-own-message-in-node-js
                        log.warn("Message was sent by a bot, therefore do not answer, because it might create a question answering loop!.");
                    } else {
                        log.info("Message was sent by user with Id '" + event.getEvent().getUser() + "' (Subtype: " + event.getEvent().getSubtype() + ")");
                        // INFO: Response will be sent asynchronously, because Slack recommends acknowledging with 200 right away before sending a new message or fetching information from your database since you only have 3 seconds to respond.
                        messageSender.sendEventResponse(event);
                    }
                } else {
                    log.warn("Message event does not contain text.");
                }
            }
            log.info("Acknowledge message ...");
            return new ResponseEntity<>("{}", HttpStatus.OK);
        } else {
            log.warn("Event type '" + event.getType() + "' not implemented!");
            return new ResponseEntity<>("{}", HttpStatus.OK);
        }
    }

    /**
     * REST interface to handle Slack interactions (also see https://api.slack.com/apps/A0184KMLJJE/interactive-messages?)
     */
/*
payload=%7B%22type%22%3A%22block_actions%22%2C%22user%22%3A%7B%22id%22%3A%22U018A7XUWSY%22%2C%22username%22%3A%22michael.wechner%22%2C%22name%22%3A%22michael.wechner%22%2C%22team_id%22%3A%22T01848J69AP%22%7D%2C%22api_app_id%22%3A%22A0184KMLJJE%22%2C%22token%22%3A%22hoMub0zJ8kMXIxgqFgrDIc3x%22%2C%22container%22%3A%7B%22type%22%3A%22message%22%2C%22message_ts%22%3A%221600724836.000800%22%2C%22channel_id%22%3A%22C017PA1MC1M%22%2C%22is_ephemeral%22%3Afalse%7D%2C%22trigger_id%22%3A%221364028095735.1276290213363.94dfc599c4ba788c5f2b9e8ab51e5e88%22%2C%22team%22%3A%7B%22id%22%3A%22T01848J69AP%22%2C%22domain%22%3A%22wyonaworkspace%22%7D%2C%22channel%22%3A%7B%22id%22%3A%22C017PA1MC1M%22%2C%22name%22%3A%22allgemein%22%7D%2C%22message%22%3A%7B%22bot_id%22%3A%22B018B5MRHFW%22%2C%22type%22%3A%22message%22%2C%22text%22%3A%22The+current+time+is+2020%5C%2F09%5C%2F21+21%3A47%3A16%2C+whereas+also+see+%3Chttps%3A%5C%2F%5C%2Fwww.timeanddate.com%5C%2F%3E%22%2C%22user%22%3A%22U018505QFA6%22%2C%22ts%22%3A%221600724836.000800%22%2C%22team%22%3A%22T01848J69AP%22%2C%22blocks%22%3A%5B%7B%22type%22%3A%22section%22%2C%22block_id%22%3A%221qH%5C%2F%22%2C%22text%22%3A%7B%22type%22%3A%22plain_text%22%2C%22text%22%3A%22The+current+time+is+2020%5C%2F09%5C%2F21+21%3A47%3A16%2C+whereas+also+see+https%3A%5C%2F%5C%2Fwww.timeanddate.com%5C%2F%22%2C%22emoji%22%3Atrue%7D%7D%2C%7B%22type%22%3A%22actions%22%2C%22block_id%22%3A%22yqELu%22%2C%22elements%22%3A%5B%7B%22type%22%3A%22button%22%2C%22action_id%22%3A%22bVdPR%22%2C%22text%22%3A%7B%22type%22%3A%22plain_text%22%2C%22text%22%3A%22Send+question+to+an+expert+...%22%2C%22emoji%22%3Afalse%7D%7D%5D%7D%5D%7D%2C%22response_url%22%3A%22https%3A%5C%2F%5C%2Fhooks.slack.com%5C%2Factions%5C%2FT00000000%5C%2FB00000000%5C%2FXXXXXXXXXXXXXXXXXXXXXXXX%22%2C%22actions%22%3A%5B%7B%22action_id%22%3A%22bVdPR%22%2C%22block_id%22%3A%22yqELu%22%2C%22text%22%3A%7B%22type%22%3A%22plain_text%22%2C%22text%22%3A%22Send+question+to+an+expert+...%22%2C%22emoji%22%3Afalse%7D%2C%22type%22%3A%22button%22%2C%22action_ts%22%3A%221600760697.097898%22%7D%5D%7D
*/
    @RequestMapping(value = "/interactivity", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="SLACK: Handle Slack interactions")
    public ResponseEntity<?> handleInteractions(HttpServletRequest request) {

        String payload = getRequestBody(request); // INFO: The payload is URL encoded JSON, see https://api.slack.com/messaging/interactivity#components

        if (!isSignatureValid(request, payload)) {
            log.error("Signature of interactivity request is not valid!");
            return new ResponseEntity<>(new Error("Signature invalid", "SIGNATURE_INVALID"), HttpStatus.BAD_REQUEST);
        }

        log.info("Slack interaction request received from remote host: " + AskController.getRemoteAddress(request));

        log.info("Payload: " + payload.substring(8)); // INFO: Remove payload=

        String decodedPayload = java.net.URLDecoder.decode(payload.substring(8));
        log.info("Decoded payload: " + decodedPayload);

        // TODO: https://www.yawintutor.com/json-string-to-java-object-array-using-json-b/
        Jsonb jsonb = JsonbBuilder.create();
        SlackInteraction interaction = jsonb.fromJson(decodedPayload, SlackInteraction.class);

        messageSender.sendInteractionResponse(interaction);

        // https://api.slack.com/interactivity/handling#acknowledgment_response
        log.info("Acknowledge interaction '" + interaction.getType() + "' ...");
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }

    // TODO: Consider introducing more commands, like for example '/katie-info' in order to return information such as for example statistics on Katie usage by Slack team/workspace

    /**
     * REST interface to handle command '/katie' (also see https://api.slack.com/interactivity/slash-commands)
     */
    @RequestMapping(value = "/command/katie", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="SLACK: Handle '/katie' command")
    public ResponseEntity<?> commandKatie(HttpServletRequest request) {

        String payload = getRequestBody(request);

        if (!isSignatureValid(request, payload)) {
            log.error("Signature of command request is not valid!");
            return new ResponseEntity<>(new Error("Signature invalid", "SIGNATURE_INVALID"), HttpStatus.BAD_REQUEST);
        }

        log.info("Slack '/katie' command request received from remote host: " + AskController.getRemoteAddress(request));

        log.info("Payload: " + payload); // INFO: The payload is documented at https://api.slack.com/interactivity/slash-commands#app_command_handling
        // Payload: token=zoMub2zJ8kBXIxrqFgrDIc1x&team_id=T0LP49B4Y&team_domain=wyona&channel_id=C02AGB0BLQ4&channel_name=test-katie&user_id=U0LP4BRLG&user_name=michi&command=%2Fkatie&text=help&api_app_id=A0184KMLJJE&is_enterprise_install=false&response_url=https%3A%2F%2Fhooks.slack.com%2Fcommands%2FT00000000%2FB00000000&trigger_id=2891180720998.20783317168.ee00f88ea326eb90948243305b68b689
        HashMap<String, String> payloadParams = parseCommandPayload(payload);

        log.info("Parameters: team_id=" + payloadParams.get("team_id") + ", channel_id=" + payloadParams.get("channel_id") + ", text=" + payloadParams.get("text"));

        log.info("Acknowledge '/katie' command request ...");

        String[] args = new String[2];
        args[0] = payloadParams.get("user_name");
        args[1] = katieHost;
        String message = messageSource.getMessage("katie.command.answer", args, new Locale("en"));
        log.debug("Katie command message: " + message);

        StringBuilder body = new StringBuilder("{");
        body.append("\"response_type\": \"ephemeral\",");
        body.append("\"text\": \"" + message + "\"");
        body.append("}");

        return new ResponseEntity<>(body.toString(), HttpStatus.OK);
    }

    /**
     * Parse command payload
     * @param payload Command payload, e.g. "token=hoNub0yJ8kMXKxfqFgrDIa7x&team_id=T01848J69AP&team_domain=wyonaworkspace&channel_id=C01BG53KWLA&channel_name=test-katie-by-michael&user_id=U018A80DU4C&user_name=michi651&command=%2Fask&text=Who+is+Michael%3F&api_app_id=A0184KMLJJE&is_enterprise_install=false&response_url=https%3A%2F%2Fhooks.slack.com%2Fcommands%2FT00000000%2FB00000000&trigger_id=1607602933366.1276290213363.89b446a5499a6a022182ac70916ea912"
     */
    private HashMap<String, String> parseCommandPayload(String payload) {
        String[] entries = payload.split("&");
        HashMap<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < entries.length; i++) {
            String[] entry = entries[i].split("=");
            if (entry.length == 2) {
                map.put(entry[0], entry[1]);
            } else if (entry.length == 1) {
                log.info("Value of parameter '" + entry[0] + "' is empty.");
                map.put(entry[0], "");
            } else {
                log.warn("Splitting of '" + entries[i] + "' did not work as expected.");
            }
        }
        return map;
    }

    /**
     * Check whether signature is valid
     * https://api.slack.com/authentication/verifying-requests-from-slack
     * @param body Raw request body, e.g. "payload=%7B%22type%22%3A%22block_actions%22...."
     * @return true when signature is valid and false otherwise
     */
    private boolean isSignatureValid(HttpServletRequest request, String body) {
        String xSlackSignatureTimestamp = request.getHeader("X-Slack-Request-Timestamp");
        log.info("X-Slack-Request-Timestamp: " + xSlackSignatureTimestamp);

        if (isMoreThanFiveMinutesFromCurrentTime(xSlackSignatureTimestamp)) {
            log.error("Request timestamp is more than five minutes from current time!");
            return false;
        }

        String xSlackSignature = request.getHeader("X-Slack-Signature");
        String[] slackAppsSigningSecrets = getSigningSecret("TODO");
        return authenticationService.isSignatureValid(body, xSlackSignatureTimestamp, xSlackSignature, slackAppsSigningSecrets);
    }

    /**
     * Check whether request timestamp is more than five minutes from current time.
     * @param timestamp Request timestamp (in seconds), e.g. 1653084377
     * @return true when request timestamp is more than five minutes from current time, It could be a replay attack.
     */
    private boolean isMoreThanFiveMinutesFromCurrentTime(String timestamp) {
        long currentTime = new java.util.Date().getTime() / 1000;
        log.info("Current time in seconds: " + currentTime);

        if ((currentTime - Long.parseLong(timestamp)) > 60 * 5) {
            log.warn("Request timestamp is more than five minutes from current time!");
            return true;
        }

        return false;
    }

    /**
     *
     */
    private String getRequestBody(HttpServletRequest request) {
        log.info("Get body of request '" + request.getRequestURI() + "' ...");
        StringBuilder body = new StringBuilder();
        try {
            BufferedReader bufferedReader = request.getReader();
            char[] charBuffer = new char[128];
            int bytesRead;
            while ((bytesRead = bufferedReader.read(charBuffer)) != -1) {
                body.append(charBuffer, 0, bytesRead);
            }

            if (body.toString().isEmpty()) {
                log.warn("Request body is empty! Make sure, that log level is not set to DEBUG, because when set to DEBUG, then Spring might consume the request body! See https://github.com/spring-projects/spring-framework/issues/28587");
            }

            return body.toString();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
