package com.wyona.katie.connectors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.wyona.katie.models.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Connector for ingesting data from Filesystem
 */
@Slf4j
@Component
public class FilesystemConnector implements Connector {

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("TODO: Implement: " + ksMeta.getName() + "' ...");
        List<Hit> hits = new ArrayList<Hit>();

        return hits.toArray(new Hit[0]);
    }

    /**
     * @see Connector#update(Context, KnowledgeSourceMeta, WebhookPayload, String)
     */
    public List<Answer> update(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayload payload, String processId) {
        File baseDir = new File(ksMeta.getFilesystemBasePath());
        if (baseDir.isDirectory()) {
            log.info("Ingest data from filesystem: " + baseDir.getAbsolutePath());
        } else {
            log.warn("No such directory: " + baseDir.getAbsolutePath());
        }
        return null;
    }
}
