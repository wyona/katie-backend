package com.wyona.katie.controllers.v1;

import com.fasterxml.jackson.databind.DeserializationFeature;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

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
    @Operation(summary="Export all questions of a particular domain including labels whether questions were recognized successfully, which can be used for training a model")
    public ResponseEntity<?> exportQuestionsDataset(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String domainId,
            HttpServletRequest request) {

        if (!domainService.existsContext(domainId)) {
            return new ResponseEntity<>(new Error("Domain '" + domainId + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            User user = iamService.getUser(false, false);
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
    @Operation(summary="Export all QnAs of a particular domain")
    public ResponseEntity<?> exportQnAs(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            User user = iamService.getUser(false, false);
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
    @Operation(summary = "Import QnAs from JSON text into a particular domain", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> importQnAsFromText(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "qnas", description = "An array of QnAs", required = true)
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
            User user = iamService.getUser(false, false);
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
    @Operation(summary = "Import QnAs from JSON file into a particular domain", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> importQnAsFromFile(
            @Parameter(name = "id", description = "Domain Id",required = true)
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
            User user = iamService.getUser(false, false);
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
    @Operation(summary = "Import PDF into a particular domain", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> importPDF(
            @Parameter(name = "id", description = "Domain Id", required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "text-splitter", description = "Text Splitter", required = true)
            @RequestParam(value = "text-splitter", required = true) TextSplitterImpl textSplitterImpl,
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
            User user = iamService.getUser(false, false);
            if (user == null) {
                throw new AccessDeniedException("User is not signed in!");
            }
            if (!domainService.isMemberOrAdmin(id)) {
                throw new AccessDeniedException("User '" + user.getUsername() + "' is neither member of domain '" + id + "' nor admin!");
            }

            Context domain = domainService.getContext(id);

            // TODO: Check size: file.getSize();
            log.info("Content type of uploaded file: " + file.getContentType());
            if (!ContentType.fromString(file.getContentType()).equals(ContentType.APPLICATION_PDF)) {
                return new ResponseEntity<>(new Error("Content type is not application/pdf", "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
            }
            log.info("Name of uploaded file: " + file.getOriginalFilename());
            String processId = UUID.randomUUID().toString();
            domainService.importPDF(file.getOriginalFilename(), file.getInputStream(), textSplitterImpl, domain, processId, user.getId());

            return new ResponseEntity<>("{\"bg-process-id\":\"" + processId + "\"}", HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Import HTML web page
     */
    @RequestMapping(value = "/{id}/import/html-web-page", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary = "Import HTML web page into a particular domain", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> importHTMLWebPage(
            @Parameter(name = "id", description = "Domain Id", required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "text-splitter", description = "Text Splitter", required = true)
            @RequestParam(value = "text-splitter", required = true) TextSplitterImpl textSplitterImpl,
            @Parameter(name = "url", description = "URL of HTML web page", required = true)
            @RequestParam(value = "url", required = true) URL url,
            @Parameter(name = "css-selector", description = "CSS selector, element[attribute=value], e.g. div[id='content'] or [id='text']", required = false)
            @RequestParam(value = "css-selector", required = false) String cssSelector,
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
            User user = iamService.getUser(false, false);
            if (user == null) {
                throw new AccessDeniedException("User is not signed in!");
            }
            if (!domainService.isMemberOrAdmin(id)) {
                throw new AccessDeniedException("User '" + user.getUsername() + "' is neither member of domain '" + id + "' nor admin!");
            }

            Context domain = domainService.getContext(id);

            String processId = UUID.randomUUID().toString();
            domainService.importHTMLWebPage(url, cssSelector, textSplitterImpl, domain, processId, user.getId());

            return new ResponseEntity<>("{\"bg-process-id\":\"" + processId + "\"}", HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to update the mail sender email address, e.g. "Katie <no-reply@wyona.com>"
     */
    @RequestMapping(value = "/{id}/mail-sender", method = RequestMethod.PATCH, produces = "application/json")
    @Operation(summary = "Update mail sender")
    public ResponseEntity<?> updateMailSender(
            @Parameter(name = "id", description = "Domain Id (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("id") String domainid,
            @Parameter(name = "mail-sender", description = "Updated mail sender (Parameter: mail-sender)", required = true) @RequestBody JsonNode body
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
    @Operation(summary = "Update mail subject")
    public ResponseEntity<?> updateMailSubject(
            @Parameter(name = "id", description = "Domain Id (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("id") String domainid,
            @Parameter(name = "mail-subject", description = "Updated mail subject (Parameter: mail-subject)", required = true) @RequestBody JsonNode body
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
    @Operation(summary = "Update domain name")
    public ResponseEntity<?> updateDomainName(
            @Parameter(name = "id", description = "Domain Id (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("id") String domainid,
            @Parameter(name = "name", description = "Updated domain name, e.g. 'Apache Lucene'", required = true) @RequestBody DomainName name
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
     * REST interface to update the score threshold of retrieval implementation used by domain, e.g. replace "0.73" by "0.78"
     */
    @RequestMapping(value = "/{id}/score-threshold", method = RequestMethod.PATCH, produces = "application/json")
    @Operation(summary = "Update score threshold of retrieval implementation used by domain")
    public ResponseEntity<?> updateScoreThreshold(
            @Parameter(name = "id", description = "Domain Id (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("id") String domainid,
            @Parameter(name = "threshold", description = "Updated score threshold, e.g. 0.73", required = true)
            @RequestParam(value = "threshold", required = true) Double threshold
    ) {
        try {
            log.info("Update score threshold of retrieval implementation used by domain '" + domainid + "': " + threshold);
            domainService.updateScoreThreshold(domainid, threshold);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // TODO: Consider returning a body
            //return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to update the completion / chat configuration used by domain
     */
    @RequestMapping(value = "/{id}/completion-config", method = RequestMethod.PATCH, produces = "application/json")
    @Operation(summary = "Update comletion / chat configuration used by domain")
    public ResponseEntity<?> updateCompletionConfiguration(
            @Parameter(name = "id", description = "Domain Id (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("id") String domainid,
            @Parameter(name = "completion-impl", description = "Completion implementation", required = true)
            @RequestParam(value = "completion-impl", required = true) CompletionImpl completionImpl,
            @Parameter(name = "completion-model", description = "Completion model", required = true)
            @RequestParam(value = "completion-model", required = true) String completionModel,
            @Parameter(name = "completion-api-key", description = "Completion API key", required = true)
            @RequestParam(value = "completion-api-key", required = true) String completionApiKey,
            @Parameter(name = "completion-host", description = "Completion host, e.g. https://openai-katie.openai.azure.com", required = false)
            @RequestParam(value = "completion-host", required = false) String completionHost
    ) {
        try {
            log.info("Update completion implementation used by domain '" + domainid + "': " + completionImpl);

            CompletionConfig completionConfig = new CompletionConfig();
            completionConfig.setCompletionImpl(completionImpl);
            completionConfig.setModel(completionModel);
            completionConfig.setApiKey(completionApiKey);
            if (completionHost != null && completionHost.trim().length() > 0) {
                completionConfig.setHost(completionHost);
            }

            domainService.updateCompletionConfig(domainid, completionConfig);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // TODO: Consider returning a body
            //return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to update the classification implementation used by domain
     */
    @RequestMapping(value = "/{id}/classification-implementation", method = RequestMethod.PATCH, produces = "application/json")
    @Operation(summary = "Update classification implementation used by domain")
    public ResponseEntity<?> updateClassificationImplementation(
            @Parameter(name = "id", description = "Domain Id (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("id") String domainid,
            @Parameter(name = "classification-impl", description = "Classification implementation", required = true)
            @RequestParam(value = "classification-impl", required = true) ClassificationImpl classificationImpl
    ) {
        try {
            log.info("Update classification implementation used by domain '" + domainid + "': " + classificationImpl);

            domainService.updateClassificationImplementation(domainid, classificationImpl);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // TODO: Consider returning a body
            //return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to update the domain tag name, e.g. replace "apachelucene" by "apache-lucene"
     */
    @RequestMapping(value = "/{id}/tag-name", method = RequestMethod.PATCH, produces = "application/json")
    @Operation(summary= "Update domain tag name")
    public ResponseEntity<?> updateDomainTagName(
            @Parameter(name = "id", description = "Domain Id (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("id") String domainid,
            @Parameter(name = "tag-name", description = "Updated domain tag name, e.g. 'apache-lucene'", required = true)
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
    @Operation(summary="Get insights summary of a particular domain")
    public ResponseEntity<?> getInsightsSummary(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "last-number-of-days", description = "Last number of days, e.g. last 30 days",required = false)
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
    @Operation(summary="Get insights history of a particular domain")
    public ResponseEntity<?> getInsightsHistory(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "last-number-of-days", description = "Last number of days, e.g. last 30 days", required = false)
            @RequestParam(value = "last-number-of-days", required = false) Integer lastNumberOfDays,
            @Parameter(name = "event-type", description = "Event type", required = true)
            @RequestParam(value = "event-type", required = true) EventType type,
            @Parameter(name = "interval", description = "Interval", required = true)
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
    @Operation(summary="Get insights re users of a particular domain")
    public ResponseEntity<?> getInsightsUsers(
            @Parameter(name = "id", description = "Domain Id",required = true)
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
     * Add TOPdesk as knowledge source
     */
    @RequestMapping(value = "/{id}/knowledge-source/topdesk", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary="Add TOPdesk as knowledge source")
    public ResponseEntity<?> addKnowledgeSourceTOPdesk(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "name", description = "Knowledge source name, e.g. 'TOPdesk Wyona'",required = true)
            @RequestParam(value = "name", required = true) String name,
            @Parameter(name = "base-url", description = "TOPdesk Base URL, e.g. 'https://topdesk.wyona.com'", required = true)
            @RequestParam(value = "base-url", required = true) String baseUrl,
            @Parameter(name = "username", description = "Username", required = true)
            @RequestParam(value = "username", required = true) String username,
            @Parameter(name = "password", description = "Password", required = true)
            @RequestParam(value = "password", required = true) String password,
            @Parameter(name = "limit", description = "Limit", required = false)
            @RequestParam(value = "limit", required = false) Integer limit,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            domainService.addKnowledgeSourceTOPdesk(id, name, baseUrl, username, password, limit);
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
     * Add OneNote as knowledge source
     */
    @RequestMapping(value = "/{id}/knowledge-source/onenote", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary="Add OneNote as knowledge source")
    public ResponseEntity<?> addKnowledgeSourceOneNote(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "name", description = "Knowledge source name, e.g. 'Katie Documentation'",required = true)
            @RequestParam(value = "name", required = true) String name,
            @Parameter(name = "apiToken", description = "MS Graph API Token", required = false)
            @RequestParam(value = "apiToken", required = false) String apiToken,
            @Parameter(name = "tenant", description = "Tenant", required = false)
            @RequestParam(value = "tenant", required = false) String tenant,
            @Parameter(name = "client-id", description = "Client Id", required = false)
            @RequestParam(value = "client-id", required = false) String clientId,
            @Parameter(name = "client-secret", description = "Client secret", required = false)
            @RequestParam(value = "client-secret", required = false) String clientSecret,
            @Parameter(name = "scope", description = "Scope, e.g. 'Notes.Read.All'", required = false)
            @RequestParam(value = "scope", required = false) String scope,
            @Parameter(name = "location", description = "Location, e.g. 'groups/c5a3125f-f85a-472a-8561-db2cf74396ea' or 'users/michael.wechner@wyona.com'", required = false)
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
     * Add Filesystem directory as knowledge source
     */
    @RequestMapping(value = "/{id}/knowledge-source/filesystem", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary="Add Filesystem directory as knowledge source (NOTE: For security reasons one cannot set a filesystem base path)")
    public ResponseEntity<?> addKnowledgeSourceFilesystem(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "name", description = "Knowledge source name, e.g. 'Katie Documentation'",required = true)
            @RequestParam(value = "name", required = true) String name,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            domainService.addKnowledgeSourceFilesystem(id, name);
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
     * Add SharePoint as knowledge source
     */
    @RequestMapping(value = "/{id}/knowledge-source/sharepoint", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary="Add SharePoint as knowledge source, whereas see https://app.katie.qa/connectors/sharepoint-connector.html")
    public ResponseEntity<?> addKnowledgeSourceSharePoint(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "name", description = "Knowledge source name, e.g. 'Katie SharePoint'",required = true)
            @RequestParam(value = "name", required = true) String name,
            @Parameter(name = "apiToken", description = "MS Graph API Token", required = false)
            @RequestParam(value = "apiToken", required = false) String apiToken,
            @Parameter(name = "tenant", description = "Tenant Id, whereas see https://portal.azure.com/#settings/directory", required = true)
            @RequestParam(value = "tenant", required = true) String tenant,
            @Parameter(name = "client-id", description = "Client Id, whereas see Microsoft Entra ID / Applications, https://portal.azure.com/#view/Microsoft_AAD_IAM/StartboardApplicationsMenuBlade/~/AppAppsPreview/menuId~/null", required = true)
            @RequestParam(value = "client-id", required = true) String clientId,
            @Parameter(name = "client-secret", description = "Client secret", required = true)
            @RequestParam(value = "client-secret", required = true) String clientSecret,
            @Parameter(name = "scope", description = "Scope, e.g. 'Sites.Read.All', whereas make sure to enable permissions for client / app id accordingly", required = true)
            @RequestParam(value = "scope", required = true) String scope,
            @Parameter(name = "site-id", description = "SharePoint site Id, e.g. 'ee403fc5-vfd8-7fhe-9171-k11dw4db239c', whereas get Site Id using Microsoft Graph, e.g. https://graph.microsoft.com/v1.0/sites?search=KatieTest or https://graph.microsoft.com/v1.0/sites/wyona.sharepoint.com:/sites/KatieTest?$select=id", required = true)
            @RequestParam(value = "site-id", required = true) String siteId,
            @Parameter(name = "base-url", description = "SharePoint base URL, e.g. https://wyona.sharepoint.com (ROOT site) or https://wyona.sharepoint.com/sites/KatieTest", required = true)
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
    @Operation(summary="Add Website (pages) as knowledge source")
    public ResponseEntity<?> addKnowledgeSourceWebsite(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "name", description = "Knowledge source name, e.g. 'Katie Documentation'",required = true)
            @RequestParam(value = "name", required = true) String name,
            @Parameter(name = "seed-url", description = "Seed URL, e.g. https://wyona.com", required = false)
            @RequestParam(value = "seed-url", required = false) String seedUrl,
            @Parameter(name = "urls", description = "Comma separated list of page URLs, e.g. https://wyona.com/about,https://wyona.com/contact", required = false)
            @RequestParam(value = "urls", required = false) String pageURLs,
            @Parameter(name = "chunk-size", description = "Chunk size, e.g. 1500", required = false)
            @RequestParam(value = "chunk-size", required = false) Integer chunkSize,
            @Parameter(name = "chunk-overlap", description = "Chunk overlap, e.g. 30", required = false)
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
    @Operation(summary="Add third-party RAG as knowledge source")
    public ResponseEntity<?> addKnowledgeSourceThirdPartyRAG(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "name", description = "Knowledge source name, e.g. 'Law Assistant RAG'",required = true)
            @RequestParam(value = "name", required = true) String name,
            @Parameter(name = "endpoint-url", description = "Endpoint URL, e.g. http://0.0.0.0:8000/chat", required = true)
            @RequestParam(value = "endpoint-url", required = true) String endpointUrl,
            @Parameter(name = "response-json-pointer", description = "Response / Answer JSON pointer, e.g. '/data/content' or '/response/docs/0/content_txt'", required = true)
            @RequestParam(value = "response-json-pointer", required = true) String responseJsonPointer,
            @Parameter(name = "reference-json-pointer", description = "Reference / Source JSON pointer, e.g. '/data/reasoning_thread/0/result/art_para/0' or '/response/docs/0/id'", required = false)
            @RequestParam(value = "reference-json-pointer", required = false) String referenceJsonPointer,
            @Parameter(name = "endpoint-payload", description = "Payload sent to endpoint, e.g. {\"message\":[{\"content\":\"{{QUESTION}}\",\"role\":\"user\"}],\"stream\":false} or {\"query\" : \"{{QUESTION}}\"}", required = true)
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
    @Operation(summary="Add Supabase as knowledge source")
    public ResponseEntity<?> addKnowledgeSourceSupabase(
            @Parameter(name = "id", description = "Domain Id", required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "name", description = "Knowledge source name, e.g. 'My Supabase'", required = true)
            @RequestParam(value = "name", required = true) String name,
            @Parameter(name = "answer-field-names", description = "Comma separated list of field names, e.g. 'abstract, text'", required = true)
            @RequestParam(value = "answer-field-names", required = true) String answerFieldNames,
            @Parameter(name = "classifications-field-names", description = "Comma separated list of field names, e.g. 'tags, custom_tags, coauthors'", required = true)
            @RequestParam(value = "classifications-field-names", required = true) String classificationsFieldNames,
            @Parameter(name = "question-field-names", description = "Comma separated list of field names, e.g. 'titel'", required = true)
            @RequestParam(value = "question-field-names", required = true) String questionFieldNames,
            @Parameter(name = "url", description = "Base URL, e.g. 'https://repid.ch'", required = true)
            @RequestParam(value = "url", required = true) String url,
            @Parameter(name = "chunk-size", description = "Chunk size, e.g. 1000", required = false, schema = @Schema(defaultValue = "1000"))
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
    @Operation(summary="Delete a particular knowledge source.")
    public ResponseEntity<?> deleteKnowledgeSource(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "ks-id", description = "Knowledge source Id",required = true)
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
    @Operation(summary = "Trigger a particular Directus based knowledge source by a webhook", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> triggerKnowledgeSourceDirectus(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "ks-id", description = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @Parameter(name = "webhook-payload", description = "Webhook payload sent by Directus", required = true)
            @RequestBody WebhookPayloadDirectus payload,
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
    @Operation(summary = "Trigger a particular Supabase based knowledge source by a webhook", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> triggerKnowledgeSourceSupabase(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "ks-id", description = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @Parameter(name = "webhook-payload", description = "Webhook payload sent by Supabase", required = true)
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
    @Operation(summary = "Trigger a particular TOPdesk based knowledge source by a webhook",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Webhook payload sent by TOPdesk. Request types: 0) Import batch of incidents, e.g. 1000 incidents 1) Import one particular incident, 2) Get visible replies of a particular incident 3) Sync categories / subcategories (remove obsolete categories / subcategories and add new categories / subcategories) 4) Analytics of batch of incidents, e.g. Analytics of 1000 incidents",
                    required = true
            ),
            security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> triggerKnowledgeSourceTOPdesk(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "ks-id", description = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @RequestBody WebhookPayloadTOPdesk payload,
            HttpServletRequest request) {

        try {
            authenticationService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        String processId = UUID.randomUUID().toString();
        String userId = authenticationService.getUserId();
        connectorService.triggerKnowledgeSourceConnectorInBackground(KnowledgeSourceConnector.TOP_DESK, id, ksId, payload, processId, userId);

        return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
    }

    /**
     * Trigger a particular Discourse based knowledge source by a webhook
     */
    @RequestMapping(value = "/{id}/knowledge-source/{ks-id}/invoke-by-discourse", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary = "Trigger a particular Discourse based knowledge source by a webhook", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> triggerKnowledgeSourceDiscourse(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "ks-id", description = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @Parameter(name = "webhook-payload", description = "Webhook payload sent by Discourse", required = true)
            @RequestBody WebhookPayloadDiscourse payload,
            HttpServletRequest request) {

        try {
            authenticationService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

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
    @Operation(summary = "Trigger a particular Confluence based knowledge source by a webhook", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> triggerKnowledgeSourceConfluence(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "ks-id", description = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @Parameter(name = "webhook-payload", description = "Webhook payload sent by Confluence", required = true)
            @RequestBody WebhookPayloadConfluence payload,
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
    @Operation(summary = "Trigger a particular Website based knowledge source by a webhook",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Webhook payload sent by Website",
                    required = false
            ),
            security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> triggerKnowledgeSourceWebsite(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "ks-id", description = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @RequestBody(required = false) WebhookPayloadWebsite payload,
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
    @Operation(summary = "Trigger a particular Outlook based knowledge source by a webhook", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> triggerKnowledgeSourceOutlook(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "ks-id", description = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @Parameter(name = "webhook-payload", description = "Webhook payload sent by Outlook", required = true)
            @RequestBody WebhookPayload payload,
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
    @Operation(summary = "Trigger a particular OneNote based knowledge source by a webhook", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> triggerKnowledgeSourceOneNote(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "ks-id", description = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @Parameter(name = "webhook-payload", description = "Webhook payload sent by OneNote", required = true)
            @RequestBody WebhookPayloadOneNote payload,
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

        String processId = UUID.randomUUID().toString();
        String userId = authenticationService.getUserId();
        connectorService.triggerKnowledgeSourceConnectorInBackground(KnowledgeSourceConnector.ONENOTE, id, ksId, payload, processId, userId);

        return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
    }

    /**
     * Trigger a particular Sharepoint based knowledge source by a webhook
     * https://learn.microsoft.com/en-us/graph/change-notifications-delivery-webhooks?tabs=http#receive-notifications
     */
    @RequestMapping(value = "/{id}/knowledge-source/{ks-id}/invoke-by-sharepoint", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Trigger a particular Sharepoint based knowledge source by a webhook")
    public ResponseEntity<String> triggerKnowledgeSourceSharepoint(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "ks-id", description = "Knowledge Source Id",required = true)
            @PathVariable(value = "ks-id", required = true) String ksId,
            @Parameter(name = "validationToken", description = "Microsoft Graph Validation Token", required = false)
            @RequestParam(value = "validationToken", required = false) String validationToken,
            HttpServletRequest request
    ) throws IOException  {

        // Handle SharePoint validation first
        if (validationToken != null) {
            log.info("Return Microsoft Graph Validation Token '{}'", validationToken);
            return ResponseEntity.ok(validationToken);
        }

        // Handle actual notification
        String payloadBody = request.getReader().lines()
                .reduce("", (acc, line) -> acc + line);

        WebhookPayloadSharepoint payload = null;
        if (!payloadBody.isBlank()) {
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            payload = mapper.readValue(payloadBody, WebhookPayloadSharepoint.class);
            log.info("Received SharePoint notification: {}", payload);
        }

        if (!domainService.existsContext(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Domain '" + id + "' does not exist!");
        }

        String processId = UUID.randomUUID().toString();
        String userId = authenticationService.getUserId();
        if (userId == null) {
            log.warn("User is not signed in.");
        }
        connectorService.triggerKnowledgeSourceConnectorInBackground(
                KnowledgeSourceConnector.SHAREPOINT, id, ksId, payload, processId, userId);

        // Return 202 Accepted for normal notifications
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(processId);
    }

    /**
     * Get knowledge sources of a particular domain
     */
    @RequestMapping(value = "/{id}/knowledge-sources", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary="Get knowledge sources of a particular domain")
    public ResponseEntity<?> getKnowledgeSources(
            @Parameter(name = "id", description = "Domain Id",required = true)
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
    @Operation(summary = "Import classification dataset from JSON file into a particular domain", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> importClassificationDatasetFromFile(
            @Parameter(name = "id", description = "Domain Id",required = true)
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
            User user = iamService.getUser(false, false);
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
    @Operation(summary="Get classification dataset")
    public ResponseEntity<?> getClassificationDataset(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "labels-only", description = "When set to true, then only labels are returned", required = false, schema = @Schema(defaultValue = "false"))
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
    @Operation(summary = "Get classification labels", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> getClassificationLabels(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "with-descriptions-only", description = "When set to true, then only labels are returned which contain a description", required = false, schema = @Schema(defaultValue = "false"))
            @RequestParam(value = "with-descriptions-only", required = false) Boolean withDescriptionsOnly,
            HttpServletRequest request) {

        if (!domainService.isAuthorized(id, request, "/" + id + "/classification/labels", JwtService.SCOPE_READ_LABELS)) {
            log.warn("Not authorized to get classification labels");
            return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
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

            // INFO: Sort labels alphabetically
            Classification[] classifications = dataset.getLabels();
            List<Classification> labels = new ArrayList<>();
            for (Classification classification : classifications) {
                if (withDescriptionsOnly != null && withDescriptionsOnly) {
                    if (classification.getDescription() != null) {
                        labels.add(classification);
                    }
                } else {
                    labels.add(classification);
                }
            }
            Collections.sort(labels);

            return new ResponseEntity<>(labels, HttpStatus.OK);
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
    @RequestMapping(value = "/{id}/classification/retrain", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary="Retrain classifier (using existing samples and with optional new samples from human preferences dataset, see /swagger-ui/#/feedback-controller/getRatingsOfPredictedLabelsUsingGET)")
    public ResponseEntity<?> retrainClassifier(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            // TODO: Add description for file, e.g. "Preference Dataset" and if not provided, then the existing samples are being used
            @RequestPart(name = "file", required = false) MultipartFile preferenceDataset,
            HttpServletRequest request) {

        if (!domainService.existsContext(id)) {
            return new ResponseEntity<>(new Error("Domain '" + id + "' does not exist!", "NO_SUCH_DOMAIN"), HttpStatus.NOT_FOUND);
        }

        try {
            Context domain = domainService.getDomain(id);
            String bgProcessId = UUID.randomUUID().toString();
            String userId = authenticationService.getUserId();

            // INFO: The method retrain(...) is using @Async, therefore we have to read the preference dataset before
            List<HumanPreferenceLabel> preferences = getHumanPreferences(preferenceDataset);

            HumanPreferenceLabel[] localPreferences = domainService.getRatingsOfPredictedLabels(domain.getId(), true, true);
            for (HumanPreferenceLabel localPreference : localPreferences) {
                preferences.add(localPreference);
            }

            classificationService.retrain(domain, preferences, bgProcessId, userId);

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
     * Get human preferences from uploaded file
     * @param preferenceDataset Uploaded file containing human preference dataset
     * @return list of preferences
     */
    private List<HumanPreferenceLabel> getHumanPreferences(MultipartFile preferenceDataset) {
        List<HumanPreferenceLabel> preferences = new ArrayList<>();
        if (preferenceDataset != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode rootNode = mapper.readTree(new BufferedInputStream(preferenceDataset.getInputStream()));

                if (rootNode.isArray()) {
                    for (int i = 0; i < rootNode.size(); i++) {
                        JsonNode preferenceNode= rootNode.get(i);
                        String text = preferenceNode.get(HumanPreferenceLabel.TEXT_FIELD).asText();
                        HumanPreferenceLabel preference = new HumanPreferenceLabel();
                        preference.setText(text);
                        if (preferenceNode.has(HumanPreferenceLabel.REJECTED_LABEL_FIELD)) {
                            JsonNode rejectedNode = preferenceNode.get(HumanPreferenceLabel.REJECTED_LABEL_FIELD);
                            // TODO: Add rest of data
                        }
                        if (preferenceNode.has(HumanPreferenceLabel.CHOSEN_LABEL_FIELD)) {
                            JsonNode chosenNode = preferenceNode.get(HumanPreferenceLabel.CHOSEN_LABEL_FIELD);
                            // TODO: Add rest of data, e.g. frequency
                            String label = chosenNode.get(HumanPreferenceLabel.LABEL_NAME_FIELD).asText();
                            String foreignId = chosenNode.get("id").asText();
                            String katieId = chosenNode.get(HumanPreferenceLabel.LABEL_KATIE_ID_FIELD).asText();
                            Classification chosenLabel = new Classification(label, foreignId, katieId);
                            preference.setChosenLabel(chosenLabel);
                        }
                        if (preferenceNode.has("meta")) {
                            JsonNode metaNode = preferenceNode.get("meta");
                            // TODO: Add rest of data
                            String clientMessageId = metaNode.get(HumanPreferenceMeta.CLIENT_MESSAGE_ID).asText();
                            Boolean approved = metaNode.get("approved").asBoolean();
                            String ratingId = metaNode.get("id").asText();
                            HumanPreferenceMeta meta = new HumanPreferenceMeta();
                            meta.setId(ratingId);
                            meta.setClientMessageId(clientMessageId);
                            meta.setApproved(approved);
                            preference.setMeta(meta);
                        }
                        preferences.add(preference);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.info("No preference dataset uploaded.");
        }
        return preferences;
    }

    /**
     * Get taxonomy entries of a particular domain
     */
    @RequestMapping(value = "/{id}/taxonomy/entries", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary="Get taxonomy entries")
    public ResponseEntity<?> getTaxonomyEntries(
            @Parameter(name = "id", description = "Domain Id",required = true)
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
    @Operation(summary="Add multiple taxomnomy entries, e.g. birthdate, birthplace")
    public ResponseEntity<?> addTaxonomyEntries(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "entries", description = "Array of entries", required = true)
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
    @Operation(summary="Get autocompletion entries")
    public ResponseEntity<?> getAutocompletionEntries(
            @Parameter(name = "id", description = "Domain Id",required = true)
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
    @Operation(summary="Add multiple autocompletion entries / questions")
    public ResponseEntity<?> addAutocompletionEntries(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "entries", description = "Array of entries", required = true)
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
    @Operation(summary="Add single autocompletion entry / question")
    public ResponseEntity<?> addAutocompletionEntry(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "entry", description = "Entry value, e.g. 'mountain' or question, e.g. 'What's the weather expected to be tomorrow?'", required = true)
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
    @Operation(summary="Delete autocompletion entry")
    public ResponseEntity<?> deleteAutocompletionEntry(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "value", description = "Entry value, e.g. 'speed of light'",required = true)
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
    @Operation(summary="Get URLs of webpages which contain imported QnAs")
    public ResponseEntity<?> getThirdPartyUrls(
            @Parameter(name = "id", description = "Domain Id",required = true)
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
    @Operation(summary="Get webhooks of a particular domain")
    public ResponseEntity<?> getWebhooks(
            @Parameter(name = "id", description = "Domain Id",required = true)
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
    @Operation(summary="Get deliveries of a particular webhook")
    public ResponseEntity<?> getWebhookDeliveries(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "webhook-id", description = "Webhook Id",required = true)
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
    @Operation(summary="Delete a particular webhook")
    public ResponseEntity<?> deleteWebhook(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "webhook-id", description = "Webhook Id",required = true)
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
    @Operation(summary="Add webhook to a particular domain")
    public ResponseEntity<?> addWebhook(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "webhook", description = "Webhook, whereas only payloadURL required, e.g. 'https://postman-echo.com/post'", required = true)
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
    @Operation(summary = "Reindex all QnAs of a particular domain", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> reindex(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "impl", description = "Detect duplicated question implementation",required = true)
            @RequestParam(value = "impl", required = true) DetectDuplicatedQuestionImpl searchImpl,
            @Parameter(name = "query-service-url", description = "Query service base URL (e.g. http://localhost:8383/api/v2) or Azure AI Search endpoint (e.g. https://katie.search.windows.net)", required = false)
            @RequestParam(value = "query-service-url", required = false) String queryServiceBaseUrl,
            @Parameter(name = "query-service-token", description = "Query service Token / Key / Secret", required = false)
            @RequestParam(value = "query-service-token", required = false) String queryServiceToken,
            @Parameter(name = "embedding-impl", description = "Embedding implementation",required = false)
            @RequestParam(value = "embedding-impl", required = false) EmbeddingsImpl embeddingImpl,
            @Parameter(name = "embedding-model", description = "Embedding model, e.g. all-mpnet-base-v2 or text-embedding-3-small",required = false)
            @RequestParam(value = "embedding-model", required = false) String embeddingModel,
            @Parameter(name = "embedding-description-type", description = "Embedding value type",required = false)
            @RequestParam(value = "embedding-value-type", required = false) EmbeddingValueType embeddingValueType,
            @Parameter(name = "embedding-endpoint", description = "OpenAI compatible embedding endpoint, e.g. https://api.mistral.ai/v1/embeddings", required = false)
            @RequestParam(value = "embedding-endpoint", required = false) String embeddingEndpoint,
            @Parameter(name = "api-token", description = "Embedding implementation API token",required = false)
            @RequestParam(value = "api-token", required = false) String apiToken,
            @Parameter(name = "index-alternative-questions", description = "Default is true, but when set to false, then alternative questions will not be indexed", required = false)
            @RequestParam(value = "index-alternative-questions", required = false, defaultValue = "true") Boolean indexAlternativeQuestions,
            @Parameter(name = "index-all-qnas", description = "Default is false, but when set to true, then also index QnAs which were not indexed yet", required = false)
            @RequestParam(value = "index-all-qnas", required = false, defaultValue = "false") Boolean indexAllQnAs,
            @Parameter(name = "throttle-time", description = "Throttle time in milliseconds",required = false)
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
    @Operation(summary="Invite user by email to a particular domain")
    public ResponseEntity<?> inviteUser(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "email", description = "Email of invited user",required = true)
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
    @Operation(summary="Add user by username to a particular domain")
    public ResponseEntity<?> addUser(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "username", description = "Username",required = true)
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
    @Operation(summary = "Remove user as member from a particular domain")
    public ResponseEntity<?> removeUser(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "username", description = "Username of domain member",required = true)
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
    @Operation(summary="Get all members of a particular domain")
    public ResponseEntity<?> getMembers(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "property", description = "User domain property (EXPERT or MODERATOR)", required = false)
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
    @Operation(summary="Update IMAP configuration")
    public ResponseEntity<?> updateIMAPConfiguration(
            @Parameter(name = "id", description = "Domain Id", required = true)
            @PathVariable(value = "id", required = true) String domainId,
            @Parameter(name = "imap_config", description = "IMAP configuration", required = true)
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
    @Operation(summary="Update match reply-to email address")
    public ResponseEntity<?> updateMatchReplyToEmailAddress(
            @Parameter(name = "id", description = "Domain Id", required = true)
            @PathVariable(value = "id", required = true) String domainId,
            @Parameter(name = "emails", description = "Match reply-to emails", required = true)
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
    @Operation(summary="Toggle whether a particular user is a moderator")
    public ResponseEntity<?> toggleModerator(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String domainId,
            @Parameter(name = "username", description = "Username",required = true)
            @PathVariable(value = "username", required = true) String userName,
            HttpServletRequest request) {

        if (!domainService.existsContext(domainId)) {
            return new ResponseEntity<>(new Error("Domain '" + domainId + "' does not exist!", "NOT_FOUND"), HttpStatus.NOT_FOUND);
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
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Toggle whether a particular user is an expert
     */
    @RequestMapping(value = "/{id}/user/{username}/expert", method = RequestMethod.PUT, produces = "application/json")
    @Operation(summary="Toggle whether a particular user is an expert")
    public ResponseEntity<?> toggleExpert(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String domainId,
            @Parameter(name = "username", description = "Username",required = true)
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
    @Operation(summary="Toggle whether answers of domain should be generally protected or public")
    public ResponseEntity<?> toggleAnswersGenerallyProtected(
            @Parameter(name = "id", description = "Domain Id",required = true)
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
    @Operation(summary="Toggle whether human feedback should be considered when answering questions")
    public ResponseEntity<?> toggleConsiderHumanFeedback(
            @Parameter(name = "id", description = "Domain Id",required = true)
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
    @Operation(summary="Toggle whether answers should be generated / completed")
    public ResponseEntity<?> toggleGenerateAnswers(
            @Parameter(name = "id", description = "Domain Id",required = true)
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
    @Operation(summary="Toggle whether answers should be re-ranked")
    public ResponseEntity<?> toggleReRankAnswers(
            @Parameter(name = "id", description = "Domain Id",required = true)
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
    @Operation(summary="Toggle whether knowledge source is enabled or disabled")
    public ResponseEntity<?> toggleKnowledgeSourceEnabled(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "ks-id", description = "Knowledge Source Id",required = true)
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
    @Operation(summary="Toggle whether answers must be approved / moderated")
    public ResponseEntity<?> toggleModeration(
            @Parameter(name = "id", description = "Domain Id",required = true)
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
    @Operation(summary="Toggle whether user (who asked question) should be informed re moderation")
    public ResponseEntity<?> toggleInformReModeration(
            @Parameter(name = "id", description = "Domain Id",required = true)
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
    @Operation(summary="Toggle whether a particular webhook is active or inactive")
    public ResponseEntity<?> toggleWebhook(
            @Parameter(name = "id", description = "Domain Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @Parameter(name = "webhook-id", description = "Webhook Id",required = true)
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
