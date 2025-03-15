package com.wyona.katie.controllers.v1;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.wyona.katie.handlers.GenerateProvider;
import com.wyona.katie.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.Error;
import com.wyona.katie.models.Username;
import com.wyona.katie.services.*;
import io.swagger.annotations.*;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;

//import org.hibernate.validator.constraints.NotEmpty;
//import javax.validation.constraints.NotEmpty;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.nio.file.AccessDeniedException;
import java.time.Duration;
import java.util.*;

/**
 * Controller to ask questions (Version 1)
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1") 
public class AskController {

    Integer counter = 0; // TODO: Do not use global variable

    // TODO: Make configurable
    private String yulupDomainId = "26cf31c2-8cb6-4e7e-9552-1c1f9f1ed035";

    @Value("${new.context.mail.body.host}")
    private String defaultHostnameMailBody;

    @Autowired
    private LearningCoachService learningCoachService;

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
    GenerativeAIService generativeAIService;

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
                    required = false, dataTypeClass = String.class, paramType = "header") })
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
            java.util.List<ResponseAnswer> responseAnswers = qaService.getAnswers(question, null, false, classifications, messageId, domain, dateSubmitted, remoteAddress, ChannelType.UNDEFINED, channelRequestId, 10, 0, true, answerContentType, includeFeedbackLinks, false, false);

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
        if (username == null) {
            return new ResponseEntity<>(new Error("User is not signed in", "UNAUTHORIZED"), HttpStatus.UNAUTHORIZED);
        }
        User user = iamService.getUserByUsername(new Username(username), false, false);
        
        Language language = Language.valueOf(user.getLanguage());

        Context domain = null;
        try {
            domain = contextService.getContext(domainId); // INFO: getContext() return ROOT context when domain id is null
            log.info("Check whether user '" + username + "' is authorized to submit question for this particular domain ...");
            if (!contextService.isMemberOrAdmin(domainId)) {
                return new ResponseEntity<>(new Error("User is neither member of domain '" + domainId + "' nor has role ADMIN", "FORBIDDEN"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }

        log.info("Submit question '" + question + "' to expert ...");
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
    @RequestMapping(value = "/ask/{domain-id}/taxonomy/inference", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    //@RequestMapping(value = "/ask/{domain-id}/taxonomy/inference", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE})
    @Operation(summary="Classify a text")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK", response = PredictedLabelsResponse.class),
            @ApiResponse(code = 403, message = "Forbidden", response = Error.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = Error.class)
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> predictTaxonomyEntries(
            @ApiParam(name = "asynchronous", value = "When set to true, then prediction will be done asynchronous", required = false, defaultValue = "false")
            @RequestParam(value = "asynchronous", required = false) Boolean asynchronous,
            @ApiParam(name = "domain-id", value = "Domain Id of knowledge base, for example 'b3158772-ac8f-4ec1-a9d7-bd0d3887fd9b', which contains its own set of questions/answers", required = true)
            @PathVariable(value = "domain-id", required = true) String domainId,
            @ApiParam(name = "limit", value = "Maximum number of labels returned", required = false, defaultValue = "3")
            @RequestParam(value = "limit", required = false) Integer limit,
            @ApiParam(name = "text", value = "Text to be classified, e.g. if the input text is 'Where was Michael born?', then the following classifications could be returned: birthplace, michael", required = true)
            @RequestBody Message text,
            HttpServletRequest request, HttpServletResponse response) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        rememberMeService.tryAutoLogin(request, response);

        try {
            int _limit = 3;
            if (limit != null) {
                _limit = limit.intValue();
            }
            String requestedLanguage = "de"; // TODO: Make language configurable

            User user = authService.getUser(false, false);
            if (user == null) {
                throw new java.nio.file.AccessDeniedException("User is not signed in!");
            }
            if (!contextService.isMemberOrAdmin(domainId, user)) {
                Context domain = contextService.getContext(domainId);
                if (domain.getAnswersGenerallyProtected()) {
                    log.info("User '" + user.getId() + "' has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
                    throw new java.nio.file.AccessDeniedException("User '" + user.getId() + "' is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
                } else {
                    log.info("User '" + user.getId() + "' has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "', but answers of domain '" + domainId + "' are generally public.");
                }
            }

            if (asynchronous != null && asynchronous) {
                String processId = UUID.randomUUID().toString();
                contextService.classifyTextAsynchronously(domainId, text.getMessage(), text.getMessageId(), _limit, requestedLanguage, user, processId);
                return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
            } else {
                return new ResponseEntity<>(contextService.classifyText(domainId, text.getMessage(), text.getMessageId(), _limit, requestedLanguage, user), HttpStatus.OK);
            }
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
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
    @Operation(summary="Get semantic analysis of message, for example that message is a question asking for instructions or that message is an announcement")
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
            // TODO: Use getDomain(String) in order to check authorization
            //Context domain = contextService.getDomain(domainId);
            Context domain = contextService.getContext(domainId);
            AnalyzedMessage analyzedMessage = questionAnalyzerService.analyze(message, domain, qcImpl);

            return new ResponseEntity<>(analyzedMessage, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to chat with a LLM
     */
    @RequestMapping(value = "/chat/completions/{domain-id}", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary="Chat with a LLM")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> chatCompletions(
            @ApiParam(name = "domain-id", value = "Domain Id of knowledge base, for example 'b3158772-ac8f-4ec1-a9d7-bd0d3887fd9b', which contains LLM configuration",required = true)
            @PathVariable(value = "domain-id", required = true) String _domainId,
            @ApiParam(name = "request-body", value = "Request body, see https://docs.mistral.ai/api/ or https://platform.openai.com/docs/api-reference/chat/create", required = true)
            @RequestBody ChatCompletionsRequest chatCompletionsRequest,
            HttpServletRequest request, HttpServletResponse response) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        rememberMeService.tryAutoLogin(request, response);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();

        if (chatCompletionsRequest.getConversation_id() != null) {
            log.info("Continue existing conversation: " + chatCompletionsRequest.getConversation_id());
        } else {
            chatCompletionsRequest.setConversation_id(UUID.randomUUID().toString());
            log.info("Create a new conversation: " + chatCompletionsRequest.getConversation_id());
        }
        body.put("conversation_id", chatCompletionsRequest.getConversation_id());

        ArrayNode choices = mapper.createArrayNode();
        body.put("choices", choices);

        if (!authService.userIsSignedInBySession(request)) {
            log.info("User is not signed in!");
            String content = getLoginSignUpMessage();
            choices = addChoice(mapper, choices, "error", content, 0);
            //choices = addChoice(mapper, choices, PromptMessageRoleLowerCase.assistant.toString(), content, 0);
            return new ResponseEntity<>(body.toString(), HttpStatus.OK);
        }

        Context domain = null;
        try {
            domain = contextService.getContext(_domainId);
            //domain = contextService.getContext(yulupDomainId);
        } catch (Exception e) {
            String content = getNoSuchDomainMessage(_domainId);
            //String content = getNoSuchDomainMessage(yulupDomainId);
            log.error(content);
            choices = addChoice(mapper, choices, "error", content, 0);
            return new ResponseEntity<>(body.toString(), HttpStatus.OK);
        }

        User user = authService.getUser(false, false);
        if (!contextService.isUserMemberOfDomain(user.getId(), domain.getId())) {
            log.info("User '" + user.getUsername() + "' is not member of domain '" + domain.getId() + "'!");
            // WARN: Do not add users to this domain, because otherwise they can see all other members!
            /*
            String subject = "User requests invitation to Katie domain '" + domain.getName() + "'";
            String message = "<html><body>The user '" + user.getUsername() + "' would like to be invited to the domain <a href=\"" + defaultHostnameMailBody + "/#/domain/" + domain.getId() + "/members\">" + domain.getName() + "</a>.</body></html>";
            contextService.notifyDomainOwnersAndAdmins(subject, message, domain.getId());
            String content = "Your user '" + user.getUsername() + "' must be a member of the Katie domain '" + domain.getName() + "', whereas a request has just been sent to the domain owners, thank you for your patience!";
            choices = addChoice(mapper, choices, "error", content, 0);
            return new ResponseEntity<>(body.toString(), HttpStatus.OK);
             */
        }

        try {
            ChosenSuggestion chosenSuggestion = chatCompletionsRequest.getchosen_suggestion();

            // TODO: Do not send suggestion prompt (secret sauce) to client, but which is currently necessary to include in message history and also for debugging purposes
            if (chosenSuggestion != null) {
                choices = addChoice(mapper, choices, PromptMessageRoleLowerCase.system.toString(), learningCoachService.getSystemPrompt(chosenSuggestion), 1);
            }

            String completedText = getCompletion(domain, chosenSuggestion, chatCompletionsRequest);
            choices = addChoice(mapper, choices, PromptMessageRoleLowerCase.assistant.toString(), completedText, 0);

            return new ResponseEntity<>(body.toString(), HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get completion message
     * @param chosenSuggestion TODO
     * @param chatCompletionsRequest TODO
     * @return completion message
     */
    private String getCompletion(Context domain, ChosenSuggestion chosenSuggestion, ChatCompletionsRequest chatCompletionsRequest) throws Exception {
        CompletionImpl completionImpl = domain.getCompletionConfig().getCompletionImpl();
        //completionImpl = CompletionImpl.OLLAMA;
        if (completionImpl == CompletionImpl.UNSET) {
            String warnMsg = "Domain '" + domain.getId() + "' has no completion implementation configured!";
            log.warn(warnMsg);
            return warnMsg;
        } else {
            log.info("Domain '" + domain.getId() + "' has '" + completionImpl + "' configured as completion implementation.");
        }
        GenerateProvider generateProvider = generativeAIService.getGenAIImplementation(completionImpl);
        String model = domain.getCompletionConfig().getModel();

        List<PromptMessage> promptMessages = new ArrayList<>();

        log.info("Conversation history contains " + chatCompletionsRequest.getMessages().length + " messages.");
        for (PromptMessageWithRoleLowerCase msg : chatCompletionsRequest.getMessages()) {
            promptMessages.add(new PromptMessage(PromptMessageRole.fromString(msg.getRole().toString()), msg.getContent()));
        }

        if (chosenSuggestion != null) {
            log.info("Chosen suggestion Id: " + chosenSuggestion.getIndex());
            promptMessages.add(new PromptMessage(PromptMessageRole.SYSTEM, learningCoachService.getSystemPrompt(chosenSuggestion)));
            // TODO: Remember that conversation was started with suggestion
            appendMessageToConversation(chatCompletionsRequest.getConversation_id(), PromptMessageRoleLowerCase.system.toString(), learningCoachService.getSystemPrompt(chosenSuggestion));
        } else {
            log.info("No suggestion provided.");
            // TODO: Check whether conversation was started with a suggestion and if so, then add suggestion to beginning of conversation
        }

        Double temperature = 0.7;
        if (chatCompletionsRequest.getTemperature() != null) {
            temperature = chatCompletionsRequest.getTemperature();
        }

        String apiToken = domain.getCompletionConfig().getApiKey();

        String completedText = "Hi, this is a mock response from Katie :-)";
        if (false) {
            log.info("Return mock completion ...");
        } else {
            log.info("Get completion from LLM ...");
            completedText = generateProvider.getCompletion(promptMessages, null, model, temperature, apiToken).getText();
        }

        appendMessageToConversation(chatCompletionsRequest.getConversation_id(), PromptMessageRoleLowerCase.assistant.toString(), completedText);
        return completedText;
    }

    /**
     * TODO
     */
    private ArrayNode addChoice(ObjectMapper mapper, ArrayNode choices, String role, String content, int id) {
        ObjectNode choice = mapper.createObjectNode();
        ObjectNode message = mapper.createObjectNode();
        message.put("role", role);
        message.put("content", content);
        choice.put("id", id);
        choice.put("message", message);
        choices.add(choice);

        return choices;
    }

    /**
     * Append message to conversation
     */
    private void appendMessageToConversation(String conversationId, String role, String message) {
        log.info("TODO: Add nessage to conversation '" + conversationId + "' ...");
    }

    /**
     * REST interface to chat with a LLM using SSE
     * @return
     */
    @PostMapping(path ="/chat/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    //@PostMapping(path ="/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary="Chat with a LLM using Server Sent Events")
    public Flux<ServerSentEvent<String>> chatCompletionsAsSSE(
            @ApiParam(name = "request-body", value = "Request body, see https://docs.mistral.ai/api/ or https://platform.openai.com/docs/api-reference/chat/create", required = true)
            @RequestBody ChatCompletionsRequest chatCompletionsRequest,
            HttpServletRequest request, HttpServletResponse response) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        rememberMeService.tryAutoLogin(request, response);

        // INFO: Use an array instead a string only, in order to workaround "Variable used in lambda expression should be final or effectively final"
        String[] mockResponse = new String[1];
        //mockResponse[0] = "Hi, this is a SSE mock response from Katie :-)";

        Context domain = null;
        try {
            domain = contextService.getContext(yulupDomainId);
        } catch (Exception e) {
            String content = getNoSuchDomainMessage(yulupDomainId);
            log.error(content);
            mockResponse[0] = content;
            return sendSSE(mockResponse[0]);
            //return Flux.error(e);
        }

        if (!authService.userIsSignedInBySession(request)) {
            log.info("User is not signed in!");
            mockResponse[0] = getLoginSignUpMessage();
            return sendSSE(mockResponse[0]);
        }

        ChosenSuggestion chosenSuggestion = chatCompletionsRequest.getchosen_suggestion();
        try {
            mockResponse[0] = getCompletion(domain, chosenSuggestion, chatCompletionsRequest);
        } catch (Exception e) {
            mockResponse[0] = e.getMessage();
            log.error(e.getMessage(), e);
        }

        return sendSSE(mockResponse[0]);
    }

    /**
     *
     */
    private Flux<ServerSentEvent<String>> sendSSE(String message) {
        counter = 0;
        String delimiter = " ";
        int limit = message.split(delimiter).length;
        return Flux.interval(Duration.ofMillis(100))
                .take(limit) // https://www.baeldung.com/spring-webflux-cancel-flux#3-cancel-using-takelong-n-operator
                .map(sequence -> ServerSentEvent.<String> builder()
                        //.id(String.valueOf(sequence))
                        //.event("periodic-event")
                        .data(getEvent(message, delimiter))
                        .build());
    }

    /**
     * Get event as JSON
     * @param mockResponse Mock response to test SSE, e.g. "Hi, this is a SSE mock response from Katie :-)"
     * @param delimiter Delimiter between chunks, e.g. an empty space
     * @return event containing a chunk of the mock response
     */
    private String getEvent(String mockResponse, String delimiter) {
        String[] words = mockResponse.split(delimiter);

        String nextWord = words[counter];
        counter++;
        //String nextWord = LocalTime.now().toString();
        log.info("Next word: " + nextWord);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("id", "chatcmpl-ASARhlYzG6L58RACHldRzeKKvrMil");
        rootNode.put("object", "chat.completion.chunk");
        rootNode.put("created", 1731276701);
        rootNode.put("model", "gpt-4o-2024-08-06");
        rootNode.put("system_fingerprint", "fp_159d8341cc");
        ArrayNode choicesNode = mapper.createArrayNode();
        rootNode.put("choices", choicesNode);
        ObjectNode choiceNode = mapper.createObjectNode();
        choicesNode.add(choiceNode);
        choiceNode.put("index", 0);
        // TODO: Put "logprobs" and "finish_reason"
        ObjectNode deltaNode = mapper.createObjectNode();
        choiceNode.put("delta", deltaNode);
        deltaNode.put("content", " " + nextWord);

        return rootNode.toString();
    }

    /**
     *
     */
    private String getLoginSignUpMessage() {
        return "Please make sure to be <a href=\"/#/login\">signed in</a> or <a href=\"/#/register\">sign up</a> for free :-)";
    }

    /**
     *
     */
    private String getNoSuchDomainMessage(String domainId) {
        return "No such domain '" + domainId + "'!";
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
