package com.wyona.katie.controllers.v1;

import com.wyona.katie.integrations.matrix.MatrixMessageSender;
import com.wyona.katie.models.Error;
import com.wyona.katie.models.Username;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.integrations.msteams.MicrosoftMessageSender;
import com.wyona.katie.integrations.slack.SlackMessageSender;
import com.wyona.katie.models.*;
import com.wyona.katie.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.context.support.ResourceBundleMessageSource;

//import org.hibernate.validator.constraints.NotEmpty;
//import javax.validation.constraints.NotEmpty;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiImplicitParam;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.nio.file.AccessDeniedException;
import java.util.*;
import java.io.StringWriter;

import freemarker.template.Template;

/**
 * Controller to get questions (all and resubmitted) (Version 1)
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/question") 
public class QuestionController {

    @Autowired
    private SlackMessageSender messageSenderSlack;

    @Autowired
    private MicrosoftMessageSender messageSenderMSTeams;

    @Autowired
    private MatrixMessageSender messageSenderMatrix;

    @Autowired
    private WebhooksService webhooksService;

    @Autowired
    private NamedEntityRecognitionService nerService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private IAMService iamService;

    //@Autowired
    //private JwtService jwtService;

    @Autowired
    private QuestionAnsweringService qaService;

    @Autowired
    private RememberMeService rememberMeService;

    @Autowired
    private DynamicExpressionEvaluationService dynamicExprEvalService;

    @Autowired
    private ResourceBundleMessageSource messageSource;

    private ContextService contextService;
    private AIService aiService;
    private AuthenticationService authService;
    private DataRepositoryService dataRepoService;
    private MailerService mailerService;
    private PushNotificationService pushNotificationService;

    @Value("${mail.body.askkatie.read_answer.url}")
    private String mailBodyAskKatieReadAnswerUrl;

    @Autowired
    public QuestionController(AIService aiService, AuthenticationService authService, DataRepositoryService dataRepoService, MailerService mailerService, PushNotificationService pushNotificationService, ContextService contextService) {
        this.aiService = aiService;
        this.authService = authService;
        this.dataRepoService = dataRepoService;
        this.mailerService = mailerService;
        this.pushNotificationService = pushNotificationService;
        this.contextService = contextService;
    }

    /**
     * REST interface to add/train a new QnA
     */
    @RequestMapping(value = "/trained/{domainid}", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value = "Add a new QnA to domain and train QnA once added")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> addQuestionAndAnswer(
            @ApiParam(name = "domainid", value = "Domain Id trained question/answer is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("domainid") String domainid,
            @ApiParam(name = "newQnA", value = "Only needs to contain originalQuestion and answer", required = true)
            @RequestBody Answer newQnA
    ) {
        log.info("Add new QnA to domain '" + domainid + "': " + newQnA.getOriginalquestion() + " | " + newQnA.getAnswer());

        try {
            authService.tryJWTLogin(request);

            if (!contextService.isMemberOrAdmin(domainid)) {
                return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
            }

            Context domain = contextService.getContext(domainid);

            newQnA = contextService.addQuestionAnswer(newQnA, domain);
            String body = "{\"uuid\":\"" + newQnA.getUuid() + "\"}";
            log.info("New QnA added: " + newQnA.getUuid());

            boolean indexAlternativeQuestions = true; // TODO: Make configurable
            contextService.train(new QnA(newQnA), domain, indexAlternativeQuestions);

            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get a trained QnA
     */
    @RequestMapping(value = "/trained/{domainid}/{uuid}", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value = "Get a particular trained QnA")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> getTrainedQnA(
        @ApiParam(name = "domainid", value = "Domain Id trained question/answer is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
        @PathVariable("domainid") String domainid,
        @ApiParam(name = "uuid", value = "UUID of trained question/answer (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
        @PathVariable("uuid") String uuid
        ) {
        log.info("Get trained question/answer '" + domainid + "' / '" + uuid + "' ...");

        try {
            authService.tryJWTLogin(request);

            Context domain = contextService.getContext(domainid);
            Answer qna = contextService.getQnA(null, uuid, domain);
            log.info("Encryption algorithm: "+ qna.getAnswerClientSideEncryptionAlgorithm());

            if (qna != null) {
                // TODO: Move authorization check to ContextService.getTrainedAnswer(...) above

                // TODO: Should we return source or evaluated dynamic expression?!
                //Sentence analyzedQuestion = new Sentence("", null);
                //qna.setAnswer(dynamicExprEvalService.postProcess(qna.getAnswer(), analyzedQuestion));

                String username = authService.getUsername();
                PermissionStatus permissionStatus = iamService.getPermissionStatus(qna, username);
                log.info("Permission status of QnA '" + uuid + "' of domain '" + domainid + "': " + permissionStatus);
                if (iamService.isAuthorized(permissionStatus)) {
                    return new ResponseEntity<>(qna, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new Error("Access denied: " + permissionStatus, "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
                }
            } else {
                return new ResponseEntity<>(new Error("No such trained question/answer with uuid '" + uuid + "'", "NO_RESUBMITTED_QUESTION_WITH_SUCH_UUID"), HttpStatus.BAD_REQUEST);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "TODO_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get only answer of a trained QnA
     */
    @RequestMapping(value = "/trained/{domainid}/{uuid}/answer", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value = "Get only answer of a particular trained QnA")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> getTrainedAnswer(
            @ApiParam(name = "domainid", value = "Domain Id trained question/answer is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("domainid") String domainid,
            @ApiParam(name = "uuid", value = "UUID of trained question/answer (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("uuid") String uuid,
            @ApiParam(name = "content-type", value = "When set, then try to return answer as selected content type, otherwise return as saved", required = false)
            @RequestParam(value = "content-type", required = false) ContentType outputContentType
    ) {
        log.info("Get answer of trained QnA '" + domainid + "' / '" + uuid + "' ...");

        try {
            authService.tryJWTLogin(request);
            Context domain = contextService.getContext(domainid);
            Answer qna = contextService.getQnA(null, uuid, domain);

            // TODO: Should we return source or evaluated dynamic expression?!
            //Sentence analyzedQuestion = new Sentence("", null);
            //qna.setAnswer(dynamicExprEvalService.postProcess(qna.getAnswer(), analyzedQuestion));

            if (qna != null) {
                // TODO: Move authorization check to ContextService.getTrainedAnswer(...) above
                String username = authService.getUsername();
                PermissionStatus permissionStatus = iamService.getPermissionStatus(qna, username);
                log.info("Permission status of QnA '" + uuid + "' of domain '" + domainid + "': " + permissionStatus);
                if (iamService.isAuthorized(permissionStatus)) {
                    ContentType contentType = qna.getAnswerContentType();
                    log.info("Content type: " + contentType);

                    String answer = qna.getAnswer();

                    ObjectMapper objectMapper = new ObjectMapper();
                    ObjectNode bodyNode = objectMapper.createObjectNode();

                    // INFO: Content-type
                    if (contentType.equals(ContentType.TEXT_HTML) && outputContentType != null && outputContentType.equals(ContentType.TEXT_SLACK_MRKDWN)) {
                        bodyNode.put("content-type", outputContentType.toString());
                        answer = Utils.convertToSlackMrkdwn(answer);
                    } else if(contentType.equals(ContentType.TEXT_HTML) && outputContentType != null && outputContentType.equals(ContentType.TEXT_MARKDOWN)) {
                        bodyNode.put("content-type", outputContentType.toString());
                        answer = Utils.convertToMarkdown(answer);
                    } else if(contentType.equals(ContentType.TEXT_HTML) && outputContentType != null && outputContentType.equals(ContentType.TEXT_PLAIN)) {
                        bodyNode.put("content-type", outputContentType.toString());
                        if (false) {
                            answer = Utils.convertHtmlToPlainText(answer); // TODO: Removes all line breaks!
                        }
                        answer = Utils.stripHTML(answer, true, false);
                    } else {
                        log.warn("No such output content type '" + outputContentType + "' implemented.");
                        bodyNode.put("content-type", contentType.toString());
                    }

                    bodyNode.put("answer", answer);

                    // INFO: Answer as JSON when answer is available as JSON
                    if (contentType != null && contentType.equals(ContentType.APPLICATION_JSON)) {
                        bodyNode.put("answerAsJson", objectMapper.readTree(answer)); // INFO: Also see ResponseAnswer#getAnswerAsJson()
                    }

                    return new ResponseEntity<>(bodyNode.toString(), HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new Error("Access denied: " + permissionStatus, "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
                }
            } else {
                return new ResponseEntity<>(new Error("No such trained question/answer with uuid '" + uuid + "'", "NO_RESUBMITTED_QUESTION_WITH_SUCH_UUID"), HttpStatus.BAD_REQUEST);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "TODO_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to update the question of a particular trained QnA
     */
    @RequestMapping(value = "/trained/{domainid}/{uuid}/question", method = RequestMethod.PATCH, produces = "application/json")
    @ApiOperation(value = "Update question of a particular trained QnA")
    public ResponseEntity<?> updateTrainedQuestion(
            @ApiParam(name = "domainid", value = "Domain Id trained question/answer is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("domainid") String domainid,
            @ApiParam(name = "uuid", value = "UUID of trained question/answer (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("uuid") String uuid,
            @ApiParam(name = "question", value = "Updated question", required = true) @RequestBody Question question
    ) {
        log.info("Update question of trained QnA '" + domainid + "' / '" + uuid + "': " + question.getQuestion());
        try {
            Context domain = contextService.getContext(domainid);
            Answer qna = contextService.updateQuestionOfTrainedQnA(domain, uuid, question.getQuestion());
            return new ResponseEntity<>(qna, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "TODO_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to update the source URL of a particular trained QnA
     */
    @RequestMapping(value = "/trained/{domainid}/{uuid}/source-url", method = RequestMethod.PATCH, produces = "application/json")
    @ApiOperation(value = "Update source URL of a particular trained QnA")
    public ResponseEntity<?> updateSourceURL(
            @ApiParam(name = "domainid", value = "Domain Id trained question/answer is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("domainid") String domainid,
            @ApiParam(name = "uuid", value = "UUID of trained QnA (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("uuid") String uuid,
            @ApiParam(name = "source-url", value = "Updated source URL (only 'url' parameter necessary)", required = true) @RequestBody QnA qna
    ) {
        log.info("Update source URL of trained QnA '" + domainid + "' / '" + uuid + "': " + qna.getUrl());
        try {
            Context domain = contextService.getContext(domainid);
            Answer qnaUpdated = contextService.updateSourceUrlOfTrainedQnA(domain, uuid, qna.getUrl());
            return new ResponseEntity<>(qnaUpdated, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to extract auto-suggest terms from QnA
     */
    @RequestMapping(value = "/trained/{domainid}/{uuid}/autocompletion-terms", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value = "Exract auto-suggest terms from QnA")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> getExtractedSuggestions(
            @ApiParam(name = "domainid", value = "Domain Id trained question/answer is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("domainid") String domainid,
            @ApiParam(name = "uuid", value = "UUID of trained question/answer (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("uuid") String uuid
    ) {
        log.info("Get trained question/answer '" + domainid + "' / '" + uuid + "' ...");

        try {
            authService.tryJWTLogin(request);

            Context domain = contextService.getContext(domainid);
            Answer qna = contextService.getQnA(null, uuid, domain);

            if (qna != null) {
                // TODO: Move authorization check to ContextService.getTrainedAnswer(...) above
                String username = authService.getUsername();
                PermissionStatus permissionStatus = iamService.getPermissionStatus(qna, username);
                log.info("Permission status of QnA '" + uuid + "' of domain '" + domainid + "': " + permissionStatus);
                if (iamService.isAuthorized(permissionStatus)) {
                    // TODO: Set language independent of FAQ setting
                    java.util.List<CardKeyword> keywords = KeywordsExtractor.getKeywordsList(getContent(qna), qna.getFaqLanguage());
                    return new ResponseEntity<>(keywords, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new Error("Access denied: " + permissionStatus, "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
                }
            } else {
                return new ResponseEntity<>(new Error("No such trained question/answer with uuid '" + uuid + "'", "NO_RESUBMITTED_QUESTION_WITH_SUCH_UUID"), HttpStatus.BAD_REQUEST);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "TODO_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get content of QnA for keyword extraction
     */
    private String getContent(Answer qna) {
        StringBuilder content = new StringBuilder();
        content.append(Utils.stripHTML(qna.getOriginalquestion(), false, false));
        if (qna.getAnswerClientSideEncryptionAlgorithm() == null) {
            content.append(Utils.stripHTML(qna.getAnswer(), false, false));
        } else {
            log.info("Answer is encrypted, therefore do not add to content for keyword extraction.");
        }
        for (String aQuestion : qna.getAlternativequestions()) {
            content.append(Utils.stripHTML(aQuestion, false, false));
        }
        return content.toString();
    }

    /**
     * REST interface to add an alternative question to a particular trained QnA
     */
    @RequestMapping(value = "/trained/{domainid}/{uuid}/alternative-question", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value = "Add alternative question to a particular trained QnA")
    public ResponseEntity<?> addAlternativeQuestion(
            @ApiParam(name = "domainid", value = "Domain Id trained question/answer is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("domainid") String domainid,
            @ApiParam(name = "uuid", value = "UUID of trained question/answer (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("uuid") String uuid,
            @ApiParam(name = "question", value = "Alternative question", required = true) @RequestBody Question question
    ) {
        log.info("Add alternative question to trained QnA '" + domainid + "' / '" + uuid + "': " + question.getQuestion());
        try {
            Context domain = contextService.getContext(domainid);
            Answer qna = contextService.addAlternativeQuestion(domain, uuid, question.getQuestion());
            return new ResponseEntity<>(qna, HttpStatus.OK);
        } catch (AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "TODO_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to delete an alternative question of a particular trained QnA
     */
    @RequestMapping(value = "/trained/{domainid}/{uuid}/alternative-question/{index}", method = RequestMethod.DELETE, produces = "application/json")
    @ApiOperation(value = "Delete alternative question of a particular trained QnA")
    public ResponseEntity<?> deleteAlternativeQuestion(
            @ApiParam(name = "domainid", value = "Domain Id trained question/answer is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("domainid") String domainid,
            @ApiParam(name = "uuid", value = "UUID of trained question/answer (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("uuid") String uuid,
            @ApiParam(name = "index", value = "Position/index of alternative question (e.g. '2')",required = true)
            @PathVariable("index") Integer index
    ) {
        log.info("Delete alternative question of trained QnA '" + domainid + "' / '" + uuid + "': " + index);
        try {
            Context domain = contextService.getContext(domainid);
            Answer qna = contextService.deleteAlternativeQuestion(domain, uuid, index.intValue());
            return new ResponseEntity<>(qna, HttpStatus.OK);
        } catch (AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "TODO_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to add a classification to a particular trained QnA
     */
    @RequestMapping(value = "/trained/{domainid}/{uuid}/classification", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value = "Add classification to a particular trained QnA")
    public ResponseEntity<?> addClassificcation(
            @ApiParam(name = "domainid", value = "Domain Id trained question/answer is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("domainid") String domainid,
            @ApiParam(name = "uuid", value = "UUID of trained question/answer (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("uuid") String uuid,
            @ApiParam(name = "classification", value = "Classification", required = true) @RequestBody Classification classification
    ) {
        log.info("Add classification to trained QnA '" + domainid + "' / '" + uuid + "': " + classification.getTerm());
        try {
            Context domain = contextService.getContext(domainid);
            Answer qna = contextService.addClassification(domain, uuid, classification);
            return new ResponseEntity<>(qna, HttpStatus.OK);
        } catch (AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "TODO_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to delete a classification of a particular trained QnA
     */
    @RequestMapping(value = "/trained/{domainid}/{uuid}/classification/{index}", method = RequestMethod.DELETE, produces = "application/json")
    @ApiOperation(value = "Delete classification of a particular trained QnA")
    public ResponseEntity<?> deleteClassification(
            @ApiParam(name = "domainid", value = "Domain Id trained question/answer is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("domainid") String domainid,
            @ApiParam(name = "uuid", value = "UUID of trained question/answer (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("uuid") String uuid,
            @ApiParam(name = "index", value = "Position/index of classification (e.g. '2')",required = true)
            @PathVariable("index") Integer index
    ) {
        log.info("Delete classification of trained QnA '" + domainid + "' / '" + uuid + "': " + index);
        try {
            Context domain = contextService.getContext(domainid);
            Answer qna = contextService.deleteClassification(domain, uuid, index.intValue());
            return new ResponseEntity<>(qna, HttpStatus.OK);
        } catch (AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "TODO_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to update a trained question/answer
     */
    @RequestMapping(value = "/trained/{domainid}/{uuid}", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value = "Update a particular trained QnA")
    public ResponseEntity<?> updateTrainedAnswer(
        @ApiParam(name = "domainid", value = "Domain Id trained question/answer is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
        @PathVariable("domainid") String domainid,
        @ApiParam(name = "uuid", value = "UUID of trained question/answer (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
        @PathVariable("uuid") String uuid,
        @ApiParam(name = "updatedAnswer", value = "Only needs to contain answer", required = true) @RequestBody Answer updatedAnswer
        ) {
        log.info("Update answer of trained question/answer '" + domainid + "' / '" + uuid + "': " + updatedAnswer.getAnswer());
        try {
            if (!contextService.isMemberOrAdmin(domainid)) {
                return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
            }

            Context domain = contextService.getContext(domainid);
            Answer qna = contextService.getQnA(null, uuid, domain);

            if (qna != null) {
                // TODO: Move authorization check to ContextService.getQnA(...) or ContextService.updateAnswerOfTrainedQnA(...)
                String username = authService.getUsername();
                PermissionStatus permissionStatus = iamService.getPermissionStatus(qna, username);
                log.info("Permission status: " + permissionStatus);
                if (iamService.isAuthorized(permissionStatus)) {
                    qna = contextService.updateTrainedQnA(domain, uuid, updatedAnswer.getAnswer(), updatedAnswer.getFaqLanguage(), updatedAnswer.getFaqTopicId());
                    return new ResponseEntity<>(qna, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new Error("Access denied: " + permissionStatus, "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
                }
            } else {
                return new ResponseEntity<>(new Error("No such trained question/answer with uuid '" + uuid + "'", "NO_QNA_WITH_SUCH_UUID"), HttpStatus.BAD_REQUEST);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "TODO_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to share a trained QnA with somebody else
     */
    @RequestMapping(value = "/trained/{domainid}/{uuid}/share", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value = "Share a particular trained question/answer with somebody else")
    public ResponseEntity<?> shareQnAWithSomebodyElse(
            @ApiParam(name = "domainid", value = "Domain Id trained question/answer is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("domainid") String domainid,
            @ApiParam(name = "uuid", value = "UUID of trained question/answer (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("uuid") String uuid,
            @ApiParam(name = "email", value = "Email of user information will be shared with",required = true)
            @RequestParam(value = "email", required = true) String email
    ) {
        log.info("Share trained question/answer '" + domainid + "' / '" + uuid + "' with '" + email + "' ...");

        try {
            contextService.shareQnAByEmail(domainid, uuid, email);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to delete trained QnA for example by backend team member
     */
    @RequestMapping(value = "/trained/{domainid}/{uuid}", method = RequestMethod.DELETE, produces = "application/json")
    @ApiOperation(value="Delete a particular trained QnA")
    public ResponseEntity<?> deleteTrainedQnA(
            @ApiParam(name = "domainid", value = "Domain Id trained QnA is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("domainid") String domainid,
            @ApiParam(name = "uuid", value = "UUID of trained QnA (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("uuid") String uuid
    ) {
        log.info("Try to delete trained QnA '" + domainid + "/" + uuid + "' ...");

        if (!contextService.isMemberOrAdmin(domainid)) {
            return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }

        try {
            Context domain = contextService.getContext(domainid);
            Answer qna = contextService.getQnA(null, uuid, domain);
            if (qna != null) {
                if (qna.isTrained()) {
                    contextService.deleteTrainedQnA(domain, uuid);
                } else {
                    log.warn("QnA '" + domainid + "/" + uuid + "' is not trained, but let's delete it anyway ...");
                    contextService.deleteUntrainedQnA(domain, uuid);
                }
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Error("No such '" + uuid + "' QnA within domain '" + domainid + "'!", "NOT_FOUND"), HttpStatus.NOT_FOUND);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get resubmitted question
     */
    @RequestMapping(value = "/resubmitted/{uuid}", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value = "Get a particular resubmitted question")
    public ResponseEntity<?> getResubmittedQuestion(
        @ApiParam(name = "uuid", value = "UUID of resubmitted question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
        @PathVariable("uuid") String uuid,
        @ApiParam(name = "analyze", value = "True when question should be analyzed using NER and false otherwise",required = false)
        @RequestParam(value = "analyze", required = false) boolean analyze
        ) {
        log.info("Get resubmitted question '" + uuid + "' ...");
        try {
            ResubmittedQuestion question = dataRepoService.getResubmittedQuestion(uuid, true);

            if (question != null) {
                if (!contextService.isMemberOrAdmin(question.getContextId())) {
                    return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
                }

                if (analyze) {
                    Context domain = contextService.getContext(question.getContextId());
                    // TODO: Get classifications
                    List<String> classifications = new ArrayList<String>();
                    Sentence analyzedQuestion = nerService.analyze(question.getQuestion(), classifications, domain);
                    question.setAnalyzedQuestion(analyzedQuestion);
                }
                return new ResponseEntity<>(question, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Error("No such resubmitted question with uuid '" + uuid + "'", "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to update question of resubmitted question, for example when question contains typos
     */
    @RequestMapping(value = "/resubmitted/{uuid}/question", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Update question of a particular resubmitted question, for example when question contains typos")
    public ResponseEntity<?> updateQuestionOfResubmittedQuestion(
            @ApiParam(name = "uuid", value = "UUID of resubmitted question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("uuid") String uuid,
            @ApiParam(name = "question", value = "Only needs to contain question", required = true) @RequestBody ResubmittedQuestion question
    ) { // TODO: Replace @RequestBody ResubmittedQuestion by @RequestBody Answer
        log.info("Update question of resubmitted question '" + uuid + "' ...");

        if (question.getQuestion() == null || question.getQuestion().isEmpty()) {
            log.error("No updated question provided!");
            return new ResponseEntity<>(new Error("No updated question provided!", "NO_UPDATED_QUESTION_PROVIDED"), HttpStatus.BAD_REQUEST);
        }

        try {
            ResubmittedQuestion resubmittedQuestion = dataRepoService.getResubmittedQuestion(uuid, false);

            if (resubmittedQuestion != null) {
                if (!contextService.isMemberOrAdmin(resubmittedQuestion.getContextId())) {
                    return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
                }

                User respondent = iamService.getUserByUsername(new com.wyona.katie.models.Username(authService.getUsername()), false, false);
                log.info("Update resubmitted question '" + uuid + "' with question: '" + question.getQuestion() + "' (Respondent: '" + respondent.getUsername() + "') ...");

                // INFO: Update database entry
                dataRepoService.updateQuestionOfResubmittedQuestion(uuid, question.getQuestion(), respondent);

                return new ResponseEntity<>(resubmittedQuestion, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Error("No such resubmitted question with UUID '" + uuid + "'", "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to update the answer of a resubmitted question for example by a backend team member in order to answer a question
     */
    @RequestMapping(value = "/resubmitted/{uuid}", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Answer a particular resubmitted question")
    public ResponseEntity<?> answerResubmittedQuestion(
        @ApiParam(name = "uuid", value = "UUID of resubmitted question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
        @PathVariable("uuid") String uuid,
        @ApiParam(name = "answeredQuestion", value = "Only needs to contain answer", required = true) @RequestBody ResubmittedQuestion answeredQuestion
        ) { // TODO: Replace @RequestBody ResubmittedQuestion by @RequestBody Answer
        log.info("Update answer of resubmitted question '" + uuid + "' ...");

        if (answeredQuestion.getAnswer() == null || answeredQuestion.getAnswer().isEmpty()) {
            log.error("No answer provided!");
            return new ResponseEntity<>(new Error("No answer provided!", "NO_ANSWER_PROVIDED"), HttpStatus.BAD_REQUEST);
        }

        try {
            ResubmittedQuestion resubmittedQuestion = dataRepoService.getResubmittedQuestion(uuid, false);

            if (resubmittedQuestion != null) {
                if (!contextService.isMemberOrAdmin(resubmittedQuestion.getContextId())) {
                    return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
                }

                User respondent = iamService.getUserByUsername(new Username(authService.getUsername()), false, false);
                log.info("Update question '" + uuid + "' with answer: '" + answeredQuestion.getAnswer() + "' (Ownership: '" + answeredQuestion.getOwnership() + "', Respondent: '" + respondent.getUsername() + "') ...");

                // INFO: Add question / answer to persistent repository
                ResubmittedQuestion rqa = dataRepoService.getResubmittedQuestion(uuid, false);
                // TODO: IMPORTANT: Block SQL injection!
                rqa.setAnswer(answeredQuestion.getAnswer());
                // TODO: Do not add ANSWER_CLIENT_SIDE_ENCRYPTED_ALGORITHM when null!
                rqa.setAnswerClientSideEncryptedAlgorithm(answeredQuestion.getAnswerClientSideEncryptedAlgorithm());
                rqa.setRespondent(respondent);
                rqa.setOwnership(answeredQuestion.getOwnership());
                contextService.addQuestionAnswer(rqa);

                // INFO: Update database entry
                dataRepoService.updateResubmittedQuestionAsAnswered(uuid, answeredQuestion.getOwnership(), respondent);

                return new ResponseEntity<>(rqa, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Error("No such resubmitted question with UUID '\" + uuid + \"'", "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to delete resubmitted question for example by backend team member
     */
    @RequestMapping(value = "/resubmitted/{uuid}", method = RequestMethod.DELETE, produces = "application/json")
    @ApiOperation(value="Delete a particular resubmitted question")
    public ResponseEntity<?> deleteResubmittedQuestion(
        @ApiParam(name = "uuid", value = "UUID of resubmitted question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
        @PathVariable("uuid") String uuid
        ) {
        log.info("Delete resubmitted question '" + uuid + "' ...");
        try {
            ResubmittedQuestion updatedQuestion = dataRepoService.getResubmittedQuestion(uuid, false);

            if (updatedQuestion != null) {
                if (!contextService.isMemberOrAdmin(updatedQuestion.getContextId())) {
                    return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
                }

                dataRepoService.deleteResubmittedQuestion(uuid);

                Context domain  = contextService.getContext(updatedQuestion.getContextId());
                Answer qna = contextService.getQnA(null, uuid, domain);
                if (qna != null && !qna.isTrained()) {
                    String[] uuids = new String[1];
                    uuids[0] = uuid;
                    contextService.deleteQnAsFromStorage(domain, uuids);
                } else {
                    log.info("Resubmitted question '" + uuid + "' is already trained, therefore do not delete from persistent domain storage.");
                }
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Error("TODO", "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to send answer to user which resubmitted question
     */
    @RequestMapping(value = "/resubmitted/{uuid}/sendAnswer", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Send answer back to user which resubmitted question")
    public ResponseEntity<?> sendAnswerToUserWhichResubmittedQuestion(
        @ApiParam(name = "uuid", value = "UUID of resubmitted question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
        @PathVariable("uuid") String uuid
        ) {
        log.info("Get QnA '" + uuid + "' ...");
        try {
            ResubmittedQuestion qna = dataRepoService.getResubmittedQuestion(uuid, false);

            if (qna != null) {
                if (!contextService.isMemberOrAdmin(qna.getContextId())) {
                    return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
                }

                Answer answer = contextService.getQnA(null, uuid, contextService.getContext(qna.getContextId()));
                qna.setAnswer(answer.getAnswer());
                qna.setAnswerClientSideEncryptedAlgorithm(answer.getAnswerClientSideEncryptionAlgorithm());

                String status = qna.getStatus();
                if (status.equals(StatusResubmittedQuestion.STATUS_ANSWERED) || status.equals(StatusResubmittedQuestion.STATUS_ANSWER_SENT) || status.equals(StatusResubmittedQuestion.STATUS_ANSWER_RATED)) {

                    if (qna.getEmail() != null) {
                        Context domain = contextService.getContext(qna.getContextId());
                        String fromEmail = domain.getMailSenderEmail();
                        if (qna.getAnswerLinkType() != null && qna.getAnswerLinkType().equals(ResubmittedQuestion.DEEP_LINK)) {
                            mailerService.send(qna.getEmail(), fromEmail, getSubject(qna), getBodyDeepLink(qna), true);
                        } else {
                            mailerService.send(qna.getEmail(), fromEmail, getSubject(qna), getBody(qna), true);
                        }
                    } else if (qna.getFCMToken() != null) {
                        PushNotificationRequest pnr = new PushNotificationRequest(getSubject(qna), getPushNotificationBody(qna), "TODO_Topic");
                        pnr.setToken(qna.getFCMToken());
                        pushNotificationService.sendPushNotificationToToken(pnr);
                    } else if (qna.getChannelType() == ChannelType.SLACK) {
                        messageSenderSlack.sendAnswerToResubmittedQuestion(qna);
                    } else if (qna.getChannelType() == ChannelType.MS_TEAMS) {
                        messageSenderMSTeams.sendAnswer(qna, false);
                    } else if (qna.getChannelType() == ChannelType.DISCORD) {
                        log.warn("TODO: Send answer to Discord!");
                    } else if (qna.getChannelType() == ChannelType.MATRIX) {
                        log.warn("Send answer to Matrix room ...");
                        messageSenderMatrix.sendRoomMessage(qna.getQuestion(), qna.getAnswer(), qna.getChannelRequestId());
                    } else {
                        log.error("Neither email nor FCM token nor channel type provided!");
                    }

                    webhooksService.deliver(WebhookTriggerEvent.SEND_ANSWER_TO_RESUBMITTED_QUESTION_TO_USER, qna.getContextId(), qna.getUuid(), qna.getQuestion(), qna.getAnswer(), ContentType.TEXT_HTML, qna.getEmail(), qna.getChannelRequestId());

                    dataRepoService.updateStatusOfResubmittedQuestion(uuid, StatusResubmittedQuestion.STATUS_ANSWER_SENT);
                    qna = dataRepoService.getResubmittedQuestion(uuid, false);
                    return new ResponseEntity<>(qna, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new Error("TODO_QuestionController", "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>(new Error("No resubmitted question with uuid '" + uuid + "'", "NO_RESUBMITTED_QUESTION_WITH_SUCH_UUID"), HttpStatus.BAD_REQUEST);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Generate email subject line to answer resubmitted question
     */
    private String getSubject(ResubmittedQuestion question) {
        Context domain = null;
        try {
            domain = contextService.getContext(question.getContextId());
            Language questionerLang = question.getQuestionerLanguage();
            Locale locale = getLocale(questionerLang);
            // TODO: Make subject line customizable per domain
            return "[" + domain.getMailSubjectTag() + "] " + messageSource.getMessage("answer.to.your.question", null, locale);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return "Exception occured while generating subject line: " + e.getMessage();
        }
    }

    /**
     *
     */
    private Locale getLocale(Language lang) {
        if (lang != null) {
            return new Locale(lang.toString());
        } else {
            return new Locale("en");
        }
    }

    /**
     * Generate email text containing a deep link in order to read answer to resubmitted question within mobile app
     */
    private String getBodyDeepLink(ResubmittedQuestion question) {
        String userLanguage = "en";

        StringBuilder sb = new StringBuilder("");
        Context context = null;
        try {
            context = contextService.getContext(question.getContextId());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        sb.append("Please read the answer to your question in your <a href=\"" + context.getMailBodyDeepLink() + "?uuid=" + question.getUuid() + "&username=" + question.getEmail() + "\">mobile app</a>.");
        sb.append("<br/><br/>");
        sb.append("Thank you for using AskKatie!");
        return sb.toString();
    }

    /**
     * Generate email text containing answer or/and a link to answer to resubmitted question
     * @param question QnA containing resubmitted question, answer, etc.
     */
    private String getBody(ResubmittedQuestion question) throws Exception {
        Language userLanguage = question.getQuestionerLanguage();
        if (userLanguage == null) {
            log.warn("No questioner language available!");
        }

        Context domain = null;
        try {
            domain = contextService.getContext(question.getContextId());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        String answerLink = contextService.getAnswerLink(question.getUuid(), question.getEmail(), domain);

        TemplateArguments tmplArgs = new TemplateArguments(domain, null);
        tmplArgs.add("question", question.getQuestion());
        tmplArgs.add("answer", question.getAnswer());
        //log.debug("Encryption algorithm of QnA '" + question.getUUID() + "': " + question.getAnswerClientSideEncryptedAlgorithm());
        if (question.getAnswerClientSideEncryptedAlgorithm() != null) {
            tmplArgs.add("answer_is_encrypted", "true");
        } else {
            tmplArgs.add("answer_is_encrypted", "false");
        }
        tmplArgs.add("answer_link", answerLink);

        StringWriter writer = new StringWriter();
        Template emailTemplate = mailerService.getTemplate("answer-to-question_email_", userLanguage, domain);
        emailTemplate.process(tmplArgs.getArgs(), writer);
        return writer.toString();
    }

    /**
     * Generate push notification text to answer resubmitted question
     */
    private String getPushNotificationBody(ResubmittedQuestion question) {
        String userLanguage = "en";

        StringBuilder sb = new StringBuilder("");
        sb.append("AskKatie's answer to your question '" + question.getQuestion() + "':");
        sb.append("\n\n");
        Context context = null;
        try {
            context = contextService.getContext(question.getContextId());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        sb.append(contextService.getAnswerLink(question.getUuid(), question.getEmail(), context));
/*
        sb.append("'" + question.getAnswer() + "'");
*/
        sb.append("\n\n");
        sb.append("Thank you for using AskKatie!");

        return sb.toString();
    }

    /**
     * REST interface such that user which resubmitted question can view answer of respondent
     */
    @RequestMapping(value = "/resubmitted/{domainid}/{uuid}/answer", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get answer to resubmitted question")
    public ResponseEntity<?> getRepliedAnswer(
        @ApiParam(name = "domainid", value = "Domain Id resubmitted question is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
        @PathVariable("domainid") String domainid,
        @ApiParam(name = "uuid", value = "UUID of resubmitted question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
        @PathVariable("uuid") String uuid
        ) {
        log.info("Get answer to resubmitted question '" + uuid + "' of domain '" + domainid + "' ...");
        try {
            ResubmittedQuestion rqa = dataRepoService.getResubmittedQuestion(uuid, false);
            Context domain = contextService.getContext(domainid);
            if (rqa != null) {
                // TODO: Refactor by returning QnA instead resubmitted question
                Answer answer = contextService.getQnA(null, uuid, domain);

                Sentence analyzedQuestion = new Sentence("", null, null);
                answer.setAnswer(dynamicExprEvalService.postProcess(answer.getAnswer(), analyzedQuestion));

                log.info("Answer: " + answer.getAnswer());
                rqa.setAnswer(answer.getAnswer());
                log.info("Encryption algorithm: " + answer.getAnswerClientSideEncryptionAlgorithm());
                rqa.setAnswerClientSideEncryptedAlgorithm(answer.getAnswerClientSideEncryptionAlgorithm());

                String status = rqa.getStatus();
                if (status.equals(StatusResubmittedQuestion.STATUS_ANSWERED) || status.equals(StatusResubmittedQuestion.STATUS_ANSWER_SENT) || status.equals(StatusResubmittedQuestion.STATUS_ANSWER_RATED)) {
                    // TODO: Only return question and answer and some minimal information
                    return new ResponseEntity<>(rqa, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new Error("Resubmitted question not answered yet!", "RESUBMITTED_QUESTION_NOT_ANSWERED_YET"), HttpStatus.BAD_REQUEST);
                }
            } else {
                try {
                    log.info("Try to get trained QnA '" + uuid + "', because resubmitted question has already been deleted.");
                    Answer answer = contextService.getQnA(null, uuid, domain);
                    Sentence analyzedQuestion = new Sentence("", null, null);
                    answer.setAnswer(dynamicExprEvalService.postProcess(answer.getAnswer(), analyzedQuestion));
                    log.info("Answer: " + answer.getAnswer());
                    ResubmittedQuestion a = new ResubmittedQuestion(uuid, answer.getOriginalquestion(), null, null, ChannelType.UNDEFINED, null, null, null, null, null, null, new Date(answer.getDateOriginalQuestion()), answer.getAnswer(), answer.getAnswerClientSideEncryptionAlgorithm(), null, null, domainid);
                    return new ResponseEntity<>(a, HttpStatus.OK);
                } catch(Exception e) {
                    log.error(e.getMessage(), e);
                    return new ResponseEntity<>(new Error("TODO_QuestionController", "NO_RESUBMITTED_QUESTION_WITH_SUCH_UUID"), HttpStatus.BAD_REQUEST);
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "SQL_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface such that user can rate received answer
     * @deprecated Use {@link FeedbackController#rateAnswer(String, String, Rating, HttpServletRequest, HttpServletResponse)} instead
     * TODO: See for example git@github.com:wyona/katie-4-faq.git
     */
    @RequestMapping(value = "/{domainid}/{uuid}/rateAnswer", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Rate answer to question")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    @Deprecated
    public ResponseEntity<?> rateAnswer(
            @ApiParam(name = "uuid", value = "UUID of asked question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("uuid") String quuid,
            @ApiParam(name = "domainid", value = "Domain Id question and answer is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("domainid") String domainid,
            @ApiParam(name = "rating", value = "User rating re replied answer, between 0 and 10, whereas 0 means not helpful and 10 means very helpful", required = true)
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
            AskedQuestion askedQuestion = contextService.getAskedQuestionByUUID(quuid);

            // INFO: We don't need the user question from the rating object, because we already have it using the question UUID
            rating.setUserquestion(askedQuestion.getQuestion());
            log.info("User question: " + rating.getUserquestion());
            if (rating.getUserquestion() != null && rating.getUserquestion().length() > 300) {
                log.warn("User question more than 300 characters, therefore shorten question ...");
                rating.setUserquestion(rating.getUserquestion().substring(0, 299));
            }

            rating.setDate(new Date());
            rating.setQnauuid(askedQuestion.getQnaUuid());

            Context domain = contextService.getContext(domainid);
            Answer answer = contextService.rateAnswer(domain, rating);
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
     * REST interface such that user can rate a QnA
     */
    @RequestMapping(value = "/resubmitted/{domainid}/{uuid}/rateQnA", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Rate QnA")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> rateQnA(
            @ApiParam(name = "uuid", value = "UUID of QnA (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("uuid") String uuid,
            @ApiParam(name = "domainid", value = "Domain Id QnA is associated with (e.g. 'ROOT' or 'df9f42a1-5697-47f0-909d-3f4b88d9baf6')",required = true)
            @PathVariable("domainid") String domainid,
            @ApiParam(name = "rating", value = "User rating re replied answer, between 0 and 10, whereas 0 means not helpful and 10 means very helpful", required = true)
            @RequestBody Rating rating,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        log.info("Rate QnA '" + uuid + "' ...");

        if (rating.getRating() < 0 || rating.getRating() > 10) {
            return new ResponseEntity<>(new Error("Rating '" + rating.getRating() + "' out of bounds!", "RATING_OUT_OF_BOUNDS"), HttpStatus.BAD_REQUEST);
        }

        if (rating.getFeedback() != null && !rating.getFeedback().isEmpty()) {
            log.info("Received optional feedback: " + rating.getFeedback());
            if (rating.getFeedback().length() > 150 ){
                return new ResponseEntity<>(new Error("Rating feedback '" + rating.getFeedback() + "' out of bounds!", "RATING_FEEDBACK_OUT_OF_BOUNDS"), HttpStatus.BAD_REQUEST);
            }
        } else {
            log.info("No optional feedback received.");
        }

        if (rating.getUserquestion() != null) {
            log.info("User question: " + rating.getUserquestion());
            log.warn("User question should not be set!");
            rating.setUserquestion(null);
        }

        rating.setDate(new Date());

        if (rating.getEmail() != null && !rating.getEmail().isEmpty()) {
            log.info("User provided email: " + rating.getEmail());
            rememberMeService.rememberEmail(rating.getEmail(), request, response, domainid);
        }

        try {
            Context domain = contextService.getDomain(domainid);

            // TODO: Replace rateAnswer by rateQnA
            rating.setQnauuid(uuid);
            Answer qna = contextService.rateAnswer(domain, rating);
            if (qna != null) {
                return new ResponseEntity<>(qna, HttpStatus.OK);
            } else {
                log.error("No such QnA with UUID '" + uuid + "'!");
                return new ResponseEntity<>(new Error("No such QnA with UUID '" + uuid + "'!", "NOT_FOUND"), HttpStatus.NOT_FOUND);
            }
        } catch(AccessDeniedException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to trigger training of AI service with resubmitted question and associated answer
     */
    @RequestMapping(value = "/resubmitted/{uuid}/train", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Trigger training of AI service with resubmitted question and associated answer")
    public ResponseEntity<?> trainAIWithResubmittedQuestionAndAssociatedAnswer(
        @ApiParam(name = "uuid", value = "UUID of resubmitted question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
        @PathVariable("uuid") String uuid
        ) {
        log.info("Get resubmitted question '" + uuid + "' ...");
        try {
            ResubmittedQuestion question = dataRepoService.getResubmittedQuestion(uuid, false);

            if (question != null) {
                if (!contextService.isMemberOrAdmin(question.getContextId())) {
                    return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
                }

                if (question.getStatus().equals(StatusResubmittedQuestion.STATUS_ANSWERED) || question.getStatus().equals(StatusResubmittedQuestion.STATUS_ANSWER_SENT) || question.getStatus().equals(StatusResubmittedQuestion.STATUS_ANSWER_RATED)) {
                    Context domain = contextService.getContext(question.getContextId());
                    Answer qna = contextService.getQnA(null, uuid, domain);
                    qna.setOriginalQuestion(question.getQuestion());

                    // INFO: Train AI with question referencing the UUID of the answer
                    boolean indexAlternativeQuestions = true; // TODO: Make configurable
                    contextService.train(new QnA(qna), domain, indexAlternativeQuestions);

                    return new ResponseEntity<>(question, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new Error("TODO", "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>(new Error("TODO", "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }
}
