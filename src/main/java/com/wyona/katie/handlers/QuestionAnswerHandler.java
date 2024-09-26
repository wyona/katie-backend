package com.wyona.katie.handlers;

import com.wyona.katie.models.Context;
import com.wyona.katie.models.Hit;
import com.wyona.katie.models.QnA;
import com.wyona.katie.models.Sentence;
import com.wyona.katie.models.Context;

import java.util.List;

/**
 * Interfaces to train QnAs and search for answers
 */
public interface QuestionAnswerHandler {

    /**
     * Get answer(s) (including confidence score) for a particular question associated with a particular domain
     * @param question Question, e.g. "What is the address of the headquarter?" or "How old is Michael?"
     * @param classifications Classifications, e.g. "num", "date", "code"
     * @param domain Domain, e.g. "wyona"
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context domain, int limit) throws Exception;

    /**
     * Get answer(s) (including confidence score) for a particular question associated with a particular domain
     * @param question Question including entities, e.g. "How old is Michael?" whereas "Michael" is a PERSON entity, and including classification, e.g. "credentials" or "performance"
     * @param domain Domain, e.g. "wyona"
     */
    public Hit[] getAnswers(Sentence question, Context domain, int limit) throws Exception;

    /**
     * Add/train a particular question/answer associated with a particular domain
     * @param qna Question and answer
     * @param domain Domain, e.g. "wyona"
     * @param indexAlternativeQuestions When set to true, then alternative questions are also indexed
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) throws Exception;

    /**
     * Add/train several QnAs for a particular domain
     * @param qnas Array of questions and answers
     * @param domain Domain, e.g. "wyona"
     * @param indexAlternativeQuestions When set to true, then alternative questions are also indexed
     * @return trained QnAs
     */
    public QnA[] train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) throws Exception;

    /**
     * Retrain a particular question/answer associated with a particular domain
     * @param qna Question and answer
     * @param domain Domain, e.g. "wyona"
     * @param indexAlternativeQuestions When set to true, then alternative questions are also indexed
     */
    public void retrain(QnA qna, Context domain, boolean indexAlternativeQuestions) throws Exception;

    /**
     * Delete particular QnA
     * @param uuid UUID of QnA, e.g. "93d29be2-0618-4397-98ad-80836ec80a09"
     * @param domain Domain, e.g. "wyona"
     * @return true when QnA was deleted successfully and false otherwise
     */
    public boolean delete(String uuid, Context domain);

    /**
     * Create tenant
     * @param domain Domain within Katie
     * @return tenant end point information, e.g. elasticsearch index name "askkatie_5bd57b92-da98-422f-8ad6-6670b9c69184" or DeepPavlov base url "https://deeppavlov.wyona.com"
     */
    public String createTenant(Context domain) throws Exception;

    /**
     * Delete tenant
     * @param domain Domain within Katie
     */
    public void deleteTenant(Context domain);
}
