package com.wyona.katie.controllers.v3;

import com.wyona.katie.models.Error;
import com.wyona.katie.controllers.v1.AskController;
import com.wyona.katie.models.*;
import com.wyona.katie.services.*;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
 * Controller to ask questions (Version 3)
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v3") 
public class AskControllerV3 {

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private QuestionAnsweringService qaService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private RememberMeService rememberMeService;

    @Autowired
    private QuestionAnalyzerService questionAnalyzerService;

    /**
     * REST interface to get answer(s) to a question
     */
    @RequestMapping(value = "/ask/{domain-id}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value="Get answer(s) to a question")
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
            @ApiParam(name = "limit", value = "Pagination: Limit the number of returned answers, e.g. return 10 answers", required = false)
            @RequestParam(value = "limit", required = false) Integer limit,
            @ApiParam(name = "offset", value = "Pagination: Offset indicates the start of the returned answers, e.g. 0 for starting with the answer with the best ranking, whereas 0 also the default", required = false)
            @RequestParam(value = "offset", required = false) Integer offset,
            @ApiParam(name = "question-and-optional-params", value = "The 'question' field is required, all other fields are optional, like for example content type accepted by client (e.g. 'text/plain' or 'text/x.topdesk-html', whereas default is 'text/html'), classification (one) resp. classifications (multiple), language of questioner, message Id of client which sent question to Katie, or contact information in case Katie does not know the answer and a human expert can send an answer to questioner", required = true)
            @RequestBody AskQuestionBody questionAndOptionalParams,
            @ApiParam(name = "include-feedback-links", value = "When true, then answer contains feedback links", required = false)
            @RequestParam(value = "include-feedback-links", required = false) Boolean includeFeedbackLinks,
            HttpServletRequest request,
            HttpServletResponse response) {

        String question = questionAndOptionalParams.getQuestion();
        String messageId = questionAndOptionalParams.getMessageId();

        Language questionerLanguage = questionAndOptionalParams.getQuestionerLanguage();
        String email = null;
        String fcmToken = null;
        String answerLinkType = null;
        String webhookEchoContent = null;
        if (questionAndOptionalParams.getOptionalContactInfo() != null) {
            email = questionAndOptionalParams.getOptionalContactInfo().getEmail();
            fcmToken = questionAndOptionalParams.getOptionalContactInfo().getFcmToken();
            answerLinkType = questionAndOptionalParams.getOptionalContactInfo().getAnswerLinkType();

            log.warn("TODO: Implement webhookEchoContent ...");
            webhookEchoContent = questionAndOptionalParams.getOptionalContactInfo().getWebhookEchoData();
        }

        Integer _limit = 2;
        if (limit != null) {
            _limit = limit;
        }
        Integer _offset = 0;
        if (offset != null) {
            _offset = offset;
        }
        boolean _includeFeedbackLinks = false;
        if (includeFeedbackLinks != null) {
            _includeFeedbackLinks = includeFeedbackLinks;
        }

        List<String> classifications = new ArrayList<String>();
        String[] cfs = questionAndOptionalParams.getClassifications();
        if (cfs != null && cfs.length > 0) {
            for (String classification : cfs) {
                if (!classification.trim().isEmpty()) {
                    classifications.add(classification);
                }
            }
        }
        if (questionAndOptionalParams.getClassification() != null && !questionAndOptionalParams.getClassification().trim().isEmpty()) {
            classifications.add(questionAndOptionalParams.getClassification());
        }

        ContentType answerContentType = null;
        if (questionAndOptionalParams.getAcceptContentType() != null) {
            answerContentType = ContentType.fromString(questionAndOptionalParams.getAcceptContentType());
            if (answerContentType == null) {
                log.error("No such content type '" + questionAndOptionalParams.getAcceptContentType() + "' supported!");
            }
        }

        return getAnswers(question, classifications, questionAndOptionalParams.getPredictClassifications(), messageId, questionerLanguage, answerContentType, email, fcmToken, answerLinkType, domainId, _limit, _offset, _includeFeedbackLinks, questionAndOptionalParams.getIncludeClassifications(), request, response);
    }

