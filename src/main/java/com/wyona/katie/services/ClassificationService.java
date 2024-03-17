package com.wyona.katie.services;

import com.wyona.katie.models.Classification;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.HitLabel;
import com.wyona.katie.models.TextSample;

/**
 * Service to manage domain specific classifications
 */
public interface ClassificationService {

    /**
     * Predict labels for a text and a particular domain
     * @param domain Domain object
     * @param text Text
     * @return array of suggested labels
     */
    public HitLabel[] predictLabels(Context domain, String text) throws Exception;

    /**
     * Get domain specific labels
     * @param domain Domain object
     * @param offset Offset indicates the start of the returned entries
     * @param limit Limit the number of returned entries
     * @return array of labels
     */
    public Classification[] getLabels(Context domain, int offset, int limit) throws Exception;

    /**
     * Train classifier with samples (texts and labels)
     */
    public void train(Context domain, TextSample[] samples) throws Exception;
}
