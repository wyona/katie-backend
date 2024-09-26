package com.wyona.katie.handlers;

import com.wyona.katie.ai.FeatureModel;
import com.wyona.katie.ai.FeatureModelKeywordsImpl;
import com.wyona.katie.ai.Memory;
import com.wyona.katie.ai.MemoryImpl;

import com.wyona.katie.models.*;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

/**
 * Katie AI based question answer implementation
 */
@Slf4j
@Component
public class KatieQuestionAnswerImpl implements QuestionAnswerHandler {

    @Value("${volume.base.path}")
    private String volumeBasePath;

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) {
        log.info("Katie implementation: Delete tenant ...");
        File memoryDir = getMemoryLocation(domain);
        try {
            FileUtils.deleteDirectory(memoryDir);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * @see QuestionAnswerHandler#createTenant(Context)
     */
    public String createTenant(Context domain) {
        log.info("Katie implementation: Create tenant ...");
        Memory memory = getMemoryImpl(domain);
        memory.erase();
        return null; 
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.info("Index QnA: " + qna.getQuestion() + " | " + qna.getUuid());
        Memory memory = getMemoryImpl(domain);
        //memory.add(qna.getUuid(), qna.getQuestion() + " " + qna.getAnswer());
        memory.add(qna.getUuid(), qna.getQuestion());
        memory.add(qna.getUuid(), qna.getAnswer());
        if (indexAlternativeQuestions) {
            for (String alternativeQuestion : qna.getAlternativeQuestions()) {
                memory.add(qna.getUuid(), alternativeQuestion);
            }
        }
    }

    /**
     * @see QuestionAnswerHandler#retrain(QnA, Context, boolean)
     */
    public void retrain(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Delete/train is just a workaround, implement retrain by itself");
        if (delete(qna.getUuid(), domain)) {
            train(qna, domain, indexAlternativeQuestions);
        } else {
            log.warn("QnA with UUID '" + qna.getUuid() + "' was not deleted and therefore was not retrained!");
        }
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public QnA[] train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Finish implementation to index more than one QnA at the same time!");
        for (QnA qna: qnas) {
            train(qna, domain, indexAlternativeQuestions);
        }

        // TODO: Only return QnAs which got trained successfully
        return qnas;
    }

    /**
     * @see QuestionAnswerHandler#delete(String, Context)
     */
    public boolean delete(String uuid, Context domain) {
        Memory memory = getMemoryImpl(domain);
        try {
            memory.forget(uuid);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(Sentence, Context, int)
     */
    public Hit[] getAnswers(Sentence question, Context context, int limit) {
        log.info("TODO: Consider entities individually and not just the question as a whole!");
        return getAnswers(question.getSentence(), question.getClassifications(), context, limit);
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(String, List, Context, int)
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context domain, int limit) {
        log.info("Get answer(s) from Katie implementation for question '" + question + "' ...");

        List<Hit> answers = new ArrayList<Hit>();

        Memory memory = getMemoryImpl(domain);
        // TODO: Apply classifications and limit
        com.wyona.katie.ai.Hit[] similarTexts = memory.search(question);
        for (com.wyona.katie.ai.Hit hit : similarTexts) {
            String orgQuestion = null;
            Date dateAnswered = null;
            Date dateAnswerModified = null;
            Date dateOriginalQuestionSubmitted = null;

            //String _answer = hit.getText();
            //ContentType answerContentType = ContentType.TEXT_PLAIN;
            String _answer = Answer.AK_UUID_COLON + hit.getForeignKey();
            ContentType answerContentType = null;

            String uuid = hit.getForeignKey(); // null
            log.info("Hit: " + hit);
            Answer answer = new Answer(question, _answer, answerContentType,null, classifications, null, null, dateAnswered, dateAnswerModified, null, domain.getId(), uuid, orgQuestion, dateOriginalQuestionSubmitted, true, null, true, null);
            answers.add(new Hit(answer, hit.getScore()));
        }

        return answers.toArray(new Hit[0]);
    }

    /**
     *
     */
    private Memory getMemoryImpl(Context domain) {
        File modelsDir = new File(volumeBasePath, "models");
        File fileModel = new File(modelsDir, "model1.json");
        File fileFeatures = new File(modelsDir, "features.json");
        FeatureModel featureModel = FeatureModelKeywordsImpl.getInstance(fileModel, fileFeatures);
        return new MemoryImpl(getMemoryLocation(domain), featureModel);

        //String fileNameModel = "model1.json";
        //String fileNameFeatures = "features.json";
        //FeatureModel featureModel = FeatureModelKeywordsImpl.getInstance(fileNameModel, fileNameFeatures);
        //return new MemoryImpl(getMemoryLocation(domain), featureModel);
    }

    /**
     * Get directory containing memory data and index
     */
    private File getMemoryLocation(Context domain) {
        File katieAiDir = new File(domain.getContextDirectory(), "katie-ai");
        File memoryDir = new File(katieAiDir, "memory");
        return memoryDir;
    }
}
