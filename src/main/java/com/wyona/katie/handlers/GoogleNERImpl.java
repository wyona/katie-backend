package com.wyona.katie.handlers;

import com.wyona.katie.models.Sentence;
import com.wyona.katie.models.Entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;
import com.google.cloud.language.v1.EncodingType;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.core.io.ClassPathResource;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.language.v1.LanguageServiceSettings;

/**
 *
 */
@Slf4j
@Component
public class GoogleNERImpl implements NERHandler {

    @Value("${google.application.credentials}")
    private String googleApplicationCredentials;

    /**
     * @see NERHandler#analyze(String, List)
     */
    public Sentence analyze(String text, List<String> classifications) {
        log.info("Analyze sentence '" + text + "'");

        try {
            log.info("Load Google Application Credentials '" + googleApplicationCredentials + "' ...");

      GoogleCredentials gc = GoogleCredentials.fromStream(new ClassPathResource(googleApplicationCredentials).getInputStream());
      CredentialsProvider cp = FixedCredentialsProvider.create(gc);
      LanguageServiceSettings settings = (LanguageServiceSettings) LanguageServiceSettings.newBuilder().setCredentialsProvider(cp).build();

      LanguageServiceClient language = LanguageServiceClient.create(settings);
      //LanguageServiceClient language = LanguageServiceClient.create();

      // The text to analyze
      log.info("Analyze text: " + text);
      Document doc = Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).build();

      // Detects the sentiment of the text
      Sentiment sentiment = language.analyzeSentiment(doc).getDocumentSentiment();
      log.info("Sentiment: " + sentiment.getScore() +  ", " + sentiment.getMagnitude());

      java.util.List<com.google.cloud.language.v1.Entity> entitiesFound = language.analyzeEntities(doc, EncodingType.UTF8).getEntitiesList();
      ArrayList<Entity> entities = new ArrayList<Entity>();
      for (com.google.cloud.language.v1.Entity entity : entitiesFound) {
          log.info("Entity: " + entity.getName());
          // TODO: Check whether found entity is a person
          entities.add(new Entity(Entity.AK_PERSON, entity.getName()));
      }

            return new Sentence(text, entities, classifications);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }


        return new Sentence(text, null, classifications);
    }
}
