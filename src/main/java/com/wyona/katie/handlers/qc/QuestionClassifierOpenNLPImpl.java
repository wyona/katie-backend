package com.wyona.katie.handlers.qc;

import com.wyona.katie.handlers.GenerateProvider;
import com.wyona.katie.handlers.MistralAIGenerate;
import com.wyona.katie.handlers.OllamaGenerate;
import com.wyona.katie.models.*;
import com.wyona.katie.services.NamedEntityRecognitionService;
import lombok.extern.slf4j.Slf4j;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * See https://pm.wyona.com/issues/2686
 */
@Slf4j
@Component
public class QuestionClassifierOpenNLPImpl implements QuestionClassifier {

    @Autowired
    private NamedEntityRecognitionService nerService;

    @Value("${mistral.api.key}")
    private String mistralAIKey;

    @Value("${mistral.ai.completion.model}")
    private String mistralAIModel;

    @Autowired
    private MistralAIGenerate mistralAIGenerate;

    @Autowired
    private OllamaGenerate ollamaGenerate;

    @Value("${ollama.completion.model}")
    private String ollamaModel;

    @Value("${re_rank.llm.impl}")
    private CompletionImpl completionImpl;

    /**
     * @see QuestionClassifier#analyze(String, Context)
     */
    public AnalyzedMessage analyze(String messageX, Context domain) {
        if (messageX.isEmpty()) {
            log.warn("Message is empty!");
        }
        log.info("Analyze message '" + messageX + "' ...");

        String _message = messageX;

        // TODO: Make sure "Date sent:" and "Subject:" is located at the beginning of the text
        if (messageX.contains("Date sent:") && messageX.contains("Subject:")) {
            _message = getEmailBody(_message);
        }

        // TODO: Implement more sophisticated question detection algorithm. Also try to differentiate between questions asking for an opinion (e.g. "What time would be good for lunch?") and knowledge questions (e.g. "What is the highest mountain in Europe?")
        // INFO: Also see the book "Natural Language Processing with Transformers" published by O'Reilly, Chapter 7. Question Answering
        // TODO: Consider similar implementation like "Sentiment Analysis", see for example https://monkeylearn.com/sentiment-analysis-online/ or https://github.com/eclipse/deeplearning4j-examples/blob/master/dl4j-examples/src/main/java/org/deeplearning4j/examples/advanced/modelling/textclassification/pretrainedword2vec/ImdbReviewClassificationRNN.java or https://github.com/eclipse/deeplearning4j-examples/blob/master/dl4j-examples/src/main/java/org/deeplearning4j/examples/advanced/modelling/textclassification/pretrainedword2vec/ImdbReviewClassificationCNN.java https://opendatahub.io/news/2019-09-04/sentiment-analysis-blog.html

        // TODO: Implement as a pipeline, e.g. https://scikit-learn.org/stable/modules/generated/sklearn.pipeline.Pipeline.html

        log.info("Split the following message into sentence: '" + _message + "'");
        String[] sentences = detectSentences(_message);
        for (int i = 0; i < sentences.length; i++) {
            log.info("Detected Sentence: " + sentences[i]);
        }

        String[] questions = detectQuestions(sentences);
        for (int i = 0; i < questions.length; i++) {
            log.debug("Detected Question: " + questions[i]);
        }

        AnalyzedMessage analyzedMessage = new AnalyzedMessage(_message, QuestionClassificationImpl.OPEN_NLP);

        // TODO: getClassifications(message);
        //analyzedMessage.addClassification("TODO");

        if (questions.length > 0) {
            log.info("Message does contain " + questions.length + " question(s).");
            analyzedMessage.setContainsQuestions(true);
        } else {
            log.info("Message does not contain question.");
            analyzedMessage.setContainsQuestions(false);
        }

        for (int i = 0; i < questions.length; i++) {
            // TODO: Include type of question, e.g. "QUESTION_OPINION", "QUESTION_COMMUNICATION", "QUESTION_SOCIAL" (e.g. "What time should we meet for lunch?"), "QUESTION_RHETORICAL"
            if (nerService != null) {
                log.debug("Add question / context: " + questions[i]);
                Sentence qest = nerService.analyze(questions[i], null, domain);
                Sentence cont = new Sentence("TODO", null, null);
                analyzedMessage.addQuestionAndContext(qest, cont);
            } else {
                log.error("NER service not initialized!");
            }
        }

        return analyzedMessage;

        // http://qcapi.harishmadabushi.com/?auth=29dkcmxwel&question=How%20many%20Abercrombie%20and%20Fitch%20stores%20are%20there?
        // TODO: Question Classification API Documentation: https://www.harishtayyarmadabushi.com/research/questionclassification/question-classification-api-documentation/ , https://aclanthology.org/C18-1278.pdf
        // TODO: Semantic Parsing of Technical Support Questions: https://aclanthology.org/C18-1275.pdf
        // TODO: https://towardsdatascience.com/how-to-use-ai-to-detect-open-ended-questions-for-non-datascientists-e2ef02427422
        // TODO: Detection of Question-Answer Pairs in Email Conversations: https://aclanthology.org/C04-1128.pdf
        // TODO: DeepDup: Duplicate Question Detection in Community Question Answering: https://webdocs.cs.ualberta.ca/~zaiane/postscript/DeepDup2021.pdf
    }

