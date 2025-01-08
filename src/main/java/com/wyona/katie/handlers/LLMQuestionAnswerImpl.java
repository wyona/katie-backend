package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import com.wyona.katie.services.GenerativeAIService;
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

        if (true) {
            _answer = getRelevantDocuments(question, classifications, domain);
            log.info("Answer from getRelevantDocuments(): " + _answer);
        }
        File[] relevantDocs = new File[1];
        relevantDocs[0] = new File("/Users/michaelwechner/Desktop/Auftragsrecht.pdf"); // TODO: Replace hard coded file

        if (false) {
            _answer = getAnswerFromRelevantDocuments(relevantDocs, question, classifications, domain);
            log.info("Answer from getAnswerFromRelevantDocuments(): " + _answer);
        }

        String uuid = null;

        ContentType answerContentType = ContentType.TEXT_PLAIN;
        String orgQuestion = null;
        Date dateAnswered = null;
        Date dateAnswerModified = null;
        Date dateOriginalQuestionSubmitted = null;
        Answer answer = new Answer(question, _answer, answerContentType,null, classifications, null, null, dateAnswered, dateAnswerModified, null, domain.getId(), uuid, orgQuestion, dateOriginalQuestionSubmitted, true, null, true, null);
        for (File relevantDoc: relevantDocs) {
            String relevantTextContext = relevantDoc.getName(); // TODO: Replace by relevant text from within document
            answer.addRelevantContext(new AnswerContext(relevantTextContext, relevantDoc.toURI()));
        }

        double score = -1; // TODO: Get score
        hits.add(new Hit(answer, score));

        return hits.toArray(new Hit[0]);
    }

    /**
     * Retrieval: Get relevant documents
     * @return list of relevant documents
     */
    private String getRelevantDocuments(String question, List<String> classifications, Context domain) {
        PromptMessage promptMessage = new PromptMessage();
        promptMessage.setRole(PromptMessageRole.USER);
        promptMessage.setContent("Which document from the attached list is relevant in connection with the following question \"" + question + "\"");
        File[] attachments = new File[1];
        attachments[0] = new File(domain.getContextDirectory(), "documents.json");
        promptMessage.setAttachments(attachments);

        List<PromptMessage> promptMessages = new ArrayList<>();
        promptMessages.add(promptMessage);

        GenerateProvider generateProvider = generativeAIService.getGenAIImplementation(domain.getCompletionImpl());
        String model = generativeAIService.getCompletionModel(domain.getCompletionImpl());
        String apiToken = generativeAIService.getApiToken(domain.getCompletionImpl());
        Double temperature = null;

        try {
            String answer = generateProvider.getCompletion(promptMessages, model, temperature, apiToken);
            return answer;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }

    /**
     * RAG: Generate answer based on relevant documents
     * @param relevantDocuments Paths of relevant documents
     */
    private String getAnswerFromRelevantDocuments(File[] relevantDocuments, String question, List<String> classifications, Context domain) {
        PromptMessage promptMessage = new PromptMessage();
        promptMessage.setRole(PromptMessageRole.USER);
        promptMessage.setContent("Based on the attached document, what is the answer to the following question \"" + question + "\"");
        promptMessage.setAttachments(relevantDocuments);

        List<PromptMessage> promptMessages = new ArrayList<>();
        promptMessages.add(promptMessage);

        GenerateProvider generateProvider = generativeAIService.getGenAIImplementation(domain.getCompletionImpl());
        String model = generativeAIService.getCompletionModel(domain.getCompletionImpl());
        String apiToken = generativeAIService.getApiToken(domain.getCompletionImpl());
        Double temperature = null;

        try {
            String answer = generateProvider.getCompletion(promptMessages, model, temperature, apiToken);
            return answer;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }
}
