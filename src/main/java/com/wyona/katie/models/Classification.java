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
     * @param term Class name, e.g. "Managed Device Services, MacOS Clients"
     * @param id Class Id, e.g. "64e3bb24-1522-4c49-8f82-f99b34a82062"
     */
    public Classification(String term, String id) {
        this.term = term;
        this.id = id;
    }

    /**
     * @param term Class name
     */
    public void setTerm(String term) {
        this.term = term;
    }

    /**
     * @return class name
     */
    public String getTerm() {
        return term;
    }

    /**
     * @param id Class Id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return class Id
     */
    public String getId() {
        return id;
    }
}
