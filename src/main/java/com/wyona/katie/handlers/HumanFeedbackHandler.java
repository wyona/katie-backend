package com.wyona.katie.handlers;

import com.wyona.katie.models.Context;
import com.wyona.katie.models.Rating;
import com.wyona.katie.models.User;

/**
 * Interfaces to index and retrieve human feedback
 */
public interface HumanFeedbackHandler {

    /**
     * Index human feedback, such that it can be reused to answer questions in the future
     * @param question Question asked by user
     * @param answerUuid UUID of QnA which was used as answer to the question of user
     * @param domain Domain associated with QnA
     * @param rating Rating of user re answer (0: completely wrong, 10: completely correct)
     * @param user User which asked question. If user anonymous, then null
     */
    public void indexHumanFeedback(String question, String answerUuid, Context domain, int rating, User user) throws Exception;

    /**
     * Retrieve human feedback
     * @param question Asked question
     * @param domain Domain associated with asked question
     */
    public Rating[] getHumanFeedback(String question, Context domain) throws Exception;
}
