package com.wyona.katie.services;

import com.wyona.katie.handlers.qc.QuestionClassifierOpenNLPImpl;
import com.wyona.katie.handlers.qc.QuestionClassifierRestImpl;
import com.wyona.katie.models.QuestionClassificationImpl;
import com.wyona.katie.handlers.qc.QuestionClassifierRestImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class QuestionAnalyzerServiceTest {
    private final QuestionAnalyzerService qas = new QuestionAnalyzerService();

    private final QuestionClassifierOpenNLPImpl openNlpImpl = new QuestionClassifierOpenNLPImpl();

    private final QuestionClassifierRestImpl restImpl = new QuestionClassifierRestImpl();

    //private final String questionClassifierScheme = "http";
    //private final String questionClassifierHostname = "localhost";
    //private final String questionClassifierPort = "5001";

    private final String questionClassifierScheme = "https";
    private final String questionClassifierHostname = "questionclassifier.ukatie.com";
    private final String questionClassifierPort = "443";

    private final String questionClassifierCertaintyThreshold = "0.55";

    /***
     * TODO: separate network service test and ? recognizer test. network test needs elaborate setup with emailing etc
     * Checks if input is recognized as a question
     */
    @Test
    void containsQuestion() {
        // INFO: Injecting fields for @Value and @Autowired during testing
        ReflectionTestUtils.setField(restImpl, "questionClassifierScheme", questionClassifierScheme);
        ReflectionTestUtils.setField(restImpl, "questionClassifierHostname", questionClassifierHostname);
        ReflectionTestUtils.setField(restImpl, "questionClassifierPort", questionClassifierPort);
        ReflectionTestUtils.setField(restImpl, "questionClassifierCertaintyThreshold", questionClassifierCertaintyThreshold);

        ReflectionTestUtils.setField(qas, "restImpl", restImpl);
        ReflectionTestUtils.setField(qas, "openNLPImpl", openNlpImpl);

        // INFO: Either use REST or OpenNLP implementation
        ReflectionTestUtils.setField(qas, "qcDefaultImpl", QuestionClassificationImpl.REST);
        //ReflectionTestUtils.setField(qas, "qcDefaultImpl", QuestionClassificationImpl.OPEN_NLP); // WARN: OpenNLP implementation is using NER service, which is not getting initialized when running tests

        log.info("Test message/question classification implementation: " + qas.getQcImpl());

        // No question
        assertFalse(qas.analyze("Hello there! Nice to meet you!", null).getContainsQuestions());
        // input with '*?'
        assertTrue(qas.analyze("What day is it?", null).getContainsQuestions());
        // input with ' ?'
        assertTrue(qas.analyze("What day is it ?", null).getContainsQuestions());
        // input with ' ? '
        assertTrue(qas.analyze("What day is it ? And I need the month too", null).getContainsQuestions());
        // input with '*?*'
        assertTrue(qas.analyze("What day is it?And I need the month too", null).getContainsQuestions());
        // input with '*?*' in link http
        assertFalse(qas.analyze("http://www.google.com/search?query=blablabla", null).getContainsQuestions());
        // input with '*?*' in link https
        assertFalse(qas.analyze("https://www.google.com/search?query=blablabla", null).getContainsQuestions());
        // input with legitimate ? and bad ? in http link
        assertTrue(qas.analyze("What day is it? I can't find the answer here http://www.google.com/search?query=blablabla", null).getContainsQuestions());
        // input with legitimate ? and bad ? in https link
        assertTrue(qas.analyze("What day is it? I can't find the answer here https://www.google.com/search?query=blablabla", null).getContainsQuestions());
        // input with bad ? in http link and legitimate ? afterwards
        assertTrue(qas.analyze("I can't find the answer here http://www.google.com/search?query=blablabla, what day is it? ", null).getContainsQuestions());
        // input with bad ? in https link and legitimate ? afterwards
        assertTrue(qas.analyze("I can't find the answer here https://www.google.com/search?query=blablabla, what day is it? ", null).getContainsQuestions());
        // input without any ? but still a question. TODO hard to detect, model needs improvement to properly detect more questions like this
        assertTrue(qas.analyze("Can I put the value 150 as the property of 'name', or does it not work like that", null).getContainsQuestions());
    }
}