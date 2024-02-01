package com.wyona.katie.services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.exceptions.UserAlreadyMemberException;
import com.wyona.katie.models.faq.TopicVisibility;
import com.wyona.katie.models.faq.FAQ;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.models.*;
import com.wyona.katie.models.faq.Topic;
import com.wyona.katie.models.insights.*;
import lombok.extern.slf4j.Slf4j;

import org.apache.lucene.index.VectorSimilarityFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.wyona.katie.models.faq.Question;
import com.wyona.katie.models.faq.Topic;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

import freemarker.template.Template;

import org.apache.commons.io.FileUtils;

import javax.mail.internet.MimeMessage;

/**
 * TODO: Consider renaming it to DomainService
 */
@Slf4j
@Component
@DependsOn("IAMService")
public class ContextService {

    @Value("${question.answer.implementation}")
    private DetectDuplicatedQuestionImpl defaultDetectDuplicatedQuestionImpl;

    @Value("${lucene.vector.search.embedding.impl}")
    private EmbeddingsImpl defaultEmbeddingImpl;

    @Value("${aleph-alpha.token}")
    private String alephAlphaToken;

    @Value("${cohere.key}")
    private String cohereKey;

    @Value("${openai.key}")
    private String openAIKey;

    @Value("${openai.azure.key}")
    private String openAIAzureKey;

    @Value("${mistral.api.key}")
    private String mistralAIKey;

    @Value("${google.key}")
    private String googleKey;

    @Value("${lucene.vector.search.similarity.metric}")
    private String defaultVectorSearchSimilarityMetric;

    @Value("${cohere.vector.search.similarity.metric}")
    private String cohereVectorSearchSimilarityMetric;

    @Value("${contexts.data_path}")
    private String contextsDataPath;

    @Value("${new.context.mail.body.host}")
    private String mailBodyHost;

    @Value("${new.context.mail.deep.link}")
    private String mailDeepLink;

    @Value("${sbert.distance.threshold}")
    private float distanceThreshold;

    @Value("${weaviate.certainty.threshold}")
    private float certaintyThreshold;

    @Value("${mail.body.askkatie.read_answer.url}")
    private String mailBodyAskKatieReadAnswerUrl;

    @Value("${qnasfwp.implementation}")
    private QnAExtractorImpl qnasFromWebpageDefaultImpl;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private XMLService xmlService;
    @Autowired
    private KnowledgeSourceXMLFileService knowledgeSourceXMLFileService;

    @Autowired
    private DataRepositoryService dataRepositoryService;

    @Autowired
    private AIService aiService;

    @Autowired
    private IAMService iamService;

    @Autowired
    private MailerService mailerService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AutoCompleteServiceLuceneImpl autoCompleteService;

    @Autowired
    private TaxonomyServiceLuceneImpl taxonomyService;

    @Autowired
    private ResourceBundleMessageSource messageSource;

    @Autowired
    private QnAsFromWebpageServiceMockImpl qnAsFromWebpageServiceMock;

    @Autowired
    private QnAsFromWebpageServiceRestImpl qnAsFromWebpageServiceRest;

    @Autowired
    private ForeignKeyIndexService foreignKeyIndexService;

    @Autowired
    private BackgroundProcessService backgroundProcessService;

    private final static String DATA_OBJECT_META_FILE = "meta.json";
    private final static String DATA_OBJECZT_FILE = "data";

