package com.wyona.katie.services;

import com.wyona.katie.connectors.*;
import com.wyona.katie.handlers.*;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.*;

import freemarker.template.Template;

/**
 *
 */
@Slf4j
@Component
public class QuestionAnsweringService {

    @Autowired
    private ResourceBundleMessageSource messageSource;

    @Autowired
    private BackgroundProcessService backgroundProcessService;

    @Autowired
    private MailerService mailerService;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private DynamicExpressionEvaluationService dynamicExprEvalService;

    @Autowired
    private IAMService iamService;

    @Autowired
    private AIService aiService;

    @Autowired
    private NamedEntityRecognitionService nerService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private DataRepositoryService dataRepoService;

    @Autowired
    private XMLService xmlService;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private AnswerFromTextServiceMockImpl answerFromTextMockImpl;

    @Autowired
    private AnswerFromTextServiceRestImpl answerFromTextRestImpl;

    @Autowired
    private SentenceBERTQuestionAnswerImpl sbertImpl;

    @Autowired
    private CohereReRank cohereReRank;
    @Autowired
    private SentenceBERTreRank sentenceBERTreRank;
    @Autowired
    private LLMReRank llmReRank;

    @Autowired
    private LuceneVectorSearchHumanFeedbackImpl luceneVectorSearchHumanFeedbackImpl;

    @Autowired
    private MockConnector mockConnector;
    @Autowired
    private SharepointConnector sharepointConnector;
    @Autowired
    private OneNoteConnector oneNoteConnector;
    @Autowired
    private WeaviateSiteSearchConnector weaviateSiteSearchConnector;
    @Autowired
    private WeaviateWikipediaSearchConnector weaviateWikipediaSearchConnector;
    @Autowired
    private ThirdPartyRAGConnector thirdPartyRAGConnector;
    @Autowired
    private DirectusConnector directusConnector;
    @Autowired
    private ConfluenceConnector confluenceConnector;
    @Autowired
    private CohereGroundedQAConnector cohereGroundedQAConnector;
    @Autowired
    private WebsiteConnector websiteConnector;
    @Autowired
    private EnerGISConnector enerGISConnector;

    @Autowired
    private OpenAIGenerate openAIGenerate;
    @Autowired
    private AlephAlphaGenerate alephAlphaGenerate;
    @Autowired
    private MistralAIGenerate mistralAIGenerate;
    @Autowired
    private OllamaGenerate ollamaGenerate;

    @Value("${aft.implementation}")
    private String aftImpl;

    @Value("${mistral.ai.completion.model}")
    private String mistralAIModel;

    @Value("${openai.generate.model}")
    private String openAIModel;

    @Value("${ollama.completion.model}")
    private String ollamaModel;

    private static final String ANONYMOUS = "anonymous";