    /**
     *
     */
    private String getEmailBody(String message) {
        GenerateProvider generateMistralCloud = mistralAIGenerate;
        GenerateProvider generateOllama = ollamaGenerate;

        List<PromptMessage> promptMessages = new ArrayList<>();
        String prompt = "Please split the following email into Salutation, Body and Signature and provide the response as JSON: \"" + message + "\"";
        promptMessages.add(new PromptMessage(PromptMessageRole.USER, prompt));
        try {
            Double temperature = null;
            String completedText = null;
            if (completionImpl.equals(CompletionImpl.MISTRAL_AI)) {
                completedText = generateMistralCloud.getCompletion(promptMessages, null, null, mistralAIModel, temperature, mistralAIKey).getText();
            } else {
                completedText = generateOllama.getCompletion(promptMessages, null, null, ollamaModel, temperature, null).getText();
            }

            log.info("Completed text: " + completedText);
                /*
                {
                "email": {
                   "salutation": "Hallo liebes Supportteam",
                   "body": "Wir können uns mit unserem Management Server nicht mehr verbinden. Logins: mchung-adm izafei-adm tknaeh-adm Wir bitten um Problembehebung.",
                   "signature": "Mit freundliche Grüssen\nIoannis Zafeiropoulos\nInformatik Universität Zürich\nFakultät Vetsuisse\nWinterthurerstrasse 260 CH-8057 Zürich\nTel: +41 44 635 91 46"
                   }
                }
                */
            int pos = completedText.indexOf("\"body\":");
            if (pos == -1) {
                pos = completedText.indexOf("\"Body\":");
            }
            if (pos > 0) {
                message = completedText.substring(pos + 7); // INFO: Remove body string
                message = message.trim();
                message = message.substring(1, message.indexOf("\",")); // INFO: Ignore start quote and select until end quote
                log.info("Body: " + message);
            } else {
                log.warn("No body found!");
            }
                /* TODO: JSON might not be valid
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(completedText);
                if (rootNode != null) {
                    if (rootNode.has("Email")) {
                        JsonNode emailNode = rootNode.get("Email");
                        if (emailNode.has("Body")) {
                            _message = emailNode.get("Body").asText();
                            log.info("Email body detected: " + _message);
                        }
                    } else {
                        log.warn("No node 'Email' available.");
                    }
                }
                */
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return message;
    }

    /**
     * Get number of question marks contained by message
     */
    private long getNumberOfQuestionmarks(String message) {
        List<Character> messageAsCharList = message.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        long numberOfQuestionmarksPresent = IntStream.range(0, messageAsCharList.size()).filter(c -> messageAsCharList.get(c) == '?' && !isQuestionMarkContainedByALink(c, message)).count();
        return numberOfQuestionmarksPresent;
    }

    /**
     * Check whether question mark might be contained by a link within the message
     * @param questionMarkPos Question mark position
     * @param message Message containing question mark
     * @return true when question mark is contained by a link and false otherwise
     */
    private boolean isQuestionMarkContainedByALink(int questionMarkPos, String message) {
        int linkStart = message.indexOf("http"); // INFO: Checking for http is also covering https
        if (linkStart >= 0 && linkStart < questionMarkPos) { // INFO: Example link https://issues.apache.org/jira/browse/NLPCRAFT-241?page=com.atlassian.jira.plugin.system.issuetabpanels%3Aall-tabpanel
            String link = getLink(linkStart, message);
            log.info("Check whether Link '" + link + "' might contain a query string with a question mark ...");
            if (message.substring(linkStart, questionMarkPos).contains(" ")) {
                // INFO: Question mark is not part of link
                return false;
            } else {
                log.info("Link '" + link + "' contains the question mark, therefore message is not considered a question.");
                return true;
            }
        } else {
            // INFO: Message does not contain a link starting with http or https
            return false;
        }
    }

    /**
     *
     */
    private String getLink(int linkStart, String message) {
        String link = message.substring(linkStart);
        int linkEnd = link.indexOf(" ");
        if (linkEnd > 0) {
            return link.substring(0, linkEnd);
        } else {
            return link;
        }
    }

    /**
     * Check whether message contains english question words, e.g. "Who", "Which", "How", "What", "Where", "Why", "When", ...
     * @param message Message which might contain a question, e.g. "If not, how come they are both 0 while there is an answer extracted" or "about the dynamic scaling which is in your roadmap, any idea or any news about this subject"
     * @return true when message contains an english question word and false otherwise
     */
    private boolean containsEnglishQuestionWords(String message) {
        // Who: Person
        // Where: Position, Place
        // When: Time, Occasion, Moment
        // Why: Reason, Explanation
        // What: Specific thing, Object
        // Which: Choice, Alternative
        // How: Way, Manner, Form
        return false;
    }

    /**
     * Check whether message contains english "question" words, e.g. "Is", "Do", "Does", "Was", "Could", "Can", ...
     * @param message Message which might contain a question, e.g. "Could that be the case" or "Does it have to be configured differently"
     */
    private boolean todo(String message) {
        return false;
    }

    /**
     * Get classification of message / question, e.g. "num", "bug", "instruction", "fact", "social", ...
     * @return classification class, e.g. "announcement" or "[0,0,1]" (https://en.wikipedia.org/wiki/Multi-label_classification)
     */
    private List<String> getClassifications(String message) {
        // TODO: Classify message
        return new ArrayList<String>();
    }

    /**
     * Split message into sentences
     */
    private String[] detectSentences(String message) {
        // TODO: Detect language of message, e.g. https://opennlp.apache.org/docs/2.3.1/manual/opennlp.html#tools.langdetect
        String language = "en";
        //String language = "de";

        try {
            // INFO: http://opennlp.sourceforge.net/models-1.5/
            java.io.InputStream in = new ClassPathResource("opennlp/" + language + "-sent.bin").getInputStream();
            SentenceModel model = new SentenceModel(in);
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
            return sentenceDetector.sentDetect(message);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new String[0];
        }
    }

    /**
     * Detect questions
     * @return sentences which are questions
     */
    private String[] detectQuestions(String[] sentences) {
        List<String> questions = new ArrayList<String>();
        for (int i = 0; i < sentences.length; i++) {
            if (getNumberOfQuestionmarks(sentences[i]) > 0) {
                questions.add(sentences[i]);
            }
        }
        return questions.toArray(new String[0]);
    }
}
