package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import com.wyona.katie.services.MailerService;
import com.wyona.katie.services.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpHost;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import technology.semi.weaviate.client.Config;
import technology.semi.weaviate.client.WeaviateClient;
import technology.semi.weaviate.client.base.Result;
import technology.semi.weaviate.client.v1.misc.model.Meta;

import org.apache.commons.codec.binary.Base64;

/**
 * Retrieval implementation based on Milvus
 */
@Slf4j
@Component
public class MilvusRetrievalImpl implements QuestionAnswerHandler {

    @Value("${milvus.host}")
    private String milvusHostDefault;

    /**
     * Check whether Weaviate is alive
     * @param endpoint API endpoint "/v1" or "/v1/.well-known/live" to do health check
     * @return true when alive and false otherwise
     */
    public boolean isAlive(String endpoint) {
        log.error("TODO: Implement method isAlive()!");
        return true;
    }

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) {
        log.error("TODO: Implement method deleteTenant()!");
        log.info("Milvus implementation of deleting tenant ...");
    }

    /**
     * @see QuestionAnswerHandler#createTenant(Context)
     */
    public String createTenant(Context domain) {;
        log.error("TODO: Implement method createTenant()!");
        domain.setMilvusBaseUrl(getHost(null));
        return domain.getMilvuseBaseUrl();
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.error("TODO: Implement method train()!");
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public QnA[] train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Finish implementation train(QnA[], Context, boolean)!");
        for (QnA qna: qnas) {
            train(qna, domain, indexAlternativeQuestions);
        }

        // TODO: Only return QnAs which got trained successfully
        return qnas;
    }

    /**
     * @see QuestionAnswerHandler#retrain(QnA, Context, boolean)
     */
    public void retrain(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Delete/train is just a workaround, implement retrain by itself");
        // TODO: https://www.semi.technology/developers/weaviate/current/restful-api-references/objects.html#update-a-data-object
        if (delete(qna.getUuid(), domain)) {
            train(qna, domain, indexAlternativeQuestions);
        } else {
            log.warn("QnA with UUID '" + qna.getUuid() + "' was not deleted and therefore was not retrained!");
        }
    }

    /**
     * @see QuestionAnswerHandler#delete(String, Context)
     */
    public boolean delete(String uuid, Context domain) {
        log.error("TODO: Implement method delete()!");
        return false;
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(Sentence, Context, int)
     */
    public Hit[] getAnswers(Sentence question, Context domain, int limit) {
        log.info("TODO: Consider using entities!");
        return getAnswers(question.getSentence(), question.getClassifications(), domain, limit);
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(String, List, Context, int)
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context domain, int limit) {
        log.error("TODO: Implement method getAnswers()!");

        List<Hit> answers = new ArrayList<Hit>();

        return answers.toArray(new Hit[0]);
    }

    /**
     * @param domain Optional domain
     * @return host, e.g. "http://0.0.0.0:8080"
     */
    public String getHost(Context domain) {
        return milvusHostDefault;
    }
}
