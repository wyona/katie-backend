package com.wyona.katie.models;
  
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.VectorSimilarityFunction;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Consider renaming it to Domain
 *
 * The context defines the space in which Katie operates. Example contexts are the open source CMS project (http://www.yanel.org), or a particular Slack channel, or the company Wyona.
 */
@Slf4j
public class Context {

    public static final String ROOT_NAME = "ROOT";
    public static final String IMPORT_URL_META_FILE = "meta.xml";
    public static final String REINDEX_LOCK_FILE = "reindex.lock";

    private String contextId;
    private String name;
    private String tagName;
    private File contextDirectory;

    private DetectDuplicatedQuestionImpl detectDuplicatedQuestionImpl;
    private EmbeddingsImpl embeddingsImpl;
    private String embeddingsModel;
    private EmbeddingValueType embeddingValueType;
    private String embeddingsEndpoint;
    private String embeddingsApiToken;
    private VectorSimilarityFunction vectorSimilarityMetric;

    private boolean analyzeMessagesAskRestApi = false;
    private ReRankImpl reRankImpl;
    private CompletionImpl reRankLLMImpl;
    private CompletionImpl generateImpl;
    private List<PromptMessage> promptMessages;

    private ClassificationImpl classifierImpl;

    private NerImpl nerImpl;

    private Double scoreThreshold; // INFO: Independent of search implementation, whereas also see implementation specific thresholds: weaviateCertaintyThreshold, sentenceBERTDistanceThreshold

    private String queryServiceUrl;
    private String weaviateQueryUrl;
    private float weaviateCertaintyThreshold;

    private String azureAISearchIndexName;
    private String azureAISearchEndpoint;
    private String azureAISearchAdminKey;

    private String knowledgeGraphQueryUrl;
    private String elasticsearchIndex;

    private String sentenceBERTCorpusId;
    private float sentenceBERTDistanceThreshold;

    private boolean answersGenerallyProtected;
    private String host;
    private String mailBodyDeepLink;
    private String mailSubjectTag;
    private String mailSenderEmail;
    private boolean answersMustBeApprovedByModerator;
    private boolean informUserReModeration;

    private IMAPConfiguration imapConfiguration;
    private GmailConfiguration gmailConfiguration;
    private String matchReplyTo;

    private DomainSlackConfiguration slackConfig;

    private boolean considerHumanFeedback = false;
    private boolean reRankAnswers = false;
    private boolean useGenerativeAI = false;
    private boolean katieSearchEnabled = true;

    private boolean informUserReNoAnswerAvailable;

    private String reindexBackgroundProcessId;


    /**
     * @param contextId Domain Id, e.g. "b7877c87-7be5-4bbb-87b1-39d491a171d6"
     * @param contextDirectory Base directory of context (containing domain configuration, etc.)
     * @param host Host name where Katie is running/deployed, e.g. http://localhost:8080 or https//ukatie.com or https://veeting.ukatie.com
     * @param mailBodyDeepLink Deep link, e.g. "http://localhost:8080/read-answer.html"
     * @param mailSenderEmail Sender email, e.g. "no-reply@wyona.com" or "Katie <no-reply@katie.qa>"
     * @param answersMustBeApprovedByModerator True when answers must be approved by a moderator
     * @param informUserReModeration TODO
     * @param useGenerativeAI True when Katie shall generate answers using Generative AI
     * @param katieSearchEnabled True when Katie shall search its own knowledge base
     * @param reindexBackgroundProcessId Background process Id when domain is being reindexed
     */
    public Context(String contextId, File contextDirectory, boolean answersGenerallyProtected, String host, String mailBodyDeepLink, String mailSubjectTag, String mailSenderEmail, boolean answersMustBeApprovedByModerator, boolean informUserReModeration, boolean considerHumanFeedback, boolean reRankAnswers, boolean useGenerativeAI, boolean katieSearchEnabled, String reindexBackgroundProcessId) {
        this.contextId = contextId;
        this.contextDirectory = contextDirectory;

        detectDuplicatedQuestionImpl = DetectDuplicatedQuestionImpl.LUCENE_DEFAULT;

        nerImpl = null; //NerImpl.DO_NOT_ANALYZE;

        this.answersGenerallyProtected = answersGenerallyProtected;
        this.host = host;
        this.mailBodyDeepLink = mailBodyDeepLink;
        this.mailSubjectTag = mailSubjectTag;
        this.mailSenderEmail = mailSenderEmail;
        this.answersMustBeApprovedByModerator = answersMustBeApprovedByModerator;
        this.informUserReModeration = informUserReModeration;

        this.slackConfig = new DomainSlackConfiguration();

        this.vectorSimilarityMetric = null;

        this.katieSearchEnabled = katieSearchEnabled;
        this.considerHumanFeedback = considerHumanFeedback;
        this.reRankAnswers = reRankAnswers;
        this.useGenerativeAI = useGenerativeAI;

        this.reRankImpl = null;
        this.reRankLLMImpl = null;
        this.generateImpl = null;
        this.promptMessages = new ArrayList<>();

        this.informUserReNoAnswerAvailable = false;

        this.reindexBackgroundProcessId = reindexBackgroundProcessId;
    }

    /**
     * @param analyzeMessagesAskRestApi When true, then the Ask REST Interfaces should use the question / message classifier to analyze submitted questions / messages
     */
    public void setAnalyzeMessagesAskRestApi(boolean analyzeMessagesAskRestApi) {
        this.analyzeMessagesAskRestApi = analyzeMessagesAskRestApi;
    }

    /**
     * @return true when also the Ask REST Interfaces should use the question / message classifier to analyze submitted questions / messages
     */
    public boolean getAnalyzeMessagesAskRestApi() {
        return analyzeMessagesAskRestApi;
    }

    /**
     *
     */
    public void setSlackConfiguration(DomainSlackConfiguration slackConfig) {
        this.slackConfig = slackConfig;
    }

    /**
     * Get Slack configuration
     */
    public DomainSlackConfiguration getSlackConfiguration() {
        return slackConfig;
    }

    /**
     * Set IMAP configuration, such that a Katie domain can access email messages via IMAP
     */
    public void setImapConfiguration(IMAPConfiguration imapConfiguration) {
        this.imapConfiguration = imapConfiguration;
    }

    /**
     * Get IMAP configuration, such that a Katie domain can access email messages via IMAP
     */
    public IMAPConfiguration getImapConfiguration() {
        return imapConfiguration;
    }

    /**
     * Set Gmail configuration, such that a Katie domain can access email messages via Gmail API
     */
    public void setGmailConfiguration(GmailConfiguration configuration) {
        this.gmailConfiguration = configuration;
    }

    /**
     * Get Gmail configuration, such that a Katie domain can access email messages via Gmail API
     */
    public GmailConfiguration getGmailConfiguration() {
        return gmailConfiguration;
    }

    /**
     * Set reply-to email address, such that only messages with this reply-to address are being processed
     * @param matchReplyTo Reply-To email address, e.g. "users@httpd.apache.org"
     */
    public void setMatchReplyTo(String matchReplyTo) {
        this.matchReplyTo = matchReplyTo;
    }

    /**
     *
     */
    public String getMatchReplyTo() {
        return matchReplyTo;
    }

    /**
     * Get NER implementation
     */
    public NerImpl getNerImpl() {
        return nerImpl;
    }

    /**
     * Set NER implementation
     */
    public void setNerImpl(NerImpl nerImpl) {
        this.nerImpl = nerImpl;
    }

    /**
     *
     */
    public void setEmbeddingsImpl(EmbeddingsImpl embeddingsImpl) {
        this.embeddingsImpl = embeddingsImpl;
    }

    /**
     *
     */
    public EmbeddingsImpl getEmbeddingsImpl() {
        return embeddingsImpl;
    }

    /**
     * @param embeddingsModel Model of embeddings implementation, e.g. "text-similarity-ada-001" in the case of OpenAI or "small" in the case of Cohere
     */
    public void setEmbeddingsModel(String embeddingsModel) {
        this.embeddingsModel = embeddingsModel;
    }

    /**
     *
     */
    public String getEmbeddingsModel() {
        return embeddingsModel;
    }

    /**
     * @param embeddingValueType Embedding value type, either float32 or int8
     */
    public void setEmbeddingValueType(EmbeddingValueType embeddingValueType) {
        this.embeddingValueType = embeddingValueType;
    }

    /**
     * @return embedding value type, either float32 or int8
     */
    public EmbeddingValueType getEmbeddingValueType() {
        return embeddingValueType;
    }

    /**
     * @param endpoint OpenAI compatble embeddings endpoint, e.g. https://api.mistral.ai/v1/embeddings
     */
    public void setEmbeddingsEndpoint(String endpoint) {
        this.embeddingsEndpoint = endpoint;
    }

    /**
     *
     */
    public String getEmbeddingsEndpoint() {
        return embeddingsEndpoint;
    }

    /**
     *
     */
    public void setEmbeddingsApiToken(String apiToken) {
        this.embeddingsApiToken = apiToken;
    }

    /**
     *
     */
    public String getEmbeddingsApiToken() {
        return embeddingsApiToken;
    }

    /**
     * See https://www.baeldung.com/cs/euclidean-distance-vs-cosine-similarity or https://en.wikipedia.org/wiki/Cosine_similarity
     */
    public VectorSimilarityFunction getVectorSimilarityMetric() {
        if (vectorSimilarityMetric == null) {
            return VectorSimilarityFunction.COSINE;
            // INFO: DOT_PRODUCT is the same like COSINE, but the DOT_PRODUCT is not normed to 1 when two vectors are equal and the angle between them is 0
            //VectorSimilarityFunction.DOT_PRODUCT;
            //VectorSimilarityFunction.EUCLIDEAN;
        } else {
            return vectorSimilarityMetric;
        }
    }

    /**
     *
     */
    public void setVectorSimilarityMetric(VectorSimilarityFunction vectorSimilarityMetric) {
        this.vectorSimilarityMetric = vectorSimilarityMetric;
    }

    /**
     * Get cognitive service to find duplicated question
     */
    public DetectDuplicatedQuestionImpl getDetectDuplicatedQuestionImpl() {
        return detectDuplicatedQuestionImpl;
    }

    /**
     * Set cognitive service to find duplicated question
     */
    public void setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl detectDuplicatedQuestionImpl) {
        this.detectDuplicatedQuestionImpl = detectDuplicatedQuestionImpl;
    }

    /**
     *
     */
    public void unsetDetectDuplicatedQuestionImpl() {
        this.sentenceBERTCorpusId = null;
        this.knowledgeGraphQueryUrl = null;
        this.queryServiceUrl = null;
        this.weaviateQueryUrl = null;
        this.elasticsearchIndex = null;

        this.detectDuplicatedQuestionImpl = DetectDuplicatedQuestionImpl.UNSET;
    }

    /**
     * Get Id of domain, e.g. "b7877c87-7be5-4bbb-87b1-39d491a171d6"
     */
    public String getId() {
        return contextId;
    }

    /**
     * Get name of domain, e.g. "Wyona" or "Apache Lucene"
     */
    public String getName() {
        if (name != null) {
            return name;
        } else { // INFO: This is possible, because 'name' was introduced at a later stage
            log.warn("No name set for domain with id '" + getId() + "'!");
            return "NO_NAME_SET";
        }
    }

    /**
     * Set name of domain
     * @param name Name of domain, e.g. "Wyona" or "Apache Lucene"
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set tag name of domain, which is used for example for readable URL
     * @param tagName Tag name of domain, e.g. "wyona" or "apache-lucene"
     */
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    /**
     * Get tag name of domain, e.g. "wyona" or "apache-lucene"
     */
    public String getTagName() {
        if (tagName != null) {
            return tagName;
        } else { // INFO: This is possible, because 'name' was introduced at a later stage
            log.warn("No tag name set for domain with id '" + getId() + "'!");
            return "NO_TAG_NAME_SET";
        }
    }

    /**
     * Get directory containing domain configuration
     * @return directory containing domain configuration, e.g. "/home/wyona/domains/a30b9bfe-0ffb-41eb-a2e2-34b238427a74"
     */
    public File getContextDirectory() {
        return contextDirectory;
    }

    /**
     *
     */
    public File getClassificationsDirectory() {
        return new File(getContextDirectory(), "classifications");
    }

    /**
     * @param language Language of FAQ, e.g. 'de' or 'en'
     */
    public File getFAQJsonDataPath(String language) {
        return new File(new File(contextDirectory, "faq"), "faq_" + language + ".json");
    }

    /**
     * @param language Two-letter language code of FAQ, e.g. 'de' or 'en'
     */
    public File getFAQXmlDataPath(String language) {
        return new File(new File(contextDirectory, "faq"), "faq_" + language + ".xml");
    }

    /**
     * Get FAQ languages
     * @return FAQ languages (e.g. "en", "de", "fr", "it", "pt") and empty array when no FAQ languages exist
     */
    public String[] getFAQLanguages() {
        ArrayList<String> languages = new ArrayList<String>();
        File faqDir = new File(contextDirectory, "faq");
        if (faqDir.isDirectory()) {
            File[] files = faqDir.listFiles(new FaqXmlFilter());
            for (int i = 0; i < files.length; i++) {
                String name = files[i].getName(); // INFO: For example "faq_en.xml"
                log.debug("File name: " + name);
                String language = name.substring(4,6); // INFO: For example "en"
                languages.add(language);
            }
            return languages.toArray(new String[0]);
        } else {
            log.debug("No such directory '" + faqDir.getAbsolutePath() + "'!");
            return new String[0];
        }
    }

    /**
     * @return name of directory which contains QnAs
     */
    public File getQuestionsAnswersDataPath() {
        return new File(contextDirectory, "questions-answers");
    }

    /**
     * @return name of directory which contains data objects
     */
    public File getDataObjectsPath() {
        // TODO: Consider "data-objects", "data-items", "data-resources", ...
        return new File(contextDirectory, "data");
    }

    /**
     *
     */
    public File getURLsDataPath() {
        return new File(contextDirectory, "urls");
    }

    /**
     * @param channelId Channel Id, e.g. "996391611275694131" or "C03LR9METBK"
     * @param channelRequestId Channel request Id, e.g. "3139c14f-ae63-4fc4-abe2-adceb67988af"
     */
    public File getChannelThreadDataPath(String channelId, String channelRequestId) {
        File threadDir = new File(getChannelThreadsDataPath(channelId), channelRequestId);
        return threadDir;
    }

    /**
     * @param channelId Channel Id, e.g. "996391611275694131" or "TODO"
     */
    public File getChannelThreadsDataPath(String channelId) {
        File channelsDir = new File(contextDirectory, "channels");
        File channelDir = new File(channelsDir, channelId);
        File threadsDir = new File(channelDir, "threads");
        return threadsDir;
    }

    /**
     * @return path of XML file, which contains QnA content
     */
    public File getQnAXmlFilePath(String uuid) {
        return new File(new File(getQuestionsAnswersDataPath(), uuid), "qa.xml");
    }

    /**
     * @return path of XML file, which contains ratings of a particular QnA
     */
    public File getRatingsXmlFilePath(String uuid) {
        return new File(new File(getQuestionsAnswersDataPath(), uuid), "ratings.xml");
    }

    /**
     * Get directory containing ratings of answers given to asked questions
     * @return path of directory containing ratings of answers of a particular domain
     */
    public File getRatingsDirectory() {
        return new File(getContextDirectory(), "ratings");
    }

    /**
     *
     */
    public File getRatingsOfPredictedLabelsDirectory() {
        return new File(getClassificationsDirectory(), "ratings");
    }

    /**
     * @return path of directory containing asked questions of a particular domain
     */
    public File getAskedQuestionsDirectory() {
        return new File(getContextDirectory(), "asked-questions");
    }

    /**
     * @return path of directory containing predicted text classifications associated with a particular domain
     */
    public File getPredictedLabelsDirectory() {
        return new File(getClassificationsDirectory(), "predicted-classifications");
    }

    /**
     * @return path of directory, which contains embedding vectors
     */
    public File getQnAEmbeddingsPath(String uuid) {
        return new File(new File(getQuestionsAnswersDataPath(), uuid), "embeddings");
    }

    /**
     * Get data path, which contains downloaded data referenced by an URL
     * @param url URL of dumped webpage, e.g. "https://www.myright.ch/en/legal-tips/corona-private/covid-certificatetrequirement"
     */
    public File getUrlDumpFile(URI url) {
        return new File(getThirdPartyUrlPath(url), "data");
    }

    /**
     * Get index file containing UUIDs of QnAs associated with a particular URL
     * @param url URL of webpage containing QnAs, e.g. "https://www.myright.ch/en/legal-tips/corona-private/covid-certificatetrequirement"
     */
    public File getUuidUrlIndex(URI url) {
        return new File(getThirdPartyUrlPath(url), "uuids_index.txt");
    }

    /**
     * Get meta file re URL / webpage containing QnAs
     * @param url URL of webpage containing QnAs, e.g. "https://www.myright.ch/en/legal-tips/corona-private/covid-certificatetrequirement"
     */
    public File getUrlMetaFile(URI url) {
        return new File(getThirdPartyUrlPath(url), IMPORT_URL_META_FILE);
    }

    /**
     * Get directory containing information re a third-party URL
     * @param url URL associated with QnA, e.g. "https://www.myright.ch/en/legal-tips/corona-private/covid-certificatetrequirement"
     */
    private File getThirdPartyUrlPath(URI url) {
        String urlPath = url.getHost() + url.getPath();
        log.info("URL file system path: " + urlPath);
        File urlDir = new File(getURLsDataPath(), urlPath);
        return urlDir;
    }

    /**
     * Get directory path where pending emails are stored, which need to get moderated
     */
    public File getPendingEmailsDataPath() {
        return new File(contextDirectory, "pending-emails");
    }

    /**
     * Get directory path where custom email templates are stored
     */
    public File getEmailTemplatesDataPath() {
        return new File(contextDirectory, "templates");
    }

    /**
     * Get list of custom email templates
     * @return list of filenames, e.g. "answer-to-question_email_en.ftl", "answer-to-question_email_de.ftl"
     */
    public String[] getCustomEmailTemplates() {
        if (getEmailTemplatesDataPath().isDirectory()) {
            return getEmailTemplatesDataPath().list();
        } else {
            return null;
        }
    }

    /**
     *
     */
    public void setAzureAISearchIndexName(String azureAISearchIndexName) {
        this.azureAISearchIndexName = azureAISearchIndexName;
    }

    /**
     *
     */
    public String getAzureAISearchIndexName() {
        return azureAISearchIndexName;
    }

    /**
     * @param endpoint Azure AI Search endpoint, e.g. https://katie.search.windows.net
     */
    public void setAzureAISearchEndpoint(String endpoint) {
        this.azureAISearchEndpoint = endpoint;
    }

    /**
     *
     */
    public String getAzureAISearchEndpoint() {
        return azureAISearchEndpoint;
    }

    /**
     *
     */
    public void setAzureAISearchAdminKey(String adminKey) {
        this.azureAISearchAdminKey = adminKey;
    }

    /**
     *
     */
    public String getAzureAISearchAdminKey() {
        return azureAISearchAdminKey;
    }

    /**
     * @return Weaviate query URL, e.g. https://katie.semi.network or https://weaviate.ukatie.com
     */
    public String getWeaviateQueryUrl() {
        return weaviateQueryUrl;
    }

    /**
     * @param weaviateQueryUrl Weaviate query URL, e.g. https://katie.semi.network or https://weaviate.ukatie.com
     */
    public void setWeaviateQueryUrl(String weaviateQueryUrl) {
        this.weaviateQueryUrl = weaviateQueryUrl;
        detectDuplicatedQuestionImpl = DetectDuplicatedQuestionImpl.WEAVIATE;
    }

    /**
     * Set Weaviate certainty threshold
     * @param certaintyThreshold Certainty threshold, e.g. 0.5
     */
    public void setWeaviateCertaintyThreshold(float certaintyThreshold) {
        this.weaviateCertaintyThreshold = certaintyThreshold;
    }

    /**
     * Get Weaviate certainty threshold
     * @return certainty threshold, e.g. 0.5
     */
    public float getWeaviateCertaintyThreshold() {
        return weaviateCertaintyThreshold;
    }

    /**
     * @return query service URL, e.g. http://localhost:8383/api/v1 or https://elasticsearch-connector.ukatie.com/api/v1 or https://weaviate-connector.ukatie.com/api/v1 or https://cyclix-connector.ukatie.com/api/v1
     */
    public String getQueryServiceUrl() {
        return queryServiceUrl;
    }

    /**
     * @param queryServiceUrl Query service URL, e.g. https://elasticsearch.ukatie.com or https://weaviate.ukatie.com
     */
    public void setQueryServiceUrl(String queryServiceUrl) {
        this.queryServiceUrl = queryServiceUrl;
        detectDuplicatedQuestionImpl = DetectDuplicatedQuestionImpl.QUERY_SERVICE;
    }

    /**
     *
     */
    public String getKnowledgeGraphQueryUrl() {
        return knowledgeGraphQueryUrl;
    }

    /**
     * Set knowledge graph query URL
     * @param knowledgeGraphQueryUrl Knowledge graph query URL, e.g. "https://query.wikidata.org/sparql
     */
    public void setKnowledgeGraphQueryUrl(String knowledgeGraphQueryUrl) {
        this.knowledgeGraphQueryUrl = knowledgeGraphQueryUrl;
        detectDuplicatedQuestionImpl = DetectDuplicatedQuestionImpl.KNOWLEDGE_GRAPH;
    }

    /**
     * Get elasticsearch index name, e.g. "askkatie_1bb13fed-683a-4e37-b531-45b5c9a4324f"
     */
    public String getElasticsearchIndex() {
        return elasticsearchIndex;
    }

    /**
     * @param elasticsearchIndex Elasticsearch index, e.g. "askkatie_5bd57b92-da98-422f-8ad6-6670b9c69184"
     */
    public void setElasticsearchIndex(String elasticsearchIndex) {
        this.elasticsearchIndex = elasticsearchIndex;
        detectDuplicatedQuestionImpl = DetectDuplicatedQuestionImpl.ELASTICSEARCH;
    }

    /**
     * Get SentenceBERT corpus id, e.g. "1bb13fed-683a-4e37-b531-45b5c9a4324f"
     */
    public String getSentenceBERTCorpusId() {
        return sentenceBERTCorpusId;
    }

    /**
     * @param sentenceBERTCorpusId SentenceBERT corpus Id, e.g. "3nk85f64-5517-4562-b3fc-2c963f16aoa9"
     */
    public void setSentenceBERTCorpusId(String sentenceBERTCorpusId) {
        this.sentenceBERTCorpusId = sentenceBERTCorpusId;
        detectDuplicatedQuestionImpl = DetectDuplicatedQuestionImpl.SENTENCE_BERT;
    }

    /**
     * @param distanceThreshold Distance threshold, e.g. 0.5
     */
    public void setSentenceBERTDistanceThreshold(float distanceThreshold) {
        this.sentenceBERTDistanceThreshold = distanceThreshold;
    }

    /**
     * @return distance threshold, e.g. 0.5
     */
    public float getSentenceBERTDistanceThreshold() {
        return sentenceBERTDistanceThreshold;
    }

    /**
     *
     */
    public void setScoreThreshold(Double scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
    }

    /**
     * @return score threshold, e.g. 0.85
     */
    public Double getScoreThreshold() {
        return scoreThreshold;
    }

    /**
     * @return true when answers of this domain are generally protected and false otherwise
     */
    public boolean getAnswersGenerallyProtected() {
        return answersGenerallyProtected;
    }

    /**
     *
     */
    public void setAnswersGenerallyProtected(boolean answersGenerallyProtected) {
        this.answersGenerallyProtected = answersGenerallyProtected;
    }

    /**
     * @return true when answers must be approved by a moderator and false otherwise
     */
    public boolean getAnswersMustBeApprovedByModerator() {
        return answersMustBeApprovedByModerator;
    }

    /**
     *
     */
    public void setAnswersMustBeApprovedByModerator(boolean answersMustBeApprovedByModerator) {
        this.answersMustBeApprovedByModerator = answersMustBeApprovedByModerator;
    }

    /**
     * @return true when a notification should be sent to the person asking a question that Katie's response must be a approved by a human moderator and false otherwise
     */
    public boolean getInformUserReModeration() {
        return informUserReModeration;
    }

    /**
     *
     */
    public void setInformUserReModeration(boolean informUserReModeration) {
        this.informUserReModeration = informUserReModeration;
    }

    /**
     * @return true when notification should be sent to the person asking a question that Katie does not know an answer
     */
    public boolean getInformUserNoAnswerAvailable() {
        return informUserReNoAnswerAvailable;
    }

    /**
     * @param informUserReNoAnswerAvailable Set to true when user should be informed that no answer is available
     */
    public void setInformUserReNoAnswerAvailable(boolean informUserReNoAnswerAvailable) {
        this.informUserReNoAnswerAvailable = informUserReNoAnswerAvailable;
    }

    /**
     * Get host name where Katie is running/deployed
     * @return host name used inside email body (or Slack message), e.g. 'https://app.katie.qa'
     */
    public String getHost() {
        return host;
    }

    /**
     *
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     *
     */
    public String getMailBodyDeepLink() {
        return mailBodyDeepLink;
    }

    /**
     * Set mail subject tag
     */
    public void setMailSubjectTag(String mailSubjectTag) {
        this.mailSubjectTag = mailSubjectTag;
    }

    /**
     * Get configured subject tag
     * @return subject tag, e.g. "My Katie" or "Katie Wyona Customer Care"
     */
    public String getMailSubjectTag() {
        return mailSubjectTag;
    }

    /**
     * Set mail sender
     * @param mailSenderEmail Mail sender email address, e.g. "no-reply@wyona.com" or "Katie <no-reply@katie.qa>"
     */
    public void setMailSenderEmail(String mailSenderEmail) {
        this.mailSenderEmail = mailSenderEmail;
    }

    /**
     * Get configured send / from email address
     * @return send / from email address, e.g. "no-reply@wyona.com" or "Katie <no-reply@katie.qa>"
     */
    public String getMailSenderEmail() {
        return mailSenderEmail;
    }

    /**
     * @return true when human feedback should be considered when answering questions
     */
    public boolean getConsiderHumanFeedback() {
        return considerHumanFeedback;
    }

    /**
     * @param considerHumanFeedback True when human feedback should be considered when answering questions
     */
    public void setConsiderHumanFeedback(boolean considerHumanFeedback) {
        this.considerHumanFeedback = considerHumanFeedback;
    }

    /**
     * @return true when answers should be re-ranked
     */
    public boolean getReRankAnswers() {
        return reRankAnswers;
    }

    /**
     * @param reRankAnswers True when answers should be re-ranked
     */
    public void setReRankAnswers(boolean reRankAnswers) {
        this.reRankAnswers = reRankAnswers;
    }

    /**
     * @return true when answers should be generated / completed using a GenAI model and false otherwise
     */
    public boolean getGenerateCompleteAnswers() {
        return useGenerativeAI;
    }

    /**
     *
     */
    public void setGenerateCompleteAnswers(boolean useGenerativeAI) {
        this.useGenerativeAI = useGenerativeAI;
    }

    /**
     *
     */
    public ReRankImpl getReRankImpl() {
        return reRankImpl;
    }

    /**
     *
     */
    public void setReRankImpl(ReRankImpl reRankImpl) {
        this.reRankImpl = reRankImpl;
    }

    /**
     *
     */
    public void setReRankLLMImpl(CompletionImpl impl) {
        this.reRankLLMImpl = impl;
    }

    /**
     *
     */
    public CompletionImpl getReRankLLMImpl() {
        return this.reRankLLMImpl;
    }

    /**
     *
     */
    public CompletionImpl getCompletionImpl() {
        return generateImpl;
    }

    /**
     *
     */
    public void setCompletionImpl(CompletionImpl generateImpl) {
        this.generateImpl = generateImpl;
    }

    /**
     *
     */
    public void setClassifierImpl(ClassificationImpl classifierImpl) {
        this.classifierImpl = classifierImpl;
    }

    /**
     *
     */
    public ClassificationImpl getClassifierImpl() {
        if (classifierImpl != null) {
            return classifierImpl;
        } else {
            return ClassificationImpl.CENTROID_MATCHING;
        }
    }

    /**
     * Get domain specific prompt templates
     * @return prompt templates, e.g. "Please answer the following '{{QUESTION}}' based on the following context '{{CONTEXT}}'."
     */
    public List<PromptMessage> getPromptMessages() {
        return promptMessages;
    }

    /**
     * Set domain specific prompt templates
     */
    public void setPromptMessages(List<PromptMessage> promptMessages) {
        this.promptMessages = promptMessages;
    }

    /**
     * @param katieSearchEnabled True when Katie shall search its own knowledge base
     */
    public void setKatieSearchEnabled(boolean katieSearchEnabled) {
        this.katieSearchEnabled = katieSearchEnabled;
    }

    /**
     * @return true when Katie shall search its own knowledge base
     */
    public boolean getKatieSearchEnabled() {
        return katieSearchEnabled;
    }

    /**
     * Get reindex background process Id when domain is being reindex, otherwise return null
     * @return background process Id
     */
    public String getReindexBackgroundProcessId() {
        return reindexBackgroundProcessId;
    }
}

/**
 *
 */
class FaqXmlFilter implements FilenameFilter {

    @Override
    public boolean accept(File directory, String fileName) {
        if (fileName.startsWith("faq_") && fileName.endsWith(".xml")) {
            return true;
        }
        return false;
    }
}
