package com.wyona.katie.handlers.qc;

import com.wyona.katie.models.QuestionClassification;
import com.wyona.katie.models.QuestionClassification;

import java.util.Optional;

public interface ClassificationHandler {

    /**
     * @param input Input sentence, e.g. "Can I put the value 150 as the property of 'name', or does it not work like that"
     * @param domainId Katie domain Id, e.g. "f8703e64-2020-42d9-bfad-736e9eb894c0"
     * @return
     */
    public Optional<QuestionClassification> getClassification(String input, String domainId);
}
