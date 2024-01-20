package com.wyona.katie.handlers.qc;

import com.wyona.katie.models.AnalyzedMessage;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.AnalyzedMessage;
import com.wyona.katie.models.Context;

/**
 *
 */
public interface QuestionClassifier {

    /**
     * Analyze message, e.g. check whether message contains question(s)
     * @param message Message, e.g. "Hello, has anyone deployed weaviate in Azure? If so, which infrastructure deployment did you use? Thanks! Michael"
     * @return analyzed message
     */
    public AnalyzedMessage analyze(String message, Context domain);
}
