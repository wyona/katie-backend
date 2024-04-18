package com.wyona.katie.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import javax.print.attribute.standard.MediaSize;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Also see https://python.langchain.com/en/latest/modules/indexes/text_splitters.html
 */
@Slf4j
@Component
public class SegmentationService {

    @Value("${ai21.key}")
    private String ai21Key;

    @Autowired
    private XMLService xmlService;

    private static final String QUESTION = "question";
    private static final String URL = "url";
    private static final String ANSWER = "answer";

    /**
     * Also see https://python.langchain.com/en/latest/modules/indexes/text_splitters/examples/character_text_splitter.html
     * @param text Large text to be divided into smaller chunks
     * @param separator Separator, e.g. ' ' or '\n'
     * @param chunkSize Measure chunk length by number of characters, e.g. 1000
     * @param chunkOverlap Measure chunk overlap by number of characters, e.g. 50
     */
    public List<String> getSegments(String text, char separator, int chunkSize, int chunkOverlap) throws Exception {
        if (chunkSize < 1) {
            log.error("Chunk size needs to be greater than 0!");
            chunkSize = 1;
        }
        if (chunkOverlap < 0) {
            log.error("Chunk overlap needs to be greater than or equal to 0!");
            chunkOverlap = 0;
        }

        log.info("Chunk text into pieces with intended length of " + chunkSize + " characters and with " + chunkOverlap + " characters overlap.");
        log.info("Provided separator '" + separator + "'");

        List<String> chunks = new ArrayList<String>();

        String remainingText = text;
        String previousChunkWithoutOverlap = null;
        while (chunkSize < remainingText.length()) {
            String chunk = remainingText.substring(0, chunkSize);
            remainingText = remainingText.substring(chunkSize);

            // INFO: Check whether a word was split at the beginning of the chunk
            if (previousChunkWithoutOverlap != null) {
                chunk = completeChunkBeginning(previousChunkWithoutOverlap, chunk, separator);
            }
            previousChunkWithoutOverlap = chunk;

            if (chunkOverlap < remainingText.length()) {
                String OVERLAP_SEPARATOR = "";
                //String OVERLAP_SEPARATOR = "OVERLAP_START"; // INFO: For debugging
                chunk = chunk + OVERLAP_SEPARATOR + remainingText.substring(0, chunkOverlap);

                // INFO: Check whether a word was split at the end of the chunk
                chunk = completeChunkEnding(chunk, remainingText, chunkOverlap, separator);
            } else {
                chunk = chunk + remainingText;
            }

            chunks.add(chunk.trim());
        }

        String lastChunk = remainingText;
        // INFO: Check whether a word was split at the beginning of the chunk
        if (previousChunkWithoutOverlap != null) {
            completeChunkBeginning(previousChunkWithoutOverlap, remainingText, separator);
        }
        chunks.add(lastChunk.trim());

        return chunks;
    }

    /**
     * If word was split at the beginning of the chunk, then complete beginning of chunk
     */
    private String completeChunkBeginning(String previousChunkWithoutOverlap, String chunk, char separator) {
        if (previousChunkWithoutOverlap.charAt(previousChunkWithoutOverlap.length() - 1) != ' ' && chunk.charAt(0) != ' ') { // TODO: Also check for linebreaks
            log.info("Word was split at beginning of chunk: " + chunk);
            String REST_OF_BEGINNING_WORD_SEPARATOR = "";
            //String REST_OF_BEGINNING_WORD_SEPARATOR = "REST_OF_BEGINNING_WORD"; // INFO: For debugging
            String previousChunkWithoutOverlapReversed = new StringBuilder(previousChunkWithoutOverlap).reverse().toString();
            int indexNextSeparator = previousChunkWithoutOverlapReversed.indexOf(separator);
            if (indexNextSeparator > 0) {
                String completion = new StringBuilder(previousChunkWithoutOverlapReversed.substring(0, indexNextSeparator)).reverse().toString();
                return completion + REST_OF_BEGINNING_WORD_SEPARATOR + chunk;
            } else {
                return previousChunkWithoutOverlap + REST_OF_BEGINNING_WORD_SEPARATOR + chunk;
            }
        } else {
            return chunk;
            //return previousChunkWithoutOverlap.charAt(previousChunkWithoutOverlap.length() - 1) + "_" + chunk.charAt(0) + "_NO_SPLIT" + chunk;
        }
    }

    /**
     * If word was split at the end of the chunk, then complete end of chunk
     */
    private String completeChunkEnding(String chunk, String remainingText, int chunkOverlap, char separator) {
        if (chunk.charAt(chunk.length() - 1) != ' ' && remainingText.charAt(chunkOverlap) != ' ') { // TODO: Also check for linebreaks
            log.info("Word was split at end of chunk with overlap: " + chunk);
            String REST_OF_ENDING_WORD_SEPARATOR = "";
            //String REST_OF_ENDING_WORD_SEPARATOR = "REST_OF_ENDING_WORD"; // INFO: For debugging
            String remainingTextWithoutOverlap = remainingText.substring(chunkOverlap);
            log.info("Remaining text without overlap: " + remainingTextWithoutOverlap);
            int indexNextSeparator = remainingTextWithoutOverlap.indexOf(separator);
            if (indexNextSeparator > 0) {
                return chunk + REST_OF_ENDING_WORD_SEPARATOR + remainingTextWithoutOverlap.substring(0, indexNextSeparator);
            } else {
                return chunk + REST_OF_ENDING_WORD_SEPARATOR + remainingTextWithoutOverlap;
            }
        } else {
            return chunk;
        }
    }

