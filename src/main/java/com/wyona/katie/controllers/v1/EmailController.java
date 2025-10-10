package com.wyona.katie.controllers.v1;

import com.wyona.katie.integrations.email.EmailService;
import com.wyona.katie.models.Error;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.User;
import com.wyona.katie.services.AuthenticationService;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.IAMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
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
 * Controller to process emails
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/email")
public class EmailController {

    @Autowired
    private ContextService contextService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private IAMService iamService;

    /**
     * REST interface to trigger processing new emails
     */
    @RequestMapping(value = "/sync", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary="Process new emails associated with a particular Katie domain")
    public ResponseEntity<?> processEmails(
        @Parameter(name = "domain", description = "Domain Id",required = true)
        @RequestParam(value = "domain", required = true) String domainId,
        @Parameter(name = "include-feedback-links", description = "When true, then answer contains feedback links", required = false)
        @RequestParam(value = "include-feedback-links", required = false) Boolean includeFeedbackLinks,
        HttpServletRequest request) {

        if (!contextService.isMemberOrAdmin(domainId)) {
            log.error("User is neither member nor administrator");
            return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }

        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        boolean trustAllSSLCertificates = true; // TODO: Add as request parameter

        boolean _includeFeedbackLinks = false;
        if (includeFeedbackLinks != null) {
            _includeFeedbackLinks = includeFeedbackLinks;
            if (_includeFeedbackLinks) {
                log.info("Include Feedback Links.");
            } else {
                log.info("Do not include Feedback Links.");
            }
        }

        if (domain.getGmailConfiguration() == null && domain.getImapConfiguration() == null) {
            String errMsg = "Domain '" + domain.getId() + "' has neither IMAP nor Gmail configuration!";
            log.error(errMsg);
            return new ResponseEntity<>(new Error(errMsg, "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String processId = UUID.randomUUID().toString();
        User user = iamService.getUser(false, false);
        emailService.processEmailsUnread(domain, trustAllSSLCertificates, _includeFeedbackLinks, user, processId);

        return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
    }
}
