package com.wyona.katie.services;

import com.wyona.katie.models.*;
import com.wyona.katie.models.faq.FAQ;
import com.wyona.katie.models.faq.Icon;
import com.wyona.katie.models.faq.Question;
import com.wyona.katie.models.faq.Topic;
import com.wyona.katie.models.faq.TopicVisibility;
import lombok.extern.slf4j.Slf4j;

import org.apache.lucene.index.VectorSimilarityFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

//import org.apache.xml.resolver.tools.CatalogResolver;

import java.io.*;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Component
public class XMLService {

    @Value("${spring.ai.azure.openai.endpoint}")
    private String openAIAzureHost;

    @Value("${mistral.ai.completion.model}")
    private String mistralAIModel;

    @Value("${openai.generate.model}")
    private String openAIModel;

    @Value("${ollama.completion.model}")
    private String ollamaModel;

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaDefaultHost;

    @Value("${iam.data_path}")
    private String iamDataPath;

    @Value("${contexts.data_path}")
    private String contextsDataPath;

    @Value("${mail.default.sender.email.address}")
    private String defaultSenderEmailAddress;

    @Value("${sbert.distance.threshold}")
    private float distanceThreshold;

    @Value("${weaviate.certainty.threshold}")
    private float certaintyThreshold;

    @Value("${re_rank.implementation}")
    private ReRankImpl reRankDefaultImpl;

    @Value("${re_rank.llm.impl}")
    private CompletionImpl reRankLLMDefaultImpl;

    // TODO: Make configurable
    private CompletionImpl completionDefaultImpl = CompletionImpl.UNSET;

    private static final String DOMAIN_CONFIG_FILE_NAME = "config.xml";

    private static final int INDENT_AMOUNT = 2;
    private static final String YES = "yes";
    private static final String NO = "no";

    private static final String QA_NAMESPACE_1_0_0 = "http://www.wyona.com/askkatie/1.0.0";
    private static final String QA_ROOT_TAG = "qa";
    private static final String QA_QUESTION_TAG = "question";
    private static final String QA_QUESTION_DATE_ATTR = "epoch";
    private static final String QA_DATE_ANSWER_MODIFIED_ATTR = "modified";
    private static final String QA_ALTERNATIVE_QUESTIONS_TAG = "alternative-questions";
    private static final String QA_ANSWER_TAG = "answer";
    private static final String QA_ANSWER_TYPE_ATTR = "type";
    private static final String QA_ANSWER_CONTENT_TYPE_ATTR = "content-type";
    private static final String QA_ANSWER_CLIENT_SIDE_ENCRYPTION_ATTR = "client-side-encryption-algorithm";
    private static final String QA_ANSWER_RESPONDENT_ID_ATTR = "respondent-id";
    private static final String QA_RATINGS_TAG = "ratings";
    private static final String QA_RATING_TAG = "rating";
    private static final String QA_RATING_ATTR = "value";
    private static final String QA_RATING_FEEDBACK_TAG = "feedback";
    private static final String QA_RATING_QUESTION_TAG = "question";
    private static final String QA_RATING_QUESTION_UUID_ATTR = "uuid";
    private static final String QA_RATING_EMAIL_TAG = "email";
    private static final String QA_SOURCE_TAG = "source";
    private static final String QA_URL_TAG = "url";
    private static final String QA_FAQ_TAG = "faq";
    private static final String QA_FAQ_LANG_ATTR = "language";
    private static final String QA_FAQ_TOPIC_ID_ATTR = "topic-id";
    private static final String QA_NO_FAQ_TAG = "no-faq";
    private static final String CLASSIFICATIONS_TAG = "classifications";
    private static final String CLASSIFICATION_TERM_TAG = "term";
    private static final String QA_PERMISSIONS_TAG = "permissions";
    private static final String QA_PUBLIC_TAG = "public";
    private static final String QA_USER_TAG = "user";
    private static final String QA_GROUP_TAG = "group";
    private static final String QA_EMAIL_ATTR = "email";
    private static final String QA_UUID_ATTR = "uuid";
    private static final String QA_IS_TRAINED_ATTR = "is_trained";
    private static final String QA_REFERENCE_TAG = "reference";
    private static final String QA_KNOWLEDGE_SOURCE = "knowledge-source";
    private static final String QA_KNOWLEDGE_SOURCE_KEY_ATTR = "foreign-key";
    private static final String QA_KNOWLEDGE_SOURCE_ID_ATTR = "id";

    private static final String KATIE_NAMESPACE_1_0_0 = "http://www.wyona.com/askkatie/1.0.0";

    private static final String CONTEXT_ROOT_TAG = "context";
    private static final String DOMAIN_ID_ATTR = "id";
    private static final String DOMAIN_NAME_ATTR = "name";

    private static final String CONTEXT_KNOWLEDGE_GRAPH_TAG = "knowledge-graph";
    private static final String CONTEXT_WEAVIATE_TAG = "weaviate";
    private static final String CONTEXT_WEAVIATE_THRESHOLD_ATTR = "certainty-threshold";
    private static final String CONTEXT_MILVUS_TAG = "milvus";
    private static final String CONTEXT_MILVUS_BASE_URL_ATTR = "base-url";
    private static final String CONTEXT_AZURE_AI_SEARCH_ENDPOINT_ATTR = "endpoint";
    private static final String CONTEXT_AZURE_AI_SEARCH_ADMIN_KEY_ATTR = "admin-key";
    private static final String CONTEXT_AZURE_AI_SEARCH_INDEX_NAME_ATTR = "index-name";
    private static final String CONTEXT_QUERY_SERVICE_TAG = "query-service";
    private static final String CONTEXT_MCP_TAG = "mcp";
    private static final String CONTEXT_LLM_SEARCH_TAG = "llm-search";
    private static final String CONTEXT_LLM_SEARCH_ASSISTANT_ID_ATTR = "assistant-id";
    private static final String CONTEXT_LLM_SEARCH_ASSISTANT_NAME_ATTR = "assistant-name";
    private static final String CONTEXT_LLM_SEARCH_ASSISTANT_INSTRUCTIONS_ATTR = "assistant-instructions";
    private static final String CONTEXT_KATIE_SEARCH_TAG = "katie-search";
    private static final String CONTEXT_AZURE_AI_SEARCH_TAG = "azure-ai-search";
    private static final String CONTEXT_GEN_AI_PROMPT_MESSAGES_TAG = "generative-prompt-messages";
    private static final String CONTEXT_GEN_AI_PROMPT_MESSAGE_TAG = "msg";
    private static final String CONTEXT_GEN_AI_PROMPT_MESSAGE_ROLE_ATTR = "role";

    private static final String CONTEXT_LUCENE_VECTOR_SEARCH_TAG = "sbert-lucene";

    private static final String CONTEXT_VECTOR_SEARCH_EMBEDDINGS_IMPL_ATTR = "embeddings-impl";
    private static final String CONTEXT_VECTOR_SEARCH_MODEL_ATTR = "model";
    private static final String CONTEXT_VECTOR_SEARCH_EMBEDDING_VALUE_TYPE_ATTR = "value-type";
    private static final String CONTEXT_VECTOR_SEARCH_EMBEDDING_ENDPOINT_ATTR = "embeddings-endpoint";
    private static final String CONTEXT_VECTOR_SEARCH_API_TOKEN_ATTR = "api-token";
    private static final String CONTEXT_VECTOR_SEARCH_SIMILARITY_METRIC_ATTR = "similarity-metric";


    private static final String CONTEXT_SENTENCE_BERT_TAG = "sbert";
    private static final String CONTEXT_SENTENCE_BERT_CORPUS_ID_TAG = "corpus_id";
    private static final String CONTEXT_SENTENCE_BERT_THRESHOLD_ATTR = "distance-threshold";

    private static final String CONTEXT_ELASTICSEARCH_TAG = "elasticsearch";
    private static final String CONTEXT_ELASTICSEARCH_INDEX_TAG = "index";

    private static final String CONTEXT_NER_TAG = "ner";
    private static final String CONTEXT_NER_IMPL_ATTR = "impl";

    private static final String CONTEXT_MODERATION_TAG = "moderation";
    private static final String CONTEXT_MODERATION_TOGGLE_ATTR = "answers-must-be-approved";
    private static final String CONTEXT_MODERATION_INFORM_USER_RE_MODERATION_ATTR = "inform-user";

    private static final String CONTEXT_INDEX_SEARCH_PIPEILINE_TAG = "index-search-pipeline";
    private static final String CONTEXT_CONSIDER_HUMAN_FEEDBACK_ATTR = "consider-human-feedback";
    private static final String CONTEXT_RE_RANK_ANSWERS_ATTR = "re-rank-answers";
    private static final String CONTEXT_RE_RANK_IMPLEMENTATION_ATTR = "re-rank-impl";
    private static final String CONTEXT_USE_GENERATIVE_AI_ATTR = "generate-complete-answers";
    public static final String CONTEXT_GENERATIVE_AI_IMPLEMENTATION_ATTR = "completion-impl";
    public static final String CONTEXT_GENERATIVE_AI_MODEL_ATTR = "completion-model";
    public static final String CONTEXT_GENERATIVE_AI_API_KEY_ATTR = "completion-api-key";
    public static final String CONTEXT_GENERATIVE_AI_HOST_ATTR = "completion-host";
    private static final String CONTEXT_KATIE_SEARCH_ENABLED_ATTR = "search-enabled";
    private static final String CONTEXT_SCORE_THRESHOLD_ATTR = "score-threshold";
    private static final String CONTEXT_ANALYZE_MESSAGES_ASK_REST_API = "analyze-messages-ask-rest-api";

    private static final String CONTEXT_CLASSIFICATION_TAG = "classification";
    private static final String CONTEXT_CLASSIFIER_IMPL_ATTR = "classifier-impl";

    private static final String CONTEXT_INFORM_WHEN_NO_ANSWER_TAG = "inform-user-when-no-answer-available";
    private static final String CONTEXT_INFORM_WHEN_NO_ANSWER_ENABLED_ATTR = "enabled";

    private static final String CONTEXT_IAM_TAG = "iam";
    private static final String CONTEXT_IAM_PROTECTED_ATTR = "answers-generally-protected";

    private static final String CONTEXT_MAIL_TAG = "mail";
    private static final String CONTEXT_MAIL_BODY_TAG = "body";
    private static final String CONTEXT_MAIL_BODY_HOST_ATTR = "host";
    private static final String CONTEXT_MAIL_DEEPLINK_TAG = "deep_link";
    private static final String CONTEXT_MAIL_SUBJECTTAG_TAG = "subject_tag";
    private static final String CONTEXT_MAIL_SENDER_EMAIL_TAG = "sender_email";

    private static final String DOMAIN_MAIL_IMAP_TAG = "imap";
    private static final String DOMAIN_MAIL_GMAIL_TAG = "gmail";
    private static final String DOMAIN_MAIL_MATCH_REPLY_TO_TAG = "match";

    private static final String DOMAIN_SLACK_TAG = "slack";
    private static final String DOMAIN_SLACK_SEND_EXPERT_BUTTON_ATTR = "send-expert-button-enabled";
    private static final String DOMAIN_SLACK_IMPROVE_BUTTON_ATTR = "improve-answer-button-enabled";
    private static final String DOMAIN_SLACK_LOGIN_BUTTON_ATTR = "login-katie-button-enabled";
    private static final String DOMAIN_SLACK_ANSWER_BUTTON_ATTR = "answer-question-button-enabled";
    private static final String DOMAIN_SLACK_SUBDOMAIN_ATTR = "subdomain";

    private static final String IAM_USER_TAG = "user";
    private static final String IAM_USER_UUID_ATTR = "uuid";

    private static final String DOMAIN_MEMBERS_TAG = "users";
    private static final String DOMAIN_MEMBER_TAG = "user";
    private static final String DOMAIN_MEMBER_ROLE_ATTR = "role";
    private static final String DOMAIN_MEMBER_EXPERT_TAG = "expert";
    private static final String DOMAIN_MEMBER_MODERATOR_TAG = "moderator";

    private static final String FAQ_NAMESPACE_1_0_0 = "http://www.wyona.com/askkatie/1.0.0";
    private static final String FAQ_TAG = "faq-by-topic";
    private static final String FAQ_TOPIC_TAG = "topic";
    private static final String FAQ_TOPIC_TITLE_TAG = "title";
    private static final String FAQ_TOPIC_ID_ATTR = "id";
    private static final String FAQ_TOPIC_VISIBILITY_ATTR = "visibility";
    private static final String FAQ_TOPIC_QUESTIONS_TAG = "questions";
    private static final String FAQ_QNA_TAG = "qna";
    private static final String FAQ_QNA_ID_ATTR = "id";
    private static final String FAQ_QNA_QUESTION_TAG = "question";
    private static final String FAQ_QNA_ANSWER_TAG = "answer";

    private static final String DOMAINS_MY_KATIE_ATTR = "my-katie";
    private static final String DOMAINS_TAG = "domains";

    private static final String WEBHOOK_ENABLED = "enabled";

    private static final String THREAD_MESSAGE_TAG = "message";

    /**
     * @param id Domain Id
     */
    private File getDomainDirectory(String id) {
        return new File(contextsDataPath, id);
    }

    /**
     * Get XML File containing members of a particular domain
     */
    private File getDomainMembersConfig(String domainId) {
        return new File(getDomainDirectory(domainId), "users.xml");
    }

    /**
     * Get XML File containing webhooks of a particular domain
     */
    private File getWebhooksConfig(String domainId) {
        return new File(getDomainDirectory(domainId),"webhooks.xml");
    }

    /**
     * Get XML File containing webhooks delivery log of a particular domain
     */
    private File getWebhooksLog(String domainId) {
        return new File(getDomainDirectory(domainId),"webhooks-deliveries.xml");
    }

    /**
     * Remove user / member from a particular domain
     * @param domainId Domain Id
     * @param userId User Id of member
     */
    protected void removeDomainMember(String domainId, String userId) throws Exception {
        File config = getDomainMembersConfig(domainId);
        log.info("Remove member '" + userId + "' from domain users file '" + config.getAbsolutePath() + "' ...");
        Document doc = read(config);
        doc.getDocumentElement().normalize();

        NodeList userNL = doc.getElementsByTagName(DOMAIN_MEMBER_TAG);
        for (int i = 0; i < userNL.getLength(); i++) {
            Element userEl = (Element) userNL.item(i);
            String id = userEl.getAttribute("uuid");
            log.debug("User Id: " + id);
            if (id.equals(userId)) {
                doc.getDocumentElement().removeChild(userEl);
                break;
            }
        }

        save(doc, config);
    }

    /**
     * Add user as member to a particular domain
     * @oaram userId User Id
     * @param domainId Domain Id
     */
    protected void addDomainMember(String userId, boolean isExpert, boolean isModerator, RoleDomain role, String domainId) throws Exception {
        File config = getDomainMembersConfig(domainId);
        if (!config.isFile()) {
            saveMembersConfig(domainId);
        }
        Document doc = read(config);
        doc.getDocumentElement().normalize();

        if (isUserMemberOfDomain(userId, domainId)) {
            throw new Exception("User '" + userId + "' is already member of domain '" + domainId + "'.");
        }

        Element userEl = doc.createElement(DOMAIN_MEMBER_TAG);
        doc.getDocumentElement().appendChild(userEl);
        userEl.setAttribute("uuid", userId);

        if (role != null) {
            userEl.setAttribute(DOMAIN_MEMBER_ROLE_ATTR, "" + role);
        }

        if (isExpert) {
            Element expertEl = doc.createElement(DOMAIN_MEMBER_EXPERT_TAG);
            userEl.appendChild(expertEl);
        }

        if (isModerator) {
            Element moderatorEl = doc.createElement(DOMAIN_MEMBER_MODERATOR_TAG);
            userEl.appendChild(moderatorEl);
        }

        save(doc, config);
    }

