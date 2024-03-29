package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.*;
import com.wyona.katie.models.Error;
import com.wyona.katie.services.AuthenticationService;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.RememberMeService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.AccessDeniedException;
import java.util.Date;

/**
 * Controller to submit feedback (e.g. rate answers and rate predicted labels) and to get saved ratings
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/feedback")
public class FeedbackController {

    @Autowired
    private ContextService domainService;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private RememberMeService rememberMeService;

    // TODO: Rate answer, move QuestionController#rateAnswer()
    // TODO: Rate predicted labels
    // TODO: Get ratings of answers (Preference dataset), move QuestionsController#getRatingsOfAnswersToAskedQuestions()
    // TODO: Get ratings of predicted labels (Preference dataset)

    /**
     * REST interface such that user can rate predicted labels
     */
    @RequestMapping(value = "/rate-predicted-labels", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Rate predicted labels")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataType = "string", paramType = "header") })
    public ResponseEntity<?> ratePredictedLabels(
            @ApiParam(name = "rating", value = "Rating of predicted labels", required = true)
            @RequestBody RatingPredictedLabels rating,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        log.info("Rate predicted labels returned for request '" + rating.getRequestuuid() + "' ...");


        if (rating.getFeedback() != null && !rating.getFeedback().isEmpty()) {
            log.info("Received optional feedback: " + rating.getFeedback());
            int FEEDBACK_MAX_LENGTH = 150;
            if (rating.getFeedback().length() > FEEDBACK_MAX_LENGTH){
                String msg = "Rating feedback out of bounds (more than " + FEEDBACK_MAX_LENGTH + " characters): " + rating.getFeedback();
                log.error(msg);
                return new ResponseEntity<>(new Error(msg, "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
            }
        } else {
            log.info("No optional feedback received.");
        }

        if (rating.getEmail() != null && !rating.getEmail().isEmpty()) {
            log.info("User provided email: " + rating.getEmail());
            rememberMeService.rememberEmail(rating.getEmail(), request, response, rating.getDomainid());
        }

        try {
            Context domain = domainService.getContext(rating.getDomainid());
            domainService.ratePredictedLabels(domain, rating);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }
}
