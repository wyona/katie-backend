package com.wyona.katie.controllers.v1;

import com.wyona.katie.integrations.email.EmailService;
import com.wyona.katie.models.Error;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.User;
import com.wyona.katie.services.AuthenticationService;
import com.wyona.katie.services.ContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

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

    /**
     * REST interface to trigger processing new emails
     */
    @RequestMapping(value = "/sync", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Process new emails associated with a particular Katie domain")
    public ResponseEntity<?> processEmails(
        @ApiParam(name = "domain", value = "Domain Id",required = true)
        @RequestParam(value = "domain", required = true) String domainId,
        @ApiParam(name = "include-feedback-links", value = "When true, then answer contains feedback links", required = false)
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
        User user = authService.getUser(false, false);
        emailService.processEmailsUnread(domain, trustAllSSLCertificates, _includeFeedbackLinks, user, processId);

        return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
    }
}
