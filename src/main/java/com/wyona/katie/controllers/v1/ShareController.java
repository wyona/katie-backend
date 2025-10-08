package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.Error;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.ResponseAnswer;
import com.wyona.katie.models.ShareInformationBody;
import com.wyona.katie.services.ContextService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//import org.hibernate.validator.constraints.NotEmpty;
//import javax.validation.constraints.NotEmpty;

import com.wyona.katie.services.QuestionAnsweringService;
import com.wyona.katie.services.IAMService;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.file.AccessDeniedException;
import java.util.Locale;

/**
 * Controller to share information with Katie, e.g. "The best seats in the movie theatre Alba are seats 12 and 13, row 5"
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1") 
public class ShareController {

    @Autowired
    private QuestionAnsweringService qaService;

    @Autowired
    private IAMService iamService;

    @Autowired
    private ResourceBundleMessageSource messageSource;

    @Autowired
    private ContextService contextService;

    /**
     * REST interface to share information / text  with Katie
     */
    @RequestMapping(value = "/share/{domain-id}/text", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Share information / text / knowledge with Katie, e.g. 'The best seats in the movie theatre Alba are seats 12 and 13, row 5'.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = ResponseAnswer.class),
            @ApiResponse(code = 403, message = "Forbidden", response = Error.class),
            @ApiResponse(code = 400, message = "Bad Request", response = Error.class)
    })
    public ResponseEntity<?> shareInformation(
            @ApiParam(name = "domain-id", value = "Domain Id of knowledge base, for example 'b3158772-ac8f-4ec1-a9d7-bd0d3887fd9b'.",required = true)
            @PathVariable(value = "domain-id", required = true) String domainId,
            @ApiParam(name = "information", value = "Text (information required, url optional) which user is sharing with Katie.", required = true)
            @RequestBody ShareInformationBody information,
            HttpServletRequest request) {

        Error domainError = validateDomain(domainId);
        if (domainError != null) {
            return new ResponseEntity<>(domainError, HttpStatus.BAD_REQUEST);
        }

        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "NO_SUCH_DOMAIN_ID"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            String info = information.getInformation();
            Error inputError = validateText(info);
            if (inputError != null) {
                return new ResponseEntity<>(inputError, HttpStatus.BAD_REQUEST);
            }
            String uuid = contextService.shareInformation(info, information.getURL(), domain);
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode body = mapper.createObjectNode();
            body.put("uuid", uuid);

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
     * REST interface to share link / URL
     */
    @RequestMapping(value = "/share/{domain-id}/link", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Share link / URL with Katie, e.g. https://www.wyona.com with Katie")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = ResponseAnswer.class),
            @ApiResponse(code = 403, message = "Forbidden", response = Error.class),
            @ApiResponse(code = 400, message = "Bad Request", response = Error.class)
    })
    public ResponseEntity<?> shareLink(
            @ApiParam(name = "domain-id", value = "Domain Id of knowledge base, for example 'b3158772-ac8f-4ec1-a9d7-bd0d3887fd9b'.",required = true)
            @PathVariable(value = "domain-id", required = true) String domainId,
            @ApiParam(name = "information", value = "Link / URL which user is sharing with Katie (keywords, url)", required = true)
            @RequestBody ShareInformationBody information,
            HttpServletRequest request) {

        Error domainError = validateDomain(domainId);
        if (domainError != null) {
            return new ResponseEntity<>(domainError, HttpStatus.BAD_REQUEST);
        }

        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "NO_SUCH_DOMAIN_ID"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            if (information.getURL() != null) {
                contextService.addUrlQnA(information.getURL(), information.getKeywords(), domain);
            } else {
                return new ResponseEntity<>("No link / URL provided!", HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>("{}", HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to share client side encrypted credentials with Katie
     */
    @RequestMapping(value = "/share/{domain-id}/credentials", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Share credentials with Katie, whereas make sure credentials are client side encrypted")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = ResponseAnswer.class),
            @ApiResponse(code = 403, message = "Forbidden", response = Error.class),
            @ApiResponse(code = 400, message = "Bad Request", response = Error.class)
    })
    public ResponseEntity<?> shareCredentials(
            @ApiParam(name = "domain-id", value = "Domain Id of knowledge base, for example 'b3158772-ac8f-4ec1-a9d7-bd0d3887fd9b'.",required = true)
            @PathVariable(value = "domain-id", required = true) String domainId,
            @ApiParam(name = "information", value = "Credentials which user is sharing with Katie (credentialsHint, encryptedCredentials, clientSideEncryptionAlgorithm)", required = true)
            @RequestBody ShareInformationBody information,
            HttpServletRequest request) {

        Error domainError = validateDomain(domainId);
        if (domainError != null) {
            return new ResponseEntity<>(domainError, HttpStatus.BAD_REQUEST);
        }

        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "NO_SUCH_DOMAIN_ID"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            contextService.shareCredentials(information.getCredentialsHint(), information.getEncryptedCredentials(), information.getClientSideEncryptionAlgorithm(), domain);

            return new ResponseEntity<>("{}", HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to share shopping list with Katie
     */
    @RequestMapping(value = "/share/{domain-id}/shopping-list", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Share shopping list with Katie")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = ResponseAnswer.class),
            @ApiResponse(code = 403, message = "Forbidden", response = Error.class),
            @ApiResponse(code = 400, message = "Bad Request", response = Error.class)
    })
    public ResponseEntity<?> shareShoppingList(
            @ApiParam(name = "domain-id", value = "Domain Id of knowledge base, for example 'b3158772-ac8f-4ec1-a9d7-bd0d3887fd9b'.",required = true)
            @PathVariable(value = "domain-id", required = true) String domainId,
            @ApiParam(name = "information", value = "Shopping list which user is sharing with Katie (keywords, information)", required = true)
            @RequestBody ShareInformationBody information,
            HttpServletRequest request) {

        Error domainError = validateDomain(domainId);
        if (domainError != null) {
            return new ResponseEntity<>(domainError, HttpStatus.BAD_REQUEST);
        }

        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "NO_SUCH_DOMAIN_ID"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            contextService.shareShoppingList(information.getKeywords(), information.getInformation(), domain);

            return new ResponseEntity<>("{}", HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validate text
     * @return null when text is valid and an Error object otherwise
     */
    private Error validateText(String text) {
        // INFO: Validate whether information was submitted
        if (text.isEmpty()) {
            return new Error("Text may not be empty", "TEXT_MAY_NOT_BE_EMPTY");
        }
        int MAX_LENGTH = 1024; // TODO: Make configurable
        if (text.length() > MAX_LENGTH) {
            return new Error("Text too long (max length is " + MAX_LENGTH + ")", "TEXT_TOO_LONG");
        }

        return null;
    }

    /**
     *
     */
    private Error validateDomain(String domainId) {
        // INFO: Validate domain id in case one was submitted
        if (domainId != null) {
            if (!contextService.existsContext(domainId)) {
                log.error("No such domain with Id '" + domainId + "'!");
                return new Error(messageSource.getMessage("error.no.such.domain.id", null, Locale.ENGLISH) + " '" + domainId + "'!", "NO_SUCH_DOMAIN_ID");
            }
        }

        return null;
    }
}
