package com.wyona.katie.handlers;

import com.wyona.katie.models.EmbeddingType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.EmbeddingValueType;
import com.wyona.katie.models.FloatVector;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 */
@Slf4j
@Component
public class CohereEmbeddings implements EmbeddingsProvider {

    @Value("${cohere.host}")
    private String cohereHost;

    @Value("${cohere.version}")
    private String cohereVersion;

    /**
     * @see EmbeddingsProvider#getEmbedding(String, String, EmbeddingType, EmbeddingValueType, String)
     */
    public FloatVector getEmbedding(String sentence, String cohereModel, EmbeddingType inputType, EmbeddingValueType valueType, String cohereKey) {
        // INFO: https://txt.cohere.com/introducing-embed-v3/
        String inputTypeStr = "search_document";
        if (inputType.equals(EmbeddingType.SEARCH_QUERY)) {
            inputTypeStr = "search_query";
        }
        //String inputType = "classification";
        //String inputType = "clustering";

        //String[] embeddingTypes = null;
        String[] embeddingTypes = new String[1];
        if (valueType == EmbeddingValueType.int8) {
            embeddingTypes[0] = "int8";
        } else {
            embeddingTypes[0] = "float";
        }

        log.info("Get embedding from Cohere (Model: " + cohereModel + ", Input type: " + inputTypeStr + ", Vector value type: " + embeddingTypes[0] + ") for sentence '" + sentence + "' ...");

        FloatVector floatVector = null;
        int[] intVector = null;
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders(cohereKey);
            HttpEntity<String> request = new HttpEntity<String>(createRequestBody(sentence, cohereModel, inputTypeStr, embeddingTypes), headers);

            String requestUrl = cohereHost + "/embed";
            log.info("Get embeddings: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("Response JSON: " + bodyNode);

            JsonNode embeddingsNode = bodyNode.get("embeddings");

            if (embeddingTypes != null) {
                for (String vectorValueType : embeddingTypes) {
                    log.info("Vector value type: " + vectorValueType);
                    JsonNode embeddingTypeNode = embeddingsNode.get(vectorValueType);
                    if (embeddingTypeNode.isArray()) {
                        JsonNode embeddingNode = embeddingTypeNode.get(0);
                        if (embeddingNode.isArray()) {
                            if (vectorValueType.equals("float")) {
                                floatVector = new FloatVector(embeddingNode.size());
                                log.info("Vector size: " + floatVector.getDimension());

                                for (int i = 0; i < floatVector.getDimension(); i++) {
                                    floatVector.set(i, Float.parseFloat(embeddingNode.get(i).asText()));
                                }
                            } else if (vectorValueType.equals("int8")) {
                                intVector = new int[embeddingNode.size()];
                                for (int i = 0;i < intVector.length; i++) {
                                    //intVector[i] = Integer.parseInt(embeddingNode.get(i).asText());
                                    intVector[i] = embeddingNode.get(i).asInt();
                                }
                            } else {
                                log.warn("No such vector value type '" + vectorValueType + "' supported!");
                            }
                        } else {
                            log.error("No embedding received for sentence '" + sentence + "'");
                        }
                    } else {
                        log.error("No embeddings received for vector value type '" + vectorValueType + "'");
                    }
                }
            } else {
                if (embeddingsNode.isArray()) {
                    JsonNode embeddingNode = embeddingsNode.get(0); // INFO: Embedding for sentence resp. first text node (see createRequestBody(...))
                    if (embeddingNode.isArray()) {
                        floatVector = new FloatVector(embeddingNode.size());
                        log.info("Vector size: " + floatVector.getDimension());

                        for (int i = 0; i < floatVector.getDimension(); i++) {
                            floatVector.set(i, Float.parseFloat(embeddingNode.get(i).asText()));
                        }
                    } else {
                        log.error("No embedding received for sentence '" + sentence + "'");
                    }
                } else {
                    log.error("No embeddings received!");
                }
            }
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 429) {
                log.warn("Cohere Rate limit reached!");
                // TODO: Throw exception, such that EmbeddingsService can throttle and retry
            }
            log.error(e.getMessage(), e);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        /*
        if (vector != null) {
            for (int i = 0; i < vector.length; i++) {
                log.info(i + ": " + vector[i]);
            }
        }
         */

        return floatVector;
    }

    /**
     * @param inputType Cohere input type, e.g. "search_document", "search_query", "classification", "clustering"
     * @param embeddingTypes Optional array of embedding types, e.g. "float", "int8"
     */
    private String createRequestBody(String sentence, String cohereModel, String inputType, String[] embeddingTypes) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();
        body.put("model", cohereModel);

        body.put("input_type", inputType);

        if (embeddingTypes != null) {
            ArrayNode embeddingsTypesArray = mapper.createArrayNode();
            for (String embeddingType : embeddingTypes) {
                embeddingsTypesArray.add(embeddingType);
            }
            body.put("embedding_types", embeddingsTypesArray);
        }

        // INFO: One could provide multiple texts with one request
        ArrayNode textsNode = mapper.createArrayNode();
        textsNode.add(sentence);
        body.put("texts", textsNode);

        return body.toString();
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders(String cohereKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Authorization", "Bearer " + cohereKey);

        // INFO: See https://docs.cohere.ai/reference/versioning
        headers.set("Cohere-Version", cohereVersion);
        return headers;
    }
}
