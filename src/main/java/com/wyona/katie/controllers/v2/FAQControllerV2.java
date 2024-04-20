package com.wyona.katie.controllers.v2;

import com.wyona.katie.models.Error;
import com.wyona.katie.services.AuthenticationService;
import com.wyona.katie.services.ImportService;
import com.wyona.katie.services.Utils;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.ExtractQnAsArgs;
import com.wyona.katie.models.User;
import com.wyona.katie.models.faq.FAQ;
import com.wyona.katie.services.ContextService;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Controller to get and set FAQ
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v2") 
public class FAQControllerV2 {

    @Autowired
    private ContextService contextService;

    @Autowired
    private ImportService importService;

    @Autowired
    private AuthenticationService authService;

    /**
     * REST interface to get status / progress of import
     */
    @RequestMapping(value = "/faq/import/progress", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get status / progress of import")
    public ResponseEntity<?> getImportProgress(
            @ApiParam(name = "import-process-id", value = "Import process Id",required = false)
            @RequestParam(value = "import-process-id", required = false) String importProcessId,
            HttpServletRequest request) {

        File file = new File(System.getProperty("java.io.tmpdir"), ImportService.IMPORT_PROGRESS_FILE_PREFIX + importProcessId);

        try {
            String progressJson = Files.readString(Paths.get(file.getAbsolutePath()));
            return new ResponseEntity<>(progressJson, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to get available languages of FAQ
     */
    @RequestMapping(value = "/faq/languages", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get available languages of FAQ")
    public ResponseEntity<?> getAvailableLanguages(
            @ApiParam(name = "context", value = "Domain Id, for example 'wyona', which represents a single realm containing its own set of FAQ, etc.",required = false)
            @RequestParam(value = "context", required = false) String context,
            HttpServletRequest request) {

        Context ctx = null;
        try {
            ctx = contextService.getContext(context);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            return new ResponseEntity<>(ctx.getFAQLanguages(), HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to get FAQ
     */
    @RequestMapping(value = "/faq", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get Frequently Asked Questions")
    public ResponseEntity<?> getFAQ(
        @ApiParam(name = "language", value = "Language of FAQ, e.g. 'de' or 'en'",required = true)
        @RequestParam(value = "language", required = true) String language,
        @ApiParam(name = "context", value = "Domain Id, for example '45c6068a-e94b-46d6-zfa1-938f755d446g', which represents a single realm containing its own set of FAQ, etc.",required = false)
        @RequestParam(value = "context", required = false) String domainId,
        @ApiParam(name = "tag-name", value = "Tag name of domain, for example 'apache-lucene', which is associated with a particular domain Id",required = false)
        @RequestParam(value = "tag-name", required = false) String tagName,
        @ApiParam(name = "uuid-only", value = "When set to true, then for performance/scalability reasons only get UUIDs of questions and do pagination inside client",required = false)
        @RequestParam(value = "uuid-only", required = false) boolean uuidOnly,
        @ApiParam(name = "public-only", value = "When set to true, then only topics where the visibility is set to public", required = false)
        @RequestParam(value = "public-only", required = false) Boolean publicOnly,
        HttpServletRequest request) {

        Context domain = null;
        try {
            if (tagName != null) {
                log.info("Get domain for tag name '" + tagName + "' ...");
                domain = contextService.getDomainByTagName(tagName);
            } else {
                domain = contextService.getContext(domainId);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        language = Utils.getTwoLetterCode(language);

        if (!contextService.existsFAQ(domain, language)) {
            String msg = "No FAQs for language '" + language + "' and domain '" + domain.getId() + "'!";
            log.error(msg);
            return new ResponseEntity<>(new Error(msg, "LANGUAGE_NOT_SUPPORTED"), HttpStatus.NOT_FOUND);
        }

        try {
            if (uuidOnly) {
                log.info("For performance/scalability reasosn only get UUIDs of questions.");
            }
            boolean _publicOnly = true;
            if (publicOnly != null) {
                _publicOnly = publicOnly.booleanValue();
            }
            FAQ faq = contextService.getFAQ(domain, language, uuidOnly, _publicOnly);
            return new ResponseEntity<>(faq.getTopics(), HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to import FAQs from a third-party webpage, e.g. https://cwiki.apache.org/confluence/display/LUCENE/LuceneFAQ
     */
    @RequestMapping(value = "/faq/import/url", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value="Batch Import of FAQs from a third-party webpage, e.g. https://cwiki.apache.org/confluence/display/LUCENE/LuceneFAQ")
    public ResponseEntity<?> importFAQsFromWebPage(
            @ApiParam(name = "context", value = "Domain Id, for example 'wyona', which represents a single realm containing its own set of FAQ, etc.",required = false)
            @RequestParam(value = "context", required = false) String context,
            @ApiParam(name = "url", value = "URL of third-party webpage, .e.g https://cwiki.apache.org/confluence/display/LUCENE/LuceneFAQ", required = true)
            @RequestBody ExtractQnAsArgs extractQnAsArgs,
            HttpServletRequest request) {

        Context domain = null;
        try {
            domain = contextService.getContext(context);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            String importProcessId = UUID.randomUUID().toString();
            // TODO: Run import as thread and use process Id to monitor progress
            if (extractQnAsArgs.getUrl() != null) {
                contextService.extractQnAs(extractQnAsArgs.getUrl(), extractQnAsArgs.getClean(), extractQnAsArgs.getQnAExtractorImpl(), domain, importProcessId);
                return new ResponseEntity<>("{\"import-process-id\":\"" + importProcessId + "\"}", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("No link / URL provided!", HttpStatus.BAD_REQUEST);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to import FAQs contained by JSON
     */
    @RequestMapping(value = "/faq/import/json", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    //@RequestMapping(value = "/faq/import/json", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value="Batch Import of Frequently Asked Questions (create and update)")
    public ResponseEntity<?> importFAQ(
            @ApiParam(name = "language", value = "Language of FAQ, e.g. 'de' or 'en'",required = true)
            @RequestParam(value = "language", required = true) String language,
            @ApiParam(name = "context", value = "Domain Id, for example 'wyona', which represents a single realm containing its own set of FAQ, etc.",required = false)
            @RequestParam(value = "context", required = false) String context,
            @ApiParam(name = "faq", value = "A set of new (without id) or existing (with id) topics including FAQs, whereas question and answer are mandatory for each FAQ", required = true)
            @RequestBody FAQ faq,
            HttpServletRequest request) {

        Context domain = null;
        try {
            domain = contextService.getContext(context);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            boolean isPublic = true; // TODO: Consider isPublic as request parameter
            String importProcessId = UUID.randomUUID().toString();
            User signedInUser = authService.getUser(false, false);
            boolean indexAlternativeQuestions = true; // TODO: Make configurable
            importService.batchImportJSON(domain, language, faq, isPublic, signedInUser, importProcessId, indexAlternativeQuestions);
            return new ResponseEntity<>("{\"import-process-id\":\"" + importProcessId + "\"}", HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to import QnAs from a file in SQuAD format (https://rajpurkar.github.io/SQuAD-explorer/)
     */
    @RequestMapping(value = "/faq/import/squad", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    //@RequestMapping(value = "/faq/import/squad", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value="Import QnAs from a file in SQuAD format (https://rajpurkar.github.io/SQuAD-explorer/). Make sure that max-file-size, max-request-size is configured accordingly!")
    public ResponseEntity<?> importSQuAD(
            @ApiParam(name = "language", value = "Language of SQuAD, e.g. 'de' or 'en'",required = true)
            @RequestParam(value = "language", required = true) String language,
            @ApiParam(name = "domainId", value = "Domain Id, for example 'wyona', which represents a single realm containing its own set of FAQ, etc.",required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            // WARNING: value does not seem to work for RequestPart, see https://github.com/springfox/springfox/issues/3571
            //@ApiParam(name = "file", value ="SQuAD file (make sure that max-file-size, max-request-size is configured accordingly)", required = true)
            //@ApiParam(name = "SQuAD file (make sure that max-file-size, max-request-size is configured accordingly)", value ="SQuAD file (make sure that max-file-size, max-request-size is configured accordingly)", required = true)
            @RequestPart("file") MultipartFile file,
            //@RequestPart(name = "file", required = true) MultipartFile file,
            //@ApiParam(name = "faq", value = "A set of QnAs in the SQuAD format", required = true)
            //@RequestBody SQuAD squad,
            HttpServletRequest request) {

        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        /* OBSOLETE
        if (contextService.existsFAQ(domain, language)) {
            String msg = "FAQs for language '" + language + "' and domain '" + domain.getId() + "' already exist!";
            log.error(msg);
            return new ResponseEntity<>(new Error(msg, "LANGUAGE_ALREADY_EXISTS"), HttpStatus.CONFLICT);
        }
         */

        try {
            boolean isPublic = true; // TODO: Consider isPublic as request parameter

            String importProcessId = UUID.randomUUID().toString();

            File tmpFile = new File(System.getProperty("java.io.tmpdir"), ImportService.SQUAD_FILE_PREFIX + importProcessId);
            log.info("Save uploaded SQuAD to temporary file: " + tmpFile.getAbsolutePath());
            FileUtils.copyInputStreamToFile(file.getInputStream(), tmpFile);

            User signedInUser = authService.getUser(false, false);
            boolean indexAlternativeQuestions = true; // TODO: Make configurable
            importService.batchImportSQuAD(domain, language, isPublic, importProcessId, signedInUser, indexAlternativeQuestions);

            return new ResponseEntity<>("{\"import-process-id\":\"" + importProcessId + "\"}", HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to import FAQ from an XML
     */
    @RequestMapping(value = "/faq/import/xml", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value="Import Frequently Asked Questions from an XML file")
    public ResponseEntity<?> importFAQfromXMLFile(
            @ApiParam(name = "language", value = "Language of FAQ, e.g. 'de' or 'en'",required = true)
            @RequestParam(value = "language", required = true) String language,
            @ApiParam(name = "context", value = "Domain Id, for example 'wyona', which represents a single realm containing its own set of FAQ, etc.",required = false)
            @RequestParam(value = "context", required = false) String context,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {

        Context domain = null;
        try {
            domain = contextService.getContext(context);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        log.info("Name of uploaded file: " + file.getOriginalFilename());
        log.info("Content type of uploaded file: " + file.getContentType());
        // TODO: Check content type "text/xml"
        try {
            FAQ newFAQs = contextService.getFAQ(file.getInputStream());
            //log.debug("Parsed FAQ XML: " + newFAQs);

            boolean isPublic = true; // TODO: Consider isPublic as request parameter
            User signedInUser = authService.getUser(false, false);
            boolean indexAlternativeQuestions = true; // TODO: Make configurable
            FAQ importedFAQ = contextService.importFAQ(domain, language, newFAQs, isPublic, signedInUser, indexAlternativeQuestions);
            return new ResponseEntity<>(importedFAQ.getTopics(), HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to get FAQ
     */
    @RequestMapping(value = "/faq/qna", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Add existing QnA to a particular FAQ topic")
    public ResponseEntity<?> addQnA(
            @ApiParam(name = "language", value = "Language of FAQ topic, e.g. 'de' or 'en'",required = true)
            @RequestParam(value = "language", required = true) String language,
            @ApiParam(name = "domainId", value = "Domain Id, for example 'wyona', which represents a single realm containing its own set of FAQ, etc.",required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            @ApiParam(name = "uuid", value = "UUID of QnA",required = true)
            @RequestParam(value = "uuid", required = true) String uuid,
            @ApiParam(name = "topicId", value = "Topic Id",required = true)
            @RequestParam(value = "topicId", required = true) String topicId,
            HttpServletRequest request) {

        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (!contextService.existsFAQ(domain, language)) {
            String msg = "No FAQs for language '" + language + "' and domain '" + domain.getId() + "'!";
            log.error(msg);
            return new ResponseEntity<>(new Error(msg, "LANGUAGE_NOT_SUPPORTED"), HttpStatus.NOT_FOUND);
        }

        try {
            FAQ faq = contextService.addQnA2FAQ(domain, language, topicId, uuid, true);

            return new ResponseEntity<>(faq.getTopics(), HttpStatus.OK);
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
