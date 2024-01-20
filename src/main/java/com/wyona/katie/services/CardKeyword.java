package com.wyona.katie.services;

import java.util.HashSet;
import java.util.Set;

/**
 * Keyword card with stem form, terms dictionary and frequency rank
 */
public class CardKeyword implements Comparable<CardKeyword> {

    /**
     * Stem form of the keyword
     */
    private final String stem;

    /**
     * Terms dictionary
     */
    private final Set<String> terms = new HashSet<>();

    /**
     * Frequency rank
     */
    private int frequency;

    /**
     * Build keyword card with stem form
     *
     * @param stem
     */
    public CardKeyword(String stem) {
        this.stem = stem;
    }

    /**
     * Add term to the dictionary and update its frequency rank
     *
     * @param term
     */
    public void add(String term) {
        this.terms.add(term);
        this.frequency++;
    }

    /**
     * Compare two keywords by frequency rank
     *
     * @param keyword
     * @return int, which contains comparison results
     */
    @Override
    public int compareTo(CardKeyword keyword) {
        return Integer.valueOf(keyword.frequency).compareTo(this.frequency);
    }

    /**
     * Get stem's hashcode
     *
     * @return int, which contains stem's hashcode
     */
    @Override
    public int hashCode() {
        return this.getStem().hashCode();
    }

    /**
     * Check if two stems are equal
     *
     * @param o
     * @return boolean, true if two stems are equal
     */
    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (!(o instanceof CardKeyword)) return false;

        CardKeyword that = (CardKeyword) o;

        return this.getStem().equals(that.getStem());
    }

    /**
     * Get stem form of keyword
     *
     * @return String, which contains getStemForm form
     */
    public String getStem() {
        return this.stem;
    }

    /**
     * Get terms dictionary of the stem
     *
     * @return Set<String>, which contains set of terms of the getStemForm
     */
    public Set<String> getTerms() {
        return this.terms;
    }

    /**
     * Get stem frequency rank
     *
     * @return int, which contains getStemForm frequency
     */
    public int getFrequency() {
        return this.frequency;
    }
}
