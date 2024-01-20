package com.wyona.katie.services;

/**
 * Get answer from text, e.g. content of a web page
 */
public interface AnswerFromTextService {

    /**
     * @param question Question, e.g. "What is the address of Wyona?"
     * @param text Text containing answer, e.g. "The headquarter of Wyona is located at Fritz-Fleiner-Weg 9, 8044 Zürich, Switzerland"
     * @return answer, e.g. "The address of Wyona is Fritz-Fleiner-Weg 9, 8044 Zürich, Switzerland"
     */
    public String getAnswerFromText(String question, String text);
}
