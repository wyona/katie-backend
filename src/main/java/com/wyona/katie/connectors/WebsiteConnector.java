package com.wyona.katie.connectors;

import com.wyona.katie.models.*;
import com.wyona.katie.services.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 */
@Slf4j
@Component
public class WebsiteConnector implements Connector {

    @Autowired
    private SegmentationService segmentationService;

    @Autowired
    private ContextService domainService;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private BackgroundProcessService backgroundProcessService;

    // WARN: Using '.' as separator can be misleading, because for example numbers can contain a dot 0.23% or abbreviations 'e.g.' or ...
    //private final char CHUNK_SEPARATOR_DEFAULT = '.';

    //private final int CHUNK_SIZE_DEFAULT = 50;
    //private final int CHUNK_OVERLAP_DEFAULT = 5;
    //private final char CHUNK_SEPARATOR_DEFAULT = ' ';

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("Do not return answers from Website connector, because Katie is indexing the Website content by itself");
        List<Hit> hits = new ArrayList<Hit>();
        return hits.toArray(new Hit[0]);
    }

    /**
     * @see Connector#update(Context, KnowledgeSourceMeta, WebhookPayload, String)
     */
    public List<Answer> update(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayload payload, String processId) {
        String pageUrl = null;
        if (payload != null) {
            WebhookPayloadWebsite payloadWebsite = (WebhookPayloadWebsite) payload;
            if (payloadWebsite.getPageUrl() != null) {
                pageUrl = payloadWebsite.getPageUrl();
            }
        }
        
        if (pageUrl != null) {
            log.info("Update knowledge source connected with Website '" + ksMeta.getWebsiteSeedUrl() + "' for page URL '" + pageUrl + "' ...");
        } else {
            log.info("Update knowledge source connected with Website '" + ksMeta.getWebsiteSeedUrl() + "' ...");
        }
        try {
            List<Answer> qnas = new ArrayList<Answer>();

            if (pageUrl != null) {
                backgroundProcessService.updateProcessStatus(processId, "TODO: Dump page " + pageUrl + " ...");
                // TODO: Implement dumping and ingesting of one particular page
            } else {
                backgroundProcessService.updateProcessStatus(processId, "Dump " + ksMeta.getWebsiteIndividualURLs().length + " pages of website '" + ksMeta.getWebsiteSeedUrl() + "' ...");
                List<String> urls = dumpWebpages(domain, ksMeta, payload);
                for (String url : urls) {
                    backgroundProcessService.updateProcessStatus(processId, "Dump content of page '" + url + "' ...");
                    File dumpFile = domain.getUrlDumpFile(new URI(url));
                    String cssSelector = null; // TODO: Allow CSS selector, e.g. div[id="content"] resp. [id="content"], because id is supposed to be unique
                    String body = domainService.extractText(dumpFile, cssSelector);
                    String title = domainService.extractTitle(dumpFile, body);
                    String[] chunks = generateSegments(body, ksMeta, url, processId);
                    for (String chunk : chunks) {
                        qnas.add(new Answer(null, chunk, ContentType.TEXT_PLAIN, url, null, null, null, null, null, null, null, null, title, null, false, null, false, null));
                    }
                }
            }

            return qnas;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @return list of dumped URLs
     */
    private List<String> dumpWebpages(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayload payload) {
        List<String> urls = new ArrayList<String>();

        if (ksMeta.getWebsiteSeedUrl() != null) {
            log.info("TODO: Use Crawler to get all pages, e.g. https://nutch.apache.org or https://github.com/simplecrawler/simplecrawler");
        }

        if (ksMeta.getWebsiteIndividualURLs() != null) {
            try {
                for (String url : ksMeta.getWebsiteIndividualURLs()) {
                    domainService.deletePreviouslyImportedChunks(url, domain);
                    File dumpFile = utilsService.dumpContent(domain, new URI(url), null);
                    ContentType contentType = ContentType.TEXT_HTML;
                    domainService.saveMetaInformation(url, url, new Date(), contentType, domain);
                    urls.add(url);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        return urls;
    }

    /**
     *
     */
    private String[] generateSegments(String text, KnowledgeSourceMeta ksMeta, String url, String processId) throws Exception {
        backgroundProcessService.updateProcessStatus(processId, "Chunk (size: " + ksMeta.getChunkSize() + ", overlap: true) content of page '" + url + "' by sentence splitter ...");
        List<String> chunks = segmentationService.splitBySentences(text, "en", ksMeta.getChunkSize(), true);

        //backgroundProcessService.updateProcessStatus(processId, "Chunk (size: " + ksMeta.getChunkSize() + ", overlap: " + ksMeta.getChunkOverlap() + ", separator: '" + ksMeta.getChunkSeparator() + "') content of page '" + url + "' ...");
        //List<String> chunks = segmentationService.getSegments(text, ksMeta.getChunkSeparator(), ksMeta.getChunkSize(), ksMeta.getChunkOverlap());

        //List<String> chunks = segmentationService.getSegmentsUsingAI21(text);
        return chunks.toArray(new String[0]);
    }
}
