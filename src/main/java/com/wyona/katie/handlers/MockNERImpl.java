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
public class MockNERImpl implements NERHandler {

    /**
     * @see NERHandler#analyze(String, List)
     */
    public Sentence analyze(String text, List<String> classifications) {
        log.info("Analyze sentence '" + text + "' ... checking for hard-coded entities ...");

        ArrayList<Entity> entities = new ArrayList<Entity>();

        String sampleLoc = "Zürich";
        if (text.indexOf(sampleLoc) >= 0) {
            entities.add(new Entity(Entity.AK_LOCATION, sampleLoc.trim()));
        }
        String sampleOrg = "Apache Software Foundation";
        if (text.indexOf(sampleOrg) >= 0) {
            entities.add(new Entity(Entity.AK_ORGANIZATION, sampleOrg.trim()));
        }
        Integer number = extractFirstInteger(text);
        if (number != null) {
            entities.add(new Entity(Entity.AK_NUMBER, number.toString()));
        }
        String sampleStreetname = "Fritz-Fleiner-Weg";
        if (text.indexOf(sampleStreetname) >= 0) {
            entities.add(new Entity(Entity.AK_STREETNAME, sampleStreetname.trim()));
        }

        // TODO: "Wechner Michael", "Michi Wechner", "Michael Hannes Wechner"
        if (text.indexOf("Michael Wechner") >= 0) {
            entities.add(new Entity(Entity.AK_PERSON, "Michael Wechner"));
        }
        if (text.indexOf("Wechner") >= 0 && text.indexOf("Michael") < 0) {
            entities.add(new Entity(Entity.AK_PERSON, "Wechner"));
        }

        if (text.indexOf("Michael Jackson") >= 0) {
            entities.add(new Entity(Entity.AK_PERSON, "Michael Jackson"));
        }
        if (text.indexOf("Jackson") >= 0 && text.indexOf("Michael") < 0) {
            entities.add(new Entity(Entity.AK_PERSON, "Jackson"));
        }

        if (text.indexOf("Michael") >= 0 && text.indexOf("Jackson") < 0 && text.indexOf("Wechner") < 0) {
            entities.add(new Entity(Entity.AK_PERSON, "Michael"));
        }

        // TODO: What about other cases, like for example "Michael Douglas"?!

        if (text.indexOf("Katerina Oliveros") >= 0) {
            entities.add(new Entity(Entity.AK_PERSON, "Katerina Oliveros"));
        }
        if (text.indexOf("Katerina") >= 0 && text.indexOf("Oliveros") < 0) {
            entities.add(new Entity(Entity.AK_PERSON, "Katerina"));
        }
        if (text.indexOf("Oliveros") >= 0 && text.indexOf("Katerina") < 0) {
            entities.add(new Entity(Entity.AK_PERSON, "Oliveros"));
        }
        if (text.indexOf("Levi Brucker") >= 0) {
            entities.add(new Entity(Entity.AK_PERSON, "Levi Brucker"));
        }

        return new Sentence(text, entities, classifications);
    }

    /**
     * Extract number from text, e.g. "Wann ist die nächste Papiersammlung für Postleitzahl 8044?"
     * See for example https://stackoverflow.com/questions/18590901/check-and-extract-a-number-from-a-string-in-java
     * @return number, e.g. "8044"
     */
    private Integer extractFirstInteger(String text) {
        StringBuilder digits = new StringBuilder();
        boolean found = false;
        for(char c : text.toCharArray()) {
            if(Character.isDigit(c)){
                digits.append(c);
                found = true;
            } else {
                if (found) {
                    break;
                } else {
                    // INFO: Continue searching ...
                }
            }
        }
        if (digits.length() > 0) {
            return Integer.parseInt(digits.toString());
        } else {
            log.info("No number found.");
            return null;
        }
    }
}
