package com.wyona.katie.services;

import com.wyona.katie.models.Context;

/**
 * Service to manage domain specific taxonomy
 * https://en.wikipedia.org/wiki/Taxonomy
 * https://en.wikipedia.org/wiki/Taxonomy_for_search_engines
 */
public interface TaxonomyService {

    /**
     * Autocomplete taxonomy entry
     * @param domain Domain object
     * @param incompleteEntry Incomplete taxonomy entry, e.g. "birth"
     * @return array of suggested questions, e.g. "birthdate" or "birthplace"
     */
    public String[] getSuggestions(Context domain, String incompleteEntry) throws Exception;

    /**
     * Get entries of taxonomy index of a particular domain
     * @param domain Domain object
     * @param offset Offset indicates the start of the returned entries
     * @param limit Limit the number of returned entries
     * @return array of entries of taxonomy index
     */
    public String[] getEntries(Context domain, int offset, int limit) throws Exception;

    /**
     * Add entries, e.g. "birthdate" and "renovation"
     */
    public void addEntries(Context domain, String[] values) throws Exception;

    /**
     * Delete entry, e.g. "birthdate"
     */
    public void deleteEntry(Context domain, String value) throws Exception;
}
