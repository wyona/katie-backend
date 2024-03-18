package com.wyona.katie.services;

import com.wyona.katie.models.*;

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
     * Get domain specific dataset including labels and samples
     * @param domain Domain object
     * @param offset Offset indicates the start of the returned entries
     * @param limit Limit the number of returned entries
     * @return dataset including labels and samples
     */
    public ClassificationDataset getDataset(Context domain, int offset, int limit) throws Exception;

    /**
     * Train classifier with samples (texts and labels)
     */
    public void train(Context domain, TextSample[] samples) throws Exception;
}