    /**
     * Chunk Akoma Ntoso XML
     * @param document Akoma Ntoso XML document
     * @param sourceUrl For example https://www.fedlex.admin.ch/eli/cc/2022/491/de#
     */
    public ArrayNode chunkAkomaNtoso(Document document, String sourceUrl) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode katieImportJSON = mapper.createArrayNode();

        NodeList chapterNL = document.getDocumentElement().getElementsByTagName("chapter");
        log.info(chapterNL.getLength() + " chapters found.");

        for (int ci = 0; ci < chapterNL.getLength(); ci++) {
            Element chapterEl = (Element) chapterNL.item(ci);
            NodeList articleNL = chapterEl.getElementsByTagName("article");
            String chapter_eId = chapterEl.getAttribute("eId");
            log.info(articleNL.getLength() + " articles found inside chapter '" + chapter_eId + "'.");

            String chapterTitle = getTitle(chapterEl);
            if (chapterTitle == null) {
                log.warn("Chapter '" + chapter_eId + "' has no heading!");
                chapterTitle = "";
            }

            // TODO: Also consider "section"

            for (int i = 0; i < articleNL.getLength(); i++) {
                Element articleEl = (Element) articleNL.item(i);
                String article_eId = articleEl.getAttribute("eId");
                log.info("Article Id: " + article_eId);

                String articleTitle = getTitle(articleEl);
                if (articleTitle == null) {
                    log.warn("Article '" + article_eId + "' has no heading, probably repealed!");
                    articleTitle = "";
                }

                ObjectNode qnaNode = mapper.createObjectNode();
                String questionText = chapterTitle + " " + articleTitle;
                qnaNode.put(QUESTION, questionText.trim());
                qnaNode.put(URL, sourceUrl + article_eId);

                StringBuilder answer = new StringBuilder("");

                NodeList contentNl = articleEl.getElementsByTagName("content");
                if (contentNl.getLength() > 0) {
                    for (int k = 0; k < contentNl.getLength(); k++) {
                        NodeList pNL = ((Element) contentNl.item(k)).getElementsByTagName("p");
                        for (int j = 0; j < pNL.getLength(); j++) {
                            String text = getText((Element) pNL.item(j)).trim();
                            if (text.length() > 0) {
                                answer.append(text);
                                answer.append("\n\n");
                            } else {
                                log.warn("Text node of Article '" + article_eId + "' is empty!");
                            }
                        }
                    }
                } else {
                    log.warn("Article '" + article_eId + "' has no paragraphs with content!");
                    answer.append("NO_PARAGRAPHS");
                }
                qnaNode.put(ANSWER, answer.toString());
                katieImportJSON.add(qnaNode);
            }
        }

        return katieImportJSON;
    }

    /**
     * @param element Chapter, section or article title
     */
    private String getTitle(Element element) {
        String title = null;
        try {
            Element numEl = xmlService.getDirectChildByTagName(element, "num");
            if (numEl != null) {
                title = numEl.getFirstChild().getTextContent();
            }

            Element headingEl = xmlService.getDirectChildByTagName(element, "heading");
            if (headingEl != null) {
                if (title == null) {
                    title = "";
                }
                title = title + " " + headingEl.getFirstChild().getTextContent();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return title;
    }

    /**
     *
     */
    private String getText(Element element) {
        StringBuilder text = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                text.append(child.getTextContent());
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                text.append(getText((Element)child));
            }
        }
        return text.toString();
    }

    /**
     * Get segments using AI21 service
     * @param text Text to be chunked
     */
    public List<String> getSegmentsUsingAI21(String text) throws Exception {
        log.info("Get segments using AI21 ...");

        List<String> chunks = new ArrayList<String>();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders(ai21Key);

        HttpEntity<String> request = new HttpEntity<String>(createRequestBody(text), headers);

        String requestUrl = "https://api.ai21.com/studio/v1/segmentation";
        log.info("Get segmentation: " + requestUrl);
        ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
        JsonNode bodyNode = response.getBody();
        log.info("Response JSON: " + bodyNode);

        ObjectMapper mapper = new ObjectMapper();

        JsonNode segmentsNode = bodyNode.get("segments");
        if (segmentsNode.isArray()) {
            for (int i = 0; i < segmentsNode.size(); i++) {
                JsonNode segmentNode = segmentsNode.get(i);
                chunks.add(segmentNode.get("segmentText").asText());
            }
        }

        return chunks;
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders(String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Authorization", "Bearer " + key);
        return headers;
    }

    /**
     *
     */
    private String createRequestBody(String text) throws Exception  {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();
        body.put("sourceType","TEXT");
        body.put("source", text.toString());

        return body.toString();
    }
}