    /**
     * Update a particular member of a specific domain
     * @param domainId Domain Id
     */
    public void updateDomainMember(User user, String domainId) throws Exception {
        File config = getDomainMembersConfig(domainId);
        if (!config.isFile()) {
            throw new FileNotFoundException("No such file '" + config.getAbsolutePath() + "'!");
        }
        Document doc = read(config);
        doc.getDocumentElement().normalize(); // TODO: Is this necessary?!

        Element userEl = getUserElementById(doc, user.getId());

        Element moderatorEl = getChildByTagName(userEl, DOMAIN_MEMBER_MODERATOR_TAG, false);
        if (moderatorEl != null) {
            if (!user.getIsModerator()) {
                userEl.removeChild(moderatorEl);
            }
        } else {
            if (user.getIsModerator()) {
                moderatorEl = doc.createElement(DOMAIN_MEMBER_MODERATOR_TAG);
                userEl.appendChild(moderatorEl);
            }
        }

        Element expertEl = getChildByTagName(userEl, DOMAIN_MEMBER_EXPERT_TAG, false);
        if (expertEl != null) {
            if (!user.getIsExpert()) {
                userEl.removeChild(expertEl);
            }
        } else {
            if (user.getIsExpert()) {
                expertEl = doc.createElement(DOMAIN_MEMBER_EXPERT_TAG);
                userEl.appendChild(expertEl);
            }
        }

        save(doc, config);
    }

