package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.Error;
import com.wyona.katie.integrations.matrix.MatrixMessageSender;
import com.wyona.katie.models.Error;
import com.wyona.katie.models.Question;
import com.wyona.katie.services.ContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.wyona.katie.models.Question;
import com.wyona.katie.integrations.matrix.MatrixMessageSender;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.QuestionAnsweringService;

import io.swagger.annotations.ApiOperation;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.AccessDeniedException;

/**
 * Matrix controller (https://matrix.org/)
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/matrix") 
public class MatrixController {

    @Autowired
    private QuestionAnsweringService qaService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private MatrixMessageSender messageSender;

    /**
     * REST interface to sync with the latest state on the configured homeserver
     * https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-sync
     */
    @RequestMapping(value = "/sync", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Synchronise Katie's state with the latest state on the Matrix homeserver")
    public ResponseEntity<?> sync(HttpServletRequest request) {

        try {
            Question[] detectedQuestions = messageSender.syncWithHomeserver();

            return new ResponseEntity<>(detectedQuestions, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "TODO"), HttpStatus.BAD_REQUEST);
        }
    }
}
