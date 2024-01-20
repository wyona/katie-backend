package com.wyona.katie.handlers;

import com.wyona.katie.models.Sentence;
import com.wyona.katie.models.Entity;

import java.util.ArrayList;
import java.util.List;

import com.wyona.katie.services.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;


/**
 * https://github.com/flairNLP/flair https://github.com/wyona/flair-flask-rest
 */
@Slf4j
@Component
public class FlairNERImpl implements NERHandler {

    @Value("${ner.flair.url}")
    private String flairURL;

    /**
     * @see NERHandler#analyze(String, List)
     */
    public Sentence analyze(String text, List<String> classifications) {
        log.info("Analyze sentence '" + text + "'");

        return new Sentence(text, getEntities(text), classifications);
    }

    /**
     * Get text entities
     * @param text Text containing entities, e.g. "Does Michael work in Zurich?"
     * @return entities, e.g. {"score":0.999977707862854,"tag":"LOC","text":"Zurich"} or {"score":0.9990990161895752,"tag":"PER","text":"Michael"}
     */
    public ArrayList<Entity> getEntities(String text) {
        log.info("Get entities of text '" + text + "' from Flair NER API '" + flairURL + "' ...");

        ArrayList<Entity> entities = new ArrayList<Entity>();
        try {
            // curl --request POST --url https://flair-ner.wyona.org/api/v1/ner --header 'content-type: application/json' --data '{ "message":"Does Michael work in Zurich?" }'
            // TODO: Use Jackson
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"message\":\"" + Utils.escape(text) + "\"");
            sb.append("}");

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders();
            headers.set("Content-Type", "application/json; charset=UTF-8");
            HttpEntity<String> request = new HttpEntity<String>(sb.toString(), headers);

            String requestUrl = flairURL;
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);

            JsonNode entitiesNode = bodyNode.get("entities");
            if (entitiesNode.isArray()) {
                for (int i = 0; i < entitiesNode.size(); i++) {
                    JsonNode entityNode = entitiesNode.get(i);
                    String tag = entityNode.get("tag").asText();
                    String score = entityNode.get("score").asText();
                    String entityType = getEntityType(tag);
                    if (entityType != null) {
                        entities.add(new Entity(entityType, entityNode.get("text").asText()));
                    }
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return entities;
    }

    /**
     * @param tag Flair tag, e.g. "PER" or "LOC"
     */
    private String getEntityType(String tag) {
        // TODO: Does Flair also support for example Entity.AK_NUMBER ...?
        if (tag.equals("PER")) {
            return Entity.AK_PERSON;
        } else if (tag.equals("LOC")) {
            return Entity.AK_LOCATION;
        } else if (tag.equals("ORG")) {
            return Entity.AK_ORGANIZATION;
        } else {
            log.info("No such entity tag '" + tag + "' implemented!");
            return null;
        }
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        /*
        if (sbertBasicAuthUsername != null && sbertBasicAuthUsername.length() > 0) {
            String auth = sbertBasicAuthUsername + ":" + sbertBasicAuthPassword;
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);
        }
         */
        return headers;
    }
}
