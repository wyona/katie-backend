package com.wyona.katie.models;

public class Classification {

    private String term;
    private Integer id;

    /**
     *
     */
    public Classification() {
    }

    /**
     * @param term Text, e.g. "Managed Device Services, MacOS Clients"
     * @param id Id, e.g. 15
     */
    public Classification(String term, Integer id) {
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
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     *
     */
    public Integer getId() {
        return id;
    }
}
