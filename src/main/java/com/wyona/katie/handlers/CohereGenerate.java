package com.wyona.katie.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 */
@Slf4j
@Component
public class CohereGenerate {

    @Value("${cohere.host}")
    private String cohereHost;

    @Value("${cohere.key}")
    private String cohereKey;

    /**
     * Get fake answer for question
     * @param question Question, e.g. "What is the highest mountain in Switzerland?" or "How old are you?"
     * @param cohereModel Cohere model, e.g. "small", "medium", "large"
     * @return fake answer, e.g. "The highest mountain in Switzerland is the Matterhorn" or "I was born in 2001"
     */
    public String getFakeAnswer(String question, String cohereModel) {
        log.info("Get fake answer from Cohere for question '" + question + "' ...");

        try {

            /* TODO: Concrete numbers have been replaced by placeholders
            Question: What is the certificate requirement?
Answer: On [date], the Federal Council ordered a certificate requirement for various areas from [date].
--
Question: Where is the certificate requirement in effect?
Answer: It applies for restaurants and bars, cultural and leisure facilities, concert halls.
--
Question: How many people live in Zürich?
Answer: Around [number] people live in Zürich, including district residents.
--
Question: How many states are their in the US?
Answer: The United States is made up of [number] states.
--
Question: What is the minimum number of days you need to quarantine?
Answer:
             */

            String stopSequences = "--";

            StringBuilder prompt = new StringBuilder();
            prompt.append("Question: What is the certificate requirement?");
            prompt.append("\n");
            prompt.append("Answer: On September 8, 2021, the Federal Council ordered a certificate requirement for various areas from September 13, 2021.");
            prompt.append("\n" + stopSequences + "\n");
            prompt.append("Question: Where is the certificate requirement in effect?");
            prompt.append("\n");
            prompt.append("Answer: It applies for restaurants and bars, cultural and leisure facilities, concert halls.");
            prompt.append("\n" + stopSequences + "\n");
            prompt.append("Question: " + question);
            prompt.append("\n");
            prompt.append("Answer:");

            // INFO: See https://docs.cohere.ai/reference/generate

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode body = mapper.createObjectNode();
            body.put("model", cohereModel);
            body.put("prompt", prompt.toString());
            body.put("max_tokens",100);
            body.put("temperature",0.8);
            body.put("k",0);
            body.put("p",1);
            body.put("frequency_penalty",0);
            body.put("presence_penalty", 0);
            ArrayNode stopSequencesNode = mapper.createArrayNode();
            stopSequencesNode.add(stopSequences);
            body.put("stop_sequences", stopSequencesNode);
            body.put("return_likelihoods","NONE");

            log.info("Body as JSON: " + body.toString());

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders();
            HttpEntity<String> request = new HttpEntity<String>(body.toString(), headers);

            String requestUrl = cohereHost + "/generate";
            log.info("Get generated text: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);

            JsonNode textNode = bodyNode.get("text");
            return textNode.asText();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Authorization", "Bearer " + cohereKey);
        return headers;
    }
}
