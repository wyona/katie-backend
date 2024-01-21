package com.wyona.katie.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Azure AI Search document
 */
public class AzureAISearchDoc {

    private String id;
    private String text;

    /**
     *
     */
    public AzureAISearchDoc() {
        this.id = null;
        this.text = null;
    }

    /**
     *
     */
    public String getId() {
        return id;
    }

    /**
     *
     */
    public AzureAISearchDoc setId(String id) {
        this.id = id;
        return this;
    }

    /**
     *
     */
    public String getText() {
        return text;
    }

    /**
     *
     */
    public AzureAISearchDoc setText(String text) {
        this.text = text;
        return this;
    }
}
