package com.wyona.katie.handlers.mcc;

import com.wyona.katie.models.*;

/**
 *
 */
public interface MulticlassTextClassifier {

    /**
     * Predict labels for a text and a particular domain
     * @param domain Domain object
     * @param text Text to be classified / labeled
     * @param limit Limit of returned labels
     * @return array of suggested labels
     */
    public HitLabel[] predictLabels(Context domain, String text, int limit) throws Exception;

    /**
     * Train classifier with samples (texts and labels)
     */
    public void train(Context domain, TextSample[] samples) throws Exception;

    /**
     * Retrain classifier on previously imported dataset
     * @param bgProcessId Background process Id
     */
    public void retrain(Context domain, String bgProcessId) throws Exception;
}
