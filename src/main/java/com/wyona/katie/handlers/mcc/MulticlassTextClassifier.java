package com.wyona.katie.handlers.mcc;

import com.wyona.katie.models.*;

/**
 *
 */
public interface MulticlassTextClassifier {

    /**
     * Predict labels for a text and a particular domain
     * @param domain Domain object
     * @param text Text
     * @return array of suggested labels
     */
    public HitLabel[] predictLabels(Context domain, String text) throws Exception;

    /**
     * Train classifier with samples (texts and labels)
     */
    public void train(Context domain, TextSample[] samples) throws Exception;
}
