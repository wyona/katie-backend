package com.wyona.katie.controllers.v1;

import com.wyona.katie.exceptions.UserAlreadyMemberException;
import com.wyona.katie.models.Error;
import com.wyona.katie.models.Username;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.*;
import com.wyona.katie.models.insights.DomainInsights;
import com.wyona.katie.models.insights.EventType;
import com.wyona.katie.models.insights.Interval;
import com.wyona.katie.models.insights.NgxChartsSeries;
import com.wyona.katie.services.*;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controller to access and manage a particular domain
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/domain") 
public class DomainController {

    @Autowired
    private ContextService domainService;

    @Autowired
    private ClassificationService classificationService;

    @Autowired
    private IAMService iamService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DataRepositoryService dataRepoService;

    @Autowired
    private ConnectorService connectorService;

    // TODO: Also see QuestionsController#getAskedQuestions(..)
    /**
     * Export all questions of a particular domain including labels whether questions were recognized successfully
     */
    @RequestMapping(value = "/{id}/export/dataset-questions", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Export all questions of a particular domain including labels whether questions were recognized successfully, which can be used for training a model")
    public ResponseEntity<?> exportQuestionsDataset(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String domainId,
            HttpServletRequest request) {

        if (!domainService.existsContext(domainId)) {
            return new ResponseEntity<>(new Error("Domain '" + domainId + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            User user = authenticationService.getUser(false, false);
            if (user == null) {
                throw new AccessDeniedException("User is not signed in!");
            }
            if (!domainService.isMemberOrAdmin(domainId)) {
                throw new AccessDeniedException("User '" + user.getUsername() + "' is neither member of domain '" + domainId + "' nor admin!");
            }

            Context domain = domainService.getContext(domainId);

            // TODO: Implement offset and limit
            AskedQuestion[] questions = dataRepoService.getQuestions(domainId, 10000, 0, false);
            List<TextSample> items = new ArrayList<>();
            if (questions != null & questions.length > 0) {
                // INFO: See for example https://github.com/handav/nlp-in-javascript-with-natural/blob/master/7-classify/example1.js
                for (AskedQuestion question : questions) {
                    // TODO: Set correct classification / label
                    Classification classification = new Classification("question", "0", null);
                    if (false) {
                        classification = new Classification("message", "1", null);
                    }

                    items.add(new TextSample(question.getUUID(), question.getQuestion(), classification));
                }
                return new ResponseEntity<>(items, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Error("No questions asked yet.", "NO_QUESTIONS"), HttpStatus.BAD_REQUEST);
            }
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Export all QnAs of a particular domain
     */
    @RequestMapping(value = "/{id}/export/qnas", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Export all QnAs of a particular domain")
    public ResponseEntity<?> exportQnAs(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            User user = authenticationService.getUser(false, false);
            if (user == null) {
                throw new AccessDeniedException("User is not signed in!");
            }
            if (!domainService.isMemberOrAdmin(id)) {
                throw new AccessDeniedException("User '" + user.getUsername() + "' is neither member of domain '" + id + "' nor admin!");
            }

            Context domain = domainService.getContext(id);

            // TODO: Implement offset and limit
            Answer[] answers = domainService.getTrainedQnAs(domain, 10000, 0);

            List<QnA> qnas = new ArrayList<QnA>();
            for (Answer answer: answers) {
                qnas.add(new QnA(answer));
            }

            return new ResponseEntity<>(qnas, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Import QnAs from JSON text into a particular domain
     */
    @RequestMapping(value = "/{id}/import/qnas-from-text", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Import QnAs from JSON text into a particular domain")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> importQnAsFromText(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "qnas", value = "An array of QnAs", required = true)
            @RequestBody QnA[] qnas,
            HttpServletRequest request) {

        try {
            authenticationService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            User user = authenticationService.getUser(false, false);
            if (user == null) {
                throw new AccessDeniedException("User is not signed in!");
            }
            if (!domainService.isMemberOrAdmin(id)) {
                throw new AccessDeniedException("User '" + user.getUsername() + "' is neither member of domain '" + id + "' nor admin!");
            }

            Context domain = domainService.getContext(id);

            String processId = UUID.randomUUID().toString();
            domainService.importQnAs(qnas, domain, processId, user.getId());

            return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Import QnAs from JSON file into a particular domain
     */
    @RequestMapping(value = "/{id}/import/qnas-from-file", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Import QnAs from JSON file into a particular domain")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> importQnAsFromFile(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {

        try {
            authenticationService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            User user = authenticationService.getUser(false, false);
            if (user == null) {
                throw new AccessDeniedException("User is not signed in!");
            }
            if (!domainService.isMemberOrAdmin(id)) {
                throw new AccessDeniedException("User '" + user.getUsername() + "' is neither member of domain '" + id + "' nor admin!");
            }

            Context domain = domainService.getContext(id);
            ObjectMapper mapper = new ObjectMapper();
            InputStream in = file.getInputStream();
            QnA[] qnas = mapper.readValue(in, QnA[].class);
            in.close();

            String processId = UUID.randomUUID().toString();
            domainService.importQnAs(qnas, domain, processId, user.getId());

            return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Import PDF
     */
    @RequestMapping(value = "/{id}/import/pdf", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Import PDF into a particular domain")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> importPDF(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {

        try {
            authenticationService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            User user = authenticationService.getUser(false, false);
            if (user == null) {
                throw new AccessDeniedException("User is not signed in!");
            }
            if (!domainService.isMemberOrAdmin(id)) {
                throw new AccessDeniedException("User '" + user.getUsername() + "' is neither member of domain '" + id + "' nor admin!");
            }

            Context domain = domainService.getContext(id);

            String processId = UUID.randomUUID().toString();
            domainService.importPDF(file.getInputStream(), domain, processId, user.getId());

            return new ResponseEntity<>("{\"bg-process-id\":\"" + processId + "\"}", HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to update the mail sender email address, e.g. "Katie <no-reply@wyona.com>"
     */
    @RequestMapping(value = "/{id}/mail-sender", method = RequestMethod.PATCH, produces = "application/json")
    @ApiOperation(value = "Update mail sender")
    public ResponseEntity<?> updateMailSender(
            @ApiParam(name = "id", value = "Domain Id (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("id") String domainid,
            @ApiParam(name = "mail-sender", value = "Updated mail sender (Parameter: mail-sender)", required = true) @RequestBody JsonNode body
    ) {
        try {
            log.info("JSON: " + body);
            String mailSender = body.get("mail-sender").asText();
            log.info("Update mail sender of domain '" + domainid + "': " + mailSender);
            domainService.updateMailSender(domainid, mailSender);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // TODO: Consider returning a body
            //return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to update the mail subject, e.g. replace "FAQ wyona" by "FAQ Wyona"
     */
    @RequestMapping(value = "/{id}/mail-subject", method = RequestMethod.PATCH, produces = "application/json")
    @ApiOperation(value = "Update mail subject")
    public ResponseEntity<?> updateMailSubject(
            @ApiParam(name = "id", value = "Domain Id (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("id") String domainid,
            @ApiParam(name = "mail-subject", value = "Updated mail subject (Parameter: mail-subject)", required = true) @RequestBody JsonNode body
    ) {
        try {
            log.info("JSON: " + body);
            String mailSubject = body.get("mail-subject").asText();
            log.info("Update mail subject of domain '" + domainid + "': " + mailSubject);
            domainService.updateMailSubject(domainid, mailSubject);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // TODO: Consider returning a body
            //return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to update the domain name, e.g. replace "FAQ wyona" by "FAQ Wyona"
     */
    @RequestMapping(value = "/{id}/name", method = RequestMethod.PATCH, produces = "application/json")
    @ApiOperation(value = "Update domain name")
    public ResponseEntity<?> updateDomainName(
            @ApiParam(name = "id", value = "Domain Id (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("id") String domainid,
            @ApiParam(name = "name", value = "Updated domain name, e.g. 'Apache Lucene'", required = true) @RequestBody DomainName name
    ) {
        try {
            log.info("Update name of domain '" + domainid + "': " + name.getName());
            domainService.updateDomainName(domainid, name.getName());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // TODO: Consider returning a body
            //return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to update the domain tag name, e.g. replace "apachelucene" by "apache-lucene"
     */
    @RequestMapping(value = "/{id}/tag-name", method = RequestMethod.PATCH, produces = "application/json")
    @ApiOperation(value = "Update domain tag name")
    public ResponseEntity<?> updateDomainTagName(
            @ApiParam(name = "id", value = "Domain Id (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("id") String domainid,
            @ApiParam(name = "tag-name", value = "Updated domain tag name, e.g. 'apache-lucene'", required = true)
            @RequestParam(value = "tag-name", required = true) String tagName
    ) {
        try {
            log.info("Update tag name of domain '" + domainid + "': " + tagName);
            domainService.updateTagName(domainid, tagName);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // TODO: Consider returning a body
            //return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get insights summary of a particular domain
     */
    @RequestMapping(value = "/{id}/insights/summary", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get insights summary of a particular domain")
    public ResponseEntity<?> getInsightsSummary(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "last-number-of-days", value = "Last number of days, e.g. last 30 days",required = false)
            @RequestParam(value = "last-number-of-days", required = false) Integer lastNumberOfDays,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
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

        try {
            int lastDays = getLastNumberOfDaysIntValue(lastNumberOfDays, id);
            DomainInsights insights = domainService.getInsightsSummary(id, lastDays);
            return new ResponseEntity<>(insights, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get insights history of a particular domain
     */
    @RequestMapping(value = "/{id}/insights/history", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get insights history of a particular domain")
    public ResponseEntity<?> getInsightsHistory(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "last-number-of-days", value = "Last number of days, e.g. last 30 days", required = false)
            @RequestParam(value = "last-number-of-days", required = false) Integer lastNumberOfDays,
            @ApiParam(name = "event-type", value = "Event type", required = true)
            @RequestParam(value = "event-type", required = true) EventType type,
            @ApiParam(name = "interval", value = "Interval", required = true)
            @RequestParam(value = "interval", required = true) Interval interval,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            int lastDays = getLastNumberOfDaysIntValue(lastNumberOfDays, id);
            log.info("Get insights series for the " + lastDays + " last days ...");
            NgxChartsSeries[] multiSeries = domainService.getInsightsHistory(id, lastDays, type, interval);
            return new ResponseEntity<>(multiSeries, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @param domainId Domain Id
     */
    private int getLastNumberOfDaysIntValue(Integer lastNumberOfDays, String domainId) {
        log.info("Last number of days provided: " + lastNumberOfDays);
        if (lastNumberOfDays == null) {
            return DefaultValues.MAX_LAST_NUMBER_OF_DAYS; // TODO: Use domain creation date
        } else {
            if (lastNumberOfDays.intValue() == DefaultValues.NO_LAST_NUMBER_OF_DAYS_PROVIDED) {
                return DefaultValues.MAX_LAST_NUMBER_OF_DAYS; // TODO: Use domain creation date
            } else {
                return lastNumberOfDays.intValue();
            }
        }
    }

    /**
     * Get insights re users of a particular domain
     */
    @RequestMapping(value = "/{id}/insights/users", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get insights re users of a particular domain")
    public ResponseEntity<?> getInsightsUsers(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            String[] emails = domainService.getInsightsUsers(id);
            return new ResponseEntity<>(emails, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add OneNote as knowledge source
     */
    @RequestMapping(value = "/{id}/knowledge-source/onenote", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Add OneNote as knowledge source")
    public ResponseEntity<?> addKnowledgeSourceOneNote(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "name", value = "Knowledge source name, e.g. 'Katie Documentation'",required = true)
            @RequestParam(value = "name", required = true) String name,
            @ApiParam(name = "apiToken", value = "MS Graph API Token", required = false)
            @RequestParam(value = "apiToken", required = false) String apiToken,
            @ApiParam(name = "tenant", value = "Tenant", required = false)
            @RequestParam(value = "tenant", required = false) String tenant,
            @ApiParam(name = "client-id", value = "Client Id", required = false)
            @RequestParam(value = "client-id", required = false) String clientId,
            @ApiParam(name = "client-secret", value = "Client secret", required = false)
            @RequestParam(value = "client-secret", required = false) String clientSecret,
            @ApiParam(name = "scope", value = "Scope, e.g. 'Notes.Read.All'", required = false)
            @RequestParam(value = "scope", required = false) String scope,
            @ApiParam(name = "location", value = "Location, e.g. 'groups/c5a3125f-f85a-472a-8561-db2cf74396ea' or 'users/michael.wechner@wyona.com'", required = false)
            @RequestParam(value = "location", required = false) String location,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            domainService.addKnowledgeSourceOneNote(id, name, apiToken, tenant, clientId, clientSecret, scope, location);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add SharePoint as knowledge source
     */
    @RequestMapping(value = "/{id}/knowledge-source/sharepoint", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Add SharePoint as knowledge source, whereas see https://app.katie.qa/connectors/sharepoint-connector.html")
    public ResponseEntity<?> addKnowledgeSourceSharePoint(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "name", value = "Knowledge source name, e.g. 'Katie SharePoint'",required = true)
            @RequestParam(value = "name", required = true) String name,
            @ApiParam(name = "apiToken", value = "MS Graph API Token", required = false)
            @RequestParam(value = "apiToken", required = false) String apiToken,
            @ApiParam(name = "tenant", value = "Tenant Id, whereas see https://portal.azure.com/#settings/directory", required = true)
            @RequestParam(value = "tenant", required = true) String tenant,
            @ApiParam(name = "client-id", value = "Client Id, whereas see Microsoft Entra ID / Applications, https://portal.azure.com/#view/Microsoft_AAD_IAM/StartboardApplicationsMenuBlade/~/AppAppsPreview/menuId~/null", required = true)
            @RequestParam(value = "client-id", required = true) String clientId,
            @ApiParam(name = "client-secret", value = "Client secret", required = true)
            @RequestParam(value = "client-secret", required = true) String clientSecret,
            @ApiParam(name = "scope", value = "Scope, e.g. 'Sites.Read.All', whereas make sure to enable permissions for client / app id accordingly", required = true)
            @RequestParam(value = "scope", required = true) String scope,
            @ApiParam(name = "site-id", value = "SharePoint site Id, e.g. 'ee403fc5-vfd8-7fhe-9171-k11dw4db239c', whereas get Site Id using Microsoft Graph, e.g. https://graph.microsoft.com/v1.0/sites?search=KatieTest or https://graph.microsoft.com/v1.0/sites/wyona.sharepoint.com:/sites/KatieTest?$select=id", required = true)
            @RequestParam(value = "site-id", required = true) String siteId,
            @ApiParam(name = "base-url", value = "SharePoint base URL, e.g. https://wyona.sharepoint.com (ROOT site) or https://wyona.sharepoint.com/sites/KatieTest", required = true)
            @RequestParam(value = "base-url", required = true) String baseUrl,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            domainService.addKnowledgeSourceSharePoint(id, name, apiToken, tenant, clientId, clientSecret, scope, siteId, baseUrl);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add Website (pages) as knowledge source
     */
    @RequestMapping(value = "/{id}/knowledge-source/website", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Add Website (pages) as knowledge source")
    public ResponseEntity<?> addKnowledgeSourceWebsite(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "name", value = "Knowledge source name, e.g. 'Katie Documentation'",required = true)
            @RequestParam(value = "name", required = true) String name,
            @ApiParam(name = "seed-url", value = "Seed URL, e.g. https://wyona.com", required = false)
            @RequestParam(value = "seed-url", required = false) String seedUrl,
            @ApiParam(name = "urls", value = "Comma separated list of page URLs, e.g. https://wyona.com/about,https://wyona.com/contact", required = false)
            @RequestParam(value = "urls", required = false) String pageURLs,
            @ApiParam(name = "chunk-size", value = "Chunk size, e.g. 1500", required = false)
            @RequestParam(value = "chunk-size", required = false) Integer chunkSize,
            @ApiParam(name = "chunk-overlap", value = "Chunk overlap, e.g. 30", required = false)
            @RequestParam(value = "chunk-overlap", required = false) Integer chunkOverlap,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            List<String> urls = new ArrayList<String>();
            if (pageURLs != null) {
                String[] _urls = pageURLs.split(",");
                for (String url : _urls) {
                    urls.add(url.trim());
                }
            }
            domainService.addKnowledgeSourceWebsite(id, name, seedUrl, urls.toArray(new String[0]), chunkSize, chunkOverlap);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add third-party RAG as knowledge source
     */
    @RequestMapping(value = "/{id}/knowledge-source/third-party-rag", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Add third-party RAG as knowledge source")
    public ResponseEntity<?> addKnowledgeSourceThirdPartyRAG(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "name", value = "Knowledge source name, e.g. 'Law Assistant RAG'",required = true)
            @RequestParam(value = "name", required = true) String name,
            @ApiParam(name = "endpoint-url", value = "Endpoint URL, e.g. http://0.0.0.0:8000/chat", required = true)
            @RequestParam(value = "endpoint-url", required = true) String endpointUrl,
            @ApiParam(name = "response-json-pointer", value = "Response / Answer JSON pointer, e.g. '/data/content' or '/response/docs/0/content_txt'", required = true)
            @RequestParam(value = "response-json-pointer", required = true) String responseJsonPointer,
            @ApiParam(name = "reference-json-pointer", value = "Reference / Source JSON pointer, e.g. '/data/reasoning_thread/0/result/art_para/0' or '/response/docs/0/id'", required = false)
            @RequestParam(value = "reference-json-pointer", required = false) String referenceJsonPointer,
            @ApiParam(name = "endpoint-payload", value = "Payload sent to endpoint, e.g. {\"message\":[{\"content\":\"{{QUESTION}}\",\"role\":\"user\"}],\"stream\":false} or {\"query\" : \"{{QUESTION}}\"}", required = true)
            @RequestBody String payload,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            domainService.addKnowledgeSourceThirdPartyRAG(id, name, endpointUrl, payload, responseJsonPointer, referenceJsonPointer);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add Supabase as knowledge source
     */
    @RequestMapping(value = "/{id}/knowledge-source/supabase", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Add Supabase as knowledge source")
    public ResponseEntity<?> addKnowledgeSourceSupabase(
            @ApiParam(name = "id", value = "Domain Id", required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "name", value = "Knowledge source name, e.g. 'My Supabase'", required = true)
            @RequestParam(value = "name", required = true) String name,
            @ApiParam(name = "answer-field-names", value = "Comma separated list of field names, e.g. 'abstract, text'", required = true)
            @RequestParam(value = "answer-field-names", required = true) String answerFieldNames,
            @ApiParam(name = "classifications-field-names", value = "Comma separated list of field names, e.g. 'tags, custom_tags, coauthors'", required = true)
            @RequestParam(value = "classifications-field-names", required = true) String classificationsFieldNames,
            @ApiParam(name = "question-field-names", value = "Comma separated list of field names, e.g. 'titel'", required = true)
            @RequestParam(value = "question-field-names", required = true) String questionFieldNames,
            @ApiParam(name = "url", value = "Base URL, e.g. 'https://repid.ch'", required = true)
            @RequestParam(value = "url", required = true) String url,
            @ApiParam(name = "chunk-size", value = "Chunk size, e.g. 1000", required = false)
            @RequestParam(value = "chunk-size", required = false) Integer chunkSize,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            domainService.addKnowledgeSourceSupabase(id, name, answerFieldNames, classificationsFieldNames, questionFieldNames, url, chunkSize);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete a particular knowledge source
     */
    @RequestMapping(value = "/{id}/knowledge-source/{ks-id}", method = RequestMethod.DELETE, produces = "application/json")
    @ApiOperation(value="Delete a particular knowledge source.")
    public ResponseEntity<?> deleteKnowledgeSource(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "ks-id", value = "Knowledge source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            domainService.deleteKnowledgeSource(id, ksId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Trigger a particular Directus based knowledge source by a webhook
     */
    @RequestMapping(value = "/{id}/knowledge-source/{ks-id}/invoke-by-directus", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Trigger a particular Directus based knowledge source by a webhook")
    public ResponseEntity<?> triggerKnowledgeSourceDirectus(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "ks-id", value = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @ApiParam(name = "webhook-payload", value = "Webhook payload sent by Directus", required = true)
            @RequestBody WebhookPayloadDirectus payload,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        // TODO: Check security token

        try {
            // TODO: connectorService.triggerKnowledgeSourceConnectorInBackground(KnowledgeSourceConnector.DIRECTUS, id, ksId, payload, processId, userId);
            connectorService.triggerKnowledgeSourceDirectus(id, ksId, payload);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Trigger a particular Supabase based knowledge source by a webhook
     */
    @RequestMapping(value = "/{id}/knowledge-source/{ks-id}/invoke-by-supabase", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Trigger a particular Supabase based knowledge source by a webhook")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> triggerKnowledgeSourceSupabase(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "ks-id", value = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @ApiParam(name = "webhook-payload", value = "Webhook payload sent by Supabase", required = true)
            @RequestBody WebhookPayloadSupabase payload,
            HttpServletRequest request) {

        try {
            authenticationService.tryJWTLogin(request);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }


        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }
        try {
            // INFO: Check whether user is authorized
            domainService.getDomain(id);
        } catch (AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String processId = UUID.randomUUID().toString();
        String userId = authenticationService.getUserId();
        connectorService.triggerKnowledgeSourceConnectorInBackground(KnowledgeSourceConnector.SUPABASE, id, ksId, payload, processId, userId);

        return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
    }

    /**
     * Trigger a particular TOPdesk based knowledge source by a webhook
     */
    @RequestMapping(value = "/{id}/knowledge-source/{ks-id}/invoke-by-topdesk", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Trigger a particular TOPdesk based knowledge source by a webhook")
    public ResponseEntity<?> triggerKnowledgeSourceTOPdesk(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "ks-id", value = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @ApiParam(name = "webhook-payload", value = "Webhook payload sent by TOPdesk", required = true)
            @RequestBody WebhookPayloadTOPdesk payload,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        // TODO: Check security token

        String processId = UUID.randomUUID().toString();
        String userId = authenticationService.getUserId();
        connectorService.triggerKnowledgeSourceConnectorInBackground(KnowledgeSourceConnector.TOP_DESK, id, ksId, payload, processId, userId);

        return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
    }

    /**
     * Trigger a particular Discourse based knowledge source by a webhook
     */
    @RequestMapping(value = "/{id}/knowledge-source/{ks-id}/invoke-by-discourse", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Trigger a particular Supabase based knowledge source by a webhook")
    public ResponseEntity<?> triggerKnowledgeSourceDiscourse(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "ks-id", value = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @ApiParam(name = "webhook-payload", value = "Webhook payload sent by Discourse", required = true)
            @RequestBody WebhookPayloadDiscourse payload,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        // TODO: Check security token

        String processId = UUID.randomUUID().toString();
        String userId = authenticationService.getUserId();
        connectorService.triggerKnowledgeSourceConnectorInBackground(KnowledgeSourceConnector.DISCOURSE, id, ksId, payload, processId, userId);

        return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
    }

    /**
     * Trigger a particular Confluence based knowledge source by a webhook
     * https://developer.atlassian.com/cloud/confluence/modules/webhook/
     * https://jira.atlassian.com/browse/CONFCLOUD-36613
     * Get configured Webhooks: GET https://wyona.atlassian.net/wiki/rest/webhooks/1.0/webhook
     */
    @RequestMapping(value = "/{id}/knowledge-source/{ks-id}/invoke-by-confluence", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Trigger a particular Confluence based knowledge source by a webhook")
    public ResponseEntity<?> triggerKnowledgeSourceConfluence(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "ks-id", value = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @ApiParam(name = "webhook-payload", value = "Webhook payload sent by Confluence", required = true)
            @RequestBody WebhookPayloadConfluence payload,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        // TODO: Check security token

        try {
            // TODO: connectorService.triggerKnowledgeSourceConnectorInBackground(KnowledgeSourceConnector.CONFLUENCE, id, ksId, payload, processId, userId);
            connectorService.triggerKnowledgeSourceConfluence(id, ksId, payload);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Trigger a particular Website based knowledge source by a webhook
     */
    @RequestMapping(value = "/{id}/knowledge-source/{ks-id}/invoke-by-website", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Trigger a particular Website based knowledge source by a webhook")
    public ResponseEntity<?> triggerKnowledgeSourceWebsite(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "ks-id", value = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @ApiParam(name = "webhook-payload", value = "Webhook payload sent by Website", required = true)
            @RequestBody WebhookPayloadWebsite payload,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        if (!domainService.isMemberOrAdmin(id)) {
            String msg = "User is neither member of domain '" + id + "', nor has role " + Role.ADMIN + "!";
            log.warn(msg);
            return new ResponseEntity<>(new Error(msg, "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }

        // TODO: Check security token

        String processId = UUID.randomUUID().toString();
        String userId = authenticationService.getUserId();
        connectorService.triggerKnowledgeSourceConnectorInBackground(KnowledgeSourceConnector.WEBSITE, id, ksId, payload, processId, userId);
        return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
    }

    /**
     * Trigger a particular Outlook based knowledge source by a webhook
     * https://learn.microsoft.com/en-us/graph/change-notifications-delivery-webhooks?tabs=http#receive-notifications
     */
    @RequestMapping(value = "/{id}/knowledge-source/{ks-id}/invoke-by-outlook", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Trigger a particular Outlook based knowledge source by a webhook")
    public ResponseEntity<?> triggerKnowledgeSourceOutlook(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "ks-id", value = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @ApiParam(name = "webhook-payload", value = "Webhook payload sent by Outlook", required = true)
            @RequestBody WebhookPayload payload,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        if (!domainService.isMemberOrAdmin(id)) {
            String msg = "User is neither member of domain '" + id + "', nor has role " + Role.ADMIN + "!";
            log.warn(msg);
            return new ResponseEntity<>(new Error(msg, "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }

        // TODO: Check security token

        String processId = UUID.randomUUID().toString();
        String userId = authenticationService.getUserId();
        connectorService.triggerKnowledgeSourceConnectorInBackground(KnowledgeSourceConnector.OUTLOOK, id, ksId, payload, processId, userId);

        return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
    }

    /**
     * Trigger a particular OneNote based knowledge source by a webhook
     * https://learn.microsoft.com/en-us/graph/change-notifications-delivery-webhooks?tabs=http#receive-notifications
     */
    @RequestMapping(value = "/{id}/knowledge-source/{ks-id}/invoke-by-onenote", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Trigger a particular OneNote based knowledge source by a webhook")
    public ResponseEntity<?> triggerKnowledgeSourceOneNote(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "ks-id", value = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @ApiParam(name = "webhook-payload", value = "Webhook payload sent by OneNote", required = true)
            @RequestBody WebhookPayloadOneNote payload,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        if (!domainService.isMemberOrAdmin(id)) {
            String msg = "User is neither member of domain '" + id + "', nor has role " + Role.ADMIN + "!";
            log.warn(msg);
            return new ResponseEntity<>(new Error(msg, "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }

        // TODO: Check security token

        String processId = UUID.randomUUID().toString();
        String userId = authenticationService.getUserId();
        connectorService.triggerKnowledgeSourceConnectorInBackground(KnowledgeSourceConnector.ONENOTE, id, ksId, payload, processId, userId);

        return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
    }

    /**
     * Trigger a particular Sharepoint based knowledge source by a webhook
     * https://learn.microsoft.com/en-us/graph/change-notifications-delivery-webhooks?tabs=http#receive-notifications
     */
    @RequestMapping(value = "/{id}/knowledge-source/{ks-id}/invoke-by-sharepoint", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Trigger a particular Sharepoint based knowledge source by a webhook")
    public ResponseEntity<?> triggerKnowledgeSourceSharepoint(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "ks-id", value = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @ApiParam(name = "webhook-payload", value = "Webhook payload sent by Sharepoint", required = true)
            @RequestBody WebhookPayload payload,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        if (!domainService.isMemberOrAdmin(id)) {
            String msg = "User is neither member of domain '" + id + "', nor has role " + Role.ADMIN + "!";
            log.warn(msg);
            return new ResponseEntity<>(new Error(msg, "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }

        // TODO: Check security token

        String processId = UUID.randomUUID().toString();
        String userId = authenticationService.getUserId();
        connectorService.triggerKnowledgeSourceConnectorInBackground(KnowledgeSourceConnector.SHAREPOINT, id, ksId, payload, processId, userId);

        return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
    }

    /**
     * Get knowledge sources of a particular domain
     */
    @RequestMapping(value = "/{id}/knowledge-sources", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get knowledge sources of a particular domain")
    public ResponseEntity<?> getKnowledgeSources(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            KnowledgeSourceMeta[] knowledgeSources = domainService.getKnowledgeSources(id, true);
            return new ResponseEntity<>(knowledgeSources, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Import Classification dataset from JSON file into a particular domain
     */
    @RequestMapping(value = "/{id}/classification/import-dataset", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Import classification dataset from JSON file into a particular domain")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> importClassificationDatasetFromFile(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {

        try {
            authenticationService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            User user = authenticationService.getUser(false, false);
            if (user == null) {
                throw new AccessDeniedException("User is not signed in!");
            }
            if (!domainService.isMemberOrAdmin(id)) {
                throw new AccessDeniedException("User '" + user.getUsername() + "' is neither member of domain '" + id + "' nor admin!");
            }

            ObjectMapper mapper = new ObjectMapper();
            InputStream in = file.getInputStream();
            ClassificationDataset dataset = mapper.readValue(in, ClassificationDataset.class);
            in.close();

            Context domain = domainService.getContext(id);
            String processId = UUID.randomUUID().toString();
            domainService.importClassificationDataset(dataset, domain, processId, user.getId());

            return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get classification dataset of a particular domain
     */
    @RequestMapping(value = "/{id}/classification/dataset", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value="Get classification dataset")
    public ResponseEntity<?> getClassificationDataset(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "labels-only", value = "When set to true, then only labels are returned", required = false, defaultValue = "false")
            @RequestParam(value = "labels-only", required = false) Boolean labelsOnly,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        boolean _labelsOnly = false;
        if (labelsOnly != null) {
            _labelsOnly = labelsOnly;
        }

        try {
            Context domain = domainService.getDomain(id); // INFO: Checks authorization

            // TODO: Implement offset and limit
            int offset = 0;
            int limit = 10000;
            ClassificationDataset dataset = classificationService.getDataset(domain, _labelsOnly, offset, limit);
            return new ResponseEntity<>(dataset, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get classification labels of a particular domain
     */
    @RequestMapping(value = "/{id}/classification/labels", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary="Get classification labels")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> getClassificationLabels(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

        if (!domainService.isAuthorized(id, request, "/" + id + "/classification/labels", JwtService.SCOPE_READ_LABELS)) {
            log.warn("Not authorized to get classification labels");
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            Context domain = domainService.getContext(id); // INFO: Does not check authorization

            // TODO: Implement offset and limit
            int offset = 0;
            int limit = 10000;
            ClassificationDataset dataset = classificationService.getDataset(domain, true, offset, limit);
            return new ResponseEntity<>(dataset.getLabels(), HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrain classifier
     */
    @RequestMapping(value = "/{id}/classification/retrain", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value="Retrain classifier")
    public ResponseEntity<?> retrainClassifier(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            Context domain = domainService.getDomain(id);
            String bgProcessId = UUID.randomUUID().toString();
            String userId = authenticationService.getUserId();
            classificationService.retrain(domain, 80, bgProcessId, userId);
            String responseBody = "{\"bg-process-id\":\"" + bgProcessId + "\"}";
            return new ResponseEntity<>(responseBody, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get taxonomy entries of a particular domain
     */
    @RequestMapping(value = "/{id}/taxonomy/entries", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get taxonomy entries")
    public ResponseEntity<?> getTaxonomyEntries(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

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

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            // TODO: Implement offset and limit
            String[] entries = domainService.getTaxonomyEntries(id, 0, 10000);
            return new ResponseEntity<>(entries, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add multiple taxonomy entries
     */
    @RequestMapping(value = "/{id}/taxonomy/entries", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Add multiple taxomnomy entries, e.g. birthdate, birthplace")
    public ResponseEntity<?> addTaxonomyEntries(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "entries", value = "Array of entries", required = true)
            @RequestBody String[] entries,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            domainService.addTaxonomyEntries(id, entries);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get autocompletion entries of a particular domain
     */
    @RequestMapping(value = "/{id}/autocompletion/entries", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get autocompletion entries")
    public ResponseEntity<?> getAutocompletionEntries(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

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

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            // TODO: Implement offset and limit
            String[] entries = domainService.getAutocompletionEntries(id, 0, 10000);
            return new ResponseEntity<>(entries, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add multiple autocompletion entries
     */
    @RequestMapping(value = "/{id}/autocompletion/entries", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Add multiple autocompletion entries / questions")
    public ResponseEntity<?> addAutocompletionEntries(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "entries", value = "Array of entries", required = true)
            @RequestBody String[] entries,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            domainService.addAutocompletionEntries(id, entries);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add single autocompletion entry
     */
    @RequestMapping(value = "/{id}/autocompletion/entry", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Add single autocompletion entry / question")
    public ResponseEntity<?> addAutocompletionEntry(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "entry", value = "Entry value, e.g. 'mountain' or question, e.g. 'What's the weather expected to be tomorrow?'", required = true)
            @RequestBody Question entry,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            domainService.addAutocompletionEntry(id, entry.getQuestion());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete autocompletion entry
     */
    @RequestMapping(value = "/{id}/autocompletion/entry", method = RequestMethod.DELETE, produces = "application/json")
    @ApiOperation(value="Delete autocompletion entry")
    public ResponseEntity<?> deleteAutocompletionEntry(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "value", value = "Entry value, e.g. 'speed of light'",required = true)
            @RequestParam(value = "value", required = true) String value,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            domainService.deleteAutocompletionEntry(id, value);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get URLs of webpages which contain imported QnAs
     */
    @RequestMapping(value = "/{id}/third-party-urls", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get URLs of webpages which contain imported QnAs")
    public ResponseEntity<?> getThirdPartyUrls(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            String[] urls = domainService.getThirdPartyUrls(id);
            return new ResponseEntity<>(urls, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get webhooks of a particular domain
     */
    @RequestMapping(value = "/{id}/webhooks", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get webhooks of a particular domain")
    public ResponseEntity<?> getWebhooks(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            Webhook[] webhooks = domainService.getWebhooks(id);
            return new ResponseEntity<>(webhooks, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get deliveries of a particular webhook
     */
    @RequestMapping(value = "/{id}/webhooks/{webhook-id}/deliveries", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get deliveries of a particular webhook")
    public ResponseEntity<?> getWebhookDeliveries(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "webhook-id", value = "Webhook Id",required = true)
            @PathVariable(value = "webhook-id", required = true) String webhookId,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            WebhookRequest[] webhookRequests = domainService.getWebhookDeliveries(id, webhookId);
            return new ResponseEntity<>(webhookRequests, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete a particular webhook
     */
    @RequestMapping(value = "/{id}/webhooks/{webhook-id}", method = RequestMethod.DELETE, produces = "application/json")
    @ApiOperation(value="Delete a particular webhook")
    public ResponseEntity<?> deleteWebhook(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "webhook-id", value = "Webhook Id",required = true)
            @PathVariable(value = "webhook-id", required = true) String webhookId,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            domainService.deleteWebhook(id, webhookId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add webhook to a particular domain
     */
    @RequestMapping(value = "/{id}/webhook", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Add webhook to a particular domain")
    public ResponseEntity<?> addWebhook(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "webhook", value = "Webhook, whereas only payloadURL required, e.g. 'https://postman-echo.com/post'", required = true)
            @RequestBody Webhook webhook,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            webhook = domainService.addWebhook(id, webhook);
            return new ResponseEntity<>(webhook, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Reindex all QnAs of a particular domain
     */
    @RequestMapping(value = "/{id}/reindex", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary = "Reindex all QnAs of a particular domain")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> reindex(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "impl", value = "Detect duplicated question implementation",required = true)
            @RequestParam(value = "impl", required = true) DetectDuplicatedQuestionImpl searchImpl,
            @ApiParam(name = "query-service-url", value = "Query service base URL (e.g. http://localhost:8383/api/v2) or Azure AI Search endpoint (e.g. https://katie.search.windows.net)", required = false)
            @RequestParam(value = "query-service-url", required = false) String queryServiceBaseUrl,
            @ApiParam(name = "query-service-token", value = "Query service Token / Key / Secret", required = false)
            @RequestParam(value = "query-service-token", required = false) String queryServiceToken,
            @ApiParam(name = "embedding-impl", value = "Embedding implementation",required = false)
            @RequestParam(value = "embedding-impl", required = false) EmbeddingsImpl embeddingImpl,
            @ApiParam(name = "embedding-model", value = "Embedding model, e.g. all-mpnet-base-v2 or text-embedding-3-small",required = false)
            @RequestParam(value = "embedding-model", required = false) String embeddingModel,
            @ApiParam(name = "embedding-value-type", value = "Embedding value type",required = false)
            @RequestParam(value = "embedding-value-type", required = false) EmbeddingValueType embeddingValueType,
            @ApiParam(name = "embedding-endpoint", value = "OpenAI compatible embedding endpoint, e.g. https://api.mistral.ai/v1/embeddings", required = false)
            @RequestParam(value = "embedding-endpoint", required = false) String embeddingEndpoint,
            @ApiParam(name = "api-token", value = "Embedding implementation API token",required = false)
            @RequestParam(value = "api-token", required = false) String apiToken,
            @ApiParam(name = "index-alternative-questions", value = "Default is true, but when set to false, then alternative questions will not be indexed",required = false)
            @RequestParam(value = "index-alternative-questions", required = false) Boolean indexAlternativeQuestions,
            @ApiParam(name = "index-all-qnas", value = "Default is false, but when set to true, then also index QnAs which were not indexed yet",required = false)
            @RequestParam(value = "index-all-qnas", required = false) Boolean indexAllQnAs,
            @ApiParam(name = "throttle-time", value = "Throttle time in milliseconds",required = false)
            @RequestParam(value = "throttle-time", required = false) Integer customThrottleTimeInMilis,
            HttpServletRequest request) {

        try {
            authenticationService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        if (!domainService.isMemberOrAdmin(id)) {
            String msg = "User is neither member of domain '" + id + "', nor has role " + Role.ADMIN + "!";
            log.warn(msg);
            return new ResponseEntity<>(new Error(msg, "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }

        int throttleTimeInMillis = -1; // INFO: No throttling
        if (customThrottleTimeInMilis != null && customThrottleTimeInMilis > 0) {
            throttleTimeInMillis = customThrottleTimeInMilis;
        }

        boolean _indexAlternativeQuestions = true;
        if (indexAlternativeQuestions != null) {
            _indexAlternativeQuestions = indexAlternativeQuestions.booleanValue();
        }

        boolean _indexAllQnAs = false;
        if (indexAllQnAs != null) {
            _indexAllQnAs = indexAllQnAs.booleanValue();
        }

        if (searchImpl.equals(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH)) {
            if (embeddingImpl == null) {
                embeddingImpl = EmbeddingsImpl.SBERT;
            } else {
                if (apiToken == null && !embeddingImpl.equals(EmbeddingsImpl.SBERT)) {
                    log.error("For Embeddings Implementation '" + embeddingImpl + "' API Token is required!");
                    return new ResponseEntity<>(new Error("For Embeddings Implementation '" + embeddingImpl + "' API Token is required!", "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
                }
            }

            if (embeddingValueType == null) {
                embeddingValueType = EmbeddingValueType.float32;
            }
            if (embeddingValueType == EmbeddingValueType.int8 && embeddingImpl != EmbeddingsImpl.COHERE) {
                return new ResponseEntity<>(new Error("Currently only Cohere provides embeddings with value type int8 / byte", "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
            }
        }

        try {
            if (domainService.existsReindexLock(id)) {
                String existingProcessId = domainService.getReindexProcessId(id);
                throw new Exception("Reindexing of domain '" + id + "' already in progress (Process Id: " + existingProcessId + "), therefore no other reindex process will be started.");
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String processId = UUID.randomUUID().toString();
        String userId = authenticationService.getUserId();

        domainService.reindexInBackground(id, searchImpl, queryServiceBaseUrl, queryServiceToken, embeddingImpl, embeddingModel, embeddingValueType, embeddingEndpoint, apiToken,_indexAlternativeQuestions, _indexAllQnAs, processId, userId, throttleTimeInMillis);

        /* INFO: Delay response to test frontend spinner
        try {
            for (int i = 0; i < 5; i++) {
                log.info("Sleep for 2 seconds ...");
                Thread.sleep(2000);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
         */

        return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
    }

    /**
     * Invite user by email to a particular domain
     */
    @RequestMapping(value = "/{id}/invite-user", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Invite user by email to a particular domain")
    public ResponseEntity<?> inviteUser(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "email", value = "Email of invited user",required = true)
            @RequestParam(value = "email", required = true) String email,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            String username = email;
            User invitedUser = domainService.inviteUserToBecomeMemberOfDomain(username, id, email);

            StringBuilder body = new StringBuilder("{");
            body.append("\"domain-id\":\"" + id + "\"");
            if (invitedUser != null) {
                body.append(",\"username\":\"" + invitedUser.getUsername() + "\"");
                body.append(",\"registration-required\":\"false\"");
            } else {
                body.append(",\"registration-required\":\"true\"");
            }
            body.append("}");

            return new ResponseEntity<>(body.toString(), HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(UserAlreadyMemberException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "USER_ALREADY_MEMBER"), HttpStatus.BAD_REQUEST);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add user by username to a particular domain
     */
    @RequestMapping(value = "/{id}/add-user", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Add user by username to a particular domain")
    public ResponseEntity<?> addUser(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "username", value = "Username",required = true)
            @RequestParam(value = "username", required = true) String username,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            User addedUser = domainService.inviteUserToBecomeMemberOfDomain(username, id, null);

            if (addedUser != null) {
                StringBuilder body = new StringBuilder("{");
                body.append("\"domain-id\":\"" + id + "\"");
                body.append("}");

                return new ResponseEntity<>(body.toString(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Error("User '" + username + "' has not been added as member", "USER_NOT_ADDED_AS_MEMBER"), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(UserAlreadyMemberException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "USER_ALREADY_MEMBER"), HttpStatus.BAD_REQUEST);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Remove user as member from a particular domain
     */
    @RequestMapping(value = "/{id}/remove-user", method = RequestMethod.DELETE, produces = "application/json")
    @ApiOperation(value="Remove user as member from a particular domain")
    public ResponseEntity<?> removeUser(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "username", value = "Username of domain member",required = true)
            @RequestParam(value = "username", required = true) String username,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            String userId = iamService.getUserByUsername(new com.wyona.katie.models.Username(username), false, false).getId();
            domainService.removeMember(id, userId, true);

            StringBuilder body = new StringBuilder("{}");

            return new ResponseEntity<>(body.toString(), HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all members of a particular domain
     */
    @RequestMapping(value = "/{id}/users", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get all members of a particular domain")
    public ResponseEntity<?> getMembers(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "property", value = "User domain property (EXPERT or MODERATOR)", required = false)
            @RequestParam(value = "property", required = false) DomainProperty property,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            if (property != null) {
                if (property.equals(DomainProperty.EXPERT)) {
                    return new ResponseEntity<>(domainService.getExperts(id, true), HttpStatus.OK);
                } else if (property.equals(DomainProperty.MODERATOR)) {
                    return new ResponseEntity<>(domainService.getModerators(id, true), HttpStatus.OK);
                } else {
                    String errorMsg = "No such domain property '" + property + "' implemented!";
                    log.error(errorMsg);
                    return new ResponseEntity<>(new Error(errorMsg, "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
                }
            } else {
                // TODO: Provide role as optional request parameter
                RoleDomain role = null; // RoleDomain.OWNER;
                return new ResponseEntity<>(domainService.getMembers(id, true, role), HttpStatus.OK);
            }
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update IMAP configuration
     */
    @RequestMapping(value = "/{id}/imap-configuration", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Update IMAP configuration")
    public ResponseEntity<?> updateIMAPConfiguration(
            @ApiParam(name = "id", value = "Domain Id", required = true)
            @PathVariable(value = "id", required = true) String domainId,
            @ApiParam(name = "imap_config", value = "IMAP configuration", required = true)
            @RequestBody IMAPConfiguration imapConfiguration,
            HttpServletRequest request) {

        try {
            domainService.updateIMAPConfiguration(domainId, imapConfiguration);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // TODO: Consider returning a body
            //return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update match reply-to email address
     */
    @RequestMapping(value = "/{id}/match-reply-to-email", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Update match reply-to email address")
    public ResponseEntity<?> updateMatchReplyToEmailAddress(
            @ApiParam(name = "id", value = "Domain Id", required = true)
            @PathVariable(value = "id", required = true) String domainId,
            @ApiParam(name = "emails", value = "Match reply-to emails", required = true)
            @RequestBody MatchReplyToEmails emails,
            HttpServletRequest request) {

        try {
            domainService.updateMatchReplyToEmails(domainId, emails);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // TODO: Consider returning a body
            //return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Toggle whether a particular user is a moderator
     */
    @RequestMapping(value = "/{id}/user/{username}/moderator", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Toggle whether a particular user is a moderator")
    public ResponseEntity<?> toggleModerator(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String domainId,
            @ApiParam(name = "username", value = "Username",required = true)
            @PathVariable(value = "username", required = true) String userName,
            HttpServletRequest request) {

        if (!domainService.existsContext(domainId)) {
            return new ResponseEntity<>(new Error("Domain '" + domainId + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            User user = iamService.getUserByUsername(new com.wyona.katie.models.Username(userName), false, false);
            if (user != null) {
                if (domainService.isUserMemberOfDomain(user.getId(), domainId)) {
                    user = domainService.toggleUserIsModerator(domainId, user);
                    String body = "{\"moderator\":\"" + user.getIsModerator() + "\"}";
                    return new ResponseEntity<>(body, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new Error("User '" + userName + "' is not member of domain '" + domainId + "'!", "USER_NOT_MEMBER_OF_DOMAIN"), HttpStatus.NOT_FOUND);
                }
            } else {
                return new ResponseEntity<>(new Error("User '" + userName + "' does not exist!", "NO_SUCH_USER"), HttpStatus.NOT_FOUND);
            }

        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Toggle whether a particular user is an expert
     */
    @RequestMapping(value = "/{id}/user/{username}/expert", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Toggle whether a particular user is an expert")
    public ResponseEntity<?> toggleExpert(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String domainId,
            @ApiParam(name = "username", value = "Username",required = true)
            @PathVariable(value = "username", required = true) String userName,
            HttpServletRequest request) {

        if (!domainService.existsContext(domainId)) {
            return new ResponseEntity<>(new Error("Domain '" + domainId + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            User user = iamService.getUserByUsername(new Username(userName), false, false);
            if (user != null) {
                if (domainService.isUserMemberOfDomain(user.getId(), domainId)) {
                    user = domainService.toggleUserIsExpert(domainId, user);
                    String body = "{\"expert\":\"" + user.getIsExpert() + "\"}";
                    return new ResponseEntity<>(body, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new Error("User '" + userName + "' is not member of domain '" + domainId + "'!", "USER_NOT_MEMBER_OF_DOMAIN"), HttpStatus.NOT_FOUND);
                }
            } else {
                return new ResponseEntity<>(new Error("User '" + userName + "' does not exist!", "NO_SUCH_USER"), HttpStatus.NOT_FOUND);
            }

        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Toggle whether answers of domain should be generally protected or public
     */
    @RequestMapping(value = "/{id}/answers-generally-protected", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Toggle whether answers of domain should be generally protected or public")
    public ResponseEntity<?> toggleAnswersGenerallyProtected(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            Context domain = domainService.toggleAnswersGenerallyProtected(id);

            String body = "{\"answers-generally-protected\":\"" + domain.getAnswersGenerallyProtected() + "\"}";
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Toggle whether human feedback should be considered when answering questions
     */
    @RequestMapping(value = "/{id}/consider-human-feedback", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Toggle whether human feedback should be considered when answering questions")
    public ResponseEntity<?> toggleConsiderHumanFeedback(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            Context domain = domainService.toggleConsiderHumanFeedback(id);

            String body = "{\"consider-human-feedback\":\"" + domain.getConsiderHumanFeedback() + "\"}";
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Toggle whether answers should be generated / completed
     */
    @RequestMapping(value = "/{id}/generate-answers", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Toggle whether answers should be generated / completed")
    public ResponseEntity<?> toggleGenerateAnswers(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            Context domain = domainService.toggleGenerateAnswers(id);

            String body = "{\"generate-answers\":\"" + domain.getGenerateCompleteAnswers() + "\"}";
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Toggle whether answers should be re-ranked
     */
    @RequestMapping(value = "/{id}/re-rank-answers", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Toggle whether answers should be re-ranked")
    public ResponseEntity<?> toggleReRankAnswers(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            Context domain = domainService.toggleReRankAnswers(id);

            String body = "{\"re-rank-answers\":\"" + domain.getReRankAnswers() + "\"}";
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Toggle whether knowledge source is enabled or disabled
     */
    @RequestMapping(value = "/{id}/knowledge-source/{ks-id}/enable-disable", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Toggle whether knowledge source is enabled or disabled")
    public ResponseEntity<?> toggleKnowledgeSourceEnabled(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "ks-id", value = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            KnowledgeSourceMeta ksMeta = domainService.toggleKnowledeSourceEnabled(id, ksId);

            if (ksMeta != null) {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode body = mapper.createObjectNode();
                body.put("knowledge-source-name", ksMeta.getName());
                body.put("enabled", ksMeta.getIsEnabled());
                return new ResponseEntity<>(body.toString(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Error("No such knowledge source!", "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
            }
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Toggle whether answers must be approved / moderated
     */
    @RequestMapping(value = "/{id}/moderation", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Toggle whether answers must be approved / moderated")
    public ResponseEntity<?> toggleModeration(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            Context domain = domainService.toggleModeration(id);

            String body = "{\"moderation\":\"" + domain.getAnswersMustBeApprovedByModerator() + "\"}";
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Toggle whether user (who asked question) should be informed re moderation
     */
    @RequestMapping(value = "/{id}/moderation/inform", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Toggle whether user (who asked question) should be informed re moderation")
    public ResponseEntity<?> toggleInformReModeration(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            Context domain = domainService.toggleInformUserReModeration(id);

            String body = "{\"inform-re-moderation\":\"" + domain.getInformUserReModeration() + "\"}";
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Toggle whether a particular webhook is active or inactive
     */
    @RequestMapping(value = "/{id}/webhooks/{webhook-id}/active", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Toggle whether a particular webhook is active or inactive")
    public ResponseEntity<?> toggleWebhook(
            @ApiParam(name = "id", value = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @ApiParam(name = "webhook-id", value = "Webhook Id",required = true)
            @PathVariable(value = "webhook-id", required = true) String webhookId,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            Webhook webhook = domainService.toggleWebhook(id, webhookId);

            StringBuilder body = new StringBuilder("{");
            body.append("\"domain-id\":\"" + id + "\",");
            body.append("\"webhook-id\":\"" + webhookId + "\",");
            body.append("\"active\":" + webhook.getEnabled() + "");
            body.append("}");

            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
