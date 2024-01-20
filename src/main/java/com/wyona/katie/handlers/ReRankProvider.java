package com.wyona.katie.handlers;

/**
 * Re-rank provider, e.g. SentenceBERT, Cohere, ...
 */
public interface ReRankProvider {

    /**
     * Re-rank answers
     * @param question Question, e.g. "How old is Michael?"
     * @param answers Array of answers, e.g. ["Michael lives in Switzerland", "Michael is 53 years old", "Michael was born 1969"]
     * @param limit Limit of returned re-ranked answers
     * @return index array of re-ranked answers, e.g. [1, 2, 0]
     */
    public Integer[] getReRankedAnswers(String question, String[] answers, int limit) throws Exception;
}
