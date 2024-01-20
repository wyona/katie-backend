package com.wyona.katie.models;

/**
 * URL, e.g. https://cwiki.apache.org/confluence/display/LUCENE/LuceneFAQ
 */
public class ExtractQnAsArgs {

    private String url;
    private boolean clean;
    private QnAExtractorImpl qnAExtractorImpl;

    /**
     *
     */
    public ExtractQnAsArgs() {
    }

    /**
     *
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Get URL of webpage containing QnAs
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param clean True when previously imported QnAs associated with URL shall be deleted
     */
    public void setClean(boolean clean) {
        this.clean = clean;
    }

    /**
     * @return true when previously imported QnAs associated with URL shall be deleted
     */
    public boolean getClean() {
        return clean;
    }

    /**
     *
     */
    public void setQnAExtractorImpl(QnAExtractorImpl qnAExtractorImpl) {
        this.qnAExtractorImpl = qnAExtractorImpl;
    }

    /**
     *
     */
    public QnAExtractorImpl getQnAExtractorImpl() {
        return qnAExtractorImpl;
    }
}
