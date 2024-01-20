package com.wyona.katie.handlers;

import com.wyona.katie.models.Sentence;
import com.wyona.katie.models.Entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
@Component
public class DoNotAnalyzeNERImpl implements NERHandler {

    /**
     * @see NERHandler#analyze(String, List)
     */
    public Sentence analyze(String text, List<String> classifications) {
        log.info("Do not analyze sentence '" + text + "'");

        ArrayList<Entity> entities = new ArrayList<Entity>();

        return new Sentence(text, entities, classifications);
    }
}