    /**
     * Get answers to a question
     * @param question Question asked by user, e.g. "What is a moderator?"
     * @param classifications Classification of question, e.g. "gravel bike", "bug", "instruction", "fact", "social", ...
     * @param messageId Message Id sent by client together with question
     * @param domain Domain the question/answer is associated with
     * @param dateSubmitted Date when question was asked
     * @param remoteAddress Remote address, e.g. "178.197.227.93"
     * @param channelType Channel type (e.g. Slack, MS Teams, Discord, ...), in order to decide what to do when moderation is enabled
     * @param channelRequestId Channel request Id, which is unique within channel (e.g. REST, SLACK, MS_TEAMS, DISCORD, EMAIL...), e.g. "3139c14f-ae63-4fc4-abe2-adceb67988af". Each channel has different input parameters, which are saved inside a channel specific table. The channelRequestId allows to retrieve these channel specific input parameters.
     * @param limit Limit number of returned answers, whereas if limit is set to -1, then return all answers
     * @param offset From where to start returning answers, whereas if offset is set to -1, then return from very first answer, which means offset == 0
     * @param checkAuthorization True when authorization must be checked
     * @param requestedAnswerContentType Content type of answer accepted by client, e.g. "text/plain" resp. ContentType.TEXT_PLAIN
     * @param includeFeedbackLinks When true, then include feedback links at the end of answer (thumb up, thumb down)
     * @return list of possible answers to question
     */
    public List<ResponseAnswer> getAnswers(String question, List<String> classifications, String messageId, Context domain, Date dateSubmitted, String remoteAddress, ChannelType channelType, String channelRequestId, int limit, int offset, boolean checkAuthorization, ContentType requestedAnswerContentType, boolean includeFeedbackLinks) throws Exception {
        if (checkAuthorization && domain.getAnswersGenerallyProtected() && !contextService.isMemberOrAdmin(domain.getId())) {
            String msg = "Answers of domain '" + domain.getId() + "' are generally protected and user has neither role " + Role.ADMIN + ", nor is member of domain '" + domain.getId() + "'.";
            log.info(msg);
            throw new java.nio.file.AccessDeniedException(msg);
        }

        log.info("Try to answer question '" + question + "' ...");

        String usernameKatie = authService.getUsername();

        // TODO: Log question and update further down log entry using log entry UUID
        //String logEntryUUID = dataRepoService.logQuestion(question, remoteAddress, dateSubmitted, domain.getId(), username, null, null);

        Sentence analyzedQuestion = nerService.analyze(utilsService.preProcessQuestion(question), classifications, domain);

        List<Hit> hits = getHits(domain, analyzedQuestion, limit);

        List<ResponseAnswer> responseAnswers = new ArrayList<ResponseAnswer>();
        if (hits != null && hits.size() > 0) {
            if (hits.size() < limit) {
                limit = hits.size();
            }
            if (limit < 0) { // INFO: No limit set
                limit = hits.size(); // INFO: Process and return all answers
            }

            if (offset < 0) { // INFO: No offset set
                offset = 0;
            }

            log.info("Inspect/process " + limit + " answer(s) ...");

            for (int i  = offset; i < limit; i++) {
                Hit hit = hits.get(i);
                Answer answer = hit.getAnswer();
                if (answer.getAnswer() != null) {
                    ResponseAnswer ra = inspectAnswer(answer, domain, usernameKatie, dateSubmitted, analyzedQuestion);
                    if (ra != null) {
                        if (ra.getType() != null && ra.getType().equals(QnAType.BOOKMARK_URL)) {
                            // INFO: If we index the individual paragraphs contained by the URL, then we do not have to get the answer from the content of the URL
                            log.info("TODO: Consider generating answer from individual paragraph");
                            if (true) { // TODO
                                ra.setAnswer("<div>" + ra.getAnswer() + "<p>Source: <a href=\"" + ra.getUrl() + "\">" + ra.getUrl() + "</a></p></div>");
                            } else {
                                // TODO: Consider generating answer from individual paragraph
                                ra.setAnswer("<div><p>" + generateAnswerFromContentReferencedByURLs(ra.getUuid(), ra.getUrl(), question, domain) + "</p><p>Source: " + ra.getUrl() + "</p></div>");
                            }
                        }
                        ra.setScore(hit.getScore());
                        ra.setRating(hit.getRating());
                        responseAnswers.add(ra);
                    } else {
                        log.warn("Answer with UUID '" + answer.getUuid() + "' does not exist! Maybe index needs to be cleaned ...");
                    }
                } else {
                    log.warn("Answer is null!");
                }
            }
        } else {
            StringBuilder logMsg = new StringBuilder("No answer available for question '" + question + "'");
            if (classifications != null && classifications.size() > 0) {
                logMsg.append(" and classifications");
                for (String classification : classifications) {
                    logMsg.append(" '" + classification + "'");
                }
            }
            log.info(logMsg.toString());
        }

        // INFO: Get UUID and permission status of top answer to log this information
        String uuidTopAnswer = null;
        String answerTopAnswer = null;
        double scoreTopAnswer = -1;
        PermissionStatus permissionStatusFirstAnswer = PermissionStatus.UNKNOWN;
        if (responseAnswers.size() > 0) {
            ResponseAnswer topAnswer = (ResponseAnswer)responseAnswers.get(0);
            uuidTopAnswer = topAnswer.getUuid();
            answerTopAnswer = topAnswer.getAnswer();
            if (uuidTopAnswer != null) {
                log.info("QnA UUID of top answer: " + uuidTopAnswer);
            } else {
                log.info("Top answer is not based on QnA.");
            }
            scoreTopAnswer = topAnswer.getScore();
            permissionStatusFirstAnswer = topAnswer.getPermissionStatus();
        } else {
            log.info("No answers found by search implementation.");
        }

        if (domain.getScoreThreshold() != null && scoreTopAnswer < domain.getScoreThreshold()) {
            log.warn("Do not return any answers, because score of top answer '" + scoreTopAnswer + "' is below score threshold '" + domain.getScoreThreshold() + "'!");
            responseAnswers.clear();
        }

        String moderationStatus = getModerationStatus(domain, question, channelType, responseAnswers.size());
        // TODO: Log when score of top answer was below score threshold
        String logEntryUUID = dataRepoService.logQuestion(question, classifications, messageId, remoteAddress, dateSubmitted, domain, usernameKatie, uuidTopAnswer, answerTopAnswer, scoreTopAnswer, domain.getScoreThreshold(), permissionStatusFirstAnswer, moderationStatus, channelType, channelRequestId, offset);
        //log.debug("Link to approve answer: " + getApproveAnswerLink(domain, logEntryUUID));

        for (ResponseAnswer ra: responseAnswers) {
            ra.setQuestionUUID(logEntryUUID);

            if (true) { // TODO: Make configurable per request, similar to "requestedAnswerContentType"
                // INFO: Answers from external sources  do not have a UUID (e.g. when using query service, e.g. https://connector-grounded-qa.ukatie.com/api/v2)
                if (ra.getUuid() != null && contextService.existsDataObject(ra.getUuid(), domain)) {
                    DataObjectMetaInformation dataObjectMeta = contextService.getDataObjectMetaInformation(ra.getUuid(), domain);
                    log.info("Content type of data object: " + dataObjectMeta.getContentType());
                    if (ContentType.fromString(dataObjectMeta.getContentType()).equals(ContentType.APPLICATION_JSON)) {
                        ra.setData(contextService.getDataObjectAsJson(ra.getUuid(), domain));
                    }
                }
            }

            if (includeFeedbackLinks) {
                String userLanguage = "en"; // TODO
                // TODO: Use i18n for Yes and No ...
                if (ra.getAnswerContentType().equals(ContentType.TEXT_PLAIN.toString())) {
                    ra.setAnswer(ra.getAnswer() + "\n\n---\n\n" + messageSource.getMessage("answer.helpful", null, new Locale(userLanguage)) + "\n\nYes: " + answerHelpfulLink(domain, logEntryUUID) + "\n\nNo: " + answerNotHelpfulLink(domain, logEntryUUID));
                } else if (ra.getAnswerContentType().equals(ContentType.TEXT_HTML.toString())) {
                    ra.setAnswer(ra.getAnswer() + "<p>" + messageSource.getMessage("answer.helpful", null, new Locale(userLanguage)) + "</p><p>Yes: <a href=\"" + answerHelpfulLink(domain, logEntryUUID) + "\">" + answerHelpfulLink(domain, logEntryUUID) + "</a></p><p>No: <a href=\"" + answerNotHelpfulLink(domain, logEntryUUID) + "\">" + answerNotHelpfulLink(domain, logEntryUUID) + "</a></p>");
                } else {
                    log.warn("Include feedback links not supported for content type '" + ra.getAnswerContentType() + "'.");
                }
            }

            if (requestedAnswerContentType != null) {
                if (ra.getAnswerContentType() != null) {
                    if (ra.getAnswer() != null) {
                        if (requestedAnswerContentType.equals(ContentType.TEXT_PLAIN) && ra.getAnswerContentType().equals(ContentType.TEXT_HTML.toString())) {
                            ra.setAnswer(Utils.convertHtmlToPlainText(ra.getAnswer()));
                            ra.setAnswerContentType(ContentType.TEXT_PLAIN); // WARN: This is overriding the content type of the answer, so if the answer is stored as JSON, then it will not be returned as JSON (see field "answerAsJson" or method getAnswerAsJson)
                        } else if (requestedAnswerContentType.equals(ContentType.TEXT_TOPDESK_HTML) && ra.getAnswerContentType().equals(ContentType.TEXT_HTML.toString())) {
                            ra.setAnswer(Utils.convertHtmlToTOPdeskHtml(ra.getAnswer()));
                            ra.setAnswerContentType(ContentType.TEXT_HTML); // WARN: This is overriding the content type of the answer, so if the answer is stored as JSON, then it will not be returned as JSON (see field "answerAsJson" or method getAnswerAsJson)
                        }
                    } else {
                        log.error("Answer with UUID '" + ra.getUuid() + "' is null and therefore cannot be converted!");
                    }
                } else {
                    log.warn("Answer content type of response answer '" + ra.getUuid() + "' is not defined!");
                }
            }
        }

        if (domain.getAnswersMustBeApprovedByModerator()) {
            String usernameChannel = usernameKatie; // TODO: Get username of user inside channel
            notifyModerators(domain, question, responseAnswers.size(), channelType, logEntryUUID, usernameChannel);
        }

        return responseAnswers;
    }