    /**
     * Get answer(s) to a question / message
     * @param question Asked question / sent message
     * @param classifications Provided classifications / labels to narrow down search / answer space
     * @param predictClassifications Truw when Katie should predict classifications based on asked question / sent message
     * @param answerContentType Content type of answer accepted by client, e.g. "text/plain" resp. ContentType.TEXT_PLAIN
     * @param includeFeedbackLinks When true, then include feedback links into answers
     * @param includeClassifications When true, then include classifications of answers and predicted classifications into answers
     */
    private ResponseEntity<?> getAnswers(String question,
                                         List<String> classifications,
                                         boolean predictClassifications,
                                         String messageId,
                                         Language questionerLanguage,
                                         ContentType answerContentType,
                                         String email,
                                         String fcmToken,
                                         String answerLinkType,
                                         String domainId,
                                         Integer limit,
                                         Integer offset,
                                         boolean includeFeedbackLinks,
                                         boolean includeClassifications,
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

            String channelRequestId = java.util.UUID.randomUUID().toString();
            // TODO: Add REST conversation values
            //dataRepoService.addRestConversationValues(channelRequestId, "POST", "V3", AskController.getRemoteAddress(request));

            if (isEmail(question)) {
                log.info("Question / Message seems to be an email ...");
                // TODO: And what now?!
            }

            if (domain.getAnalyzeMessagesAskRestApi()) {
                AnalyzedMessage analyzedMessage = questionAnalyzerService.analyze(question, domain, QuestionClassificationImpl.OPEN_NLP);
                if (analyzedMessage.getContainsQuestions()) {
                    log.info("Message '" + question + "' contains question");
                    List<QuestionContext> questionContexts = analyzedMessage.getQuestionsAndContexts();
                    for (QuestionContext qc : questionContexts) {
                        log.info("Question: " + qc.getQuestion());
                    }
                } else {
                    log.warn("Message '" + question + "' does not seem to contain a question.");
                }

                question = analyzedMessage.getMessage();
            }

            // TODO: Return AskResponse (including meta info) instead list of ResponseAnswer
            java.util.List<ResponseAnswer> responseAnswers = qaService.getAnswers(question, predictClassifications, classifications, messageId, domain, dateSubmitted, AskController.getRemoteAddress(request), ChannelType.UNDEFINED, channelRequestId, _limit, _offset, true, answerContentType, includeFeedbackLinks, includeClassifications);

            if (responseAnswers != null) {
                String questionUUID = null;
                if (responseAnswers.size() > 0) {
                    responseAnswers.get(0).getQuestionUUID(); // TODO: Do not get from response answers
                }
                AskResponse askResponse = new AskResponse(question, questionUUID, classifications, domain.getDetectDuplicatedQuestionImpl(), domain.getEmbeddingsImpl(), domain.getVectorSimilarityMetric());
                askResponse.setAnswers(responseAnswers);
                askResponse.setOffset(_offset);
                askResponse.setLimit(_limit);
                //askResponse.setPredictedLabels(predictedLabels);

                // TODO: This is a hack, which should be improved! When there are no answers, then there still should be a question UUID!
                if (responseAnswers.size() > 0) {
                    askResponse.setQuestionUUID(responseAnswers.get(0).getQuestionUUID());
                }

                if (responseAnswers.size() == 0 && !contextService.hasTrainedQnAs(domain)) {
                    askResponse.setKnowledgeBaseEmpty(true);
                } else {
                    askResponse.setKnowledgeBaseEmpty(false);
                }
                return new ResponseEntity<>(askResponse, HttpStatus.OK);
            } else {
                log.error("No answers!");
                return new ResponseEntity<>(new Error("Unknown error", "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (AccessDeniedException e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "UNKNOWN_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     *
     */
    private boolean isEmail(String text) {
        if (text.contains("Sender:") || text.contains("Subject:") || text.contains("Date sent:")) {
            return true;
        } else {
            return false;
        }
    }
}
