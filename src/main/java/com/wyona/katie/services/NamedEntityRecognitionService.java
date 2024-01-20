package com.wyona.katie.services;

import com.wyona.katie.handlers.*;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.NerImpl;
import com.wyona.katie.models.Sentence;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class NamedEntityRecognitionService {

    @Autowired
    private DoNotAnalyzeNERImpl doNotAnalyzeNER;

    @Autowired
    private MockNERImpl mockImpl;

    @Autowired
    private TextRazorNERImpl textRazorImpl;

    @Autowired
    private GoogleNERImpl googleImpl;

    @Autowired
    private FlairNERImpl flairImpl;

    @Value("${ner.implementation}")
    private NerImpl defaultNerImplementation;

    /*
    @Autowired
    public NamedEntityRecognitionService() {
    }

     */

    /**
     * Recognize named entities within text
     * @param text Text/sentence to be analyzed
     * @param classifications Classifications, e.g. "num", "hum"
     * @param domain Domain containing optional NER configuration
     * @return sentence with all recognized named entities
     */
    public Sentence analyze(String text, List<String> classifications, Context domain) {

        NerImpl nerImpl = getNerImplementation(domain);

        log.info("Analyze sentence '" + text + "' using NER implementation '" + nerImpl + "' ...");

        if (nerImpl.equals(NerImpl.TEXTRAZOR)) {
            return textRazorImpl.analyze(text, classifications);
        } else if (nerImpl.equals(NerImpl.GOOGLE)) {
            return googleImpl.analyze(text, classifications);
        } else if (nerImpl.equals(NerImpl.FLAIR)) {
            return flairImpl.analyze(text, classifications);
        } else if (nerImpl.equals(NerImpl.MOCK)) {
            return mockImpl.analyze(text, classifications);
        } else if (nerImpl.equals(NerImpl.DO_NOT_ANALYZE)) {
            return doNotAnalyzeNER.analyze(text, classifications);
        } else {
            log.error("No such NER implementation: " + nerImpl);
            return mockImpl.analyze(text, classifications);
        }
    }

    /**
     * Get NER implementation
     * @param domain Domain which might have a different NER implementation configured
     */
    protected NerImpl getNerImplementation(Context domain) {
        if (domain == null) {
            log.warn("No domain provided!");
            return defaultNerImplementation;
        } else {
            if (domain.getNerImpl() != null) {
                if (!domain.getNerImpl().equals(defaultNerImplementation)) {
                    log.warn("Domain uses different NER implementation '" + domain.getNerImpl() + "' than default NER implementation '" + defaultNerImplementation + "'.");
                }
                return domain.getNerImpl();
            } else {
                return defaultNerImplementation;
            }
        }
    }
}