    /**
     *
     */
    private void notifyModerators(Context domain, String question, int numberOfAnswers, ChannelType channelType, String logEntryUUID, String usernameChannel) {
        if (numberOfAnswers > 0) {
            if (!channelType.equals(ChannelType.UNDEFINED)) {
                log.info("Moderation approval enabled for domain '" + domain.getName() + "' (Id: " + domain.getId() + "), moderator(s) will be notified ...");
                notifyModeratorsByEmail(question, usernameChannel, channelType, logEntryUUID, domain, true);
            } else {
                log.info("Answers for domain '" + domain.getId() + "' must be approved by moderator, but question '" + question + "' (Log entry UUID: " + logEntryUUID + ") was received by undefined channel '" + channelType + "', therefore moderator(s) will NOT be notified.");
            }
        } else {
            if (!channelType.equals(ChannelType.UNDEFINED)) {
                log.info("Moderation approval enabled for domain '" + domain.getName() + "' (Id: " + domain.getId() + ") and no answers found to question '" + question + "', therefore moderator(s) will be notified ...");
                notifyModeratorsByEmail(question, usernameChannel, channelType, logEntryUUID, domain, false);
            } else {
                log.info("Moderation approval enabled for domain '" + domain.getName() + "' (Id: " + domain.getId() + ") and no answers found to question '" + question + "', but question '" + question + "' was received by undefined channel '" + channelType + "', therefore moderator(s) will NOT be notified ...");
            }
        }
    }

    /**
     *
     */
    private String getModerationStatus(Context domain, String question, ChannelType channelType, int numberOfAnswers) {
        String moderationStatus = null;
        if (domain.getAnswersMustBeApprovedByModerator()) {
            if (numberOfAnswers > 0) {
                if (!channelType.equals(ChannelType.UNDEFINED)) {
                    moderationStatus = ModerationStatus.NEEDS_APPROVAL;
                    log.info("Answers for domain '" + domain.getId() + "' must be approved by moderator for channel type '" + channelType + "' ...");

                } else {
                    log.info("Answers for domain '" + domain.getId() + "' must be approved by moderator, but channel type is undefined, therefore skip moderation.");
                }
            } else {
                log.info("Moderation approval enabled for domain '" + domain.getName() + "' (Id: " + domain.getId() + "), but no answers found to question '" + question + "', therefore moderation status will not be set.");
            }
        }
        return moderationStatus;
    }

    /**
     *
     */
    private String answerHelpfulLink(Context domain, String questionUUID) {
        int rating = 10;
        return domain.getHost() + "/#/domain/" + domain.getId() + "/asked-questions/" + questionUUID + "/rate-answer?rating=" + rating;
    }

    /**
     *
     */
    private String answerNotHelpfulLink(Context domain, String questionUUID) {
        int rating = 0;
        return domain.getHost() + "/#/domain/" +domain.getId() + "/asked-questions/" + questionUUID + "/rate-answer?rating=" + rating;
    }

    /**
     * @param limit TODO
     */
    // TODO: Why is there no offset parameter?!
    private List<Hit> getHits(Context domain, Sentence analyzedQuestion, int limit) throws Exception {
        List<Hit> hits = new ArrayList<Hit>();

        // INFO: Get answers from the knowledge base managed by Katie
        if (domain.getKatieSearchEnabled()) {
            log.info("Search Katie, whereas number of returned hits is limited to " + limit + " ...");
            Hit[] knowledgeBaseHits = aiService.findAnswers(analyzedQuestion, domain, limit);
            for (Hit hit : knowledgeBaseHits) {
                // INFO: Some search answers implementations might not set the submitted question
                hit.getAnswer().setSubmittedQuestion(analyzedQuestion.getSentence());

                hits.add(hit);
            }
        } else {
            log.info("Katie search disabled.");
        }

        // INFO: Get answers from thirdy party data sources, e.g. Sharepoint or Confluence
        Hit[] thirdPartyHits = getThirdPartyAnswers(analyzedQuestion, domain.getId(), limit);
        if (thirdPartyHits.length > 0) {
            log.info("TODO: Merge results sets, for example based on normalized confidence score or RFF (Reciprocal Rank Fusion) or use re-ranking algorithm");
            // https://medium.com/@sowmiyajaganathan/hybrid-search-with-re-ranking-ff120c8a426d
            // https://medium.com/@sowmiyajaganathan/hybrid-search-with-re-ranking-ff120c8a426d
            for (Hit hit : thirdPartyHits) {
                hits.add(hit);
            }
        } else {
            log.info("No third-party hits.");
        }

        // TODO: Implement "Know what you don't know" (Epistemic Neural Networks, or https://huggingface.co/roberta-large-mnli?text=I+like+you.+I+hate+you.)

        // INFO: See for example https://weaviate.io/blog/ranking-models-for-better-search
        if (domain.getReRankAnswers()) {
            if (hits.size() > 0) {
                hits = reRankAnswers(analyzedQuestion, domain, hits, limit);
            } else {
                log.info("No answers available, therefore nothing to re-rank.");
            }
        } else {
            log.info("Re-ranking disabled.");
        }

        if (domain.getConsiderHumanFeedback()) {
            hits = correctByConsideringPreviousHumanFeedback(analyzedQuestion, domain, hits);
        } else {
            log.info("Consider human feedback disabled.");
        }

        if (domain.getGenerateCompleteAnswers() && !domain.getCompletionImpl().equals(CompletionImpl.UNSET)) {
            hits = generateAnswer(analyzedQuestion, domain, hits);
        } else {
            log.info("Complete answer disabled.");
        }

        return hits;
    }

