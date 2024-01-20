package com.wyona.katie.models;
  
import lombok.extern.slf4j.Slf4j;

import com.wyona.katie.models.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Slf4j
public class Sentence {

    private String sentence;
    private List<Entity> entities;
    private List<String> classifications;

    /**
     * @param classifications Classifications, e.g. "num", "hum"
     */
    public Sentence(String sentence, ArrayList<Entity> entities, List<String> classifications) {
        this.sentence = sentence;
        if (entities != null) {
            this.entities = entities;
        } else {
            this.entities = new ArrayList<Entity>();
        }
        if (classifications != null) {
            this.classifications = classifications;
        } else {
            this.classifications = new ArrayList<String>();
        }
    }

    /**
     *
     */
    public String getSentence() {
        return sentence;
    }

    /**
     *
     */
    public List<String> getClassifications() {
        return classifications;
    }

    /**
     * Get all entities
     */
    public Entity[] getAllEntities() {
        return entities.toArray(new Entity[0]);
    }

    /**
     * @param name Entity name, for example 'ak-entity:person_name' or '/people/person'
     */
    public Entity[] getEntities(String name) {
        ArrayList<Entity> es = new ArrayList<Entity>();
        if (entities != null) {
            for (Entity entity : entities) {
                log.info("Entity: " + entity);
                if (entity.getName().equals(name)) {
                    es.add(entity);
                }
            }
        } else {
            log.warn("No entities available for setence '" + sentence + "'.");
        }
        return es.toArray(new Entity[0]);
    }

    /**
     * Get sentence without entities, for example "How old is?" instead "How old is Michael Wechner?"
     */
    public String getSentenceWithoutEntities() {
        String without = sentence;
        if (entities != null) {
            for (Entity entity : entities) {
                log.info("Remove entity from sentence: " + entity);
                without = without.replace(entity.getValue(), "");
            }
        } else {
            log.warn("No entities available for setence '" + sentence + "'.");
        }
        return without;
    }

    /**
     *
     */
    @Override
    public String toString() {
        return sentence;
    }
}
