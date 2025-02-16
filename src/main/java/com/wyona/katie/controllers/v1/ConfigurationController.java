package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.Error;

import com.wyona.katie.mail.EmailSenderConfig;
import com.wyona.katie.models.*;
import com.wyona.katie.services.*;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller to ask questions (Version 1)
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1") 
public class ConfigurationController {

    @Value("${katie.version}")
    private String katieVersion;

    @Value("${katie.environment}")
    private String katieEnv;

    @Value("${ner.implementation}")
    private NerImpl defaultNerImplementation;

    @Value("${qc.implementation}")
    private QuestionClassificationImpl questionClassificationImpl;

    @Value("${lucene.vector.search.embedding.impl}")
    private EmbeddingsImpl defaultEmbeddings;

    @Value("${re_rank.implementation}")
    private ReRankImpl defaultReRankImpl;

    @Value("${slack.redirect.uri}")
    private String slackRedirectUri;

    @Value("${ms.redirect.uri}")
    private String microsoftRedirectUri;

    @Value("${new.context.mail.body.host}")
    private String defaultHostnameMailBody;

    @Value("${mail.default.admin.email.address}")
    private String emailSystemAdmin;

    // TODO: Parse application.properties
    @Value("${app.mail.host}")
    private String appMailHost;
    @Value("${app.mail.port}")
    private String appMailPort;
    @Value("${app.mail.username}")
    private String appMailUsername;
    @Value("${app.mail.password}")
    private String appMailPassword;
    @Value("${app.mail.smtp.starttls.enabled}")
    private String appMailSmtpStarttlsEnabled;
    @Value("${mail.default.sender.email.address}")
    private String mailDefaultSenderEmailAddress;
    @Value("${discord.enabled}")
    private Boolean discordEnableld;
    @Value("${new.context.mail.body.host}")
    private String newContextMailBodyHost;
    @Value("${volume.base.path}")
    private String volumeBasePath;

    @Value("${ms.oauth.url}")
    private String msOauthUrl;
    @Value("${ms.grant_type}")
    private String msGrant_type;
    @Value("${ms.client.id}")
    private String msClientId;
    @Value("${ms.client.secret}")
    private String msClientSecret;
    @Value("${ms.scope}")
    private String msScope;
    @Value("${ms.public_keys.url}")
    private String msPublic_keysUrl;

    @Value("${sbert.scheme}")
    private String sbert_scheme;
    @Value("${sbert.hostname}")
    private String sbert_hostname;
    @Value("${sbert.port}")
    private String sbert_port;
    @Value("${sbert.basic.auth.username}")
    private String sbert_basic_auth_username;
    @Value("${sbert.basic.auth.password}")
    private String sbert_basic_auth_password;

    @Value("${http.proxy.enabled}")
    private Boolean httpProxyEnabled;

    @Value("${http.proxy.host}")
    private String httpProxyHost;
    @Value("${http.proxy.port}")
    private String httpProxyPort;

    @Value("${https.proxy.host}")
    private String httpsProxyHost;
    @Value("${https.proxy.port}")
    private String httpsProxyPort;

    @Value("${http.non.proxy.hosts}")
    private String httpNonProxyHosts;

    @Value("${http.proxy.user}")
    private String httpProxyUser;
    @Value("${http.proxy.password}")
    private String httpProxyPassword;

    @Value("${https.proxy.user}")
    private String httpsProxyUser;
    @Value("${https.proxy.password}")
    private String httpsProxyPassword;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private IAMService iamService;

    @Autowired
    private RememberMeService rememberMeService;

    @Autowired
    private EmailSenderConfig emailSenderCofig;

    /**
     * REST interface to get version of Katie
     */
    @RequestMapping(value = "/configuration/version", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get version of Katie and environment type")
    public ResponseEntity<?> getVersion(
            HttpServletRequest request) {

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"version\":\"" + katieVersion + "\",");
        sb.append("\"environment\":\"" + katieEnv + "\"");
        sb.append("}");

        return new ResponseEntity<>(sb.toString(), HttpStatus.OK);
    }

