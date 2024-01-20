package com.wyona.katie.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock implementation to return answer from text
 */
@Slf4j
@Component
public class AnswerFromTextServiceMockImpl implements AnswerFromTextService {

    private final static int MAX_LENGTH = 100;

    /**
     * @see AnswerFromTextService#getAnswerFromText(String, String)
     */
    public String getAnswerFromText(String question, String text) {
        log.info("Get answer from text using Mock implementation ...");
        if (text.length() > MAX_LENGTH) {
            return text.substring(0, MAX_LENGTH) + " ...";
        } else {
            return text;
        }
    }
}
