package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import com.wyona.katie.services.CardKeyword;
import com.wyona.katie.services.KeywordsExtractor;
import com.wyona.katie.services.Utils;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Katie experimental retrieval implementation
 */
@Slf4j
@Component
public class KatieExperimentalRetrievalImpl implements QuestionAnswerHandler {

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) {
        log.info("Delete tenant, nothing to delete though yet.");
    }

    /**
     * @see QuestionAnswerHandler#createTenant(Context)
     */
    public String createTenant(Context domain) {
        log.info("Create tenant, nothing to create though yet.");
        return null;
    }

    /**
     * @see QuestionAnswerHandler#delete(String, Context)
     */
    public boolean delete(String uuid, Context domain) {
        log.error("TODO: Implement delete(String, Context) ...");
        return false;
    }

    /**
     * @see QuestionAnswerHandler#retrain(QnA, Context, boolean)
     */
    public void retrain(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.error("TODO: Finish implementation of retrain(QnA, Context, boolean) ...");

        // TODO: Consider deleting old references
        train(qna, domain, indexAlternativeQuestions);
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public QnA[] train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Finish implementation to index more than one QnA at the same time!");
        for (QnA qna: qnas) {
            train(qna, domain, indexAlternativeQuestions);
        }

        // TODO: Only return QnAs which got trained successfully
        return qnas;
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.error("TODO: Finish implementation of train(QnA, Context, boolean) ...");
        Language languageOfQnA = null;
        try {
            List<CardKeyword> keywords = KeywordsExtractor.getKeywordsV2(Utils.getContent(qna), languageOfQnA, true);
            for (CardKeyword keyword : keywords) {
                log.info("Keyword: " + keyword);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(Sentence, Context, int)
     */
    public Hit[] getAnswers(Sentence question, Context context, int limit) {
        List<Hit> hits = new ArrayList<>();
        log.error("TODO: Implement getAnswers(Sentence, Context, int) ...");

        Language languageOfQuestion = null;
        try {
            List<CardKeyword> keywords = KeywordsExtractor.getKeywordsV2(question.getSentence(), languageOfQuestion, true);
            for (CardKeyword keyword : keywords) {
                log.info("Keyword: " + keyword);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return hits.toArray(new Hit[0]);
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(String, List, Context, int)
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context domain, int limit) {
        log.error("TODO: Implement getAnswers(String, List, Context, int) ...");
        return null;
    }
}
