package com.wyona.katie.services;

import com.wyona.katie.handlers.qc.QuestionClassifierOpenNLPImpl;
import com.wyona.katie.handlers.qc.QuestionClassifierRestImpl;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * See https://pm.wyona.com/issues/2686
 */
@Slf4j
@Component
public class QuestionAnalyzerService {

    @Value("${qc.implementation}")
    private QuestionClassificationImpl qcDefaultImpl;

    @Autowired
    private QuestionClassifierRestImpl restImpl;

    @Autowired
    private QuestionClassifierOpenNLPImpl openNLPImpl;

    /**
     * Analyze message, e.g. check whether message contains questions
     * @param message Message/Text, which might contain a question, e.g. "What time is it?" or "What time should we meet for lunch?" or "Let's meet at noon for lunch"
     * @return analyzed message
     */
    public AnalyzedMessage analyze(String message, Context domain) {
        if (qcDefaultImpl.equals(QuestionClassificationImpl.OPEN_NLP)) {
            return openNLPImpl.analyze(message, domain);
        } else if (qcDefaultImpl.equals(QuestionClassificationImpl.REST)) {
            return restImpl.analyze(message, domain);
        } else {
            log.error("No such question classifier implementation '" + qcDefaultImpl + "'!");
            return null;
        }
    }

    /**
     * Analyze message, e.g. check whether message contains questions
     * @param message Message/Text, which might contain a question, e.g. "What time is it?" or "What time should we meet for lunch?" or "Let's meet at noon for lunch"
     * @return analyzed message
     */
    public AnalyzedMessage analyze(String message, Context domain, QuestionClassificationImpl impl) {
        if (impl.equals(QuestionClassificationImpl.OPEN_NLP)) {
            return openNLPImpl.analyze(message, domain);
        } else if (impl.equals(QuestionClassificationImpl.REST)) {
            return restImpl.analyze(message, domain);
        } else {
            log.error("No such question classifier implementation '" + impl + "'!");
            return null;
        }
    }

    /**
     * Get message/question classification default implementation
     */
    public QuestionClassificationImpl getQcImpl() {
        return qcDefaultImpl;
    }
}
