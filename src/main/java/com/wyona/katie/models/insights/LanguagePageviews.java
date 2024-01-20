package com.wyona.katie.models.insights;

/**
 * Language Pageviews tuple
 */
public class LanguagePageviews {

    private String language;
    private int pageviews;

    /**
     *
     */
    public LanguagePageviews(String language, int pageviews) {
        this.language = language;
        this.pageviews = pageviews;
    }

    /**
     *
     */
    public String getLanguage() {
        return language;
    }

    /**
     *
     */
    public int getPageviews() {
        return pageviews;
    }
}