    /**
     *
     */
    @javax.annotation.PostConstruct
    void initROOTDomain() throws Exception {
        log.info("Check whether Katie ROOT domain already exists ...");
        if (!existsContext("ROOT")) {
            log.info("Katie ROOT domain does not exist yet (" + contextsDataPath + "), therefore we create one ...");
            createROOTDomain();

            try {
                iamService.initSuperadmin();

                User sysadmin = iamService.getUserByUsername(new Username("superadmin"), false, false);

                String domainId = "ROOT";
                addMember(sysadmin.getId(), false, false, RoleDomain.OWNER, domainId);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Save resubmitted question and ask expert to answer question
     * @param question Resubmitted question
     * @param questionerUser User which submitted question
     * @param channelType Channel type, e.g. EMAIL, FCM_TOKEN, SLACK, MS_TEAMS, MATRIX, WEBHOOK
     * @param channelRequestId Channel request Id (generated by channel specific implementation)
     * @param email Email of user which submitted question (if user is not signed in)
     * @param fcmToken Firebase Cloud Messaging token associated with mobile device of user which submitted question
     * @param questionerLanguage Language of user asking question
     * @param answerLinkType Answer link type, for example 'deeplink'
     * @param domain Domain
     * @return UUID of *resubmitted* question
     */
    public String answerQuestionByNaturalIntelligence(String question, User questionerUser, ChannelType channelType, String channelRequestId, String email, String fcmToken, Language questionerLanguage, String answerLinkType, String remoteAddress, Context domain) {
        analyticsService.logQuestionSentToExpert(domain.getId(), channelType, email);

        if (channelType == ChannelType.EMAIL && email != null) {
            log.info("Try to answer question '" + question + "' for user with email '" + email + "' ...");
        } else if (channelType == ChannelType.FCM_TOKEN&& fcmToken != null) {
            log.info("Try to answer question '" + question + "' for user with FCM token '" + fcmToken + "' ...");
        } else if (channelType == ChannelType.SLACK) {
            String slackChannelId = "TODO";
            String slackUserName = "TODO";
            log.info("Try to answer question '" + question + "' which was asked on Slack channel '" + slackChannelId + "' by user '" + slackUserName + "' ...");
        } else if(channelType == ChannelType.MS_TEAMS) {
            log.info("Try to answer question '" + question + "' which was asked on MS Teams '" + "TODO" + "' by user '" + questionerUser.getId() + "' ...");
        } else if(channelType == ChannelType.MATRIX) {
            log.info("Try to answer question '" + question + "' which was asked in the Matrix room '" + "TODO" + "' by user '" + "TODO" + "' ...");
        } else if (channelType == ChannelType.WEBHOOK) {
            log.info("Try to answer question '" + question + "' which was asked in combination with Webhook channel data ...");
        } else {
            log.warn("Neither email nor FCM token nor Slack channel Id nor MS Teams information nor Matrix room ID nor Webhook echo data provided!");
        }

        String uuid = null;
        try {
            String questionerUserId = null;
            if (questionerUser != null) {
                questionerUserId = questionerUser.getId();
            }
            uuid = dataRepositoryService.saveResubmittedQuestion(question, questionerUserId, questionerLanguage, channelType, channelRequestId, email, fcmToken, answerLinkType, remoteAddress, domain.getId());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        notifyExpertsToAnswerResubmittedQuestion(question, questionerUser, channelType, email, fcmToken, questionerLanguage, uuid, domain);
        return uuid;
    }

    /**
     * @return fake / synthetic answer
     */
    public String getFakeAnswer(Sentence question, Context domain) throws AccessDeniedException {
        if (!isMemberOrAdmin(domain.getId())) {
            throw new AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        return aiService.getFakeAnswer(question, domain);
    }

    /**
     * Notify experts to answer resubmitted question
     * @param question Resubmitted question
     * @param email Email of user which submitted question
     * @param fcmToken FCM token associated with mobile device of user
     * @param questionerLanguage Language of user asking question
     * @param uuid UUID of resubmitted question
     * @param context Domain
     */
    private void notifyExpertsToAnswerResubmittedQuestion(String question, User questionerUser, ChannelType channelType, String email, String fcmToken, Language questionerLanguage, String uuid, Context context) {
        try {
            String[] emailsTo = getMailNotificationAddresses(context.getId());
            if (emailsTo.length == 0) {
                log.info("No experts configured for domain '" + context.getId() + "'.");
            }
            for (int i = 0; i < emailsTo.length; i++) {
                // TODO: Get language of Backend Team Member and send notification accordingly
                String backendTeamMemberLanguage = "en";
                mailerService.send(emailsTo[i], context.getMailSenderEmail(), "[" + context.getMailSubjectTag() + "] Please answer question ...", getNotificationBody(backendTeamMemberLanguage, question, questionerUser, channelType, email, fcmToken, questionerLanguage, uuid, context), true);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Notify experts to approve better answer
     * @param qna QnA with better answer
     * @param domain Domain associated with
     * @param askedQuestion Asked question which received wrong answer
     */
    public void notifyExpertsToApproveProvidedAnswer(Answer qna, Context domain, AskedQuestion askedQuestion) {
        try {
            String[] emailsTo = getMailNotificationAddresses(domain.getId());
            if (emailsTo.length == 0) {
                log.info("No experts configured for domain '" + domain.getId() + "'.");
            }
            for (int i = 0; i < emailsTo.length; i++) {
                // TODO: Get language of Backend Team Member and send notification accordingly
                String backendTeamMemberLanguage = "en";
                String body = getApproveBetterAnswerNotificationBody(domain, qna.getUuid(), askedQuestion.getQuestion(), backendTeamMemberLanguage);
                mailerService.send(emailsTo[i], domain.getMailSenderEmail(), "[" + domain.getMailSubjectTag() + "] Please approve better answer ...", body, true);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Get notification body text
     * @param backendTeamMemberLanguage Language of backend team member which receives notification re resubmitted question
     * @param email Email of user which submitted question
     * @param fcmToken FCM token associated with mobile device of user
     * @param userLanguage Language of user which submitted question
     */
    private String getNotificationBody(String backendTeamMemberLanguage, String question, User questionerUser, ChannelType channelType, String email, String fcmToken, Language userLanguage, String uuid, Context context) throws Exception {
        TemplateArguments tmplArgs = new TemplateArguments(context, null);
        tmplArgs.add("user_email", email);
        tmplArgs.add("fcm_token", fcmToken);
        if (questionerUser != null) {
            tmplArgs.add("user_id", questionerUser.getId());
            tmplArgs.add("user_name", questionerUser.getUsername());
            tmplArgs.add("user_firstname", questionerUser.getFirstname());
            tmplArgs.add("user_lastname", questionerUser.getLastname());

            // INFO: First and last name together
            if (questionerUser.getFirstname() != null || questionerUser.getLastname() != null) {
                tmplArgs.add("user_firstname_lastname", getFirstLastName(questionerUser));
            }
        }
        tmplArgs.add("user_language", userLanguage);
        tmplArgs.add("question", question);
        tmplArgs.add("answer_question_link", context.getHost() + "/#/answer-question" + "?uuid=" + uuid);

        log.info("Backend team member language: " + backendTeamMemberLanguage);
        Language lang = Language.valueOf(backendTeamMemberLanguage);
        StringWriter writer = new StringWriter();
        if (email != null) {
            Template emailTemplate = mailerService.getTemplate("question-resubmitted_email_", lang, context);
            emailTemplate.process(tmplArgs.getArgs(), writer);
        } else if (fcmToken != null) {
            Template fcmTokenTemplate = mailerService.getTemplate("question-resubmitted_fcm_token_", lang, context);
            fcmTokenTemplate.process(tmplArgs.getArgs(), writer);
        } else if (channelType == ChannelType.SLACK) {
            Template slackChannelTemplate = mailerService.getTemplate("question-resubmitted_slack_channel_", lang, context);
            String slackChannelId = null;
            if (slackChannelId != null) {
                tmplArgs.add("slack_channel_id", slackChannelId);
            } else {
                log.error("No Slack channel Id!");
                tmplArgs.add("slack_channel_id", "ERROR_slack_channel_id");
            }
            String slackUserName = null;
            if (slackUserName != null) {
                tmplArgs.add("slack_user_name", slackUserName);
            } else {
                log.error("No Slack user name!");
                tmplArgs.add("slack_user_name", "ERROR_slack_user_name");
            }
            log.info("Template arguments: " + tmplArgs);
            slackChannelTemplate.process(tmplArgs.getArgs(), writer);
        } else if (channelType == ChannelType.MS_TEAMS) {
            Template msTeamsTemplate = mailerService.getTemplate("question-resubmitted_msteams_", lang, context);
            msTeamsTemplate.process(tmplArgs.getArgs(), writer);
        } else if (channelType == ChannelType.MATRIX) {
            // TODO: Get matrix room Id using channel entry Id
            tmplArgs.add("matrix_room_id", "TODO");
            Template matrixTemplate = mailerService.getTemplate("question-resubmitted_matrix_", lang, context);
            matrixTemplate.process(tmplArgs.getArgs(), writer);
        } else if (channelType == ChannelType.WEBHOOK) {
            Template matrixTemplate = mailerService.getTemplate("question-resubmitted_webhook_", lang, context);
            matrixTemplate.process(tmplArgs.getArgs(), writer);
        } else {
            log.warn("Neither email nor FCM token nor Slack channel Id nor MS Teams information nor Matrix room ID nor Webhook echo data provided!");
            Template unknownTemplate = mailerService.getTemplate("question-resubmitted_unknown_", lang, context);
            unknownTemplate.process(tmplArgs.getArgs(), writer);
        }
        return writer.toString();
    }

    /**
     * Get first name and last name of user together
     */
    private String getFirstLastName(User user) {
        StringBuilder sb = new StringBuilder();
        if (user.getFirstname() != null) {
            sb.append(user.getFirstname());
            if (user.getLastname() != null) {
                sb.append(" ");
            }
        }
        if (user.getLastname() != null) {
            sb.append(user.getLastname());
        }
        return sb.toString();
    }

    /**
     * Check whether domain / knowledge base with a particular domain id exists
     * @param id Domain Id
     * @return true when domain exists and false otherwise
     */
    public boolean existsContext(String id) {
        return getDomainDirectory(id).isDirectory();
    }

    /**
     * Get all domain IDs (TODO: Scalability!)
     */
    public String[] getContextIDs() {
        return new File(contextsDataPath).list();
    }

    /**
     * Get all domain IDs a particular user is member of
     * @param user User
     */
    public String[] getDomainIDsUserIsMemberOf(User user) {
        // INFO: Migration code
        migrateDomainIDsUserIsMemberOf(user.getId());

        File domainsFile = getUserDomainsListFile(user.getId());
        return xmlService.getDomainIDsUserIsMemberOf(domainsFile);
    }

    /**
     * @param userId User Id
     * @return all domain IDs user is member of
     */
    private void migrateDomainIDsUserIsMemberOf(String userId) {
        File domainsFile = getUserDomainsListFile(userId);

        if (!domainsFile.exists()) {
            log.info("Migrate list of domain IDs where user '" + userId + "' is member of to: " + domainsFile.getAbsolutePath());

            String[] domainIDs = getContextIDs();

            for (int i = 0; i < domainIDs.length; i++) {
                String domainId = domainIDs[i];
                try {
                    if (isUserMemberOfDomain(userId, domainId)) {
                        xmlService.connectDomainWithUser(domainsFile, domainId);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            if (!domainsFile.exists()) {
                try {
                    xmlService.createUserDomainsFile(domainsFile);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } else {
            log.info("Migration not necessary.");
        }
    }

    /**
     * @param userId User Id
     * @param domainId Domain Id
     */
    public void setPersonalDomainId(String userId, String domainId) {
        log.info("Set domain '" + domainId + "' as personal domain (MyKatie) of user '" + userId + "' ...");
        try {
            // INFO: Migration code
            migrateDomainIDsUserIsMemberOf(userId);

            File domainsFile = getUserDomainsListFile(userId);
            xmlService.setPersonalDomainId(domainsFile, domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * @param id User Id
     * @return MyKatie domain Id of user and null if MyKatie domain Id is not configured for user
     */
    public String getPersonalDomainId(String id) throws Exception {
        log.info("Get domain Id which is personal domain (MyKatie) of user '" + id + "' ...");

        // INFO: Migration code
        migrateDomainIDsUserIsMemberOf(id);

        File domainsFile = getUserDomainsListFile(id);
        return xmlService.getPersonalDomainId(domainsFile);
    }

    /**
     * Get a particular domain, whereas authorization is being checked
     * @param id Domain Id, e.g. "a84581a3-302f-4b73-80d9-0e60da5238f9"
     * @return domain associated with provided Id
     */
    public Context getDomain(String id) throws  Exception {
        if (!isMemberOrAdmin(id)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + id + "', nor has role " + Role.ADMIN + "!");
        }

        return getContext(id);
    }

    /**
     * Get a particular domain without checking authorization
     * @param domainId Domain Id, e.g. 'ROOT' or '45c6068a-e94b-46d6-zfa1-938f755d446g', whereas when domain Id is null, then return ROOT domain
     * @return domain associated with provided Id and ROOT domain when Id is null
     */
    public Context getContext(String domainId) throws Exception {
        //private Context getContext(String domainId) throws Exception {

        Context domain = xmlService.parseContextConfig(domainId);

        String tagName = dataRepositoryService.getTagName(domainId);
        if (tagName != null) {
            domain.setTagName(tagName);
        }

        return domain;
    }

    /**
     * @param name Tag name, e.g. "apache-lucene"
     * @return domain associated with tag name
     */
    public Context getDomainByTagName(String name) throws Exception {
        String domainId = dataRepositoryService.getDomainIdForTagName(name);
        if (domainId != null) {
            return getContext(domainId);
        } else {
            throw new Exception("No domain for tag name '" + name + "'!");
        }
    }

    /**
     * Autocomplete taxonomy entry
     * @param domainId Domain Id
     * @param incompleteTerm Incomplete taxonomy term, e.g. "birth"
     * @return array of suggested terms, e.g. "birthdate", "birthplace"
     */
    public String[] getSuggestedTaxonomyEntries(String domainId, String incompleteTerm) throws Exception {

        Context domain = getContext(domainId);

        if (!isMemberOrAdmin(domainId)) {
            if (domain.getAnswersGenerallyProtected()) {
                log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
                throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
            } else {
                log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "', but answers of domain '" + domainId + "' are generally public.");
            }
        }

        String[] suggestions = taxonomyService.getSuggestions(domain, incompleteTerm);


        return suggestions;
    }

    /**
     * Classify a text
     * @param domainId Domain Id
     * @param text Text, e.g. "When was Michael born?"
     * @return array of taxonomy terms, e.g. "birthdate", "michael"
     */
    public String[] classifyText(String domainId, String text) throws Exception {

        Context domain = getContext(domainId);

        if (!isMemberOrAdmin(domainId)) {
            if (domain.getAnswersGenerallyProtected()) {
                log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
                throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
            } else {
                log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "', but answers of domain '" + domainId + "' are generally public.");
            }
        }

        // TODO: Classify text based on domain specific taxonomy
        String[] terms = new String[1];
        terms[0] = "not_implemented_yet";

        return terms;
    }

    /**
     * Autocomplete question
     * @param domainId Domain Id
     * @param incompleteQuestion Incomplete question, e.g. "highe"
     * @return array of suggested questions, e.g. "What is the highest mountain"
     */
    public com.wyona.katie.models.Question[] getSuggestedQuestions(String domainId, String incompleteQuestion) throws Exception {

        Context domain = getContext(domainId);

        if (!isMemberOrAdmin(domainId)) {
            if (domain.getAnswersGenerallyProtected()) {
                log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
                throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
            } else {
                log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "', but answers of domain '" + domainId + "' are generally public.");
            }
        }

        String[] suggestions = autoCompleteService.getSuggestions(domain, incompleteQuestion);

        SuggestedQuestions suggestedQuestions = new SuggestedQuestions();
        for (int i = 0; i < suggestions.length; i++) {
            suggestedQuestions.addQuestion(suggestions[i]);
        }


        return suggestedQuestions.getQuestions();
    }

    /**
     * Get taxonomy entries of a particular domain
     * @return array of taxonomy entries
     */
    public String[] getTaxonomyEntries(String domainId, int offset, int limit) throws Exception {
        Context domain = getContext(domainId);

        if (!isMemberOrAdmin(domainId)) {
            if (domain.getAnswersGenerallyProtected()) {
                log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
                throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
            } else {
                log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "', but answers of domain '" + domainId + "' are generally public.");
            }
        }

        return taxonomyService.getEntries(domain, offset, limit);
    }

    /**
     * Get autocompletion entries of a particular domain
     * @return array of autocompletion entries
     */
    public String[] getAutocompletionEntries(String domainId, int offset, int limit) throws Exception {
        Context domain = getContext(domainId);

        if (!isMemberOrAdmin(domainId)) {
            if (domain.getAnswersGenerallyProtected()) {
                log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
                throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
            } else {
                log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "', but answers of domain '" + domainId + "' are generally public.");
            }
        }

        return autoCompleteService.getEntries(domain, offset, limit);
    }

    /**
     * Add multiple entries to autocompletion index
     */
    public void addAutocompletionEntries(String domainId, String[] values) throws Exception {
        Context domain = getContext(domainId);

        if (!isMemberOrAdmin(domainId)) {
            log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        autoCompleteService.addEntries(domain, values);
    }

    /**
     * Add single entry to autocompletion index
     */
    public void addAutocompletionEntry(String domainId, String value) throws Exception {
        Context domain = getContext(domainId);

        if (!isMemberOrAdmin(domainId)) {
            log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        String[] values = new String[1];
        values[0] = value;
        autoCompleteService.addEntries(domain, values);
    }

    /**
     * Delete entry from autocompletion index
     * @param value Entry value, e.g. "mountain"
     */
    public void deleteAutocompletionEntry(String domainId, String value) throws Exception {
        Context domain = getContext(domainId);

        if (!isMemberOrAdmin(domainId)) {
            log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        autoCompleteService.deleteEntry(domain, value);
    }

    /**
     * Add OneNote as knowledge source
     * Also see https://developers.glean.com/docs/indexing_api_getting_started/#set-up-a-datasource and https://developers.glean.com/docs/indexing_api_datasource_category/
     */
    public void addKnowledgeSourceOneNote(String domainId, String name, String apiToken, String tenant, String clientId, String clientSecret, String scope, String location) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        //Context domain = getContext(domainId);
        String ksUUID = knowledgeSourceXMLFileService.addOneNote(domainId, name, apiToken, tenant, clientId, clientSecret, scope, location);
    }

    /**
     * Add SharePoint as knowledge source
     * Also see https://developers.glean.com/docs/indexing_api_getting_started/#set-up-a-datasource and https://developers.glean.com/docs/indexing_api_datasource_category/
     * @param baseUrl SharePoint base URL, e.g. "https://wyona.sharepoint.com"
     */
    public void addKnowledgeSourceSharePoint(String domainId, String name, String apiToken, String tenant, String clientId, String clientSecret, String scope, String siteId, String baseUrl) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        //Context domain = getContext(domainId);
        String ksUUID = knowledgeSourceXMLFileService.addSharePoint(domainId, name, apiToken, tenant, clientId, clientSecret, scope, siteId, baseUrl);
    }

    /**
     * Add Website as knowledge source
     */
    public void addKnowledgeSourceWebsite(String domainId, String name, String seedUrl, String[] urls, Integer chunkSize, Integer chunkOverlap) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        //Context domain = getContext(domainId);
        String ksUUID = knowledgeSourceXMLFileService.addWebsite(domainId, name, seedUrl, urls, chunkSize, chunkOverlap);
    }

    /**
     * Add third-party RAG as knowledge source
     */
    public void addKnowledgeSourceThirdPartyRAG(String domainId, String name, String endpointUrl) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        //Context domain = getContext(domainId);
        String ksUUID = knowledgeSourceXMLFileService.addThirdPartyRAG(domainId, name, endpointUrl);
    }

    /**
     *
     */
    public void deleteKnowledgeSource(String domainId, String ksId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        knowledgeSourceXMLFileService.delete(domainId, ksId);
    }

    /**
     *
     */
    protected KnowledgeSourceMeta getKnowledgeSource(String domainId, String ksId, KnowledgeSourceConnector connector) throws Exception {
        KnowledgeSourceMeta ksMeta = getKnowledgeSource(domainId, ksId);
        if (ksMeta != null) {
            if (!ksMeta.getConnector().equals(connector)) {
                log.error("Knowledge source '" + domainId + " / " + ksId + "' does not use connector '" + connector + "'!");
                return null;
            }
            return ksMeta;
        }

        log.warn("No such knowledge source: " + ksId);
        return null;
    }

    /**
     *
     */
    public KnowledgeSourceMeta getKnowledgeSource(String domainId, String ksId) throws Exception {
        KnowledgeSourceMeta[] knowledgeSourceMetas = getKnowledgeSources(domainId, false);
        for (KnowledgeSourceMeta ksMeta : knowledgeSourceMetas) {
            if (ksMeta.getId().equals(ksId)) {
                return ksMeta;
            }
        }

        log.warn("No such knowledge source: " + ksId);
        return null;
    }

    /**
     *
     */
    protected void deleteKnowledgeSourceItem(Context domain, KnowledgeSourceMeta ksMeta, String key) {
        String katieUUID = foreignKeyIndexService.getUUID(domain, ksMeta, key);
        try {
            if (katieUUID != null && existsQnA(katieUUID, domain)) {
                deleteTrainedQnA(domain, katieUUID);
            }
            foreignKeyIndexService.deleteForeignKey(domain, ksMeta, key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Get knowledge sources of a particular domain
     * @param domainId Domain Id
     * @param checkAuthorization True when authorization must be checked
     */
    public KnowledgeSourceMeta[] getKnowledgeSources(String domainId, boolean checkAuthorization) throws Exception {
        if (checkAuthorization && !isMemberOrAdmin(domainId)) {
            log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "'.");
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        return knowledgeSourceXMLFileService.getKnowledgeSources(domainId);
    }

    /**
     * Toggle whether knowledge source is enabled or disabled
     */
    public KnowledgeSourceMeta toggleKnowledeSourceEnabled(String domainId, String ksId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        KnowledgeSourceMeta ksMeta = getKnowledgeSource(domainId, ksId);
        if (ksMeta != null) {
            ksMeta.setIsEnabled(!ksMeta.getIsEnabled());
            knowledgeSourceXMLFileService.toggleEnabled(domainId, ksId);
            return ksMeta;
        } else {
            log.warn("No such knowledge source: " + ksId);
            return null;
        }
    }

    /**
     * Toggle whether knowledge source is enabled or disabled
     * @param token API Token, e.g. "eyJ0eXAiOiJKV1QiLCJub....Fa7TwAuA8bUl1cxw"
     */
    public KnowledgeSourceMeta setMicrosoftGraphAPIToken(String domainId, String ksId, String token) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            log.info("User has neither role " + Role.ADMIN + ", nor is member of domain '" + domainId + "' and answers of domain '" + domainId + "' are generally protected.");
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        KnowledgeSourceMeta ksMeta = getKnowledgeSource(domainId, ksId);
        if (ksMeta != null) {
            ksMeta.setMicrosoftGraphApiToken(token);
            knowledgeSourceXMLFileService.setMsGraphApiTokenAttr(domainId, ksId, token);
            return ksMeta;
        } else {
            log.warn("No such knowledge source: " + ksId);
            return null;
        }
    }

    /**
     * Get insights summary of a particular domain in the specified time period
     * @param domainId Domain Id
     * @param lastNumberOfDays Last number of days, e.g. 30 or -1 when for all time
     */
    public DomainInsights getInsightsSummary(String domainId, int lastNumberOfDays) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        DomainInsights insights = new DomainInsights();
        Context domain = getContext(domainId);
        String[] languages = domain.getFAQLanguages();

        Date end = new Date();
        log.debug("End of period: " + end + ", " + end.getTime());
        Date start = new Date(0);
        if (lastNumberOfDays > 0) {
            long daysInMilliseconds = java.util.concurrent.TimeUnit.DAYS.toMillis(lastNumberOfDays);
            log.debug("Days in milliseconds: " + daysInMilliseconds);
            long startTime = end.getTime() - daysInMilliseconds;
            start = new Date(startTime);
            log.debug("Start of period: " + start + ", " + startTime);
        }
        log.info("Get insights summary for time period '" + start + "' to '" + end + "' ...");

        for (String language: languages) {
            insights.addFaqPageviews(language, analyticsService.getFAQPageviews(domainId, language, start, end));
        }

        insights.setNumberOfQnAs(getNumberOfQnAs(domain));

        insights.setNumberOfAskedQuestions(analyticsService.getNumberOfAskedQuestions(domainId, start, end));

        insights.setNumberOfNextBestAnswer(analyticsService.getNumberOfNextBestAnswer(domainId, start, end));

        insights.setNumberOfReceivedMessages(analyticsService.getNumberOfReceivedMessages(domainId, start, end));

        insights.setNumberOfAnsweredQuestions(analyticsService.getNumberOfAnsweredQuestions(domainId, start, end));

        insights.setNumberOfAskedQuestionsWithoutAnswer(insights.getNumberofaskedquestions() - insights.getNumberOfAnsweredQuestions());

        insights.setNumberOfQuestionsSentToExpert(analyticsService.getNumberOfQuestionsSentToExpert(domainId, start, end));

        insights.setNumberOfPositiveFeedbacks(analyticsService.getNumberOfPositiveFeedbacks(domainId, start, end));

        insights.setNumberOfNegativeFeedbacks(analyticsService.getNumberOfNegativeFeedbacks(domainId, start, end));

        insights.setNumberOfApprovedAnswers(analyticsService.getNumberOfApprovedAnswers(domainId, start, end));
        insights.setNumberOfDiscardedAnswers(analyticsService.getNumberOfDiscardedAnswers(domainId, start, end));
        insights.setNumberOfCorrectedAnswers(analyticsService.getNumberOfCorrectedAnswers(domainId, start, end));
        insights.setNumberOfIgnoredAnswers(analyticsService.getNumberOfIgnoredAnswers(domainId, start, end));

        return insights;
    }

    /**
     * Get number of QnAs which a particular domain contains
     */
    private int getNumberOfQnAs(Context domain) {
        File questionsAnswersDir = domain.getQuestionsAnswersDataPath();
        if (questionsAnswersDir.isDirectory()) {
            String username = authService.getUsername();

            String[] answerUUIDs = questionsAnswersDir.list();
            return answerUUIDs.length;
        } else {
            return 0;
        }
    }

    /**
     * Get insights history of a particular domain in the specified time period
     * @param domainId Domain Id
     * @param lastNumberOfDays Last number of days, e.g. 30 or -1 when for all time
     */
    public NgxChartsSeries[] getInsightsHistory(String domainId, int lastNumberOfDays, EventType type, Interval interval) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        if (interval !=  Interval.DAY) {
            throw new Exception("Interval '" + interval + "' not implemented yet, only interval '" + Interval.DAY + "' implemented!");
        }

        NgxChartsSeries series = new NgxChartsSeries("" + type);
        Date today = new Date();
        long dayInMilliseconds = java.util.concurrent.TimeUnit.DAYS.toMillis(1);
        for (int i = 0; i < lastNumberOfDays; i++) {
            int numberOfDaysAgo = lastNumberOfDays - i;
            long daysAgoInMilliseconds = java.util.concurrent.TimeUnit.DAYS.toMillis(numberOfDaysAgo);
            Date start = new Date(today.getTime() - daysAgoInMilliseconds);
            Date end = new Date(today.getTime() - daysAgoInMilliseconds + dayInMilliseconds);

            log.debug("Get number of events of type '" + type + "' (Domain: " + domainId + ") for time period '" + start + "' to '" + end + "' ...");

            int numberOfEvents = getNumberOfEvents(domainId, type, start, end);
            series.addDataPoint(new NgxChartsDataPoint(end, numberOfEvents));
        }

        List<NgxChartsSeries> multiSeries = new ArrayList<NgxChartsSeries>();
        multiSeries.add(series);

        return multiSeries.toArray(new NgxChartsSeries[0]);
    }

    /**
     *
     */
    private int getNumberOfEvents(String domainId, EventType type, Date start, Date end) throws Exception {
        if (type == EventType.QUESTION_SENT_TO_EXPERT) {
            return analyticsService.getNumberOfQuestionsSentToExpert(domainId, start, end);
        } else if(type == EventType.MESSAGE_RECEIVED) {
            return analyticsService.getNumberOfReceivedMessages(domainId, start, end);
        } else if(type == EventType.FEEDBACK_1) {
            return analyticsService.getNumberOfNegativeFeedbacks(domainId, start, end);
        } else if(type == EventType.FEEDBACK_10) {
            return analyticsService.getNumberOfPositiveFeedbacks(domainId, start, end);
        } else if(type == EventType.GET_FAQ) {
            // TODO: Make language selectable
            return analyticsService.getFAQPageviews(domainId, "en", start, end);
        } else if (type == EventType.ASKED_QUESTION) {
            return analyticsService.getNumberOfAskedQuestions(domainId, start, end);
        } else if (type == EventType.ANSWERED_QUESTION) {
            return analyticsService.getNumberOfAnsweredQuestions(domainId, start, end);
        } else if (type == EventType.NEXT_BEST_ANSWER) {
            return analyticsService.getNumberOfNextBestAnswer(domainId, start, end);
        } else {
            log.error("No such event type '" + type + "' supported!");
            return -1;
        }
    }

    /**
     * Get insights re users of a particular domain
     * @param domainId Domain Id
     */
    public String[] getInsightsUsers(String domainId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        String[] emails  = dataRepositoryService.getUserInfo(domainId);

        return emails;
    }

    /**
     * Get URLs of webpages which contain imported QnAs
     */
    public String[] getThirdPartyUrls(String domainId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        Context domain = getContext(domainId);
        File urlsBaseDir = domain.getURLsDataPath();

        List<String> urls = new ArrayList<String>();

        if (urlsBaseDir.isDirectory()) {
            File[] metaFiles = Utils.searchFiles(urlsBaseDir, Context.IMPORT_URL_META_FILE);
            for (File metaFile: metaFiles) {
                log.info("Meta file: " + metaFile.getAbsolutePath());
                URLMeta urlMeta = xmlService.getUrlMeta(metaFile);
                urls.add(urlMeta.getUrl());
            }
        }

        return urls.toArray(new String[0]);
    }

    /**
     * Get webhooks of a particular domain
     * @param domainId Domain Id
     */
    public Webhook[] getWebhooks(String domainId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        return xmlService.getWebhooks(domainId);
    }

    /**
     * Get a particular webhook
     */
    public Webhook getWebhook(String domainId, String webhookId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        Webhook[] webhooks = xmlService.getWebhooks(domainId);
        for (Webhook webhook : webhooks) {
            if (webhook.getId().equals(webhookId)) {
                return webhook;
            }
        }

        throw new Exception("Domain '" + domainId + "' does not contain webhook with id '" + webhookId + "'!");
    }

    /**
     * Get deliveries of a particular webhook
     * @param domainId Domain Id
     * @param webhookId Webhook Id
     */
    public WebhookRequest[] getWebhookDeliveries(String domainId, String webhookId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        return xmlService.getWebhookDeliveries(domainId, webhookId);
    }

    /**
     * Delete a particular webhook
     */
    public void deleteWebhook(String domainId, String webhookId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }
        xmlService.deleteWebhook(domainId, webhookId);
    }

    /**
     * Toggle a particular webhook to be active or inactive
     */
    public Webhook toggleWebhook(String domainId, String webhookId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        xmlService.toggleWebhook(domainId, webhookId);

        return getWebhook(domainId, webhookId);
    }

    /**
     * Add webhook
     * @param webhook Webhook data
     * @return new webhook
     */
    public Webhook addWebhook(String domainId, Webhook webhook) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }
        if (!webhook.getPayloadURL().startsWith("https:")) {
            throw new Exception("Payload URL '" + webhook.getPayloadURL() + "' does not seem to use SSL!");
        }

        webhook.setId(UUID.randomUUID().toString());
        webhook.setContentType("application/json");
        webhook.setEnabled(true);

        xmlService.addWebhook(domainId, webhook);

        return webhook;
    }

    /**
     * Get public display information of a particular domain
     * @param domainId Domain Id, e.g. 'wyona', whereas when domain Id is null, then return public display information of ROOT domain
     * @return public display information of domain
     */
    public DomainDisplayInformation getDomainDisplayInformationt(String domainId) throws Exception {
        // INFO: getDomain(String) is not used, because of authorization check
        Context domain =  xmlService.parseContextConfig(domainId);
        domain.setTagName(dataRepositoryService.getTagName(domainId));

        return new DomainDisplayInformation(domain.getId(), domain.getName(), domain.getTagName());
    }

    /**
     * Get list of moderator email addresses
     */
    public String[] getModeratorEmailAddresses(String domainId) {
        User[] moderators = null;
        try {
            moderators = getModerators(domainId, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        List<String> moderatorEmails = new ArrayList<String>();
        for (int i = 0; i < moderators.length; i++) {
            if (moderators[i].getEmail() != null) { // INFO: A Slack or MS Teams user might not have an email address
                moderatorEmails.add(moderators[i].getEmail());
            } else {
                log.warn("Moderator user with Id '" + moderators[i].getId() + "' has no email address configured!");
            }
        }
        return moderatorEmails.toArray(new String[0]);
    }

    /**
     * Get list of expert email addresses of a particular domain
     */
    public String[] getMailNotificationAddresses(String domainId) {
        User[] experts = null;
        try {
            experts = getExperts(domainId, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        List<String> emails = new ArrayList<String>();
        for (int i = 0; i < experts.length; i++) {
            if (experts[i].getEmail() != null) { // INFO: A Slack or MS Teams user might not have an email address
                emails.add(experts[i].getEmail());
            } else {
                log.warn("Expert user with Id '" + experts[i].getId() + "' has no email address configured!");
            }
        }
       return emails.toArray(new String[0]);
    }

    /**
     * Invite user to a particular domain
     *
     * @param username Username, e.g. "michael.wechner@wyona.com" or "29:1frI-wsBCNyHx_KFsXe27D0m6Fa9FyCcdEwKwus9eUxABf1DcdkUzk71NDyA-38BRNicrMLTn5-2Cbs9w55csEg"
     * @param domainId Id of domain for which user is being invited
     * @param email E-Mail of user to be invited, whereas only necessary when user does not have an account yet
     *
     * @return user object if user has been added as member to domain, return null otherwise
     */
    public User inviteUserToBecomeMemberOfDomain(String username, String domainId, String email) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        User signedInUser = authService.getUser(false, false);

        User user = iamService.getUserByUsername(new Username(username), false, false);
        if (user != null) {
            log.info("User '" + user.getUsername() + "' has already a Katie account");
            if (!isUserMemberOfDomain(user.getId(), domainId)) {
                // TODO/TBD: Maybe we should ask user for confirmation?!
                addMember(user.getId(), false, false, null, domainId);
                sendInvitation(signedInUser, user.getEmail(), user.getUsername(), domainId);
                return user;
            } else {
                throw new UserAlreadyMemberException("User '" + user.getUsername() + "' is already a member of domain '" + domainId + "'.");
            }
        } else {
            log.info("User '" + username + "' does not have an account yet.");
            if (email != null) {
                sendInvitation(signedInUser, email, null, domainId);
            } else {
                log.warn("No email provided to invite user which does not have a Katie account yet.");
            }
            return null;
        }
    }

    /**
     * Notify user by email that user as been added as member to a particular domain
     *
     * @param signedInUser Signed in user inviting another user
     * @param email Email of invited user
     * @param username Username of invited user if account exists already, otherwise null
     * @param domainId Domain Id for which user has been invited
     */
    public void sendInvitation(User signedInUser, String email, String username, String domainId) throws Exception {
        log.info("Send invitation ...");
        Context domain = getContext(domainId);
        // TODO: Make language settable
        String body = getInvitationBody(Language.en, signedInUser, email, username, domain);

        String hostUserName = signedInUser.getUsername();
        if (signedInUser.getFirstname() != null && signedInUser.getFirstname().length() > 0) {
            hostUserName = signedInUser.getFirstname();
        }
        String subject = "[" + domain.getMailSubjectTag() + "] " + hostUserName + " invited you to Katie";
        log.info("Subject: " + subject);

        mailerService.send(email, domain.getMailSenderEmail(), subject, body, true);
    }

    /**
     * @param language Language of invitation
     * @param hostUser User which sends invitation
     * @param email Email of invited user to which invitation will be sent
     * @param username Username of invited user if account exists already, otherwise null
     * @param domain Domain for which this invitation will be sent
     */
    private String getInvitationBody(Language language, User hostUser, String email, String username, Context domain) throws Exception {
        TemplateArguments tmplArgs = new TemplateArguments(domain, null);
        if (hostUser.getFirstname() != null && hostUser.getFirstname().length() > 0) {
            tmplArgs.add("host_name", hostUser.getFirstname());
        } else {
            tmplArgs.add("host_name", hostUser.getUsername());
        }
        tmplArgs.add("host_email", hostUser.getEmail());
        tmplArgs.add("domain_name", domain.getName());
        String joinLink = domain.getHost() + "/#/";
        if (username != null) {
            joinLink = joinLink + "login?username=" + username;
        } else {
            joinLink = joinLink + "register?email=" + email;
        }
        tmplArgs.add("join_link", joinLink);

        StringWriter writer = new StringWriter();
        Template template = mailerService.getTemplate("invite-user-to-domain_", language, domain);
        template.process(tmplArgs.getArgs(), writer);
        return writer.toString();
    }

    /**
     * @param isExpert If true, then user is expert re this particular domain
     * @param isModerator If true, then user is moderator of this particular domain
     * @param role Domain role
     */
    public void addMember(String userId, boolean isExpert, boolean isModerator, RoleDomain role, String domainId) throws Exception {
        log.info("Add user '" + userId + "' to domain '" + domainId + "' ..." );
        if (!isUserMemberOfDomain(userId, domainId)) {
            xmlService.addDomainMember(userId, isExpert, isModerator, role, domainId);
            xmlService.connectDomainWithUser(getUserDomainsListFile(userId), domainId);
        } else {
            throw new Exception("User '" + userId + "' is already member of domain '" + domainId + "'!");
        }
    }

    /**
     * Get file containing list of domain IDs which user is member of
     * @param userId User Id
     */
    private File getUserDomainsListFile(String userId) {
        return new File(iamService.getIAMDataPath(), userId + "/domains.xml");
    }

    /**
     * @return true when user is member of a particular domain and false otherwise
     */
    public boolean isUserMemberOfDomain(String userId, String domainId) {
        try {
            return xmlService.isUserMemberOfDomain(userId, domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Reindex in background "all" QnAs of a particular domain
     * @param domainId Domain Id
     * @param detectDuplicatedQuestionsImpl New detect duplicated question implementation
     * @param queryServiceBaseUrl Query service base URL when the "query service implementation" is set
     * @param queryServiceToken Query service token / key / secret
     * @param embeddingImpl Embedding implementation when LUCENE_VECTOR_SEARCH is selected as search implementation
     * @param apiToken Embedding implementation API token
     * @param indexAlternativeQuestions When set to true, then alternative questions are also indexed
     * @param indexAllQnAs When set to true, then index all QnAs, also the ones which were not indexed yet
     * @param processId Background process UUID
     * @param userId Id of signed in user
     */
    @Async
    public void reindexInBackground(String domainId, DetectDuplicatedQuestionImpl detectDuplicatedQuestionsImpl, String queryServiceBaseUrl, String queryServiceToken, EmbeddingsImpl embeddingImpl, String apiToken, boolean indexAlternativeQuestions, boolean indexAllQnAs, String processId, String userId, int throttleTimeInMillis) {
        if (existsReindexLock(domainId)) {
            String existingProcessId = getReindexProcessId(domainId);
            log.warn("Reindexing of domain '" + domainId + "' already in progress (Process Id: " + existingProcessId + "), therefore no other reindex process will be started.");
            return;
        }

        try {
            createReindexLock(domainId, processId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        backgroundProcessService.startProcess(processId, "Reindex domain '" + domainId + "'.", userId);

        // TEST: Uncomment lines below to test thread
        /*
        try {
            for (int i = 0; i < 5; i++) {
                log.info("Sleep for 2 seconds ...");
                Thread.sleep(2000);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
         */

        try {
            reindex(domainId, detectDuplicatedQuestionsImpl, queryServiceBaseUrl, queryServiceToken, embeddingImpl, apiToken, indexAlternativeQuestions, indexAllQnAs, processId, throttleTimeInMillis);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
        }

        backgroundProcessService.stopProcess(processId);
        removeReindexLock(domainId);
    }

    /**
     * @param domainId Domain Id
     */
    private void createReindexLock(String domainId, String processId) throws Exception {
        File reindexLockFile = new File(getDomainDirectory(domainId), Context.REINDEX_LOCK_FILE);

        Utils.saveText(processId, reindexLockFile, false);
    }

    /**
     * @return true when reindex lock file exists and false otherwise
     */
    public boolean existsReindexLock(String domainId) {
        File reindexLockFile = new File(getDomainDirectory(domainId), Context.REINDEX_LOCK_FILE);
        if (reindexLockFile.isFile()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     */
    public String getReindexProcessId(String domainId) {
        return xmlService.getReindexProcessId(domainId);
    }

    /**
     *
     */
    private void removeReindexLock(String domainId) {
        File reindexLockFile = new File(getDomainDirectory(domainId), Context.REINDEX_LOCK_FILE);
        reindexLockFile.delete();
    }

    /**
     * Reindex "all" QnAs of a particular domain
     * @param domainId Domain Id
     * @param detectDuplicatedQuestionsImpl New detect duplicated question implementation
     * @param queryServiceBaseUrl Query service base URL when the "query service implementation" is set
     * @param queryServiceToken Query service token / key / secret
     * @param embeddingImpl Embedding implementation when LUCENE_VECTOR_SEARCH is selected as search implementation
     * @param apiToken Embedding implementation API token
     * @param indexAlternativeQuestions When set to true, then alternative questions are also indexed
     * @param indexAllQnAs When set to true, then index all QnAs, also the ones which were not indexed yet
     * @param processId Background process UUID
     * @param throttleTimeInMillis Time in milliseconds to throttle re-indexing, because OpenAI, Cohere, etc. do have rate limits
     */
    protected void reindex(String domainId, DetectDuplicatedQuestionImpl detectDuplicatedQuestionsImpl, String queryServiceBaseUrl, String queryServiceToken, EmbeddingsImpl embeddingImpl, String apiToken, boolean indexAlternativeQuestions, boolean indexAllQnAs, String processId, int throttleTimeInMillis) throws Exception {
        Context domain = getContext(domainId);
        log.info("Reindex domain '" + domainId + "' and replace current index implementation '" + domain.getDetectDuplicatedQuestionImpl() + "' by '" + detectDuplicatedQuestionsImpl+ "' ...");

        // TODO: Test API token

        // TODO: Unset at end, only when re-indexing was successful
        backgroundProcessService.updateProcessStatus(processId, "Unset current index / search implementation '" + domain.getDetectDuplicatedQuestionImpl() + "' ...");
        log.info("Unset previous implementation ...");
        try { // INFO: If previous AI Service is not available, then deleteTenant() can fail, but we would like to reindex with the new AI Service anyway
            aiService.deleteTenant(domain);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        domain.unsetDetectDuplicatedQuestionImpl();

        backgroundProcessService.updateProcessStatus(processId, "Set new index / search implementation '" + detectDuplicatedQuestionsImpl + "' (Embedding: " + embeddingImpl + ") ...");
        log.info("Set new implementation: " + detectDuplicatedQuestionsImpl + " (Embedding: " + embeddingImpl + ")");
        if (detectDuplicatedQuestionsImpl.equals(DetectDuplicatedQuestionImpl.QUERY_SERVICE)) {
            if (queryServiceBaseUrl != null) {
                domain.setQueryServiceUrl(queryServiceBaseUrl.trim());
                // TODO: Add queryServiceToken
                log.info("Query service base URL: " + domain.getQueryServiceUrl()); // INFO: Will be used as aiServiceBaseUrl below
            } else {
                throw new Exception("No query service base URL provided!");
            }
        }
        if (detectDuplicatedQuestionsImpl.equals(DetectDuplicatedQuestionImpl.AZURE_AI_SEARCH)) {
            if (queryServiceBaseUrl != null) {
                domain.setAzureAISearchEndpoint(queryServiceBaseUrl.trim());
                domain.setAzureAISearchAdminKey(queryServiceToken);
            } else {
                throw new Exception("No Azure AI Search endpoint prvided");
            }
        }

        String aiServiceBaseUrl = aiService.createTenant(domain, detectDuplicatedQuestionsImpl);
        log.info("AI Service base URL or Id: " + aiServiceBaseUrl);

        domain = setQuestionAnswerImplementation(domain, detectDuplicatedQuestionsImpl, aiServiceBaseUrl, embeddingImpl, apiToken);
        saveDomainConfig(domain);


        backgroundProcessService.updateProcessStatus(processId, "Select QnAs for training ...");
        File questionsAnswersDir = domain.getQuestionsAnswersDataPath();
        if (questionsAnswersDir.isDirectory()) {
            String[] answerUUIDs = questionsAnswersDir.list();
            List<QnA> qnas = new ArrayList<QnA>();
            for (int i = 0; i < answerUUIDs.length; i++) {
                log.info("Check whether QnA '" + answerUUIDs[i] + "' should be trained ...");
                try {
                    Answer qna = getQnA(null, answerUUIDs[i], domain);
                    if (indexAllQnAs || qna.isTrained()) {
                        qnas.add(new QnA(qna));
                    } else {
                        log.warn("QnA '" + qna.getUuid() + "' not trained yet, therefore will not be re-trained.");
                    }
                } catch(Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            int batchSize = 100; // TOOD: Make configurable
            backgroundProcessService.updateProcessStatus(processId, "Start indexing of " + qnas.size() + " QnAs ...");
            log.info("Re-train all " + qnas.size() + " QnAs which were trained before ...");
            train(qnas.toArray(new QnA[0]), domain, indexAlternativeQuestions, batchSize, processId, throttleTimeInMillis);
            backgroundProcessService.updateProcessStatus(processId, "Indexing of QnAs completed.");
        } else {
            log.warn("No QnAs to be trained yet.");
        }
    }

    /**
     * Remove user as member from domain
     * @param userId User Id of member
     */
    public void removeMember(String domainId, String userId, boolean checkAuthorization) throws Exception {
        if (checkAuthorization && !isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }
        xmlService.removeDomainMember(domainId, userId);
        xmlService.disconnectDomainFromUser(getUserDomainsListFile(userId), domainId);
    }

    /**
     * Remove user from domains where user is member of
     * @param id User Id
     * @param deleteDomains When true then delete domains where user is the only member
     * @return domain Ids where user was member of
     */
    public String[] removeUserFromDomains(String id, boolean deleteDomains) throws Exception {
        if (!isAdmin()) {
            throw new AccessDeniedException("User is either not signed in or has not role ADMIN!");
        }

        User user = iamService.getUserById(id, false);
        String[] domainIds = getDomainIDsUserIsMemberOf(user);
        for (String domainId: domainIds) {
            log.info("Remove user '" + id + "' from domain '" + domainId + "' ...");
            User[] members = getMembers(domainId, false);
            if (members.length == 1 && members[0].getId().equals(id)) {
                log.info("User '" + id + "' is the only member of domain '" + domainId + "'.");
                removeMember(domainId, id, true);
                if (deleteDomains) {
                    log.info("Delete domain '" + domainId + "' ...");
                    deleteDomain(domainId);
                }
            } else {
                log.info("Domain '" + domainId + "' has more members than just user '" + id + "'.");
                removeMember(domainId, id, true);
            }
        }
        return domainIds;
    }

    /**
     * Get all users which have access to a particular domain
     * @param domainId Domain Id
     * @param checkAuthorization Check whether user is authorized to get list of members
     * @return list of users which have access to a particular domain
     */
    public User[] getMembers(String domainId, boolean checkAuthorization) throws Exception {
        if (checkAuthorization && !isMemberOrAdmin(domainId)) {
            throw new AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        try {
            // TODO: Introduce class Member
            User[] members = xmlService.getMembers(domainId, false, false);
            List<User> users = new ArrayList<User>();
            for (int i = 0; i < members.length; i++) {
                User user = iamService.getUserByIdWithoutAuthCheck(members[i].getId());
                if (user != null) {
                    if (members[i].getIsModerator()) {
                        user.setIsModerator(true);
                    }
                    if (members[i].getIsExpert()) {
                        user.setIsExpert(true);
                    }
                    user.setDomainRole(members[i].getDomainRole());
                    users.add(user);
                } else {
                    log.error("No such user with Id '" + members[i].getId() + "'! Maybe user got deleted, but was not removed as member from domain '" + domainId + "'. If so, clean up domain members list.");
                }
            }
            return users.toArray(new User[0]);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get all moderators of a particular domain
     * @param domainId Domain Id
     * @return list of users which are moderators of particular domain
     */
    public User[] getModerators(String domainId, boolean checkAuthorization) throws AccessDeniedException  {
        if (checkAuthorization && !isMemberOrAdmin(domainId)) {
            throw new AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        try {
            // TODO: Introduce class Member
            User[] moderators = xmlService.getMembers(domainId, false, true);
            List<User> users = new ArrayList<User>();
            for (int i = 0; i < moderators.length; i++) {
                User user = iamService.getUserByIdWithoutAuthCheck(moderators[i].getId());
                user.setIsModerator(true);
                users.add(user);
            }
            return users.toArray(new User[0]);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get all experts of a particular domain
     * @param domainId Domain Id
     * @return list of users which are experts of particular domain
     */
    public User[] getExperts(String domainId, boolean checkAuthorization) throws AccessDeniedException {
        if (checkAuthorization && !isMemberOrAdmin(domainId)) {
            throw new AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        try {
            // TODO: Introduce class Member
            User[] experts = xmlService.getMembers(domainId, true, false);
            List<User> users = new ArrayList<User>();
            for (int i = 0; i < experts.length; i++) {
                User user = iamService.getUserByIdWithoutAuthCheck(experts[i].getId());
                user.setIsExpert(true);
                users.add(user);
            }
            return users.toArray(new User[0]);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Delete domain
     * @param id Domain Id
     * @return domain Id of deleted domain
     */
    public String deleteDomain(String id) throws Exception {
        // TODO: Check authorization here, whereas is currently protected by SecurityConfig
        // TODO: Add authorization check to Postman monitor
        return deleteDomain(id, authService.getUserId());
    }

    /**
     * @param userId Id of user deleting domain
     */
    protected String deleteDomain(String domainId, String userId) throws Exception {
        RoleDomain role = xmlService.getRole(userId, domainId);
        if (role != RoleDomain.OWNER) {
            String msg = "Member (User Id: " + userId + ") of domain '" + domainId + "' has not domain role " + RoleDomain.OWNER + ", therefore cannot delete domain!";
            log.info(msg);
            throw new java.nio.file.AccessDeniedException(msg);
        }

        log.info("Delete domain ...");
        try {
            // INFO: Integrator/Connector specific cleanup, like for example the Slack Team/Domain table, has to be handled by the Integrator/Connector itself
            // See com.wyona.katie.integrations.slack.services.DomainService#getDomain(String)
            // See com.wyona.katie.integrations.msteams.services.MicrosoftDomainService#getDomain(String)
            // See TODO Discord

            aiService.deleteTenant(getContext(domainId));

            User[] members = getMembers(domainId, false);
            for (User member: members) {
                removeMember(domainId, member.getId(), false);
            }

            deleteDomainDirectory(domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return domainId;
    }

    /**
     * Create a context/domain from scratch
     * @param name Name of Katie domain, e.g. "Wyona"
     * @param mailSubjectTag, e.g. "AskKatie/Wyona"
     * @param answersMustBeApproved True when answers must be approved by a moderator
     * @param creator User creating domain
     */
    public Context createDomain(boolean answersGenerallyProtected, String name, String mailSubjectTag, boolean answersMustBeApproved, User creator) throws Exception {
        log.info("Create context/domain ...");
        String domainId = UUID.randomUUID().toString();
        boolean informUserReModeration = false; // TODO: Use argument
        boolean considerHumanFeedback = false; // TODO: Use argument
        boolean reRankAnswers = false; // TODO: Use argument
        boolean useGenerativeAI = false; // TODO: Use argument
        boolean katieSearchEnabled = true; // TODO: Use argument
        Context newDomain = createDomainConfiguration(domainId, name, answersGenerallyProtected, mailSubjectTag, answersMustBeApproved, informUserReModeration, considerHumanFeedback, reRankAnswers, useGenerativeAI, katieSearchEnabled);

        xmlService.saveMembersConfig(newDomain.getId());

        if (creator != null) {
            try {
                addMember(creator.getId(), true, false, RoleDomain.OWNER, newDomain.getId());
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        return newDomain;
    }

    /**
     * Create ROOT domain from scratch
     */
    private Context createROOTDomain() throws Exception {
        log.info("Create context/domain ...");
        boolean answersGenerallyProtected = true;
        Context newDomain = createDomainConfiguration("ROOT", "Root Domain", answersGenerallyProtected, "AskKatie", false, false, false, false, false, true);

        xmlService.saveMembersConfig(newDomain.getId());

        return newDomain;
    }

    /**
     * @param name Name of Katie domain, e.g. "Wyona"
     * @param answersMustBeApproved True when answers must be approved by a moderator
     */
    private Context createDomainConfiguration(String domainId, String name, boolean answersGenerallyProtected, String mailSubjectTag, boolean answersMustBeApproved, boolean informUserReModeration, boolean considerHumanFeedback, boolean reRankAnswers, boolean useGenerativeAI, boolean katieSearchEnabled) throws Exception {
        File domainDir = createDomainDirectory(domainId);
        Context newContext = new Context(domainId, domainDir, answersGenerallyProtected, mailBodyHost, mailDeepLink, mailSubjectTag, null, answersMustBeApproved, informUserReModeration, considerHumanFeedback, reRankAnswers, useGenerativeAI, katieSearchEnabled, null);
        newContext.setName(name);

        if (defaultDetectDuplicatedQuestionImpl.equals(DetectDuplicatedQuestionImpl.QUERY_SERVICE)) {
            throw new Exception("Query service implementation can not be chosen as default question answering implementation, because no question service URL available!");
        }
        if (defaultDetectDuplicatedQuestionImpl.equals(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH)) {
            newContext.setEmbeddingsApiToken(getApiToken(defaultEmbeddingImpl));
        }

        String aiServiceBaseUrl = aiService.createTenant(newContext, defaultDetectDuplicatedQuestionImpl);
        log.info("AI Service base URL or Id: " + aiServiceBaseUrl);

        newContext = setQuestionAnswerImplementation(newContext, defaultDetectDuplicatedQuestionImpl, aiServiceBaseUrl, defaultEmbeddingImpl, newContext.getEmbeddingsApiToken());
        saveDomainConfig(newContext);

        return newContext;
    }

    /**
     * Get API token of embedding implementation
     */
    public String getApiToken(EmbeddingsImpl embeddingsImpl) {
        if (embeddingsImpl.equals(EmbeddingsImpl.ALEPH_ALPHA)) {
            return alephAlphaToken;
        } else if (embeddingsImpl.equals(EmbeddingsImpl.COHERE)) {
            return cohereKey;
        } else if (embeddingsImpl.equals(EmbeddingsImpl.OPENAI)) {
            return openAIKey;
        } else if (embeddingsImpl.equals(EmbeddingsImpl.OPENAI_AZURE)) {
            return openAIAzureKey;
        } else if (embeddingsImpl.equals(EmbeddingsImpl.GOOGLE)) {
            return googleKey;
        } else {
            // INFO: SBERT
            return null;
        }
    }

    /**
     * Get API token of completion implementation
     */
    protected String getApiToken(CompletionImpl generateImpl) {
        if (generateImpl.equals(CompletionImpl.ALEPH_ALPHA)) {
            return alephAlphaToken;
        } else if (generateImpl.equals(CompletionImpl.OPENAI)) {
            return openAIKey;
            //} else if (generateImpl.equals(EmbeddingsImpl.OPENAI_AZURE)) {
            //    return openAIAzureKey;
        } else if (generateImpl.equals(CompletionImpl.MISTRAL_AI)) {
            return mistralAIKey;
        } else {
            return null;
        }
    }

    /**
     * @param aiServiceBaseUrl DeepKatie base URL or index or corpus Id, e.g. "https://deeppavlov.wyona.com" or "askkatie_5bd57b92-da98-422f-8ad6-6670b9c69184"
     * @param apiToken API Token of Embeddings Implementation
     */
    private Context setQuestionAnswerImplementation(Context domain, DetectDuplicatedQuestionImpl questionAnswerImplementation, String aiServiceBaseUrl, EmbeddingsImpl embeddingImpl, String apiToken) {
        if (questionAnswerImplementation.equals(DetectDuplicatedQuestionImpl.LUCENE_DEFAULT)) {
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.LUCENE_DEFAULT);
        } else if (questionAnswerImplementation.equals(DetectDuplicatedQuestionImpl.KNOWLEDGE_GRAPH)) {
            domain.setKnowledgeGraphQueryUrl(aiServiceBaseUrl);
        } else if (questionAnswerImplementation.equals(DetectDuplicatedQuestionImpl.WEAVIATE)) {
            domain.setWeaviateQueryUrl(aiServiceBaseUrl);
            domain.setWeaviateCertaintyThreshold(certaintyThreshold);
        } else if (questionAnswerImplementation.equals(DetectDuplicatedQuestionImpl.QUERY_SERVICE)) {
            // INFO: Might be already set before
            domain.setQueryServiceUrl(aiServiceBaseUrl);
        } else if (questionAnswerImplementation.equals(DetectDuplicatedQuestionImpl.KATIE)) {
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.KATIE);
        } else if (questionAnswerImplementation.equals(DetectDuplicatedQuestionImpl.AZURE_AI_SEARCH)) {
            domain.setAzureAISearchIndexName(aiServiceBaseUrl);
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.AZURE_AI_SEARCH);
        } else if (questionAnswerImplementation.equals(DetectDuplicatedQuestionImpl.ELASTICSEARCH)) {
            domain.setElasticsearchIndex(aiServiceBaseUrl);
        } else if (questionAnswerImplementation.equals(DetectDuplicatedQuestionImpl.SENTENCE_BERT)) {
            domain.setSentenceBERTCorpusId(aiServiceBaseUrl);
            domain.setSentenceBERTDistanceThreshold(distanceThreshold);
        } else if (questionAnswerImplementation.equals(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH)) {
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH);
            domain.setEmbeddingsImpl(embeddingImpl);
            domain.setEmbeddingsApiToken(apiToken);

            String vectorSimilarityMetric = defaultVectorSearchSimilarityMetric;

            if (embeddingImpl != null) {
                if (embeddingImpl.equals(EmbeddingsImpl.SBERT)) {
                    domain.setEmbeddingsModel(null);
                } else {
                    domain.setEmbeddingsModel(aiService.getEmbeddingModel(embeddingImpl));
                }
                if (embeddingImpl.equals(EmbeddingsImpl.COHERE)) {
                    vectorSimilarityMetric = cohereVectorSearchSimilarityMetric;
                }
                /*
                if (embeddingImpl.equals(EmbeddingsImpl.OPENAI)) {
                    domain.setEmbeddingsModel(openAIModel);
                } else if (embeddingImpl.equals(EmbeddingsImpl.COHERE)) {
                    domain.setEmbeddingsModel(cohereModel);
                } else if (embeddingImpl.equals(EmbeddingsImpl.ALEPH_ALPHA)) {
                    domain.setEmbeddingsModel(alephAlphaModel);
                } else if (embeddingImpl.equals(EmbeddingsImpl.NUMENTA)) {
                    domain.setEmbeddingsModel(numentaModel);
                } else if (embeddingImpl.equals(EmbeddingsImpl.GOOGLE)) {
                    domain.setEmbeddingsModel(googleModel);
                } else {
                    // INFO: SentenceBERT
                    domain.setEmbeddingsModel(null);
                }
                 */
            } else {
                domain.setEmbeddingsModel(null);
            }

            domain.setVectorSimilarityMetric(VectorSimilarityFunction.valueOf(vectorSimilarityMetric));
        } else {
            log.error("No such question/answer implementation: " + questionAnswerImplementation);
        }
        return domain;
    }

    /**
     * @param id Domain id
     * @return base directory of domain which contains domain configuration
     */
    private File createDomainDirectory(String id) {
        File domainDir = getDomainDirectory(id);
        domainDir.mkdirs();
        return domainDir;
    }

    /**
     * @param id Domain id
     */
    private void deleteDomainDirectory(String id) throws Exception {
        FileUtils.deleteDirectory(getDomainDirectory(id));
    }

    /**
     * @param id Domain id
     */
    private File getDomainDirectory(String id) {
        return new File(contextsDataPath, id);
    }

    /**
     * Update mail body hostname
     * @param domainId Domain Id
     * @param hostname Updated hostname
     */
    public void updateMailBodyHostname(String domainId, Hostname hostname) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        log.info("Update message body hostname of domain '" + domainId + "' by '" + hostname.getHostname() + "'");
        Context domain = getContext(domainId);
        domain.setHost(hostname.getHostname());
        saveDomainConfig(domain);
    }

    /**
     * Update domain name
     * @param domainId Domain Id
     * @param name New domain name, e.g. "Apache Lucene"
     */
    public void updateDomainName(String domainId, String name) throws Exception {
        if (!existsContext(domainId)) {
            throw new Exception("Domain '" + domainId + "' does not exist!");
        }
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }
        log.info("Update domain name ...");
        // TODO: Validate name
        if (!name.trim().isEmpty()) {
            int maxLength = 50;
            if (name.trim().length() >= maxLength) {
                throw new Exception("Length of name must be less than " + maxLength + " characters!");
            }
            Context domain = getContext(domainId);
            domain.setName(name);
            saveDomainConfig(domain);
        } else {
            throw new Exception("No name provided!");
        }
    }

    /**
     * Update domain tag name
     * @param domainId Domain Id
     * @param tagName New tag name, e.g. "apache-lucene"
     */
    public void updateTagName(String domainId, String tagName) throws Exception {
        if (tagName.equals("root")) {
            throw new Exception("Tag name not available!");
        }
        if (domainId.equals(Context.ROOT_NAME)) {
            throw new Exception("Tag name of ROOT domain cannot be changed!");
        }

        if (tagName.length() > 30) {
            log.info("Tag name is too long, therefore shorten it ...");
            tagName = tagName.substring(0, 30);
        }

        tagName = tagName.replace(" ", "-");

        // TODO: Implement additional validation, e.g. valid characters, etc.

        if (!existsContext(domainId)) {
            throw new Exception("Domain '" + domainId + "' does not exist!");
        }
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        dataRepositoryService.updateTagName(domainId, tagName);
    }

    /**
     * Update mail subject
     * @param domainId Domain Id
     * @param value New mail subject
     */
    public void updateMailSubject(String domainId, String value) throws Exception {
        if (!existsContext(domainId)) {
            throw new Exception("Domain '" + domainId + "' does not exist!");
        }
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }
        log.info("Update mail subject ...");
        // TODO: Validate value
        if (!value.trim().isEmpty()) {
            int maxLength = 50;
            if (value.trim().length() >= maxLength) {
                throw new Exception("Length of name must be less than " + maxLength + " characters!");
            }
            Context domain = getContext(domainId);
            domain.setMailSubjectTag(value);
            saveDomainConfig(domain);
        } else {
            throw new Exception("No mail subject value provided!");
        }
    }

    /**
     * Update mail sender
     * @param domainId Domain Id
     * @param value New mail sender email address
     */
    public void updateMailSender(String domainId, String value) throws Exception {
        if (!existsContext(domainId)) {
            throw new Exception("Domain '" + domainId + "' does not exist!");
        }
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }
        log.info("Update mail sender ...");
        // TODO: Validate value
        if (!value.trim().isEmpty()) {
            int maxLength = 50;
            if (value.trim().length() >= maxLength) {
                throw new Exception("Length of name must be less than " + maxLength + " characters!");
            }
            Context domain = getContext(domainId);
            domain.setMailSenderEmail(value);
            saveDomainConfig(domain);
        } else {
            throw new Exception("No mail sender value provided!");
        }
    }

    /**
     * Update match reply-to emails
     */
    public void updateMatchReplyToEmails(String domainId, MatchReplyToEmails emails) throws Exception {
        if (!existsContext(domainId)) {
            throw new Exception("Domain '" + domainId + "' does not exist!");
        }
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }
        log.info("Update Match reply-to email addresses ...");
        // TODO: Validate email addresses
        if (emails.getEmails().length > 0) {
            if (emails.getEmails().length > 1) {
                log.warn("TODO: Save more than one match reply-to email!");
            }
            Context domain = getContext(domainId);
            domain.setMatchReplyTo(emails.getEmails()[0]);
            saveDomainConfig(domain);
        } else {
            throw new Exception("No emails provided!");
        }
    }

    /**
     * Update IMAP configuration
     */
    public void updateIMAPConfiguration(String domainId, IMAPConfiguration imapConfiguration) throws Exception {
        if (!existsContext(domainId)) {
            throw new Exception("Domain '" + domainId + "' does not exist!");
        }
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }
        log.info("Update IMAP configuration: " + imapConfiguration.getHostname() + ":" + imapConfiguration.getPort());
        // TODO: Validate IMAP configuration
        Context domain = getContext(domainId);
        domain.setImapConfiguration(imapConfiguration);

        // INFO: To be on the safe side we overwrite the configuration
        domain.setAnswersMustBeApprovedByModerator(true);
        domain.setInformUserReModeration(false);

        saveDomainConfig(domain);
    }

    /**
     * Save domain configuration
     */
    private void saveDomainConfig(Context domain) {
        xmlService.saveContextConfig(domain);
    }

    /**
     * Toggle whether a particular user is moderator of a specific domain
     */
    public User toggleUserIsModerator(String domainId, User user) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }
        User member = xmlService.getMember(user, domainId);
        if (member.getIsModerator()) {
            log.info("Unset user '" + user.getUsername() + "' as moderator.");
            member.setIsModerator(false);
        } else {
            log.info("Set user '" + user.getUsername() + "' as moderator.");
            member.setIsModerator(true);
        }

       xmlService.updateDomainMember(member, domainId);

        return member;
    }

    /**
     * Toggle whether a particular user is expert of a specific domain
     */
    public User toggleUserIsExpert(String domainId, User user) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }
        User member = xmlService.getMember(user, domainId);
        if (member.getIsExpert()) {
            log.info("Unset user '" + user.getUsername() + "' as exprt.");
            member.setIsExpert(false);
        } else {
            log.info("Set user '" + user.getUsername() + "' as expert.");
            member.setIsExpert(true);
        }

        xmlService.updateDomainMember(member, domainId);

        return member;
    }

    /**
     * Toggle whether answers of domain should be generally protected or public
     * @param domainId Domain Id
     */
    public Context toggleAnswersGenerallyProtected(String domainId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        log.info("Toggle whether answers of domain '" + domainId + "' should be generally protected or public ...");
        Context domain = getContext(domainId);
        
        if (domain.getAnswersGenerallyProtected()) {
            domain.setAnswersGenerallyProtected(false);
        } else {
            domain.setAnswersGenerallyProtected(true);
        }

        saveDomainConfig(domain);
        return domain;
    }

    /**
     * Toggle whether human feedback should be considered when answering questions
     * @param domainId Domain Id
     */
    public Context toggleConsiderHumanFeedback(String domainId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        log.info("Toggle whether human feedback associated with domain / knowledge base '" + domainId + "' should be considered when answering questions ...");
        Context domain = getContext(domainId);

        if (domain.getConsiderHumanFeedback()) {
            domain.setConsiderHumanFeedback(false);
        } else {
            domain.setConsiderHumanFeedback(true);
        }

        saveDomainConfig(domain);
        return domain;
    }

    /**
     * Toggle whether answers should be generated / completed
     * @param domainId Domain Id
     */
    public Context toggleGenerateAnswers(String domainId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        log.info("Toggle whether answers associated with domain / knowledge base '" + domainId + "' should be generated / completed ...");
        Context domain = getContext(domainId);

        if (domain.getGenerateCompleteAnswers()) {
            domain.setGenerateCompleteAnswers(false);
        } else {
            domain.setGenerateCompleteAnswers(true);
        }

        saveDomainConfig(domain);
        return domain;
    }

    /**
     * Toggle whether answers should be re-ranked
     * @param domainId Domain Id
     */
    public Context toggleReRankAnswers(String domainId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        log.info("Toggle whether answers associated with domain / knowledge base '" + domainId + "' should be re-ranked ...");
        Context domain = getContext(domainId);

        if (domain.getReRankAnswers()) {
            domain.setReRankAnswers(false);
        } else {
            domain.setReRankAnswers(true);
        }

        saveDomainConfig(domain);
        return domain;
    }

    /**
     *
     */
    public Context enableReRankAnswers(String domainId) throws Exception {
        Context domain = getContext(domainId);
        domain.setReRankAnswers(true);
        saveDomainConfig(domain);
        return domain;
    }

    /**
     * Toggle whether answers must be moderated
     * @param domainId Domain Id
     */
    public Context toggleModeration(String domainId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        log.info("Toggle whether answers of domain '" + domainId + "' must be moderated ...");
        Context domain = getContext(domainId);
        if (domain.getAnswersMustBeApprovedByModerator()) {
            domain.setAnswersMustBeApprovedByModerator(false);
        } else {
            domain.setAnswersMustBeApprovedByModerator(true);
        }
        saveDomainConfig(domain);
        return domain;
    }

    /**
     * Toggle whether user should be informed re moderation
     * @param domainId Domain Id
     */
    public Context toggleInformUserReModeration(String domainId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        log.info("Toggle whether user should be informed when moderation is enabled for domain '" + domainId + "' ...");
        Context domain = getContext(domainId);
        if (domain.getInformUserReModeration()) {
            domain.setInformUserReModeration(false);
        } else {
            domain.setInformUserReModeration(true);
        }
        saveDomainConfig(domain);
        return domain;
    }

    /**
     * Check whether user is signed in and if so whether user is member of domain or administrator
     * @return true when user is signed in and is member of domain or administrator, and false otherwise
     */
    public boolean isMemberOrAdmin(String domainId) {
        User signedInUser = authService.getUser(false, false);
        if (signedInUser != null) {
            return isMemberOrAdmin(domainId, signedInUser);
        } else {
            log.warn("Cannot check whether user is member of domain '" + domainId + "' or admin, because user is not signed in!");
            return false;
        }
    }

    /**
     * Check whether user is member of domain or administrator
     * @return true when user is member of domain or administrator and false otherwise
     */
    public boolean isMemberOrAdmin(String domainId, User user) {
        log.info("Check whether user has role " + Role.ADMIN + " or is member of domain '" + domainId + "' ...");

        if (user != null) {
            log.debug("Provided user Id: " + user.getUsername());

            if (user.getRole() == Role.ADMIN || isUserMemberOfDomain(user.getId(), domainId)) {
                return true;
            } else {
                log.warn("User '" + user.getId() + "' is neither member of domain '" + domainId + "', nor has role '" + Role.ADMIN + "'!");
                return false;
            }
        } else {
            log.warn("No user provided!");
            return false;
        }
    }

    /**
     * Check whether user is signed in and if so whether user is administrator
     * @return true when user is administrator and false otherwise
     */
    public boolean isAdmin() {
        log.info("Check whether user is signed in and has role " + Role.ADMIN + " ...");

        User signedInUser = authService.getUser(false, false);
        if (signedInUser != null) {
            log.debug("Signed in user: " + signedInUser.getUsername());

            if (signedInUser.getRole() == Role.ADMIN) {
                return true;
            } else {
                log.warn("User '" + signedInUser.getId() + "' has not role '" + Role.ADMIN + "'!");
                return false;
            }
        } else {
            log.warn("User is not signed in!");
            return false;
        }
    }

    /**
     * Add QnA to FAQ
     * @param domain Domain associated with FAQ
     * @param language Two-letter Language code of FAQ, e.g. "de" or "en"
     * @param uuid UUID of QnA
     * @param checkAuthorization Check whether user is authorized to add QnA to FAQ
     */
    public FAQ addQnA2FAQ(Context domain, String language, String topicId, String uuid, boolean checkAuthorization) throws Exception {
        if (checkAuthorization && !isMemberOrAdmin(domain.getId())) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        FAQ faq = getFAQ(domain, language, true, false);
        faq.addQnA(topicId, uuid);
        xmlService.addQnAToFAQ(domain, language, topicId, uuid);

        Answer qna = getQnA(null, uuid, domain);
        qna.setFaqLanguage(Language.valueOf(language));
        qna.setFaqTopicId(topicId);
        xmlService.saveQuestionAnswer(qna, domain.getQnAXmlFilePath(uuid));

        log.info("Qna '" + uuid + "' has been added to FAQ persistently.");

        return faq;
    }

    /**
     *
     */
    @Async
    public void importQnAs(QnA[] qnas, Context domain, String processId, String userId) throws Exception {
        log.info("Import " + qnas.length + " QnAs ...");
        backgroundProcessService.startProcess(processId, "Import " + qnas.length + " QnAs into domain '" + domain.getId() + "'.", userId);

        int counter = 0;
        final int BATCH_SIZE = 20;
        for (QnA qna : qnas) {
            if (qna.getQuestion() != null) {
                log.info("Import QnA: " + qna.getQuestion());
            } else {
                log.info("Import QnA, whereas no question provided, probably only chunk with a URL as reference.");
            }
            Date dateQuestion = new Date(); // TODO: When no question is set, then date question will also not be set!
            Date dateAnswered = dateQuestion;
            Date dateAnswerModified = dateAnswered;
            Answer answer = new Answer(null, qna.getAnswer(), null, qna.getUrl(), qna.getClassifications(), null, qna.getAnswerClientSideEncryptionAlgorithm(), dateAnswered, dateAnswerModified, null, domain.getId(), null, qna.getQuestion(), dateQuestion, false, null, false, null);
            String[] alternativeQuestions = qna.getAlternativeQuestions();
            if (alternativeQuestions != null && alternativeQuestions.length > 0) {
                for (String aq : alternativeQuestions) {
                    answer.addAlternativeQuestion(aq);
                }
            }
            answer = addQuestionAnswer(answer, domain);
            qna.setUuid(answer.getUuid());
            train(qna, domain, true);

            counter++;
            if (counter % BATCH_SIZE == 0) {
                backgroundProcessService.updateProcessStatus(processId, counter + " QnAs imported, " + (qnas.length - counter) + " QnAs remaining ...");
            }
        }

        backgroundProcessService.stopProcess(processId);
    }

    /**
     * Import FAQ
     * @param domain Domain FAQs are being associated with
     * @param language Two-letter language code of FAQ, e.g. "de" or "en"
     * @param importFAQ New / additional FAQs
     * @param isPublic True when QnAs are public and false when QnAs are not public
     * @param user User trying to import FAQ
     */
    public FAQ importFAQ(Context domain, String language, FAQ importFAQ, boolean isPublic, User user, boolean indexAlternativeQuestions) throws Exception {
        if (!isMemberOrAdmin(domain.getId(), user)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        if (!Utils.isLanguageValid(language)) {
            throw new Exception("Language '" + language + "' is not valid!");
        }

        // INFO: Check whether FAQ for this domain and language exists
        Topic[] importTopics = importFAQ.getTopics();
        if (existsFAQ(domain, language)) {
            FAQ existingFAQ = getFAQ(domain, language, true, false);
            log.info("There are " + existingFAQ.getTopics().length + " existing FAQ topics for domain '" + domain.getId() + "' and language '" + language + "'.");

            // INFO: Append new topics
            Topic[] existingTopics = existingFAQ.getTopics();
            for (int i = 0; i < importTopics.length; i++) {
                Topic importTopic = importTopics[i];

                if (importTopic.getId() != null) {
                    Topic existingTopic = getTopic(importTopic.getId(), existingTopics);
                    if (existingTopic == null) {
                        log.warn("FAQ of doamin '" + domain.getId() + "'  and language '" + language + "' does not contain topic with id '" + importTopic.getId() + "', therefore add it ...");
                        existingFAQ.addTopic(new Topic(importTopic.getId(), importTopic.getTitle(), null, selectVisibility(importTopic.getVisibility())));
                        xmlService.createFAQ(domain, language, existingFAQ);
                    } else {
                        log.info("Topic '" + existingTopic.getTitle() + "' (" + existingTopic.getId() + ") already exists.");
                    }
                } else {
                    String newTopicId = UUID.randomUUID().toString();
                    existingFAQ.addTopic(new Topic(newTopicId, importTopic.getTitle(), null, selectVisibility(importTopic.getVisibility())));
                    xmlService.createFAQ(domain, language, existingFAQ);
                    importTopic.setId(newTopicId);
                }
            }
        } else {
            log.info("There are no existing FAQ topics for domain '" + domain.getId() + "' and language '" + language + "', therefore create one ...");
            importTopics = xmlService.createFAQ(domain, language, importTopics);
        }

        log.info("Save new QnAs ...");
        ArrayList<QnA> qnas = new ArrayList<QnA>();
        for (int i = 0; i < importTopics.length; i++) {
            Topic topic = importTopics[i];

            com.wyona.katie.models.faq.Question[] questions = topic.getQuestions();
            for (int k = 0; k < questions.length; k++) {
                com.wyona.katie.models.faq.Question question = questions[k];

                Date answered = new Date(); // TODO: Get date from FAQ import
                Date dateAnswerModified = answered; // TODO: Get date from FAQ import
                Date dateOriginalQuestionSubmitted = answered; // TODO: Get date from FAQ import

                List<String> classifications  = new ArrayList<String>();
                classifications.add(topic.getTitle()); // TODO: Consider to replace topic title as classification

                String url = null; // TODO: Get URL in case one is provided
                ContentType contentType = null;

                Answer newQnA = new Answer(null, question.getAnswer(), contentType, url, classifications, QnAType.DEFAULT, null, answered, dateAnswerModified, null, domain.getId(), null, question.getQuestion(), dateOriginalQuestionSubmitted, isPublic, new Permissions(isPublic), false, user.getId());

                newQnA = addQuestionAnswer(newQnA, domain);

                addQnA2FAQ(domain, language, topic.getId(), newQnA.getUuid(), false);
                qnas.add(new QnA(newQnA));
            }
        }

        log.info("Train Katie with " + qnas.size() + " new QnAs at once ...");
        String processId = null; // TODO
        int batchSize = 100; // TOOD: Make configurable
        int throttleTimeInMillis = -1; // TODO: Add as argument
        train(qnas.toArray(new QnA[0]), domain, indexAlternativeQuestions, batchSize, processId, throttleTimeInMillis);

        return getFAQ(domain, language, true, false);
    }

    /**
     *
     */
    private TopicVisibility selectVisibility(TopicVisibility visibility) {
        if (visibility != null) {
            if (!(visibility.equals(TopicVisibility.PRIVATE) || visibility.equals(TopicVisibility.PUBLIC))) {
                log.warn("No such visibility value: " + visibility);
                return TopicVisibility.PRIVATE;
            } else {
                return visibility;
            }
        } else {
            return TopicVisibility.PRIVATE;
        }
    }

    /**
     * Get a particular topic
     * @param uuid Topic UUID
     * @return topic when UUID matches and null otherwise
     */
    private Topic getTopic(String uuid, Topic[] topics) {
        if (topics != null) {
            for (int i = 0; i < topics.length; i++) {
                Topic topic = topics[i];
                if (topic.getId().equals(uuid)) {
                    return topic;
                }
            }
        }

        return null;
    }

    /**
     * Get FAQ
     * @param domain Domain associated with FAQ
     * @param language Language of FAQ
     * @param uuidOnly When set to true, then for performance/scalability reasons only get UUIDs of questions
     * @param publicOnly When set to true, then only topics where the visibility is set to public
     */
    public FAQ getFAQ(Context domain, String language, boolean uuidOnly, boolean publicOnly) throws Exception {
        analyticsService.logFAQRequest(domain.getId(), language);

        FAQ faq = xmlService.getFAQ(domain, language, publicOnly);

        if (uuidOnly) {
            log.info("Do not resolve QnAs which are referenced by a UUID.");
        } else {
            String username = authService.getUsername();

            for (Topic topic: faq.getTopics()) {
                for (com.wyona.katie.models.faq.Question question: topic.getQuestions()) {
                    String qnaUUID = question.getUuid();
                    if (qnaUUID != null && qnaUUID.length() > 0) {
                        log.info("Resolve QnA '" + qnaUUID + "'.");

                        String userId = authService.getUserId();
                        Answer qna = xmlService.parseQuestionAnswer(null, domain.getAnswersGenerallyProtected(), domain, qnaUUID, userId);
                        if (qna != null) {
                            if (qna.isPublic()) {
                                question.setQuestion(qna.getOriginalquestion());
                                question.setAnswer(qna.getAnswer());
                            } else {
                                PermissionStatus permissionsStatus = iamService.getPermissionStatus(qna, username);
                                log.warn("QnA '" + qnaUUID + "' is not public, therefore check permission status '" + permissionsStatus + "' ...");
                                if (permissionsStatus == PermissionStatus.MEMBER_AUTHORIZED_TO_READ_ANSWER || permissionsStatus == PermissionStatus.USER_AUTHORIZED_TO_READ_ANSWER) {
                                    question.setQuestion(qna.getOriginalquestion());
                                    question.setAnswer(qna.getAnswer());
                                } else {
                                    log.info("User is not authorized to access QnA (Permission status: " + permissionsStatus + ").");
                                    topic.removeQuestion(qnaUUID);
                                }
                            }
                        } else {
                            log.error("No such QnA '" + qnaUUID + "'!");
                            topic.addQuestion(qnaUUID, "ERROR: No such QnA '" + qnaUUID + "'!", null);
                        }
                    }
                }
            }
        }

        return faq;
    }

    /**
     * Get FAQ
     * @param in Input stream containing DOM of FAQ
     */
    public FAQ getFAQ(InputStream in) throws Exception {
        return xmlService.getFAQ(in);
    }

    /**
     * Check whether FAQ exists for a particular domain and language
     * @param language Two-letter language code, e.g. "de" or "en" or "fr"
     * @return true when FAQ exists for a particular domain and language and false otherwise
     */
    public boolean existsFAQ(Context domain, String language) {
        File file = domain.getFAQXmlDataPath(language);
        log.info("FAQ dataset path: " + file.getAbsolutePath());

        if (file.exists()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Update whether AI was trained with particular QnA
     * @param domain Domain of QnA
     * @param uuid UUID of QnA
     * @param isTrained True when AI was trained and false otherwise
     */
    public void updateTrained(Context domain, String uuid, boolean isTrained) throws Exception {
        log.info("Update isTrained flag of QnA '" + uuid + "' of domain '" + domain.getId() + "' ...");
        Answer answer = getQnA(null, uuid, domain);
        if (!answer.getIsReference()) {
            answer.setTrained(isTrained);
            saveQuestionAnswer(domain, uuid, answer);
        } else {
            log.warn("TODO: QnA '" + domain.getId() + " / " + uuid + "' is a reference and must be handled differently!");
        }
    }

    /**
     * Rate answer to question using thumb up and down
     * @param thumbUp When set to true, then "thumb up" and when set to false, then "thumb down"
     */
    public void thumbUpDown(AskedQuestion askedQuestion, boolean thumbUp, Context domain) throws Exception {
        if (askedQuestion.getAnswerUUID() != null) {
            Rating rating = new Rating();
            rating.setQuestionuuid(askedQuestion.getUUID());
            rating.setQnauuid(askedQuestion.getAnswerUUID());
            rating.setUserquestion(askedQuestion.getQuestion());
            rating.setEmail(askedQuestion.getUsername()); // TODO
            rating.setDate(new Date());

            if (thumbUp) {
                rating.setRating(10);
            } else {
                rating.setRating(0);
            }

            log.info("Rate answer of QnA '" + rating.getQnauuid() + "' to question '" + rating.getUserquestion() + "': " + rating.getRating());
            rateAnswer(rating.getQnauuid(), domain, rating);
        } else {
            // TODO: Could also be a connector, e.g. third-party RAG connector
            log.warn("Answer does not have a UUID, because it was probably retrieved from a third-party retriever: " + domain.getQueryServiceUrl());
        }
    }

    /**
     * Rate answer to question (either sent by user or part of QnA)
     * @param qnaUuid UUID of QnA which was used as answer to the question of user
     * @param domain Domain associated with QnA
     * @param rating Rating of user, also containing question of user when available
     * @return rated answer
     */
    public Answer rateAnswer(String qnaUuid, Context domain, Rating rating) throws Exception {
        Answer qna = getQnA(null, qnaUuid, domain);

        if (qna != null) {
            rating.setQnauuid(qna.getUuid());

            PermissionStatus permissionStatus = null;

            User signedInUser = authService.getUser(false, false);
            if (signedInUser != null) {
                log.debug("Signed in user: " + signedInUser.getUsername());
                permissionStatus = iamService.getPermissionStatus(qna, signedInUser.getUsername());
                rating.setEmail(signedInUser.getEmail());
            } else {
                log.warn("User is not signed in!");
                permissionStatus = iamService.getPermissionStatus(qna, null);
            }

            /* TODO: Authoriization check disabled temporarily
            if (!iamService.isAuthorized(permissionStatus)) {
                String msg = "User is not authorized to rate answer '" + domain.getId() + " / " + qna.getUuid() + "', because permission status is '" + permissionStatus + "'!";
                log.warn(msg);
                throw new AccessDeniedException(msg);
            }
             */

            qna.addRating(rating);
            saveRating(domain, qna);
            saveRating(domain, rating, Utils.convertHtmlToPlainText(qna.getAnswer()));
            dataRepositoryService.updateStatusOfResubmittedQuestion(qnaUuid, StatusResubmittedQuestion.STATUS_ANSWER_RATED);

            analyticsService.logFeedback(domain.getId(), rating.getRating(), rating.getEmail());

            String askedQuestion = qna.getOriginalquestion();
            if (rating.getUserquestion() != null) {
                askedQuestion = rating.getUserquestion();
                if (domain.getConsiderHumanFeedback()) {
                    aiService.indexHumanFeedback(askedQuestion, qnaUuid, domain, rating.getRating(), signedInUser);
                }
            }

            User[] experts = getExperts(domain.getId(), false);
            for (User expert: experts) {
                sendNotificationReRating(domain, qnaUuid, expert.getId(), askedQuestion);
            }
            if (qna.getRespondentId() != null) {
                if (!isExpert(qna.getRespondentId(), experts)) {
                    sendNotificationReRating(domain, qnaUuid, qna.getRespondentId(), askedQuestion);
                } else {
                    log.info("Author '" + qna.getRespondentId() + "' already notified as expert.");
                }
            } else {
                log.info("No author available for domain / QnA '" + domain.getId() + " / " + qnaUuid + "'.");
            }

            return qna;
        } else {
            log.error("No such such QnA: " + domain.getId() + " / " + qnaUuid);
            return null;
        }
    }

    /**
     * Send notification that a user provided feedback re an answer
     * @param userId Id of user to be notified
     */
    public void sendNotificationReRating(Context domain, String uuid, String userId, String askedQuestion) {
        try {
            User user = iamService.getUserByIdWithoutAuthCheck(userId);
            if (user != null) {
                log.info("Notify user '" + user.getEmail() + "' (" + user.getLanguage() + "), that user has provided feedback re answer '" + uuid + "' ...");
                String email = user.getEmail();
                String body = getFeedbackNotificationBody(domain, uuid, askedQuestion, user.getLanguage());
                String subject = getSubjectPrefix(domain) + " " + messageSource.getMessage("provide.feedback.on.answer", null, new Locale(user.getLanguage()));
                mailerService.send(email, domain.getMailSenderEmail(), subject, body, true);
            } else {
                log.warn("No such user '" + userId + "'.");
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Generate email text re user feedback
     * @param domain Domain containing QnA
     * @param uuid UUID of QnA which was used as answer and user provided feedback to
     * @return email body, which will be sent to experts of domain
     */
    private String getFeedbackNotificationBody(Context domain, String uuid, String askedQuestion, String userLanguage) throws Exception {
        //Answer qna = getQnA(null, uuid, domain);

        String answerLink = domain.getHost() + "/#/domain/" + domain.getId() + "/qna/" + uuid;
        String insightsLink = domain.getHost() + "/#/domain/" + domain.getId() + "/insights";

        TemplateArguments tmplArgs = new TemplateArguments(domain, null);
        tmplArgs.add("question_answer_link", answerLink);
        tmplArgs.add("insights_link", insightsLink);
        tmplArgs.add("userquestion", askedQuestion);

        StringWriter writer = new StringWriter();
        Template emailTemplate = mailerService.getTemplate("feedback_re_answer_", Language.valueOf(userLanguage), domain);
        emailTemplate.process(tmplArgs.getArgs(), writer);
        return writer.toString();
    }

    /**
     * Generate email text to approve better answer
     * @param domain
     * @param uuid
     * @param askedQuestion
     * @param userLanguage
     * @return
     * @throws Exception
     */
    private String getApproveBetterAnswerNotificationBody(Context domain, String uuid, String askedQuestion, String userLanguage) throws Exception {
        String body = "Please review better answer: " + domain.getHost() + "/#/domain/" + domain.getId() + "/qna/" + uuid; // TODO

        String qnaLink = domain.getHost() + "/#/domain/" + domain.getId() + "/qna/" + uuid;

        TemplateArguments tmplArgs = new TemplateArguments(domain, null);
        tmplArgs.add("qna_link", qnaLink);
        tmplArgs.add("userquestion", askedQuestion);

        StringWriter writer = new StringWriter();
        Template emailTemplate = mailerService.getTemplate("approve_better_answer_", Language.valueOf(userLanguage), domain);
        emailTemplate.process(tmplArgs.getArgs(), writer);
        return writer.toString();
    }

    /**
     *
     */
    private String getSubjectPrefix(Context domain) {
        return "[" + domain.getMailSubjectTag() + "]";
    }

    /**
     * @return true when user is expert and false otherwise
     */
    private boolean isExpert(String userId, User[] experts) {
        for (User expert : experts) {
            if (expert.getId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update question of trained QnA
     * @param question Updated question
     * @return updated QnA
     */
    public Answer updateQuestionOfTrainedQnA(Context domain, String uuid, String question) throws Exception {
        if (!isMemberOrAdmin(domain.getId())) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        log.info("Update question of trained QnA '" + domain.getId() + "/" + uuid + "' ...");

        Answer qna = getQnA(null, uuid, domain);

        String username = authService.getUsername();
        PermissionStatus permissionStatus = iamService.getPermissionStatus(qna, username);
        log.info("Permission status: " + permissionStatus);
        if (!iamService.isAuthorized(permissionStatus)) {
            throw new java.nio.file.AccessDeniedException("User '" + username + "' is not authorized to add alternative question!");
        }

        qna.setOriginalQuestion(question);

        saveQuestionAnswer(domain, uuid, qna);
        retrain(new QnA(qna), domain, true);

        return qna;
    }

    /**
     * Update source URL of trained QnA
     * @param url Source URL
     * @return updated QnA
     */
    public Answer updateSourceUrlOfTrainedQnA(Context domain, String uuid, String url) throws Exception {
        if (!isMemberOrAdmin(domain.getId())) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        log.info("Update source URL of trained QnA '" + domain.getId() + "/" + uuid + "' ...");

        Answer qna = getQnA(null, uuid, domain);

        String username = authService.getUsername();
        PermissionStatus permissionStatus = iamService.getPermissionStatus(qna, username);
        log.info("Permission status: " + permissionStatus);
        if (!iamService.isAuthorized(permissionStatus)) {
            throw new java.nio.file.AccessDeniedException("User '" + username + "' is not authorized to update source URL!");
        }

        qna.setUrl(url);

        saveQuestionAnswer(domain, uuid, qna);
        retrain(new QnA(qna), domain, true);

        return qna;
    }

    /**
     * Add alternative question to trained QnA
     * @param question Alternative question
     */
    public Answer addAlternativeQuestion(Context domain, String uuid, String question) throws Exception {
        if (!isMemberOrAdmin(domain.getId())) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        Answer qna = getQnA(null, uuid, domain);

        String username = authService.getUsername();
        PermissionStatus permissionStatus = iamService.getPermissionStatus(qna, username);
        log.info("Permission status: " + permissionStatus);
        if (!iamService.isAuthorized(permissionStatus)) {
            throw new java.nio.file.AccessDeniedException("User '" + username + "' is not authorized to add alternative question!");
        }

        log.info("Add alternative question to trained QnA '" + domain.getId() + "/" + uuid + "' ...");

        qna.addAlternativeQuestion(question);

        saveQuestionAnswer(domain, uuid, qna);
        retrain(new QnA(qna), domain, true);

        return qna;
    }

    /**
     * Delete alternative question to trained QnA
     * @param index Index of alternative question, e.g. "2"
     */
    public Answer deleteAlternativeQuestion(Context domain, String uuid, int index) throws Exception {
        if (!isMemberOrAdmin(domain.getId())) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        Answer qna = getQnA(null, uuid, domain);

        String username = authService.getUsername();
        PermissionStatus permissionStatus = iamService.getPermissionStatus(qna, username);
        log.info("Permission status: " + permissionStatus);
        if (!iamService.isAuthorized(permissionStatus)) {
            throw new java.nio.file.AccessDeniedException("User '" + username + "' is not authorized to delete alternative question!");
        }

        log.info("Delete alternative question '" + index + "' of trained QnA '" + domain.getId() + "/" + uuid + "' ...");

        qna.deleteAlternativeQuestion(index);

        saveQuestionAnswer(domain, uuid, qna);
        retrain(new QnA(qna), domain, true);

        return qna;
    }

    /**
     * Add classification to trained QnA
     * @param classification Classification, e.g. "gravel bike"
     */
    public Answer addClassification(Context domain, String uuid, String classification) throws Exception {
        if (!isMemberOrAdmin(domain.getId())) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        Answer qna = getQnA(null, uuid, domain);

        String username = authService.getUsername();
        PermissionStatus permissionStatus = iamService.getPermissionStatus(qna, username);
        log.info("Permission status: " + permissionStatus);
        if (!iamService.isAuthorized(permissionStatus)) {
            throw new java.nio.file.AccessDeniedException("User '" + username + "' is not authorized to add classification!");
        }

        log.info("Add classification to trained QnA '" + domain.getId() + "/" + uuid + "' ...");

        qna.addClassification(classification.toLowerCase());

        saveQuestionAnswer(domain, uuid, qna);
        retrain(new QnA(qna), domain, true);

        return qna;
    }

    /**
     * Delete classification of trained QnA
     * @param index Index of classification, e.g. "2"
     */
    public Answer deleteClassification(Context domain, String uuid, int index) throws Exception {
        if (!isMemberOrAdmin(domain.getId())) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        Answer qna = getQnA(null, uuid, domain);

        String username = authService.getUsername();
        PermissionStatus permissionStatus = iamService.getPermissionStatus(qna, username);
        log.info("Permission status: " + permissionStatus);
        if (!iamService.isAuthorized(permissionStatus)) {
            throw new java.nio.file.AccessDeniedException("User '" + username + "' is not authorized to delete classification!");
        }

        log.info("Delete classification '" + index + "' of trained QnA '" + domain.getId() + "/" + uuid + "' ...");

        qna.deleteClassification(index);

        saveQuestionAnswer(domain, uuid, qna);
        retrain(new QnA(qna), domain, true);

        return qna;
    }

    /**
     * Update trained QnA
     * @param answer Updated answer
     * @param faqLanguage FAQ language, e.g. "en" or "de"
     * @param faqTopicId FAQ topic Id
     * @return updated QnA
     */
    public Answer updateTrainedQnA(Context domain, String uuid, String answer, Language faqLanguage, String faqTopicId) throws Exception {
        log.info("Update answer of trained QnA '" + domain.getId() + "/" + uuid + "' ...");

        Answer qna = getQnA(null, uuid, domain);


        if (qna.getFaqTopicId() != null) {
            String[] uuids = new String[1];
            uuids[0] = qna.getUuid();
            deleteQnAsFromFAQ(domain, uuids);
        }
        if (faqTopicId != null) {
            addQnA2FAQ(domain, faqLanguage.toString(), faqTopicId, qna.getUuid(), false);
        }
        qna.setFaqLanguage(faqLanguage);
        qna.setFaqTopicId(faqTopicId);


        if (qna.getIsReference()) {
            QnAReference reference = qna.getReference();
            log.info("Update referenced QnA '" + reference + "' ...");
            Context refDomain = getContext(reference.getDomainId());
            Answer refQnA = getQnA(null, reference.getUuid(), refDomain);
            refQnA.setAnswer(answer);
            qna.setDateAnswerModified(new Date());
            saveQuestionAnswer(refDomain, reference.getUuid(), refQnA);
            notifyOtherUsersAboutUpdatedSharedInformation(refQnA, domain);

            log.info("TODO: Retrain");
        } else {
            qna.setAnswer(answer);
            qna.setDateAnswerModified(new Date());
            saveQuestionAnswer(domain, uuid, qna);
            // TODO: Reconsider this notification!
            //notifyOtherUsersAboutUpdatedSharedInformation(qna, domain);
            
            retrain(new QnA(qna), domain, true);

        }

        return qna;
    }

    /**
     * Add question / answer of a resubmitted question to the knowledge base accordingly
     */
    public void addQuestionAnswer(ResubmittedQuestion qa) {
        log.info("Add question/answer for UUID '" + qa.getUuid() + "' ...");
        Context context = null;
        try {
            context = getContext(qa.getContextId());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        Answer answer = map(qa);

        saveQuestionAnswer(context, qa.getUuid(), answer);
    }

    /**
     * Add a new QnA to a particular knowledge base / domain
     * @param qna QnA data
     * @param domain Domain QnA is associated with
     * @return new QnA
     */
    public Answer addQuestionAnswer(Answer qna, Context domain) {
        String uuid = UUID.randomUUID().toString();
        qna.setUUID(uuid);

        if (qna.getDateOriginalQuestion() == -1) {
            qna.setDateOriginalQuestion(new Date());
        } else {
            log.info("Epoch time of original question already set: " + qna.getDateOriginalQuestion());
        }

        saveQuestionAnswer(domain, uuid, qna);

        return qna;
    }

    /**
     * Save data object in a particular knowledge base / domain
     * @param domain Domain data object is associated with
     * @param uuid UUID associated with data object
     * @param data JSON data object
     * @return meta information about data object, e.g. content type, creation and modification date
     */
    public DataObjectMetaInformation saveDataObject(Context domain, String uuid, JsonNode data) throws Exception {
        File uuidDir = new File(domain.getDataObjectsPath(), uuid);
        if (!uuidDir.isDirectory()) {
            log.info("Directory containing data object is being created: " + uuidDir.getAbsolutePath());
            uuidDir.mkdirs();
        }
        File dataFile = new File(uuidDir, DATA_OBJECZT_FILE);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(dataFile, data);

        DataObjectMetaInformation meta = new DataObjectMetaInformation(ContentType.APPLICATION_JSON);
        File metaFile = new File(uuidDir, DATA_OBJECT_META_FILE);
        objectMapper.writeValue(metaFile, meta);
        return meta;
    }

    /**
     * Check whether a particular data object exists
     * @return true when data object with a particular UUID exists and false otherwise
     */
    public boolean existsDataObject(String uuid, Context domain) {
        File uuidDir = new File(domain.getDataObjectsPath(), uuid);
        File metaFile = new File(uuidDir, DATA_OBJECT_META_FILE);

        if (metaFile.isFile()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get meta information of data object
     * @param uuid UUID of data object
     * @retur meta information of data object
     */
    public DataObjectMetaInformation getDataObjectMetaInformation(String uuid, Context domain) {
        File uuidDir = new File(domain.getDataObjectsPath(), uuid);
        File metaFile = new File(uuidDir, DATA_OBJECT_META_FILE);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            DataObjectMetaInformation meta = objectMapper.readValue(metaFile, DataObjectMetaInformation.class);
            return meta;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get data object as JSON
     */
    public JsonNode getDataObjectAsJson(String uuid, Context domain) {
        File uuidDir = new File(domain.getDataObjectsPath(), uuid);
        File dataFile = new File(uuidDir, DATA_OBJECZT_FILE);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(dataFile);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Link existing QnA from within another domain in order to share QnA
     * @param qnaDomainId Domain Id of existing QnA
     * @param qnaUuid UUID of existing QnA
     * @param domain Domain where link to existing QnA will be added
     */
    private Answer linkQuestionAnswer(String qnaDomainId, String qnaUuid, Context domain) throws Exception {
        String newUuid = saveQuestionAnswerLink(qnaDomainId, qnaUuid, domain);

        String userId = authService.getUserId();
        Answer newQnA = xmlService.parseQuestionAnswer(null, domain.getAnswersGenerallyProtected(), domain, newUuid, userId);

        return newQnA;
    }

    /**
     * Share information with Katie, e.g. 'The best seats in the movie theatre Alba are seats 12 and 13, row 5'
     * @param text Data shared, e.g. 'The best seats in the movie theatre Alba are seats 12 and 13, row 5'
     * @param url Source URL of shared text
     * @return UUID of newly created knowledge base item
     */
    public String shareInformation(String text, String url, Context domain) throws Exception {
        if (!isMemberOrAdmin(domain.getId())) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        String question = null;
        Date dateQuestion = new Date(); // TODO: When no question is set, then date question will also not be set!

        String answer = text;
        Date dateAnswered = dateQuestion;
        Date dateAnswerModified = dateAnswered;

        // TODO: Reconsider using QnA type as classification
        List<String> classifications = new ArrayList<String>();
        classifications.add(QnAType.DEFAULT.toString());

        ContentType contentType = null;
        Answer newQnA = new Answer(null, answer, contentType, url, classifications, QnAType.DEFAULT,null, dateAnswered, dateAnswerModified, null, null, null, question, dateQuestion,false, null, false, null);
        newQnA = addQuestionAnswer(newQnA, domain);

        train(new QnA(newQnA), domain, true);

        return newQnA.getUuid();
    }

    /**
     * Train / index QnA
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) throws Exception {
        aiService.train(qna, domain, indexAlternativeQuestions);
        updateTrained(domain, qna.getUuid(), true);
    }

    /**
     * Train multiple QnAs
     * @param qnas Array of QnAs
     * @param domain Domain the QnAs are associated with
     * @param indexAlternativeQuestions When set to true, then alternative questions are also indexed
     * @param processId Background process Id
     * @param throttleTimeInMillis Throttle training, whereas throttle time in milliseconds, e.g. 1500 milliseconds (Cohere allows max 100 API calls per minute for trial key)
     */
    private void train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions, int batchSize, String processId, int throttleTimeInMillis) throws Exception {
        if (throttleTimeInMillis > 0) {
            batchSize = 1;
            backgroundProcessService.updateProcessStatus(processId, "Throttle time in millis: " + throttleTimeInMillis + ", Batch size: " + batchSize);
        } else {
            backgroundProcessService.updateProcessStatus(processId, "No throttling, Batch size: " + batchSize);
        }

        int counter = 0;
        int batchStart = - batchSize;
        while (counter < qnas.length) {
            batchStart = batchStart + batchSize;
            int batchEnd = batchStart + batchSize;
            if (batchEnd > qnas.length) {
                batchEnd = qnas.length;
            }
            if (batchSize > 1) {
                backgroundProcessService.updateProcessStatus(processId, "Process Batch " + batchStart + " - " + (batchEnd - 1) + " ...");
            }
            List<QnA> batchQnAs = new ArrayList<QnA>();
            for (int i = batchStart; i < batchEnd; i++) {
                counter++;
                batchQnAs.add(qnas[i]);
            }

            if (throttleTimeInMillis > 0) {
                try {
                    log.info("Sleep for " + throttleTimeInMillis + " milliseconds ...");
                    Thread.sleep(throttleTimeInMillis);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            try {
                aiService.train(batchQnAs.toArray(new QnA[0]), domain, indexAlternativeQuestions, processId);
                /*
                for (QnA qna : batchQnAs) {
                    backgroundProcessService.updateProcessStatus(processId, "QnA '" + qna.getUuid() + "' trained.");
                }
                 */
                backgroundProcessService.updateProcessStatus(processId, batchEnd + " QnAs of total " + qnas.length + " trained.");
            } catch(Exception e) {
                backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
            }
        }

        backgroundProcessService.updateProcessStatus(processId, "Update status of QnAs ...");
        for (QnA qna : qnas) {
            // TODO: Only set true when QnA got trained successfully
            updateTrained(domain, qna.getUuid(), true);
        }
    }

    /**
     *
     */
    public void retrain(QnA qna, Context domain, boolean indexAlternativeQuestions) throws Exception {
        aiService.retrain(qna, domain, indexAlternativeQuestions);
        // TODO: Is the following necessary?!
        updateTrained(domain, qna.getUuid(), true);
    }

    /**
     * Add QnA(s) contained by the content of a particular URL
     * @param url URL to remember, for example "https://www.sciencedirect.com/science/article/pii/S2590188520300032#fn0004" or "https://en.wikipedia.org/wiki/Brazil"
     * @param keywords Keywords, for example "Translating natural language to SPARQL"
     */
    public void addUrlQnA(String url, String keywords, Context domain) throws Exception {
        if (!isMemberOrAdmin(domain.getId())) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        String question = null;
        if (keywords != null && !keywords.isEmpty()) {
            question = keywords;
        } else {
            // TODO: Prompt question
            log.warn("TODO: Extract keywords from URL");
            question = "TODO";
        }

        String answer = "<p>See <a href=\"" + url + "\">" + url + "</a></p>";

        Date dateQuestion = new Date();

        // TODO: Reconsider using QnA type as classification
        List<String> classifications = new ArrayList<String>();
        classifications.add(QnAType.BOOKMARK_URL.toString());

        String uuid = addBookmarkQnA(domain, question, answer, url, classifications, dateQuestion);
        addToUuidUrlIndex(uuid, url, domain);
    }

    /**
     * Add QnA referencing the content of a third-party webpage
     * @param url URL of third-party webpage, e.g. https://cwiki.apache.org/confluence/display/LUCENE/LuceneFAQ
     * @param question Question related with third-party webpage, e.g. "Does Lucene support auto-suggest / autocomplete?"
     * @param answer Answer related with third-party webpage, e.g. "Yes, see https://lucene.apache.org/core/9_4_2/suggest/index.html"
     * @return UUID of new QnA
     */
    private String addBookmarkQnA(Context domain, String question, String answer, String url, List<String> classifications, Date dateQuestion) throws Exception {
        Date dateAnswerModified = null;
        ContentType contentType = null;
        Answer newQnA = new Answer(null, answer, contentType, url, classifications, QnAType.BOOKMARK_URL, null, null, dateAnswerModified, null, null, null, question, dateQuestion,false, null, false, null);
        newQnA = addQuestionAnswer(newQnA, domain);
        train(new QnA(newQnA), domain, true);
        return newQnA.getUuid();
    }

    /**
     * Extract QnAs from the content referenced by URL
     * @param url URL of referenced document, e.g. "https://www.myright.ch/en/business/legal-tips/corona-companies/corona-effects-sme"
     * @param clean When set to true, then delete previously imported QnAs associated with URL
     * @param domain Domain the extracted QnAs will be associated with
     * @param processId Background process UUID
     */
    public void extractQnAs(String url, boolean clean, QnAExtractorImpl qnAExtractorImpl, Context domain, String processId) throws Exception {
        if (!isMemberOrAdmin(domain.getId())) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        String userId = authService.getUserId();
        backgroundProcessService.startProcess(processId, "Extract QnAs from '" + url + "' and add to domain '" + domain.getId() + "' ...", userId);

        if (domain.getUrlMetaFile(URI.create(url)).isFile()) {
            if (clean) {
                deletePreviouslyImportedChunks(url, domain);
            } else {
                log.warn("QnAs got already extracted from '" + url + "'!");
                throw new Exception("QnAs got already extracted from '" + url + "'!");
            }
        }

        QnAsFromWebpageService qnAsFromWebpageService = qnAsFromWebpageServiceMock;
        /*
        if (qnasFromWebpageDefaultImpl.equals(QnAExtractorImpl.REST)) {
            qnAsFromWebpageService = qnAsFromWebpageServiceRest;
        }
        */
        if (qnAExtractorImpl.equals(QnAExtractorImpl.REST)) {
            qnAsFromWebpageService = qnAsFromWebpageServiceRest;
        } else {
            qnAsFromWebpageService = qnAsFromWebpageServiceMock;
        }

        backgroundProcessService.updateProcessStatus(processId, "Extract QnAs ...");
        QnA[] qnas = qnAsFromWebpageService.getQnAs(URI.create(url), domain);

        Date currentDate = new Date();

        if (qnas != null && qnas.length > 0) {
            log.info("Webpage '" + url + "' contains " + qnas.length + " QnAs.");
            saveMetaInformation(url, url, currentDate, domain);
        } else {
            log.warn("No QnAs extracted from webpage '" + url + "'!");
            throw new Exception("No QnAs extracted from webpage '" + url + "'!");
        }

        backgroundProcessService.updateProcessStatus(processId, "Add extracted QnAs to domain '" + domain.getId() + "' ...");
        // TODO: Reconsider using QnA type as classification
        List<String> classifications = new ArrayList<String>();
        classifications.add(QnAType.BOOKMARK_URL.toString());
        for (QnA qna: qnas) {
            String uuid = addBookmarkQnA(domain, qna.getQuestion(), qna.getAnswer(), url, classifications, currentDate);
            addToUuidUrlIndex(uuid, url, domain);
        }

        backgroundProcessService.stopProcess(processId);
    }

    /**
     *
     */
    public void deletePreviouslyImportedChunks(String url, Context domain) {
        List<String> uuids = new ArrayList<String>();

        File uuidIndexFile = domain.getUuidUrlIndex(URI.create(url));
        if (uuidIndexFile.isFile()) {
            try {
                FileInputStream fis = new FileInputStream(uuidIndexFile.getAbsolutePath());
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                String line;
                while((line = br.readLine()) != null) {
                    uuids.add(line);
                }
                br.close();
                fis.close();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.warn("No such file: " + uuidIndexFile.getAbsolutePath());
            return;
        }

        for (String uuid: uuids) {
            try {
                deleteTrainedQnA(domain, uuid);
                // TODO: Delete UUID from index file, whereas see below
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        // TODO: Delete UUID from index instead just deleting whole index, whereas see above
        uuidIndexFile.delete();
    }

    /**
     * Save meta information re extraction of text / QnAs from URL
     * @param contentUrl Content URL, e.g. "https://graph.microsoft.com/v1.0/groups/c5a3125f-f85a-472a-8561-db2cf74396ea/onenote/pages/1-fd1e338afe640a3219c58b850ad3c4f6!1-5aaade12-a1fc-478c-b98c-1f888fed25a0/content"
     * @param webUrl Web URL, e.g. "https://szhglobal.sharepoint.com/sites/MSGR-00000778/Shared%20Documents/General/WIKI%20Energieberatung?wd=target%28F%C3%B6rderprogramme.one%7Cfb8f3fb7-e89b-4d08-b9f2-b52248c15f1e%2FFAQ%20F%C3%B6rderprogramme%7C9d034704-bbf1-43f6-8208-e5a29c649b04%2F%29"
     * @param date Date when text / QnAs got extracted
     */
    public void saveMetaInformation(String contentUrl, String webUrl, Date date, Context domain) {
        File metaFile = domain.getUrlMetaFile(URI.create(contentUrl));
        if (!metaFile.getParentFile().isDirectory()) {
            metaFile.getParentFile().mkdirs();
        }
        xmlService.createUrlMeta(metaFile, contentUrl, webUrl, date.getTime());
    }

    /**
     * Add UUID to index file
     */
    protected void addToUuidUrlIndex(String uuid, String url, Context domain) throws Exception {
        File file = domain.getUuidUrlIndex(URI.create(url));
        if (!file.getParentFile().isDirectory()) {
            file.getParentFile().mkdirs();
        }
        Utils.saveText(uuid + "\n", file, true);
    }

    /**
     * @param hint Hint re credentials, e.g. "My Netflix credentials"
     * @param encryptedCredentials Client side encrypted credentials
     * @param clientSideEncryptionAlgorithm Client side encryption algorithm, e.g. "aes-256"
     * @return UUID of shared information item
     */
    public String shareCredentials(String hint, String encryptedCredentials, String clientSideEncryptionAlgorithm, Context domain) throws Exception {
        if (!isMemberOrAdmin(domain.getId())) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        String question = hint;
        String answer = encryptedCredentials;

        Date dateQuestion = new Date();
        Date dateAnswerModified = null;

        // TODO: Reconsider using QnA type as classification
        List<String> classifications = new ArrayList<String>();
        classifications.add(QnAType.CREDENTIALS.toString());
        ContentType contentType = null;
        Answer newQnA = new Answer(null, answer, contentType,null, classifications, QnAType.CREDENTIALS, clientSideEncryptionAlgorithm, null, dateAnswerModified, null, null, null, question, dateQuestion,false, null, false, null);
        newQnA = addQuestionAnswer(newQnA, domain);

        train(new QnA(newQnA), domain, true);

        return newQnA.getUuid();
    }

    /**
     * @param hint Hint re shopping list, e.g. "Things to buy at supermarket"
     * @param shoppingList Shopping list items, "bread, butter, milk, ..."
     * @return UUID of shared information item
     */
    public String shareShoppingList(String hint, String shoppingList, Context domain) throws Exception {
        if (!isMemberOrAdmin(domain.getId())) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        String question = hint;

        Date dateQuestion = new Date();
        String answer = shoppingList;
        Date dateAnswerModified = null;
        // TODO: Reconsider using QnA type as classification
        List<String> classifications = new ArrayList<String>();
        classifications.add(QnAType.SHOPPING_LIST.toString());
        ContentType contentType = null;
        Answer newQnA = new Answer(null, answer, contentType,null, classifications, QnAType.SHOPPING_LIST, null, null, dateAnswerModified, null, null, null, question, dateQuestion,false, null, false, null);
        newQnA = addQuestionAnswer(newQnA, domain);

        train(new QnA(newQnA), domain, true);

        return newQnA.getUuid();
    }

    /**
     * Save question / answer to the knowledge base of a particular domain
     * @oaram domain Domain QnA is associated with
     * @oaram uuid UUID of QnA
     * @param qna QnA data
     */
    public void saveQuestionAnswer(Context domain, String uuid, Answer qna) {
        log.info("Save QnA with UUID '" + uuid + "' ...");

        File uuidDir = new File(domain.getQuestionsAnswersDataPath(), uuid);
        if (!uuidDir.isDirectory()) {
            log.info("Directory containing QnA is being created: " + uuidDir.getAbsolutePath());
            uuidDir.mkdirs();
        }
        
        xmlService.saveQuestionAnswer(qna, domain.getQnAXmlFilePath(uuid));
    }

    /**
     * Save rating connected with a particular QnA
     * @oaram domain Domain QnA is associated with
     * @param qna QnA connected with rating
     */
    private void saveRating(Context domain, Answer qna) {
        log.info("Save rating for QnA with UUID '" + qna.getUuid() + "' ...");

        xmlService.saveRatings(qna, domain.getRatingsXmlFilePath(qna.getUuid()));
    }

    /**
     * Save rating per domain
     * @param domain Knowledge base where question was asked
     * @param rating Rating of answer for question asked
     * @param answer Plain text answer
     */
    private void saveRating(Context domain, Rating rating, String answer) {
        log.info("Save rating of answer '" + rating.getQnauuid() + "' for question '" + rating.getQuestionuuid() + "' ...");

        HumanPreference humanPreference = new HumanPreference();
        humanPreference.setHumanMessage(rating.getUserquestion());
        if (rating.getRating() < 5) {
            humanPreference.setRejectedAnswer(answer);
        } else {
            humanPreference.setChosenAnswer(answer);
        }

        // INFO: See HumanPreference.java
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("humanMessage", rating.getUserquestion());
        if (rating.getRating() < 5) {
            rootNode.put("rejectedAnswer", answer);
        } else {
            rootNode.put("chosenAnswer", answer);
        }
        ObjectNode metaNode = mapper.createObjectNode();
        rootNode.put("meta", metaNode);
        metaNode.put("rating", rating.getRating());
        if (rating.getDate() != null) {
            metaNode.put("epochTime", rating.getDate().getTime());
        }
        metaNode.put("questionUuid", rating.getQuestionuuid());
        metaNode.put("qnaUuid", rating.getQnauuid());
        metaNode.put("humanFeedback", rating.getFeedback());
        metaNode.put("userEmail", rating.getEmail());
        try {
            AskedQuestion askedQuestion = dataRepositoryService.getQuestionByUUID(rating.getQuestionuuid());
            if (askedQuestion.getClientMessageId() != null) {
                metaNode.put("clientMessageId", askedQuestion.getClientMessageId());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            if (!domain.getRatingsDirectory().isDirectory()) {
                domain.getRatingsDirectory().mkdir();
            }
            // INFO: Multiple users can rate the same question / answer pair, therefore each rating requires a unique id
            String ratingFilename = UUID.randomUUID().toString() + ".json";
            File ratingFile = new File(domain.getRatingsDirectory(), ratingFilename);
            mapper.writeValue(ratingFile, rootNode);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Get preferences / ratings of answers of a particular domain
     */
    public HumanPreference[] getRatings(String domainId) throws Exception {
        List<HumanPreference> preferences = new ArrayList<>();

        Context domain = getDomain(domainId);
        File ratingsDir = domain.getRatingsDirectory();
        File[] ratingFiles = ratingsDir.listFiles();
        ObjectMapper mapper = new ObjectMapper();
        if (ratingFiles != null) {
            for (File ratingFile : ratingFiles) {
                HumanPreference humanPreference = mapper.readValue(ratingFile, HumanPreference.class);
                preferences.add(humanPreference);
            }
        } else {
            log.warn("No preferences / ratings yet.");
        }

        return preferences.toArray(new HumanPreference[0]);
    }
    /**
     * @param channelRequestId Channel request Id, e.g. "3139c14f-ae63-4fc4-abe2-adceb67988af"
     * @param threadId Thread Id, e.g. "1084254275661725697" in the case of Discord or "C045ZFM7PUH-1678626022.589689" in the case of Slack
     * @param message Thread message, e.g. "Michael has 3 sons"
     */
    public void saveThreadMessage(Context domain, String channelId, String  channelRequestId, String threadId, String message) {
        File threadDir = domain.getChannelThreadDataPath(channelId, channelRequestId);
        if (!threadDir.isDirectory()) {
            log.info("Directory containing threads of channel is being created: " + threadDir.getAbsolutePath());
            threadDir.mkdirs();
        }

        log.info("Save thread message (Thread Id: " + threadId + ") ...");
        String fileName = new Date().getTime() + ".xml";
        File threadMessageFile = new File(threadDir, fileName);
        xmlService.saveThreadMessage(threadId, message, threadMessageFile);
    }

    /**
     * Get thread messages
     * @param domain Katie domain
     * @param channelId Channel Id
     * @param channelRequestId Channel request Id
     * @return thread messages and null when thread does not exist or no messages yet
     */
    public String[] getThreadMessages(Context domain, String channelId, String channelRequestId) {
        File threadDir = domain.getChannelThreadDataPath(channelId, channelRequestId);
        log.info("Check thread directory '" + threadDir + "' ...");

        if (threadDir.isDirectory()) {
            File[] threadMessages = threadDir.listFiles();
            if (threadMessages.length > 0) {
                List<String> messages = new ArrayList<String>();
                for (File file: threadMessages) {
                    try {
                        messages.add(xmlService.readThreadMessage(file));
                    } catch(Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
                return messages.toArray(new String[0]);
            } else {
                log.warn("Empty thread directory: " + threadDir.getAbsolutePath());
                return null;
            }
        } else {
            log.warn("No such thread directory: " + threadDir.getAbsolutePath());
            return null;
        }
    }

    /**
     * Save question/answer link persistently as XML file
     * @param linkDomainId Referenced Domain Id
     * @param linkUuid Referenced UUID
     * @return UUID of new reference QnA
     */
    private String saveQuestionAnswerLink(String linkDomainId, String linkUuid, Context domain) throws Exception {
        String uuid = UUID.randomUUID().toString();

        File uuidDir = new File(domain.getQuestionsAnswersDataPath(), uuid);
        if (!uuidDir.isDirectory()) {
            log.info("Directory containing QnA is being created: " + uuidDir.getAbsolutePath());
            uuidDir.mkdirs();
        }
        File xmlFile = domain.getQnAXmlFilePath(uuid);

        FileOutputStream out = new FileOutputStream(xmlFile);

        org.w3c.dom.Document qaDoc = xmlService.createQALinkDocument(linkDomainId, linkUuid, uuid);

        xmlService.writeDocument(qaDoc, out, true);
        out.close();

        return uuid;
    }

    /**
     * Save email persistently
     * @param uuid UUID of email
     */
    public void saveEmail(Context domain, String uuid, javax.mail.Message message) {
        File emailsDir = domain.getPendingEmailsDataPath();
        if (!emailsDir.isDirectory()) {
            emailsDir.mkdirs();
        }
        File emailFile = new File(emailsDir, uuid + ".eml");
        log.info("Save email persistently to '" + emailFile.getAbsolutePath() + "' ...");
        try {
            message.writeTo(new FileOutputStream(emailFile));
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Read persistently stored email
     * @param uuid UUID of email, e.g. "a9f226b6-5482-47ce-bccf-bcf8beb7e81c"
     */
    public javax.mail.Message readEmail(Context domain, String uuid) {
        File emailFile = getEmailFile(domain, uuid);
        try {
            return new MimeMessage(null, new FileInputStream(emailFile));
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     *
     */
    public File getEmailFile(Context domain, String uuid) {
        File emailsDir = domain.getPendingEmailsDataPath();
        return new File(emailsDir, uuid + ".eml");
    }

    /**
     * Map resubmitted question (including answer) onto answer object
     */
    private Answer map(ResubmittedQuestion qa) {
        boolean isPublic = false; // TODO: Undefined actually, because ResubmittedQuestion does not contain this information
        Permissions permissions = null;

        Ownership ownership = qa.getOwnership();
        if (ownership != null) {
            if (ownership == Ownership.iam_source) {
                List<String> users = new ArrayList<String>();

                // INFO: User which answered question
                users.add(qa.getRespondentUserId());
                log.info("Respondent user '" + qa.getRespondentUserId() + "' is allowed to read answer.");

                // INFO: User which asked question
                User sourceUser = iamService.getUser(qa);
                if (sourceUser != null && !sourceUser.getId().equals(qa.getRespondentUserId())) {
                    log.info("Question user '" + sourceUser.getId() + "' is allowed to read answer.");
                    users.add(sourceUser.getId());
                } else {
                    log.info("No question user available.");
                }

                permissions = new Permissions(users.toArray(new String[0]), null);
            } else if (ownership == Ownership.iam_context) {
                isPublic = false;
            } else if (ownership == Ownership.iam_public) {
                try {
                    Context context = getContext(qa.getContextId());
                    if (context.getAnswersGenerallyProtected()) {
                        log.info("Permit everyone to view answer.");
                        isPublic = true;
                        permissions = new Permissions(true);
                    }
                } catch(Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.warn("No such ownership '" + ownership + "' implemented!");
            }
        } else {
            log.info("Answer has no owner.");
        }

        // TODO: Date dateAnswered = qa.getTimestampAnswered();
        Date dateAnswered = null;
        Date dateAnswerModified = null;

        // TODO/TBD: Get type from resubmitted QnA?!
        // TODO: Get classification from resubmitted QnA
        List<String> classifications = new ArrayList<String>();

        String url = null; // TODO: Get URL in case one is provided

        ContentType contentType = null;
        return new Answer(null, qa.getAnswer(), contentType, url, classifications, QnAType.DEFAULT, qa.getAnswerClientSideEncryptedAlgorithm(), dateAnswered, dateAnswerModified,  qa.getEmail(), qa.getContextId(), qa.getUuid(), qa.getQuestion(), qa.getTimestampResubmitted(), isPublic, permissions, false, qa.getRespondentUserId());
    }

    /**
     * Check whether QnA for a particular UUID and domain exists
     * @param uuid UUID of QnA
     * @return true when QnA exists and false otherwise
     */
    public boolean existsQnA(String uuid, Context domain) {
        log.info("Check whether QnA with UUID '" + uuid + "' exists inside domain / knowledge base '" + domain.getId() + "' ...");
        File xmlFile = domain.getQnAXmlFilePath(uuid);
        if (xmlFile.isFile()) {
            return true;
        }
        return false;
    }

    /**
     * Get QnA for a particular domain and UUID
     * @param submittedQuestion Submitted question by user
     * @param uuid UUID of question/answer
     * @return QnA data
     */
    public Answer getQnA(String submittedQuestion, String uuid, Context domain) throws Exception {
        log.info("Get QnA with UUID '" + uuid + "' of domain '" + domain.getId() + "' ...");
        try {
            String userId = authService.getUserId();
            Answer qna = xmlService.parseQuestionAnswer(submittedQuestion, domain.getAnswersGenerallyProtected(), domain, uuid, userId);
            if (qna == null) {
                log.error("No such QnA '" + domain.getId() + " / " + uuid + "'!");
                //throw new Exception("No QnA with UUID '" + uuid + "'!");
                return null;
            }

            String username = authService.getUsername();
            PermissionStatus permissionStatus = iamService.getPermissionStatus(qna, username);
            log.info("TODO/TBD: Check authorization (Permission status of QnA '" + uuid + "': " + permissionStatus + ") and if access denied, then throw AuthorizationException(permissionStatus");

            return qna;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get dumped content referenced by URL
     */
    protected String getDumpedContent(String url, Context domain) {
        File file = domain.getUrlDumpFile(URI.create(url));
        if (file.isFile()) {
            try {
                //Utils.convertInputStreamToString(new FileInputStream(file));
                return Files.readString(Paths.get(file.getAbsolutePath()));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }
        } else {
            log.warn("For URL '" + url + "' no dumped content available: " + file.getAbsolutePath());
            return null;
        }
    }

    /**
     * Get link to QnA (TODO: Move this method to UtilService)
     */
    public String getAnswerLink(ResubmittedQuestion question, Context domain) {
        String answerLink = domain.getHost() + "/" + mailBodyAskKatieReadAnswerUrl + "?domain-id=" + question.getContextId() + "&uuid=" + question.getUuid();
        if (question.getEmail() != null) {
            answerLink = domain.getHost() + "/" + mailBodyAskKatieReadAnswerUrl + "?domain-id=" +question.getContextId() + "&uuid=" + question.getUuid() + "&username=" + question.getEmail();
        }
        return answerLink;
    }

    /**
     * Generate report for experts, which is sent periodically, e.g. weekly or monthly
     * @param lastNumberOfDays Last number of days, -1 when no last number of days provided
     * @return number of reports sent
     */
    public int generateReport(Context domain, int lastNumberOfDays) throws Exception {
        if (!isMemberOrAdmin(domain.getId())) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domain.getId() + "', nor has role " + Role.ADMIN + "!");
        }

        int limit = 100;
        int offset = 0;

        // INFO: Answer pending questions
        ResubmittedQuestion[] pendingQuestions = dataRepositoryService.getResubmittedQuestions(StatusResubmittedQuestion.STATUS_PENDING, domain.getId(), limit, offset);
        if (pendingQuestions.length == 0) {
            log.info("No pending questions.");
        }

        // INFO: Review and rate sent answers to resubmitted questions
        ResubmittedQuestion[] answeredQuestions = dataRepositoryService.getResubmittedQuestions(StatusResubmittedQuestion.STATUS_ANSWER_SENT, domain.getId(), limit, offset);
        if (answeredQuestions.length == 0) {
            log.info("No answered resubmitted questions.");
        }
        List<ResubmittedQuestion> qnas = new ArrayList<ResubmittedQuestion>();
        for (ResubmittedQuestion reQuestion : answeredQuestions) {
            qnas.add(fixNullValues(reQuestion));
        }

        DomainInsights insights = getInsightsSummary(domain.getId(), lastNumberOfDays);

        TemplateArguments templArgs = new TemplateArguments(domain, null);
        templArgs.add("last_number_of_days", lastNumberOfDays);
        templArgs.add("insights", insights);

        LanguagePageviews[] faqPageviews = insights.getFaqPageviews();
        templArgs.add("faq_pageviews", faqPageviews);

        templArgs.add("pqs", pendingQuestions);
        templArgs.add("qas", qnas);
        templArgs.add("read_answer_url", domain.getHost() + "/" + mailBodyAskKatieReadAnswerUrl);

        User[] experts = getExperts(domain.getId(), false);
        for (User expert : experts) {
            templArgs.add("user_firstname_lastname", getFirstLastName(expert));
            templArgs.add("email", expert.getEmail());

            StringWriter writer = new StringWriter();
            //log.debug("Template default encoding: " + configuration.getDefaultEncoding());
            //log.debug("Template encoding: " + configuration.getEncoding(new java.util.Locale("de")));
            Template template = mailerService.getTemplate("katie-needs-your-help-as-expert_", Language.valueOf(expert.getLanguage()), domain);
            template.process(templArgs.getArgs(), writer);
            String html = writer.toString();

            mailerService.send(expert.getEmail(), domain.getMailSenderEmail(), getReportMailSubject(domain, expert.getLanguage(), lastNumberOfDays), html, true);
        }

        return experts.length;
    }

    /**
     * Get mail subject for sending report
     */
    private String getReportMailSubject(Context domain, String language, int lastNumberOfDays) {
        log.info("Generate subject for sending report in '" + language + "' for the last " + lastNumberOfDays + " days ...");
        String[] args = new String[1];
        args[0] = "" + lastNumberOfDays;
        String subject = "[" + domain.getMailSubjectTag() + "] " + messageSource.getMessage("katies.insights.last.days", args, new Locale(language));
        log.info("Generated subject: " + subject);
        return subject;
    }

    /**
     *
     */
    private ResubmittedQuestion fixNullValues(ResubmittedQuestion qna) {
        if (qna.getUuid() == null) {
            log.error("UUID missing!");
            qna.setUuid("error_uuid_missing");
        }
        if (qna.getQuestion() == null) {
            log.error("Question missing!");
            qna.setQuestion("Error: Question missing");
        }
        if (qna.getAnswer() == null) {
            log.info("Answer missing");
            qna.setAnswer("Error: Answer missing");
        }
        return qna;
    }

    /**
     * @param email E-Mail of other user
     */
    public void shareQnAByEmail(String domainId, String uuid, String email) throws Exception {
        log.info("Try to share QnA '" + domainId + " / " + uuid + "' with '" + email + "' ...");
        if (!isMemberOrAdmin(domainId)) {
            throw new java.nio.file.AccessDeniedException("User is neither member of domain '" + domainId + "', nor has role " + Role.ADMIN + "!");
        }

        User user = iamService.getUserByUsername(new Username(email), false, false);

        if (user != null) {
            if (user.getId().equals(authService.getUserId())) {
                throw new Exception("Other user is the same as signed in user!");
            }

            log.info("User with email '" + email + "' also has a Katie domain: " + getFirstLastName(user));
            String myKatieDomainIdOfOtherUser = getPersonalDomainId(user.getId());
            if (myKatieDomainIdOfOtherUser != null) {
                Context domain = getContext(domainId);
                Answer qna = getQnA(null, uuid, domain);

                if (qna != null) {
                    if (qna.getIsReference()) {
                        // TODO: Allow referencing a reference, but prevent infinite loops
                        throw new Exception("QnA '" + domain.getId() + "/" + uuid + "' itself is a reference to another QnA. Referecing a reference is not supported yet!");
                    } else {
                        log.info("Link QnA '" + qna.getOriginalquestion() + "' from within MyKatie domain '" + myKatieDomainIdOfOtherUser + "' of other user ...");

                        Answer newQnA = linkQuestionAnswer(domain.getId(), uuid, getContext(myKatieDomainIdOfOtherUser));
                        log.info("New QnA added: " + newQnA.getUuid());

                        Context domainOfOtherUser = getContext(myKatieDomainIdOfOtherUser);
                        train(new QnA(newQnA), domainOfOtherUser, true);

                        notifyOtherUserAboutSharedInformation(user, null, domain);
                    }
                } else {
                    log.error("No such QnA '" + domainId  + " / " + uuid + "'!");
                }
            } else {
                log.error("User '" + user.getId() + "' does not have a MyKatie domain!");
            }
        } else {
            log.info("No such user with email '" + email + "' within Katie! Therefore send notification containing information ...");
            Context domain = getContext(domainId);
            Answer qna = getQnA(null, uuid, domain);
            notifyOtherUserContainingSharedInformation(email, qna, domain);
        }
    }

    /**
     * Notify all users that the shared information has been updated
     */
    private void notifyOtherUsersAboutUpdatedSharedInformation(Answer sourceQnA, Context domain) {
        log.info("Notify all other users that the shared information '" + sourceQnA.getDomainid() + " / " + sourceQnA.getUuid() + "' has been updated ...");

        try {
            User[] users = getMembers(sourceQnA.getDomainid(), false);
            // TODO: Also get users from other domains, where this information is shared with

            User signedInUser = authService.getUser(false, false);

            String firstLastName = signedInUser.getUsername();
            if (signedInUser.getFirstname() != null || signedInUser.getLastname() != null) {
                firstLastName = getFirstLastName(signedInUser);
            }

            String[] args = new String[1];
            args[0] = firstLastName;

            for (int i = 0; i < users.length; i++) {
                if (!signedInUser.getId().equals(users[i].getId())) {
                    Locale locale = new Locale(users[i].getLanguage());
                    String subject = "[MyKatie] " + messageSource.getMessage("mykatie.shared.information.updated", args, locale);

                    String body = body = getSharedInformationUpdatedBody(Language.valueOf(users[i].getLanguage()), firstLastName, domain);
                    mailerService.send(users[i].getEmail(), null, subject, body, true);
                } else {
                    log.info("Do not send notification to yourself :-)");
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Notify other user about shared information
     * @param user The other user
     */
    private void notifyOtherUserAboutSharedInformation(User user, Answer qna, Context domain) throws Exception {
        String fromAddress = null; // INFO: Default from address of Katie will be used

        User signedInUser = authService.getUser(false, false);
        String firstLastName = signedInUser.getUsername();
        if (signedInUser.getFirstname() != null || signedInUser.getLastname() != null) {
            firstLastName = getFirstLastName(signedInUser);
        }

        String[] args = new String[1];
        args[0] = firstLastName;
        Locale locale = new Locale(user.getLanguage());
        String subject = "[MyKatie] " + messageSource.getMessage("mykatie.just.shared.information", args, locale);

        String body = body = getSharedInformationBody(Language.valueOf(user.getLanguage()), firstLastName, domain);

        mailerService.send(user.getEmail(), fromAddress, subject, body, true);
    }

    /**
     * @param lang Language of user receiving notification
     * @param firstLastName Name of user which is sharing information
     */
    private String getSharedInformationBody(Language lang, String firstLastName, Context domain) throws Exception {
        TemplateArguments tmplArgs = new TemplateArguments(domain, null);

        tmplArgs.add("user_name", firstLastName);

        tmplArgs.add("mykatie_link", domain.getHost() + "/#/my-katie");

        StringWriter writer = new StringWriter();
        Template template = mailerService.getTemplate("notify-user-about-shared-information_", lang, domain);
        template.process(tmplArgs.getArgs(), writer);
        return writer.toString();
    }

    /**
     * @param lang Language of user receiving notification
     * @param firstLastName Name of user which is sharing information
     */
    private String getSharedInformationUpdatedBody(Language lang, String firstLastName, Context domain) throws Exception {
        TemplateArguments tmplArgs = new TemplateArguments(domain, null);

        tmplArgs.add("user_name", firstLastName);

        tmplArgs.add("mykatie_link", domain.getHost() + "/#/my-katie");

        StringWriter writer = new StringWriter();
        Template template = mailerService.getTemplate("notify-user-about-updated-shared-information_", lang, domain);
        template.process(tmplArgs.getArgs(), writer);
        return writer.toString();
    }

    /**
     * Notify other user about shared information
     * @param email E-Mail of other user
     */
    private void notifyOtherUserContainingSharedInformation(String email, Answer qna, Context domain) throws Exception {
        String fromAddress = null; // INFO: Default from address of Katie will be used

        User signedInUser = authService.getUser(false, false);
        String firstLastName = signedInUser.getUsername();
        if (signedInUser.getFirstname() != null || signedInUser.getLastname() != null) {
            firstLastName = getFirstLastName(signedInUser);
        }

        String[] args = new String[1];
        args[0] = firstLastName;
        Locale locale = new Locale("en");
        String subject = "[MyKatie] " + messageSource.getMessage("mykatie.just.shared.information", args, locale);

        String body = getSharedInformationBodyContainingInfo(Language.valueOf("en"), firstLastName, qna, domain);

        mailerService.send(email, fromAddress, subject, body, true);
    }

    /**
     * @param lang Language of user receiving notification
     * @param firstLastName Name of user which is sharing information
     */
    private String getSharedInformationBodyContainingInfo(Language lang, String firstLastName, Answer qna, Context domain) throws Exception {
        TemplateArguments tmplArgs = new TemplateArguments(domain, null);

        tmplArgs.add("user_name", firstLastName);

        tmplArgs.add("information", qna.getAnswer());

        tmplArgs.add("katie_link", domain.getHost());

        StringWriter writer = new StringWriter();
        Template template = mailerService.getTemplate("notify-user-containing-shared-information_", lang, domain);
        template.process(tmplArgs.getArgs(), writer);
        return writer.toString();
    }

    /**
     * Delete trained QnA from index and from persistent storage
     */
    public void deleteTrainedQnA(Context domain, String uuid) throws Exception {
        boolean deleted = aiService.delete(uuid, domain);

        String[] uuids = new String[1];
        uuids[0] = uuid;

        deleteQnAsFromFAQ(domain, uuids);
        deleteQnAsFromStorage(domain, uuids);
    }

    /**
     * Delete untrained QnA from persistent storage
     */
    public void deleteUntrainedQnA(Context domain, String uuid) throws Exception {
        String[] uuids = new String[1];
        uuids[0] = uuid;

        deleteQnAsFromFAQ(domain, uuids);
        deleteQnAsFromStorage(domain, uuids);
    }

    /**
     * Delete QnAs from FAQ of a particular domain
     * @param domain Domain associated with QnAs
     * @param uuids Array of UUIDs of QnAs
     */
    private void deleteQnAsFromFAQ(Context domain, String[] uuids) throws Exception {
        log.info("Try to delete QnAs from FAQ of domain '" + domain.getId() + "' ...");
        String[] faqLanguages = domain.getFAQLanguages();
        for (String faqLang: faqLanguages) {
            FAQ faq = getFAQ(domain, faqLang, true, false);
            for (String uuid : uuids) {
                if (faq.removeQnA(uuid)) {
                    xmlService.createFAQ(domain, faqLang, faq);
                    log.info("QnA '" + uuid + "' has been removed from FAQ with language '" + faqLang + "'.");
                }
            }
        }
    }

    /**
     * Delete QnAs from persistent storage
     */
    public void deleteQnAsFromStorage(Context domain, String[] uuids) throws Exception {
        for (String uuid : uuids) {
            log.info("Delete QnA with UUID '" + uuid + "' and domain '" + domain.getId() + "' from storage ...");
            File uuidDir = new File(domain.getQuestionsAnswersDataPath(), uuid);
            if (uuidDir.isDirectory()) {
                FileUtils.deleteDirectory(uuidDir);
            } else {
                log.warn("No such directory '" + uuidDir.getAbsolutePath() + "'!");
            }
        }
    }

    /**
     * Check whether domain contains trained QnAs
     * @return true when domain contains trained QnAs and false otherwise
     */
    public boolean hasTrainedQnAs(Context domain) {
        Path path = Paths.get(domain.getQuestionsAnswersDataPath().getAbsolutePath());

        if (Files.isDirectory(path)) {
            log.debug("Check whether directory '" + path + "' contains QnAs ...");
            try {
                DirectoryStream<Path> directory = Files.newDirectoryStream(path);
                return directory.iterator().hasNext();
            } catch(Exception e) {
                log.error(e.getMessage(), e);
                return false;
            }
        }

        log.debug("No QnAs directory: " + path);
        return false;
    }

    /**
     * Delete all trained QnAs of a particular domain, which the user is authorized to access
     * @param domainId Domain Id
     */
    public void deleteAllTrainedQnAs(String domainId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            String msg = "User is neither member of domain '" + domainId + "', nor has system role " + Role.ADMIN + "!";
            log.info(msg);
            throw new java.nio.file.AccessDeniedException(msg);
        }

        RoleDomain role = xmlService.getRole(authService.getUserId(), domainId);
        if (!(role == RoleDomain.ADMIN || role == RoleDomain.OWNER)) {
            String msg = "Member of domain '" + domainId + "' has neither domain role " + RoleDomain.ADMIN + " nor " + RoleDomain.OWNER + "!";
            log.info(msg);
            throw new java.nio.file.AccessDeniedException(msg);
        }

        // TODO: Implement batch delete
        Context domain = getContext(domainId);
        Answer[] qnas = getTrainedQnAs(domain, 0, -1);
        for (Answer qna : qnas) {
            deleteTrainedQnA(domain, qna.getUuid());
        }
    }

    /**
     * Delete all QnAs of a particular domain
     * @param domainId Domain Id
     */
    public void deleteAllQnAs(String domainId) throws Exception {
        if (!isMemberOrAdmin(domainId)) {
            String msg = "User is neither member of domain '" + domainId + "', nor has system role " + Role.ADMIN + "!";
            log.info(msg);
            throw new java.nio.file.AccessDeniedException(msg);
        }

        RoleDomain role = xmlService.getRole(authService.getUserId(), domainId);
        if (!(role == RoleDomain.ADMIN || role == RoleDomain.OWNER)) {
            String msg = "Member of domain '" + domainId + "' has neither domain role " + RoleDomain.ADMIN + " nor " + RoleDomain.OWNER + "!";
            log.info(msg);
            throw new java.nio.file.AccessDeniedException(msg);
        }

        Context domain = getContext(domainId);

        Answer[] qnas = getAllQnAs(domain);
        String[] uuids = new String[qnas.length];
        for (int i = 0; i < qnas.length; i++) {
            uuids[i] = qnas[i].getUuid();
        }

        aiService.clean(domain);

        deleteQnAsFromFAQ(domain, uuids);
        deleteQnAsFromStorage(domain, uuids);

        File urlsBaseDir = domain.getURLsDataPath();
        log.info("Try to delete urls base dir: " + urlsBaseDir.getAbsolutePath());
        if (urlsBaseDir.isDirectory()) {
            FileUtils.deleteDirectory(urlsBaseDir);
        }
    }

    /**
     *
     */
    private Answer[] getAllQnAs(Context domain) {
        log.info("Get all QnAs of domain '" + domain.getId() + "' ...");
        List<Answer> answers = new ArrayList<Answer>();

        File questionsAnswersDir = domain.getQuestionsAnswersDataPath();
        if (questionsAnswersDir.isDirectory()) {
            String[] answerUUIDs = questionsAnswersDir.list();
            if (questionsAnswersDir.isDirectory()) {
                for (int i = 0; i < answerUUIDs.length; i++) {
                    try {
                        Answer qna = getQnA(null, answerUUIDs[i], domain);
                        if (qna != null) {
                            answers.add(qna);
                        } else {
                            log.error("QnA '" + answerUUIDs[i] + "' of domain '" + domain.getId() + "' could not be initiated!");
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        } else {
            String errMsg = "No such directory '" + questionsAnswersDir.getAbsolutePath() + "'!";
            log.error(errMsg);
        }
        return answers.toArray(new Answer[0]);
    }

    /**
     * Get all trained QnAs of a particular domain, which the user is authorized to access
     * @param domain Domain containing QnAs
     * @param limit Limit number of returned QnAs
     * @param offset From where to start returning QnAs
     */
    public Answer[] getTrainedQnAs(Context domain, int limit, int offset) throws Exception {
        log.info("Get all trained QnAs of domain '" + domain.getId() + "' ...");

        List<Answer> answers = new ArrayList<Answer>();

        File questionsAnswersDir = domain.getQuestionsAnswersDataPath();
        if (questionsAnswersDir.isDirectory()) {
            String username = authService.getUsername();

            String[] answerUUIDs = questionsAnswersDir.list();

            for (int i = 0; i < answerUUIDs.length; i++) {
                log.info("Trained answer UUID: " + answerUUIDs[i]);
                try {
                    Answer qna = getQnA(null, answerUUIDs[i], domain);
                    if (qna.isTrained()) {
                        PermissionStatus ps = iamService.getPermissionStatus(qna, username);
                        if (iamService.isAuthorized(ps)) {
                            answers.add(qna);
                        } else {
                            log.info("User '" + username + "' is not authorized (permission status: '" + ps + "') to access answer '" + answerUUIDs[i] + "'.");
                            qna.setAnswer("INFO: Answer protected: " + ps);
                            answers.add(qna);
                        }
                    } else {
                        log.info("Qna '" + qna.getUuid() + "' of domain '" + qna.getDomainid() + "' is not trained yet.");
                    }
                } catch(Exception e) {
                    log.error("Something is wrong with QnA '" + domain.getId()  + " / " + answerUUIDs[i] + "': " + e.getMessage());
                }
            }
        } else {
            String errMsg = "No such directory '" + questionsAnswersDir.getAbsolutePath() + "'!";
            log.error(errMsg);
        }

        // TODO: Scalability / Performance
        Collections.sort(answers, Answer.DateComparator);

        if (offset >= 0 && limit >= 0 && offset < answers.size() && offset + limit < answers.size()) {
            log.warn("TODO: Improve pagination (limit '" + limit + "' and offset '" + offset + "')!");
            List<Answer> pAnswers = new ArrayList<Answer>();
            for (int i = offset; i < offset + limit; i++) {
                pAnswers.add(answers.get(i));
            }
            return pAnswers.toArray(new Answer[0]);
        }

        return answers.toArray(new Answer[0]);
    }
}