    /**
     * REST interface to get domain Id for a particular domain tag name
     */
    @RequestMapping(value = "/configuration/domain-id", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get domain Id for a particular domain tag name")
    public ResponseEntity<?> getDomainId(
            @ApiParam(name = "tag-name", value = "Tag name of domain, e.g. 'apache-lucene'",required = true)
            @RequestParam("tag-name") String tagName,
            HttpServletRequest request) {

        try {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"domain-id\":\"" + contextService.getDomainByTagName(tagName).getId() + "\"");
            sb.append("}");

            return new ResponseEntity<>(sb.toString(), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "UNKNOWN_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get server configuration
     */
    @RequestMapping(value = "/configuration", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get server configuration")
    public ResponseEntity<?> getConfiguration(
        HttpServletRequest request) {

        if (contextService.isAdmin()) {
            String[] domainIDs = contextService.getContextIDs();
            ServerConfiguration config = new ServerConfiguration(katieEnv, katieVersion, domainIDs, defaultNerImplementation, questionClassificationImpl, defaultEmbeddings, defaultReRankImpl, defaultHostnameMailBody, slackRedirectUri, microsoftRedirectUri, emailSenderCofig, emailSystemAdmin);
            return new ResponseEntity<>(config, HttpStatus.OK);
        } else {
            ServerConfigurationPublic configPublic = new ServerConfigurationPublic(katieEnv, katieVersion, defaultHostnameMailBody, slackRedirectUri, microsoftRedirectUri, emailSystemAdmin);
            return new ResponseEntity<>(configPublic, HttpStatus.OK);
            //return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * REST interface to get application properties
     */
    @RequestMapping(value = "/application-properties", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get application properties")
    public ResponseEntity<?> getApplicationProperties(
            HttpServletRequest request) {

        User user = authService.getUser(false, false);
        if (!(user != null && user.getRole().equals(Role.ADMIN))) {
            log.warn("User either not signed in or does not have system role '" + Role.ADMIN + "'!");
            return new ResponseEntity<>(new Error("Access denied", "UNAUTHORIZED"), HttpStatus.UNAUTHORIZED);
        }

        Map<String, String> properties = new HashMap<String, String>();
        // TODO: Get all properties except secrets!
        properties.put("katie.version", katieVersion);

        properties.put("app.mail.host", appMailHost);
        properties.put("app.mail.port", appMailPort);
        properties.put("app.mail.username", appMailUsername);
        properties.put("app.mail.password", Utils.obfuscateSecret(appMailPassword));
        properties.put("app.mail.smtp.starttls.enabled", appMailSmtpStarttlsEnabled);
        properties.put("mail.default.sender.email.address", mailDefaultSenderEmailAddress);

        properties.put("discord.enabled", discordEnableld.toString());
        properties.put("new.context.mail.body.host", newContextMailBodyHost);
        properties.put("volume.base.path", volumeBasePath);

        properties.put("ms.oauth.url", msOauthUrl);
        properties.put("ms.grant_type", msGrant_type);
        properties.put("ms.client.id", msClientId);
        properties.put("ms.client.secret", Utils.obfuscateSecret(msClientSecret));
        properties.put("ms.scope", msScope);
        properties.put("ms.public_keys.url", msPublic_keysUrl);
        properties.put("ms.redirect.uri", microsoftRedirectUri);

        properties.put("sbert.scheme", sbert_scheme);
        properties.put("sbert.hostname", sbert_hostname);
        properties.put("sbert.port", sbert_port);
        properties.put("sbert.basic.auth.username", sbert_basic_auth_username);
        properties.put("sbert.basic.auth.password", sbert_basic_auth_password);

        properties.put("http.proxy.enabled", httpProxyEnabled.toString());
        properties.put("http.proxy.host", httpProxyHost);
        properties.put("http.proxy.port", httpProxyPort);
        properties.put("https.proxy.host", httpsProxyHost);
        properties.put("https.proxy.port", httpsProxyPort);
        properties.put("http.non.proxy.hosts", httpNonProxyHosts);
        properties.put("http.proxy.user", httpProxyUser);
        properties.put("http.proxy.password", Utils.obfuscateSecret(httpProxyPassword));
        properties.put("https.proxy.user", httpsProxyUser);
        properties.put("https.proxy.password", Utils.obfuscateSecret(httpsProxyPassword));

        String tunnelingDisabledSchemes = System.getProperty("jdk.http.auth.tunneling.disabledSchemes");
        properties.put("jdk.http.auth.tunneling.disabledSchemes", tunnelingDisabledSchemes);
        String proxyingDisabledSchemes = System.getProperty("jdk.http.auth.proxying.disabledSchemes");
        properties.put("jdk.http.auth.proxying.disabledSchemes", proxyingDisabledSchemes);

        return new ResponseEntity<>(properties, HttpStatus.OK);
    }

    /**
     * REST interface to get all domain configurations which signed in user has access to
     */
    // TODO: Rename to my-domains
    @RequestMapping(value = "/configuration/context", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get all domain configurations which signed in user has access to")
    public ResponseEntity<?> getContextConfiguration(HttpServletRequest request, HttpServletResponse response) {

        rememberMeService.tryAutoLogin(request, response);

        User user = authService.getUser(false, false);
        if (user != null) {
            log.info("Get list of domains which signed in user '" + user.getUsername() + "'  has access to.");
        } else {
            log.warn("User not signed in!");
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }

        // TEST: Uncomment lines below to test frotend spinner
        /*
        try {
            for (int i = 0; i < 1; i++) {
                log.info("Sleep for 2 seconds ...");
                Thread.sleep(2000);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
         */

        String[] domainIDs = null;
        if (user.getRole() == Role.ADMIN) {
            domainIDs = contextService.getContextIDs();
        } else {
            domainIDs = contextService.getDomainIDsUserIsMemberOf(user);
        }

        java.util.List<Context> configs = new java.util.ArrayList<Context>();
        for (int i = 0; i < domainIDs.length; i++) {
            try {
                configs.add(contextService.getDomain(domainIDs[i]));
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        return new ResponseEntity<>(configs, HttpStatus.OK);
    }

    /**
     * REST interface to get all domain configurations a particular user is member of
     */
    @RequestMapping(value = "/configuration/domains/{user-id}", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get all domain configurations a particular user is member of")
    public ResponseEntity<?> getDomainIDsUserIsMemberOf(
            @ApiParam(name = "user-id", value = "User Id",required = true)
            @PathVariable("user-id") String userId,
            HttpServletRequest request, HttpServletResponse response) {

        rememberMeService.tryAutoLogin(request, response);

        try {
            User user = iamService.getUserById(userId, false);
            if (user == null) {
                return new ResponseEntity<>(new Error("No such user '" + userId + "'!", "NO_SUCH_USER"), HttpStatus.BAD_REQUEST);
            }

            String[] domainIDs = contextService.getDomainIDsUserIsMemberOf(user);

            java.util.List<Context> configs = new java.util.ArrayList<Context>();
            for (int i = 0; i < domainIDs.length; i++) {
                try {
                    configs.add(contextService.getDomain(domainIDs[i]));
                } catch(Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            return new ResponseEntity<>(configs, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to get MyKatie domain of signed in user
     */
    @RequestMapping(value = "/configuration/my-katie-domain", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get MyKatie domain of signed in user")
    public ResponseEntity<?> getMyKatieDomain(HttpServletRequest request, HttpServletResponse response) {

        rememberMeService.tryAutoLogin(request, response);

        User user = authService.getUser(false, false);
        if (user != null) {
            log.info("Get MyKatie domain of signed in user '" + user.getUsername() + "' ...");
        } else {
            log.warn("User not signed in!");
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }

        try {
            String domainId = contextService.getPersonalDomainId(user.getId());
            if (domainId != null) {
                Context myKatieDomain = contextService.getDomain(domainId);

                return new ResponseEntity<>(myKatieDomain, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Error("MyKatie domain Id is not configured for user '" + user.getId() + "'.", "MYKATIE_ID_NOT_CONFIGURED"), HttpStatus.NOT_FOUND);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "UNKNOWN_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get a particular domain configuration
     */
    @RequestMapping(value = "/configuration/context/{id}", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get domain configuration")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> getContextConfiguration(
        @ApiParam(name = "id", value = "Id of domain (e.g. 'ROOT' or 'jmeter')",required = true)
        @PathVariable("id") String id,
        HttpServletRequest request, HttpServletResponse response) {

        try {
            authService.tryJWTLogin(request);
            rememberMeService.tryAutoLogin(request, response);

            if (!contextService.existsContext(id)) {
                return new ResponseEntity<>(new Error("No such domain", "NOT_FOUND"), HttpStatus.NOT_FOUND);
            }
            Context domain = contextService.getDomain(id);
            return new ResponseEntity<>(domain, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get public display information of a particular domain
     */
    @RequestMapping(value = "/configuration/domain/{id}/display-information", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get public display information of a domain")
    public ResponseEntity<?> getDomainDisplayInformation(
            @ApiParam(name = "id", value = "Id of domain (e.g. 'ROOT' or 'e4ff3246-372b-4042-a9e2-d30f612d1244')",required = true)
            @PathVariable("id") String id,
            HttpServletRequest request) {

        try {
            // TOOD: Check whether domain exists
            DomainDisplayInformation info = contextService.getDomainDisplayInformationt(id);
            return new ResponseEntity<>(info, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "UNKNOWN_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to update the hostname used inside mail/message body
     */
    @RequestMapping(value = "/configuration/domain/{id}/message-body-hostname", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Update the hostname used inside mail/message body")
    public ResponseEntity<?> updateMessageBodyHostname(
        @ApiParam(name = "id", value = "Id of domain (e.g. 'ROOT' or 'jmeter')",required = true)
        @PathVariable("id") String id,
        @ApiParam(name = "hostname", value = "Mail/message body hostname", required = true) @RequestBody Hostname hostname,
        HttpServletRequest request) {

        try {
            contextService.updateMailBodyHostname(id, hostname);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
        String body = "{\"domain_id\":\"" + id + "\"}";
        return new ResponseEntity<>(body, HttpStatus.OK);
    }
 
    /**
     * REST interface to create a new domain
     */
    @RequestMapping(value = "/configuration/domain", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Create a new domain")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> createDomain(
        @ApiParam(name = "name", value = "Name of domain (e.g. 'Wyona Research & Development')",required = true)
        @RequestParam("name") String name,
        @ApiParam(name = "mailSubjectTag", value = "E-Mail subject tag (e.g. 'Katie of Wyona')",required = true)
        @RequestParam("mailSubjectTag") String mailSubjectTag,
        @ApiParam(name = "answersGenerallyProtected", value = "When true, then answers are generally protected",required = true)
        @RequestParam(value = "answersGenerallyProtected", required = true) boolean answersGenerallyProtected,
        HttpServletRequest request) {

        Context domain = null;
        try {
            authService.tryJWTLogin(request);
            User user = authService.getUser(false, false);
            if (user != null && user.getRole() == Role.ADMIN) {
                // TODO: Check input parameters, for example domain name must not be empty
                if (name == null || name.trim().length() == 0) {
                    return new ResponseEntity<>(new Error("Domain name must not be empty", "DOMAIN_NAME_EMPTY"), HttpStatus.BAD_REQUEST);
                }
                domain = contextService.createDomain(answersGenerallyProtected, name, mailSubjectTag, false, user);
                String body = "{\"domain_id\":\"" + domain.getId() + "\"}";
                return new ResponseEntity<>(body, HttpStatus.OK);
            } else if (user != null && user.getRole() == Role.USER) {
                // TODO: Regular users can create one domain for free, but only one
                log.error("Access denied, because user with id '" + user.getId() + "' is not ADMIN!");
                return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            } else {
                log.error("Access denied!");
                return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "CREATE_DOMAIN_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to delete a particular domain
     */
    @RequestMapping(value = "/configuration/domain/{id}", method = RequestMethod.DELETE, produces = "application/json")
    @Operation(summary="Delete domain")
    public ResponseEntity<?> deleteDomain(
        @ApiParam(name = "id", value = "Id of domain (e.g. 'ROOT' or 'jmeter')",required = true)
        @PathVariable("id") String id,
        HttpServletRequest request) {

        try {
            // TOOD: Check whether domain exists
            contextService.deleteDomain(id);
            String body = "{\"domain_id\":\"" + id + "\"}";
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "UNAUTHORIZED"), HttpStatus.UNAUTHORIZED);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface that browsers supporting "Content-Security-Policy" and "Content-Security-Policy-Report-Only" can report CSP violations
     */
    @RequestMapping(value = "/configuration/csp-violation-report", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Process Content Security Policy violation report")
    public ResponseEntity<?> cspViolationReport(
            HttpServletRequest request) {

        StringBuilder report = new StringBuilder();
        try {
            BufferedReader bufferedReader = request.getReader();
            char[] charBuffer = new char[128];
            int bytesRead;
            while ((bytesRead = bufferedReader.read(charBuffer)) != -1) {
                report.append(charBuffer, 0, bytesRead);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        log.warn("CSP violation report received: " + report.toString());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
