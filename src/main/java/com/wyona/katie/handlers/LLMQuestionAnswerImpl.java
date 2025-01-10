package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import com.wyona.katie.services.GenerativeAIService;
import com.wyona.katie.services.KnowledgeSourceXMLFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 */
@Slf4j
@Component
public class LLMQuestionAnswerImpl implements QuestionAnswerHandler {

    @Autowired
    private GenerativeAIService generativeAIService;

    @Autowired
    private KnowledgeSourceXMLFileService knowledgeSourceXMLFileService;

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) {
        log.info("TODO: LLM search implementation of deleting tenant ...");
        // TODO
    }

    /**
     * @see QuestionAnswerHandler#createTenant(Context)
     */
    public String createTenant(Context domain) {
        log.info("TODO: LLM search implementation of creating tenant ...");
        // TODO
        return null;
    }

    /**
     * @see QuestionAnswerHandler#delete(String, Context)
     */
    public boolean delete(String uuid, Context domain) {
        log.info("TODO: Delete Q&A with UUID '" + uuid + "' of domain '" + domain.getId() + "' from LLM search implementation ...");
        // TODO
        return false;
    }

    /**
     * @see QuestionAnswerHandler#retrain(QnA, Context, boolean)
     */
    public void retrain(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Delete/train is just a workaround, implement retrain by itself");
        if (delete(qna.getUuid(), domain)) {
            train(qna, domain, indexAlternativeQuestions);
        } else {
            log.warn("QnA with UUID '" + qna.getUuid() + "' was not deleted and therefore was not retrained!");
        }
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context context, boolean indexAlternativeQuestions) {
        log.info("TODO: Index QnA '" + qna.getUuid() + "' with LLM search implementation ...");
        // TODO
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public QnA[] train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) {
        log.info("TODO: Implement batch training.");
        for (QnA qna: qnas) {
            train(qna, domain, indexAlternativeQuestions);
        }

        // TODO: Only return QnAs which got trained successfully
        return qnas;
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(Sentence, Context, int)
     */
    public Hit[] getAnswers(Sentence question, Context context, int limit) {
        log.info("TODO: Consider using entities!");
        return getAnswers(question.getSentence(), question.getClassifications(), context, limit);
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(String, List, Context, int)
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context domain, int limit) {
        List<Hit> hits = new ArrayList<Hit>();

        log.info("Get answer from LLM search implementation ...");

        String _answer = null;

        File[] relevantDocs = null;
        if (true) {
            try {
                relevantDocs = getRelevantDocuments(question, classifications, domain);
                for (File relevantDoc: relevantDocs) {
                    log.info("Relevant document: " + relevantDoc.getAbsolutePath());
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                _answer = "ERROR: " + e.getMessage();
            }
        } else {
            relevantDocs = new File[1];
            relevantDocs[0] = new File("/Users/michaelwechner/Desktop/Auftragsrecht.pdf");
            log.warn("Use mock document: " + relevantDocs[0].getAbsolutePath());
        }

        if (true && _answer == null) {
            if (relevantDocs != null && relevantDocs.length > 0) {
                _answer = getAnswerFromRelevantDocuments(relevantDocs, question, classifications, domain);
                log.info("Answer from getAnswerFromRelevantDocuments(): " + _answer);
            } else {
                _answer = "No relevant documents found!";
                log.warn(_answer);
            }
        }

        String uuid = null;
        ContentType answerContentType = ContentType.TEXT_PLAIN;
        String orgQuestion = null;
        Date dateAnswered = null;
        Date dateAnswerModified = null;
        Date dateOriginalQuestionSubmitted = null;
        Answer answer = new Answer(question, _answer, answerContentType,null, classifications, null, null, dateAnswered, dateAnswerModified, null, domain.getId(), uuid, orgQuestion, dateOriginalQuestionSubmitted, true, null, true, null);
        if (relevantDocs != null && relevantDocs.length > 0) {
            for (File relevantDoc : relevantDocs) {
                String relevantTextContext = relevantDoc.getName(); // TODO: Replace by relevant text from within document
                answer.addRelevantContext(new AnswerContext(relevantTextContext, relevantDoc.toURI()));
            }
        }

        double score = -1; // TODO: Get score
        hits.add(new Hit(answer, score));

        return hits.toArray(new Hit[0]);
    }

    /**
     * Retrieval: Get relevant documents
     * @return list of relevant documents
     */
    private File[] getRelevantDocuments(String question, List<String> classifications, Context domain) throws Exception {
        File baseDiretory = getBaseDirectory(domain);

        PromptMessage promptMessage = new PromptMessage();
        promptMessage.setRole(PromptMessageRole.USER);
        promptMessage.setContent("Which document from the attached list of documents is relevant in connection with the following question \"" + question + "\"\n\nIf the attached list of documents does not contain any relevant document, then answer with N/A, otherwise make sure to return the file path of the relevant document.");
        File[] attachments = new File[1];
        attachments[0] = new File(domain.getContextDirectory(), "documents.json");
        promptMessage.setAttachments(attachments);

        List<PromptMessage> promptMessages = new ArrayList<>();
        promptMessages.add(promptMessage);

        GenerateProvider generateProvider = generativeAIService.getGenAIImplementation(domain.getCompletionImpl());
        String model = generativeAIService.getCompletionModel(domain.getCompletionImpl());
        String apiToken = generativeAIService.getApiToken(domain.getCompletionImpl());
        Double temperature = null;

        List<File> relevantDocs = new ArrayList<>();
        try {
            List<CompletionTool> tools = new ArrayList<>();
            CompletionTool getFilePathTool = new CompletionTool("function");
            getFilePathTool.setFunctionArgument("file_path");
            // TODO: Finish tool / function definition (See OpenAIGenerate#createAssistant(...)

            tools.add(getFilePathTool);

            CompletionResponse completionResponse = generateProvider.getCompletion(promptMessages, tools, model, temperature, apiToken);
            log.info("Answer getRelevantDocuments(): " + completionResponse.getText());
            String filePath = completionResponse.getFunctionArgumentValue(getFilePathTool.getFunctionArgument());
            if (filePath != null) {
                relevantDocs.add(new File(baseDiretory, filePath));
            } else {
                log.warn("No relevant document(s) found!");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }

        return relevantDocs.toArray(new File[0]);
    }

    /**
     * Get base directory containing documents
     */
    private File getBaseDirectory(Context domain) throws Exception {
        KnowledgeSourceMeta[] knowledgeSourceMetas = knowledgeSourceXMLFileService.getKnowledgeSources(domain.getId());
        if (knowledgeSourceMetas.length > 0) {
            for (KnowledgeSourceMeta ksMeta : knowledgeSourceMetas) {
                if (ksMeta.getConnector() == KnowledgeSourceConnector.FILESYSTEM) {
                    if (!ksMeta.getIsEnabled()) {
                        log.warn("Knowledge source is disabled!");
                        String errMsg = "Knowledge source '" + ksMeta.getId() + "' of domain '" + domain + "' is disabled!";
                        log.warn(errMsg);
                        throw new Exception(errMsg);
                    }
                    // TODO: Get base directory from knowledge source
                    //baseDiretory = new File(ksMeta.getFilesystemBasePath());
                }
            }
            return new File(domain.getContextDirectory(), "documents");
        } else {
            String errMsg = "Domain '" + domain + "' has no " + KnowledgeSourceConnector.FILESYSTEM + " knowledge source configured!";
            log.warn(errMsg);
            throw new Exception(errMsg);
        }
    }

    /**
     * RAG: Generate answer based on relevant documents
     * @param relevantDocuments Paths of relevant documents
     */
    private String getAnswerFromRelevantDocuments(File[] relevantDocuments, String question, List<String> classifications, Context domain) {
        PromptMessage promptMessage = new PromptMessage();
        promptMessage.setRole(PromptMessageRole.USER);
        // TODO: Make language configurable
        promptMessage.setContent("Wie lautet auf der Grundlage des beigefügten Dokuments die Antwort auf die folgende Frage \"" + question + "\"");
        //promptMessage.setContent("Based on the attached document, what is the answer to the following question \"" + question + "\"");
        promptMessage.setAttachments(relevantDocuments);

        List<PromptMessage> promptMessages = new ArrayList<>();
        promptMessages.add(promptMessage);

        GenerateProvider generateProvider = generativeAIService.getGenAIImplementation(domain.getCompletionImpl());
        String model = generativeAIService.getCompletionModel(domain.getCompletionImpl());
        String apiToken = generativeAIService.getApiToken(domain.getCompletionImpl());
        Double temperature = null;

        try {
            String answer = generateProvider.getCompletion(promptMessages, null, model, temperature, apiToken).getText();
            return answer;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }
}
