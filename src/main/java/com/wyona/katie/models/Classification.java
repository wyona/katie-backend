package com.wyona.katie.models;

public class Classification {

    private String term;
    private String id;

    /**
     *
     */
    public Classification() {
    }

    /**
     * @param term Text, e.g. "Managed Device Services, MacOS Clients"
     * @param id Id, e.g. "64e3bb24-1522-4c49-8f82-f99b34a82062"
     */
    public Classification(String term, String id) {
        this.term = term;
        this.id = id;
    }

    /**
     *
     */
    public void setTerm(String term) {
        this.term = term;
    }

    /**
     *
     */
    public String getTerm() {
        return term;
    }

    /**
     *
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     */
    public String getId() {
        return id;
    }
}
