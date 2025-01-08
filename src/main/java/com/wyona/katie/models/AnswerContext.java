package com.wyona.katie.models;

import java.net.URI;

/**
 * Context on which answer is based on
 */
public class AnswerContext {

    private String context;
    private URI uri;

    /**
     * @param context Relevant context, e.g. a text article/section relevant to the question and answer
     * @param uri Source URI of relevant context
     */
    public AnswerContext(String context, URI uri) {
        this.context = context;
        this.uri = uri;
    }

    /**
     * @return relevant context, e.g. a text article/section relevant to the question and answer
     */
    public String getContext() {
        return context;
    }

    /**
     * @return source URI of relevant context
     */
    public URI getUri() {
        return uri;
    }
}
