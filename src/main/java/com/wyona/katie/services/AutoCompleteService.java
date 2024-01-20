package com.wyona.katie.services;

import com.wyona.katie.models.Context;
import com.wyona.katie.models.Context;

/**
 * Service to autocomplete question
 */
public interface AutoCompleteService {

    /**
     * Autocomplete question
     * @param domain Domain object
     * @param incompleteQuestion Incomplete question, e.g. "highe"
     * @return array of suggested questions, e.g. "What is the highest mountain"
     */
    public String[] getSuggestions(Context domain, String incompleteQuestion) throws Exception;

    /**
     * Get entries of autocomplete index of a particular domain
     * @param domain Domain object
     * @param offset Offset indicates the start of the returned entries
     * @param limit Limit the number of returned entries
     * @return array of entries of autocomplete index
     */
    public String[] getEntries(Context domain, int offset, int limit) throws Exception;

    /**
     * Add entries, e.g. "mountain" and "atlantic ocean"
     */
    public void addEntries(Context domain, String[] values) throws Exception;

    /**
     * Delete entry, e.g. "mountain"
     */
    public void deleteEntry(Context domain, String value) throws Exception;
}
