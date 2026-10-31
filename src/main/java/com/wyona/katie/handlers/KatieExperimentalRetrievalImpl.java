package com.wyona.katie.handlers;

import com.wyona.katie.models.Context;
import com.wyona.katie.models.Hit;
import com.wyona.katie.models.QnA;
import com.wyona.katie.models.Sentence;
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
        log.error("TODO: Implement retrain(QnA, Context, boolean) ...");
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public QnA[] train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) {
        log.error("TODO: Implement train(QnA[], Context, boolean) ...");
        return qnas;
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.error("TODO: Implement train(QnA, Context, boolean) ...");
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(Sentence, Context, int)
     */
    public Hit[] getAnswers(Sentence question, Context context, int limit) {
        List<Hit> hits = new ArrayList<>();
        log.error("Implement getAnswers(Sentence, Context, int) ...");
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