    /**
     * Get child by tag name, but only when it is direct child
     * @param parent Parent element
     * @param name Tag name of direct child
     * @return direct child element and null when there is no such direct child element
     */
    protected Element getDirectChildByTagName(Element parent, String name) throws Exception {
        NodeList nl = parent.getChildNodes();
        if (nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element child = (Element)nl.item(i);
                    if (child.getTagName().equals(name)) {
                        return (Element) child;
                    }
                }
            }
            log.debug("Parent element '" + parent.getTagName() + "' has no such child '" + name + "'.");
            return null;
        } else {
            log.warn("Parent element '" + parent.getTagName() + "' has no children at all.");
            return null;
        }
    }

    /**
     * Get all children with the same tag name, but only when they are direct children of the provided parent element
     * @param parent Parent element
     * @param name Tag name of direct child
     * @return all direct child elements and null when there is no such direct child elements
     */
    protected List<Element> getDirectChildrenByTagName(Element parent, String name) throws Exception {
        List<Element> directChildren = new ArrayList<Element>();
        NodeList nl = parent.getChildNodes();
        if (nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element child = (Element)nl.item(i);
                    if (child.getTagName().equals(name)) {
                        directChildren.add((Element) child);
                    }
                }
            }
            return directChildren;
        } else {
            log.warn("Parent element '" + parent.getTagName() + "' has no children at all.");
            return null;
        }
    }

    /**
     * Get child by tag name, whereas from all levels
     * @param parent Parent element
     * @param name Tag name of child
     * @param create If set to true, then create child element when child element does not exist yet
     * @return child element if it exists and null otherwise
     */
    private Element getChildByTagName(Element parent, String name, boolean create) {
        NodeList nl = parent.getElementsByTagName(name);
        if (nl.getLength() == 1) {
            return (Element) nl.item(0);
        } else if (nl.getLength() > 1) {
            log.warn("There are more than one child elements with name '" + name + "'!");
            return (Element) nl.item(0);
        } else {
            log.debug("Parent element '" + parent.getTagName() + "' does not have child '" + name + "'.");
            if (create) {
                Element child = parent.getOwnerDocument().createElement(name);
                parent.appendChild(child);
                return child;
            } else {
                return null;
            }
        }
    }

    /**
     *
     */
    private Element getUserElementById(Document doc, String userId) {
        NodeList userNL = doc.getElementsByTagName(IAM_USER_TAG);
        for (int i = 0; i < userNL.getLength(); i++) {
            Element userEl = (Element)userNL.item(i);
            String id = userEl.getAttribute(IAM_USER_UUID_ATTR);
            log.debug("User Id: " + id);
            if (id.equals(userId)) {
                return userEl;
            }
        }
        log.error("No such user with user Id '" + userId + "'!");
        return null;
    }

    /**
     * @return appended Element if value is not null, otherwise do not append element and return null
     */
    protected Element appendElement(Element parent, String childName, String childValue) {
        if (childValue != null) {
            Element childElement = parent.getOwnerDocument().createElement(childName);
            parent.appendChild(childElement);
            childElement.setTextContent(childValue);
            return childElement;
        } else {
            return null;
        }
    }

    /**
     * Create XML file containing meta information re QnAs extraction
     */
    public URLMeta createUrlMeta(File file, String contentUrl, String webUrl, long date, ContentType contentType) {
        Document doc = createDocument(KATIE_NAMESPACE_1_0_0, "meta");
        doc.getDocumentElement().setAttribute("url", contentUrl);
        doc.getDocumentElement().setAttribute("web-url", webUrl);
        doc.getDocumentElement().setAttribute("date", "" + date);
        if (contentType != null) {
            doc.getDocumentElement().setAttribute("content-type", contentType.toString());
        }
        save(doc, file);

        return new URLMeta(contentUrl, date, contentType);
    }

    /**
     *
     */
    public URLMeta getUrlMeta(File file) throws Exception {
        Document doc = read(file);
        String url = doc.getDocumentElement().getAttribute("url");
        long importDate = Long.parseLong(doc.getDocumentElement().getAttribute("date"));
        ContentType contentType = null;
        if (doc.getDocumentElement().hasAttribute("content-type")) {
            contentType = ContentType.fromString(doc.getDocumentElement().getAttribute("content-type"));
        }
        URLMeta urlMeta = new URLMeta(url, importDate, contentType);
        return urlMeta;
    }

    /**
     * Create FAQ for a particular domain and language, but only topics
     */
    public Topic[] createFAQ(Context domain, String language, Topic[] topics) throws Exception {
        Document doc = createDocument(FAQ_NAMESPACE_1_0_0, FAQ_TAG);
        for (int i = 0; i < topics.length; i++) {
            Topic topic = topics[i];
            if (topic.getId() == null) {
                topic.setId(UUID.randomUUID().toString());
                topic.setVisibility(TopicVisibility.PRIVATE);
            }

            Element topicEl = doc.createElement(FAQ_TOPIC_TAG);
            doc.getDocumentElement().appendChild(topicEl);

            log.debug("Add topic '" + topic.getTitle() + "' (" + topic.getId() + ") ...");
            topicEl.setAttribute(FAQ_TOPIC_ID_ATTR, topic.getId());
            if (topic.getVisibility() != null && topic.getVisibility().equals(TopicVisibility.PRIVATE)) {
                topicEl.setAttribute(FAQ_TOPIC_VISIBILITY_ATTR, "private");
            }

            Element titleEl = doc.createElement(FAQ_TOPIC_TITLE_TAG);
            titleEl.setTextContent(topic.getTitle());
            topicEl.appendChild(titleEl);

            com.wyona.katie.models.faq.Question[] questions = topic.getQuestions();
            Element questionsEl = doc.createElement(FAQ_TOPIC_QUESTIONS_TAG);
            topicEl.appendChild(questionsEl);
        }

        File file = domain.getFAQXmlDataPath(language);
        if (!file.getParentFile().isDirectory()) {
            file.getParentFile().mkdir();
        }
        save(doc, file);

        log.info("FAQ for domain '" + domain.getId() + "' and language '" + language + "' has been created persistently.");

        return topics;
    }

    /**
     * Create FAQ for a particular domain and language
     * @param language Language of faq, e.g. "de" or "en"
     */
    public FAQ createFAQ(Context domain, String language, FAQ faq) {
        Document doc = createDocument(FAQ_NAMESPACE_1_0_0, FAQ_TAG);
        Topic[] topics = faq.getTopics();
        for (int i = 0; i < topics.length; i++) {
            Topic topic = topics[i];
            if (topic.getId() == null) {
                topic.setId(UUID.randomUUID().toString());
                topic.setVisibility(TopicVisibility.PRIVATE);
            }

            Element topicEl = doc.createElement(FAQ_TOPIC_TAG);
            doc.getDocumentElement().appendChild(topicEl);

            log.debug("Add topic '" + topic.getTitle() + "' (" + topic.getId() + ") ...");
            topicEl.setAttribute(FAQ_TOPIC_ID_ATTR, topic.getId());
            if (topic.getVisibility() != null && topic.getVisibility().equals(TopicVisibility.PRIVATE)) {
                topicEl.setAttribute(FAQ_TOPIC_VISIBILITY_ATTR, "private");
            }

            Element titleEl = doc.createElement(FAQ_TOPIC_TITLE_TAG);
            titleEl.setTextContent(topic.getTitle());
            topicEl.appendChild(titleEl);

            com.wyona.katie.models.faq.Question[] questions = topic.getQuestions();
            Element questionsEl = doc.createElement(FAQ_TOPIC_QUESTIONS_TAG);
            topicEl.appendChild(questionsEl);

            log.info("Topic '" + topic.getTitle() + "' contains " + questions.length + " questions.");
            for (int k = 0; k < questions.length; k++) {
                com.wyona.katie.models.faq.Question question = questions[k];
                Element qnaEl = doc.createElement(FAQ_QNA_TAG);
                qnaEl.setAttribute(FAQ_QNA_ID_ATTR, question.getUuid());
                questionsEl.appendChild(qnaEl);
            }
        }

        File file = domain.getFAQXmlDataPath(language);
        if (!file.getParentFile().isDirectory()) {
            file.getParentFile().mkdir();
        }
        save(doc, file);

        return faq;
    }

    /**
     * Get FAQ from XML file
     * @param domain Domain associated with FAQ
     * @param language Language of FAQ
     * @param publicOnly When true, then only public topics are being returned
     */
    protected FAQ getFAQ(Context domain, String language, boolean publicOnly) throws Exception {
        Document doc = read(domain.getFAQXmlDataPath(language));
        return getFAQ(doc, publicOnly);
    }

    /**
     * Get FAQ from InputStream
     * @param in InputStream
     */
    protected FAQ getFAQ(InputStream in) throws Exception {
        Document doc = read(in);
        return getFAQ(doc, false);
    }

    /**
     * Get FAQ from XML document
     * @param doc XML document containing FAQs
     * @param publicOnly When true, then only public topics are being returned
     */
    private FAQ getFAQ(Document doc, boolean publicOnly) throws Exception {
        FAQ faq = new FAQ();

        NodeList topicNL = doc.getElementsByTagName(FAQ_TOPIC_TAG);
        for (int i = 0; i < topicNL.getLength(); i++) {
            Element topicEl = (Element)topicNL.item(i);
            Element titleEl = getChildByTagName(topicEl, FAQ_TOPIC_TITLE_TAG, false);

            Element iconEl = getChildByTagName(topicEl, "icon", false);
            Icon icon = null;
            if (iconEl != null) {
                new Icon(iconEl.getAttribute("code"), iconEl.getAttribute("name"));
            } else {
                log.info("Topic '" + titleEl.getTextContent() + "' does not have an icon.");
            }

            TopicVisibility visibility = TopicVisibility.PUBLIC;
            if (topicEl.hasAttribute(FAQ_TOPIC_VISIBILITY_ATTR)) {
                String v = topicEl.getAttribute(FAQ_TOPIC_VISIBILITY_ATTR);
                if (v.equals("public")) {
                    visibility = TopicVisibility.PUBLIC;
                } else if (v.equals("private")) {
                    visibility = TopicVisibility.PRIVATE;
                } else {
                    log.error("No such topic visibility '" + v + "' supported!");
                    visibility = TopicVisibility.PRIVATE;
                }
                //visibility = TopicVisibility.valueOf(topicEl.getAttribute(FAQ_TOPIC_VISIBILITY_ATTR));
            }

            String topicId = null;
            if (topicEl.hasAttribute(FAQ_TOPIC_ID_ATTR)) {
                topicId = topicEl.getAttribute(FAQ_TOPIC_ID_ATTR);
            }
            Topic topic = new Topic(topicId, titleEl.getTextContent(), icon, visibility);

            if (publicOnly && visibility.equals("private")) {
                log.info("Do not add topic '" + topic.getTitle() + "', because topic is private.");
            } else {
                Element questionsEl = getChildByTagName(topicEl, FAQ_TOPIC_QUESTIONS_TAG, false);

                NodeList questionsNL = questionsEl.getElementsByTagName(FAQ_QNA_TAG);
                for (int k = 0; k < questionsNL.getLength(); k++) {
                    Element qnaEl = (Element) questionsNL.item(k);
                    String qUUID = qnaEl.getAttribute(FAQ_QNA_ID_ATTR);

                    if (qUUID != null && qUUID.length() > 0) {
                        log.debug("QnA element has UUID attribute '" + qUUID + "'.");
                        topic.addQuestion(qUUID, null, null);
                    } else {
                        log.info("QnA element has no UUID attribute");

                        Element questionEl = getChildByTagName(qnaEl, FAQ_QNA_QUESTION_TAG, false);
                        String question = null;
                        if (questionEl != null) {
                            question = questionEl.getTextContent();
                        }

                        Element answerEl = getChildByTagName(qnaEl, FAQ_QNA_ANSWER_TAG, false);
                        String answer = null;
                        if (answerEl != null) {
                            log.debug("Get answer as rich text ...");
                            answer = removeTag(FAQ_QNA_ANSWER_TAG, convertNodeToString(answerEl));
                        }

                        if (question != null || answer != null) {
                            topic.addQuestion(qUUID, question, answer);
                        } else {
                            log.warn("QnA element has neither UUID attribute, nor question or answer element!");
                        }
                    }
                }

                faq.addTopic(topic);
            }
        }

        return faq;
    }

    /**
     * Add QnA to FAQ
     * @param domain Domain associated with FAQ
     * @param language Language of FAQ
     * @param topicId Topic Id, e.g. "slack_integration" or "legal"
     * @param uuid UUID of QnA
     */
    public void addQnAToFAQ(Context domain, String language, String topicId, String uuid) throws Exception {
        File faqFile = domain.getFAQXmlDataPath(language);
        log.info("Add QnA '" + uuid + "' to '" + faqFile.getAbsolutePath() + "' ...");

        Document doc = read(faqFile);

        NodeList topicNL = doc.getElementsByTagName(FAQ_TOPIC_TAG);
        for (int i = 0; i < topicNL.getLength(); i++) {
            Element topicEl = (Element) topicNL.item(i);
            if (topicEl.getAttribute(FAQ_TOPIC_ID_ATTR).equals(topicId)) {
                Element questionsEl = getChildByTagName(topicEl, FAQ_TOPIC_QUESTIONS_TAG, false);
                Element qnaEl = doc.createElement(FAQ_QNA_TAG);
                qnaEl.setAttribute(FAQ_QNA_ID_ATTR, uuid);
                questionsEl.appendChild(qnaEl);
                save(doc, domain.getFAQXmlDataPath(language));
                return;
            }
        }

        log.warn("FAQ '" + faqFile.getAbsolutePath() + "' does not contain topic '" + topicId + "'!");
    }

    /**
     * Read XML file
     * @param file File containing XML
     * @return DOM
     */
    protected Document read(File file) throws Exception {
        log.debug("Read XML from file '" + file.getAbsolutePath() + "' ...");
        FileInputStream in = new FileInputStream(file);
        Document doc = read(in);
        in.close();
        return doc;
    }

    /**
     * Parse input stream containing XML
     * @return DOM
     */
    private Document read(InputStream in) throws Exception {
        log.info("Init document builder factory ...");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);

        log.debug("Init DocumentBuilder ...");
        DocumentBuilder builder = factory.newDocumentBuilder();
        //DocumentBuilder builder = createBuilder(false);

        log.debug("Parse input stream ...");
        Document doc = builder.parse(in);
        return doc;
    }

    /**
     * Read XML from string
     */
    private Document read(String xml) throws Exception {
        log.debug("Read XML from String: " + xml);
        InputStream in = new ByteArrayInputStream(xml.getBytes());
        Document doc = read(in);
        in.close();
        return doc;
    }

    /**
     * Save DOM as XML file
     */
    protected void save(Document doc, File xmlFile) {
        try {
            FileOutputStream out = new FileOutputStream(xmlFile);
            writeDocument(doc, out, true);
            out.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Save question/answer persistently as XML file
     */
    public void saveQuestionAnswer(Answer qna, File xmlFile) {
        try {
            Document qnaDoc = createQADocument(qna);
            FileOutputStream out = new FileOutputStream(xmlFile);
            writeDocument(qnaDoc, out, true);
            out.close();

            Document ratingsDoc = createRatingsDocument(qna);
            if (ratingsDoc != null) {
                FileOutputStream outRatings = new FileOutputStream(new File(xmlFile.getParent(), "ratings.xml"));
                writeDocument(ratingsDoc, outRatings, true);
                outRatings.close();
            } else {
                log.warn("No ratings!");
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Save ratings persistently as XML file
     */
    public void saveRatings(Answer qna, File xmlFile) {
        try {
            Document ratingsDoc = createRatingsDocument(qna);
            if (ratingsDoc != null) {
                FileOutputStream outRatings = new FileOutputStream(xmlFile);
                writeDocument(ratingsDoc, outRatings, true);
                outRatings.close();
            } else {
                log.warn("No ratings!");
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Save thread message persistently as XML file
     * @param threadId Channel dependent thread Id, e.g. "1084436366051528765" in the case of Discord
     * @param message Thread message, e.g. "Michael has 3 sons"
     */
    public void saveThreadMessage(String threadId, String message, File xmlFile) {
        try {
            FileOutputStream out = new FileOutputStream(xmlFile);

            org.w3c.dom.Document qaDoc = createThreadMessageDocument(threadId, message);

            writeDocument(qaDoc, out, true);
            out.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Get thread message from XML file
     */
    public String readThreadMessage(File xmlFile) throws Exception {
        Document doc = read(xmlFile);

        String threadId = null;

        if (doc.getDocumentElement().hasAttribute("thread-id")) {
            threadId = doc.getDocumentElement().getAttribute("thread-id");
        }
        String message = getDirectChildByTagName(doc.getDocumentElement(), THREAD_MESSAGE_TAG).getTextContent();

        // TODO: Return thread message object (including thread Id, etc.)

        return message;
    }

    /**
     * @return true when user is member of a particular domain and false otherwise
     */
    public boolean isUserMemberOfDomain(String userId, String domainId) throws Exception {
        File config = getDomainMembersConfig(domainId);
        if (!config.isFile()) {
            log.error("No domain users configuration file exists: " + config.getAbsolutePath());
            return false;
        }

        Document doc = read(config);
        doc.getDocumentElement().normalize();

        NodeList userNL = doc.getElementsByTagName(DOMAIN_MEMBER_TAG);
        for (int i = 0; i < userNL.getLength(); i++) {
            Element userEl = (Element) userNL.item(i);
            String id = userEl.getAttribute("uuid");
            log.debug("User Id: " + id);
            if (id.equals(userId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get a particular member of a specific domain
     */
    public User getMember(User user, String domainId) throws Exception {
        File config = getDomainMembersConfig(domainId);
        if (!config.isFile()) {
            log.error("No domain users configuration file exists: " + config.getAbsolutePath());
            return null;
        }

        Document doc = read(config);
        doc.getDocumentElement().normalize();

        NodeList userNL = doc.getElementsByTagName(DOMAIN_MEMBER_TAG);
        List<User> users = new ArrayList<User>();
        for (int i = 0; i < userNL.getLength(); i++) {
            Element userEl = (Element) userNL.item(i);
            String id = userEl.getAttribute("uuid");
            log.debug("User Id: " + id);
            if (id.equals(user.getId())) {
                user.setIsModerator(isModerator(userEl));
                user.setIsExpert(isExpert(userEl));
                return user;
            }
        }

        log.info("Domain '" + domainId + "' does not contain member with user Id '" + user.getId() + "'.");
        return null;
    }

    /**
     * Get domain role of a particular member of a specific domain
     */
    public RoleDomain getRole(String userId, String domainId) throws Exception {
        File config = getDomainMembersConfig(domainId);
        if (!config.isFile()) {
            log.error("No domain users configuration file exists: " + config.getAbsolutePath());
            return null;
        }

        Document doc = read(config);
        doc.getDocumentElement().normalize();

        NodeList userNL = doc.getElementsByTagName(DOMAIN_MEMBER_TAG);
        List<User> users = new ArrayList<User>();
        for (int i = 0; i < userNL.getLength(); i++) {
            Element userEl = (Element) userNL.item(i);
            String id = userEl.getAttribute("uuid");
            log.debug("User Id: " + id);
            if (id.equals(userId)) {
                if (userEl.hasAttribute(DOMAIN_MEMBER_ROLE_ATTR)) {
                    return RoleDomain.valueOf(userEl.getAttribute(DOMAIN_MEMBER_ROLE_ATTR));
                } else {
                    log.info("No domain role configured for member with user Id '" + id + "' of domain '" + domainId + "'.");
                    return null;
                }
            }
        }

        log.info("Domain '" + domainId + "' does not contain member with user Id '" + userId + "'.");
        return null;
    }

    /**
     * Check whether member is moderator
     * @return true when member is moderator and false otherwise
     */
    private boolean isModerator(Element userEl) {
        NodeList moderatorNL = userEl.getElementsByTagName(DOMAIN_MEMBER_MODERATOR_TAG);
        if (moderatorNL.getLength() == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check whether member is expert
     * @return true when member is expert and false otherwise
     */
    private boolean isExpert(Element userEl) {
        NodeList expertNL = userEl.getElementsByTagName(DOMAIN_MEMBER_EXPERT_TAG);
        if (expertNL.getLength() == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get webhooks
     */
    public Webhook[] getWebhooks(String domainId) throws Exception {
        File config = getWebhooksConfig(domainId);
        if (!config.isFile()) {
            log.warn("No domain webhooks configuration file exists: " + config.getAbsolutePath());
            return new Webhook[0];
        }
        Document doc = read(config);
        doc.getDocumentElement().normalize();

        NodeList webhookNL = doc.getElementsByTagName("webhook");
        List<Webhook> webhooks = new ArrayList<Webhook>();
        for (int i = 0; i < webhookNL.getLength(); i++) {
            Element webhookEl = (Element)webhookNL.item(i);

            String id = webhookEl.getAttribute("id");

            String url = webhookEl.getAttribute("payload-url");
            log.debug("Payload URL: " + url);

            boolean isEnabled = Boolean.parseBoolean(webhookEl.getAttribute(WEBHOOK_ENABLED));

            Webhook webhook = new Webhook(id, url, isEnabled);

            if (webhookEl.hasAttribute("trigger-events")) {
                String[] triggerEvents = webhookEl.getAttribute("trigger-events").split(",");
                if (triggerEvents != null && triggerEvents.length > 0) {
                    for (String triggerEvent : triggerEvents) {
                        if (!triggerEvent.trim().isEmpty()) {
                            log.debug("Trigger event: " + triggerEvent.trim());
                            try {
                                WebhookTriggerEvent event = WebhookTriggerEvent.valueOf(triggerEvent.trim());
                                webhook.add(event);
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                            }
                        } else {
                            log.warn("Webhook '" + webhook.getId() + "' of Domain '" + domainId + "' contains empty trigger event!");
                        }
                    }
                }
            }

            if (webhookEl.hasAttribute("content-type")) {
                webhook.setContentType(webhookEl.getAttribute("content-type"));
            }

            if (webhookEl.hasAttribute("api-key")) {
                webhook.setApiKey(webhookEl.getAttribute("api-key"));
            }

            webhooks.add(webhook);
        }

        return webhooks.toArray(new Webhook[0]);
    }

    /**
     * Delete webhook
     */
    public void deleteWebhook(String domainId, String webhookId) throws Exception {
        File config = getWebhooksConfig(domainId);
        if (!config.isFile()) {
            log.warn("No domain webhooks configuration file exists: " + config.getAbsolutePath());
            return;
        }
        Document doc = read(config);
        doc.getDocumentElement().normalize();

        NodeList webhookNL = doc.getElementsByTagName("webhook");
        for (int i = 0; i < webhookNL.getLength(); i++) {
            Element webhookEl = (Element)webhookNL.item(i);
            String id = webhookEl.getAttribute("id");
            if (id.equals(webhookId)) {
                // TBD: Also delete deliveries
                doc.getDocumentElement().removeChild(webhookEl);
                save(doc, config);
                return;
            }
        }
    }

    /**
     * Toggle webhook whether to be active or inactive
     */
    public void toggleWebhook(String domainId, String webhookId) throws Exception {
        File config = getWebhooksConfig(domainId);
        if (!config.isFile()) {
            log.warn("No domain webhooks configuration file exists: " + config.getAbsolutePath());
            return;
        }
        Document doc = read(config);
        doc.getDocumentElement().normalize();

        NodeList webhookNL = doc.getElementsByTagName("webhook");
        for (int i = 0; i < webhookNL.getLength(); i++) {
            Element webhookEl = (Element)webhookNL.item(i);
            String id = webhookEl.getAttribute("id");
            if (id.equals(webhookId)) {
                boolean isActive = Boolean.parseBoolean(webhookEl.getAttribute(WEBHOOK_ENABLED));
                boolean isActiveToggled = !isActive;
                webhookEl.setAttribute(WEBHOOK_ENABLED, "" + isActiveToggled);
                save(doc, config);
                return;
            }
        }
    }

    /**
     * Add webhook
     */
    public void addWebhook(String domainId, Webhook webhook) throws Exception {
        Document doc = null;

        File config = getWebhooksConfig(domainId);
        if (!config.isFile()) {
            log.warn("No domain webhooks configuration file exists yet, therefore create one: " + config.getAbsolutePath());
            doc = createDocument(KATIE_NAMESPACE_1_0_0, "webhooks");
        } else {
            doc = read(config);
        }

        doc.getDocumentElement().normalize();

        Element webhookEl = doc.createElement("webhook");
        webhookEl.setAttribute("id", webhook.getId());
        webhookEl.setAttribute("content-type", webhook.getContentType());
        webhookEl.setAttribute("payload-url", webhook.getPayloadURL());
        webhookEl.setAttribute(WEBHOOK_ENABLED, "" + webhook.getEnabled());

        //webhookEl.setAttribute("api-key", webhook.getApiKey());
        //webhookEl.setAttribute("method", webhook.getMethod());

        doc.getDocumentElement().appendChild(webhookEl);

        save(doc, config);
    }

    /**
     * Get deliveries of a particular webhook
     */
    public WebhookRequest[] getWebhookDeliveries(String domainId, String webhookId) throws Exception {
        File config = getWebhooksLog(domainId);
        if (!config.isFile()) {
            log.warn("No domain webhooks log file exists: " + config.getAbsolutePath());
            return new WebhookRequest[0];
        }
        Document doc = read(config);
        doc.getDocumentElement().normalize();

        NodeList webhookNL = doc.getElementsByTagName("webhook");
        List<WebhookRequest> requests = new ArrayList<WebhookRequest>();
        for (int i = 0; i < webhookNL.getLength(); i++) {
            Element webhookEl = (Element)webhookNL.item(i);
            String id = webhookEl.getAttribute("id");
            if (id.equals(webhookId)) {
                NodeList requestkNL = webhookEl.getElementsByTagName("request");
                for (int k = 0; k < requestkNL.getLength(); k++) {
                    Element requestEl = (Element)requestkNL.item(k);
                    long sentAt = Long.parseLong(requestEl.getAttribute("sent-at"));
                    int statusCode = Integer.parseInt(requestEl.getAttribute("status-code"));
                    WebhookRequest request = new WebhookRequest(webhookId, sentAt, statusCode);
                    requests.add(request);
                }
                break;
            }
        }

        return requests.toArray(new WebhookRequest[0]);
    }

    /**
     * Log webhook delivery (timestamp sent, status code)
     */
    public void logWebhookDelivery(String domainId, Webhook webhook, int statusCode, long timestampSent) throws Exception {
        File logFile = getWebhooksLog(domainId);

        Document doc = null;
        Element webhookEl = null;

        if (logFile.isFile()) {
            log.info("Read existing webhhooks log file: " + logFile.getAbsolutePath());
            doc = read(logFile);
            doc.getDocumentElement().normalize();

            NodeList webhooks = doc.getElementsByTagName("webhook");
            for (int i = 0; i < webhooks.getLength(); i++) {
                Element whEl = (Element)webhooks.item(i);
                if (whEl.getAttribute("id").equals(webhook.getId())) {
                    webhookEl = whEl;
                    break;
                }
            }
        } else {
            log.info("Create new webhooks log file: " + logFile.getAbsolutePath());
            doc = createDocument(KATIE_NAMESPACE_1_0_0, "webhooks-deliveries");
        }

        if (webhookEl == null) {
            webhookEl = doc.createElement("webhook");
            webhookEl.setAttribute("id", webhook.getId());
            doc.getDocumentElement().appendChild(webhookEl);
        }

        Element requestEl = doc.createElement("request");
        requestEl.setAttribute("status-code", "" + statusCode);
        requestEl.setAttribute("sent-at", "" + timestampSent);
        webhookEl.appendChild(requestEl);

        save(doc, logFile);
    }

    /**
     * Parse members configuration file of domain
     * @param domainId Domain Id
     * @param onlyExperts If true, then only return users which are experts
     * @param onlyModerators If true, then only return users which are moderators
     * @return users, including user Id, user role, but without first or last name, etc.
     */
    public User[] getMembers(String domainId, boolean onlyExperts, boolean onlyModerators) throws Exception {
        File config = getDomainMembersConfig(domainId);
        if (!config.isFile()) {
            log.warn("No domain users configuration file exists: " + config.getAbsolutePath());
            return new User[0];
        }

        Document doc = read(config);
        doc.getDocumentElement().normalize();

        NodeList userNL = doc.getElementsByTagName(DOMAIN_MEMBER_TAG);
        List<User> users = new ArrayList<User>();
        for (int i = 0; i < userNL.getLength(); i++) {
            Element userEl = (Element)userNL.item(i);
            String id = userEl.getAttribute("uuid");
            log.debug("User Id: " + id);

            boolean memberIsModerator = isModerator(userEl);

            boolean memberIsExpert = isExpert(userEl);

            boolean locked = false;
            // TODO: Check whether member is locked

            boolean approved = true;
            // TODO: Check whether user account was approved

            if (onlyExperts && !memberIsExpert) {
                log.debug("Member '" + id + "' is not an expert.");
                continue;
            }
            if (onlyModerators && !memberIsModerator) {
                log.debug("Member '" + id + "' is not a moderator.");
                continue;
            }
            RoleDomain domainRole = null;
            if (userEl.hasAttribute(DOMAIN_MEMBER_ROLE_ATTR)) {
                domainRole = RoleDomain.valueOf(userEl.getAttribute(DOMAIN_MEMBER_ROLE_ATTR));
            }

            // TODO: Resolve member/user values
            String language = "en";

            Date created = null;
            // TODO: Get date when user account of member was created
            users.add(new User(id, null, null, null, null, null, null, null, null, memberIsExpert, memberIsModerator, domainRole, language, locked, approved, created));
        }

        return users.toArray(new User[0]);
    }

    /**
     *
     */
    protected void setPersonalDomainId(File domainsFile, String domainId) throws Exception {
        Document doc = read(domainsFile);
        NodeList idNL = doc.getElementsByTagName("id");
        for (int i = 0; i < idNL.getLength(); i++) {
            Element idEl = (Element)idNL.item(i);
            String id = idEl.getTextContent();
            if (id.equals(domainId)) {
                doc.getDocumentElement().setAttribute(DOMAINS_MY_KATIE_ATTR, domainId);
                save(doc, domainsFile);
                return;
            }
        }

        throw new Exception("Domain list '" + domainsFile.getAbsolutePath()  + "' does not contain domain '" + domainId + "'!");
    }

    /**
     * @param domainsFile XML file containing list of domain Ids where user is member of
     * @return MyKatie domain Id of user and null if MyKatie domain Id is not configured for user
     */
    protected String getPersonalDomainId(File domainsFile) throws Exception {
        Document doc = read(domainsFile);
        if (doc.getDocumentElement().hasAttribute(DOMAINS_MY_KATIE_ATTR)) {
            return doc.getDocumentElement().getAttribute(DOMAINS_MY_KATIE_ATTR);
        }
        log.warn("Domain list '" + domainsFile.getAbsolutePath() + "' does not contain MyKatie domain!");
        return null;
    }

    /**
     * Connect domain Id with user Id where user is member of
     * @param domainsFile XML file containing list of domain Ids where user is member of
     * @param domainId Domain Id where user is member of
     */
    protected void connectDomainWithUser(File domainsFile, String domainId) throws Exception {
        Document doc = null;
        if (domainsFile.isFile()) {
            doc = read(domainsFile);
        } else {
            domainsFile.getParentFile().mkdirs();
            doc = createDocument(KATIE_NAMESPACE_1_0_0, DOMAINS_TAG);
        }

        Element idEl = doc.createElement("id");
        idEl.appendChild(doc.createTextNode(domainId));
        doc.getDocumentElement().appendChild(idEl);

        save(doc, domainsFile);
    }

    /**
     * Create "empty" user domains file
     * @param domainsFile XML file containing list of domain Ids where user is member of
     */
    protected void createUserDomainsFile(File domainsFile) throws Exception {
        if (!domainsFile.isFile()) {
            domainsFile.getParentFile().mkdirs();
            Document doc = createDocument(KATIE_NAMESPACE_1_0_0, DOMAINS_TAG);
            save(doc, domainsFile);
        } else {
            log.warn("User domains file '" + domainsFile.getAbsolutePath() + "' already exists!");
        }
    }

    /**
     * Disconnect domain Id from user Id where user is member of
     * @param domainsFile XML file containing list of domain Ids where user is member of
     * @param domainId Domain Id where user is member of
     */
    protected void disconnectDomainFromUser(File domainsFile, String domainId) throws Exception {
        log.info("Remove domain '" + domainId + "' from user's domain list: " + domainsFile.getAbsolutePath());
        Document doc = null;
        if (domainsFile.isFile()) {
            doc = read(domainsFile);
        } else {
            domainsFile.getParentFile().mkdirs();
            doc = createDocument(KATIE_NAMESPACE_1_0_0, DOMAINS_TAG);
        }

        NodeList idNL = doc.getElementsByTagName("id");
        for (int i = 0; i < idNL.getLength(); i++) {
            Element idEl = (Element) idNL.item(i);
            String id = idEl.getTextContent();
            log.debug("Domain Id: " + id);
            if (id.equals(domainId)) {
                doc.getDocumentElement().removeChild(idEl);
                break;
            }
        }

        save(doc, domainsFile);
    }

    /**
     * @param domainsFile XML file containing list of domain Ids where user is member of
     */
    protected String[] getDomainIDsUserIsMemberOf(File domainsFile) {
        return getIds(domainsFile);
    }

    /**
     * @param file XML file containing IDs
     */
    protected String[] getIds(File file) {
        List<String> ids = new ArrayList<String>();

        try {
            Document doc = read(file);
            NodeList idNL = doc.getElementsByTagName("id");
            for (int i = 0; i < idNL.getLength(); i++) {
                ids.add(idNL.item(i).getTextContent());
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return ids.toArray(new String[0]);
    }

    /**
     *
     */
    private String getRoleAsString(Role role) {
        if (role.equals(Role.ADMIN)) {
            return "ADMIN";
        } else if (role.equals(Role.USER)) {
            return "USER";
        } else {
            log.error("No such role '" + role + "' implemented!");
            return null;
        }
    }

    /**
     * @param domainId Domain Id
     */
    public void saveMembersConfig(String domainId) {
        File config = getDomainMembersConfig(domainId);
        log.info("Save members configuration as XML '" + config.getAbsolutePath() + "' ...");

        Document doc = createDocument(KATIE_NAMESPACE_1_0_0, DOMAIN_MEMBERS_TAG);
        save(doc, config);
    }

    /**
     * @param context Domain object
     */
    public void saveContextConfig(Context context) {
        File config = new File(context.getContextDirectory(), DOMAIN_CONFIG_FILE_NAME);
        log.info("Save domain configuration as XML '" + config.getAbsolutePath() + "' ...");

        Document doc = createDocument(KATIE_NAMESPACE_1_0_0, CONTEXT_ROOT_TAG);
        doc.getDocumentElement().setAttribute(DOMAIN_ID_ATTR, context.getId());

        if (context.getName() != null) {
            doc.getDocumentElement().setAttribute(DOMAIN_NAME_ATTR, context.getName());
        }

        // INFO: Moderation approval
        Element moderationElement = doc.createElement(CONTEXT_MODERATION_TAG);
        moderationElement.setAttribute(CONTEXT_MODERATION_TOGGLE_ATTR, "" + context.getAnswersMustBeApprovedByModerator());
        moderationElement.setAttribute(CONTEXT_MODERATION_INFORM_USER_RE_MODERATION_ATTR, "" + context.getInformUserReModeration());
        doc.getDocumentElement().appendChild(moderationElement);

        if (context.getInformUserNoAnswerAvailable()) {
            Element informEl = doc.createElement(CONTEXT_INFORM_WHEN_NO_ANSWER_TAG);
            informEl.setAttribute(CONTEXT_INFORM_WHEN_NO_ANSWER_ENABLED_ATTR, "" + context.getInformUserNoAnswerAvailable());
            doc.getDocumentElement().appendChild(informEl);
        }

        Element indexSearchPipelineEl = doc.createElement(CONTEXT_INDEX_SEARCH_PIPEILINE_TAG);
        indexSearchPipelineEl.setAttribute(CONTEXT_ANALYZE_MESSAGES_ASK_REST_API, "" + context.getAnalyzeMessagesAskRestApi());
        indexSearchPipelineEl.setAttribute(CONTEXT_CONSIDER_HUMAN_FEEDBACK_ATTR, "" + context.getConsiderHumanFeedback());
        indexSearchPipelineEl.setAttribute(CONTEXT_RE_RANK_ANSWERS_ATTR, "" + context.getReRankAnswers());
        indexSearchPipelineEl.setAttribute(CONTEXT_USE_GENERATIVE_AI_ATTR, "" + context.getGenerateCompleteAnswers());
        indexSearchPipelineEl.setAttribute(CONTEXT_KATIE_SEARCH_ENABLED_ATTR, "" + context.getKatieSearchEnabled());
        if (context.getScoreThreshold() != null) {
            indexSearchPipelineEl.setAttribute(CONTEXT_SCORE_THRESHOLD_ATTR, "" + context.getScoreThreshold());
        }
        if (context.getReRankImpl() == null) {
            indexSearchPipelineEl.setAttribute(CONTEXT_RE_RANK_IMPLEMENTATION_ATTR, "" + reRankDefaultImpl);
        } else {
            indexSearchPipelineEl.setAttribute(CONTEXT_RE_RANK_IMPLEMENTATION_ATTR, "" + context.getReRankImpl());
        }
        if (context.getCompletionConfig(false) == null) {
            indexSearchPipelineEl.setAttribute(CONTEXT_GENERATIVE_AI_IMPLEMENTATION_ATTR, "" + completionDefaultImpl);
        } else {
            CompletionConfig completionConfig = context.getCompletionConfig(false);
            indexSearchPipelineEl.setAttribute(CONTEXT_GENERATIVE_AI_IMPLEMENTATION_ATTR, "" + completionConfig.getCompletionImpl());
            indexSearchPipelineEl.setAttribute(CONTEXT_GENERATIVE_AI_MODEL_ATTR, completionConfig.getModel());
            indexSearchPipelineEl.setAttribute(CONTEXT_GENERATIVE_AI_API_KEY_ATTR, completionConfig.getApiKey());
            if (completionConfig.getHost() != null) {
                indexSearchPipelineEl.setAttribute(CONTEXT_GENERATIVE_AI_HOST_ATTR, completionConfig.getHost());
            } else {
                // Use default host set globally
            }
        }
        doc.getDocumentElement().appendChild(indexSearchPipelineEl);

        Element classificationEl = doc.createElement(CONTEXT_CLASSIFICATION_TAG);
        classificationEl.setAttribute(CONTEXT_CLASSIFIER_IMPL_ATTR, context.getClassifierImpl().toString());
        doc.getDocumentElement().appendChild(classificationEl);

        if (context.getPromptMessages().size() > 0) {
            Element promptMessagesEl = doc.createElement(CONTEXT_GEN_AI_PROMPT_MESSAGES_TAG);
            doc.getDocumentElement().appendChild(promptMessagesEl);
            for (PromptMessage msg : context.getPromptMessages()) {
                Element promptMsgEl = doc.createElement(CONTEXT_GEN_AI_PROMPT_MESSAGE_TAG);
                promptMsgEl.setAttribute(CONTEXT_GEN_AI_PROMPT_MESSAGE_ROLE_ATTR, msg.getRole().toString());
                promptMsgEl.setTextContent(msg.getContent());
                promptMessagesEl.appendChild(promptMsgEl);
            }
        }

        if (context.getNerImpl() != null && !context.getNerImpl().equals(NerImpl.DO_NOT_ANALYZE)) {
            Element nerElement = doc.createElement(CONTEXT_NER_TAG);
            nerElement.setAttribute(CONTEXT_NER_IMPL_ATTR, "" + context.getNerImpl());
            doc.getDocumentElement().appendChild(nerElement);
        }

        DetectDuplicatedQuestionImpl ddqi = context.getDetectDuplicatedQuestionImpl();

        if (ddqi.equals(DetectDuplicatedQuestionImpl.MCP)) {
            log.info("Add element " + CONTEXT_MCP_TAG);
            Element mcpElement = doc.createElement(CONTEXT_MCP_TAG);
        }

        if (ddqi.equals(DetectDuplicatedQuestionImpl.LLM)) {
            log.info("Add element " + CONTEXT_LLM_SEARCH_TAG);
            Element llmSearchElement = doc.createElement(CONTEXT_LLM_SEARCH_TAG);
            if (context.getLlmSearchAssistantId() != null) {
                llmSearchElement.setAttribute(CONTEXT_LLM_SEARCH_ASSISTANT_ID_ATTR, context.getLlmSearchAssistantId());
            }
            if (context.getLlmSearchAssistantName() != null) {
                llmSearchElement.setAttribute(CONTEXT_LLM_SEARCH_ASSISTANT_NAME_ATTR, context.getLlmSearchAssistantName());
            }
            if (context.getLlmSearchAssistantInstructions() != null) {
                llmSearchElement.setAttribute(CONTEXT_LLM_SEARCH_ASSISTANT_INSTRUCTIONS_ATTR, context.getLlmSearchAssistantInstructions());
            }
            doc.getDocumentElement().appendChild(llmSearchElement);
        }

        // INFO: Katie AI search implementation
        if (ddqi.equals(DetectDuplicatedQuestionImpl.KATIE)) {
            log.info("Add element " + CONTEXT_KATIE_SEARCH_TAG);
            Element katieSearchElement = doc.createElement(CONTEXT_KATIE_SEARCH_TAG);
            // TODO
            doc.getDocumentElement().appendChild(katieSearchElement);
        }

        // INFO: Azure AI Search implementation
        if (ddqi.equals(DetectDuplicatedQuestionImpl.AZURE_AI_SEARCH)) {
            Element azureAISearchEl = doc.createElement(CONTEXT_AZURE_AI_SEARCH_TAG);
            azureAISearchEl.setAttribute(CONTEXT_AZURE_AI_SEARCH_ENDPOINT_ATTR, context.getAzureAISearchEndpoint());
            azureAISearchEl.setAttribute(CONTEXT_AZURE_AI_SEARCH_ADMIN_KEY_ATTR, context.getAzureAISearchAdminKey());
            azureAISearchEl.setAttribute(CONTEXT_AZURE_AI_SEARCH_INDEX_NAME_ATTR, context.getAzureAISearchIndexName());
            doc.getDocumentElement().appendChild(azureAISearchEl);
        }

        // INFO: Generic query service
        if (ddqi.equals(DetectDuplicatedQuestionImpl.QUERY_SERVICE) && context.getQueryServiceUrl() != null) {
            Element qsElement = doc.createElement(CONTEXT_QUERY_SERVICE_TAG);
            Text qsQueryURLText = doc.createTextNode(context.getQueryServiceUrl());
            qsElement.appendChild(qsQueryURLText);
            doc.getDocumentElement().appendChild(qsElement);
        }

        // INFO: Milvus
        if (ddqi.equals(DetectDuplicatedQuestionImpl.MILVUS) && context.getMilvusBaseUrl() != null) {
            Element milvusEl = doc.createElement(CONTEXT_MILVUS_TAG);
            milvusEl.setAttribute(CONTEXT_MILVUS_BASE_URL_ATTR, context.getMilvusBaseUrl());

            // TODO: Make configurable ....
            milvusEl.setAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDINGS_IMPL_ATTR, EmbeddingsImpl.SBERT.toString());
            // TODO: Also set model, etc.

            doc.getDocumentElement().appendChild(milvusEl);
        }

        // INFO: Weaviate
        if (ddqi.equals(DetectDuplicatedQuestionImpl.WEAVIATE) && context.getWeaviateQueryUrl() != null) {
            Element weaviateElement = doc.createElement(CONTEXT_WEAVIATE_TAG);
            Text weaviateQueryURLText = doc.createTextNode(context.getWeaviateQueryUrl());
            weaviateElement.appendChild(weaviateQueryURLText);
            weaviateElement.setAttribute(CONTEXT_WEAVIATE_THRESHOLD_ATTR, "" + context.getWeaviateCertaintyThreshold());
            doc.getDocumentElement().appendChild(weaviateElement);
        }

        // INFO: Knowledge Graph
        if (ddqi.equals(DetectDuplicatedQuestionImpl.KNOWLEDGE_GRAPH) && context.getKnowledgeGraphQueryUrl() != null) {
            Element knowledgeGraphElement = doc.createElement(CONTEXT_KNOWLEDGE_GRAPH_TAG);
            Text knowledgeGraphQueryURLText = doc.createTextNode(context.getKnowledgeGraphQueryUrl());
            knowledgeGraphElement.appendChild(knowledgeGraphQueryURLText);
            doc.getDocumentElement().appendChild(knowledgeGraphElement);
        }

        // INFO: Lucene vector search
        if (ddqi.equals(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH)) {
            Element luceneVectorSearchElement = doc.createElement(CONTEXT_LUCENE_VECTOR_SEARCH_TAG);
            if (context.getEmbeddingsImpl() != null && !context.getEmbeddingsImpl().equals(EmbeddingsImpl.UNSET)) {
                luceneVectorSearchElement.setAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDINGS_IMPL_ATTR, context.getEmbeddingsImpl().toString());
            }
            if (context.getEmbeddingsModel() != null) {
                luceneVectorSearchElement.setAttribute(CONTEXT_VECTOR_SEARCH_MODEL_ATTR, context.getEmbeddingsModel());
            }
            if (context.getEmbeddingValueType() != null) {
                luceneVectorSearchElement.setAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDING_VALUE_TYPE_ATTR, context.getEmbeddingValueType().toString());
            }
            if (context.getEmbeddingsEndpoint() != null) {
                luceneVectorSearchElement.setAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDING_ENDPOINT_ATTR, context.getEmbeddingsEndpoint());
            }
            if (context.getEmbeddingsApiToken() != null) {
                luceneVectorSearchElement.setAttribute(CONTEXT_VECTOR_SEARCH_API_TOKEN_ATTR, context.getEmbeddingsApiToken());
            }
            if (context.getVectorSimilarityMetric() != null) {
                luceneVectorSearchElement.setAttribute(CONTEXT_VECTOR_SEARCH_SIMILARITY_METRIC_ATTR, "" + context.getVectorSimilarityMetric());
            }
            doc.getDocumentElement().appendChild(luceneVectorSearchElement);
        }

        // INFO: Sentence-BERT
        if (ddqi.equals(DetectDuplicatedQuestionImpl.SENTENCE_BERT) && context.getSentenceBERTCorpusId() != null) {
            Element sbertElement = doc.createElement(CONTEXT_SENTENCE_BERT_TAG);
            sbertElement.setAttribute(CONTEXT_SENTENCE_BERT_THRESHOLD_ATTR, "" + context.getSentenceBERTDistanceThreshold());
            doc.getDocumentElement().appendChild(sbertElement);

            Element sbertCorpusIdElement = doc.createElement(CONTEXT_SENTENCE_BERT_CORPUS_ID_TAG);
            sbertElement.appendChild(sbertCorpusIdElement);
            Text sbertCorpusIdText = doc.createTextNode(context.getSentenceBERTCorpusId());
            sbertCorpusIdElement.appendChild(sbertCorpusIdText);
        }

        // INFO: Elasticsearch
        if (ddqi.equals(DetectDuplicatedQuestionImpl.ELASTICSEARCH) && context.getElasticsearchIndex() != null) {
            Element esElement = doc.createElement(CONTEXT_ELASTICSEARCH_TAG);
            doc.getDocumentElement().appendChild(esElement);

            Element esIndexElement = doc.createElement(CONTEXT_ELASTICSEARCH_INDEX_TAG);
            esElement.appendChild(esIndexElement);
            Text esIndexText = doc.createTextNode(context.getElasticsearchIndex());
            esIndexElement.appendChild(esIndexText);
        }

        Element mailElement = doc.createElement(CONTEXT_MAIL_TAG);
        doc.getDocumentElement().appendChild(mailElement);

        Element mailBodyElement = doc.createElement(CONTEXT_MAIL_BODY_TAG);
        mailElement.appendChild(mailBodyElement);
        mailBodyElement.setAttribute(CONTEXT_MAIL_BODY_HOST_ATTR, context.getHost());

        Element deepLinkElement = doc.createElement(CONTEXT_MAIL_DEEPLINK_TAG);
        mailElement.appendChild(deepLinkElement);
        Text deepLinkText = doc.createTextNode(context.getMailBodyDeepLink());
        deepLinkElement.appendChild(deepLinkText);

        Element subjectTagElement = doc.createElement(CONTEXT_MAIL_SUBJECTTAG_TAG);
        mailElement.appendChild(subjectTagElement);
        Text subjectTagText = doc.createTextNode(context.getMailSubjectTag());
        subjectTagElement.appendChild(subjectTagText);

        log.info("Sender email address: " + context.getMailSenderEmail());
        if (context.getMailSenderEmail() != null && !context.getMailSenderEmail().equals(defaultSenderEmailAddress)) {
            Element senderEmailElement = doc.createElement(CONTEXT_MAIL_SENDER_EMAIL_TAG);
            mailElement.appendChild(senderEmailElement);
            Text senderEmailText = doc.createTextNode(context.getMailSenderEmail());
            senderEmailElement.appendChild(senderEmailText);
        }

        GmailConfiguration gmailConfiguration = context.getGmailConfiguration();
        if (gmailConfiguration != null) {
            Element gmailElement = doc.createElement(DOMAIN_MAIL_GMAIL_TAG);
            gmailElement.setAttribute("credentials", gmailConfiguration.getCredentialsPath());
            gmailElement.setAttribute("username", gmailConfiguration.getUsername());
            mailElement.appendChild(gmailElement);
        }

        IMAPConfiguration imapConfiguration = context.getImapConfiguration();
        if (imapConfiguration != null) {
            Element imapElement = doc.createElement(DOMAIN_MAIL_IMAP_TAG);
            imapElement.setAttribute("hostname", imapConfiguration.getHostname());
            imapElement.setAttribute("port", "" + imapConfiguration.getPort());
            imapElement.setAttribute("username", imapConfiguration.getUsername());
            imapElement.setAttribute("password", imapConfiguration.getPassword());
            mailElement.appendChild(imapElement);
        }

        if (context.getMatchReplyTo() != null) {
            Element matchElement = doc.createElement(DOMAIN_MAIL_MATCH_REPLY_TO_TAG);
            matchElement.setAttribute("reply-to", context.getMatchReplyTo());
            mailElement.appendChild(matchElement);
        }

        DomainSlackConfiguration slackConfiguration = context.getSlackConfiguration();
        if (slackConfiguration != null) {
            Element slackElement = doc.createElement(DOMAIN_SLACK_TAG);
            slackElement.setAttribute(DOMAIN_SLACK_SEND_EXPERT_BUTTON_ATTR, "" + slackConfiguration.getButtonSendToExpertEnabled());
            slackElement.setAttribute(DOMAIN_SLACK_IMPROVE_BUTTON_ATTR, "" + slackConfiguration.getButtonImproveAnswerEnabled());
            slackElement.setAttribute(DOMAIN_SLACK_LOGIN_BUTTON_ATTR, "" + slackConfiguration.getButtonLoginKatieEnabled());
            slackElement.setAttribute(DOMAIN_SLACK_ANSWER_BUTTON_ATTR, "" + slackConfiguration.getButtonAnswerQuestionEnabled());
            slackElement.setAttribute(DOMAIN_SLACK_SUBDOMAIN_ATTR, slackConfiguration.getSubdomain());
            doc.getDocumentElement().appendChild(slackElement);
        }

        Element iamElement = doc.createElement(CONTEXT_IAM_TAG);
        doc.getDocumentElement().appendChild(iamElement);
        iamElement.setAttribute(CONTEXT_IAM_PROTECTED_ATTR, "" + context.getAnswersGenerallyProtected());


        save(doc, config);
    }

    /**
     * Parse domain configuration
     * @param domainId Domain Id
     * @return domain object
     */
    public Context parseContextConfig(String domainId) throws Exception {
        File config = null;
        if (domainId != null) {
            log.debug("Parse domain configuration with Id '" + domainId + "' ...");
            config = new File(getDomainDirectory(domainId), DOMAIN_CONFIG_FILE_NAME);
        } else {
            log.info("Use " + Context.ROOT_NAME + " context ...");
            config = new File(getDomainDirectory(Context.ROOT_NAME), DOMAIN_CONFIG_FILE_NAME);
        }

        if (!config.isFile()) {
            log.error("No such domain configuration file '" + config.getAbsolutePath() + "'!");
            throw new Exception("No such domain '" + domainId + "'!");
        }

        log.debug("Parse domain configuration XML '" + config.getAbsolutePath() + "' ...");

        File contextDir = new File(config.getParent());
        String contextId = contextDir.getName();

        Document doc = read(config);

        doc.getDocumentElement().normalize();

        String name = null;
        if (doc.getDocumentElement().hasAttribute(DOMAIN_NAME_ATTR)) {
            name = doc.getDocumentElement().getAttribute(DOMAIN_NAME_ATTR);
        }

        EmbeddingsImpl embeddingsImpl = EmbeddingsImpl.UNSET;
        String embeddingsModel = null;
        EmbeddingValueType embeddingValueType = null;
        String embeddingsEndpoint = null;
        String embeddingsApiToken = null;
        String similarityMetricStr = null;

        String nerImpl = getAttributeStringValue(doc, CONTEXT_NER_TAG, CONTEXT_NER_IMPL_ATTR, null);

        Element mcpEl = getDirectChildByTagName(doc.getDocumentElement(), CONTEXT_MCP_TAG);
        Element llmSearchEl = getDirectChildByTagName(doc.getDocumentElement(), CONTEXT_LLM_SEARCH_TAG);
        Element katieSearchEl = getDirectChildByTagName(doc.getDocumentElement(), CONTEXT_KATIE_SEARCH_TAG);
        Element azureAISearchEl = getDirectChildByTagName(doc.getDocumentElement(), CONTEXT_AZURE_AI_SEARCH_TAG);

        // INFO: Query service configuration
        String qsQueryUrl = null;
        NodeList qsNL = doc.getElementsByTagName(CONTEXT_QUERY_SERVICE_TAG);
        if (qsNL.getLength() > 0) {
            qsQueryUrl = ((Element)qsNL.item(0)).getTextContent();;
        }

        // INFO: Weaviate configuration
        String weaviateQueryUrl = null;
        NodeList weaviateNL = doc.getElementsByTagName(CONTEXT_WEAVIATE_TAG);
        float weaviateCertaintyThreshold = certaintyThreshold;
        if (weaviateNL.getLength() > 0) {
            Element weaviateEl = (Element)weaviateNL.item(0);
            weaviateQueryUrl = weaviateEl.getTextContent();

            if (weaviateEl.hasAttribute(CONTEXT_WEAVIATE_THRESHOLD_ATTR)) {
                weaviateCertaintyThreshold = Float.parseFloat(weaviateEl.getAttribute(CONTEXT_WEAVIATE_THRESHOLD_ATTR));
            }
        }

        // INFO: Milvus configuration
        String milvusBaseUrl = null;
        Element milvusEl = getChildByTagName(doc.getDocumentElement(), CONTEXT_MILVUS_TAG, false);
        if (milvusEl != null && milvusEl.hasAttribute(CONTEXT_MILVUS_BASE_URL_ATTR)) {
            milvusBaseUrl = milvusEl.getAttribute(CONTEXT_MILVUS_BASE_URL_ATTR);

            if (milvusEl.hasAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDINGS_IMPL_ATTR)) {
                embeddingsImpl = EmbeddingsImpl.valueOf(milvusEl.getAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDINGS_IMPL_ATTR));
            }
            if (milvusEl.hasAttribute(CONTEXT_VECTOR_SEARCH_MODEL_ATTR)) {
                embeddingsModel = milvusEl.getAttribute(CONTEXT_VECTOR_SEARCH_MODEL_ATTR);
            }
            if (milvusEl.hasAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDING_VALUE_TYPE_ATTR)) {
                embeddingValueType = EmbeddingValueType.valueOf(milvusEl.getAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDING_VALUE_TYPE_ATTR));
            } else {
                embeddingValueType = EmbeddingValueType.float32;
            }
            if (milvusEl.hasAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDING_ENDPOINT_ATTR)) {
                embeddingsEndpoint = milvusEl.getAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDING_ENDPOINT_ATTR);
            }
            if (milvusEl.hasAttribute(CONTEXT_VECTOR_SEARCH_API_TOKEN_ATTR)) {
                embeddingsApiToken = milvusEl.getAttribute(CONTEXT_VECTOR_SEARCH_API_TOKEN_ATTR);
            }
            if (milvusEl.hasAttribute(CONTEXT_VECTOR_SEARCH_SIMILARITY_METRIC_ATTR)) {
                similarityMetricStr = milvusEl.getAttribute(CONTEXT_VECTOR_SEARCH_SIMILARITY_METRIC_ATTR);
            }
        }

        // INFO: Knowledge graph configuration
        String knowledgeGraphQueryUrl = null;
        NodeList knowledgeGraphNL = doc.getElementsByTagName(CONTEXT_KNOWLEDGE_GRAPH_TAG);
        if (knowledgeGraphNL.getLength() > 0) {
            knowledgeGraphQueryUrl = ((Element)knowledgeGraphNL.item(0)).getTextContent();;
        }

        // INFO: SentenceBERT-Lucene configuration
        boolean luceneVectorSearch = false;
        NodeList luceneVectorSearchNL = doc.getElementsByTagName(CONTEXT_LUCENE_VECTOR_SEARCH_TAG);
        if (luceneVectorSearchNL.getLength() > 0) {
            luceneVectorSearch = true;
            Element luceneVectorSearchEl = (Element) luceneVectorSearchNL.item(0);
            if (luceneVectorSearchEl.hasAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDINGS_IMPL_ATTR)) {
                embeddingsImpl = EmbeddingsImpl.valueOf(luceneVectorSearchEl.getAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDINGS_IMPL_ATTR));
            }
            if (luceneVectorSearchEl.hasAttribute(CONTEXT_VECTOR_SEARCH_MODEL_ATTR)) {
                embeddingsModel = luceneVectorSearchEl.getAttribute(CONTEXT_VECTOR_SEARCH_MODEL_ATTR);
            }
            if (luceneVectorSearchEl.hasAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDING_VALUE_TYPE_ATTR)) {
                embeddingValueType = EmbeddingValueType.valueOf(luceneVectorSearchEl.getAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDING_VALUE_TYPE_ATTR));
            } else {
                embeddingValueType = EmbeddingValueType.float32;
            }

            if (luceneVectorSearchEl.hasAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDING_ENDPOINT_ATTR)) {
                embeddingsEndpoint = luceneVectorSearchEl.getAttribute(CONTEXT_VECTOR_SEARCH_EMBEDDING_ENDPOINT_ATTR);
            }
            if (embeddingsImpl.equals(EmbeddingsImpl.OPENAI_AZURE)) {
                // TODO: Get from domain / context
                embeddingsEndpoint = openAIAzureHost;
            }

            if (luceneVectorSearchEl.hasAttribute(CONTEXT_VECTOR_SEARCH_API_TOKEN_ATTR)) {
                embeddingsApiToken = luceneVectorSearchEl.getAttribute(CONTEXT_VECTOR_SEARCH_API_TOKEN_ATTR);
            }
            if (luceneVectorSearchEl.hasAttribute(CONTEXT_VECTOR_SEARCH_SIMILARITY_METRIC_ATTR)) {
                similarityMetricStr = luceneVectorSearchEl.getAttribute(CONTEXT_VECTOR_SEARCH_SIMILARITY_METRIC_ATTR);
            }
        }

        // INFO: SentenceBERT
        String sentenceBERTCorpusId = null;
        float sentenceBERTDistanceThreshold = distanceThreshold;
        NodeList sentenceBertNL = doc.getElementsByTagName(CONTEXT_SENTENCE_BERT_TAG);
        if (sentenceBertNL.getLength() > 0) {
            Element sentenceBertEl = (Element)sentenceBertNL.item(0);

            if (sentenceBertEl.hasAttribute(CONTEXT_SENTENCE_BERT_THRESHOLD_ATTR)) {
                sentenceBERTDistanceThreshold = Float.parseFloat(sentenceBertEl.getAttribute(CONTEXT_SENTENCE_BERT_THRESHOLD_ATTR));
            }

            NodeList sbertCorpusIdNL = sentenceBertEl.getElementsByTagName(CONTEXT_SENTENCE_BERT_CORPUS_ID_TAG);
            sentenceBERTCorpusId = ((Element)sbertCorpusIdNL.item(0)).getTextContent();
        }

        // INFO: Elasticsearch
        String elasticsearchIndex = null;
        NodeList elasticsearchNL = doc.getElementsByTagName(CONTEXT_ELASTICSEARCH_TAG);
        if (elasticsearchNL.getLength() > 0) {
            Element elasticsearchEl = (Element)elasticsearchNL.item(0);

            NodeList esIndexNL = elasticsearchEl.getElementsByTagName(CONTEXT_ELASTICSEARCH_INDEX_TAG);
            elasticsearchIndex = ((Element)esIndexNL.item(0)).getTextContent();
        }

        boolean answersGenerallyProtected = getAttributeBooleanValue(doc, CONTEXT_IAM_TAG, CONTEXT_IAM_PROTECTED_ATTR, true);
        log.debug("Answers of context '" + contextId + "' are generally protected: " + answersGenerallyProtected);

        NodeList mailNL = doc.getElementsByTagName(CONTEXT_MAIL_TAG);
        Element mailEl = (Element)mailNL.item(0);

        NodeList mailSubjectTagNL = mailEl.getElementsByTagName(CONTEXT_MAIL_SUBJECTTAG_TAG);
        String mailSubjectTag = ((Element)mailSubjectTagNL.item(0)).getTextContent();

        String mailSenderEmail = null;
        NodeList mailSenderEmailNL = mailEl.getElementsByTagName(CONTEXT_MAIL_SENDER_EMAIL_TAG);
        if (mailSenderEmailNL.getLength() == 1) {
            mailSenderEmail = ((Element) mailSenderEmailNL.item(0)).getTextContent();
        } else {
            mailSenderEmail = defaultSenderEmailAddress;
        }

        NodeList mailDeepLinkNL = mailEl.getElementsByTagName(CONTEXT_MAIL_DEEPLINK_TAG);
        String mailBodyDeepLink = ((Element)mailDeepLinkNL.item(0)).getTextContent();
        log.debug("Mail body deep link of context '" + contextId + "': " + mailBodyDeepLink);

        NodeList mailBodyNL = mailEl.getElementsByTagName(CONTEXT_MAIL_BODY_TAG);
        Element mailBodyEl = (Element)mailBodyNL.item(0);
        String mailBodyAskKatieHost = mailBodyEl.getAttribute(CONTEXT_MAIL_BODY_HOST_ATTR);
        log.debug("Mail body host of context '" + contextId + "': " + mailBodyAskKatieHost);

        GmailConfiguration gmailConfiguration = null;
        Element gmailEl = getChildByTagName(mailEl, DOMAIN_MAIL_GMAIL_TAG, false);
        if (gmailEl != null) {
            gmailConfiguration = new GmailConfiguration(gmailEl.getAttribute("credentials"), gmailEl.getAttribute("username"));
        }

        IMAPConfiguration imapConfiguration = null;
        Element imapEl = getChildByTagName(mailEl, DOMAIN_MAIL_IMAP_TAG, false);
        if (imapEl != null) {
            imapConfiguration = new IMAPConfiguration(imapEl.getAttribute("hostname"), Integer.valueOf(imapEl.getAttribute("port")), imapEl.getAttribute("username"), imapEl.getAttribute("password"));
        }

        DomainSlackConfiguration slackConfig = null;
        Element slackEl = getChildByTagName(doc.getDocumentElement(), DOMAIN_SLACK_TAG, false);
        if (slackEl != null) {
            boolean sendToExpertEnabled = true;
            if (slackEl.hasAttribute(DOMAIN_SLACK_SEND_EXPERT_BUTTON_ATTR)) {
                sendToExpertEnabled = Boolean.valueOf(slackEl.getAttribute(DOMAIN_SLACK_SEND_EXPERT_BUTTON_ATTR));
            }
            boolean improveButtonEnabled = Boolean.valueOf(slackEl.getAttribute(DOMAIN_SLACK_IMPROVE_BUTTON_ATTR));
            boolean loginButtonEnabled = Boolean.valueOf(slackEl.getAttribute(DOMAIN_SLACK_LOGIN_BUTTON_ATTR));
            boolean answerButtonEnabled = Boolean.valueOf(slackEl.getAttribute(DOMAIN_SLACK_ANSWER_BUTTON_ATTR));
            String subdomain = slackEl.getAttribute(DOMAIN_SLACK_SUBDOMAIN_ATTR);
            slackConfig = new DomainSlackConfiguration(sendToExpertEnabled, improveButtonEnabled, loginButtonEnabled, answerButtonEnabled, subdomain);
        }

        String matchReplyTo = null;
        Element matchEl = getChildByTagName(mailEl, DOMAIN_MAIL_MATCH_REPLY_TO_TAG, false);
        if (matchEl != null) {
            matchReplyTo = matchEl.getAttribute("reply-to");
        }

        boolean answersMustBeApproved = getAttributeBooleanValue(doc, CONTEXT_MODERATION_TAG, CONTEXT_MODERATION_TOGGLE_ATTR, false);
        log.debug("Answers of context '" + contextId + "' must be approved by a moderator: " + answersMustBeApproved);
        boolean informUserReModeration = getAttributeBooleanValue(doc, CONTEXT_MODERATION_TAG, CONTEXT_MODERATION_INFORM_USER_RE_MODERATION_ATTR, false);

        boolean informUserReNoAnswerAvailable = false;
        NodeList informUserNoAnswerkNL = doc.getElementsByTagName(CONTEXT_INFORM_WHEN_NO_ANSWER_TAG);
        if (informUserNoAnswerkNL.getLength() > 0) {
            informUserReNoAnswerAvailable = Boolean.parseBoolean(((Element)informUserNoAnswerkNL.item(0)).getAttribute(CONTEXT_INFORM_WHEN_NO_ANSWER_ENABLED_ATTR));
        }

        boolean considerHumanFeedback = false;
        boolean reRankAnswers = false;
        ReRankImpl reRankImpl = reRankDefaultImpl;
        boolean useGenerativeAI = false;
        CompletionConfig genAIConfig = new CompletionConfig(completionDefaultImpl, null, null, null);
        boolean katieSearchEnabled = true;
        Double scoreThreshold = null;
        boolean analyzeMessagesAskRestApi = false;
        Element indexSearchPipelineEl = getDirectChildByTagName(doc.getDocumentElement(), CONTEXT_INDEX_SEARCH_PIPEILINE_TAG);
        if (indexSearchPipelineEl != null) {

            if (indexSearchPipelineEl.hasAttribute(CONTEXT_ANALYZE_MESSAGES_ASK_REST_API)) {
                analyzeMessagesAskRestApi = Boolean.parseBoolean(indexSearchPipelineEl.getAttribute(CONTEXT_ANALYZE_MESSAGES_ASK_REST_API));
            }

            if (indexSearchPipelineEl.hasAttribute(CONTEXT_SCORE_THRESHOLD_ATTR)) {
                scoreThreshold = Double.parseDouble(indexSearchPipelineEl.getAttribute(CONTEXT_SCORE_THRESHOLD_ATTR));
            }

            considerHumanFeedback = getAttributeBooleanValue(indexSearchPipelineEl, CONTEXT_CONSIDER_HUMAN_FEEDBACK_ATTR, false);

            reRankAnswers = getAttributeBooleanValue(indexSearchPipelineEl, CONTEXT_RE_RANK_ANSWERS_ATTR, false);
            if (indexSearchPipelineEl.hasAttribute(CONTEXT_RE_RANK_IMPLEMENTATION_ATTR)) {
                reRankImpl = ReRankImpl.valueOf(indexSearchPipelineEl.getAttribute(CONTEXT_RE_RANK_IMPLEMENTATION_ATTR));
            }
            
            useGenerativeAI = getAttributeBooleanValue(indexSearchPipelineEl, CONTEXT_USE_GENERATIVE_AI_ATTR, false);
            if (indexSearchPipelineEl.hasAttribute(CONTEXT_GENERATIVE_AI_IMPLEMENTATION_ATTR)) {
                genAIConfig.setCompletionImpl(CompletionImpl.valueOf(indexSearchPipelineEl.getAttribute(CONTEXT_GENERATIVE_AI_IMPLEMENTATION_ATTR)));
                if (indexSearchPipelineEl.hasAttribute(CONTEXT_GENERATIVE_AI_MODEL_ATTR)) {
                    genAIConfig.setModel(indexSearchPipelineEl.getAttribute(CONTEXT_GENERATIVE_AI_MODEL_ATTR));
                } else {
                    genAIConfig.setModel(getCompletionModel(genAIConfig.getCompletionImpl()));
                }
                if (indexSearchPipelineEl.hasAttribute(CONTEXT_GENERATIVE_AI_API_KEY_ATTR)) {
                    genAIConfig.setApiKey(indexSearchPipelineEl.getAttribute(CONTEXT_GENERATIVE_AI_API_KEY_ATTR));
                }
                if (indexSearchPipelineEl.hasAttribute(CONTEXT_GENERATIVE_AI_HOST_ATTR)) {
                    genAIConfig.setHost(indexSearchPipelineEl.getAttribute(CONTEXT_GENERATIVE_AI_HOST_ATTR));
                } else {
                    if (genAIConfig.getCompletionImpl().equals(CompletionImpl.OLLAMA)) {
                        genAIConfig.setHost(ollamaDefaultHost);
                    } else {
                        log.warn("TODO: Set default host for completion implementation " + genAIConfig.getCompletionImpl());
                    }
                }
            }

            katieSearchEnabled = getAttributeBooleanValue(indexSearchPipelineEl, CONTEXT_KATIE_SEARCH_ENABLED_ATTR, true);
        }

        ClassificationImpl classificationImpl = ClassificationImpl.CENTROID_MATCHING; // INFO: Default
        Element classificationEl = getDirectChildByTagName(doc.getDocumentElement(), CONTEXT_CLASSIFICATION_TAG);
        if (classificationEl != null) {
            classificationImpl = ClassificationImpl.valueOf(classificationEl.getAttribute(CONTEXT_CLASSIFIER_IMPL_ATTR));
        }

        String reindexBackgroundProcessId = getReindexProcessId(domainId);

        Context domain = new Context(contextId, contextDir, answersGenerallyProtected, mailBodyAskKatieHost, mailBodyDeepLink, mailSubjectTag, mailSenderEmail, answersMustBeApproved, informUserReModeration, considerHumanFeedback, reRankAnswers, useGenerativeAI, genAIConfig, katieSearchEnabled, reindexBackgroundProcessId);
        domain.setInformUserReNoAnswerAvailable(informUserReNoAnswerAvailable);
        domain.setReRankImpl(reRankImpl);
        domain.setReRankLLMImpl(reRankLLMDefaultImpl); // TODO: Make configurable
        domain.setClassifierImpl(classificationImpl);

        Element generativePromptMessagesEl = getDirectChildByTagName(doc.getDocumentElement(), CONTEXT_GEN_AI_PROMPT_MESSAGES_TAG);
        if (generativePromptMessagesEl != null) {
            List<PromptMessage> promptMessages = new ArrayList<>();
            NodeList pmsgs = generativePromptMessagesEl.getElementsByTagName(CONTEXT_GEN_AI_PROMPT_MESSAGE_TAG);
            if (pmsgs !=  null && pmsgs.getLength() > 0) {
                for (int i = 0; i < pmsgs.getLength(); i++) {
                    Element pmsg = ((Element)pmsgs.item(i));
                    promptMessages.add(new PromptMessage(PromptMessageRole.fromString(pmsg.getAttribute(CONTEXT_GEN_AI_PROMPT_MESSAGE_ROLE_ATTR)), pmsg.getFirstChild().getTextContent()));
                }
            }
            domain.setPromptMessages(promptMessages);
        }

        domain.setScoreThreshold(scoreThreshold);
        domain.setAnalyzeMessagesAskRestApi(analyzeMessagesAskRestApi);

        if (name != null) {
            domain.setName(name);
        }

        if (imapConfiguration != null) {
            domain.setImapConfiguration(imapConfiguration);
        }
        if (gmailConfiguration != null) {
            domain.setGmailConfiguration(gmailConfiguration);
        }
        if (slackConfig != null) {
            domain.setSlackConfiguration(slackConfig);
        }
        if (matchReplyTo != null) {
            domain.setMatchReplyTo(matchReplyTo);
        }

        // INFO: Set answer question implementation
        domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.LUCENE_DEFAULT); // INFO: Set LUCENE_DEFAULT implementation by default

        if (mcpEl != null) {
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.MCP);
        }

        if (llmSearchEl != null) {
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.LLM);
            if (llmSearchEl.hasAttribute(CONTEXT_LLM_SEARCH_ASSISTANT_ID_ATTR)) {
                domain.setLlmSearchAssistantId(llmSearchEl.getAttribute(CONTEXT_LLM_SEARCH_ASSISTANT_ID_ATTR));
            }
            if (llmSearchEl.hasAttribute(CONTEXT_LLM_SEARCH_ASSISTANT_NAME_ATTR)) {
                domain.setLlmSearchAssistantName(llmSearchEl.getAttribute(CONTEXT_LLM_SEARCH_ASSISTANT_NAME_ATTR));
            }
            if (llmSearchEl.hasAttribute(CONTEXT_LLM_SEARCH_ASSISTANT_INSTRUCTIONS_ATTR)) {
                domain.setLlmSearchAssistantInstructions(llmSearchEl.getAttribute(CONTEXT_LLM_SEARCH_ASSISTANT_INSTRUCTIONS_ATTR));
            }
        }
        if (katieSearchEl != null) {
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.KATIE);
        }
        if (azureAISearchEl != null) {
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.AZURE_AI_SEARCH);
            domain.setAzureAISearchEndpoint(azureAISearchEl.getAttribute(CONTEXT_AZURE_AI_SEARCH_ENDPOINT_ATTR));
            domain.setAzureAISearchAdminKey(azureAISearchEl.getAttribute(CONTEXT_AZURE_AI_SEARCH_ADMIN_KEY_ATTR));
            domain.setAzureAISearchIndexName(azureAISearchEl.getAttribute(CONTEXT_AZURE_AI_SEARCH_INDEX_NAME_ATTR));
        }
        if (qsQueryUrl != null) {
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.QUERY_SERVICE);
            domain.setQueryServiceUrl(qsQueryUrl);
        }
        if (weaviateQueryUrl != null) {
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.WEAVIATE);
            domain.setWeaviateQueryUrl(weaviateQueryUrl);
            domain.setWeaviateCertaintyThreshold(weaviateCertaintyThreshold);
        }
        if (milvusBaseUrl != null) {
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.MILVUS);
            domain.setMilvusBaseUrl(milvusBaseUrl);
        }
        if (knowledgeGraphQueryUrl != null) {
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.KNOWLEDGE_GRAPH);
            domain.setKnowledgeGraphQueryUrl(knowledgeGraphQueryUrl);
        }

        if (luceneVectorSearch) {
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH);
        }

        if (sentenceBERTCorpusId != null) {
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.SENTENCE_BERT);
            domain.setSentenceBERTCorpusId(sentenceBERTCorpusId);
            domain.setSentenceBERTDistanceThreshold(sentenceBERTDistanceThreshold);
        }
        if (elasticsearchIndex != null) {
            domain.setDetectDuplicatedQuestionImpl(DetectDuplicatedQuestionImpl.ELASTICSEARCH);
            domain.setElasticsearchIndex(elasticsearchIndex);
        }

        // INFO: Set NER implementation
        if (nerImpl != null) {
            domain.setNerImpl(NerImpl.valueOf(nerImpl));
        }

        if (embeddingsImpl != EmbeddingsImpl.UNSET) {
            domain.setEmbeddingsImpl(embeddingsImpl);
            domain.setEmbeddingsModel(embeddingsModel);
            domain.setEmbeddingValueType(embeddingValueType);
            domain.setEmbeddingsEndpoint(embeddingsEndpoint);
            domain.setEmbeddingsApiToken(embeddingsApiToken);
        }
        if (similarityMetricStr != null) {
            domain.setVectorSimilarityMetric(VectorSimilarityFunction.valueOf(similarityMetricStr));
        }

        return domain;
    }

    /**
     * Get GenAI model
     * @param completionImpl Completion implementation, e.g. OpenAI, Mistral or DeepSeek
     * @return GenAI model, e.g. "deepseek-r1"
     */
    private String getCompletionModel(CompletionImpl completionImpl) {
        String model = null;
        if (completionImpl.equals(CompletionImpl.ALEPH_ALPHA)) {
            model = "luminous-base"; // TODO: Make configurable
        } else if (completionImpl.equals(CompletionImpl.OPENAI)) {
            model = openAIModel;
        } else if (completionImpl.equals(CompletionImpl.AZURE)) {
            // TODO: Offer alternative?!
            model = openAIModel;
        } else if (completionImpl.equals(CompletionImpl.MISTRAL_AI)) {
            model = mistralAIModel;
        } else if (completionImpl.equals(CompletionImpl.OLLAMA)) {
            model = ollamaModel;
        } else if (completionImpl.equals(CompletionImpl.UNSET)) {
            log.info("No completion implementation configured.");
            model = null;
        } else {
            log.error("No such completion implemention supported yet: " + completionImpl);
            model = null;
        }
        return model;
    }

    /**
     * @return reindex process Id
     */
    protected String getReindexProcessId(String domainId) {
        String reindexBackgroundProcessId = null;
        File reindexLockFile = new File(getDomainDirectory(domainId), Context.REINDEX_LOCK_FILE);
        if (reindexLockFile.isFile()) {
            try {
                FileInputStream fis = new FileInputStream(reindexLockFile.getAbsolutePath());
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                reindexBackgroundProcessId = br.readLine();
                br.close();
                fis.close();
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return reindexBackgroundProcessId;
    }

    /**
     * @param defaultValue Default value when attribute does not exist
     */
    private boolean getAttributeBooleanValue(Element element, String attributeName, boolean defaultValue) {
        if (element.hasAttribute(attributeName)) {
            return Boolean.parseBoolean(element.getAttribute(attributeName));
        } else {
            log.warn("Attribute '" + attributeName + "' does not exist, therefore use default value '" + defaultValue + "'.");
            return defaultValue;
        }
    }

    /**
     * Get boolean attribute value, e.g. <moderation answers-must-be-approved="true"/>
     * @param defaultValue Default value when element does not exist
     */
    private boolean getAttributeBooleanValue(Document doc, String elementName, String attributeName, boolean defaultValue) {
        boolean value = defaultValue;
        NodeList nodeList = doc.getElementsByTagName(elementName);
        if (nodeList.getLength() > 0) {
            Element element = (Element)nodeList.item(0);
            value = Boolean.valueOf(element.getAttribute(attributeName));
        }
        return value;
    }

    /**
     * Get string attribute value, e.g. <ner impl="FLAIR"/>
     * @param defaultValue Default value when element does not exist
     */
    private String getAttributeStringValue(Document doc, String elementName, String attributeName, String defaultValue) {
        String value = defaultValue;
        NodeList nodeList = doc.getElementsByTagName(elementName);
        if (nodeList.getLength() > 0) {
            Element element = (Element)nodeList.item(0);
            value = element.getAttribute(attributeName);
        }
        return value;
    }

    /**
     * Get answer from XML
     * @param submittedQuestion Question submitted by user which is similar to original question
     * @param answersGenerallyProtected Flag whether answers of particular domain are generally protected
     * @param domain Domain associated with QnA
     * @param uuid UUID of QnA
     * @param userId User Id of signed in user
     * @return QnA and null if XML file of QnA does not exist
     */
    // deserialize from XML to Java (https://www.baeldung.com/jackson-xml-serialization-and-deserialization)
    public Answer parseQuestionAnswer(String submittedQuestion, boolean answersGenerallyProtected, Context domain, String uuid, String userId) throws Exception {
        File qnaXMLFile = domain.getQnAXmlFilePath(uuid);

        if (!qnaXMLFile.isFile()) {
            log.error("No such QnA XML file: " + qnaXMLFile.getAbsolutePath());
            return null;
        }

        log.info("Parse QnA XML '" + qnaXMLFile.getAbsolutePath() + "' ...");

        Document doc = read(qnaXMLFile);

        doc.getDocumentElement().normalize();

        NodeList referenceNL = doc.getElementsByTagName(QA_REFERENCE_TAG);
        if (referenceNL != null && referenceNL.getLength() == 1) {
            Element referenceEl = (Element)referenceNL.item(0);
            String refDomainId = referenceEl.getAttribute("domain-id");
            String refUuid = referenceEl.getAttribute("uuid");
            log.info("Parse QnA '" + refDomainId + " / " + refUuid + "' ...");
            Context refDomain = parseContextConfig(refDomainId);
            // TODO: Break infinite loop
            log.warn("Beware of infinite loops!");
            Answer qna = parseQuestionAnswer(submittedQuestion, answersGenerallyProtected, refDomain, refUuid, userId);
            qna.setDomainId(domain.getId());
            qna.setUUID(uuid);
            qna.setReference(new QnAReference(refDomainId, refUuid));

            return qna;
        }

        // INFO: Example "Bank": All answers are protected, except public FAQs
        // INFO: Example "Open Source Organization": All answers are public, except member votes
        boolean isAnswerPublic = false;
        Permissions permissions = null;
        NodeList permissionsNL = doc.getElementsByTagName(QA_PERMISSIONS_TAG);
        log.debug("Answers of domain '" + domain.getId() + "' are generally protected: " + answersGenerallyProtected);
        if (!answersGenerallyProtected) {
            if (permissionsNL == null || permissionsNL.getLength() == 0) {
                isAnswerPublic = true;
            } else {
                log.info("Read permissions from XML and check whether answer is really protected ...");
                permissions = readPermissions((Element)permissionsNL.item(0));
                if (permissions != null && permissions.isPublic()) {
                    isAnswerPublic = true;
                } else {
                    isAnswerPublic = false;
                }
            }
        } else {
            if (permissionsNL != null && permissionsNL.getLength() == 1) {
                log.info("Read permissions from XML and check whether answer might not be protected ...");
                permissions = readPermissions((Element)permissionsNL.item(0));
                if (permissions != null && permissions.isPublic()) {
                    isAnswerPublic = true;
                } else {
                    isAnswerPublic = false;
                }
            } else {
                isAnswerPublic = false;
            }
        }

        if (!isAnswerPublic) {
            log.info("Answer contained by file '" + qnaXMLFile.getAbsolutePath() + "' is protected.");
        }

        String uuidAttr = null;
        if (doc.getDocumentElement().hasAttribute(QA_UUID_ATTR)) {
            uuidAttr = doc.getDocumentElement().getAttribute(QA_UUID_ATTR);
            if (!uuid.equals(uuidAttr)) {
                log.error("UUID does not match!");
                throw new Exception("UUID does not match!");
            }
        } else {
            log.warn("TODO: Please set uuid Attribute inside XML file '" + qnaXMLFile.getAbsolutePath() + "'!");
        }

        String knowledgeSourceUuid = null;
        String knowledgeSourceItemKey = null;
        Element knowledgeSourceEl = getDirectChildByTagName(doc.getDocumentElement(), QA_KNOWLEDGE_SOURCE);
        if (knowledgeSourceEl != null) {
            knowledgeSourceUuid = knowledgeSourceEl.getAttribute(QA_KNOWLEDGE_SOURCE_ID_ATTR);
            knowledgeSourceItemKey = knowledgeSourceEl.getAttribute(QA_KNOWLEDGE_SOURCE_KEY_ATTR);
        }

        Element answerEl = (Element)doc.getElementsByTagName(QA_ANSWER_TAG).item(0);
        String answer = removeTag(QA_ANSWER_TAG, convertNodeToString(answerEl));
        log.debug("Answer: " + answer);

        String clientSideEncryptionAlgorithm = null;
        if (answerEl.hasAttribute(QA_ANSWER_CLIENT_SIDE_ENCRYPTION_ATTR)) {
            clientSideEncryptionAlgorithm = answerEl.getAttribute(QA_ANSWER_CLIENT_SIDE_ENCRYPTION_ATTR);
        }

        QnAType type = QnAType.DEFAULT; // INFO: Text
        if (answerEl.hasAttribute(QA_ANSWER_TYPE_ATTR)) {
            type = QnAType.valueOf(answerEl.getAttribute(QA_ANSWER_TYPE_ATTR));
        }

        ContentType contentType = ContentType.TEXT_HTML;
        if (answerEl.hasAttribute(QA_ANSWER_CONTENT_TYPE_ATTR)) {
            String contentTypeString = answerEl.getAttribute(QA_ANSWER_CONTENT_TYPE_ATTR);
            contentType = ContentType.fromString(contentTypeString);
        }

        List<String> classifications = new ArrayList<String>();
        Element classificationsEl = (Element)doc.getElementsByTagName(CLASSIFICATIONS_TAG).item(0);
        if (classificationsEl !=  null) {
            NodeList terms = classificationsEl.getElementsByTagName(CLASSIFICATION_TERM_TAG);
            if (terms !=  null && terms.getLength() > 0) {
                for (int i = 0; i < terms.getLength(); i++) {
                    classifications.add(terms.item(i).getTextContent());
                }
            }
        }

        String respondentId = null;
        if (answerEl.hasAttribute(QA_ANSWER_RESPONDENT_ID_ATTR)) {
            respondentId = answerEl.getAttribute(QA_ANSWER_RESPONDENT_ID_ATTR);
        }

        List<Rating> ratings = parseRatings(domain, uuid);

        Element originalQuestionEl = getDirectChildByTagName(doc.getDocumentElement(), QA_QUESTION_TAG);
        String originalQuestion = null;
        Date dateOriginalQuestionSubmitted = null;
        if (originalQuestionEl != null) {
            originalQuestion = originalQuestionEl.getTextContent();
            log.debug("Original question: " + originalQuestion);

            if (originalQuestionEl.hasAttribute(QA_QUESTION_DATE_ATTR)) {
                dateOriginalQuestionSubmitted = new Date(Long.parseLong(originalQuestionEl.getAttribute(QA_QUESTION_DATE_ATTR)));
                log.debug("Date when original question was submitted: " + dateOriginalQuestionSubmitted);
            } else {
                // TODO
                log.warn("No date available when original question was submitted!");
            }
        } else {
            log.info("No original question available.");
        }

        // INFO: Read alternative questions
        List<String> alternativeQuestions = new ArrayList<String>();
        Element alternativeQuestionsEl = (Element)doc.getElementsByTagName(QA_ALTERNATIVE_QUESTIONS_TAG).item(0);
        if (alternativeQuestionsEl != null) {
            NodeList aQuestions = alternativeQuestionsEl.getElementsByTagName(QA_QUESTION_TAG);
            if (aQuestions !=  null && aQuestions.getLength() > 0) {
                for (int i = 0; i < aQuestions.getLength(); i++) {
                    Element aq = ((Element)aQuestions.item(i));
                    alternativeQuestions.add(aq.getTextContent());
                }
            }
        }

        String email = getAttributeStringValue(doc, QA_SOURCE_TAG, QA_EMAIL_ATTR, null);

        log.info("TODO: Get dates when answer provided and original question submitted");
        Date dateAnswered = null;

        Date dateAnswerModified = null;
        if (answerEl.hasAttribute(QA_DATE_ANSWER_MODIFIED_ATTR)) {
            dateAnswerModified = new Date(Long.parseLong(answerEl.getAttribute(QA_DATE_ANSWER_MODIFIED_ATTR)));
        } else {
            // TODO
            log.warn("No date available when answer was modified!");
        }

        boolean isTrained = false;
        if (doc.getDocumentElement().hasAttribute(QA_IS_TRAINED_ATTR)) {
            isTrained = Boolean.parseBoolean(doc.getDocumentElement().getAttribute(QA_IS_TRAINED_ATTR));
        } else {
            log.info("Because of backwards compatibility we set isTrained to true for QnA '" + uuid + "'.");
            isTrained = true;
        }

        String url = null;
        Element urlEl = (Element)doc.getElementsByTagName(QA_URL_TAG).item(0);
        if (urlEl !=  null) {
            url = urlEl.getTextContent();
        }

        // INFO: Create answer instance
        Answer qna = new Answer(submittedQuestion, answer, contentType, url, classifications, type, clientSideEncryptionAlgorithm, dateAnswered, dateAnswerModified, email, domain.getId(), uuid, originalQuestion, dateOriginalQuestionSubmitted, isAnswerPublic, permissions, isTrained, respondentId);

        if (knowledgeSourceUuid != null) {
            qna.setKnowledgeSource(knowledgeSourceUuid, knowledgeSourceItemKey);
        }

        for (Rating rating: ratings) {
            qna.addRating(rating);
        }

        for (String q: alternativeQuestions) {
            qna.addAlternativeQuestion(q);
        }

        if (qna.isPublic()) {
            qna.setOwnership(Ownership.iam_public);
        } else {
            qna.setOwnership(null);
            if (answersGenerallyProtected) {
                qna.setOwnership(Ownership.iam_context);
            }

            log.info("Check whether signed in user '" + userId + "' is the only one authorized to access QnA '" + uuid + "' ...");
            if (userId != null && permissions != null && permissions.isUserAuthorized(userId)) {
                qna.setOwnership(Ownership.iam_source);
            }

            if (qna.getOwnership() == null) {
                log.warn("Ownership not settable!");
            }
        }

        boolean faqInfoMigrated = false;
        Element faqElement = (Element)doc.getElementsByTagName(QA_FAQ_TAG).item(0);
        if (faqElement !=  null) {
            faqInfoMigrated = true;
            qna.setFaqLanguage(Language.valueOf(faqElement.getAttribute(QA_FAQ_LANG_ATTR)));
            qna.setFaqTopicId(faqElement.getAttribute(QA_FAQ_TOPIC_ID_ATTR));
        }
        Element noFaqElement = (Element)doc.getElementsByTagName(QA_NO_FAQ_TAG).item(0);
        if (noFaqElement !=  null) {
            faqInfoMigrated = true;
            qna.setFaqLanguage(null);
            qna.setFaqTopicId(null);
        }

        if (!faqInfoMigrated) {
            log.warn("Migrate FAQ information for QnA '" + domain.getId() + "/" + uuid + "' ...");
            com.wyona.katie.models.faq.Question faq = migrateFAQ(domain, uuid);
            if (faq != null) {
                qna.setFaqLanguage(Language.valueOf(faq.getLanguage()));
                qna.setFaqTopicId(faq.getTopicId());
            } else {
                qna.setFaqLanguage(null);
                qna.setFaqTopicId(null);
            }
            saveQuestionAnswer(qna, qnaXMLFile);
        }

        return qna;
    }

    /**
     *
     */
    private List<Rating> parseRatings(Context domain, String uuid) throws Exception {
        List<Rating> ratings = new ArrayList<Rating>();

        File ratingsXMLFile = domain.getRatingsXmlFilePath(uuid);

        if (!ratingsXMLFile.isFile()) {
            log.warn("No such ratings file: " + ratingsXMLFile.getAbsolutePath());
            return ratings;
        }

        log.info("Parse Ratings XML '" + ratingsXMLFile.getAbsolutePath() + "' ...");

        Document doc = read(ratingsXMLFile);

        doc.getDocumentElement().normalize();

        Element ratingsEl = (Element)doc.getElementsByTagName(QA_RATINGS_TAG).item(0);
        if (ratingsEl != null) {
            NodeList ratingEls = ratingsEl.getElementsByTagName(QA_RATING_TAG);
            if (ratingEls !=  null && ratingEls.getLength() > 0) {
                for (int i = 0; i < ratingEls.getLength(); i++) {
                    Element ratingEl = ((Element)ratingEls.item(i));
                    Rating rating = new Rating();
                    rating.setRating(Integer.parseInt(ratingEl.getAttribute(QA_RATING_ATTR)));
                    Element feedbackEl = (Element)ratingEl.getElementsByTagName(QA_RATING_FEEDBACK_TAG).item(0);
                    if (feedbackEl !=  null) {
                        rating.setFeedback(feedbackEl.getTextContent());
                    }
                    Element userQuestionEl = (Element)ratingEl.getElementsByTagName(QA_RATING_QUESTION_TAG).item(0);
                    if (userQuestionEl !=  null) {
                        rating.setUserquestion(userQuestionEl.getTextContent());
                        if (userQuestionEl.hasAttribute(QA_RATING_QUESTION_UUID_ATTR)) {
                            rating.setQuestionuuid(userQuestionEl.getAttribute(QA_RATING_QUESTION_UUID_ATTR));
                        }
                    }
                    Element emailEl = (Element)ratingEl.getElementsByTagName(QA_RATING_EMAIL_TAG).item(0);
                    if (emailEl !=  null) {
                        rating.setEmail(emailEl.getTextContent());
                    }

                    ratings.add(rating);
                }
            }
        }
        return ratings;
    }

    /**
     * Add FAQ information to QnA XML
     * @param domain Domain associated with QnA
     * @param uuid UUID of QnA
     * @return FAQ language and topic Id
     */
    private com.wyona.katie.models.faq.Question migrateFAQ(Context domain, String uuid) {
        try {
            String[] languages = domain.getFAQLanguages();
            for (String lang: languages) {
                FAQ faq = getFAQ(domain, lang,false);
                Topic[] topics = faq.getTopics();
                for (Topic topic: topics) {
                    com.wyona.katie.models.faq.Question[] questions = topic.getQuestions();
                    for (Question q: questions) {
                        if (q.getUuid().equals(uuid)) {
                            log.info("QnA '' is connected with FAQ '" + lang + " / " + topic.getId() + "'.");
                            q.setLanguage(lang);
                            q.setTopicId(topic.getId());
                            return q;
                        }
                    }
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Read permissions parsing XML node
     * @param permissionsNode XML node containing permissions for users and groups
     */
    private Permissions readPermissions(Element permissionsNode) {
        NodeList users = permissionsNode.getElementsByTagName(QA_USER_TAG);
        String[] userIDs = null;
        if (users !=  null && users.getLength() > 0) {
            userIDs = new String[users.getLength()];
            for (int i = 0; i < users.getLength(); i++) {
                userIDs[i] = ((Element)users.item(i)).getAttribute("id");
            }
        }

        NodeList groups = permissionsNode.getElementsByTagName(QA_GROUP_TAG);
        String[] groupIDs = null;
        if (groups !=  null && groups.getLength() > 0) {
            groupIDs = new String[groups.getLength()];
            for (int i = 0; i < groups.getLength(); i++) {
                groupIDs[i] = ((Element)groups.item(i)).getAttribute("id");
            }
        }

        if ((userIDs != null && userIDs.length > 0) || (groupIDs != null && groupIDs.length > 0)) {
            return new Permissions(userIDs, null);
        }

        NodeList isPublicNL = permissionsNode.getElementsByTagName(QA_PUBLIC_TAG);
        if (isPublicNL != null && isPublicNL.getLength() == 1) {
            return new Permissions(true);
        }

        return null;
    }

    /**
     * @param xmlString XML string, e.g. "<answer>The answer is ...</answer>"
     * @param tagName Tag name, e.g. "answer"
     * @return XML string without tag at beginning and end, e.g. "The answer is ..."
     */
    private String removeTag(String tagName, String xmlString) {
        int indexEndOfStartTag = xmlString.indexOf(">");

        // INFO: Handle empty element (when there is no text available), e.g. <answer content-type="text/html" type="DEFAULT"/>
        if (xmlString.charAt(indexEndOfStartTag - 1 ) == '/') {
            log.warn("The element " + xmlString.trim() + " seems to be empty!");
            return ""; // TODO: Return empty string or null?
        }

        String withoutStartTag = xmlString.substring(indexEndOfStartTag + 1);

        int indexEndTag = withoutStartTag.length() - (tagName.length() + 3);
        return withoutStartTag.substring(0, indexEndTag - 1);
    }

    /**
     * Get node as string (including child nodes)
     * See https://frontbackend.com/java/how-to-convert-xml-node-object-to-string-in-java
     * @return node as string, e.g. "<answer><ak-exec>com.wyona.askkatie.answers.StringUtil#echo(ak-entity:person_name)</ak-exec> is currently <ak-exec>com.wyona.askkatie.answers.DateOfBirth#getAgeByPersonName(ak-entity:person_name)</ak-exec> years old.</answer>"
     */
    private String convertNodeToString(Node node) {
        try {
            StringWriter writer = new StringWriter();

            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.transform(new DOMSource(node), new StreamResult(writer));

            return writer.toString();
        } catch (TransformerException e) {
            log.error(e.getMessage(), e);
            return "ERROR_XMLService";
        }
    }

    /**
     *
     */
    public Document createQALinkDocument(String linkDomainId, String linkUuid, String uuid) {
        Document qaDoc = createDocument(QA_NAMESPACE_1_0_0, QA_ROOT_TAG);
        qaDoc.getDocumentElement().setAttribute(QA_UUID_ATTR, uuid);

        Element referenceElement = qaDoc.createElement(QA_REFERENCE_TAG);
        referenceElement.setAttribute("domain-id", linkDomainId);
        referenceElement.setAttribute("uuid", linkUuid);
        qaDoc.getDocumentElement().appendChild(referenceElement);

        return qaDoc;
    }

    /**
     * Create XML document representing thread message
     * @param message Thread message
     * @return XML document containing content of thread message
     */
    // serialize Java to XML (https://www.baeldung.com/jackson-xml-serialization-and-deserialization)
    public Document createThreadMessageDocument(String threadId, String message) {
        Document tmDoc = createDocument(QA_NAMESPACE_1_0_0, QA_ROOT_TAG);

        tmDoc.getDocumentElement().setAttribute("thread-id", threadId);

        Element msgElement = tmDoc.createElement(THREAD_MESSAGE_TAG);
        tmDoc.getDocumentElement().appendChild(msgElement);

        Text msgText = tmDoc.createTextNode(message);
        msgElement.appendChild(msgText);

        return tmDoc;
    }

    /**
     * Create XML document representing question/answer
     * @param answer QnA object
     * @return XML document containing all QnA content
     */
    // serialize Java to XML (https://www.baeldung.com/jackson-xml-serialization-and-deserialization)
    public Document createQADocument(Answer answer) {
        Document qaDoc = createDocument(QA_NAMESPACE_1_0_0, QA_ROOT_TAG);
        qaDoc.getDocumentElement().setAttribute(QA_UUID_ATTR, answer.getUuid());
        qaDoc.getDocumentElement().setAttribute(QA_IS_TRAINED_ATTR, "" + answer.isTrained());

        if (answer.getKnowledgeSourceUuid() != null) {
            Element knowledgeSourceEl = qaDoc.createElement(QA_KNOWLEDGE_SOURCE);
            knowledgeSourceEl.setAttribute(QA_KNOWLEDGE_SOURCE_ID_ATTR, answer.getKnowledgeSourceUuid());
            knowledgeSourceEl.setAttribute(QA_KNOWLEDGE_SOURCE_KEY_ATTR, answer.getKnowledgeSourceItemForeignKey());
            qaDoc.getDocumentElement().appendChild(knowledgeSourceEl);
        }

        if (answer.getOriginalquestion() != null) {
            Element questionElement = qaDoc.createElement(QA_QUESTION_TAG);
            qaDoc.getDocumentElement().appendChild(questionElement);
            Text questionText = qaDoc.createTextNode(answer.getOriginalquestion());
            questionElement.appendChild(questionText);

            if (answer.getDateOriginalQuestion() >= 0) {
                questionElement.setAttribute(QA_QUESTION_DATE_ATTR, "" + answer.getDateOriginalQuestion());
            } else {
                log.warn("QnA with id '" + answer.getUuid() + "' does not contain date when original question was asked!");
            }
        } else {
            log.error("No original question provided");
        }

        // INFO: Set alternative questions
        String[] alternativeQuestions = answer.getAlternativequestions();
        if (alternativeQuestions.length > 0) {
            Element alternativeQuestionsEl = qaDoc.createElement(QA_ALTERNATIVE_QUESTIONS_TAG);
            qaDoc.getDocumentElement().appendChild(alternativeQuestionsEl);
            for (String q:alternativeQuestions) {
                Element aQuestionEl = qaDoc.createElement(QA_QUESTION_TAG);
                aQuestionEl.appendChild(qaDoc.createTextNode(q));
                alternativeQuestionsEl.appendChild(aQuestionEl);
            }
        }

        Element answerElement = qaDoc.createElement(QA_ANSWER_TAG);
        qaDoc.getDocumentElement().appendChild(answerElement);

        if (answer.getDateAnswerModified() >= 0) {
            answerElement.setAttribute(QA_DATE_ANSWER_MODIFIED_ATTR, "" + answer.getDateAnswerModified());
        }

        log.info("Answer might be semi-structured text, check well-formedness ...");
        Document answerDoc = getWellFormedXML(answer.getAnswer());
        if (answerDoc != null) {
            NodeList answerNodes = answerDoc.getDocumentElement().getChildNodes();
            for (int i = 0; i < answerNodes.getLength(); i++) {
                Node node = answerNodes.item(i);
                // TODO: Do not add empty namespaces, e.g. <ak-exec xmlns="">
                Node copiedNode = qaDoc.importNode(node, true);
                answerElement.appendChild(copiedNode);
            }
        } else {
            log.warn("Answer is not well-formed, therefore we add answer as text node ...");
            Text answerText = qaDoc.createTextNode(stripNonValidXMLCharacters(answer.getAnswer()));
            answerElement.appendChild(answerText);
        }
        // TODO: answerElement.setAttribute(QA_QUESTION_DATE_ATTR, "" + answer.getDateAnswered().getTime());

        //if (qa.getAnswerClientSideEncryptedAlgorithm() != null) {
        if (answer.getAnswerClientSideEncryptionAlgorithm() != null) {
            answerElement.setAttribute(QA_ANSWER_CLIENT_SIDE_ENCRYPTION_ATTR, answer.getAnswerClientSideEncryptionAlgorithm());
        }

        if (answer.getType() != null) {
            answerElement.setAttribute(QA_ANSWER_TYPE_ATTR, "" + answer.getType());
        }

        if (answer.getAnswerContentType() != null) {
            answerElement.setAttribute(QA_ANSWER_CONTENT_TYPE_ATTR, answer.getAnswerContentType().toString());
        }

        List<String> classifications = answer.getClassifications();
        if (classifications != null && classifications.size() > 0) {
            Element classificationsEl = qaDoc.createElement(CLASSIFICATIONS_TAG);
            qaDoc.getDocumentElement().appendChild(classificationsEl);

            for (int i = 0; i < classifications.size(); i++) {
                Element termEl = qaDoc.createElement(CLASSIFICATION_TERM_TAG);
                termEl.appendChild(qaDoc.createTextNode(classifications.get(i)));
                classificationsEl.appendChild(termEl);
            }
        }

        if (answer.getRespondentId() != null) {
            answerElement.setAttribute(QA_ANSWER_RESPONDENT_ID_ATTR, answer.getRespondentId());
        }

        if (answer.getUrl() != null) {
            Element urlElement = qaDoc.createElement(QA_URL_TAG);
            urlElement.appendChild(qaDoc.createTextNode(answer.getUrl()));
            qaDoc.getDocumentElement().appendChild(urlElement);
        }

        if (answer.getEmail() != null && answer.getEmail().length() > 0) {
            Element sourceElement = qaDoc.createElement(QA_SOURCE_TAG);
            qaDoc.getDocumentElement().appendChild(sourceElement);
            sourceElement.setAttribute(QA_EMAIL_ATTR, answer.getEmail());
        } else {
            log.info("No email provided of the user asking the question originally.");
        }
        // TODO: Add user id in case user is registered

        // TODO: Add qa.getRespondentUserId() such that one knows who is the expert

        Permissions permissions = answer.getPermissions();
        if (permissions != null) {
            Element permissionsElement = qaDoc.createElement(QA_PERMISSIONS_TAG);
            boolean addPermissions = false;

            String[] userIds = permissions.getUserIDs();
            if (userIds != null && userIds.length > 0) {
                addPermissions = true;
                for (int i = 0; i < userIds.length; i++) {
                    addUser(permissionsElement, userIds[i]);
                }
            }

            if (permissions.isPublic()) {
                addPermissions = true;
                permissionsElement.appendChild(qaDoc.createElement(QA_PUBLIC_TAG));
            }

            if (addPermissions) {
                qaDoc.getDocumentElement().appendChild(permissionsElement);
            }
        } else {
            log.info("Answer has no permissions set.");
        }

        if (answer.getFaqLanguage() != null && answer.getFaqTopicId() != null) {
            Element faqEl = qaDoc.createElement(QA_FAQ_TAG);
            faqEl.setAttribute(QA_FAQ_LANG_ATTR, answer.getFaqLanguage().toString());
            faqEl.setAttribute(QA_FAQ_TOPIC_ID_ATTR, answer.getFaqTopicId());
            qaDoc.getDocumentElement().appendChild(faqEl);
        } else {
            Element noFaqEl = qaDoc.createElement(QA_NO_FAQ_TAG);
            qaDoc.getDocumentElement().appendChild(noFaqEl);
        }

        return qaDoc;
    }

    /**
     * @param content Text, e.g. "das <b>Zivilverfahren</b> von zen\u0002traler Bedeutung. In BGE 144 III 67"
     */
    private Document getWellFormedXML(String content) {
        Document doc = isWellFormed(content);
        if (doc != null) {
            return doc;
        } else {
            log.warn("Content is not well-formed XML, try to fix it ...");
            doc = isWellFormed(stripNonValidXMLCharacters(content));
            if (doc != null) {
                log.info("Well-formedness achieved :-)");
                return doc;
            } else {
                log.error("Could not fix well-formedness!");
                return null;
            }
        }
    }

    /**
     * Check whether content is well-formed
     * @return XML document when content well-formed and null when content not well-formed
     */
    private Document isWellFormed(String content) {
        try {
            Document answerDoc = read("<?xml version=\"1.0\"?><test-well-formedness>" + content + "</test-well-formedness>");
            log.info("Content is well-formed XML :-)");
            return answerDoc;
        } catch(Exception e) {
            log.warn("Content is not well-formed XML: " + content);
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Strip non valie XML characters
     * @param in
     * @return
     */
    private String stripNonValidXMLCharacters(String in) {
        StringBuilder out = new StringBuilder(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || ("".equals(in))) return ""; // vacancy test.
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if ((current == 0x9) ||
                    (current == 0xA) ||
                    (current == 0xD) ||
                    ((current >= 0x20) && (current <= 0xD7FF)) ||
                    ((current >= 0xE000) && (current <= 0xFFFD)) ||
                    ((current >= 0x10000) && (current <= 0x10FFFF)))
                out.append(current);
        }
        return out.toString();
    }

    /**
     *
     */
    public Document createRatingsDocument(Answer answer) {
        Rating[] ratings = answer.getRatings();
        if (ratings != null && ratings.length > 0) {
            Document qaDoc = createDocument(QA_NAMESPACE_1_0_0, QA_RATINGS_TAG);
            Element ratingsEl = qaDoc.getDocumentElement();
            for (Rating rating: ratings) {
                log.debug("Rating: " + rating.getRating());

                Element ratingEl = qaDoc.createElement(QA_RATING_TAG);
                ratingEl.setAttribute(QA_RATING_ATTR, "" + rating.getRating());
                if (rating.getFeedback() != null) {
                    Element feedbackEl = qaDoc.createElement(QA_RATING_FEEDBACK_TAG);
                    feedbackEl.appendChild(qaDoc.createTextNode(rating.getFeedback()));
                    ratingEl.appendChild(feedbackEl);
                }
                if (rating.getUserquestion() != null) {
                    Element userQuestionEl = qaDoc.createElement(QA_RATING_QUESTION_TAG);
                    userQuestionEl.appendChild(qaDoc.createTextNode(rating.getUserquestion()));
                    ratingEl.appendChild(userQuestionEl);
                    if (rating.getQuestionuuid() != null) {
                        userQuestionEl.setAttribute(QA_RATING_QUESTION_UUID_ATTR, rating.getQuestionuuid());
                    }
                }
                if (rating.getEmail() != null) {
                    Element emailEl = qaDoc.createElement(QA_RATING_EMAIL_TAG);
                    emailEl.appendChild(qaDoc.createTextNode(rating.getEmail()));
                    ratingEl.appendChild(emailEl);
                }

                ratingsEl.appendChild(ratingEl);
            }
            return qaDoc;
        } else {
            log.warn("No ratings yet.");
            return null;
        }
    }

    /**
     * Add a particular user to permissions
     * @param userId User Id of particular user
     */
    private void addUser(Element permissionsEl, String userId) {
        log.info("Add user with id '" + userId + "' to permissions ...");
        Element userEl = permissionsEl.getOwnerDocument().createElement(QA_USER_TAG);
        userEl.setAttribute("id", userId);
        permissionsEl.appendChild(userEl);
    }

    /**
     * Creates a Document instance (DOM)
     * 
     * @param namespace Namespace of root element
     * @param localname Local name of the root element
     * @return Document object representing the rootname
     * @throws RuntimeException
     */
    protected final Document createDocument(String namespace, String localname) throws RuntimeException {
        try {
            DocumentBuilder docBuilder = createBuilder(false);
            //DocumentBuilder docBuilder = createBuilder(true);
            org.w3c.dom.DOMImplementation domImpl = docBuilder.getDOMImplementation();
            org.w3c.dom.DocumentType doctype = null;
            Document doc = domImpl.createDocument(namespace, localname, doctype);
            if (namespace != null) {
                doc.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", namespace);
            }
            return doc;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException( "Unable to create a DOM document, check your xml configuration" );
        }
    }

    /**
     * Creates a non-validating and namespace-aware DocumentBuilder.
     * @param resolving True if the parser produced will resolve entities as the document is parsed; false otherwise.
     * @return A new DocumentBuilder object.
     * @throws ParserConfigurationException if an error occurs
     */
    private DocumentBuilder createBuilder(boolean resolving) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        log.info("Init DocumentBuilder ...");
        DocumentBuilder builder = factory.newDocumentBuilder();

        if (resolving) {
            log.error("Not implemented yet!");
/*
            CatalogResolver cr = new CatalogResolver();
            builder.setEntityResolver(cr);
*/
        } else {
            builder.setEntityResolver(new org.xml.sax.EntityResolver() {
                @Override
                public org.xml.sax.InputSource resolveEntity(String publicId, String systemId) throws org.xml.sax.SAXException, IOException {
                    log.warn("Do not resolve entities: " + publicId + ", " + systemId);
                    return new org.xml.sax.InputSource(new StringReader(""));
                }
            });
        }

        return builder;
    }

    /**
     * Write DOM document into output stream
     *
     * @param doc DOM document which will be written into OutputStream
     * @param out OutputStream into which the XML document is written
     * @param indent If true, then the XML will be indented
     */
    protected void writeDocument(Document doc, OutputStream out, boolean indent) throws Exception {
        if (doc == null) {
            throw new Exception("Document is null");
        } else if (out == null) {
            throw new Exception("OutputStream is null");
        } else {
            // INFO: Remove extra empty lines
            XPath xp = XPathFactory.newInstance().newXPath();
            NodeList nl = (NodeList) xp.evaluate("//text()[normalize-space(.)='']", doc, XPathConstants.NODESET);
            for (int i=0; i < nl.getLength(); ++i) {
                Node node = nl.item(i);
                node.getParentNode().removeChild(node);
            }

            int indentationValue = 0;
            if (indent) {
                indentationValue = INDENT_AMOUNT;
            }

            Transformer t = getXMLidentityTransformer(indentationValue);
            t.transform(new javax.xml.transform.dom.DOMSource(doc), new javax.xml.transform.stream.StreamResult(out));
            out.close();
        }
    }

    /**
     * @param indentation If greater than zero, then the XML will be indented by the value specified
     */
    private Transformer getXMLidentityTransformer(int indentation) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer t = factory.newTransformer(); // identity transform
        t.setOutputProperty(OutputKeys.INDENT, (indentation != 0) ? YES : NO);
        t.setOutputProperty(OutputKeys.METHOD, "xml"); //xml, html, text
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "" + indentation);
        return t;
    }
}
