package com.wyona.katie.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.User;
import com.wyona.katie.models.faq.FAQ;
import com.wyona.katie.models.faq.TopicVisibility;
import com.wyona.katie.models.squad.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
public class ImportService {

    public static final String SQUAD_FILE_PREFIX = "squad-import_";
    public static final String IMPORT_PROGRESS_FILE_PREFIX = "progress-import_";

    private static final int BATCH_SIZE = 100;
    private static final int MAX_SIZE = 807;

    @Autowired
    private ContextService domainService;

    /**
     * Batch import of Katie FAQ JSON
     * @param importProcessId UUID of batch import
     */
    @Async
    public void batchImportJSON(Context domain, String language, FAQ faq, boolean isPublic, User signedInUser, String importProcessId, boolean indexAlternativeQuestions) throws Exception {

        // TEST: Uncomment lines below to test thread
        /*
        try {
            for (int i = 0; i < 5; i++) {
                log.info("Sleep for 2 seconds ...");
                Thread.sleep(2000);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
         */

        int totalCountTopics = faq.getTopics().length;
        int totalCountQnAs = -1;

        int currentCountImportedTopics = 0;
        int currentCountImportedQnAs = 0;

        updatedProgressInfo(importProcessId, totalCountTopics, totalCountQnAs, currentCountImportedTopics, currentCountImportedQnAs, "started");

        FAQ currentFAQ = domainService.importFAQ(domain, language, faq, isPublic, signedInUser, indexAlternativeQuestions);

        currentCountImportedTopics = faq.getTopics().length;
        currentCountImportedQnAs = -1;

        updatedProgressInfo(importProcessId, totalCountTopics, totalCountQnAs, currentCountImportedTopics, currentCountImportedQnAs, "completed");
    }

    /**
     * Batch import of SQuAD
     * @param importProcessId UUID of batch import
     */
    @Async
    public void batchImportSQuAD(Context domain, String language, boolean isPublic, String importProcessId, User user, boolean indexAlternativeQuestions) throws Exception {

        // TEST: Uncomment lines below to test thread
        /*
        try {
            for (int i = 0; i < 5; i++) {
                log.info("Sleep for 2 seconds ...");
                Thread.sleep(2000);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
         */

        ObjectMapper jsonPojoMapper = new ObjectMapper();
        log.info("Reading dataset ...");
        File file = new File(System.getProperty("java.io.tmpdir"), SQUAD_FILE_PREFIX + importProcessId);
        if (!file.isFile()) {
            log.error("No such file: " + file.getAbsolutePath());
            return;
        }
        InputStream in = new FileInputStream(file);
        SQuAD squad = jsonPojoMapper.readValue(in, SQuAD.class);
        in.close();
        file.delete();

        int totalCountTopics = squad.getData().length;
        int totalCountQnAs = -1;

        int currentCountImportedTopics = 0;
        int currentCountImportedQnAs = 0;
        updatedProgressInfo(importProcessId, totalCountTopics, totalCountQnAs, currentCountImportedTopics, currentCountImportedQnAs, "started");

        log.info("Processing dataset ...");
        FAQ faq = new FAQ();
        int batchCounter = 0;
        for (Topic topic: squad.getData()) {
            log.info("Add Topic: " + topic.getTitle());
            com.wyona.katie.models.faq.Topic currentTopic = new com.wyona.katie.models.faq.Topic(UUID.randomUUID().toString(), topic.getTitle(), null, TopicVisibility.PUBLIC);
            faq.addTopic(currentTopic);
            int topicParagraphCounter = 0;
            for (Paragraph para: topic.getParagraphs()) {
                for (QnAs qnas: para.getQas()) {
                    for (Answer answer: qnas.getAnswers()) {
                        log.info("Add QnA (Q: " + qnas.getQuestion() + " | A: " + answer.getText() + ") of topic '" + currentTopic.getTitle() + "' ...");
                        currentTopic.addQuestion(null, qnas.getQuestion(), answer.getText());
                        currentCountImportedQnAs++;
                        batchCounter++;

                        if (batchCounter == BATCH_SIZE) {
                            log.info("Batch size '" + BATCH_SIZE + "' reached, therefore save QnAs persistently ...");
                            domainService.importFAQ(domain, language, faq, isPublic, user, indexAlternativeQuestions);
                            updatedProgressInfo(importProcessId, totalCountTopics, totalCountQnAs, currentCountImportedTopics, currentCountImportedQnAs, "in-progress");
                            faq = new FAQ();
                            currentTopic = new com.wyona.katie.models.faq.Topic(currentTopic.getId(), topic.getTitle(), null, currentTopic.getVisibility());
                            faq.addTopic(currentTopic);
                            batchCounter = 0;
                        }

                        if ( currentCountImportedQnAs == MAX_SIZE) {
                            domainService.importFAQ(domain, language, faq, isPublic, user, indexAlternativeQuestions);
                            updatedProgressInfo(importProcessId, totalCountTopics, totalCountQnAs, currentCountImportedTopics, currentCountImportedQnAs, "aborted");
                            log.info("Import aborted, because max import size " + MAX_SIZE + " reached.");
                            return;
                        }

                        break; // INFO: Only first answer
                    }
                }
                topicParagraphCounter++;
                log.info(topicParagraphCounter + " paragraphs of topic '" + currentTopic.getTitle() + "' processed.");
            }
            domainService.importFAQ(domain, language, faq, isPublic, user, indexAlternativeQuestions);
            log.info("End of topic '" + topic.getTitle() + "' reached, therefore remaining QnAs have been saved persistently");
            currentCountImportedTopics++;
            updatedProgressInfo(importProcessId, totalCountTopics, totalCountQnAs, currentCountImportedTopics, currentCountImportedQnAs, "in-progress");
            faq = new FAQ();
            batchCounter = 0;
        }

        updatedProgressInfo(importProcessId, totalCountTopics, totalCountQnAs, currentCountImportedTopics, currentCountImportedQnAs, "completed");
    }

    /**
     * @param status Progress status, e.g. "started", "in-progress", "completed", "aborted"
     */
    private void updatedProgressInfo(String importProcessId, int totalCountTopics, int totalCountQnAs, int currentCountImportedTopics, int currentCountImportedQnAs, String status) {
        // TODO: Link "import process id" with "thread id", such that client can stop import

        StringBuilder body = new StringBuilder("{");
        body.append("\"status\":\"" + status + "\"");
        body.append(",");
        body.append("\"batch-size\":" + BATCH_SIZE);
        body.append(",");
        body.append("\"max-size\":" + MAX_SIZE);
        body.append(",");
        body.append("\"total-topics\":" + totalCountTopics);
        body.append(",");
        body.append("\"imported-topics\":" + currentCountImportedTopics);
        body.append(",");
        body.append("\"total-qnas\":" + totalCountQnAs);
        body.append(",");
        body.append("\"imported-qnas\":" + currentCountImportedQnAs);
        body.append("}");

        log.info(body.toString());

        File file = new File(System.getProperty("java.io.tmpdir"), IMPORT_PROGRESS_FILE_PREFIX + importProcessId);

        try {
            log.info("Log progress of import: " + file.getAbsolutePath());
            Files.write(Paths.get(file.getAbsolutePath()), body.toString().getBytes());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
