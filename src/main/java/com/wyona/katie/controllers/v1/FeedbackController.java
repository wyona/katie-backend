package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.*;
import com.wyona.katie.models.Error;
import com.wyona.katie.services.AuthenticationService;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.RememberMeService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.AccessDeniedException;
import java.util.Date;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

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

    // TODO: Rate QnA, move QuestionController#rateQnA()

    /**
     * REST interface such that user can rate received answer
     */
    @RequestMapping(value = "/{domainid}/{uuid}/rate-answer", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary = "Rate answer to question")
    @Parameter(
            name = "Authorization",
            description = "Bearer JWT",
            required = false,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string")
    )
    public ResponseEntity<?> rateAnswer(
            @Parameter(name = "uuid", description = "UUID of asked question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("uuid") String quuid,
            @Parameter(name = "domainid", description = "Domain Id question and answer is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("domainid") String domainid,
            @Parameter(name = "rating", description = "User rating re replied answer, between 0 and 10, whereas 0 means not helpful and 10 means very helpful", required = true)
            @RequestBody Rating rating,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        rating.setQuestionuuid(quuid);
        log.info("Rate answer to the asked question '" + quuid + "' ...");

        if (rating.getRating() < 0 || rating.getRating() > 10) {
            return new ResponseEntity<>(new Error("Rating '" + rating.getRating() + "' out of bounds!", "RATING_OUT_OF_BOUNDS"), HttpStatus.BAD_REQUEST);
        }

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
            rememberMeService.rememberEmail(rating.getEmail(), request, response, domainid);
        }

        try {
            AskedQuestion askedQuestion = domainService.getAskedQuestionByUUID(quuid);

            // INFO: We don't need the user question from the rating object, because we already have it using the question UUID
            rating.setUserquestion(askedQuestion.getQuestion());
            log.info("User question: " + rating.getUserquestion());
            if (rating.getUserquestion() != null && rating.getUserquestion().length() > 300) {
                log.warn("User question more than 300 characters, therefore shorten question ...");
                rating.setUserquestion(rating.getUserquestion().substring(0, 299));
            }

            rating.setDate(new Date());
            rating.setQnauuid(askedQuestion.getQnaUuid());

            Context domain = domainService.getContext(domainid);
            Answer answer = domainService.rateAnswer(domain, rating);
            return new ResponseEntity<>(answer, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get ratings of answers to asked questions (as preference dataset)
     * TODO: Filtering, Sorting, Pagination: https://www.moesif.com/blog/technical/api-design/REST-API-Design-Filtering-Sorting-and-Pagination/
     */
    @RequestMapping(value = "/ratings-of-answers", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary = "Get ratings of answers to asked questions (as preference dataset)")
    public ResponseEntity<?> getRatingsOfAnswersToAskedQuestions(
            @Parameter(name = "domain-id", description = "Domain Id associated with asked questions (e.g. 'wyona' or 'ROOT')", required = true)
            @RequestParam(value = "domain-id", required = true) String domainId,
            @Parameter(name = "limit", description = "Pagination: Limit the number of returned ratings", required = true, schema = @Schema(defaultValue = "10"))
            @RequestParam(value = "limit", required = true) int limit,
            @Parameter(name = "offset", description = "Pagination: Offset indicates the start of the returned ratings", required = true, schema = @Schema(defaultValue = "0"))
            @RequestParam(value = "offset", required = true) int offset,
            HttpServletRequest request, HttpServletResponse response) {

        log.info("Get all ratings ...");

        rememberMeService.tryAutoLogin(request, response);

        try {
            if (domainService.isMemberOrAdmin(domainId)) {

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

                HumanPreferenceAnswer[] preferences = domainService.getRatingsOfAnswers(domainId);
                return new ResponseEntity<>(preferences, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new com.wyona.katie.models.Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface such that user can rate predicted labels
     */
    @RequestMapping(value = "/rate-predicted-labels", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary = "Rate predicted labels")
    @Parameter(
            name = "Authorization",
            description = "Bearer JWT",
            required = false,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string")
    )
    public ResponseEntity<?> ratePredictedLabels(
            @Parameter(name = "rating", description = "Rating of predicted labels", required = true)
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

            // TODO: Measure time spent above and if very fast, then delay response artificially such that frontend spinner can be seen
            // TEST: Uncomment lines below to test frotend spinner
            /*
            try {
                for (int i = 0; i < 2; i++) {
                    log.info("Sleep for 2 seconds ...");
                    Thread.sleep(2000);
                }
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
             */

            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get ratings of predicted labels (as preference dataset)
     * TODO: Filtering, Sorting, Pagination: https://www.moesif.com/blog/technical/api-design/REST-API-Design-Filtering-Sorting-and-Pagination/
     */
    @RequestMapping(value = "/ratings-of-predicted-labels", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary = "Get ratings of predicted labels (as preference dataset)")
    public ResponseEntity<?> getRatingsOfPredictedLabels(
            @Parameter(name = "domain-id", description = "Domain Id associated with asked questions (e.g. 'wyona' or 'ROOT')", required = true)
            @RequestParam(value = "domain-id", required = true) String domainId,
            @Parameter(name = "get-chosen", description = "Labels which were predicted correctly", required = false, schema = @Schema(defaultValue = "true"))
            @RequestParam(value = "get-chosen", required = false) Boolean getChosen,
            @Parameter(name = "get-rejected", description = "Labels which were rejected respectively not predicted correctly", required = false, schema = @Schema(defaultValue = "true"))
            @RequestParam(value = "get-rejected", required = false) Boolean getRejected,
            @Parameter(name = "limit", description = "Pagination: Limit the number of returned ratings", required = true, schema = @Schema(defaultValue = "10"))
            @RequestParam(value = "limit", required = true) int limit,
            @Parameter(name = "offset", description = "Pagination: Offset indicates the start of the returned ratings", required = true, schema = @Schema(defaultValue = "0"))
            @RequestParam(value = "offset", required = true) int offset,
            HttpServletRequest request, HttpServletResponse response) {

        log.info("Get all ratings ...");

        rememberMeService.tryAutoLogin(request, response);

        try {
            if (domainService.isMemberOrAdmin(domainId)) {

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

                HumanPreferenceLabel[] preferences = domainService.getRatingsOfPredictedLabels(domainId, getChosen, getRejected);
                return new ResponseEntity<>(preferences, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new com.wyona.katie.models.Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Delete label rating
     */
    @RequestMapping(value = "/{domain-id}/rating-label/{rating-id}", method = RequestMethod.DELETE, produces = "application/json")
    @Operation(summary="Delete label rating")
    public ResponseEntity<?> deleteLabelRating(
            @Parameter(name = "domain-id", description = "Domain Id",required = true)
            @PathVariable(value = "domain-id", required = true) String domainId,
            @Parameter(name = "rating-id", description = "Rating Id",required = true)
            @PathVariable(value = "rating-id", required = true) String ratingId,
            HttpServletRequest request) {

        if (!domainService.existsContext(domainId)) {
            return new ResponseEntity<>(new Error("Domain '" + domainId + "' does not exist!", "NOT_FOUND"), HttpStatus.NOT_FOUND);
        }

        try {
            boolean deleted = domainService.deleteLabelRating(domainId, ratingId);
            if (deleted) {
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                String errMsg = "Label rating '" + ratingId + "' did not get deleted.";
                log.error(errMsg);
                return new ResponseEntity<>(new Error(errMsg, "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
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
     * Toggle approval of a label rating
     */
    @RequestMapping(value = "/{domain-id}/rating-label/{rating-id}/approve-disapprove", method = RequestMethod.PUT, produces = "application/json")
    @Operation(summary="Toggle approval of a label rating")
    public ResponseEntity<?> toggleApprovalOfLabelRating(
            @Parameter(name = "domain-id", description = "Domain Id",required = true)
            @PathVariable(value = "domain-id", required = true) String domainId,
            @Parameter(name = "rating-id", description = "Rating Id",required = true)
            @PathVariable(value = "rating-id", required = true) String ratingId,
            HttpServletRequest request) {

        if (!domainService.existsContext(domainId)) {
            return new ResponseEntity<>(new Error("Domain '" + domainId + "' does not exist!", "NOT_FOUND"), HttpStatus.NOT_FOUND);
        }

        try {
            log.info("Toggle approval of a label rating '" + ratingId + "'");
            boolean isApproved = domainService.toggleApprovalOfLabelRating(domainId, ratingId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
