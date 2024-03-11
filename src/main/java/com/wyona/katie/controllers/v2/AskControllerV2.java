package com.wyona.katie.controllers.v2;

import com.wyona.katie.models.Error;
import com.wyona.katie.controllers.v1.AskController;
import com.wyona.katie.models.*;
import com.wyona.katie.services.AuthenticationService;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.QuestionAnsweringService;
import com.wyona.katie.services.RememberMeService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//import org.hibernate.validator.constraints.NotEmpty;
//import javax.validation.constraints.NotEmpty;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Controller to ask questions (Version 2)
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v2") 
public class AskControllerV2 {

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private QuestionAnsweringService qaService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private RememberMeService rememberMeService;

    @Autowired
    public AskControllerV2() {
    //public AskControllerV2(EmailValidation emailValidation) {
    }

    /**
     * REST interface to get answer(s) for a particular question
     */
    @RequestMapping(value = "/ask", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get answer(s) for a previously asked and answered question. If no answer is available, then an empty array will be returned.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = ResponseAnswer.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Bad Request", response = Error.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = Error.class)
    })
    @ApiImplicitParams({
    @ApiImplicitParam(name = "Authorization", value = "Bearer JWT", 
                      required = false, dataType = "string", paramType = "header") })
    public ResponseEntity<?> getAnswer(
            //public ResponseEntity<Answer, AnswerError> getAnswer(
            @ApiParam(name = "question", value = "Question, e.g. 'What is the highest mountain of the world?'", required = true)
            @RequestParam(value = "question", required = true) String question,
            @ApiParam(name = "classification", value = "Classification filter, e.g. 'gravel bike' or 'mountain bike'", required = false)
            @RequestParam(value = "classification", required = false) String classification,
            @ApiParam(name = "questionerLanguage", value = "Language code of user asking question, e.g. 'en' or 'de'", required = false)
            @RequestParam(value = "questionerLanguage", required = false) String questionerLanguage,
            @ApiParam(name = "email", value = "Email address of user asking question (e.g. 'louise@wyona.com'), such that user can be notified by email when answer has been found",required = false)
            @RequestParam(value = "email", required = false) String email,
            @ApiParam(name = "fcm_token", value = "Firebase Cloud Messaging token associated with mobile device of user asking question, such that a push notification can be sent when answer has been found",required = false)
            @RequestParam(value = "fcm_token", required = false) String fcmToken,
            @ApiParam(name = "answer_link_type", value = "Answer link type, for example 'deeplink', which is used for linking answer when user is being notified either by email or push notification",required = false)
            @RequestParam(value = "answer_link_type", required = false) String answerLinkType,
            @ApiParam(name = "domainId", value = "Domain Id, for example 'wyona', which represents a single realm containing its own set of questions/answers, etc.",required = false)
            @RequestParam(value = "domainId", required = false) String domainId,
            @ApiParam(name = "limit", value = "Pagination: Limit the number of returned answers, e.g. return 10 answers", required = false)
            @RequestParam(value = "limit", required = false) Integer limit,
            @ApiParam(name = "offset", value = "Pagination: Offset indicates the start of the returned answers, e.g. 0 for starting with the answer with the best ranking, whereas 0 also the default", required = false)
            @RequestParam(value = "offset", required = false) Integer offset,
            @ApiParam(name = "accept-content-type", value = "Content type of answer accepted by client, e.g. 'text/plain' or 'text/x.topdesk-html', whereas default is 'text/html'", required = false)
            @RequestParam(value = "accept-content-type", required = false) ContentType acceptContentType,
            HttpServletRequest request, HttpServletResponse response) {

        Language questionerLang = null;
        if (questionerLanguage != null) {
            questionerLang = Language.valueOf(questionerLanguage);
        }

        List<String> classifications = new ArrayList<String>();
        if (classification != null) {
            String[] features = classification.split(",");
            for (String feature : features) {
                String _feature = feature.toLowerCase().trim();
                if (!_feature.isEmpty()) {
                    classifications.add(_feature);
                }
            }
        }

        String messageId = null; // TODO

        return getAnswers(question, classifications, messageId, questionerLang, acceptContentType, email, fcmToken, answerLinkType, domainId, limit, offset, request, response);

    }

    /**
     * REST interface to get answer(s) for a particular question
     */
    @RequestMapping(value = "/ask/{domain-id}", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Get answer(s) for a previously asked and answered question. If no answer is available, then an empty array will be returned.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = ResponseAnswer.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Bad Request", response = Error.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = Error.class)
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataType = "string", paramType = "header") })
    public ResponseEntity<?> postQuestion(
            @ApiParam(name = "domain-id", value = "Domain Id of knowledge base, for example 'b3158772-ac8f-4ec1-a9d7-bd0d3887fd9b', which contains its own set of questions/answers",required = true)
            @PathVariable(value = "domain-id", required = true) String domainId,
            @ApiParam(name = "question-and-contact-info", value = "The 'question' field is required, all other fields are optional, like for example classification (comma separated classifications), language of questioner or contact information in case Katie does not know the answer and a human expert can send an answer to questioner", required = true)
            @RequestBody AskQuestionBody questionAndContact,
            HttpServletRequest request,
            HttpServletResponse response) {

        String question = questionAndContact.getQuestion();
        String messageId  = questionAndContact.getMessageId();

        Language questionerLanguage = questionAndContact.getQuestionerLanguage();
        String email = null;
        String fcmToken = null;
        String answerLinkType = null;
        String webhookEchoContent = null;
        if (questionAndContact.getOptionalContactInfo() != null) {
            email = questionAndContact.getOptionalContactInfo().getEmail();
            fcmToken = questionAndContact.getOptionalContactInfo().getFcmToken();
            answerLinkType = questionAndContact.getOptionalContactInfo().getAnswerLinkType();

            log.warn("TODO: Implement webhookEchoContent ...");
            webhookEchoContent = questionAndContact.getOptionalContactInfo().getWebhookEchoData();
        }

        int limit = 2; // TODO
        int offset = 0; // TODO

        List<String> classifications = new ArrayList<String>();
        if (questionAndContact.getClassification() != null) {
            String[] features = questionAndContact.getClassification().split(",");
            for (String feature : features) {
                String _feature = feature.toLowerCase().trim();
                if (!_feature.isEmpty()) {
                    classifications.add(_feature);
                }
            }
        }

        ContentType answerContentType = null;
        if (questionAndContact.getAcceptContentType() != null) {
            answerContentType = ContentType.fromString(questionAndContact.getAcceptContentType());
            if (answerContentType == null) {
                log.error("No such content type '" + questionAndContact.getAcceptContentType() + "' supported!");
            }
        }

        return getAnswers(question, classifications, messageId, questionerLanguage, answerContentType, email, fcmToken, answerLinkType, domainId, limit, offset, request, response);
    }

    /**
     * Get answer(s) to question
     */
    private ResponseEntity<?> getAnswers(String question,
                                         List<String> classifications,
                                         String messageId,
                                         Language questionerLanguage,
                                         ContentType answerContentType,
                                         String email,
                                         String fcmToken,
                                         String answerLinkType,
                                         String domainId,
                                         Integer limit,
                                         Integer offset,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        rememberMeService.tryAutoLogin(request, response);

        // INFO: Validate whether a question was submitted
        if (question.isEmpty()) {
            return new ResponseEntity<>(new Error("Question may not be empty", "QUESTION_MAY_NOT_BE_EMPTY"), HttpStatus.BAD_REQUEST);
        }

        // INFO: Validate answer link type value in case one was submitted
        if (answerLinkType != null) {
            if (!answerLinkType.equals(ResubmittedQuestion.DEEP_LINK)) {
                return new ResponseEntity<>(new Error("No such answer link type '" + answerLinkType + "'!", "NO_SUCH_ANSWER_LINK_TYPE"), HttpStatus.BAD_REQUEST);
            }
        }

        // INFO: Validate domain id in case one was submitted
        if (domainId != null) {
            if (!contextService.existsContext(domainId)) {
                return new ResponseEntity<>(new Error("No such domain Id '" + domainId + "'!", "NO_SUCH_DOMAIN_ID"), HttpStatus.BAD_REQUEST);
            }
        }
        Context domain = null;
        try {
            domain = contextService.getContext(domainId); // INFO: getContext() return ROOT context when domain id is null
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "NO_SUCH_DOMAIN_ID"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Date dateSubmitted = new Date();

        if (questionerLanguage != null) {
            log.info("Questioner language: " + questionerLanguage);
        } else {
            log.info("No questioner language set.");
        }

        // INFO: Answer by AI was not helpful, therefore user submitted email or FCM token in order to get a better answer by a human
        if (email != null) {
            log.info("Submitted email '" + email + "'");
            if (email.isEmpty()) {
                String errMsg = "Parameter 'email' submitted, but without value!";
                log.warn(errMsg);
                return new ResponseEntity<>(new Error(errMsg, "NO_EMAIL_VALUE"), HttpStatus.BAD_REQUEST);
            } else {
                // TODO: Check whether email is valid
                // INFO: If email is submitted, then user should not be signed in and/or does not have a user account
                rememberMeService.rememberEmail(email, request, response, domainId);
                contextService.answerQuestionByNaturalIntelligence(question, null, ChannelType.EMAIL, null, email, null, questionerLanguage, answerLinkType, AskController.getRemoteAddress(request), domain);
                // TODO: Consider different response
                return new ResponseEntity<>(new ResponseAnswer(null, question, dateSubmitted, null, null, new ArrayList<String>(), null, null, email, null, null, PermissionStatus.UNKNOWN, null, null), HttpStatus.OK);
            }
        } else if (fcmToken != null) {
            log.info("Submitted FCM token '" + fcmToken + "'");
            if (fcmToken.isEmpty()) {
                String errMsg = "Parameter 'fcm_token' submitted, but without value!";
                log.warn(errMsg);
                return new ResponseEntity<>(new Error(errMsg, "NO_FCM_TOKEN_VALUE"), HttpStatus.BAD_REQUEST);
            } else {
                contextService.answerQuestionByNaturalIntelligence(question, null, ChannelType.FCM_TOKEN, null, null, fcmToken, questionerLanguage, answerLinkType, AskController.getRemoteAddress(request), domain);
                // TODO: return FCM token value
                return new ResponseEntity<>(new ResponseAnswer(null, question, dateSubmitted, null, null, new ArrayList<String>(), null, null, null, null, null, PermissionStatus.UNKNOWN, null, null), HttpStatus.OK);
            }
        }

        try {
            int _limit = DefaultValues.NO_LIMIT_PROVIDED;
            if (limit != null) {
                _limit = limit.intValue();
            }
            int _offset = DefaultValues.NO_OFFSET_PROVIDED;
            if (offset != null) {
                _offset = offset.intValue();
            }

            boolean includeFeedbackLinks = false;
            java.util.List<ResponseAnswer> responseAnswers = qaService.getAnswers(question, classifications, messageId, domain, dateSubmitted, AskController.getRemoteAddress(request), ChannelType.UNDEFINED, null, _limit, _offset, true, answerContentType, includeFeedbackLinks);

            if (responseAnswers != null) {
                if (responseAnswers.size() == 0 && !contextService.hasTrainedQnAs(domain)) {
                    // INFO: Knowledge base empty means, that there are neither QnAs nor any Knowledge graph items
                    log.info("Knowledge base of domain '" + domainId + "' is empty.");
                    // WARN: A WAF / Firewall might rewrite the response and might drop custom codes like for example  "KNOWLEDGE_BASE_EMPTY", which means the frontend client cannot rely on custom codes
                    return new ResponseEntity<>(new Error("Knowledge base is empty", "KNOWLEDGE_BASE_EMPTY"), HttpStatus.NOT_FOUND);
                }
                return new ResponseEntity<>(responseAnswers, HttpStatus.OK);
            } else {
                log.error("No answers!");
                return new ResponseEntity<>(new Error("Unknown error", "UNKNOWN_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (AccessDeniedException e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "UNKNOWN_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
