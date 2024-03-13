package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.Error;
import com.wyona.katie.models.Username;
import com.wyona.katie.services.*;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//import org.hibernate.validator.constraints.NotEmpty;
//import javax.validation.constraints.NotEmpty;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.nio.file.AccessDeniedException;
import java.util.*;

/**
 * Controller to ask questions (Version 1)
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1") 
public class AskController {

    @Autowired
    private ResourceBundleMessageSource messageSource;

    private QuestionAnsweringService qaService;
    private IAMService iamService;
    private RememberMeService rememberMeService;
    private AuthenticationService authService;
    private DataRepositoryService dataRepoService;
    private ContextService contextService;

    @Autowired
    private QuestionAnalyzerService questionAnalyzerService;

    @Autowired
    public AskController(QuestionAnsweringService qaService, IAMService iamService, RememberMeService rememberMeService, AuthenticationService authService, DataRepositoryService dataRepoService, ContextService contextService) {
        this.qaService = qaService;
        this.iamService = iamService;
        this.rememberMeService = rememberMeService;
        this.authService = authService;
        this.dataRepoService = dataRepoService;
        this.contextService = contextService;

        //this.emailValidation = emailValidation;
    }

    /**
     * REST interface to generate a fake / synthetic answer for a particular question, which can be used to search for the actual answer
     */
    @RequestMapping(value = "/fake-answer/{domain-id}", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Generate fake / synthetic answer")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = ResponseAnswer.class),
            @ApiResponse(code = 403, message = "Forbidden", response = Error.class),
            @ApiResponse(code = 400, message = "Bad Request", response = Error.class)
    })
    public ResponseEntity<?> getFakeAnswer(
            //public ResponseEntity<ResponseAnswer> getAnswer(
            @ApiParam(name = "question", value = "Question, e.g. 'What is the highest mountain of the world?'",required = true)
            @RequestParam(value = "question", required = true) String question,
            @ApiParam(name = "questionerLanguage", value = "Language code of user asking question, e.g. 'en' or 'de'", required = false)
            @RequestParam(value = "questionerLanguage", required = false) String questionerLanguage,
            @ApiParam(name = "domain-id", value = "Domain Id of knowledge base, for example 'b3158772-ac8f-4ec1-a9d7-bd0d3887fd9b', which contains its own set of questions/answers",required = true)
            @PathVariable(value = "domain-id", required = true) String domainId,
            HttpServletRequest request,
            HttpServletResponse response) {

        Context domain = null;
        try {
            if (domainId != null) {
                domain = contextService.getContext(domainId);
            } else {
                domain = contextService.getContext("ROOT");
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "NO_SUCH_DOMAIN_ID"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Language questionerLang = null;
        if (questionerLanguage != null) {
            questionerLang = Language.valueOf(questionerLanguage);
            log.info("Questioner language: " + questionerLang);
        } else {
            log.info("No questioner language set.");
        }

        // TODO
        List<String> classifications = new ArrayList<String>();

        // TODO: Analyze question
        Sentence analyzedQuestion = new Sentence(question, null, classifications);

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode body = mapper.createObjectNode();
            body.put("fake-answer", contextService.getFakeAnswer(analyzedQuestion, domain));

            return new ResponseEntity<>(body.toString(), HttpStatus.OK);
        } catch (AccessDeniedException e) {
            log.warn(e.getMessage(), e);
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * REST interface to get an answer for a particular question
     */
    @RequestMapping(value = "/ask", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Ask question and get answer of a previously asked duplicated question. If no answer is available, then the uuid and answer field of the response body will be null.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = ResponseAnswer.class),
            @ApiResponse(code = 403, message = "Forbidden", response = Error.class),
            @ApiResponse(code = 400, message = "Bad Request", response = Error.class)
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataType = "string", paramType = "header") })
    public ResponseEntity<?> getAnswer(
    //public ResponseEntity<ResponseAnswer> getAnswer(
        @ApiParam(name = "question", value = "Question, e.g. 'What is the highest mountain of the world?'",required = true)
        @RequestParam(value = "question", required = true) String question,
        @ApiParam(name = "questionerLanguage", value = "Language code of user asking question, e.g. 'en' or 'de'", required = false)
        @RequestParam(value = "questionerLanguage", required = false) String questionerLanguage,
        @ApiParam(name = "email", value = "Email address of user asking question (e.g. 'louise@wyona.com'), such that user can be notified by email when an expert has answered the question",required = false)
        @RequestParam(value = "email", required = false) String email,
        @ApiParam(name = "fcm_token", value = "Firebase Cloud Messaging token associated with mobile device of user asking question, such that a push notification can be sent when an expert has answered the question",required = false)
        @RequestParam(value = "fcm_token", required = false) String fcmToken,
        @ApiParam(name = "webhook_echo_content", value = "Content which is echoed back by webhook(s), in case webhook(s) configured for the given domain Id ",required = false)
        @RequestParam(value = "webhook_echo_content", required = false) String webhookEchoContent,
        @ApiParam(name = "answer_link_type", value = "Answer link type. When value is set to 'deeplink' and as soon as expert will have answered question, then email or push notification to questioner will contain a deep link to answer, such that mobile app is opened and answer can be read within mobile app",required = false)
        @RequestParam(value = "answer_link_type", required = false) String answerLinkType,
        @ApiParam(name = "domainId", value = "Domain Id, for example 'wyona', which represents a single realm containing its own set of questions/answers. When no domain Id is set, then the ROOT domain Id will be used.",required = false)
        @RequestParam(value = "domainId", required = false) String domainId,
        HttpServletRequest request,
        HttpServletResponse response) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        Error inputError = validateInput(question, answerLinkType, domainId);
        if (inputError != null) {
            return new ResponseEntity<>(inputError, HttpStatus.BAD_REQUEST);
        }

        Context domain = null;
        try {
            if (domainId != null) {
                domain = contextService.getContext(domainId);
            } else {
                domain = contextService.getContext("ROOT");
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "NO_SUCH_DOMAIN_ID"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Date dateSubmitted = new Date();
        String remoteAddress = getRemoteAddress(request);

        Language questionerLang = null;
        if (questionerLanguage != null) {
            questionerLang = Language.valueOf(questionerLanguage);
            log.info("Questioner language: " + questionerLang);
        } else {
            log.info("No questioner language set.");
        }

        if (email != null || fcmToken != null || webhookEchoContent != null) {
            return answerByExpert(question, questionerLang, email, fcmToken, answerLinkType, webhookEchoContent, domain, dateSubmitted, remoteAddress, request, response);
        }

        // TODO: Get classifications from request, whereas force to lower case
        List<String> classifications = new ArrayList<String>();
        return answerByKatie(question, classifications, domain, dateSubmitted, remoteAddress);
    }

    /**
     * REST interface to get an answer for a particular question
     */
    @RequestMapping(value = "/ask/{domain-id}", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Ask question and get answer of a previously asked duplicated question. If no answer is available, then the uuid and answer field of the response body will be null.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = ResponseAnswer.class),
            @ApiResponse(code = 403, message = "Forbidden", response = Error.class),
            @ApiResponse(code = 400, message = "Bad Request", response = Error.class)
    })
    public ResponseEntity<?> postQuestion(
            @ApiParam(name = "domain-id", value = "Domain Id of knowledge base, for example 'b3158772-ac8f-4ec1-a9d7-bd0d3887fd9b', which contains its own set of questions/answers",required = true)
            @PathVariable(value = "domain-id", required = true) String domainId,
            @ApiParam(name = "question-and-contact-info", value = "The 'question' field is required, all other fields are optional, like for example classification (comma separated classifications), language of questioner or contact information in case Katie does not know the answer and a human expert can send an answer to questioner", required = true)
            @RequestBody AskQuestionBody questionAndContact,
            HttpServletRequest request,
            HttpServletResponse response) {

        String question = questionAndContact.getQuestion();

        Language questionerLanguage = questionAndContact.getQuestionerLanguage();
        String email = null;
        String fcmToken = null;
        String answerLinkType = null;
        String webhookEchoContent = null;
        if (questionAndContact.getOptionalContactInfo() != null) {
            email = questionAndContact.getOptionalContactInfo().getEmail();
            fcmToken = questionAndContact.getOptionalContactInfo().getFcmToken();
            answerLinkType = questionAndContact.getOptionalContactInfo().getAnswerLinkType();
            webhookEchoContent = questionAndContact.getOptionalContactInfo().getWebhookEchoData();
        }

        Error inputError = validateInput(question, answerLinkType, domainId);
        if (inputError != null) {
            return new ResponseEntity<>(inputError, HttpStatus.BAD_REQUEST);
        }

        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "NO_SUCH_DOMAIN_ID"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Date dateSubmitted = new Date();
        String remoteAddress = getRemoteAddress(request);

        if (email != null || fcmToken != null || webhookEchoContent != null) {
            return answerByExpert(question, questionerLanguage, email, fcmToken, answerLinkType, webhookEchoContent, domain, dateSubmitted, remoteAddress, request, response);
        }
        
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

        return answerByKatie(question, classifications, domain, dateSubmitted, remoteAddress);
    }

    /**
     *
     */
    private ResponseEntity<?> answerByKatie(String question, List<String> classifications, Context domain, Date dateSubmitted, String remoteAddress) {
        try {
            // TODO: Get limit and offset as request parameters
            String messageId = null; // TODO
            String channelRequestId = null; // TODO
            boolean includeFeedbackLinks = false;
            ContentType answerContentType = null;
            java.util.List<ResponseAnswer> responseAnswers = qaService.getAnswers(question, false, classifications, messageId, domain, dateSubmitted, remoteAddress, ChannelType.UNDEFINED, channelRequestId, 10, 0, true, answerContentType, includeFeedbackLinks, false);

            if (responseAnswers != null) {
                if (responseAnswers.size() > 0) {
                    if (responseAnswers.size() > 1) {
                        log.info("There are more answers available than just one, please use version 2 to receive all answers.");
                    }

                    ResponseAnswer responseAnswer = responseAnswers.get(0);

                    // TODO: Permission check on API level
                    if (responseAnswer.getPermissionStatus() == PermissionStatus.PERMISSION_DENIED) {
                        return new ResponseEntity<>(new Error("Access denied, authentication required", "ANSWER_IS_PROTECTED"), HttpStatus.UNAUTHORIZED);
                    } else if (responseAnswer.getPermissionStatus() == PermissionStatus.NOT_SUFFICIENT_PERMISSIONS_TO_READ_ANSWER) {
                        String username = authService.getUsername();
                        if (username != null) {
                            log.info("User '" + username + "' is asking question '" + question + "' ...");
                        } else {
                            log.info("Anonymous user is asking question '" + question + "' ...");
                        }
                        HashMap<String, String> properties = new HashMap<String, String>();
                        properties.put("username", username);
                        return new ResponseEntity<>(new Error("Access denied, authenticated as '" + username + "', but not sufficient permissions", "NOT_SUFFICIENT_PERMISSIONS_TO_READ_ANSWER", properties), HttpStatus.FORBIDDEN);
                    } else {
                        return new ResponseEntity<>(responseAnswer, HttpStatus.OK);
                    }
                } else {
                    log.info("No answer available for question '" + question + "'.");
                    ResponseAnswer responseAnswer = new ResponseAnswer(null, question, dateSubmitted, null, null, new ArrayList<String>(), null, null, null, null, null, PermissionStatus.UNKNOWN, null, null);
                    return new ResponseEntity<>(responseAnswer, HttpStatus.OK);
                }
            } else {
                log.error("Unknown error!");
                return new ResponseEntity<>(new Error("Unknown error", "UNKNOWN_ERROR"), HttpStatus.BAD_REQUEST);
            }
        } catch (AccessDeniedException e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "UNKNOWN_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * If answer by AI/Katie was not helpful, then user can submit question again together with the user's email address or FCM token of user's mobile device, in order to get a better answer by a human expert, either by email or by push notification
     * @param question Question resubmitted by user, because Katie's answer was not helpful
     * @param questionerLanguage Language of user asking question
     * @param email E-Mail of user, which (re)submitted question
     * @param fcmToken FCM token associated with user's device, which resubmitted question
     * @param webhookEchoContent Custom data associated with system (e.g. third-party proxy), which resubmitted question
     */
    private ResponseEntity<?> answerByExpert(String question, Language questionerLanguage, String email, String fcmToken, String answerLinkType, String webhookEchoContent, Context domain, Date dateSubmitted, String remoteAddress, HttpServletRequest request, HttpServletResponse response) {
        if (email != null) { // INFO: Check whether user submitted question together with email in order to get answer from expert by email
            log.info("Submitted email '" + email + "'");
            if (email.isEmpty()) {
                String errMsg = "Parameter 'email' submitted, but without value!";
                log.warn(errMsg);
                return new ResponseEntity<>(new Error(errMsg, "NO_EMAIL_VALUE"), HttpStatus.BAD_REQUEST);
            } else {
                // TODO: Check whether email is valid
                // INFO: If email is submitted, then user is probably not signed in and/or does not have a user account
                rememberMeService.rememberEmail(email, request, response, domain.getId());
                contextService.answerQuestionByNaturalIntelligence(question, null, ChannelType.EMAIL, null, email, null, questionerLanguage, answerLinkType, remoteAddress, domain);
                return new ResponseEntity<>(new ResponseAnswer(null, question, dateSubmitted, null, null, new ArrayList<String>(), null, null, email, null, null, PermissionStatus.UNKNOWN, null, null), HttpStatus.OK);
            }
        } else if (fcmToken != null) { // INFO: Check whether user submittedd question together with FCM token in order to get answer from expert by push notification to mobile device
            log.info("Submitted FCM token '" + fcmToken + "'");
            if (fcmToken.isEmpty()) {
                String errMsg = "Parameter 'fcm_token' submitted, but without value!";
                log.warn(errMsg);
                return new ResponseEntity<>(new Error(errMsg, "NO_FCM_TOKEN_VALUE"), HttpStatus.BAD_REQUEST);
            } else {
                contextService.answerQuestionByNaturalIntelligence(question, null, ChannelType.FCM_TOKEN, null,  null, fcmToken, questionerLanguage, answerLinkType, remoteAddress, domain);
                // TODO: return FCM token value
                return new ResponseEntity<>(new ResponseAnswer(null, question, dateSubmitted, null, null, new ArrayList<String>(), null, null, null, null, null, PermissionStatus.UNKNOWN, null, null), HttpStatus.OK);
            }
        } else if (webhookEchoContent != null) {
            log.info("Submitted webhook echo data '" + webhookEchoContent + "'");
            if (webhookEchoContent.isEmpty()) {
                String errMsg = "Parameter 'webhook_echo_content' submitted, but without value!";
                log.warn(errMsg);
                return new ResponseEntity<>(new Error(errMsg, "NO_WEBHOOK_ECHO_DATA_VALUE"), HttpStatus.BAD_REQUEST);
            } else {
                try {
                    Webhook[] webhooks = contextService.getWebhooks(domain.getId());
                    if (webhooks.length == 0) {
                        String errMsg = "No webhooks configured for domain '" + domain.getId() + "'!";
                        log.warn(errMsg);
                        return new ResponseEntity<>(new Error(errMsg, "NO_WEBHOOKS_CONFIGURED"), HttpStatus.BAD_REQUEST);
                    }
                } catch(Exception e) {
                    log.error(e.getMessage(), e);
                }
                String channelRequestId = null;
                try {
                    channelRequestId = dataRepoService.addWebhookEchoData(webhookEchoContent, domain.getId());
                } catch(Exception e) {
                    log.error(e.getMessage(), e);
                }
                String uuidResubmittedQuestion = contextService.answerQuestionByNaturalIntelligence(question, null, ChannelType.WEBHOOK, channelRequestId, null, null, questionerLanguage, null, remoteAddress, domain);
                return new ResponseEntity<>(new ResponseAnswer(null, question, dateSubmitted, null, null, new ArrayList<String>(), null, null, null, null, null, PermissionStatus.UNKNOWN, null, null), HttpStatus.OK);
            }
        }

        log.error("Neither email nor FCM token nor Webhook echo data provided!");
        return null;
    }

    /**
     *
     */
    private Error validateInput(String question, String answerLinkType, String domainId) {
        // INFO: Validate whether a question was submitted
        if (question.isEmpty()) {
            return new Error("Question may not be empty", "QUESTION_MAY_NOT_BE_EMPTY");
        }

        // INFO: Validate answer link type value in case one was submitted
        if (answerLinkType != null) {
            if (!answerLinkType.equals(ResubmittedQuestion.DEEP_LINK)) {
                return new Error("No such answer link type '" + answerLinkType + "'!", "NO_SUCH_ANSWER_LINK_TYPE");
            }
        }

        // INFO: Validate domain id in case one was submitted
        if (domainId != null) {
            if (!contextService.existsContext(domainId)) {
                log.error("No such domain with Id '" + domainId + "'!");
                return new Error(messageSource.getMessage("error.no.such.domain.id", null, Locale.ENGLISH) + " '" + domainId + "'!", "NO_SUCH_DOMAIN_ID");
            }
        }

        return null;
    }

    /**
     * REST interface to submit question (when signed in) to expert
     */
    @RequestMapping(value = "/submitQuestionToExpert", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Submit question (when signed in) to expert")
    public ResponseEntity<?> submitQuestionToExpert(
        @ApiParam(name = "question", value = "Question, e.g. 'What is the highest mountain of the world?'",required = true)
        @RequestParam(value = "question", required = true) String question,
        @ApiParam(name = "domainId", value = "Domain Id, for example 'wyona', which represents a single realm containing its own set of questions/answers, etc.",required = false)
        @RequestParam(value = "domainId", required = false) String domainId,
        HttpServletRequest request) {

        // INFO: Validate whether a question was submitted
        if (question.isEmpty()) {
            return new ResponseEntity<>(new Error("Question may not be empty", "QUESTION_MAY_NOT_BE_EMPTY"), HttpStatus.BAD_REQUEST);
        }

        String username = authService.getUsername();
        User user = iamService.getUserByUsername(new Username(username), false, false);
        log.info("Submit question '" + question + "' to expert ...");
        log.info("TODO: Check whether user '" + username + "' is authorized to submit question for this particular domain?!");
        
        Language language = Language.valueOf(user.getLanguage());

        Context domain = null;
        try {
            domain = contextService.getContext(domainId); // INFO: getContext() return ROOT context when domain id is null
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "NO_SUCH_DOMAIN_ID"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        contextService.answerQuestionByNaturalIntelligence(question, user, ChannelType.EMAIL, null, user.getEmail(), null, language, null, getRemoteAddress(request), domain);
        Date dateSubmitted = null;
        return new ResponseEntity<>(new ResponseAnswer(null, question, dateSubmitted, null, null, new ArrayList<String>(), null, null, user.getEmail(), null, null, PermissionStatus.UNKNOWN, null, null), HttpStatus.OK);
    }

    /**
     * REST interface to autocomplete taxonomy term / return suggestions
     */
    @RequestMapping(value = "/ask/{domain-id}/taxonomy/suggest", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Complete taxonomy term (e.g. birth) / return suggestions (e.g. birthdate, birthplace)")
    public ResponseEntity<?> completeTaxonomyEntry(
            @ApiParam(name = "domain-id", value = "Domain Id of knowledge base, for example 'b3158772-ac8f-4ec1-a9d7-bd0d3887fd9b', which contains its own set of questions/answers", required = true)
            @PathVariable(value = "domain-id", required = true) String domainId,
            @ApiParam(name = "incomplete-term", value = "Incomplete taxonomy term, e.g. 'birth'", required = true)
            @RequestParam(value = "incomplete-term", required = true) String incompleteTerm,
            HttpServletRequest request, HttpServletResponse response) {

        rememberMeService.tryAutoLogin(request, response);

        try {
            return new ResponseEntity<>(contextService.getSuggestedTaxonomyEntries(domainId, incompleteTerm), HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to classify a text
     */
    @RequestMapping(value = "/ask/{domain-id}/taxonomy/inference", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Classify a text")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataType = "string", paramType = "header") })
    public ResponseEntity<?> predictTaxonomyEntries(
            @ApiParam(name = "domain-id", value = "Domain Id of knowledge base, for example 'b3158772-ac8f-4ec1-a9d7-bd0d3887fd9b', which contains its own set of questions/answers", required = true)
            @PathVariable(value = "domain-id", required = true) String domainId,
            @ApiParam(name = "text", value = "Text to be classified, e.g. if the input text is 'Where was Michael born?', then the following classifications could be returned: birthplace, michael", required = true)
            @RequestParam(value = "text", required = true) String text,
            HttpServletRequest request, HttpServletResponse response) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        rememberMeService.tryAutoLogin(request, response);

        try {
            return new ResponseEntity<>(contextService.classifyText(domainId, text), HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to autocomplete question / return suggestions
     */
    @RequestMapping(value = "/ask/{domain-id}/autocomplete", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Complete question / return suggestions")
    public ResponseEntity<?> completeQuestion(
            @ApiParam(name = "domain-id", value = "Domain Id of knowledge base, for example 'b3158772-ac8f-4ec1-a9d7-bd0d3887fd9b', which contains its own set of questions/answers",required = true)
            @PathVariable(value = "domain-id", required = true) String domainId,
            @ApiParam(name = "question", value = "Incomplete question, e.g. 'highest moun'",required = true)
            @RequestParam(value = "question", required = true) String question,
            HttpServletRequest request, HttpServletResponse response) {

        rememberMeService.tryAutoLogin(request, response);

        try {
            return new ResponseEntity<>(contextService.getSuggestedQuestions(domainId, question), HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to get semantic analysis of a text
     */
    @RequestMapping(value = "/ask/{domain-id}/semantics", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Get semantic analysis of message, for example that message is a question asking for instructions or that message is an announcement")
    public ResponseEntity<?> analyzeMessage(
            @ApiParam(name = "domain-id", value = "Domain Id of knowledge base, for example 'b3158772-ac8f-4ec1-a9d7-bd0d3887fd9b', which contains its own set of questions/answers",required = true)
            @PathVariable(value = "domain-id", required = true) String domainId,
            @ApiParam(name = "message", value = "Message, e.g. 'Hi :-), my name is Michael' or 'What is the easiest way to remove a bike tyre'",required = true)
            @RequestParam(value = "message", required = true) String message,
            @ApiParam(name = "mc-impl", value = "Message classification implementation",required = true)
            @RequestParam(value = "mc-impl", required = true) QuestionClassificationImpl qcImpl,
            HttpServletRequest request, HttpServletResponse response) {

        rememberMeService.tryAutoLogin(request, response);

        try {
            Context domain = contextService.getContext(domainId);
            AnalyzedMessage analyzedMessage = questionAnalyzerService.analyze(message, domain, qcImpl);

            return new ResponseEntity<>(analyzedMessage, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get remote host address
     * @return remote host address, e.g. '178.197.227.93'
     */
    public static String getRemoteAddress(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        String xForwardedFor = request.getHeader("X-FORWARDED-FOR");
        if (xForwardedFor != null) {
            remoteAddress = xForwardedFor;
        }
        return remoteAddress;
    }
}
