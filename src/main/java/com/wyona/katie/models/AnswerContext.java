package com.wyona.katie.models;

import java.net.URI;

/**
 * Context on which answer is based on
 */
public class AnswerContext {

    private String context;
    private URI uri;

    /**
     *
     */
    public AnswerContext(String context, URI uri) {
        this.context = context;
        this.uri = uri;
    }

    /**
     *
     */
    public String getContext() {
        return context;
    }

    /**
     *
     */
    public URI getUri() {
        return uri;
    }
}
