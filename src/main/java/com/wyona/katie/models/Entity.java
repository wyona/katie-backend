package com.wyona.katie.models;
  
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class Entity {

    public static final String AK_ENTITY_PREFIX = "ak-entity:";
    public static final String CUSTOM_ENTITY_PREFIX = "custom-entity:";

    // INFO: See https://developers.google.com/freebase/guide/basic_concepts
    public static final String FREEBASE_PERSON = "/people/person"; // INFO: See https://github.com/freebase-schema/freebase/wiki/people-person

    public static final String AK_PERSON = "ak-entity:person_name"; // WARN: Used also by expressions inside answers, see for example src/main/resources/contexts/ROOT/questions-answers
    public static final String AK_LOCATION = "ak-entity:location";
    public static final String AK_ORGANIZATION = "ak-entity:organization";
    public static final String AK_NUMBER = "ak-entity:number";
    public static final String AK_STREETNAME = "ak-entity:streetname";

    private String name;
    private String value;
    private Entity[] entities; // INFO: For example a 'price' entity can have an 'amount' and a 'currecy' entity as sub-entities

    /**
     * @param name Entity name, e.g. "ak-entity:person_name"
     * @param value Entity value, e.g. "Michael Wechner"
     */
    public Entity(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     *
     */
    public Entity(String name, String value, Entity[] entities) {
        this.name = name;
        this.value = value;
        this.entities = entities;
    }

    /**
     * @return Entity name, e.g. "ak-entity:person_name"
     */
    public String getName() {
        return name;
    }

    /**
     *
     */
    public String getValue() {
        return value;
    }

    /**
     *
     */
    @Override
    public String toString() {
        return "'" + name + "':'" + value + "'";
    }
}
