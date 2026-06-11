package com.wyona.katie.connectors;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.wyona.katie.models.*;
import com.wyona.katie.services.BackgroundProcessService;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.DataIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Connector for ingesting data from Filesystem
 */
@Slf4j
@Component
public class FilesystemConnector implements Connector {

    @Autowired
    ContextService domainService;

    @Autowired
    private BackgroundProcessService backgroundProcessService;

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
        List<Answer> qnas = new ArrayList<Answer>();

        File baseDir = new File(ksMeta.getFilesystemBasePath());
        if (baseDir.isDirectory()) {
            log.info("Ingest data from filesystem: " + baseDir.getAbsolutePath());
            backgroundProcessService.updateProcessStatus(processId, "Ingest data from filesystem: " + baseDir.getAbsolutePath());
            String[] files = baseDir.list();
            for (String filename : files) {
                try {
                    String url = "http://host.katie.internal/" + domain.getId() + "/" + filename;
                    InputStream in = new FileInputStream(new File(baseDir, filename));
                    // TODO: Make text splitter configurable
                    domainService.importPDF(url, in, TextSplitterImpl.FIXED_SIZE, domain, processId);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
                }
            }
        } else {
            log.warn("No such directory: " + baseDir.getAbsolutePath());
        }

        return qnas;
    }
}
