package com.wyona.katie.handlers;

import com.wyona.katie.models.Sentence;
import com.wyona.katie.models.Entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.textrazor.TextRazor;
//import com.textrazor.annotations.Entity;
import com.textrazor.annotations.AnalyzedText;

/**
 *
 */
@Slf4j
@Component
public class TextRazorNERImpl implements NERHandler {

    @Value("${ner.textrazor.api-key}")
    private String API_KEY;

    /**
     * @see NERHandler#analyze(String, List)
     */
    public Sentence analyze(String text, List<String> classifications) {
        log.info("Analyze sentence '" + text + "'");

        TextRazor client = new TextRazor(API_KEY);

        client.addExtractor("words");
        client.addExtractor("entities");

        try {
            AnalyzedText response = client.analyze(text);

            ArrayList<Entity> entities = new ArrayList<Entity>();
            if (response.getResponse().getEntities() != null) {
                for (com.textrazor.annotations.Entity entity : response.getResponse().getEntities()) {
                    log.info("Matched Entity: " + entity.getEntityId());
                    java.util.List<String> types = entity.getFreebaseTypes();
                    if (types != null) {
                        for (String type: types) {
                            log.info("Freebase type: " + type);
                            if (type.equals(Entity.FREEBASE_PERSON)) {
                                entities.add(new Entity(Entity.AK_PERSON, entity.getEntityId()));
                            }
                        }
                    } else {
                        log.warn("Entity '" + entity.getEntityId() + "' recognized inside text '" + text + "', but no freebase type!");
                    }
                }
            } else {
                log.info("No entities recognized inside text '" + text + "'.");
            }

            return new Sentence(text, entities, classifications);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new Sentence(text, new ArrayList<Entity>(), classifications);
        }
    }
}