    /**
     * Use Generative AI to generate / complete answer based on retrieved hits
     */
    private List<Hit> generateAnswer(Sentence question, Context domain, List<Hit> hits) {
        GenerateProvider generateProvider = null;
        String model = null;
        if (domain.getCompletionImpl().equals(CompletionImpl.ALEPH_ALPHA)) {
            generateProvider = alephAlphaGenerate;
            model = "luminous-base"; // TODO: Make configurable
        } else if (domain.getCompletionImpl().equals(CompletionImpl.OPENAI)) {
            generateProvider = openAIGenerate;
            model = openAIModel;
        } else if (domain.getCompletionImpl().equals(CompletionImpl.MISTRAL_AI)) {
            generateProvider = mistralAIGenerate;
            model = mistralAIModel;
        } else if (domain.getCompletionImpl().equals(CompletionImpl.MISTRAL_OLLAMA)) {
            generateProvider = ollamaGenerate;
            model = ollamaModel;
        } else {
            log.error("No such completion implemention supported yet: " + domain.getCompletionImpl());
            return hits;
        }

        try {
            if (hits.size() > 0) {
                Answer qna = getTextAnswerV2(hits.get(0).getAnswer().getAnswer(), domain);
                String prompt = getPrompt(question, qna.getAnswer(), qna.getUrl());

                // TODO: Domain specific API token, similar to domain.getEmbeddingsApiToken();
                String apiToken = contextService.getApiToken(domain.getCompletionImpl());
                log.warn("Send prompt '" + prompt + "' to " + model + " ...");
                Double temperature = null;
                String completedText = generateProvider.getCompletion(prompt, model, temperature, apiToken);

                log.info("Completed text: " + completedText);

                StringBuilder newAnswer = new StringBuilder();
                newAnswer.append("<p>" + completedText + "</p>");
                newAnswer.append("<p>Generated by: " + model + "</p>");
                if (false) { // TODO: Make configurable
                    newAnswer.append("<p>Prompt: " + prompt + "</p>");
                }

                hits.get(0).getAnswer().setAnswer(newAnswer.toString());

                if (qna!= null && qna.getUrl() != null) {
                    hits.get(0).getAnswer().setUrl(qna.getUrl());
                }
            } else {
                log.info("No hits available.");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return hits;
    }

    /**
     *
     */
    private String getPrompt(Sentence question, String answer, String url) throws Exception {
        // TODO: Get parametrized prompt from domain configuration
        if (url != null) {
            return "Bitte beantworte die folgende Frage \"" + question.getSentence() + "\" basierend auf dem Inhalt der folgenden Webseite " + url + " und dem folgenden Text Abschnitt daraus \"" + answer + "\"";
        } else {
            return "Bitte beantworte die folgende Frage \"" + question.getSentence() + "\" basierend auf dem folgenden Text Abschnitt \"" + answer + "\"";
        }
    }

    /**
     * Correct / adjust found answers by considering previous human feedback
     * @param question Asked question
     * @param domain Domain associated with question
     * @param answers Found answers
     * @return annotated hits / answers based on previous human feedback
     */
    private List<Hit> correctByConsideringPreviousHumanFeedback(Sentence question, Context domain, List<Hit> answers) {
        Rating[] ratings = getHumanFeedback(question.getSentence(), domain);

        List<Hit> filteredAnswers = new ArrayList<Hit>();
        for (Hit hit: answers) {
            for (Rating rating: ratings) {
                if (hit.getAnswer().getUuid().equals(rating.getQnauuid())) {
                    log.info("Question '" + question + "' was asked before and the accuracy of found answer of QnA '" + rating.getQnauuid() + "' was rated as '" + rating.getRating() + "'");
                    hit.setRating(rating.getRating());
                }
            }
            if (hit.getRating() == 0) {
                log.info("Ignore answer '" + hit.getAnswer().getUuid() + "' and all other answers.");
                return filteredAnswers;
            } else {
                filteredAnswers.add(hit);
            }
        }

        return filteredAnswers;
    }

    /**
     * @param question Asked question
     * @param domain Domain associated with question
     */
    private Rating[] getHumanFeedback(String question, Context domain) {
        HumanFeedbackHandler humanFeedbackImpl = getHumanFeedbackImpl();
        try {
            return humanFeedbackImpl.getHumanFeedback(question, domain);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new Rating[0];
        }
    }

    /**
     * Get implementation to handle human feedback
     */
    private HumanFeedbackHandler getHumanFeedbackImpl() {
        return luceneVectorSearchHumanFeedbackImpl;
    }

    /**
     * Re-rank answers
     * @param question Asked question
     * @param domain Domain associated with question
     * @param answers Found answers
     * @param limit Limit of returned hits, whereas no limit when set to -1
     * @return re-ranked answers
     */
    private List<Hit> reRankAnswers(Sentence question, Context domain, List<Hit> answers, int limit) throws Exception {
        log.info("Re-rank " + answers.size() + " answers ...");

        List<String> _answers = new ArrayList<String>();
        for (Hit answer: answers) {
            _answers.add(getTextAnswer(answer.getAnswer().getAnswer(), domain));
        }

        ReRankProvider reRankImpl = null;
        if (domain.getReRankImpl().equals(ReRankImpl.SBERT)) {
            reRankImpl = sentenceBERTreRank;
        } else if (domain.getReRankImpl().equals(ReRankImpl.COHERE)) {
            reRankImpl = cohereReRank;
        } else if (domain.getReRankImpl().equals(ReRankImpl.LLM)) {
            reRankImpl = llmReRank;
        } else {
            log.error("Re-rank implementation not set!");
            return answers;
        }

        Integer[] reRankedIndex = reRankImpl.getReRankedAnswers(question.getSentence(), _answers.toArray(new String[0]), limit);

        List<Hit> reRankedAnswers = new ArrayList<Hit>();
        for (int i:reRankedIndex) {
            reRankedAnswers.add(answers.get(i));
        }

        return reRankedAnswers;
    }

    /**
     * @param answer Either UUID, e.g. "ak-uuid:b0bc5269-ce62-4bd6-848d-23bc1cbd74d9" or actual answer, e.g. "Bern is the capital of Switzerland"
     */
    private String getTextAnswer(String answer, Context domain) throws Exception {
        // TODO: Use / Merge getTextAnswerV2(String, Context)
        if (answer.startsWith(Answer.AK_UUID_COLON)) {
            String uuid = Answer.removePrefix(answer);
            log.info("Get answer with UUID '" + uuid + "' ...");
            Answer _answer = xmlService.parseQuestionAnswer(null, false, domain, uuid, null);

            // INFO: Concatenate question and answer to provide more context
            //answer = _answer.getAnswer();
            answer = _answer.getOriginalquestion() + " " + _answer.getAnswer();
        } else {
            log.info("Answer from AI service already contains actual answer.");
        }

        return Utils.stripHTML(answer, false, false);
    }

    /**
     * @param answer Either UUID, e.g. "ak-uuid:b0bc5269-ce62-4bd6-848d-23bc1cbd74d9" or actual answer, e.g. "Bern is the capital of Switzerland"
     */
    private Answer getTextAnswerV2(String answer, Context domain) throws Exception {
        if (answer.startsWith(Answer.AK_UUID_COLON)) {
            String uuid = Answer.removePrefix(answer);
            log.info("Get answer with UUID '" + uuid + "' ...");
            Answer qna = xmlService.parseQuestionAnswer(null, false, domain, uuid, null);

            qna.setAnswer(Utils.stripHTML(qna.getAnswer(), true, true));
            return qna;
        } else {
            log.info("Answer from AI service already contains actual answer.");
            return null; // TODO: Init Answer object
        }
    }

    /**
     * Get answers from third party knowledge sources, e.g. sharepoint or confluence
     */
    private Hit[] getThirdPartyAnswers(Sentence question, String domainId, int limit) throws Exception {
        List<Hit> hits = new ArrayList<Hit>();

        KnowledgeSourceMeta[] knowledgeSourceMetas = contextService.getKnowledgeSources(domainId, false);
        if (knowledgeSourceMetas != null) {
            for (KnowledgeSourceMeta knowledgeSourceMeta : knowledgeSourceMetas) {
                if (knowledgeSourceMeta.getIsEnabled()) {
                    log.info("Get answers using connector '" + knowledgeSourceMeta.getConnector() + "' ...");
                    Connector connector = getConnector(knowledgeSourceMeta.getConnector());
                    if (connector != null) {
                        Hit[] connectorHits = connector.getAnswers(question, limit, knowledgeSourceMeta);
                        for (Hit hit : connectorHits) {
                            // TODO: Set knowledge source name
                            //knowledgeSourceMeta.getName();
                            hits.add(hit);
                        }
                    } else {
                        log.error("No such connector implementation '" + knowledgeSourceMeta.getConnector() + "'!");
                    }
                } else {
                    log.debug("Knowledge source '" + knowledgeSourceMeta.getName() + "' disabled.");
                }
            }
        }

        return hits.toArray(new Hit[0]);
    }

    /**
     *
     */
    private Connector getConnector(KnowledgeSourceConnector knowledgeSourceConnector) {
        if (knowledgeSourceConnector.equals(KnowledgeSourceConnector.MOCK)) {
            return mockConnector;
        } else if (knowledgeSourceConnector.equals(KnowledgeSourceConnector.SHAREPOINT)) {
            return sharepointConnector;
        } else if (knowledgeSourceConnector.equals(KnowledgeSourceConnector.ONENOTE)) {
            return oneNoteConnector;
        } else if (knowledgeSourceConnector.equals(KnowledgeSourceConnector.WEAVIATE_SITE_SEARCH)) {
            return weaviateSiteSearchConnector;
        } else if (knowledgeSourceConnector.equals(KnowledgeSourceConnector.WEAVIATE_WIKIPEDIA_SEARCH)) {
            return weaviateWikipediaSearchConnector;
        } else if (knowledgeSourceConnector.equals(KnowledgeSourceConnector.THIRD_PARTY_RAG)) {
            return thirdPartyRAGConnector;
        } else if (knowledgeSourceConnector.equals(KnowledgeSourceConnector.DIRECTUS)) {
            return directusConnector;
        } else if (knowledgeSourceConnector.equals(KnowledgeSourceConnector.CONFLUENCE)) {
            return confluenceConnector;
        } else if (knowledgeSourceConnector.equals(KnowledgeSourceConnector.GROUNDED_QA)) {
            return cohereGroundedQAConnector;
        } else if (knowledgeSourceConnector.equals(KnowledgeSourceConnector.WEBSITE)) {
            return websiteConnector;
        } else if (knowledgeSourceConnector.equals(KnowledgeSourceConnector.ENERGIS)) {
            return enerGISConnector;
        } else {
            return null;
        }
    }

    /**
     * Get answer from content referenced by URL (See for example https://txt.cohere.ai/building-a-search-based-discord-bot-with-language-models/)
     * @param question Question asked by user, e.g. "How long is the coastline of Brazil?"
     * @return answer to question
     */
    private String generateAnswerFromContentReferencedByURLs(String uuid, String url, String question, Context domain) {
        log.info("TODO: Get answer from content referenced by URL '" + url + "' ...!");

        // TODO: Select implementation based on Domain configuration
        AnswerFromTextService answerFromText = answerFromTextMockImpl;
        if (aftImpl.equals("REST")) {
            answerFromText = answerFromTextRestImpl;
        }

        String answer = answerFromText.getAnswerFromText(question, getTextOfWebPage(url, domain));

        return answer;
    }

    /**
     * Get text referenced by a particular URL
     * @param url URL of web page
     * @param domain Domain associated with QnA
     */
    private String getTextOfWebPage(String url, Context domain) {
        String text = contextService.getDumpedContent(url, domain);
        if (text != null) {
            return text;
        } else {
            log.warn("URL content not cached, must retrieve it first ...");
            // TODO: Get content referenced by URL and scrape content using for example Apache Tika. Also see https://learn.microsoft.com/en-us/azure/cognitive-services/language-service/question-answering/reference/document-format-guidelines
            // See ContextService#download(...)
            return "TODO: Get content from URL '" + url + "', in order to extract relevant section!";
        }
    }

    /**
     * Ask questions of all QnAs contained by domain and check whether the correct answers are replied
     * @return test report containing accuracy and additional benchmark information
     */
    public TestReport getAccuracyTruePositives(String domainId) throws Exception {
        log.info("Ask questions in order to get accuracy of true positives ...");
        Context domain = contextService.getContext(domainId);

        TestReport report = initTestReport(domain);

        Answer[] qnas = contextService.getTrainedQnAs(domain, -1, -1);
        if (qnas.length > 0) {
            log.info("Domain '" + domainId + "' has " + qnas.length + " QnA(s).");
            for (Answer qna : qnas) {
                String originalQuestion = qna.getOriginalquestion();
                if (originalQuestion != null) {
                    log.info("Ask original question: " + originalQuestion);
                    report.addResult(testQuestionTruePositive(originalQuestion, qna.getUuid(), domain));
                } else {
                    log.info("QnA '" + qna.getUuid() + "' has no original question.");
                }

                String[] alternativeQuestions = qna.getAlternativequestions();
                if (alternativeQuestions != null && alternativeQuestions.length > 0) {
                    for (String altQuestion : alternativeQuestions) {
                        log.info("Ask alternative question: " + altQuestion);
                        report.addResult(testQuestionTruePositive(altQuestion, qna.getUuid(), domain));
                    }
                } else {
                    log.info("QnA '" + qna.getUuid() + "' has no alternative questions.");
                }
            }
        } else {
            log.info("Domain '" + domainId + "' does not have any QnAs.");
        }

        return report;
    }

    /**
     * TODO
     * @param questions Questions which do not have an answer inside domain
     * @return test report containing accuracy and additional benchmark information
     */
    public TestReport getAccuracyTrueNegatives(String domainId, String[] questions) throws Exception {
        log.info("Ask questions ...");

        Context domain = contextService.getContext(domainId);

        TestReport report = initTestReport(domain);

        if (questions != null && questions.length > 0) {
            for (String question : questions) {
                log.info("Ask question: " + question);
                report.addResult(testQuestionTrueNegative(question, domain));
            }
        }

        return report;
    }

    /**
     *
     */
    private TestReport initTestReport(Context domain) {
        TestReport report = new TestReport(domain.getId(), domain.getDetectDuplicatedQuestionImpl());

        if (domain.getDetectDuplicatedQuestionImpl().equals(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH)) {
            report.setEmbeddingsImpl(domain.getEmbeddingsImpl());
            if (domain.getEmbeddingsImpl().equals(EmbeddingsImpl.SBERT)) {
                report.setEmbeddingsImplModel(sbertImpl.getVersionAndModel().get(sbertImpl.MODEL));
            } else {
                report.setEmbeddingsImplModel(domain.getEmbeddingsModel());
            }
            report.setLuceneVectorSearchMetric("" + domain.getVectorSimilarityMetric());
        }

        if (domain.getDetectDuplicatedQuestionImpl().equals(DetectDuplicatedQuestionImpl.SENTENCE_BERT)) {
            report.setSentenceBERTDistanceThreshold(domain.getSentenceBERTDistanceThreshold());
            report.setEmbeddingsImplModel(sbertImpl.getVersionAndModel().get(sbertImpl.MODEL));
        }
        
        return report;
    }

    /**
     * Generate Accuracy, Precision and Recall Benchmark
     * @param questions Questions and associated relevant UUIDs to measure precision and recall
     * @return precision and recall benchmark
     */
    public BenchmarkPrecision getAccuracyAndPrecisionAndRecallBenchmark(String domainId, BenchmarkQuestion[] questions, int throttleTimeInMillis, String processId) throws Exception {
        String msg = "Ask questions in order to measure accuracy, precision and recall ...";
        log.info(msg);
        backgroundProcessService.updateProcessStatus(processId, msg);

        Context domain = contextService.getContext(domainId);

        BenchmarkPrecision benchmark = new BenchmarkPrecision(domain.getId(), domain.getDetectDuplicatedQuestionImpl());
        if (domain.getDetectDuplicatedQuestionImpl().equals(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH)) {
            benchmark.setEmbeddingsImpl(domain.getEmbeddingsImpl());
        }

        int counter = 0;
        final int BATCH_SIZE = 100;
        for (BenchmarkQuestion question : questions) {
            log.info("Ask question: " + question.getQuestion());
            benchmark.addResult(getAccuracyAndPrecisionAndRecall(question.getQuestion(), question.getRelevantUuids(), domain, throttleTimeInMillis));
            counter++;

            if (counter % BATCH_SIZE == 0) {
                backgroundProcessService.updateProcessStatus(processId, counter + " questions asked ...");
            }
        }
        backgroundProcessService.updateProcessStatus(processId, "Benchmark completed, " + counter + " questions asked in total.");

        return benchmark;
    }

    /**
     * Test question and whether correct answer (of QnA) is found
     * @param question Question
     * @paramm uuid UUID of QnA which is supposed to match
     * @param domain Domain associated with question and QnAs
     */
    private TestResult testQuestionTruePositive(String question, String uuid, Context domain) {
        try {
            List<String> classifications = new ArrayList<String>();
            Sentence analyzedQuestion = nerService.analyze(utilsService.preProcessQuestion(question), classifications, domain);

            int limit = -1; // TODO
            Hit[] answers = getHits(domain, analyzedQuestion, limit).toArray(new Hit[0]);

            if (answers.length > 0) {
                log.info(answers.length + " answer(s) found");
                log.info("Check whether top answer matches ...");
                Answer topAnswer = answers[0].getAnswer();
                if (uuid.equals(topAnswer.getUuid())) {
                    log.info("Top answer matches :-)");
                    return new TestResult(question, true, uuid, uuid, answers[0].getScore());
                } else {
                    log.warn("Top answer does not match!");
                    return new TestResult(question, false, uuid, topAnswer.getUuid(), answers[0].getScore());
                }
            } else {
                log.warn("No answer available for question '" + question + "'!");
                return new TestResult(question, false, uuid, null, -1);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new TestResult(question, false, uuid, null, -1);
        }
    }

    /**
     * Test question and check whether no answer available
     * @param question Question which should have no answer within domain
     * @param domain Domain associated with question and QnAs
     */
    private TestResult testQuestionTrueNegative(String question, Context domain) {
        try {
            List<String> classifications = new ArrayList<String>();
            Sentence analyzedQuestion = nerService.analyze(utilsService.preProcessQuestion(question), classifications, domain);

            int limit = -1; // TODO
            Hit[] answers = getHits(domain, analyzedQuestion, limit).toArray(new Hit[0]);

            log.info("Check whether answer found ...");
            if (answers.length > 0) {
                log.info(answers.length + " answer(s) found");
                Answer topAnswer = answers[0].getAnswer();
                // TODO: What about using a threshold to compare the score with?!
                return new TestResult(question, false, null, topAnswer.getUuid(), answers[0].getScore());
            } else {
                log.warn("No answer available");
                return new TestResult(question, true, null, null, -1);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new TestResult(question, false, null, null, -1);
        }
    }

    /**
     * Accuracy and Precision (Number of retrieved correct resp. relevant answers divided by total number of retrieved answers) and Recall (Number of retrieved correct resp. relevant answers divided by total number of relevant answers)
     * @param question Question
     * @param relevantUuids UUIDs of relevant answers to question
     * @param domain Domain associated with question and QnAs
     * @param throttleTimeInMillis Throttle time in milliseconds in order to avoid rate limits of third-party services (e.g. Cohere re-ranking)
     */
    private BenchmarkPrecisionResult getAccuracyAndPrecisionAndRecall(String question, String[] relevantUuids, Context domain, int throttleTimeInMillis) {

        if (throttleTimeInMillis > 0) {
            try {
                log.info("Sleep for " + throttleTimeInMillis + " milliseconds ...");
                Thread.sleep(throttleTimeInMillis);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        String expectedUuid = relevantUuids[0];

        try {
            List<String> classifications = new ArrayList<String>();

            //int limit = -1; // INFO: No limit, use implementation specific limit, whereas then precision and recall won't be comparable really
            int limit = 5; // INFO: All search implementations should have the same limit, such that precision and recall can be compared
            // INFO: Assuming that accuracy is 1 and the top answer is the only correct answer, then precision should be 1/5 = 0.2

            Sentence analyzedQuestion = nerService.analyze(utilsService.preProcessQuestion(question), classifications, domain);
            Hit[] answers = getHits(domain, analyzedQuestion, limit).toArray(new Hit[0]);

            log.info("Check whether answer(s) found ...");
            if (answers.length > 0) {
                log.info(answers.length + " answer(s) found for question '" + question + "'.");

                // INFO: Measure accuracy
                Answer topAnswer = answers[0].getAnswer();
                boolean accuracy = false;
                if (expectedUuid.equals(topAnswer.getUuid())) {
                    log.info("Top answer matches :-)");
                    accuracy = true;
                    //new TestResult(question, true, uuid, uuid, answers[0].getScore());
                } else {
                    log.warn("Top answer '" + topAnswer.getUuid() + "' does not match with expected answer '" + expectedUuid + "' for question '" + question + "'!");
                    accuracy = false;
                    //new TestResult(question, false, uuid, topAnswer.getUuid(), answers[0].getScore());
                }

                int numberRetrievedRelevantAnswers = getNumberRetrievedRelevantAnswers(answers, relevantUuids);

                // Precision (Number of retrieved correct resp. relevant answers divided by total number of retrieved answers)
                // https://en.wikipedia.org/wiki/Evaluation_measures_(information_retrieval)#Precision
                double precision = (double)numberRetrievedRelevantAnswers / (double)answers.length;

                // Recall (Number of retrieved correct resp. relevant answers divided by total number of relevant answers)
                // https://en.wikipedia.org/wiki/Evaluation_measures_(information_retrieval)#Recall
                double recall = (double)numberRetrievedRelevantAnswers / (double)relevantUuids.length;

                List<String> retrievedUuids = new ArrayList<String>();
                for (Hit answer: answers) {
                    retrievedUuids.add(answer.getAnswer().getUuid());
                }

                return new BenchmarkPrecisionResult(question, expectedUuid, accuracy, precision, recall, relevantUuids, retrievedUuids.toArray(new String[0]));
            } else {
                log.warn("No answer available");
                return new BenchmarkPrecisionResult(question, expectedUuid,false,0, 0, relevantUuids, null);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new BenchmarkPrecisionResult(question, expectedUuid,false, 0, 0, relevantUuids, null);
        }
    }

    /**
     * @param answers Retrieved answers
     * @param relevantUuids UUIDs of relevant answers
     * @return number of retrieved relevant answers
     */
    private int getNumberRetrievedRelevantAnswers(Hit[] answers, String[] relevantUuids) {
        int numberRetrievedRelevantAnswers = 0;
        for (Hit answer : answers) {
            for (String uuid : relevantUuids) {
                if (answer.getAnswer().getUuid().equals(uuid)) {
                    numberRetrievedRelevantAnswers++;
                }
            }
        }
        return numberRetrievedRelevantAnswers;
    }

    /**
     * Notify moderators when by Katie suggested answer needs approval or no answer was found to a question
     * @param username Name of user which submitted question
     * @param logEntryUUID Question log entry UUID, e.g. "4a68057e-1b17-4fe6-abf6-2f95db889e60"
     * @param answerFound True when Katie found an answer to question and false otherwise
     */
    private void notifyModeratorsByEmail(String question, String username, ChannelType channelType, String logEntryUUID, Context domain, boolean answerFound) {
        try {
            String[] emailsTo = contextService.getModeratorEmailAddresses(domain.getId());

            if (emailsTo.length == 0) {
                log.info("No moderators configured for domain '" + domain.getId() + "'.");
            }

            for (int i = 0; i < emailsTo.length; i++) {
                // TODO: Get language of moderator and send notification accordingly
                String moderatorLanguage = "en";

                if (answerFound) {
                    mailerService.send(emailsTo[i], domain.getMailSenderEmail(), "[" + domain.getMailSubjectTag() + "] Answer needs approval ...", getNotificationBody(moderatorLanguage, question, username, logEntryUUID, domain, channelType), true);
                } else {
                    mailerService.send(emailsTo[i], domain.getMailSenderEmail(), "[" + domain.getMailSubjectTag() + "] No answer to question ...", getNoAnswerNotificationBody(moderatorLanguage, question, username, logEntryUUID, domain), true);
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Generate notification text when answer needs approval
     * @param moderatorLanguage Language of moderator
     * @param question Asked question
     * @param username Name of user which submitted question
     * @param logEntryUUID Question log entry UUID, e.g. "4a68057e-1b17-4fe6-abf6-2f95db889e60"
     */
    private String getNotificationBody(String moderatorLanguage, String question, String username, String logEntryUUID, Context domain, ChannelType channelType) throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("question", question);
        if (username == null) {
            username = ANONYMOUS;
        }
        input.put("username", username);
        input.put("qna_link", getApproveAnswerLink(domain, logEntryUUID));
        //input.put("qna_link", domain.getMailBodyHost() + "/#/asked-questions/" + domain.getId() + "?id=" + logEntryUUID);
        input.put("domain_name", domain.getName());
        input.put("channel", channelType.toString());

        StringWriter writer = new StringWriter();
        Template template = mailerService.getTemplate("qna-needs-approval_email_", Language.valueOf(moderatorLanguage), domain);
        template.process(input, writer);
        return writer.toString();
    }

    /**
     * Get link to approve answer
     * @param logEntryUUID Question log entry UUID, e.g. "4a68057e-1b17-4fe6-abf6-2f95db889e60"
     */
    private String getApproveAnswerLink(Context domain, String logEntryUUID) {
        return domain.getHost() + "/#/approve-answer?qid=" + logEntryUUID;
    }

    /**
     * Generate notification text when answer needs approval, but no answer was found
     * @param username Name of user which submitted question
     */
    private String getNoAnswerNotificationBody(String moderatorLanguage, String question, String username, String logEntryUUID, Context domain) throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("question", question);
        if (username == null) {
            username = ANONYMOUS;
        }
        input.put("username", username);
        input.put("question_link", domain.getHost() + "/#/asked-questions/" + domain.getId() + "?id=" + logEntryUUID);
        input.put("domain_name", domain.getName());

        StringWriter writer = new StringWriter();
        Template template = mailerService.getTemplate("no_answer_to_question_", Language.valueOf(moderatorLanguage), domain);
        template.process(input, writer);
        return writer.toString();
    }

    /**
     * Get answer for a particular UUID
     * @param uuid UUID of QnA
     * @param username Username
     * @param evaluate Evaluate answer when set to true and do not evanluate when set to false
     * @param askedQuestion Asked question
     */
    public ResponseAnswer getAnswer(String uuid, Context domain, String username, boolean evaluate, String askedQuestion) {
        try {
            Answer answer = contextService.getQnA(null, uuid, domain);
            if (evaluate) {
                // INFO: Do not use original question, but actually asked question
                Sentence analyzedQuestion = nerService.analyze(utilsService.preProcessQuestion(askedQuestion), null, domain);
                //Sentence analyzedQuestion = nerService.analyze(utilsService.preProcessQuestion(answer.getOriginalquestion()), null, domain);
                answer.setAnswer(dynamicExprEvalService.postProcess(answer.getAnswer(), analyzedQuestion));
            }
            PermissionStatus permissionsStatus = iamService.getPermissionStatus(answer, username);
            return mapAnswer(answer, null, permissionsStatus);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Check permission and map Answer onto ResponseAnswer
     * @param domain Domain containing answer
     * @param username Username of user asking question
     * @param dateSubmitted Date when question was submitted
     * @param analyzedQuestion Analyzed version of question
     */
    private ResponseAnswer inspectAnswer(Answer answer, Context domain, String username, Date dateSubmitted, Sentence analyzedQuestion) {
        try {
            if (answer.getUuid() != null) {
                log.info("Check whether answer '" + answer.getUuid() + "' is protected ...");
            } else {
                // INFO: Third-party answers do not have a UUID
                log.info("Check whether answer is protected ...");
            }

            answer = getFromUUID(answer, domain);
            if (answer == null) {
                log.warn("Answer '" + answer.getUuid() + "' does not exist! Maybe index needs to be cleaned ...");
                return null;
            }

            answer = evaluateAnswer(answer, analyzedQuestion);

            PermissionStatus permissionsStatus = iamService.getPermissionStatus(answer, username);

            String HIDDEN_ANSWER = "*****";
            String HIDDEN_ORIGINAL_QUESTION = "*****";
            String HIDDEN_SOURCE_URL = "*****";

            if (permissionsStatus == PermissionStatus.IS_PUBLIC) {
                return mapAnswer(answer, dateSubmitted, PermissionStatus.IS_PUBLIC);
            } else if (permissionsStatus == PermissionStatus.PERMISSION_DENIED) {
                return new ResponseAnswer(answer.getUuid(), answer.getSubmittedQuestion(), dateSubmitted, HIDDEN_ANSWER, answer.getAnswerContentType(), answer.getClassifications(), null, null, null, HIDDEN_ORIGINAL_QUESTION, new Date(answer.getDateOriginalQuestion()), PermissionStatus.PERMISSION_DENIED, answer.getType(), HIDDEN_SOURCE_URL);
            } else if (permissionsStatus == PermissionStatus.NOT_SUFFICIENT_PERMISSIONS_TO_READ_ANSWER) {
                return new ResponseAnswer(answer.getUuid(), answer.getSubmittedQuestion(), dateSubmitted, HIDDEN_ANSWER, answer.getAnswerContentType(), answer.getClassifications(), null, null, null, HIDDEN_ORIGINAL_QUESTION, new Date(answer.getDateOriginalQuestion()), PermissionStatus.NOT_SUFFICIENT_PERMISSIONS_TO_READ_ANSWER, answer.getType(), HIDDEN_SOURCE_URL);
            } else if (permissionsStatus == PermissionStatus.USER_AUTHORIZED_TO_READ_ANSWER) {
                return mapAnswer(answer, dateSubmitted, PermissionStatus.USER_AUTHORIZED_TO_READ_ANSWER);
            } else if (permissionsStatus == PermissionStatus.MEMBER_AUTHORIZED_TO_READ_ANSWER) {
                return mapAnswer(answer, dateSubmitted, PermissionStatus.MEMBER_AUTHORIZED_TO_READ_ANSWER);
            } else {
                log.error("No such permission status '" + permissionsStatus + "' implemented!");
                return null;
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Check whether answer from AI service only contains an AskKatie UUID and if so, then return answer from AskKatie
     * @param answer Answer received from AI service (e.g. DeepPavlov)
     * @param domain Domain containing answer
     * @return AskKatie answer if UUID exists and otherwise return answer from AI service
     */
    private Answer getFromUUID(Answer answer, Context domain) throws Exception {
        if (answer.getAnswer().startsWith(Answer.AK_UUID_COLON)) {
            String uuid = Answer.removePrefix(answer.getAnswer());
            log.info("Get answer with UUID '" + uuid + "' ...");
            return contextService.getQnA(answer.getSubmittedQuestion(), uuid, domain);
        } else {
            log.info("Answer from AI service already contains actual answer.");
            return answer;
        }
    }

    /**
     * Map Answer object to ResponseAnswer object
     */
    public ResponseAnswer mapAnswer(Answer answer, Date dateSubmitted, PermissionStatus permissionStatus) {
        String cipherAlgorithm = answer.getAnswerClientSideEncryptionAlgorithm();

        log.info("Map QnA '" + answer.getUuid() + "' to ResponseAnswer ...");
        log.info("QnA answer content type: " + answer.getAnswerContentType());
        return new ResponseAnswer(answer.getUuid(), answer.getSubmittedQuestion(), dateSubmitted, answer.getAnswer(), answer.getAnswerContentType(), answer.getClassifications(), new Date(answer.getDateAnswerModified()), cipherAlgorithm, null, answer.getOriginalquestion(), new Date(answer.getDateOriginalQuestion()), permissionStatus, answer.getType(), answer.getUrl());
    }

    /**
     *
     */
    private Answer evaluateAnswer(Answer answer, Sentence analyzedQuestion) {
        answer.setAnswer(dynamicExprEvalService.postProcess(answer.getAnswer(), analyzedQuestion));
        return answer;
    }
}
