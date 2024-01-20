package com.wyona.katie.handlers;

import com.wyona.katie.models.Sentence;

import java.util.List;

/**
 *
 */
public interface NERHandler {

    /**
     * @param classifications Classifications, e.g. "num", "hum"
     */
    public Sentence analyze(String text, List<String> classifications);
}
